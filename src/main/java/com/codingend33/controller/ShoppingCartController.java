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

    /**
     添加菜品到购物车
     但是shoppingCart中 UserId并没有传递过来，所以需要设置用户ID，以确定当前菜品或套餐属于哪个用户
     传递的参数是json格式，所以需要封装为对象
     */

    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){

        log.info("购物车添加信息：{}",shoppingCart);

        //使用工具类，获取当前用户id
        Long currentId = BaseContext.getCurrentId();

        //给购物车设置当前用户id，指定当前是哪个用户的购物车数据
        shoppingCart.setUserId(currentId);

        //查询请求的菜品或者套餐是否已经在购物车当中，
        //添加的可能是餐品，也可能是套餐。所以先判断添加的是菜品还是套餐（通过传递过来的ID进行判断）

        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //条件1：匹配购物车的userid与 当前用户的id
        queryWrapper.eq(ShoppingCart::getUserId,currentId);

        //获取请求的菜品id
        Long dishId = shoppingCart.getDishId();

        //如果请求的是dishid，说明是餐品
        if (dishId != null) {
            //条件2.1 添加到购物车的为菜品，匹配购物车中的dishid 与请求的dishid
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        } else {
            //条件2.2 如果dishid是空，添加到购物车的为套餐，匹配购物车中的setmealid 与请求的setmealid
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }

        //SQL:select *from shopping_cart where user_id=? and dish_id或者setmeal_id =?
        //根据条件，获取的结果只有一个，请求的是餐品或套餐，所以使用getOne，
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        //如果有返回值，说明请求的餐品或套餐已经在购物车中了
        if (cartServiceOne != null) {
            //获取当前的数量（number是购物车对象的一个属性）
            Integer number = cartServiceOne.getNumber();
            //再加1。
            cartServiceOne.setNumber(number + 1);
            //更新购物车
            shoppingCartService.updateById(cartServiceOne);

        } else {
            //如果不存在，则还需设置一下请求参数对象的创建时间.方便后面查看购物车时，能够按照顺序排列。
            shoppingCart.setCreateTime(LocalDateTime.now());
            //如果不存在，则添加到购物车，数量默认为1
            shoppingCartService.save(shoppingCart);

            //这里是为了统一结果，最后都返回cartServiceOne。
            //因为这里是进入的else分支，说明cartServiceOne是空的，现在将新添加的购物车赋值给他。
            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);

    }


    /**
     *查看购物车
     *把前端假数据改回来，之前为了不报错，我们将查看购物车的地址换成了一个死数据，那现在我们要做的就是换成真数据：
     * 在确认订单页面，会自动发送另外一个ajax请求，用于获取当前登录用户的购物车数据
     *
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {

        //创建条件对象
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();

        //获取当前用户的ID
        Long userId = BaseContext.getCurrentId();
        //条件1：匹配到当前用户的购物车
        queryWrapper.eq(ShoppingCart::getUserId, userId);

        //条件2：添加一个排序条件，最后添加的排在最前面。
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        //展示购物车信息
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);

        return R.success(shoppingCarts);
    }


    /**
     清空购物车
     我们点击上图中的清空按钮，请求路径为/shoppingCart/clean，请求方式为DELETE
     清空购物车的逻辑倒是比较简单，获取用户id，然后去shopping__cart表中删除对应id的数据即可
     */

    @DeleteMapping("/clean")
    public R<String> clean() {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //条件1：匹配当前用户的购物车
        queryWrapper.eq(userId != null, ShoppingCart::getUserId, userId);

        //删除当前用户id的所有购物车数据
        shoppingCartService.remove(queryWrapper);

        return R.success("成功清空购物车");

    }



    /**
     * 修改购物车
     * 之前下单的时候，只有加号按钮能用，减号按钮还没配置，我们点击减号看看啥请求
     * 请求网址: http://localhost/shoppingCart/sub
     * 请求方法: POST，json数据，只有dishId和setmealId
     * 根据这两个id，来对不同的菜品/套餐的number属性修改（对应的数量-1），如果number等于0，则删除
     */
    //减少
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart) {
        //获取购物车中餐品的id
        Long dishId = shoppingCart.getDishId();
        //获取购物车中套餐的id
        Long setmealId = shoppingCart.getSetmealId();

        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //只查询当前用户ID的购物车
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        //如果dishid不是空，说明购物车中有餐品
        if (dishId != null) {

            //条件1：匹配购物车菜品的id 与 请求减少的菜品id
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
            //如果有返回值，说明购物车中有需要删除的餐品
            ShoppingCart dishCart = shoppingCartService.getOne(queryWrapper);
            //数量减1
            dishCart.setNumber(dishCart.getNumber() - 1);
            //获取当前的数量
            Integer currentNum = dishCart.getNumber();

            //然后判断
            if (currentNum > 0) {
                //大于0则更新购物车中这个餐品的数量
                shoppingCartService.updateById(dishCart);
            } else if (currentNum == 0) {
                //小于0则从购物车删除
                shoppingCartService.removeById(dishCart.getId());
            }
            return R.success(dishCart);
        }

        //如果setmealid不是空，说明购物车中有套餐
        if (setmealId != null) {

            //匹配购物车中的套餐id与请求修改的套餐id
            queryWrapper.eq(ShoppingCart::getSetmealId, setmealId);
            //如果有返回值，说明购物车中有需要修改的套餐
            ShoppingCart setmealCart = shoppingCartService.getOne(queryWrapper);

            //将购物车中的套餐的数量-1
            setmealCart.setNumber(setmealCart.getNumber() - 1);
            //获取当前购物车中套餐的数量
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
