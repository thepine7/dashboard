package com.andrew.hnt.api.service;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

/**
 * 세션 관리 공통 서비스
 * 모든 컨트롤러에서 공통으로 사용하는 세션 검증 및 관리 로직
 */
@Service
public interface SessionManagementService {
    
    /**
     * 세션 유효성 검사 및 사용자 정보 반환
     * @param session HttpSession
     * @param req HttpServletRequest (URL 파라미터 확인용)
     * @return SessionInfo 세션 정보 객체
     */
    SessionInfo validateAndGetSessionInfo(HttpSession session, Object req);
    
    /**
     * 권한 검사
     * @param session HttpSession
     * @param requiredGrade 최소 필요 등급
     * @return boolean 권한 여부
     */
    boolean hasPermission(HttpSession session, String requiredGrade);
    
    /**
     * 관리자 권한 검사
     * @param session HttpSession
     * @return boolean 관리자 권한 여부
     */
    boolean isAdmin(HttpSession session);
    
    /**
     * 일반 사용자 권한 검사 (A, U 등급)
     * @param session HttpSession
     * @return boolean 일반 사용자 권한 여부
     */
    boolean isUser(HttpSession session);
    
    /**
     * 부계정 권한 검사 (B 등급)
     * @param session HttpSession
     * @return boolean 부계정 권한 여부
     */
    boolean isSubAccount(HttpSession session);
    
    /**
     * 장치 관리 권한 검사 (A, U 등급만 장치 삭제/수정 가능)
     * @param session HttpSession
     * @return boolean 장치 관리 권한 여부
     */
    boolean canManageDevices(HttpSession session);
    
    /**
     * 사용자 관리 권한 검사 (A 등급만 사용자 관리 가능)
     * @param session HttpSession
     * @return boolean 사용자 관리 권한 여부
     */
    boolean canManageUsers(HttpSession session);
    
    /**
     * 세션 유효성 검사
     * @param session HttpSession
     * @return boolean 유효성 여부
     */
    boolean isValidSession(HttpSession session);
    
    /**
     * 세션 정보 설정
     * @param session HttpSession
     * @param userId 사용자 ID
     * @param userGrade 사용자 등급
     * @param userNm 사용자 이름
     */
    void setSessionInfo(HttpSession session, String userId, String userGrade, String userNm);
    
    /**
     * 부계정 정보 복구 (세션 만료 시)
     * @param session HttpSession
     * @param userId 사용자 ID
     */
    void restoreSubAccountInfo(HttpSession session, String userId);
    
    /**
     * 세션 정보를 모델에 설정
     * @param session HttpSession
     * @param model Model
     * @return boolean 설정 성공 여부
     */
    boolean setSessionInfoToModel(HttpSession session, Model model);
    
    /**
     * 세션 정보를 모델에 설정 (사용자 ID 지정)
     * @param session HttpSession
     * @param model Model
     * @param userId 사용자 ID
     * @return boolean 설정 성공 여부
     */
    boolean setSessionInfoToModel(HttpSession session, Model model, String userId);
    
    /**
     * 세션 만료 처리 (통일된 방식)
     * @param session HttpSession
     * @param request HttpServletRequest
     * @return 리다이렉트 URL
     */
    String handleSessionExpired(HttpSession session, HttpServletRequest request);
    
    /**
     * 세션 무효화 처리
     * @param session HttpSession
     * @param reason 무효화 사유
     */
    void invalidateSession(HttpSession session, String reason);
    
    /**
     * 세션 갱신 (마지막 접근 시간 업데이트)
     * @param session HttpSession
     */
    void refreshSession(HttpSession session);
    
    /**
     * 세션 정보 클래스
     */
    class SessionInfo {
        private String userId;
        private String userGrade;
        private String userNm;
        private boolean isValid;
        private String redirectUrl;
        
        public SessionInfo(String userId, String userGrade, String userNm, boolean isValid, String redirectUrl) {
            this.userId = userId;
            this.userGrade = userGrade;
            this.userNm = userNm;
            this.isValid = isValid;
            this.redirectUrl = redirectUrl;
        }
        
        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUserGrade() { return userGrade; }
        public void setUserGrade(String userGrade) { this.userGrade = userGrade; }
        
        public String getUserNm() { return userNm; }
        public void setUserNm(String userNm) { this.userNm = userNm; }
        
        public boolean isValid() { return isValid; }
        public void setValid(boolean valid) { isValid = valid; }
        
        public String getRedirectUrl() { return redirectUrl; }
        public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    }
}
