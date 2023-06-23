package com.codingend33.filter;

import com.alibaba.fastjson.JSON;
import com.codingend33.common.BaseContext;
import com.codingend33.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.String;

/*
* 检查用户是否已经登录成功
* */
@Slf4j
// 定义过滤器名称。过滤路径是全部
@WebFilter(filterName = "LoginCheckFilter",urlPatterns = "/*")
// 过滤器要实现一个filter接口,注意选择servlet的
public class LoginCheckFilter implements Filter {

    //路径匹配器，支持通配符。
    //因为下面那些不需要处理的请求路径中有通配符**，按照字符串不好解析。
    //所以使用匹配器让通配符**，就代表是文件夹下的所有路径
    //这样才能让/backend/index.html 与 /backend/**匹配上。
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        // 当前的request和response都是Servlet类型，我们需要的是HttpServlet类型，所先强转
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;


        //	1.获取本次请求的URI（就是本次请求的路径）
        String requestURI = request.getRequestURI();
        log.info("拦截到请求：{}",requestURI);

        //通常需要处理的都是要控制层匹配的路径，这些都是要调用数据层的，所以要过滤处理
        //定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",  //登录路径
                "/employee/logout", //登出路径
                "/backend/**",      //都是静态资源，允许用户看页面。
                "/front/**",       //都是静态资源，允许用户看页面。
                "/common/**",
                //对用户登陆操作放行
                "/user/login",
                "/user/sendMsg"


        };


        //	2.判断本次请求是否需要处理
        // 调用check方法，将urls和requestURL传递进去进行匹配。
        boolean check = check(urls, requestURI);

        //	3.如果不需要处理，则直接放行
        if(check){
            log.info("本次请求{}不需要处理",requestURI);
            //放行,允许访问资源，否则数据不会展示
            filterChain.doFilter(request,response);
            //因为都放行了，后面代码无需执行，直接return
            return;

        }


        //	4.判断登录状态，如果已登录，则直接放行

        // 4-1、判断后台人员是否登录
        // 登录的时候session中存放了employee,所以只要session不是空，就说明已经登录了
        if(request.getSession().getAttribute("employee")!=null){
            log.info("用户已登录，用户ID为{}",request.getSession().getAttribute("employee"));
            //根据session来获取当前我们存的id值
            Long empId = (Long) request.getSession().getAttribute("employee");
            //使用BaseContext将id封装到线程中
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


        //4-2 判断前台用户是否登录
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("user"));
            Long userId = (Long)request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request,response);
            return;
        }


        log.info("用户未登录");

        //	5.如果未登录则返回未登录结果,通过输出流向客户端页面响应数据
        //  此处前端backend文件中request.js文件中已经定义了统一响应结果的响应拦截器，
        //  当msg是NOTLOGIN时会跳转到登录页面，所以这个MSG不能乱写，否则不会跳转

        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;


/*      // 原本是后面直接用加号链接string。现在使用{}占位符。
        log.info("拦截到请求：{}",request.getRequestURI());
        //放行,允许访问资源，否则数据不会展示
        filterChain.doFilter(request,response);*/

    }

        //创建一个check方法，用于路径匹配，检查本次请求是否需要放行
        //获取的请求路径requestURI与不需要处理的路径urls进行匹配，所以形参是这两个参数
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

}
