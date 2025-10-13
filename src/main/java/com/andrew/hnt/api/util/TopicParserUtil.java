package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * 토픽 구조별 파싱 유틸리티 클래스
 * HnT Sensor API 프로젝트 전용
 * 
 * MQTT 토픽 구조를 안정적으로 파싱하고
 * 다양한 토픽 형식을 지원
 */
@Component
public class TopicParserUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(TopicParserUtil.class);
    
    // 토픽 구조 패턴 정의
    private static final Pattern TOPIC_PATTERN_5_SEGMENTS = Pattern.compile(
        "^HBEE/([^/]+)/([^/]+)/([^/]+)/([^/]+)$"
    );
    
    private static final Pattern TOPIC_PATTERN_4_SEGMENTS = Pattern.compile(
        "^HBEE/([^/]+)/([^/]+)/([^/]+)$"
    );
    
    private static final Pattern TOPIC_PATTERN_3_SEGMENTS = Pattern.compile(
        "^HBEE/([^/]+)/([^/]+)$"
    );
    
    // 유효한 토픽 접미사
    private static final String[] VALID_TOPIC_SUFFIXES = {"DEV", "SER", "CMD", "RESP"};
    
    // 유효한 센서 타입
    private static final String[] VALID_SENSOR_TYPES = {"TC", "SENSOR", "DEVICE", "IOT"};
    
    // 토픽 파싱 결과 클래스
    public static class TopicParseResult {
        private final boolean valid;
        private final String reason;
        private final String topicType;
        private final int segmentCount;
        private final Map<String, String> segments;
        private final Map<String, Object> additionalInfo;
        
        public TopicParseResult(boolean valid, String reason, String topicType, int segmentCount) {
            this.valid = valid;
            this.reason = reason;
            this.topicType = topicType;
            this.segmentCount = segmentCount;
            this.segments = new HashMap<>();
            this.additionalInfo = new HashMap<>();
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getReason() {
            return reason;
        }
        
        public String getTopicType() {
            return topicType;
        }
        
        public int getSegmentCount() {
            return segmentCount;
        }
        
        public Map<String, String> getSegments() {
            return segments;
        }
        
        public Map<String, Object> getAdditionalInfo() {
            return additionalInfo;
        }
        
        public void addSegment(String key, String value) {
            this.segments.put(key, value);
        }
        
        public void addInfo(String key, Object value) {
            this.additionalInfo.put(key, value);
        }
        
        public String getSegment(String key) {
            return this.segments.get(key);
        }
    }
    
    /**
     * 토픽 파싱
     * @param topic 토픽 문자열
     * @return 토픽 파싱 결과
     */
    public static TopicParseResult parseTopic(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            return new TopicParseResult(false, "토픽이 null이거나 비어있음", "UNKNOWN", 0);
        }
        
        String trimmedTopic = topic.trim();
        
        // 5개 세그먼트 토픽 파싱 (HBEE/userId/sensorType/sensorUuid/suffix)
        if (TOPIC_PATTERN_5_SEGMENTS.matcher(trimmedTopic).matches()) {
            return parse5SegmentTopic(trimmedTopic);
        }
        
        // 4개 세그먼트 토픽 파싱 (HBEE/userId/sensorType/suffix) - 사용하지 않음
        if (TOPIC_PATTERN_4_SEGMENTS.matcher(trimmedTopic).matches()) {
            return new TopicParseResult(false, "4개 세그먼트 토픽은 지원하지 않음: " + trimmedTopic, "UNSUPPORTED", 4);
        }
        
        // 3개 세그먼트 토픽 파싱 (HBEE/userId/suffix) - 사용하지 않음
        if (TOPIC_PATTERN_3_SEGMENTS.matcher(trimmedTopic).matches()) {
            return new TopicParseResult(false, "3개 세그먼트 토픽은 지원하지 않음: " + trimmedTopic, "UNSUPPORTED", 3);
        }
        
        return new TopicParseResult(false, "지원되지 않는 토픽 형식: " + trimmedTopic, "UNKNOWN", 0);
    }
    
    /**
     * 5개 세그먼트 토픽 파싱
     * @param topic 토픽 문자열
     * @return 파싱 결과
     */
    private static TopicParseResult parse5SegmentTopic(String topic) {
        String[] parts = topic.split("/");
        
        if (parts.length != 5) {
            return new TopicParseResult(false, "5개 세그먼트 토픽이 아님: " + topic, "UNKNOWN", parts.length);
        }
        
        String prefix = parts[0];
        String userId = parts[1];
        String sensorType = parts[2];
        String sensorUuid = parts[3];
        String suffix = parts[4];
        
        // 접두사 검증
        if (!"HBEE".equals(prefix)) {
            return new TopicParseResult(false, "잘못된 접두사: " + prefix, "UNKNOWN", parts.length);
        }
        
        // 사용자 ID 검증
        if (userId == null || userId.trim().isEmpty()) {
            return new TopicParseResult(false, "사용자 ID가 비어있음", "UNKNOWN", parts.length);
        }
        
        // 센서 타입 검증
        if (!isValidSensorType(sensorType)) {
            return new TopicParseResult(false, "유효하지 않은 센서 타입: " + sensorType, "UNKNOWN", parts.length);
        }
        
        // 센서 UUID 검증
        if (sensorUuid == null || sensorUuid.trim().isEmpty()) {
            return new TopicParseResult(false, "센서 UUID가 비어있음", "UNKNOWN", parts.length);
        }
        
        // 접미사 검증
        if (!isValidTopicSuffix(suffix)) {
            return new TopicParseResult(false, "유효하지 않은 토픽 접미사: " + suffix, "UNKNOWN", parts.length);
        }
        
        TopicParseResult result = new TopicParseResult(true, "5개 세그먼트 토픽 파싱 성공", "FULL_TOPIC", parts.length);
        result.addSegment("prefix", prefix);
        result.addSegment("userId", userId);
        result.addSegment("sensorType", sensorType);
        result.addSegment("sensorUuid", sensorUuid);
        result.addSegment("suffix", suffix);
        result.addInfo("isResponseTopic", "DEV".equals(suffix));
        result.addInfo("isRequestTopic", "SER".equals(suffix));
        result.addInfo("isCommandTopic", "CMD".equals(suffix));
        result.addInfo("isResponseTopic", "RESP".equals(suffix));
        
        return result;
    }
    
    /**
     * 4개 세그먼트 토픽 파싱 (센서 타입 필수)
     * @param topic 토픽 문자열
     * @return 파싱 결과
     */
    private static TopicParseResult parse4SegmentTopic(String topic) {
        String[] parts = topic.split("/");
        
        if (parts.length != 4) {
            return new TopicParseResult(false, "4개 세그먼트 토픽이 아님: " + topic, "UNKNOWN", parts.length);
        }
        
        String prefix = parts[0];
        String userId = parts[1];
        String sensorType = parts[2];
        String suffix = parts[3];
        
        // 접두사 검증
        if (!"HBEE".equals(prefix)) {
            return new TopicParseResult(false, "잘못된 접두사: " + prefix, "UNKNOWN", parts.length);
        }
        
        // 사용자 ID 검증
        if (userId == null || userId.trim().isEmpty()) {
            return new TopicParseResult(false, "사용자 ID가 비어있음", "UNKNOWN", parts.length);
        }
        
        // 센서 타입 검증 (4개 세그먼트에서도 센서 타입 필수)
        if (!isValidSensorType(sensorType)) {
            return new TopicParseResult(false, "유효하지 않은 센서 타입: " + sensorType, "UNKNOWN", parts.length);
        }
        
        // 접미사 검증
        if (!isValidTopicSuffix(suffix)) {
            return new TopicParseResult(false, "유효하지 않은 토픽 접미사: " + suffix, "UNKNOWN", parts.length);
        }
        
        TopicParseResult result = new TopicParseResult(true, "4개 세그먼트 토픽 파싱 성공 (센서 타입: " + sensorType + ")", "SIMPLE_TOPIC", parts.length);
        result.addSegment("prefix", prefix);
        result.addSegment("userId", userId);
        result.addSegment("sensorType", sensorType);
        result.addSegment("suffix", suffix);
        result.addInfo("isResponseTopic", "DEV".equals(suffix));
        result.addInfo("isRequestTopic", "SER".equals(suffix));
        result.addInfo("isCommandTopic", "CMD".equals(suffix));
        result.addInfo("isResponseTopic", "RESP".equals(suffix));
        
        return result;
    }
    
    /**
     * 3개 세그먼트 토픽 파싱
     * @param topic 토픽 문자열
     * @return 파싱 결과
     */
    private static TopicParseResult parse3SegmentTopic(String topic) {
        String[] parts = topic.split("/");
        
        if (parts.length != 3) {
            return new TopicParseResult(false, "3개 세그먼트 토픽이 아님: " + topic, "UNKNOWN", parts.length);
        }
        
        String prefix = parts[0];
        String userId = parts[1];
        String suffix = parts[2];
        
        // 접두사 검증
        if (!"HBEE".equals(prefix)) {
            return new TopicParseResult(false, "잘못된 접두사: " + prefix, "UNKNOWN", parts.length);
        }
        
        // 사용자 ID 검증
        if (userId == null || userId.trim().isEmpty()) {
            return new TopicParseResult(false, "사용자 ID가 비어있음", "UNKNOWN", parts.length);
        }
        
        // 접미사 검증
        if (!isValidTopicSuffix(suffix)) {
            return new TopicParseResult(false, "유효하지 않은 토픽 접미사: " + suffix, "UNKNOWN", parts.length);
        }
        
        TopicParseResult result = new TopicParseResult(true, "3개 세그먼트 토픽 파싱 성공", "USER_TOPIC", parts.length);
        result.addSegment("prefix", prefix);
        result.addSegment("userId", userId);
        result.addSegment("suffix", suffix);
        result.addInfo("isResponseTopic", "DEV".equals(suffix));
        result.addInfo("isRequestTopic", "SER".equals(suffix));
        result.addInfo("isCommandTopic", "CMD".equals(suffix));
        result.addInfo("isResponseTopic", "RESP".equals(suffix));
        
        return result;
    }
    
    /**
     * 토픽 접미사 유효성 검사
     * @param suffix 접미사
     * @return 유효성 여부
     */
    private static boolean isValidTopicSuffix(String suffix) {
        if (suffix == null || suffix.trim().isEmpty()) {
            return false;
        }
        
        for (String validSuffix : VALID_TOPIC_SUFFIXES) {
            if (validSuffix.equals(suffix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 센서 타입 유효성 검사
     * @param sensorType 센서 타입
     * @return 유효성 여부
     */
    private static boolean isValidSensorType(String sensorType) {
        if (sensorType == null || sensorType.trim().isEmpty()) {
            return false;
        }
        
        for (String validType : VALID_SENSOR_TYPES) {
            if (validType.equals(sensorType)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 토픽이 응답 토픽인지 확인
     * @param topic 토픽 문자열
     * @return 응답 토픽 여부
     */
    public static boolean isResponseTopic(String topic) {
        TopicParseResult result = parseTopic(topic);
        if (!result.isValid()) {
            return false;
        }
        
        String suffix = result.getSegment("suffix");
        return "DEV".equals(suffix) || "RESP".equals(suffix);
    }
    
    /**
     * 토픽이 요청 토픽인지 확인
     * @param topic 토픽 문자열
     * @return 요청 토픽 여부
     */
    public static boolean isRequestTopic(String topic) {
        TopicParseResult result = parseTopic(topic);
        if (!result.isValid()) {
            return false;
        }
        
        String suffix = result.getSegment("suffix");
        return "SER".equals(suffix) || "CMD".equals(suffix);
    }
    
    /**
     * 토픽에서 사용자 ID 추출
     * @param topic 토픽 문자열
     * @return 사용자 ID
     */
    public static String extractUserId(String topic) {
        TopicParseResult result = parseTopic(topic);
        if (!result.isValid()) {
            return null;
        }
        
        return result.getSegment("userId");
    }
    
    /**
     * 토픽에서 센서 UUID 추출
     * @param topic 토픽 문자열
     * @return 센서 UUID
     */
    public static String extractSensorUuid(String topic) {
        TopicParseResult result = parseTopic(topic);
        if (!result.isValid()) {
            return null;
        }
        
        return result.getSegment("sensorUuid");
    }
    
    /**
     * 토픽에서 센서 타입 추출
     * @param topic 토픽 문자열
     * @return 센서 타입
     */
    public static String extractSensorType(String topic) {
        TopicParseResult result = parseTopic(topic);
        if (!result.isValid()) {
            return null;
        }
        
        return result.getSegment("sensorType");
    }
    
    /**
     * 토픽에서 접미사 추출
     * @param topic 토픽 문자열
     * @return 접미사
     */
    public static String extractSuffix(String topic) {
        TopicParseResult result = parseTopic(topic);
        if (!result.isValid()) {
            return null;
        }
        
        return result.getSegment("suffix");
    }
    
    /**
     * 토픽 구조 검증
     * @param topic 토픽 문자열
     * @return 검증 결과
     */
    public static boolean isValidTopicStructure(String topic) {
        TopicParseResult result = parseTopic(topic);
        return result.isValid();
    }
    
    /**
     * 토픽 통계 조회
     * @return 토픽 통계
     */
    public static Map<String, Object> getTopicStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("validSuffixes", VALID_TOPIC_SUFFIXES);
        stats.put("validSensorTypes", VALID_SENSOR_TYPES);
        stats.put("supportedTopicTypes", new String[]{"FULL_TOPIC", "SIMPLE_TOPIC", "USER_TOPIC"});
        stats.put("maxSegmentCount", 5);
        stats.put("minSegmentCount", 3);
        return stats;
    }
}
