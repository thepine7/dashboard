/**
 * 프론트엔드 에러 복구 관리자
 * 클라이언트 측 에러 발생 시 통합된 복구 전략을 제공
 */
var FrontendErrorRecoveryManager = (function() {
    'use strict';
    
    // 에러 복구 전략 맵
    var recoveryStrategies = {};
    
    // 복구 시도 기록
    var recoveryAttempts = {};
    
    // 통계 정보
    var stats = {
        totalRecoveryAttempts: 0,
        successfulRecoveries: 0,
        failedRecoveries: 0,
        lastRecoveryTime: 0
    };
    
    // 최대 재시도 횟수
    var MAX_RETRY_ATTEMPTS = 3;
    var RECOVERY_TIMEOUT = 30000; // 30초
    
    /**
     * 에러 복구 전략 등록
     * @param {string} errorType 에러 타입
     * @param {Object} strategy 복구 전략 객체
     */
    function registerRecoveryStrategy(errorType, strategy) {
        if (!recoveryStrategies[errorType]) {
            recoveryStrategies[errorType] = [];
        }
        
        recoveryStrategies[errorType].push(strategy);
        
        // 우선순위별 정렬
        recoveryStrategies[errorType].sort(function(a, b) {
            return (a.priority || 0) - (b.priority || 0);
        });
        
        console.log('프론트엔드 에러 복구 전략 등록 완료 - 타입:', errorType, '우선순위:', strategy.priority || 0);
    }
    
    /**
     * 에러 복구 시도
     * @param {Object} errorContext 에러 컨텍스트
     * @returns {Promise<Object>} 복구 결과
     */
    function attemptRecovery(errorContext) {
        return new Promise(function(resolve, reject) {
            var errorId = generateErrorId(errorContext);
            var attempt = getOrCreateRecoveryAttempt(errorId, errorContext);
            
            // 최대 재시도 횟수 확인
            if (attempt.retryCount >= MAX_RETRY_ATTEMPTS) {
                console.warn('최대 재시도 횟수 초과 - 에러 ID:', errorId, '재시도 횟수:', attempt.retryCount);
                resolve({
                    success: false,
                    message: '최대 재시도 횟수를 초과했습니다.'
                });
                return;
            }
            
            // 복구 시도 기록
            attempt.retryCount++;
            stats.totalRecoveryAttempts++;
            
            console.log('프론트엔드 에러 복구 시도 시작 - 에러 ID:', errorId, '타입:', errorContext.errorType, '재시도 횟수:', attempt.retryCount);
            
            try {
                // 에러 타입에 따른 복구 전략 선택
                var strategies = recoveryStrategies[errorContext.errorType];
                
                if (!strategies || strategies.length === 0) {
                    console.warn('해당 에러 타입에 대한 복구 전략이 없음 - 타입:', errorContext.errorType);
                    resolve({
                        success: false,
                        message: '복구 전략이 없습니다.'
                    });
                    return;
                }
                
                // 복구 전략 순차 실행
                executeStrategies(strategies, errorContext, 0, function(result) {
                    if (result.success) {
                        stats.successfulRecoveries++;
                        attempt.lastSuccessfulStrategy = result.strategyName;
                        attempt.lastRecoveryTime = Date.now();
                        
                        console.log('프론트엔드 에러 복구 성공 - 전략:', result.strategyName, '에러 ID:', errorId, '메시지:', result.message);
                    } else {
                        stats.failedRecoveries++;
                        console.warn('프론트엔드 에러 복구 실패 - 전략:', result.strategyName, '에러 ID:', errorId, '메시지:', result.message);
                    }
                    
                    resolve(result);
                });
                
            } catch (error) {
                stats.failedRecoveries++;
                console.error('프론트엔드 에러 복구 시도 중 예외 발생:', error);
                
                resolve({
                    success: false,
                    message: '복구 시도 중 예외가 발생했습니다: ' + error.message
                });
            }
        });
    }
    
    /**
     * 복구 전략 순차 실행
     */
    function executeStrategies(strategies, errorContext, index, callback) {
        if (index >= strategies.length) {
            // 모든 전략 실패
            console.error('모든 복구 전략 실패 - 에러 타입:', errorContext.errorType);
            callback({
                success: false,
                message: '모든 복구 전략이 실패했습니다.',
                strategyName: 'All'
            });
            return;
        }
        
        var strategy = strategies[index];
        
        if (!strategy.canRecover || !strategy.canRecover(errorContext)) {
            console.debug('복구 전략 건너뜀 - 전략:', strategy.name, '에러 타입:', errorContext.errorType);
            executeStrategies(strategies, errorContext, index + 1, callback);
            return;
        }
        
        console.log('복구 전략 실행 - 전략:', strategy.name, '에러 타입:', errorContext.errorType);
        
        try {
            var result = strategy.attemptRecovery(errorContext);
            
            if (result && result.success) {
                result.strategyName = strategy.name;
                callback(result);
            } else {
                console.warn('복구 전략 실패 - 전략:', strategy.name, '메시지:', result ? result.message : 'Unknown error');
                executeStrategies(strategies, errorContext, index + 1, callback);
            }
            
        } catch (error) {
            console.error('복구 전략 실행 중 예외 발생 - 전략:', strategy.name, error);
            executeStrategies(strategies, errorContext, index + 1, callback);
        }
    }
    
    /**
     * 에러 복구 가능 여부 확인
     * @param {Object} errorContext 에러 컨텍스트
     * @returns {boolean} 복구 가능 여부
     */
    function canRecover(errorContext) {
        try {
            var strategies = recoveryStrategies[errorContext.errorType];
            
            if (!strategies || strategies.length === 0) {
                return false;
            }
            
            return strategies.some(function(strategy) {
                return strategy.canRecover && strategy.canRecover(errorContext);
            });
            
        } catch (error) {
            console.warn('복구 가능 여부 확인 중 오류 발생:', error);
            return false;
        }
    }
    
    /**
     * 복구 시도 기록 조회
     * @param {string} errorId 에러 ID
     * @returns {Object} 복구 시도 기록
     */
    function getRecoveryAttempt(errorId) {
        return recoveryAttempts[errorId];
    }
    
    /**
     * 복구 통계 조회
     * @returns {Object} 복구 통계
     */
    function getRecoveryStats() {
        return {
            totalRecoveryAttempts: stats.totalRecoveryAttempts,
            successfulRecoveries: stats.successfulRecoveries,
            failedRecoveries: stats.failedRecoveries,
            successRate: calculateSuccessRate(),
            activeRecoveryAttempts: Object.keys(recoveryAttempts).length,
            lastRecoveryTime: stats.lastRecoveryTime
        };
    }
    
    /**
     * 복구 시도 기록 초기화
     * @param {string} errorId 에러 ID
     */
    function clearRecoveryAttempt(errorId) {
        delete recoveryAttempts[errorId];
    }
    
    /**
     * 모든 복구 시도 기록 초기화
     */
    function clearAllRecoveryAttempts() {
        recoveryAttempts = {};
    }
    
    /**
     * 에러 ID 생성
     */
    function generateErrorId(errorContext) {
        return errorContext.errorType + '_' + errorContext.timestamp + '_' + Math.random().toString(36).substr(2, 9);
    }
    
    /**
     * 복구 시도 기록 조회 또는 생성
     */
    function getOrCreateRecoveryAttempt(errorId, errorContext) {
        if (!recoveryAttempts[errorId]) {
            recoveryAttempts[errorId] = {
                errorId: errorId,
                originalContext: errorContext,
                retryCount: 0,
                firstAttemptTime: Date.now()
            };
        }
        return recoveryAttempts[errorId];
    }
    
    /**
     * 성공률 계산
     */
    function calculateSuccessRate() {
        var total = stats.totalRecoveryAttempts;
        if (total === 0) return 0;
        return ((stats.successfulRecoveries / total) * 100).toFixed(2);
    }
    
    // Public API
    return {
        registerRecoveryStrategy: registerRecoveryStrategy,
        attemptRecovery: attemptRecovery,
        canRecover: canRecover,
        getRecoveryAttempt: getRecoveryAttempt,
        getRecoveryStats: getRecoveryStats,
        clearRecoveryAttempt: clearRecoveryAttempt,
        clearAllRecoveryAttempts: clearAllRecoveryAttempts
    };
})();
