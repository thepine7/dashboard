package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * JSON 유효성 검사 유틸리티 클래스
 * HnT Sensor API 프로젝트 전용
 * 
 * MQTT 메시지 JSON 파싱 전 유효성 검사를 강화하고
 * 안전한 JSON 처리를 제공
 */
@Component
public class JsonValidationUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonValidationUtil.class);
    
    // JSON 파서 (싱글톤)
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // MQTT 메시지 필수 필드 정의
    private static final String[] REQUIRED_FIELDS = {"actcode"};
    
    // MQTT 메시지 선택적 필드 정의
    private static final String[] OPTIONAL_FIELDS = {
        "name", "type", "ch", "value", 
        "p01", "p02", "p03", "p04", "p05", "p06", "p07", "p08", 
        "p09", "p10", "p11", "p12", "p13", "p14", "p15", "p16"
    };
    
    // actcode 유효값 정의
    private static final String[] VALID_ACTCODES = {
        "live", "setres", "actres", "error", "status"
    };
    
    // name 유효값 정의
    private static final String[] VALID_NAMES = {
        "ain", "din", "output", "forcedef", "userId"
    };
    
    // UUID 패턴 정의
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", 
        Pattern.CASE_INSENSITIVE
    );
    
    // 숫자 패턴 정의
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    
    // JSON 유효성 검사 결과 클래스
    public static class JsonValidationResult {
        private final boolean valid;
        private final String reason;
        private final JsonNode data;
        private final Map<String, Object> additionalInfo;
        
        public JsonValidationResult(boolean valid, String reason, JsonNode data) {
            this.valid = valid;
            this.reason = reason;
            this.data = data;
            this.additionalInfo = new HashMap<>();
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getReason() {
            return reason;
        }
        
        public JsonNode getData() {
            return data;
        }
        
        public Map<String, Object> getAdditionalInfo() {
            return additionalInfo;
        }
        
        public void addInfo(String key, Object value) {
            this.additionalInfo.put(key, value);
        }
    }
    
    /**
     * JSON 문자열 유효성 검사 및 파싱
     * @param jsonString JSON 문자열
     * @return JSON 유효성 검사 결과
     */
    public static JsonValidationResult validateAndParseJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new JsonValidationResult(false, "JSON 문자열이 null이거나 비어있습니다", null);
        }
        
        try {
            // 1단계: 기본 JSON 파싱
            JsonNode rootNode = objectMapper.readTree(jsonString);
            
            // 2단계: 필수 필드 검증
            JsonValidationResult fieldValidation = validateRequiredFields(rootNode);
            if (!fieldValidation.isValid()) {
                return fieldValidation;
            }
            
            // 3단계: 필드 값 검증
            JsonValidationResult valueValidation = validateFieldValues(rootNode);
            if (!valueValidation.isValid()) {
                return valueValidation;
            }
            
            // 4단계: 구조 검증
            JsonValidationResult structureValidation = validateStructure(rootNode);
            if (!structureValidation.isValid()) {
                return structureValidation;
            }
            
            // 모든 검증 통과
            JsonValidationResult result = new JsonValidationResult(true, "JSON 유효성 검사 통과", rootNode);
            result.addInfo("fieldCount", rootNode.size());
            result.addInfo("hasRequiredFields", true);
            
            return result;
            
        } catch (JsonProcessingException e) {
            logger.warn("JSON 파싱 실패: {}", e.getMessage());
            return new JsonValidationResult(false, "JSON 파싱 실패: " + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("JSON 유효성 검사 중 예외 발생", e);
            return new JsonValidationResult(false, "JSON 유효성 검사 중 예외 발생: " + e.getMessage(), null);
        }
    }
    
    /**
     * 필수 필드 검증
     * @param rootNode JSON 루트 노드
     * @return 검증 결과
     */
    private static JsonValidationResult validateRequiredFields(JsonNode rootNode) {
        for (String field : REQUIRED_FIELDS) {
            if (!rootNode.has(field) || rootNode.get(field).isNull()) {
                return new JsonValidationResult(false, 
                    "필수 필드가 누락되었습니다: " + field, null);
            }
        }
        return new JsonValidationResult(true, "필수 필드 검증 통과", rootNode);
    }
    
    /**
     * 필드 값 검증
     * @param rootNode JSON 루트 노드
     * @return 검증 결과
     */
    private static JsonValidationResult validateFieldValues(JsonNode rootNode) {
        // actcode 검증
        if (rootNode.has("actcode")) {
            String actcode = rootNode.get("actcode").asText();
            if (!isValidActcode(actcode)) {
                return new JsonValidationResult(false, 
                    "유효하지 않은 actcode: " + actcode, null);
            }
        }
        
        // name 검증
        if (rootNode.has("name")) {
            String name = rootNode.get("name").asText();
            if (!isValidName(name)) {
                return new JsonValidationResult(false, 
                    "유효하지 않은 name: " + name, null);
            }
        }
        
        // value 검증 (문자열이어야 함)
        if (rootNode.has("value")) {
            JsonNode valueNode = rootNode.get("value");
            if (!valueNode.isTextual()) {
                return new JsonValidationResult(false, 
                    "value 필드는 문자열이어야 합니다", null);
            }
        }
        
        // p01-p16 파라미터 검증
        for (int i = 1; i <= 16; i++) {
            String paramName = "p" + String.format("%02d", i);
            if (rootNode.has(paramName)) {
                JsonNode paramNode = rootNode.get(paramName);
                if (!paramNode.isTextual()) {
                    return new JsonValidationResult(false, 
                        paramName + " 필드는 문자열이어야 합니다", null);
                }
                
                String paramValue = paramNode.asText();
                if (!isValidParameterValue(paramValue)) {
                    return new JsonValidationResult(false, 
                        "유효하지 않은 " + paramName + " 값: " + paramValue, null);
                }
            }
        }
        
        return new JsonValidationResult(true, "필드 값 검증 통과", rootNode);
    }
    
    /**
     * JSON 구조 검증
     * @param rootNode JSON 루트 노드
     * @return 검증 결과
     */
    private static JsonValidationResult validateStructure(JsonNode rootNode) {
        // 루트 노드는 객체여야 함
        if (!rootNode.isObject()) {
            return new JsonValidationResult(false, 
                "JSON 루트는 객체여야 합니다", null);
        }
        
        // 필드 개수 검증 (너무 많은 필드가 있으면 의심)
        if (rootNode.size() > 20) {
            return new JsonValidationResult(false, 
                "필드 개수가 너무 많습니다: " + rootNode.size(), null);
        }
        
        // 중첩 객체나 배열이 있으면 의심
        for (JsonNode field : rootNode) {
            if (field.isObject() || field.isArray()) {
                return new JsonValidationResult(false, 
                    "중첩된 객체나 배열은 허용되지 않습니다", null);
            }
        }
        
        return new JsonValidationResult(true, "구조 검증 통과", rootNode);
    }
    
    /**
     * actcode 유효성 검사
     * @param actcode actcode 값
     * @return 유효성 여부
     */
    private static boolean isValidActcode(String actcode) {
        if (actcode == null || actcode.trim().isEmpty()) {
            return false;
        }
        
        for (String validActcode : VALID_ACTCODES) {
            if (validActcode.equals(actcode)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * name 유효성 검사
     * @param name name 값
     * @return 유효성 여부
     */
    private static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        for (String validName : VALID_NAMES) {
            if (validName.equals(name)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 파라미터 값 유효성 검사
     * @param value 파라미터 값
     * @return 유효성 여부
     */
    private static boolean isValidParameterValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        // 숫자 형식 검증
        if (NUMBER_PATTERN.matcher(value).matches()) {
            try {
                double numValue = Double.parseDouble(value);
                // 범위 검증 (-10000 ~ 10000)
                return numValue >= -10000 && numValue <= 10000;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // 문자열 길이 검증 (최대 100자)
        return value.length() <= 100;
    }
    
    /**
     * UUID 유효성 검사
     * @param uuid UUID 문자열
     * @return 유효성 여부
     */
    public static boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }
        
        return UUID_PATTERN.matcher(uuid.trim()).matches();
    }
    
    /**
     * JSON 문자열 안전성 검사
     * @param jsonString JSON 문자열
     * @return 안전성 여부
     */
    public static boolean isSafeJsonString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        
        // 길이 검증 (최대 10KB)
        if (jsonString.length() > 10240) {
            return false;
        }
        
        // 위험한 문자 검증
        String[] dangerousChars = {"<script", "javascript:", "onload=", "onerror=", "eval("};
        String lowerJson = jsonString.toLowerCase();
        
        for (String dangerous : dangerousChars) {
            if (lowerJson.contains(dangerous)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * JSON 메시지 타입 분류
     * @param rootNode JSON 루트 노드
     * @return 메시지 타입
     */
    public static String classifyMessageType(JsonNode rootNode) {
        if (!rootNode.has("actcode")) {
            return "UNKNOWN";
        }
        
        String actcode = rootNode.get("actcode").asText();
        
        switch (actcode) {
            case "live":
                return "LIVE_DATA";
            case "setres":
                return "SETTING_RESPONSE";
            case "actres":
                return "ACTION_RESPONSE";
            case "error":
                return "ERROR_MESSAGE";
            case "status":
                return "STATUS_MESSAGE";
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * JSON 메시지 통계 생성
     * @param rootNode JSON 루트 노드
     * @return 메시지 통계
     */
    public static Map<String, Object> generateMessageStats(JsonNode rootNode) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("fieldCount", rootNode.size());
        stats.put("hasActcode", rootNode.has("actcode"));
        stats.put("hasName", rootNode.has("name"));
        stats.put("hasValue", rootNode.has("value"));
        stats.put("hasParameters", hasParameters(rootNode));
        stats.put("messageType", classifyMessageType(rootNode));
        
        // 파라미터 개수 계산
        int paramCount = 0;
        for (int i = 1; i <= 16; i++) {
            String paramName = "p" + String.format("%02d", i);
            if (rootNode.has(paramName)) {
                paramCount++;
            }
        }
        stats.put("parameterCount", paramCount);
        
        return stats;
    }
    
    /**
     * 파라미터 존재 여부 확인
     * @param rootNode JSON 루트 노드
     * @return 파라미터 존재 여부
     */
    private static boolean hasParameters(JsonNode rootNode) {
        for (int i = 1; i <= 16; i++) {
            String paramName = "p" + String.format("%02d", i);
            if (rootNode.has(paramName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * JSON 유효성 검사 통계
     */
    private static long totalValidations = 0;
    private static long successfulValidations = 0;
    private static long failedValidations = 0;
    
    /**
     * 검증 통계 조회
     * @return 검증 통계
     */
    public static Map<String, Object> getValidationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalValidations", totalValidations);
        stats.put("successfulValidations", successfulValidations);
        stats.put("failedValidations", failedValidations);
        stats.put("successRate", totalValidations > 0 ? 
            (double) successfulValidations / totalValidations * 100 : 0);
        return stats;
    }
    
    /**
     * 검증 통계 리셋
     */
    public static void resetValidationStats() {
        totalValidations = 0;
        successfulValidations = 0;
        failedValidations = 0;
    }
}
