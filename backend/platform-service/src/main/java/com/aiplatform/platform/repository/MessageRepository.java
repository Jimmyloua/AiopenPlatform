package com.aiplatform.platform.repository;

import com.aiplatform.platform.model.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Message Repository
 */
@Mapper
public interface MessageRepository extends BaseMapper<Message> {

}