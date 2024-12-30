package com.codingend33.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codingend33.entity.Orders;

public interface OrderService extends IService<Orders> {

    //自定义的订单提交方法
    void submit(Orders orders);

}
