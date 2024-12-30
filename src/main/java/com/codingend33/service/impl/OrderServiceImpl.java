package com.codingend33.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codingend33.common.BaseContext;
import com.codingend33.common.CustomException;
import com.codingend33.entity.*;
import com.codingend33.mapper.OrderMapper;
import com.codingend33.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 用户下单去支付
     * 流程：
     * 	获取当前用户id
     * 	根据用户id查询其购物车数据
     * 	根据查询到的购物车数据，对订单表插入数据（1条），因为不管多少买了多少商品，都在一个订单中
     * 	根据查询到的购物车数据，对订单明细表插入数据（多条），不同的菜品或套餐都是不同的明细
     *  清空购物车数据
     *
     *  请求中只有这三个参数：
     *  addressBookId: "1579828298672885762",
     *  payMethod: 1,
     *  remark: ""
     *
     *  需要通过addressBookId获取订单的地址。而用户的id可以通过session获取，就能获取到购物车信息
     */

    @Override
    @Transactional
    public void submit(Orders orders) {

        //获得当前用户id
        Long userId = BaseContext.getCurrentId();

        //查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        //条件1：匹配数据库中购物车的userid 与 当前用户的id
        wrapper.eq(ShoppingCart::getUserId,userId);

        //获取当前用户的所有购物车信息，
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(wrapper);

        //购物车为空，抛出异常，“不能下单”
        if(shoppingCarts == null || shoppingCarts.size() == 0){
            throw new CustomException("购物车为空，不能下单");
        }

        //获取订单表需要的数据，需要用户的数据，以及用户的地址，然后插入到订单表中。
        //查询用户数据
        User user = userService.getById(userId);

        //从订单中获取地址id
        Long addressBookId = orders.getAddressBookId();
        //根据订单的地址id 获取用户的的地址
        AddressBook addressBook = addressBookService.getById(addressBookId);

        if(addressBook == null){
            throw new CustomException("用户地址信息有误，不能下单");
        }

        //获取订单明细表需要的的数据，主要是餐品或套餐的，然后插入到订单明细表中
        //当前提供的数据比较少，而实际的订单明细表中有很多属性需要填充，所以需要手动设置一些。


        //通过MP内置的IdWorker方法生成订单号
        long orderId = IdWorker.getId();//订单号

        //声明一个初始值为0的订单总金额变量
        AtomicInteger amount = new AtomicInteger(0);

        //为订单细节表填充相关数据，并遍历订单明细中的订单金额以便计算
        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            //创建一个订单明细对象
            OrderDetail orderDetail = new OrderDetail();

            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());

            // 金额的累加操作
            // 单份金额*份数。
            //BigDecimal是进行精确的运算，但不能使用 + - * / ，而是调用相应的方法multiply，参数也必须是BigDecimal的对象。
            //intValue()是转成整数类型
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;

        }).collect(Collectors.toList());

        //为订单表填充相关数据
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));//计算总金额
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));

        //向订单表插入数据，一条数据
        this.save(orders);

        //向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetails);

        //清空购物车数据,这个条件对象在上面已经生成了
        shoppingCartService.remove(wrapper);

    }
}