package com.codingend33.dto;

import com.codingend33.entity.OrderDetail;
import com.codingend33.entity.Orders;
import lombok.Data;

import java.util.List;

@Data
public class OrderDto extends Orders {
    private String userName;

    private String phone;

    private String address;

    private String consignee;
    private int sumNum;

    private List<OrderDetail> orderDetails;
}
