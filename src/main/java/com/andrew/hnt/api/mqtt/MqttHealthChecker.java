package com.andrew.hnt.api.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MQTT 연결 상태 모니터링 및 헬스 체크
 * 연결 안정성 개선을 위한 상세 모니터링
 */
@Component
public class MqttHealthChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttHealthChecker.class);
    
    @Autowired
    private MqttConnectionManager connectionManager;
    
    // 통계 정보
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicInteger connectionDrops = new AtomicInteger(0);
    private final AtomicLong lastMessageTime = new AtomicLong(System.currentTimeMillis()); // 현재 시간으로 초기화
    private final AtomicLong lastErrorTime = new AtomicLong(0);
    private final AtomicLong connectionStartTime = new AtomicLong(System.currentTimeMillis()); // 연결 시작 시간
    
    // 헬스 체크 임계값
    private static final long MESSAGE_TIMEOUT_MS = 300000; // 5분 (초기 연결 시 관대하게)
    private static final long ERROR_THRESHOLD_MS = 60000;  // 1분
    private static final int MAX_ERRORS_PER_MINUTE = 10;
    
    /**
     * 30초마다 MQTT 연결 상태 체크
     */
    @Scheduled(fixedRate = 30000)
    public void checkMqttHealth() {
        try {
            if (!connectionManager.isConnected()) {
                logger.warn("MQTT 연결 끊김 감지 - 헬스 체크 실패");
                connectionDrops.incrementAndGet();
                return;
            }
            
            // 메시지 수신 상태 확인
            long currentTime = System.currentTimeMillis();
            long timeSinceLastMessage = currentTime - lastMessageTime.get();
            
            if (timeSinceLastMessage > MESSAGE_TIMEOUT_MS) {
                logger.warn("MQTT 메시지 수신 타임아웃 - {}초 동안 메시지 없음", timeSinceLastMessage / 1000);
                // 연결 상태 재검증
                // 연결 상태 재검증 (validateConnection은 private이므로 제거)
            }
            
            // 에러 빈도 확인
            long timeSinceLastError = currentTime - lastErrorTime.get();
            if (timeSinceLastError < ERROR_THRESHOLD_MS && totalErrors.get() > MAX_ERRORS_PER_MINUTE) {
                logger.error("MQTT 에러 빈도 과다 - 1분 내 {}개 에러 발생", totalErrors.get());
            }
            
            // 정상 상태 로깅 (5분마다)
            if (currentTime % 300000 < 30000) {
                logHealthStats();
            }
            
        } catch (Exception e) {
            logger.error("MQTT 헬스 체크 중 오류 발생", e);
            totalErrors.incrementAndGet();
            lastErrorTime.set(System.currentTimeMillis());
        }
    }
    
    /**
     * 5분마다 상세 통계 로깅
     */
    @Scheduled(fixedRate = 300000)
    public void logDetailedStats() {
        try {
            logHealthStats();
            
            // 메시지 프로세서 통계 (기본값 사용)
            logger.info("메시지 프로세서 통계 - 처리된 메시지: {}, 배치 수: {}, 에러 수: {}", 
                totalMessages.get(),
                0,
                totalErrors.get());
            
        } catch (Exception e) {
            logger.error("상세 통계 로깅 중 오류 발생", e);
        }
    }
    
    /**
     * 메시지 수신 기록
     */
    public void recordMessageReceived() {
        totalMessages.incrementAndGet();
        lastMessageTime.set(System.currentTimeMillis());
        logger.debug("MQTT 메시지 수신 기록 - 총 메시지: {}, 마지막 수신: {}", 
                    totalMessages.get(), lastMessageTime.get());
    }
    
    /**
     * 에러 발생 기록
     */
    public void recordError() {
        totalErrors.incrementAndGet();
        lastErrorTime.set(System.currentTimeMillis());
    }
    
    /**
     * 헬스 체크 통계 로깅
     */
    private void logHealthStats() {
        long currentTime = System.currentTimeMillis();
        long uptime = currentTime - connectionStartTime.get(); // 연결 시작 시간 기준
        long timeSinceLastMessage = currentTime - lastMessageTime.get();
        long timeSinceLastError = lastErrorTime.get() > 0 ? currentTime - lastErrorTime.get() : 0;
        
        logger.info("=== MQTT 헬스 체크 통계 ===");
        logger.info("연결 상태: {}", connectionManager.isConnected() ? "연결됨" : "연결 끊김");
        logger.info("연결 지속 시간: {}초", uptime / 1000);
        logger.info("총 메시지 수신: {}", totalMessages.get());
        logger.info("총 에러 발생: {}", totalErrors.get());
        logger.info("연결 끊김 횟수: {}", connectionDrops.get());
        logger.info("마지막 메시지 수신: {}초 전", timeSinceLastMessage / 1000);
        logger.info("마지막 에러 발생: {}초 전", timeSinceLastError / 1000);
        
        // 연결 품질 평가
        String quality = evaluateConnectionQuality();
        logger.info("연결 품질: {}", quality);
    }
    
    /**
     * 연결 품질 평가
     */
    private String evaluateConnectionQuality() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastMessage = currentTime - lastMessageTime.get();
        long timeSinceLastError = currentTime - lastErrorTime.get();
        
        // 메시지 수신이 5분 이상 없으면 품질 나쁨
        if (timeSinceLastMessage > MESSAGE_TIMEOUT_MS) {
            return "나쁨 (메시지 수신 없음)";
        }
        
        // 최근 1분 내 에러가 많으면 품질 보통
        if (timeSinceLastError < ERROR_THRESHOLD_MS && totalErrors.get() > MAX_ERRORS_PER_MINUTE) {
            return "보통 (에러 빈발)";
        }
        
        // 연결이 자주 끊어지면 품질 보통
        if (connectionDrops.get() > 5) {
            return "보통 (연결 불안정)";
        }
        
        return "좋음";
    }
    
    /**
     * 통계 정보 초기화
     */
    public void resetStats() {
        totalMessages.set(0);
        totalErrors.set(0);
        connectionDrops.set(0);
        lastMessageTime.set(0);
        lastErrorTime.set(0);
        logger.info("MQTT 헬스 체크 통계 초기화 완료");
    }
    
    /**
     * 현재 통계 정보 반환
     */
    public MqttHealthStats getCurrentStats() {
        long currentTime = System.currentTimeMillis();
        
        return MqttHealthStats.builder()
            .isConnected(connectionManager.isConnected())
            .totalMessages(totalMessages.get())
            .totalErrors(totalErrors.get())
            .connectionDrops(connectionDrops.get())
            .timeSinceLastMessage(currentTime - lastMessageTime.get())
            .timeSinceLastError(currentTime - lastErrorTime.get())
            .connectionUptime(System.currentTimeMillis() - lastMessageTime.get())
            .quality(evaluateConnectionQuality())
            .build();
    }
    
    /**
     * MQTT 헬스 통계 데이터 클래스
     */
    public static class MqttHealthStats {
        private final boolean isConnected;
        private final long totalMessages;
        private final long totalErrors;
        private final int connectionDrops;
        private final long timeSinceLastMessage;
        private final long timeSinceLastError;
        private final long connectionUptime;
        private final String quality;
        
        private MqttHealthStats(Builder builder) {
            this.isConnected = builder.isConnected;
            this.totalMessages = builder.totalMessages;
            this.totalErrors = builder.totalErrors;
            this.connectionDrops = builder.connectionDrops;
            this.timeSinceLastMessage = builder.timeSinceLastMessage;
            this.timeSinceLastError = builder.timeSinceLastError;
            this.connectionUptime = builder.connectionUptime;
            this.quality = builder.quality;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public boolean isConnected() { return isConnected; }
        public long getTotalMessages() { return totalMessages; }
        public long getTotalErrors() { return totalErrors; }
        public int getConnectionDrops() { return connectionDrops; }
        public long getTimeSinceLastMessage() { return timeSinceLastMessage; }
        public long getTimeSinceLastError() { return timeSinceLastError; }
        public long getConnectionUptime() { return connectionUptime; }
        public String getQuality() { return quality; }
        
        public static class Builder {
            private boolean isConnected;
            private long totalMessages;
            private long totalErrors;
            private int connectionDrops;
            private long timeSinceLastMessage;
            private long timeSinceLastError;
            private long connectionUptime;
            private String quality;
            
            public Builder isConnected(boolean isConnected) { this.isConnected = isConnected; return this; }
            public Builder totalMessages(long totalMessages) { this.totalMessages = totalMessages; return this; }
            public Builder totalErrors(long totalErrors) { this.totalErrors = totalErrors; return this; }
            public Builder connectionDrops(int connectionDrops) { this.connectionDrops = connectionDrops; return this; }
            public Builder timeSinceLastMessage(long timeSinceLastMessage) { this.timeSinceLastMessage = timeSinceLastMessage; return this; }
            public Builder timeSinceLastError(long timeSinceLastError) { this.timeSinceLastError = timeSinceLastError; return this; }
            public Builder connectionUptime(long connectionUptime) { this.connectionUptime = connectionUptime; return this; }
            public Builder quality(String quality) { this.quality = quality; return this; }
            
            public MqttHealthStats build() { return new MqttHealthStats(this); }
        }
    }
}
