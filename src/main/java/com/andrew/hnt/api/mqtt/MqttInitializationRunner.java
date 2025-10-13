package com.andrew.hnt.api.mqtt;

import com.andrew.hnt.api.config.MqttConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * MQTT 초기화 실행자
 * Spring Boot 애플리케이션 시작 시 MQTT 연결을 가장 먼저 초기화
 * 
 * 주요 기능:
 * - 애플리케이션 시작 시 즉시 MQTT 연결 초기화
 * - 프론트엔드보다 먼저 백엔드 MQTT 연결 보장
 * - 초기화 상태를 전역적으로 관리
 */
@Component
@Order(1) // 가장 높은 우선순위로 실행
public class MqttInitializationRunner implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttInitializationRunner.class);
    
    @Autowired
    private MqttConnectionManager mqttConnectionManager;
    
    @Autowired
    private MqttConfig mqttConfig;
    
    private static volatile boolean isBackendMqttReady = false;
    private static volatile long initializationStartTime = 0;
    private static volatile long initializationEndTime = 0;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("=== MQTT 초기화 실행자 시작 ===");
        initializationStartTime = System.currentTimeMillis();
        
        try {
            // 1. MQTT 설정 검증
            validateMqttConfig();
            
            // 2. 백엔드 MQTT 연결 초기화
            initializeBackendMqtt();
            
            // 3. 초기화 완료 상태 설정
            setBackendMqttReady(true);
            initializationEndTime = System.currentTimeMillis();
            
            long initDuration = initializationEndTime - initializationStartTime;
            logger.info("=== MQTT 백엔드 초기화 완료 - 소요시간: {}ms ===", initDuration);
            
        } catch (Exception e) {
            logger.error("MQTT 백엔드 초기화 실패 - 백그라운드에서 재연결 시도", e);
            setBackendMqttReady(false);
            // throw e; 제거 - 비치명적 처리 (MQTT 연결 실패해도 애플리케이션은 정상 시작)
        }
    }
    
    /**
     * MQTT 설정 검증
     */
    private void validateMqttConfig() {
        logger.info("MQTT 설정 검증 시작");
        
        if (mqttConfig == null) {
            throw new IllegalStateException("MQTT 설정 객체가 null입니다");
        }
        
        if (mqttConfig.getServer() == null || mqttConfig.getServer().trim().isEmpty()) {
            throw new IllegalStateException("MQTT 서버 주소가 설정되지 않았습니다");
        }
        
        if (mqttConfig.getUsername() == null || mqttConfig.getUsername().trim().isEmpty()) {
            throw new IllegalStateException("MQTT 사용자명이 설정되지 않았습니다");
        }
        
        if (mqttConfig.getPassword() == null || mqttConfig.getPassword().trim().isEmpty()) {
            throw new IllegalStateException("MQTT 비밀번호가 설정되지 않았습니다");
        }
        
        logger.info("MQTT 설정 검증 완료 - 서버: {}, 사용자명: {}", 
            mqttConfig.getServer(), mqttConfig.getUsername());
    }
    
    /**
     * 백엔드 MQTT 연결 초기화
     */
    private void initializeBackendMqtt() {
        logger.info("백엔드 MQTT 연결 초기화 시작");
        
        try {
            // 1. MqttConnectionManager 초기화
            mqttConnectionManager.initialize();
            
            // 2. MQTT 연결 초기화
            mqttConnectionManager.initializeConnection();
            
            // 3. 연결 상태 확인
            if (mqttConnectionManager.isConnected()) {
                logger.info("백엔드 MQTT 연결 성공");
            } else {
                logger.warn("백엔드 MQTT 초기 연결 실패 - 백그라운드에서 재시도");
                // 예외를 던지지 않고 백그라운드 재연결에 맡김
            }
            
        } catch (Exception e) {
            logger.error("백엔드 MQTT 연결 초기화 중 오류 발생 - 백그라운드에서 재시도", e);
            // 예외를 던지지 않음
        }
    }
    
    /**
     * 백엔드 MQTT 준비 상태 설정
     */
    private static void setBackendMqttReady(boolean ready) {
        isBackendMqttReady = ready;
        logger.info("백엔드 MQTT 준비 상태: {}", ready ? "준비됨" : "준비 안됨");
    }
    
    /**
     * 백엔드 MQTT 준비 상태 확인
     */
    public static boolean isBackendMqttReady() {
        return isBackendMqttReady;
    }
    
    /**
     * 초기화 소요 시간 조회
     */
    public static long getInitializationDuration() {
        if (initializationStartTime == 0) {
            return 0;
        }
        
        long endTime = initializationEndTime > 0 ? initializationEndTime : System.currentTimeMillis();
        return endTime - initializationStartTime;
    }
    
    /**
     * 초기화 상태 정보 조회
     */
    public static String getInitializationStatus() {
        if (initializationStartTime == 0) {
            return "초기화 시작 안됨";
        }
        
        if (initializationEndTime == 0) {
            long currentDuration = System.currentTimeMillis() - initializationStartTime;
            return String.format("초기화 진행 중 (소요시간: %dms)", currentDuration);
        }
        
        long duration = initializationEndTime - initializationStartTime;
        return String.format("초기화 완료 (소요시간: %dms, 상태: %s)", 
            duration, isBackendMqttReady ? "성공" : "실패");
    }
}
