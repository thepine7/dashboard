package com.andrew.hnt.api.mqtt;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MQTT 토픽 파싱 통합 클래스
 * 모든 MQTT 토픽을 일관되게 파싱하여 표준화된 구조로 제공
 */
@Component
public class MqttTopicParser {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttTopicParser.class);
    
    /**
     * 토픽 구조 상수
     */
    public static final class TopicStructure {
        public static final String SEPARATOR = "/";
        public static final int MIN_ELEMENTS = 3;
        public static final int MAX_ELEMENTS = 5;
        
        // 토픽 요소 인덱스
        public static final int INDEX_PROTOCOL = 0;  // HBEE
        public static final int INDEX_USER_ID = 1;   // 사용자 ID
        public static final int INDEX_SENSOR_TYPE = 2; // 센서 타입 (TC, etc.)
        public static final int INDEX_UUID = 3;      // 장치 UUID
        public static final int INDEX_MESSAGE_TYPE = 4; // 메시지 타입 (DEV, SER)
    }
    
    /**
     * 파싱된 토픽 정보를 담는 클래스
     */
    public static class ParsedTopic {
        private String protocol;
        private String userId;
        private String sensorType;
        private String uuid;
        private String messageType;
        private boolean isValid;
        private String originalTopic;
        
        // 생성자
        public ParsedTopic(String originalTopic) {
            this.originalTopic = originalTopic;
            this.isValid = false;
        }
        
        // Getters
        public String getProtocol() { return protocol; }
        public String getUserId() { return userId; }
        public String getSensorType() { return sensorType; }
        public String getUuid() { return uuid; }
        public String getMessageType() { return messageType; }
        public boolean isValid() { return isValid; }
        public String getOriginalTopic() { return originalTopic; }
        
        // Setters
        public void setProtocol(String protocol) { this.protocol = protocol; }
        public void setUserId(String userId) { this.userId = userId; }
        public void setSensorType(String sensorType) { this.sensorType = sensorType; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        public void setValid(boolean valid) { this.isValid = valid; }
        
        @Override
        public String toString() {
            return String.format("ParsedTopic{protocol='%s', userId='%s', sensorType='%s', uuid='%s', messageType='%s', isValid=%s}", 
                protocol, userId, sensorType, uuid, messageType, isValid);
        }
    }
    
    /**
     * MQTT 토픽 파싱 (통합 로직)
     * @param topic MQTT 토픽 문자열
     * @return 파싱된 토픽 정보
     */
    public ParsedTopic parseTopic(String topic) {
        ParsedTopic parsedTopic = new ParsedTopic(topic);
        
        try {
            if (topic == null || topic.trim().isEmpty()) {
                logger.warn("토픽이 null이거나 비어있음");
                return parsedTopic;
            }
            
            // 토픽을 "/"로 분리
            String[] topicElements = topic.split(TopicStructure.SEPARATOR);
            
            if (topicElements.length < TopicStructure.MIN_ELEMENTS) {
                logger.warn("토픽 요소가 부족함 - topic: {}, elements: {}", topic, topicElements.length);
                return parsedTopic;
            }
            
            // 기본 프로토콜 확인
            if (!"HBEE".equals(topicElements[TopicStructure.INDEX_PROTOCOL])) {
                logger.warn("지원하지 않는 프로토콜 - topic: {}, protocol: {}", topic, topicElements[TopicStructure.INDEX_PROTOCOL]);
                return parsedTopic;
            }
            
            // 기본 정보 설정
            parsedTopic.setProtocol(topicElements[TopicStructure.INDEX_PROTOCOL]);
            parsedTopic.setUserId(topicElements[TopicStructure.INDEX_USER_ID]);
            parsedTopic.setSensorType(topicElements[TopicStructure.INDEX_SENSOR_TYPE]);
            
            // 토픽 길이에 따른 처리
            if (topicElements.length == 3) {
                // 3개 요소: HBEE/userId/sensorType
                parsedTopic.setUuid(null);
                parsedTopic.setMessageType(null);
                parsedTopic.setValid(true);
                logger.debug("3개 요소 토픽 파싱 완료: {}", parsedTopic);
                
            } else if (topicElements.length == 4) {
                // 4개 요소: HBEE/userId/sensorType/uuid 또는 HBEE/userId/uuid/messageType
                if (isUuid(topicElements[TopicStructure.INDEX_UUID])) {
                    // HBEE/userId/sensorType/uuid
                    parsedTopic.setUuid(topicElements[TopicStructure.INDEX_UUID]);
                    parsedTopic.setMessageType(null);
                } else {
                    // HBEE/userId/uuid/messageType (레거시 형식)
                    parsedTopic.setUuid(topicElements[TopicStructure.INDEX_SENSOR_TYPE]);
                    parsedTopic.setSensorType("TC"); // 기본값
                    parsedTopic.setMessageType(topicElements[TopicStructure.INDEX_UUID]);
                }
                parsedTopic.setValid(true);
                logger.debug("4개 요소 토픽 파싱 완료: {}", parsedTopic);
                
            } else if (topicElements.length == 5) {
                // 5개 요소: HBEE/userId/sensorType/uuid/messageType
                parsedTopic.setUuid(topicElements[TopicStructure.INDEX_UUID]);
                parsedTopic.setMessageType(topicElements[TopicStructure.INDEX_MESSAGE_TYPE]);
                parsedTopic.setValid(true);
                logger.debug("5개 요소 토픽 파싱 완료: {}", parsedTopic);
                
            } else {
                logger.warn("지원하지 않는 토픽 길이 - topic: {}, elements: {}", topic, topicElements.length);
                return parsedTopic;
            }
            
            // 유효성 검증
            if (!isValidUserId(parsedTopic.getUserId())) {
                logger.warn("유효하지 않은 사용자 ID - topic: {}, userId: {}", topic, parsedTopic.getUserId());
                parsedTopic.setValid(false);
                return parsedTopic;
            }
            
            logger.debug("토픽 파싱 성공: {}", parsedTopic);
            return parsedTopic;
            
        } catch (Exception e) {
            logger.error("토픽 파싱 중 오류 발생 - topic: {}, error: {}", topic, e.getMessage());
            return parsedTopic;
        }
    }
    
    /**
     * UUID 형식 검증
     * @param uuid 검증할 UUID 문자열
     * @return UUID 형식 여부
     */
    private boolean isUuid(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }
        
        // UUID는 12자리 16진수 문자열 (예: 0008DC7553A4)
        return uuid.matches("^[0-9A-Fa-f]{12}$");
    }
    
    /**
     * 사용자 ID 유효성 검증
     * @param userId 검증할 사용자 ID
     * @return 유효성 여부
     */
    private boolean isValidUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        // 사용자 ID는 영문자, 숫자, 언더스코어만 허용
        return userId.matches("^[a-zA-Z0-9_]+$");
    }
    
    /**
     * 토픽에서 사용자 ID 추출 (간편 메서드)
     * @param topic MQTT 토픽
     * @return 사용자 ID (파싱 실패 시 null)
     */
    public String extractUserId(String topic) {
        ParsedTopic parsed = parseTopic(topic);
        return parsed.isValid() ? parsed.getUserId() : null;
    }
    
    /**
     * 토픽에서 UUID 추출 (간편 메서드)
     * @param topic MQTT 토픽
     * @return UUID (파싱 실패 시 null)
     */
    public String extractUuid(String topic) {
        ParsedTopic parsed = parseTopic(topic);
        return parsed.isValid() ? parsed.getUuid() : null;
    }
    
    /**
     * 토픽에서 센서 타입 추출 (간편 메서드)
     * @param topic MQTT 토픽
     * @return 센서 타입 (파싱 실패 시 null)
     */
    public String extractSensorType(String topic) {
        ParsedTopic parsed = parseTopic(topic);
        return parsed.isValid() ? parsed.getSensorType() : null;
    }
    
    /**
     * 토픽이 응답 메시지인지 확인 (DEV로 끝나는지)
     * @param topic MQTT 토픽
     * @return 응답 메시지 여부
     */
    public boolean isResponseTopic(String topic) {
        ParsedTopic parsed = parseTopic(topic);
        return parsed.isValid() && "DEV".equals(parsed.getMessageType());
    }
    
    /**
     * 토픽이 요청 메시지인지 확인 (SER로 끝나는지)
     * @param topic MQTT 토픽
     * @return 요청 메시지 여부
     */
    public boolean isRequestTopic(String topic) {
        ParsedTopic parsed = parseTopic(topic);
        return parsed.isValid() && "SER".equals(parsed.getMessageType());
    }
    
    /**
     * 표준 토픽 생성
     * @param userId 사용자 ID
     * @param sensorType 센서 타입
     * @param uuid 장치 UUID
     * @param messageType 메시지 타입 (DEV, SER)
     * @return 생성된 토픽
     */
    public String createTopic(String userId, String sensorType, String uuid, String messageType) {
        if (userId == null || sensorType == null || uuid == null || messageType == null) {
            throw new IllegalArgumentException("토픽 생성에 필요한 모든 파라미터가 필요합니다.");
        }
        
        return String.format("HBEE/%s/%s/%s/%s", userId, sensorType, uuid, messageType);
    }
    
    /**
     * 토픽 파싱 결과를 Map으로 변환
     * @param topic MQTT 토픽
     * @return 파싱 결과 Map
     */
    public Map<String, Object> parseTopicToMap(String topic) {
        ParsedTopic parsed = parseTopic(topic);
        Map<String, Object> result = new HashMap<>();
        
        result.put("protocol", parsed.getProtocol());
        result.put("userId", parsed.getUserId());
        result.put("sensorType", parsed.getSensorType());
        result.put("uuid", parsed.getUuid());
        result.put("messageType", parsed.getMessageType());
        result.put("isValid", parsed.isValid());
        result.put("originalTopic", parsed.getOriginalTopic());
        
        return result;
    }
}

