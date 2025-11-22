package com.andrew.hnt.api.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public interface AdminService {

	public Map<String, Object> getUserInfo(String userId, String sensorUuid);

	public Map<String, Object> getSensorInfo(Map<String, Object> param);
	
	public Map<String, Object> getSensorInfoByUuid(String sensorUuid, String userId);

	public String getUserToken(String userId);

	public List<Map<String, Object>> getSensorList(String userId);
	
	public List<Map<String, Object>> getSubSensorList(String mainUserId, String subUserId);

	public void insertSetting(Map<String, Object> settingMap);

	public Map<String, Object> selectSetting(Map<String, Object> param);
	
	public Map<String, Object> getSensorSettings(String sensorUuid);

	public void deleteNoti2(Map<String, Object> param);

	public void insertNoti(Map<String, Object> param);

	public Map<String, Object> selectNoti(Map<String, Object> param);

	public Map<String, Object> selectChkNoti(Map<String, Object> param);

	public void updateSensorInfo(Map<String, Object> param);

	public Map<String, Object> getAlarmSetting(Map<String, Object> param);



	public String getCurTemp(Map<String, Object> param);

	public int chkError(Map<String, Object> param);

	public void updateNoti(Map<String, Object> param);

	public Map<String, Object> selectUrgentNoti(Map<String, Object> param);

	public Map<String, Object> selectReleaseNoti(Map<String, Object> param);

	public void insertUrgentNoti(Map<String, Object> param);

	public void updateUrgentNoti(Map<String, Object> param);

	public void deleteUser(String userId);

	public void deleteSubUser(String userId);

	public void updateUser(Map<String, Object> param);

	public void createSubProc(Map<String, Object> param) throws Exception;
	
	public void updateUserGrade(Map<String, Object> param) throws Exception;

	/**
	 * 모든 사용자의 활동 상태를 비활성으로 초기화
	 */
	public void resetAllUserActivityStatus() throws Exception;
	
	public void resetSpecificUserActivityStatus(String userId) throws Exception;
	
	/**
	 * 부계정 여부 확인 (hnt_sensor_info 테이블 기반)
	 * @param userId 사용자 ID
	 * @return true: 부계정, false: 주계정
	 */
	public boolean isSubAccount(String userId);
	
	/**
	 * 부계정의 메인 사용자 ID 조회
	 * @param subUserId 부계정 사용자 ID
	 * @return 메인 사용자 ID
	 */
	public String getMainUserIdForSubUser(String subUserId);

	/**
	 * 사용자 관리에서 주계정의 모든 센서 조회
	 * @param userId 사용자 ID
	 * @return 센서 리스트
	 */
	public List<Map<String, Object>> getUserSensorList(String userId);
	
	/**
	 * 알람 설정 저장
	 * @param alarmData 알람 설정 데이터
	 */
	public void saveAlarmSetting(Map<String, Object> alarmData);
	
	/**
	 * 주계정/부계정 장치 전체 조회 (parent_user_id 기반)
	 * @param targetUserId 조회할 사용자 ID
	 * @param parentUserId 주계정 사용자 ID
	 * @return 센서 리스트
	 */
	public List<Map<String, Object>> getFullSensorList(String targetUserId, String parentUserId);
	
	/**
	 * 사용자 ID로 센서 토큰 업데이트
	 * @param param userId, token
	 * @return 업데이트된 행 수
	 */
	public int updateSensorTokenByUserId(Map<String, Object> param);
}
