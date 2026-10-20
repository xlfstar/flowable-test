package com.example.flowable.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 岗位表。
 *
 * <p>岗位描述员工的职级/职能，部门负责人仍以 sys_department.leader_id 为准。</p>
 */
@Data
@TableName("sys_position")
public class SysPosition implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String positionCode;

    private String positionName;

    /**
     * 岗位层级，数字越小级别越高
     */
    private Integer positionLevel;

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
