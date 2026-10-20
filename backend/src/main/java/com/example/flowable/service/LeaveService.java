package com.example.flowable.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.flowable.common.BizException;
import com.example.flowable.common.PageResult;
import com.example.flowable.common.ProcessConstants;
import com.example.flowable.dto.ApprovalChainVO;
import com.example.flowable.dto.ApproverVO;
import com.example.flowable.dto.LeaveApplyRequest;
import com.example.flowable.dto.LeaveVO;
import com.example.flowable.dto.ProcessInstanceVO;
import com.example.flowable.dto.StartProcessRequest;
import com.example.flowable.entity.LeaveRequest;
import com.example.flowable.entity.SysUser;
import com.example.flowable.mapper.LeaveRequestMapper;
import com.example.flowable.mapper.SysUserMapper;
import com.example.flowable.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {

    public static final String PROCESS_KEY = ProcessConstants.LEAVE_PROCESS_KEY;

    private final LeaveRequestMapper leaveRequestMapper;

    private final SysUserMapper sysUserMapper;

    private final OrganizationService organizationService;

    private final ProcessInstanceService processInstanceService;

    private final RuntimeService runtimeService;

    private final HistoryService historyService;

    private final TaskService taskService;

    @Transactional(rollbackFor = Exception.class)
    public LeaveVO apply(LeaveApplyRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BizException("结束日期不能早于开始日期");
        }
        Long userId = SecurityUtils.getUserId();
        SysUser applicant = SecurityUtils.getLoginUser().getUser();
        ApprovalChainVO approval = organizationService.resolveApprovalChain(userId, request.getDays());
        ApproverVO departmentLeader = approval.getApprovers().get(0);
        ApproverVO upperLeader = approval.getApprovers().size() > 1 ? approval.getApprovers().get(1) : null;

        LeaveRequest entity = new LeaveRequest();
        entity.setApplyUserId(userId);
        entity.setApplyUserName(SecurityUtils.getRealName());
        entity.setDeptId(approval.getDeptId());
        entity.setDeptName(approval.getDeptName());
        entity.setPositionId(applicant.getPositionId());
        entity.setPositionName(approval.getPositionName());
        entity.setDeptLeaderId(departmentLeader.getUserId());
        entity.setDeptLeaderName(departmentLeader.getUserName());
        if (upperLeader != null) {
            entity.setUpperLeaderId(upperLeader.getUserId());
            entity.setUpperLeaderName(upperLeader.getUserName());
        }
        entity.setDays(request.getDays());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setReason(request.getReason());
        entity.setStatus(0);
        entity.setDeleted(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        leaveRequestMapper.insert(entity);

        Map<String, Object> variables = new HashMap<>();
        variables.put("days", request.getDays());
        variables.put("leaveDays", request.getDays());
        variables.put("reason", request.getReason());
        variables.put("startDate", request.getStartDate().toString());
        variables.put("endDate", request.getEndDate().toString());
        variables.put("leaveId", entity.getId());
        variables.put("deptId", approval.getDeptId());
        variables.put("deptName", approval.getDeptName());
        variables.put("positionName", approval.getPositionName());
        variables.put("multiLevel", Boolean.TRUE.equals(approval.getMultiLevel()));
        variables.put("deptLeaderId", String.valueOf(departmentLeader.getUserId()));
        variables.put("deptLeaderName", departmentLeader.getUserName());
        if (upperLeader != null) {
            variables.put("upperLeaderId", String.valueOf(upperLeader.getUserId()));
            variables.put("upperLeaderName", upperLeader.getUserName());
        }

        StartProcessRequest startRequest = new StartProcessRequest();
        startRequest.setProcessDefinitionKey(PROCESS_KEY);
        startRequest.setBusinessKey(String.valueOf(entity.getId()));
        startRequest.setVariables(variables);
        ProcessInstanceVO instance = processInstanceService.start(startRequest);

        entity.setProcessInstanceId(instance.getId());
        entity.setUpdateTime(LocalDateTime.now());
        leaveRequestMapper.updateById(entity);
        return toVO(entity);
    }

    public PageResult<LeaveVO> pageMy(int page, int size, Integer status) {
        Long userId = SecurityUtils.getUserId();
        var wrapper = Wrappers.<LeaveRequest>lambdaQuery()
                .eq(LeaveRequest::getApplyUserId, userId)
                .eq(status != null, LeaveRequest::getStatus, status)
                .orderByDesc(LeaveRequest::getCreateTime);
        IPage<LeaveRequest> result = leaveRequestMapper.selectPage(new Page<>(page, size), wrapper);
        List<LeaveVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(result.getTotal(), records);
    }

    public LeaveVO detail(Long id) {
        LeaveRequest entity = leaveRequestMapper.selectById(id);
        if (entity == null) {
            throw new BizException("请假申请不存在");
        }
        return toVO(entity);
    }

    /**
     * 流程结束后同步请假申请状态
     */
    public void handleProcessEnd(String processInstanceId) {
        boolean running = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).count() > 0;
        if (running) {
            return;
        }
        LeaveRequest entity = leaveRequestMapper.selectOne(Wrappers.<LeaveRequest>lambdaQuery()
                .eq(LeaveRequest::getProcessInstanceId, processInstanceId));
        if (entity == null || entity.getStatus() != null && entity.getStatus() != 0) {
            return;
        }
        HistoricVariableInstance approved = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName("approved")
                .singleResult();
        boolean pass = approved != null && Boolean.TRUE.equals(approved.getValue());
        entity.setStatus(pass ? 1 : 2);
        entity.setUpdateTime(LocalDateTime.now());
        leaveRequestMapper.updateById(entity);
    }

    private LeaveVO toVO(LeaveRequest entity) {
        LeaveVO.LeaveVOBuilder builder = LeaveVO.builder()
                .id(entity.getId())
                .processInstanceId(entity.getProcessInstanceId())
                .applyUserId(entity.getApplyUserId())
                .applyUserName(entity.getApplyUserName())
                .deptId(entity.getDeptId())
                .deptName(entity.getDeptName())
                .positionId(entity.getPositionId())
                .positionName(entity.getPositionName())
                .deptLeaderId(entity.getDeptLeaderId())
                .deptLeaderName(entity.getDeptLeaderName())
                .upperLeaderId(entity.getUpperLeaderId())
                .upperLeaderName(entity.getUpperLeaderName())
                .days(entity.getDays())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .statusText(statusText(entity.getStatus()))
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime());

        String processInstanceId = entity.getProcessInstanceId();
        if (processInstanceId != null && entity.getStatus() != null && entity.getStatus() == 0) {
            Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
            if (task != null) {
                builder.currentActivityName(task.getName());
                builder.currentTaskId(task.getId());
                builder.currentAssigneeName(resolveAssigneeName(task.getAssignee()));
            }
        }
        return builder.build();
    }

    private String resolveAssigneeName(String assignee) {
        if (!StringUtils.hasText(assignee)) {
            return null;
        }
        try {
            SysUser user = sysUserMapper.selectById(Long.valueOf(assignee));
            if (user == null) {
                return assignee;
            }
            return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
        } catch (NumberFormatException e) {
            return assignee;
        }
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "审批中";
            case 1 -> "已通过";
            case 2 -> "已驳回";
            default -> "未知";
        };
    }
}
