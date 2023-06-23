package com.codingend33.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codingend33.entity.Category;

public interface CategoryService extends IService<Category> {

    //因为要判断删除的分类是否关联菜品和套餐，所以不再使用Iservie中提供的removebyid
    //需要自己创建一个remove方法。
    public void remove(Long id);
}
