package com.apix.common.model;

import java.util.Map;

/**
 * 事件包 — Agent 和前端之间的通信协议单元。
 * 对标 Python: ApixEventEnvelope
 */
public class ApixEventEnvelope {

    private String event;
    private ApixEventEnvelopeTarget target;
    private Map<String, Object> data;
    private String generationId;
    private double timestamp;

    public ApixEventEnvelope() {}

    public ApixEventEnvelope(String event, ApixEventEnvelopeTarget target,
                             Map<String, Object> data, String generationId, double timestamp) {
        this.event = event;
        this.target = target;
        this.data = data;
        this.generationId = generationId;
        this.timestamp = timestamp;
    }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public ApixEventEnvelopeTarget getTarget() { return target; }
    public void setTarget(ApixEventEnvelopeTarget target) { this.target = target; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public String getGenerationId() { return generationId; }
    public void setGenerationId(String generationId) { this.generationId = generationId; }

    public double getTimestamp() { return timestamp; }
    public void setTimestamp(double timestamp) { this.timestamp = timestamp; }
}
