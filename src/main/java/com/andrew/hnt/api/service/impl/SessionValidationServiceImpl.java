package com.andrew.hnt.api.service.impl;

import com.andrew.hnt.api.service.SessionValidationService;
import com.andrew.hnt.api.service.SessionManagementService;
import com.andrew.hnt.api.service.SessionSecurityService;
import com.andrew.hnt.api.util.StringUtil;
import com.andrew.hnt.api.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 통합 세션 검증 서비스 구현
 * 모든 컨트롤러에서 일관된 세션 검증 시퀀스를 제공
 */
@Service
public class SessionValidationServiceImpl implements SessionValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionValidationServiceImpl.class);
    
    @Autowired
    private SessionManagementService sessionManagementService;
    
    @Autowired
    private SessionSecurityService sessionSecurityService;
    
    // 검증 통계
    private final AtomicLong totalValidations = new AtomicLong(0);
    private final AtomicLong successfulValidations = new AtomicLong(0);
    private final AtomicLong failedValidations = new AtomicLong(0);
    private final AtomicLong securityViolations = new AtomicLong(0);
    private final AtomicLong permissionDenied = new AtomicLong(0);
    
    @Override
    public ValidationResult validateSession(HttpSession session, HttpServletRequest request, Model model, String requiredPermission) {
        logger.debug("=== 통합 세션 검증 시작 ===");
        totalValidations.incrementAndGet();
        
        try {
            // 1. 기본 세션 검증
            ValidationResult basicResult = validateBasicSession(session, request);
            if (!basicResult.isValid()) {
                failedValidations.incrementAndGet();
                logger.warn("기본 세션 검증 실패: {}", basicResult.getErrorMessage());
                return basicResult;
            }
            
            // 2. 보안 검증
            if (!sessionSecurityService.validateSessionSecurity(session, request)) {
                securityViolations.incrementAndGet();
                failedValidations.incrementAndGet();
                logger.warn("세션 보안 검증 실패");
                return new ValidationResult(false, "/login/login", "세션 보안 검증 실패");
            }
            
            // 3. 권한 검증 (필요한 경우)
            if (StringUtil.isNotEmpty(requiredPermission)) {
                if (!hasPermission(session, requiredPermission)) {
                    permissionDenied.incrementAndGet();
                    failedValidations.incrementAndGet();
                    logger.warn("권한 검증 실패 - 필요한 권한: {}", requiredPermission);
                    return new ValidationResult(false, "/main/main", "권한이 없습니다");
                }
            }
            
            // 4. 모델 설정 (필요한 경우)
            if (model != null) {
                if (!setSessionInfoToModel(session, model)) {
                    logger.warn("모델 설정 실패");
                    return new ValidationResult(false, "/login/login", "세션 정보 설정 실패");
                }
            }
            
            // 5. 사용자 정보 추출 (Constants 키 우선, 없으면 기본 키 사용)
            String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
            String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
            String userNm = (String) session.getAttribute(Constants.SESSION_USER_NM);
            
            if (userId == null || userId.isEmpty()) {
                userId = (String) session.getAttribute("userId");
            }
            if (userGrade == null || userGrade.isEmpty()) {
                userGrade = (String) session.getAttribute("userGrade");
            }
            if (userNm == null || userNm.isEmpty()) {
                userNm = (String) session.getAttribute("userNm");
            }
            String sensorId = (String) session.getAttribute("sensorId");
            
            if (StringUtil.isEmpty(sensorId)) {
                sensorId = userId; // 기본값
            }
            
            successfulValidations.incrementAndGet();
            logger.debug("통합 세션 검증 성공 - userId: {}, userGrade: {}", userId, userGrade);
            
            return new ValidationResult(true, userId, userGrade, userNm, sensorId);
            
        } catch (Exception e) {
            failedValidations.incrementAndGet();
            logger.error("세션 검증 중 오류 발생", e);
            return new ValidationResult(false, "/login/login", "세션 검증 중 오류가 발생했습니다");
        }
    }
    
    @Override
    public ValidationResult validateSession(HttpSession session, HttpServletRequest request, String requiredPermission) {
        return validateSession(session, request, null, requiredPermission);
    }
    
    @Override
    public ValidationResult validateSession(HttpSession session, HttpServletRequest request, Model model) {
        return validateSession(session, request, model, null);
    }
    
    @Override
    public ValidationResult validateBasicSession(HttpSession session, HttpServletRequest request) {
        logger.debug("기본 세션 검증 시작");
        
        try {
            // 세션 유효성 검사
            if (session == null) {
                logger.warn("세션이 null입니다");
                return new ValidationResult(false, "/login/login", "세션이 없습니다");
            }
            
            // 세션 관리 서비스를 통한 검증
            SessionManagementService.SessionInfo sessionInfo = sessionManagementService.validateAndGetSessionInfo(session, request);
            
            if (!sessionInfo.isValid()) {
                logger.warn("세션 정보 검증 실패: {}", sessionInfo.getRedirectUrl());
                return new ValidationResult(false, sessionInfo.getRedirectUrl(), "세션이 유효하지 않습니다");
            }
            
            // 세션 타임아웃 검사
            if (!sessionManagementService.isValidSession(session)) {
                logger.warn("세션 타임아웃");
                return new ValidationResult(false, "/login/login?timeout=true", "세션이 만료되었습니다");
            }
            
            logger.debug("기본 세션 검증 성공");
            return new ValidationResult(true, null, null);
            
        } catch (Exception e) {
            logger.error("기본 세션 검증 중 오류 발생", e);
            return new ValidationResult(false, "/login/login", "세션 검증 중 오류가 발생했습니다");
        }
    }
    
    @Override
    public boolean setSessionInfoToModel(HttpSession session, Model model) {
        // SessionManagementService의 setSessionInfoToModel 메서드 위임
        return sessionManagementService.setSessionInfoToModel(session, model);
    }
    
    @Override
    public boolean hasPermission(HttpSession session, String requiredPermission) {
        // SessionManagementService의 hasPermission 메서드 위임
        return sessionManagementService.hasPermission(session, requiredPermission);
    }
    
    @Override
    public Map<String, Object> getValidationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalValidations", totalValidations.get());
        stats.put("successfulValidations", successfulValidations.get());
        stats.put("failedValidations", failedValidations.get());
        stats.put("securityViolations", securityViolations.get());
        stats.put("permissionDenied", permissionDenied.get());
        
        long total = totalValidations.get();
        if (total > 0) {
            stats.put("successRate", (double) successfulValidations.get() / total * 100);
            stats.put("failureRate", (double) failedValidations.get() / total * 100);
        } else {
            stats.put("successRate", 0.0);
            stats.put("failureRate", 0.0);
        }
        
        return stats;
    }
}
