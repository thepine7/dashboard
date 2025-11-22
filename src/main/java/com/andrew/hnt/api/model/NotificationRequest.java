package com.andrew.hnt.api.model;

/**
 * 알림 요청 모델
 */
public class NotificationRequest {
    private String userId;
    private String fcmToken;
    private String sensorUuid;
    private String message;
    private String alarmType;
    
    public NotificationRequest() {
    }
    
    public NotificationRequest(String userId, String fcmToken, String sensorUuid, String message) {
        this.userId = userId;
        this.fcmToken = fcmToken;
        this.sensorUuid = sensorUuid;
        this.message = message;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getFcmToken() {
        return fcmToken;
    }
    
    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
    
    public String getSensorUuid() {
        return sensorUuid;
    }
    
    public void setSensorUuid(String sensorUuid) {
        this.sensorUuid = sensorUuid;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getAlarmType() {
        return alarmType;
    }
    
    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }
}

