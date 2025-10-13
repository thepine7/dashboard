package com.andrew.hnt.api.exception;

/**
 * MQTT 예외 클래스
 * MQTT 통신 관련 오류를 처리
 */
public class MqttException extends BaseException {
    
    private static final long serialVersionUID = 1L;
    
    public MqttException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
    
    public MqttException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
    
    // 자주 사용되는 MQTT 예외 생성 메서드들
    public static MqttException connectionFailed() {
        return new MqttException("MQTT_CONNECTION_FAILED", "MQTT 연결에 실패했습니다.");
    }
    
    public static MqttException connectionFailed(Throwable cause) {
        return new MqttException("MQTT_CONNECTION_FAILED", "MQTT 연결에 실패했습니다.", cause);
    }
    
    public static MqttException publishFailed(String topic) {
        return new MqttException("MQTT_PUBLISH_FAILED", "MQTT 메시지 발행에 실패했습니다: " + topic);
    }
    
    public static MqttException publishFailed(String topic, Throwable cause) {
        return new MqttException("MQTT_PUBLISH_FAILED", "MQTT 메시지 발행에 실패했습니다: " + topic, cause);
    }
    
    public static MqttException subscribeFailed(String topic) {
        return new MqttException("MQTT_SUBSCRIBE_FAILED", "MQTT 토픽 구독에 실패했습니다: " + topic);
    }
    
    public static MqttException subscribeFailed(String topic, Throwable cause) {
        return new MqttException("MQTT_SUBSCRIBE_FAILED", "MQTT 토픽 구독에 실패했습니다: " + topic, cause);
    }
    
    public static MqttException unsubscribeFailed(String topic) {
        return new MqttException("MQTT_UNSUBSCRIBE_FAILED", "MQTT 토픽 구독 해제에 실패했습니다: " + topic);
    }
    
    public static MqttException unsubscribeFailed(String topic, Throwable cause) {
        return new MqttException("MQTT_UNSUBSCRIBE_FAILED", "MQTT 토픽 구독 해제에 실패했습니다: " + topic, cause);
    }
    
    public static MqttException messageParseFailed(String message) {
        return new MqttException("MQTT_MESSAGE_PARSE_FAILED", "MQTT 메시지 파싱에 실패했습니다: " + message);
    }
    
    public static MqttException messageParseFailed(String message, Throwable cause) {
        return new MqttException("MQTT_MESSAGE_PARSE_FAILED", "MQTT 메시지 파싱에 실패했습니다: " + message, cause);
    }
    
    public static MqttException topicInvalid(String topic) {
        return new MqttException("MQTT_TOPIC_INVALID", "유효하지 않은 MQTT 토픽입니다: " + topic);
    }
    
    public static MqttException brokerUnavailable() {
        return new MqttException("MQTT_BROKER_UNAVAILABLE", "MQTT 브로커를 사용할 수 없습니다.");
    }
    
    public static MqttException brokerUnavailable(Throwable cause) {
        return new MqttException("MQTT_BROKER_UNAVAILABLE", "MQTT 브로커를 사용할 수 없습니다.", cause);
    }
    
    public static MqttException authenticationFailed() {
        return new MqttException("MQTT_AUTH_FAILED", "MQTT 인증에 실패했습니다.");
    }
    
    public static MqttException authenticationFailed(Throwable cause) {
        return new MqttException("MQTT_AUTH_FAILED", "MQTT 인증에 실패했습니다.", cause);
    }
}