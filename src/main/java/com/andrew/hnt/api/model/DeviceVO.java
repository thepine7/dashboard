package com.andrew.hnt.api.model;

public class DeviceVO {
    public String userId;
    public String sensorName;
    public String sensorUuid;
    public String chgSensorName;
    
    // Getters
    public String getUserId() { return userId; }
    public String getSensorName() { return sensorName; }
    public String getSensorUuid() { return sensorUuid; }
    public String getChgSensorName() { return chgSensorName; }
    
    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setSensorName(String sensorName) { this.sensorName = sensorName; }
    public void setSensorUuid(String sensorUuid) { this.sensorUuid = sensorUuid; }
    public void setChgSensorName(String chgSensorName) { this.chgSensorName = chgSensorName; }
}
