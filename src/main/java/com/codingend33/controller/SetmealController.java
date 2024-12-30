package com.codingend33.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐和套餐餐品控制器
 */


@RestController // controller + responsebody
@RequestMapping("/setmeal")  // 路径和方法绑定
@Slf4j
public class SetmealController {

    //因为要操作两个表，所以配置和SetmealDishService 和SetmealService的 bean对象。
    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;

    //分类bean
    @Autowired
    private CategoryService categoryService;

    //餐品bean
    @Autowired
    private DishService dishService;


    /**
     *套餐保存功能
     *请求参数中有setmeal参数，也有setmealdish的参数，所以只有setmeal对象或setmealdish对象接收参数是不行的
     * 需要一个新的数据传输类型SetmealDto，继承了setmeal所有属性，并扩展一些属性能接收setmealdish
     * RequestBody注解将请求参数封装为一个SetmealDto对象
     */
    @PostMapping
    //设置allEntries为true，清空缓存名称为setmealCache的所有缓存
    @CacheEvict(value = "setmealCache", allEntries = true)
    public R<String> save(@RequestBody SetmealDto setmealDto) {

        log.info("套餐信息：{}", setmealDto);

        //因为保存涉及两个表的参数，所以没有内置的方法可以使用
        setmealService.saveWithDish(setmealDto);

        return R.success("套餐添加成功");
    }

    /**
     * 套餐分页查询
     * 请求地址：http://localhost/setmeal/page?page=1&pageSize=10&name=1
     *  page, pageSize, name 参数都是url携带的，所以方法形参名称一致就能绑定
     *  前端需要分页数据中的recordt和total等属性，这些只有MP提供的Page类有，所以返回值类型是page
     *
     */


    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){

        //构造Setmeal分页构造器
        Page<Setmeal> pageInfo=new Page<>(page,pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();

        //条件1：根据name进行模糊查询
        queryWrapper.like(!StringUtils.isEmpty(name),Setmeal::getName,name);
        //条件2：添加排序条件，根据sort进行排序
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        //进行分页查询，传入分页对象和分页查询条件
        setmealService.page(pageInfo,queryWrapper);


        //但是套餐分类名称这一列没有展示，因为setmeal里没有这个属性，它只有套餐ID
        //所以就需要使用SetmealDto，这个类型里面有扩展的套餐分类名称的属性。
        //我们就可以通过需要套餐ID获取套餐分类名称。

        //构造SetmealDto类型的分页构造器
        Page<SetmealDto> pageDtoInfo=new Page<>();

        //对象拷贝。将pageinfo的分页结果数据先拷贝到pageDtoInfo。（原，新，排除）
        //除了records,其他的都可以拷贝过去。因为records里是所有套餐的数据，每个record都是一个setmeal对象
        //需要额外处理更改成SetmealDto对象，把套餐分类名称加进去
        BeanUtils.copyProperties(pageInfo,pageDtoInfo,"records");

        //获取原有的records的值
        List<Setmeal> records=pageInfo.getRecords();

        //遍历records，获取套餐数据
        List<SetmealDto> list= records.stream().map((item)->{

            //新建SetmealDto对象
            SetmealDto setmealDto=new SetmealDto();

            //将遍历后的每一个套餐数据复制给SetmealDto对象
            //item里没有categoryname，但有CategoryId，就可以通过它获取categoryname
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

            //将所有setmealDto对象收起来并转成集合类型，最终赋值给‘List<SetmealDto> list’
        }).collect(Collectors.toList());

        //给pageDtoInfo分页构造器设置新的records值。
        pageDtoInfo.setRecords(list);

        //返回分页对象，以显示到界面
        return R.success(pageDtoInfo);
    }


    /**
     删除套餐，
     单个和批量删除的请求地址是一样的，就是id个数不同，所以形参是list集合.需要添加注解RequestParam
     删除套餐涉及两个表，套餐表和套餐餐品表，所以不能是能默认的删除方法。

     */

    @DeleteMapping
    public R<String> deleteByIds(@RequestParam List<Long> ids) {

        log.info("要删除的套餐id为：{}",ids);
        //自定义一个删除方法。
        setmealService.removeWithDish(ids);

        return R.success("删除成功");
    }


    /**
     //起售和停售
     //通过数组保存ids，批量起售停售都能生效
     请求参数有两个，一个是状态码，非键值对形式，所以使用注解解析。第二个是ids,用list集合封装。
     */

    @PostMapping("/status/{status}")
    //设置allEntries为true，清空缓存名称为setmealCache的所有缓存
    @CacheEvict(value = "setmealCache", allEntries = true)
    public R<String> status(@PathVariable String status, @RequestParam List<Long> ids) {

        //创建一个更新条件构造器
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
        //条件1：套餐id中在请求参数ids的范围内的
        updateWrapper.in(Setmeal::getId, ids);
        //条件2：设置状态
        updateWrapper.set(Setmeal::getStatus, status);
        // 调用更新方法，更新状态
        setmealService.update(updateWrapper);
        return R.success("批量操作成功");
    }

    /**
     //修改套餐
     //根据id查询当前套餐信息，用于回显数据到修改界面
     */

    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id){

        //因为操作两张表，所以要自定义查询的方法。
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);

        return R.success(setmealDto);
    }

    /**
     * 保存修改的套餐信息，因为请求提交json数据包含套餐和套餐餐品的信息，所以使用setmealDto接受
     */
    @PutMapping()
    //设置allEntries为true，清空缓存名称为setmealCache的所有缓存
    @CacheEvict(value = "setmealCache", allEntries = true)
    public R<String> update(@RequestBody SetmealDto setmealDto){

        //自定义保存方法
        setmealService.updateWithDish(setmealDto);

        return R.success("修改成功");
    }

    /**
     展示套餐分类中的套餐。【用户下单时，需要选择套餐，然后在右侧显示】
     点击套餐标签，后在右侧显示具体的餐品
     请求参数有套餐分类的categoryID和status。是键值对的方式，不是json格式，可以直接封装到setmeal对象中。
     setmeal中都有这些属性。
     返回值是一个套餐分类的多个套餐，所以是list集合，里面的元素就是套餐
     */

    @GetMapping("/list")
    //根据categoryId和status生成动态key,将返回值存入缓存
    @Cacheable(value = "setmealCache", key = "#setmeal.categoryId + '_' + #setmeal.status")
    public R<List<Setmeal>> list(Setmeal setmeal){

        //条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        //条件1：匹配数据库中的套餐里的套餐分类id 与 请求参数中的套餐里的套餐分类ID
        queryWrapper.eq(setmeal.getCategoryId()!=null,Setmeal::getCategoryId,setmeal.getCategoryId());
        //条件2：匹配数据库中的套餐的状态 与 请求参数中的套餐的状态
        queryWrapper.eq(setmeal.getStatus()!=null,Setmeal::getStatus,setmeal.getStatus());
        //条件3：排序
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        //执行条件，匹配到所有起售状态的，与请求的套餐分类相关的所有套餐
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
