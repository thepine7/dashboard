package com.andrew.hnt.api.service.impl;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrew.hnt.api.service.UnifiedSessionService;
import com.andrew.hnt.api.service.SessionManagementService;
import com.andrew.hnt.api.service.SessionSecurityService;
import com.andrew.hnt.api.service.SessionValidationService;
import com.andrew.hnt.api.util.RedirectUtil;
import com.andrew.hnt.api.common.Constants;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 통합 세션 관리 서비스 구현체
 * 
 * 모든 세션 관련 기능을 하나의 서비스로 통합하여
 * 기존의 분산된 세션 관리 로직을 중앙화
 */
@Service
public class UnifiedSessionServiceImpl implements UnifiedSessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedSessionServiceImpl.class);
    
    @Autowired
    private SessionManagementService sessionManagementService;
    
    @Autowired
    private SessionSecurityService sessionSecurityService;
    
    @Autowired
    private SessionValidationService sessionValidationService;
    
    // 통계용 카운터
    private final AtomicLong totalValidations = new AtomicLong(0);
    private final AtomicLong successfulValidations = new AtomicLong(0);
    private final AtomicLong failedValidations = new AtomicLong(0);
    private final AtomicLong permissionChecks = new AtomicLong(0);
    private final AtomicLong securityChecks = new AtomicLong(0);
    
    /**
     * 통합 세션 검증 (기본 + 보안 + 권한)
     */
    @Override
    public SessionValidationResult validateSession(HttpSession session, HttpServletRequest request, Model model, String requiredPermission) {
        totalValidations.incrementAndGet();
        
        try {
            // 1. 기본 세션 검증
            SessionValidationResult basicResult = validateBasicSession(session, request);
            if (!basicResult.isValid()) {
                failedValidations.incrementAndGet();
                return basicResult;
            }
            
            // 2. 보안 검증
            if (!validateSessionSecurity(session, request)) {
                failedValidations.incrementAndGet();
                RedirectUtil.RedirectResult redirectResult = RedirectUtil.redirectToLogin(session, request, "세션 보안 검증 실패");
                logger.info(RedirectUtil.createRedirectLog(redirectResult, request));
                return new SessionValidationResult(false, redirectResult.getRedirectUrl(), redirectResult.getReason());
            }
            securityChecks.incrementAndGet();
            
            // 3. 권한 검증 (필요한 경우)
            if (requiredPermission != null && !requiredPermission.isEmpty()) {
                if (!hasPermission(session, requiredPermission)) {
                    failedValidations.incrementAndGet();
                    RedirectUtil.RedirectResult redirectResult = RedirectUtil.redirectToMain(session, request, "권한이 없습니다");
                    logger.info(RedirectUtil.createRedirectLog(redirectResult, request));
                    return new SessionValidationResult(false, redirectResult.getRedirectUrl(), redirectResult.getReason());
                }
                permissionChecks.incrementAndGet();
            }
            
            // 4. 모델에 세션 정보 설정 (필요한 경우)
            if (model != null) {
                setSessionInfoToModel(session, model);
            }
            
            successfulValidations.incrementAndGet();
            
            // 성공 시 사용자 정보 반환
            String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
            String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
            String userNm = (String) session.getAttribute(Constants.SESSION_USER_NM);
            String sensorId = (String) session.getAttribute(Constants.SESSION_SENSOR_ID);
            
            return new SessionValidationResult(true, userId, userGrade, userNm, sensorId);
            
        } catch (Exception e) {
            logger.error("통합 세션 검증 중 오류 발생", e);
            failedValidations.incrementAndGet();
            RedirectUtil.RedirectResult redirectResult = RedirectUtil.redirectToLogin(session, request, "세션 검증 중 오류가 발생했습니다");
            logger.info(RedirectUtil.createRedirectLog(redirectResult, request));
            return new SessionValidationResult(false, redirectResult.getRedirectUrl(), redirectResult.getReason());
        }
    }
    
    /**
     * 통합 세션 검증 (모델 설정 없음)
     */
    @Override
    public SessionValidationResult validateSession(HttpSession session, HttpServletRequest request, String requiredPermission) {
        return validateSession(session, request, null, requiredPermission);
    }
    
    /**
     * 통합 세션 검증 (권한 검증 없음)
     */
    @Override
    public SessionValidationResult validateSession(HttpSession session, HttpServletRequest request, Model model) {
        return validateSession(session, request, model, null);
    }
    
    /**
     * 기본 세션 검증만 실행
     */
    @Override
    public SessionValidationResult validateBasicSession(HttpSession session, HttpServletRequest request) {
        try {
            // SessionManagementService의 기본 검증 사용
            SessionManagementService.SessionInfo sessionInfo = sessionManagementService.validateAndGetSessionInfo(session, request);
            
            if (!sessionInfo.isValid()) {
                return new SessionValidationResult(false, "redirect:/login/login", "세션이 유효하지 않습니다");
            }
            
            // 사용자 정보 반환 (sensorId는 userId와 동일)
            return new SessionValidationResult(true, 
                sessionInfo.getUserId(), 
                sessionInfo.getUserGrade(), 
                sessionInfo.getUserNm(), 
                sessionInfo.getUserId());
                
        } catch (Exception e) {
            logger.error("기본 세션 검증 중 오류 발생", e);
            return new SessionValidationResult(false, "redirect:/login/login", "세션 검증 중 오류가 발생했습니다");
        }
    }
    
    /**
     * 세션 정보 설정
     */
    @Override
    public void setSessionInfo(HttpSession session, String userId, String userGrade, String userNm) {
        try {
            sessionManagementService.setSessionInfo(session, userId, userGrade, userNm);
            logger.debug("세션 정보 설정 완료: userId={}, userGrade={}, userNm={}", userId, userGrade, userNm);
        } catch (Exception e) {
            logger.error("세션 정보 설정 중 오류 발생", e);
        }
    }
    
    /**
     * 세션 정보를 모델에 설정
     */
    @Override
    public boolean setSessionInfoToModel(HttpSession session, Model model) {
        try {
            return sessionManagementService.setSessionInfoToModel(session, model);
        } catch (Exception e) {
            logger.error("세션 정보 모델 설정 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 세션 정보를 모델에 설정 (사용자 ID 지정)
     */
    @Override
    public boolean setSessionInfoToModel(HttpSession session, Model model, String userId) {
        try {
            return sessionManagementService.setSessionInfoToModel(session, model, userId);
        } catch (Exception e) {
            logger.error("세션 정보 모델 설정 중 오류 발생: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 권한 검증
     */
    @Override
    public boolean hasPermission(HttpSession session, String requiredPermission) {
        try {
            return sessionManagementService.hasPermission(session, requiredPermission);
        } catch (Exception e) {
            logger.error("권한 검증 중 오류 발생: permission={}", requiredPermission, e);
            return false;
        }
    }
    
    /**
     * 관리자 권한 검사
     */
    @Override
    public boolean isAdmin(HttpSession session) {
        try {
            String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
            return "A".equals(userGrade);
        } catch (Exception e) {
            logger.error("관리자 권한 검사 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 일반 사용자 권한 검사 (A, U 등급)
     */
    @Override
    public boolean isUser(HttpSession session) {
        try {
            String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
            return "A".equals(userGrade) || "U".equals(userGrade);
        } catch (Exception e) {
            logger.error("일반 사용자 권한 검사 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 부계정 권한 검사 (B 등급)
     */
    @Override
    public boolean isSubAccount(HttpSession session) {
        try {
            String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
            return "B".equals(userGrade);
        } catch (Exception e) {
            logger.error("부계정 권한 검사 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 장치 관리 권한 검사 (A, U 등급만 장치 삭제/수정 가능)
     */
    @Override
    public boolean canManageDevices(HttpSession session) {
        try {
            String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
            return "A".equals(userGrade) || "U".equals(userGrade);
        } catch (Exception e) {
            logger.error("장치 관리 권한 검사 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 사용자 관리 권한 검사 (A 등급만 사용자 관리 가능)
     */
    @Override
    public boolean canManageUsers(HttpSession session) {
        try {
            String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
            return "A".equals(userGrade);
        } catch (Exception e) {
            logger.error("사용자 관리 권한 검사 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 세션 유효성 검사
     */
    @Override
    public boolean isValidSession(HttpSession session) {
        try {
            return sessionManagementService.isValidSession(session);
        } catch (Exception e) {
            logger.error("세션 유효성 검사 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 세션 보안 검증
     */
    @Override
    public boolean validateSessionSecurity(HttpSession session, HttpServletRequest request) {
        try {
            // 앱 요청 감지 (User-Agent 확인)
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            boolean isAppRequest = userAgent != null && (userAgent.contains("hnt_android") || userAgent.contains("okhttp"));
            
            if (isAppRequest) {
                logger.debug("앱 요청 감지 - 세션 보안 검증 건너뜀");
                return true; // 앱 요청은 보안 검증 건너뜀
            }
            
            return sessionSecurityService.validateSessionSecurity(session, request);
        } catch (Exception e) {
            logger.error("세션 보안 검증 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 세션 갱신
     */
    @Override
    public void refreshSession(HttpSession session) {
        try {
            sessionSecurityService.refreshSession(session);
            logger.debug("세션 갱신 완료");
        } catch (Exception e) {
            logger.error("세션 갱신 중 오류 발생", e);
        }
    }
    
    /**
     * 세션 무효화
     */
    @Override
    public void invalidateSession(HttpSession session, String reason) {
        try {
            logger.info("세션 무효화 시작: reason={}", reason);
            sessionSecurityService.invalidateSession(session);
            logger.info("세션 무효화 완료: reason={}", reason);
        } catch (Exception e) {
            logger.error("세션 무효화 중 오류 발생: reason={}", reason, e);
        }
    }
    
    /**
     * 세션 만료 처리
     */
    @Override
    public String handleSessionExpired(HttpSession session, HttpServletRequest request) {
        try {
            return sessionManagementService.handleSessionExpired(session, request);
        } catch (Exception e) {
            logger.error("세션 만료 처리 중 오류 발생", e);
            return "redirect:/login/login";
        }
    }
    
    /**
     * 세션 통계 조회
     */
    @Override
    public Map<String, Object> getSessionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalValidations", totalValidations.get());
        stats.put("successfulValidations", successfulValidations.get());
        stats.put("failedValidations", failedValidations.get());
        stats.put("permissionChecks", permissionChecks.get());
        stats.put("securityChecks", securityChecks.get());
        stats.put("successRate", totalValidations.get() > 0 ? 
            (double) successfulValidations.get() / totalValidations.get() * 100 : 0.0);
        return stats;
    }
    
    /**
     * 검증 통계 조회
     */
    @Override
    public Map<String, Object> getValidationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalValidations", totalValidations.get());
        stats.put("successfulValidations", successfulValidations.get());
        stats.put("failedValidations", failedValidations.get());
        stats.put("permissionChecks", permissionChecks.get());
        stats.put("securityChecks", securityChecks.get());
        return stats;
    }
}
