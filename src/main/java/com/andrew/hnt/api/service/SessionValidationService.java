package com.andrew.hnt.api.service;

import org.springframework.ui.Model;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 통합 세션 검증 서비스
 * 모든 컨트롤러에서 일관된 세션 검증 시퀀스를 제공
 * 
 * 주요 기능:
 * - 표준화된 세션 검증 순서
 * - 권한 검증 통합
 * - 모델 설정 자동화
 * - 에러 처리 표준화
 */
public interface SessionValidationService {
    
    /**
     * 세션 검증 결과
     */
    class ValidationResult {
        private boolean valid;
        private String redirectUrl;
        private String errorMessage;
        private String userId;
        private String userGrade;
        private String userNm;
        private String sensorId;
        
        public ValidationResult(boolean valid, String redirectUrl, String errorMessage) {
            this.valid = valid;
            this.redirectUrl = redirectUrl;
            this.errorMessage = errorMessage;
        }
        
        public ValidationResult(boolean valid, String userId, String userGrade, String userNm, String sensorId) {
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
     * 표준 세션 검증 시퀀스 실행
     * 1. 기본 세션 검증
     * 2. 보안 검증
     * 3. 권한 검증 (선택적)
     * 4. 모델 설정 (선택적)
     * 
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param model 뷰 모델 (선택적)
     * @param requiredPermission 필요한 권한 (선택적, null이면 권한 검증 생략)
     * @return 검증 결과
     */
    ValidationResult validateSession(HttpSession session, HttpServletRequest request, Model model, String requiredPermission);
    
    /**
     * 표준 세션 검증 시퀀스 실행 (모델 설정 없음)
     * 
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param requiredPermission 필요한 권한 (선택적)
     * @return 검증 결과
     */
    ValidationResult validateSession(HttpSession session, HttpServletRequest request, String requiredPermission);
    
    /**
     * 표준 세션 검증 시퀀스 실행 (권한 검증 없음)
     * 
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param model 뷰 모델 (선택적)
     * @return 검증 결과
     */
    ValidationResult validateSession(HttpSession session, HttpServletRequest request, Model model);
    
    /**
     * 기본 세션 검증만 실행
     * 
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @return 검증 결과
     */
    ValidationResult validateBasicSession(HttpSession session, HttpServletRequest request);
    
    /**
     * 세션 정보를 모델에 설정
     * 
     * @param session HTTP 세션
     * @param model 뷰 모델
     * @return 설정 성공 여부
     */
    boolean setSessionInfoToModel(HttpSession session, Model model);
    
    /**
     * 권한 검증
     * 
     * @param session HTTP 세션
     * @param requiredPermission 필요한 권한
     * @return 권한 보유 여부
     */
    boolean hasPermission(HttpSession session, String requiredPermission);
    
    /**
     * 세션 검증 통계 조회
     * 
     * @return 검증 통계 정보
     */
    Map<String, Object> getValidationStats();
}
