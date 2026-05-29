package kr.co.cleverchat.domain.crawl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

class CrawlUrlPolicyTest {

    private final CrawlUrlPolicy policy = new CrawlUrlPolicy();

    @Test
    void blocksLocalhost() {
        assertBlocked("http://localhost/admin");
    }

    @Test
    void blocksPrivateIp() {
        assertBlocked("https://192.168.0.10/help");
    }

    @Test
    void blocksNonHttpScheme() {
        assertBlocked("file:///etc/passwd");
    }

    @Test
    void allowsPublicHttpUrl() {
        assertThat(policy.validateAndNormalize("https://93.184.216.34/help").toString())
                .isEqualTo("https://93.184.216.34/help");
    }

    @Test
    void allowsLocalhostWhenExplicitlyAllowlisted() {
        CrawlUrlPolicy allowlistedPolicy = new CrawlUrlPolicy("localhost");

        assertThat(allowlistedPolicy.validateAndNormalize("http://localhost:3000/help").toString())
                .isEqualTo("http://localhost:3000/help");
    }

    @Test
    void allowsPrivateIpWhenExplicitlyAllowlisted() {
        CrawlUrlPolicy allowlistedPolicy = new CrawlUrlPolicy("10.0.0.5");

        assertThat(allowlistedPolicy.validateAndNormalize("http://10.0.0.5/help").toString())
                .isEqualTo("http://10.0.0.5/help");
    }

    @Test
    void allowlistDoesNotOpenOtherPrivateHosts() {
        CrawlUrlPolicy allowlistedPolicy = new CrawlUrlPolicy("localhost");

        assertThatThrownBy(() -> allowlistedPolicy.validateAndNormalize("http://127.0.0.1/help"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CRAWL_URL_BLOCKED);
    }

    private void assertBlocked(String url) {
        assertThatThrownBy(() -> policy.validateAndNormalize(url))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CRAWL_URL_BLOCKED);
    }
}
