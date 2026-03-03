package com.aiplatform.platform.repository;

import com.aiplatform.platform.model.ApiKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * API Key Repository
 */
@Mapper
public interface ApiKeyRepository extends BaseMapper<ApiKey> {

}