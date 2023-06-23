package com.codingend33.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codingend33.common.R;
import com.codingend33.entity.User;
import com.codingend33.service.UserService;
import com.codingend33.utils.MailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;


    //发送验证码请求
    //是json格式，可以封装到User对象中，因为它有一个Phone属性
    //验证码需要存在方session中，方便后面比对
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) throws MessagingException {

        String phone = user.getPhone();

        if (!phone.isEmpty()) {
            //随机生成一个验证码
            String code = MailUtils.achieveCode();
            log.info(code);
            //这里的phone其实就是邮箱，code是我们生成的验证码
            MailUtils.sendTestMail(phone, code);
            //验证码存session，方便后面拿出来比对
            //session.setAttribute(phone, code);
            //验证码缓存到Redis，设置存活时间5分钟,单位是5分钟
            redisTemplate.opsForValue().set(phone, code,5, TimeUnit.MINUTES);
            return R.success("验证码发送成功");
        }
        return R.error("验证码发送失败");
    }



    //输入验证码后就需要登录了，所以添加login方法处理登录请求
    //因为发送请求的数据有键值对类型，所以使用map对象接收。
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session) {
        log.info(map.toString());

        //获取邮箱
        String phone = map.get("phone").toString();
        //获取验证码
        String code = map.get("code").toString();


        //从session中获取验证码
        //String codeInSession = String.valueOf(session.getAttribute(phone));

        //把Redis中缓存的code拿出来
        Object codeInRedis = redisTemplate.opsForValue().get(phone);


        //比较这用户输入的验证码和session中存的验证码是否一致
        if (code != null && code.equals(codeInRedis)) {
            //如果输入正确，判断一下当前用户是否存在
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            //判断依据是从数据库中查询是否有其邮箱
            queryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(queryWrapper);
            //如果不存在，则创建一个，存入数据库
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);

            }
            //存个session，表示登录状态，否则登录后就出现闪退。
            session.setAttribute("user",user.getId());

            //如果登录成功，则删除Redis中的验证码
            redisTemplate.delete(phone);
            //并将其作为结果返回
            return R.success(user);
        }
        return R.error("登录失败");
    }

    @PostMapping("/loginout")
    public R<String> logout(HttpServletRequest request) {
        request.getSession().removeAttribute("user");
        return R.success("退出成功");
    }

}
