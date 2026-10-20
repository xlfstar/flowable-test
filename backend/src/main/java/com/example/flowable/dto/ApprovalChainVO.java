package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApprovalChainVO {

    private Integer days;

    /**
     * 是否需要上级领导审批（days > 3）
     */
    private Boolean multiLevel;

    /**
     * 当前组织配置是否足以完成该请假审批
     */
    private Boolean valid;

    private String message;

    private Long applicantId;

    private String applicantName;

    private Long deptId;

    private String deptName;

    private String positionName;

    /**
     * 按请假天数实际需要的审批人列表（1 或 2 个）
     */
    private List<ApproverVO> approvers;
}
