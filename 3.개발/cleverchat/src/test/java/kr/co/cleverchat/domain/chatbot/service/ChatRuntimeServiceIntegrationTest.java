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
                tb_chat_failure,
                tb_chat_message,
                tb_chat_session,
                tb_chat_recommendation,
                tb_scenario_node_option,
                tb_scenario_node,
                tb_scenario_version,
                tb_scenario,
                tb_scenario_category
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
            FROM tb_chat_failure
            WHERE session_no = CAST(? AS uuid)
              AND reason = 'INVALID_OPTION'
              AND detail ->> 'requestedOptionId' = '999'
            """,
                        Integer.class,
                        SESSION_ID.toString());
        Integer messageCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM tb_chat_message WHERE session_no = CAST(? AS uuid)",
                        Integer.class,
                        SESSION_ID.toString());

        assertThat(failureCount).isOne();
        assertThat(messageCount).isZero();
    }

    private void seedActiveSession() {
        Long categoryId =
                jdbcTemplate.queryForObject(
                        "INSERT INTO tb_scenario_category (name) VALUES ('rollback-test') RETURNING scenario_category_no",
                        Long.class);
        Long scenarioId =
                jdbcTemplate.queryForObject(
                        """
            INSERT INTO tb_scenario (category_no, title, status)
            VALUES (?, 'rollback scenario', 'ACTIVE')
            RETURNING scenario_no
            """,
                        Long.class,
                        categoryId);
        Long versionId =
                jdbcTemplate.queryForObject(
                        """
            INSERT INTO tb_scenario_version (scenario_no, version_no, status, frst_regr_empno)
            VALUES (?, 1, 'PUBLISHED', 'integration-test')
            RETURNING scenario_version_no
            """,
                        Long.class,
                        scenarioId);
        Long nodeId =
                jdbcTemplate.queryForObject(
                        """
            INSERT INTO tb_scenario_node (version_no, node_key, node_type, title, content, metadata)
            VALUES (?, 'start', 'QUESTION', 'start', 'start', '{}'::jsonb)
            RETURNING scenario_node_no
            """,
                        Long.class,
                        versionId);
        jdbcTemplate.update(
                "UPDATE tb_scenario_version SET start_node_no = ? WHERE scenario_version_no = ?",
                nodeId,
                versionId);
        jdbcTemplate.update(
                "UPDATE tb_scenario SET active_version_no = ? WHERE scenario_no = ?",
                versionId,
                scenarioId);
        jdbcTemplate.update(
                """
            INSERT INTO tb_chat_session (
                chat_session_no, anonymous_id, scenario_no, version_no, current_node_no,
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
