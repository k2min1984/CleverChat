package kr.co.cleverchat.domain.crawl.browser;

import kr.co.cleverchat.domain.settings.RuntimeSetting;
import kr.co.cleverchat.domain.settings.RuntimeSettingsService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cleverchat.crawl.browser")
public class CrawlBrowserProperties {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RuntimeSettingsService runtimeSettings;

    private boolean enabled = false;
    private int pollDelayMs = 5000;
    private int maxConcurrent = 1;
    private int jobTimeoutMinutes = 180;
    private int maxAttempts = 2;
    private int boardRetryAttempts = 3;
    private int boardRetryDelayMs = 10000;

    /** 0 means unlimited. Completion is determined by the board's last page. */
    private int maxPages = 0;

    /** 0 means unlimited. */
    private int maxDetails = 0;

    private int navigationTimeoutMs = 30000;
    private String browsersPath;
    private String nativeLibraryPath;
    private String proxyServer;
    private String proxyUsername;
    private String proxyPassword;
    private String caCertificatePath;
    private boolean nativeUploadEnabled = false;
    private String nativeStagingPath;
    private String nativeActivePath;
    private long nativeMaxBundleBytes = 50L * 1024L * 1024L;
    private String nativeAllowedSonames = "";
    private String nativeTrustedSignatures = "";
    private String rowSelector =
            "main a[href^='javascript:fn_Detail'], " + "main a[href^='javascript:fn_SubDetail']";
    private String inlineRowSelector =
            "main .card-list-box.column-03 > .card.link.style01, "
                    + "main .row > .card.link.style01, "
                    + "main .internalrule-content .card.board, "
                    + "main .public-resources-list > .public-resources-list-item, "
                    + "main .esgreport-container .esgreport-top-box, "
                    + "main .esgreport-container .card, "
                    + "main .media-container .media-list-item, "
                    + "main .content-list-wrap > .list-item, "
                    + "main .board-list-tbody > .board-list-row";
    private String titleSelector =
            "main .board-detail .sub-component-title, main .board-detail .detail-top h4, "
                    + "main h1, main h2";
    private String contentSelector =
            "main .detail-content, main .board-detail .detail-content, "
                    + "main .board-detail article, main .board-detail, main .container-box-m";
    private String nextSelector = ".pagination .arrow-box.next a[onclick], a[rel=next], a.next";

    public boolean isEnabled() {
        return runtimeSettings == null
                ? enabled
                : runtimeSettings.current().bool(RuntimeSetting.BROWSER_ENABLED);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPollDelayMs() {
        return pollDelayMs;
    }

    public void setPollDelayMs(int pollDelayMs) {
        this.pollDelayMs = Math.max(1000, pollDelayMs);
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = Math.max(1, maxConcurrent);
    }

    public int getJobTimeoutMinutes() {
        return runtimeSettings == null
                ? jobTimeoutMinutes
                : runtimeSettings.current().integer(RuntimeSetting.JOB_TIMEOUT_MINUTES);
    }

    public void setJobTimeoutMinutes(int jobTimeoutMinutes) {
        this.jobTimeoutMinutes = Math.max(1, jobTimeoutMinutes);
    }

    public int getMaxAttempts() {
        return runtimeSettings == null
                ? maxAttempts
                : runtimeSettings.current().integer(RuntimeSetting.JOB_ATTEMPTS);
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public int getBoardRetryAttempts() {
        return runtimeSettings == null
                ? boardRetryAttempts
                : runtimeSettings.current().integer(RuntimeSetting.BOARD_ATTEMPTS);
    }

    public void setBoardRetryAttempts(int boardRetryAttempts) {
        this.boardRetryAttempts = Math.max(1, boardRetryAttempts);
    }

    public int getBoardRetryDelayMs() {
        return runtimeSettings == null
                ? boardRetryDelayMs
                : runtimeSettings.current().integer(RuntimeSetting.BOARD_RETRY_DELAY_MS);
    }

    public void setBoardRetryDelayMs(int boardRetryDelayMs) {
        this.boardRetryDelayMs = Math.max(0, boardRetryDelayMs);
    }

    public int getMaxPages() {
        return runtimeSettings == null
                ? maxPages
                : runtimeSettings.current().integer(RuntimeSetting.BOARD_PAGE_LIMIT);
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = Math.max(0, maxPages);
    }

    public int getMaxDetails() {
        return runtimeSettings == null
                ? maxDetails
                : runtimeSettings.current().integer(RuntimeSetting.BOARD_DETAIL_LIMIT);
    }

    public void setMaxDetails(int maxDetails) {
        this.maxDetails = Math.max(0, maxDetails);
    }

    public int getNavigationTimeoutMs() {
        return runtimeSettings == null
                ? navigationTimeoutMs
                : runtimeSettings.current().integer(RuntimeSetting.BROWSER_TIMEOUT_MS);
    }

    public void setNavigationTimeoutMs(int navigationTimeoutMs) {
        this.navigationTimeoutMs = Math.max(5000, navigationTimeoutMs);
    }

    public String getBrowsersPath() {
        return browsersPath;
    }

    public void setBrowsersPath(String browsersPath) {
        this.browsersPath = blankToNull(browsersPath);
    }

    public String getNativeLibraryPath() {
        return nativeLibraryPath;
    }

    public void setNativeLibraryPath(String nativeLibraryPath) {
        this.nativeLibraryPath = blankToNull(nativeLibraryPath);
    }

    public String getProxyServer() {
        return proxyServer;
    }

    public void setProxyServer(String proxyServer) {
        this.proxyServer = blankToNull(proxyServer);
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = blankToNull(proxyUsername);
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = blankToNull(proxyPassword);
    }

    public String getCaCertificatePath() {
        return caCertificatePath;
    }

    public void setCaCertificatePath(String caCertificatePath) {
        this.caCertificatePath = blankToNull(caCertificatePath);
    }

    public boolean isNativeUploadEnabled() {
        return nativeUploadEnabled;
    }

    public void setNativeUploadEnabled(boolean nativeUploadEnabled) {
        this.nativeUploadEnabled = nativeUploadEnabled;
    }

    public String getNativeStagingPath() {
        return nativeStagingPath;
    }

    public void setNativeStagingPath(String nativeStagingPath) {
        this.nativeStagingPath = blankToNull(nativeStagingPath);
    }

    public String getNativeActivePath() {
        return nativeActivePath == null ? nativeLibraryPath : nativeActivePath;
    }

    public void setNativeActivePath(String nativeActivePath) {
        this.nativeActivePath = blankToNull(nativeActivePath);
    }

    public long getNativeMaxBundleBytes() {
        return nativeMaxBundleBytes;
    }

    public void setNativeMaxBundleBytes(long nativeMaxBundleBytes) {
        this.nativeMaxBundleBytes = Math.max(1024L, nativeMaxBundleBytes);
    }

    public String getNativeAllowedSonames() {
        return nativeAllowedSonames;
    }

    public void setNativeAllowedSonames(String nativeAllowedSonames) {
        this.nativeAllowedSonames = nativeAllowedSonames == null ? "" : nativeAllowedSonames.trim();
    }

    public String getNativeTrustedSignatures() {
        return nativeTrustedSignatures;
    }

    public void setNativeTrustedSignatures(String nativeTrustedSignatures) {
        this.nativeTrustedSignatures =
                nativeTrustedSignatures == null ? "" : nativeTrustedSignatures.trim();
    }

    public String getRowSelector() {
        return rowSelector;
    }

    public void setRowSelector(String rowSelector) {
        this.rowSelector = defaultIfBlank(rowSelector, this.rowSelector);
    }

    public String getInlineRowSelector() {
        return inlineRowSelector;
    }

    public void setInlineRowSelector(String inlineRowSelector) {
        this.inlineRowSelector = defaultIfBlank(inlineRowSelector, this.inlineRowSelector);
    }

    public String getTitleSelector() {
        return titleSelector;
    }

    public void setTitleSelector(String titleSelector) {
        this.titleSelector = defaultIfBlank(titleSelector, this.titleSelector);
    }

    public String getContentSelector() {
        return contentSelector;
    }

    public void setContentSelector(String contentSelector) {
        this.contentSelector = defaultIfBlank(contentSelector, this.contentSelector);
    }

    public String getNextSelector() {
        return nextSelector;
    }

    public void setNextSelector(String nextSelector) {
        this.nextSelector = defaultIfBlank(nextSelector, this.nextSelector);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String normalized = blankToNull(value);
        return normalized == null ? defaultValue : normalized;
    }
}
