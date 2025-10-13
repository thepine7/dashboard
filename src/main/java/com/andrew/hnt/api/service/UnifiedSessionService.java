package com.andrew.hnt.api.service;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import java.util.Map;

/**
 * 통합 세션 관리 서비스
 * 모든 세션 관련 기능을 하나의 서비스로 통합
 * 
 * 주요 기능:
 * - 세션 검증 (기본 + 보안)
 * - 권한 관리
 * - 세션 정보 설정/조회
 * - 모델 설정
 * - 세션 통계
 */
public interface UnifiedSessionService {
    
    /**
     * 세션 검증 결과
     */
    class SessionValidationResult {
        private boolean valid;
        private String redirectUrl;
        private String errorMessage;
        private String userId;
        private String userGrade;
        private String userNm;
        private String sensorId;
        
        public SessionValidationResult(boolean valid, String redirectUrl, String errorMessage) {
            this.valid = valid;
            this.redirectUrl = redirectUrl;
            this.errorMessage = errorMessage;
        }
        
        public SessionValidationResult(boolean valid, String userId, String userGrade, String userNm, String sensorId) {
            this.valid = valid;
            this.userId = userId;
            this.userGrade = userGrade;
            this.userNm = userNm;
            this.sensorId = sensorId;
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public String getRedirectUrl() { return redirectUrl; }
        public String getErrorMessage() { return errorMessage; }
        public String getUserId() { return userId; }
        public String getUserGrade() { return userGrade; }
        public String getUserNm() { return userNm; }
        public String getSensorId() { return sensorId; }
    }
    
    /**
     * 통합 세션 검증 (기본 + 보안 + 권한)
     * 
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param model 뷰 모델 (선택적)
     * @param requiredPermission 필요한 권한 (선택적)
     * @return 검증 결과
     */
    SessionValidationResult validateSession(HttpSession session, HttpServletRequest request, Model model, String requiredPermission);
    
    /**
     * 통합 세션 검증 (모델 설정 없음)
     * 
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param requiredPermission 필요한 권한 (선택적)
     * @return 검증 결과
     */
    SessionValidationResult validateSession(HttpSession session, HttpServletRequest request, String requiredPermission);
    
    /**
     * 통합 세션 검증 (권한 검증 없음)
     * 
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param model 뷰 모델 (선택적)
     * @return 검증 결과
     */
    SessionValidationResult validateSession(HttpSession session, HttpServletRequest request, Model model);
    
    /**
     * 기본 세션 검증만 실행
     * 
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @return 검증 결과
     */
    SessionValidationResult validateBasicSession(HttpSession session, HttpServletRequest request);
    
    /**
     * 세션 정보 설정
     * 
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param userId 사용자 ID
     * @param userGrade 사용자 등급
     * @param userNm 사용자 이름
     */
    void setSessionInfo(HttpSession session, String userId, String userGrade, String userNm);
    
    /**
     * 세션 정보를 모델에 설정
     * 
     * @param session HTTP 세션
     * @param model 뷰 모델
     * @return 설정 성공 여부
     */
    boolean setSessionInfoToModel(HttpSession session, Model model);
    
    /**
     * 세션 정보를 모델에 설정 (사용자 ID 지정)
     * 
     * @param session HTTP 세션
     * @param model 뷰 모델
     * @param userId 사용자 ID
     * @return 설정 성공 여부
     */
    boolean setSessionInfoToModel(HttpSession session, Model model, String userId);
    
    /**
     * 권한 검증
     * 
     * @param session HTTP 세션
     * @param requiredPermission 필요한 권한
     * @return 권한 보유 여부
     */
    boolean hasPermission(HttpSession session, String requiredPermission);
    
    /**
     * 관리자 권한 검사
     * 
     * @param session HTTP 세션
     * @return 관리자 권한 여부
     */
    boolean isAdmin(HttpSession session);
    
    /**
     * 일반 사용자 권한 검사 (A, U 등급)
     * 
     * @param session HTTP 세션
     * @return 일반 사용자 권한 여부
     */
    boolean isUser(HttpSession session);
    
    /**
     * 부계정 권한 검사 (B 등급)
     * 
     * @param session HTTP 세션
     * @return 부계정 권한 여부
     */
    boolean isSubAccount(HttpSession session);
    
    /**
     * 장치 관리 권한 검사 (A, U 등급만 장치 삭제/수정 가능)
     * 
     * @param session HTTP 세션
     * @return 장치 관리 권한 여부
     */
    boolean canManageDevices(HttpSession session);
    
    /**
     * 사용자 관리 권한 검사 (A 등급만 사용자 관리 가능)
     * 
     * @param session HTTP 세션
     * @return 사용자 관리 권한 여부
     */
    boolean canManageUsers(HttpSession session);
    
    /**
     * 세션 유효성 검사
     * 
     * @param session HTTP 세션
     * @return 유효성 여부
     */
    boolean isValidSession(HttpSession session);
    
    /**
     * 세션 보안 검증
     * 
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @return 보안 검증 결과
     */
    boolean validateSessionSecurity(HttpSession session, HttpServletRequest request);
    
    /**
     * 세션 갱신
     * 
     * @param session HTTP 세션
     */
    void refreshSession(HttpSession session);
    
    /**
     * 세션 무효화
     * 
     * @param session HTTP 세션
     * @param reason 무효화 사유
     */
    void invalidateSession(HttpSession session, String reason);
    
    /**
     * 세션 만료 처리
     * 
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @return 리다이렉트 URL
     */
    String handleSessionExpired(HttpSession session, HttpServletRequest request);
    
    /**
     * 세션 통계 조회
     * 
     * @return 세션 통계 정보
     */
    Map<String, Object> getSessionStats();
    
    /**
     * 검증 통계 조회
     * 
     * @return 검증 통계 정보
     */
    Map<String, Object> getValidationStats();
}
