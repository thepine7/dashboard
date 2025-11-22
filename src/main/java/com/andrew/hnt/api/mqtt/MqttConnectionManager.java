package com.andrew.hnt.api.mqtt;

import com.andrew.hnt.api.config.MqttConfig;
import com.andrew.hnt.api.mqtt.common.MQTT;
import com.andrew.hnt.api.service.MqttService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;

/**
 * MQTT 연결 관리자
 * 연결 안정성 개선 및 상태 모니터링
 */
@Component
public class MqttConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttConnectionManager.class);
    
    @Autowired
    private MqttConfig mqttConfig;
    
    @Autowired
    @Lazy // 순환 참조 방지
    private MqttService mqttService;
    
    private MQTT mqttClient;
    private ScheduledExecutorService scheduler;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private AtomicBoolean isReconnecting = new AtomicBoolean(false);
    
    // MQTT 연결 설정
    private String brokerUrl;
    private String clientId;
    private String userName;
    private String password;
    
    /**
     * MQTT 연결 관리자 초기화 (ApplicationRunner에서 호출)
     * @PostConstruct 제거 - MqttInitializationRunner에서 초기화 제어
     */
    public void initialize() {
        logger.info("=== MQTT 연결 관리자 초기화 시작 ===");
        logger.info("MQTT 설정 객체 존재 여부: {}", mqttConfig != null ? "존재" : "없음");
        
        if (mqttConfig != null) {
            logger.info("MQTT 설정 - 서버: {}, 사용자명: {}, 비밀번호: {}", 
                mqttConfig.getServer(), mqttConfig.getUsername(), 
                mqttConfig.getPassword() != null ? "설정됨" : "없음");
            
            // MQTT 연결 설정 저장
            this.brokerUrl = mqttConfig.getServer();
            this.clientId = mqttConfig.getClientId();
            this.userName = mqttConfig.getUsername();
            this.password = mqttConfig.getPassword();
        } else {
            logger.error("MQTT 설정 객체가 null입니다!");
        }
        
        scheduler = Executors.newScheduledThreadPool(2);
        startConnectionMonitoring();
        
        logger.info("=== MQTT 연결 관리자 초기화 완료 ===");
    }
    
    @PreDestroy
    public void cleanup() {
        logger.info("=== MQTT 연결 관리자 정리 시작 ===");
        
        // 1. 재연결 중단
        isReconnecting.set(false);
        isConnected.set(false);
        
        // 2. MQTT 클라이언트 정리 (우선순위)
        if (mqttClient != null) {
            try {
                logger.info("MQTT 클라이언트 연결 해제 시작");
                
                // 먼저 일반 disconnect 시도
                try {
                    mqttClient.disconnect();
                    logger.info("MQTT 클라이언트 일반 연결 해제 완료");
                } catch (Exception e) {
                    logger.warn("MQTT 일반 연결 해제 실패, 강제 종료 시도: {}", e.getMessage());
                }
                
                // 강제 종료로 내부 스레드 완전 정리
                try {
                    mqttClient.forceShutdown();
                    logger.info("MQTT 클라이언트 강제 종료 완료");
                } catch (Exception e) {
                    logger.error("MQTT 강제 종료 중 오류 발생: {}", e.getMessage());
                }
                
                // 상태 플래그 초기화
                isConnected.set(false);
                reconnectAttempts.set(0);
                isReconnecting.set(false);
                
            } catch (Exception e) {
                logger.error("MQTT 연결 해제 중 오류 발생", e);
            } finally {
                mqttClient = null;
            }
        }
        
        // 3. 스케줄러 정리 (강화)
        if (scheduler != null && !scheduler.isShutdown()) {
            logger.info("스케줄러 종료 시작");
            scheduler.shutdownNow(); // 즉시 강제 종료
            try {
                // 종료 완료 대기 (최대 5초로 증가)
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("스케줄러 강제 종료 타임아웃 - 일부 작업이 완료되지 않았을 수 있음");
                } else {
                    logger.info("스케줄러 종료 완료");
                }
            } catch (InterruptedException e) {
                logger.warn("스케줄러 정리 중 인터럽트 발생");
                Thread.currentThread().interrupt();
            } finally {
                scheduler = null;
            }
        }
        
        // 4. 추가 대기 시간 (내부 스레드 완전 정리)
        try {
            Thread.sleep(2000);
            logger.info("MQTT 내부 스레드 완전 정리 대기 완료");
        } catch (InterruptedException e) {
            logger.warn("MQTT 스레드 정리 대기 중 인터럽트 발생");
            Thread.currentThread().interrupt();
        }
        
        // 5. 강제 가비지 컬렉션 실행
        System.gc();
        logger.info("=== MQTT 연결 관리자 정리 완료 ===");
    }
    
    /**
     * MQTT 연결 초기화
     */
    public void initializeConnection() {
        logger.info("=== MQTT 연결 초기화 시작 ===");
        try {
            String clientId = "HnTSensorAPI_" + System.currentTimeMillis();
            logger.info("MQTT 클라이언트 생성 - 서버: {}, 클라이언트 ID: {}", 
                mqttConfig.getServer(), clientId);
            
            mqttClient = new MQTT(
                mqttConfig.getServer(),
                clientId,
                mqttConfig.getUsername(),
                mqttConfig.getPassword()
            );
            
            // MqttService 설정 (메시지 처리용)
            mqttClient.setMqttService(mqttService);
            logger.info("MQTT 클라이언트에 MqttService 설정 완료");
            
            logger.info("MQTT 클라이언트 생성 완료 - init() 호출 시작");
            // 실제 MQTT 연결 수행
            mqttClient.init("#", "Y"); // 모든 토픽 구독 활성화
            logger.info("MQTT init() 호출 완료");
            
            // 연결 상태 확인 및 업데이트
            if (mqttClient != null && mqttClient.isConnected()) {
                isConnected.set(true);
                reconnectAttempts.set(0);
                logger.info("MQTT 연결 초기화 완료 - 서버: {}, 클라이언트 ID: {}", 
                    mqttConfig.getServer(), clientId);
                
                // 연결 성공 이벤트 발행 (프론트엔드 동기화용)
                publishConnectionStatusEvent(true, "연결 성공");
            } else {
                isConnected.set(false);
                logger.warn("MQTT 연결 실패 - 클라이언트가 연결되지 않음");
                
                // 연결 실패 이벤트 발행
                publishConnectionStatusEvent(false, "연결 실패");
                
                // 연결 실패 시 재시도 로직 실행
                if (reconnectAttempts.get() < mqttConfig.getMaxReconnectAttempts()) {
                    attemptReconnect();
                } else {
                    logger.error("최대 재연결 시도 횟수 초과 - MQTT 연결 포기");
                }
            }
                
        } catch (Exception e) {
            logger.error("MQTT 연결 초기화 실패 - 서버: {}, 오류: {}", 
                mqttConfig.getServer(), e.getMessage(), e);
            isConnected.set(false);
            
            // 연결 실패 시 재시도 로직 실행
            if (reconnectAttempts.get() < mqttConfig.getMaxReconnectAttempts()) {
                attemptReconnect();
            } else {
                logger.error("최대 재연결 시도 횟수 초과 - MQTT 연결 포기");
            }
        }
    }
    
    /**
     * 연결 상태 모니터링 시작
     */
    private void startConnectionMonitoring() {
        // 10초마다 연결 상태 확인 (더 빠른 감지)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (mqttClient != null && !isConnected.get()) {
                    logger.warn("MQTT 연결 끊김 감지 - 재연결 시도 중...");
                    attemptReconnect();
                } else if (mqttClient != null && isConnected.get()) {
                    // 연결 상태 검증 (핑 테스트)
                    validateConnection();
                }
            } catch (Exception e) {
                logger.error("연결 상태 모니터링 중 오류 발생", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
        
        // 1분마다 연결 통계 로깅 (더 자주 모니터링)
        scheduler.scheduleAtFixedRate(() -> {
            logger.info("MQTT 연결 상태 - 연결됨: {}, 재연결 시도 횟수: {}, 재연결 중: {}", 
                isConnected.get(), reconnectAttempts.get(), isReconnecting.get());
        }, 60, 60, TimeUnit.SECONDS);
        
        // 5분마다 상세 상태 로깅
        scheduler.scheduleAtFixedRate(() -> {
            logDetailedConnectionStatus();
        }, 300, 300, TimeUnit.SECONDS);
    }
    
    /**
     * 연결 상태 검증 (핑 테스트)
     */
    private void validateConnection() {
        try {
            if (mqttClient != null) {
                // MQTT 클라이언트의 실제 연결 상태 확인
                boolean actualConnected = mqttClient.isConnected();
                if (!actualConnected) {
                    logger.warn("MQTT 실제 연결 상태 확인 - 연결 끊김 감지");
                    isConnected.set(false);
                } else {
                    // 연결이 실제로 유지되고 있으면 상태 동기화
                    isConnected.set(true);
                    logger.debug("MQTT 연결 상태 검증 성공 - 연결 유지됨");
                }
            } else {
                logger.warn("MQTT 클라이언트가 null - 연결 상태를 false로 설정");
                isConnected.set(false);
            }
        } catch (Exception e) {
            logger.warn("MQTT 연결 상태 검증 중 오류 발생", e);
            isConnected.set(false);
        }
    }
    
    /**
     * 상세 연결 상태 로깅
     */
    private void logDetailedConnectionStatus() {
        try {
            logger.info("=== MQTT 연결 상태 상세 정보 ===");
            logger.info("서버: {}", mqttConfig.getServer());
            logger.info("사용자명: {}", mqttConfig.getUsername());
            logger.info("연결 상태: {}", isConnected.get());
            logger.info("재연결 시도 횟수: {}", reconnectAttempts.get());
            logger.info("재연결 중: {}", isReconnecting.get());
            logger.info("클라이언트 ID: {}", mqttClient != null ? "설정됨" : "없음");
            logger.info("=================================");
        } catch (Exception e) {
            logger.error("상세 연결 상태 로깅 중 오류 발생", e);
        }
    }
    
    /**
     * 재연결 시도
     */
    private void attemptReconnect() {
        if (isReconnecting.get()) {
            return; // 이미 재연결 중
        }
        
        isReconnecting.set(true);
        
        try {
            int currentAttempts = reconnectAttempts.incrementAndGet();
            
            if (currentAttempts > mqttConfig.getMaxReconnectAttempts()) {
                logger.error("최대 재연결 시도 횟수 초과 - 재연결 중단");
                return;
            }
            
            // 지수 백오프 방식으로 재연결 지연
            long delay = calculateReconnectDelay(currentAttempts);
            logger.info("MQTT 재연결 시도 {} - {}ms 후 재연결", currentAttempts, delay);
            
            scheduler.schedule(() -> {
                try {
                    initializeConnection();
                } catch (Exception e) {
                    logger.error("MQTT 재연결 실패", e);
                } finally {
                    isReconnecting.set(false);
                }
            }, delay, TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            logger.error("재연결 시도 중 오류 발생", e);
            isReconnecting.set(false);
        }
    }
    
    /**
     * 재연결 지연 시간 계산 (지수 백오프)
     */
    private long calculateReconnectDelay(int attemptCount) {
        long baseDelay = mqttConfig.getBaseReconnectDelay();
        long delay = baseDelay * (1L << (attemptCount - 1)); // 2^(attemptCount-1) * baseDelay
        return Math.min(delay, mqttConfig.getMaxReconnectDelay());
    }
    
    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        return isConnected.get();
    }
    
    /**
     * 연결 상태 이벤트 발행 (프론트엔드 동기화용)
     */
    private void publishConnectionStatusEvent(boolean connected, String message) {
        try {
            // 로그로 상태 발행
            logger.info("MQTT 연결 상태 이벤트 발행 - 연결: {}, 메시지: {}", connected, message);
            
            // TODO: 향후 WebSocket이나 Server-Sent Events를 통한 실시간 상태 전송 구현
            // 현재는 로그로만 상태를 기록하고, 프론트엔드에서 주기적으로 API 호출로 확인
            
        } catch (Exception e) {
            logger.warn("연결 상태 이벤트 발행 중 오류 발생", e);
        }
    }
    
    /**
     * 연결 상태 문자열 반환
     * @return 연결 상태
     */
    public String getConnectionStatus() {
        if (isConnected.get()) {
            return "CONNECTED";
        } else if (isReconnecting.get()) {
            return "RECONNECTING";
        } else {
            return "DISCONNECTED";
        }
    }
    
    /**
     * 마지막 메시지 시간 반환
     * @return 마지막 메시지 시간 (밀리초)
     */
    public long getLastMessageTime() {
        // 현재는 시스템 시간 반환 (실제 구현 시 마지막 메시지 시간 추적 필요)
        return System.currentTimeMillis();
    }
    
    /**
     * 재연결 시도 횟수 반환
     * @return 재연결 시도 횟수
     */
    public int getReconnectAttempts() {
        return reconnectAttempts.get();
    }
    
    /**
     * 연결 상태 설정
     */
    public void setConnected(boolean connected) {
        isConnected.set(connected);
        if (connected) {
            reconnectAttempts.set(0);
        }
    }
    
    /**
     * MQTT 클라이언트 인스턴스 반환
     */
    public MQTT getMqttClient() {
        return mqttClient;
    }
    
    /**
     * 연결 통계 정보 반환
     */
    public String getConnectionStats() {
        return String.format("연결됨: %s, 재연결 시도: %d, 재연결 중: %s", 
            isConnected.get(), reconnectAttempts.get(), isReconnecting.get());
    }
    
    /**
     * MQTT 재연결 시도
     * @return 재연결 성공 여부
     */
    public boolean reconnect() {
        try {
            logger.info("MQTT 재연결 시도");
            
            if (isReconnecting.get()) {
                logger.warn("이미 재연결 중입니다.");
                return false;
            }
            
            isReconnecting.set(true);
            
            // 기존 연결 정리
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            
            // 새 연결 시도
            mqttClient.init("#", "Y"); // 모든 토픽 구독 활성화
            boolean connected = mqttClient.isConnected();
            
            if (connected) {
                logger.info("MQTT 재연결 성공");
                isConnected.set(true);
                reconnectAttempts.set(0);
                return true;
            } else {
                logger.warn("MQTT 재연결 실패");
                return false;
            }
            
        } catch (Exception e) {
            logger.error("MQTT 재연결 중 오류 발생", e);
            return false;
        } finally {
            isReconnecting.set(false);
        }
    }
    
    /**
     * MQTT 연결 풀 재초기화
     */
    public void reinitialize() {
        try {
            logger.info("MQTT 연결 풀 재초기화 시작");
            
            // 기존 연결 정리
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            
            // 새 클라이언트 생성
            mqttClient = new MQTT(this.brokerUrl, this.clientId, this.userName, this.password);
            
            logger.info("MQTT 연결 풀 재초기화 완료");
            
        } catch (Exception e) {
            logger.error("MQTT 연결 풀 재초기화 중 오류 발생", e);
        }
    }
    
    /**
     * 백엔드 MQTT 상태 조회
     * @return 백엔드 상태 정보
     */
    public Map<String, Object> getBackendStatus() {
        try {
            // 실제 구현에서는 백엔드 API를 호출하여 상태를 조회
            // 여기서는 간단한 구현만 제공
            Map<String, Object> status = new HashMap<>();
            status.put("status", "connected");
            status.put("timestamp", System.currentTimeMillis());
            return status;
            
        } catch (Exception e) {
            logger.error("백엔드 MQTT 상태 조회 중 오류 발생", e);
            return null;
        }
    }
    
    
}
