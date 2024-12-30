package com.codingend33.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codingend33.common.R;
import com.codingend33.entity.Employee;
import com.codingend33.service.EmployeeService;
import com.codingend33.service.impl.EmployeeServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j //输出日志
@RequestMapping("/employee") //将 请求路径 和 控制器方法 绑定，所有方法都继承这个路径的前缀。
@RestController // @Controller + @ResponseBody，应用在类上的注解，将返回值转换为json格式相应给浏览器
public class EmployeeController {

    //注入service的对象，因为不能直接使用new对象，防止耦合。
    @Autowired
    private EmployeeService employeeService;

    //登录请求的参数是json格式的，所以要使用@RequestBody（应用在方法参数的注解） 注解解析后接收并封装成一个employee对象。
    //employee实体类对象中的属性要与数据库中的一致。
    //HttpServletRequest request是为了在登录成功以后，将员工ID的存放在session一份，作为一个登录成功的标记。
    //以后要获取当前登录用户时，直接用request对象get即可。
    //因为前端需要这个对象的数据，解析到并保存到了浏览器的localstorage中。所以返回值是employee类型的R对象，
    //@ResponseBody，最后将返回值转换为json格式相应给浏览器
    @PostMapping("/login")// 映射路径，只要请求路径中有login,就会调用这个方法
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee){

        //1、获取提交的密码并加密处理
        // 因为用户提交的数据已经封装为一个employee对象了，数据与对象的属性已经完成映射
        // 就可以通过对象的方法获取到密码
        String password = employee.getPassword();
        // 调用 DigestUtils方法将用户提交的密码进行md5加密处理
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2、根据页面提交的用户名来查数据库
        // LambdaQueryWrapper<>， MP中的方法。封装一个查询对象
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();

        // 使用查询对象，调用eq方法，设定查询条件。要求数据库中的用户名与请求中用户名一致。
        // 这个方法有两个参数，（数据库表的字段，要匹配的值）
        // Employee::getUsername通过反射在数据库中获取 getUsername 方法对应的字段 username，
        // 也可以直接使用字段名username但是不推荐，因为是直接写死了，不够灵活。
        // employee.getUsername()就是请求参数封装的对象调用相关方法获取用户名
        queryWrapper.eq(Employee::getUsername,employee.getUsername());

        // 通过服务层的employeeService对象，根据定义的查询条件查询数据库。因为用户名是唯一的，所以使用getOne即可。
        // getOne是Service 层方法，它用于根据查询条件从数据库中获取单个记录。getOne的参数是一个包装对象，也就是查询条件。
        // 最后将获取到的用户信息封装成一个员工对象，因为是getone，如果没有查询到记录，返回 null。如果查询到多条记录，会抛出异常（默认行为）。
        Employee emp = employeeService.getOne(queryWrapper);

        //3、如果没有查询到则返回失败结果
        // 因为返回值是R类型的对象。所以调用R的error方法得到的是一个封装了统一格式数据的R对象并返回。
        if(emp == null){
            return R.error("登录失败");
        }

        //4、比对密码，如果不一致则返回失败结果
        // 因为getone返回的是一个employee对象，所以调用成员方法获取密码（是数据库中的），与用户调教的密码比较。
        if(!emp.getPassword().equals(password)){
            return R.error("密码错误");
        }

        //5、查看员工状态，如果已禁用状态，则返回员工已禁用结果
        // 数据库中的状态字段属性是0，说明员工已禁用。
        if(emp.getStatus() ==0){
            return R.error("账户已禁用");
        }

        //6、登录成功，将用户id存入Session并返回成功结果
        // 登录成功了，就通过request对象，获取session，将员工ID存放进去（键值对的形式）
        // 浏览器中看不到 Session 数据，应为是存储在服务端的。
        request.getSession().setAttribute("employee",emp.getId());

        //登录成功返回的也是一个Employee类型的对象，所以还是调用R类的success方法获取一个统一格式数据的R对象并返回。
        //参数是我们上面在数据库中查询到的emp对象。如果使用管理员登录项目的后台，那么在登录成功后，这个对象会被响应给浏览器
        //vue中的逻辑处理是将这个对象解析到并保存到了浏览器的localstorage中，此时在浏览器中就能查询到这个对象的信息。
        //对于登录不成功的几种情况返回的对象，在vue中的逻辑处理是直接将错误信息展示在页面的弹出框中。

        //在发送请求后，浏览器的response中都能查看服务器响应回来的数据对象，也就是R对象。
        return R.success(emp);
    }

    // 员工退出
    // 因为退出传入的信息就是字符串，前端也没有需要特殊的数据类型进行其他的处理，所以返回的是string类型的R对象。
    // 因为不用接收用户提交的数据，所以也不用requestbody去解析数据和封装对象了。
    // 但是退出需要清空session，所以还需要HttpServletRequest的对象
    @PostMapping("/logout") //映射路径，只要请求路径中有logout,就会调用这个方法
    public R<String> logout(HttpServletRequest request){
        //清理Session中保存的当前员工ID，因为属性是键值对的形式，所以删除key即可。
        request.getSession().removeAttribute("employee");
        //因为logout方法的返回值类型是string，而且退出时不需要传递对象数据，只需要一个字符串。
        //而R的success方法是泛型方法，所以可以接受字符串。调用后得到的是一个字符串类型的R对象并返回。
        //在vue中设定了逻辑，退出后会将存储在浏览器localstorage中的用户数据清除掉。然后跳转到登录页面。
        return R.success("退出成功");
    }

    // 新增员工
    // 返回值类型就是string类型的R对象，因为响应给浏览器时只需要状态码，前端只根据状态码进行逻辑处理
    // 前端提交的是新员工的json数据，所以需要requestbody解析和封装为一个对象。
    // postmapping不需要写路径了，因为请求路径没有二级路径
    @PostMapping //保存没有二级路径，所以根据拦截的请求和请求类型，就会调用这个方法
    public R<String> save(HttpServletRequest request,@RequestBody Employee employee){
        //日志输出新增的员工信息。
        log.info("新增员工信息{}",employee.toString());

        //设置默认密码为123456，并采用MD5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        //项目使用了 MyBatis-Plus，可以通过其字段填充功能来实现公共字段的自动设置。
        //在实体类中，对需要自动填充的字段添加注解 @TableField，并设置其填充策略。
        //使用了公共字段填充，所以原来的手动方法都取消了，直接调用service层方法即可。

        //设置createTime和updateTime,
        //employee.setCreateTime(LocalDateTime.now());
        //employee.setUpdateTime(LocalDateTime.now());
        //根据session来设置创建人的id，因为当前登录的人是创建人（管理人）。
        //Long empId = (Long) request.getSession().getAttribute("employee");
        //并设置创建和更新人，都是管理人
        //employee.setCreateUser(empId);
        //employee.setUpdateUser(empId);

        //调用服务层的save方法，存入数据库
        // 这个save方法不是自己写的，是继承MP父接口后，使用的内置方法。
        employeeService.save(employee);
        //返回统一结果对象
        return R.success("添加员工成功");
    }



    /**
     * 员工页面的分页查询
     * 创建的MyBatis Plus的分页插件会拦截分页查询。自动将 SQL 转换为带分页条件的形式。
     * 因为是get请求，所以使用getmapping注解
     * page、pageSize和name参数是请求连接携带的，键值对的形式，所以形参中的命名保持一致，就能完成绑定。
     * 因为分页结果是需要获取页面相关的records和total数据，然后返回给浏览器，而employee实体没有这些属性。
     * 所以这里的泛型使用MP提供的Page,其中就包含很多关于页面的数据。返回值是Page类型的R对象
     */
    @GetMapping ("/page") //映射路径，只要请求路径中有page,就会调用这个方法
    public R<Page> page(int page,int pageSize,String name){


        // 前端传递的参数有 当前页（默认1）、 每页展示数（默认10）和查询name的条件
        log.info("page={},pageSize={},name={}",page,pageSize,name);

        //1.使用分页构造器创建一个分页对象。
        //因为是基于MP提供的分页插件实现分页查询，也就是创建page对象
        //将page和pagesize这些信息传递进去，构建一个分页对象。
        Page pageInfo = new Page(page,pageSize);

        //2.构造条件构造器。
        //LambdaQueryWrapper<>， MP中的方法。封装一个查询对象。用于动态查询条件，实现左上角根据姓名查询。
        //因为姓名是查询条件，它是employee的属性，所以泛型使用employee
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();

        // 这里使用Like查询，模糊匹配。
        // 当搜索框里有值的时候，就会根据输入的模糊name与数据库的name进行条件查询
        // 当没有输入name时，那模糊条件查询就不用了，相当于展示所有分页查询的结果。
        //like方法可以传递三个参数，第一个是booleen判断，也就是在有name输入的情况下。第二个是数据库中的name，第三个是输入的name
        //Employee::getName是方法引用，类名：：实例方法，
        // 当方法被调用时，Employee::getUpdateTime 会作用在某个 Employee 实例上，获取对应的属性值。
        queryWrapper.like(!StringUtils.isEmpty(name),Employee::getName,name);
        //给查询对象添加排序条件,根据更新时间
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        //3.调用服务层方法，执行查询，传递两个参数，一个分页查询对象，一个分页查询条件对象
        //不用返回值，因为MP会在内部对分页结果封装，并赋值到pageInfo对象中对应的属性上。比如records和total数据这些。
        employeeService.page(pageInfo,queryWrapper);

        //最后返回pageInfo分页对象给统一查询结果。响应给浏览器以展示结果.
        return R.success(pageInfo);

    }


    /**
     * 通用方法，状态的更新 和 根据ID修改员工信息
     * 修改后只需要返回状态码给前端进行处理，所以返回值类型是string。
     * 更新提交的数据是json格式的，所以用RequestBody解析和封装为一个对象。
     *
     * 由于修改员工信息也是发送的PUT请求，和启用/禁用员工账号状态是一致的，所以公用这一个方法。
     */
    @PutMapping //映射路径，根据请求路径和类型,会调用这个方法
    public R<String> update(@RequestBody Employee employee,HttpServletRequest request) {

        log.info(employee.toString());
/*      //获取线程ID 验证属于同一个线程
        long id = Thread.currentThread().getId() ;
        log.info("线程id:{}" ,id);*/

        // 使用了公共字段填充，所以这些都取消了
        // 通过request域获取当前登陆者的ID，强转为long类型.
        //Long id = (Long) request.getSession().getAttribute("employee");
        // 设置更新人的ID，就是当前登陆者的ID
        //employee.setUpdateUser(id);
        // 设置更新时间
        //employee.setUpdateTime(LocalDateTime.now());

        // 调用服务层的方法根据ID更新信息
        // 发送请求携带的信息以及上面设置的信息都已经封装到employee对象了，作为参数传递进去。
        // 通过查看日志，我们发现更新操作并没有完成，这是怎么回事呢？查看笔记进行完善。
        employeeService.updateById(employee);

        return R.success("员工信息修改成功");
    }



    /**
     * 根据id查询员工信息
     * 将获取的员工对象以json形式响应给页面（类上的注解@ResponseBody就是实现json数据给页面回显）
     * 因为前端需要员工数据进行回显操作，所以返回值类型是Employee类。
     * ID是在url中进行传递的，非键值对的形式，是可变的，所以路径参数需要使用{},
     * 然后使用@PathVariable获取路径参数id并绑定到形参变量 string id上。
     */
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable String id){
        log.info("根据id查对象");
        //根据id，调用服务层方法，获取对应的员工信息对象
        Employee emp = employeeService.getById(id);
        //如果对象不是空的，说明获取成功，将对象封装为统一格式并返回
        if(emp!=null){
            return R.success(emp);
        }
        //没有得到对象，封装为统一格式并返回。
        return R.error("没有查询到该用户信息");
    }
}
