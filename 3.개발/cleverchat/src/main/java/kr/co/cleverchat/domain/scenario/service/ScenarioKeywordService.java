package kr.co.cleverchat.domain.scenario.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.co.cleverchat.common.audit.AuditTrailRecorder;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.scenario.dto.ScenarioKeywordDtos.KeywordRequest;
import kr.co.cleverchat.domain.scenario.dto.ScenarioKeywordDtos.ReplaceRequest;
import kr.co.cleverchat.domain.scenario.dto.ScenarioKeywordDtos.SynonymRequest;
import kr.co.cleverchat.domain.scenario.event.ScenarioMatchingCacheInvalidator;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioKeywordMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioSynonymMapper;
import kr.co.cleverchat.domain.scenario.model.ScenarioKeyword;
import kr.co.cleverchat.domain.scenario.model.ScenarioSynonym;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScenarioKeywordService {

    private final ScenarioMapper scenarioMapper;
    private final ScenarioKeywordMapper keywordMapper;
    private final ScenarioSynonymMapper synonymMapper;
    private final AuditTrailRecorder auditTrailRecorder;
    private final ScenarioMatchingCacheInvalidator matchingCacheInvalidator;

    public ScenarioKeywordService(
        ScenarioMapper scenarioMapper,
        ScenarioKeywordMapper keywordMapper,
        ScenarioSynonymMapper synonymMapper,
        AuditTrailRecorder auditTrailRecorder,
        ScenarioMatchingCacheInvalidator matchingCacheInvalidator
    ) {
        this.scenarioMapper = scenarioMapper;
        this.keywordMapper = keywordMapper;
        this.synonymMapper = synonymMapper;
        this.auditTrailRecorder = auditTrailRecorder;
        this.matchingCacheInvalidator = matchingCacheInvalidator;
    }

    public List<ScenarioKeyword> findKeywords(Long scenarioId) {
        return keywordMapper.findByScenarioId(scenarioId);
    }

    public List<ScenarioSynonym> findSynonyms(Long keywordId) {
        return synonymMapper.findByKeywordId(keywordId);
    }

    @Transactional
    public void replace(Long scenarioId, ReplaceRequest request) {
        if (scenarioMapper.findById(scenarioId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        keywordMapper.deleteByScenarioId(scenarioId);
        int synonymCount = 0;
        Set<String> keywords = new LinkedHashSet<>();
        for (KeywordRequest item : request.keywords() == null ? List.<KeywordRequest>of() : request.keywords()) {
            String normalized = normalize(item.keyword());
            validateWeight(item.weight());
            if (!keywords.add(normalized)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "중복된 키워드가 있습니다.");
            }
            ScenarioKeyword keyword = new ScenarioKeyword();
            keyword.setScenarioId(scenarioId);
            keyword.setKeyword(normalized);
            keyword.setWeight(item.weight());
            keyword.setEnabled(item.enabled());
            keywordMapper.insert(keyword);
            Set<String> synonyms = new LinkedHashSet<>();
            for (SynonymRequest synonymRequest : item.synonyms() == null ? List.<SynonymRequest>of() : item.synonyms()) {
                String synonymValue = normalize(synonymRequest.synonym());
                validateWeight(synonymRequest.weight());
                if (!synonyms.add(synonymValue)) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "중복된 유사어가 있습니다.");
                }
                ScenarioSynonym synonym = new ScenarioSynonym();
                synonym.setKeywordId(keyword.getId());
                synonym.setSynonym(synonymValue);
                synonym.setWeight(synonymRequest.weight());
                synonym.setEnabled(synonymRequest.enabled());
                synonymMapper.insert(synonym);
                synonymCount++;
            }
        }
        auditTrailRecorder.record("SCENARIO_KEYWORD_SAVE", "scenario", scenarioId, Map.of(
            "keywordCount", keywords.size(),
            "synonymCount", synonymCount
        ));
        matchingCacheInvalidator.onScenarioChanged(scenarioId);
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "키워드와 유사어는 1~100자여야 합니다.");
        }
        return normalized;
    }

    private void validateWeight(Integer weight) {
        if (weight == null || weight < 0 || weight > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "가중치는 0~100이어야 합니다.");
        }
    }
}
