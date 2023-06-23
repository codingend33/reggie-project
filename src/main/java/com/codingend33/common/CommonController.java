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

@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {
    @Value("${reggie.path}")
    private String basePath;

    //文件上传
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file){
        //file 是一个临时文件，需要转存到指定位置，否则请求完成后临时文件会删除
        //log.info("file:{}",file.toString());

        //获取一下传入的原文件名
        String originalFilename = file.getOriginalFilename();
        //我们只需要获取一下格式后缀，取子串，起始点为最后一个.
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //为了防止出现重复的文件名，我们需要使用UUID
        String fileName = UUID.randomUUID() + suffix;

        //创建一个目录对象,判断配置文件中定义的目录是否存在，不存在则直接创建
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        //我们将其转存到我们的指定目录下，目录我们使用动态方式获取。
        //basePath是路径，fileName是文件名。 合并字符传就是 F:\\reggie\\img\\filename
        //但方法会抛异常，我们这里用try/catch处理一下
        try {
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //将文件名返回给前端，便于后面增加餐品时，能将文件名保存在数据库中
        return R.success(fileName);
    }

    @GetMapping("/download")
    // 因为不需要返回值，所以是void
    // 前端发送的请求中有name。因为是向浏览器写出数据，还需要通过response获取输出流。这两个都要作为参数参。
    public void download(String name, HttpServletResponse response){

        try {
            //输入流，通过输入流读取文件内容
            FileInputStream fileInputStream=new FileInputStream(new File(basePath+name));

            //输出流，通过输出流将文件写回浏览器，在浏览器中展示图片
            ServletOutputStream outputStream = response.getOutputStream();
            //设置响应给浏览器的数据类型
            response.setContentType("image/jpeg");

            //输出的数据需要存放到一个数组中，所以创建byte数据
            int len=0;
            byte[] bytes = new byte[1024];
            //通过输入流一直读取数据，然后通过输出流将数据存入数组中，至到读取的长度为-1。
            while ((len=fileInputStream.read(bytes))!=-1){
                outputStream.write(bytes,0,len);
                outputStream.flush();
            }
            outputStream.close();
            fileInputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
