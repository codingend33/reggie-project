package com.codingend33.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.codingend33.common.BaseContext;
import com.codingend33.common.CustomException;
import com.codingend33.common.R;
import com.codingend33.entity.AddressBook;
import com.codingend33.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/addressBook") //路径和方法绑定
public class AddressBookController {

    //注入服务层对象
    @Autowired
    private AddressBookService addressBookService;


    /**
     //新增地址，
     请求参数是json格式，所以需要requestbody封装为addressbook对象
     返回的也是addressbook对象。
     */

    @PostMapping
    public R<AddressBook> addAddress(@RequestBody AddressBook addressBook){

        // 地址簿中有一个属性是用户ID，所以每个地址都需要设置用户ID，否则不知道这个地址属于谁的。

        // 通过自己创建的工具类BaseContext获得的当前登录用户的ID
        addressBook.setUserId(BaseContext.getCurrentId());

        log.info("addressBook:{}", addressBook);

        //调用服务层的save方法保存地址
        addressBookService.save(addressBook);

        return R.success(addressBook);
    }

    /**
     * 根据id查询地址,回显地址
     * 这个id是用户id，请求的url携带这个id,是非键值对的形式，所以需要PathVariable解析
     */
    @GetMapping("/{id}")
    public R<AddressBook> getById(@PathVariable Long id) {
        //通过id在服务器中获取地址
        AddressBook addressBook = addressBookService.getById(id);

        if (addressBook != null) {
            return R.success(addressBook);
        } else {
            return R.error("地址不存在");
        }
    }


    /**
     * 查询指定用户的全部地址
     * 前端请求的url中有userid, 我们接受的参数可以是long类型，但是为了扩展性，使用addressbook进行封装。
     * 返回的可能是多个地址，所以是一个list

     */
    @GetMapping("/list")
    public R<List<AddressBook>> list(AddressBook addressBook){

        //获取当前用户的id
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("addressBook={}",addressBook);

        //条件构造器
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        //条件1:数据库中的地址薄中的userid 与 传进来的地址簿中的id匹配
        queryWrapper.eq(addressBook.getUserId() != null, AddressBook::getUserId, addressBook.getUserId());
        //条件2：排序
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);

        //SQL:select * from address_book where user_id = ? order by update_time desc
        //获取所有当前用户id一致的地址信息
        List<AddressBook> addressBooks = addressBookService.list(queryWrapper);

        return R.success(addressBooks);
    }


    /**
     * 设置默认地址
     * 将当前的一个地址设置为默认地址，
     * 那请求参数是json格式，需要requestbody封装为一个addressbook对象
     */
    @PutMapping("/default")
    public R<AddressBook> setDefaultAddress(@RequestBody AddressBook addressBook) {

        //条件构造器，注意是LambdaUpdateWrapper。
        //因为是把所有地址的is_default字段设置为0，再把当前的地址设置为1，都是更新的操作。
        LambdaUpdateWrapper<AddressBook> queryWrapper = new LambdaUpdateWrapper<>();

        //条件1：匹配当前用户Id与数据库中的地址
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        //将当前用户的所有地址的is_default字段全部设为0
        queryWrapper.set(AddressBook::getIsDefault, 0);

        //执行更新操作，是根据user_id执行的更新操作，所以该用户的所有地址都设置为0了。
        //SQL:update address_book set is_default = 0 where user_id = ?
        addressBookService.update(queryWrapper);

        //再将当前发送请求的地址对象的is_default字段设为1
        addressBook.setIsDefault(1);

        //再次执行更新操作，这里是根据当前地址的id更新。
        //SQL:update address_book set is_default = 1 where id = ?
        addressBookService.updateById(addressBook);

        return R.success(addressBook);
    }



    /**
     * 查询默认地址
     * 请求网址: http://localhost/addressBook/default
     * 请求方法: GET
     * 在结算的时候，会跳转到订单页面，同时发送请求默认地址。
     */
    @GetMapping("/default")
    public R<AddressBook> getDefault() {

        //创建条件对象
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();

        //条件1：匹配当前用户的所有地址
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());

        //条件2：所有地址的isdefault属性是1的
        queryWrapper.eq(AddressBook::getIsDefault, 1);

        //因为默认的只有1个，所以使用getone获取这个地址。
        //SQL:select * from address_book where user_id = ? and is_default = 1
        AddressBook addressBook = addressBookService.getOne(queryWrapper);

        if (null == addressBook) {
            return R.error("没有找到该对象");
        } else {
            return R.success(addressBook);
        }
    }


    /**
     * 修改地址
     * 请求参数中有新的地址信息，是json格式，使用注解封装为对象

     */
    @PutMapping
    public R<String> updateAdd(@RequestBody AddressBook addressBook) {

        if (addressBook == null) {
            throw new CustomException("地址信息不存在，请刷新重试");
        }
        //直接根据地址id更新即可，
        addressBookService.updateById(addressBook);
        return R.success("地址修改成功");
    }


    /**
     * //删除地址
     * 请求参数是一个地址id，使用RequestParam将ids映射到形参id。
     */
    @DeleteMapping
    public R<String> deleteAdd(@RequestParam("ids") Long id) {

        if (id == null) {
            throw new CustomException("地址信息不存在，请刷新重试");
        }

        //先从数据库中根据id获取到地址。
        AddressBook addressBook = addressBookService.getById(id);

        if (addressBook == null) {
            throw new CustomException("地址信息不存在，请刷新重试");
        }
        //如果有这个地址，就删除
        addressBookService.removeById(id);

        return R.success("地址删除成功");
    }

}
