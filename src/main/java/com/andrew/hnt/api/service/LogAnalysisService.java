package com.andrew.hnt.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 로그 분석 서비스
 * 로그 파일을 분석하여 통계 정보와 패턴을 제공
 */
@Service
public class LogAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(LogAnalysisService.class);
    
    private static final String LOG_DIR = "logs/";
    private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // 로그 패턴 정규식
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[(.*?)\\] (\\w+) \\[(.*?)\\] - (.*)"
    );

    /**
     * 로그 파일 분석 - 에러 통계
     */
    public Map<String, Object> analyzeErrorLogs(String logFile, int hours) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
            Map<String, Integer> errorCounts = new HashMap<>();
            List<Map<String, String>> recentErrors = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(new FileReader(LOG_DIR + logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = LOG_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String timestamp = matcher.group(1);
                        String level = matcher.group(3);
                        String logger = matcher.group(4);
                        String message = matcher.group(5);
                        
                        if ("ERROR".equals(level)) {
                            LocalDateTime logTime = LocalDateTime.parse(timestamp, LOG_DATE_FORMAT);
                            if (logTime.isAfter(cutoffTime)) {
                                // 에러 카운트
                                errorCounts.merge(logger, 1, Integer::sum);
                                
                                // 최근 에러 정보
                                if (recentErrors.size() < 50) { // 최대 50개
                                    Map<String, String> errorInfo = new HashMap<>();
                                    errorInfo.put("timestamp", timestamp);
                                    errorInfo.put("logger", logger);
                                    errorInfo.put("message", message);
                                    recentErrors.add(errorInfo);
                                }
                            }
                        }
                    }
                }
            }
            
            result.put("success", true);
            result.put("errorCounts", errorCounts);
            result.put("recentErrors", recentErrors);
            result.put("totalErrors", errorCounts.values().stream().mapToInt(Integer::intValue).sum());
            result.put("analysisPeriod", hours + " hours");
            
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "로그 파일 분석 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("로그 파일 분석 실패", e);
        }
        
        return result;
    }

    /**
     * 로그 파일 분석 - 성능 통계
     */
    public Map<String, Object> analyzePerformanceLogs(String logFile, int hours) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
            Map<String, List<Long>> responseTimes = new HashMap<>();
            int totalRequests = 0;
            
            try (BufferedReader reader = new BufferedReader(new FileReader(LOG_DIR + logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = LOG_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String timestamp = matcher.group(1);
                        String logger = matcher.group(4);
                        String message = matcher.group(5);
                        
                        LocalDateTime logTime = LocalDateTime.parse(timestamp, LOG_DATE_FORMAT);
                        if (logTime.isAfter(cutoffTime)) {
                            
                            // 응답 시간 추출 (예: "Response time: 150ms")
                            Pattern responseTimePattern = Pattern.compile("Response time: (\\d+)ms");
                            Matcher responseTimeMatcher = responseTimePattern.matcher(message);
                            if (responseTimeMatcher.find()) {
                                long responseTime = Long.parseLong(responseTimeMatcher.group(1));
                                responseTimes.computeIfAbsent(logger, k -> new ArrayList<>()).add(responseTime);
                                totalRequests++;
                            }
                            
                            // 쿼리 실행 시간 추출 (예: "Query execution time: 25ms")
                            Pattern queryTimePattern = Pattern.compile("Query execution time: (\\d+)ms");
                            Matcher queryTimeMatcher = queryTimePattern.matcher(message);
                            if (queryTimeMatcher.find()) {
                                long queryTime = Long.parseLong(queryTimeMatcher.group(1));
                                responseTimes.computeIfAbsent("database", k -> new ArrayList<>()).add(queryTime);
                            }
                        }
                    }
                }
            }
            
            // 통계 계산
            Map<String, Map<String, Object>> performanceStats = new HashMap<>();
            for (Map.Entry<String, List<Long>> entry : responseTimes.entrySet()) {
                List<Long> times = entry.getValue();
                if (!times.isEmpty()) {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("count", times.size());
                    stats.put("min", Collections.min(times));
                    stats.put("max", Collections.max(times));
                    stats.put("avg", times.stream().mapToLong(Long::longValue).average().orElse(0.0));
                    stats.put("p95", calculatePercentile(times, 95));
                    stats.put("p99", calculatePercentile(times, 99));
                    performanceStats.put(entry.getKey(), stats);
                }
            }
            
            result.put("success", true);
            result.put("performanceStats", performanceStats);
            result.put("totalRequests", totalRequests);
            result.put("analysisPeriod", hours + " hours");
            
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "성능 로그 분석 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("성능 로그 분석 실패", e);
        }
        
        return result;
    }

    /**
     * 로그 파일 분석 - MQTT 통계
     */
    public Map<String, Object> analyzeMqttLogs(String logFile, int hours) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
            Map<String, Integer> messageCounts = new HashMap<>();
            Map<String, Integer> connectionEvents = new HashMap<>();
            int totalMessages = 0;
            
            try (BufferedReader reader = new BufferedReader(new FileReader(LOG_DIR + logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = LOG_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String timestamp = matcher.group(1);
                        String message = matcher.group(5);
                        
                        LocalDateTime logTime = LocalDateTime.parse(timestamp, LOG_DATE_FORMAT);
                        if (logTime.isAfter(cutoffTime)) {
                            
                            // MQTT 메시지 카운트
                            if (message.contains("MQTT 메시지 수신") || message.contains("MQTT 메시지 발송")) {
                                messageCounts.merge("messages", 1, Integer::sum);
                                totalMessages++;
                            }
                            
                            // 연결 이벤트 카운트
                            if (message.contains("연결 성공")) {
                                connectionEvents.merge("success", 1, Integer::sum);
                            } else if (message.contains("연결 실패")) {
                                connectionEvents.merge("failure", 1, Integer::sum);
                            } else if (message.contains("연결 끊김")) {
                                connectionEvents.merge("disconnect", 1, Integer::sum);
                            }
                            
                            // 토픽별 메시지 카운트
                            Pattern topicPattern = Pattern.compile("토픽: ([^,]+)");
                            Matcher topicMatcher = topicPattern.matcher(message);
                            if (topicMatcher.find()) {
                                String topic = topicMatcher.group(1);
                                messageCounts.merge("topic_" + topic, 1, Integer::sum);
                            }
                        }
                    }
                }
            }
            
            result.put("success", true);
            result.put("messageCounts", messageCounts);
            result.put("connectionEvents", connectionEvents);
            result.put("totalMessages", totalMessages);
            result.put("analysisPeriod", hours + " hours");
            
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "MQTT 로그 분석 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("MQTT 로그 분석 실패", e);
        }
        
        return result;
    }

    /**
     * 로그 파일 분석 - 일반 통계
     */
    public Map<String, Object> analyzeGeneralLogs(String logFile, int hours) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
            Map<String, Integer> levelCounts = new HashMap<>();
            Map<String, Integer> loggerCounts = new HashMap<>();
            int totalLogs = 0;
            
            try (BufferedReader reader = new BufferedReader(new FileReader(LOG_DIR + logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = LOG_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String timestamp = matcher.group(1);
                        String level = matcher.group(3);
                        String logger = matcher.group(4);
                        
                        LocalDateTime logTime = LocalDateTime.parse(timestamp, LOG_DATE_FORMAT);
                        if (logTime.isAfter(cutoffTime)) {
                            levelCounts.merge(level, 1, Integer::sum);
                            loggerCounts.merge(logger, 1, Integer::sum);
                            totalLogs++;
                        }
                    }
                }
            }
            
            result.put("success", true);
            result.put("levelCounts", levelCounts);
            result.put("loggerCounts", loggerCounts);
            result.put("totalLogs", totalLogs);
            result.put("analysisPeriod", hours + " hours");
            
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "일반 로그 분석 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("일반 로그 분석 실패", e);
        }
        
        return result;
    }

    /**
     * 백분위수 계산
     */
    private double calculatePercentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0.0;
        
        List<Long> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);
        
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, index));
    }

    /**
     * 로그 파일 목록 조회
     */
    public List<String> getAvailableLogFiles() {
        List<String> logFiles = new ArrayList<>();
        logFiles.add("hnt-sensor-api.log");
        logFiles.add("hnt-sensor-api-error.log");
        logFiles.add("hnt-sensor-api-json.log");
        logFiles.add("hnt-sensor-api-mqtt.log");
        logFiles.add("hnt-sensor-api-db.log");
        logFiles.add("hnt-sensor-api-performance.log");
        return logFiles;
    }
}
