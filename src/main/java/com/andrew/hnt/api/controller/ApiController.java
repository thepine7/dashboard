package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.mqtt.MqttInitializationRunner;
import com.andrew.hnt.api.mqtt.MqttConnectionManager;
import com.andrew.hnt.api.util.UnifiedErrorHandler;
import com.andrew.hnt.api.service.UnifiedSessionService;
import com.andrew.hnt.api.service.UnifiedSessionService.SessionValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * API 컨트롤러
 * REST API 엔드포인트 제공
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    
    @Autowired
    private MqttInitializationRunner mqttInitializationRunner;
    
    @Autowired
    private MqttConnectionManager mqttConnectionManager;
    
    @Autowired
    private UnifiedErrorHandler unifiedErrorHandler;
    
    @Autowired
    private UnifiedSessionService unifiedSessionService;
    
    /**
     * MQTT 상태 확인 API
     * @param req HTTP 요청
     * @param res HTTP 응답
     * @return MQTT 연결 상태 정보
     */
    @RequestMapping(value = "/mqtt/status", method = RequestMethod.GET)
    public Map<String, Object> getMqttStatus(
            HttpServletRequest req,
            HttpServletResponse res
    ) {
        Map<String, Object> resultMap = new HashMap<>();
        
        try {
            logger.info("=== MQTT 상태 확인 API 호출 ===");
            
            // 세션 검증 (API 접근 권한 확인)
            HttpSession session = req.getSession();
            SessionValidationResult validationResult = unifiedSessionService.validateSession(session, req, "B");
            
            if (!validationResult.isValid()) {
                logger.warn("MQTT 상태 확인 API - 세션 검증 실패: {}", validationResult.getErrorMessage());
                resultMap.put("success", false);
                resultMap.put("resultCode", "401");
                resultMap.put("resultMessage", "인증이 필요합니다.");
                resultMap.put("timestamp", System.currentTimeMillis());
                return resultMap;
            }
            
            // MQTT 연결 상태 확인
            boolean isConnected = mqttConnectionManager.isConnected();
            boolean isBackendReady = MqttInitializationRunner.isBackendMqttReady();
            long initializationDuration = MqttInitializationRunner.getInitializationDuration();
            String initializationStatus = MqttInitializationRunner.getInitializationStatus();
            
            // 응답 데이터 구성
            resultMap.put("success", true);
            resultMap.put("connected", isConnected);
            resultMap.put("backendReady", isBackendReady);
            resultMap.put("initializationDuration", initializationDuration);
            resultMap.put("initializationStatus", initializationStatus);
            resultMap.put("timestamp", System.currentTimeMillis());
            
            // 상태 메시지
            if (isConnected && isBackendReady) {
                resultMap.put("message", "MQTT 연결 정상");
            } else if (isBackendReady && !isConnected) {
                resultMap.put("message", "백엔드 준비 완료, MQTT 연결 시도 중");
            } else {
                resultMap.put("message", "MQTT 초기화 중 또는 연결 실패");
            }
            
            logger.info("MQTT 상태 확인 완료 - 연결: {}, 백엔드준비: {}, 초기화시간: {}ms", 
                       isConnected, isBackendReady, initializationDuration);
            
        } catch (Exception e) {
            logger.error("MQTT 상태 확인 중 오류 발생", e);
            unifiedErrorHandler.logError("MQTT 상태 확인", e);
            
            resultMap.put("success", false);
            resultMap.put("connected", false);
            resultMap.put("backendReady", false);
            resultMap.put("message", "MQTT 상태 확인 실패: " + e.getMessage());
            resultMap.put("error", e.getMessage());
            resultMap.put("timestamp", System.currentTimeMillis());
        }
        
        return resultMap;
    }
    
    /**
     * 시스템 헬스 체크 API
     * @param req HTTP 요청
     * @param res HTTP 응답
     * @return 시스템 상태 정보
     */
    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public Map<String, Object> getHealthStatus(
            HttpServletRequest req,
            HttpServletResponse res
    ) {
        Map<String, Object> resultMap = new HashMap<>();
        
        try {
            logger.info("=== 시스템 헬스 체크 API 호출 ===");
            
            // 세션 검증 (API 접근 권한 확인)
            HttpSession session = req.getSession();
            SessionValidationResult validationResult = unifiedSessionService.validateSession(session, req, "B");
            
            if (!validationResult.isValid()) {
                logger.warn("시스템 헬스 체크 API - 세션 검증 실패: {}", validationResult.getErrorMessage());
                resultMap.put("status", "UNAUTHORIZED");
                resultMap.put("resultCode", "401");
                resultMap.put("resultMessage", "인증이 필요합니다.");
                resultMap.put("timestamp", System.currentTimeMillis());
                return resultMap;
            }
            
            // MQTT 상태 확인
            boolean mqttConnected = mqttConnectionManager.isConnected();
            boolean mqttBackendReady = MqttInitializationRunner.isBackendMqttReady();
            
            // 전체 시스템 상태 판단
            boolean systemHealthy = mqttConnected && mqttBackendReady;
            
            resultMap.put("status", systemHealthy ? "UP" : "DOWN");
            resultMap.put("timestamp", System.currentTimeMillis());
            
            // Java 8 호환: Map.of 대신 HashMap 사용
            Map<String, Object> mqttComponent = new HashMap<>();
            mqttComponent.put("status", mqttConnected ? "UP" : "DOWN");
            mqttComponent.put("backendReady", mqttBackendReady);
            mqttComponent.put("initializationDuration", MqttInitializationRunner.getInitializationDuration());
            
            Map<String, Object> components = new HashMap<>();
            components.put("mqtt", mqttComponent);
            
            resultMap.put("components", components);
            
            logger.info("시스템 헬스 체크 완료 - 상태: {}, MQTT: {}", 
                       systemHealthy ? "UP" : "DOWN", mqttConnected ? "UP" : "DOWN");
            
        } catch (Exception e) {
            logger.error("시스템 헬스 체크 중 오류 발생", e);
            unifiedErrorHandler.logError("시스템 헬스 체크", e);
            
            resultMap.put("status", "DOWN");
            resultMap.put("timestamp", System.currentTimeMillis());
            resultMap.put("error", e.getMessage());
        }
        
        return resultMap;
    }
    
    /**
     * 세션 관리 상태 확인 API
     * @param req HTTP 요청
     * @param res HTTP 응답
     * @return 세션 관리 상태 정보
     */
    @RequestMapping(value = "/session/status", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getSessionStatus(
            HttpServletRequest req
            , HttpServletResponse res
            ) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        
        try {
            // 세션 검증 (API 접근 권한 확인)
            HttpSession session = req.getSession();
            SessionValidationResult validationResult = unifiedSessionService.validateSession(session, req, "A");
            
            if (!validationResult.isValid()) {
                logger.warn("세션 상태 확인 API - 세션 검증 실패: {}", validationResult.getErrorMessage());
                resultMap.put("success", false);
                resultMap.put("resultCode", "401");
                resultMap.put("resultMessage", "관리자 권한이 필요합니다.");
                resultMap.put("timestamp", System.currentTimeMillis());
                return resultMap;
            }
            
            // 세션 통계 조회
            Map<String, Object> sessionStats = unifiedSessionService.getSessionStats();
            Map<String, Object> validationStats = unifiedSessionService.getValidationStats();
            
            resultMap.put("success", true);
            resultMap.put("resultCode", "200");
            resultMap.put("resultMessage", "세션 관리 상태 조회 성공");
            resultMap.put("timestamp", System.currentTimeMillis());
            resultMap.put("sessionStats", sessionStats);
            resultMap.put("validationStats", validationStats);
            
            logger.info("세션 관리 상태 조회 완료 - 통계: {}", sessionStats);
            
        } catch (Exception e) {
            logger.error("세션 관리 상태 조회 실패", e);
            resultMap.put("success", false);
            resultMap.put("resultCode", "500");
            resultMap.put("resultMessage", "세션 관리 상태 조회 실패: " + e.getMessage());
            resultMap.put("timestamp", System.currentTimeMillis());
        }
        
        return resultMap;
    }
}