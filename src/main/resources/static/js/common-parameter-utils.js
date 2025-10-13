/**
 * 공통 파라미터 인코딩/디코딩 유틸리티
 * HnT Sensor API 프로젝트
 * 
 * 주요 기능:
 * - 파라미터 인코딩 (실제값 → 인코딩값)
 * - 파라미터 디코딩 (인코딩값 → 실제값)
 * - 음수 처리 지원
 * - 소수점 자릿수 제어
 */

/**
 * 파라미터 디코딩 함수
 * @param {string} value 인코딩된 값 (예: "100", "-100")
 * @param {number} decimals 소수점 자릿수 (기본값: 1)
 * @returns {string} 디코딩된 값 (예: "10.0", "-10.0")
 */
function decodeParameter(value, decimals) {
    if (!value || value === 'null' || value === 'undefined') {
        return "0.0";
    }
    
    decimals = decimals || 1;
    var num = parseInt(value);
    
    if (isNaN(num)) {
        console.warn('파라미터 디코딩 실패 - 유효하지 않은 숫자:', value);
        return "0.0";
    }
    
    var divisor = Math.pow(10, decimals);
    var result = (num / divisor).toFixed(decimals);
    
    console.log('파라미터 디코딩:', value, '→', result);
    return result;
}

/**
 * 파라미터 인코딩 함수
 * @param {string} value 실제값 (예: "10.0", "-10.0")
 * @param {number} decimals 소수점 자릿수 (기본값: 1)
 * @returns {string} 인코딩된 값 (예: "100", "-100")
 */
function encodeParameter(value, decimals) {
    if (!value || value === 'null' || value === 'undefined') {
        return "0";
    }
    
    decimals = decimals || 1;
    var num = parseFloat(value);
    
    if (isNaN(num)) {
        console.warn('파라미터 인코딩 실패 - 유효하지 않은 숫자:', value);
        return "0";
    }
    
    var multiplier = Math.pow(10, decimals);
    var result = Math.round(num * multiplier).toString();
    
    console.log('파라미터 인코딩:', value, '→', result);
    return result;
}

/**
 * 온도 파라미터 디코딩 (p01, p04, p10 등)
 * @param {string} value 인코딩된 온도값
 * @returns {string} 디코딩된 온도값 (소수점 1자리)
 */
function decodeTemperature(value) {
    return decodeParameter(value, 1);
}

/**
 * 온도 파라미터 인코딩 (p01, p04, p10 등)
 * @param {string} value 실제 온도값
 * @returns {string} 인코딩된 온도값
 */
function encodeTemperature(value) {
    return encodeParameter(value, 1);
}

/**
 * 시간 파라미터 디코딩 (p08, p09 등)
 * @param {string} value 인코딩된 시간값
 * @returns {string} 디코딩된 시간값 (소수점 1자리)
 */
function decodeTime(value) {
    return decodeParameter(value, 1);
}

/**
 * 시간 파라미터 인코딩 (p08, p09 등)
 * @param {string} value 실제 시간값
 * @returns {string} 인코딩된 시간값
 */
function encodeTime(value) {
    return encodeParameter(value, 1);
}

/**
 * 정수 파라미터 디코딩 (p03, p05, p06 등)
 * @param {string} value 인코딩된 정수값
 * @returns {string} 디코딩된 정수값
 */
function decodeInteger(value) {
    if (!value || value === 'null' || value === 'undefined') {
        return "0";
    }
    
    var num = parseInt(value);
    if (isNaN(num)) {
        console.warn('정수 파라미터 디코딩 실패:', value);
        return "0";
    }
    
    return num.toString();
}

/**
 * 정수 파라미터 인코딩 (p03, p05, p06 등)
 * @param {string} value 실제 정수값
 * @returns {string} 인코딩된 정수값
 */
function encodeInteger(value) {
    if (!value || value === 'null' || value === 'undefined') {
        return "0";
    }
    
    var num = parseInt(value);
    if (isNaN(num)) {
        console.warn('정수 파라미터 인코딩 실패:', value);
        return "0";
    }
    
    return num.toString();
}

/**
 * 파라미터 값 정규화 (입력값 정리)
 * @param {string} value 입력값
 * @returns {string} 정규화된 값
 */
function normalizeValue(value) {
    if (!value || value === 'null' || value === 'undefined') {
        return "0";
    }
    
    // 공백 제거
    value = value.toString().trim();
    
    // 빈 문자열 처리
    if (value === '') {
        return "0";
    }
    
    return value;
}

// 전역 함수로 노출
window.decodeParameter = decodeParameter;
window.encodeParameter = encodeParameter;
window.decodeTemperature = decodeTemperature;
window.encodeTemperature = encodeTemperature;
window.decodeTime = decodeTime;
window.encodeTime = encodeTime;
window.decodeInteger = decodeInteger;
window.encodeInteger = encodeInteger;
window.normalizeValue = normalizeValue;

console.log('공통 파라미터 유틸리티 로드 완료');



