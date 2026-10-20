package com.example.flowable.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.flowable.common.BizException;
import com.example.flowable.dto.ApprovalChainVO;
import com.example.flowable.dto.ApproverVO;
import com.example.flowable.dto.DepartmentVO;
import com.example.flowable.dto.OrgUserVO;
import com.example.flowable.dto.PositionVO;
import com.example.flowable.entity.SysDepartment;
import com.example.flowable.entity.SysPosition;
import com.example.flowable.entity.SysUser;
import com.example.flowable.mapper.SysDepartmentMapper;
import com.example.flowable.mapper.SysPositionMapper;
import com.example.flowable.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 组织架构与请假审批人解析。
 *
 * <p>审批人解析规则:</p>
 * <ol>
 *     <li>第一审批人: 申请人所在部门的负责人 (leader_id)</li>
 *     <li>若申请人本人就是部门负责人, 跳过本人, 自动由上级部门负责人审批</li>
 *     <li>请假天数 &gt; 3 天时, 增加第二审批人: 部门树上再上一级的负责人</li>
 *     <li>停用/不存在的负责人会被跳过, 沿部门树继续向上查找</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {

    /**
     * 超过该天数需要增加一级审批
     */
    public static final int MULTI_LEVEL_DAYS = 3;

    private static final int MAX_DEPARTMENT_DEPTH = 20;

    private final SysDepartmentMapper departmentMapper;

    private final SysPositionMapper positionMapper;

    private final SysUserMapper userMapper;

    public List<DepartmentVO> departmentTree() {
        List<SysDepartment> departments = departmentMapper.selectList(
                Wrappers.<SysDepartment>lambdaQuery()
                        .orderByAsc(SysDepartment::getSort)
                        .orderByAsc(SysDepartment::getId));
        Map<Long, SysUser> leaders = loadUsers(departments.stream()
                .map(SysDepartment::getLeaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, DepartmentVO> voMap = new LinkedHashMap<>();
        for (SysDepartment department : departments) {
            SysUser leader = department.getLeaderId() == null ? null : leaders.get(department.getLeaderId());
            voMap.put(department.getId(), DepartmentVO.builder()
                    .id(department.getId())
                    .deptCode(department.getDeptCode())
                    .deptName(department.getDeptName())
                    .parentId(department.getParentId())
                    .leaderId(department.getLeaderId())
                    .leaderName(leader == null ? null : displayName(leader))
                    .sort(department.getSort())
                    .status(department.getStatus())
                    .children(new ArrayList<>())
                    .build());
        }
        List<DepartmentVO> roots = new ArrayList<>();
        for (SysDepartment department : departments) {
            DepartmentVO current = voMap.get(department.getId());
            DepartmentVO parent = department.getParentId() == null ? null : voMap.get(department.getParentId());
            if (parent == null) {
                roots.add(current);
            } else {
                parent.getChildren().add(current);
            }
        }
        return roots;
    }

    public List<PositionVO> positions() {
        return positionMapper.selectList(Wrappers.<SysPosition>lambdaQuery()
                        .orderByAsc(SysPosition::getPositionLevel)
                        .orderByAsc(SysPosition::getSort)
                        .orderByAsc(SysPosition::getId))
                .stream()
                .map(position -> PositionVO.builder()
                        .id(position.getId())
                        .positionCode(position.getPositionCode())
                        .positionName(position.getPositionName())
                        .positionLevel(position.getPositionLevel())
                        .sort(position.getSort())
                        .status(position.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    public List<OrgUserVO> users() {
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .orderByAsc(SysUser::getDeptId)
                .orderByAsc(SysUser::getId));
        Map<Long, SysDepartment> departments = departmentMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDepartment::getId, Function.identity(), (a, b) -> a));
        Map<Long, SysPosition> positions = positionMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysPosition::getId, Function.identity(), (a, b) -> a));
        return users.stream()
                .map(user -> {
                    SysDepartment department = user.getDeptId() == null ? null : departments.get(user.getDeptId());
                    SysPosition position = user.getPositionId() == null ? null : positions.get(user.getPositionId());
                    return OrgUserVO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .realName(user.getRealName())
                            .role(user.getRole())
                            .deptId(user.getDeptId())
                            .deptName(department == null ? null : department.getDeptName())
                            .positionId(user.getPositionId())
                            .positionName(position == null ? null : position.getPositionName())
                            .departmentLeader(department != null && user.getId().equals(department.getLeaderId()))
                            .status(user.getStatus())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 预览当前用户按指定天数需要的审批路径。组织配置不足时返回 valid=false, 不抛异常。
     */
    public ApprovalChainVO previewApprovalChain(Long applicantId, int days) {
        int normalizedDays = Math.max(days, 1);
        boolean multiLevel = normalizedDays > MULTI_LEVEL_DAYS;
        try {
            SysUser applicant = requireApplicant(applicantId);
            SysDepartment department = requireDepartment(applicant);
            List<ApproverVO> fullChain = buildApprovalChain(applicant, department);
            int requiredLevels = multiLevel ? 2 : 1;
            if (fullChain.size() < requiredLevels) {
                String message = multiLevel
                        ? "请假超过" + MULTI_LEVEL_DAYS + "天需要上级领导审批，但【"
                        + department.getDeptName() + "】的上级部门未配置有效负责人"
                        : "未找到有效的审批领导，请联系管理员配置部门负责人";
                return invalidChain(normalizedDays, multiLevel, message);
            }
            return ApprovalChainVO.builder()
                    .days(normalizedDays)
                    .multiLevel(multiLevel)
                    .valid(true)
                    .applicantId(applicant.getId())
                    .applicantName(displayName(applicant))
                    .deptId(department.getId())
                    .deptName(department.getDeptName())
                    .positionName(positionName(applicant.getPositionId()))
                    .approvers(new ArrayList<>(fullChain.subList(0, requiredLevels)))
                    .build();
        } catch (BizException e) {
            return invalidChain(normalizedDays, multiLevel, e.getMessage());
        }
    }

    /**
     * 发起流程前解析审批链。配置不足时抛出业务异常, 避免产生无法审批的流程实例。
     */
    public ApprovalChainVO resolveApprovalChain(Long applicantId, int days) {
        ApprovalChainVO chain = previewApprovalChain(applicantId, days);
        if (!Boolean.TRUE.equals(chain.getValid())) {
            throw new BizException(StringUtils.hasText(chain.getMessage())
                    ? chain.getMessage() : "审批人解析失败，请联系管理员");
        }
        return chain;
    }

    /**
     * 解析申请人所在部门负责人；申请人本人是负责人时自动向上级部门查找。
     */
    public ApproverVO resolveDepartmentLeader(Long applicantId) {
        return resolveApprovalChain(applicantId, 1).getApprovers().get(0);
    }

    /**
     * 解析申请人所在部门的上一级负责人。
     */
    public ApproverVO resolveUpperDepartmentLeader(Long applicantId) {
        ApprovalChainVO chain = resolveApprovalChain(applicantId, MULTI_LEVEL_DAYS + 1);
        if (chain.getApprovers().size() < 2) {
            throw new BizException("未配置上级部门负责人，无法使用“上级部门领导”审批人");
        }
        return chain.getApprovers().get(1);
    }

    /**
     * 按岗位解析审批人。
     *
     * @param sameDepartmentOnly true 时只匹配申请人所在部门，适合“部门经理”这类跨部门复用的岗位
     */
    public ApproverVO resolvePositionApprover(Long applicantId, String positionCode, boolean sameDepartmentOnly) {
        if (!StringUtils.hasText(positionCode)) {
            throw new BizException("请选择审批岗位");
        }
        SysPosition position = positionMapper.selectOne(Wrappers.<SysPosition>lambdaQuery()
                .eq(SysPosition::getPositionCode, positionCode));
        if (position == null) {
            throw new BizException("岗位不存在: " + positionCode);
        }
        if (position.getStatus() != null && position.getStatus() != 1) {
            throw new BizException("岗位已停用: " + positionCode);
        }
        SysUser applicant = requireApplicant(applicantId);
        List<SysUser> candidates = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getPositionId, position.getId())
                .eq(SysUser::getStatus, 1));
        if (sameDepartmentOnly) {
            candidates = candidates.stream()
                    .filter(user -> Objects.equals(user.getDeptId(), applicant.getDeptId()))
                    .collect(Collectors.toList());
        }
        if (candidates.isEmpty()) {
            String scope = sameDepartmentOnly ? "本部门" : "";
            throw new BizException("岗位【" + position.getPositionName() + "】" + scope + "没有可用审批人");
        }
        if (candidates.size() > 1) {
            String suggestion = sameDepartmentOnly
                    ? "该岗位在本部门仍有多人，请改为指定用户"
                    : "请改用“本部门指定岗位”或指定用户";
            throw new BizException("岗位【" + position.getPositionName() + "】存在多个人员，" + suggestion);
        }
        SysUser approver = candidates.get(0);
        if (approver.getId().equals(applicantId)) {
            throw new BizException("岗位审批人不能是申请人本人");
        }
        SysDepartment department = approver.getDeptId() == null
                ? null : departmentMapper.selectById(approver.getDeptId());
        return ApproverVO.builder()
                .userId(approver.getId())
                .userName(displayName(approver))
                .deptId(approver.getDeptId())
                .deptName(department == null ? null : department.getDeptName())
                .positionName(position.getPositionName())
                .level(1)
                .levelName("岗位审批")
                .build();
    }

    public String departmentName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        SysDepartment department = departmentMapper.selectById(deptId);
        return department == null ? null : department.getDeptName();
    }

    public String positionName(Long positionId) {
        if (positionId == null) {
            return null;
        }
        SysPosition position = positionMapper.selectById(positionId);
        return position == null ? null : position.getPositionName();
    }

    private SysUser requireApplicant(Long applicantId) {
        SysUser applicant = userMapper.selectById(applicantId);
        if (applicant == null) {
            throw new BizException("申请人不存在");
        }
        if (!isActive(applicant)) {
            throw new BizException("申请人账号已被禁用，无法发起流程");
        }
        if (applicant.getDeptId() == null) {
            throw new BizException("当前用户未分配部门，无法发起请假流程，请联系管理员");
        }
        return applicant;
    }

    private SysDepartment requireDepartment(SysUser applicant) {
        SysDepartment department = departmentMapper.selectById(applicant.getDeptId());
        if (department == null) {
            throw new BizException("当前用户所在部门不存在，请联系管理员");
        }
        return department;
    }

    private List<ApproverVO> buildApprovalChain(SysUser applicant, SysDepartment startDepartment) {
        if (startDepartment.getLeaderId() == null) {
            throw new BizException("您所在部门【" + startDepartment.getDeptName()
                    + "】未配置部门领导，请联系管理员");
        }
        List<ApproverVO> chain = new ArrayList<>();
        Set<Long> visitedDepartmentIds = new HashSet<>();
        Set<Long> addedLeaderIds = new HashSet<>();
        Long currentDepartmentId = startDepartment.getId();
        int depth = 0;
        while (currentDepartmentId != null) {
            depth++;
            if (depth > MAX_DEPARTMENT_DEPTH || !visitedDepartmentIds.add(currentDepartmentId)) {
                throw new BizException("部门层级配置存在循环或层级过深，请联系管理员");
            }
            SysDepartment department = currentDepartmentId.equals(startDepartment.getId())
                    ? startDepartment
                    : departmentMapper.selectById(currentDepartmentId);
            if (department == null) {
                break;
            }
            Long leaderId = department.getLeaderId();
            if (leaderId != null && !leaderId.equals(applicant.getId()) && addedLeaderIds.add(leaderId)) {
                SysUser leader = userMapper.selectById(leaderId);
                if (isActive(leader)) {
                    int level = chain.size() + 1;
                    chain.add(ApproverVO.builder()
                            .userId(leader.getId())
                            .userName(displayName(leader))
                            .deptId(department.getId())
                            .deptName(department.getDeptName())
                            .positionName(positionName(leader.getPositionId()))
                            .level(level)
                            .levelName(levelName(level))
                            .build());
                }
            }
            currentDepartmentId = department.getParentId();
        }
        return chain;
    }

    private Map<Long, SysUser> loadUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
    }

    private boolean isActive(SysUser user) {
        return user != null && (user.getStatus() == null || user.getStatus() == 1);
    }

    private String displayName(SysUser user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private String levelName(int level) {
        if (level == 1) {
            return "部门领导";
        }
        if (level == 2) {
            return "上级领导";
        }
        return "第" + level + "级领导";
    }

    private ApprovalChainVO invalidChain(int days, boolean multiLevel, String message) {
        return ApprovalChainVO.builder()
                .days(days)
                .multiLevel(multiLevel)
                .valid(false)
                .message(message)
                .approvers(List.of())
                .build();
    }
}
