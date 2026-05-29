package kr.co.cleverchat.domain.crawl.service;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CrawlUrlPolicy {

    private final Set<String> allowedHosts;

    CrawlUrlPolicy() {
        this("");
    }

    @Autowired
    public CrawlUrlPolicy(
            @Value("${cleverchat.crawl.url-policy.allowed-hosts:}") String allowedHosts) {
        this.allowedHosts = parseAllowedHosts(allowedHosts);
    }

    public URI validateAndNormalize(String rawUrl) {
        URI uri;
        try {
            uri = URI.create(rawUrl == null ? "" : rawUrl.trim()).normalize();
        } catch (IllegalArgumentException e) {
            throw blocked("Crawl URL is invalid.");
        }

        String scheme = uri.getScheme();
        if (scheme == null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw blocked("Only http and https crawl URLs are allowed.");
        }
        if (uri.getUserInfo() != null) {
            throw blocked("Crawl URL user info is not allowed.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw blocked("Crawl URL host is required.");
        }
        validateHost(host);
        return uri;
    }

    private void validateHost(String host) {
        String normalizedHost = normalizeHost(host);
        if (allowedHosts.contains(normalizedHost)) {
            return;
        }
        if ("localhost".equals(normalizedHost) || normalizedHost.endsWith(".localhost")) {
            throw blocked("Localhost crawl URLs are blocked.");
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw blocked("Crawl URL host cannot be resolved.");
        }
        for (InetAddress address : addresses) {
            if (isBlocked(address)) {
                throw blocked("Private or local crawl URLs are blocked.");
            }
        }
    }

    private boolean isBlocked(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address);
    }

    private Set<String> parseAllowedHosts(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split("[,;]"))
                .map(String::trim)
                .filter(host -> !host.isBlank())
                .map(this::normalizeHost)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalizeHost(String host) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
            return normalizedHost.substring(1, normalizedHost.length() - 1);
        }
        return normalizedHost;
    }

    private boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte firstByte = address.getAddress()[0];
        return (firstByte & (byte) 0xfe) == (byte) 0xfc;
    }

    private BusinessException blocked(String message) {
        return new BusinessException(ErrorCode.CRAWL_URL_BLOCKED, message);
    }
}
