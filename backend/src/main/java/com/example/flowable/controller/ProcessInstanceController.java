package com.example.flowable.controller;

import com.example.flowable.common.PageResult;
import com.example.flowable.common.Result;
import com.example.flowable.dto.ProcessFormDataVO;
import com.example.flowable.dto.ProcessInstanceVO;
import com.example.flowable.dto.StartProcessRequest;
import com.example.flowable.service.ProcessInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "流程实例")
@RestController
@RequestMapping("/api/process-instances")
@RequiredArgsConstructor
public class ProcessInstanceController {

    private final ProcessInstanceService processInstanceService;

    @Operation(summary = "启动流程实例")
    @PostMapping
    public Result<ProcessInstanceVO> start(@Valid @RequestBody StartProcessRequest request) {
        return Result.ok("启动成功", processInstanceService.start(request));
    }

    @Operation(summary = "运行中流程实例分页列表")
    @GetMapping
    public Result<PageResult<ProcessInstanceVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String startedBy) {
        return Result.ok(processInstanceService.pageRunning(page, size, key, startedBy));
    }

    @Operation(summary = "我发起的流程")
    @GetMapping("/my")
    public Result<PageResult<ProcessInstanceVO>> my(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(processInstanceService.pageMyStarted(page, size));
    }

    @Operation(summary = "流程实例详情")
    @GetMapping("/{processInstanceId}")
    public Result<ProcessInstanceVO> detail(@PathVariable String processInstanceId) {
        return Result.ok(processInstanceService.getDetail(processInstanceId));
    }

    @Operation(summary = "当前活动节点ID(用于流程图高亮)")
    @GetMapping("/{processInstanceId}/activities")
    public Result<List<String>> activities(@PathVariable String processInstanceId) {
        return Result.ok(processInstanceService.getActiveActivityIds(processInstanceId));
    }

    @Operation(summary = "流程实例历史表单数据")
    @GetMapping("/{processInstanceId}/form-data")
    public Result<ProcessFormDataVO> formData(@PathVariable String processInstanceId) {
        return Result.ok(processInstanceService.getFormData(processInstanceId));
    }

    @Operation(summary = "删除/撤销流程实例")
    @DeleteMapping("/{processInstanceId}")
    public Result<Void> delete(@PathVariable String processInstanceId,
                               @RequestParam(required = false) String reason) {
        processInstanceService.delete(processInstanceId, reason);
        return Result.ok();
    }
}
