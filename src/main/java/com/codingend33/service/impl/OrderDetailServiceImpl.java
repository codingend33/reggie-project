package com.codingend33.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codingend33.entity.OrderDetail;
import com.codingend33.mapper.OrderDetailMapper;
import com.codingend33.service.OrderDetailService;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {

}