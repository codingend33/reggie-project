package com.codingend33.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

public class MailUtils {
    public static void main(String[] args) throws MessagingException {
        //可以在这里直接测试方法，填自己的邮箱即可
        sendTestMail("your-email@gmail.com", new MailUtils().achieveCode());  // 替换为你的 Gmail
    }

    public static void sendTestMail(String email, String code) throws MessagingException {
        // 创建Properties 类用于记录邮箱的一些属性
        Properties props = new Properties();
        // 表示SMTP发送邮件，必须进行身份验证
        props.put("mail.smtp.auth", "true");
        // SMTP服务器设置，Gmail使用的SMTP服务器
        props.put("mail.smtp.host", "smtp.gmail.com");
        // 使用SSL的端口号
//        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.port", "587");
        // 启用SSL连接
//        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.starttls.enable", "true");
        // 此处填写你的Gmail账号
        props.put("mail.user", "codingend33@gmail.com");  // 替换为你的 Gmail
        // 此处填写你生成的应用密码（在Gmail账户设置中生成）
        props.put("mail.password", "nqwepkminhwlufpi");  // 替换为你的应用密码
        // 构建授权信息，用于进行SMTP身份验证
        Authenticator authenticator = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                String userName = props.getProperty("mail.user");
                String password = props.getProperty("mail.password");
                return new PasswordAuthentication(userName, password);
            }
        };
        // 使用环境属性和授权信息，创建邮件会话
        Session mailSession = Session.getInstance(props, authenticator);
        // 创建邮件消息
        MimeMessage message = new MimeMessage(mailSession);
        // 设置发件人
        InternetAddress form = new InternetAddress(props.getProperty("mail.user"));
        message.setFrom(form);
        // 设置收件人的邮箱
        InternetAddress to = new InternetAddress(email);
        message.setRecipient(RecipientType.TO, to);
        // 设置邮件标题
        message.setSubject("邮件测试");
        // 设置邮件内容体
        message.setContent("尊敬的用户:你好!\n注册验证码为:" + code + "(有效期为一分钟,请勿告知他人)", "text/html;charset=UTF-8");
        // 发送邮件
        try {
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();  // 处理异常，例如日志记录或提示用户
        }
    }

    public static String achieveCode() {  // 由于数字 1 、 0 和字母 O 、l 有时分不清楚，所以没有数字 1、0
        String[] beforeShuffle = new String[]{"2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F",
                "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "a",
                "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
                "w", "x", "y", "z"};
        List<String> list = Arrays.asList(beforeShuffle);  // 将数组转换为集合
        Collections.shuffle(list);  // 打乱集合顺序
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s); // 将集合转化为字符串
        }
        return sb.substring(3, 8);
    }
}
