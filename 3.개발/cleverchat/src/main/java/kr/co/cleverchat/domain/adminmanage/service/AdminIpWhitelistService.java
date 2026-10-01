package kr.co.cleverchat.domain.adminmanage.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import kr.co.cleverchat.common.audit.Audited;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.adminmanage.mapper.AdminIpWhitelistMapper;
import kr.co.cleverchat.domain.adminmanage.model.AdminIpWhitelist;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminIpWhitelistService {
    private static final String ACTIVE_CACHE_KEY = "active";
    private static final String CURRENT_IP_BLOCKED_MESSAGE = "현재 접속 IP가 허용목록에서 제외되어 변경할 수 없습니다.";

    private final AdminIpWhitelistMapper mapper;
    private final Cache<String, List<AdminIpWhitelist>> activeCache =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(60)).maximumSize(1).build();

    public AdminIpWhitelistService(AdminIpWhitelistMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AdminIpWhitelist> entries() {
        return mapper.findAll();
    }

    @Transactional(readOnly = true)
    public AdminIpWhitelist entry(Long id) {
        AdminIpWhitelist entry = mapper.findById(id);
        if (entry == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entry;
    }

    public boolean isAllowed(String clientIp) {
        if (isLoopback(clientIp)) {
            return true;
        }
        return isAllowedByRules(activeEntries(), clientIp);
    }

    @Transactional
    @Audited(action = "ADMIN_IP_WHITELIST_ADD", targetType = "ADMIN_IP_WHITELIST")
    @RequireRole("ADMIN")
    public AdminIpWhitelist addEntry(
            String ipCidr, String description, String actor, String clientIp) {
        AdminIpWhitelist entry = new AdminIpWhitelist();
        entry.setIpCidr(normalizeRule(ipCidr));
        entry.setDescription(truncate(blankToNull(description), 200));
        entry.setUseYn("Y");
        entry.setFrstRegrEmpno(actor);
        entry.setLstChgrEmpno(actor);
        entry.setFrstRegrIp(clientIp);
        entry.setLstChgrIp(clientIp);
        ensureValidRule(entry.getIpCidr());
        ensureCurrentIpStillAllowed(afterAdd(entry), clientIp);
        mapper.insert(entry);
        invalidate();
        return mapper.findById(entry.getAdminIpWhitelistNo());
    }

    @Transactional
    @Audited(action = "ADMIN_IP_WHITELIST_UPDATE", targetType = "ADMIN_IP_WHITELIST")
    @RequireRole("ADMIN")
    public AdminIpWhitelist updateEntry(
            Long id,
            String ipCidr,
            String description,
            String useYn,
            String actor,
            String clientIp) {
        AdminIpWhitelist existing = entry(id);
        AdminIpWhitelist updated = new AdminIpWhitelist();
        updated.setAdminIpWhitelistNo(id);
        updated.setIpCidr(normalizeRule(ipCidr));
        updated.setDescription(truncate(blankToNull(description), 200));
        updated.setUseYn("N".equalsIgnoreCase(useYn) ? "N" : "Y");
        updated.setLstChgrEmpno(actor);
        updated.setLstChgrIp(clientIp);
        ensureValidRule(updated.getIpCidr());
        ensureCurrentIpStillAllowed(afterUpdate(existing, updated), clientIp);
        int updatedRows = mapper.update(updated);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        invalidate();
        return mapper.findById(id);
    }

    @Transactional
    @Audited(action = "ADMIN_IP_WHITELIST_DISABLE", targetType = "ADMIN_IP_WHITELIST")
    @RequireRole("ADMIN")
    public AdminIpWhitelist disableEntry(Long id, String actor, String clientIp) {
        AdminIpWhitelist existing = entry(id);
        ensureCurrentIpStillAllowed(afterDisable(existing), clientIp);
        int updatedRows = mapper.disable(id, actor, clientIp);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        invalidate();
        return mapper.findById(id);
    }

    private List<AdminIpWhitelist> activeEntries() {
        return activeCache.get(
                ACTIVE_CACHE_KEY,
                key -> {
                    List<AdminIpWhitelist> active = mapper.findActive();
                    return active == null ? List.of() : List.copyOf(active);
                });
    }

    private List<AdminIpWhitelist> afterAdd(AdminIpWhitelist newEntry) {
        List<AdminIpWhitelist> next = new ArrayList<>(activeEntries());
        if ("Y".equals(newEntry.getUseYn())) {
            next.add(newEntry);
        }
        return next;
    }

    private List<AdminIpWhitelist> afterUpdate(
            AdminIpWhitelist existing, AdminIpWhitelist updatedEntry) {
        List<AdminIpWhitelist> next = new ArrayList<>();
        for (AdminIpWhitelist entry : activeEntries()) {
            if (!entry.getAdminIpWhitelistNo().equals(existing.getAdminIpWhitelistNo())) {
                next.add(entry);
            }
        }
        if ("Y".equals(updatedEntry.getUseYn())) {
            next.add(updatedEntry);
        }
        return next;
    }

    private List<AdminIpWhitelist> afterDisable(AdminIpWhitelist disabledEntry) {
        List<AdminIpWhitelist> next = new ArrayList<>();
        for (AdminIpWhitelist entry : activeEntries()) {
            if (!entry.getAdminIpWhitelistNo().equals(disabledEntry.getAdminIpWhitelistNo())) {
                next.add(entry);
            }
        }
        return next;
    }

    private void ensureCurrentIpStillAllowed(List<AdminIpWhitelist> nextActive, String clientIp) {
        if (!isLoopback(clientIp) && !isAllowedByRules(nextActive, clientIp)) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, CURRENT_IP_BLOCKED_MESSAGE);
        }
    }

    private boolean isAllowedByRules(List<AdminIpWhitelist> rules, String clientIp) {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        for (AdminIpWhitelist rule : rules) {
            if (rule != null && IpCidrMatcher.matches(rule.getIpCidr(), clientIp)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLoopback(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }
        try {
            return InetAddress.getByName(clientIp.trim()).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private void ensureValidRule(String ipCidr) {
        if (!IpCidrMatcher.isValidRule(ipCidr)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "IP 또는 CIDR 형식을 확인해 주세요.");
        }
    }

    private void invalidate() {
        activeCache.invalidate(ACTIVE_CACHE_KEY);
    }

    private String normalizeRule(String value) {
        String normalized = blankToNull(value);
        if (normalized == null || normalized.length() > 64) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "IP 또는 CIDR 형식을 확인해 주세요.");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
