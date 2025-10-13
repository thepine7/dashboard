package com.andrew.hnt.api.service.impl;

import com.andrew.hnt.api.service.MetricsStorageService;
import com.andrew.hnt.api.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 메트릭 저장 서비스 구현체
 * 파일 기반 메트릭 저장 및 조회 기능 제공
 */
@Service
public class MetricsStorageServiceImpl implements MetricsStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsStorageServiceImpl.class);
    
    private static final String METRICS_DIR = "metrics/";
    private static final String METRICS_FILE_PREFIX = "metrics_";
    private static final String METRICS_FILE_SUFFIX = ".json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 메모리 캐시 (최신 메트릭)
    private final Map<String, Object> latestMetrics = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> metricsCache = new ConcurrentHashMap<>();
    
    // 메트릭 저장소 초기화
    public MetricsStorageServiceImpl() {
        initializeStorage();
    }
    
    /**
     * 저장소 초기화
     */
    private void initializeStorage() {
        try {
            Path metricsDir = Paths.get(METRICS_DIR);
            if (!Files.exists(metricsDir)) {
                Files.createDirectories(metricsDir);
                logger.info("메트릭 저장소 디렉토리 생성: {}", METRICS_DIR);
            }
        } catch (Exception e) {
            logger.error("메트릭 저장소 초기화 실패", e);
        }
    }
    
    @Override
    public void saveMetrics(Map<String, Object> metrics, LocalDateTime timestamp) {
        try {
            // 메트릭 데이터에 타임스탬프 추가
            Map<String, Object> metricsWithTimestamp = new HashMap<>(metrics);
            metricsWithTimestamp.put("timestamp", timestamp.format(DATETIME_FORMATTER));
            metricsWithTimestamp.put("timestampMs", System.currentTimeMillis());
            
            // 파일명 생성 (날짜별)
            String dateStr = timestamp.format(DATE_FORMATTER);
            String fileName = METRICS_FILE_PREFIX + dateStr + METRICS_FILE_SUFFIX;
            Path filePath = Paths.get(METRICS_DIR, fileName);
            
            // 메트릭 데이터를 JSON 형태로 저장
            String jsonData = convertToJson(metricsWithTimestamp);
            
            // 파일에 추가 (기존 파일이 있으면 추가, 없으면 생성)
            try (FileWriter fw = new FileWriter(filePath.toFile(), true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(jsonData);
                bw.newLine();
            }
            
            // 최신 메트릭 업데이트
            latestMetrics.putAll(metricsWithTimestamp);
            
            // 캐시 업데이트
            updateCache(dateStr, metricsWithTimestamp);
            
            logger.debug("메트릭 저장 완료: {}", fileName);
            
        } catch (Exception e) {
            logger.error("메트릭 저장 실패", e);
        }
    }
    
    @Override
    public List<Map<String, Object>> getMetrics(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> allMetrics = new ArrayList<>();
        
        try {
            // 시작일부터 종료일까지의 모든 파일 조회
            LocalDateTime currentDate = startTime.toLocalDate().atStartOfDay();
            LocalDateTime endDate = endTime.toLocalDate().atStartOfDay();
            
            while (!currentDate.isAfter(endDate)) {
                String dateStr = currentDate.format(DATE_FORMATTER);
                String fileName = METRICS_FILE_PREFIX + dateStr + METRICS_FILE_SUFFIX;
                Path filePath = Paths.get(METRICS_DIR, fileName);
                
                if (Files.exists(filePath)) {
                    List<Map<String, Object>> dayMetrics = loadMetricsFromFile(filePath);
                    
                    // 시간 범위 필터링
                    List<Map<String, Object>> filteredMetrics = dayMetrics.stream()
                        .filter(metric -> {
                            String timestampStr = (String) metric.get("timestamp");
                            if (StringUtil.isEmpty(timestampStr)) {
                                return false;
                            }
                            
                            try {
                                LocalDateTime metricTime = LocalDateTime.parse(timestampStr, DATETIME_FORMATTER);
                                return !metricTime.isBefore(startTime) && !metricTime.isAfter(endTime);
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
                    
                    allMetrics.addAll(filteredMetrics);
                }
                
                currentDate = currentDate.plusDays(1);
            }
            
            // 타임스탬프 순으로 정렬
            allMetrics.sort((m1, m2) -> {
                String t1 = (String) m1.get("timestamp");
                String t2 = (String) m2.get("timestamp");
                return t1.compareTo(t2);
            });
            
        } catch (Exception e) {
            logger.error("메트릭 조회 실패", e);
        }
        
        return allMetrics;
    }
    
    @Override
    public Map<String, Object> getLatestMetrics() {
        return new HashMap<>(latestMetrics);
    }
    
    @Override
    public Map<String, Object> getMetricsStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            List<Map<String, Object>> metrics = getMetrics(startTime, endTime);
            
            if (metrics.isEmpty()) {
                statistics.put("count", 0);
                statistics.put("message", "조회된 메트릭이 없습니다.");
                return statistics;
            }
            
            // 기본 통계
            statistics.put("count", metrics.size());
            statistics.put("startTime", startTime.format(DATETIME_FORMATTER));
            statistics.put("endTime", endTime.format(DATETIME_FORMATTER));
            
            // CPU 사용률 통계
            List<Double> cpuValues = extractNumericValues(metrics, "system.cpuUsage");
            if (!cpuValues.isEmpty()) {
                statistics.put("cpu", calculateStatistics(cpuValues, "CPU 사용률"));
            }
            
            // 메모리 사용률 통계
            List<Double> memoryValues = extractNumericValues(metrics, "jvm.memoryUsage");
            if (!memoryValues.isEmpty()) {
                statistics.put("memory", calculateStatistics(memoryValues, "메모리 사용률"));
            }
            
            // 응답 시간 통계
            List<Double> responseTimeValues = extractNumericValues(metrics, "application.avgResponseTime");
            if (!responseTimeValues.isEmpty()) {
                statistics.put("responseTime", calculateStatistics(responseTimeValues, "응답 시간"));
            }
            
            // 데이터베이스 연결 통계
            List<Double> dbConnectionValues = extractNumericValues(metrics, "database.activeConnections");
            if (!dbConnectionValues.isEmpty()) {
                statistics.put("database", calculateStatistics(dbConnectionValues, "DB 연결 수"));
            }
            
            // MQTT 메시지 통계
            List<Double> mqttMessageValues = extractNumericValues(metrics, "mqtt.messagesReceived");
            if (!mqttMessageValues.isEmpty()) {
                statistics.put("mqtt", calculateStatistics(mqttMessageValues, "MQTT 메시지 수"));
            }
            
        } catch (Exception e) {
            logger.error("메트릭 통계 계산 실패", e);
            statistics.put("error", e.getMessage());
        }
        
        return statistics;
    }
    
    @Override
    public int cleanupOldMetrics(int retentionDays) {
        int cleanedCount = 0;
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            Path metricsDir = Paths.get(METRICS_DIR);
            
            if (Files.exists(metricsDir)) {
                Files.list(metricsDir)
                    .filter(path -> path.getFileName().toString().startsWith(METRICS_FILE_PREFIX))
                    .filter(path -> path.getFileName().toString().endsWith(METRICS_FILE_SUFFIX))
                    .forEach(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            String dateStr = fileName.substring(METRICS_FILE_PREFIX.length(), 
                                                             fileName.length() - METRICS_FILE_SUFFIX.length());
                            LocalDateTime fileDate = LocalDateTime.parse(dateStr + " 00:00:00", DATETIME_FORMATTER);
                            
                            if (fileDate.isBefore(cutoffDate)) {
                                Files.delete(path);
                                logger.info("오래된 메트릭 파일 삭제: {}", fileName);
                            }
                        } catch (Exception e) {
                            logger.warn("메트릭 파일 삭제 실패: {}", path.getFileName(), e);
                        }
                    });
            }
            
        } catch (Exception e) {
            logger.error("메트릭 정리 실패", e);
        }
        
        return cleanedCount;
    }
    
    @Override
    public Map<String, Object> getStorageStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            Path metricsDir = Paths.get(METRICS_DIR);
            
            if (Files.exists(metricsDir)) {
                long totalFiles = Files.list(metricsDir)
                    .filter(path -> path.getFileName().toString().startsWith(METRICS_FILE_PREFIX))
                    .count();
                
                long totalSize = Files.list(metricsDir)
                    .filter(path -> path.getFileName().toString().startsWith(METRICS_FILE_PREFIX))
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .sum();
                
                status.put("totalFiles", totalFiles);
                status.put("totalSizeBytes", totalSize);
                status.put("totalSizeMB", totalSize / (1024.0 * 1024.0));
                status.put("directory", METRICS_DIR);
                status.put("exists", true);
            } else {
                status.put("exists", false);
                status.put("message", "메트릭 저장소가 존재하지 않습니다.");
            }
            
            status.put("latestMetricsCount", latestMetrics.size());
            status.put("cacheSize", metricsCache.size());
            
        } catch (Exception e) {
            logger.error("저장소 상태 조회 실패", e);
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    /**
     * 메트릭 데이터를 JSON 문자열로 변환
     */
    private String convertToJson(Map<String, Object> data) {
        // 간단한 JSON 변환 (실제 구현에서는 Jackson 등 사용)
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
            
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * 파일에서 메트릭 데이터 로드
     */
    private List<Map<String, Object>> loadMetricsFromFile(Path filePath) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (StringUtil.isNotEmpty(line.trim())) {
                    try {
                        Map<String, Object> metric = parseJsonLine(line);
                        if (metric != null) {
                            metrics.add(metric);
                        }
                    } catch (Exception e) {
                        logger.warn("메트릭 라인 파싱 실패: {}", line, e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("메트릭 파일 로드 실패: {}", filePath, e);
        }
        
        return metrics;
    }
    
    /**
     * JSON 라인을 Map으로 파싱
     */
    private Map<String, Object> parseJsonLine(String jsonLine) {
        // 간단한 JSON 파싱 (실제 구현에서는 Jackson 등 사용)
        Map<String, Object> result = new HashMap<>();
        
        try {
            // JSON 문자열에서 키-값 쌍 추출
            String content = jsonLine.trim();
            if (content.startsWith("{") && content.endsWith("}")) {
                content = content.substring(1, content.length() - 1);
                
                String[] pairs = content.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replace("\"", "");
                        String value = keyValue[1].trim();
                        
                        // 값 타입 변환
                        Object parsedValue = parseValue(value);
                        result.put(key, parsedValue);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("JSON 파싱 실패: {}", jsonLine, e);
            return null;
        }
        
        return result;
    }
    
    /**
     * 값 타입 변환
     */
    private Object parseValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        } else if (value.equals("true")) {
            return true;
        } else if (value.equals("false")) {
            return false;
        } else if (value.equals("null")) {
            return null;
        } else {
            try {
                if (value.contains(".")) {
                    return Double.parseDouble(value);
                } else {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException e) {
                return value;
            }
        }
    }
    
    /**
     * 캐시 업데이트
     */
    private void updateCache(String dateStr, Map<String, Object> metrics) {
        metricsCache.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(metrics);
        
        // 캐시 크기 제한 (최대 1000개)
        List<Map<String, Object>> dayCache = metricsCache.get(dateStr);
        if (dayCache.size() > 1000) {
            dayCache.subList(0, dayCache.size() - 1000).clear();
        }
    }
    
    /**
     * 숫자 값 추출
     */
    private List<Double> extractNumericValues(List<Map<String, Object>> metrics, String keyPath) {
        return metrics.stream()
            .map(metric -> getNestedValue(metric, keyPath))
            .filter(Objects::nonNull)
            .filter(value -> value instanceof Number)
            .map(value -> ((Number) value).doubleValue())
            .collect(Collectors.toList());
    }
    
    /**
     * 중첩된 값 추출 (예: "system.cpuUsage")
     */
    private Object getNestedValue(Map<String, Object> map, String keyPath) {
        String[] keys = keyPath.split("\\.");
        Object current = map;
        
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * 통계 계산
     */
    private Map<String, Object> calculateStatistics(List<Double> values, String name) {
        Map<String, Object> stats = new HashMap<>();
        
        if (values.isEmpty()) {
            return stats;
        }
        
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double avg = sum / values.size();
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        
        // 표준편차 계산
        double variance = values.stream()
            .mapToDouble(value -> Math.pow(value - avg, 2))
            .average()
            .orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        stats.put("name", name);
        stats.put("count", values.size());
        stats.put("sum", sum);
        stats.put("avg", Math.round(avg * 100.0) / 100.0);
        stats.put("min", min);
        stats.put("max", max);
        stats.put("stdDev", Math.round(stdDev * 100.0) / 100.0);
        
        return stats;
    }
}
