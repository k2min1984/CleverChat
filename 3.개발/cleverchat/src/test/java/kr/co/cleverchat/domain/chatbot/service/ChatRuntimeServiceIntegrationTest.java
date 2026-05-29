package kr.co.cleverchat.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.chatbot.service.ChatRuntimeService.ChatRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@Tag("integration")
class ChatRuntimeServiceIntegrationTest {

    private static final UUID SESSION_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ANONYMOUS_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("cleverchat")
                    .withUsername("cleverchat")
                    .withPassword("cleverchat");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired ChatRuntimeService service;

    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute(
                """
            TRUNCATE TABLE
                chat_failure,
                chat_message,
                chat_session,
                chat_recommendation,
                scenario_node_option,
                scenario_node,
                scenario_version,
                scenario,
                scenario_category
            CASCADE
            """);
    }

    @Test
    void selectOptionRollsBackButFailureRecordRemains() {
        seedActiveSession();

        assertThatThrownBy(
                        () ->
                                service.selectOption(
                                        SESSION_ID,
                                        999L,
                                        new ChatRequestContext(ANONYMOUS_ID, "127.0.0.1", "JUnit")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        Integer failureCount =
                jdbcTemplate.queryForObject(
                        """
            SELECT COUNT(*)
            FROM chat_failure
            WHERE session_id = CAST(? AS uuid)
              AND reason = 'INVALID_OPTION'
              AND detail ->> 'requestedOptionId' = '999'
            """,
                        Integer.class,
                        SESSION_ID.toString());
        Integer messageCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM chat_message WHERE session_id = CAST(? AS uuid)",
                        Integer.class,
                        SESSION_ID.toString());

        assertThat(failureCount).isOne();
        assertThat(messageCount).isZero();
    }

    private void seedActiveSession() {
        Long categoryId =
                jdbcTemplate.queryForObject(
                        "INSERT INTO scenario_category (name) VALUES ('rollback-test') RETURNING id",
                        Long.class);
        Long scenarioId =
                jdbcTemplate.queryForObject(
                        """
            INSERT INTO scenario (category_id, title, status)
            VALUES (?, 'rollback scenario', 'ACTIVE')
            RETURNING id
            """,
                        Long.class,
                        categoryId);
        Long versionId =
                jdbcTemplate.queryForObject(
                        """
            INSERT INTO scenario_version (scenario_id, version_no, status, created_by)
            VALUES (?, 1, 'PUBLISHED', 'integration-test')
            RETURNING id
            """,
                        Long.class,
                        scenarioId);
        Long nodeId =
                jdbcTemplate.queryForObject(
                        """
            INSERT INTO scenario_node (version_id, node_key, node_type, title, content, metadata)
            VALUES (?, 'start', 'QUESTION', 'start', 'start', '{}'::jsonb)
            RETURNING id
            """,
                        Long.class,
                        versionId);
        jdbcTemplate.update(
                "UPDATE scenario_version SET start_node_id = ? WHERE id = ?", nodeId, versionId);
        jdbcTemplate.update(
                "UPDATE scenario SET active_version_id = ? WHERE id = ?", versionId, scenarioId);
        jdbcTemplate.update(
                """
            INSERT INTO chat_session (
                id, anonymous_id, scenario_id, version_id, current_node_id,
                state, expires_at, ip_hash, user_agent_hash
            )
            VALUES (CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?, 'ACTIVE', ?, repeat('0', 64), repeat('1', 64))
            """,
                SESSION_ID.toString(),
                ANONYMOUS_ID.toString(),
                scenarioId,
                versionId,
                nodeId,
                OffsetDateTime.now().plusMinutes(30));
    }
}
