package com.andrew.hnt.api.common;

import com.andrew.hnt.api.service.LoggingService;
import com.andrew.hnt.api.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 기본 서비스 클래스
 * 모든 서비스 클래스의 공통 기능을 제공
 */
public abstract class BaseService {
    
    @Autowired
    protected LoggingService loggingService;
    
    /**
     * 로거 인스턴스 반환
     */
    protected abstract Logger getLogger();
    
    /**
     * 에러 로깅
     */
    protected void logError(String operation, String message, Throwable throwable) {
        Map<String, Object> context = createContext();
        loggingService.logError(getLogger(), operation, message, throwable, context);
    }
    
    /**
     * 에러 로깅 (컨텍스트 포함)
     */
    protected void logError(String operation, String message, Throwable throwable, Map<String, Object> context) {
        loggingService.logError(getLogger(), operation, message, throwable, context);
    }
    
    /**
     * 경고 로깅
     */
    protected void logWarning(String operation, String message) {
        Map<String, Object> context = createContext();
        loggingService.logWarning(getLogger(), operation, message, context);
    }
    
    /**
     * 경고 로깅 (컨텍스트 포함)
     */
    protected void logWarning(String operation, String message, Map<String, Object> context) {
        loggingService.logWarning(getLogger(), operation, message, context);
    }
    
    /**
     * 정보 로깅
     */
    protected void logInfo(String operation, String message) {
        Map<String, Object> context = createContext();
        loggingService.logInfo(getLogger(), operation, message, context);
    }
    
    /**
     * 정보 로깅 (컨텍스트 포함)
     */
    protected void logInfo(String operation, String message, Map<String, Object> context) {
        loggingService.logInfo(getLogger(), operation, message, context);
    }
    
    /**
     * 디버그 로깅
     */
    protected void logDebug(String operation, String message) {
        Map<String, Object> context = createContext();
        loggingService.logDebug(getLogger(), operation, message, context);
    }
    
    /**
     * 디버그 로깅 (컨텍스트 포함)
     */
    protected void logDebug(String operation, String message, Map<String, Object> context) {
        loggingService.logDebug(getLogger(), operation, message, context);
    }
    
    /**
     * 성능 로깅
     */
    protected void logPerformance(String operation, long duration) {
        Map<String, Object> context = createContext();
        loggingService.logPerformance(getLogger(), operation, duration, context);
    }
    
    /**
     * 성능 로깅 (컨텍스트 포함)
     */
    protected void logPerformance(String operation, long duration, Map<String, Object> context) {
        loggingService.logPerformance(getLogger(), operation, duration, context);
    }
    
    /**
     * 보안 로깅
     */
    protected void logSecurity(String operation, String message) {
        Map<String, Object> context = createContext();
        loggingService.logSecurity(getLogger(), operation, message, context);
    }
    
    /**
     * 보안 로깅 (컨텍스트 포함)
     */
    protected void logSecurity(String operation, String message, Map<String, Object> context) {
        loggingService.logSecurity(getLogger(), operation, message, context);
    }
    
    /**
     * 기본 컨텍스트 생성
     */
    protected Map<String, Object> createContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("service", this.getClass().getSimpleName());
        context.put("timestamp", System.currentTimeMillis());
        return context;
    }
    
    /**
     * 사용자 컨텍스트 생성
     */
    protected Map<String, Object> createUserContext(String userId) {
        Map<String, Object> context = createContext();
        if (StringUtil.isNotEmpty(userId)) {
            context.put("userId", userId);
        }
        return context;
    }
    
    /**
     * 센서 컨텍스트 생성
     */
    protected Map<String, Object> createSensorContext(String userId, String sensorUuid) {
        Map<String, Object> context = createContext();
        if (StringUtil.isNotEmpty(userId)) {
            context.put("userId", userId);
        }
        if (StringUtil.isNotEmpty(sensorUuid)) {
            context.put("sensorUuid", sensorUuid);
        }
        return context;
    }
    
    /**
     * 성능 측정 시작
     */
    protected long startPerformanceMeasurement() {
        return System.currentTimeMillis();
    }
    
    /**
     * 성능 측정 종료
     */
    protected void endPerformanceMeasurement(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        logPerformance(operation, duration);
    }
    
    /**
     * 성능 측정 종료 (컨텍스트 포함)
     */
    protected void endPerformanceMeasurement(String operation, long startTime, Map<String, Object> context) {
        long duration = System.currentTimeMillis() - startTime;
        logPerformance(operation, duration, context);
    }
    
    /**
     * 안전한 문자열 변환
     */
    protected String safeToString(Object obj) {
        return StringUtil.defaultString(obj, "");
    }
    
    /**
     * 안전한 문자열 변환 (기본값 포함)
     */
    protected String safeToString(Object obj, String defaultValue) {
        return StringUtil.defaultString(obj, defaultValue);
    }
    
    /**
     * 안전한 숫자 변환
     */
    protected int safeToInt(Object obj, int defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        
        try {
            if (obj instanceof Number) {
                return ((Number) obj).intValue();
            }
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 안전한 숫자 변환 (Long)
     */
    protected long safeToLong(Object obj, long defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        
        try {
            if (obj instanceof Number) {
                return ((Number) obj).longValue();
            }
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 안전한 불린 변환
     */
    protected boolean safeToBoolean(Object obj, boolean defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        
        String str = obj.toString().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }
    
    /**
     * 입력 검증
     */
    protected boolean isValidInput(String input, int maxLength) {
        if (StringUtil.isEmpty(input)) {
            return false;
        }
        
        if (input.length() > maxLength) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 입력 검증 (최소 길이 포함)
     */
    protected boolean isValidInput(String input, int minLength, int maxLength) {
        if (StringUtil.isEmpty(input)) {
            return false;
        }
        
        if (input.length() < minLength || input.length() > maxLength) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 이메일 형식 검증
     */
    protected boolean isValidEmail(String email) {
        if (StringUtil.isEmpty(email)) {
            return false;
        }
        
        return email.matches(Constants.EMAIL_PATTERN);
    }
    
    /**
     * 전화번호 형식 검증
     */
    protected boolean isValidPhone(String phone) {
        if (StringUtil.isEmpty(phone)) {
            return false;
        }
        
        return phone.matches(Constants.PHONE_PATTERN);
    }
    
    /**
     * 사용자명 형식 검증
     */
    protected boolean isValidUsername(String username) {
        if (StringUtil.isEmpty(username)) {
            return false;
        }
        
        return username.matches(Constants.USERNAME_PATTERN);
    }
    
    /**
     * 비밀번호 형식 검증
     */
    protected boolean isValidPassword(String password) {
        if (StringUtil.isEmpty(password)) {
            return false;
        }
        
        return password.matches(Constants.PASSWORD_PATTERN);
    }
    
    /**
     * 페이지 번호 검증
     */
    protected int validatePageNumber(int pageNumber) {
        if (pageNumber < Constants.DEFAULT_PAGE_NUMBER) {
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
    protected int validatePageSize(int pageSize) {
        if (pageSize < 1) {
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
    protected boolean isValidUserGrade(String userGrade) {
        if (StringUtil.isEmpty(userGrade)) {
            return false;
        }
        
        return Constants.USER_GRADE_ADMIN.equals(userGrade) ||
               Constants.USER_GRADE_USER.equals(userGrade) ||
               Constants.USER_GRADE_SUB.equals(userGrade);
    }
    
    /**
     * 센서 타입 검증
     */
    protected boolean isValidSensorType(String sensorType) {
        if (StringUtil.isEmpty(sensorType)) {
            return false;
        }
        
        return Constants.SENSOR_TYPE_TEMPERATURE.equals(sensorType) ||
               Constants.SENSOR_TYPE_HUMIDITY.equals(sensorType) ||
               Constants.SENSOR_TYPE_PRESSURE.equals(sensorType) ||
               Constants.SENSOR_TYPE_CO2.equals(sensorType) ||
               Constants.SENSOR_TYPE_PT100.equals(sensorType) ||
               Constants.SENSOR_TYPE_4_20MA.equals(sensorType);
    }
    
    /**
     * 센서 상태 검증
     */
    protected boolean isValidSensorStatus(String sensorStatus) {
        if (StringUtil.isEmpty(sensorStatus)) {
            return false;
        }
        
        return Constants.SENSOR_STATUS_NORMAL.equals(sensorStatus) ||
               Constants.SENSOR_STATUS_ERROR.equals(sensorStatus) ||
               Constants.SENSOR_STATUS_OFFLINE.equals(sensorStatus) ||
               Constants.SENSOR_STATUS_MAINTENANCE.equals(sensorStatus);
    }
    
    /**
     * 알림 타입 검증
     */
    protected boolean isValidAlarmType(String alarmType) {
        if (StringUtil.isEmpty(alarmType)) {
            return false;
        }
        
        return Constants.ALARM_TYPE_TEMPERATURE.equals(alarmType) ||
               Constants.ALARM_TYPE_HUMIDITY.equals(alarmType) ||
               Constants.ALARM_TYPE_CONNECTION.equals(alarmType) ||
               Constants.ALARM_TYPE_MAINTENANCE.equals(alarmType);
    }
    
    /**
     * 알림 레벨 검증
     */
    protected boolean isValidAlarmLevel(String alarmLevel) {
        if (StringUtil.isEmpty(alarmLevel)) {
            return false;
        }
        
        return Constants.ALARM_LEVEL_INFO.equals(alarmLevel) ||
               Constants.ALARM_LEVEL_WARNING.equals(alarmLevel) ||
               Constants.ALARM_LEVEL_ERROR.equals(alarmLevel) ||
               Constants.ALARM_LEVEL_CRITICAL.equals(alarmLevel);
    }
}
