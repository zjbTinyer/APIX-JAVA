package com.apix.common.model;

/**
 * 子 Agent 状态 — 对标 Python: SubAgentState。
 * 在 MainAgentState 基础上增加任务追踪字段。
 */
public class SubAgentState extends MainAgentState {

    private String finalGoal;
    private String taskId;
    private String parentTaskId;
    private long startTimestamp;
    private long finishTimestamp;
    private String status;       // in_progress / done / completed / pending / failed / cancelled
    private String outputs;
    private String errors;

    public String getFinalGoal() { return finalGoal; }
    public void setFinalGoal(String finalGoal) { this.finalGoal = finalGoal; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getParentTaskId() { return parentTaskId; }
    public void setParentTaskId(String parentTaskId) { this.parentTaskId = parentTaskId; }

    public long getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(long startTimestamp) { this.startTimestamp = startTimestamp; }

    public long getFinishTimestamp() { return finishTimestamp; }
    public void setFinishTimestamp(long finishTimestamp) { this.finishTimestamp = finishTimestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOutputs() { return outputs; }
    public void setOutputs(String outputs) { this.outputs = outputs; }

    public String getErrors() { return errors; }
    public void setErrors(String errors) { this.errors = errors; }
}
