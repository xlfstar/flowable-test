package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PositionVO {

    private Long id;

    private String positionCode;

    private String positionName;

    private Integer positionLevel;

    private Integer sort;

    private Integer status;
}
