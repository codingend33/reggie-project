package com.codingend33.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codingend33.entity.ShoppingCart;
import com.codingend33.mapper.ShoppingCartMapper;
import com.codingend33.service.ShoppingCartService;
import org.springframework.stereotype.Service;

@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {
}
