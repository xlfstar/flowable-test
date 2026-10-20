package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProcessFormDataVO {

    private String processInstanceId;

    private FormInstanceVO startForm;

    private List<FormInstanceVO> taskForms;

    private Map<String, Object> variables;
}
