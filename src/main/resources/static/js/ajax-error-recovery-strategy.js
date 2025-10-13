/**
 * AJAX 에러 복구 전략 (프론트엔드)
 * AJAX 요청 실패 시 다양한 복구 방법을 시도
 */
var AjaxErrorRecoveryStrategy = {
    name: 'AjaxErrorRecoveryStrategy',
    priority: 2,
    
    /**
     * 복구 시도
     * @param {Object} errorContext 에러 컨텍스트
     * @returns {Object} 복구 결과
     */
    attemptRecovery: function(errorContext) {
        console.log('AJAX 에러 복구 시도 시작 - 재시도 횟수:', errorContext.retryCount);
        
        try {
            // 1단계: 네트워크 상태 확인
            if (navigator.onLine === false) {
                console.log('오프라인 상태 - 네트워크 연결 대기');
                return {
                    success: false,
                    message: '오프라인 상태입니다. 네트워크 연결을 확인해주세요.'
                };
            }
            
            // 2단계: 세션 상태 확인
            if (errorContext.errorType === 'SESSION_EXPIRED_ERROR') {
                console.log('세션 만료 에러 - 로그인 페이지로 리다이렉트');
                setTimeout(function() {
                    window.location.href = '/login/login?timeout=true&reason=expired';
                }, 1000);
                
                return {
                    success: true,
                    message: '세션 만료로 인한 로그인 페이지 리다이렉트'
                };
            }
            
            // 3단계: AJAX 요청 재시도
            if (errorContext.originalRequest) {
                console.log('AJAX 요청 재시도');
                
                var originalRequest = errorContext.originalRequest;
                var retryDelay = Math.min(1000 * Math.pow(2, errorContext.retryCount), 10000); // 지수 백오프
                
                setTimeout(function() {
                    if (typeof UnifiedAjaxManager !== 'undefined') {
                        UnifiedAjaxManager.ajax(originalRequest);
                    } else if (typeof $.ajax === 'function') {
                        $.ajax(originalRequest);
                    }
                }, retryDelay);
                
                return {
                    success: true,
                    message: 'AJAX 요청 재시도 예약 (지연: ' + retryDelay + 'ms)'
                };
            }
            
            // 4단계: 페이지 새로고침 (마지막 수단)
            if (errorContext.retryCount >= 2) {
                console.log('AJAX 에러 복구를 위한 페이지 새로고침');
                setTimeout(function() {
                    window.location.reload();
                }, 2000);
                
                return {
                    success: true,
                    message: '페이지 새로고침으로 AJAX 에러 복구 시도'
                };
            }
            
            console.warn('AJAX 에러 복구 실패 - 모든 전략 시도 완료');
            return {
                success: false,
                message: 'AJAX 에러 복구에 실패했습니다.'
            };
            
        } catch (error) {
            console.error('AJAX 에러 복구 중 예외 발생:', error);
            return {
                success: false,
                message: 'AJAX 에러 복구 중 예외 발생: ' + error.message
            };
        }
    },
    
    /**
     * 복구 가능 여부 확인
     * @param {Object} errorContext 에러 컨텍스트
     * @returns {boolean} 복구 가능 여부
     */
    canRecover: function(errorContext) {
        // AJAX 관련 에러만 복구 가능
        return errorContext.errorType === 'AJAX_ERROR' ||
               errorContext.errorType === 'NETWORK_CONNECTION_ERROR' ||
               errorContext.errorType === 'SESSION_EXPIRED_ERROR';
    }
};
