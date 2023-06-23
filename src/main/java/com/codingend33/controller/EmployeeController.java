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

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RequestMapping("/employee")
@RestController
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee){

        //1、将页面提交的密码进行md5加密处理
        // 通过对象获取到密码
        String password = employee.getPassword();
        // 调用 DigestUtils方法给密码加密
        password = DigestUtils.md5DigestAsHex(password.getBytes());


        //2、根据页面提交的用户名来查数据库
        // LambdaQueryWrapper<>， MP中的方法。封装一个查询对象
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，设定查询条件。要求数据库中的用户名与请求中用户名一致。
        queryWrapper.eq(Employee::getUsername,employee.getUsername());
        // 通过服务层的employeeService对象，根据查询条件查询数据库。因为用户名是唯一的，所以使用getOne即可。
        // 将获取到的用户信息封装到员工对象中
        Employee emp = employeeService.getOne(queryWrapper);

        //3、如果没有查询到则返回失败结果
        if(emp == null){
            return R.error("登录失败");
        }


        //4、比对密码，如果不一致则返回失败结果
        if(!emp.getPassword().equals(password)){
            return R.error("密码错误");
        }


        //5、查看员工状态，如果已禁用状态，则返回员工已禁用结果
        // 数据库中的状态字段属性是0，说明员工已禁用。
        if(emp.getStatus() ==0){
            return R.error("账户已禁用");
        }

        //6、登录成功，将用户id存入Session并返回成功结果
        // 登录成功了，就通过request对象，获取session，将员工ID存放进去
        request.getSession().setAttribute("employee",emp.getId());
        return R.success(emp);

    }

    // 员工退出

    // 因为退出返回的信息就是字符串，所以泛型就是string
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        //清理Session中保存的当前员工ID
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }


    @PostMapping
    public R<String> save(HttpServletRequest request,@RequestBody Employee employee){
        log.info("新增员工信息{}",employee.toString());

        //设置默认密码为123456，并采用MD5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        //设置createTime和updateTime,使用了公共字段填充，所以全部都替换掉了。
        //employee.setCreateTime(LocalDateTime.now());
        //employee.setUpdateTime(LocalDateTime.now());
        //根据session来获取创建人的id，因为当前登录的人是创建人（管理人）。
        //Long empId = (Long) request.getSession().getAttribute("employee");
        //并设置创建和更新人，都是管理人
        //employee.setCreateUser(empId);
        //employee.setUpdateUser(empId);

        //调用服务层方法，存入数据库
        employeeService.save(employee);
        //返回统一结果
        return R.success("添加员工成功");
    }

    @GetMapping ("/page")
    public R<Page> page(int page,int pageSize,String name){
        // 因为分页结果是需要获取数据的，所以这里的泛型使用MP提供的Page,其中就包含很多关于页面的数据。
        // 前端传递的参数有 当前页 每页展示数 和查询name的条件
        log.info("page={},pageSize={},name={}",page,pageSize,name);

        //创建分页构造器。因为是基于MP提供的分页插件实现分页查询，也就是创建page对象
        //page对象中封装了page和pagesize属性，所以需要将这些信息传递进去
        Page pageInfo = new Page(page,pageSize);

        //构造条件构造器。用于动态查询条件，也就是左上角根据姓名查询
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // 当搜索框里有值的时候，就会根据输入的模糊name与数据库的name进行条件查询
        // 当没有输入name时，那模糊条件查询就不用了，相当于展示所有分页查询的结果。
        queryWrapper.like(!StringUtils.isEmpty(name),Employee::getName,name);
        //添加排序条件,根据更新时间
        queryWrapper.orderByDesc(Employee::getUpdateTime);
        //执行查询
        employeeService.page(pageInfo,queryWrapper);

        return R.success(pageInfo);

    }

    // 此方法是一个通用的方法，完成状态的更新和其他信息的更新
    @PutMapping
    public R<String> update(@RequestBody Employee employee,HttpServletRequest request) {
        log.info(employee.toString());
/*        //获取线程ID 验证属于同一个线程
        long id = Thread.currentThread().getId() ;
        log.info("线程id:{}" ,id);*/

        // 通过request域获取当前登陆者的ID，强转为long类型.使用了公共字段填充，所以全部都替换掉了。
        //Long id = (Long) request.getSession().getAttribute("employee");
        // 设置更新人的ID，就是当前登陆者的ID
        //employee.setUpdateUser(id);
        // 设置更新时间
        //employee.setUpdateTime(LocalDateTime.now());
        // 调用服务层根据ID更新信息
        // 发送请求携带的信息以及上面设置的信息都已经封装到employee对象了，作为参数传递进去。
        employeeService.updateById(employee);
        return R.success("员工信息修改成功");

    }
    //根据id查询员工信息
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable String id){
        log.info("根据id查对象");
        Employee emp = employeeService.getById(id);
        if(emp!=null){
            return R.success(emp);
        }
        return R.error("没有查询到该用户信息");
    }



}
