package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.util.DataSyncMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * 프론트엔드-백엔드 데이터 동기화 모니터링 컨트롤러
 * HnT Sensor API 프로젝트 전용
 * 
 * 실시간 데이터 동기화 상태를 조회하고
 * 동기화 문제를 모니터링하는 API 제공
 */
@RestController
@RequestMapping("/api/sync")
public class DataSyncController {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSyncController.class);
    
    /**
     * 동기화 상태 조회
     * @param key 동기화 키
     * @return 동기화 상태 정보
     */
    @GetMapping("/status/{key}")
    public ResponseEntity<Map<String, Object>> getSyncStatus(@PathVariable String key) {
        try {
            DataSyncMonitor.SyncStatus status = DataSyncMonitor.getSyncStatus(key);
            
            Map<String, Object> response = new HashMap<>();
            if (status != null) {
                response.put("success", true);
                response.put("key", status.getKey());
                response.put("state", status.getState().name());
                response.put("stateDescription", status.getState().getDescription());
                response.put("lastSyncTime", status.getLastSyncTime());
                response.put("syncDuration", status.getSyncDuration());
                response.put("consecutiveFailures", status.getConsecutiveFailures());
                response.put("message", status.getMessage());
                response.put("metadata", status.getMetadata());
            } else {
                response.put("success", false);
                response.put("message", "동기화 상태를 찾을 수 없습니다");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("동기화 상태 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "동기화 상태 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 모든 동기화 상태 조회
     * @return 모든 동기화 상태 정보
     */
    @GetMapping("/status/all")
    public ResponseEntity<Map<String, Object>> getAllSyncStatuses() {
        try {
            Map<String, DataSyncMonitor.SyncStatus> statuses = DataSyncMonitor.getAllSyncStatuses();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", statuses.size());
            response.put("statuses", statuses);
            response.put("message", "모든 동기화 상태 조회 성공");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("모든 동기화 상태 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "모든 동기화 상태 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 동기화 통계 조회
     * @return 동기화 통계 정보
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getSyncStatistics() {
        try {
            Map<String, Object> statistics = DataSyncMonitor.getSyncStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);
            response.put("message", "동기화 통계 조회 성공");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("동기화 통계 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "동기화 통계 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 최근 동기화 실패 목록 조회
     * @param key 동기화 키
     * @param limit 조회 개수
     * @return 최근 실패 목록
     */
    @GetMapping("/failures/{key}")
    public ResponseEntity<Map<String, Object>> getRecentFailures(
            @PathVariable String key,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<DataSyncMonitor.SyncFailure> failures = 
                DataSyncMonitor.getRecentFailures(key, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("key", key);
            response.put("count", failures.size());
            response.put("failures", failures);
            response.put("message", "최근 동기화 실패 목록 조회 성공");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("최근 동기화 실패 목록 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "최근 동기화 실패 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 동기화 상태 건강 체크
     * @return 건강 상태 정보
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = DataSyncMonitor.healthCheck();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("health", health);
            response.put("message", "동기화 상태 건강 체크 완료");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("동기화 상태 건강 체크 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "동기화 상태 건강 체크 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 동기화 문제 감지
     * @return 감지된 문제 목록
     */
    @GetMapping("/issues")
    public ResponseEntity<Map<String, Object>> detectSyncIssues() {
        try {
            List<Map<String, Object>> issues = DataSyncMonitor.detectSyncIssues();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("issueCount", issues.size());
            response.put("issues", issues);
            response.put("message", "동기화 문제 감지 완료");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("동기화 문제 감지 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "동기화 문제 감지 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 동기화 시작 기록
     * @param key 동기화 키
     * @return 처리 결과
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startSync(@RequestParam String key) {
        try {
            DataSyncMonitor.startSync(key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("key", key);
            response.put("message", "동기화 시작 기록 완료");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("동기화 시작 기록 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "동기화 시작 기록 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 동기화 성공 기록
     * @param key 동기화 키
     * @param duration 동기화 소요 시간
     * @return 처리 결과
     */
    @PostMapping("/success")
    public ResponseEntity<Map<String, Object>> syncSuccess(
            @RequestParam String key,
            @RequestParam long duration) {
        try {
            DataSyncMonitor.syncSuccess(key, duration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("key", key);
            response.put("duration", duration);
            response.put("message", "동기화 성공 기록 완료");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("동기화 성공 기록 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "동기화 성공 기록 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 동기화 실패 기록
     * @param key 동기화 키
     * @param reason 실패 이유
     * @param details 실패 상세 정보
     * @return 처리 결과
     */
    @PostMapping("/failure")
    public ResponseEntity<Map<String, Object>> syncFailure(
            @RequestParam String key,
            @RequestParam String reason,
            @RequestParam(required = false) String details) {
        try {
            DataSyncMonitor.syncFailure(key, reason, details != null ? details : "");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("key", key);
            response.put("reason", reason);
            response.put("message", "동기화 실패 기록 완료");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("동기화 실패 기록 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "동기화 실패 기록 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 동기화 상태 리셋
     * @param key 동기화 키
     * @return 처리 결과
     */
    @DeleteMapping("/status/{key}")
    public ResponseEntity<Map<String, Object>> resetSyncStatus(@PathVariable String key) {
        try {
            DataSyncMonitor.resetSyncStatus(key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("key", key);
            response.put("message", "동기화 상태 리셋 완료");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("동기화 상태 리셋 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "동기화 상태 리셋 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 모든 동기화 상태 리셋
     * @return 처리 결과
     */
    @DeleteMapping("/status/all")
    public ResponseEntity<Map<String, Object>> resetAllSyncStatuses() {
        try {
            DataSyncMonitor.resetAllSyncStatuses();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "모든 동기화 상태 리셋 완료");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("모든 동기화 상태 리셋 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "모든 동기화 상태 리셋 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
