package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 보안 유틸리티 클래스
 * HnT Sensor API 프로젝트 전용
 * 모든 컨트롤러에서 공통으로 사용되는 보안 관련 기능을 제공합니다.
 *
 * 주요 기능:
 * - 입력 데이터 검증 및 정제
 * - XSS 공격 방지
 * - SQL Injection 방지
 * - 세션 보안 검증
 * - 권한 검증
 */
@Component
public class SecurityUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtil.class);
    
    // XSS 방지를 위한 패턴들
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONLOAD_PATTERN = Pattern.compile("onload\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONERROR_PATTERN = Pattern.compile("onerror\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONCLICK_PATTERN = Pattern.compile("onclick\\s*=", Pattern.CASE_INSENSITIVE);
    
    // SQL Injection 방지를 위한 패턴들
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(union|select|insert|update|delete|drop|create|alter|exec|execute)\\s+", 
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("--|/\\*|\\*/");
    private static final Pattern SQL_QUOTE_PATTERN = Pattern.compile("'|\"|`");
    
    // 사용자 ID 패턴 (영문, 숫자, 언더스코어, 하이픈만 허용)
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
    
    // UUID 패턴 (표준 UUID 형식)
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    // 이메일 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    // 전화번호 패턴 (한국 형식)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$"
    );
    
    /**
     * XSS 공격 방지를 위한 입력 데이터 정제
     * @param input 정제할 입력 데이터
     * @return 정제된 데이터
     */
    public String sanitizeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String sanitized = input.trim();
        
        // XSS 패턴 제거
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ONLOAD_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ONERROR_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ONCLICK_PATTERN.matcher(sanitized).replaceAll("");
        
        // HTML 엔티티 인코딩
        sanitized = sanitized.replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("\"", "&quot;")
                            .replace("'", "&#x27;")
                            .replace("&", "&amp;");
        
        return sanitized;
    }
    
    /**
     * SQL Injection 공격 방지를 위한 입력 데이터 검증
     * @param input 검증할 입력 데이터
     * @return SQL Injection 패턴 포함 여부
     */
    public boolean hasSqlInjection(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        
        return SQL_INJECTION_PATTERN.matcher(lowerInput).find() ||
               SQL_COMMENT_PATTERN.matcher(input).find() ||
               SQL_QUOTE_PATTERN.matcher(input).find();
    }
    
    /**
     * 사용자 ID 형식 검증
     * @param userId 검증할 사용자 ID
     * @return 유효한 형식인지 여부
     */
    public boolean isValidUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        return USER_ID_PATTERN.matcher(userId.trim()).matches();
    }
    
    /**
     * UUID 형식 검증
     * @param uuid 검증할 UUID
     * @return 유효한 형식인지 여부
     */
    public boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }
        
        return UUID_PATTERN.matcher(uuid.trim()).matches();
    }
    
    /**
     * 이메일 형식 검증
     * @param email 검증할 이메일
     * @return 유효한 형식인지 여부
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * 전화번호 형식 검증
     * @param phone 검증할 전화번호
     * @return 유효한 형식인지 여부
     */
    public boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }
    
    /**
     * 세션 보안 검증
     * @param session 검증할 세션
     * @return 보안상 유효한 세션인지 여부
     */
    public boolean isSecureSession(HttpSession session) {
        if (session == null) {
            return false;
        }
        
        try {
            // 세션 ID 검증
            String sessionId = session.getId();
            if (sessionId == null || sessionId.length() < 20) {
                logger.warn("유효하지 않은 세션 ID: {}", sessionId);
                return false;
            }
            
            // 세션 속성 검증
            Object userId = session.getAttribute("userId");
            if (userId == null || !(userId instanceof String)) {
                logger.warn("세션에 유효한 사용자 ID가 없음");
                return false;
            }
            
            // 사용자 ID 형식 검증
            if (!isValidUserId((String) userId)) {
                logger.warn("세션의 사용자 ID 형식이 유효하지 않음: {}", userId);
            return false;
        }
        
        return true;
            
        } catch (Exception e) {
            logger.error("세션 보안 검증 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 요청 보안 검증
     * @param request 검증할 HTTP 요청
     * @return 보안상 유효한 요청인지 여부
     */
    public boolean isSecureRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        
        try {
            // Referer 헤더 검증 (CSRF 방지)
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.startsWith(request.getScheme() + "://" + request.getServerName())) {
                logger.warn("의심스러운 Referer 헤더: {}", referer);
                return false;
            }
            
            // User-Agent 헤더 검증
            String userAgent = request.getHeader("User-Agent");
            if (userAgent == null || userAgent.trim().isEmpty()) {
                logger.warn("User-Agent 헤더가 없음");
                return false;
            }
            
            // 요청 URI 검증
            String requestURI = request.getRequestURI();
            if (requestURI == null || requestURI.contains("..") || requestURI.contains("//")) {
                logger.warn("의심스러운 요청 URI: {}", requestURI);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("요청 보안 검증 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 권한 검증
     * @param session 현재 세션
     * @param requiredGrade 필요한 권한 등급
     * @return 권한이 있는지 여부
     */
    public boolean hasPermission(HttpSession session, String requiredGrade) {
        if (session == null || requiredGrade == null) {
            return false;
        }
        
        try {
            String userGrade = (String) session.getAttribute("userGrade");
            if (userGrade == null) {
                return false;
            }
            
            // 권한 등급별 검증
            switch (requiredGrade.toUpperCase()) {
                case "A": // 관리자
                    return "A".equals(userGrade);
                case "U": // 일반 사용자
                    return "A".equals(userGrade) || "U".equals(userGrade);
                case "B": // 부계정
                    return "A".equals(userGrade) || "U".equals(userGrade) || "B".equals(userGrade);
                default:
                    return false;
            }
            
        } catch (Exception e) {
            logger.error("권한 검증 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 입력 데이터 종합 검증
     * @param input 검증할 입력 데이터
     * @param type 입력 데이터 타입 (userId, uuid, email, phone, general)
     * @return 검증 결과
     */
    public Map<String, Object> validateInput(String input, String type) {
        Map<String, Object> result = new HashMap<>();
        
        if (input == null || input.trim().isEmpty()) {
            result.put("valid", false);
            result.put("message", "입력 데이터가 비어있습니다.");
            return result;
        }
        
        // XSS 검증
        String sanitized = sanitizeInput(input);
        if (!sanitized.equals(input)) {
            result.put("valid", false);
            result.put("message", "XSS 공격 패턴이 감지되었습니다.");
            result.put("sanitized", sanitized);
            return result;
        }
        
        // SQL Injection 검증
        if (hasSqlInjection(input)) {
            result.put("valid", false);
            result.put("message", "SQL Injection 패턴이 감지되었습니다.");
            return result;
        }
        
        // 타입별 형식 검증
        boolean formatValid = false;
        switch (type.toLowerCase()) {
            case "userid":
                formatValid = isValidUserId(input);
                break;
            case "uuid":
                formatValid = isValidUuid(input);
                break;
            case "email":
                formatValid = isValidEmail(input);
                break;
            case "phone":
                formatValid = isValidPhone(input);
                break;
            case "general":
                formatValid = true; // 일반 텍스트는 형식 검증 생략
                break;
            default:
                formatValid = false;
        }
        
        if (!formatValid) {
            result.put("valid", false);
            result.put("message", "입력 데이터 형식이 올바르지 않습니다.");
            return result;
        }
        
        result.put("valid", true);
        result.put("message", "입력 데이터가 유효합니다.");
        result.put("sanitized", sanitized);
        
        return result;
    }
    
    /**
     * 보안 로깅
     * @param context 컨텍스트
     * @param message 메시지
     * @param level 로그 레벨 (INFO, WARN, ERROR)
     */
    public void logSecurity(String context, String message, String level) {
        String logMessage = String.format("[보안] [%s] %s", context, message);
        
        switch (level.toUpperCase()) {
            case "ERROR":
                logger.error(logMessage);
                break;
            case "WARN":
                logger.warn(logMessage);
                break;
            case "INFO":
            default:
                logger.info(logMessage);
                break;
        }
    }
}