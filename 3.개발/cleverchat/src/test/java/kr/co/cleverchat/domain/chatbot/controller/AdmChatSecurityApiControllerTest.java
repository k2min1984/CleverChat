package kr.co.cleverchat.domain.chatbot.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.co.cleverchat.common.error.GlobalExceptionHandler;
import kr.co.cleverchat.domain.chatbot.dto.ChatFieldEncryptionDtos.MaintenanceResponse;
import kr.co.cleverchat.domain.chatbot.service.ChatFieldEncryptionMaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdmChatSecurityApiControllerTest {

    @Mock ChatFieldEncryptionMaintenanceService maintenanceService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new AdmChatSecurityApiController(maintenanceService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void backfillApiReturnsEnvelope() throws Exception {
        when(maintenanceService.backfill(true, 100))
                .thenReturn(new MaintenanceResponse(3, 0, 0, 0, "dry-run"));

        mockMvc.perform(
                        post("/admin/api/security/field-encryption/backfill")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"dryRun\":true,\"limit\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scanned").value(3))
                .andExpect(jsonPath("$.data.updated").value(0));

        verify(maintenanceService).backfill(true, 100);
    }

    @Test
    void rotateApiReturnsEnvelope() throws Exception {
        when(maintenanceService.rotate(false, 50))
                .thenReturn(new MaintenanceResponse(2, 2, 0, 0, "done"));

        mockMvc.perform(
                        post("/admin/api/security/field-encryption/rotate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"dryRun\":false,\"limit\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.updated").value(2));

        verify(maintenanceService).rotate(false, 50);
    }

    @Test
    void rejectsInvalidLimit() throws Exception {
        mockMvc.perform(
                        post("/admin/api/security/field-encryption/backfill")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"dryRun\":true,\"limit\":5001}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
