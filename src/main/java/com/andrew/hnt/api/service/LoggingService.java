package com.andrew.hnt.api.service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 통합 로깅 서비스 인터페이스
 * 애플리케이션 전체의 로깅을 통합 관리
 */
public interface LoggingService {
    
    /**
     * 에러 로깅
     * @param logger Logger 인스턴스
     * @param operation 작업명
     * @param message 에러 메시지
     * @param throwable 예외 객체
     * @param context 추가 컨텍스트 정보
     */
    void logError(org.slf4j.Logger logger, String operation, String message, Throwable throwable, Map<String, Object> context);
    
    /**
     * 경고 로깅
     * @param logger Logger 인스턴스
     * @param operation 작업명
     * @param message 경고 메시지
     * @param context 추가 컨텍스트 정보
     */
    void logWarning(org.slf4j.Logger logger, String operation, String message, Map<String, Object> context);
    
    /**
     * 정보 로깅
     * @param logger Logger 인스턴스
     * @param operation 작업명
     * @param message 정보 메시지
     * @param context 추가 컨텍스트 정보
     */
    void logInfo(org.slf4j.Logger logger, String operation, String message, Map<String, Object> context);
    
    /**
     * 디버그 로깅
     * @param logger Logger 인스턴스
     * @param operation 작업명
     * @param message 디버그 메시지
     * @param context 추가 컨텍스트 정보
     */
    void logDebug(org.slf4j.Logger logger, String operation, String message, Map<String, Object> context);
    
    /**
     * 성능 로깅
     * @param logger Logger 인스턴스
     * @param operation 작업명
     * @param duration 실행 시간 (ms)
     * @param context 추가 컨텍스트 정보
     */
    void logPerformance(org.slf4j.Logger logger, String operation, long duration, Map<String, Object> context);
    
    /**
     * 보안 로깅
     * @param logger Logger 인스턴스
     * @param operation 작업명
     * @param message 보안 메시지
     * @param context 추가 컨텍스트 정보
     */
    void logSecurity(org.slf4j.Logger logger, String operation, String message, Map<String, Object> context);
    
    /**
     * 로그 통계 정보 반환
     * @return LogStats
     */
    LogStats getLogStats();
    
    /**
     * 로그 통계 데이터 클래스
     */
    class LogStats {
        private final long totalLogs;
        private final long errorLogs;
        private final long warningLogs;
        private final long infoLogs;
        private final long debugLogs;
        private final LocalDateTime lastLogTime;
        
        public LogStats(long totalLogs, long errorLogs, long warningLogs, long infoLogs, long debugLogs, LocalDateTime lastLogTime) {
            this.totalLogs = totalLogs;
            this.errorLogs = errorLogs;
            this.warningLogs = warningLogs;
            this.infoLogs = infoLogs;
            this.debugLogs = debugLogs;
            this.lastLogTime = lastLogTime;
        }
        
        // Getters
        public long getTotalLogs() { return totalLogs; }
        public long getErrorLogs() { return errorLogs; }
        public long getWarningLogs() { return warningLogs; }
        public long getInfoLogs() { return infoLogs; }
        public long getDebugLogs() { return debugLogs; }
        public LocalDateTime getLastLogTime() { return lastLogTime; }
    }
}
