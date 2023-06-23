package com.codingend33.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codingend33.entity.Orders;

public interface OrderService extends IService<Orders> {
    void submit(Orders orders);
}
