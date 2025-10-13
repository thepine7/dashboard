package com.andrew.hnt.api.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 동적 로그 레벨 관리 설정
 * 런타임에 로그 레벨을 변경할 수 있는 기능 제공
 */
@Configuration
@RestController
@RequestMapping("/admin/logging")
public class LoggingConfig {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LoggingConfig.class);

    /**
     * 현재 로그 레벨 조회
     */
    @GetMapping("/levels")
    public Map<String, String> getLogLevels() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Map<String, String> logLevels = new HashMap<>();
        
        // 주요 로거들의 현재 레벨 조회
        String[] loggers = {
            "com.andrew.hnt.api",
            "com.andrew.hnt.api.mqtt",
            "com.andrew.hnt.api.mapper",
            "org.springframework",
            "org.springframework.web",
            "org.mybatis",
            "com.zaxxer.hikari",
            "org.eclipse.paho"
        };
        
        for (String loggerName : loggers) {
            Logger logger = context.getLogger(loggerName);
            logLevels.put(loggerName, logger.getLevel() != null ? logger.getLevel().toString() : "null");
        }
        
        return logLevels;
    }

    /**
     * 로그 레벨 동적 변경
     */
    @PostMapping("/levels")
    public Map<String, Object> setLogLevel(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String loggerName = request.get("logger");
            String level = request.get("level");
            
            if (loggerName == null || level == null) {
                result.put("success", false);
                result.put("message", "logger와 level 파라미터가 필요합니다.");
                return result;
            }
            
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = context.getLogger(loggerName);
            
            // 유효한 레벨인지 확인
            Level newLevel = Level.toLevel(level.toUpperCase());
            if (newLevel == null) {
                result.put("success", false);
                result.put("message", "유효하지 않은 로그 레벨입니다. (TRACE, DEBUG, INFO, WARN, ERROR)");
                return result;
            }
            
            logger.setLevel(newLevel);
            
            result.put("success", true);
            result.put("message", String.format("로그 레벨이 변경되었습니다: %s -> %s", loggerName, level));
            result.put("logger", loggerName);
            result.put("level", level);
            
            // 변경 로그 기록
            logger.info("로그 레벨이 동적으로 변경됨: {} -> {}", loggerName, level);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "로그 레벨 변경 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("로그 레벨 변경 실패", e);
        }
        
        return result;
    }

    /**
     * 로그 레벨 초기화 (기본값으로 복원)
     */
    @PostMapping("/levels/reset")
    public Map<String, Object> resetLogLevels() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            
            // 기본 로그 레벨 설정
            Map<String, Level> defaultLevels = new HashMap<>();
            defaultLevels.put("com.andrew.hnt.api", Level.INFO);
            defaultLevels.put("com.andrew.hnt.api.mqtt", Level.INFO);
            defaultLevels.put("com.andrew.hnt.api.mapper", Level.DEBUG);
            defaultLevels.put("org.springframework", Level.WARN);
            defaultLevels.put("org.springframework.web", Level.INFO);
            defaultLevels.put("org.mybatis", Level.WARN);
            defaultLevels.put("com.zaxxer.hikari", Level.INFO);
            defaultLevels.put("org.eclipse.paho", Level.WARN);
            
            for (Map.Entry<String, Level> entry : defaultLevels.entrySet()) {
                Logger logger = context.getLogger(entry.getKey());
                logger.setLevel(entry.getValue());
            }
            
            result.put("success", true);
            result.put("message", "모든 로그 레벨이 기본값으로 초기화되었습니다.");
            
            logger.info("로그 레벨이 기본값으로 초기화됨");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "로그 레벨 초기화 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("로그 레벨 초기화 실패", e);
        }
        
        return result;
    }

    /**
     * 로그 파일 정보 조회
     */
    @GetMapping("/files")
    public Map<String, Object> getLogFiles() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, String> logFiles = new HashMap<>();
            logFiles.put("main", "logs/hnt-sensor-api.log");
            logFiles.put("error", "logs/hnt-sensor-api-error.log");
            logFiles.put("json", "logs/hnt-sensor-api-json.log");
            logFiles.put("mqtt", "logs/hnt-sensor-api-mqtt.log");
            logFiles.put("database", "logs/hnt-sensor-api-db.log");
            logFiles.put("performance", "logs/hnt-sensor-api-performance.log");
            
            result.put("success", true);
            result.put("logFiles", logFiles);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "로그 파일 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("로그 파일 정보 조회 실패", e);
        }
        
        return result;
    }

    /**
     * 로그 통계 정보 조회
     */
    @GetMapping("/stats")
    public Map<String, Object> getLogStats() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalLoggers", context.getLoggerList().size());
            stats.put("contextName", context.getName());
            stats.put("startTime", context.getBirthTime());
            
            result.put("success", true);
            result.put("stats", stats);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "로그 통계 조회 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("로그 통계 조회 실패", e);
        }
        
        return result;
    }
}
