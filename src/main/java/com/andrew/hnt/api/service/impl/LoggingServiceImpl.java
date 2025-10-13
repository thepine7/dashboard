package com.andrew.hnt.api.service.impl;

import com.andrew.hnt.api.service.LoggingService;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 통합 로깅 서비스 구현체
 * 애플리케이션 전체의 로깅을 통합 관리
 */
@Service
public class LoggingServiceImpl implements LoggingService {
    
    // 로그 통계
    private final AtomicLong totalLogs = new AtomicLong(0);
    private final AtomicLong errorLogs = new AtomicLong(0);
    private final AtomicLong warningLogs = new AtomicLong(0);
    private final AtomicLong infoLogs = new AtomicLong(0);
    private final AtomicLong debugLogs = new AtomicLong(0);
    private volatile LocalDateTime lastLogTime = LocalDateTime.now();
    
    @Override
    public void logError(Logger logger, String operation, String message, Throwable throwable, Map<String, Object> context) {
        if (logger.isErrorEnabled()) {
            String formattedMessage = formatLogMessage(operation, message, context);
            if (throwable != null) {
                logger.error(formattedMessage, throwable);
            } else {
                logger.error(formattedMessage);
            }
            
            totalLogs.incrementAndGet();
            errorLogs.incrementAndGet();
            lastLogTime = LocalDateTime.now();
        }
    }
    
    @Override
    public void logWarning(Logger logger, String operation, String message, Map<String, Object> context) {
        if (logger.isWarnEnabled()) {
            String formattedMessage = formatLogMessage(operation, message, context);
            logger.warn(formattedMessage);
            
            totalLogs.incrementAndGet();
            warningLogs.incrementAndGet();
            lastLogTime = LocalDateTime.now();
        }
    }
    
    @Override
    public void logInfo(Logger logger, String operation, String message, Map<String, Object> context) {
        if (logger.isInfoEnabled()) {
            String formattedMessage = formatLogMessage(operation, message, context);
            logger.info(formattedMessage);
            
            totalLogs.incrementAndGet();
            infoLogs.incrementAndGet();
            lastLogTime = LocalDateTime.now();
        }
    }
    
    @Override
    public void logDebug(Logger logger, String operation, String message, Map<String, Object> context) {
        if (logger.isDebugEnabled()) {
            String formattedMessage = formatLogMessage(operation, message, context);
            logger.debug(formattedMessage);
            
            totalLogs.incrementAndGet();
            debugLogs.incrementAndGet();
            lastLogTime = LocalDateTime.now();
        }
    }
    
    @Override
    public void logPerformance(Logger logger, String operation, long duration, Map<String, Object> context) {
        if (logger.isInfoEnabled()) {
            String message = String.format("성능 측정 - 실행 시간: %dms", duration);
            String formattedMessage = formatLogMessage(operation, message, context);
            logger.info(formattedMessage);
            
            totalLogs.incrementAndGet();
            infoLogs.incrementAndGet();
            lastLogTime = LocalDateTime.now();
        }
    }
    
    @Override
    public void logSecurity(Logger logger, String operation, String message, Map<String, Object> context) {
        if (logger.isWarnEnabled()) {
            String formattedMessage = formatLogMessage(operation, message, context);
            logger.warn("[SECURITY] " + formattedMessage);
            
            totalLogs.incrementAndGet();
            warningLogs.incrementAndGet();
            lastLogTime = LocalDateTime.now();
        }
    }
    
    @Override
    public LogStats getLogStats() {
        return new LogStats(
            totalLogs.get(),
            errorLogs.get(),
            warningLogs.get(),
            infoLogs.get(),
            debugLogs.get(),
            lastLogTime
        );
    }
    
    /**
     * 로그 메시지 포맷팅
     */
    private String formatLogMessage(String operation, String message, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(operation).append("] ").append(message);
        
        if (context != null && !context.isEmpty()) {
            sb.append(" | Context: ");
            context.forEach((key, value) -> {
                sb.append(key).append("=").append(value).append(", ");
            });
            // 마지막 쉼표 제거
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2);
            }
        }
        
        return sb.toString();
    }
}
