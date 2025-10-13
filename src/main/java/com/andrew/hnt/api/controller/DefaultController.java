package com.andrew.hnt.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.andrew.hnt.api.service.UnifiedSessionService;
import com.andrew.hnt.api.service.UnifiedSessionService.SessionValidationResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
public class DefaultController {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultController.class);
    
    @Autowired
    private UnifiedSessionService unifiedSessionService;
    
    /**
     * 루트 경로 처리 - 로그인 페이지로 리다이렉트
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String root(HttpServletRequest req, HttpServletResponse res) {
        logger.info("루트 경로 접근 - 로그인 페이지로 리다이렉트");
        return "redirect:/login/login";
    }
    
    /**
     * 빈 경로 처리 - 로그인 페이지로 리다이렉트
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public String rootAlt(HttpServletRequest req, HttpServletResponse res) {
        logger.info("빈 경로 접근 - 로그인 페이지로 리다이렉트");
        return "redirect:/login/login";
    }
    
    /**
     * 통합 세션 검증 (기본 + 보안 + 권한)
     * @param session HttpSession
     * @param req HttpServletRequest
     * @param requiredGrade 최소 필요 등급 (A: 관리자, U: 일반사용자, B: 부계정)
     * @return SessionValidationResult
     */
    protected SessionValidationResult validateSession(HttpSession session, HttpServletRequest req, String requiredGrade) {
        return unifiedSessionService.validateSession(session, req, requiredGrade);
    }
    
    /**
     * 통합 세션 검증 (기본 + 보안 + 권한 + 모델 설정)
     * @param session HttpSession
     * @param req HttpServletRequest
     * @param model Model
     * @param requiredGrade 최소 필요 등급 (A: 관리자, U: 일반사용자, B: 부계정)
     * @return SessionValidationResult
     */
    protected SessionValidationResult validateSession(HttpSession session, HttpServletRequest req, org.springframework.ui.Model model, String requiredGrade) {
        return unifiedSessionService.validateSession(session, req, model, requiredGrade);
    }
    
    /**
     * 세션 유효성 검사 (레거시 호환성)
     * @param session HttpSession
     * @return boolean
     * @deprecated validateSession() 사용 권장
     */
    @Deprecated
    protected boolean isValidSession(HttpSession session) {
        if (session == null) {
            logger.warn("세션이 null입니다.");
            return false;
        }
        
        String userId = (String) session.getAttribute("userId");
        if (userId == null || userId.isEmpty()) {
            logger.warn("세션에 userId가 없습니다. 세션이 만료되었거나 로그인이 필요합니다.");
            return false;
        }
        
        logger.info("세션 유효성 검사 통과 - userId: {}", userId);
        return true;
    }
    
    /**
     * 권한 검사 (레거시 호환성)
     * @param session HttpSession
     * @param requiredGrade 최소 필요 등급
     * @return boolean
     * @deprecated validateSession() 사용 권장
     */
    @Deprecated
    protected boolean hasPermission(HttpSession session, String requiredGrade) {
        if (!isValidSession(session)) {
            return false;
        }
        
        String userGrade = (String) session.getAttribute("userGrade");
        if (userGrade == null || userGrade.isEmpty()) {
            logger.warn("세션에 userGrade가 없습니다.");
            return false;
        }
        
        // 권한 등급 비교 (A > U > B)
        if ("A".equals(userGrade)) {
            return true; // 관리자는 모든 권한
        } else if ("U".equals(userGrade)) {
            return "U".equals(requiredGrade) || "B".equals(requiredGrade); // 일반 사용자는 U, B 권한
        } else if ("B".equals(userGrade)) {
            return "B".equals(requiredGrade); // 부계정은 부계정 권한만
        }
        
        return false;
    }
}
