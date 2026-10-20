package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class TaskVO {

    private String id;

    private String name;

    private String taskDefinitionKey;

    private String processInstanceId;

    private String processDefinitionId;

    private String processDefinitionKey;

    private String processDefinitionName;

    private String businessKey;

    private String assignee;

    private String assigneeName;

    private String description;

    private String category;

    private String formKey;

    private Date createTime;

    private Date claimTime;

    private Date endTime;

    private Long durationInMillis;

    private String deleteReason;
}
