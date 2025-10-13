package com.andrew.hnt.api.util;

import com.andrew.hnt.api.service.SessionManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 세션 만료 에러 복구 전략
 * 세션 만료 시 다양한 복구 방법을 시도
 */
@Component
public class SessionExpiredRecoveryStrategy implements ErrorRecoveryStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionExpiredRecoveryStrategy.class);
    
    @Autowired
    private SessionManagementService sessionManagementService;
    
    @Override
    public RecoveryResult attemptRecovery(ErrorContext errorContext) {
        logger.info("세션 만료 복구 시도 시작 - 재시도 횟수: {}", errorContext.getRetryCount());
        
        try {
            // 1단계: 세션 객체 확인
            HttpSession session = getSessionFromContext(errorContext);
            if (session == null) {
                logger.warn("세션 객체를 찾을 수 없음");
                return new RecoveryResult(false, "세션 객체를 찾을 수 없습니다.");
            }
            
            // 2단계: 세션 유효성 재검증
            if (sessionManagementService.isValidSession(session)) {
                logger.info("세션이 이미 유효한 상태");
                return new RecoveryResult(true, "세션이 이미 유효한 상태입니다.");
            }
            
            // 3단계: 세션 연장 시도
            if (errorContext.getRetryCount() >= 1) {
                logger.info("세션 연장 시도");
                
                try {
                    sessionManagementService.refreshSession(session);
                    
                    if (sessionManagementService.isValidSession(session)) {
                        logger.info("세션 연장 성공");
                        return new RecoveryResult(true, "세션 연장 성공");
                    }
                } catch (Exception e) {
                    logger.warn("세션 연장 실패", e);
                }
            }
            
            // 4단계: 세션 속성 복구 시도
            if (errorContext.getRetryCount() >= 2) {
                logger.info("세션 속성 복구 시도");
                
                try {
                    // 컨텍스트에서 사용자 정보 추출
                    Map<String, Object> contextData = errorContext.getContextData();
                    if (contextData != null) {
                        String userId = (String) contextData.get("userId");
                        String userGrade = (String) contextData.get("userGrade");
                        String userNm = (String) contextData.get("userNm");
                        
                        if (userId != null && !userId.isEmpty()) {
                            // 세션 속성 재설정
                            session.setAttribute("userId", userId);
                            if (userGrade != null) {
                                session.setAttribute("userGrade", userGrade);
                            }
                            if (userNm != null) {
                                session.setAttribute("userNm", userNm);
                            }
                            
                            // 세션 유효성 재검증
                            if (sessionManagementService.isValidSession(session)) {
                                logger.info("세션 속성 복구 성공");
                                return new RecoveryResult(true, "세션 속성 복구 성공");
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("세션 속성 복구 실패", e);
                }
            }
            
            logger.warn("세션 만료 복구 실패 - 모든 전략 시도 완료");
            return new RecoveryResult(false, "세션 만료 복구에 실패했습니다.");
            
        } catch (Exception e) {
            logger.error("세션 만료 복구 중 예외 발생", e);
            return new RecoveryResult(false, "세션 만료 복구 중 예외 발생: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRecover(ErrorContext errorContext) {
        // 세션 관련 에러만 복구 가능
        return "SESSION_EXPIRED_ERROR".equals(errorContext.getErrorType()) ||
               "PERMISSION_DENIED_ERROR".equals(errorContext.getErrorType());
    }
    
    @Override
    public int getPriority() {
        return 2; // 중간 우선순위
    }
    
    @Override
    public ErrorType getErrorType() {
        return ErrorType.SESSION_EXPIRED_ERROR;
    }
    
    /**
     * 컨텍스트에서 세션 객체 추출
     */
    private HttpSession getSessionFromContext(ErrorContext errorContext) {
        Map<String, Object> contextData = errorContext.getContextData();
        if (contextData != null) {
            return (HttpSession) contextData.get("session");
        }
        return null;
    }
}
