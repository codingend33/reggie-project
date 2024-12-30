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
@RestController // @Controller + @ResponseBody，应用在类上的注解，将返回值转换为json格式相应给浏览器
@RequestMapping("/category") //将 请求路径 和 控制器类 绑定，所有方法都继承这个路径的前缀。
public class CategoryController {

    //注入服务层对象
    @Autowired
    private CategoryService categoryService;

    //新增菜品分类和新增套餐分类 请求的服务端地址和提交的json数据结构相同，所用公用一个方法。
    //根据前端请求发送的json数据使用RequestBody注解解析并封装为一个实体类对象category
    //添加成功后，封装成一个统一响应格式的对象。
    ///因为前端只需要一个状态码，success传入的参数只是string。所以返回类型是string类型的R对象即可.
    // 然后ResponseBody注解会转换为json返回给浏览器
    @PostMapping //映射路径，根据请求路径和类型调用这个方法
    public R<String> save(@RequestBody Category category){
        log.info("category:{}",category);

        //调用服务层进行逻辑处理，然后保存到数据库。
        categoryService.save(category);
        return R.success("新增分类功能");
    }


    /**
     * 分类页面分页查询
     * 创建的MyBatis Plus的分页插件会拦截分页查询。自动将 SQL 转换为带分页条件的形式。
     * 因为是get请求，所以使用getmapping注解
     * page、pageSize参数是请求连接携带的，键值对的形式，所以形参中的命名保持一致，就能完成绑定。
     * 因为分页结果是需要获取页面相关的records和total数据，然后返回给浏览器，而分类实体没有这些属性。
     * 所以这里的泛型使用MP提供的Page类型,其中就包含很多关于页面的数据。返回值就是page类型的R对象。
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize){

        //1.使用分页构造器创建一个分页对象。
        //因为是基于MP提供的分页插件实现分页查询，也就是创建page对象
        //将page和pagesize这些信息传递进去，构建一个分页对象。
        Page<Category> pageInfo=new Page<>(page,pageSize);


        //2.构造条件构造器
        //LambdaQueryWrapper<>， MP中的方法。封装一个查询对象。用于动态查询条件，
        //条件查询是sort，是category的属性，所以泛型是Category
        LambdaQueryWrapper<Category> queryWrapper=new LambdaQueryWrapper<>();

        //添加排序条件，根据sort进行排序
        //Category::getSort方法引用，类名：：实例方法，
        //当方法被调用时，Category::getSort会作用在某个 Category实例上，获取对应的sort属性值。
        queryWrapper.orderByAsc(Category::getSort);

        //3.进行分页查询
        //执行查询，传递两个参数，一个分页查询对象，一个分页查询条件对象
        //不用返回值，因为MP会在内部对分页结果封装，并赋值到pageInfo对象中对应的属性上。比如records和total数据这些。
        categoryService.page(pageInfo,queryWrapper);

        //对pageInfo分页对象进行统一查询结果处理，响应给浏览器以展示结果.
        return R.success(pageInfo);

    }

    /**
     *删除分类
     *因为删除传入的参数就是一个简单的字符串。前端也只需要一个状态码处理相关逻辑。所以返回类型是string类型的R对象，
     * id是通过url传递的，是键值对的形式，所以形参名字与请求参数保持一致就能绑定。
     */
    @DeleteMapping
    public R<String> delete(Long id){
        log.info("删除分类，id为{}",id);

        //原来使用的方法是MP内置的方法，根据ID删除：categoryService.removeById(id);
        //但是并没有检查删除的分类是否关联了菜品或者套餐，所以我们需要进行功能完善。

        //代码完善之后，在服务层自定义一个删除方法。
        categoryService.remove(id);

        return R.success("分类信息删除成功");
    }

    /**
     * 修改分类
     * 前端发送的请求是一个json数据，有分类id，name,sort，那么我们根据id就能完成对应的分类修改
     * 因为是json数据，所以使用RequestBody注解封装为一个分类对象。
     * 前端只需要一个状态码，并不需要额外的数据进行操作，success方法的参数也只是一个字符串，所以返回值类型就是string类型的R对象。
     * 对于更新的操作，我们已经设定的公共属性自动注入，所以这里不用额外设定更新时间和修改人属性了。
     */
    @PutMapping // 根据请求类型，绑定这个方法
    public R<String> update(@RequestBody Category category){
        log.info("修改分类信息为：{}", category);
        //调用MP封装的updateById方法就能实现。
        categoryService.updateById(category);

        return R.success("修改分类信息成功");
    }

    /**
     * 根据type获取对应分类的列表数据。【下拉框中显示，和用户下单时在左侧显示分类】
     * 在前端有两个页面会发送这个请求，添加餐品页面和添加套餐页面，
     * 在相应的页面，有下拉框要显示对应的分类数据。所以前端请求中会有固定的值（餐品分类1和套餐分类2），以区分是哪种分类，
     * 让对应的数据显示在相应页面的下拉框中。
     * 前端发送get请求，路径是category/list。
     * 因为返回值是一个分类的列表，里面的元素是分类List<Category>，所以返回值的泛型是嵌套的
     * url中携带了type参数，可以string type的形式接收参数。
     * 也可以写Category类，因为type是Category的一个属性，这样传递的参数spring会自动封装到category实体对象中。
     * 这个功能在后面添加套餐时也会用到
     */

    @GetMapping("/list")//路径和方法绑定
    public R<List<Category>> list(Category category){

        //条件构造器创建条件对象
        LambdaQueryWrapper<Category> categoryLambdaQueryWrapper = new LambdaQueryWrapper<>();

        //添加匹配条件
        //这个eq可以有三个参数，首先判断请求参数存储是否有type，是布尔值，如果有才会进行比较。
        //从数据库中Category类的对象中获取getType值，然后与请求参数的type值比较。（type为1是菜品，type为2是套餐）
        categoryLambdaQueryWrapper.eq(category.getType()!= null,Category::getType,category.getType());

        //添加排序条件。
        //Category类的对象中有排序的属性，所以根据它的排序属性作为条件，再加上第二个以更新时间排序的条件
        categoryLambdaQueryWrapper.orderByAsc(Category::getSort).orderByAsc(Category::getUpdateTime);

        //调用服务层方法将 条件对象传进去获取匹配成功的值，返回的是一个列表
        List<Category> list = categoryService.list(categoryLambdaQueryWrapper);

        //将列表封装为统一格式
        return R.success(list);

    }
}
