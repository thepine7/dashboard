package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 리다이렉트 유틸리티 클래스
 * HnT Sensor API 프로젝트 전용
 * 
 * 세션 정보 누락 시 로그인 페이지 리다이렉트 로직을 표준화하고
 * 일관된 리다이렉트 처리를 제공
 */
@Component
public class RedirectUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(RedirectUtil.class);
    
    // 리다이렉트 URL 상수
    public static final String LOGIN_PAGE = "redirect:/login/login";
    public static final String MAIN_PAGE = "redirect:/main/main";
    public static final String ERROR_PAGE = "redirect:/error/error";
    
    // 리다이렉트 타입 상수
    public static final String REDIRECT_TYPE_LOGIN = "LOGIN";
    public static final String REDIRECT_TYPE_MAIN = "MAIN";
    public static final String REDIRECT_TYPE_ERROR = "ERROR";
    
    /**
     * 리다이렉트 결과 클래스
     */
    public static class RedirectResult {
        private final String redirectUrl;
        private final String redirectType;
        private final String reason;
        private final boolean isAjax;
        private final Map<String, Object> additionalData;
        
        public RedirectResult(String redirectUrl, String redirectType, String reason, boolean isAjax) {
            this.redirectUrl = redirectUrl;
            this.redirectType = redirectType;
            this.reason = reason;
            this.isAjax = isAjax;
            this.additionalData = new HashMap<>();
        }
        
        public String getRedirectUrl() {
            return redirectUrl;
        }
        
        public String getRedirectType() {
            return redirectType;
        }
        
        public String getReason() {
            return reason;
        }
        
        public boolean isAjax() {
            return isAjax;
        }
        
        public Map<String, Object> getAdditionalData() {
            return additionalData;
        }
        
        public void addData(String key, Object value) {
            this.additionalData.put(key, value);
        }
    }
    
    /**
     * 세션 정보 누락 시 로그인 페이지 리다이렉트
     * @param session HttpSession
     * @param req HttpServletRequest
     * @param reason 리다이렉트 이유
     * @return RedirectResult
     */
    public static RedirectResult redirectToLogin(HttpSession session, HttpServletRequest req, String reason) {
        logger.warn("세션 정보 누락으로 로그인 페이지 리다이렉트 - 이유: {}", reason);
        
        // AJAX 요청인지 확인
        boolean isAjax = isAjaxRequest(req);
        
        // 세션 정보 로깅
        logSessionInfo(session, "리다이렉트 전");
        
        return new RedirectResult(LOGIN_PAGE, REDIRECT_TYPE_LOGIN, reason, isAjax);
    }
    
    /**
     * 권한 부족 시 메인 페이지 리다이렉트
     * @param session HttpSession
     * @param req HttpServletRequest
     * @param reason 리다이렉트 이유
     * @return RedirectResult
     */
    public static RedirectResult redirectToMain(HttpSession session, HttpServletRequest req, String reason) {
        logger.warn("권한 부족으로 메인 페이지 리다이렉트 - 이유: {}", reason);
        
        // AJAX 요청인지 확인
        boolean isAjax = isAjaxRequest(req);
        
        // 세션 정보 로깅
        logSessionInfo(session, "리다이렉트 전");
        
        return new RedirectResult(MAIN_PAGE, REDIRECT_TYPE_MAIN, reason, isAjax);
    }
    
    /**
     * 에러 발생 시 에러 페이지 리다이렉트
     * @param session HttpSession
     * @param req HttpServletRequest
     * @param reason 리다이렉트 이유
     * @return RedirectResult
     */
    public static RedirectResult redirectToError(HttpSession session, HttpServletRequest req, String reason) {
        logger.error("에러 발생으로 에러 페이지 리다이렉트 - 이유: {}", reason);
        
        // AJAX 요청인지 확인
        boolean isAjax = isAjaxRequest(req);
        
        // 세션 정보 로깅
        logSessionInfo(session, "리다이렉트 전");
        
        return new RedirectResult(ERROR_PAGE, REDIRECT_TYPE_ERROR, reason, isAjax);
    }
    
    /**
     * AJAX 요청인지 확인
     * @param req HttpServletRequest
     * @return AJAX 요청 여부
     */
    public static boolean isAjaxRequest(HttpServletRequest req) {
        String requestedWith = req.getHeader("X-Requested-With");
        String contentType = req.getContentType();
        
        boolean isAjax = "XMLHttpRequest".equals(requestedWith) || 
                        (contentType != null && contentType.contains("application/json"));
        
        logger.debug("AJAX 요청 확인 - X-Requested-With: {}, Content-Type: {}, isAjax: {}", 
                    requestedWith, contentType, isAjax);
        
        return isAjax;
    }
    
    /**
     * 세션 정보 로깅
     * @param session HttpSession
     * @param context 로깅 컨텍스트
     */
    private static void logSessionInfo(HttpSession session, String context) {
        if (session == null) {
            logger.warn("{} - 세션이 null입니다.", context);
            return;
        }
        
        String userId = (String) session.getAttribute("userId");
        String userGrade = (String) session.getAttribute("userGrade");
        String userNm = (String) session.getAttribute("userNm");
        long lastAccessedTime = session.getLastAccessedTime();
        int maxInactiveInterval = session.getMaxInactiveInterval();
        
        logger.info("{} - 세션 정보: userId={}, userGrade={}, userNm={}, lastAccessedTime={}, maxInactiveInterval={}", 
                   context, userId, userGrade, userNm, lastAccessedTime, maxInactiveInterval);
    }
    
    /**
     * 세션 만료 여부 확인
     * @param session HttpSession
     * @return 세션 만료 여부
     */
    public static boolean isSessionExpired(HttpSession session) {
        if (session == null) {
            return true;
        }
        
        try {
            // 세션에 접근하여 만료 여부 확인
            String userId = (String) session.getAttribute("userId");
            return userId == null || userId.isEmpty();
        } catch (IllegalStateException e) {
            logger.warn("세션이 무효화됨: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * 세션 정보 누락 여부 확인
     * @param session HttpSession
     * @return 세션 정보 누락 여부
     */
    public static boolean isSessionInfoMissing(HttpSession session) {
        if (session == null) {
            return true;
        }
        
        String userId = (String) session.getAttribute("userId");
        String userGrade = (String) session.getAttribute("userGrade");
        
        boolean isMissing = userId == null || userId.isEmpty() || 
                           userGrade == null || userGrade.isEmpty();
        
        if (isMissing) {
            logger.warn("세션 정보 누락 - userId: {}, userGrade: {}", userId, userGrade);
        }
        
        return isMissing;
    }
    
    /**
     * 리다이렉트 URL에 파라미터 추가
     * @param baseUrl 기본 URL
     * @param params 파라미터 맵
     * @return 파라미터가 추가된 URL
     */
    public static String addParametersToUrl(String baseUrl, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        
        StringBuilder url = new StringBuilder(baseUrl);
        url.append("?");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) {
                url.append("&");
            }
            url.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        
        return url.toString();
    }
    
    /**
     * AJAX 응답용 에러 정보 생성
     * @param redirectResult RedirectResult
     * @return AJAX 응답용 Map
     */
    public static Map<String, Object> createAjaxErrorResponse(RedirectResult redirectResult) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("resultCode", "401");
        response.put("resultMessage", redirectResult.getReason());
        response.put("redirectUrl", redirectResult.getRedirectUrl());
        response.put("redirectType", redirectResult.getRedirectType());
        response.put("timestamp", System.currentTimeMillis());
        
        // 추가 데이터가 있으면 포함
        if (!redirectResult.getAdditionalData().isEmpty()) {
            response.putAll(redirectResult.getAdditionalData());
        }
        
        return response;
    }
    
    /**
     * 리다이렉트 로그 생성
     * @param redirectResult RedirectResult
     * @param req HttpServletRequest
     * @return 로그 메시지
     */
    public static String createRedirectLog(RedirectResult redirectResult, HttpServletRequest req) {
        return String.format("리다이렉트 실행 - URL: %s, 타입: %s, 이유: %s, AJAX: %s, 요청URI: %s", 
                           redirectResult.getRedirectUrl(), 
                           redirectResult.getRedirectType(), 
                           redirectResult.getReason(), 
                           redirectResult.isAjax(),
                           req.getRequestURI());
    }
}
