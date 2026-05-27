package kr.co.cleverchat.domain.scenario.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.co.cleverchat.common.audit.AuditTrailRecorder;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.auth.security.CurrentAdminProvider;
import kr.co.cleverchat.domain.scenario.dto.ScenarioDtos.SaveRequest;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos.NodeRequest;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos.OptionRequest;
import kr.co.cleverchat.domain.scenario.event.ScenarioMatchingCacheInvalidator;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeOptionMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioVersionMapper;
import kr.co.cleverchat.domain.scenario.model.Scenario;
import kr.co.cleverchat.domain.scenario.model.ScenarioNode;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeOption;
import kr.co.cleverchat.domain.scenario.model.ScenarioVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScenarioService {

    private final ScenarioMapper scenarioMapper;
    private final ScenarioVersionMapper versionMapper;
    private final ScenarioNodeMapper nodeMapper;
    private final ScenarioNodeOptionMapper optionMapper;
    private final ScenarioGraphValidator graphValidator;
    private final AuditTrailRecorder auditTrailRecorder;
    private final ScenarioMatchingCacheInvalidator matchingCacheInvalidator;

    public ScenarioService(
        ScenarioMapper scenarioMapper,
        ScenarioVersionMapper versionMapper,
        ScenarioNodeMapper nodeMapper,
        ScenarioNodeOptionMapper optionMapper,
        ScenarioGraphValidator graphValidator,
        AuditTrailRecorder auditTrailRecorder,
        ScenarioMatchingCacheInvalidator matchingCacheInvalidator
    ) {
        this.scenarioMapper = scenarioMapper;
        this.versionMapper = versionMapper;
        this.nodeMapper = nodeMapper;
        this.optionMapper = optionMapper;
        this.graphValidator = graphValidator;
        this.auditTrailRecorder = auditTrailRecorder;
        this.matchingCacheInvalidator = matchingCacheInvalidator;
    }

    public List<Scenario> findAll(String status) {
        return scenarioMapper.findAll(status);
    }

    public Scenario get(Long id) {
        Scenario scenario = scenarioMapper.findById(id);
        if (scenario == null || "DELETED".equals(scenario.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return scenario;
    }

    public List<ScenarioVersion> versions(Long scenarioId) {
        return versionMapper.findByScenarioId(scenarioId);
    }

    public ScenarioVersion version(Long versionId) {
        ScenarioVersion version = versionMapper.findById(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return version;
    }

    public List<ScenarioNode> nodes(Long versionId) {
        return nodeMapper.findByVersionId(versionId);
    }

    public List<ScenarioNodeOption> options(Long nodeId) {
        return optionMapper.findByNodeId(nodeId);
    }

    public ScenarioGraphDtos.SaveRequest graph(Long versionId) {
        ScenarioVersion version = version(versionId);
        return readPersistedGraph(version);
    }

    @Transactional
    public Scenario create(SaveRequest request) {
        Scenario scenario = new Scenario();
        scenario.setCategoryId(request.categoryId());
        scenario.setTitle(request.title().trim());
        scenario.setDescription(request.description());
        scenario.setStatus("DRAFT");
        scenarioMapper.insert(scenario);
        auditTrailRecorder.record("SCENARIO_CREATE", "scenario", scenario.getId(), Map.of("after", Map.of(
            "title", scenario.getTitle(),
            "categoryId", scenario.getCategoryId()
        )));
        createVersion(scenario.getId());
        return get(scenario.getId());
    }

    @Transactional
    public Scenario update(Long id, SaveRequest request) {
        Scenario before = get(id);
        if (!List.of("DRAFT", "INACTIVE").contains(before.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "DRAFT 또는 INACTIVE 시나리오만 수정할 수 있습니다.");
        }
        Scenario scenario = new Scenario();
        scenario.setId(id);
        scenario.setCategoryId(request.categoryId());
        scenario.setTitle(request.title().trim());
        scenario.setDescription(request.description());
        scenarioMapper.update(scenario);
        Scenario after = get(id);
        auditTrailRecorder.record("SCENARIO_UPDATE", "scenario", id, Map.of(
            "before", Map.of("title", before.getTitle(), "status", before.getStatus()),
            "after", Map.of("title", after.getTitle(), "status", after.getStatus())
        ));
        return after;
    }

    @Transactional
    public void delete(Long id) {
        Scenario before = get(id);
        if (!List.of("DRAFT", "INACTIVE").contains(before.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "DRAFT 또는 INACTIVE 시나리오만 삭제할 수 있습니다.");
        }
        scenarioMapper.updateStatus(id, "DELETED");
        auditTrailRecorder.record("SCENARIO_DELETE", "scenario", id, Map.of(
            "before", Map.of("status", before.getStatus()),
            "after", Map.of("status", "DELETED")
        ));
        matchingCacheInvalidator.onScenarioChanged(id);
    }

    @Transactional
    public ScenarioVersion createVersion(Long scenarioId) {
        Scenario scenario = get(scenarioId);
        if ("DELETED".equals(scenario.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        ScenarioVersion version = new ScenarioVersion();
        version.setScenarioId(scenarioId);
        version.setVersionNo(versionMapper.nextVersionNo(scenarioId));
        version.setStatus("DRAFT");
        version.setCreatedBy(currentUsername());
        versionMapper.insert(version);
        return versionMapper.findById(version.getId());
    }

    @Transactional
    public void saveGraph(Long versionId, ScenarioGraphDtos.SaveRequest request) {
        ScenarioVersion version = draftVersion(versionId);
        graphValidator.validateForSave(request);
        optionMapper.deleteByVersionId(versionId);
        nodeMapper.deleteByVersionId(versionId);

        Map<String, ScenarioNode> savedNodes = new LinkedHashMap<>();
        for (NodeRequest nodeRequest : request.nodes()) {
            ScenarioNode node = new ScenarioNode();
            node.setVersionId(versionId);
            node.setNodeKey(nodeRequest.nodeKey());
            node.setNodeType(nodeRequest.nodeType());
            node.setTitle(nodeRequest.title());
            node.setContent(nodeRequest.content());
            node.setSortOrder(nodeRequest.sortOrder());
            node.setMetadata(hasText(nodeRequest.metadata()) ? nodeRequest.metadata() : "{}");
            nodeMapper.insert(node);
            savedNodes.put(node.getNodeKey(), node);
        }

        int optionCount = 0;
        for (NodeRequest nodeRequest : request.nodes()) {
            ScenarioNode node = savedNodes.get(nodeRequest.nodeKey());
            for (OptionRequest optionRequest : nodeRequest.options() == null ? List.<OptionRequest>of() : nodeRequest.options()) {
                ScenarioNodeOption option = new ScenarioNodeOption();
                option.setNodeId(node.getId());
                option.setNextNodeId(hasText(optionRequest.nextNodeKey()) ? savedNodes.get(optionRequest.nextNodeKey()).getId() : null);
                option.setLabel(optionRequest.label());
                option.setConditionExpr(optionRequest.conditionExpr());
                option.setSortOrder(optionRequest.sortOrder());
                option.setEnabled(optionRequest.enabled());
                optionMapper.insert(option);
                optionCount++;
            }
        }
        versionMapper.setStartNode(versionId, savedNodes.get(request.startNodeKey()).getId());
        auditTrailRecorder.record("SCENARIO_VERSION_SAVE", "scenario_version", versionId, Map.of(
            "scenarioId", version.getScenarioId(),
            "nodeCount", savedNodes.size(),
            "optionCount", optionCount
        ));
        matchingCacheInvalidator.onScenarioChanged(version.getScenarioId());
    }

    @Transactional
    public void publish(Long versionId) {
        ScenarioVersion version = draftVersion(versionId);
        ScenarioGraphDtos.SaveRequest graph = readPersistedGraph(version);
        if (graph.startNodeKey() == null || graph.nodes().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "저장된 그래프가 없습니다.");
        }
        graphValidator.validateForPublish(graph);
        versionMapper.archivePublished(version.getScenarioId());
        versionMapper.publish(versionId);
        auditTrailRecorder.record("SCENARIO_VERSION_PUBLISH", "scenario_version", versionId, Map.of(
            "before", Map.of("status", "DRAFT"),
            "after", Map.of("status", "PUBLISHED")
        ));
        matchingCacheInvalidator.onScenarioChanged(version.getScenarioId());
    }

    @Transactional
    public void activate(Long scenarioId, Long versionId) {
        Scenario scenario = get(scenarioId);
        if (!List.of("DRAFT", "INACTIVE").contains(scenario.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "DRAFT 또는 INACTIVE 시나리오만 활성화할 수 있습니다.");
        }
        ScenarioVersion version = versionMapper.findById(versionId);
        if (version == null || !scenarioId.equals(version.getScenarioId()) || !"PUBLISHED".equals(version.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "게시된 버전만 활성화할 수 있습니다.");
        }
        scenarioMapper.activate(scenarioId, versionId);
        auditTrailRecorder.record("SCENARIO_ACTIVATE", "scenario", scenarioId, Map.of(
            "before", Map.of("status", scenario.getStatus(), "activeVersionId", scenario.getActiveVersionId()),
            "after", Map.of("status", "ACTIVE", "activeVersionId", versionId)
        ));
        matchingCacheInvalidator.onScenarioChanged(scenarioId);
    }

    @Transactional
    public void deactivate(Long scenarioId) {
        Scenario scenario = get(scenarioId);
        if (!"ACTIVE".equals(scenario.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "ACTIVE 시나리오만 비활성화할 수 있습니다.");
        }
        scenarioMapper.updateStatus(scenarioId, "INACTIVE");
        auditTrailRecorder.record("SCENARIO_DEACTIVATE", "scenario", scenarioId, Map.of(
            "before", Map.of("status", "ACTIVE"),
            "after", Map.of("status", "INACTIVE")
        ));
        matchingCacheInvalidator.onScenarioChanged(scenarioId);
    }

    private ScenarioVersion draftVersion(Long versionId) {
        ScenarioVersion version = versionMapper.findById(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!"DRAFT".equals(version.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "DRAFT 버전만 수정하거나 게시할 수 있습니다.");
        }
        return version;
    }

    private ScenarioGraphDtos.SaveRequest readPersistedGraph(ScenarioVersion version) {
        List<ScenarioNode> nodes = nodeMapper.findByVersionId(version.getId());
        if (nodes.isEmpty()) {
            return new ScenarioGraphDtos.SaveRequest(null, List.of());
        }
        Map<Long, ScenarioNode> byId = nodes.stream().collect(Collectors.toMap(ScenarioNode::getId, Function.identity()));
        List<NodeRequest> nodeRequests = new ArrayList<>();
        for (ScenarioNode node : nodes) {
            List<OptionRequest> optionRequests = optionMapper.findByNodeId(node.getId()).stream()
                .map(option -> new OptionRequest(
                    option.getLabel(),
                    option.getNextNodeId() == null ? null : byId.get(option.getNextNodeId()).getNodeKey(),
                    option.getConditionExpr(),
                    option.getSortOrder(),
                    option.isEnabled()
                ))
                .toList();
            nodeRequests.add(new NodeRequest(
                node.getNodeKey(),
                node.getNodeType(),
                node.getTitle(),
                node.getContent(),
                node.getSortOrder(),
                node.getMetadata(),
                optionRequests
            ));
        }
        String startNodeKey = version.getStartNodeId() == null ? null : byId.get(version.getStartNodeId()).getNodeKey();
        return new ScenarioGraphDtos.SaveRequest(startNodeKey, nodeRequests);
    }

    private String currentUsername() {
        String username = CurrentAdminProvider.currentUsername();
        return username == null ? "system" : username;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
