package com.andrew.hnt.api.service;

import java.util.List;
import java.util.Map;

import com.andrew.hnt.api.model.LoginVO;
import com.andrew.hnt.api.model.SensorVO;
import com.andrew.hnt.api.model.UserInfo;
import org.springframework.stereotype.Service;

@Service
public interface LoginService {
	
	public Map<String, Object> insertUser(UserInfo userInfo) throws Exception;

	public Map<String, Object> getUserInfo(LoginVO loginVO) throws Exception;

	public Map<String, Object> getUserList() throws Exception;
	
	public Map<String, Object> getSubUserList(String userId) throws Exception;
	
	public Map<String, Object> getUserAndSubUserList(String userId) throws Exception;

	// 최적화된 사용자 목록 조회 (활동 상태 포함) - N+1 쿼리 문제 해결
	public Map<String, Object> getUserListWithActivityStatus(String currentUserId) throws Exception;
	
	// 최적화된 사용자 및 부계정 목록 조회 (활동 상태 포함) - N+1 쿼리 문제 해결
	public Map<String, Object> getUserAndSubUserListWithActivityStatus(String userId, String currentUserId) throws Exception;
	
	public void insertSensorData(SensorVO sensorVO) throws Exception;

	public void insertSensorInfo(Map<String, Object> param) throws Exception;

	public void updateUserInfo(LoginVO loginVO) throws Exception;

	public Map<String, Object> insertSensorInfo2(Map<String, Object> param) throws Exception;
	
	public void updateLoginDtm(LoginVO loginVO) throws Exception;

	public void updateLogoutDtm(LoginVO loginVO) throws Exception;

	public void insertData(String str);
	
	public UserInfo getUserInfoByUserId(String userId) throws Exception;

	// 사용자 활동 상태 업데이트
	public void updateUserActivityStatus(String userId, String activityStatus, String focusStatus) throws Exception;

	// 사용자 포커스 상태 업데이트
	public void updateUserFocusStatus(String userId, String focusStatus) throws Exception;

	// 사용자 활동 상태 조회 (세션 기반)
	public String getUserActivityStatus(String userId, javax.servlet.http.HttpSession session) throws Exception;

	// 세션 타임아웃 체크 및 자동 비활성 처리
	public void checkAndUpdateSessionTimeout() throws Exception;
	
	// 마지막 로그인 시간 업데이트
	public void updateLastLoginTime(String userId) throws Exception;
	
	public List<UserInfo> getTimeoutUsersList() throws Exception;
	
	public void processUserTimeout(String userId) throws Exception;
	
	// 부계정의 메인 사용자 ID 조회
	public String getMainUserIdForSubUser(String subUserId) throws Exception;
	
	// 사용자 활동 시간 업데이트 (하트비트)
	public void updateUserActivity(String userId) throws Exception;
}
