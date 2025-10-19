package com.andrew.hnt.api.mapper;

import java.util.List;
import java.util.Map;

import com.andrew.hnt.api.model.LoginVO;
import com.andrew.hnt.api.model.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface LoginMapper {

	public UserInfo getUserInfo(LoginVO loginVO);

	public UserInfo getUserInfoByUserId(String userId);

	public List<UserInfo> getUserList();
	
	public List<UserInfo> getSubUserList(String userId);
	
	public List<UserInfo> getUserAndSubUserList(String userId);

	// 최적화된 사용자 목록 조회 (활동 상태 포함) - N+1 쿼리 문제 해결
	public List<UserInfo> getUserListWithActivityStatus(Map<String, Object> param);
	
	// 최적화된 사용자 및 부계정 목록 조회 (활동 상태 포함) - N+1 쿼리 문제 해결
	public List<UserInfo> getUserAndSubUserListWithActivityStatus(Map<String, Object> param);

	public void insertUser(UserInfo userInfo);

	public void updateUserInfo(LoginVO loginVO);

	public void updateLoginDtm(LoginVO loginVO);

	public void updateLogoutDtm(LoginVO loginVO);

	public void removeLogoutDtm(LoginVO loginVO);

	public Map<String, Object> getMainId(Map<String, Object> param);

	public void deleteAlarm(LoginVO loginVO);

	// 사용자 활동 상태 업데이트
	public void updateUserActivityStatus(Map<String, Object> param);
	
	// 마지막 로그인 시간 업데이트
	public void updateLastLoginTime(Map<String, Object> param);

	// 사용자 포커스 상태 업데이트
	public void updateUserFocusStatus(Map<String, Object> param);

	// 세션 타임아웃으로 인한 비활성 사용자 업데이트
	public void updateInactiveUsersByTimeout();
	
	// 타임아웃 대상 사용자 조회 (로그용)
	public List<UserInfo> getTimeoutUsers();
    
    public void updateSpecificUserTimeout(Map<String, Object> param);
    
    // 부계정의 메인 사용자 ID 조회
    public Map<String, Object> getMainUserIdForSubUser(Map<String, Object> param);
    
    // 사용자 활동 시간 업데이트 (하트비트)
    public void updateUserActivity(Map<String, Object> param);
}
