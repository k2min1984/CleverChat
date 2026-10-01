package kr.co.cleverchat.domain.crawl.browser;

import java.time.OffsetDateTime;

public class CrawlNativeLibBundle {
    private Long nativeLibRegistryNo;
    private String bundleVersion;
    private String originalFileName;
    private String stagingPath;
    private String activePath;
    private String checksumSha256;
    private String signature;
    private String allowedSonames;
    private String status;
    private Long uploadedBy;
    private OffsetDateTime uploadedAt;
    private Long activatedBy;
    private OffsetDateTime activatedAt;
    private String message;

    public Long getNativeLibRegistryNo() {
        return nativeLibRegistryNo;
    }

    public void setNativeLibRegistryNo(Long nativeLibRegistryNo) {
        this.nativeLibRegistryNo = nativeLibRegistryNo;
    }

    public String getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(String bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStagingPath() {
        return stagingPath;
    }

    public void setStagingPath(String stagingPath) {
        this.stagingPath = stagingPath;
    }

    public String getActivePath() {
        return activePath;
    }

    public void setActivePath(String activePath) {
        this.activePath = activePath;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAllowedSonames() {
        return allowedSonames;
    }

    public void setAllowedSonames(String allowedSonames) {
        this.allowedSonames = allowedSonames;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(OffsetDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Long getActivatedBy() {
        return activatedBy;
    }

    public void setActivatedBy(Long activatedBy) {
        this.activatedBy = activatedBy;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(OffsetDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
