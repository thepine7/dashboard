package com.andrew.hnt.api.util;

import org.springframework.web.util.HtmlUtils;

/**
 * XSS 공격 방지 유틸리티
 * 사용자 입력 데이터의 XSS 공격을 방지하기 위한 유틸리티 클래스
 */
public class XssUtil {
    
    /**
     * HTML 이스케이프 처리
     * @param input 입력 문자열
     * @return XSS 방지 처리된 문자열
     */
    public static String escapeHtml(String input) {
        if (StringUtil.isEmpty(input)) {
            return "";
        }
        return HtmlUtils.htmlEscape(input);
    }
    
    /**
     * HTML 이스케이프 처리 (속성값용)
     * @param input 입력 문자열
     * @return XSS 방지 처리된 문자열
     */
    public static String escapeHtmlAttribute(String input) {
        if (StringUtil.isEmpty(input)) {
            return "";
        }
        return HtmlUtils.htmlEscape(input, "UTF-8");
    }
    
    /**
     * JavaScript 이스케이프 처리
     * @param input 입력 문자열
     * @return XSS 방지 처리된 문자열
     */
    public static String escapeJavaScript(String input) {
        if (StringUtil.isEmpty(input)) {
            return "";
        }
        
        return input.replace("\\", "\\\\")
                   .replace("'", "\\'")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t")
                   .replace("/", "\\/");
    }
    
    /**
     * SQL Injection 방지를 위한 문자열 검증
     * @param input 입력 문자열
     * @return SQL Injection 패턴이 포함된 경우 false
     */
    public static boolean isValidInput(String input) {
        if (StringUtil.isEmpty(input)) {
            return true;
        }
        
        // SQL Injection 패턴 검사
        String[] sqlPatterns = {
            "'", "\"", ";", "--", "/*", "*/", "xp_", "sp_",
            "exec", "execute", "select", "insert", "update", "delete",
            "drop", "create", "alter", "union", "script", "javascript:",
            "vbscript:", "onload", "onerror", "onclick", "onmouseover"
        };
        
        String lowerInput = input.toLowerCase();
        for (String pattern : sqlPatterns) {
            if (lowerInput.contains(pattern)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 사용자 입력 데이터 정제
     * @param input 입력 문자열
     * @return 정제된 문자열
     */
    public static String sanitizeInput(String input) {
        if (StringUtil.isEmpty(input)) {
            return "";
        }
        
        // HTML 태그 제거
        String sanitized = input.replaceAll("<[^>]*>", "");
        
        // 특수 문자 이스케이프
        sanitized = escapeHtml(sanitized);
        
        return sanitized;
    }
    
    /**
     * 파일명 XSS 방지
     * @param filename 파일명
     * @return XSS 방지 처리된 파일명
     */
    public static String sanitizeFilename(String filename) {
        if (StringUtil.isEmpty(filename)) {
            return "";
        }
        
        // 위험한 문자 제거
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

