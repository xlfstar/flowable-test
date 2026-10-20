package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
@Builder
public class FormInstanceVO {

    private String taskId;

    private String taskName;

    private String taskDefinitionKey;

    private String formKey;

    private String formName;

    private String schemaJson;

    private String assigneeId;

    private String assigneeName;

    private Date startTime;

    private Date endTime;

    private boolean active;

    private boolean canHandle;

    private Map<String, Object> values;
}
