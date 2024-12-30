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
    //有默认值
    private Integer status;

    //Mybatis Plus公共字段自动填充，也就是在插入（insert）或者更新(update)的时候为指定字段赋予指定的值
    //在实体类中，对需要自动填充的字段添加注解 @TableField，并指定自动填充的策略 insert或者update

    //公共字段自动填充功能，插入时填充字段，只有在新增人员时需要输入创建时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    //公共字段自动填充功能，插入和更新时填充字段，在新增和更新时需要输入更新时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    //公共字段自动填充功能，插入时填充字段，新增员工的时候，需要插入创建人ID
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    //公共字段自动填充功能，插入和更新时填充字段，新增和更新员工时，都需要插入更新人员ID
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

}
