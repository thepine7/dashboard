package com.andrew.hnt.api.mapper;

import com.andrew.hnt.api.model.LoginVO;
import com.andrew.hnt.api.model.SensorVO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Mapper
@Repository
public interface MqttMapper {

    public void insertSensorData(SensorVO sensorVO);

    public Map<String, Object> getSensorValue(String userId);

    public Map<String, Object> getSensorInfo(Map<String, Object> param);

    public void insertSensorInfo(Map<String, Object> param);
    
    // 장치 이름 변경
    public void updateSensorNameDirect(Map<String, Object> param);
    
    // 장치 마지막 접속 시간 업데이트 (중복 등록 시)
    public void updateSensorLastAccessTime(Map<String, Object> param);
    
    public Map<String, Object> selectSetting(Map<String, Object> param);

    public Map<String, Object> getUserInfoForMqtt(LoginVO loginVO);

    public Map<String, Object> selectNoti(Map<String, Object> param);

    public void insertNoti(Map<String, Object> param);

    // 장치 전송 기능을 위한 메서드들
    public Map<String, Object> getSensorInfoByUuid(Map<String, Object> param);
    public void deleteSensorInfoByUuid(Map<String, Object> param);
    public void deleteConfigByUuid(Map<String, Object> param);
    public void deleteSensorDataByUuid(Map<String, Object> param);
    public void deleteAlarmByUuid(Map<String, Object> param);
    
    // 센서 데이터 배치 삭제 (비동기 삭제용)
    public int deleteSensorDataBatch(Map<String, Object> param);
    
    // 기본 설정 정보 삽입
    public void insertDefaultConfig(Map<String, Object> param);
}
