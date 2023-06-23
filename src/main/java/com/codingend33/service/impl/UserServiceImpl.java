package com.codingend33.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codingend33.entity.User;
import com.codingend33.mapper.UserMapper;
import com.codingend33.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService {
}
