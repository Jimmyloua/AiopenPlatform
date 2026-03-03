package com.aiplatform.platform.repository;

import com.aiplatform.platform.model.Credential;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Credential Repository
 */
@Mapper
public interface CredentialRepository extends BaseMapper<Credential> {

}