package com.example.flowable.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class StartProcessRequest {

    @NotBlank(message = "流程定义Key不能为空")
    private String processDefinitionKey;

    private String businessKey;

    private Map<String, Object> variables = new HashMap<>();
}
