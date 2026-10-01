package kr.co.cleverchat.domain.settings;

import static org.assertj.core.api.Assertions.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GatewayIdentityServiceTest {
    private final GatewayIdentityService service = new GatewayIdentityService("test-secret");

    private MockHttpServletRequest request(String roles) {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Clever-Gw-Secret", "test-secret");
        request.addHeader("X-Clever-User", "E1001");
        request.addHeader("X-Clever-Roles", roles);
        return request;
    }

    @Test
    void readsOnlyOwnServiceRoleAndDecodesPercentHeaders() {
        var request = request("other=ADMIN;cleverchat=USER");
        request.addHeader("X-Clever-Name", URLEncoder.encode("홍길동+연구원", StandardCharsets.UTF_8));
        assertThat(service.verify(request, "cleverchat").roles()).containsExactly("USER");
        assertThat(service.verify(request, "cleverchat").name()).isEqualTo("홍길동+연구원");
    }

    @Test
    void rejectsUnknownOrMissingOwnRole() {
        assertThatThrownBy(() -> service.verify(request("other=ADMIN"), "cleverchat"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.verify(request("cleverchat=SUPER"), "cleverchat"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void forgedIdentityCannotPassWithoutSecret() {
        var request = request("cleverchat=ADMIN");
        request.removeHeader("X-Clever-Gw-Secret");
        assertThatThrownBy(() -> service.verify(request, "cleverchat"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void platformAdminWinsButSecretIsStillRequired() {
        var request = request("other=USER");
        request.addHeader("X-Clever-Admin", "1");
        assertThat(service.verify(request, "cleverchat").roles()).containsExactly("ADMIN");
        request.removeHeader("X-Clever-Gw-Secret");
        request.addHeader("X-Clever-Gw-Secret", "wrong");
        assertThatThrownBy(() -> service.verify(request, "cleverchat"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
