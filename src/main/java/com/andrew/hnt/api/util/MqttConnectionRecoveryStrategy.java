package com.andrew.hnt.api.util;

import com.andrew.hnt.api.mqtt.MqttConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * MQTT 연결 에러 복구 전략
 * MQTT 연결 실패 시 다양한 복구 방법을 시도
 */
@Component
public class MqttConnectionRecoveryStrategy implements ErrorRecoveryStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttConnectionRecoveryStrategy.class);
    
    @Autowired
    private MqttConnectionManager mqttConnectionManager;
    
    @Override
    public RecoveryResult attemptRecovery(ErrorContext errorContext) {
        logger.info("MQTT 연결 복구 시도 시작 - 재시도 횟수: {}", errorContext.getRetryCount());
        
        try {
            // 1단계: 연결 상태 확인
            if (mqttConnectionManager.isConnected()) {
                logger.info("MQTT 연결이 이미 복구됨");
                return new RecoveryResult(true, "MQTT 연결이 이미 복구되었습니다.");
            }
            
            // 2단계: 연결 재시도
            boolean reconnected = mqttConnectionManager.reconnect();
            if (reconnected) {
                logger.info("MQTT 연결 재시도 성공");
                return new RecoveryResult(true, "MQTT 연결 재시도 성공");
            }
            
            // 3단계: 연결 풀 재초기화
            if (errorContext.getRetryCount() >= 2) {
                logger.info("MQTT 연결 풀 재초기화 시도");
                mqttConnectionManager.reinitialize();
                
                boolean reconnectedAfterReinit = mqttConnectionManager.reconnect();
                if (reconnectedAfterReinit) {
                    logger.info("MQTT 연결 풀 재초기화 후 연결 성공");
                    return new RecoveryResult(true, "MQTT 연결 풀 재초기화 후 연결 성공");
                }
            }
            
            // 4단계: 백엔드 상태 확인
            if (errorContext.getRetryCount() >= 3) {
                logger.info("백엔드 MQTT 상태 확인 시도");
                Map<String, Object> backendStatus = mqttConnectionManager.getBackendStatus();
                
                if (backendStatus != null && "connected".equals(backendStatus.get("status"))) {
                    logger.info("백엔드 MQTT는 정상, 프론트엔드 연결만 재시도");
                    boolean frontendReconnected = mqttConnectionManager.reconnect();
                    if (frontendReconnected) {
                        return new RecoveryResult(true, "백엔드 정상 상태에서 프론트엔드 연결 성공");
                    }
                }
            }
            
            logger.warn("MQTT 연결 복구 실패 - 모든 전략 시도 완료");
            return new RecoveryResult(false, "MQTT 연결 복구에 실패했습니다.");
            
        } catch (Exception e) {
            logger.error("MQTT 연결 복구 중 예외 발생", e);
            return new RecoveryResult(false, "MQTT 연결 복구 중 예외 발생: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRecover(ErrorContext errorContext) {
        // MQTT 관련 에러만 복구 가능
        return "MQTT_CONNECTION_ERROR".equals(errorContext.getErrorType()) ||
               "NETWORK_CONNECTION_ERROR".equals(errorContext.getErrorType());
    }
    
    @Override
    public int getPriority() {
        return 1; // 높은 우선순위
    }
    
    @Override
    public ErrorType getErrorType() {
        return ErrorType.MQTT_CONNECTION_ERROR;
    }
}
