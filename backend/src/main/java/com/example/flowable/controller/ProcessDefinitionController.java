package com.example.flowable.controller;

import com.example.flowable.common.PageResult;
import com.example.flowable.common.Result;
import com.example.flowable.dto.ProcessDefinitionVO;
import com.example.flowable.service.ProcessDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "流程定义")
@RestController
@RequestMapping("/api/process-definitions")
@RequiredArgsConstructor
public class ProcessDefinitionController {

    private final ProcessDefinitionService processDefinitionService;

    @Operation(summary = "流程定义分页列表(最新版本)")
    @GetMapping
    public Result<PageResult<ProcessDefinitionVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String key) {
        return Result.ok(processDefinitionService.page(page, size, name, key));
    }

    @Operation(summary = "部署流程定义(上传 BPMN 文件)")
    @PostMapping(value = "/deploy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ProcessDefinitionVO> deploy(@RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) String name) {
        return Result.ok("部署成功", processDefinitionService.deploy(file, name));
    }

    @Operation(summary = "删除流程定义(部署)")
    @DeleteMapping("/{deploymentId}")
    public Result<Void> delete(@PathVariable String deploymentId,
                               @RequestParam(defaultValue = "true") boolean cascade) {
        processDefinitionService.deleteDeployment(deploymentId, cascade);
        return Result.ok();
    }

    @Operation(summary = "获取流程定义 XML")
    @GetMapping("/{processDefinitionId}/xml")
    public Result<String> xml(@PathVariable String processDefinitionId) {
        return Result.ok(processDefinitionService.getXml(processDefinitionId));
    }

    @Operation(summary = "获取流程图")
    @GetMapping("/{processDefinitionId}/diagram")
    public ResponseEntity<byte[]> diagram(@PathVariable String processDefinitionId) {
        byte[] bytes = processDefinitionService.getDiagram(processDefinitionId);
        if (bytes == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(bytes);
    }

    @Operation(summary = "挂起流程定义")
    @PostMapping("/{processDefinitionId}/suspend")
    public Result<Void> suspend(@PathVariable String processDefinitionId) {
        processDefinitionService.suspend(processDefinitionId);
        return Result.ok();
    }

    @Operation(summary = "激活流程定义")
    @PostMapping("/{processDefinitionId}/activate")
    public Result<Void> activate(@PathVariable String processDefinitionId) {
        processDefinitionService.activate(processDefinitionId);
        return Result.ok();
    }
}
