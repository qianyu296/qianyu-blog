package com.study.qianyu_blog.dto;

public class AiChatResponse {
    private String reply;
    private String model;

    public AiChatResponse(String reply, String model) {
        this.reply = reply;
        this.model = model;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
