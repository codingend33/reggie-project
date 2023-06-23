package com.codingend33.controller;
import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codingend33.common.BaseContext;
import com.codingend33.common.R;
import com.codingend33.dto.OrderDto;
import com.codingend33.entity.OrderDetail;
import com.codingend33.entity.Orders;
import com.codingend33.entity.ShoppingCart;
import com.codingend33.service.OrderDetailService;
import com.codingend33.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.codingend33.service.OrderService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;


    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }


//用户订单明细
@Transactional
@GetMapping("/userPage")
public R<Page> userPage(int page,int pageSize){
    //构造分页构造器
    Page<Orders> pageInfo = new Page<>(page, pageSize);

    Page<OrderDto> orderDtoPage = new Page<>();

    //构造条件构造器
    LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();

    //添加排序条件
    queryWrapper.orderByDesc(Orders::getOrderTime);

    //进行分页查询
    orderService.page(pageInfo,queryWrapper);

    //对象拷贝
    BeanUtils.copyProperties(pageInfo,orderDtoPage,"records");

    List<Orders> records=pageInfo.getRecords();

    List<OrderDto> list = records.stream().map((item) -> {
        OrderDto ordersDto = new OrderDto();

        BeanUtils.copyProperties(item, ordersDto);
        Long Id = item.getId();
        //根据id查分类对象
        Orders orders = orderService.getById(Id);
        String number = orders.getNumber();
        LambdaQueryWrapper<OrderDetail> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(OrderDetail::getOrderId,number);
        List<OrderDetail> orderDetailList = orderDetailService.list(lambdaQueryWrapper);
        int num=0;

        for(OrderDetail l:orderDetailList){
            num+=l.getNumber().intValue();
        }

        ordersDto.setSumNum(num);
        return ordersDto;
    }).collect(Collectors.toList());

    orderDtoPage.setRecords(list);

    return R.success(orderDtoPage);
}


    //再来一单
    @PostMapping("/again")
    public R<String> again(@RequestBody Map<String,String> map){
        //获取order_id
        Long orderId = Long.valueOf(map.get("id"));

        //条件构造器
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();

        //查询订单细节的数据
        queryWrapper.eq(OrderDetail::getOrderId,orderId);
        List<OrderDetail> details = orderDetailService.list(queryWrapper);

        //获取用户id，待会需要set操作
        Long userId = BaseContext.getCurrentId();

        List<ShoppingCart> shoppingCarts = details.stream().map((item) ->{
            ShoppingCart shoppingCart = new ShoppingCart();
            //Copy对应属性值
            BeanUtils.copyProperties(item,shoppingCart);
            //设置一下userId
            shoppingCart.setUserId(userId);
            //设置一下创建时间为当前时间
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;

        }).collect(Collectors.toList());

        //加入购物车
        shoppingCartService.saveBatch(shoppingCarts);

        return R.success("喜欢吃就再来一单吖~");
    }



    //后台订单明细
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, Long number, String beginTime, String endTime) {

        //获取当前id
        Page<Orders> pageInfo = new Page<>(page, pageSize);

        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //按时间降序排序
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //订单号
        queryWrapper.eq(number != null, Orders::getId, number);
        //时间段，大于开始，小于结束
        queryWrapper.gt(!StringUtils.isEmpty(beginTime), Orders::getOrderTime, beginTime)
                .lt(!StringUtils.isEmpty(endTime), Orders::getOrderTime, endTime);

        orderService.page(pageInfo, queryWrapper);


        Page<OrderDto> orderDtoPage = new Page<>(page, pageSize);
        BeanUtils.copyProperties(pageInfo, orderDtoPage, "records");


        List<OrderDto> list = pageInfo.getRecords().stream().map((item) -> {
            OrderDto orderDto = new OrderDto();
            //获取orderId,然后根据这个id，去orderDetail表中查数据
            Long orderId = item.getId();
            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> details = orderDetailService.list(wrapper);
            BeanUtils.copyProperties(item, orderDto);
            //之后set一下属性
            orderDto.setOrderDetails(details);
            return orderDto;
        }).collect(Collectors.toList());

        orderDtoPage.setRecords(list);

        //日志输出看一下
        log.info("list:{}", list);

        return R.success(orderDtoPage);
    }



    //修改订单状态
    @PutMapping
    public R<String> changeStatus(@RequestBody Map<String, String> map) {
        int status = Integer.parseInt(map.get("status"));
        Long orderId = Long.valueOf(map.get("id"));
        log.info("修改订单状态:status={status},id={id}", status, orderId);
        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Orders::getId, orderId);
        updateWrapper.set(Orders::getStatus, status);
        orderService.update(updateWrapper);

        return R.success("订单状态修改成功");
    }



}