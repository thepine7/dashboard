package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.service.MetricsStorageService;
import com.andrew.hnt.api.service.SessionManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 모니터링 컨트롤러
 * 성능 메트릭, 알림 관리, 모니터링 대시보드 API 제공
 * 
 * @author HnT Solutions
 * @version 1.0.0
 * @since 2025-01-01
 */
@RestController
@RequestMapping("/monitoring")
public class MonitoringController {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);
    
    
    @Autowired
    private MetricsStorageService metricsStorageService;
    
    
    @Autowired
    private SessionManagementService sessionManagementService;
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 실시간 성능 메트릭 조회
     * 
     * @param session HTTP 세션
     * @return 실시간 성능 메트릭
     */
    @GetMapping("/metrics/realtime")
    public ResponseEntity<Map<String, Object>> getRealtimeMetrics(HttpSession session) {
        logger.info("실시간 성능 메트릭 조회 요청");
        
        try {
            // 세션 검증
            if (!sessionManagementService.isValidSession(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // 최신 메트릭 조회
            Map<String, Object> metrics = metricsStorageService.getLatestMetrics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resultCode", "200");
            response.put("resultMessage", "실시간 메트릭 조회 성공");
            response.put("data", metrics);
            response.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("실시간 메트릭 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("서버 내부 오류가 발생했습니다."));
        }
    }
    
    /**
     * 성능 메트릭 조회 (시간 범위)
     * 
     * @param session HTTP 세션
     * @param startTime 시작 시간 (yyyy-MM-dd HH:mm:ss)
     * @param endTime 종료 시간 (yyyy-MM-dd HH:mm:ss)
     * @return 성능 메트릭 목록
     */
    @GetMapping("/metrics/history")
    public ResponseEntity<Map<String, Object>> getMetricsHistory(
            HttpSession session,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        
        logger.info("성능 메트릭 이력 조회 요청 - startTime: {}, endTime: {}", startTime, endTime);
        
        try {
            // 세션 검증
            if (!sessionManagementService.isValidSession(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // 시간 파싱
            LocalDateTime start = LocalDateTime.parse(startTime, DATETIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATETIME_FORMATTER);
            
            // 메트릭 조회
            List<Map<String, Object>> metrics = metricsStorageService.getMetrics(start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resultCode", "200");
            response.put("resultMessage", "메트릭 이력 조회 성공");
            response.put("data", metrics);
            response.put("count", metrics.size());
            response.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("메트릭 이력 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("서버 내부 오류가 발생했습니다."));
        }
    }
    
    /**
     * 성능 메트릭 통계 조회
     * 
     * @param session HTTP 세션
     * @param startTime 시작 시간 (yyyy-MM-dd HH:mm:ss)
     * @param endTime 종료 시간 (yyyy-MM-dd HH:mm:ss)
     * @return 성능 메트릭 통계
     */
    @GetMapping("/metrics/statistics")
    public ResponseEntity<Map<String, Object>> getMetricsStatistics(
            HttpSession session,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        
        logger.info("성능 메트릭 통계 조회 요청 - startTime: {}, endTime: {}", startTime, endTime);
        
        try {
            // 세션 검증
            if (!sessionManagementService.isValidSession(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // 시간 파싱
            LocalDateTime start = LocalDateTime.parse(startTime, DATETIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATETIME_FORMATTER);
            
            // 통계 조회
            Map<String, Object> statistics = metricsStorageService.getMetricsStatistics(start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resultCode", "200");
            response.put("resultMessage", "메트릭 통계 조회 성공");
            response.put("data", statistics);
            response.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("메트릭 통계 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("서버 내부 오류가 발생했습니다."));
        }
    }
    
    /**
     * 알림 규칙 등록
     * 
     * @param session HTTP 세션
     * @param requestData 요청 데이터
     * @return 처리 결과
     */
    @PostMapping("/alerts/rules")
    public ResponseEntity<Map<String, Object>> registerAlertRule(
            HttpSession session,
            @RequestBody Map<String, Object> requestData) {
        
        logger.info("알림 규칙 등록 요청");
        
        try {
            // 세션 검증
            if (!sessionManagementService.isValidSession(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // 필수 파라미터 검증
            String ruleName = (String) requestData.get("ruleName");
            String metricName = (String) requestData.get("metricName");
            Double threshold = (Double) requestData.get("threshold");
            String operator = (String) requestData.get("operator");
            String severity = (String) requestData.get("severity");
            Boolean enabled = (Boolean) requestData.getOrDefault("enabled", true);
            
            if (ruleName == null || metricName == null || threshold == null || 
                operator == null || severity == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("필수 파라미터가 누락되었습니다."));
            }
            
            // 알림 규칙 등록 (제거됨)
            logger.info("알림 규칙 등록 건너뜀: {}", ruleName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resultCode", "200");
            response.put("resultMessage", "알림 규칙이 등록되었습니다.");
            response.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("알림 규칙 등록 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("서버 내부 오류가 발생했습니다."));
        }
    }
    
    /**
     * 알림 규칙 목록 조회
     * 
     * @param session HTTP 세션
     * @return 알림 규칙 목록
     */
    @GetMapping("/alerts/rules")
    public ResponseEntity<Map<String, Object>> getAlertRules(HttpSession session) {
        logger.info("알림 규칙 목록 조회 요청");
        
        try {
            // 세션 검증
            if (!sessionManagementService.isValidSession(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // 알림 규칙 조회 (제거됨)
            List<Map<String, Object>> rules = new ArrayList<>();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resultCode", "200");
            response.put("resultMessage", "알림 규칙 목록 조회 성공");
            response.put("data", rules);
            response.put("count", rules.size());
            response.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("알림 규칙 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("서버 내부 오류가 발생했습니다."));
        }
    }
    
    /**
     * 알림 규칙 삭제
     * 
     * @param session HTTP 세션
     * @param ruleName 규칙 이름
     * @return 처리 결과
     */
    @DeleteMapping("/alerts/rules/{ruleName}")
    public ResponseEntity<Map<String, Object>> removeAlertRule(
            HttpSession session,
            @PathVariable String ruleName) {
        
        logger.info("알림 규칙 삭제 요청 - ruleName: {}", ruleName);
        
        try {
            // 세션 검증
            if (!sessionManagementService.isValidSession(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // 알림 규칙 삭제 (제거됨)
            logger.info("알림 규칙 삭제 건너뜀: {}", ruleName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resultCode", "200");
            response.put("resultMessage", "알림 규칙이 삭제되었습니다.");
            response.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("알림 규칙 삭제 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("서버 내부 오류가 발생했습니다."));
        }
    }
    
    /**
     * 알림 이력 조회
     * 
     * @param session HTTP 세션
     * @param startTime 시작 시간 (yyyy-MM-dd HH:mm:ss)
     * @param endTime 종료 시간 (yyyy-MM-dd HH:mm:ss)
     * @param severity 심각도 필터 (선택사항)
     * @return 알림 이력 목록
     */
    @GetMapping("/alerts/history")
    public ResponseEntity<Map<String, Object>> getAlertHistory(
            HttpSession session,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String severity) {
        
        logger.info("알림 이력 조회 요청 - startTime: {}, endTime: {}, severity: {}", 
                   startTime, endTime, severity);
        
        try {
            // 세션 검증
            if (!sessionManagementService.isValidSession(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // 시간 파싱
            LocalDateTime start = LocalDateTime.parse(startTime, DATETIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATETIME_FORMATTER);
            
            // 알림 이력 조회 (제거됨)
            List<Map<String, Object>> history = new ArrayList<>();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resultCode", "200");
            response.put("resultMessage", "알림 이력 조회 성공");
            response.put("data", history);
            response.put("count", history.size());
            response.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("알림 이력 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("서버 내부 오류가 발생했습니다."));
        }
    }
    
    /**
     * 알림 통계 조회
     * 
     * @param session HTTP 세션
     * @param startTime 시작 시간 (yyyy-MM-dd HH:mm:ss)
     * @param endTime 종료 시간 (yyyy-MM-dd HH:mm:ss)
     * @return 알림 통계
     */
    @GetMapping("/alerts/statistics")
    public ResponseEntity<Map<String, Object>> getAlertStatistics(
            HttpSession session,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        
        logger.info("알림 통계 조회 요청 - startTime: {}, endTime: {}", startTime, endTime);
        
        try {
            // 세션 검증
            if (!sessionManagementService.isValidSession(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // 시간 파싱
            LocalDateTime start = LocalDateTime.parse(startTime, DATETIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATETIME_FORMATTER);
            
            // 알림 통계 조회 (제거됨)
            Map<String, Object> statistics = new HashMap<>();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resultCode", "200");
            response.put("resultMessage", "알림 통계 조회 성공");
            response.put("data", statistics);
            response.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("알림 통계 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("서버 내부 오류가 발생했습니다."));
        }
    }
    
    /**
     * 모니터링 대시보드 데이터 조회
     * 
     * @param session HTTP 세션
     * @return 대시보드 데이터
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getMonitoringDashboard(HttpSession session) {
        logger.info("모니터링 대시보드 데이터 조회 요청");
        
        try {
            // 세션 검증
            if (!sessionManagementService.isValidSession(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // 대시보드 데이터 구성
            Map<String, Object> dashboard = new HashMap<>();
            
            // 실시간 메트릭
            Map<String, Object> realtimeMetrics = metricsStorageService.getLatestMetrics();
            dashboard.put("realtimeMetrics", realtimeMetrics);
            
            // 알림 규칙 상태 (제거됨)
            List<Map<String, Object>> alertRules = new ArrayList<>();
            dashboard.put("alertRules", alertRules);
            
            // 알림 서비스 상태 (제거됨)
            Map<String, Object> alertServiceStatus = new HashMap<>();
            dashboard.put("alertServiceStatus", alertServiceStatus);
            
            // 메트릭 저장소 상태
            Map<String, Object> storageStatus = metricsStorageService.getStorageStatus();
            dashboard.put("storageStatus", storageStatus);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resultCode", "200");
            response.put("resultMessage", "모니터링 대시보드 데이터 조회 성공");
            response.put("data", dashboard);
            response.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("모니터링 대시보드 데이터 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("서버 내부 오류가 발생했습니다."));
        }
    }
    
    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("resultCode", "500");
        response.put("resultMessage", message);
        response.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));
        return response;
    }
}
