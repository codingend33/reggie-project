package com.codingend33.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/*
* 员工实体类
* */
@Data
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String name;

    private String password;

    private String phone;

    private String sex;


    //身份证号码。数据库中是id_number，但也能匹配，因为已经在配置文件中开启了驼峰命名的匹配
    private String idNumber;

    private Integer status;


    //公共字段自动填充功能，插入时填充字段
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    //公共字段自动填充功能，插入和更新时填充字段
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    //公共字段自动填充功能，插入时填充字段
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    //公共字段自动填充功能，插入和更新时填充字段
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

}
