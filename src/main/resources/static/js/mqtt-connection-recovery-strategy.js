/**
 * MQTT 연결 에러 복구 전략 (프론트엔드)
 * MQTT 연결 실패 시 다양한 복구 방법을 시도
 */
var MqttConnectionRecoveryStrategy = {
    name: 'MqttConnectionRecoveryStrategy',
    priority: 1,
    
    /**
     * 복구 시도
     * @param {Object} errorContext 에러 컨텍스트
     * @returns {Object} 복구 결과
     */
    attemptRecovery: function(errorContext) {
        console.log('MQTT 연결 복구 시도 시작 - 재시도 횟수:', errorContext.retryCount);
        
        try {
            // 1단계: 연결 상태 확인
            if (typeof UnifiedMQTTManager !== 'undefined' && UnifiedMQTTManager.isConnected()) {
                console.log('MQTT 연결이 이미 복구됨');
                return {
                    success: true,
                    message: 'MQTT 연결이 이미 복구되었습니다.'
                };
            }
            
            // 2단계: 연결 재시도
            if (typeof UnifiedMQTTManager !== 'undefined') {
                console.log('MQTT 연결 재시도');
                UnifiedMQTTManager.connect();
                
                // 연결 상태 확인 (2초 후)
                setTimeout(function() {
                    if (UnifiedMQTTManager.isConnected()) {
                        console.log('MQTT 연결 재시도 성공');
                        return {
                            success: true,
                            message: 'MQTT 연결 재시도 성공'
                        };
                    }
                }, 2000);
            }
            
            // 3단계: 페이지 새로고침 (마지막 수단)
            if (errorContext.retryCount >= 2) {
                console.log('MQTT 연결 복구를 위한 페이지 새로고침');
                setTimeout(function() {
                    window.location.reload();
                }, 3000);
                
                return {
                    success: true,
                    message: '페이지 새로고침으로 MQTT 연결 복구 시도'
                };
            }
            
            console.warn('MQTT 연결 복구 실패 - 모든 전략 시도 완료');
            return {
                success: false,
                message: 'MQTT 연결 복구에 실패했습니다.'
            };
            
        } catch (error) {
            console.error('MQTT 연결 복구 중 예외 발생:', error);
            return {
                success: false,
                message: 'MQTT 연결 복구 중 예외 발생: ' + error.message
            };
        }
    },
    
    /**
     * 복구 가능 여부 확인
     * @param {Object} errorContext 에러 컨텍스트
     * @returns {boolean} 복구 가능 여부
     */
    canRecover: function(errorContext) {
        // MQTT 관련 에러만 복구 가능
        return errorContext.errorType === 'MQTT_CONNECTION_ERROR' ||
               errorContext.errorType === 'NETWORK_CONNECTION_ERROR';
    }
};
