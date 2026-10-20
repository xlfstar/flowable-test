package com.example.flowable.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.flowable.common.BizException;
import com.example.flowable.common.PageResult;
import com.example.flowable.dto.FormSaveRequest;
import com.example.flowable.dto.FormVO;
import com.example.flowable.entity.WfForm;
import com.example.flowable.mapper.WfFormMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.FormService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormDefinitionService {

    private final WfFormMapper wfFormMapper;

    private final TaskService taskService;

    private final FormService formService;

    public PageResult<FormVO> page(int page, int size, String name, String key) {
        var wrapper = Wrappers.<WfForm>lambdaQuery()
                .like(StringUtils.hasText(name), WfForm::getName, name)
                .like(StringUtils.hasText(key), WfForm::getFormKey, key)
                .orderByDesc(WfForm::getUpdateTime);
        IPage<WfForm> result = wfFormMapper.selectPage(new Page<>(page, size), wrapper);
        List<FormVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(result.getTotal(), records);
    }

    public List<FormVO> listEnabled() {
        List<WfForm> forms = wfFormMapper.selectList(Wrappers.<WfForm>lambdaQuery()
                .eq(WfForm::getStatus, 1)
                .orderByDesc(WfForm::getUpdateTime));
        return forms.stream().map(this::toVO).collect(Collectors.toList());
    }

    public FormVO get(Long id) {
        WfForm entity = wfFormMapper.selectById(id);
        if (entity == null) {
            throw new BizException("表单不存在: " + id);
        }
        return toVO(entity);
    }

    public FormVO getByKey(String formKey) {
        if (!StringUtils.hasText(formKey)) {
            return null;
        }
        WfForm entity = findByKey(formKey);
        return entity == null ? null : toVO(entity);
    }

    public FormVO create(FormSaveRequest request) {
        if (findByKey(request.getFormKey()) != null) {
            throw new BizException("表单标识已存在: " + request.getFormKey());
        }
        WfForm entity = new WfForm();
        apply(entity, request);
        entity.setDeleted(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        wfFormMapper.insert(entity);
        return toVO(entity);
    }

    public FormVO update(Long id, FormSaveRequest request) {
        WfForm entity = wfFormMapper.selectById(id);
        if (entity == null) {
            throw new BizException("表单不存在: " + id);
        }
        WfForm sameKey = findByKey(request.getFormKey());
        if (sameKey != null && !sameKey.getId().equals(id)) {
            throw new BizException("表单标识已存在: " + request.getFormKey());
        }
        apply(entity, request);
        entity.setUpdateTime(LocalDateTime.now());
        wfFormMapper.updateById(entity);
        return toVO(entity);
    }

    public void delete(Long id) {
        wfFormMapper.deleteById(id);
    }

    public FormVO resolveByTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return null;
        }
        return getByKey(task.getFormKey());
    }

    public FormVO resolveByProcessDefinition(String processDefinitionId) {
        if (!StringUtils.hasText(processDefinitionId)) {
            return null;
        }
        String formKey;
        try {
            formKey = formService.getStartFormKey(processDefinitionId);
        } catch (RuntimeException e) {
            return null;
        }
        return getByKey(formKey);
    }

    private WfForm findByKey(String formKey) {
        return wfFormMapper.selectOne(Wrappers.<WfForm>lambdaQuery()
                .eq(WfForm::getFormKey, formKey)
                .last("limit 1"));
    }

    private void apply(WfForm entity, FormSaveRequest request) {
        entity.setFormKey(request.getFormKey());
        entity.setName(request.getName());
        entity.setCategory(request.getCategory());
        entity.setSchemaJson(StringUtils.hasText(request.getSchemaJson()) ? request.getSchemaJson() : "[]");
        entity.setRemark(request.getRemark());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private FormVO toVO(WfForm entity) {
        return FormVO.builder()
                .id(entity.getId())
                .formKey(entity.getFormKey())
                .name(entity.getName())
                .category(entity.getCategory())
                .schemaJson(entity.getSchemaJson())
                .remark(entity.getRemark())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
