package com.andrew.hnt.api.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.andrew.hnt.api.service.AdminService;

/**
 * FCM 토큰 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/fcm")
public class FcmController {
    
    private static final Logger logger = LoggerFactory.getLogger(FcmController.class);
    
    @Autowired
    private AdminService adminService;
    
    /**
     * FCM 토큰 업데이트
     * 
     * @param userId 사용자 ID
     * @param token FCM 토큰
     * @param session HTTP 세션
     * @return 결과 맵
     */
    @PostMapping("/update-token")
    public Map<String, Object> updateFcmToken(
            @RequestParam("userId") String userId,
            @RequestParam("token") String token,
            HttpSession session) {
        
        Map<String, Object> resultMap = new HashMap<>();
        
        try {
            logger.info("=== FCM 토큰 업데이트 요청 ===");
            logger.info("userId: {}", userId);
            logger.info("token: {}...", token.substring(0, Math.min(20, token.length())));
            
            // 파라미터 검증
            if (userId == null || userId.trim().isEmpty()) {
                logger.warn("userId가 비어있음");
                resultMap.put("resultCode", "400");
                resultMap.put("resultMessage", "userId가 필요합니다.");
                return resultMap;
            }
            
            if (token == null || token.trim().isEmpty()) {
                logger.warn("token이 비어있음");
                resultMap.put("resultCode", "400");
                resultMap.put("resultMessage", "token이 필요합니다.");
                return resultMap;
            }
            
            // DB 업데이트 (hnt_user 테이블의 user_token 컬럼 또는 hnt_sensor_info 테이블의 token 컬럼)
            Map<String, Object> param = new HashMap<>();
            param.put("userId", userId);
            param.put("token", token);
            
            // hnt_sensor_info 테이블의 모든 센서에 대해 token 업데이트
            int updateCount = adminService.updateSensorTokenByUserId(param);
            
            logger.info("FCM 토큰 업데이트 완료 - userId: {}, updateCount: {}", userId, updateCount);
            
            resultMap.put("resultCode", "200");
            resultMap.put("resultMessage", "FCM 토큰 업데이트 성공");
            resultMap.put("updateCount", updateCount);
            
        } catch (Exception e) {
            logger.error("FCM 토큰 업데이트 중 오류 발생", e);
            resultMap.put("resultCode", "500");
            resultMap.put("resultMessage", "FCM 토큰 업데이트 실패: " + e.getMessage());
        }
        
        return resultMap;
    }
}

