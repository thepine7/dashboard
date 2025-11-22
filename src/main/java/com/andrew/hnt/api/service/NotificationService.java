package com.andrew.hnt.api.service;

import com.andrew.hnt.api.model.NotificationRequest;
import com.andrew.hnt.api.mqtt.MqttConnectionManager;
import com.andrew.hnt.api.mqtt.common.MQTT;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 이중화 알림 서비스 (FCM 우선, MQTT 백업)
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired(required = false)
    private MqttConnectionManager mqttConnectionManager;
    
    @Autowired(required = false)
    private FCMService fcmService;
    
    /**
     * 이중화 알림 발송 (FCM 우선, MQTT 백업)
     */
    public boolean sendDualNotification(NotificationRequest request) {
        String userId = request.getUserId();
        String fcmToken = request.getFcmToken();
        String sensorUuid = request.getSensorUuid();
        String message = request.getMessage();
        
        logger.info("이중화 알림 발송 시작 - userId: {}, sensorUuid: {}", userId, sensorUuid);
        
        // 1. FCM 토큰이 있으면 FCM 발송 시도
        if (fcmToken != null && !fcmToken.isEmpty() && !"null".equals(fcmToken)) {
            boolean fcmSuccess = sendFcmNotification(fcmToken, sensorUuid, message);
            
            if (fcmSuccess) {
                logger.info("FCM 알림 발송 성공 - userId: {}", userId);
                return true;
            } else {
                logger.warn("FCM 알림 발송 실패 - MQTT 백업 사용 - userId: {}", userId);
            }
        } else {
            logger.info("FCM 토큰 없음 - MQTT 알림 사용 - userId: {}", userId);
        }
        
        // 2. FCM 실패 또는 토큰 없음 → MQTT 알림 발송
        return sendMqttNotification(userId, sensorUuid, message);
    }
    
    /**
     * FCM v1 API 알림 발송
     */
    private boolean sendFcmNotification(String fcmToken, String sensorUuid, String message) {
        try {
            // FCMService가 없으면 건너뛰기
            if (fcmService == null) {
                logger.warn("FCMService가 초기화되지 않음 - MQTT로 전환");
                return false;
            }
            
            logger.info("===============================================");
            logger.info("📱 FCM v1 API 알림 발송 시작");
            logger.info("   - FCM 토큰: {}...", fcmToken.substring(0, Math.min(fcmToken.length(), 20)));
            logger.info("   - 센서 UUID: {}", sensorUuid);
            logger.info("   - 메시지: {}", message);
            
            // FCM v1 API 호출
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("sensorUuid", sensorUuid);
            data.put("message", message);
            data.put("type", "alarm");
            
            boolean success = fcmService.sendNotification(
                fcmToken, 
                "HnT 센서 알람",  // title
                message,          // body
                data              // data
            );
            
            logger.info("   - FCM v1 API 발송 결과: {}", success ? "성공" : "실패");
            logger.info("===============================================");
            
            return success;
            
        } catch (Exception e) {
            logger.error("FCM v1 API 발송 오류", e);
            return false;
        }
    }
    
    /**
     * MQTT 알림 발송 (백업)
     */
    private boolean sendMqttNotification(String userId, String sensorUuid, String message) {
        try {
            boolean isThepine = "thepine".equals(userId);
            
            if (isThepine) {
                logger.info("===============================================");
                logger.info("📡 [thepine] MQTT 알림 발송 시작");
                logger.info("   - 사용자 ID: {}", userId);
                logger.info("   - 센서 UUID: {}", sensorUuid);
                logger.info("   - 메시지: {}", message);
            } else {
                logger.info("📡 MQTT 알림 발송 시작 - userId: {}", userId);
            }
            
            // MqttConnectionManager에서 MQTT 클라이언트 가져오기
            MQTT mqtt = null;
            if (mqttConnectionManager != null) {
                mqtt = mqttConnectionManager.getMqttClient();
            }
            
            if (mqtt == null) {
                logger.warn("MQTT 클라이언트가 없음 - MQTT 알림 발송 실패");
                return false;
            }
            
            String alarmTopic = "HBEE/" + userId + "/ALARM";
            
            JSONObject alarmData = new JSONObject();
            alarmData.put("type", "alarm");
            alarmData.put("sensorUuid", sensorUuid);
            alarmData.put("message", message);
            alarmData.put("timestamp", System.currentTimeMillis());
            
            // MQTT 발행 (QoS 1)
            // 파라미터 순서: (메시지, QoS, 토픽)
            mqtt.publish(alarmData.toString(), 1, alarmTopic);
            
            if (isThepine) {
                logger.info("✅ [thepine] MQTT 알림 발송 성공");
                logger.info("   - Topic: {}", alarmTopic);
                logger.info("   - Payload: {}", alarmData.toString());
                logger.info("===============================================");
            } else {
                logger.info("✅ MQTT 알림 발송 성공 - Topic: {}", alarmTopic);
            }
            return true;
            
        } catch (Exception e) {
            logger.error("❌ MQTT 알림 발송 오류", e);
            logger.info("===============================================");
            return false;
        }
    }
    
    /**
     * 시스템 상태 알림 전송
     */
    public void sendSystemStatusNotification(String title, String message) {
        logger.info("시스템 상태 알림: {} - {}", title, message);
        // 시스템 알림은 로그만 기록 (필요시 관리자에게 FCM 발송 추가 가능)
    }
    
    /**
     * 백업 알림 전송
     */
    public void sendBackupNotification(String title, String message) {
        logger.info("백업 알림: {} - {}", title, message);
        // 백업 알림은 로그만 기록 (필요시 관리자에게 FCM 발송 추가 가능)
    }
}
