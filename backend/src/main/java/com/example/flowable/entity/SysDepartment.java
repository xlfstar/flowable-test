package com.example.flowable.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门表。
 *
 * <p>部门通过 {@link #parentId} 组成组织树，{@link #leaderId} 指向部门负责人。
 * 请假审批人由组织树动态解析，不在流程定义中写死具体用户或角色。</p>
 */
@Data
@TableName("sys_department")
public class SysDepartment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deptCode;

    private String deptName;

    /**
     * 上级部门ID，NULL 表示根部门
     */
    private Long parentId;

    /**
     * 部门负责人用户ID
     */
    private Long leaderId;

    private Integer sort;

    /**
     * 1 启用 0 停用
     */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
