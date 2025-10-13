package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.util.RealtimeUpdateConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * 실시간 데이터 업데이트 간격 최적화 컨트롤러
 * HnT Sensor API 프로젝트 전용
 * 
 * 실시간 데이터 업데이트 간격을 동적으로 관리하고
 * 성능 최적화를 위한 API를 제공
 */
@RestController
@RequestMapping("/api/realtime")
public class RealtimeUpdateController {
    
    private static final Logger logger = LoggerFactory.getLogger(RealtimeUpdateController.class);
    
    /**
     * 기본 업데이트 간격 조회
     * @return 기본 업데이트 간격 정보
     */
    @GetMapping("/interval/default")
    public ResponseEntity<Map<String, Object>> getDefaultInterval() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("defaultInterval", RealtimeUpdateConfig.getDefaultUpdateInterval());
            response.put("minInterval", 1000);
            response.put("maxInterval", 10000);
            response.put("message", "기본 업데이트 간격 조회 성공");
            
            logger.debug("기본 업데이트 간격 조회: {}ms", RealtimeUpdateConfig.getDefaultUpdateInterval());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("기본 업데이트 간격 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "기본 업데이트 간격 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 업데이트 간격 최적화 계산
     * @param context 업데이트 컨텍스트
     * @param currentInterval 현재 간격
     * @param performanceMetrics 성능 메트릭
     * @return 최적화된 간격 설정
     */
    @PostMapping("/interval/optimize")
    public ResponseEntity<Map<String, Object>> optimizeInterval(
            @RequestParam String context,
            @RequestParam int currentInterval,
            @RequestBody Map<String, Object> performanceMetrics) {
        try {
            // 성능 메트릭 파싱
            long totalUpdates = Long.parseLong(String.valueOf(performanceMetrics.getOrDefault("totalUpdates", 0)));
            long averageResponseTime = Long.parseLong(String.valueOf(performanceMetrics.getOrDefault("averageResponseTime", 0)));
            long maxResponseTime = Long.parseLong(String.valueOf(performanceMetrics.getOrDefault("maxResponseTime", 0)));
            long minResponseTime = Long.parseLong(String.valueOf(performanceMetrics.getOrDefault("minResponseTime", 0)));
            double errorRate = Double.parseDouble(String.valueOf(performanceMetrics.getOrDefault("errorRate", 0.0)));
            long lastUpdateTime = Long.parseLong(String.valueOf(performanceMetrics.getOrDefault("lastUpdateTime", System.currentTimeMillis())));
            
            // 성능 메트릭 객체 생성
            RealtimeUpdateConfig.UpdatePerformanceMetrics metrics = 
                new RealtimeUpdateConfig.UpdatePerformanceMetrics(
                    totalUpdates, averageResponseTime, maxResponseTime, 
                    minResponseTime, errorRate, lastUpdateTime
                );
            
            // 최적화된 간격 계산
            RealtimeUpdateConfig.UpdateIntervalConfig config = 
                RealtimeUpdateConfig.calculateOptimalInterval(context, currentInterval, metrics);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("interval", config.getInterval());
            response.put("reason", config.getReason());
            response.put("timestamp", config.getTimestamp());
            response.put("context", context);
            response.put("previousInterval", currentInterval);
            response.put("additionalInfo", config.getAdditionalInfo());
            response.put("message", "업데이트 간격 최적화 완료");
            
            logger.info("업데이트 간격 최적화 완료 - 컨텍스트: {}, 간격: {}ms → {}ms, 이유: {}", 
                context, currentInterval, config.getInterval(), config.getReason());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("업데이트 간격 최적화 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "업데이트 간격 최적화 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 업데이트 간격 유효성 검사
     * @param interval 검사할 간격
     * @return 유효성 검사 결과
     */
    @GetMapping("/interval/validate")
    public ResponseEntity<Map<String, Object>> validateInterval(@RequestParam int interval) {
        try {
            boolean isValid = RealtimeUpdateConfig.isValidInterval(interval);
            int normalizedInterval = RealtimeUpdateConfig.normalizeInterval(interval);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isValid", isValid);
            response.put("normalizedInterval", normalizedInterval);
            response.put("originalInterval", interval);
            response.put("message", isValid ? "유효한 간격입니다" : "간격이 정규화되었습니다");
            
            logger.debug("업데이트 간격 유효성 검사 - 간격: {}ms, 유효: {}, 정규화: {}ms", 
                interval, isValid, normalizedInterval);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("업데이트 간격 유효성 검사 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "업데이트 간격 유효성 검사 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 업데이트 통계 조회
     * @return 업데이트 통계 정보
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getUpdateStatistics() {
        try {
            Map<String, Object> statistics = RealtimeUpdateConfig.getUpdateStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);
            response.put("message", "업데이트 통계 조회 성공");
            
            logger.debug("업데이트 통계 조회 - 전체 업데이트: {}", statistics.get("totalUpdates"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("업데이트 통계 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "업데이트 통계 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 업데이트 간격별 성능 분석
     * @param intervals 분석할 간격 배열
     * @return 간격별 성능 분석 결과
     */
    @PostMapping("/interval/analyze")
    public ResponseEntity<Map<String, Object>> analyzeIntervalPerformance(@RequestBody int[] intervals) {
        try {
            Map<Integer, Map<String, Object>> analysis = 
                RealtimeUpdateConfig.analyzeIntervalPerformance(intervals);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("analysis", analysis);
            response.put("message", "간격별 성능 분석 완료");
            
            logger.info("간격별 성능 분석 완료 - 분석 간격 수: {}", intervals.length);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("간격별 성능 분석 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "간격별 성능 분석 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 업데이트 통계 리셋
     * @return 리셋 결과
     */
    @PostMapping("/statistics/reset")
    public ResponseEntity<Map<String, Object>> resetUpdateStatistics() {
        try {
            RealtimeUpdateConfig.resetUpdateStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "업데이트 통계 리셋 완료");
            
            logger.info("업데이트 통계 리셋 완료");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("업데이트 통계 리셋 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "업데이트 통계 리셋 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 동적 간격 조정 필요 여부 확인
     * @param context 업데이트 컨텍스트
     * @param performanceMetrics 성능 메트릭
     * @return 조정 필요 여부
     */
    @PostMapping("/interval/adjustment-needed")
    public ResponseEntity<Map<String, Object>> checkAdjustmentNeeded(
            @RequestParam String context,
            @RequestBody Map<String, Object> performanceMetrics) {
        try {
            // 성능 메트릭 파싱
            long totalUpdates = Long.parseLong(String.valueOf(performanceMetrics.getOrDefault("totalUpdates", 0)));
            long averageResponseTime = Long.parseLong(String.valueOf(performanceMetrics.getOrDefault("averageResponseTime", 0)));
            long maxResponseTime = Long.parseLong(String.valueOf(performanceMetrics.getOrDefault("maxResponseTime", 0)));
            long minResponseTime = Long.parseLong(String.valueOf(performanceMetrics.getOrDefault("minResponseTime", 0)));
            double errorRate = Double.parseDouble(String.valueOf(performanceMetrics.getOrDefault("errorRate", 0.0)));
            long lastUpdateTime = Long.parseLong(String.valueOf(performanceMetrics.getOrDefault("lastUpdateTime", System.currentTimeMillis())));
            
            // 성능 메트릭 객체 생성
            RealtimeUpdateConfig.UpdatePerformanceMetrics metrics = 
                new RealtimeUpdateConfig.UpdatePerformanceMetrics(
                    totalUpdates, averageResponseTime, maxResponseTime, 
                    minResponseTime, errorRate, lastUpdateTime
                );
            
            // 조정 필요 여부 확인
            boolean needsAdjustment = RealtimeUpdateConfig.needsIntervalAdjustment(context, metrics);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("needsAdjustment", needsAdjustment);
            response.put("context", context);
            response.put("performanceMetrics", performanceMetrics);
            response.put("message", needsAdjustment ? "간격 조정이 필요합니다" : "현재 간격을 유지하세요");
            
            logger.debug("동적 간격 조정 필요 여부 확인 - 컨텍스트: {}, 조정 필요: {}", 
                context, needsAdjustment);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("동적 간격 조정 필요 여부 확인 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "동적 간격 조정 필요 여부 확인 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
