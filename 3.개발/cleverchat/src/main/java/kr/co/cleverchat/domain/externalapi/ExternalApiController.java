package kr.co.cleverchat.domain.externalapi;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.externalapi.ExternalApiAuthService.ExternalApiAuthResult.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/external/api/v1")
public class ExternalApiController {

    private final ExternalApiAuthService authService;

    public ExternalApiController(ExternalApiAuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ExternalApiStatusResponse>> status(
            HttpServletRequest request) {
        Status authStatus = authService.authenticate(request).status();
        if (authStatus == Status.DISABLED) {
            return error(
                    HttpStatus.NOT_FOUND, "EXTERNAL_API_DISABLED", "External API is disabled.");
        }
        if (authStatus == Status.NOT_CONFIGURED) {
            return error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EXTERNAL_API_NOT_CONFIGURED",
                    "External API is not configured.");
        }
        if (authStatus == Status.UNAUTHORIZED) {
            return error(
                    HttpStatus.UNAUTHORIZED,
                    "EXTERNAL_API_UNAUTHORIZED",
                    "External API authentication failed.");
        }

        return ResponseEntity.ok(
                ApiResponse.ok(new ExternalApiStatusResponse("v1", true, List.of("status"))));
    }

    private ResponseEntity<ApiResponse<ExternalApiStatusResponse>> error(
            HttpStatus status, String code, String message) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ApiResponse<ExternalApiStatusResponse> body =
                (ApiResponse) ApiResponse.error(code, message);
        return ResponseEntity.status(status).body(body);
    }

    public record ExternalApiStatusResponse(
            String version, boolean enabled, List<String> capabilities) {}
}
