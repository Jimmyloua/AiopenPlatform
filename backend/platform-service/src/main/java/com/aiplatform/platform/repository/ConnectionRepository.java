package com.aiplatform.platform.repository;

import com.aiplatform.platform.model.Connection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Connection Repository
 */
@Mapper
public interface ConnectionRepository extends BaseMapper<Connection> {

}