package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApproverVO {

    private Long userId;

    private String userName;

    private Long deptId;

    private String deptName;

    private String positionName;

    /**
     * 审批层级, 从 1 开始
     */
    private Integer level;

    /**
     * 层级名称: 部门领导 / 上级领导 / 第N级领导
     */
    private String levelName;
}
