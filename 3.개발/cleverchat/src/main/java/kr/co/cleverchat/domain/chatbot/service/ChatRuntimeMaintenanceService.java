package kr.co.cleverchat.domain.chatbot.service;

import java.time.OffsetDateTime;
import kr.co.cleverchat.domain.chatbot.mapper.ChatSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatRuntimeMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(ChatRuntimeMaintenanceService.class);
    private static final int RETENTION_DAYS = 90;
    private static final int DELETE_LIMIT = 500;

    private final ChatSessionMapper sessionMapper;

    public ChatRuntimeMaintenanceService(ChatSessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public int expireActiveSessions() {
        int updated = sessionMapper.expireActiveBefore(OffsetDateTime.now());
        if (updated > 0) {
            log.info("Expired chat sessions: count={}", updated);
        }
        return updated;
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public int deleteExpiredRetentionData() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(RETENTION_DAYS);
        int total = 0;
        int deleted;
        do {
            deleted = sessionMapper.deleteCreatedBefore(cutoff, DELETE_LIMIT);
            total += deleted;
        } while (deleted == DELETE_LIMIT);
        if (total > 0) {
            log.info(
                    "Deleted retained chat sessions: count={}, retentionDays={}",
                    total,
                    RETENTION_DAYS);
        }
        return total;
    }
}
