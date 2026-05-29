package kr.co.cleverchat.domain.externalapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ExternalApiControllerTest {

    @Test
    void disabledStatusDoesNotExposeBusinessMetadata() throws Exception {
        MockMvc mvc = mockMvc(false, "");

        MvcResult result =
                mvc.perform(get("/external/api/v1/status"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.error.code").value("EXTERNAL_API_DISABLED"))
                        .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("scenario")
                .doesNotContain("chat_session")
                .doesNotContain("admin")
                .doesNotContain("search_log");
    }

    @Test
    void enabledWithoutConfiguredKeyFailsClosed() throws Exception {
        MockMvc mvc = mockMvc(true, "");

        mvc.perform(get("/external/api/v1/status").header("X-CleverChat-Api-Key", "raw-secret"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EXTERNAL_API_NOT_CONFIGURED"));
    }

    @Test
    void enabledWithMissingOrInvalidKeyReturnsUnauthorizedWithoutRawKey() throws Exception {
        MockMvc mvc = mockMvc(true, sha256("expected-secret"));

        MvcResult result =
                mvc.perform(
                                get("/external/api/v1/status")
                                        .header("Authorization", "Bearer wrong-secret"))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.error.code").value("EXTERNAL_API_UNAUTHORIZED"))
                        .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("wrong-secret");
    }

    @Test
    void enabledWithValidBearerKeyReturnsStatusMetadataOnly() throws Exception {
        MockMvc mvc = mockMvc(true, sha256("expected-secret"));

        MvcResult result =
                mvc.perform(
                                get("/external/api/v1/status")
                                        .header("Authorization", "Bearer expected-secret"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.version").value("v1"))
                        .andExpect(jsonPath("$.data.enabled").value(true))
                        .andExpect(jsonPath("$.data.capabilities[0]").value("status"))
                        .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("expected-secret")
                .doesNotContain("scenario")
                .doesNotContain("chat_session")
                .doesNotContain("admin");
    }

    @Test
    void enabledWithValidHeaderKeyReturnsStatusMetadata() throws Exception {
        MockMvc mvc = mockMvc(true, sha256("expected-secret"));

        mvc.perform(
                        get("/external/api/v1/status")
                                .header("X-CleverChat-Api-Key", "expected-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private MockMvc mockMvc(boolean enabled, String apiKeyHash) {
        ExternalApiProperties properties = new ExternalApiProperties();
        properties.setEnabled(enabled);
        properties.setApiKeySha256(apiKeyHash);
        ExternalApiAuthService authService = new ExternalApiAuthService(properties);
        return MockMvcBuilders.standaloneSetup(new ExternalApiController(authService)).build();
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
