package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ProcessDefinitionVO {

    private String id;

    private String key;

    private String name;

    private Integer version;

    private String deploymentId;

    private String resourceName;

    private String diagramResourceName;

    private String category;

    private String description;

    /**
     * 1 激活 2 挂起
     */
    private Integer suspensionState;

    private boolean suspended;

    private Date deployTime;
}
