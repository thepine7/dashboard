package com.andrew.hnt.api.controller;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.andrew.hnt.api.service.UnifiedSessionService;
import com.andrew.hnt.api.service.UnifiedSessionService.SessionValidationResult;

import java.util.Map;
import java.util.HashMap;

/**
 * 통합 세션 관리 컨트롤러
 * 
 * 통합 세션 관리 서비스의 사용 예제를 보여주는 컨트롤러
 * 기존 컨트롤러들이 통합 서비스를 사용하도록 마이그레이션하는 가이드
 */
@Controller
public class UnifiedSessionController {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedSessionController.class);
    
    @Autowired
    private UnifiedSessionService unifiedSessionService;
    
    /**
     * 통합 세션 검증 예제 - 관리자 페이지
     * 
     * 기존 방식:
     * - SessionValidationService.validateSession() 호출
     * - 권한 검증을 별도로 수행
     * - 모델 설정을 별도로 수행
     * 
     * 통합 방식:
     * - UnifiedSessionService.validateSession() 한 번 호출로 모든 처리
     */
    @RequestMapping(value = "/unified/admin", method = RequestMethod.GET)
    public String unifiedAdminPage(HttpServletRequest req, HttpServletResponse res, Model model) {
        HttpSession session = req.getSession();
        
        // 통합 세션 검증 (기본 + 보안 + 권한 + 모델 설정)
        SessionValidationResult result = unifiedSessionService.validateSession(
            session, req, model, "A"); // 관리자 권한 필요
        
        if (!result.isValid()) {
            logger.warn("통합 세션 검증 실패 - 리다이렉트: {}, 오류: {}", 
                result.getRedirectUrl(), result.getErrorMessage());
            return result.getRedirectUrl();
        }
        
        // 검증 성공 시 사용자 정보 사용
        String userId = result.getUserId();
        String userGrade = result.getUserGrade();
        String userNm = result.getUserNm();
        
        logger.info("통합 세션 검증 성공 - userId: {}, userGrade: {}, userNm: {}", 
                   userId, userGrade, userNm);
        
        // 추가 비즈니스 로직 수행
        model.addAttribute("pageTitle", "통합 세션 관리 예제 - 관리자 페이지");
        model.addAttribute("message", "통합 세션 관리 서비스를 사용한 관리자 페이지입니다.");
        
        return "admin/unified-admin";
    }
    
    /**
     * 통합 세션 검증 예제 - 일반 사용자 페이지
     */
    @RequestMapping(value = "/unified/user", method = RequestMethod.GET)
    public String unifiedUserPage(HttpServletRequest req, HttpServletResponse res, Model model) {
        HttpSession session = req.getSession();
        
        // 통합 세션 검증 (기본 + 보안 + 권한 + 모델 설정)
        SessionValidationResult result = unifiedSessionService.validateSession(
            session, req, model, "U"); // 일반 사용자 권한 필요
        
        if (!result.isValid()) {
            logger.warn("통합 세션 검증 실패 - 리다이렉트: {}, 오류: {}", 
                result.getRedirectUrl(), result.getErrorMessage());
            return result.getRedirectUrl();
        }
        
        // 검증 성공 시 사용자 정보 사용
        String userId = result.getUserId();
        String userGrade = result.getUserGrade();
        String userNm = result.getUserNm();
        
        logger.info("통합 세션 검증 성공 - userId: {}, userGrade: {}, userNm: {}", 
                   userId, userGrade, userNm);
        
        // 추가 비즈니스 로직 수행
        model.addAttribute("pageTitle", "통합 세션 관리 예제 - 사용자 페이지");
        model.addAttribute("message", "통합 세션 관리 서비스를 사용한 사용자 페이지입니다.");
        
        return "user/unified-user";
    }
    
    /**
     * 통합 세션 검증 예제 - 권한 검증 없음
     */
    @RequestMapping(value = "/unified/public", method = RequestMethod.GET)
    public String unifiedPublicPage(HttpServletRequest req, HttpServletResponse res, Model model) {
        HttpSession session = req.getSession();
        
        // 통합 세션 검증 (기본 + 보안 + 모델 설정, 권한 검증 없음)
        SessionValidationResult result = unifiedSessionService.validateSession(
            session, req, model); // 권한 검증 없음
        
        if (!result.isValid()) {
            logger.warn("통합 세션 검증 실패 - 리다이렉트: {}, 오류: {}", 
                result.getRedirectUrl(), result.getErrorMessage());
            return result.getRedirectUrl();
        }
        
        // 검증 성공 시 사용자 정보 사용
        String userId = result.getUserId();
        String userGrade = result.getUserGrade();
        String userNm = result.getUserNm();
        
        logger.info("통합 세션 검증 성공 - userId: {}, userGrade: {}, userNm: {}", 
                   userId, userGrade, userNm);
        
        // 추가 비즈니스 로직 수행
        model.addAttribute("pageTitle", "통합 세션 관리 예제 - 공개 페이지");
        model.addAttribute("message", "통합 세션 관리 서비스를 사용한 공개 페이지입니다.");
        
        return "public/unified-public";
    }
    
    /**
     * 권한 검증 예제 API
     */
    @RequestMapping(value = "/unified/check-permission", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> checkPermission(HttpServletRequest req, HttpServletResponse res) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        HttpSession session = req.getSession();
        
        try {
            String permission = req.getParameter("permission");
            
            if (permission == null || permission.isEmpty()) {
                resultMap.put("success", false);
                resultMap.put("resultCode", "400");
                resultMap.put("resultMessage", "권한 파라미터가 필요합니다");
                return resultMap;
            }
            
            // 권한 검증
            boolean hasPermission = unifiedSessionService.hasPermission(session, permission);
            
            resultMap.put("success", true);
            resultMap.put("resultCode", "200");
            resultMap.put("resultMessage", "권한 검증 완료");
            resultMap.put("hasPermission", hasPermission);
            resultMap.put("permission", permission);
            
            logger.info("권한 검증 완료 - permission: {}, hasPermission: {}", permission, hasPermission);
            
        } catch (Exception e) {
            logger.error("권한 검증 중 오류 발생", e);
            resultMap.put("success", false);
            resultMap.put("resultCode", "500");
            resultMap.put("resultMessage", "권한 검증 실패: " + e.getMessage());
        }
        
        return resultMap;
    }
    
    /**
     * 세션 통계 조회 API
     */
    @RequestMapping(value = "/unified/session-stats", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getSessionStats(HttpServletRequest req, HttpServletResponse res) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        
        try {
            // 세션 통계 조회
            Map<String, Object> sessionStats = unifiedSessionService.getSessionStats();
            Map<String, Object> validationStats = unifiedSessionService.getValidationStats();
            
            resultMap.put("success", true);
            resultMap.put("resultCode", "200");
            resultMap.put("resultMessage", "세션 통계 조회 성공");
            resultMap.put("sessionStats", sessionStats);
            resultMap.put("validationStats", validationStats);
            
            logger.info("세션 통계 조회 완료 - 통계: {}", sessionStats);
            
        } catch (Exception e) {
            logger.error("세션 통계 조회 실패", e);
            resultMap.put("success", false);
            resultMap.put("resultCode", "500");
            resultMap.put("resultMessage", "세션 통계 조회 실패: " + e.getMessage());
        }
        
        return resultMap;
    }
}
