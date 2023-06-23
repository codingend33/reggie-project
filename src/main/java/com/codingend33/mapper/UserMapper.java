package com.codingend33.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codingend33.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
