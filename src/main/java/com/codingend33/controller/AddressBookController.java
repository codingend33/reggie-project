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
@RequestMapping("/addressBook")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    //查询指定用户的全部地址
    @GetMapping("/list")
    public R<List<AddressBook>> list(AddressBook addressBook){

        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("addressBook={}",addressBook);

        //条件构造器
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        //设定条件
        queryWrapper.eq(addressBook.getUserId() != null, AddressBook::getUserId, addressBook.getUserId());
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);

        //SQL:select * from address_book where user_id = ? order by update_time desc
        List<AddressBook> addressBooks = addressBookService.list(queryWrapper);
        return R.success(addressBooks);

    }


    //保存新增地址
    @PostMapping
    public R<AddressBook> addAddress(@RequestBody AddressBook addressBook){
        // 地址簿中有一个属性是用户ID，所以每个地址都需要设置用户ID，否则不知道这个地址属于谁的。
        // 通过自己创建的工具类BaseContext获得的当前登录用户的ID
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("addressBook:{}", addressBook);
        addressBookService.save(addressBook);
        return R.success(addressBook);
    }


    //设置默认地址
    @PutMapping("/default")
    public R<AddressBook> setDefaultAddress(@RequestBody AddressBook addressBook) {

        //条件构造器，注意是LambdaUpdateWrapper。
        //因为是把所有地址的is_default字段设置为0，再把当前的地址设置为1，都是更新的操作。
        LambdaUpdateWrapper<AddressBook> queryWrapper = new LambdaUpdateWrapper<>();
        //条件：匹配当前用户Id。
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        //将当前用户的所有地址的is_default字段全部设为0
        queryWrapper.set(AddressBook::getIsDefault, 0);
        //执行更新操作，是根据user_id执行的更新操作，所以该用户的所有地址都设置为0了。
        //SQL:update address_book set is_default = 0 where user_id = ?
        addressBookService.update(queryWrapper);

        //随后再将当前发送请求的地址的is_default字段设为1
        addressBook.setIsDefault(1);
        //再次执行更新操作，这里是根据当前地址的id更新。
        //SQL:update address_book set is_default = 1 where id = ?
        addressBookService.updateById(addressBook);
        return R.success(addressBook);
    }


    /**
     * 查询默认地址
     */
    @GetMapping("/default")
    public R<AddressBook> getDefault() {

        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());

        //获取默认地址
        queryWrapper.eq(AddressBook::getIsDefault, 1);

        //SQL:select * from address_book where user_id = ? and is_default = 1
        AddressBook addressBook = addressBookService.getOne(queryWrapper);

        if (null == addressBook) {
            return R.error("没有找到该对象");
        } else {
            return R.success(addressBook);
        }
    }


    /**
     * 根据id查询地址,回显地址
     */
    @GetMapping("/{id}")
    public R<AddressBook> getById(@PathVariable Long id) {
        AddressBook addressBook = addressBookService.getById(id);
        if (addressBook != null) {
            return R.success(addressBook);
        } else {
            return R.error("地址不存在");
        }
    }



    //修改地址
    @PutMapping
    public R<String> updateAdd(@RequestBody AddressBook addressBook) {
        if (addressBook == null) {
            throw new CustomException("地址信息不存在，请刷新重试");
        }
        addressBookService.updateById(addressBook);
        return R.success("地址修改成功");
    }


    //删除地址
    @DeleteMapping
    public R<String> deleteAdd(@RequestParam("ids") Long id) {
        if (id == null) {
            throw new CustomException("地址信息不存在，请刷新重试");
        }
        AddressBook addressBook = addressBookService.getById(id);
        if (addressBook == null) {
            throw new CustomException("地址信息不存在，请刷新重试");
        }
        addressBookService.removeById(id);

        return R.success("地址删除成功");
    }




}
