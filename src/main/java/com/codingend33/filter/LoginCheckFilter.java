package com.codingend33.filter;

import com.alibaba.fastjson2.JSON;
import com.codingend33.common.BaseContext;
import com.codingend33.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.util.AntPathMatcher;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.String;

/*
* 检查用户是否已经登录成功
* 注意：在启动类上加入注解@ServletComponentScan：使过滤器生效。
* */

@Slf4j //提供日志记录和输出功能。
// 配置过过滤器，定义过滤器名称和定义拦截范围 urlPatterns = "/*" 表示拦截所有请求。
@WebFilter(filterName = "LoginCheckFilter",urlPatterns = "/*")
// 过滤器要实现一个filter接口,注意选择servlet的。
public class LoginCheckFilter implements Filter {

    // 静态页面资源不需要过滤，通常需要过滤的都是需要控制层匹配的路径，这些都是要调用数据层的，所以要过滤处理。
    //定义不需要处理的请求路径，这些都是排除掉的。
    String[] urls = new String[]{
            "/employee/login",  //登录路径，本来就是要登录，所以不用过滤
            "/employee/logout", //登出路径，要退出也不用过滤
            "/backend/**",      //都是静态资源，都是允许用户看页面，不用过滤
            "/front/**",       //都是静态资源，都是允许用户看页面，不用过滤
            "/common/**",      //文件上传和下载不需要过滤
            //对用户登陆操作放行
            "/user/login",
            "/user/sendMsg",

            "/doc.html",
            "/webjars/**",
            "/swagger-resources",
            "/v2/api-docs"
    };

    //这个路径匹配器，支持通配符匹配。
    //因为在处理那些不需要处理的请求路径中有通配符**，按照字符串不好解析。
    //所以使用这个匹配器匹配通配符**，就代表了文件夹下的所有路径
    //这样才能让/backend/index.html 与 /backend/**匹配上。
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    //创建一个check方法，用于路径匹配，检查本次请求是否需要放行
    //不需要处理的路径urls和获取的请求路径requestURI进行匹配，所以形参是这两个参数
    public boolean check(String[] urls,String requestURI){

        //将urls中的路径逐一遍历出来。
        for(String url :urls){
            // 使用路径匹配器，将url与请求路径匹配，获取一个布尔值
            boolean match = PATH_MATCHER.match(url, requestURI);
            // 匹配上就返回true，就是能放行
            if(match == true){
                return true;
            }
        }
        // 没有匹配上就返回false
        return false;
    }

    /*  日志输出时，原本是后面直接用+string。现在使用{}占位符。逗号后面是需要输出的信息。
    比如： 将request.getRequestURI())输出在日志中。
        log.info("拦截到请求：{}",request.getRequestURI());*/


    // 重写这个方法，这是过滤器的核心方法。每个请求都会触发此方法，进行过滤逻辑处理
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        // 当前的request和response都是Servlet类型，但我们需要的是HttpServlet类型，所以要先强转。
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //1.获取用户本次请求的URI（就是本次请求的路径）
        //URL是URI的一个子集，也就是URL是URI的一种，当前流行都是使用URL作为资源标识（地址），所以URI都是以URL的形式存在。
        String requestURI = request.getRequestURI();
        log.info("拦截到请求：{}",requestURI);

        //	2.判断本次请求是否需要处理
        // 调用自定义的check方法，将urls和requestURL传递进去进行匹配。
        boolean check = check(urls, requestURI);

        //	3.如果请求的链接在不需要处理的数组中，则直接放行
        if(check){
            log.info("本次请求{}不需要处理",requestURI);
            //放行方法,允许访问资源，否则数据不会展示
            filterChain.doFilter(request,response);
            //因为都放行了，后面代码无需执行，直接return
            return;
        }

        //	4.判断登录状态，如果已登录，则直接放行
        // 4-1、判断后台人员是否登录
        // 登录的时候session中存放了employee,所以只要session不是空，就说明已经登录了，可以放行
        if(request.getSession().getAttribute("employee")!=null){
            log.info("用户已登录，用户ID为{}",request.getSession().getAttribute("employee"));
            //根据session来获取当前我们存的id值
            Long empId = (Long) request.getSession().getAttribute("employee");
            //BaseContext是自定义的一个类。调用BaseContext将id封装到线程中，用于其他方法可以从线程中获取用户id
            BaseContext.setCurrentId(empId);
            //放行,允许访问资源，否则数据不会展示
            filterChain.doFilter(request,response);
            //因为都放行了，后面代码无需执行，直接return
            return;
/*
        //获取线程ID，验证属于同一个线程
            long id = Thread.currentThread().getId() ;
            log.info("线程id:{}" ,id);
*/
        }

        //4-2 判断前台用户是否登录，如果是登录的状态，直接放行
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("user"));
            Long userId = (Long)request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request,response);
            return;
        }

        // 走到这里就说明是未登录的状态了。
        log.info("用户未登录");

        //	5.如果未登录则跳转到未登录界面,通过输出流向浏览器response一个 JSON 格式的错误响应对象。
        //  backend和front的文件中都有request.js文件中已经定义了统一响应结果的响应拦截器，这个是前端的拦截器。
        //  针对服务器响应的数据，如果msg是NOTLOGIN 并且响应代码是0，说明没有登录。
        //  然后会跳转到登录页面，所以这个MSG不能乱写，要和js中的一致，否则不会跳转
        // 在浏览器中的sources中，可以对js进行调试。
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));

    }

}
