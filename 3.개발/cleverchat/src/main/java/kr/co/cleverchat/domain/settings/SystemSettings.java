package kr.co.cleverchat.domain.settings;

import java.time.OffsetDateTime;

public record SystemSettings(
        String operationMode,
        String gatewayUrl,
        String serviceId,
        String crawlExportDirectory,
        long version,
        long authVersion,
        String updatedBy,
        OffsetDateTime updatedAt) {
    public boolean isLinked() {
        return "GATEWAY".equals(operationMode);
    }
}
