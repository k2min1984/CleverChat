package kr.co.cleverchat.domain.settings;

import static kr.co.cleverchat.domain.settings.RuntimeSetting.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalTime;
import java.util.*;
import kr.co.cleverchat.common.audit.Audited;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuntimeSettingsService {
    private final SystemSettingsMapper mapper;
    private final ObjectMapper json;
    private final Map<String, Object> defaults;
    private volatile Snapshot cached;
    private volatile long expires;
    private final ThreadLocal<Snapshot> crawlSnapshot = new ThreadLocal<>();

    public RuntimeSettingsService(SystemSettingsMapper mapper, ObjectMapper json, Environment env) {
        this.mapper = mapper;
        this.json = json;
        var values = new LinkedHashMap<String, Object>();
        for (RuntimeSetting setting : RuntimeSetting.values()) {
            String raw = setting.property == null ? null : env.getProperty(setting.property);
            values.put(setting.name(), raw == null ? setting.defaultValue : parse(setting, raw));
        }
        defaults = Map.copyOf(values);
    }

    public Snapshot current() {
        Snapshot scope = crawlSnapshot.get();
        if (scope != null) return scope;
        Snapshot value = cached;
        if (value != null && System.nanoTime() < expires) return value;
        synchronized (this) {
            if (cached == null || System.nanoTime() >= expires) {
                var values = new LinkedHashMap<>(defaults);
                values.putAll(readSaved());
                cached = new Snapshot(Collections.unmodifiableMap(values));
                expires = System.nanoTime() + 1_000_000_000L;
            }
            return cached;
        }
    }

    public synchronized void invalidate() {
        cached = null;
    }

    public void runWithSnapshot(Runnable work) {
        Snapshot previous = crawlSnapshot.get();
        crawlSnapshot.set(current());
        try {
            work.run();
        } finally {
            if (previous == null) crawlSnapshot.remove();
            else crawlSnapshot.set(previous);
        }
    }

    @Transactional
    @RequireRole("ADMIN")
    @Audited(action = "RUNTIME_SETTINGS_UPDATE", targetType = "SYSTEM_SETTINGS")
    public void save(String section, Map<String, String> input, long version, String actor) {
        List<RuntimeSetting> fields = fields(section);
        if (fields.isEmpty() || version < 0) throw new IllegalArgumentException("올바른 설정 항목이 아닙니다.");
        var saved = readSaved();
        for (RuntimeSetting field : fields) {
            String raw = input.get(field.name());
            if (raw == null) throw new IllegalArgumentException(field.label + " 값을 입력해 주세요.");
            saved.put(field.name(), parse(field, raw));
        }
        var combined = new LinkedHashMap<>(defaults);
        combined.putAll(saved);
        Snapshot proposed = new Snapshot(combined);
        validate(proposed, section);
        try {
            if (mapper.updateRuntime(json.writeValueAsString(saved), version, actor) != 1)
                throw new IllegalArgumentException("다른 관리자가 설정을 변경했습니다. 새로고침 후 다시 저장하세요.");
            invalidate();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<RuntimeSetting> fields(String section) {
        return Arrays.stream(RuntimeSetting.values())
                .filter(s -> s.section.equals(section))
                .toList();
    }

    private Map<String, Object> readSaved() {
        try {
            String value = mapper.getRuntimeJson();
            return value == null
                    ? new LinkedHashMap<>()
                    : json.readValue(value, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("운영 설정을 읽을 수 없습니다.", e);
        }
    }

    private Object parse(RuntimeSetting field, String raw) {
        try {
            String value = raw.trim();
            if (field.type.equals("boolean")) {
                if (!value.equals("true") && !value.equals("false"))
                    throw new IllegalArgumentException();
                return Boolean.parseBoolean(value);
            }
            if (field.type.equals("number") || field.type.equals("decimal")) {
                double number = Double.parseDouble(value);
                if (!Double.isFinite(number) || number < field.min || number > field.max)
                    throw new IllegalArgumentException();
                if (field.type.equals("number")) {
                    if (number != Math.rint(number)) throw new IllegalArgumentException();
                    return (int) number;
                }
                return number;
            }
            if (field.type.equals("time")) {
                if (!value.matches("[0-2][0-9]:[0-5][0-9]")) throw new IllegalArgumentException();
                LocalTime.parse(value);
            } else if (value.length() < field.min
                    || value.length() > field.max
                    || value.chars().anyMatch(c -> c == 0 || c == 127))
                throw new IllegalArgumentException();
            if (field == AI_BASE_URL && !value.isEmpty()) {
                URI uri = URI.create(value);
                if (!Set.of("http", "https").contains(uri.getScheme())
                        || uri.getHost() == null
                        || uri.getUserInfo() != null
                        || uri.getQuery() != null
                        || uri.getFragment() != null) throw new IllegalArgumentException();
                value = value.replaceAll("/+$", "");
                if (!URI.create(value).getPath().endsWith("/v1"))
                    throw new IllegalArgumentException();
            }
            return value;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    field.label
                            + " 값을 확인하세요."
                            + (field == AI_BASE_URL
                                    ? " http(s)로 시작하고 /v1으로 끝나는 API 기본 주소를 입력하세요."
                                    : ""));
        }
    }

    private void validate(Snapshot config, String section) {
        if (section.equals("search")
                && config.integer(SEARCH_POOL) < config.integer(SEARCH_DISPLAY))
            throw new IllegalArgumentException("검색 후보 개수는 표시 개수 이상이어야 합니다.");
        if (section.equals("ai")
                && config.bool(AI_ENABLED)
                && (config.text(AI_BASE_URL).isBlank() || config.text(AI_MODEL).isBlank()))
            throw new IllegalArgumentException("AI 답변을 사용하려면 vLLM 주소와 모델명을 입력하세요.");
    }

    public record Snapshot(Map<String, Object> values) {
        public boolean bool(RuntimeSetting key) {
            return Boolean.TRUE.equals(values.get(key.name()));
        }

        public int integer(RuntimeSetting key) {
            return ((Number) values.get(key.name())).intValue();
        }

        public double decimal(RuntimeSetting key) {
            return ((Number) values.get(key.name())).doubleValue();
        }

        public String text(RuntimeSetting key) {
            return (String) values.get(key.name());
        }
    }
}
