package com.example.flowable.controller;

import com.example.flowable.common.PageResult;
import com.example.flowable.common.Result;
import com.example.flowable.dto.LeaveApplyRequest;
import com.example.flowable.dto.LeaveVO;
import com.example.flowable.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "请假申请")
@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @Operation(summary = "提交请假申请(同时启动流程)")
    @PostMapping("/apply")
    public Result<LeaveVO> apply(@Valid @RequestBody LeaveApplyRequest request) {
        return Result.ok("提交成功", leaveService.apply(request));
    }

    @Operation(summary = "我的请假记录")
    @GetMapping("/my")
    public Result<PageResult<LeaveVO>> my(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(leaveService.pageMy(page, size, status));
    }

    @Operation(summary = "请假申请详情")
    @GetMapping("/{id}")
    public Result<LeaveVO> detail(@PathVariable Long id) {
        return Result.ok(leaveService.detail(id));
    }
}
