package org.myweb.campusscoremanager.pojo;

import lombok.Data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @Desc: 用户实体
 * @Author: huangzhiyuan
 * @CreateDate: 2026/1/18 20:50
 * @Modify:
 */
@Data
@TableName("user")
public class User {

    public static final String ID = "id";
    public static final String REAL_NAME = "realName";
    public static final String PASSWORD = "password";
    public static final String LOGIN_NAME = "loginName";

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String realName;

    private String password;

    private Integer priviledge;

    private String loginName;
}
