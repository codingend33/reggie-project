package com.codingend33.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codingend33.common.R;
import com.codingend33.dto.DishDto;
import com.codingend33.dto.SetmealDto;
import com.codingend33.entity.Category;
import com.codingend33.entity.Dish;
import com.codingend33.entity.Setmeal;
import com.codingend33.entity.SetmealDish;
import com.codingend33.service.CategoryService;
import com.codingend33.service.DishService;
import com.codingend33.service.SetmealDishService;
import com.codingend33.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    //因为要操作两个表，所以配置和SetmealDishService 和SetmealService的 bean对象。
    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;


    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;



    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息：{}", setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("套餐添加成功");
    }


    //套餐分页
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){

        //构造Setmeal分页构造器
        Page<Setmeal> pageInfo=new Page<>(page,pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        //根据name进行模糊查询
        queryWrapper.like(!StringUtils.isEmpty(name),Setmeal::getName,name);
        //添加排序条件，根据sort进行排序
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        //进行分页查询
        setmealService.page(pageInfo,queryWrapper);

        //但是套餐分类名称这一列没有展示，因为setmeal里没有这个属性，它只有套餐ID
        //所以就需要使用SetmealDto，这个类型里面有扩展的套餐分类名称的属性。
        //我们就可以通过需要套餐ID获取套餐分类名称。

        //构造SetmealDto分页构造器
        Page<SetmealDto> pageDtoInfo=new Page<>();

        //对象拷贝。将pageinfo的分页结果数据先拷贝到pageDtoInfo。（原，新，排除）
        //除了records,其他的都可以拷贝过去。因为records里是所有套餐的数据，需要额外处理，而且泛型也不一样。
        BeanUtils.copyProperties(pageInfo,pageDtoInfo,"records");

        //获取records的值
        List<Setmeal> records=pageInfo.getRecords();
        //遍历records，获取套餐数据
        List<SetmealDto> list= records.stream().map((item)->{
            //新建SetmealDto对象
            SetmealDto setmealDto=new SetmealDto();

            //将遍历后的每一个套餐数据复制给SetmealDto
            //item里没有categoryname，但有CategoryId，可以通过它获取categoryname
            BeanUtils.copyProperties(item,setmealDto);
            //从套餐数据中获取CategoryId
            Long categoryId = item.getCategoryId();
            //根据categoryid查找分类对象
            Category category = categoryService.getById(categoryId);
            //从category对象中获取categoryname
            if(category!=null){
                String categoryName = category.getName();
                //给setmealDto对象中的category添加categoryname
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
            //将setmealDto对象收起来并转成集合类型，最终赋值给‘List<SetmealDto> list’
        }).collect(Collectors.toList());

        //给pageDtoInfo添加setmealDto对象的数据
        pageDtoInfo.setRecords(list);

        return R.success(pageDtoInfo);

    }

    //删除套餐
    //因为存在批量删除的可能，所以形参是list集合
    @DeleteMapping
    public R<String> deleteByIds(@RequestParam List<Long> ids) {

        log.info("要删除的套餐id为：{}",ids);
        setmealService.removeWithDish(ids);
        return R.success("删除成功");
    }

    //起售和停售
    //通过数组保存ids，批量起售停售都能生效
    @PostMapping("/status/{status}")
    public R<String> sale(@PathVariable int status,String[] ids){
        for (String id:ids){
            Setmeal setmeal = setmealService.getById(id);
            setmeal.setStatus(status);
            setmealService.updateById(setmeal);
        }
        return R.success("修改成功");

    }

    //修改套餐

    //根据id查询当前套餐信息，用于回显数据到修改界面
    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id){
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);

        return R.success(setmealDto);
    }


    //操作修改的套餐信息
    @PutMapping()
    public R<String> update(@RequestBody SetmealDto setmealDto){
        setmealService.updateWithDish(setmealDto);
        return R.success("修改成功");
    }


    // 左侧套餐展示
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId()!=null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus()!=null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);
    }

    //点击图片查看套餐详情
    @GetMapping("/dish/{id}")
    public R<List<DishDto>> showSetmealDish(@PathVariable Long id) {
        //条件构造器
        LambdaQueryWrapper<SetmealDish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //手里的数据只有setmealId
        dishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);

        //查询数据
        List<SetmealDish> records = setmealDishService.list(dishLambdaQueryWrapper);

        List<DishDto> dtoList = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            //copy数据
            BeanUtils.copyProperties(item,dishDto);
            //查询对应菜品id
            Long dishId = item.getDishId();

            //根据菜品id获取具体菜品数据，这里要自动装配 dishService
            Dish dish = dishService.getById(dishId);
            //其实主要数据是要那个图片，不过我们这里多copy一点也没事
            BeanUtils.copyProperties(dish,dishDto);

            return dishDto;

        }).collect(Collectors.toList());

        return R.success(dtoList);
    }



}
