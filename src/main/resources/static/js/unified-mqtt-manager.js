/**
 * 통합 MQTT 관리자
 * HnT Sensor API 프로젝트 전용
 * 
 * 주요 기능:
 * - 모든 MQTT 연결 기능을 하나의 파일로 통합
 * - 페이지 이동 시 연결 유지
 * - 지수 백오프 재연결
 * - 네트워크 상태 모니터링
 * - 페이지별 토픽 구독 관리
 */

// CustomEvent 폴리필 (일부 Android WebView 호환)
(function() {
  if (typeof window.CustomEvent !== "function") {
    function CustomEventPoly(event, params) {
      params = params || { bubbles: false, cancelable: false, detail: undefined };
      var evt = document.createEvent('CustomEvent');
      evt.initCustomEvent(event, params.bubbles, params.cancelable, params.detail);
      return evt;
    }
    CustomEventPoly.prototype = window.Event ? window.Event.prototype : {};
    window.CustomEvent = CustomEventPoly;
  }
})();

var UnifiedMQTTManager = (function() {
    'use strict';
    
    // 전역 MQTT 연결 인스턴스
    var globalConnection = null;
    var client = null;
    
    // 연결 상태 관리
    var connectionState = {
        connected: false,
        connecting: false,
        reconnectAttempts: 0,
        maxReconnectAttempts: 5,
        baseReconnectDelay: 5000,
        lastMessageTime: 0,
        pageVisible: true,
        lastUserActivityTime: Date.now(),
        healthCheckInterval: null,
        lastHealthCheck: 0,
        backendStatusCheckInterval: null,
        lastBackendStatusCheck: 0,
        backendStatusCheckAttempts: 0,
        backendReady: false,
        // 초기화 상태 관리 추가
        initializationStarted: false,
        initializationCompleted: false,
        pageLoadCompleted: false,
        mqttConnected: false,
        initialSyncScheduled: false,
        initialSyncCompleted: false,
        lastInitialSyncTime: 0,
        // 중복 초기화 방지 강화
        duplicatePrevention: {
            enabled: true,
            maxAttempts: 3,
            attemptCount: 0,
            lastAttemptTime: 0,
            cooldownPeriod: 5000, // 5초 쿨다운
            blacklist: new Set(), // 블랙리스트 (페이지별)
            retryDelay: 1000 // 재시도 지연
        },
        // 초기 동기화 타이밍 설정
        initialSyncTiming: {
            setSensorDelay: 500,    // 0.5초 후 setSensor 요청
            getStatusDelay: 2000,   // 2초 후 getStatus 요청
            duplicatePrevention: 2000 // 2초 내 중복 방지
        }
    };
    
    // 구독 토픽 관리
    var subscribedTopics = new Set();
    var topicCallbacks = new Map();
    
    // 파싱 오류 통계 및 로깅
    var parsingErrorStats = {
        totalErrors: 0,
        errorTypes: {},
        recentErrors: [],
        maxRecentErrors: 50
    };
    
    // 실시간 업데이트 간격 최적화 설정
    // 참고: MQTT 데이터는 장치에서 5초마다 전송되며 인터럽트 방식으로 즉시 처리됨
    // 이 설정은 에러 체크 간격을 위한 것이며, 실제 데이터 수신은 MQTT 콜백으로 처리됨
    var updateConfig = {
        defaultInterval: 3000,    // 3초 (에러 체크 간격 - 더 빠른 에러 감지)
        minInterval: 1000,        // 1초 (최소)
        maxInterval: 15000,       // 15초 (최대 - 에러 감지 임계값)
        errorDetectionThreshold: 15000, // 15초 동안 데이터 미수신 시 에러
        mqttDataInterval: 5000,   // 5초 (MQTT 장치 데이터 전송 주기 - 참고용)
        performanceThreshold: 100, // 100ms 이상 지연 시 간격 조정
        adjustmentFactor: 2,      // 간격 조정 배수
        updateCounts: {},
        lastUpdateTimes: {},
        performanceMetrics: {
            totalUpdates: 0,
            averageResponseTime: 0,
            maxResponseTime: 0,
            minResponseTime: Infinity,
            errorRate: 0,
            responseTimes: []
        }
    };
    
    // 프론트엔드-백엔드 데이터 동기화 모니터링
    var syncMonitor = {
        syncStatuses: {},
        syncCounts: {},
        lastSyncTimes: {},
        syncFailures: {},
        maxFailureRecords: 100,
        syncTimeoutMs: 10000,      // 10초
        maxSyncFailures: 5,        // 최대 연속 실패 횟수
        
        // 동기화 상태
        SYNC_STATES: {
            SYNCED: 'SYNCED',
            SYNCING: 'SYNCING',
            OUT_OF_SYNC: 'OUT_OF_SYNC',
            SYNC_ERROR: 'SYNC_ERROR',
            UNKNOWN: 'UNKNOWN'
        }
    };
    
    // 데이터 불일치 감지 및 자동 복구
    var consistencyChecker = {
        dataStates: {},
        lastCheckTimes: {},
        inconsistencies: {},
        maxInconsistencyRecords: 100,
        
        // 복구 통계
        recoveryAttempts: {},
        recoverySuccesses: {},
        recoveryFailures: {},
        
        // 설정
        inconsistencyThresholdMs: 5000,  // 5초
        maxRecoveryAttempts: 3,          // 최대 복구 시도 횟수
        recoveryCooldownMs: 10000        // 10초 (복구 시도 간격)
    };
    
    // 네트워크 복구 관리
    var networkRecovery = {
        // 네트워크 상태
        networkStates: {},
        lastNetworkCheckTimes: {},
        recoveryEvents: [],
        maxRecoveryEvents: 100,
        
        // 복구 통계
        recoveryAttempts: {},
        recoverySuccesses: {},
        recoveryFailures: {},
        
        // 네트워크 상태 타입
        NETWORK_STATUS: {
            ONLINE: 'ONLINE',
            OFFLINE: 'OFFLINE',
            UNSTABLE: 'UNSTABLE',
            RECOVERING: 'RECOVERING',
            UNKNOWN: 'UNKNOWN'
        },
        
        // 설정
        networkCheckIntervalMs: 5000,    // 5초
        networkTimeoutMs: 15000,         // 15초
        maxRecoveryRetries: 5            // 최대 복구 재시도
    };
    
    // 연결 설정
    var config = {
        host: 'hntsolution.co.kr',
        port: 9001,
        path: '/',
        username: 'hnt1',
        password: 'abcde',
        keepAlive: 60,
        cleanSession: true,
        connectTimeout: 30000
    };
    
    // 페이지별 토픽 구독 함수들
    var pageTopicHandlers = {
        'main': function() {
            console.log('메인 페이지 토픽 구독');
            // 모든 센서의 실제 소유자 ID로 토픽 구독
            var sensorIds = window.allowedSensorIds || [];
            console.log('구독할 센서 ID 목록:', sensorIds);
            
            if (sensorIds.length > 0) {
                // 각 센서 소유자 ID별로 토픽 구독 (중복 제거)
                var uniqueSensorIds = Array.from(new Set(sensorIds));
                uniqueSensorIds.forEach(function(sensorId) {
                    var topic = 'HBEE/' + sensorId + '/+/+/DEV';
                    console.log('메인 페이지 토픽 구독:', topic);
                    subscribe(topic, function(message) {
                        console.log('메인 페이지 메시지 수신:', message.destinationName, message.payloadString);
                        handleSensorMessage(message);
                    });
                });
            } else {
                // fallback: 현재 사용자 ID로 구독
                var currentUserId = getCurrentUserId();
                console.log('allowedSensorIds가 없어 currentUserId로 구독:', currentUserId);
                if (currentUserId) {
                    var topic = 'HBEE/' + currentUserId + '/+/+/DEV';
                    console.log('메인 페이지 토픽 구독 (fallback):', topic);
                    subscribe(topic, function(message) {
                        console.log('메인 페이지 메시지 수신:', message.destinationName, message.payloadString);
                        handleSensorMessage(message);
                    });
                } else {
                    console.error('사용자 ID가 없어 토픽 구독 불가');
                }
            }
        },
        'sensorSetting': function() {
            console.log('센서설정 페이지 토픽 구독');
            // 센서설정 페이지는 센서의 실제 소유자 ID로 토픽 구독 (부계정 지원)
            var sensorId = $('#sensorId').val(); // 센서 실제 소유자 ID
            var currentUserId = getCurrentUserId();
            
            console.log('센서설정 페이지 토픽 구독 정보:', {
                sensorId: sensorId,
                currentUserId: currentUserId
            });
            
            if (sensorId) {
                // 센서 소유자 ID로 구독 (부계정이 주계정 센서를 조회하는 경우)
                var topic = 'HBEE/' + sensorId + '/+/+/DEV';
                console.log('센서설정 페이지 토픽 구독 (센서 소유자 ID):', topic);
                subscribe(topic, function(message) {
                    console.log('센서설정 페이지 메시지 수신:', message.destinationName, message.payloadString);
                    handleSensorMessage(message);
                });
            } else if (currentUserId) {
                // fallback: 현재 사용자 ID로 구독
                var topic = 'HBEE/' + currentUserId + '/+/+/DEV';
                console.log('센서설정 페이지 토픽 구독 (현재 사용자 ID):', topic);
                subscribe(topic, function(message) {
                    console.log('센서설정 페이지 메시지 수신:', message.destinationName, message.payloadString);
                    handleSensorMessage(message);
                });
            } else {
                console.error('센서설정 페이지: 센서 소유자 ID 및 사용자 ID가 없어 토픽 구독 불가');
            }
        },
        'chart': function() {
            console.log('차트 페이지 토픽 구독');
            // 차트 페이지는 센서의 실제 소유자 ID로 토픽 구독 (부계정 지원)
            var sensorId = $('#sensorId').val(); // 센서 실제 소유자 ID
            var currentUserId = getCurrentUserId();
            
            console.log('차트 페이지 토픽 구독 정보:', {
                sensorId: sensorId,
                currentUserId: currentUserId
            });
            
            if (sensorId) {
                // 센서 소유자 ID로 구독 (부계정이 주계정 센서를 조회하는 경우)
                var topic = 'HBEE/' + sensorId + '/+/+/DEV';
                console.log('차트 페이지 토픽 구독 (센서 소유자 ID):', topic);
                subscribe(topic, function(message) {
                    console.log('차트 페이지 메시지 수신:', message.destinationName, message.payloadString);
                    handleSensorMessage(message);
                });
            } else if (currentUserId) {
                // fallback: 현재 사용자 ID로 구독
                var topic = 'HBEE/' + currentUserId + '/+/+/DEV';
                console.log('차트 페이지 토픽 구독 (현재 사용자 ID):', topic);
                subscribe(topic, function(message) {
                    console.log('차트 페이지 메시지 수신:', message.destinationName, message.payloadString);
                    handleSensorMessage(message);
                });
            } else {
                console.error('차트 페이지: 센서 소유자 ID 및 사용자 ID가 없어 토픽 구독 불가');
            }
        }
    };
    
    /**
     * 사용자가 접근할 수 있는 장치인지 확인
     * @param {string} sensorUuid 센서 UUID
     * @param {string} userId 사용자 ID
     * @returns {boolean} 접근 가능 여부
     */
    function isValidDeviceForUser(sensorUuid, userId) {
        try {
            // 1. 페이지별 장치 목록 확인
            var deviceList = getDeviceListForCurrentPage();
            if (deviceList && deviceList.length > 0) {
                var isValid = deviceList.some(function(device) {
                    return device.sensor_uuid === sensorUuid || device.uuid === sensorUuid;
                });
                
                if (isValid) {
                    console.log('장치 접근 권한 확인됨 (페이지별 목록):', sensorUuid);
                    return true;
                }
            }
            
            // 2. 세션에서 장치 목록 확인
            var sessionDevices = getSessionDeviceList();
            if (sessionDevices && sessionDevices.length > 0) {
                var isValid = sessionDevices.some(function(device) {
                    return device.sensor_uuid === sensorUuid || device.uuid === sensorUuid;
                });
                
                if (isValid) {
                    console.log('장치 접근 권한 확인됨 (세션 목록):', sensorUuid);
                    return true;
                }
            }
            
            // 3. DOM에서 장치 정보 확인
            var deviceElements = document.querySelectorAll('[data-sensor-uuid="' + sensorUuid + '"]');
            if (deviceElements.length > 0) {
                console.log('장치 접근 권한 확인됨 (DOM 요소):', sensorUuid);
                return true;
            }
            
            // 4. 기본적으로 허용 (기존 동작 유지)
            console.log('장치 접근 권한 기본 허용:', sensorUuid);
            return true;
            
        } catch (error) {
            console.error('장치 접근 권한 확인 중 오류:', error);
            // 오류 발생 시 기본적으로 허용 (기존 동작 유지)
            return true;
        }
    }
    
    /**
     * 현재 페이지의 장치 목록 조회
     */
    function getDeviceListForCurrentPage() {
        try {
            // 메인 페이지의 경우
            if (window.location.pathname === '/main/main') {
                var deviceList = [];
                var deviceElements = document.querySelectorAll('.device-item, .sensor-item');
                deviceElements.forEach(function(element) {
                    var uuid = element.getAttribute('data-sensor-uuid') || 
                              element.getAttribute('data-uuid') ||
                              element.querySelector('[data-sensor-uuid]')?.getAttribute('data-sensor-uuid');
                    if (uuid) {
                        deviceList.push({ sensor_uuid: uuid, uuid: uuid });
                    }
                });
                return deviceList;
            }
            
            // 차트 페이지의 경우
            if (window.location.pathname === '/chart/chart') {
                var sensorUuid = document.getElementById('sensorUuid') ? document.getElementById('sensorUuid').value : null;
                if (sensorUuid) {
                    return [{ sensor_uuid: sensorUuid, uuid: sensorUuid }];
                }
            }
            
            return null;
        } catch (error) {
            console.error('페이지별 장치 목록 조회 중 오류:', error);
            return null;
        }
    }
    
    /**
     * 세션에서 장치 목록 조회
     */
    function getSessionDeviceList() {
        try {
            // 세션 데이터에서 장치 목록 추출
            var sessionData = window.SessionData || {};
            if (sessionData.deviceList) {
                return sessionData.deviceList;
            }
            
            // 전역 변수에서 장치 목록 추출
            if (window.deviceList) {
                return window.deviceList;
            }
            
            return null;
        } catch (error) {
            console.error('세션 장치 목록 조회 중 오류:', error);
            return null;
        }
    }
    
    /**
     * actcode별 메시지 유효성 검사
     * @param {Object} data 메시지 데이터
     * @param {string} sensorUuid 센서 UUID
     * @returns {Object} {isValid: boolean, reason: string}
     */
    function validateMessageByActcode(data, sensorUuid) {
        try {
            switch (data.actcode) {
                case 'live':
                    return validateLiveMessage(data, sensorUuid);
                case 'setres':
                    return validateSetresMessage(data, sensorUuid);
                case 'actres':
                    return validateActresMessage(data, sensorUuid);
                default:
                    return { isValid: false, reason: '알 수 없는 actcode: ' + data.actcode };
            }
        } catch (error) {
            console.error('메시지 유효성 검사 중 오류:', error);
            return { isValid: false, reason: '유효성 검사 오류: ' + error.message };
        }
    }
    
    /**
     * live 메시지 유효성 검사
     */
    function validateLiveMessage(data, sensorUuid) {
        // name 필드 필수
        if (!data.name || typeof data.name !== 'string') {
            return { isValid: false, reason: 'live 메시지에 유효하지 않은 name 필드: ' + data.name };
        }
        
        // 허용된 name 필드만 처리
        var allowedNames = ['ain', 'din', 'output'];
        if (allowedNames.indexOf(data.name) === -1) {
            return { isValid: false, reason: '허용되지 않은 name 필드: ' + data.name };
        }
        
        // name별 추가 검증
        switch (data.name) {
            case 'ain':
                // 온도 데이터 검증
                if (data.value === undefined || data.value === null) {
                    return { isValid: false, reason: 'ain 메시지에 value 필드가 없음' };
                }
                if (data.value !== 'Error' && (isNaN(parseFloat(data.value)) && data.value !== 'Error')) {
                    return { isValid: false, reason: 'ain 메시지의 value가 숫자가 아님: ' + data.value };
                }
                break;
                
            case 'din':
                // DIN 상태 검증
                if (data.value === undefined || data.value === null) {
                    return { isValid: false, reason: 'din 메시지에 value 필드가 없음' };
                }
                var dinValue = parseInt(data.value);
                if (isNaN(dinValue) || dinValue < 0 || dinValue > 65000) {
                    return { isValid: false, reason: 'din 메시지의 value가 유효하지 않음: ' + data.value };
                }
                break;
                
            case 'output':
                // 출력 상태 검증
                if (data.value === undefined || data.value === null) {
                    return { isValid: false, reason: 'output 메시지에 value 필드가 없음' };
                }
                if (data.value !== '0' && data.value !== '1') {
                    return { isValid: false, reason: 'output 메시지의 value가 0 또는 1이 아님: ' + data.value };
                }
                break;
        }
        
        return { isValid: true, reason: 'live 메시지 유효성 검사 통과' };
    }
    
    /**
     * setres 메시지 유효성 검사
     */
    function validateSetresMessage(data, sensorUuid) {
        // setres 메시지는 파라미터 정보를 포함해야 함
        var hasParameter = false;
        for (var key in data) {
            if (key.startsWith('p') && key.length >= 2) {
                var paramNum = key.substring(1);
                if (!isNaN(paramNum) && parseInt(paramNum) >= 1 && parseInt(paramNum) <= 16) {
                    hasParameter = true;
                    break;
                }
            }
        }
        
        if (!hasParameter) {
            return { isValid: false, reason: 'setres 메시지에 파라미터 정보가 없음' };
        }
        
        return { isValid: true, reason: 'setres 메시지 유효성 검사 통과' };
    }
    
    /**
     * actres 메시지 유효성 검사
     */
    function validateActresMessage(data, sensorUuid) {
        // actres 메시지는 name 필드가 있어야 함
        if (!data.name || typeof data.name !== 'string') {
            return { isValid: false, reason: 'actres 메시지에 name 필드가 없음' };
        }
        
        // 허용된 name 필드만 처리
        var allowedNames = ['forcedef', 'output', 'userId'];
        if (allowedNames.indexOf(data.name) === -1) {
            return { isValid: false, reason: 'actres 메시지에 허용되지 않은 name 필드: ' + data.name };
        }
        
        return { isValid: true, reason: 'actres 메시지 유효성 검사 통과' };
    }
    
    /**
     * JSON 유효성 검사 및 파싱
     * @param {string} payload 원본 페이로드
     * @returns {Object} {isValid: boolean, data: Object, reason: string}
     */
    function validateAndParseJson(payload) {
        try {
            // 1. 기본 유효성 검사
            if (!payload || typeof payload !== 'string') {
                return { isValid: false, reason: '페이로드가 문자열이 아님', data: null };
            }
            
            // 2. 빈 문자열 검사
            if (payload.trim() === '') {
                return { isValid: false, reason: '페이로드가 비어있음', data: null };
            }
            
            // 3. 길이 검사 (최대 10KB)
            if (payload.length > 10240) {
                return { isValid: false, reason: '페이로드가 너무 큼 (10KB 초과)', data: null };
            }
            
            // 4. 위험한 문자 검사 (XSS 방지)
            var dangerousChars = ['<script', 'javascript:', 'onload=', 'onerror=', 'eval('];
            var lowerPayload = payload.toLowerCase();
            for (var i = 0; i < dangerousChars.length; i++) {
                if (lowerPayload.indexOf(dangerousChars[i]) !== -1) {
                    return { isValid: false, reason: '위험한 문자가 포함됨: ' + dangerousChars[i], data: null };
                }
            }
            
            // 5. JSON 형식 검사 (기본적인 괄호 검사)
            var trimmedPayload = payload.trim();
            if (!trimmedPayload.startsWith('{') || !trimmedPayload.endsWith('}')) {
                return { isValid: false, reason: 'JSON 형식이 아님 (중괄호 없음)', data: null };
            }
            
            // 6. 배열 형태 메시지 처리 (개선)
            if (trimmedPayload.startsWith('[') && trimmedPayload.endsWith(']')) {
                return processArrayMessage(trimmedPayload);
            }
            
            // 7. JSON 파싱 시도
            var data = JSON.parse(trimmedPayload);
            
            // 8. 파싱된 데이터 유효성 검사
            if (!data || typeof data !== 'object') {
                return { isValid: false, reason: '파싱된 데이터가 객체가 아님', data: null };
            }
            
            // 9. 빈 객체 검사
            if (Object.keys(data).length === 0) {
                return { isValid: false, reason: '빈 객체', data: null };
            }
            
            // 10. 필수 필드 검사 (actcode)
            if (!data.hasOwnProperty('actcode')) {
                return { isValid: false, reason: 'actcode 필드가 없음', data: null };
            }
            
            // 11. actcode 값 검증
            var validActcodes = ['live', 'setres', 'actres', 'error', 'status'];
            if (validActcodes.indexOf(data.actcode) === -1) {
                return { isValid: false, reason: '유효하지 않은 actcode: ' + data.actcode, data: null };
            }
            
            // 12. name 필드 검증 (있는 경우)
            if (data.hasOwnProperty('name')) {
                var validNames = ['ain', 'din', 'output', 'forcedef', 'userId'];
                if (validNames.indexOf(data.name) === -1) {
                    return { isValid: false, reason: '유효하지 않은 name: ' + data.name, data: null };
                }
            }
            
            // 13. 데이터 타입 검사
            for (var key in data) {
                if (data.hasOwnProperty(key)) {
                    var value = data[key];
                    // null, undefined, string, number, boolean만 허용
                    if (value !== null && 
                        value !== undefined && 
                        typeof value !== 'string' && 
                        typeof value !== 'number' && 
                        typeof value !== 'boolean') {
                        return { isValid: false, reason: '유효하지 않은 데이터 타입: ' + key + ' = ' + typeof value, data: null };
                    }
                }
            }
            
            // 14. 필드 개수 검증 (너무 많은 필드가 있으면 의심)
            if (Object.keys(data).length > 20) {
                return { isValid: false, reason: '필드 개수가 너무 많음: ' + Object.keys(data).length, data: null };
            }
            
            // 15. 중첩 객체나 배열 검증 (허용하지 않음)
            for (var key in data) {
                if (data.hasOwnProperty(key)) {
                    var value = data[key];
                    if (value && typeof value === 'object' && !Array.isArray(value)) {
                        return { isValid: false, reason: '중첩 객체는 허용되지 않음: ' + key, data: null };
                    }
                    if (Array.isArray(value)) {
                        return { isValid: false, reason: '배열은 허용되지 않음: ' + key, data: null };
                    }
                }
            }
            
            return { isValid: true, data: data, reason: 'JSON 유효성 검사 통과' };
            
        } catch (error) {
            console.error('JSON 파싱 오류:', error, 'Payload:', payload);
            
            // 파싱 오류 처리 및 로깅
            handleParsingError('JSON_PARSE_ERROR', 'JSON 파싱 실패: ' + error.message, payload, 'validateAndParseJson');
            
            return { isValid: false, reason: 'JSON 파싱 실패: ' + error.message, data: null };
        }
    }
    
    /**
     * 배열 형태 메시지 처리 (개선된 로직)
     * @param {string} jsonString JSON 문자열
     * @returns {Object} {isValid: boolean, data: Object, reason: string}
     */
    function processArrayMessage(jsonString) {
        try {
            // 1. 기본 유효성 검사
            if (!jsonString || typeof jsonString !== 'string') {
                return { isValid: false, reason: 'JSON 문자열이 아님', data: null };
            }
            
            // 2. 길이 검사 (최대 50KB)
            if (jsonString.length > 51200) {
                return { isValid: false, reason: '배열 메시지가 너무 큼 (50KB 초과)', data: null };
            }
            
            // 3. JSON 파싱
            var arrayData = JSON.parse(jsonString);
            
            // 4. 배열 검증
            if (!Array.isArray(arrayData)) {
                return { isValid: false, reason: '파싱된 데이터가 배열이 아님', data: null };
            }
            
            // 5. 배열 크기 검증
            if (arrayData.length === 0) {
                return { isValid: false, reason: '빈 배열', data: null };
            }
            
            if (arrayData.length > 100) {
                return { isValid: false, reason: '배열 크기가 너무 큼: ' + arrayData.length, data: null };
            }
            
            // 6. 유효한 요소 찾기
            var validElements = [];
            var invalidReasons = [];
            
            for (var i = 0; i < arrayData.length; i++) {
                var element = arrayData[i];
                
                if (!element || typeof element !== 'object') {
                    invalidReasons.push('요소[' + i + ']: 객체가 아님');
                    continue;
                }
                
                // 각 요소에 대해 JSON 유효성 검사 적용
                var elementValidation = validateArrayElement(element);
                if (!elementValidation.isValid) {
                    invalidReasons.push('요소[' + i + ']: ' + elementValidation.reason);
                    continue;
                }
                
                validElements.push(element);
            }
            
            // 7. 처리 결과 결정
            if (validElements.length === 0) {
                return { 
                    isValid: false, 
                    reason: '유효한 배열 요소가 없음: ' + invalidReasons.join(', '), 
                    data: null 
                };
            }
            
            // 8. 첫 번째 유효한 요소 사용 (기존 로직 유지)
            var firstValidElement = validElements[0];
            
            console.log('배열 형태 메시지 처리 완료:', {
                originalSize: arrayData.length,
                validElements: validElements.length,
                invalidElements: arrayData.length - validElements.length,
                selectedElement: firstValidElement
            });
            
            return { 
                isValid: true, 
                data: firstValidElement, 
                reason: '배열 처리 성공 - ' + validElements.length + '개 요소 중 첫 번째 사용',
                additionalInfo: {
                    originalArraySize: arrayData.length,
                    validElementCount: validElements.length,
                    invalidElementCount: arrayData.length - validElements.length,
                    invalidReasons: invalidReasons,
                    processingStrategy: 'FIRST_VALID_ELEMENT'
                }
            };
            
        } catch (error) {
            console.error('배열 메시지 처리 오류:', error, 'JSON:', jsonString);
            
            // 파싱 오류 처리 및 로깅
            handleParsingError('ARRAY_PARSE_ERROR', '배열 처리 실패: ' + error.message, jsonString, 'processArrayMessage');
            
            return { isValid: false, reason: '배열 처리 실패: ' + error.message, data: null };
        }
    }
    
    /**
     * 배열 요소 유효성 검사
     * @param {Object} element 배열 요소
     * @returns {Object} {isValid: boolean, reason: string}
     */
    function validateArrayElement(element) {
        try {
            // 1. 기본 검사
            if (!element || typeof element !== 'object') {
                return { isValid: false, reason: '요소가 객체가 아님' };
            }
            
            // 2. 빈 객체 검사
            if (Object.keys(element).length === 0) {
                return { isValid: false, reason: '빈 객체' };
            }
            
            // 3. 필수 필드 검사 (actcode)
            if (!element.hasOwnProperty('actcode')) {
                return { isValid: false, reason: 'actcode 필드가 없음' };
            }
            
            // 4. actcode 값 검증
            var validActcodes = ['live', 'setres', 'actres', 'error', 'status'];
            if (validActcodes.indexOf(element.actcode) === -1) {
                return { isValid: false, reason: '유효하지 않은 actcode: ' + element.actcode };
            }
            
            // 5. name 필드 검증 (있는 경우)
            if (element.hasOwnProperty('name')) {
                var validNames = ['ain', 'din', 'output', 'forcedef', 'userId'];
                if (validNames.indexOf(element.name) === -1) {
                    return { isValid: false, reason: '유효하지 않은 name: ' + element.name };
                }
            }
            
            // 6. 데이터 타입 검사
            for (var key in element) {
                if (element.hasOwnProperty(key)) {
                    var value = element[key];
                    if (value !== null && 
                        value !== undefined && 
                        typeof value !== 'string' && 
                        typeof value !== 'number' && 
                        typeof value !== 'boolean') {
                        return { isValid: false, reason: '유효하지 않은 데이터 타입: ' + key };
                    }
                }
            }
            
            // 7. 필드 개수 검증
            if (Object.keys(element).length > 20) {
                return { isValid: false, reason: '필드 개수가 너무 많음: ' + Object.keys(element).length };
            }
            
            return { isValid: true, reason: '배열 요소 유효성 검사 통과' };
            
        } catch (error) {
            return { isValid: false, reason: '배열 요소 검사 중 오류: ' + error.message };
        }
    }
    
    /**
     * 현재 센서 UUID 가져오기
     */
    function getCurrentSensorUuid() {
        // 1. URL 파라미터에서 센서 UUID 추출
        var urlParams = new URLSearchParams(window.location.search);
        var sensorUuid = urlParams.get('sensorUuid');
        if (sensorUuid) {
            return sensorUuid;
        }
        
        // 2. Hidden input에서 센서 UUID 가져오기
        var sensorUuidInput = document.querySelector('#sensorUuid');
        if (sensorUuidInput && sensorUuidInput.value) {
            return sensorUuidInput.value;
        }
        
        // 3. SessionData에서 센서 UUID 가져오기
        if (window.SessionData && window.SessionData.sensorUuid) {
            return window.SessionData.sensorUuid;
        }
        
        console.warn('센서 UUID를 찾을 수 없습니다');
        return null;
    }
    
    /**
     * 현재 사용자 ID 가져오기
     */
    function getCurrentUserId() {
        console.log('=== 사용자 ID 검색 시작 ===');
        
        var userId = null;
        
        // 1. Hidden input에서 가져오기 (우선순위 1)
        try {
            var userIdInput = document.getElementById('userId');
            console.log('userId input 요소:', userIdInput);
            if (userIdInput) {
                console.log('userId input value:', userIdInput.value);
                console.log('userId input value length:', userIdInput.value ? userIdInput.value.length : 'null');
                if (userIdInput.value && userIdInput.value.trim() !== '') {
                    userId = userIdInput.value.trim();
                    console.log('userId input 값 설정:', userId);
                } else {
                    console.log('userId input 값이 비어있음');
                }
            } else {
                console.log('userId input 요소를 찾을 수 없음');
            }
        } catch (e) {
            console.log('userId input 검색 오류:', e);
        }
        
        // 2. loginUserId input에서 가져오기 (우선순위 2)
        if (!userId) {
            try {
                var loginUserIdInput = document.getElementById('loginUserId');
                console.log('loginUserId input 요소:', loginUserIdInput);
                if (loginUserIdInput && loginUserIdInput.value && loginUserIdInput.value.trim() !== '') {
                    userId = loginUserIdInput.value.trim();
                    console.log('loginUserId input 값 설정:', userId);
                } else {
                    console.log('loginUserId input 값이 비어있음');
                }
            } catch (e) {
                console.log('loginUserId input 검색 오류:', e);
            }
        }
        
        // 3. 전역 변수에서 가져오기 (우선순위 3)
        if (!userId) {
            try {
                if (typeof window.currentUserId !== 'undefined' && window.currentUserId && window.currentUserId.trim() !== '') {
                    userId = window.currentUserId.trim();
                    console.log('window.currentUserId:', userId);
                } else {
                    console.log('window.currentUserId 값이 비어있음');
                }
            } catch (e) {
                console.log('window.currentUserId 검색 오류:', e);
            }
        }
        
        // 4. SessionData에서 가져오기 (우선순위 4)
        if (!userId) {
            try {
                if (typeof window.SessionData !== 'undefined' && window.SessionData && window.SessionData.userId && window.SessionData.userId.trim() !== '') {
                    userId = window.SessionData.userId.trim();
                    console.log('window.SessionData.userId:', userId);
                } else {
                    console.log('window.SessionData.userId 값이 비어있음');
                }
            } catch (e) {
                console.log('window.SessionData 검색 오류:', e);
            }
        }
        
        // 5. URL 파라미터에서 가져오기 (우선순위 5)
        if (!userId) {
            try {
                var urlParams = new URLSearchParams(window.location.search);
                var urlUserId = urlParams.get('userId');
                if (urlUserId && urlUserId.trim() !== '') {
                    userId = urlUserId.trim();
                    console.log('URL 파라미터 userId:', userId);
                } else {
                    console.log('URL 파라미터 userId 값이 비어있음');
                }
            } catch (e) {
                console.log('URL 파라미터 검색 오류:', e);
            }
        }
        
        // 6. 사용자 ID가 없으면 빈 문자열 반환
        if (!userId) {
            console.warn('사용자 ID를 찾을 수 없음 - MQTT 연결 중단');
            return '';
        }
        
        console.log('최종 사용자 ID:', userId || 'undefined');
        console.log('=== 사용자 ID 검색 완료 ===');
        return userId;
    }
    
    /**
     * 파싱 오류 처리 및 로깅 강화
     * @param {string} errorType 오류 타입
     * @param {string} errorMessage 오류 메시지
     * @param {string} originalData 원본 데이터
     * @param {string} context 컨텍스트
     */
    function handleParsingError(errorType, errorMessage, originalData, context) {
        try {
            // 1. 오류 통계 업데이트
            parsingErrorStats.totalErrors++;
            
            if (!parsingErrorStats.errorTypes[errorType]) {
                parsingErrorStats.errorTypes[errorType] = 0;
            }
            parsingErrorStats.errorTypes[errorType]++;
            
            // 2. 최근 오류 목록에 추가
            var errorInfo = {
                timestamp: new Date().toISOString(),
                errorType: errorType,
                errorMessage: errorMessage,
                originalData: originalData ? originalData.substring(0, 100) + '...' : 'null',
                context: context
            };
            
            parsingErrorStats.recentErrors.push(errorInfo);
            
            // 최대 개수 초과 시 오래된 것 제거
            if (parsingErrorStats.recentErrors.length > parsingErrorStats.maxRecentErrors) {
                parsingErrorStats.recentErrors.shift();
            }
            
            // 3. 상세 로깅
            logParsingError(errorType, errorMessage, originalData, context);
            
        } catch (e) {
            console.error('파싱 오류 처리 중 예외 발생:', e);
        }
    }
    
    /**
     * 파싱 오류 상세 로깅
     * @param {string} errorType 오류 타입
     * @param {string} errorMessage 오류 메시지
     * @param {string} originalData 원본 데이터
     * @param {string} context 컨텍스트
     */
    function logParsingError(errorType, errorMessage, originalData, context) {
        var logMessage = '파싱 오류 발생 [' + errorType + '] - 컨텍스트: ' + context + 
                        ', 메시지: ' + errorMessage + 
                        ', 데이터: ' + (originalData ? originalData.substring(0, 200) + '...' : 'null');
        
        // 오류 타입별 로그 레벨 조정
        switch (errorType) {
            case 'JSON_PARSE_ERROR':
            case 'TOPIC_PARSE_ERROR':
                console.warn(logMessage);
                break;
            case 'ARRAY_PARSE_ERROR':
            case 'VALIDATION_ERROR':
                console.info(logMessage);
                break;
            case 'FORMAT_ERROR':
            case 'ENCODING_ERROR':
                console.error(logMessage);
                break;
            default:
                console.warn(logMessage);
                break;
        }
    }
    
    /**
     * 파싱 오류 통계 조회
     * @returns {Object} 오류 통계
     */
    function getParsingErrorStatistics() {
        return {
            totalErrors: parsingErrorStats.totalErrors,
            errorTypes: Object.assign({}, parsingErrorStats.errorTypes),
            recentErrors: parsingErrorStats.recentErrors.slice(),
            topErrorTypes: getTopErrorTypes(5)
        };
    }
    
    /**
     * 상위 오류 타입 조회
     * @param {number} limit 상위 N개
     * @returns {Array} 상위 오류 타입 목록
     */
    function getTopErrorTypes(limit) {
        return Object.entries(parsingErrorStats.errorTypes)
            .sort((a, b) => b[1] - a[1])
            .slice(0, limit)
            .map(([type, count]) => ({ type: type, count: count }));
    }
    
    /**
     * 파싱 오류 통계 리셋
     */
    function resetParsingErrorStatistics() {
        parsingErrorStats.totalErrors = 0;
        parsingErrorStats.errorTypes = {};
        parsingErrorStats.recentErrors = [];
        console.info('파싱 오류 통계가 리셋되었습니다.');
    }
    
    /**
     * 실시간 업데이트 간격 최적화 함수들
     */
    
    /**
     * 업데이트 간격 계산 및 최적화
     * @param {string} context 업데이트 컨텍스트
     * @param {number} currentInterval 현재 간격
     * @returns {Object} 최적화된 간격 설정
     */
    function calculateOptimalInterval(context, currentInterval) {
        try {
            var newInterval = currentInterval;
            var reason = '현재 간격 유지';
            
            // 성능 기반 간격 조정
            if (updateConfig.performanceMetrics.averageResponseTime > updateConfig.performanceThreshold) {
                // 응답 시간이 임계값을 초과하면 간격을 늘림
                newInterval = Math.min(currentInterval * updateConfig.adjustmentFactor, updateConfig.maxInterval);
                reason = '응답 시간 초과로 간격 증가: ' + updateConfig.performanceMetrics.averageResponseTime + 'ms';
            } else if (updateConfig.performanceMetrics.averageResponseTime < updateConfig.performanceThreshold / 2) {
                // 응답 시간이 임계값의 절반 미만이면 간격을 줄임
                newInterval = Math.max(currentInterval / updateConfig.adjustmentFactor, updateConfig.minInterval);
                reason = '응답 시간 양호로 간격 감소: ' + updateConfig.performanceMetrics.averageResponseTime + 'ms';
            }
            
            // 에러율 기반 간격 조정
            if (updateConfig.performanceMetrics.errorRate > 0.1) { // 10% 이상 에러율
                newInterval = Math.min(newInterval * 2, updateConfig.maxInterval);
                reason += ' (에러율 높음: ' + (updateConfig.performanceMetrics.errorRate * 100).toFixed(1) + '%)';
            }
            
            return {
                interval: newInterval,
                reason: reason,
                timestamp: Date.now(),
                context: context,
                previousInterval: currentInterval,
                averageResponseTime: updateConfig.performanceMetrics.averageResponseTime,
                errorRate: updateConfig.performanceMetrics.errorRate
            };
            
        } catch (error) {
            console.error('업데이트 간격 계산 중 오류 발생:', error);
            return {
                interval: updateConfig.defaultInterval,
                reason: '오류 발생으로 기본값 사용',
                timestamp: Date.now(),
                context: context
            };
        }
    }
    
    /**
     * 업데이트 성능 메트릭 업데이트
     * @param {string} context 업데이트 컨텍스트
     * @param {number} responseTime 응답 시간
     * @param {boolean} isError 에러 여부
     */
    function updatePerformanceMetrics(context, responseTime, isError) {
        try {
            // 업데이트 카운트 증가
            if (!updateConfig.updateCounts[context]) {
                updateConfig.updateCounts[context] = 0;
            }
            updateConfig.updateCounts[context]++;
            updateConfig.lastUpdateTimes[context] = Date.now();
            
            // 전체 성능 메트릭 업데이트
            updateConfig.performanceMetrics.totalUpdates++;
            
            // 응답 시간 업데이트
            updateConfig.performanceMetrics.responseTimes.push(responseTime);
            if (updateConfig.performanceMetrics.responseTimes.length > 100) {
                updateConfig.performanceMetrics.responseTimes.shift(); // 최근 100개만 유지
            }
            
            // 최대/최소 응답 시간 업데이트
            updateConfig.performanceMetrics.maxResponseTime = Math.max(
                updateConfig.performanceMetrics.maxResponseTime, 
                responseTime
            );
            updateConfig.performanceMetrics.minResponseTime = Math.min(
                updateConfig.performanceMetrics.minResponseTime, 
                responseTime
            );
            
            // 평균 응답 시간 계산
            var totalResponseTime = updateConfig.performanceMetrics.responseTimes.reduce(function(sum, time) {
                return sum + time;
            }, 0);
            updateConfig.performanceMetrics.averageResponseTime = totalResponseTime / updateConfig.performanceMetrics.responseTimes.length;
            
            // 에러율 업데이트
            if (isError) {
                updateConfig.performanceMetrics.errorRate = 
                    (updateConfig.performanceMetrics.errorRate * (updateConfig.performanceMetrics.totalUpdates - 1) + 1) / 
                    updateConfig.performanceMetrics.totalUpdates;
            } else {
                updateConfig.performanceMetrics.errorRate = 
                    (updateConfig.performanceMetrics.errorRate * (updateConfig.performanceMetrics.totalUpdates - 1)) / 
                    updateConfig.performanceMetrics.totalUpdates;
            }
            
        } catch (error) {
            console.error('성능 메트릭 업데이트 중 오류 발생:', error);
        }
    }
    
    /**
     * 업데이트 간격 유효성 검사
     * @param {number} interval 검사할 간격
     * @returns {boolean} 유효성 여부
     */
    function isValidInterval(interval) {
        return interval >= updateConfig.minInterval && interval <= updateConfig.maxInterval;
    }
    
    /**
     * 업데이트 간격 정규화
     * @param {number} interval 정규화할 간격
     * @returns {number} 정규화된 간격
     */
    function normalizeInterval(interval) {
        if (interval < updateConfig.minInterval) {
            return updateConfig.minInterval;
        } else if (interval > updateConfig.maxInterval) {
            return updateConfig.maxInterval;
        }
        return interval;
    }
    
    /**
     * 업데이트 통계 조회
     * @returns {Object} 업데이트 통계
     */
    function getUpdateStatistics() {
        var totalUpdates = Object.values(updateConfig.updateCounts).reduce(function(sum, count) {
            return sum + count;
        }, 0);
        
        return {
            totalUpdates: totalUpdates,
            contextStats: Object.assign({}, updateConfig.updateCounts),
            lastUpdateTimes: Object.assign({}, updateConfig.lastUpdateTimes),
            performanceMetrics: Object.assign({}, updateConfig.performanceMetrics),
            recommendedInterval: updateConfig.defaultInterval,
            minInterval: updateConfig.minInterval,
            maxInterval: updateConfig.maxInterval
        };
    }
    
    /**
     * 업데이트 간격 리셋
     */
    function resetUpdateStatistics() {
        updateConfig.updateCounts = {};
        updateConfig.lastUpdateTimes = {};
        updateConfig.performanceMetrics = {
            totalUpdates: 0,
            averageResponseTime: 0,
            maxResponseTime: 0,
            minResponseTime: Infinity,
            errorRate: 0,
            responseTimes: []
        };
        console.info('업데이트 통계가 리셋되었습니다.');
    }
    
    /**
     * 프론트엔드-백엔드 데이터 동기화 모니터링 함수들
     */
    
    /**
     * 동기화 시작 기록
     * @param {string} key 동기화 키 (예: userId:sensorUuid)
     */
    function startSync(key) {
        if (!syncMonitor.syncStatuses[key]) {
            syncMonitor.syncStatuses[key] = {
                state: syncMonitor.SYNC_STATES.UNKNOWN,
                lastSyncTime: Date.now(),
                syncDuration: 0,
                consecutiveFailures: 0,
                message: '',
                metadata: {}
            };
        }
        
        syncMonitor.syncStatuses[key].state = syncMonitor.SYNC_STATES.SYNCING;
        syncMonitor.syncStatuses[key].lastSyncTime = Date.now();
        syncMonitor.syncStatuses[key].message = '동기화 시작';
        
        console.debug('동기화 시작:', key);
    }
    
    /**
     * 동기화 성공 기록
     * @param {string} key 동기화 키
     * @param {number} duration 동기화 소요 시간 (밀리초)
     */
    function syncSuccess(key, duration) {
        if (!syncMonitor.syncStatuses[key]) {
            syncMonitor.syncStatuses[key] = {
                state: syncMonitor.SYNC_STATES.UNKNOWN,
                lastSyncTime: Date.now(),
                syncDuration: 0,
                consecutiveFailures: 0,
                message: '',
                metadata: {}
            };
        }
        
        syncMonitor.syncStatuses[key].state = syncMonitor.SYNC_STATES.SYNCED;
        syncMonitor.syncStatuses[key].lastSyncTime = Date.now();
        syncMonitor.syncStatuses[key].syncDuration = duration;
        syncMonitor.syncStatuses[key].consecutiveFailures = 0;
        syncMonitor.syncStatuses[key].message = '동기화 성공';
        
        // 동기화 카운트 증가
        if (!syncMonitor.syncCounts[key]) {
            syncMonitor.syncCounts[key] = 0;
        }
        syncMonitor.syncCounts[key]++;
        syncMonitor.lastSyncTimes[key] = Date.now();
        
        console.debug('동기화 성공:', key, '(소요 시간: ' + duration + 'ms)');
    }
    
    /**
     * 동기화 실패 기록
     * @param {string} key 동기화 키
     * @param {string} reason 실패 이유
     * @param {string} details 실패 상세 정보
     */
    function syncFailure(key, reason, details) {
        if (!syncMonitor.syncStatuses[key]) {
            syncMonitor.syncStatuses[key] = {
                state: syncMonitor.SYNC_STATES.UNKNOWN,
                lastSyncTime: Date.now(),
                syncDuration: 0,
                consecutiveFailures: 0,
                message: '',
                metadata: {}
            };
        }
        
        syncMonitor.syncStatuses[key].state = syncMonitor.SYNC_STATES.OUT_OF_SYNC;
        syncMonitor.syncStatuses[key].lastSyncTime = Date.now();
        syncMonitor.syncStatuses[key].consecutiveFailures++;
        syncMonitor.syncStatuses[key].message = '동기화 실패: ' + reason;
        
        // 실패 정보 기록
        if (!syncMonitor.syncFailures[key]) {
            syncMonitor.syncFailures[key] = [];
        }
        
        syncMonitor.syncFailures[key].push({
            timestamp: Date.now(),
            reason: reason,
            details: details,
            context: {}
        });
        
        // 최대 기록 개수 유지
        if (syncMonitor.syncFailures[key].length > syncMonitor.maxFailureRecords) {
            syncMonitor.syncFailures[key].shift();
        }
        
        // 연속 실패가 임계값을 초과하면 에러 상태로 전환
        if (syncMonitor.syncStatuses[key].consecutiveFailures >= syncMonitor.maxSyncFailures) {
            syncMonitor.syncStatuses[key].state = syncMonitor.SYNC_STATES.SYNC_ERROR;
            console.error('동기화 에러:', key, '(연속 실패 ' + syncMonitor.syncStatuses[key].consecutiveFailures + '회)');
        } else {
            console.warn('동기화 실패:', key, '-', reason, '(연속 실패 ' + syncMonitor.syncStatuses[key].consecutiveFailures + '회)');
        }
    }
    
    /**
     * 동기화 상태 조회
     * @param {string} key 동기화 키
     * @returns {Object} 동기화 상태
     */
    function getSyncStatus(key) {
        return syncMonitor.syncStatuses[key] || null;
    }
    
    /**
     * 모든 동기화 상태 조회
     * @returns {Object} 모든 동기화 상태
     */
    function getAllSyncStatuses() {
        return Object.assign({}, syncMonitor.syncStatuses);
    }
    
    /**
     * 동기화 타임아웃 체크
     * @param {string} key 동기화 키
     * @returns {boolean} 타임아웃 여부
     */
    function isSyncTimeout(key) {
        var status = syncMonitor.syncStatuses[key];
        if (!status) {
            return false;
        }
        
        var timeSinceLastSync = Date.now() - status.lastSyncTime;
        return timeSinceLastSync > syncMonitor.syncTimeoutMs && 
               status.state === syncMonitor.SYNC_STATES.SYNCING;
    }
    
    /**
     * 동기화 통계 조회
     * @returns {Object} 동기화 통계
     */
    function getSyncStatistics() {
        var totalSyncs = Object.values(syncMonitor.syncCounts).reduce(function(sum, count) {
            return sum + count;
        }, 0);
        
        // 상태별 통계
        var stateStats = {};
        Object.values(syncMonitor.syncStatuses).forEach(function(status) {
            if (!stateStats[status.state]) {
                stateStats[status.state] = 0;
            }
            stateStats[status.state]++;
        });
        
        // 실패 통계
        var totalFailures = Object.values(syncMonitor.syncFailures).reduce(function(sum, failures) {
            return sum + failures.length;
        }, 0);
        
        return {
            totalSyncs: totalSyncs,
            stateStats: stateStats,
            keyStats: Object.assign({}, syncMonitor.syncCounts),
            totalFailures: totalFailures,
            lastSyncTimes: Object.assign({}, syncMonitor.lastSyncTimes)
        };
    }
    
    /**
     * 최근 동기화 실패 목록 조회
     * @param {string} key 동기화 키
     * @param {number} limit 조회 개수
     * @returns {Array} 최근 실패 목록
     */
    function getRecentFailures(key, limit) {
        var failures = syncMonitor.syncFailures[key];
        if (!failures || failures.length === 0) {
            return [];
        }
        
        var size = failures.length;
        var fromIndex = Math.max(0, size - limit);
        return failures.slice(fromIndex, size);
    }
    
    /**
     * 동기화 상태 리셋
     * @param {string} key 동기화 키
     */
    function resetSyncStatus(key) {
        delete syncMonitor.syncStatuses[key];
        delete syncMonitor.syncCounts[key];
        delete syncMonitor.lastSyncTimes[key];
        delete syncMonitor.syncFailures[key];
        console.info('동기화 상태 리셋:', key);
    }
    
    /**
     * 모든 동기화 상태 리셋
     */
    function resetAllSyncStatuses() {
        syncMonitor.syncStatuses = {};
        syncMonitor.syncCounts = {};
        syncMonitor.lastSyncTimes = {};
        syncMonitor.syncFailures = {};
        console.info('모든 동기화 상태 리셋 완료');
    }
    
    /**
     * 동기화 상태 건강 체크
     * @returns {Object} 건강 상태 정보
     */
    function healthCheck() {
        var now = Date.now();
        var healthyCount = 0;
        var unhealthyCount = 0;
        var unhealthyKeys = [];
        
        Object.keys(syncMonitor.syncStatuses).forEach(function(key) {
            var status = syncMonitor.syncStatuses[key];
            
            // 건강 체크 조건
            var isHealthy = status.state === syncMonitor.SYNC_STATES.SYNCED &&
                           status.consecutiveFailures === 0 &&
                           (now - status.lastSyncTime) < syncMonitor.syncTimeoutMs;
            
            if (isHealthy) {
                healthyCount++;
            } else {
                unhealthyCount++;
                unhealthyKeys.push(key);
            }
        });
        
        return {
            healthy: healthyCount,
            unhealthy: unhealthyCount,
            unhealthyKeys: unhealthyKeys,
            overallHealth: unhealthyCount === 0 ? 'HEALTHY' : 'UNHEALTHY',
            timestamp: now
        };
    }
    
    /**
     * 동기화 문제 감지
     * @returns {Array} 감지된 문제 목록
     */
    function detectSyncIssues() {
        var issues = [];
        
        Object.keys(syncMonitor.syncStatuses).forEach(function(key) {
            var status = syncMonitor.syncStatuses[key];
            
            // 연속 실패 감지
            if (status.consecutiveFailures >= syncMonitor.maxSyncFailures) {
                issues.push({
                    key: key,
                    type: 'CONSECUTIVE_FAILURES',
                    severity: 'HIGH',
                    message: '연속 ' + status.consecutiveFailures + '회 동기화 실패',
                    details: status.message
                });
            }
            
            // 타임아웃 감지
            if (isSyncTimeout(key)) {
                issues.push({
                    key: key,
                    type: 'SYNC_TIMEOUT',
                    severity: 'MEDIUM',
                    message: '동기화 타임아웃 발생',
                    details: '마지막 동기화 후 ' + syncMonitor.syncTimeoutMs + 'ms 경과'
                });
            }
            
            // 에러 상태 감지
            if (status.state === syncMonitor.SYNC_STATES.SYNC_ERROR) {
                issues.push({
                    key: key,
                    type: 'SYNC_ERROR',
                    severity: 'CRITICAL',
                    message: '동기화 에러 상태',
                    details: status.message
                });
            }
        });
        
        return issues;
    }
    
    /**
     * 토픽 파싱 (개선된 로직)
     * @param {string} topic 토픽 문자열
     * @returns {Object} {isValid: boolean, topicType: string, segments: Object, reason: string}
     */
    function parseTopic(topic) {
        try {
            // 1. 기본 유효성 검사
            if (!topic || typeof topic !== 'string') {
                return { isValid: false, reason: '토픽이 문자열이 아님', topicType: 'UNKNOWN', segments: {} };
            }
            
            // 2. 빈 문자열 검사
            if (topic.trim() === '') {
                return { isValid: false, reason: '토픽이 비어있음', topicType: 'UNKNOWN', segments: {} };
            }
            
            // 3. 토픽 분할
            var topicParts = topic.split('/');
            var segmentCount = topicParts.length;
            
            // 4. 세그먼트 개수 검증
            if (segmentCount < 3 || segmentCount > 5) {
                return { 
                    isValid: false, 
                    reason: '지원되지 않는 토픽 세그먼트 개수: ' + segmentCount, 
                    topicType: 'UNKNOWN', 
                    segments: {} 
                };
            }
            
            // 5. 접두사 검증
            if (topicParts[0] !== 'HBEE') {
                return { 
                    isValid: false, 
                    reason: '잘못된 토픽 접두사: ' + topicParts[0], 
                    topicType: 'UNKNOWN', 
                    segments: {} 
                };
            }
            
            // 6. 토픽 타입별 파싱
            var result = { isValid: true, topicType: 'UNKNOWN', segments: {}, reason: '' };
            
            if (segmentCount === 5) {
                // HBEE/userId/sensorType/sensorUuid/suffix (유일하게 지원되는 토픽)
                result = parse5SegmentTopic(topicParts);
            } else if (segmentCount === 4) {
                // HBEE/userId/sensorType/suffix - 지원하지 않음
                return { 
                    isValid: false, 
                    reason: '4개 세그먼트 토픽은 지원하지 않음: ' + topic, 
                    topicType: 'UNSUPPORTED', 
                    segments: {} 
                };
            } else if (segmentCount === 3) {
                // HBEE/userId/suffix - 지원하지 않음
                return { 
                    isValid: false, 
                    reason: '3개 세그먼트 토픽은 지원하지 않음: ' + topic, 
                    topicType: 'UNSUPPORTED', 
                    segments: {} 
                };
            }
            
            return result;
            
        } catch (error) {
            console.error('토픽 파싱 오류:', error, 'Topic:', topic);
            
            // 파싱 오류 처리 및 로깅
            handleParsingError('TOPIC_PARSE_ERROR', '토픽 파싱 실패: ' + error.message, topic, 'parseTopic');
            
            return { 
                isValid: false, 
                reason: '토픽 파싱 실패: ' + error.message, 
                topicType: 'UNKNOWN', 
                segments: {} 
            };
        }
    }
    
    /**
     * 5개 세그먼트 토픽 파싱
     * @param {Array} topicParts 토픽 세그먼트 배열
     * @returns {Object} 파싱 결과
     */
    function parse5SegmentTopic(topicParts) {
        var prefix = topicParts[0];
        var userId = topicParts[1];
        var sensorType = topicParts[2];
        var sensorUuid = topicParts[3];
        var suffix = topicParts[4];
        
        // 사용자 ID 검증
        if (!userId || userId.trim() === '') {
            return { isValid: false, reason: '사용자 ID가 비어있음', topicType: 'UNKNOWN', segments: {} };
        }
        
        // 센서 타입 검증
        var validSensorTypes = ['TC', 'SENSOR', 'DEVICE', 'IOT'];
        if (validSensorTypes.indexOf(sensorType) === -1) {
            return { isValid: false, reason: '유효하지 않은 센서 타입: ' + sensorType, topicType: 'UNKNOWN', segments: {} };
        }
        
        // 센서 UUID 검증
        if (!sensorUuid || sensorUuid.trim() === '') {
            return { isValid: false, reason: '센서 UUID가 비어있음', topicType: 'UNKNOWN', segments: {} };
        }
        
        // 접미사 검증
        var validSuffixes = ['DEV', 'SER', 'CMD', 'RESP'];
        if (validSuffixes.indexOf(suffix) === -1) {
            return { isValid: false, reason: '유효하지 않은 토픽 접미사: ' + suffix, topicType: 'UNKNOWN', segments: {} };
        }
        
        return {
            isValid: true,
            topicType: 'FULL_TOPIC',
            segments: {
                prefix: prefix,
                userId: userId,
                sensorType: sensorType,
                sensorUuid: sensorUuid,
                suffix: suffix
            },
            reason: '5개 세그먼트 토픽 파싱 성공'
        };
    }
    
    /**
     * 4개 세그먼트 토픽 파싱 (센서 타입 필수)
     * @param {Array} topicParts 토픽 세그먼트 배열
     * @returns {Object} 파싱 결과
     */
    function parse4SegmentTopic(topicParts) {
        var prefix = topicParts[0];
        var userId = topicParts[1];
        var sensorType = topicParts[2];
        var suffix = topicParts[3];
        
        // 사용자 ID 검증
        if (!userId || userId.trim() === '') {
            return { isValid: false, reason: '사용자 ID가 비어있음', topicType: 'UNKNOWN', segments: {} };
        }
        
        // 센서 타입 검증 (4개 세그먼트에서도 센서 타입 필수)
        var validSensorTypes = ['TC', 'SENSOR', 'DEVICE', 'IOT'];
        if (validSensorTypes.indexOf(sensorType) === -1) {
            return { isValid: false, reason: '유효하지 않은 센서 타입: ' + sensorType, topicType: 'UNKNOWN', segments: {} };
        }
        
        // 접미사 검증
        var validSuffixes = ['DEV', 'SER', 'CMD', 'RESP'];
        if (validSuffixes.indexOf(suffix) === -1) {
            return { isValid: false, reason: '유효하지 않은 토픽 접미사: ' + suffix, topicType: 'UNKNOWN', segments: {} };
        }
        
        return {
            isValid: true,
            topicType: 'SIMPLE_TOPIC',
            segments: {
                prefix: prefix,
                userId: userId,
                sensorType: sensorType,
                suffix: suffix
            },
            reason: '4개 세그먼트 토픽 파싱 성공 (센서 타입: ' + sensorType + ')'
        };
    }
    
    /**
     * 3개 세그먼트 토픽 파싱
     * @param {Array} topicParts 토픽 세그먼트 배열
     * @returns {Object} 파싱 결과
     */
    function parse3SegmentTopic(topicParts) {
        var prefix = topicParts[0];
        var userId = topicParts[1];
        var suffix = topicParts[2];
        
        // 사용자 ID 검증
        if (!userId || userId.trim() === '') {
            return { isValid: false, reason: '사용자 ID가 비어있음', topicType: 'UNKNOWN', segments: {} };
        }
        
        // 접미사 검증
        var validSuffixes = ['DEV', 'SER', 'CMD', 'RESP'];
        if (validSuffixes.indexOf(suffix) === -1) {
            return { isValid: false, reason: '유효하지 않은 토픽 접미사: ' + suffix, topicType: 'UNKNOWN', segments: {} };
        }
        
        return {
            isValid: true,
            topicType: 'USER_TOPIC',
            segments: {
                prefix: prefix,
                userId: userId,
                suffix: suffix
            },
            reason: '3개 세그먼트 토픽 파싱 성공'
        };
    }
    
    /**
     * 센서 메시지 처리
     */
    function handleSensorMessage(message) {
        try {
            var topic = message.destinationName;
            var payload = message.payloadString;
            
            console.log('센서 메시지 처리:', topic, payload);
            
            // 토픽 파싱 (개선된 로직)
            var topicParseResult = parseTopic(topic);
            if (!topicParseResult.isValid) {
                console.warn('토픽 파싱 실패:', topicParseResult.reason);
                return;
            }
            
            // 응답 토픽인지 확인 (DEV, RESP)
            var suffix = topicParseResult.segments.suffix;
            if (suffix !== 'DEV' && suffix !== 'RESP') {
                console.log('응답 토픽이 아니므로 메시지 필터링됨:', suffix);
                return;
            }
            
            var userId = topicParseResult.segments.userId;
            var sensorType = topicParseResult.segments.sensorType;
            var sensorUuid = topicParseResult.segments.sensorUuid;
            
            console.log('토픽 파싱 성공:', {
                topicType: topicParseResult.topicType,
                userId: userId,
                sensorType: sensorType,
                sensorUuid: sensorUuid,
                suffix: suffix
            });
                
                // 1단계: 사용자 ID 필터링 (부계정 지원)
                var currentUserId = getCurrentUserId();
                if (!currentUserId) {
                    console.warn('현재 사용자 ID가 없어 메시지를 처리할 수 없습니다.');
                    return;
                }
                
                // 부계정인 경우 allowedSensorIds에 포함된 userId도 허용
                var allowedSensorIds = window.allowedSensorIds || [];
                var isAllowedUser = (userId === currentUserId) || (allowedSensorIds.indexOf(userId) >= 0);
                
                if (!isAllowedUser) {
                    console.log('사용자 ID 불일치로 메시지 필터링됨:', {
                        messageUserId: userId,
                        currentUserId: currentUserId,
                        allowedSensorIds: allowedSensorIds
                    });
                    return;
                }
                
                // 2단계: JSON 유효성 검사 (강화)
                var data;
                var jsonValidation = validateAndParseJson(payload);
                if (!jsonValidation.isValid) {
                    console.warn('JSON 유효성 검사 실패:', jsonValidation.reason);
                    return;
                }
                data = jsonValidation.data;
                
                console.log('메시지 데이터:', data);
                
                // 3단계: 장치 UUID 필터링 (강화)
                if (!sensorUuid || sensorUuid.trim() === '') {
                    console.warn('유효하지 않은 센서 UUID:', sensorUuid);
                    return;
                }
                
                // UUID 형식 검증 (표준 UUID 또는 MAC 주소 형식)
                // 표준 UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
                // MAC 주소: xxxxxxxxxxxx (12자리 16진수, 콜론/하이픈 없음)
                var standardUuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
                var macAddressPattern = /^[0-9a-f]{12}$/i;
                if (!standardUuidPattern.test(sensorUuid) && !macAddressPattern.test(sensorUuid)) {
                    console.warn('잘못된 UUID 형식 (표준 UUID 또는 MAC 주소 필요):', sensorUuid);
                    return;
                }
                
                // 토픽 타입별 추가 검증
                if (topicParseResult.topicType === 'FULL_TOPIC' && !sensorType) {
                    console.warn('5개 세그먼트 토픽에서 센서 타입이 없음');
                    return;
                }
                
                // 현재 사용자가 접근할 수 있는 장치인지 확인
                if (!isValidDeviceForUser(sensorUuid, currentUserId)) {
                    console.log('사용자가 접근할 수 없는 장치로 메시지 필터링됨:', {
                        sensorUuid: sensorUuid,
                        userId: currentUserId
                    });
                    return;
                }
                
                // 4단계: actcode 필터링 (강화)
                if (!data.actcode || typeof data.actcode !== 'string') {
                    console.warn('유효하지 않은 actcode:', data.actcode);
                    return;
                }
                
                // 허용된 actcode만 처리
                var allowedActcodes = ['live', 'setres', 'actres'];
                if (allowedActcodes.indexOf(data.actcode) === -1) {
                    console.log('허용되지 않은 actcode로 메시지 필터링됨:', data.actcode);
                    return;
                }
                
                // 5단계: 메시지 타입별 필터링 (세분화)
                var messageValidation = validateMessageByActcode(data, sensorUuid);
                if (!messageValidation.isValid) {
                    console.log('메시지 유효성 검사 실패:', messageValidation.reason);
                    return;
                }
                
                console.log('메시지 필터링 통과 - 처리 시작:', {
                    userId: userId,
                    sensorUuid: sensorUuid,
                    actcode: data.actcode,
                    name: data.name
                });
                
                // 센서 데이터 처리
                if (data.actcode === 'live' && data.name === 'ain') {
                    // 온도 데이터 처리
                    console.log('온도 데이터 수신:', sensorUuid, data.value);
                    console.log('updateTemperature 함수 호출 전:', {
                        sensorUuid: sensorUuid,
                        value: data.value,
                        currentPath: window.location.pathname
                    });
                    updateTemperature(sensorUuid, data.value);
                } else if (data.actcode === 'live' && data.name === 'din') {
                    // DIN 상태 처리
                    console.log('DIN 상태 수신:', sensorUuid, data.value);
                    updateDinStatus(sensorUuid, data.value);
                } else if (data.actcode === 'live' && data.name === 'output') {
                    // 출력 상태 처리
                    console.log('출력 상태 수신:', sensorUuid, data.value);
                    updateOutputStatus(sensorUuid, data.value);
                } else if (data.actcode === 'setres') {
                    // 설정 응답 처리
                    console.log('설정 응답 수신:', sensorUuid, data);
                    updateSensorSettings(sensorUuid, data);
                }
                
                // 기존 rcvMsg 함수 호출 (상태표시등 업데이트용)
                if (typeof window.rcvMsg === 'function') {
                    // 차트 페이지인 경우 현재 페이지의 sensorUuid와 일치하는 메시지만 처리
                    if (window.location.pathname === '/chart/chart') {
                        var currentSensorUuid = document.getElementById('sensorUuid') ? document.getElementById('sensorUuid').value : null;
                        if (currentSensorUuid && sensorUuid === currentSensorUuid) {
                            console.log('차트 페이지 rcvMsg 함수 호출 (필터링됨):', message.destinationName, message.payloadString);
                            window.rcvMsg(message.destinationName, message.payloadString);
                        } else {
                            console.log('차트 페이지 메시지 필터링됨:', sensorUuid, '!==', currentSensorUuid);
                        }
                    } else if (window.location.pathname === '/admin/sensorSetting') {
                        var currentSensorUuid = document.getElementById('sensorUuid') ? document.getElementById('sensorUuid').value : null;
                        if (currentSensorUuid && sensorUuid === currentSensorUuid) {
                        console.log('센서설정 페이지 rcvMsg 함수 호출 (필터링됨):', message.destinationName, message.payloadString);
                        window.rcvMsg(message.destinationName, message.payloadString);
                    } else {
                        console.debug('센서설정 페이지 메시지 필터링됨:', sensorUuid, '!==', currentSensorUuid);
                    }
                    } else {
                        console.log('메인 페이지 rcvMsg 함수 호출:', message.destinationName, message.payloadString);
                        window.rcvMsg(message.destinationName, message.payloadString);
                    }
                } else {
                    console.warn('rcvMsg 함수가 정의되지 않음');
                }
        } catch (error) {
            console.error('센서 메시지 처리 오류:', error);
        }
    }
    
    /**
     * 온도 업데이트
     */
    function updateTemperature(sensorUuid, value) {
        console.log('온도 업데이트:', sensorUuid, value);
        console.log('updateTemperature 함수 시작:', {
            sensorUuid: sensorUuid,
            value: value,
            currentPath: window.location.pathname,
            isChartPage: window.location.pathname === '/chart/chart'
        });
        
        // 차트 페이지인 경우 현재 페이지의 sensorUuid와 일치하는 경우만 업데이트
        if (window.location.pathname === '/chart/chart') {
            console.log('차트 페이지 조건 충족 - 필터링 로직 시작');
            var currentSensorUuid = document.getElementById('sensorUuid') ? document.getElementById('sensorUuid').value : null;
            console.log('차트 페이지 필터링 체크:', {
                sensorUuid: sensorUuid,
                currentSensorUuid: currentSensorUuid,
                shouldFilter: currentSensorUuid && sensorUuid !== currentSensorUuid
            });
            if (currentSensorUuid && sensorUuid !== currentSensorUuid) {
                console.log('차트 페이지 온도 업데이트 필터링됨:', sensorUuid, '!==', currentSensorUuid);
                return;
            }
        } else {
            console.log('차트 페이지가 아님 - 필터링 건너뜀:', window.location.pathname);
        }
        
        // 센서 설정 페이지에서 deviceLastDataTime 업데이트 (객체 키 방식으로 변경)
        if (window.location.pathname === '/admin/sensorSetting') {
            if (typeof window.deviceLastDataTime !== 'undefined') {
                if (typeof window.deviceLastDataTime === 'object') {
                    window.deviceLastDataTime[sensorUuid] = Date.now();
                    console.log('센서 설정 페이지 deviceLastDataTime 업데이트 (객체):', sensorUuid, window.deviceLastDataTime[sensorUuid]);
                } else {
                    // number인 경우 객체로 변환
                    window.deviceLastDataTime = {};
                    window.deviceLastDataTime[sensorUuid] = Date.now();
                    console.log('센서 설정 페이지 deviceLastDataTime 업데이트 (객체 변환):', sensorUuid, window.deviceLastDataTime[sensorUuid]);
                }
            }
        }
        
        // 메인 페이지의 updateCurrentTemperature 함수가 있으면 사용
        if (typeof window.updateCurrentTemperature === 'function') {
            var isError = (value === 'Error');
            window.updateCurrentTemperature(sensorUuid, value, isError);
            console.log('메인 페이지 updateCurrentTemperature 함수 사용:', sensorUuid, value);
            return;
        }
        
        // 다양한 DOM 요소에서 현재온도 업데이트 시도
        var tempSelectors = [
            'sensorVal' + sensorUuid,
            'curTemp_' + sensorUuid,
            'currentTemp_' + sensorUuid,
            'temp_' + sensorUuid,
            'temperature_' + sensorUuid,
            // 센서 설정 페이지 전용 선택자
            'curTemp',
            // 차트 데이터 페이지 전용 선택자
            'sensorVal'
        ];
        
        var updated = false;
        for (var i = 0; i < tempSelectors.length; i++) {
            var tempElement = document.getElementById(tempSelectors[i]);
            if (tempElement) {
                // HTML 포맷팅으로 업데이트 (메인 페이지와 동일한 방식)
                if (value === 'Error') {
                    tempElement.innerHTML = '<font size="50px">Error</font>';
                } else {
                    tempElement.innerHTML = '<font size="50px">' + value + '°C</font>';
                }
                console.log('현재온도 업데이트 완료:', tempSelectors[i], value);
                updated = true;
                break;
            }
        }
        
        if (!updated) {
            // 차트 페이지에서는 현재온도 요소가 없으므로 경고 출력하지 않음
            if (window.location.pathname !== '/chart/chart') {
                console.warn('현재온도 요소를 찾을 수 없음. 시도한 선택자:', tempSelectors);
            }
        }
        
        // 에러 상태 해제
        if (typeof window.deviceErrorStates !== 'undefined') {
            if (typeof window.deviceErrorStates === 'object') {
                window.deviceErrorStates[sensorUuid] = false;
                console.log('에러 상태 해제:', sensorUuid);
            } else {
                // boolean인 경우 객체로 변환
                window.deviceErrorStates = {};
                window.deviceErrorStates[sensorUuid] = false;
                console.log('에러 상태 해제 (객체 변환):', sensorUuid);
            }
        }
        
        // deviceLastDataTime 업데이트
        if (typeof window.deviceLastDataTime !== 'undefined') {
            if (typeof window.deviceLastDataTime === 'object') {
                window.deviceLastDataTime[sensorUuid] = Date.now();
                console.log('마지막 데이터 시간 업데이트:', sensorUuid);
            } else {
                // number인 경우 객체로 변환
                window.deviceLastDataTime = {};
                window.deviceLastDataTime[sensorUuid] = Date.now();
                console.log('마지막 데이터 시간 업데이트 (객체 변환):', sensorUuid);
            }
        }
        
        // 기존 온도 업데이트 로직 호출 (호환성)
        if (typeof window.updateTemperature === 'function') {
            window.updateTemperature(sensorUuid, value);
        }
    }
    
    /**
     * DIN 상태 업데이트
     */
    function updateDinStatus(sensorUuid, value) {
        console.log('DIN 상태 업데이트:', sensorUuid, value);
        
        // 차트 페이지인 경우 현재 페이지의 sensorUuid와 일치하는 경우만 업데이트
        if (window.location.pathname === '/chart/chart') {
            var currentSensorUuid = document.getElementById('sensorUuid') ? document.getElementById('sensorUuid').value : null;
            if (currentSensorUuid && sensorUuid !== currentSensorUuid) {
                console.log('차트 페이지 DIN 상태 업데이트 필터링됨:', sensorUuid, '!==', currentSensorUuid);
                return;
            }
        }
        
        // 기존 DIN 상태 업데이트 로직 호출
        if (typeof window.updateDinStatus === 'function') {
            window.updateDinStatus(sensorUuid, value);
        }
    }
    
    /**
     * 출력 상태 업데이트
     */
    function updateOutputStatus(sensorUuid, value) {
        console.log('출력 상태 업데이트:', sensorUuid, value);
        
        // 차트 페이지인 경우 현재 페이지의 sensorUuid와 일치하는 경우만 업데이트
        if (window.location.pathname === '/chart/chart') {
            var currentSensorUuid = document.getElementById('sensorUuid') ? document.getElementById('sensorUuid').value : null;
            if (currentSensorUuid && sensorUuid !== currentSensorUuid) {
                console.log('차트 페이지 출력 상태 업데이트 필터링됨:', sensorUuid, '!==', currentSensorUuid);
                return;
            }
        }
        
        // 기존 출력 상태 업데이트 로직 호출
        if (typeof window.updateOutputStatus === 'function') {
            window.updateOutputStatus(sensorUuid, value);
        }
    }
    
    /**
     * 센서 설정 업데이트
     */
    function updateSensorSettings(sensorUuid, data) {
        console.log('센서 설정 업데이트:', sensorUuid, data);
        
        // 센서 설정 페이지인 경우
        if (window.location.pathname === '/admin/sensorSetting') {
            var currentSensorUuid = document.getElementById('sensorUuid') ? document.getElementById('sensorUuid').value : null;
            
            // 현재 페이지의 센서 UUID와 일치하는지 확인
            if (currentSensorUuid && sensorUuid !== currentSensorUuid) {
                console.log('센서 설정 페이지 - 다른 장치의 메시지 필터링됨:', sensorUuid, '!==', currentSensorUuid);
                return;
            }
            
            // handleSetresMessage 함수 호출
            if (typeof window.handleSetresMessage === 'function') {
                console.log('센서 설정 페이지 handleSetresMessage 호출');
                window.handleSetresMessage(data);
                return;
            } else {
                console.warn('handleSetresMessage 함수를 찾을 수 없음');
            }
        }
        
        // 차트 페이지인 경우 현재 페이지의 sensorUuid와 일치하는 경우만 업데이트
        if (window.location.pathname === '/chart/chart') {
            var currentSensorUuid = document.getElementById('sensorUuid') ? document.getElementById('sensorUuid').value : null;
            if (currentSensorUuid && sensorUuid !== currentSensorUuid) {
                console.log('차트 페이지 센서 설정 업데이트 필터링됨:', sensorUuid, '!==', currentSensorUuid);
                return;
            }
        }
        
        // 설정온도 업데이트 (p01 파라미터)
        if (data.p01) {
            // 다양한 설정온도 요소 ID 시도
            var setTempSelectors = [
                'setTmp' + sensorUuid,
                'setTmp',
                'setTemp' + sensorUuid,
                'setTemp'
            ];
            
            var setTemp = (parseInt(data.p01) / 10).toFixed(1);
            var updated = false;
            
            for (var i = 0; i < setTempSelectors.length; i++) {
                var setTempElement = document.getElementById(setTempSelectors[i]);
                if (setTempElement) {
                    setTempElement.innerHTML = setTemp + '°C';
                    console.log('설정온도 업데이트:', setTempSelectors[i], setTemp + '°C');
                    updated = true;
                    break;
                }
            }
            
            if (!updated) {
                console.log('설정온도 요소를 찾을 수 없음:', setTempSelectors);
            }
        }
        
        // 기존 센서 설정 업데이트 로직 호출
        if (typeof window.updateSensorSettings === 'function') {
            window.updateSensorSettings(sensorUuid, data);
        }
    }
    
    /**
     * MQTT 연결 초기화
     */
    function initializeConnection() {
        if (globalConnection) {
            console.log('MQTT 연결이 이미 초기화됨');
            return globalConnection;
        }
        
        try {
            if (typeof Paho === 'undefined') {
                throw new Error('Paho MQTT 라이브러리가 로딩되지 않음');
            }
            
            // 클라이언트 ID 생성
            var clientId = 'hnt_web_' + Math.random().toString(16).substr(2, 8);
            
            // MQTT 클라이언트 생성 (호스트, 포트, 경로, 클라이언트 ID)
            client = new Paho.MQTT.Client(config.host, config.port, config.path, clientId);
            globalConnection = client;
            
            // 연결 옵션 설정
            var connectOptions = {
                userName: config.username,
                password: config.password,
                keepAliveInterval: config.keepAlive,
                cleanSession: config.cleanSession,
                onSuccess: onConnect,
                onFailure: onConnectFailure,
                mqttVersion: 4,
                timeout: config.connectTimeout,
                useSSL: false
            };
            
            // 메시지 수신 콜백 설정
            client.onMessageArrived = onMessageArrived;
            client.onConnectionLost = onConnectionLost;
            
            console.log('MQTT 클라이언트 초기화 완료');
            return client;
            
        } catch (error) {
            console.error('MQTT 클라이언트 초기화 실패:', error);
            throw error;
        }
    }
    
    /**
     * MQTT 연결
     */
    function connect() {
        console.log('=== MQTT connect() 함수 호출됨 ===');
        console.log('현재 상태 - connected:', connectionState.connected, 'connecting:', connectionState.connecting);
        
        return new Promise(function(resolve, reject) {
            try {
                // 이미 연결되어 있으면 성공 반환
                if (connectionState.connected && client && client.isConnected()) {
                    console.log('MQTT 이미 연결됨');
                    resolve(client);
                    return;
                }
                
                // 연결 중이면 대기
                if (connectionState.connecting) {
                    console.log('MQTT 연결 중...');
                    // 연결 완료까지 대기
                    var checkInterval = setInterval(function() {
                        if (connectionState.connected) {
                            clearInterval(checkInterval);
                            resolve(client);
                        } else if (!connectionState.connecting) {
                            clearInterval(checkInterval);
                            reject(new Error('연결 실패'));
                        }
                    }, 100);
                    return;
                }
                
                console.log('MQTT 새 연결 시작...');
                // 연결 상태 설정
                connectionState.connecting = true;
                connectionState.reconnectAttempts = 0;
                
                // 클라이언트 초기화
                if (!client) {
                    console.log('MQTT 클라이언트 초기화 시작...');
                    initializeConnection();
                }
                
                // 연결 시작
                console.log('MQTT 연결 시작...');
                client.connect({
                    userName: config.username,
                    password: config.password,
                    keepAliveInterval: config.keepAlive,
                    cleanSession: config.cleanSession,
                    onSuccess: function() {
                        console.log('MQTT 연결 성공');
                        onConnect();
                        
                        // 헬스 체크 시작
                        startHealthCheck();
                        
                        resolve(client);
                    },
                    onFailure: function(error) {
                        console.error('MQTT 연결 실패:', error);
                        connectionState.connected = false;
                        connectionState.connecting = false;
                        reject(error);
                    },
                    mqttVersion: 4,
                    timeout: config.connectTimeout,
                    useSSL: false
                });
                
            } catch (error) {
                console.error('MQTT 연결 중 오류:', error);
                connectionState.connecting = false;
                reject(error);
            }
        });
    }
    
    /**
     * MQTT 연결 해제 (내부 전용)
     */
    function disconnect() {
        if (client && client.isConnected()) {
            console.log('MQTT 연결 해제');
            client.disconnect();
            connectionState.connected = false;
            connectionState.connecting = false;
            subscribedTopics.clear();
            topicCallbacks.clear();
            
            // 헬스 체크 중지
            stopHealthCheck();
        }
    }
    
    /**
     * 연결 상태 확인 함수
     */
    function isConnected() {
        return client && client.isConnected && client.isConnected();
    }
    
    /**
     * 스마트 재연결 함수 (연결 끊어진 경우에만)
     */
    function ensureConnected() {
        if (!isConnected() && !connectionState.connecting) {
            console.log('MQTT 연결 끊김 감지 - 재연결 시도');
            connect();
            return false;
        }
        return isConnected();
    }
    
    /**
     * 로그아웃으로 인한 MQTT 연결 해제 (외부 API)
     */
    function disconnectOnLogout() {
        console.log('로그아웃으로 인한 MQTT 연결 해제');
        
        // 헬스 체크 중지
        stopHealthCheck();
        
        if (client) {
            try {
                // 연결 상태 확인 후 안전하게 해제
                if (client.isConnected()) {
                    console.log('MQTT 클라이언트 연결 해제 중...');
                    client.disconnect();
                }
                
                // 클라이언트 완전 정리
                client.close();
                console.log('MQTT 클라이언트 완전 정리 완료');
            } catch (error) {
                console.warn('MQTT 클라이언트 정리 중 오류:', error);
            } finally {
                // 상태 초기화
                connectionState.connected = false;
                connectionState.connecting = false;
                subscribedTopics.clear();
                topicCallbacks.clear();
                client = null;
            }
        }
    }
    
    /**
     * 토픽 구독
     */
    function subscribe(topic, callback) {
        console.log('=== subscribe 함수 호출 ===');
        console.log('토픽:', topic || 'undefined');
        console.log('클라이언트 존재:', !!client);
        console.log('클라이언트 연결 상태:', client ? client.isConnected() : 'N/A');
        
        if (!client || !client.isConnected()) {
            console.warn('MQTT 연결이 없어 토픽 구독 불가:', topic);
            return false;
        }
        
        try {
            console.log('client.subscribe() 호출 시작');
            client.subscribe(topic);
            console.log('client.subscribe() 호출 완료');
            
            subscribedTopics.add(topic);
            console.log('subscribedTopics에 추가 완료');
            
            if (callback) {
                topicCallbacks.set(topic, callback);
                console.log('콜백 함수 설정 완료');
            }
            
            console.log('토픽 구독 완료:', topic);
            return true;
        } catch (error) {
            console.error('토픽 구독 실패:', topic, error);
            return false;
        }
    }
    
    /**
     * 토픽 구독 해제
     */
    function unsubscribe(topic) {
        if (!client || !client.isConnected()) {
            console.warn('MQTT 연결이 없어 토픽 구독 해제 불가:', topic);
            return false;
        }
        
        try {
            client.unsubscribe(topic);
            subscribedTopics.delete(topic);
            topicCallbacks.delete(topic);
            console.log('토픽 구독 해제 완료:', topic);
            return true;
        } catch (error) {
            console.error('토픽 구독 해제 실패:', topic, error);
            return false;
        }
    }
    
    /**
     * 센서 설정/상태 요청 발행 (토픽 자동 생성)
     */
    function publishSensorRequest(message) {
        if (!client || !client.isConnected()) {
            console.warn('MQTT 연결이 없어 센서 요청 발행 불가:', message);
            return false;
        }
        
        // 센서 소유자 ID와 센서 UUID 가져오기 (부계정 지원)
        var sensorOwnerId = $('#sensorId').val(); // 센서 실제 소유자 ID
        var currentUserId = getCurrentUserId();
        var sensorUuid = getCurrentSensorUuid();
        
        // 센서 소유자 ID 우선, 없으면 현재 사용자 ID 사용
        var userId = sensorOwnerId || currentUserId;
        
        if (!userId || !sensorUuid) {
            console.error('사용자 ID 또는 센서 UUID가 없어 센서 요청 발행 불가', {
                sensorOwnerId: sensorOwnerId,
                currentUserId: currentUserId,
                sensorUuid: sensorUuid
            });
            return false;
        }
        
        // 토픽 생성: HBEE/{sensorOwnerId}/TC/{sensorUuid}/SER (부계정 지원)
        var topic = 'HBEE/' + userId + '/TC/' + sensorUuid + '/SER';
        
        console.log('센서 요청 발행 - 토픽:', topic, '메시지:', message, {
            sensorOwnerId: sensorOwnerId,
            currentUserId: currentUserId,
            userId: userId,
            sensorUuid: sensorUuid
        });
        
        return publish(topic, message, 0, false);
    }
    
    /**
     * 메시지 발행
     */
    function publish(topic, message, qos, retained) {
        if (!client || !client.isConnected()) {
            console.warn('MQTT 연결이 없어 메시지 발행 불가:', topic);
            return false;
        }
        
        // 토픽 유효성 검사
        if (!validatePublishTopic(topic)) {
            console.error('잘못된 발행 토픽:', topic);
            return false;
        }
        
        try {
            var messageObj = new Paho.MQTT.Message(message);
            messageObj.destinationName = topic;
            messageObj.qos = qos || 0;
            messageObj.retained = retained || false;
            
            client.send(messageObj);
            console.log('메시지 발행 완료:', topic, message);
            return true;
        } catch (error) {
            console.error('메시지 발행 실패:', topic, error);
            return false;
        }
    }
    
    /**
     * 발행 토픽 유효성 검사
     * @param {string} topic 발행할 토픽
     * @returns {boolean} 유효성 여부
     */
    function validatePublishTopic(topic) {
        if (!topic || typeof topic !== 'string') {
            console.error('토픽이 유효하지 않음:', topic);
            return false;
        }
        
        // 와일드카드 검사
        if (topic.indexOf('+') !== -1 || topic.indexOf('#') !== -1) {
            console.error('발행 토픽에 와일드카드 사용 불가:', topic);
            return false;
        }
        
        // 토픽 형식 검증 (HBEE/{sensorId}/TC/{uuid}/SER)
        var topicPattern = /^HBEE\/[a-zA-Z0-9_-]+\/TC\/[a-zA-Z0-9_-]+\/SER$/;
        if (!topicPattern.test(topic)) {
            console.error('잘못된 토픽 형식:', topic);
            console.error('올바른 형식: HBEE/{sensorId}/TC/{uuid}/SER');
            return false;
        }
        
        // 토픽 길이 검사 (너무 긴 토픽 방지)
        if (topic.length > 200) {
            console.error('토픽이 너무 깁니다:', topic.length, 'bytes');
            return false;
        }
        
        return true;
    }
    
    /**
     * 연결 성공 콜백
     */
    function onConnect() {
        console.log('=== MQTT 연결 성공 ===');
        connectionState.connected = true;
        connectionState.connecting = false;
        connectionState.mqttConnected = true;
        connectionState.reconnectAttempts = 0;
        connectionState.lastMessageTime = Date.now();
        
        // 백엔드 상태 주기적 확인 시작
        startBackendStatusCheck();
        
        // 초기화 조건 재확인
        checkInitializationConditions();
        
        // 연결 성공 이벤트 발생
        var event = new CustomEvent('mqtt:connected', {
            detail: { client: client }
        });
        document.dispatchEvent(event);
    }
    
    /**
     * 연결 실패 콜백
     */
    function onConnectFailure(error) {
        console.error('=== MQTT 연결 실패 ===', error);
        connectionState.connected = false;
        connectionState.connecting = false;
        
        // 재연결 시도
        if (connectionState.reconnectAttempts < connectionState.maxReconnectAttempts) {
            connectionState.reconnectAttempts++;
            var delay = Math.min(
                connectionState.baseReconnectDelay * Math.pow(2, connectionState.reconnectAttempts - 1),
                30000
            );
            
            console.log('MQTT 재연결 시도:', connectionState.reconnectAttempts, '지연:', delay + 'ms');
            
            // 네트워크 상태 확인
            if (!navigator.onLine) {
                console.warn('네트워크 오프라인 상태 - 재연결 연기');
                setTimeout(function() {
                    if (navigator.onLine) {
                        console.log('네트워크 복구됨 - 재연결 시도');
                        if (!connectionState.connected) {
                            connect().catch(function(err) {
                                console.error('MQTT 재연결 실패:', err);
                            });
                        }
                    } else {
                        console.log('네트워크 여전히 오프라인 - 재연결 연기');
                        // 네트워크가 여전히 오프라인이면 재연결 시도 횟수를 증가시키지 않고 다시 시도
                        connectionState.reconnectAttempts--;
                        setTimeout(function() {
                            if (!connectionState.connected) {
                                connect().catch(function(err) {
                                    console.error('MQTT 재연결 실패:', err);
                                });
                            }
                        }, 5000);
                    }
                }, 5000);
                return;
            }
            
            setTimeout(function() {
                if (!connectionState.connected) {
                    connect().catch(function(err) {
                        console.error('MQTT 재연결 실패:', err);
                    });
                }
            }, delay);
        } else {
            console.error('MQTT 최대 재연결 시도 횟수 초과');
            console.log('재연결 실패 통계 - 총 시도:', connectionState.reconnectAttempts, '최대 허용:', connectionState.maxReconnectAttempts);
        }
        
        // 연결 실패 이벤트 발생
        var event = new CustomEvent('mqtt:disconnected', {
            detail: { error: error }
        });
        document.dispatchEvent(event);
    }
    
    /**
     * 연결 끊김 콜백
     */
    function onConnectionLost(responseObject) {
        console.warn('=== MQTT 연결 끊김 ===', responseObject);
        connectionState.connected = false;
        connectionState.connecting = false;
        
        // 백엔드 상태 확인 중지
        stopBackendStatusCheck();
        
        // 자동 재연결 시도
        if (connectionState.reconnectAttempts < connectionState.maxReconnectAttempts) {
            connectionState.reconnectAttempts++;
            var delay = Math.min(
                connectionState.baseReconnectDelay * Math.pow(2, connectionState.reconnectAttempts - 1),
                30000
            );
            
            console.log('MQTT 자동 재연결 시도:', connectionState.reconnectAttempts, '지연:', delay + 'ms');
            
            setTimeout(function() {
                if (!connectionState.connected) {
                    reconnectWithTopicRestore().catch(function(err) {
                        console.error('MQTT 자동 재연결 실패:', err);
                        // 재연결 실패 시 사용자에게 알림
                        showConnectionError('MQTT 연결이 끊어졌습니다. 페이지를 새로고침해주세요.');
                    });
                }
            }, delay);
        } else {
            console.error('MQTT 최대 재연결 시도 횟수 초과');
            showConnectionError('MQTT 연결이 불안정합니다. 페이지를 새로고침해주세요.');
        }
        
        // 연결 끊김 이벤트 발생
        var event = new CustomEvent('mqtt:disconnected', {
            detail: { responseObject: responseObject }
        });
        document.dispatchEvent(event);
    }
    
    /**
     * 토픽 구독 복구와 함께 재연결
     */
    function reconnectWithTopicRestore() {
        console.log('=== 토픽 구독 복구와 함께 재연결 시작 ===');
        
        // 기존 구독 토픽 백업
        var backupTopics = Array.from(subscribedTopics);
        var backupCallbacks = new Map(topicCallbacks);
        
        console.log('백업된 토픽:', backupTopics);
        
        return connect().then(function() {
            console.log('재연결 성공 - 토픽 구독 복구 시작');
            
            // 백업된 토픽들 재구독
            var restorePromises = backupTopics.map(function(topic) {
                return new Promise(function(resolve, reject) {
                    try {
                        client.subscribe(topic);
                        console.log('토픽 재구독 완료:', topic);
                        resolve(topic);
                    } catch (error) {
                        console.error('토픽 재구독 실패:', topic, error);
                        reject(error);
                    }
                });
            });
            
            return Promise.all(restorePromises);
        }).then(function() {
            console.log('모든 토픽 재구독 완료');
            
            // 콜백 함수 복구
            topicCallbacks = backupCallbacks;
            
            // 재연결 성공 이벤트 발생
            var event = new CustomEvent('mqtt:reconnected', {
                detail: { restoredTopics: backupTopics }
            });
            document.dispatchEvent(event);
            
        }).catch(function(error) {
            console.error('토픽 구독 복구 실패:', error);
            throw error;
        });
    }
    
    /**
     * 연결 에러 표시
     */
    function showConnectionError(message) {
        console.error('MQTT 연결 에러:', message);
        
        // 기존 에러 메시지 제거
        var existingError = document.getElementById('mqtt-connection-error');
        if (existingError) {
            existingError.remove();
        }
        
        // 에러 메시지 표시
        var errorDiv = document.createElement('div');
        errorDiv.id = 'mqtt-connection-error';
        errorDiv.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #ff4444;
            color: white;
            padding: 15px 20px;
            border-radius: 5px;
            z-index: 10000;
            font-family: Arial, sans-serif;
            font-size: 14px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.3);
            max-width: 300px;
        `;
        errorDiv.innerHTML = `
            <div style="font-weight: bold; margin-bottom: 5px;">연결 오류</div>
            <div>${message}</div>
            <button onclick="this.parentElement.remove()" style="
                background: rgba(255,255,255,0.2);
                border: none;
                color: white;
                padding: 5px 10px;
                border-radius: 3px;
                cursor: pointer;
                margin-top: 10px;
                float: right;
            ">닫기</button>
        `;
        
        document.body.appendChild(errorDiv);
        
        // 10초 후 자동 제거
        setTimeout(function() {
            if (errorDiv.parentElement) {
                errorDiv.remove();
            }
        }, 10000);
    }
    
    /**
     * 메시지 수신 콜백
     */
    function onMessageArrived(message) {
        try {
            connectionState.lastMessageTime = Date.now();
            
            var topic = message.destinationName;
            var payload = message.payloadString;
            
            console.log('MQTT 메시지 수신:', topic, payload);
            
            // 센서 메시지 처리
            handleSensorMessage(message);
            
            // 토픽별 콜백 실행
            if (topicCallbacks.has(topic)) {
                var callback = topicCallbacks.get(topic);
                callback(payload, topic);
            }
            
            // 전역 메시지 수신 이벤트 발생
            var event = new CustomEvent('mqtt:message', {
                detail: { topic: topic, payload: payload, message: message }
            });
            document.dispatchEvent(event);
            
        } catch (error) {
            console.error('MQTT 메시지 처리 중 오류:', error);
        }
    }
    
    /**
     * 페이지별 MQTT 연결 처리
     */
    function handlePageMQTT(pageType) {
        console.log('=== ' + pageType + ' 페이지 MQTT 처리 시작 ===');
        console.log('현재 연결 상태:', connectionState);
        console.log('클라이언트 상태:', client ? '존재' : '없음');
        
        return connect().then(function() {
            console.log(pageType + ' 페이지 MQTT 연결 완료');
            
            // 페이지별 토픽 구독 함수 실행
            if (pageTopicHandlers[pageType]) {
                console.log('페이지별 토픽 구독 함수 실행:', pageType);
                pageTopicHandlers[pageType]();
            } else {
                console.log('페이지별 토픽 구독 함수가 정의되지 않음:', pageType);
            }
            
            return client;
        }).catch(function(error) {
            console.error(pageType + ' 페이지 MQTT 연결 실패:', error);
            throw error;
        });
    }
    
    /**
     * 페이지별 토픽 구독 함수 등록
     */
    function registerPageHandler(pageType, handler) {
        pageTopicHandlers[pageType] = handler;
        console.log('페이지 핸들러 등록:', pageType);
    }
    
    /**
     * 연결 상태 확인
     */
    function isConnected() {
        return connectionState.connected && client && client.isConnected();
    }
    
    /**
     * 연결 상태 정보 반환
     */
    function getConnectionState() {
        return {
            connected: connectionState.connected,
            connecting: connectionState.connecting,
            reconnectAttempts: connectionState.reconnectAttempts,
            lastMessageTime: connectionState.lastMessageTime,
            subscribedTopics: Array.from(subscribedTopics),
            lastHealthCheck: connectionState.lastHealthCheck
        };
    }
    
    /**
     * MQTT 연결 헬스 체크
     */
    function startHealthCheck() {
        console.log('MQTT 헬스 체크 시작');
        
        // 기존 헬스 체크 중지
        if (connectionState.healthCheckInterval) {
            clearInterval(connectionState.healthCheckInterval);
        }
        
        // 30초마다 헬스 체크
        connectionState.healthCheckInterval = setInterval(function() {
            if (connectionState.connected && client && client.isConnected()) {
                connectionState.lastHealthCheck = Date.now();
                console.log('MQTT 헬스 체크 정상');
            } else {
                console.warn('MQTT 헬스 체크 실패 - 연결 끊김 감지');
                connectionState.connected = false;
                connectionState.connecting = false;
                
                // 자동 재연결 시도
                if (connectionState.reconnectAttempts < connectionState.maxReconnectAttempts) {
                    console.log('헬스 체크 실패로 인한 재연결 시도');
                    reconnectWithTopicRestore().catch(function(error) {
                        console.error('헬스 체크 실패 후 재연결 실패:', error);
                    });
                }
            }
        }, 30000);
    }
    
    /**
     * MQTT 연결 헬스 체크 중지
     */
    function stopHealthCheck() {
        if (connectionState.healthCheckInterval) {
            clearInterval(connectionState.healthCheckInterval);
            connectionState.healthCheckInterval = null;
            console.log('MQTT 헬스 체크 중지');
        }
    }
    
    // 페이지 가시성 변경 이벤트 처리
    document.addEventListener('visibilitychange', function() {
        connectionState.pageVisible = !document.hidden;
        
        if (document.hidden) {
            console.log('페이지 숨김 - MQTT 연결 유지');
        } else {
            console.log('페이지 복귀 - MQTT 연결 상태 확인');
            if (!isConnected()) {
                console.log('MQTT 연결 없음 - 재연결 시도');
                reconnectWithTopicRestore().catch(function(error) {
                    console.error('페이지 복귀 시 MQTT 재연결 실패:', error);
                    showConnectionError('페이지 복귀 시 MQTT 연결에 실패했습니다.');
                });
            } else {
                console.log('MQTT 연결 상태 정상 - 토픽 구독 확인');
                // 연결은 되어있지만 토픽 구독이 없을 수 있음
                if (subscribedTopics.size === 0) {
                    console.log('토픽 구독 없음 - 페이지별 토픽 구독 복구');
                    handlePageMQTT();
                }
            }
        }
    });
    
    // 네트워크 상태 변경 이벤트 처리
    window.addEventListener('online', function() {
        console.log('네트워크 연결됨 - MQTT 재연결 시도');
        if (!isConnected()) {
            reconnectWithTopicRestore().catch(function(error) {
                console.error('네트워크 복구 시 MQTT 재연결 실패:', error);
                showConnectionError('네트워크 복구 후 MQTT 연결에 실패했습니다.');
            });
        } else {
            console.log('MQTT 연결 상태 정상 - 토픽 구독 확인');
            // 연결은 되어있지만 토픽 구독이 없을 수 있음
            if (subscribedTopics.size === 0) {
                console.log('토픽 구독 없음 - 페이지별 토픽 구독 복구');
                handlePageMQTT();
            }
        }
    });
    
    window.addEventListener('offline', function() {
        console.log('네트워크 연결 끊김 - MQTT 연결 유지');
        // 네트워크 끊김 시 연결 상태를 false로 설정하지 않음 (재연결 시도 유지)
    });
    
    /**
     * 페이지 로딩 완료 후 MQTT 연결 시퀀스 실행 (강화된 중복 방지)
     */
    function initializePageMQTT() {
        console.log('=== 페이지 MQTT 초기화 시작 ===');
        
        // 강화된 중복 초기화 방지 체크
        if (connectionState.initializationStarted) {
            console.log('MQTT 초기화가 이미 시작됨 - 중복 실행 방지');
            return;
        }
        
        // 중복 방지 설정이 비활성화된 경우 체크 건너뛰기
        if (!connectionState.duplicatePrevention.enabled) {
            console.log('중복 방지 설정이 비활성화됨 - 초기화 진행');
        } else {
            // 중복 방지 체크
            var duplicateCheckResult = checkDuplicateInitialization();
            if (!duplicateCheckResult.allowed) {
                console.log('MQTT 초기화 중복 방지:', duplicateCheckResult.reason);
                return;
            }
        }
        
        connectionState.initializationStarted = true;
        
        // 1. DOM 로딩 완료 확인
        if (document.readyState === 'loading') {
            console.log('DOM 로딩 대기 중...');
            document.addEventListener('DOMContentLoaded', function() {
                console.log('DOM 로딩 완료 - 페이지 로드 완료 대기');
                connectionState.pageLoadCompleted = true;
                checkInitializationConditions();
            });
        } else {
            console.log('DOM 이미 로딩 완료 - 페이지 로드 완료 대기');
            connectionState.pageLoadCompleted = true;
            checkInitializationConditions();
        }
    }
    
    /**
     * 초기화 조건 확인 및 실행
     */
    function checkInitializationConditions() {
        console.log('=== 초기화 조건 확인 ===');
        console.log('페이지 로드 완료:', connectionState.pageLoadCompleted);
        console.log('MQTT 연결됨:', connectionState.mqttConnected);
        console.log('초기화 완료:', connectionState.initializationCompleted);
        
        // 페이지 로드가 완료되지 않았으면 대기
        if (!connectionState.pageLoadCompleted) {
            console.log('페이지 로드 완료 대기 중...');
            return;
        }
        
        // 이미 초기화가 완료되었으면 중복 실행 방지
        if (connectionState.initializationCompleted) {
            console.log('초기화가 이미 완료됨 - 중복 실행 방지');
            return;
        }
        
        // 백엔드 MQTT 상태 확인 후 연결 시작
        console.log('백엔드 MQTT 상태 확인 후 연결 시작');
        checkBackendMqttStatusAndConnect();
    }
    
    /**
     * 백엔드 MQTT 상태 확인 후 프론트엔드 연결 시작
     */
    function checkBackendMqttStatusAndConnect() {
        console.log('=== 백엔드 MQTT 상태 확인 시작 ===');
        
        // 백엔드 MQTT 상태 확인
        fetch('/api/mqtt/status')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('백엔드 MQTT 상태:', data);
                connectionState.lastBackendStatusCheck = Date.now();
                
                if (data.success && data.backendReady && data.connected) {
                    console.log('백엔드 MQTT 준비 완료 - 프론트엔드 연결 시작');
                    console.log('초기화 소요시간:', data.initializationDuration + 'ms');
                    console.log('초기화 상태:', data.initializationStatus);
                    connectionState.backendReady = true;
                    startMQTTConnection();
                } else {
                    console.warn('백엔드 MQTT 준비 안됨 - 재시도 예정');
                    console.log('백엔드 준비:', data.backendReady, '연결:', data.connected);
                    console.log('상태 메시지:', data.message);
                    connectionState.backendReady = false;
                    
                    // 백엔드가 준비되지 않은 경우 재시도 (최대 10회)
                    if (connectionState.backendStatusCheckAttempts < 10) {
                        connectionState.backendStatusCheckAttempts++;
                        setTimeout(function() {
                            console.log('백엔드 MQTT 상태 재확인 시도 (' + connectionState.backendStatusCheckAttempts + '/10)');
                            checkBackendMqttStatusAndConnect();
                        }, 2000); // 2초 후 재시도
                    } else {
                        console.error('백엔드 MQTT 상태 확인 최대 재시도 횟수 초과 - 프론트엔드 연결 시도');
                        startMQTTConnection();
                    }
                }
            })
            .catch(error => {
                console.error('백엔드 MQTT 상태 확인 실패:', error);
                console.log('백엔드 상태 확인 실패 - 프론트엔드 연결 시도');
                connectionState.backendReady = false;
                
                // 백엔드 상태 확인 실패 시에도 프론트엔드 연결 시도
                startMQTTConnection();
            });
    }
    
    /**
     * 백엔드 MQTT 상태 주기적 확인 시작
     */
    function startBackendStatusCheck() {
        if (connectionState.backendStatusCheckInterval) {
            clearInterval(connectionState.backendStatusCheckInterval);
        }
        
        connectionState.backendStatusCheckInterval = setInterval(function() {
            checkBackendMqttStatus();
        }, 10000); // 10초마다 확인
        
        console.log('백엔드 MQTT 상태 주기적 확인 시작');
    }
    
    /**
     * 백엔드 MQTT 상태 주기적 확인 중지
     */
    function stopBackendStatusCheck() {
        if (connectionState.backendStatusCheckInterval) {
            clearInterval(connectionState.backendStatusCheckInterval);
            connectionState.backendStatusCheckInterval = null;
        }
        console.log('백엔드 MQTT 상태 주기적 확인 중지');
    }
    
    /**
     * 백엔드 MQTT 상태 확인 (주기적)
     */
    function checkBackendMqttStatus() {
        fetch('/api/mqtt/status')
            .then(response => response.json())
            .then(data => {
                connectionState.lastBackendStatusCheck = Date.now();
                
                if (data.success && data.backendReady && data.connected) {
                    if (!connectionState.backendReady) {
                        console.log('백엔드 MQTT 상태 복구됨');
                        connectionState.backendReady = true;
                        
                        // 프론트엔드 연결이 끊어진 경우 재연결 시도
                        if (!connectionState.connected) {
                            console.log('백엔드 복구로 인한 프론트엔드 재연결 시도');
                            startMQTTConnection();
                        }
                    }
                } else {
                    if (connectionState.backendReady) {
                        console.warn('백엔드 MQTT 상태 이상 감지');
                        connectionState.backendReady = false;
                    }
                }
            })
            .catch(error => {
                console.error('백엔드 MQTT 상태 확인 실패:', error);
                connectionState.backendReady = false;
            });
    }
    
    /**
     * MQTT 연결 시작
     */
    function startMQTTConnection() {
        console.log('=== MQTT 연결 시작 ===');
        
        // 2. 사용자 ID 확인
        var currentUserId = getCurrentUserId();
        console.log('현재 사용자 ID:', currentUserId || 'undefined');
        
        if (!currentUserId) {
            console.error('사용자 ID를 가져올 수 없음 - MQTT 연결 중단');
            return;
        }
        
        // 3. MQTT 연결
        var connection = connect();
        if (connection) {
            console.log('MQTT 연결 시도 완료');
            
            // 4. 연결 성공 이벤트 리스너 등록 (중복 등록 방지)
            if (!connectionState.initialSyncScheduled) {
                connectionState.initialSyncScheduled = true;
                
                document.addEventListener('mqtt:connected', function() {
                    console.log('MQTT 연결 성공 - 페이지별 토픽 구독 시작');
                    
                    // 현재 페이지 타입 자동 감지
                    var currentPage = getCurrentPageType();
                    console.log('감지된 페이지 타입:', currentPage);
                    
                    if (currentPage) {
                        handlePageMQTT(currentPage);
                    } else {
                        console.warn('페이지 타입을 감지할 수 없습니다. 기본값으로 main 사용');
                        handlePageMQTT('main');
                    }
                    
                    // 초기화 완료 처리
                    completeInitialization();
                });
            }
        } else {
            console.error('MQTT 연결 실패');
        }
    }
    
    /**
     * 초기화 완료 처리
     */
    function completeInitialization() {
        console.log('=== MQTT 초기화 완료 ===');
        connectionState.initializationCompleted = true;
        
        // 초기화 완료 이벤트 발생
        var event = new CustomEvent('mqtt:initialization-complete', {
            detail: { 
                client: client,
                connectionState: connectionState
            }
        });
        document.dispatchEvent(event);
        
        console.log('MQTT 초기화 완료 이벤트 발생');
        
        // 초기 동기화 실행
        executeInitialSync();
    }
    
    /**
     * 통합된 초기 동기화 실행 (강화된 중복 방지)
     */
    function executeInitialSync() {
        console.log('=== 통합 초기 동기화 실행 ===');
        
        // 강화된 중복 실행 방지 체크
        var duplicateCheckResult = checkDuplicateInitialization();
        if (!duplicateCheckResult.allowed) {
            console.log('초기 동기화 중복 방지:', duplicateCheckResult.reason);
            return;
        }
        
        // 중복 방지 상태 업데이트
        updateDuplicatePreventionState();
        
        console.log('초기 동기화 시작 - setSensor:', connectionState.initialSyncTiming.setSensorDelay + 'ms, getStatus:', connectionState.initialSyncTiming.getStatusDelay + 'ms');
        
        // 1. setSensor 요청 (0.5초 후)
        setTimeout(function() {
            console.log('=== setSensor 요청 실행 (0.5초 후) ===');
            executeSetSensorRequests();
        }, connectionState.initialSyncTiming.setSensorDelay);
        
        // 2. getStatus 요청 (2초 후)
        setTimeout(function() {
            console.log('=== getStatus 요청 실행 (2초 후) ===');
            executeGetStatusRequests();
            
            // 초기 동기화 완료 처리
            connectionState.initialSyncCompleted = true;
            console.log('초기 동기화 완료');
        }, connectionState.initialSyncTiming.getStatusDelay);
    }
    
    /**
     * 중복 초기화 방지 체크 (강화된 버전)
     */
    function checkDuplicateInitialization() {
        var now = Date.now();
        var currentPage = getCurrentPageType();
        
        // 1. 기본 중복 방지 체크
        if (connectionState.initialSyncCompleted) {
            return {
                allowed: false,
                reason: '초기 동기화가 이미 완료됨'
            };
        }
        
        // 2. 시간 기반 중복 방지 체크
        if (now - connectionState.lastInitialSyncTime < connectionState.initialSyncTiming.duplicatePrevention) {
            return {
                allowed: false,
                reason: '최근 실행으로부터 ' + connectionState.initialSyncTiming.duplicatePrevention + 'ms 미경과'
            };
        }
        
        // 3. 쿨다운 기간 체크
        if (now - connectionState.duplicatePrevention.lastAttemptTime < connectionState.duplicatePrevention.cooldownPeriod) {
            return {
                allowed: false,
                reason: '쿨다운 기간 중 (' + connectionState.duplicatePrevention.cooldownPeriod + 'ms)'
            };
        }
        
        // 4. 최대 시도 횟수 체크
        if (connectionState.duplicatePrevention.attemptCount >= connectionState.duplicatePrevention.maxAttempts) {
            return {
                allowed: false,
                reason: '최대 시도 횟수 초과 (' + connectionState.duplicatePrevention.maxAttempts + '회)'
            };
        }
        
        // 5. 페이지별 블랙리스트 체크
        if (connectionState.duplicatePrevention.blacklist.has(currentPage)) {
            return {
                allowed: false,
                reason: '페이지 블랙리스트에 등록됨: ' + currentPage
            };
        }
        
        return {
            allowed: true,
            reason: '중복 방지 체크 통과'
        };
    }
    
    /**
     * 중복 방지 상태 업데이트
     */
    function updateDuplicatePreventionState() {
        var now = Date.now();
        var currentPage = getCurrentPageType();
        
        // 상태 업데이트
        connectionState.lastInitialSyncTime = now;
        connectionState.initialSyncScheduled = true;
        connectionState.duplicatePrevention.attemptCount++;
        connectionState.duplicatePrevention.lastAttemptTime = now;
        
        console.log('중복 방지 상태 업데이트 - 시도 횟수:', connectionState.duplicatePrevention.attemptCount, '페이지:', currentPage);
    }
    
    /**
     * 중복 방지 상태 리셋
     */
    function resetDuplicatePreventionState() {
        connectionState.duplicatePrevention.attemptCount = 0;
        connectionState.duplicatePrevention.lastAttemptTime = 0;
        connectionState.duplicatePrevention.blacklist.clear();
        connectionState.initialSyncCompleted = false;
        connectionState.initialSyncScheduled = false;
        
        console.log('중복 방지 상태 리셋 완료');
    }
    
    /**
     * 페이지를 블랙리스트에 추가
     */
    function addPageToBlacklist(pageType, reason) {
        connectionState.duplicatePrevention.blacklist.add(pageType);
        console.log('페이지 블랙리스트 추가:', pageType, '사유:', reason);
    }
    
    /**
     * 페이지를 블랙리스트에서 제거
     */
    function removePageFromBlacklist(pageType) {
        connectionState.duplicatePrevention.blacklist.delete(pageType);
        console.log('페이지 블랙리스트 제거:', pageType);
    }
    
    /**
     * 중복 방지 설정 업데이트
     */
    function updateDuplicatePreventionSettings(settings) {
        if (settings.maxAttempts !== undefined) {
            connectionState.duplicatePrevention.maxAttempts = settings.maxAttempts;
        }
        if (settings.cooldownPeriod !== undefined) {
            connectionState.duplicatePrevention.cooldownPeriod = settings.cooldownPeriod;
        }
        if (settings.retryDelay !== undefined) {
            connectionState.duplicatePrevention.retryDelay = settings.retryDelay;
        }
        if (settings.enabled !== undefined) {
            connectionState.duplicatePrevention.enabled = settings.enabled;
        }
        
        console.log('중복 방지 설정 업데이트:', settings);
    }
    
    /**
     * setSensor 요청 실행
     */
    function executeSetSensorRequests() {
        // 현재 페이지의 모든 센서에 대해 setSensor 요청
        var currentPage = getCurrentPageType();
        console.log('setSensor 요청 실행 - 페이지:', currentPage);
        
        if (currentPage === 'main') {
            // 메인 페이지: 모든 센서에 대해 setSensor 요청
            executeSetSensorForAllSensors();
        } else if (currentPage === 'chart' || currentPage === 'sensorSetting') {
            // 차트/센서설정 페이지: 단일 센서에 대해 setSensor 요청
            executeSetSensorForCurrentSensor();
        }
    }
    
    /**
     * getStatus 요청 실행
     */
    function executeGetStatusRequests() {
        // 현재 페이지의 모든 센서에 대해 getStatus 요청
        var currentPage = getCurrentPageType();
        console.log('getStatus 요청 실행 - 페이지:', currentPage);
        
        if (currentPage === 'main') {
            // 메인 페이지: 모든 센서에 대해 getStatus 요청
            executeGetStatusForAllSensors();
        } else if (currentPage === 'chart' || currentPage === 'sensorSetting') {
            // 차트/센서설정 페이지: 단일 센서에 대해 getStatus 요청
            executeGetStatusForCurrentSensor();
        }
    }
    
    /**
     * 모든 센서에 대해 setSensor 요청 실행
     */
    function executeSetSensorForAllSensors() {
        // 센서 UUID 목록 가져오기
        var sensorUuids = getSensorUuidList();
        console.log('모든 센서 setSensor 요청 - 센서 수:', sensorUuids.length);
        
        sensorUuids.forEach(function(uuid) {
            if (typeof window['setSensor_' + uuid] === 'function') {
                console.log('setSensor_' + uuid + ' 함수 호출');
                window['setSensor_' + uuid]();
            } else {
                console.warn('setSensor_' + uuid + ' 함수가 정의되지 않음');
            }
        });
    }
    
    /**
     * 모든 센서에 대해 getStatus 요청 실행
     */
    function executeGetStatusForAllSensors() {
        // 센서 UUID 목록 가져오기
        var sensorUuids = getSensorUuidList();
        console.log('모든 센서 getStatus 요청 - 센서 수:', sensorUuids.length);
        
        sensorUuids.forEach(function(uuid) {
            if (typeof window['getStatus_' + uuid] === 'function') {
                console.log('getStatus_' + uuid + ' 함수 호출');
                window['getStatus_' + uuid]();
            } else {
                console.warn('getStatus_' + uuid + ' 함수가 정의되지 않음');
            }
        });
    }
    
    /**
     * 현재 센서에 대해 setSensor 요청 실행
     */
    function executeSetSensorForCurrentSensor() {
        if (typeof window.setSensor === 'function') {
            console.log('setSensor 함수 호출');
            window.setSensor();
        } else {
            console.warn('setSensor 함수가 정의되지 않음');
        }
    }
    
    /**
     * 현재 센서에 대해 getStatus 요청 실행
     */
    function executeGetStatusForCurrentSensor() {
        if (typeof window.getStatus === 'function') {
            console.log('getStatus 함수 호출');
            window.getStatus();
        } else {
            console.warn('getStatus 함수가 정의되지 않음');
        }
    }
    
    /**
     * 센서 UUID 목록 가져오기
     */
    function getSensorUuidList() {
        var uuids = [];
        var uuidSet = new Set();  // 중복 제거를 위한 Set
        
        // DOM에서 센서 UUID 추출
        $('input[id^="sensorUuid"]').each(function() {
            var uuid = $(this).val();
            if (uuid && uuid.trim() !== '') {
                uuidSet.add(uuid.trim());
            }
        });
        
        // window.SessionData에서 센서 목록 추출 (중복 자동 제거)
        if (window.SessionData && window.SessionData.sensorList) {
            window.SessionData.sensorList.forEach(function(sensor) {
                if (sensor.sensor_uuid) {
                    uuidSet.add(sensor.sensor_uuid);
                }
            });
        }
        
        // Set을 배열로 변환 (중복 제거됨)
        uuids = Array.from(uuidSet);
        
        console.log('센서 UUID 목록:', uuids);
        return uuids;
    }
    
    /**
     * 현재 페이지 타입 감지
     */
    function getCurrentPageType() {
        var path = window.location.pathname;
        console.log('현재 경로:', path);
        
        if (path.includes('/main/main')) {
            return 'main';
        } else if (path.includes('/admin/sensorSetting')) {
            return 'sensorSetting';
        } else if (path.includes('/chart/chart')) {
            return 'chart';
        } else if (path.includes('/login/login')) {
            return 'login';
        } else {
            console.log('알 수 없는 페이지 타입:', path);
            return null;
        }
    }
    
    /**
     * 페이지 로딩 완료 후 자동 초기화 (개선된 버전)
     */
    function autoInitialize() {
        console.log('=== MQTT 자동 초기화 설정 ===');
        
        // 중복 초기화 방지
        if (connectionState.initializationStarted) {
            console.log('MQTT 초기화가 이미 시작됨 - 자동 초기화 건너뜀');
            return;
        }
        
        // 페이지 로딩 완료 후 MQTT 초기화
        if (document.readyState === 'complete') {
            console.log('페이지 로딩 완료 - MQTT 자동 초기화');
            connectionState.pageLoadCompleted = true;
            initializePageMQTT();
        } else {
            window.addEventListener('load', function() {
                console.log('페이지 로딩 완료 - MQTT 자동 초기화');
                connectionState.pageLoadCompleted = true;
                initializePageMQTT();
            });
        }
        
        // MQTT 초기화 완료 이벤트 리스너 등록
        document.addEventListener('mqtt:initialization-complete', function(event) {
            console.log('MQTT 초기화 완료 이벤트 수신');
            console.log('연결 상태:', event.detail.connectionState);
        });
    }
    
    // 자동 초기화 실행
    autoInitialize();

    // 공개 API
    return {
        connect: connect,
        // disconnect: disconnect,  // 제거 - 일반 페이지에서 호출 불가
        disconnectOnLogout: disconnectOnLogout,  // 로그아웃 전용
        subscribe: subscribe,
        unsubscribe: unsubscribe,
        publish: publish,
        publishSensorRequest: publishSensorRequest,
        handlePageMQTT: handlePageMQTT,
        registerPageHandler: registerPageHandler,
        isConnected: isConnected,
        ensureConnected: ensureConnected,  // 스마트 재연결
        getConnectionState: getConnectionState,
        getClient: function() { return client; },
        initializePageMQTT: initializePageMQTT,
        startMQTTConnection: startMQTTConnection,
        reconnectWithTopicRestore: reconnectWithTopicRestore,
        startHealthCheck: startHealthCheck,
        stopHealthCheck: stopHealthCheck,
        showConnectionError: showConnectionError,
        executeInitialSync: executeInitialSync,
        getInitialSyncTiming: function() { return connectionState.initialSyncTiming; },
        // 중복 방지 관련 API
        resetDuplicatePreventionState: resetDuplicatePreventionState,
        addPageToBlacklist: addPageToBlacklist,
        removePageFromBlacklist: removePageFromBlacklist,
        updateDuplicatePreventionSettings: updateDuplicatePreventionSettings,
        getDuplicatePreventionState: function() { return connectionState.duplicatePrevention; },
        checkDuplicateInitialization: checkDuplicateInitialization
    };
    
    // 객체 반환
    return api;
})();

// 전역 함수로도 사용 가능하도록 설정
window.MQTTConnectionPool = UnifiedMQTTManager;
window.handlePageMQTT = UnifiedMQTTManager.handlePageMQTT;

// 기존 호환성을 위한 별칭
window.MQTTManager = UnifiedMQTTManager;
