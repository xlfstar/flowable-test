package com.example.flowable.service;

import com.example.flowable.common.BizException;
import com.example.flowable.common.PageResult;
import com.example.flowable.dto.CommentVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.flowable.dto.CompleteTaskRequest;
import com.example.flowable.dto.TaskDetailVO;
import com.example.flowable.dto.TaskVO;
import com.example.flowable.entity.SysUser;
import com.example.flowable.mapper.SysUserMapper;
import com.example.flowable.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowTaskService {

    private static final String ADMIN = "admin";

    private final TaskService taskService;

    private final RuntimeService runtimeService;

    private final HistoryService historyService;

    private final RepositoryService repositoryService;

    private final IdentityService identityService;

    private final ProcessInstanceService processInstanceService;

    private final LeaveService leaveService;

    private final SysUserMapper sysUserMapper;

    private final ObjectMapper objectMapper;

    public PageResult<TaskVO> pageTodo(int page, int size, String name) {
        String userId = String.valueOf(SecurityUtils.getUserId());
        TaskQuery query = taskService.createTaskQuery();
        if (!ADMIN.equalsIgnoreCase(SecurityUtils.getRole())) {
            query.or()
                    .taskAssignee(userId)
                    .taskCandidateGroupIn(List.of(SecurityUtils.getRole()))
                    .endOr();
        }
        if (StringUtils.hasText(name)) {
            query.taskNameLike("%" + name + "%");
        }
        long total = query.count();
        List<Task> tasks = query.orderByTaskCreateTime().desc().listPage((page - 1) * size, size);
        List<TaskVO> records = tasks.stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(total, records);
    }

    public PageResult<TaskVO> pageDone(int page, int size, String name) {
        String userId = String.valueOf(SecurityUtils.getUserId());
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished();
        if (StringUtils.hasText(name)) {
            query.taskNameLike("%" + name + "%");
        }
        long total = query.count();
        List<HistoricTaskInstance> tasks = query.orderByHistoricTaskInstanceEndTime().desc()
                .listPage((page - 1) * size, size);
        List<TaskVO> records = tasks.stream().map(this::toHistoricVO).collect(Collectors.toList());
        return PageResult.of(total, records);
    }

    public TaskDetailVO detail(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task != null) {
            Map<String, Object> variables = taskService.getVariables(taskId);
            List<CommentVO> comments = listComments(task.getProcessInstanceId());
            return TaskDetailVO.builder()
                    .task(toVO(task))
                    .variables(variables)
                    .comments(comments)
                    .processInstance(processInstanceService.getDetail(task.getProcessInstanceId()))
                    .activeActivityIds(processInstanceService.getActiveActivityIds(task.getProcessInstanceId()))
                    .build();
        }
        HistoricTaskInstance historic = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId).singleResult();
        if (historic == null) {
            throw new BizException("任务不存在或已办理: " + taskId);
        }
        String processInstanceId = historic.getProcessInstanceId();
        return TaskDetailVO.builder()
                .task(toHistoricVO(historic))
                .variables(getHistoricVariables(processInstanceId))
                .comments(listComments(processInstanceId))
                .processInstance(processInstanceService.getDetail(processInstanceId))
                .activeActivityIds(processInstanceService.getActiveActivityIds(processInstanceId))
                .build();
    }

    private Map<String, Object> getHistoricVariables(String processInstanceId) {
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).list();
        Map<String, Object> map = new HashMap<>();
        for (HistoricVariableInstance variable : variables) {
            map.put(variable.getVariableName(), variable.getValue());
        }
        return map;
    }

    public void complete(String taskId, CompleteTaskRequest request) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BizException("任务不存在或已办理: " + taskId);
        }
        String processInstanceId = task.getProcessInstanceId();
        Map<String, Object> variables = request == null || request.getVariables() == null
                ? new HashMap<>() : new HashMap<>(request.getVariables());
        String comment = request == null ? null : request.getComment();
        if (!StringUtils.hasText(comment) && variables.get("comment") != null) {
            comment = String.valueOf(variables.get("comment"));
        }
        identityService.setAuthenticatedUserId(String.valueOf(SecurityUtils.getUserId()));
        try {
            if (StringUtils.hasText(comment)) {
                taskService.addComment(taskId, processInstanceId, comment);
            }
            variables.putIfAbsent("approverName", SecurityUtils.getRealName());
            try {
                variables.put("taskFormData_" + taskId, objectMapper.writeValueAsString(variables));
            } catch (JsonProcessingException e) {
                throw new BizException("保存表单快照失败: " + e.getMessage());
            }
            taskService.complete(taskId, variables);
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
        leaveService.handleProcessEnd(processInstanceId);
    }

    public void claim(String taskId) {
        taskService.claim(taskId, String.valueOf(SecurityUtils.getUserId()));
    }

    public void unclaim(String taskId) {
        taskService.unclaim(taskId);
    }

    public List<CommentVO> listComments(String processInstanceId) {
        List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);
        if (comments == null) {
            return Collections.emptyList();
        }
        return comments.stream().map(this::toCommentVO).collect(Collectors.toList());
    }

    private CommentVO toCommentVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setType(comment.getType());
        vo.setUserId(comment.getUserId());
        vo.setUserName(resolveUserName(comment.getUserId()));
        vo.setMessage(comment.getFullMessage());
        vo.setTaskId(comment.getTaskId());
        vo.setProcessInstanceId(comment.getProcessInstanceId());
        vo.setTime(comment.getTime());
        return vo;
    }

    private TaskVO toVO(Task task) {
        ProcessDefinition definition = repositoryService.getProcessDefinition(task.getProcessDefinitionId());
        return TaskVO.builder()
                .id(task.getId())
                .name(task.getName())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .processDefinitionKey(definition == null ? null : definition.getKey())
                .processDefinitionName(definition == null ? null : definition.getName())
                .assignee(task.getAssignee())
                .assigneeName(resolveUserName(task.getAssignee()))
                .description(task.getDescription())
                .category(task.getCategory())
                .formKey(task.getFormKey())
                .createTime(task.getCreateTime())
                .claimTime(task.getClaimTime())
                .build();
    }

    private TaskVO toHistoricVO(HistoricTaskInstance task) {
        ProcessDefinition definition = repositoryService.getProcessDefinition(task.getProcessDefinitionId());
        return TaskVO.builder()
                .id(task.getId())
                .name(task.getName())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .processDefinitionKey(definition == null ? null : definition.getKey())
                .processDefinitionName(definition == null ? null : definition.getName())
                .assignee(task.getAssignee())
                .assigneeName(resolveUserName(task.getAssignee()))
                .createTime(task.getCreateTime())
                .claimTime(task.getClaimTime())
                .endTime(task.getEndTime())
                .durationInMillis(task.getDurationInMillis())
                .deleteReason(task.getDeleteReason())
                .build();
    }

    private String resolveUserName(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        try {
            SysUser user = sysUserMapper.selectById(Long.valueOf(userId));
            if (user == null) {
                return userId;
            }
            return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
        } catch (NumberFormatException e) {
            return userId;
        }
    }
}
