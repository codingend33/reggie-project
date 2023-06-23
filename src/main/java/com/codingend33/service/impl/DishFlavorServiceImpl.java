package com.codingend33.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codingend33.entity.DishFlavor;
import com.codingend33.mapper.DishFlavorMapper;
import com.codingend33.service.DishFlavorService;
import org.springframework.stereotype.Service;

@Service
public class DishFlavorServiceImpl extends ServiceImpl<DishFlavorMapper,DishFlavor> implements DishFlavorService {
}
