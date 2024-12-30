package com.codingend33.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codingend33.common.CustomException;
import com.codingend33.common.R;
import com.codingend33.dto.DishDto;
import com.codingend33.dto.SetmealDto;
import com.codingend33.entity.Category;
import com.codingend33.entity.Dish;
import com.codingend33.entity.DishFlavor;
import com.codingend33.service.CategoryService;
import com.codingend33.service.DishFlavorService;
import com.codingend33.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController // controller + responsebody
@RequestMapping("/dish") //路径和方法绑定
@Slf4j
public class DishController {

    //注入dishService bean对象，处理餐品的逻辑
    @Autowired
    private DishService dishService;

    //注入DishFlavorService bean对象，处理餐品口味的逻辑
    @Autowired
    private DishFlavorService dishFlavorService;

    //注入CategoryService bean对象，处理分类的逻辑
    @Autowired
    private CategoryService categoryService;

    //注入RedisTemplate bean对象，处理  的逻辑
    @Autowired
    private RedisTemplate redisTemplate;




    /**
     *新增餐品
     * 最后success传入的参数就是字符串，所以返回值是string类型的R对象，
     * 这些请求参数涉及两个表，单一的实体对象无法接受完整的参数，所以需要一个新的数据传输类DishDto。
     * 它集成了所有dish属性，并扩展了flavor属性，能够接受请求参数。
     * 因为请求参数是json格式的，所以使用RequestBody注解将参数封装为一个实例对象dishDto，类型是DishDto。
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {

        // 查看数据是否封装正确
        log.info("接收到的数据为：{}",dishDto);

        //调用dish的服务层中的方法，
        dishService.saveWithFlavor(dishDto);

        //清理对应分类的缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }


    /**
     * 餐品的分页查询：根据name查询菜品
     * 特殊点：
     *   图片列：会用到文件的下载功能以显示图片
     *   菜品分类列：由于我们的菜品表dish只保存了category_id，需要从category表中获取category_id对应的菜品分类名称，从而回显数据。
     *
     * 分页查询的返回值类型使用MP的一个封装类Page.
     * page、pageSize和name是url携带的参数，所以形参的名字与其保持一致就能完成绑定
     *
     */
    @GetMapping("/page") // 路径和方法绑定。
    public R<Page> page(int page,int pageSize,String name){

        //构造分页构造器对象.使用MP的内置对象Page
        Page<Dish> pageInfo = new Page<>(page,pageSize);

        //根据name查询餐品
        //条件构造器对象
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加name搜索条件，使用like模糊查询，第一个参数是布尔值，参数name不为空时才会执行
        queryWrapper.like(name!=null,Dish::getName,name);
        //根据更新时间降序排列
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行分页查询，传入分页对象和分页查询条件
        dishService.page(pageInfo,queryWrapper);

        //根据dish名字查询出来的结果是一个Dish类型的page对象，分页数据里有分页查询的结果，以及record记录的所有餐品数据，但是存在问题：
        //1.有些图片无法显示，因为还没有传到本地文件夹中。所以把资料中提供好的图片复制到我们存放图片的目录下即可。
        //2.菜品分类名称是空的，因为Dish对象中存的是分类id，无法匹配上。所以需要通过id在分类表中获取分类名称。

        //DishDto类中的另外一个属性（categoryName）就派上用场了，可以返回一个DishDto类型的page对象就有菜品分类名称数据了。
        //所以将当前已经获取的dish类型的page对象的数据都复制给DishDto对象，然后通过category_id去找到category_name。

        //创建一个DishDto类型的分页构造器对象，这个是最终加工过的分页对象，存放的是全部正确的数据。
        Page<DishDto> dishDtoPage = new Page<>(page, pageSize);

        //将Dish获取到的分页查询的数据pageInfo复制给dishDtoPage（这个复制的是分页查询结果相关的数据，如当前页，多少条数据等）
        //其中records里封装的口是在页面展示的所有餐品数据，先拷贝除了records以外的数据，因为records需要额外处理。
        //record中只有id，所以要进一步将id更换为名称。
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        //获取原records数据，它是一个Dish类型的list集合。
        //record里是所有餐品数据，其中就有我们需要的category_id，需要通过这个id去获取category_name。
        List<Dish> records = pageInfo.getRecords();

        //使用map逐一处理records中每一条数据，然后返回新的DishDto对象。最后将所有DishDto对象封装成一个list集合，
        List<DishDto> list = records.stream().map((item) -> {

            //创建DishDto对象
            DishDto dishDto = new DishDto();

            //将遍历获得的数据复制给dishDto对象。此时只有category_id，还没有categoryName
            BeanUtils.copyProperties(item, dishDto);

            //然后获取一下dish对象的category_id属性（item中有getCategoryId）
            Long categoryId = item.getCategoryId();  //分类id

            //用@Autowired注入一个CategoryService对象，根据id获取分类对象
            Category category = categoryService.getById(categoryId);

            // 防止category为空时进行get和set操作报错。
            if(category!=null){
                //获取Category对象的name属性，也就是菜品分类名称
                String categoryName = category.getName();

                //最后将菜品分类名称赋给dishDto对象中对应的属性
                dishDto.setCategoryName(categoryName);
            }

            //返回一个dishDto对象，此时对象中就有categoryName属性值了
            return dishDto;

            //并将所有处理过的dishDto对象封装成一个集合，作为我们的最终结果
        }).collect(Collectors.toList());

        //将处理好的list集合作为records赋值给dishDtoPage分页对象，此时分页查询结果中就有了所有信息。
        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);

    }


    /**
     * 根据菜品ID获取菜品信息和口味信息
     * 这个方法用于修改菜品，因为修改菜品页面有菜品信息和口味信息，所以需要同时操作两个表的属性。
     * 我们继续使用dishDto类，因为它继承了dish的所有属性,也有口味属性，所以返回值类型是DishDto
     * 因为id是url携带的，而且不是键值对的形式，所以只能用{}占位符，还需要PathVariable注解解析参数
     */

    @GetMapping("/{id}")
    public R<DishDto> getByIdWithFlavor(@PathVariable Long id) {

        //因为没有内置方法可以同时处理两个表的数据，所以需要在服务层接口中自定义方法并在实现类中实现
        //这里调用这个方法，将id传进去，获取的是dishDto类型的对象。
        DishDto dishDto = dishService.getByIdWithFlavor(id);

        log.info("查询到的数据为：{}", dishDto);

        return R.success(dishDto);
    }

    /**
     * 餐品数据修改更新
     * 因为整个修改界面有多个请求，上一个是数据的回显请求，返回的是一个DishDto对象，
     * 所以提交保存时的请求中携带的数据是DishDto类型的，我们封装也需要疯转成这个类型，否则无法接受。
     */
    @PutMapping // 注意是put类型
    public R<String> update(@RequestBody DishDto dishDto) {

        log.info("接收到的数据为：{}", dishDto);

        //dishDto类型的数据无法使用内置方法，所以使用自定义方法。
        dishService.updateWithFlavor(dishDto);

        //清理对应分类的缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("修改菜品成功");
    }


    /**
     * 批量停售/起售菜品
     * status：请求路径中的状态值（如 0 表示停售，1 表示起售）。
     * ids：前端通过请求体传递的菜品 ID 列表。对于列表参数我们选择将其封装为一个集合，就需要@RequestParam注解进行绑定。
     * 如果没有注解@RequestParam，会报错。因为spring默认将List看做是一个POJO对象来处理，创建一个对象并准备把前端的数据封装到对象中，
     * 但是List是一个接口无法创建对象，所以报错。
     */

    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable Integer status, @RequestParam List<Long> ids) {

        log.info("status:{},ids:{}", status, ids);

        //LambdaUpdateWrapper：建立更新条件对象，可以使用set方法
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();

        //in 方法：指定要更新的菜品 ID 列表。
        //判断是否提供了 ids 参数，如果 ids 不为 null，才执行这个条件，避免空指针异常。
        //Dish::getId: 表示使用 Dish 实体类的 id 字段作为查询条件。ids是需要操作的菜品的 id。
        //也就是在Dish 实体类的 id字段中，指定ids中的id作为更新范围
        updateWrapper.in(ids != null, Dish::getId, ids);
        //将 Dish 表中菜品的状态字段（status）更新为请求中的状态值。
        //因为上面的in已经设定了范围，所以就是在既定范围内将这个id的dish的状态进行更新
        updateWrapper.set(Dish::getStatus, status);

        //清理缓存
        //构造条件查询对象
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //设定条件。匹配数据库中的id与发送请求的ids。
        lambdaQueryWrapper.in(Dish::getId, ids);
        //根据ID获取菜品
        List<Dish> dishes = dishService.list(lambdaQueryWrapper);
        //通过菜品获取分类ID，并动态生成key。
        for (Dish dish : dishes) {
            String key = "dish_" + dish.getCategoryId() + "_1";
            redisTemplate.delete(key);
        }

        //批量更新数据库中符合条件的菜品状态。
        dishService.update(updateWrapper);

        return R.success("批量操作成功");
    }



    /**
     * 批量删除菜品
     * 从前端接收一组菜品的 ID 列表。
     * ids：前端通过请求体传递的菜品 ID 列表。对于列表参数我们选择将其封装为一个集合，就需要@RequestParam注解进行绑定。
     * 如果没有注解@RequestParam，会报错。因为spring默认将List看做是一个POJO对象来处理，创建一个对象并准备把前端的数据封装到对象中，
     * 但是List是一个接口无法创建对象，所以报错。
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {

        log.info("删除的ids：{}", ids);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        //in 方法：筛选出指定 ID 的菜品，也就是ids中的菜品
        queryWrapper.in(Dish::getId, ids);

        //eq 方法：根据上面指定的范围内，筛选状态为 1（启售）的菜品。
        queryWrapper.eq(Dish::getStatus, 1);

        //如果待删除的ids中有启售状态的菜品，则抛出异常并返回错误信息
        long count = dishService.count(queryWrapper);
        if (count > 0) {
            throw new CustomException("删除列表中存在启售状态商品，无法删除");
        }

        //如果没有待删除的ids中起售的，就批量删除指定的菜品。
        dishService.removeByIds(ids);

        return R.success("删除成功");
    }


    //添加套餐功能中，添加菜品的页面，实现展示餐品。
    //请求连接是dish/list?categoryId=xxx，
    //根据categoryId查询对应的餐品，因为查询的是餐品且dish对象中有CategoryId属性，
    //所以用Dish类型对象接收参数，通用性更好
    //因为查出的菜品是多个，所以是list集合嵌套着菜品
    //这个功能后面有修改

   /* @GetMapping("/list")
    public R<List<Dish>> get(Dish dish){

        //条件查询器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        //条件1：根据传进来的categoryId 与 Dish对象中的CategoryId匹配
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());

        //条件2：只查询状态为1的菜品（启售菜品）
        queryWrapper.eq(Dish::getStatus,1);

        //条件3：简单排下序，其实也没啥太大作用
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        //获取查询到的结果作为返回值
        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }*/


    /**
     *菜品展示功能，对之前的代码进行了修改。
     *请求连接是dish/list?categoryId=xxx，
     *根据categoryId查询对应的餐品，因为查询的是餐品且dish对象中有CategoryId属性，
     *所以用Dish类型对象接收参数，通用性更好
     *因为查出的菜品是多个，所以是list集合嵌套着菜品
     *选择规格按钮，是根据服务端返回数据中是否有flavors字段来决定的，但我们返回的是一个List<Dish>，其中并没有flavors属性，
     *将方法返回值改为DishDto，DishDto继承了Dish，且扩展了flavors属性和分类名称的属性
     *
     */

    @GetMapping("/list")
    public R<List<DishDto>> get(Dish dish){

        //提前声明变量，DishDto类型的餐品集合
        List<DishDto> dishDtoList;
        //动态构造Key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();

        //先从redis中获取缓存数据.
        //一个分类下有多个菜品，每个key代表一个分类，也就是一份缓存数据。
        //而且查询的请求是CategoryId和Status参数构成，所以我们动态拼接一下就生成了这key。
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        //如果存在，则直接返回，无需查询数据库
         if (dishDtoList != null){
             return R.success(dishDtoList);
         }

        //如果缓存中不存在相应数据，则查询数据库，继续按照原来的代码执行。
        //条件查询器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        //条件1：根据传进来的categoryId 与 Dish对象中的CategoryId匹配
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());

        //条件2：只查询状态为1的菜品（启售菜品）
        queryWrapper.eq(Dish::getStatus,1);

        //条件3：简单排下序，其实也没啥太大作用
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        //获取到匹配的餐品
        List<Dish> list = dishService.list(queryWrapper);

        //以下是新增的代码(类似上面的page分页查询，因为缺少口味选择，需要使用中间表dishDto)

        //item就是list中的每一条数据，相当于遍历了
        //List<DishDto> dishDtoList = list.stream().map((item) -> {
        dishDtoList = list.stream().map((item) -> {

            //创建一个dishDto对象
            DishDto dishDto = new DishDto();

            //将item的属性全都copy到dishDto里
            BeanUtils.copyProperties(item, dishDto);

            //由于dish表中没有categoryName属性，只存了categoryId
            Long categoryId = item.getCategoryId();
            //所以我们要根据categoryId查询对应的category名称
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                //然后取出categoryName，赋值给dishDto
                dishDto.setCategoryName(category.getName());
            }

            //然后获取一下菜品id，根据菜品id去dishFlavor表中查询对应的口味，并赋值给dishDto
            Long itemId = item.getId();

            //条件构造器
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            //条件1：匹配数据库中的菜品ID与当前分类的餐品ID
            lambdaQueryWrapper.eq(itemId != null, DishFlavor::getDishId, itemId);

            //根据菜品id，查询到菜品对应的口味
            List<DishFlavor> flavors = dishFlavorService.list(lambdaQueryWrapper);
            //将口味赋给dishDto的flavors属性
            dishDto.setFlavors(flavors);

            //将dishDto作为结果返回
            return dishDto;

            //将所有返回结果收集起来，封装成List
            }).collect(Collectors.toList());

        //最后将查询到的菜品数据添加到缓存中
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        //此时的餐品对象，有分类名称，也有相应的口味信息。
        return R.success(dishDtoList);
    }
}
