package com.example.flowable.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.flowable.common.BizException;
import com.example.flowable.entity.SysDepartment;
import com.example.flowable.entity.SysUser;
import com.example.flowable.mapper.SysDepartmentMapper;
import com.example.flowable.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程设计器可配置审批人的统一解析入口。
 *
 * <p>BPMN 中通过表达式调用，例如:</p>
 * <pre>
 * ${approverResolver.deptLeader(applyUserId)}
 * ${approverResolver.upperDeptLeader(applyUserId)}
 * ${approverResolver.position(applyUserId, 'FINANCE_MANAGER')}
 * ${approverResolver.positionInDept(applyUserId, 'P003')}
 * ${approverResolver.user(approverId)}
 * </pre>
 */
@Component("approverResolver")
@RequiredArgsConstructor
public class ApproverResolver {

    private final OrganizationService organizationService;

    private final SysUserMapper userMapper;

    private final SysDepartmentMapper departmentMapper;

    public String user(Object userId) {
        SysUser user = requireUser(userId);
        return String.valueOf(user.getId());
    }

    public String deptLeader(Object applicantId) {
        return String.valueOf(organizationService
                .resolveDepartmentLeader(toUserId(applicantId)).getUserId());
    }

    public String upperDeptLeader(Object applicantId) {
        return String.valueOf(organizationService
                .resolveUpperDepartmentLeader(toUserId(applicantId)).getUserId());
    }

    public String position(Object applicantId, String positionCode) {
        return String.valueOf(organizationService
                .resolvePositionApprover(toUserId(applicantId), positionCode, false).getUserId());
    }

    public String positionInDept(Object applicantId, String positionCode) {
        return String.valueOf(organizationService
                .resolvePositionApprover(toUserId(applicantId), positionCode, true).getUserId());
    }

    /**
     * 申请人所在部门的全部有效用户（排除申请人本人），用于部门或签/会签。
     */
    public List<String> departmentUserIds(Object applicantId) {
        SysUser applicant = requireUser(applicantId);
        if (applicant.getDeptId() == null) {
            throw new BizException("申请人未分配部门，无法按部门审批");
        }
        return departmentUserIds(applicant.getDeptId(), applicant.getId());
    }

    /**
     * 指定部门的全部有效用户（排除申请人本人），用于指定部门或签/会签。
     */
    public List<String> departmentUserIdsByCode(String deptCode, Object applicantId) {
        if (!StringUtils.hasText(deptCode)) {
            throw new BizException("请选择审批部门");
        }
        SysDepartment department = departmentMapper.selectOne(Wrappers.<SysDepartment>lambdaQuery()
                .eq(SysDepartment::getDeptCode, deptCode));
        if (department == null) {
            throw new BizException("部门不存在: " + deptCode);
        }
        if (department.getStatus() != null && department.getStatus() != 1) {
            throw new BizException("部门已停用: " + deptCode);
        }
        Long excludeUserId = applicantId == null ? null : toUserId(applicantId);
        return departmentUserIds(department.getId(), excludeUserId);
    }

    private List<String> departmentUserIds(Long deptId, Long excludeUserId) {
        List<String> userIds = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getDeptId, deptId)
                        .eq(SysUser::getStatus, 1))
                .stream()
                .filter(user -> excludeUserId == null || !excludeUserId.equals(user.getId()))
                .map(user -> String.valueOf(user.getId()))
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            throw new BizException("该部门没有其他可用的审批人员");
        }
        return userIds;
    }

    private SysUser requireUser(Object userId) {
        Long id = toUserId(userId);
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("审批人不存在: " + id);
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BizException("审批人已被禁用: " + id);
        }
        return user;
    }

    private Long toUserId(Object value) {
        if (value == null) {
            throw new BizException("审批人参数不能为空");
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            throw new BizException("审批人参数不能为空");
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            throw new BizException("审批人参数不是有效的用户ID: " + text);
        }
    }
}
