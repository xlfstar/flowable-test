package com.example.flowable.controller;

import com.example.flowable.common.Result;
import com.example.flowable.dto.ApprovalChainVO;
import com.example.flowable.dto.DepartmentVO;
import com.example.flowable.dto.OrgUserVO;
import com.example.flowable.dto.PositionVO;
import com.example.flowable.security.SecurityUtils;
import com.example.flowable.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "组织架构")
@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(summary = "部门树")
    @GetMapping("/departments")
    public Result<List<DepartmentVO>> departments() {
        return Result.ok(organizationService.departmentTree());
    }

    @Operation(summary = "岗位列表")
    @GetMapping("/positions")
    public Result<List<PositionVO>> positions() {
        return Result.ok(organizationService.positions());
    }

    @Operation(summary = "组织成员列表")
    @GetMapping("/users")
    public Result<List<OrgUserVO>> users() {
        return Result.ok(organizationService.users());
    }

    @Operation(summary = "当前用户审批路径预览")
    @GetMapping("/approval-chain")
    public Result<ApprovalChainVO> approvalChain(@RequestParam(defaultValue = "1") int days) {
        return Result.ok(organizationService.previewApprovalChain(SecurityUtils.getUserId(), days));
    }
}
