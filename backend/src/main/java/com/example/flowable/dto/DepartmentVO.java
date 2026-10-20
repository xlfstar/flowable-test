package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DepartmentVO {

    private Long id;

    private String deptCode;

    private String deptName;

    private Long parentId;

    private Long leaderId;

    private String leaderName;

    private Integer sort;

    private Integer status;

    private List<DepartmentVO> children;
}
