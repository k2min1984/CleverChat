package kr.co.cleverchat.domain.scenario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import kr.co.cleverchat.common.audit.AuditTrailRecorder;
import kr.co.cleverchat.domain.scenario.event.ScenarioMatchingCacheInvalidator;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeOptionMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioVersionMapper;
import kr.co.cleverchat.domain.scenario.model.Scenario;
import kr.co.cleverchat.domain.scenario.model.ScenarioNode;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeOption;
import kr.co.cleverchat.domain.scenario.model.ScenarioVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScenarioServiceTest {

    @Mock private ScenarioMapper scenarioMapper;

    @Mock private ScenarioVersionMapper versionMapper;

    @Mock private ScenarioNodeMapper nodeMapper;

    @Mock private ScenarioNodeOptionMapper optionMapper;

    @Mock private AuditTrailRecorder auditTrailRecorder;

    @Mock private ScenarioMatchingCacheInvalidator matchingCacheInvalidator;

    private ScenarioService scenarioService;

    @BeforeEach
    void setUp() {
        scenarioService =
                new ScenarioService(
                        scenarioMapper,
                        versionMapper,
                        nodeMapper,
                        optionMapper,
                        new ScenarioGraphValidator(),
                        auditTrailRecorder,
                        matchingCacheInvalidator);
    }

    @Test
    void createVersionReturnsExistingDraftWithoutInsert() {
        ScenarioVersion draft = version(10L, 1L, 2, "DRAFT");
        when(scenarioMapper.findById(1L)).thenReturn(scenario(1L, "ACTIVE"));
        when(versionMapper.findDraftByScenarioId(1L)).thenReturn(draft);

        ScenarioVersion result = scenarioService.createVersion(1L);

        assertThat(result).isSameAs(draft);
        verify(versionMapper, never()).nextVersionNo(1L);
        verify(versionMapper, never()).insert(any(ScenarioVersion.class));
    }

    @Test
    void createVersionInsertsNewDraftWhenNoDraftExists() {
        when(scenarioMapper.findById(1L)).thenReturn(scenario(1L, "ACTIVE"));
        when(versionMapper.findDraftByScenarioId(1L)).thenReturn(null);
        when(versionMapper.findByScenarioId(1L)).thenReturn(List.of());
        when(versionMapper.nextVersionNo(1L)).thenReturn(3);
        when(versionMapper.findById(30L)).thenReturn(version(30L, 1L, 3, "DRAFT"));

        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            ScenarioVersion version = invocation.getArgument(0);
                            version.setId(30L);
                            return null;
                        })
                .when(versionMapper)
                .insert(any(ScenarioVersion.class));

        ScenarioVersion result = scenarioService.createVersion(1L);

        assertThat(result.getId()).isEqualTo(30L);
        assertThat(result.getVersionNo()).isEqualTo(3);
        verify(versionMapper).insert(any(ScenarioVersion.class));
    }

    @Test
    void createVersionCopiesLatestPublishedGraphToDraft() {
        ScenarioVersion published = version(20L, 1L, 1, "PUBLISHED");
        published.setStartNodeId(100L);
        ScenarioNode sourceNode = node(100L);
        sourceNode.setMetadata("{\"memo\":\"old\"}");
        when(scenarioMapper.findById(1L)).thenReturn(scenario(1L, "ACTIVE"));
        when(versionMapper.findDraftByScenarioId(1L)).thenReturn(null);
        when(versionMapper.findByScenarioId(1L)).thenReturn(List.of(published));
        when(versionMapper.nextVersionNo(1L)).thenReturn(2);
        when(nodeMapper.findByVersionId(20L)).thenReturn(List.of(sourceNode));
        when(optionMapper.findByNodeId(100L)).thenReturn(List.of(option(200L, 100L, null)));
        when(versionMapper.findById(30L)).thenReturn(version(30L, 1L, 2, "DRAFT"));

        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            ScenarioVersion version = invocation.getArgument(0);
                            version.setId(30L);
                            return null;
                        })
                .when(versionMapper)
                .insert(any(ScenarioVersion.class));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            ScenarioNode node = invocation.getArgument(0);
                            node.setId(300L);
                            return null;
                        })
                .when(nodeMapper)
                .insert(any(ScenarioNode.class));

        ScenarioVersion result = scenarioService.createVersion(1L);

        assertThat(result.getId()).isEqualTo(30L);
        verify(nodeMapper).insert(any(ScenarioNode.class));
        verify(optionMapper).insert(any(ScenarioNodeOption.class));
        verify(versionMapper).setStartNode(30L, 300L);
    }

    @Test
    void publishabilityRequiresDraftGraphAndStartNode() {
        ScenarioVersion noGraph = version(10L, 1L, 1, "DRAFT");
        ScenarioVersion noStart = version(11L, 1L, 2, "DRAFT");
        ScenarioVersion ready = version(12L, 1L, 3, "DRAFT");
        noStart.setStartNodeId(999L);
        ready.setStartNodeId(100L);
        when(nodeMapper.findByVersionId(10L)).thenReturn(List.of());
        when(nodeMapper.findByVersionId(11L)).thenReturn(List.of(node(100L)));
        when(nodeMapper.findByVersionId(12L)).thenReturn(List.of(node(100L)));

        var noGraphResult = scenarioService.publishability(noGraph);
        assertThat(noGraphResult.hasGraph()).isFalse();
        assertThat(noGraphResult.hasStartNode()).isFalse();
        assertThat(noGraphResult.publishable()).isFalse();
        assertThat(noGraphResult.reason()).isEqualTo("그래프 저장 후 게시할 수 있습니다.");

        var noStartResult = scenarioService.publishability(noStart);
        assertThat(noStartResult.hasGraph()).isTrue();
        assertThat(noStartResult.hasStartNode()).isFalse();
        assertThat(noStartResult.publishable()).isFalse();
        assertThat(noStartResult.reason()).isEqualTo("시작 노드를 지정한 뒤 게시할 수 있습니다.");

        var readyResult = scenarioService.publishability(ready);
        assertThat(readyResult.hasGraph()).isTrue();
        assertThat(readyResult.hasStartNode()).isTrue();
        assertThat(readyResult.publishable()).isTrue();
        assertThat(readyResult.reason()).isEqualTo("게시할 수 있습니다.");
    }

    @Test
    void emptyDraftGraphUsesLatestPublishedGraphAsEditableBaseline() {
        ScenarioVersion draft = version(30L, 1L, 2, "DRAFT");
        ScenarioVersion published = version(20L, 1L, 1, "PUBLISHED");
        published.setStartNodeId(100L);
        when(versionMapper.findById(30L)).thenReturn(draft);
        when(nodeMapper.findByVersionId(30L)).thenReturn(List.of());
        when(versionMapper.findByScenarioId(1L)).thenReturn(List.of(draft, published));
        when(nodeMapper.findByVersionId(20L)).thenReturn(List.of(node(100L)));
        when(optionMapper.findByNodeId(100L)).thenReturn(List.of());

        var graph = scenarioService.graph(30L);

        assertThat(graph.startNodeKey()).isEqualTo("start");
        assertThat(graph.nodes()).hasSize(1);
        assertThat(graph.nodes().get(0).title()).isEqualTo("Start");
    }

    private Scenario scenario(Long id, String status) {
        Scenario scenario = new Scenario();
        scenario.setId(id);
        scenario.setStatus(status);
        scenario.setTitle("테스트 시나리오");
        return scenario;
    }

    private ScenarioVersion version(Long id, Long scenarioId, int versionNo, String status) {
        ScenarioVersion version = new ScenarioVersion();
        version.setId(id);
        version.setScenarioId(scenarioId);
        version.setVersionNo(versionNo);
        version.setStatus(status);
        return version;
    }

    private ScenarioNode node(Long id) {
        ScenarioNode node = new ScenarioNode();
        node.setId(id);
        node.setVersionId(1L);
        node.setNodeKey("start");
        node.setNodeType("MESSAGE");
        node.setTitle("Start");
        return node;
    }

    private ScenarioNodeOption option(Long id, Long nodeId, Long nextNodeId) {
        ScenarioNodeOption option = new ScenarioNodeOption();
        option.setId(id);
        option.setNodeId(nodeId);
        option.setNextNodeId(nextNodeId);
        option.setLabel("다음");
        option.setSortOrder(1);
        option.setEnabled(true);
        return option;
    }
}
