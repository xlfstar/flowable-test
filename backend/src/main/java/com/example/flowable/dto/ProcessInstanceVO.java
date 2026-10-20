package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class ProcessInstanceVO {

    private String id;

    private String processDefinitionId;

    private String processDefinitionKey;

    private String processDefinitionName;

    private String processDefinitionVersion;

    private String businessKey;

    private String startUserId;

    private String startUserName;

    private Date startTime;

    private Date endTime;

    private Long durationInMillis;

    private String deleteReason;

    private boolean ended;

    /**
     * 当前活动任务节点名称
     */
    private String currentActivityName;

    /**
     * 当前活动节点ID集合 (用于高亮)
     */
    private List<String> activeActivityIds;
}
