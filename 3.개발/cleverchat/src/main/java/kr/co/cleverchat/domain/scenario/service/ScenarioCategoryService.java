package kr.co.cleverchat.domain.scenario.service;

import java.util.List;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import kr.co.cleverchat.domain.scenario.dto.ScenarioCategoryDtos.SaveRequest;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioCategoryMapper;
import kr.co.cleverchat.domain.scenario.model.ScenarioCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScenarioCategoryService {

    private final ScenarioCategoryMapper categoryMapper;

    public ScenarioCategoryService(ScenarioCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<ScenarioCategory> findAll() {
        return categoryMapper.findAll();
    }

    @Transactional
    @RequireRole("OPERATOR")
    public ScenarioCategory create(SaveRequest request) {
        ScenarioCategory category = toCategory(new ScenarioCategory(), request);
        categoryMapper.insert(category);
        return categoryMapper.findById(category.getScenarioCategoryNo());
    }

    @Transactional
    @RequireRole("OPERATOR")
    public ScenarioCategory update(Long id, SaveRequest request) {
        ScenarioCategory category = categoryMapper.findById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        toCategory(category, request);
        categoryMapper.update(category);
        return categoryMapper.findById(id);
    }

    private ScenarioCategory toCategory(ScenarioCategory category, SaveRequest request) {
        category.setPScenarioCategoryNo(request.pScenarioCategoryNo());
        category.setName(request.name().trim());
        category.setSortOrder(request.sortOrder());
        category.setUseYn(request.useYn());
        return category;
    }
}
