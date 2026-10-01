package kr.co.cleverchat.domain.scenario.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.co.cleverchat.common.audit.AuditTrailRecorder;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.auth.security.CurrentAdminProvider;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import kr.co.cleverchat.domain.scenario.dto.ScenarioDtos.Publishability;
import kr.co.cleverchat.domain.scenario.dto.ScenarioDtos.SaveRequest;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos.LinkRequest;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos.NodeRequest;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos.OptionRequest;
import kr.co.cleverchat.domain.scenario.event.ScenarioMatchingCacheInvalidator;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeLinkMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeOptionMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioVersionMapper;
import kr.co.cleverchat.domain.scenario.model.Scenario;
import kr.co.cleverchat.domain.scenario.model.ScenarioNode;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeLink;
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
    private final ScenarioNodeLinkMapper linkMapper;
    private final ScenarioGraphValidator graphValidator;
    private final AuditTrailRecorder auditTrailRecorder;
    private final ScenarioMatchingCacheInvalidator matchingCacheInvalidator;

    public ScenarioService(
            ScenarioMapper scenarioMapper,
            ScenarioVersionMapper versionMapper,
            ScenarioNodeMapper nodeMapper,
            ScenarioNodeOptionMapper optionMapper,
            ScenarioNodeLinkMapper linkMapper,
            ScenarioGraphValidator graphValidator,
            AuditTrailRecorder auditTrailRecorder,
            ScenarioMatchingCacheInvalidator matchingCacheInvalidator) {
        this.scenarioMapper = scenarioMapper;
        this.versionMapper = versionMapper;
        this.nodeMapper = nodeMapper;
        this.optionMapper = optionMapper;
        this.linkMapper = linkMapper;
        this.graphValidator = graphValidator;
        this.auditTrailRecorder = auditTrailRecorder;
        this.matchingCacheInvalidator = matchingCacheInvalidator;
    }

    public List<Scenario> findAll(String status) {
        return scenarioMapper.findAll(status);
    }

    public List<Scenario> activeForOrdering() {
        return scenarioMapper.findActiveForMatching();
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

    public List<ScenarioNodeLink> links(Long nodeId) {
        List<ScenarioNodeLink> links = linkMapper.findEnabledByNodeId(nodeId);
        return links == null ? List.of() : links;
    }

    public Publishability publishability(ScenarioVersion version) {
        if (!"DRAFT".equals(version.getStatus())) {
            return new Publishability(false, false, false, "초안 버전만 게시할 수 있습니다.");
        }
        List<ScenarioNode> nodes = nodeMapper.findByVersionId(version.getScenarioVersionNo());
        boolean hasGraph = !nodes.isEmpty();
        boolean hasStartNode =
                version.getStartNodeNo() != null
                        && nodes.stream()
                                .anyMatch(
                                        node ->
                                                version.getStartNodeNo()
                                                        .equals(node.getScenarioNodeNo()));
        if (!hasGraph) {
            return new Publishability(false, false, false, "그래프 저장 후 게시할 수 있습니다.");
        }
        if (!hasStartNode) {
            return new Publishability(true, false, false, "시작 노드를 지정한 뒤 게시할 수 있습니다.");
        }
        return new Publishability(true, true, true, "게시할 수 있습니다.");
    }

    public ScenarioGraphDtos.SaveRequest graph(Long versionId) {
        ScenarioVersion version = version(versionId);
        ScenarioGraphDtos.SaveRequest graph = readPersistedGraph(version);
        if ("DRAFT".equals(version.getStatus()) && graph.nodes().isEmpty()) {
            ScenarioVersion source =
                    latestSourceVersion(version.getScenarioNo(), version.getScenarioVersionNo());
            if (source != null) {
                return readPersistedGraph(source);
            }
        }
        return graph;
    }

    @Transactional
    @RequireRole("OPERATOR")
    public Scenario create(SaveRequest request) {
        Scenario scenario = new Scenario();
        scenario.setCategoryNo(request.categoryNo());
        scenario.setTitle(request.title().trim());
        scenario.setDescription(request.description());
        scenario.setSortOrder(request.sortOrder());
        scenario.setStatus("DRAFT");
        scenarioMapper.insert(scenario);
        auditTrailRecorder.record(
                "SCENARIO_CREATE",
                "scenario",
                scenario.getScenarioNo(),
                Map.of(
                        "after",
                        Map.of(
                                "title", scenario.getTitle(),
                                "categoryId", scenario.getCategoryNo())));
        createVersion(scenario.getScenarioNo());
        return get(scenario.getScenarioNo());
    }

    @Transactional
    @RequireRole("OPERATOR")
    public Scenario update(Long id, SaveRequest request) {
        Scenario before = get(id);
        if (!List.of("DRAFT", "INACTIVE").contains(before.getStatus())) {
            throw new BusinessException(
                    ErrorCode.STATE_CONFLICT, "DRAFT 또는 INACTIVE 시나리오만 수정할 수 있습니다.");
        }
        Scenario scenario = new Scenario();
        scenario.setScenarioNo(id);
        scenario.setCategoryNo(request.categoryNo());
        scenario.setTitle(request.title().trim());
        scenario.setDescription(request.description());
        scenario.setSortOrder(request.sortOrder());
        scenarioMapper.update(scenario);
        Scenario after = get(id);
        auditTrailRecorder.record(
                "SCENARIO_UPDATE",
                "scenario",
                id,
                Map.of(
                        "before", Map.of("title", before.getTitle(), "status", before.getStatus()),
                        "after", Map.of("title", after.getTitle(), "status", after.getStatus())));
        return after;
    }

    @Transactional
    @RequireRole("OPERATOR")
    public Scenario updateSortOrder(Long id, Integer sortOrder) {
        Scenario before = get(id);
        Integer normalizedSortOrder = sortOrder == null ? 0 : Math.max(0, sortOrder);
        scenarioMapper.updateSortOrder(id, normalizedSortOrder);
        Scenario after = get(id);
        auditTrailRecorder.record(
                "SCENARIO_SORT_ORDER_UPDATE",
                "scenario",
                id,
                Map.of(
                        "before", Map.of("sortOrder", String.valueOf(before.getSortOrder())),
                        "after", Map.of("sortOrder", String.valueOf(after.getSortOrder()))));
        matchingCacheInvalidator.onScenarioChanged(id);
        return after;
    }

    @Transactional
    @RequireRole("OPERATOR")
    public void reorderActiveScenarios(List<Long> scenarioIds) {
        if (scenarioIds == null || scenarioIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "정렬할 활성 시나리오가 없습니다.");
        }
        Set<Long> requestedIds = new HashSet<>(scenarioIds);
        if (requestedIds.size() != scenarioIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "중복된 시나리오가 포함되어 있습니다.");
        }

        List<Scenario> activeScenarios = activeForOrdering();
        Set<Long> activeIds =
                activeScenarios.stream().map(Scenario::getScenarioNo).collect(Collectors.toSet());
        if (!activeIds.equals(requestedIds)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "활성 시나리오 목록이 변경되었습니다. 새로고침 후 다시 저장해 주세요.");
        }

        int weight = scenarioIds.size() * 100;
        for (Long scenarioId : scenarioIds) {
            scenarioMapper.updateSortOrder(scenarioId, weight);
            weight -= 100;
            matchingCacheInvalidator.onScenarioChanged(scenarioId);
        }
        auditTrailRecorder.record(
                "SCENARIO_ORDER_UPDATE", "scenario", null, Map.of("scenarioIds", scenarioIds));
    }

    @Transactional
    @RequireRole("OPERATOR")
    public void delete(Long id) {
        Scenario before = get(id);
        if (!List.of("DRAFT", "INACTIVE").contains(before.getStatus())) {
            throw new BusinessException(
                    ErrorCode.STATE_CONFLICT, "DRAFT 또는 INACTIVE 시나리오만 삭제할 수 있습니다.");
        }
        scenarioMapper.updateStatus(id, "DELETED");
        auditTrailRecorder.record(
                "SCENARIO_DELETE",
                "scenario",
                id,
                Map.of(
                        "before", Map.of("status", before.getStatus()),
                        "after", Map.of("status", "DELETED")));
        matchingCacheInvalidator.onScenarioChanged(id);
    }

    @Transactional
    @RequireRole("OPERATOR")
    public ScenarioVersion createVersion(Long scenarioId) {
        Scenario scenario = get(scenarioId);
        if ("DELETED".equals(scenario.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        ScenarioVersion existingDraft = versionMapper.findDraftByScenarioId(scenarioId);
        if (existingDraft != null) {
            List<ScenarioNode> draftNodes =
                    nodeMapper.findByVersionId(existingDraft.getScenarioVersionNo());
            if (draftNodes == null || draftNodes.isEmpty()) {
                copyLatestGraphToDraft(
                        latestSourceVersion(scenarioId, existingDraft.getScenarioVersionNo()),
                        existingDraft.getScenarioVersionNo());
                ScenarioVersion refreshed =
                        versionMapper.findById(existingDraft.getScenarioVersionNo());
                return refreshed == null ? existingDraft : refreshed;
            }
            return existingDraft;
        }
        ScenarioVersion sourceVersion = latestSourceVersion(scenarioId, null);
        ScenarioVersion version = new ScenarioVersion();
        version.setScenarioNo(scenarioId);
        version.setVersionNo(versionMapper.nextVersionNo(scenarioId));
        version.setStatus("DRAFT");
        version.setFrstRegrEmpno(currentUsername());
        versionMapper.insert(version);
        copyLatestGraphToDraft(sourceVersion, version.getScenarioVersionNo());
        return versionMapper.findById(version.getScenarioVersionNo());
    }

    @Transactional
    @RequireRole("OPERATOR")
    public ScenarioVersion createVersionFromSource(Long scenarioId, Long sourceVersionId) {
        Scenario scenario = get(scenarioId);
        if ("DELETED".equals(scenario.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        ScenarioVersion sourceVersion = version(sourceVersionId);
        if (!scenarioId.equals(sourceVersion.getScenarioNo())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ScenarioVersion version = new ScenarioVersion();
        version.setScenarioNo(scenarioId);
        version.setVersionNo(versionMapper.nextVersionNo(scenarioId));
        version.setStatus("DRAFT");
        version.setFrstRegrEmpno(currentUsername());
        versionMapper.insert(version);
        copyLatestGraphToDraft(sourceVersion, version.getScenarioVersionNo());
        auditTrailRecorder.record(
                "SCENARIO_VERSION_COPY",
                "scenario_version",
                version.getScenarioVersionNo(),
                Map.of(
                        "scenarioId", scenarioId,
                        "sourceVersionId", sourceVersionId,
                        "versionNo", version.getVersionNo()));
        return versionMapper.findById(version.getScenarioVersionNo());
    }

    @Transactional
    @RequireRole("OPERATOR")
    public void saveGraph(Long versionId, ScenarioGraphDtos.SaveRequest request) {
        ScenarioVersion version = draftVersion(versionId);
        graphValidator.validateForSave(request);
        linkMapper.deleteByVersionId(versionId);
        optionMapper.deleteByVersionId(versionId);
        nodeMapper.deleteByVersionId(versionId);

        Map<String, ScenarioNode> savedNodes = new LinkedHashMap<>();
        for (NodeRequest nodeRequest : request.nodes()) {
            ScenarioNode node = new ScenarioNode();
            node.setVersionNo(versionId);
            node.setNodeKey(nodeRequest.nodeKey());
            node.setNodeType(nodeRequest.nodeType());
            node.setTitle(nodeRequest.title());
            node.setContent(nodeRequest.content());
            node.setSortOrder(nodeRequest.sortOrder());
            node.setMetadata(hasText(nodeRequest.metadata()) ? nodeRequest.metadata() : "{}");
            nodeMapper.insert(node);
            savedNodes.put(node.getNodeKey(), node);
        }

        int linkCount = 0;
        for (NodeRequest nodeRequest : request.nodes()) {
            ScenarioNode node = savedNodes.get(nodeRequest.nodeKey());
            for (LinkRequest linkRequest :
                    nodeRequest.links() == null ? List.<LinkRequest>of() : nodeRequest.links()) {
                ScenarioNodeLink link = new ScenarioNodeLink();
                link.setNodeNo(node.getScenarioNodeNo());
                link.setLabel(linkRequest.label());
                link.setUrl(linkRequest.url());
                link.setLinkType(
                        hasText(linkRequest.linkType()) ? linkRequest.linkType() : "EXTERNAL");
                link.setSortOrder(linkRequest.sortOrder());
                link.setUseYn(hasText(linkRequest.useYn()) ? linkRequest.useYn() : "Y");
                linkMapper.insert(link);
                linkCount++;
            }
        }

        int optionCount = 0;
        for (NodeRequest nodeRequest : request.nodes()) {
            ScenarioNode node = savedNodes.get(nodeRequest.nodeKey());
            for (OptionRequest optionRequest :
                    nodeRequest.options() == null
                            ? List.<OptionRequest>of()
                            : nodeRequest.options()) {
                ScenarioNodeOption option = new ScenarioNodeOption();
                option.setNodeNo(node.getScenarioNodeNo());
                option.setNextNodeNo(
                        hasText(optionRequest.nextNodeKey())
                                ? savedNodes.get(optionRequest.nextNodeKey()).getScenarioNodeNo()
                                : null);
                option.setLabel(optionRequest.label());
                option.setConditionExpr(optionRequest.conditionExpr());
                option.setSortOrder(optionRequest.sortOrder());
                option.setUseYn(optionRequest.useYn());
                optionMapper.insert(option);
                optionCount++;
            }
        }
        versionMapper.setStartNode(
                versionId, savedNodes.get(request.startNodeKey()).getScenarioNodeNo());
        auditTrailRecorder.record(
                "SCENARIO_VERSION_SAVE",
                "scenario_version",
                versionId,
                Map.of(
                        "scenarioId",
                        version.getScenarioNo(),
                        "nodeCount",
                        savedNodes.size(),
                        "linkCount",
                        linkCount,
                        "optionCount",
                        optionCount));
        matchingCacheInvalidator.onScenarioChanged(version.getScenarioNo());
    }

    @Transactional
    @RequireRole("OPERATOR")
    public void publish(Long versionId) {
        ScenarioVersion version = draftVersion(versionId);
        ScenarioGraphDtos.SaveRequest graph = readPersistedGraph(version);
        if (graph.startNodeKey() == null || graph.nodes().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "저장된 그래프가 없습니다.");
        }
        graphValidator.validateForPublish(graph);
        versionMapper.archivePublished(version.getScenarioNo());
        versionMapper.publish(versionId);
        auditTrailRecorder.record(
                "SCENARIO_VERSION_PUBLISH",
                "scenario_version",
                versionId,
                Map.of(
                        "before", Map.of("status", "DRAFT"),
                        "after", Map.of("status", "PUBLISHED")));
        matchingCacheInvalidator.onScenarioChanged(version.getScenarioNo());
    }

    @Transactional
    @RequireRole("OPERATOR")
    public void activate(Long scenarioId, Long versionId) {
        Scenario scenario = get(scenarioId);
        if (!List.of("DRAFT", "INACTIVE", "ACTIVE").contains(scenario.getStatus())) {
            throw new BusinessException(
                    ErrorCode.STATE_CONFLICT, "DRAFT 또는 INACTIVE 시나리오만 활성화할 수 있습니다.");
        }
        ScenarioVersion version = versionMapper.findById(versionId);
        if (version == null
                || !scenarioId.equals(version.getScenarioNo())
                || !List.of("PUBLISHED", "ARCHIVED").contains(version.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "게시된 버전만 활성화할 수 있습니다.");
        }
        ScenarioGraphDtos.SaveRequest graph = readPersistedGraph(version);
        graphValidator.validateForPublish(graph);
        versionMapper.archivePublished(scenarioId);
        versionMapper.publish(versionId);
        scenarioMapper.activate(scenarioId, versionId);
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("status", scenario.getStatus());
        before.put("activeVersionId", scenario.getActiveVersionNo());
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", "ACTIVE");
        after.put("activeVersionId", versionId);
        auditTrailRecorder.record(
                "SCENARIO_ACTIVATE",
                "scenario",
                scenarioId,
                Map.of("before", before, "after", after));
        matchingCacheInvalidator.onScenarioChanged(scenarioId);
    }

    @Transactional
    @RequireRole("OPERATOR")
    public void deactivate(Long scenarioId) {
        Scenario scenario = get(scenarioId);
        if (!"ACTIVE".equals(scenario.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "ACTIVE 시나리오만 비활성화할 수 있습니다.");
        }
        scenarioMapper.updateStatus(scenarioId, "INACTIVE");
        auditTrailRecorder.record(
                "SCENARIO_DEACTIVATE",
                "scenario",
                scenarioId,
                Map.of(
                        "before", Map.of("status", "ACTIVE"),
                        "after", Map.of("status", "INACTIVE")));
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
        List<ScenarioNode> nodes =
                nodeMapper.findByVersionId(version.getScenarioVersionNo()).stream()
                        .sorted(
                                Comparator.comparingInt(ScenarioNode::getSortOrder)
                                        .thenComparing(
                                                ScenarioNode::getScenarioNodeNo,
                                                Comparator.nullsLast(Long::compareTo)))
                        .toList();
        if (nodes.isEmpty()) {
            return new ScenarioGraphDtos.SaveRequest(null, List.of());
        }
        Map<Long, ScenarioNode> byId =
                nodes.stream()
                        .collect(
                                Collectors.toMap(
                                        ScenarioNode::getScenarioNodeNo, Function.identity()));
        List<NodeRequest> nodeRequests = new ArrayList<>();
        for (ScenarioNode node : nodes) {
            List<LinkRequest> linkRequests =
                    nodeLinks(node.getScenarioNodeNo()).stream()
                            .sorted(
                                    Comparator.comparingInt(ScenarioNodeLink::getSortOrder)
                                            .thenComparing(
                                                    ScenarioNodeLink::getScenarioNodeLinkNo,
                                                    Comparator.nullsLast(Long::compareTo)))
                            .map(
                                    link ->
                                            new LinkRequest(
                                                    link.getLabel(),
                                                    link.getUrl(),
                                                    link.getLinkType(),
                                                    link.getSortOrder(),
                                                    link.getUseYn()))
                            .toList();
            List<OptionRequest> optionRequests =
                    optionMapper.findByNodeId(node.getScenarioNodeNo()).stream()
                            .sorted(
                                    Comparator.comparingInt(ScenarioNodeOption::getSortOrder)
                                            .thenComparing(
                                                    ScenarioNodeOption::getScenarioNodeOptionNo,
                                                    Comparator.nullsLast(Long::compareTo)))
                            .map(
                                    option ->
                                            new OptionRequest(
                                                    option.getLabel(),
                                                    nextNodeKey(byId, option.getNextNodeNo()),
                                                    option.getConditionExpr(),
                                                    option.getSortOrder(),
                                                    option.getUseYn()))
                            .toList();
            nodeRequests.add(
                    new NodeRequest(
                            node.getNodeKey(),
                            node.getNodeType(),
                            node.getTitle(),
                            node.getContent(),
                            node.getSortOrder(),
                            node.getMetadata(),
                            linkRequests,
                            optionRequests));
        }
        String startNodeKey = nextNodeKey(byId, version.getStartNodeNo());
        return new ScenarioGraphDtos.SaveRequest(startNodeKey, nodeRequests);
    }

    private String nextNodeKey(Map<Long, ScenarioNode> nodesById, Long nodeId) {
        if (nodeId == null) {
            return null;
        }
        ScenarioNode node = nodesById.get(nodeId);
        return node == null ? null : node.getNodeKey();
    }

    private ScenarioVersion latestSourceVersion(Long scenarioId, Long excludedVersionId) {
        List<ScenarioVersion> versions = versionMapper.findByScenarioId(scenarioId);
        if (versions == null) {
            return null;
        }
        return versions.stream()
                .filter(
                        version ->
                                excludedVersionId == null
                                        || !excludedVersionId.equals(
                                                version.getScenarioVersionNo()))
                .filter(version -> !"DRAFT".equals(version.getStatus()))
                .findFirst()
                .orElse(null);
    }

    private void copyLatestGraphToDraft(ScenarioVersion source, Long targetVersionId) {
        if (source == null) {
            return;
        }
        List<ScenarioNode> sourceNodes = nodeMapper.findByVersionId(source.getScenarioVersionNo());
        if (sourceNodes.isEmpty()) {
            return;
        }

        Map<Long, ScenarioNode> copiedBySourceId = new LinkedHashMap<>();
        for (ScenarioNode sourceNode : sourceNodes) {
            ScenarioNode copied = new ScenarioNode();
            copied.setVersionNo(targetVersionId);
            copied.setNodeKey(sourceNode.getNodeKey());
            copied.setNodeType(sourceNode.getNodeType());
            copied.setTitle(sourceNode.getTitle());
            copied.setContent(sourceNode.getContent());
            copied.setSortOrder(sourceNode.getSortOrder());
            copied.setMetadata(hasText(sourceNode.getMetadata()) ? sourceNode.getMetadata() : "{}");
            nodeMapper.insert(copied);
            copiedBySourceId.put(sourceNode.getScenarioNodeNo(), copied);
        }

        for (ScenarioNode sourceNode : sourceNodes) {
            ScenarioNode copiedNode = copiedBySourceId.get(sourceNode.getScenarioNodeNo());
            for (ScenarioNodeLink sourceLink : nodeLinks(sourceNode.getScenarioNodeNo())) {
                ScenarioNodeLink copiedLink = new ScenarioNodeLink();
                copiedLink.setNodeNo(copiedNode.getScenarioNodeNo());
                copiedLink.setLabel(sourceLink.getLabel());
                copiedLink.setUrl(sourceLink.getUrl());
                copiedLink.setLinkType(sourceLink.getLinkType());
                copiedLink.setSortOrder(sourceLink.getSortOrder());
                copiedLink.setUseYn(sourceLink.getUseYn());
                linkMapper.insert(copiedLink);
            }
            for (ScenarioNodeOption sourceOption :
                    optionMapper.findByNodeId(sourceNode.getScenarioNodeNo())) {
                ScenarioNodeOption copiedOption = new ScenarioNodeOption();
                copiedOption.setNodeNo(copiedNode.getScenarioNodeNo());
                ScenarioNode copiedNext = copiedBySourceId.get(sourceOption.getNextNodeNo());
                copiedOption.setNextNodeNo(
                        copiedNext == null ? null : copiedNext.getScenarioNodeNo());
                copiedOption.setLabel(sourceOption.getLabel());
                copiedOption.setConditionExpr(sourceOption.getConditionExpr());
                copiedOption.setSortOrder(sourceOption.getSortOrder());
                copiedOption.setUseYn(sourceOption.getUseYn());
                optionMapper.insert(copiedOption);
            }
        }

        ScenarioNode copiedStart = copiedBySourceId.get(source.getStartNodeNo());
        if (copiedStart != null) {
            versionMapper.setStartNode(targetVersionId, copiedStart.getScenarioNodeNo());
        }
    }

    private String currentUsername() {
        String username = CurrentAdminProvider.currentUsername();
        return username == null ? "system" : username;
    }

    private List<ScenarioNodeLink> nodeLinks(Long nodeId) {
        List<ScenarioNodeLink> links = linkMapper.findByNodeId(nodeId);
        return links == null ? List.of() : links;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
