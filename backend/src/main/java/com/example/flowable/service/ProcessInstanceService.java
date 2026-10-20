package com.example.flowable.service;

import com.example.flowable.common.BizException;
import com.example.flowable.common.PageResult;
import com.example.flowable.common.ProcessConstants;
import com.example.flowable.dto.ApprovalChainVO;
import com.example.flowable.dto.ApproverVO;
import com.example.flowable.dto.FormInstanceVO;
import com.example.flowable.dto.FormVO;
import com.example.flowable.dto.ProcessFormDataVO;
import com.example.flowable.dto.ProcessInstanceVO;
import com.example.flowable.dto.StartProcessRequest;
import com.example.flowable.entity.SysUser;
import com.example.flowable.mapper.SysUserMapper;
import com.example.flowable.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessInstanceService {

    private final RuntimeService runtimeService;

    private final HistoryService historyService;

    private final RepositoryService repositoryService;

    private final IdentityService identityService;

    private final TaskService taskService;

    private final OrganizationService organizationService;

    private final FormDefinitionService formDefinitionService;

    private final SysUserMapper sysUserMapper;

    private final ObjectMapper objectMapper;

    public ProcessInstanceVO start(StartProcessRequest request) {
        Long userId = SecurityUtils.getUserId();
        Map<String, Object> variables = new HashMap<>(request.getVariables());
        variables.put("initiator", String.valueOf(userId));
        variables.put("applyUserId", userId);
        variables.put("applyUserName", SecurityUtils.getRealName());
        if (ProcessConstants.LEAVE_PROCESS_KEY.equals(request.getProcessDefinitionKey())
                && !variables.containsKey("deptLeaderId")) {
            putLeaveApprovalVariables(variables, userId);
        }

        identityService.setAuthenticatedUserId(String.valueOf(userId));
        try {
            ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                    request.getProcessDefinitionKey(), request.getBusinessKey(), variables);
            return getDetail(instance.getId());
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
    }

    public PageResult<ProcessInstanceVO> pageRunning(int page, int size, String key, String startedBy) {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        if (StringUtils.hasText(key)) {
            query.processDefinitionKey(key);
        }
        if (StringUtils.hasText(startedBy)) {
            query.startedBy(startedBy);
        }
        long total = query.count();
        List<ProcessInstance> instances = query.orderByProcessInstanceId().desc()
                .listPage((page - 1) * size, size);
        List<ProcessInstanceVO> records = instances.stream()
                .map(this::toRunningVO)
                .collect(Collectors.toList());
        return PageResult.of(total, records);
    }

    public PageResult<ProcessInstanceVO> pageMyStarted(int page, int size) {
        Long userId = SecurityUtils.getUserId();
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .startedBy(String.valueOf(userId))
                .orderByProcessInstanceStartTime().desc();
        long total = query.count();
        List<HistoricProcessInstance> instances = query.listPage((page - 1) * size, size);
        List<ProcessInstanceVO> records = instances.stream()
                .map(this::toHistoricVO)
                .collect(Collectors.toList());
        return PageResult.of(total, records);
    }

    public ProcessInstanceVO getDetail(String processInstanceId) {
        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (historic != null) {
            return toHistoricVO(historic);
        }
        ProcessInstance running = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (running == null) {
            throw new BizException("流程实例不存在: " + processInstanceId);
        }
        return toRunningVO(running);
    }

    public List<String> getActiveActivityIds(String processInstanceId) {
        boolean running = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).count() > 0;
        if (!running) {
            return Collections.emptyList();
        }
        return runtimeService.getActiveActivityIds(processInstanceId);
    }

    public void delete(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId,
                StringUtils.hasText(reason) ? reason : "用户主动删除");
    }

    public ProcessFormDataVO getFormData(String processInstanceId) {
        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (historic == null) {
            throw new BizException("流程实例不存在: " + processInstanceId);
        }
        Map<String, Object> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        HistoricVariableInstance::getVariableName,
                        HistoricVariableInstance::getValue,
                        (a, b) -> b));
        Map<String, Object> publicVariables = variables.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("taskFormData_"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
        FormVO startForm = formDefinitionService.resolveByProcessDefinition(historic.getProcessDefinitionId());
        FormInstanceVO start = startForm == null ? null : FormInstanceVO.builder()
                .taskName("发起表单")
                .formKey(startForm.getFormKey())
                .formName(startForm.getName())
                .schemaJson(startForm.getSchemaJson())
                .startTime(historic.getStartTime())
                .endTime(historic.getEndTime())
                .values(publicVariables)
                .build();

        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricTaskInstanceStartTime().asc()
                .list();
        List<FormInstanceVO> taskForms = new ArrayList<>();
        for (HistoricTaskInstance task : tasks) {
            String formKey = task.getFormKey();
            if (!StringUtils.hasText(formKey)) {
                continue;
            }
            FormVO form = formDefinitionService.getByKey(formKey);
            if (form == null) {
                continue;
            }
            taskForms.add(FormInstanceVO.builder()
                    .taskId(task.getId())
                    .taskName(task.getName())
                    .taskDefinitionKey(task.getTaskDefinitionKey())
                    .formKey(formKey)
                    .formName(form.getName())
                    .schemaJson(form.getSchemaJson())
                    .assigneeId(task.getAssignee())
                    .assigneeName(userName(task.getAssignee()))
                    .startTime(task.getStartTime())
                    .endTime(task.getEndTime())
                    .active(false)
                    .canHandle(false)
                    .values(taskValues(processInstanceId, task.getId(), publicVariables))
                    .build());
        }

        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime().asc()
                .list();
        for (Task task : activeTasks) {
            String formKey = task.getFormKey();
            if (!StringUtils.hasText(formKey)) {
                continue;
            }
            FormVO form = formDefinitionService.getByKey(formKey);
            if (form == null) {
                continue;
            }
            taskForms.add(FormInstanceVO.builder()
                    .taskId(task.getId())
                    .taskName(task.getName())
                    .taskDefinitionKey(task.getTaskDefinitionKey())
                    .formKey(formKey)
                    .formName(form.getName())
                    .schemaJson(form.getSchemaJson())
                    .assigneeId(task.getAssignee())
                    .assigneeName(userName(task.getAssignee()))
                    .startTime(task.getCreateTime())
                    .active(true)
                    .canHandle(canHandle(task))
                    .values(publicVariables)
                    .build());
        }
        return ProcessFormDataVO.builder()
                .processInstanceId(processInstanceId)
                .startForm(start)
                .taskForms(taskForms)
                .variables(publicVariables)
                .build();
    }

    private Map<String, Object> taskValues(String processInstanceId, String taskId,
                                           Map<String, Object> fallback) {
        String snapshot = getHistoricVariable(processInstanceId, "taskFormData_" + taskId);
        if (!StringUtils.hasText(snapshot)) {
            return fallback;
        }
        try {
            return objectMapper.readValue(snapshot, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            return fallback;
        }
    }

    private boolean canHandle(Task task) {
        String userId = String.valueOf(SecurityUtils.getUserId());
        if ("admin".equalsIgnoreCase(SecurityUtils.getRole())) {
            return true;
        }
        if (StringUtils.hasText(task.getAssignee())) {
            return userId.equals(task.getAssignee());
        }
        String role = SecurityUtils.getRole();
        if (StringUtils.hasText(role) && taskService.createTaskQuery()
                .taskId(task.getId())
                .taskCandidateGroupIn(List.of(role))
                .count() > 0) {
            return true;
        }
        return taskService.createTaskQuery()
                .taskId(task.getId())
                .taskCandidateUser(userId)
                .count() > 0;
    }

    private String userName(String userId) {
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

    private ProcessInstanceVO toRunningVO(ProcessInstance instance) {
        String startUserName = getHistoricVariable(instance.getId(), "applyUserName");
        List<String> active = runtimeService.getActiveActivityIds(instance.getId());
        return ProcessInstanceVO.builder()
                .id(instance.getId())
                .processDefinitionId(instance.getProcessDefinitionId())
                .processDefinitionKey(instance.getProcessDefinitionKey())
                .processDefinitionName(definitionName(instance.getProcessDefinitionId()))
                .processDefinitionVersion(instance.getProcessDefinitionVersion() == null ? null
                        : String.valueOf(instance.getProcessDefinitionVersion()))
                .businessKey(instance.getBusinessKey())
                .startUserId(instance.getStartUserId())
                .startUserName(startUserName)
                .startTime(instance.getStartTime())
                .ended(false)
                .activeActivityIds(active)
                .build();
    }

    private ProcessInstanceVO toHistoricVO(HistoricProcessInstance historic) {
        String startUserName = getHistoricVariable(historic.getId(), "applyUserName");
        boolean ended = historic.getEndTime() != null;
        List<String> active = ended ? Collections.emptyList() : getActiveActivityIds(historic.getId());
        return ProcessInstanceVO.builder()
                .id(historic.getId())
                .processDefinitionId(historic.getProcessDefinitionId())
                .processDefinitionKey(historic.getProcessDefinitionKey())
                .processDefinitionName(definitionName(historic.getProcessDefinitionId()))
                .processDefinitionVersion(historic.getProcessDefinitionVersion() == null ? null
                        : String.valueOf(historic.getProcessDefinitionVersion()))
                .businessKey(historic.getBusinessKey())
                .startUserId(historic.getStartUserId())
                .startUserName(startUserName)
                .startTime(historic.getStartTime())
                .endTime(historic.getEndTime())
                .durationInMillis(historic.getDurationInMillis())
                .deleteReason(historic.getDeleteReason())
                .ended(ended)
                .activeActivityIds(active)
                .build();
    }

    private String definitionName(String processDefinitionId) {
        if (!StringUtils.hasText(processDefinitionId)) {
            return null;
        }
        ProcessDefinition definition = repositoryService.getProcessDefinition(processDefinitionId);
        return definition == null ? null : definition.getName();
    }

    private String getHistoricVariable(String processInstanceId, String name) {
        var variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(name)
                .singleResult();
        return variable == null || variable.getValue() == null ? null : String.valueOf(variable.getValue());
    }

    private void putLeaveApprovalVariables(Map<String, Object> variables, Long userId) {
        ApprovalChainVO approval = organizationService.resolveApprovalChain(userId, extractDays(variables.get("days")));
        variables.put("deptId", approval.getDeptId());
        variables.put("deptName", approval.getDeptName());
        variables.put("positionName", approval.getPositionName());
        variables.put("multiLevel", Boolean.TRUE.equals(approval.getMultiLevel()));
        ApproverVO departmentLeader = approval.getApprovers().get(0);
        variables.put("deptLeaderId", String.valueOf(departmentLeader.getUserId()));
        variables.put("deptLeaderName", departmentLeader.getUserName());
        if (approval.getApprovers().size() > 1) {
            ApproverVO upperLeader = approval.getApprovers().get(1);
            variables.put("upperLeaderId", String.valueOf(upperLeader.getUserId()));
            variables.put("upperLeaderName", upperLeader.getUserName());
        }
    }

    private int extractDays(Object value) {
        if (value instanceof Number number) {
            return Math.max(number.intValue(), 1);
        }
        if (value == null) {
            return 1;
        }
        try {
            return Math.max(Integer.parseInt(String.valueOf(value)), 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
