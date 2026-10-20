package com.example.flowable.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class TaskDetailVO {

    private TaskVO task;

    private Map<String, Object> variables;

    private List<CommentVO> comments;

    private ProcessInstanceVO processInstance;

    private List<String> activeActivityIds;
}
