package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 파싱 실패 시 예외 처리 및 로깅 강화 유틸리티 클래스
 * HnT Sensor API 프로젝트 전용
 * 
 * MQTT 메시지 파싱 실패 시 상세한 로깅과
 * 통계 수집을 통한 문제 진단 지원
 */
@Component
public class ParsingErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ParsingErrorHandler.class);
    
    // 파싱 오류 통계
    private static final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> errorTypes = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> recentErrors = new ConcurrentHashMap<>();
    
    // 최대 저장할 최근 오류 개수
    private static final int MAX_RECENT_ERRORS = 100;
    
    // 파싱 오류 타입 정의
    public enum ParsingErrorType {
        JSON_PARSE_ERROR("JSON 파싱 오류"),
        TOPIC_PARSE_ERROR("토픽 파싱 오류"),
        ARRAY_PARSE_ERROR("배열 파싱 오류"),
        VALIDATION_ERROR("유효성 검사 오류"),
        FORMAT_ERROR("형식 오류"),
        ENCODING_ERROR("인코딩 오류"),
        UNKNOWN_ERROR("알 수 없는 오류");
        
        private final String description;
        
        ParsingErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 파싱 오류 정보 클래스
    public static class ParsingErrorInfo {
        private final ParsingErrorType errorType;
        private final String errorMessage;
        private final String originalData;
        private final String context;
        private final long timestamp;
        private final Map<String, Object> additionalInfo;
        
        public ParsingErrorInfo(ParsingErrorType errorType, String errorMessage, 
                               String originalData, String context) {
            this.errorType = errorType;
            this.errorMessage = errorMessage;
            this.originalData = originalData;
            this.context = context;
            this.timestamp = System.currentTimeMillis();
            this.additionalInfo = new HashMap<>();
        }
        
        public ParsingErrorType getErrorType() {
            return errorType;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getOriginalData() {
            return originalData;
        }
        
        public String getContext() {
            return context;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public Map<String, Object> getAdditionalInfo() {
            return additionalInfo;
        }
        
        public void addInfo(String key, Object value) {
            this.additionalInfo.put(key, value);
        }
    }
    
    /**
     * 파싱 오류 로깅 및 통계 수집
     * @param errorInfo 오류 정보
     */
    public static void handleParsingError(ParsingErrorInfo errorInfo) {
        try {
            // 1. 오류 통계 업데이트
            updateErrorStatistics(errorInfo);
            
            // 2. 최근 오류 목록에 추가
            addToRecentErrors(errorInfo);
            
            // 3. 상세 로깅
            logParsingError(errorInfo);
            
        } catch (Exception e) {
            logger.error("파싱 오류 처리 중 예외 발생", e);
        }
    }
    
    /**
     * JSON 파싱 오류 처리
     * @param errorMessage 오류 메시지
     * @param originalData 원본 데이터
     * @param context 컨텍스트
     */
    public static void handleJsonParseError(String errorMessage, String originalData, String context) {
        ParsingErrorInfo errorInfo = new ParsingErrorInfo(
            ParsingErrorType.JSON_PARSE_ERROR, 
            errorMessage, 
            originalData, 
            context
        );
        
        // 추가 정보 설정
        errorInfo.addInfo("dataLength", originalData != null ? originalData.length() : 0);
        errorInfo.addInfo("dataType", originalData != null ? originalData.getClass().getSimpleName() : "null");
        
        handleParsingError(errorInfo);
    }
    
    /**
     * 토픽 파싱 오류 처리
     * @param errorMessage 오류 메시지
     * @param originalTopic 원본 토픽
     * @param context 컨텍스트
     */
    public static void handleTopicParseError(String errorMessage, String originalTopic, String context) {
        ParsingErrorInfo errorInfo = new ParsingErrorInfo(
            ParsingErrorType.TOPIC_PARSE_ERROR, 
            errorMessage, 
            originalTopic, 
            context
        );
        
        // 추가 정보 설정
        errorInfo.addInfo("topicLength", originalTopic != null ? originalTopic.length() : 0);
        errorInfo.addInfo("segmentCount", originalTopic != null ? originalTopic.split("/").length : 0);
        
        handleParsingError(errorInfo);
    }
    
    /**
     * 배열 파싱 오류 처리
     * @param errorMessage 오류 메시지
     * @param originalData 원본 데이터
     * @param context 컨텍스트
     */
    public static void handleArrayParseError(String errorMessage, String originalData, String context) {
        ParsingErrorInfo errorInfo = new ParsingErrorInfo(
            ParsingErrorType.ARRAY_PARSE_ERROR, 
            errorMessage, 
            originalData, 
            context
        );
        
        // 추가 정보 설정
        errorInfo.addInfo("dataLength", originalData != null ? originalData.length() : 0);
        errorInfo.addInfo("isArrayFormat", originalData != null && originalData.trim().startsWith("["));
        
        handleParsingError(errorInfo);
    }
    
    /**
     * 유효성 검사 오류 처리
     * @param errorMessage 오류 메시지
     * @param originalData 원본 데이터
     * @param context 컨텍스트
     */
    public static void handleValidationError(String errorMessage, String originalData, String context) {
        ParsingErrorInfo errorInfo = new ParsingErrorInfo(
            ParsingErrorType.VALIDATION_ERROR, 
            errorMessage, 
            originalData, 
            context
        );
        
        // 추가 정보 설정
        errorInfo.addInfo("dataLength", originalData != null ? originalData.length() : 0);
        errorInfo.addInfo("validationFailed", true);
        
        handleParsingError(errorInfo);
    }
    
    /**
     * 오류 통계 업데이트
     * @param errorInfo 오류 정보
     */
    private static void updateErrorStatistics(ParsingErrorInfo errorInfo) {
        // 전체 오류 카운트 증가
        errorCounts.computeIfAbsent("total", k -> new AtomicLong(0)).incrementAndGet();
        
        // 오류 타입별 카운트 증가
        String errorTypeKey = errorInfo.getErrorType().name();
        errorTypes.computeIfAbsent(errorTypeKey, k -> new AtomicLong(0)).incrementAndGet();
        
        // 컨텍스트별 카운트 증가
        String contextKey = "context_" + errorInfo.getContext();
        errorCounts.computeIfAbsent(contextKey, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 최근 오류 목록에 추가
     * @param errorInfo 오류 정보
     */
    private static void addToRecentErrors(ParsingErrorInfo errorInfo) {
        String contextKey = errorInfo.getContext();
        recentErrors.computeIfAbsent(contextKey, k -> new ArrayList<>()).add(
            String.format("[%s] %s: %s", 
                errorInfo.getErrorType().getDescription(),
                errorInfo.getErrorMessage(),
                errorInfo.getOriginalData() != null ? 
                    errorInfo.getOriginalData().substring(0, Math.min(100, errorInfo.getOriginalData().length())) + "..." :
                    "null"
            )
        );
        
        // 최대 개수 초과 시 오래된 것 제거
        List<String> contextErrors = recentErrors.get(contextKey);
        if (contextErrors.size() > MAX_RECENT_ERRORS) {
            contextErrors.remove(0);
        }
    }
    
    /**
     * 파싱 오류 상세 로깅
     * @param errorInfo 오류 정보
     */
    private static void logParsingError(ParsingErrorInfo errorInfo) {
        String logMessage = String.format(
            "파싱 오류 발생 [%s] - 컨텍스트: %s, 메시지: %s, 데이터: %s",
            errorInfo.getErrorType().getDescription(),
            errorInfo.getContext(),
            errorInfo.getErrorMessage(),
            errorInfo.getOriginalData() != null ? 
                errorInfo.getOriginalData().substring(0, Math.min(200, errorInfo.getOriginalData().length())) + "..." :
                "null"
        );
        
        // 오류 타입별 로그 레벨 조정
        switch (errorInfo.getErrorType()) {
            case JSON_PARSE_ERROR:
            case TOPIC_PARSE_ERROR:
                logger.warn(logMessage);
                break;
            case ARRAY_PARSE_ERROR:
            case VALIDATION_ERROR:
                logger.info(logMessage);
                break;
            case FORMAT_ERROR:
            case ENCODING_ERROR:
                logger.error(logMessage);
                break;
            default:
                logger.warn(logMessage);
                break;
        }
        
        // 추가 정보가 있으면 디버그 로그로 출력
        if (!errorInfo.getAdditionalInfo().isEmpty()) {
            logger.debug("파싱 오류 추가 정보: {}", errorInfo.getAdditionalInfo());
        }
    }
    
    /**
     * 파싱 오류 통계 조회
     * @return 오류 통계
     */
    public static Map<String, Object> getParsingErrorStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 전체 통계
        stats.put("totalErrors", errorCounts.getOrDefault("total", new AtomicLong(0)).get());
        
        // 오류 타입별 통계
        Map<String, Long> errorTypeStats = new HashMap<>();
        errorTypes.forEach((key, value) -> errorTypeStats.put(key, value.get()));
        stats.put("errorTypes", errorTypeStats);
        
        // 컨텍스트별 통계
        Map<String, Long> contextStats = new HashMap<>();
        errorCounts.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("context_"))
            .forEach(entry -> contextStats.put(entry.getKey(), entry.getValue().get()));
        stats.put("contexts", contextStats);
        
        // 최근 오류 목록
        Map<String, List<String>> recentErrorsCopy = new HashMap<>();
        recentErrors.forEach((key, value) -> recentErrorsCopy.put(key, new ArrayList<>(value)));
        stats.put("recentErrors", recentErrorsCopy);
        
        return stats;
    }
    
    /**
     * 파싱 오류 통계 리셋
     */
    public static void resetParsingErrorStatistics() {
        errorCounts.clear();
        errorTypes.clear();
        recentErrors.clear();
        logger.info("파싱 오류 통계가 리셋되었습니다.");
    }
    
    /**
     * 특정 컨텍스트의 최근 오류 조회
     * @param context 컨텍스트
     * @return 최근 오류 목록
     */
    public static List<String> getRecentErrorsByContext(String context) {
        return recentErrors.getOrDefault(context, new ArrayList<>());
    }
    
    /**
     * 오류 발생 빈도가 높은 컨텍스트 조회
     * @param limit 상위 N개
     * @return 오류 빈도가 높은 컨텍스트 목록
     */
    public static List<Map.Entry<String, Long>> getTopErrorContexts(int limit) {
        return errorCounts.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("context_"))
            .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
            .limit(limit)
            .map(entry -> new HashMap.SimpleEntry<>(entry.getKey(), entry.getValue().get()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 파싱 오류 패턴 분석
     * @return 오류 패턴 분석 결과
     */
    public static Map<String, Object> analyzeParsingErrorPatterns() {
        Map<String, Object> analysis = new HashMap<>();
        
        // 가장 빈번한 오류 타입
        String mostFrequentErrorType = errorTypes.entrySet().stream()
            .max(Comparator.comparing(e -> e.getValue().get()))
            .map(Map.Entry::getKey)
            .orElse("NONE");
        analysis.put("mostFrequentErrorType", mostFrequentErrorType);
        
        // 가장 문제가 많은 컨텍스트
        List<Map.Entry<String, Long>> topContexts = getTopErrorContexts(5);
        analysis.put("topErrorContexts", topContexts);
        
        // 전체 오류 발생률 (시간당)
        long totalErrors = errorCounts.getOrDefault("total", new AtomicLong(0)).get();
        analysis.put("totalErrorCount", totalErrors);
        
        return analysis;
    }
}
