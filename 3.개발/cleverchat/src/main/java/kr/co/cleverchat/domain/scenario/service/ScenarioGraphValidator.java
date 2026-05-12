package kr.co.cleverchat.domain.scenario.service;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos.NodeRequest;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos.OptionRequest;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos.SaveRequest;
import org.springframework.stereotype.Component;

@Component
public class ScenarioGraphValidator {

    public void validateForSave(SaveRequest request) {
        Map<String, NodeRequest> nodes = nodeMap(request.nodes());
        if (!nodes.containsKey(request.startNodeKey())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "시작 노드가 노드 목록에 없습니다.");
        }
        for (NodeRequest node : request.nodes()) {
            if ("END".equals(node.nodeType()) && node.options() != null && !node.options().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "END 노드는 옵션을 가질 수 없습니다.");
            }
            for (OptionRequest option : safeOptions(node)) {
                if (hasText(option.nextNodeKey()) && !nodes.containsKey(option.nextNodeKey())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 다음 노드가 있습니다.");
                }
            }
        }
    }

    public void validateForPublish(SaveRequest request) {
        validateForSave(request);
        Map<String, NodeRequest> nodes = nodeMap(request.nodes());
        Set<String> visited = reachable(request.startNodeKey(), nodes);
        if (visited.size() != nodes.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "시작 노드에서 도달할 수 없는 노드가 있습니다.");
        }
    }

    private Map<String, NodeRequest> nodeMap(List<NodeRequest> nodes) {
        Map<String, NodeRequest> result = new HashMap<>();
        for (NodeRequest node : nodes) {
            if (result.put(node.nodeKey(), node) != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "중복된 노드 키가 있습니다.");
            }
            if (!node.nodeKey().matches("[A-Za-z0-9_-]{1,80}")) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "노드 키는 영문, 숫자, 하이픈, 언더스코어만 허용합니다.");
            }
            if (!Set.of("QUESTION", "ANSWER", "BRANCH", "END").contains(node.nodeType())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "허용되지 않는 노드 유형입니다.");
            }
        }
        return result;
    }

    private Set<String> reachable(String startNodeKey, Map<String, NodeRequest> nodes) {
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(startNodeKey);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            for (OptionRequest option : safeOptions(nodes.get(current))) {
                if (hasText(option.nextNodeKey())) {
                    queue.add(option.nextNodeKey());
                }
            }
        }
        return visited;
    }

    private List<OptionRequest> safeOptions(NodeRequest node) {
        return node.options() == null ? List.of() : node.options();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
