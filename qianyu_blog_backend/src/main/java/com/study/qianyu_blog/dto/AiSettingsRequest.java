package com.study.qianyu_blog.dto;

public class AiSettingsRequest {
    private String chatApiKey;
    private String imageApiKey;
    private String apiEndpoint;
    private String imageApiEndpoint;
    private String modelName;
    private String imageModelName;
    private Boolean enabled;
    private Integer maxTokens;

    public String getChatApiKey() { return chatApiKey; }
    public void setChatApiKey(String chatApiKey) { this.chatApiKey = chatApiKey; }
    public String getImageApiKey() { return imageApiKey; }
    public void setImageApiKey(String imageApiKey) { this.imageApiKey = imageApiKey; }
    public String getApiEndpoint() { return apiEndpoint; }
    public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }
    public String getImageApiEndpoint() { return imageApiEndpoint; }
    public void setImageApiEndpoint(String imageApiEndpoint) { this.imageApiEndpoint = imageApiEndpoint; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getImageModelName() { return imageModelName; }
    public void setImageModelName(String imageModelName) { this.imageModelName = imageModelName; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
}
