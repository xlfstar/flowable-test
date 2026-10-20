package com.example.flowable.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CompleteTaskRequest {

    private Map<String, Object> variables = new HashMap<>();

    /**
     * 审批意见
     */
    private String comment;
}
