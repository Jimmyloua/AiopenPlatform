package com.aiplatform.platform.repository;

import com.aiplatform.platform.model.Skill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Skill Repository
 */
@Mapper
public interface SkillRepository extends BaseMapper<Skill> {

}