package com.example.flowable.controller;

import com.example.flowable.common.PageResult;
import com.example.flowable.common.Result;
import com.example.flowable.dto.CommentVO;
import com.example.flowable.dto.CompleteTaskRequest;
import com.example.flowable.dto.TaskDetailVO;
import com.example.flowable.dto.TaskVO;
import com.example.flowable.service.WorkflowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "任务")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final WorkflowTaskService workflowTaskService;

    @Operation(summary = "我的待办")
    @GetMapping("/todo")
    public Result<PageResult<TaskVO>> todo(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        return Result.ok(workflowTaskService.pageTodo(page, size, name));
    }

    @Operation(summary = "我的已办")
    @GetMapping("/done")
    public Result<PageResult<TaskVO>> done(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        return Result.ok(workflowTaskService.pageDone(page, size, name));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/{taskId}")
    public Result<TaskDetailVO> detail(@PathVariable String taskId) {
        return Result.ok(workflowTaskService.detail(taskId));
    }

    @Operation(summary = "办理任务")
    @PostMapping("/{taskId}/complete")
    public Result<Void> complete(@PathVariable String taskId,
                                 @RequestBody(required = false) CompleteTaskRequest request) {
        workflowTaskService.complete(taskId, request);
        return Result.ok();
    }

    @Operation(summary = "认领任务")
    @PostMapping("/{taskId}/claim")
    public Result<Void> claim(@PathVariable String taskId) {
        workflowTaskService.claim(taskId);
        return Result.ok();
    }

    @Operation(summary = "取消认领")
    @PostMapping("/{taskId}/unclaim")
    public Result<Void> unclaim(@PathVariable String taskId) {
        workflowTaskService.unclaim(taskId);
        return Result.ok();
    }

    @Operation(summary = "流程审批意见/评论")
    @GetMapping("/comments")
    public Result<List<CommentVO>> comments(@RequestParam String processInstanceId) {
        return Result.ok(workflowTaskService.listComments(processInstanceId));
    }
}
