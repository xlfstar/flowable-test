package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FormVO {

    private Long id;

    private String formKey;

    private String name;

    private String category;

    private String schemaJson;

    private String remark;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
