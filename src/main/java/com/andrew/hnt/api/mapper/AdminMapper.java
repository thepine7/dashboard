package com.andrew.hnt.api.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface AdminMapper {

	public Map<String, Object> getSensorInfo(Map<String, Object> param);
	
	public Map<String, Object> getSensorInfoByUuid(Map<String, Object> param);

	public List<Map<String, Object>> getSensorList(Map<String, Object> param);
	
	public List<Map<String, Object>> getSensorListBySensorId(Map<String, Object> param);
	
	public List<Map<String, Object>> getSubSensorList(Map<String, Object> param);

	public List<Map<String, Object>> getNotiInfo();

	public void updateNoti(Map<String, Object> param);

	public Map<String, Object> getTime();

	public Map<String, Object> selectSensorId(Map<String, Object> param);

	public void insertSetting(Map<String, Object> param);

	public void updateSetting(Map<String, Object> param);

	public Map<String, Object> selectSetting(Map<String, Object> param);

	public Map<String, Object> selectSettingByUuid(Map<String, Object> param);

	public void deleteNoti2(Map<String, Object> param);

	public void insertNoti(Map<String, Object> param);

	public Map<String, Object> selectNoti(Map<String, Object> param);

	public List<Map<String, Object>> selectChkNoti(Map<String, Object> param);

	public void updateSensorInfo(Map<String, Object> param);

	public Map<String, Object> getAlarmSetting(Map<String, Object> param);



	public Map<String, Object> getCurTemp(Map<String, Object> param);

	public Map<String, Object> chkError(Map<String, Object> param);

	public Map<String, Object> selectUrgentNoti(Map<String, Object> param);

	public Map<String, Object> selectReleaseNoti(Map<String, Object> param);

	public void insertUrgentNoti(Map<String, Object> param);

	public void updateUrgentNoti(Map<String, Object> param);

	public void updateUrgentNoti2(Map<String, Object> param);

	public void deleteNoti(Map<String, Object> param);

	public void deleteDeviceAlarm(Map<String, Object> param);

	public void deleteUser(String userId);

	public void deleteSensor(String userId);

	public void deleteConfig(String userId, String sensorUuid);

	public void deleteUserSensorData(String userId);

	public void deleteSubUser(String userId);

	public void updateUser(Map<String, Object> param);

	public void insertSubSensorInfo(Map<String, Object> param);

	public Map<String, Object> getSensorVal(Map<String, Object> param);

	public void updateUserGrade(Map<String, Object> param);

	/**
	 * 모든 사용자의 활동 상태를 비활성으로 초기화
	 */
	public void resetAllUserActivityStatus() throws Exception;
	
	public void resetSpecificUserActivityStatus(String userId) throws Exception;
	
	/**
	 * 부계정 여부 확인 (hnt_sensor_info 테이블 기반)
	 * @param param userId 포함
	 * @return true: 부계정, false: 주계정
	 */
	public boolean isSubAccountBySensorInfo(Map<String, Object> param);
	
	public boolean isSubAccount(Map<String, Object> param);
	
	public Map<String, Object> getMainUserIdForSubUser(Map<String, Object> param);

	/**
	 * 부계정의 메인 사용자 ID 조회 (hnt_sensor_info 테이블 기반)
	 * @param param subUserId 포함
	 * @return sensor_id (메인 사용자 ID)
	 */
	public Map<String, Object> getMainUserIdForSubUserBySensorInfo(Map<String, Object> param);

	/**
	 * 사용자 관리에서 주계정의 모든 센서 조회
	 * @param param userId 포함
	 * @return 센서 리스트
	 */
	public List<Map<String, Object>> getUserSensorList(Map<String, Object> param);
	
	/**
	 * 사용자 수 조회 (헬스체크용)
	 * @return 사용자 수
	 */
	public int getUserCount();
	
	/**
	 * 설정 정보 삽입
	 * @param param 설정 정보
	 */
	public void insertConfig(Map<String, Object> param);
	
	/**
	 * 설정 정보 조회
	 * @param param 조회 조건
	 * @return 설정 정보
	 */
	public Map<String, Object> getConfig(Map<String, Object> param);

	/**
	 * 알람 설정 저장
	 * @param alarmData 알람 설정 데이터
	 */
	public void saveAlarmSetting(Map<String, Object> alarmData);

}
