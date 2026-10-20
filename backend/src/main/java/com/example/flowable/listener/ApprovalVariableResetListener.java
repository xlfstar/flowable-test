package com.example.flowable.listener;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * 每次进入审批节点时清理上一次审批结果，避免“仅完成任务”时沿用上一级领导的 approved 变量。
 */
@Component("approvalVariableResetListener")
public class ApprovalVariableResetListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.removeVariable("approved");
        delegateTask.removeVariable("comment");
    }
}
