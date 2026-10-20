package com.example.flowable.service;

import com.example.flowable.common.BizException;
import com.example.flowable.common.PageResult;
import com.example.flowable.dto.ProcessDefinitionVO;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessDefinitionService {

    private final RepositoryService repositoryService;

    public PageResult<ProcessDefinitionVO> page(int page, int size, String name, String key) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().latestVersion();
        if (StringUtils.hasText(name)) {
            query.processDefinitionNameLike("%" + name + "%");
        }
        if (StringUtils.hasText(key)) {
            query.processDefinitionKeyLike("%" + key + "%");
        }
        long total = query.count();
        List<ProcessDefinition> definitions = query.orderByProcessDefinitionKey().asc()
                .listPage((page - 1) * size, size);

        Map<String, Date> deployTimes = repositoryService.createDeploymentQuery().list().stream()
                .collect(Collectors.toMap(Deployment::getId, Deployment::getDeploymentTime, (a, b) -> a));

        List<ProcessDefinitionVO> records = definitions.stream()
                .map(def -> toVO(def, deployTimes.get(def.getDeploymentId())))
                .collect(Collectors.toList());
        return PageResult.of(total, records);
    }

    public ProcessDefinitionVO deploy(MultipartFile file, String deploymentName) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            filename = "process.bpmn20.xml";
        } else if (!filename.endsWith(".bpmn20.xml") && !filename.endsWith(".bpmn")
                && !filename.endsWith(".zip")) {
            filename = filename + ".bpmn20.xml";
        }
        try (InputStream in = file.getInputStream()) {
            Deployment deployment = repositoryService.createDeployment()
                    .name(StringUtils.hasText(deploymentName) ? deploymentName : filename)
                    .addInputStream(filename, in)
                    .deploy();
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();
            if (definition == null) {
                throw new BizException("部署成功但未找到流程定义, 请检查 BPMN 文件");
            }
            return toVO(definition, deployment.getDeploymentTime());
        } catch (IOException e) {
            throw new BizException("读取上传文件失败: " + e.getMessage());
        }
    }

    public void deleteDeployment(String deploymentId, boolean cascade) {
        repositoryService.deleteDeployment(deploymentId, cascade);
    }

    public void suspend(String processDefinitionId) {
        repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);
    }

    public void activate(String processDefinitionId) {
        repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);
    }

    public String getXml(String processDefinitionId) {
        try (InputStream in = repositoryService.getProcessModel(processDefinitionId)) {
            if (in == null) {
                throw new BizException("未找到流程定义 XML");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("读取流程定义 XML 失败: " + e.getMessage());
        }
    }

    public byte[] getDiagram(String processDefinitionId) {
        ProcessDefinition definition = repositoryService.getProcessDefinition(processDefinitionId);
        if (definition == null || definition.getDiagramResourceName() == null) {
            return null;
        }
        try (InputStream in = repositoryService.getResourceAsStream(
                definition.getDeploymentId(), definition.getDiagramResourceName())) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            throw new BizException("读取流程图失败: " + e.getMessage());
        }
    }

    private ProcessDefinitionVO toVO(ProcessDefinition def, Date deployTime) {
        return ProcessDefinitionVO.builder()
                .id(def.getId())
                .key(def.getKey())
                .name(def.getName())
                .version(def.getVersion())
                .deploymentId(def.getDeploymentId())
                .resourceName(def.getResourceName())
                .diagramResourceName(def.getDiagramResourceName())
                .category(def.getCategory())
                .description(def.getDescription())
                .suspensionState(def.isSuspended() ? 2 : 1)
                .suspended(def.isSuspended())
                .deployTime(deployTime)
                .build();
    }
}
