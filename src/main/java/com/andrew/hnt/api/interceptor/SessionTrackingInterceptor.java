package com.andrew.hnt.api.interceptor;

import com.andrew.hnt.api.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 세션 추적을 위한 HandlerInterceptor
 * 컨트롤러 진입 전/후 세션 변화를 추적합니다.
 */
@Component
public class SessionTrackingInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionTrackingInterceptor.class);
    
    // ThreadLocal을 사용하여 요청별 세션 스냅샷 저장
    private static final ThreadLocal<Map<String, Object>> PRE_HANDLE_SESSION = new ThreadLocal<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String handlerName = handler.toString();
        
        logger.info("=== [INTERCEPTOR] 컨트롤러 진입 전 ===");
        logger.info("Handler: {}", handlerName);
        logger.info("URI: {} {}", method, requestURI);
        
        // 세션 스냅샷 저장
        HttpSession session = request.getSession(false);
        if (session != null) {
            Map<String, Object> sessionSnapshot = createSessionSnapshot(session);
            PRE_HANDLE_SESSION.set(sessionSnapshot);
            
            logger.info("[INTERCEPTOR] preHandle 세션 스냅샷 저장 완료");
            logKeySessionAttributes(sessionSnapshot, "preHandle");
        } else {
            logger.info("[INTERCEPTOR] preHandle 세션: 없음");
            PRE_HANDLE_SESSION.remove();
        }
        
        return true; // 계속 진행
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, 
                          ModelAndView modelAndView) throws Exception {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        logger.info("=== [INTERCEPTOR] 컨트롤러 처리 후 ===");
        logger.info("URI: {} {}", method, requestURI);
        
        // 현재 세션 스냅샷 생성
        HttpSession session = request.getSession(false);
        if (session != null) {
            Map<String, Object> currentSnapshot = createSessionSnapshot(session);
            Map<String, Object> preSnapshot = PRE_HANDLE_SESSION.get();
            
            if (preSnapshot != null) {
                // 세션 변화 비교 (diff)
                compareSessionSnapshots(preSnapshot, currentSnapshot, requestURI);
            } else {
                logger.info("[INTERCEPTOR] postHandle 세션 스냅샷 비교 불가 (preHandle 데이터 없음)");
                logKeySessionAttributes(currentSnapshot, "postHandle");
            }
        } else {
            logger.info("[INTERCEPTOR] postHandle 세션: 없음");
        }
        
        // ThreadLocal 정리
        PRE_HANDLE_SESSION.remove();
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        
        if (ex != null) {
            logger.error("[INTERCEPTOR] afterCompletion 예외 발생: {}", ex.getMessage(), ex);
        }
        
        // ThreadLocal 정리 (안전장치)
        PRE_HANDLE_SESSION.remove();
    }
    
    /**
     * 세션 스냅샷을 생성합니다.
     */
    private Map<String, Object> createSessionSnapshot(HttpSession session) {
        Map<String, Object> snapshot = new HashMap<>();
        
        snapshot.put("sessionId", session.getId());
        snapshot.put("isNew", session.isNew());
        snapshot.put("creationTime", session.getCreationTime());
        snapshot.put("lastAccessedTime", session.getLastAccessedTime());
        snapshot.put("maxInactiveInterval", session.getMaxInactiveInterval());
        
        // 세션 속성들 저장
        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            Object value = session.getAttribute(name);
            snapshot.put("attr_" + name, value);
        }
        
        return snapshot;
    }
    
    /**
     * 핵심 세션 속성을 로깅합니다.
     */
    private void logKeySessionAttributes(Map<String, Object> snapshot, String phase) {
        logger.info("[INTERCEPTOR] {} 핵심 세션 속성:", phase);
        
        String[] keyAttributes = {
            "attr_" + Constants.SESSION_USER_ID, "attr_" + Constants.SESSION_USER_GRADE, "attr_" + Constants.SESSION_USER_NM,
            "attr_userId", "attr_userGrade", "attr_userNm", 
            "attr_loginUserId", "attr_sensorId"
        };
        
        for (String key : keyAttributes) {
            Object value = snapshot.get(key);
            if (value != null) {
                logger.info("  🔑 {}: {}", key.replace("attr_", ""), value);
            }
        }
    }
    
    /**
     * 두 세션 스냅샷을 비교하여 변화를 로깅합니다.
     */
    private void compareSessionSnapshots(Map<String, Object> preSnapshot, 
                                       Map<String, Object> currentSnapshot, 
                                       String requestURI) {
        
        logger.info("=== [INTERCEPTOR] 세션 변화 분석 ===");
        
        // 세션 ID 변화 확인
        String preSessionId = (String) preSnapshot.get("sessionId");
        String currentSessionId = (String) currentSnapshot.get("sessionId");
        if (!preSessionId.equals(currentSessionId)) {
            logger.warn("🔄 세션 ID 변화: {} → {}", preSessionId, currentSessionId);
        }
        
        // isNew 변화 확인
        Boolean preIsNew = (Boolean) preSnapshot.get("isNew");
        Boolean currentIsNew = (Boolean) currentSnapshot.get("isNew");
        if (!preIsNew.equals(currentIsNew)) {
            logger.info("🆕 isNew 변화: {} → {}", preIsNew, currentIsNew);
        }
        
        // 핵심 속성 변화 확인
        String[] keyAttributes = {
            Constants.SESSION_USER_ID, Constants.SESSION_USER_GRADE, Constants.SESSION_USER_NM,
            "userId", "userGrade", "userNm", "loginUserId", "sensorId"
        };
        
        boolean hasChanges = false;
        for (String attr : keyAttributes) {
            String preKey = "attr_" + attr;
            String currentKey = "attr_" + attr;
            
            Object preValue = preSnapshot.get(preKey);
            Object currentValue = currentSnapshot.get(currentKey);
            
            if (!Objects.equals(preValue, currentValue)) {
                if (!hasChanges) {
                    logger.info("📝 핵심 속성 변화:");
                    hasChanges = true;
                }
                
                if (preValue == null && currentValue != null) {
                    logger.info("  ➕ {}: null → {}", attr, currentValue);
                } else if (preValue != null && currentValue == null) {
                    logger.info("  ➖ {}: {} → null", attr, preValue);
                } else {
                    logger.info("  🔄 {}: {} → {}", attr, preValue, currentValue);
                }
            }
        }
        
        if (!hasChanges) {
            logger.info("📝 핵심 속성 변화: 없음");
        }
        
        // 새로운 속성 추가 확인
        for (String key : currentSnapshot.keySet()) {
            if (key.startsWith("attr_") && !preSnapshot.containsKey(key)) {
                String attrName = key.replace("attr_", "");
                Object value = currentSnapshot.get(key);
                logger.info("➕ 새 속성 추가: {} = {}", attrName, value);
            }
        }
        
        // 속성 삭제 확인
        for (String key : preSnapshot.keySet()) {
            if (key.startsWith("attr_") && !currentSnapshot.containsKey(key)) {
                String attrName = key.replace("attr_", "");
                Object value = preSnapshot.get(key);
                logger.info("➖ 속성 삭제: {} = {}", attrName, value);
            }
        }
        
        logger.info("=== [INTERCEPTOR] 세션 변화 분석 완료 ===");
    }
}
