package com.example.flowable.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FormSaveRequest {

    @NotBlank(message = "表单标识不能为空")
    private String formKey;

    @NotBlank(message = "表单名称不能为空")
    private String name;

    private String category;

    private String schemaJson;

    private String remark;

    private Integer status;
}
