package com.codingend33.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codingend33.common.R;
import com.codingend33.entity.Category;
import com.codingend33.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public R<String> save(@RequestBody Category category){
        log.info("category:{}",category);
        categoryService.save(category);
        return R.success("新增分类功能");

    }



    @GetMapping("/page")
    public R<Page> page(int page,int pageSize){
        //构造分页构造器
        Page<Category> pageInfo=new Page<>(page,pageSize);
        //构造条件构造器
        LambdaQueryWrapper<Category> queryWrapper=new LambdaQueryWrapper<>();
        //添加排序条件，根据sort进行排序
        queryWrapper.orderByAsc(Category::getSort);
        //进行分页查询
        categoryService.page(pageInfo,queryWrapper);

        return R.success(pageInfo);

    }

    @DeleteMapping()
    public R<String> delete(Long id){
        log.info("删除分类，id为{}",id);
        //categoryService.removeById(id);
        //代码完善之后categoryService.remove(id);
        categoryService.remove(id);
        return R.success("分类信息删除成功");
    }


    @PutMapping
    public R<String> update(@RequestBody Category category){
        log.info("修改分类信息为：{}", category);
        //调用MP封装的updateById方法
        categoryService.updateById(category);
        return R.success("修改分类信息成功");
    }

    //根据条件查询分类数据
    //泛型是嵌套的，因为餐品分类是数组形式的。
    @GetMapping("/list")
    public R<List<Category>> list(Category category){
        //条件构造器
        LambdaQueryWrapper<Category> categoryLambdaQueryWrapper = new LambdaQueryWrapper<>();

        //添加条件，这里只需要判断是否为菜品（type为1是菜品，type为2是套餐）
        categoryLambdaQueryWrapper.eq(category.getType()!= null,Category::getType,category.getType());

        //添加排序条件。category类中有排序的属性，所以根据它的排序作为条件，再加上第二个以更新时间排序的条件
        categoryLambdaQueryWrapper.orderByAsc(Category::getSort).orderByAsc(Category::getUpdateTime);

        List<Category> list = categoryService.list(categoryLambdaQueryWrapper);

        return R.success(list);
    }

}
