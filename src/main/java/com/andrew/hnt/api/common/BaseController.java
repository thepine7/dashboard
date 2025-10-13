package com.andrew.hnt.api.common;

import com.andrew.hnt.api.exception.*;
import com.andrew.hnt.api.util.StringUtil;
import com.andrew.hnt.api.service.LoginService;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 기본 컨트롤러 클래스
 * 모든 컨트롤러의 공통 기능을 제공
 */
public abstract class BaseController {
    
    /**
     * 로거 인스턴스 반환
     */
    protected abstract Logger getLogger();
    
    /**
     * 통일된 세션 정보를 모델에 설정
     * 모든 컨트롤러에서 사용할 공통 메서드
     * @param session HttpSession
     * @param model Model
     * @param loginService LoginService (DB 조회용)
     * @return boolean 설정 성공 여부
     */
    protected boolean setSessionInfoToModel(HttpSession session, Model model, LoginService loginService) {
        // SessionManagementService를 사용하도록 수정 필요
        // 임시로 기본 구현
        if (session == null || model == null) {
            return false;
        }
        
        String userId = (String) session.getAttribute("userId");
        String userGrade = (String) session.getAttribute("userGrade");
        String userNm = (String) session.getAttribute("userNm");
        
        if (userId != null && !userId.isEmpty()) {
            model.addAttribute("userId", userId);
            model.addAttribute("userGrade", userGrade);
            model.addAttribute("userNm", userNm);
            return true;
        }
        
        return false;
    }
    
    /**
     * 세션 정보 유효성 검증
     * @param session HttpSession
     * @return boolean 유효성 여부
     */
    protected boolean validateSession(HttpSession session) {
        if (session == null) {
            return false;
        }
        
        String userId = (String) session.getAttribute("userId");
        return userId != null && !userId.isEmpty();
    }
    
    /**
     * 세션에서 사용자 ID 가져오기
     * @param session HttpSession
     * @return String 사용자 ID
     */
    protected String getUserIdFromSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute("userId");
    }
    
    /**
     * 세션에서 사용자 등급 가져오기
     * @param session HttpSession
     * @return String 사용자 등급
     */
    protected String getUserGradeFromSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute("userGrade");
    }
    
    /**
     * 세션 정보 디버깅
     * @param session HttpSession
     * @param context String 컨텍스트
     */
    protected void debugSessionInfo(HttpSession session, String context) {
        if (session == null) {
            getLogger().warn("{} - 세션이 null입니다.", context);
            return;
        }
        
        String userId = (String) session.getAttribute("userId");
        String userGrade = (String) session.getAttribute("userGrade");
        String userNm = (String) session.getAttribute("userNm");
        
        getLogger().info("{} - 세션 정보: userId={}, userGrade={}, userNm={}", 
                        context, userId, userGrade, userNm);
    }
    
    /**
     * 성공 응답 생성
     */
    protected ResponseEntity<Map<String, Object>> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("resultCode", Constants.SUCCESS_CODE);
        response.put("resultMessage", message);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 성공 응답 생성 (데이터 포함)
     */
    protected ResponseEntity<Map<String, Object>> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("resultCode", Constants.SUCCESS_CODE);
        response.put("resultMessage", message);
        response.put("data", data);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 에러 응답 생성
     */
    protected ResponseEntity<Map<String, Object>> createErrorResponse(String errorCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("resultCode", errorCode);
        response.put("resultMessage", message);
        response.put("timestamp", LocalDateTime.now().toString());
        
        HttpStatus status = getHttpStatus(errorCode);
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * 에러 응답 생성 (상세 정보 포함)
     */
    protected ResponseEntity<Map<String, Object>> createErrorResponse(String errorCode, String message, Object details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("resultCode", errorCode);
        response.put("resultMessage", message);
        response.put("details", details);
        response.put("timestamp", LocalDateTime.now().toString());
        
        HttpStatus status = getHttpStatus(errorCode);
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * 에러 코드에 따른 HTTP 상태 코드 반환
     */
    private HttpStatus getHttpStatus(String errorCode) {
        switch (errorCode) {
            case Constants.VALIDATION_ERROR_CODE:
                return HttpStatus.BAD_REQUEST;
            case Constants.AUTHENTICATION_ERROR_CODE:
                return HttpStatus.UNAUTHORIZED;
            case Constants.AUTHORIZATION_ERROR_CODE:
                return HttpStatus.FORBIDDEN;
            case Constants.NOT_FOUND_ERROR_CODE:
                return HttpStatus.NOT_FOUND;
            case Constants.DATABASE_ERROR_CODE:
                return HttpStatus.SERVICE_UNAVAILABLE;
            case Constants.MQTT_ERROR_CODE:
                return HttpStatus.GATEWAY_TIMEOUT;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
    
    
    /**
     * 세션에서 사용자명 가져오기
     */
    protected String getUserNameFromSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute("userNm");
    }
    
    /**
     * 권한 검증
     */
    protected boolean hasPermission(HttpSession session, String requiredGrade) {
        if (!validateSession(session)) {
            return false;
        }
        
        String userGrade = getUserGradeFromSession(session);
        if (StringUtil.isEmpty(userGrade)) {
            return false;
        }
        
        // 관리자는 모든 권한
        if (Constants.USER_GRADE_ADMIN.equals(userGrade)) {
            return true;
        }
        
        // 일반 사용자는 U, B 권한
        if (Constants.USER_GRADE_USER.equals(userGrade)) {
            return Constants.USER_GRADE_USER.equals(requiredGrade) || 
                   Constants.USER_GRADE_SUB.equals(requiredGrade);
        }
        
        // 부계정은 B 권한만
        if (Constants.USER_GRADE_SUB.equals(userGrade)) {
            return Constants.USER_GRADE_SUB.equals(requiredGrade);
        }
        
        return false;
    }
    
    /**
     * 관리자 권한 검증
     */
    protected boolean isAdmin(HttpSession session) {
        return hasPermission(session, Constants.USER_GRADE_ADMIN);
    }
    
    /**
     * 일반 사용자 권한 검증
     */
    protected boolean isUser(HttpSession session) {
        return hasPermission(session, Constants.USER_GRADE_USER);
    }
    
    /**
     * 부계정 권한 검증
     */
    protected boolean isSubUser(HttpSession session) {
        return hasPermission(session, Constants.USER_GRADE_SUB);
    }
    
    /**
     * 클라이언트 IP 주소 가져오기
     */
    protected String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtil.isNotEmpty(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (StringUtil.isNotEmpty(xRealIP)) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * User-Agent 가져오기
     */
    protected String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        return StringUtil.defaultString(request.getHeader("User-Agent"), "unknown");
    }
    
    /**
     * 요청 정보 로깅
     */
    protected void logRequest(HttpServletRequest request, String operation) {
        String clientIp = getClientIpAddress(request);
        String userAgent = getUserAgent(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        
        getLogger().info("요청 정보 - IP: {}, Method: {}, URI: {}, Query: {}, User-Agent: {}, Operation: {}", 
                        clientIp, method, uri, queryString, userAgent, operation);
    }
    
    /**
     * 응답 정보 로깅
     */
    protected void logResponse(String operation, int statusCode, long duration) {
        getLogger().info("응답 정보 - Operation: {}, Status: {}, Duration: {}ms", 
                        operation, statusCode, duration);
    }
    
    /**
     * 입력 데이터 검증
     */
    protected void validateInput(String input, String fieldName, int maxLength) {
        if (StringUtil.isEmpty(input)) {
            throw new ValidationException("400", fieldName + "은(는) 필수 입력 항목입니다.");
        }
        
        if (input.length() > maxLength) {
            throw new ValidationException("400", fieldName + "은(는) " + maxLength + "자를 초과할 수 없습니다.");
        }
    }
    
    /**
     * 입력 데이터 검증 (최소 길이 포함)
     */
    protected void validateInput(String input, String fieldName, int minLength, int maxLength) {
        if (StringUtil.isEmpty(input)) {
            throw new ValidationException("400", fieldName + "은(는) 필수 입력 항목입니다.");
        }
        
        if (input.length() < minLength) {
            throw new ValidationException("400", fieldName + "은(는) " + minLength + "자 이상이어야 합니다.");
        }
        
        if (input.length() > maxLength) {
            throw new ValidationException("400", fieldName + "은(는) " + maxLength + "자를 초과할 수 없습니다.");
        }
    }
    
    /**
     * 이메일 형식 검증
     */
    protected void validateEmail(String email) {
        if (StringUtil.isEmpty(email)) {
            throw new ValidationException("400", "이메일은 필수 입력 항목입니다.");
        }
        
        if (!email.matches(Constants.EMAIL_PATTERN)) {
            throw new ValidationException("400", "올바른 이메일 형식이 아닙니다.");
        }
    }
    
    /**
     * 전화번호 형식 검증
     */
    protected void validatePhone(String phone) {
        if (StringUtil.isEmpty(phone)) {
            throw new ValidationException("400", "전화번호는 필수 입력 항목입니다.");
        }
        
        if (!phone.matches(Constants.PHONE_PATTERN)) {
            throw new ValidationException("400", "올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)");
        }
    }
    
    /**
     * 사용자명 형식 검증
     */
    protected void validateUsername(String username) {
        if (StringUtil.isEmpty(username)) {
            throw new ValidationException("400", "사용자명은 필수 입력 항목입니다.");
        }
        
        if (!username.matches(Constants.USERNAME_PATTERN)) {
            throw new ValidationException("400", "사용자명은 3-20자의 영문, 숫자, 언더스코어만 사용 가능합니다.");
        }
    }
    
    /**
     * 비밀번호 형식 검증
     */
    protected void validatePassword(String password) {
        if (StringUtil.isEmpty(password)) {
            throw new ValidationException("400", "비밀번호는 필수 입력 항목입니다.");
        }
        
        if (!password.matches(Constants.PASSWORD_PATTERN)) {
            throw new ValidationException("400", "비밀번호는 8-50자의 영문, 숫자, 특수문자를 포함해야 합니다.");
        }
    }
    
    /**
     * 페이지 번호 검증
     */
    protected int validatePageNumber(Integer pageNumber) {
        if (pageNumber == null || pageNumber < Constants.DEFAULT_PAGE_NUMBER) {
            return Constants.DEFAULT_PAGE_NUMBER;
        }
        
        if (pageNumber > Constants.MAX_PAGE_NUMBER) {
            return Constants.MAX_PAGE_NUMBER;
        }
        
        return pageNumber;
    }
    
    /**
     * 페이지 크기 검증
     */
    protected int validatePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return Constants.DEFAULT_PAGE_SIZE;
        }
        
        if (pageSize > Constants.MAX_PAGE_SIZE) {
            return Constants.MAX_PAGE_SIZE;
        }
        
        return pageSize;
    }
    
    /**
     * 정렬 방향 검증
     */
    protected String validateSortDirection(String sortDirection) {
        if (StringUtil.isEmpty(sortDirection)) {
            return Constants.SORT_ASC;
        }
        
        String upper = sortDirection.toUpperCase();
        if (Constants.SORT_ASC.equals(upper) || Constants.SORT_DESC.equals(upper)) {
            return upper;
        }
        
        return Constants.SORT_ASC;
    }
    
    /**
     * 사용자 등급 검증
     */
    protected void validateUserGrade(String userGrade) {
        if (StringUtil.isEmpty(userGrade)) {
            throw new ValidationException("400", "사용자 등급은 필수 입력 항목입니다.");
        }
        
        if (!Constants.USER_GRADE_ADMIN.equals(userGrade) &&
            !Constants.USER_GRADE_USER.equals(userGrade) &&
            !Constants.USER_GRADE_SUB.equals(userGrade)) {
            throw new ValidationException("400", "올바른 사용자 등급이 아닙니다.");
        }
    }
    
    /**
     * 센서 타입 검증
     */
    protected void validateSensorType(String sensorType) {
        if (StringUtil.isEmpty(sensorType)) {
            throw new ValidationException("400", "센서 타입은 필수 입력 항목입니다.");
        }
        
        if (!Constants.SENSOR_TYPE_TEMPERATURE.equals(sensorType) &&
            !Constants.SENSOR_TYPE_HUMIDITY.equals(sensorType) &&
            !Constants.SENSOR_TYPE_PRESSURE.equals(sensorType) &&
            !Constants.SENSOR_TYPE_CO2.equals(sensorType) &&
            !Constants.SENSOR_TYPE_PT100.equals(sensorType) &&
            !Constants.SENSOR_TYPE_4_20MA.equals(sensorType)) {
            throw new ValidationException("400", "올바른 센서 타입이 아닙니다.");
        }
    }
    
    /**
     * 센서 상태 검증
     */
    protected void validateSensorStatus(String sensorStatus) {
        if (StringUtil.isEmpty(sensorStatus)) {
            throw new ValidationException("400", "센서 상태는 필수 입력 항목입니다.");
        }
        
        if (!Constants.SENSOR_STATUS_NORMAL.equals(sensorStatus) &&
            !Constants.SENSOR_STATUS_ERROR.equals(sensorStatus) &&
            !Constants.SENSOR_STATUS_OFFLINE.equals(sensorStatus) &&
            !Constants.SENSOR_STATUS_MAINTENANCE.equals(sensorStatus)) {
            throw new ValidationException("400", "올바른 센서 상태가 아닙니다.");
        }
    }
    
    /**
     * 알림 타입 검증
     */
    protected void validateAlarmType(String alarmType) {
        if (StringUtil.isEmpty(alarmType)) {
            throw new ValidationException("400", "알림 타입은 필수 입력 항목입니다.");
        }
        
        if (!Constants.ALARM_TYPE_TEMPERATURE.equals(alarmType) &&
            !Constants.ALARM_TYPE_HUMIDITY.equals(alarmType) &&
            !Constants.ALARM_TYPE_CONNECTION.equals(alarmType) &&
            !Constants.ALARM_TYPE_MAINTENANCE.equals(alarmType)) {
            throw new ValidationException("400", "올바른 알림 타입이 아닙니다.");
        }
    }
    
    /**
     * 알림 레벨 검증
     */
    protected void validateAlarmLevel(String alarmLevel) {
        if (StringUtil.isEmpty(alarmLevel)) {
            throw new ValidationException("400", "알림 레벨은 필수 입력 항목입니다.");
        }
        
        if (!Constants.ALARM_LEVEL_INFO.equals(alarmLevel) &&
            !Constants.ALARM_LEVEL_WARNING.equals(alarmLevel) &&
            !Constants.ALARM_LEVEL_ERROR.equals(alarmLevel) &&
            !Constants.ALARM_LEVEL_CRITICAL.equals(alarmLevel)) {
            throw new ValidationException("400", "올바른 알림 레벨이 아닙니다.");
        }
    }
    
    /**
     * 비즈니스 로직 예외 발생
     */
    protected void throwBusinessException(String message) {
        throw new BusinessException("500", message);
    }
    
    /**
     * 비즈니스 로직 예외 발생 (에러 코드 포함)
     */
    protected void throwBusinessException(String errorCode, String message) {
        throw new BusinessException(errorCode, message);
    }
    
    /**
     * 인증 예외 발생
     */
    protected void throwAuthenticationException(String message) {
        throw new AuthenticationException(message);
    }
    
    /**
     * 권한 예외 발생
     */
    protected void throwAuthorizationException(String message) {
        throw new AuthorizationException(message);
    }
    
    /**
     * 데이터베이스 예외 발생
     */
    protected void throwDatabaseException(String message) {
        throw new DatabaseException("500", message);
    }
    
    /**
     * 데이터베이스 예외 발생 (원인 포함)
     */
    protected void throwDatabaseException(String message, Throwable cause) {
        throw new DatabaseException("500", message, cause);
    }
    
    /**
     * MQTT 예외 발생
     */
    protected void throwMqttException(String message) {
        throw new MqttException("502", message);
    }
    
    /**
     * MQTT 예외 발생 (원인 포함)
     */
    protected void throwMqttException(String message, Throwable cause) {
        throw new MqttException("502", message, cause);
    }
}
