package com.aiplatform.platform.repository;

import com.aiplatform.platform.model.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * User Repository
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {

}