package com.aiplatform.platform.service;

import com.aiplatform.platform.dto.SkillCreateRequest;
import com.aiplatform.platform.dto.SkillResponse;
import com.aiplatform.platform.model.Skill;
import com.aiplatform.platform.repository.SkillRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Skill Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    /**
     * Create a new skill
     */
    public SkillResponse createSkill(SkillCreateRequest request, Long userId) {
        Skill skill = new Skill();
        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        skill.setCategory(request.getCategory());
        skill.setSchemaJson(request.getSchemaJson());
        skill.setHandlerConfig(request.getHandlerConfig());
        skill.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);
        skill.setCreatedBy(userId);

        skillRepository.insert(skill);

        return toSkillResponse(skill);
    }

    /**
     * Get skill by ID
     */
    public SkillResponse getSkillById(Long id) {
        Skill skill = skillRepository.selectById(id);
        if (skill == null) {
            throw new RuntimeException("Skill not found");
        }
        return toSkillResponse(skill);
    }

    /**
     * List skills with pagination
     */
    public Page<SkillResponse> listSkills(int page, int size, String category, Boolean publicOnly) {
        Page<Skill> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();

        if (category != null && !category.isEmpty()) {
            wrapper.eq(Skill::getCategory, category);
        }

        if (publicOnly != null && publicOnly) {
            wrapper.eq(Skill::getIsPublic, true);
        }

        wrapper.orderByDesc(Skill::getCreatedAt);

        Page<Skill> result = skillRepository.selectPage(pageParam, wrapper);

        Page<SkillResponse> responsePage = new Page<>();
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setTotal(result.getTotal());
        responsePage.setRecords(result.getRecords().stream()
                .map(this::toSkillResponse)
                .collect(Collectors.toList()));

        return responsePage;
    }

    /**
     * Update skill
     */
    public SkillResponse updateSkill(Long id, SkillCreateRequest request, Long userId) {
        Skill skill = skillRepository.selectById(id);
        if (skill == null) {
            throw new RuntimeException("Skill not found");
        }

        if (skill.getCreatedBy() != null && !skill.getCreatedBy().equals(userId)) {
            throw new RuntimeException("Not authorized to update this skill");
        }

        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        skill.setCategory(request.getCategory());
        skill.setSchemaJson(request.getSchemaJson());
        skill.setHandlerConfig(request.getHandlerConfig());
        skill.setIsPublic(request.getIsPublic());

        skillRepository.updateById(skill);

        return toSkillResponse(skill);
    }

    /**
     * Delete skill
     */
    public void deleteSkill(Long id, Long userId) {
        Skill skill = skillRepository.selectById(id);
        if (skill == null) {
            throw new RuntimeException("Skill not found");
        }

        if (skill.getCreatedBy() != null && !skill.getCreatedBy().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this skill");
        }

        skillRepository.deleteById(id);
    }

    /**
     * Get skills by IDs
     */
    public List<SkillResponse> getSkillsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Skill> skills = skillRepository.selectBatchIds(ids);
        return skills.stream()
                .map(this::toSkillResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert to SkillResponse
     */
    private SkillResponse toSkillResponse(Skill skill) {
        SkillResponse response = new SkillResponse();
        response.setId(skill.getId());
        response.setName(skill.getName());
        response.setDescription(skill.getDescription());
        response.setCategory(skill.getCategory());
        response.setSchemaJson(skill.getSchemaJson());
        response.setHandlerConfig(skill.getHandlerConfig());
        response.setIsPublic(skill.getIsPublic());
        response.setCreatedAt(skill.getCreatedAt());
        return response;
    }

}