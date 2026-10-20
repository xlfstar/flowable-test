package com.example.flowable.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    /**
     * 角色: admin / executive / manager / hr / employee
     */
    private String role;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 岗位ID
     */
    private Long positionId;

    private String email;

    private String phone;

    /**
     * 1 启用 0 禁用
     */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
