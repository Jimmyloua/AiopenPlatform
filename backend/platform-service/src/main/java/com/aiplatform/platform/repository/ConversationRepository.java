package com.aiplatform.platform.repository;

import com.aiplatform.platform.model.Conversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Conversation Repository
 */
@Mapper
public interface ConversationRepository extends BaseMapper<Conversation> {

}