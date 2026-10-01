package kr.co.cleverchat.domain.crawl.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PlaywrightBrowserProvider implements AutoCloseable {
    private final CrawlBrowserProperties properties;
    private Playwright playwright;
    private Browser browser;

    public PlaywrightBrowserProvider(CrawlBrowserProperties properties) {
        this.properties = properties;
    }

    public synchronized Browser browser() {
        validateRuntimePaths();
        if (browser == null || !browser.isConnected()) {
            if (properties.getBrowsersPath() != null) {
                System.setProperty("PLAYWRIGHT_BROWSERS_PATH", properties.getBrowsersPath());
            }
            playwright = Playwright.create();
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true);
            Map<String, String> env = new LinkedHashMap<>();
            if (properties.getBrowsersPath() != null) {
                env.put("PLAYWRIGHT_BROWSERS_PATH", properties.getBrowsersPath());
            }
            if (properties.getNativeLibraryPath() != null) {
                env.put("LD_LIBRARY_PATH", properties.getNativeLibraryPath());
            }
            if (properties.getCaCertificatePath() != null) {
                env.put("NODE_EXTRA_CA_CERTS", properties.getCaCertificatePath());
                env.put("SSL_CERT_FILE", properties.getCaCertificatePath());
            }
            if (!env.isEmpty()) {
                options.setEnv(env);
            }
            if (properties.getProxyServer() != null) {
                Proxy proxy = new Proxy(properties.getProxyServer());
                proxy.setUsername(properties.getProxyUsername());
                proxy.setPassword(properties.getProxyPassword());
                options.setProxy(proxy);
            }
            browser = playwright.chromium().launch(options);
        }
        return browser;
    }

    private void validateRuntimePaths() {
        requireDirectory(properties.getBrowsersPath(), "PLAYWRIGHT_BROWSERS_PATH");
        requireDirectory(properties.getNativeLibraryPath(), "LD_LIBRARY_PATH");
        requireReadableFile(properties.getCaCertificatePath(), "CA certificate");
    }

    private void requireDirectory(String value, String label) {
        if (value != null && !Files.isDirectory(Path.of(value))) {
            throw new IllegalStateException(
                    label + " does not exist or is not a directory: " + value);
        }
    }

    private void requireReadableFile(String value, String label) {
        if (value != null && !Files.isReadable(Path.of(value))) {
            throw new IllegalStateException(label + " is not readable: " + value);
        }
    }

    @Override
    public synchronized void close() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }
}
