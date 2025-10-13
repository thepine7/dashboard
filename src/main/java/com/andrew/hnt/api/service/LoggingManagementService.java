package com.andrew.hnt.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 로깅 시스템 관리 서비스
 * 동적 로그 레벨 변경 및 로깅 통계 제공
 */
@Service
public class LoggingManagementService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingManagementService.class);
    
    /**
     * 현재 로그 레벨 정보 조회
     */
    public Map<String, Object> getLoggingInfo() {
        Map<String, Object> info = new HashMap<>();
        
        try {
            // 루트 로거 레벨
            ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            info.put("rootLevel", rootLogger.getLevel().toString());
            
            // 애플리케이션 로거 레벨
            ch.qos.logback.classic.Logger appLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.andrew.hnt.api");
            info.put("appLevel", appLogger.getLevel() != null ? appLogger.getLevel().toString() : "INHERITED");
            
            // 주요 로거들의 레벨 정보
            Map<String, String> loggerLevels = new HashMap<>();
            String[] loggerNames = {
                "com.andrew.hnt.api.mqtt",
                "com.andrew.hnt.api.mapper",
                "com.andrew.hnt.api.service.QueryPerformanceService",
                "com.andrew.hnt.api.service.MemoryOptimizationService",
                "org.springframework",
                "org.springframework.web",
                "org.mybatis",
                "com.zaxxer.hikari"
            };
            
            for (String loggerName : loggerNames) {
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerName);
                loggerLevels.put(loggerName, logger.getLevel() != null ? logger.getLevel().toString() : "INHERITED");
            }
            
            info.put("loggerLevels", loggerLevels);
            info.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.error("로깅 정보 조회 중 오류 발생", e);
            info.put("error", e.getMessage());
        }
        
        return info;
    }
    
    /**
     * 로그 레벨 동적 변경
     */
    public boolean changeLogLevel(String loggerName, String level) {
        try {
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerName);
            ch.qos.logback.classic.Level newLevel = ch.qos.logback.classic.Level.toLevel(level);
            
            if (newLevel == null) {
                logger.warn("유효하지 않은 로그 레벨: {}", level);
                return false;
            }
            
            logger.setLevel(newLevel);
            logger.info("로그 레벨 변경 완료: {} -> {}", loggerName, level);
            return true;
            
        } catch (Exception e) {
            logger.error("로그 레벨 변경 실패: {} -> {}", loggerName, level, e);
            return false;
        }
    }
    
    /**
     * 로그 레벨 초기화
     */
    public void resetLogLevels() {
        try {
            // 애플리케이션 로거 레벨 초기화
            ch.qos.logback.classic.Logger appLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.andrew.hnt.api");
            appLogger.setLevel(null); // 상위 로거 레벨 상속
            
            // MQTT 로거 레벨 초기화
            ch.qos.logback.classic.Logger mqttLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.andrew.hnt.api.mqtt");
            mqttLogger.setLevel(null);
            
            // 데이터베이스 로거 레벨 초기화
            ch.qos.logback.classic.Logger dbLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.andrew.hnt.api.mapper");
            dbLogger.setLevel(null);
            
            logger.info("로그 레벨 초기화 완료");
            
        } catch (Exception e) {
            logger.error("로그 레벨 초기화 실패", e);
        }
    }
    
    /**
     * 로깅 통계 정보 조회
     */
    public Map<String, Object> getLoggingStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 로그 파일 크기 정보 (실제 구현에서는 파일 시스템 접근 필요)
            Map<String, Object> fileStats = new HashMap<>();
            fileStats.put("mainLog", "logs/hnt-sensor-api.log");
            fileStats.put("errorLog", "logs/hnt-sensor-api-error.log");
            fileStats.put("mqttLog", "logs/hnt-sensor-api-mqtt.log");
            fileStats.put("dbLog", "logs/hnt-sensor-api-db.log");
            fileStats.put("performanceLog", "logs/hnt-sensor-api-performance.log");
            fileStats.put("jsonLog", "logs/hnt-sensor-api-json.log");
            
            stats.put("logFiles", fileStats);
            stats.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.error("로깅 통계 조회 중 오류 발생", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 로그 레벨 변경 이력 기록
     */
    public void logLevelChangeHistory(String loggerName, String oldLevel, String newLevel) {
        logger.info("로그 레벨 변경 이력 - Logger: {}, 이전: {}, 현재: {}", loggerName, oldLevel, newLevel);
    }
}
