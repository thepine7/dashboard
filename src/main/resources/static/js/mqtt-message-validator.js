/**
 * MQTT 메시지 검증자
 * HnT Sensor API 프로젝트 전용
 * 
 * 주요 기능:
 * - MQTT 토픽 형식 검증
 * - JSON 메시지 페이로드 검증
 * - 센서 값 범위 및 형식 검증
 * - 보안 위협 방지
 * - 에러 로깅 및 통계
 */

var MqttMessageValidator = (function() {
    'use strict';
    
    // 검증 통계
    var validationStats = {
        totalMessages: 0,
        validMessages: 0,
        invalidMessages: 0,
        errors: {
            invalidTopic: 0,
            invalidJson: 0,
            invalidActcode: 0,
            invalidSensorValue: 0,
            securityThreat: 0,
            other: 0
        }
    };
    
    // 토픽 패턴 검증
    var TOPIC_PATTERN = /^HBEE\/([a-zA-Z0-9_-]+)\/(TC|SENSOR)\/([a-zA-Z0-9_-]+)\/(SER|DEV)$/;
    var USER_ID_PATTERN = /^[a-zA-Z0-9_-]+$/;
    var UUID_PATTERN = /^[a-zA-Z0-9_-]+$/;
    
    // 허용되는 센서 타입
    var ALLOWED_SENSOR_TYPES = [
        'temp', 'humidity', 'pressure', 'co2', 'pt100', '4-20ma',
        'alarm', 'h/l', 'counter', 'freq', 'comp', 'def', 'fan', 'dout', 'rout', 'aout'
    ];
    
    // 허용되는 액션 코드
    var ALLOWED_ACT_CODES = ['live', 'setres', 'actres'];
    
    // 허용되는 센서 이름
    var ALLOWED_SENSOR_NAMES = ['ain', 'din', 'output'];
    
    // 센서 값 범위 검증
    var SENSOR_VALUE_RANGES = {
        'ain': { min: -200, max: 850 },      // 온도 센서
        'din': { min: 0, max: 65000 },       // 디지털 입력
        'output': { min: 0, max: 1 }         // 출력 (0 또는 1)
    };
    
    /**
     * MQTT 메시지 검증 메인 함수
     * @param {string} topic MQTT 토픽
     * @param {string} payload 메시지 페이로드
     * @param {string} userId 현재 사용자 ID
     * @param {Array} deviceList 등록된 장치 목록
     * @returns {Object} 검증 결과
     */
    function validateMessage(topic, payload, userId, deviceList) {
        validationStats.totalMessages++;
        
        var result = {
            isValid: false,
            error: null,
            sanitizedTopic: null,
            sanitizedPayload: null,
            parsedData: null
        };
        
        try {
            // 1. 토픽 검증
            var topicValidation = validateTopic(topic, userId);
            if (!topicValidation.isValid) {
                validationStats.errors.invalidTopic++;
                validationStats.invalidMessages++;
                result.error = topicValidation.error;
                return result;
            }
            
            // 2. 페이로드 검증
            var payloadValidation = validatePayload(payload);
            if (!payloadValidation.isValid) {
                validationStats.errors.invalidJson++;
                validationStats.invalidMessages++;
                result.error = payloadValidation.error;
                return result;
            }
            
            // 3. 장치 UUID 검증
            var uuid = topicValidation.parsedTopic.uuid;
            if (deviceList && deviceList.indexOf(uuid) === -1) {
                validationStats.errors.invalidTopic++;
                validationStats.invalidMessages++;
                result.error = '유효하지 않은 장치 UUID: ' + uuid;
                return result;
            }
            
            // 4. 메시지 내용 검증
            var contentValidation = validateMessageContent(payloadValidation.parsedData);
            if (!contentValidation.isValid) {
                validationStats.errors.invalidActcode++;
                validationStats.invalidMessages++;
                result.error = contentValidation.error;
                return result;
            }
            
            // 5. 보안 검증
            var securityValidation = validateSecurity(topic, payload);
            if (!securityValidation.isValid) {
                validationStats.errors.securityThreat++;
                validationStats.invalidMessages++;
                result.error = securityValidation.error;
                return result;
            }
            
            // 검증 성공
            validationStats.validMessages++;
            result.isValid = true;
            result.sanitizedTopic = topic;
            result.sanitizedPayload = payload;
            result.parsedData = payloadValidation.parsedData;
            
            return result;
            
        } catch (error) {
            validationStats.errors.other++;
            validationStats.invalidMessages++;
            result.error = '메시지 검증 중 오류 발생: ' + error.message;
            console.error('MQTT 메시지 검증 오류:', error);
            return result;
        }
    }
    
    /**
     * 토픽 형식 검증
     * @param {string} topic MQTT 토픽
     * @param {string} userId 현재 사용자 ID
     * @returns {Object} 검증 결과
     */
    function validateTopic(topic, userId) {
        if (!topic || typeof topic !== 'string') {
            return { isValid: false, error: '토픽이 비어있습니다.' };
        }
        
        // 토픽 길이 검증
        if (topic.length > 200) {
            return { isValid: false, error: '토픽이 너무 깁니다.' };
        }
        
        // 토픽 패턴 검증
        var match = topic.match(TOPIC_PATTERN);
        if (!match) {
            return { isValid: false, error: '잘못된 토픽 형식: ' + topic };
        }
        
        var parsedTopic = {
            userId: match[1],
            sensorType: match[2],
            uuid: match[3],
            direction: match[4]
        };
        
        // 사용자 ID 검증
        if (!USER_ID_PATTERN.test(parsedTopic.userId)) {
            return { isValid: false, error: '잘못된 사용자 ID 형식: ' + parsedTopic.userId };
        }
        
        // UUID 검증
        if (!UUID_PATTERN.test(parsedTopic.uuid)) {
            return { isValid: false, error: '잘못된 UUID 형식: ' + parsedTopic.uuid };
        }
        
        // 센서 타입 검증
        if (!ALLOWED_SENSOR_TYPES.includes(parsedTopic.sensorType.toLowerCase())) {
            return { isValid: false, error: '허용되지 않는 센서 타입: ' + parsedTopic.sensorType };
        }
        
        // 방향 검증
        if (!['SER', 'DEV'].includes(parsedTopic.direction)) {
            return { isValid: false, error: '잘못된 방향: ' + parsedTopic.direction };
        }
        
        return { isValid: true, parsedTopic: parsedTopic };
    }
    
    /**
     * 페이로드 검증
     * @param {string} payload 메시지 페이로드
     * @returns {Object} 검증 결과
     */
    function validatePayload(payload) {
        if (!payload || typeof payload !== 'string') {
            return { isValid: false, error: '페이로드가 비어있습니다.' };
        }
        
        // 페이로드 길이 검증
        if (payload.length > 1000) {
            return { isValid: false, error: '페이로드가 너무 깁니다.' };
        }
        
        // JSON 파싱 시도
        try {
            var parsedData = JSON.parse(payload);
            if (typeof parsedData !== 'object' || parsedData === null) {
                return { isValid: false, error: 'JSON이 객체가 아닙니다.' };
            }
            return { isValid: true, parsedData: parsedData };
        } catch (error) {
            return { isValid: false, error: '잘못된 JSON 형식: ' + error.message };
        }
    }
    
    /**
     * 메시지 내용 검증
     * @param {Object} data 파싱된 JSON 데이터
     * @returns {Object} 검증 결과
     */
    function validateMessageContent(data) {
        if (!data || typeof data !== 'object') {
            return { isValid: false, error: '메시지 데이터가 없습니다.' };
        }
        
        // actcode 검증
        if (!data.actcode || typeof data.actcode !== 'string') {
            return { isValid: false, error: 'actcode가 없거나 잘못된 형식입니다.' };
        }
        
        if (!ALLOWED_ACT_CODES.includes(data.actcode)) {
            return { isValid: false, error: '허용되지 않는 actcode: ' + data.actcode };
        }
        
        // actcode별 추가 검증
        switch (data.actcode) {
            case 'live':
                return validateLiveMessage(data);
            case 'setres':
                return validateSetResMessage(data);
            case 'actres':
                return validateActResMessage(data);
            default:
                return { isValid: false, error: '알 수 없는 actcode: ' + data.actcode };
        }
    }
    
    /**
     * live 메시지 검증
     * @param {Object} data 메시지 데이터
     * @returns {Object} 검증 결과
     */
    function validateLiveMessage(data) {
        // 필수 필드 검증
        if (!data.name || typeof data.name !== 'string') {
            return { isValid: false, error: 'live 메시지에 name이 없습니다.' };
        }
        
        if (!data.value || typeof data.value !== 'string') {
            return { isValid: false, error: 'live 메시지에 value가 없습니다.' };
        }
        
        // 센서 이름 검증
        if (!ALLOWED_SENSOR_NAMES.includes(data.name)) {
            return { isValid: false, error: '허용되지 않는 센서 이름: ' + data.name };
        }
        
        // 센서 값 검증
        var valueValidation = validateSensorValue(data.name, data.value);
        if (!valueValidation.isValid) {
            return { isValid: false, error: valueValidation.error };
        }
        
        // type과 ch 필드 검증 (선택적)
        if (data.type && (!Number.isInteger(parseInt(data.type)) || parseInt(data.type) < 1 || parseInt(data.type) > 99)) {
            return { isValid: false, error: '잘못된 type 값: ' + data.type };
        }
        
        if (data.ch && (!Number.isInteger(parseInt(data.ch)) || parseInt(data.ch) < 1 || parseInt(data.ch) > 99)) {
            return { isValid: false, error: '잘못된 ch 값: ' + data.ch };
        }
        
        return { isValid: true };
    }
    
    /**
     * setres 메시지 검증
     * @param {Object} data 메시지 데이터
     * @returns {Object} 검증 결과
     */
    function validateSetResMessage(data) {
        // p01~p16 파라미터 검증
        for (var i = 1; i <= 16; i++) {
            var paramKey = 'p' + (i < 10 ? '0' + i : i);
            if (data[paramKey]) {
                var value = data[paramKey];
                if (typeof value !== 'string') {
                    return { isValid: false, error: paramKey + ' 값이 문자열이 아닙니다.' };
                }
                
                // 숫자 값 검증
                var numValue = parseFloat(value);
                if (isNaN(numValue)) {
                    return { isValid: false, error: paramKey + ' 값이 숫자가 아닙니다: ' + value };
                }
                
                // 파라미터별 범위 검증
                var rangeValidation = validateParameterRange(i, numValue);
                if (!rangeValidation.isValid) {
                    return { isValid: false, error: rangeValidation.error };
                }
            }
        }
        
        return { isValid: true };
    }
    
    /**
     * actres 메시지 검증
     * @param {Object} data 메시지 데이터
     * @returns {Object} 검증 결과
     */
    function validateActResMessage(data) {
        // name 필드 검증
        if (!data.name || typeof data.name !== 'string') {
            return { isValid: false, error: 'actres 메시지에 name이 없습니다.' };
        }
        
        var allowedNames = ['forcedef', 'output', 'userId'];
        if (!allowedNames.includes(data.name)) {
            return { isValid: false, error: '허용되지 않는 name: ' + data.name };
        }
        
        return { isValid: true };
    }
    
    /**
     * 센서 값 검증
     * @param {string} sensorName 센서 이름
     * @param {string} value 센서 값
     * @returns {Object} 검증 결과
     */
    function validateSensorValue(sensorName, value) {
        if (value === 'Error') {
            return { isValid: true }; // Error는 허용
        }
        
        var numValue = parseFloat(value);
        if (isNaN(numValue)) {
            return { isValid: false, error: '센서 값이 숫자가 아닙니다: ' + value };
        }
        
        var range = SENSOR_VALUE_RANGES[sensorName];
        if (range) {
            if (numValue < range.min || numValue > range.max) {
                return { isValid: false, error: '센서 값이 범위를 벗어났습니다: ' + value + ' (범위: ' + range.min + '~' + range.max + ')' };
            }
        }
        
        return { isValid: true };
    }
    
    /**
     * 파라미터 범위 검증
     * @param {number} paramIndex 파라미터 인덱스 (1-16)
     * @param {number} value 파라미터 값
     * @returns {Object} 검증 결과
     */
    function validateParameterRange(paramIndex, value) {
        switch (paramIndex) {
            case 1: // p01: 설정 온도
                if (value < -200 || value > 850) {
                    return { isValid: false, error: 'p01 값이 범위를 벗어났습니다: ' + value + ' (범위: -200~850)' };
                }
                break;
            case 2: // p02: 히스테리시스 편차
                if (value < 0.1 || value > 19.9) {
                    return { isValid: false, error: 'p02 값이 범위를 벗어났습니다: ' + value + ' (범위: 0.1~19.9)' };
                }
                break;
            case 3: // p03: COMP 출력 지연시간
                if (value < 0 || value > 599) {
                    return { isValid: false, error: 'p03 값이 범위를 벗어났습니다: ' + value + ' (범위: 0~599)' };
                }
                break;
            case 4: // p04: 온도 보정
                if (value < -10.0 || value > 10.0) {
                    return { isValid: false, error: 'p04 값이 범위를 벗어났습니다: ' + value + ' (범위: -10.0~10.0)' };
                }
                break;
            case 5: // p05: 제상 정지시간
                if (value < 0 || value > 250) {
                    return { isValid: false, error: 'p05 값이 범위를 벗어났습니다: ' + value + ' (범위: 0~250)' };
                }
                break;
            case 6: // p06: 제상 시간
                if (value < 0 || value > 250) {
                    return { isValid: false, error: 'p06 값이 범위를 벗어났습니다: ' + value + ' (범위: 0~250)' };
                }
                break;
            case 7: // p07: 팬 설정
                if (value < 0 || value > 3) {
                    return { isValid: false, error: 'p07 값이 범위를 벗어났습니다: ' + value + ' (범위: 0~3)' };
                }
                break;
            case 8: // p08: 제상 후 FAN ON 지연시간
                if (value < 0 || value > 599) {
                    return { isValid: false, error: 'p08 값이 범위를 벗어났습니다: ' + value + ' (범위: 0~599)' };
                }
                break;
            case 9: // p09: FAN OFF 지연시간
                if (value < 0 || value > 599) {
                    return { isValid: false, error: 'p09 값이 범위를 벗어났습니다: ' + value + ' (범위: 0~599)' };
                }
                break;
            case 10: // p10: 저온 방지 온도편차
                if (value < 0.0 || value > 9.9) {
                    return { isValid: false, error: 'p10 값이 범위를 벗어났습니다: ' + value + ' (범위: 0.0~9.9)' };
                }
                break;
            case 11: // p11: COMP 누적 시간 제상 선택
                if (value < 0 || value > 1) {
                    return { isValid: false, error: 'p11 값이 범위를 벗어났습니다: ' + value + ' (범위: 0~1)' };
                }
                break;
            case 12: // p12: 온도 센서 타입
                if (value < 0 || value > 2) {
                    return { isValid: false, error: 'p12 값이 범위를 벗어났습니다: ' + value + ' (범위: 0~2)' };
                }
                break;
            case 13: // p13: 수동조작 on/off
                if (value < 0 || value > 1) {
                    return { isValid: false, error: 'p13 값이 범위를 벗어났습니다: ' + value + ' (범위: 0~1)' };
                }
                break;
            case 14: // p14: 통신 국번
                if (value < 1 || value > 99) {
                    return { isValid: false, error: 'p14 값이 범위를 벗어났습니다: ' + value + ' (범위: 1~99)' };
                }
                break;
            case 15: // p15: 통신 속도
                if (value < 0 || value > 4) {
                    return { isValid: false, error: 'p15 값이 범위를 벗어났습니다: ' + value + ' (범위: 0~4)' };
                }
                break;
            case 16: // p16: Cooler/Heater 모드 선택
                if (value < 0 || value > 1) {
                    return { isValid: false, error: 'p16 값이 범위를 벗어났습니다: ' + value + ' (범위: 0~1)' };
                }
                break;
        }
        
        return { isValid: true };
    }
    
    /**
     * 보안 검증
     * @param {string} topic MQTT 토픽
     * @param {string} payload 메시지 페이로드
     * @returns {Object} 검증 결과
     */
    function validateSecurity(topic, payload) {
        // SQL 인젝션 패턴 검증
        var sqlPatterns = [
            /(\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION|SCRIPT)\b)/i,
            /(\b(OR|AND)\s+\d+\s*=\s*\d+)/i,
            /(\b(OR|AND)\s+['"]\s*=\s*['"])/i,
            /(UNION\s+SELECT)/i,
            /(DROP\s+TABLE)/i,
            /(INSERT\s+INTO)/i,
            /(UPDATE\s+SET)/i,
            /(DELETE\s+FROM)/i
        ];
        
        for (var i = 0; i < sqlPatterns.length; i++) {
            if (sqlPatterns[i].test(topic) || sqlPatterns[i].test(payload)) {
                return { isValid: false, error: 'SQL 인젝션 공격 패턴 감지' };
            }
        }
        
        // XSS 패턴 검증
        var xssPatterns = [
            /<script[^>]*>.*?<\/script>/gi,
            /<iframe[^>]*>.*?<\/iframe>/gi,
            /<object[^>]*>.*?<\/object>/gi,
            /<embed[^>]*>.*?<\/embed>/gi,
            /<link[^>]*>.*?<\/link>/gi,
            /<meta[^>]*>.*?<\/meta>/gi,
            /javascript:/gi,
            /vbscript:/gi,
            /onload\s*=/gi,
            /onerror\s*=/gi,
            /onclick\s*=/gi
        ];
        
        for (var i = 0; i < xssPatterns.length; i++) {
            if (xssPatterns[i].test(topic) || xssPatterns[i].test(payload)) {
                return { isValid: false, error: 'XSS 공격 패턴 감지' };
            }
        }
        
        // 특수 문자 검증
        var dangerousChars = ['<', '>', '"', "'", '&', ';', '(', ')', '{', '}', '[', ']'];
        for (var i = 0; i < dangerousChars.length; i++) {
            if (topic.includes(dangerousChars[i]) || payload.includes(dangerousChars[i])) {
                // JSON에서는 일부 특수 문자가 허용되므로 더 정교한 검증 필요
                if (dangerousChars[i] === '<' || dangerousChars[i] === '>') {
                    return { isValid: false, error: '위험한 특수 문자 감지: ' + dangerousChars[i] };
                }
            }
        }
        
        return { isValid: true };
    }
    
    /**
     * 검증 통계 반환
     * @returns {Object} 검증 통계
     */
    function getValidationStats() {
        return {
            totalMessages: validationStats.totalMessages,
            validMessages: validationStats.validMessages,
            invalidMessages: validationStats.invalidMessages,
            successRate: validationStats.totalMessages > 0 ? 
                (validationStats.validMessages / validationStats.totalMessages * 100).toFixed(2) + '%' : '0%',
            errors: validationStats.errors
        };
    }
    
    /**
     * 검증 통계 초기화
     */
    function resetValidationStats() {
        validationStats = {
            totalMessages: 0,
            validMessages: 0,
            invalidMessages: 0,
            errors: {
                invalidTopic: 0,
                invalidJson: 0,
                invalidActcode: 0,
                invalidSensorValue: 0,
                securityThreat: 0,
                other: 0
            }
        };
    }
    
    /**
     * 검증 통계 로깅
     */
    function logValidationStats() {
        var stats = getValidationStats();
        console.log('=== MQTT 메시지 검증 통계 ===');
        console.log('총 메시지 수:', stats.totalMessages);
        console.log('유효한 메시지:', stats.validMessages);
        console.log('무효한 메시지:', stats.invalidMessages);
        console.log('성공률:', stats.successRate);
        console.log('에러 유형별 통계:');
        for (var errorType in stats.errors) {
            if (stats.errors[errorType] > 0) {
                console.log('  ' + errorType + ':', stats.errors[errorType]);
            }
        }
        console.log('===============================');
    }
    
    // 공개 API
    return {
        validateMessage: validateMessage,
        getValidationStats: getValidationStats,
        resetValidationStats: resetValidationStats,
        logValidationStats: logValidationStats
    };
})();

// 전역 함수로도 사용 가능하도록 설정
window.MqttMessageValidator = MqttMessageValidator;
