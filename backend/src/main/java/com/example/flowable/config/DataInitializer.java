package com.example.flowable.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.flowable.entity.SysDepartment;
import com.example.flowable.entity.SysPosition;
import com.example.flowable.entity.SysUser;
import com.example.flowable.entity.WfForm;
import com.example.flowable.mapper.SysDepartmentMapper;
import com.example.flowable.mapper.SysPositionMapper;
import com.example.flowable.mapper.SysUserMapper;
import com.example.flowable.mapper.WfFormMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "123456";

    private static final String LEAVE_START_FORM = """
            [
              {"key":"reason","label":"请假事由","type":"textarea","required":true,"placeholder":"请输入请假事由","span":2},
              {"key":"days","label":"请假天数","type":"number","required":true,"min":1,"max":365,"defaultValue":1},
              {"key":"startDate","label":"开始日期","type":"date","required":true},
              {"key":"endDate","label":"结束日期","type":"date","required":true}
            ]
            """;

    private static final String LEAVE_APPROVAL_FORM = """
            [
              {"key":"approved","label":"审批结论","type":"radio","required":true,"valueType":"boolean","span":2,"options":[{"label":"同意","value":"true"},{"label":"驳回","value":"false"}]},
              {"key":"comment","label":"审批意见","type":"textarea","span":2,"placeholder":"请输入审批意见(可选)"}
            ]
            """;

    private final SysUserMapper sysUserMapper;

    private final SysDepartmentMapper sysDepartmentMapper;

    private final SysPositionMapper sysPositionMapper;

    private final WfFormMapper wfFormMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        assertSchema();
        Map<String, SysPosition> positions = initPositions();
        Map<String, SysDepartment> departments = initDepartments();
        initUsers(positions, departments);
        initDepartmentLeaders(departments);
        initForms();
    }

    private void assertSchema() {
        try {
            sysDepartmentMapper.selectCount(null);
            sysPositionMapper.selectCount(null);
            sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery().last("LIMIT 1"));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "组织架构表或字段缺失，请先执行 backend/sql/upgrade-org.sql 或重新执行 backend/sql/init.sql", e);
        }
    }

    private Map<String, SysPosition> initPositions() {
        Map<String, SysPosition> positions = new HashMap<>();
        positions.put("P001", ensurePosition("P001", "董事长", 1, 1));
        positions.put("P002", ensurePosition("P002", "总经理", 2, 2));
        positions.put("P003", ensurePosition("P003", "部门经理", 3, 3));
        positions.put("P004", ensurePosition("P004", "主管", 4, 4));
        positions.put("P005", ensurePosition("P005", "高级工程师", 5, 5));
        positions.put("P006", ensurePosition("P006", "工程师", 6, 6));
        positions.put("P007", ensurePosition("P007", "专员", 7, 7));
        positions.put("P008", ensurePosition("P008", "系统管理员", 8, 8));
        positions.put("P009", ensurePosition("P009", "业务员", 9, 9));
        positions.put("P010", ensurePosition("P010", "财务专员", 10, 10));
        positions.put("P011", ensurePosition("P011", "顾问主管", 11, 11));
        positions.put("P012", ensurePosition("P012", "顾问", 12, 12));
        positions.put("P013", ensurePosition("P013", "会计主管", 13, 13));
        positions.put("P014", ensurePosition("P014", "会计", 14, 14));
        log.info("岗位初始化完成, 共 {} 个", positions.size());
        return positions;
    }

    private Map<String, SysDepartment> initDepartments() {
        Map<String, SysDepartment> departments = new HashMap<>();
        SysDepartment group = ensureDepartment("D001", "集团总部", null, 1);
        SysDepartment company = ensureDepartment("D002", "科技公司", group.getId(), 2);
        departments.put("D001", group);
        departments.put("D002", company);
        departments.put("D003", ensureDepartment("D003", "技术部", company.getId(), 3));
        departments.put("D004", ensureDepartment("D004", "人事部", company.getId(), 4));
        departments.put("D005", ensureDepartment("D005", "财务部", company.getId(), 5));
        departments.put("D006", ensureDepartment("D006", "业务部", company.getId(), 6));
        departments.put("D007", ensureDepartment("D007", "顾问部", company.getId(), 7));
        departments.put("D008", ensureDepartment("D008", "会计部", company.getId(), 8));
        log.info("部门初始化完成, 共 {} 个", departments.size());
        return departments;
    }

    private void initUsers(Map<String, SysPosition> positions, Map<String, SysDepartment> departments) {
        ensureUser("admin", "系统管理员", "admin", departments.get("D002"), positions.get("P008"));
        ensureUser("dongshizhang", "陈建国", "executive", departments.get("D001"), positions.get("P001"));
        ensureUser("zongjingli", "周明", "executive", departments.get("D002"), positions.get("P002"));
        ensureUser("manager", "王强", "manager", departments.get("D003"), positions.get("P003"));
        ensureUser("hrManager", "李芳", "manager", departments.get("D004"), positions.get("P003"));
        ensureUser("caiwuManager", "赵磊", "manager", departments.get("D005"), positions.get("P003"));
        ensureUser("hr", "刘敏", "hr", departments.get("D004"), positions.get("P007"));
        ensureUser("zhangsan", "张三", "employee", departments.get("D003"), positions.get("P006"));
        ensureUser("lisi", "李四", "employee", departments.get("D003"), positions.get("P005"));
        ensureUser("wangwu", "王五", "employee", departments.get("D004"), positions.get("P007"));
        ensureUser("ywy", "李业务", "sales", departments.get("D006"), positions.get("P009"));
        ensureUser("caiwu", "陈财务", "finance", departments.get("D005"), positions.get("P010"));
        ensureUser("gws", "周顾问主管", "consultant_manager", departments.get("D007"), positions.get("P011"));
        ensureUser("gw", "林顾问", "consultant", departments.get("D007"), positions.get("P012"));
        ensureUser("kjs", "吴会计主管", "accounting_manager", departments.get("D008"), positions.get("P013"));
        ensureUser("kj", "赵会计", "accountant", departments.get("D008"), positions.get("P014"));
        log.info("默认用户初始化完成, 默认密码: {}", DEFAULT_PASSWORD);
    }

    private void initDepartmentLeaders(Map<String, SysDepartment> departments) {
        setLeaderIfAbsent(departments.get("D001"), "dongshizhang");
        setLeaderIfAbsent(departments.get("D002"), "zongjingli");
        setLeaderIfAbsent(departments.get("D003"), "manager");
        setLeaderIfAbsent(departments.get("D004"), "hrManager");
        setLeaderIfAbsent(departments.get("D005"), "caiwuManager");
        setLeaderIfAbsent(departments.get("D006"), "ywy");
        setLeaderIfAbsent(departments.get("D007"), "gws");
        setLeaderIfAbsent(departments.get("D008"), "kjs");
        log.info("部门负责人初始化完成");
    }

    private SysPosition ensurePosition(String code, String name, int level, int sort) {
        SysPosition position = sysPositionMapper.selectOne(Wrappers.<SysPosition>lambdaQuery()
                .eq(SysPosition::getPositionCode, code));
        if (position != null) {
            return position;
        }
        position = new SysPosition();
        position.setPositionCode(code);
        position.setPositionName(name);
        position.setPositionLevel(level);
        position.setSort(sort);
        position.setStatus(1);
        position.setDeleted(0);
        position.setCreateTime(LocalDateTime.now());
        position.setUpdateTime(LocalDateTime.now());
        sysPositionMapper.insert(position);
        return position;
    }

    private SysDepartment ensureDepartment(String code, String name, Long parentId, int sort) {
        SysDepartment department = sysDepartmentMapper.selectOne(Wrappers.<SysDepartment>lambdaQuery()
                .eq(SysDepartment::getDeptCode, code));
        if (department != null) {
            return department;
        }
        department = new SysDepartment();
        department.setDeptCode(code);
        department.setDeptName(name);
        department.setParentId(parentId);
        department.setSort(sort);
        department.setStatus(1);
        department.setDeleted(0);
        department.setCreateTime(LocalDateTime.now());
        department.setUpdateTime(LocalDateTime.now());
        sysDepartmentMapper.insert(department);
        return department;
    }

    private SysUser ensureUser(String username, String realName, String role,
                               SysDepartment department, SysPosition position) {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            user.setRealName(realName);
            user.setRole(role);
            user.setDeptId(department == null ? null : department.getId());
            user.setPositionId(position == null ? null : position.getId());
            user.setEmail(username + "@example.com");
            user.setStatus(1);
            user.setDeleted(0);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            sysUserMapper.insert(user);
            return user;
        }

        boolean changed = false;
        if (user.getDeptId() == null && department != null) {
            user.setDeptId(department.getId());
            changed = true;
        }
        if (user.getPositionId() == null && position != null) {
            user.setPositionId(position.getId());
            changed = true;
        }
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole(role);
            changed = true;
        }
        if (changed) {
            user.setUpdateTime(LocalDateTime.now());
            sysUserMapper.updateById(user);
        }
        return user;
    }

    private void setLeaderIfAbsent(SysDepartment department, String leaderUsername) {
        if (department == null || department.getLeaderId() != null) {
            return;
        }
        SysUser leader = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, leaderUsername));
        if (leader == null) {
            log.warn("部门 [{}] 的负责人 [{}] 不存在, 跳过设置", department.getDeptName(), leaderUsername);
            return;
        }
        department.setLeaderId(leader.getId());
        department.setUpdateTime(LocalDateTime.now());
        sysDepartmentMapper.updateById(department);
    }

    private void initForms() {
        ensureForm("leaveStartForm", "请假申请表单", "请假", LEAVE_START_FORM);
        ensureForm("leaveApprovalForm", "请假审批表单", "请假", LEAVE_APPROVAL_FORM);
        ensureForm("contractHandoverStartForm", "记账合同交接-资料提交", "记账合同",
                readResource("forms/contract-handover-start.json"));
        ensureForm("contractSupplementForm", "记账合同交接-补充资料", "记账合同",
                readResource("forms/contract-supplement.json"));
        ensureForm("contractFinanceReviewForm", "记账合同交接-财务审核", "记账合同",
                readResource("forms/contract-finance-review.json"));
        ensureForm("financeDispatchForm", "记账合同交接-财务派工", "记账合同",
                readResource("forms/finance-dispatch.json"));
        ensureForm("consultantDispatchForm", "记账合同交接-顾问主管派工", "记账合同",
                readResource("forms/consultant-dispatch.json"));
        ensureForm("consultantReviewForm", "记账合同交接-顾问审核", "记账合同",
                readResource("forms/consultant-review.json"));
        ensureForm("accountingDispatchForm", "记账合同交接-会计主管派工", "记账合同",
                readResource("forms/accounting-dispatch.json"));
        ensureForm("accountingReviewForm", "记账合同交接-会计审核", "记账合同",
                readResource("forms/accounting-review.json"));
        ensureForm("contractFinanceFinalForm", "记账合同交接-财务最终确认", "记账合同",
                readResource("forms/contract-finance-final.json"));
        log.info("表单初始化完成");
    }

    private void ensureForm(String formKey, String name, String category, String schemaJson) {
        WfForm existing = wfFormMapper.selectOne(Wrappers.<WfForm>lambdaQuery()
                .eq(WfForm::getFormKey, formKey));
        if (existing != null) {
            return;
        }
        createForm(formKey, name, category, schemaJson);
    }

    private String readResource(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取表单定义失败: " + path, e);
        }
    }

    private void createForm(String formKey, String name, String category, String schemaJson) {
        WfForm form = new WfForm();
        form.setFormKey(formKey);
        form.setName(name);
        form.setCategory(category);
        form.setSchemaJson(schemaJson.trim());
        form.setStatus(1);
        form.setDeleted(0);
        form.setCreateTime(LocalDateTime.now());
        form.setUpdateTime(LocalDateTime.now());
        wfFormMapper.insert(form);
    }
}
