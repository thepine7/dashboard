package com.andrew.hnt.api.service;

import com.andrew.hnt.api.model.SensorVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 실시간 동기화 서비스
 * 프론트엔드-백엔드 간 실시간 데이터 동기화를 위한 서비스
 * 
 * 주요 기능:
 * - Server-Sent Events (SSE) 기반 실시간 통신
 * - MQTT 메시지 실시간 전달
 * - 센서 데이터 실시간 업데이트
 * - 사용자별 세션 관리
 */
public interface RealtimeSyncService {
    
    /**
     * SSE 연결 생성
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     * @return SSE Emitter
     */
    SseEmitter createConnection(String userId, String sessionId);
    
    /**
     * SSE 연결 해제
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     */
    void closeConnection(String userId, String sessionId);
    
    /**
     * 센서 데이터 실시간 전송
     * @param userId 사용자 ID
     * @param sensorData 센서 데이터
     */
    void sendSensorData(String userId, SensorVO sensorData);
    
    /**
     * 센서 데이터 배치 전송
     * @param userId 사용자 ID
     * @param sensorDataList 센서 데이터 리스트
     */
    void sendSensorDataBatch(String userId, List<SensorVO> sensorDataList);
    
    /**
     * MQTT 메시지 실시간 전송
     * @param userId 사용자 ID
     * @param topic MQTT 토픽
     * @param message 메시지 내용
     */
    void sendMqttMessage(String userId, String topic, String message);
    
    /**
     * 센서 설정 변경 알림
     * @param userId 사용자 ID
     * @param sensorUuid 센서 UUID
     * @param settings 설정 정보
     */
    void sendSensorSettingsUpdate(String userId, String sensorUuid, Map<String, Object> settings);
    
    /**
     * 알림 메시지 전송
     * @param userId 사용자 ID
     * @param alarmType 알림 타입
     * @param message 알림 메시지
     * @param sensorUuid 센서 UUID (선택적)
     */
    void sendAlarmNotification(String userId, String alarmType, String message, String sensorUuid);
    
    /**
     * 시스템 상태 업데이트
     * @param userId 사용자 ID
     * @param status 시스템 상태
     */
    void sendSystemStatusUpdate(String userId, Map<String, Object> status);
    
    /**
     * 연결된 사용자 수 조회
     * @return 연결된 사용자 수
     */
    int getConnectedUserCount();
    
    /**
     * 사용자별 연결 상태 조회
     * @param userId 사용자 ID
     * @return 연결 상태
     */
    boolean isUserConnected(String userId);
    
    /**
     * 모든 연결된 사용자에게 브로드캐스트
     * @param eventType 이벤트 타입
     * @param data 전송할 데이터
     */
    void broadcastToAllUsers(String eventType, Object data);
    
    /**
     * 특정 사용자 그룹에게 메시지 전송
     * @param userIds 사용자 ID 리스트
     * @param eventType 이벤트 타입
     * @param data 전송할 데이터
     */
    void sendToUserGroup(List<String> userIds, String eventType, Object data);
    
    /**
     * 동기화 통계 조회
     * @return 동기화 통계 정보
     */
    Map<String, Object> getSyncStats();
    
    /**
     * 연결 상태 모니터링
     * @return 연결 상태 정보
     */
    Map<String, Object> getConnectionStatus();
}
