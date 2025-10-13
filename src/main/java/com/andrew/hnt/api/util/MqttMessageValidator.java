package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.Map;

/**
 * MQTT 메시지 검증 클래스
 * HnT Sensor API 프로젝트
 * 
 * 주요 기능:
 * - MQTT 토픽 형식 검증
 * - 메시지 페이로드 검증
 * - 보안 위협 방지
 * - 입력값 정제
 */
@Component
public class MqttMessageValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttMessageValidator.class);
    
    // 토픽 패턴 검증
    private static final Pattern TOPIC_PATTERN = Pattern.compile("^HBEE/[a-zA-Z0-9_-]+/TC/[a-zA-Z0-9_-]+/(SER|DEV)$");
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    
    // 메시지 페이로드 패턴 검증
    private static final Pattern SET_PAYLOAD_PATTERN = Pattern.compile("^SET&p\\d{2}=[0-9.-]+(?:&p\\d{2}=[0-9.-]+)*$");
    private static final Pattern GET_PAYLOAD_PATTERN = Pattern.compile("^GET&type=[12]$");
    private static final Pattern ACT_PAYLOAD_PATTERN = Pattern.compile("^ACT&name=(forcedef|output|userId)&(?:value=[01]|type=\\d+&ch=\\d+&value=[01])$");
    
    // 허용되는 센서 타입
    private static final String[] ALLOWED_SENSOR_TYPES = {
        "temp", "humidity", "pressure", "co2", "pt100", "4-20ma", "alarm", "h/l", "counter", "freq",
        "comp", "def", "fan", "dout", "rout", "aout"
    };
    
    // 허용되는 액션 코드
    private static final String[] ALLOWED_ACT_CODES = {
        "live", "setres", "actres"
    };
    
    /**
     * MQTT 토픽 검증
     */
    public boolean isValidTopic(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            logger.warn("토픽이 비어있습니다.");
            return false;
        }
        
        if (!TOPIC_PATTERN.matcher(topic).matches()) {
            logger.warn("잘못된 토픽 형식: {}", topic);
            return false;
        }
        
        return true;
    }
    
    /**
     * 사용자 ID 검증
     */
    public boolean isValidUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("사용자 ID가 비어있습니다.");
            return false;
        }
        
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            logger.warn("잘못된 사용자 ID 형식: {}", userId);
            return false;
        }
        
        // 길이 제한 (보안)
        if (userId.length() > 50) {
            logger.warn("사용자 ID가 너무 깁니다: {}", userId);
            return false;
        }
        
        return true;
    }
    
    /**
     * UUID 검증
     */
    public boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            logger.warn("UUID가 비어있습니다.");
            return false;
        }
        
        if (!UUID_PATTERN.matcher(uuid).matches()) {
            logger.warn("잘못된 UUID 형식: {}", uuid);
            return false;
        }
        
        // 길이 제한 (보안)
        if (uuid.length() > 100) {
            logger.warn("UUID가 너무 깁니다: {}", uuid);
            return false;
        }
        
        return true;
    }
    
    /**
     * SET 페이로드 검증
     */
    public boolean isValidSetPayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            logger.warn("SET 페이로드가 비어있습니다.");
            return false;
        }
        
        if (!SET_PAYLOAD_PATTERN.matcher(payload).matches()) {
            logger.warn("잘못된 SET 페이로드 형식: {}", payload);
            return false;
        }
        
        // 파라미터 값 범위 검증
        return validateParameterValues(payload);
    }
    
    /**
     * GET 페이로드 검증
     */
    public boolean isValidGetPayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            logger.warn("GET 페이로드가 비어있습니다.");
            return false;
        }
        
        return GET_PAYLOAD_PATTERN.matcher(payload).matches();
    }
    
    /**
     * ACT 페이로드 검증
     */
    public boolean isValidActPayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            logger.warn("ACT 페이로드가 비어있습니다.");
            return false;
        }
        
        return ACT_PAYLOAD_PATTERN.matcher(payload).matches();
    }
    
    /**
     * JSON 메시지 검증
     */
    public boolean isValidJsonMessage(Map<String, Object> jsonMessage) {
        if (jsonMessage == null || jsonMessage.isEmpty()) {
            logger.warn("JSON 메시지가 비어있습니다.");
            return false;
        }
        
        // 필수 필드 검증
        if (!jsonMessage.containsKey("actcode")) {
            logger.warn("actcode 필드가 없습니다.");
            return false;
        }
        
        String actcode = String.valueOf(jsonMessage.get("actcode"));
        if (!isValidActCode(actcode)) {
            logger.warn("잘못된 actcode: {}", actcode);
            return false;
        }
        
        // actcode별 추가 검증
        switch (actcode) {
            case "live":
                return validateLiveMessage(jsonMessage);
            case "setres":
                return validateSetResMessage(jsonMessage);
            case "actres":
                return validateActResMessage(jsonMessage);
            default:
                logger.warn("알 수 없는 actcode: {}", actcode);
                return false;
        }
    }
    
    /**
     * live 메시지 검증
     */
    private boolean validateLiveMessage(Map<String, Object> message) {
        if (!message.containsKey("name") || !message.containsKey("value")) {
            logger.warn("live 메시지에 필수 필드가 없습니다.");
            return false;
        }
        
        String name = String.valueOf(message.get("name"));
        String value = String.valueOf(message.get("value"));
        
        // name 검증
        if (!isValidSensorName(name)) {
            logger.warn("잘못된 센서 이름: {}", name);
            return false;
        }
        
        // value 검증
        if (!isValidSensorValue(value)) {
            logger.warn("잘못된 센서 값: {}", value);
            return false;
        }
        
        return true;
    }
    
    /**
     * setres 메시지 검증
     */
    private boolean validateSetResMessage(Map<String, Object> message) {
        // p01~p16 파라미터 검증
        for (int i = 1; i <= 16; i++) {
            String paramKey = "p" + String.format("%02d", i);
            if (message.containsKey(paramKey)) {
                String value = String.valueOf(message.get(paramKey));
                if (!isValidParameterValue(value)) {
                    logger.warn("잘못된 파라미터 값: {} = {}", paramKey, value);
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * actres 메시지 검증
     */
    private boolean validateActResMessage(Map<String, Object> message) {
        if (!message.containsKey("name")) {
            logger.warn("actres 메시지에 name 필드가 없습니다.");
            return false;
        }
        
        String name = String.valueOf(message.get("name"));
        if (!isValidActionName(name)) {
            logger.warn("잘못된 액션 이름: {}", name);
            return false;
        }
        
        return true;
    }
    
    /**
     * 센서 이름 검증
     */
    private boolean isValidSensorName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        for (String allowedType : ALLOWED_SENSOR_TYPES) {
            if (allowedType.equals(name.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 센서 값 검증
     */
    private boolean isValidSensorValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        // "Error" 문자열 허용
        if ("Error".equals(value)) {
            return true;
        }
        
        // 숫자 값 검증
        try {
            double numValue = Double.parseDouble(value);
            // 범위 검증 (-200 ~ 1000)
            return numValue >= -200 && numValue <= 1000;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 액션 코드 검증
     */
    private boolean isValidActCode(String actcode) {
        if (actcode == null || actcode.trim().isEmpty()) {
            return false;
        }
        
        for (String allowedCode : ALLOWED_ACT_CODES) {
            if (allowedCode.equals(actcode)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 액션 이름 검증
     */
    private boolean isValidActionName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        String[] allowedActions = {"forcedef", "output", "userId"};
        for (String allowedAction : allowedActions) {
            if (allowedAction.equals(name)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 파라미터 값 검증
     */
    private boolean validateParameterValues(String payload) {
        try {
            String[] pairs = payload.split("&");
            for (String pair : pairs) {
                if (pair.startsWith("p")) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        String value = keyValue[1];
                        if (!isValidParameterValue(value)) {
                            logger.warn("잘못된 파라미터 값: {}", pair);
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.warn("파라미터 값 검증 중 오류: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 개별 파라미터 값 검증
     */
    private boolean isValidParameterValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        try {
            double numValue = Double.parseDouble(value);
            // 파라미터별 범위 검증 (기본적으로 -1000 ~ 1000)
            return numValue >= -1000 && numValue <= 1000;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 메시지 정제 (XSS, SQL Injection 방지)
     */
    public String sanitizeMessage(String message) {
        if (message == null) {
            return null;
        }
        
        // HTML 태그 제거
        String sanitized = message.replaceAll("<[^>]*>", "");
        
        // SQL Injection 패턴 제거
        sanitized = sanitized.replaceAll("(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)", "");
        
        // 스크립트 태그 제거
        sanitized = sanitized.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        
        // 특수 문자 정제
        sanitized = sanitized.replaceAll("[<>\"'&]", "");
        
        return sanitized.trim();
    }
    
    /**
     * 토픽에서 사용자 ID 추출
     */
    public String extractUserIdFromTopic(String topic) {
        if (!isValidTopic(topic)) {
            return null;
        }
        
        try {
            String[] parts = topic.split("/");
            if (parts.length >= 2) {
                return parts[1];
            }
        } catch (Exception e) {
            logger.warn("토픽에서 사용자 ID 추출 실패: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 토픽에서 UUID 추출
     */
    public String extractUuidFromTopic(String topic) {
        if (!isValidTopic(topic)) {
            return null;
        }
        
        try {
            String[] parts = topic.split("/");
            if (parts.length >= 4) {
                return parts[3];
            }
        } catch (Exception e) {
            logger.warn("토픽에서 UUID 추출 실패: {}", e.getMessage());
        }
        
        return null;
    }
    
    
    /**
     * 보안 위협 검증
     */
    public boolean hasSecurityThreat(String message) {
        if (message == null) {
            return false;
        }
        
        // SQL 인젝션 패턴 검증
        String[] sqlPatterns = {
            "(?i)(union|select|insert|update|delete|drop|create|alter|exec)",
            "(?i)(or|and)\\s+\\d+\\s*=\\s*\\d+",
            "(?i)(or|and)\\s+['\"]\\s*=\\s*['\"]",
            "(?i)union\\s+select",
            "(?i)drop\\s+table",
            "(?i)insert\\s+into",
            "(?i)update\\s+set",
            "(?i)delete\\s+from"
        };
        
        for (String pattern : sqlPatterns) {
            if (message.matches(".*" + pattern + ".*")) {
                logger.warn("SQL 인젝션 패턴 감지: {}", message);
                return true;
            }
        }
        
        // XSS 패턴 검증
        String[] xssPatterns = {
            "(?i)<script[^>]*>.*?</script>",
            "(?i)<iframe[^>]*>.*?</iframe>",
            "(?i)<object[^>]*>.*?</object>",
            "(?i)<embed[^>]*>.*?</embed>",
            "(?i)<link[^>]*>.*?</link>",
            "(?i)<meta[^>]*>.*?</meta>",
            "(?i)javascript:",
            "(?i)vbscript:",
            "(?i)onload\\s*=",
            "(?i)onerror\\s*=",
            "(?i)onclick\\s*="
        };
        
        for (String pattern : xssPatterns) {
            if (message.matches(".*" + pattern + ".*")) {
                logger.warn("XSS 패턴 감지: {}", message);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 메시지 길이 검증
     */
    public boolean isValidMessageLength(String message) {
        if (message == null) {
            return false;
        }
        
        // 최대 길이 제한 (1KB)
        if (message.length() > 1024) {
            logger.warn("메시지가 너무 깁니다: {} bytes", message.length());
            return false;
        }
        
        return true;
    }
    
    /**
     * 토픽 길이 검증
     */
    public boolean isValidTopicLength(String topic) {
        if (topic == null) {
            return false;
        }
        
        // 최대 길이 제한 (200자)
        if (topic.length() > 200) {
            logger.warn("토픽이 너무 깁니다: {} bytes", topic.length());
            return false;
        }
        
        return true;
    }
}
