package com.example.flowable.dto;

import lombok.Data;

import java.util.Date;

@Data
public class CommentVO {

    private String id;

    private String type;

    private String userId;

    private String userName;

    private String message;

    private String taskId;

    private String processInstanceId;

    private Date time;
}
