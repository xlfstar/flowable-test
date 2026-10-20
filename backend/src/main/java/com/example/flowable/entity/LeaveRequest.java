package com.example.flowable.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("leave_request")
public class LeaveRequest implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String processInstanceId;

    private Long applyUserId;

    private String applyUserName;

    /**
     * 申请人部门快照
     */
    private Long deptId;

    private String deptName;

    /**
     * 申请人岗位快照
     */
    private Long positionId;

    private String positionName;

    /**
     * 部门领导快照（实际第一审批人）
     */
    private Long deptLeaderId;

    private String deptLeaderName;

    /**
     * 上级领导快照（请假超过3天时的第二审批人）
     */
    private Long upperLeaderId;

    private String upperLeaderName;

    private Integer days;

    private LocalDate startDate;

    private LocalDate endDate;

    private String reason;

    /**
     * 0 审批中 1 已通过 2 已驳回
     */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
