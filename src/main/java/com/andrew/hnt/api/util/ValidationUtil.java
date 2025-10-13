package com.andrew.hnt.api.util;

import com.andrew.hnt.api.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 공통 검증 유틸리티 클래스
 * HnT Sensor API 프로젝트 전용
 * 모든 컨트롤러에서 일관된 검증 로직 제공
 */
@Component
public class ValidationUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationUtil.class);
    
    // ==================== 기본 검증 메서드 ====================
    
    /**
     * 문자열이 null이거나 비어있는지 검증
     * @param value 검증할 문자열
     * @param fieldName 필드명 (에러 메시지용)
     * @return 검증 결과
     */
    public static ValidationResult validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.error(fieldName + "은(는) 필수 입력 항목입니다.");
        }
        return ValidationResult.success();
    }
    
    /**
     * 객체가 null인지 검증
     * @param value 검증할 객체
     * @param fieldName 필드명 (에러 메시지용)
     * @return 검증 결과
     */
    public static ValidationResult validateNotNull(Object value, String fieldName) {
        if (value == null) {
            return ValidationResult.error(fieldName + "은(는) 필수 입력 항목입니다.");
        }
        return ValidationResult.success();
    }
    
    /**
     * 정규식 패턴으로 문자열 검증
     * @param value 검증할 문자열
     * @param pattern 정규식 패턴
     * @param fieldName 필드명 (에러 메시지용)
     * @param errorMessage 에러 메시지
     * @return 검증 결과
     */
    public static ValidationResult validatePattern(String value, String pattern, String fieldName, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.error(fieldName + "은(는) 필수 입력 항목입니다.");
        }
        
        if (!Pattern.matches(pattern, value)) {
            return ValidationResult.error(errorMessage);
        }
        
        return ValidationResult.success();
    }
    
    // ==================== 사용자 정보 검증 ====================
    
    /**
     * 사용자 ID 검증
     * @param userId 사용자 ID
     * @return 검증 결과
     */
    public static ValidationResult validateUserId(String userId) {
        return validatePattern(userId, Constants.Patterns.USER_ID, "사용자 ID", 
            "사용자 ID는 영문, 숫자, 언더스코어, 하이픈만 사용 가능하며 3-20자리여야 합니다.");
    }
    
    /**
     * UUID 검증
     * @param uuid UUID 문자열
     * @return 검증 결과
     */
    public static ValidationResult validateUuid(String uuid) {
        return validatePattern(uuid, Constants.Patterns.UUID, "UUID", 
            "유효하지 않은 UUID 형식입니다.");
    }
    
    /**
     * 이메일 검증
     * @param email 이메일 주소
     * @return 검증 결과
     */
    public static ValidationResult validateEmail(String email) {
        return validatePattern(email, Constants.Patterns.EMAIL, "이메일", 
            "유효하지 않은 이메일 형식입니다.");
    }
    
    /**
     * 전화번호 검증
     * @param phone 전화번호
     * @return 검증 결과
     */
    public static ValidationResult validatePhone(String phone) {
        return validatePattern(phone, Constants.Patterns.PHONE, "전화번호", 
            "유효하지 않은 전화번호 형식입니다. (예: 010-1234-5678)");
    }
    
    /**
     * 사용자 등급 검증
     * @param userGrade 사용자 등급
     * @return 검증 결과
     */
    public static ValidationResult validateUserGrade(String userGrade) {
        if (userGrade == null || userGrade.trim().isEmpty()) {
            return ValidationResult.error("사용자 등급은 필수 입력 항목입니다.");
        }
        
        if (!Constants.UserGrade.ADMIN.equals(userGrade) && 
            !Constants.UserGrade.USER.equals(userGrade) && 
            !Constants.UserGrade.SUB_USER.equals(userGrade)) {
            return ValidationResult.error("유효하지 않은 사용자 등급입니다. (A, U, B 중 하나)");
        }
        
        return ValidationResult.success();
    }
    
    // ==================== 센서 데이터 검증 ====================
    
    /**
     * 센서 값 검증
     * @param sensorValue 센서 값
     * @return 검증 결과
     */
    public static ValidationResult validateSensorValue(String sensorValue) {
        if (sensorValue == null || sensorValue.trim().isEmpty()) {
            return ValidationResult.error("센서 값은 필수 입력 항목입니다.");
        }
        
        // Error 값은 허용 (센서 연결 오류 상태)
        if (Constants.Sensor.ERROR_VALUE.equals(sensorValue)) {
            return ValidationResult.success();
        }
        
        try {
            double value = Double.parseDouble(sensorValue);
            if (value < Constants.Sensor.MIN_TEMP_VALUE || value > Constants.Sensor.MAX_TEMP_VALUE) {
                return ValidationResult.error("센서 값이 허용 범위를 벗어났습니다. (" + 
                    Constants.Sensor.MIN_TEMP_VALUE + " ~ " + Constants.Sensor.MAX_TEMP_VALUE + ")");
            }
        } catch (NumberFormatException e) {
            return ValidationResult.error("센서 값은 숫자여야 합니다.");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 센서 타입 검증
     * @param sensorType 센서 타입
     * @return 검증 결과
     */
    public static ValidationResult validateSensorType(String sensorType) {
        if (sensorType == null || sensorType.trim().isEmpty()) {
            return ValidationResult.error("센서 타입은 필수 입력 항목입니다.");
        }
        
        // 유효한 센서 타입 목록
        String[] validTypes = {
            Constants.Sensor.TYPE_TEMP, Constants.Sensor.TYPE_HUMIDITY, 
            Constants.Sensor.TYPE_PRESSURE, Constants.Sensor.TYPE_CO2,
            Constants.Sensor.TYPE_PT100, Constants.Sensor.TYPE_4_20MA,
            Constants.Sensor.TYPE_ALARM, Constants.Sensor.TYPE_H_L,
            Constants.Sensor.TYPE_COUNTER, Constants.Sensor.TYPE_FREQ
        };
        
        for (String validType : validTypes) {
            if (validType.equals(sensorType)) {
                return ValidationResult.success();
            }
        }
        
        return ValidationResult.error("유효하지 않은 센서 타입입니다.");
    }
    
    // ==================== MQTT 메시지 검증 ====================
    
    /**
     * MQTT 토픽 검증
     * @param topic MQTT 토픽
     * @return 검증 결과
     */
    public static ValidationResult validateMqttTopic(String topic) {
        return validatePattern(topic, Constants.Patterns.MQTT_TOPIC, "MQTT 토픽", 
            "유효하지 않은 MQTT 토픽 형식입니다. (예: HBEE/userId/TC/uuid/SER)");
    }
    
    /**
     * MQTT 액션 코드 검증
     * @param actCode 액션 코드
     * @return 검증 결과
     */
    public static ValidationResult validateMqttActCode(String actCode) {
        if (actCode == null || actCode.trim().isEmpty()) {
            return ValidationResult.error("액션 코드는 필수 입력 항목입니다.");
        }
        
        if (!Constants.Sensor.ACT_CODE_LIVE.equals(actCode) && 
            !Constants.Sensor.ACT_CODE_SETRES.equals(actCode) && 
            !Constants.Sensor.ACT_CODE_ACTRES.equals(actCode)) {
            return ValidationResult.error("유효하지 않은 액션 코드입니다. (live, setres, actres 중 하나)");
        }
        
        return ValidationResult.success();
    }
    
    // ==================== 보안 검증 ====================
    
    /**
     * XSS 공격 방지 검증
     * @param input 입력 문자열
     * @param fieldName 필드명
     * @return 검증 결과
     */
    public static ValidationResult validateXssPrevention(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            return ValidationResult.success();
        }
        
        // XSS 패턴 검사
        String[] xssPatterns = {
            Constants.Patterns.SCRIPT_TAG,
            Constants.Patterns.JAVASCRIPT_PROTOCOL,
            Constants.Patterns.ONLOAD_ATTRIBUTE,
            Constants.Patterns.ONERROR_ATTRIBUTE,
            Constants.Patterns.ONCLICK_ATTRIBUTE
        };
        
        for (String pattern : xssPatterns) {
            if (Pattern.matches(".*" + pattern + ".*", input)) {
                logger.warn("XSS 공격 시도 감지 - 필드: {}, 입력값: {}", fieldName, input);
                return ValidationResult.error("잘못된 입력이 감지되었습니다.");
            }
        }
        
        return ValidationResult.success();
    }
    
    /**
     * SQL Injection 방지 검증
     * @param input 입력 문자열
     * @param fieldName 필드명
     * @return 검증 결과
     */
    public static ValidationResult validateSqlInjectionPrevention(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            return ValidationResult.success();
        }
        
        // SQL Injection 패턴 검사
        String[] sqlPatterns = {
            Constants.Patterns.SQL_INJECTION,
            Constants.Patterns.SQL_COMMENT,
            Constants.Patterns.SQL_QUOTE
        };
        
        for (String pattern : sqlPatterns) {
            if (Pattern.matches(".*" + pattern + ".*", input)) {
                logger.warn("SQL Injection 공격 시도 감지 - 필드: {}, 입력값: {}", fieldName, input);
                return ValidationResult.error("잘못된 입력이 감지되었습니다.");
            }
        }
        
        return ValidationResult.success();
    }
    
    // ==================== 복합 검증 ====================
    
    /**
     * 사용자 정보 전체 검증
     * @param userId 사용자 ID
     * @param userNm 사용자 이름
     * @param userPass 비밀번호
     * @param userEmail 이메일 (선택적)
     * @param userTel 전화번호 (선택적)
     * @return 검증 결과
     */
    public static ValidationResult validateUserInfo(String userId, String userNm, String userPass, 
                                                   String userEmail, String userTel) {
        // 필수 항목 검증
        ValidationResult result = validateUserId(userId);
        if (!result.isValid()) return result;
        
        result = validateRequired(userNm, "사용자 이름");
        if (!result.isValid()) return result;
        
        result = validateRequired(userPass, "비밀번호");
        if (!result.isValid()) return result;
        
        // 선택적 항목 검증
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            result = validateEmail(userEmail);
            if (!result.isValid()) return result;
        }
        
        if (userTel != null && !userTel.trim().isEmpty()) {
            result = validatePhone(userTel);
            if (!result.isValid()) return result;
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 센서 설정 데이터 검증
     * @param sensorUuid 센서 UUID
     * @param sensorName 센서 이름
     * @param sensorType 센서 타입
     * @return 검증 결과
     */
    public static ValidationResult validateSensorInfo(String sensorUuid, String sensorName, String sensorType) {
        ValidationResult result = validateUuid(sensorUuid);
        if (!result.isValid()) return result;
        
        result = validateRequired(sensorName, "센서 이름");
        if (!result.isValid()) return result;
        
        result = validateSensorType(sensorType);
        if (!result.isValid()) return result;
        
        return ValidationResult.success();
    }
    
    // ==================== 검증 결과 클래스 ====================
    
    /**
     * 검증 결과를 담는 내부 클래스
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public Map<String, Object> toResponseMap() {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("success", valid);
            if (!valid) {
                result.put("message", errorMessage);
            }
            return result;
        }
    }
}
