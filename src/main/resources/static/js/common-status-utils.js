/**
 * 공통 상태 표시 유틸리티
 * HnT Sensor API 프로젝트
 * 
 * 주요 기능:
 * - 상태 표시등 업데이트 통일
 * - 아이콘 클래스 관리
 * - 깜빡임 방지
 * - 페이지별 호환성 유지
 */

/**
 * 상태 표시등 업데이트 (통일된 함수)
 * @param {string} elementId DOM 요소 ID
 * @param {string} status 색상 상태 ('red', 'green', 'gray')
 * @param {string} type 표시등 타입 ('status', 'comp', 'defr', 'fan', 'error')
 */
function updateStatusIndicator(elementId, status, type) {
    if (!elementId || !status || !type) {
        console.warn('updateStatusIndicator: 필수 파라미터 누락', {elementId, status, type});
        return;
    }
    
    var iconClass = getIconClass(type);
    var element = document.getElementById(elementId);
    
    if (!element) {
        console.warn('updateStatusIndicator: DOM 요소를 찾을 수 없음:', elementId);
        return;
    }
    
    // 깜빡임 방지: 현재 상태와 다른 경우에만 업데이트
    if (!element.classList.contains(status)) {
        element.innerHTML = '<i class="bi ' + iconClass + '"></i>';
        element.className = 'status-indicator ' + status;
        
        console.log('상태 표시등 업데이트:', elementId, status, type);
    }
}

/**
 * 아이콘 클래스 반환
 * @param {string} type 표시등 타입
 * @returns {string} Bootstrap 아이콘 클래스
 */
function getIconClass(type) {
    var iconMap = {
        'status': 'bi-play-circle-fill',      // 운전 상태
        'comp': 'bi-gear-fill',               // 콤프
        'defr': 'bi-snow',                    // 제상 (Cooler)
        'defr-heater': 'bi-thermometer-half', // 히터 (Heater)
        'fan': 'bi-fan',                      // FAN
        'error': 'bi-exclamation-triangle-fill' // 이상
    };
    
    return iconMap[type] || 'bi-circle';
}

/**
 * 장치 종류에 따른 제상 아이콘 반환
 * @param {string} deviceType 장치 종류 ('0': Cooler, '1': Heater)
 * @returns {string} 아이콘 클래스
 */
function getDefrostIconClass(deviceType) {
    return deviceType === '1' ? 'bi-thermometer-half' : 'bi-snow';
}

/**
 * 장치 종류에 따른 제상 라벨 반환
 * @param {string} deviceType 장치 종류 ('0': Cooler, '1': Heater)
 * @returns {string} 라벨 텍스트
 */
function getDefrostLabel(deviceType) {
    return deviceType === '1' ? '히터' : '제상';
}

/**
 * 상태 표시등 초기화
 * @param {string} elementId DOM 요소 ID
 * @param {string} type 표시등 타입
 */
function initializeStatusIndicator(elementId, type) {
    updateStatusIndicator(elementId, 'gray', type);
}

/**
 * 다중 장치용 상태 표시등 업데이트
 * @param {string} uuid 장치 UUID
 * @param {string} statusType 상태 타입 ('status', 'comp', 'defr', 'fan', 'error')
 * @param {string} color 색상 ('red', 'green', 'gray')
 */
function updateStatusIndicatorMulti(uuid, statusType, color) {
    var elementId = statusType + uuid;
    updateStatusIndicator(elementId, color, statusType);
}

/**
 * 단일 장치용 상태 표시등 업데이트 (고정 ID)
 * @param {string} statusType 상태 타입 ('status', 'comp', 'defr', 'fan', 'error')
 * @param {string} color 색상 ('red', 'green', 'gray')
 */
function updateStatusIndicatorSingle(statusType, color) {
    var elementId = statusType + 'Chart'; // chart.jsp용 고정 ID
    updateStatusIndicator(elementId, color, statusType);
}

/**
 * 센서 설정 페이지용 상태 표시등 업데이트
 * @param {string} uuid 장치 UUID
 * @param {string} statusType 상태 타입
 * @param {string} color 색상
 */
function updateStatusIndicatorSensorSetting(uuid, statusType, color) {
    var elementId = statusType + uuid;
    updateStatusIndicator(elementId, color, statusType);
}

/**
 * 장치 종류에 따른 제상 표시등 업데이트
 * @param {string} elementId DOM 요소 ID
 * @param {string} status 색상 상태
 * @param {string} deviceType 장치 종류
 */
function updateDefrostIndicator(elementId, status, deviceType) {
    var iconClass = getDefrostIconClass(deviceType);
    var element = document.getElementById(elementId);
    
    if (!element) {
        console.warn('updateDefrostIndicator: DOM 요소를 찾을 수 없음:', elementId);
        return;
    }
    
    // 깜빡임 방지
    if (!element.classList.contains(status)) {
        element.innerHTML = '<i class="bi ' + iconClass + '"></i>';
        element.className = 'status-indicator ' + status;
        
        console.log('제상 표시등 업데이트:', elementId, status, deviceType);
    }
}

/**
 * 제상 라벨 업데이트
 * @param {string} elementId DOM 요소 ID
 * @param {string} deviceType 장치 종류
 */
function updateDefrostLabel(elementId, deviceType) {
    var element = document.getElementById(elementId);
    if (element) {
        element.textContent = getDefrostLabel(deviceType);
        console.log('제상 라벨 업데이트:', elementId, deviceType);
    }
}

// 전역 함수로 노출
window.updateStatusIndicator = updateStatusIndicator;
window.getIconClass = getIconClass;
window.getDefrostIconClass = getDefrostIconClass;
window.getDefrostLabel = getDefrostLabel;
window.initializeStatusIndicator = initializeStatusIndicator;
window.updateStatusIndicatorMulti = updateStatusIndicatorMulti;
window.updateStatusIndicatorSingle = updateStatusIndicatorSingle;
window.updateStatusIndicatorSensorSetting = updateStatusIndicatorSensorSetting;
window.updateDefrostIndicator = updateDefrostIndicator;
window.updateDefrostLabel = updateDefrostLabel;

console.log('공통 상태 표시 유틸리티 로드 완료');









