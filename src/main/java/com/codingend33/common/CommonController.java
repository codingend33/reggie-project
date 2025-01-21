package com.codingend33.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件的上传和下载
 */

@Slf4j //日志输出
@RestController // @controller + @responsebody
@RequestMapping("/common") // 映射请求路径
public class CommonController {

    // 用于转存文件的路径，reggie.path在yml文件中定义，这里只是引用一下。
    // @value注解获取路径，并声明一个basePath变量允许使用该对象。
    @Value("${reggie.path}")
    private String basePath;

    //文件上传
    //返回值string,因为返回的是文件名
    //形参是MultipartFile类，允许接收上传的文件。形参名字file必须与前端表单中的字段名称name="file"一致，否则接收不到
    //上传方法必须在登录的才能实现，因为需要经过过滤器。或者直接在过滤器中对common路径方形。
    @PostMapping("/upload") //路径映射，请求与方法绑定
    public R<String> upload(MultipartFile file){

        // file 是一个临时文件，需要转存到指定位置，否则请求完成后临时文件会删除。

        // 日志输出debug
        log.info("file:{}",file.toString());

        //1.获取传入的原文件名，重命名为唯一标识符。example.jpg -》123e4567-e89b-12d3-a456-426614174000.jpg
        String originalFilename = file.getOriginalFilename();
        //我们只需要获取一下格式后缀，取子串，起始点为最后一个点
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //为了防止出现重复的文件名，我们需要使用UUID代替文件名
        String fileName = UUID.randomUUID() + suffix;

        //2.创建一个转存目录对象,判断配置文件中定义的目录是否存在，不存在则需要先创建
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        //3.拼接新的目录路径，将原文件转存。
        // transferTo性能高：直接转移文件，无需手动复制文件流。
        // 将临时文件转存到我们的指定目录下，目录的路径使用动态方式获取，
        // basePath是目标文件存储的基础路径，fileName是文件名。 合并字符传就是最终路径 F:\\reggie\\img\\filename
        // 但方法会抛异常，我们这里用try/catch处理一下
        try {
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //将文件名返回给前端，便于后面增加餐品时，能将文件名保存在数据库中. 同样下载文件的时候这个filename也是url的参数
        return R.success(fileName);
    }



    /**
     * 文件下载（上传文件后，在浏览器中显示）
     */

    @GetMapping("/download") //路径映射，将请求路径和方法绑定
    // 因为下载不需要返回值，所以是void
    // 前端发送的请求中有name属性，所以形参中用name接收。
    // 因为下载的目的是在浏览器中显示，所以向浏览器输出数据，需要使用response获取输出流。
    public void download(String name, HttpServletResponse response){

        try {
            //输入流，从服务器磁盘上读取指定路径下的文件内容
            //需要先根据文件路径创建一个file对象，再把对象放入输入流中。
            FileInputStream fileInputStream=new FileInputStream(new File(basePath+name));

            //输出流，HttpServletResponse 提供的 getOutputStream 方法获取输出流，在浏览器中展示图片
            ServletOutputStream outputStream = response.getOutputStream();
            //设置响应给浏览器的数据类型
            response.setContentType("image/jpeg");

            //输出的数据需要存放到一个数组中，所以创建byte数据
            int len=0;
            byte[] bytes = new byte[1024];
            //通过输入流一直读取数据并存到bytes数组中，每次数组满了就通过输出流输出到浏览器
            //至到输入流读取的长度为-1时，循坏结束，表示所有数据都读取完了。
            while ((len=fileInputStream.read(bytes))!=-1){
                outputStream.write(bytes,0,len);
                outputStream.flush(); //刷新
            }
            //关闭输入流和输出流
            outputStream.close();
            fileInputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
