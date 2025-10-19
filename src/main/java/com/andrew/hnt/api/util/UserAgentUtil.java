package com.andrew.hnt.api.util;

import javax.servlet.http.HttpServletRequest;

/**
 * User-Agent 유틸리티
 * PC/앱 구분 및 클라이언트 타입 판별
 */
public class UserAgentUtil {
    
    /**
     * 앱에서 온 요청인지 확인
     * @param req HttpServletRequest
     * @return true: 앱 요청, false: PC/웹 요청
     */
    public static boolean isAppRequest(HttpServletRequest req) {
        String userAgent = req.getHeader("User-Agent");
        if (userAgent == null) {
            return false;
        }
        
        // Android 앱 User-Agent 패턴 확인
        // 일반적으로 okhttp나 Retrofit을 사용하는 경우
        return userAgent.contains("okhttp") || 
               userAgent.contains("Dalvik") ||
               (userAgent.contains("Android") && !userAgent.contains("Mobile Safari"));
    }
    
    /**
     * User-Agent로 접속 유형 반환
     * @param userAgent User-Agent 문자열
     * @return "APP" 또는 "PC"
     */
    public static String getClientType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "PC";
        }
        
        if (userAgent.contains("okhttp") || 
            userAgent.contains("Dalvik") ||
            (userAgent.contains("Android") && !userAgent.contains("Mobile Safari"))) {
            return "APP";
        }
        
        return "PC";
    }
}

