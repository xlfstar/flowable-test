package com.example.flowable.controller;

import com.example.flowable.common.PageResult;
import com.example.flowable.common.Result;
import com.example.flowable.dto.FormSaveRequest;
import com.example.flowable.dto.FormVO;
import com.example.flowable.service.FormDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "自定义表单")
@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
public class FormController {

    private final FormDefinitionService formDefinitionService;

    @Operation(summary = "表单分页列表")
    @GetMapping
    public Result<PageResult<FormVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String key) {
        return Result.ok(formDefinitionService.page(page, size, name, key));
    }

    @Operation(summary = "全部启用的表单(用于流程设计器绑定)")
    @GetMapping("/all")
    public Result<List<FormVO>> all() {
        return Result.ok(formDefinitionService.listEnabled());
    }

    @Operation(summary = "表单详情")
    @GetMapping("/{id}")
    public Result<FormVO> detail(@PathVariable Long id) {
        return Result.ok(formDefinitionService.get(id));
    }

    @Operation(summary = "按表单标识获取")
    @GetMapping("/by-key/{formKey}")
    public Result<FormVO> byKey(@PathVariable String formKey) {
        return Result.ok(formDefinitionService.getByKey(formKey));
    }

    @Operation(summary = "解析任务绑定表单")
    @GetMapping("/resolve/task/{taskId}")
    public Result<FormVO> resolveTask(@PathVariable String taskId) {
        return Result.ok(formDefinitionService.resolveByTask(taskId));
    }

    @Operation(summary = "解析流程启动表单")
    @GetMapping("/resolve/start/{processDefinitionId}")
    public Result<FormVO> resolveStart(@PathVariable String processDefinitionId) {
        return Result.ok(formDefinitionService.resolveByProcessDefinition(processDefinitionId));
    }

    @Operation(summary = "新建表单")
    @PostMapping
    public Result<FormVO> create(@Valid @RequestBody FormSaveRequest request) {
        return Result.ok("保存成功", formDefinitionService.create(request));
    }

    @Operation(summary = "修改表单")
    @PutMapping("/{id}")
    public Result<FormVO> update(@PathVariable Long id, @Valid @RequestBody FormSaveRequest request) {
        return Result.ok("保存成功", formDefinitionService.update(id, request));
    }

    @Operation(summary = "删除表单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        formDefinitionService.delete(id);
        return Result.ok();
    }
}
