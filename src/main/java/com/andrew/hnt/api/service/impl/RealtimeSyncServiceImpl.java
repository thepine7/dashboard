package com.andrew.hnt.api.service.impl;

import com.andrew.hnt.api.model.SensorVO;
import com.andrew.hnt.api.service.RealtimeSyncService;
import com.andrew.hnt.api.util.UnifiedErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 실시간 동기화 서비스 구현
 * Server-Sent Events (SSE) 기반 실시간 통신
 */
@Service
public class RealtimeSyncServiceImpl implements RealtimeSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(RealtimeSyncServiceImpl.class);
    
    @Autowired
    private UnifiedErrorHandler errorHandler;
    
    // 사용자별 SSE 연결 관리
    private final Map<String, Map<String, SseEmitter>> userConnections = new ConcurrentHashMap<>();
    
    // 연결 통계
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong failedMessages = new AtomicLong(0);
    private final AtomicLong activeConnections = new AtomicLong(0);
    
    // 메시지 타입 상수
    private static final String EVENT_SENSOR_DATA = "sensor_data";
    private static final String EVENT_MQTT_MESSAGE = "mqtt_message";
    private static final String EVENT_SENSOR_SETTINGS = "sensor_settings";
    private static final String EVENT_ALARM = "alarm";
    private static final String EVENT_SYSTEM_STATUS = "system_status";
    private static final String EVENT_HEARTBEAT = "heartbeat";
    
    // SSE 타임아웃 설정 (30분)
    private static final long SSE_TIMEOUT = 30 * 60 * 1000;
    
    @Override
    public SseEmitter createConnection(String userId, String sessionId) {
        try {
            logger.info("SSE 연결 생성 시작 - userId: {}, sessionId: {}", userId, sessionId);
            
            // SSE Emitter 생성
            SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
            
            // 연결 완료 콜백
            emitter.onCompletion(() -> {
                logger.info("SSE 연결 완료 - userId: {}, sessionId: {}", userId, sessionId);
                removeConnection(userId, sessionId);
            });
            
            // 타임아웃 콜백
            emitter.onTimeout(() -> {
                logger.warn("SSE 연결 타임아웃 - userId: {}, sessionId: {}", userId, sessionId);
                removeConnection(userId, sessionId);
            });
            
            // 에러 콜백
            emitter.onError((ex) -> {
                logger.error("SSE 연결 에러 - userId: {}, sessionId: {}", userId, sessionId, ex);
                removeConnection(userId, sessionId);
            });
            
            // 사용자별 연결 저장
            userConnections.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                          .put(sessionId, emitter);
            
            // 통계 업데이트
            totalConnections.incrementAndGet();
            activeConnections.incrementAndGet();
            
            // 초기 연결 확인 메시지 전송
            sendHeartbeat(emitter);
            
            logger.info("SSE 연결 생성 완료 - userId: {}, sessionId: {}, 활성 연결 수: {}", 
                       userId, sessionId, activeConnections.get());
            
            return emitter;
            
        } catch (Exception e) {
            errorHandler.logError("SSE 연결 생성", e);
            throw new RuntimeException("SSE 연결 생성 중 오류가 발생했습니다.", e);
        }
    }
    
    @Override
    public void closeConnection(String userId, String sessionId) {
        try {
            logger.info("SSE 연결 해제 시작 - userId: {}, sessionId: {}", userId, sessionId);
            
            Map<String, SseEmitter> userSessions = userConnections.get(userId);
            if (userSessions != null) {
                SseEmitter emitter = userSessions.remove(sessionId);
                if (emitter != null) {
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                        logger.warn("SSE 연결 완료 처리 중 오류 - userId: {}, sessionId: {}", userId, sessionId, e);
                    }
                }
                
                // 사용자 세션이 비어있으면 사용자 연결 제거
                if (userSessions.isEmpty()) {
                    userConnections.remove(userId);
                }
            }
            
            // 통계 업데이트
            activeConnections.decrementAndGet();
            
            logger.info("SSE 연결 해제 완료 - userId: {}, sessionId: {}, 활성 연결 수: {}", 
                       userId, sessionId, activeConnections.get());
            
        } catch (Exception e) {
            errorHandler.logError("SSE 연결 해제", e);
        }
    }
    
    @Override
    public void sendSensorData(String userId, SensorVO sensorData) {
        try {
            Map<String, SseEmitter> userSessions = userConnections.get(userId);
            if (userSessions == null || userSessions.isEmpty()) {
                logger.debug("사용자 연결이 없음 - userId: {}", userId);
                return;
            }
            
            // 센서 데이터를 JSON으로 변환
            Map<String, Object> data = new HashMap<>();
            data.put("uuid", sensorData.getUuid());
            data.put("sensorValue", sensorData.getSensorValue());
            data.put("instDtm", sensorData.getInstDtm());
            data.put("timestamp", System.currentTimeMillis());
            
            // 모든 세션에 전송
            sendToUserSessions(userId, EVENT_SENSOR_DATA, data);
            
            totalMessages.incrementAndGet();
            
        } catch (Exception e) {
            failedMessages.incrementAndGet();
            errorHandler.logError("센서 데이터 전송", e);
        }
    }
    
    @Override
    public void sendSensorDataBatch(String userId, List<SensorVO> sensorDataList) {
        try {
            if (sensorDataList == null || sensorDataList.isEmpty()) {
                return;
            }
            
            Map<String, SseEmitter> userSessions = userConnections.get(userId);
            if (userSessions == null || userSessions.isEmpty()) {
                logger.debug("사용자 연결이 없음 - userId: {}", userId);
                return;
            }
            
            // 배치 데이터를 JSON으로 변환
            List<Map<String, Object>> batchData = new ArrayList<>();
            for (SensorVO sensorData : sensorDataList) {
                Map<String, Object> data = new HashMap<>();
                data.put("uuid", sensorData.getUuid());
                data.put("sensorValue", sensorData.getSensorValue());
                data.put("instDtm", sensorData.getInstDtm());
                data.put("timestamp", System.currentTimeMillis());
                batchData.add(data);
            }
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("batchData", batchData);
            payload.put("count", batchData.size());
            payload.put("timestamp", System.currentTimeMillis());
            
            // 모든 세션에 전송
            sendToUserSessions(userId, EVENT_SENSOR_DATA, payload);
            
            totalMessages.incrementAndGet();
            
        } catch (Exception e) {
            failedMessages.incrementAndGet();
            errorHandler.logError("센서 데이터 배치 전송", e);
        }
    }
    
    @Override
    public void sendMqttMessage(String userId, String topic, String message) {
        try {
            Map<String, SseEmitter> userSessions = userConnections.get(userId);
            if (userSessions == null || userSessions.isEmpty()) {
                logger.debug("사용자 연결이 없음 - userId: {}", userId);
                return;
            }
            
            // MQTT 메시지를 JSON으로 변환
            Map<String, Object> data = new HashMap<>();
            data.put("topic", topic);
            data.put("message", message);
            data.put("timestamp", System.currentTimeMillis());
            
            // 모든 세션에 전송
            sendToUserSessions(userId, EVENT_MQTT_MESSAGE, data);
            
            totalMessages.incrementAndGet();
            
        } catch (Exception e) {
            failedMessages.incrementAndGet();
            errorHandler.logError("MQTT 메시지 전송", e);
        }
    }
    
    @Override
    public void sendSensorSettingsUpdate(String userId, String sensorUuid, Map<String, Object> settings) {
        try {
            Map<String, SseEmitter> userSessions = userConnections.get(userId);
            if (userSessions == null || userSessions.isEmpty()) {
                logger.debug("사용자 연결이 없음 - userId: {}", userId);
                return;
            }
            
            // 설정 정보를 JSON으로 변환
            Map<String, Object> data = new HashMap<>();
            data.put("sensorUuid", sensorUuid);
            data.put("settings", settings);
            data.put("timestamp", System.currentTimeMillis());
            
            // 모든 세션에 전송
            sendToUserSessions(userId, EVENT_SENSOR_SETTINGS, data);
            
            totalMessages.incrementAndGet();
            
        } catch (Exception e) {
            failedMessages.incrementAndGet();
            errorHandler.logError("센서 설정 업데이트 전송", e);
        }
    }
    
    @Override
    public void sendAlarmNotification(String userId, String alarmType, String message, String sensorUuid) {
        try {
            Map<String, SseEmitter> userSessions = userConnections.get(userId);
            if (userSessions == null || userSessions.isEmpty()) {
                logger.debug("사용자 연결이 없음 - userId: {}", userId);
                return;
            }
            
            // 알림 정보를 JSON으로 변환
            Map<String, Object> data = new HashMap<>();
            data.put("alarmType", alarmType);
            data.put("message", message);
            data.put("sensorUuid", sensorUuid);
            data.put("timestamp", System.currentTimeMillis());
            data.put("formattedTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 모든 세션에 전송
            sendToUserSessions(userId, EVENT_ALARM, data);
            
            totalMessages.incrementAndGet();
            
        } catch (Exception e) {
            failedMessages.incrementAndGet();
            errorHandler.logError("알림 전송", e);
        }
    }
    
    @Override
    public void sendSystemStatusUpdate(String userId, Map<String, Object> status) {
        try {
            Map<String, SseEmitter> userSessions = userConnections.get(userId);
            if (userSessions == null || userSessions.isEmpty()) {
                logger.debug("사용자 연결이 없음 - userId: {}", userId);
                return;
            }
            
            // 시스템 상태 정보를 JSON으로 변환
            Map<String, Object> data = new HashMap<>();
            data.put("status", status);
            data.put("timestamp", System.currentTimeMillis());
            
            // 모든 세션에 전송
            sendToUserSessions(userId, EVENT_SYSTEM_STATUS, data);
            
            totalMessages.incrementAndGet();
            
        } catch (Exception e) {
            failedMessages.incrementAndGet();
            errorHandler.logError("시스템 상태 업데이트 전송", e);
        }
    }
    
    @Override
    public int getConnectedUserCount() {
        return userConnections.size();
    }
    
    @Override
    public boolean isUserConnected(String userId) {
        Map<String, SseEmitter> userSessions = userConnections.get(userId);
        return userSessions != null && !userSessions.isEmpty();
    }
    
    @Override
    public void broadcastToAllUsers(String eventType, Object data) {
        try {
            logger.info("모든 사용자에게 브로드캐스트 - 이벤트 타입: {}", eventType);
            
            for (Map.Entry<String, Map<String, SseEmitter>> userEntry : userConnections.entrySet()) {
                String userId = userEntry.getKey();
                Map<String, SseEmitter> userSessions = userEntry.getValue();
                
                for (Map.Entry<String, SseEmitter> sessionEntry : userSessions.entrySet()) {
                    String sessionId = sessionEntry.getKey();
                    SseEmitter emitter = sessionEntry.getValue();
                    
                    try {
                        emitter.send(SseEmitter.event()
                            .name(eventType)
                            .data(data));
                    } catch (Exception e) {
                        logger.warn("브로드캐스트 전송 실패 - userId: {}, sessionId: {}", userId, sessionId, e);
                        removeConnection(userId, sessionId);
                    }
                }
            }
            
            totalMessages.incrementAndGet();
            
        } catch (Exception e) {
            failedMessages.incrementAndGet();
            errorHandler.logError("브로드캐스트 전송", e);
        }
    }
    
    @Override
    public void sendToUserGroup(List<String> userIds, String eventType, Object data) {
        try {
            logger.info("사용자 그룹에게 전송 - 사용자 수: {}, 이벤트 타입: {}", userIds.size(), eventType);
            
            for (String userId : userIds) {
                if (isUserConnected(userId)) {
                    Map<String, SseEmitter> userSessions = userConnections.get(userId);
                    if (userSessions != null) {
                        for (SseEmitter emitter : userSessions.values()) {
                            try {
                                emitter.send(SseEmitter.event()
                                    .name(eventType)
                                    .data(data));
                            } catch (Exception e) {
                                logger.warn("사용자 그룹 전송 실패 - userId: {}", userId, e);
                            }
                        }
                    }
                }
            }
            
            totalMessages.incrementAndGet();
            
        } catch (Exception e) {
            failedMessages.incrementAndGet();
            errorHandler.logError("사용자 그룹 전송", e);
        }
    }
    
    @Override
    public Map<String, Object> getSyncStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConnections", totalConnections.get());
        stats.put("activeConnections", activeConnections.get());
        stats.put("totalMessages", totalMessages.get());
        stats.put("failedMessages", failedMessages.get());
        stats.put("connectedUsers", userConnections.size());
        stats.put("successRate", calculateSuccessRate());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }
    
    @Override
    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isActive", activeConnections.get() > 0);
        status.put("activeConnections", activeConnections.get());
        status.put("connectedUsers", userConnections.size());
        status.put("userList", new ArrayList<>(userConnections.keySet()));
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }
    
    /**
     * 사용자의 모든 세션에 메시지 전송
     */
    private void sendToUserSessions(String userId, String eventType, Object data) {
        Map<String, SseEmitter> userSessions = userConnections.get(userId);
        if (userSessions == null) {
            return;
        }
        
        for (Map.Entry<String, SseEmitter> sessionEntry : userSessions.entrySet()) {
            String sessionId = sessionEntry.getKey();
            SseEmitter emitter = sessionEntry.getValue();
            
            try {
                emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data));
            } catch (Exception e) {
                logger.warn("메시지 전송 실패 - userId: {}, sessionId: {}", userId, sessionId, e);
                removeConnection(userId, sessionId);
            }
        }
    }
    
    /**
     * 연결 제거
     */
    private void removeConnection(String userId, String sessionId) {
        Map<String, SseEmitter> userSessions = userConnections.get(userId);
        if (userSessions != null) {
            userSessions.remove(sessionId);
            if (userSessions.isEmpty()) {
                userConnections.remove(userId);
            }
        }
        activeConnections.decrementAndGet();
    }
    
    /**
     * 하트비트 전송
     */
    private void sendHeartbeat(SseEmitter emitter) {
        try {
            Map<String, Object> heartbeat = new HashMap<>();
            heartbeat.put("timestamp", System.currentTimeMillis());
            heartbeat.put("status", "connected");
            
            emitter.send(SseEmitter.event()
                .name(EVENT_HEARTBEAT)
                .data(heartbeat));
        } catch (Exception e) {
            logger.warn("하트비트 전송 실패", e);
        }
    }
    
    /**
     * 성공률 계산
     */
    private double calculateSuccessRate() {
        long total = totalMessages.get();
        if (total == 0) return 100.0;
        long failed = failedMessages.get();
        return (double) (total - failed) / total * 100;
    }
}
