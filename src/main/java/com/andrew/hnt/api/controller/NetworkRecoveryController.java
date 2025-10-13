package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.util.NetworkRecoveryManager;
import com.andrew.hnt.api.util.NetworkRecoveryManager.NetworkState;
import com.andrew.hnt.api.util.NetworkRecoveryManager.RecoveryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 네트워크 복구 관리 REST API 컨트롤러
 * HnT Sensor API 프로젝트 전용
 * 
 * 네트워크 상태 모니터링 및 복구 관리 API 제공
 */
@RestController
@RequestMapping("/api/network")
public class NetworkRecoveryController {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkRecoveryController.class);
    
    /**
     * 네트워크 상태 조회
     * @param key 네트워크 키
     * @return 네트워크 상태
     */
    @GetMapping("/status/{key}")
    public ResponseEntity<Map<String, Object>> getNetworkStatus(@PathVariable String key) {
        try {
            NetworkState state = NetworkRecoveryManager.getNetworkState(key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("key", key);
            
            if (state != null) {
                Map<String, Object> stateData = new HashMap<>();
                stateData.put("status", state.getStatus().name());
                stateData.put("statusDescription", state.getStatus().getDescription());
                stateData.put("lastOnlineTime", state.getLastOnlineTime());
                stateData.put("lastOfflineTime", state.getLastOfflineTime());
                stateData.put("disconnectionDuration", state.getDisconnectionDuration());
                stateData.put("recoveryRetryCount", state.getRecoveryRetryCount());
                stateData.put("syncPending", state.isSyncPending());
                stateData.put("metadata", state.getMetadata());
                
                response.put("state", stateData);
            } else {
                response.put("state", null);
                response.put("message", "네트워크 상태 정보가 없습니다.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("네트워크 상태 조회 실패: " + key, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "네트워크 상태 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 모든 네트워크 상태 조회
     * @return 모든 네트워크 상태
     */
    @GetMapping("/status/all")
    public ResponseEntity<Map<String, Object>> getAllNetworkStatuses() {
        try {
            Map<String, NetworkState> states = NetworkRecoveryManager.getAllNetworkStates();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", states.size());
            
            Map<String, Map<String, Object>> statesData = new HashMap<>();
            states.forEach((key, state) -> {
                Map<String, Object> stateData = new HashMap<>();
                stateData.put("status", state.getStatus().name());
                stateData.put("statusDescription", state.getStatus().getDescription());
                stateData.put("lastOnlineTime", state.getLastOnlineTime());
                stateData.put("lastOfflineTime", state.getLastOfflineTime());
                stateData.put("disconnectionDuration", state.getDisconnectionDuration());
                stateData.put("recoveryRetryCount", state.getRecoveryRetryCount());
                stateData.put("syncPending", state.isSyncPending());
                statesData.put(key, stateData);
            });
            
            response.put("states", statesData);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("모든 네트워크 상태 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "모든 네트워크 상태 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 네트워크 복구 통계 조회
     * @return 복구 통계
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getRecoveryStatistics() {
        try {
            Map<String, Object> stats = NetworkRecoveryManager.getRecoveryStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("복구 통계 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "복구 통계 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 복구 이벤트 조회
     * @param key 네트워크 키
     * @param limit 조회 개수 (기본값: 20)
     * @return 복구 이벤트 목록
     */
    @GetMapping("/events/{key}")
    public ResponseEntity<Map<String, Object>> getRecoveryEvents(
            @PathVariable String key,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<RecoveryEvent> events = NetworkRecoveryManager.getRecoveryEvents(key, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("key", key);
            response.put("count", events.size());
            response.put("limit", limit);
            response.put("events", events);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("복구 이벤트 조회 실패: " + key, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "복구 이벤트 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 네트워크 상태 요약
     * @return 네트워크 상태 요약
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getNetworkSummary() {
        try {
            Map<String, Object> summary = NetworkRecoveryManager.getNetworkSummary();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summary", summary);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("네트워크 상태 요약 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "네트워크 상태 요약 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 네트워크 온라인 전환
     * @param key 네트워크 키
     * @return 처리 결과
     */
    @PostMapping("/online/{key}")
    public ResponseEntity<Map<String, Object>> markOnline(@PathVariable String key) {
        try {
            NetworkRecoveryManager.markOnline(key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("key", key);
            response.put("message", "네트워크 온라인 전환 완료");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("네트워크 온라인 전환: {}", key);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("네트워크 온라인 전환 실패: " + key, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "네트워크 온라인 전환 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 네트워크 오프라인 전환
     * @param key 네트워크 키
     * @return 처리 결과
     */
    @PostMapping("/offline/{key}")
    public ResponseEntity<Map<String, Object>> markOffline(@PathVariable String key) {
        try {
            NetworkRecoveryManager.markOffline(key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("key", key);
            response.put("message", "네트워크 오프라인 전환 완료");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.warn("네트워크 오프라인 전환: {}", key);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("네트워크 오프라인 전환 실패: " + key, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "네트워크 오프라인 전환 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 동기화 재시작 시도
     * @param key 네트워크 키
     * @return 처리 결과
     */
    @PostMapping("/restart-sync/{key}")
    public ResponseEntity<Map<String, Object>> attemptSyncRestart(@PathVariable String key) {
        try {
            boolean success = NetworkRecoveryManager.attemptSyncRestart(key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("key", key);
            response.put("message", success ? 
                "동기화 재시작 성공" : "동기화 재시작 실패");
            response.put("timestamp", System.currentTimeMillis());
            
            if (success) {
                logger.info("동기화 재시작 성공: {}", key);
            } else {
                logger.warn("동기화 재시작 실패: {}", key);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("동기화 재시작 시도 실패: " + key, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "동기화 재시작 시도 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 네트워크 상태 리셋
     * @param key 네트워크 키
     * @return 처리 결과
     */
    @DeleteMapping("/status/{key}")
    public ResponseEntity<Map<String, Object>> resetNetworkState(@PathVariable String key) {
        try {
            NetworkRecoveryManager.resetNetworkState(key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("key", key);
            response.put("message", "네트워크 상태 리셋 완료");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("네트워크 상태 리셋: {}", key);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("네트워크 상태 리셋 실패: " + key, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "네트워크 상태 리셋 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 모든 네트워크 상태 리셋
     * @return 처리 결과
     */
    @DeleteMapping("/status/all")
    public ResponseEntity<Map<String, Object>> resetAllNetworkStates() {
        try {
            NetworkRecoveryManager.resetAllNetworkStates();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "모든 네트워크 상태 리셋 완료");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("모든 네트워크 상태 리셋 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("모든 네트워크 상태 리셋 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "모든 네트워크 상태 리셋 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
