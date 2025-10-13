package com.andrew.hnt.api.model;


public class SensorVO {
    public String userId;
    public String sensorId;
    public String uuid;
    public String sensorType;
    public String sensorValue;
    public String topic;
    public String name;
    public String type;
    public String dinVal;
    public String outputVal;
    public String rawData; 
    public String instId;
    public String mdfId;
    public String instDtm;
    public String dataJson;
    
    // Getters
    public String getUserId() { return userId; }
    public String getSensorId() { return sensorId; }
    public String getUuid() { return uuid; }
    public String getSensorType() { return sensorType; }
    public String getSensorValue() { return sensorValue; }
    public String getTopic() { return topic; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDinVal() { return dinVal; }
    public String getOutputVal() { return outputVal; }
    public String getRawData() { return rawData; }
    public String getInstId() { return instId; }
    public String getMdfId() { return mdfId; }
    public String getInstDtm() { return instDtm; }
    public String getDataJson() { return dataJson; }
    
    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }
    public void setSensorValue(String sensorValue) { this.sensorValue = sensorValue; }
    public void setTopic(String topic) { this.topic = topic; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setDinVal(String dinVal) { this.dinVal = dinVal; }
    public void setOutputVal(String outputVal) { this.outputVal = outputVal; }
    public void setRawData(String rawData) { this.rawData = rawData; }
    public void setInstId(String instId) { this.instId = instId; }
    public void setMdfId(String mdfId) { this.mdfId = mdfId; }
    public void setInstDtm(String instDtm) { this.instDtm = instDtm; }
    public void setDataJson(String dataJson) { this.dataJson = dataJson; }
}
