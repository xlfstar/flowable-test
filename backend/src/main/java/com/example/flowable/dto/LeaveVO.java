package com.example.flowable.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LeaveVO {

    private Long id;

    private String processInstanceId;

    private Long applyUserId;

    private String applyUserName;

    private Long deptId;

    private String deptName;

    private Long positionId;

    private String positionName;

    private Long deptLeaderId;

    private String deptLeaderName;

    private Long upperLeaderId;

    private String upperLeaderName;

    private Integer days;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String reason;

    /**
     * 0 审批中 1 已通过 2 已驳回
     */
    private Integer status;

    private String statusText;

    private String currentActivityName;

    private String currentTaskId;

    private String currentAssigneeName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
