package com.codingend33.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codingend33.common.BaseContext;
import com.codingend33.common.R;
import com.codingend33.entity.ShoppingCart;
import com.codingend33.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    //添加菜品到购物车
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车添加信息：{}",shoppingCart);

        //获取当前用户id
        Long currentId = BaseContext.getCurrentId();

        //给购物车设置当前用户id，指定当前是哪个用户的购物车数据
        shoppingCart.setUserId(currentId);

        //查询当前菜品或者套餐是否已经在购物车当中

        //获取当前菜品id
        Long dishId = shoppingCart.getDishId();
        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //判断添加的是菜品还是套餐（通过传递过来的ID进行判断）
        if (dishId != null) {
            //添加到购物车的为菜品
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        } else {
            //添加到购物车的为套餐
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }

        //SQL:select *from shopping_cart where user_id=? and dish_id/setmeal_id =?
        //查询当前菜品或者套餐是否在购物车中
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if (cartServiceOne != null) {
            //如果已存在就在当前的数量基础上再加1
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number + 1);
            shoppingCartService.updateById(cartServiceOne);
        } else {
            //如果不存在，则还需设置一下创建时间.方便后面查看购物车时，能够按照顺序排列。
            shoppingCart.setCreateTime(LocalDateTime.now());
            //如果不存在，则添加到购物车，数量默认为1
            shoppingCartService.save(shoppingCart);

            //这里是为了统一结果，最后都返回cartServiceOne。
            //因为这里是进入的else分支，说明cartServiceOne是空的，现在给他赋值即可。
            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);

    }



    //查看购物车
    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //获取当前用户的ID
        Long userId = BaseContext.getCurrentId();
        //匹配当前用户的购物车
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        //添加一个排序条件，最后添加的排在最前面。
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);
        //展示购物车信息
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);

        return R.success(shoppingCarts);
    }


    //清空购物车
    @DeleteMapping("/clean")
    public R<String> clean() {
        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        queryWrapper.eq(userId != null, ShoppingCart::getUserId, userId);
        //删除当前用户id的所有购物车数据
        shoppingCartService.remove(queryWrapper);
        return R.success("成功清空购物车");

    }


    //减少
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart) {
        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();
        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //只查询当前用户ID的购物车
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        //代表数量减少的是菜品数量
        if (dishId != null) {
            //通过dishId查出购物车菜品数据
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
            ShoppingCart dishCart = shoppingCartService.getOne(queryWrapper);
            //将查出来的数据的数量-1
            dishCart.setNumber(dishCart.getNumber() - 1);
            Integer currentNum = dishCart.getNumber();
            //然后判断
            if (currentNum > 0) {
                //大于0则更新
                shoppingCartService.updateById(dishCart);
            } else if (currentNum == 0) {
                //小于0则删除
                shoppingCartService.removeById(dishCart.getId());
            }
            return R.success(dishCart);
        }

        if (setmealId != null) {
            //通过setmealId查询购物车套餐数据
            queryWrapper.eq(ShoppingCart::getSetmealId, setmealId);
            ShoppingCart setmealCart = shoppingCartService.getOne(queryWrapper);
            //将查出来的数据的数量-1
            setmealCart.setNumber(setmealCart.getNumber() - 1);
            Integer currentNum = setmealCart.getNumber();
            //然后判断
            if (currentNum > 0) {
                //大于0则更新
                shoppingCartService.updateById(setmealCart);
            } else if (currentNum == 0) {
                //等于0则删除
                shoppingCartService.removeById(setmealCart.getId());
            }
            return R.success(setmealCart);
        }
        return R.error("系统繁忙，请稍后再试");
    }





}
