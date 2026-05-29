package kr.co.cleverchat.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import kr.co.cleverchat.domain.chatbot.mapper.ChatSessionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChatRuntimeMaintenanceServiceTest {

    private final ChatSessionMapper sessionMapper =
            org.mockito.Mockito.mock(ChatSessionMapper.class);
    private final ChatRuntimeMaintenanceService service =
            new ChatRuntimeMaintenanceService(sessionMapper);

    @Test
    void expireActiveSessionsDelegatesToMapper() {
        when(sessionMapper.expireActiveBefore(any(OffsetDateTime.class))).thenReturn(3);

        assertThat(service.expireActiveSessions()).isEqualTo(3);

        verify(sessionMapper).expireActiveBefore(any(OffsetDateTime.class));
    }

    @Test
    void deleteExpiredRetentionDataUsesNinetyDayCutoffAndChunks() {
        when(sessionMapper.deleteCreatedBefore(any(OffsetDateTime.class), eq(500)))
                .thenReturn(500, 2);
        ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

        assertThat(service.deleteExpiredRetentionData()).isEqualTo(502);

        verify(sessionMapper, org.mockito.Mockito.times(2))
                .deleteCreatedBefore(cutoffCaptor.capture(), eq(500));
        assertThat(cutoffCaptor.getAllValues().get(0)).isBefore(OffsetDateTime.now().minusDays(89));
    }
}
