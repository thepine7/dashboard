package com.andrew.hnt.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT 설정 중앙화
 * 하드코딩된 MQTT 설정을 application.yml로 이동
 */
@Configuration
@ConfigurationProperties(prefix = "custom.mqtt")
public class MqttConfig {
    
    private String server = "tcp://hntsolution.co.kr:1883";
    private String clientId = "hnt-sensor-api";
    private String username = "hnt1";
    private String password = "abcde";
    private String defaultTopic = "#";
    private int connectionTimeout = 30;
    private int keepAliveInterval = 60;
    private int maxReconnectAttempts = 5;
    private int baseReconnectDelay = 2000;
    private int maxReconnectDelay = 30000;
    
    // Getters and Setters
    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getDefaultTopic() { return defaultTopic; }
    public void setDefaultTopic(String defaultTopic) { this.defaultTopic = defaultTopic; }
    
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    
    public int getKeepAliveInterval() { return keepAliveInterval; }
    public void setKeepAliveInterval(int keepAliveInterval) { this.keepAliveInterval = keepAliveInterval; }
    
    public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
    public void setMaxReconnectAttempts(int maxReconnectAttempts) { this.maxReconnectAttempts = maxReconnectAttempts; }
    
    public int getBaseReconnectDelay() { return baseReconnectDelay; }
    public void setBaseReconnectDelay(int baseReconnectDelay) { this.baseReconnectDelay = baseReconnectDelay; }
    
    public int getMaxReconnectDelay() { return maxReconnectDelay; }
    public void setMaxReconnectDelay(int maxReconnectDelay) { this.maxReconnectDelay = maxReconnectDelay; }
}

