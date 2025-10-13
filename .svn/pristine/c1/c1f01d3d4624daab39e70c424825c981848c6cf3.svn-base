package com.andrew.hnt.api.service;

import com.andrew.hnt.api.model.SensorVO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface MqttService {

    public Map<String, Object> getData() throws Exception;

    public void insertSensorData(SensorVO sensorVO) throws Exception;

    public void receiveData(String str);
}
