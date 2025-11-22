package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.service.NotificationService;
import com.andrew.hnt.api.service.AdminService;
import com.andrew.hnt.api.service.LoginService;
import com.andrew.hnt.api.model.NotificationRequest;
import com.andrew.hnt.api.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트용 알람 컨트롤러
 * MQTT 알람 기능을 직접 테스트하기 위한 엔드포인트 제공
 */
@RestController
@RequestMapping("/test")
public class TestAlarmController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestAlarmController.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private LoginService loginService;
    
    /**
     * MQTT 알람 테스트 엔드포인트
     * GET /test/send-alarm?userId=thepine&sensorUuid=0008DC755397&message=테스트알람
     */
    @GetMapping("/send-alarm")
    public Map<String, Object> sendTestAlarm(
            @RequestParam(required = false, defaultValue = "thepine") String userId,
            @RequestParam(required = false, defaultValue = "0008DC755397") String sensorUuid,
            @RequestParam(required = false, defaultValue = "테스트 알람 메시지") String message
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 알람 요청 생성
            NotificationRequest request = new NotificationRequest();
            request.setUserId(userId);
            request.setSensorUuid(sensorUuid);
            request.setMessage(sensorUuid + "장치 이상 발생 : " + message);
            request.setFcmToken(null); // FCM 없이 MQTT만 사용
            
            // 이중화 알람 발송 (FCM 없으면 자동으로 MQTT 사용)
            notificationService.sendDualNotification(request);
            
            result.put("success", true);
            result.put("message", "MQTT 알람 발송 성공");
            result.put("userId", userId);
            result.put("sensorUuid", sensorUuid);
            result.put("alarmMessage", message);
        } catch(Exception e) {
            result.put("success", false);
            result.put("message", "MQTT 알람 발송 실패: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * FCM 토큰 확인 엔드포인트
     * GET /test/check-fcm-token?userId=thepine
     */
    @GetMapping("/check-fcm-token")
    public Map<String, Object> checkFcmToken(
            @RequestParam(required = false, defaultValue = "thepine") String userId
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("===============================================");
            logger.info("🔍 FCM 토큰 확인 시작 - userId: {}", userId);
            logger.info("===============================================");
            
            // 사용자 정보 조회
            UserInfo userInfo = loginService.getUserInfoByUserId(userId);
            
            if(userInfo != null) {
                String token = userInfo.getToken();
                
                result.put("success", true);
                result.put("userId", userId);
                result.put("hasToken", token != null && !token.isEmpty() && !"null".equals(token));
                result.put("tokenLength", token != null ? token.length() : 0);
                result.put("tokenPreview", token != null && token.length() > 20 ? token.substring(0, 20) + "..." : token);
                
                logger.info("✅ FCM 토큰 확인 완료");
                logger.info("   - 토큰 존재: {}", result.get("hasToken"));
                logger.info("   - 토큰 길이: {}", result.get("tokenLength"));
                logger.info("   - 토큰 미리보기: {}", result.get("tokenPreview"));
                logger.info("===============================================");
            } else {
                result.put("success", false);
                result.put("message", "사용자를 찾을 수 없습니다");
                logger.warn("❌ 사용자를 찾을 수 없음 - userId: {}", userId);
                logger.info("===============================================");
            }
        } catch(Exception e) {
            result.put("success", false);
            result.put("message", "FCM 토큰 확인 실패: " + e.getMessage());
            logger.error("❌ FCM 토큰 확인 중 오류 발생", e);
            logger.info("===============================================");
        }
        
        return result;
    }
    
    /**
     * 알람 설정 확인 엔드포인트
     * GET /test/check-alarm-settings?userId=thepine&sensorUuid=0008DC755397
     */
    @GetMapping("/check-alarm-settings")
    public Map<String, Object> checkAlarmSettings(
            @RequestParam(required = false, defaultValue = "thepine") String userId,
            @RequestParam(required = false, defaultValue = "0008DC755397") String sensorUuid
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("===============================================");
            logger.info("🔍 알람 설정 확인 시작");
            logger.info("   - userId: {}", userId);
            logger.info("   - sensorUuid: {}", sensorUuid);
            logger.info("===============================================");
            
            // 센서 설정 조회
            Map<String, Object> param = new HashMap<>();
            param.put("userId", userId);
            param.put("sensorId", userId);
            param.put("sensorUuid", sensorUuid);
            
            Map<String, Object> config = adminService.selectSetting(param);
            
            if(config != null && !config.isEmpty()) {
                result.put("success", true);
                result.put("userId", userId);
                result.put("sensorUuid", sensorUuid);
                
                // 알람 설정 정보 (실제 DB 컬럼명 사용)
                result.put("highAlarmYn", config.get("alarm_yn1"));
                result.put("highAlarmTemp", config.get("set_val1"));
                result.put("highAlarmTime", config.get("delay_time1"));
                result.put("lowAlarmYn", config.get("alarm_yn2"));
                result.put("lowAlarmTemp", config.get("set_val2"));
                result.put("lowAlarmTime", config.get("delay_time2"));
                result.put("specificAlarmYn", config.get("alarm_yn3"));
                result.put("specificAlarmTemp", config.get("set_val3"));
                result.put("specificAlarmTime", config.get("delay_time3"));
                result.put("diAlarmYn", config.get("alarm_yn4"));
                result.put("diAlarmTime", config.get("delay_time4"));
                result.put("networkAlarmYn", config.get("alarm_yn5"));
                result.put("networkAlarmTime", config.get("delay_time5"));
                
                logger.info("✅ 알람 설정 확인 완료");
                logger.info("   - 고온 알람: {} (온도: {}°C, 지연: {}분)", 
                    config.get("alarm_yn1"), 
                    config.get("set_val1"),
                    config.get("delay_time1"));
                logger.info("   - 저온 알람: {} (온도: {}°C, 지연: {}분)", 
                    config.get("alarm_yn2"), 
                    config.get("set_val2"),
                    config.get("delay_time2"));
                logger.info("   - 특정온도 알람: {} (온도: {}°C, 지연: {}분)", 
                    config.get("alarm_yn3"), 
                    config.get("set_val3"),
                    config.get("delay_time3"));
                logger.info("   - DI 알람: {} (지연: {}분)", 
                    config.get("alarm_yn4"),
                    config.get("delay_time4"));
                logger.info("   - 통신 알람: {} (지연: {}분)", 
                    config.get("alarm_yn5"),
                    config.get("delay_time5"));
                logger.info("===============================================");
            } else {
                result.put("success", false);
                result.put("message", "센서 설정을 찾을 수 없습니다");
                logger.warn("❌ 센서 설정을 찾을 수 없음");
                logger.info("===============================================");
            }
        } catch(Exception e) {
            result.put("success", false);
            result.put("message", "알람 설정 확인 실패: " + e.getMessage());
            logger.error("❌ 알람 설정 확인 중 오류 발생", e);
            logger.info("===============================================");
        }
        
        return result;
    }
    
    /**
     * 전체 진단 엔드포인트
     * GET /test/diagnose-alarm?userId=thepine&sensorUuid=0008DC755397
     */
    @GetMapping("/diagnose-alarm")
    public Map<String, Object> diagnoseAlarm(
            @RequestParam(required = false, defaultValue = "thepine") String userId,
            @RequestParam(required = false, defaultValue = "0008DC755397") String sensorUuid
    ) {
        Map<String, Object> result = new HashMap<>();
        
        logger.info("===============================================");
        logger.info("🔬 알람 시스템 전체 진단 시작");
        logger.info("   - userId: {}", userId);
        logger.info("   - sensorUuid: {}", sensorUuid);
        logger.info("===============================================");
        
        // 1. FCM 토큰 확인
        Map<String, Object> tokenCheck = checkFcmToken(userId);
        result.put("fcmTokenCheck", tokenCheck);
        
        // 2. 알람 설정 확인
        Map<String, Object> settingsCheck = checkAlarmSettings(userId, sensorUuid);
        result.put("alarmSettingsCheck", settingsCheck);
        
        // 3. 종합 진단 결과
        boolean fcmTokenOk = (boolean) tokenCheck.getOrDefault("hasToken", false);
        boolean settingsOk = (boolean) settingsCheck.getOrDefault("success", false);
        
        result.put("overallStatus", fcmTokenOk && settingsOk ? "정상" : "문제있음");
        result.put("fcmTokenStatus", fcmTokenOk ? "정상" : "토큰 없음");
        result.put("alarmSettingsStatus", settingsOk ? "정상" : "설정 없음");
        
        logger.info("===============================================");
        logger.info("📊 진단 결과 요약");
        logger.info("   - 전체 상태: {}", result.get("overallStatus"));
        logger.info("   - FCM 토큰: {}", result.get("fcmTokenStatus"));
        logger.info("   - 알람 설정: {}", result.get("alarmSettingsStatus"));
        logger.info("===============================================");
        
        return result;
    }
}

