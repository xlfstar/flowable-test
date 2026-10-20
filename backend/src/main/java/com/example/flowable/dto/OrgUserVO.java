package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgUserVO {

    private Long id;

    private String username;

    private String realName;

    private String role;

    private Long deptId;

    private String deptName;

    private Long positionId;

    private String positionName;

    private Boolean departmentLeader;

    private Integer status;
}
