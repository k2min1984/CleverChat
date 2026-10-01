package kr.co.cleverchat.domain.settings;

import java.net.URI;
import kr.co.cleverchat.common.audit.Audited;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import kr.co.cleverchat.domain.crawl.service.CrawlJsonExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingsService {
    private final SystemSettingsMapper mapper;
    private final String gatewaySecret;

    public SystemSettingsService(
            SystemSettingsMapper mapper,
            @Value("${cleverchat.gateway.shared-secret:}") String gatewaySecret) {
        this.mapper = mapper;
        this.gatewaySecret = gatewaySecret;
    }

    public SystemSettings current() {
        SystemSettings value = mapper.get();
        if (value == null) throw new IllegalStateException("시스템 설정이 초기화되지 않았습니다.");
        return value;
    }

    public boolean isGatewayConfigured() {
        return gatewaySecret != null && !gatewaySecret.isBlank();
    }

    public String exportDirectory(String targetDirectory) {
        String directory = trim(targetDirectory);
        return directory == null ? current().crawlExportDirectory() : directory;
    }

    @Transactional
    @RequireRole("ADMIN")
    @Audited(action = "SYSTEM_SETTINGS_UPDATE", targetType = "SYSTEM_SETTINGS")
    public SystemSettings save(SystemSettings value, String actor) {
        if (!"LOCAL".equals(value.operationMode()) && !"GATEWAY".equals(value.operationMode()))
            throw new IllegalArgumentException("운영 방식을 선택해 주세요.");
        String serviceId = trim(value.serviceId());
        if (serviceId == null || !serviceId.matches("[a-z0-9-]{1,32}"))
            throw new IllegalArgumentException("서비스 ID는 영소문자·숫자·하이픈 1~32자로 입력하세요.");
        String gatewayUrl = trim(value.gatewayUrl());
        if (gatewayUrl != null) {
            try {
                URI uri = URI.create(gatewayUrl);
                if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                        || uri.getHost() == null
                        || uri.getUserInfo() != null
                        || uri.getFragment() != null
                        || uri.getQuery() != null
                        || gatewayUrl.length() > 1000) throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("게이트웨이 주소는 사용자 정보·쿼리 없는 http(s) 주소로 입력하세요.");
            }
        }
        if (value.isLinked() && (!isGatewayConfigured() || gatewayUrl == null))
            throw new IllegalArgumentException(
                    "연계 적용에는 게이트웨이 주소와 서버의 SSO 시크릿 설정이 필요합니다. 준비 전에는 독립으로 저장하세요.");
        String directory = trim(value.crawlExportDirectory());
        if (directory == null && mapper.inheritedExportTargets() > 0)
            throw new IllegalArgumentException("기본 폴더를 사용하는 크롤링 대상이 있습니다. 다른 저장 폴더를 지정해 주세요.");
        if (directory != null) directory = CrawlJsonExporter.validateDirectory(directory);
        SystemSettings normalized =
                new SystemSettings(
                        value.operationMode(),
                        gatewayUrl,
                        serviceId,
                        directory,
                        value.version(),
                        value.authVersion(),
                        actor,
                        null);
        if (mapper.update(normalized, actor) != 1)
            throw new IllegalArgumentException("다른 관리자가 설정을 변경했습니다. 새로고침 후 다시 저장하세요.");
        return current();
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
