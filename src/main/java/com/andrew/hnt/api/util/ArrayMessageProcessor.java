package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * 배열 형태 메시지 처리 유틸리티 클래스
 * HnT Sensor API 프로젝트 전용
 * 
 * MQTT 메시지 중 배열 형태로 수신되는 메시지를
 * 안전하고 효율적으로 처리
 */
@Component
public class ArrayMessageProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ArrayMessageProcessor.class);
    
    // JSON 파서 (싱글톤)
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 배열 처리 통계
    private static long totalArrayMessages = 0;
    private static long successfulArrayProcesses = 0;
    private static long failedArrayProcesses = 0;
    
    // 배열 형태 메시지 처리 결과 클래스
    public static class ArrayProcessingResult {
        private final boolean success;
        private final String reason;
        private final JsonNode processedData;
        private final int originalArraySize;
        private final int processedElementCount;
        private final Map<String, Object> additionalInfo;
        
        public ArrayProcessingResult(boolean success, String reason, JsonNode processedData, 
                                   int originalArraySize, int processedElementCount) {
            this.success = success;
            this.reason = reason;
            this.processedData = processedData;
            this.originalArraySize = originalArraySize;
            this.processedElementCount = processedElementCount;
            this.additionalInfo = new HashMap<>();
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getReason() {
            return reason;
        }
        
        public JsonNode getProcessedData() {
            return processedData;
        }
        
        public int getOriginalArraySize() {
            return originalArraySize;
        }
        
        public int getProcessedElementCount() {
            return processedElementCount;
        }
        
        public Map<String, Object> getAdditionalInfo() {
            return additionalInfo;
        }
        
        public void addInfo(String key, Object value) {
            this.additionalInfo.put(key, value);
        }
    }
    
    /**
     * 배열 형태 메시지 처리
     * @param jsonString JSON 문자열
     * @return 배열 처리 결과
     */
    public static ArrayProcessingResult processArrayMessage(String jsonString) {
        totalArrayMessages++;
        
        try {
            // 1. 기본 유효성 검사
            if (jsonString == null || jsonString.trim().isEmpty()) {
                failedArrayProcesses++;
                return new ArrayProcessingResult(false, "JSON 문자열이 null이거나 비어있음", 
                    null, 0, 0);
            }
            
            // 2. 배열 형태 검사
            String trimmedJson = jsonString.trim();
            if (!trimmedJson.startsWith("[") || !trimmedJson.endsWith("]")) {
                failedArrayProcesses++;
                return new ArrayProcessingResult(false, "배열 형태가 아님", 
                    null, 0, 0);
            }
            
            // 3. JSON 파싱
            JsonNode arrayNode = objectMapper.readTree(trimmedJson);
            if (!arrayNode.isArray()) {
                failedArrayProcesses++;
                return new ArrayProcessingResult(false, "파싱된 데이터가 배열이 아님", 
                    null, 0, 0);
            }
            
            int arraySize = arrayNode.size();
            if (arraySize == 0) {
                failedArrayProcesses++;
                return new ArrayProcessingResult(false, "빈 배열", 
                    null, 0, 0);
            }
            
            // 4. 배열 크기 제한 검사
            if (arraySize > 100) {
                failedArrayProcesses++;
                return new ArrayProcessingResult(false, "배열 크기가 너무 큼: " + arraySize, 
                    null, arraySize, 0);
            }
            
            // 5. 배열 요소 처리 전략 선택
            ArrayProcessingResult result = processArrayElements(arrayNode);
            
            if (result.isSuccess()) {
                successfulArrayProcesses++;
            } else {
                failedArrayProcesses++;
            }
            
            return result;
            
        } catch (JsonProcessingException e) {
            failedArrayProcesses++;
            logger.warn("배열 JSON 파싱 실패: {}", e.getMessage());
            return new ArrayProcessingResult(false, "JSON 파싱 실패: " + e.getMessage(), 
                null, 0, 0);
        } catch (Exception e) {
            failedArrayProcesses++;
            logger.error("배열 메시지 처리 중 예외 발생", e);
            return new ArrayProcessingResult(false, "처리 중 예외 발생: " + e.getMessage(), 
                null, 0, 0);
        }
    }
    
    /**
     * 배열 요소 처리
     * @param arrayNode 배열 JSON 노드
     * @return 처리 결과
     */
    private static ArrayProcessingResult processArrayElements(JsonNode arrayNode) {
        int arraySize = arrayNode.size();
        List<JsonNode> validElements = new ArrayList<>();
        List<String> invalidReasons = new ArrayList<>();
        
        // 각 배열 요소 검증 및 처리
        for (int i = 0; i < arraySize; i++) {
            JsonNode element = arrayNode.get(i);
            
            if (element == null || element.isNull()) {
                invalidReasons.add("요소[" + i + "]: null");
                continue;
            }
            
            if (!element.isObject()) {
                invalidReasons.add("요소[" + i + "]: 객체가 아님");
                continue;
            }
            
            // JSON 유효성 검사 적용
            JsonValidationUtil.JsonValidationResult validation = 
                JsonValidationUtil.validateAndParseJson(element.toString());
            
            if (!validation.isValid()) {
                invalidReasons.add("요소[" + i + "]: " + validation.getReason());
                continue;
            }
            
            validElements.add(element);
        }
        
        // 처리 결과 생성
        if (validElements.isEmpty()) {
            return new ArrayProcessingResult(false, 
                "유효한 배열 요소가 없음: " + String.join(", ", invalidReasons), 
                null, arraySize, 0);
        }
        
        // 첫 번째 유효한 요소를 사용 (기존 로직 유지)
        JsonNode firstValidElement = validElements.get(0);
        
        ArrayProcessingResult result = new ArrayProcessingResult(true, 
            "배열 처리 성공 - " + validElements.size() + "개 요소 중 첫 번째 사용", 
            firstValidElement, arraySize, validElements.size());
        
        // 추가 정보 설정
        result.addInfo("validElementCount", validElements.size());
        result.addInfo("invalidElementCount", arraySize - validElements.size());
        result.addInfo("invalidReasons", invalidReasons);
        result.addInfo("processingStrategy", "FIRST_VALID_ELEMENT");
        
        return result;
    }
    
    /**
     * 배열 형태 메시지인지 확인
     * @param jsonString JSON 문자열
     * @return 배열 형태 여부
     */
    public static boolean isArrayMessage(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = jsonString.trim();
        return trimmed.startsWith("[") && trimmed.endsWith("]");
    }
    
    /**
     * 배열 크기 검증
     * @param arraySize 배열 크기
     * @return 유효한 크기 여부
     */
    public static boolean isValidArraySize(int arraySize) {
        return arraySize > 0 && arraySize <= 100;
    }
    
    /**
     * 배열 요소 유효성 검사
     * @param element 배열 요소
     * @return 유효성 여부
     */
    public static boolean isValidArrayElement(JsonNode element) {
        if (element == null || element.isNull()) {
            return false;
        }
        
        if (!element.isObject()) {
            return false;
        }
        
        // JSON 유효성 검사 적용
        JsonValidationUtil.JsonValidationResult validation = 
            JsonValidationUtil.validateAndParseJson(element.toString());
        
        return validation.isValid();
    }
    
    /**
     * 배열 메시지 통계 조회
     * @return 처리 통계
     */
    public static Map<String, Object> getArrayProcessingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalArrayMessages", totalArrayMessages);
        stats.put("successfulArrayProcesses", successfulArrayProcesses);
        stats.put("failedArrayProcesses", failedArrayProcesses);
        stats.put("successRate", totalArrayMessages > 0 ? 
            (double) successfulArrayProcesses / totalArrayMessages * 100 : 0);
        return stats;
    }
    
    /**
     * 배열 처리 통계 리셋
     */
    public static void resetArrayProcessingStats() {
        totalArrayMessages = 0;
        successfulArrayProcesses = 0;
        failedArrayProcesses = 0;
    }
    
    /**
     * 배열 요소 병합 처리 (여러 요소를 하나로 합치는 경우)
     * @param arrayNode 배열 JSON 노드
     * @return 병합된 데이터
     */
    public static JsonNode mergeArrayElements(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.size() == 0) {
            return null;
        }
        
        Map<String, Object> mergedData = new HashMap<>();
        
        // 모든 유효한 요소의 필드를 병합
        for (JsonNode element : arrayNode) {
            if (isValidArrayElement(element)) {
                Iterator<Map.Entry<String, JsonNode>> fields = element.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    JsonNode value = field.getValue();
                    
                    // 중복 키는 나중 값으로 덮어쓰기
                    if (value.isTextual()) {
                        mergedData.put(key, value.asText());
                    } else if (value.isNumber()) {
                        mergedData.put(key, value.asDouble());
                    } else if (value.isBoolean()) {
                        mergedData.put(key, value.asBoolean());
                    }
                }
            }
        }
        
        try {
            return objectMapper.valueToTree(mergedData);
        } catch (Exception e) {
            logger.error("배열 요소 병합 중 오류 발생", e);
            return null;
        }
    }
    
    /**
     * 배열 요소 필터링 (특정 조건에 맞는 요소만 선택)
     * @param arrayNode 배열 JSON 노드
     * @param filterCondition 필터 조건
     * @return 필터링된 요소
     */
    public static List<JsonNode> filterArrayElements(JsonNode arrayNode, String filterCondition) {
        List<JsonNode> filteredElements = new ArrayList<>();
        
        if (arrayNode == null || !arrayNode.isArray()) {
            return filteredElements;
        }
        
        for (JsonNode element : arrayNode) {
            if (isValidArrayElement(element)) {
                // 필터 조건에 따른 처리
                if (filterCondition == null || filterCondition.isEmpty()) {
                    filteredElements.add(element);
                } else {
                    // actcode 기반 필터링
                    if ("actcode_live".equals(filterCondition)) {
                        if (element.has("actcode") && "live".equals(element.get("actcode").asText())) {
                            filteredElements.add(element);
                        }
                    } else if ("actcode_setres".equals(filterCondition)) {
                        if (element.has("actcode") && "setres".equals(element.get("actcode").asText())) {
                            filteredElements.add(element);
                        }
                    } else if ("actcode_actres".equals(filterCondition)) {
                        if (element.has("actcode") && "actres".equals(element.get("actcode").asText())) {
                            filteredElements.add(element);
                        }
                    }
                }
            }
        }
        
        return filteredElements;
    }
}
