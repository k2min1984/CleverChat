package kr.co.cleverchat.domain.externalapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cleverchat.external-api")
public class ExternalApiProperties {

    private boolean enabled;
    private String apiKeySha256 = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKeySha256() {
        return apiKeySha256;
    }

    public void setApiKeySha256(String apiKeySha256) {
        this.apiKeySha256 = apiKeySha256 == null ? "" : apiKeySha256;
    }
}
