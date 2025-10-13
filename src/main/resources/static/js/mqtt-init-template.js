/**
 * 표준 MQTT 초기화 템플릿
 * HnT Sensor API 프로젝트
 * 
 * 모든 페이지에서 동일한 MQTT 초기화 순서를 보장합니다.
 * 
 * 초기화 순서:
 * 1. 변수 초기화 (즉시)
 * 2. 설정값 요청 (2초 후) - GET&type=1
 * 3. 상태값 요청 (4초 후) - GET&type=2
 * 4. 에러 체크 시작 (5초 후)
 * 5. 페이지별 추가 초기화
 */

/**
 * 표준 MQTT 초기화 함수
 * @param {Object} options 초기화 옵션
 * @param {string} options.pageName 페이지 이름 (로깅용)
 * @param {Function} options.requestSettings 설정값 요청 함수
 * @param {Function} options.requestStatus 상태값 요청 함수
 * @param {Function} options.startErrorCheck 에러 체크 시작 함수
 * @param {Function} options.initializePageSpecific 페이지별 초기화 함수 (선택적)
 */
function initializeMQTTStandard(options) {
    if (!options || !options.pageName) {
        console.error('MQTT 초기화 옵션이 올바르지 않습니다:', options);
        return;
    }
    
    var pageName = options.pageName;
    console.log('[' + pageName + '] 표준 MQTT 초기화 시작');
    
    // Phase 1: 변수 초기화 (즉시)
    console.log('[' + pageName + '] Phase 1: 변수 초기화');
    initializePageVariables();
    
    // Phase 2: 설정값 요청 (2초 후)
    setTimeout(function() {
        console.log('[' + pageName + '] Phase 2: 설정값 요청 (GET&type=1)');
        if (typeof options.requestSettings === 'function') {
            options.requestSettings();
        } else {
            console.warn('[' + pageName + '] requestSettings 함수가 정의되지 않았습니다');
        }
    }, 2000);
    
    // Phase 3: 상태값 요청 (4초 후)
    setTimeout(function() {
        console.log('[' + pageName + '] Phase 3: 상태값 요청 (GET&type=2)');
        if (typeof options.requestStatus === 'function') {
            options.requestStatus();
        } else {
            console.warn('[' + pageName + '] requestStatus 함수가 정의되지 않았습니다');
        }
    }, 4000);
    
    // Phase 4: 에러 체크 시작 (5초 후)
    setTimeout(function() {
        console.log('[' + pageName + '] Phase 4: 에러 체크 시작');
        if (typeof options.startErrorCheck === 'function') {
            options.startErrorCheck();
        } else {
            console.warn('[' + pageName + '] startErrorCheck 함수가 정의되지 않았습니다');
        }
    }, 5000);
    
    // Phase 5: 페이지별 추가 초기화
    if (typeof options.initializePageSpecific === 'function') {
        console.log('[' + pageName + '] Phase 5: 페이지별 추가 초기화');
        options.initializePageSpecific();
    }
    
    console.log('[' + pageName + '] 표준 MQTT 초기화 완료');
}

/**
 * 페이지 변수 초기화 (공통)
 */
function initializePageVariables() {
    // 에러 체크 관련 변수 초기화
    if (typeof deviceLastDataTime !== 'undefined') {
        deviceLastDataTime = Date.now();
    }
    if (typeof deviceErrorCounters !== 'undefined') {
        deviceErrorCounters = 0;
    }
    if (typeof deviceErrorStates !== 'undefined') {
        deviceErrorStates = false;
    }
    if (typeof deviceStatusStates !== 'undefined') {
        deviceStatusStates = 'gray';
    }
    if (typeof deviceErrorDisplayStates !== 'undefined') {
        deviceErrorDisplayStates = 'gray';
    }
    if (typeof deviceDinErrorStates !== 'undefined') {
        deviceDinErrorStates = false;
    }
    
    console.log('페이지 변수 초기화 완료');
}

/**
 * MQTT 초기화 완료 이벤트 리스너 등록
 * @param {Object} options 초기화 옵션
 */
function registerMQTTInitialization(options) {
    // MQTT 초기화 완료 이벤트 리스너
    document.addEventListener('mqtt:initialization-complete', function(event) {
        console.log('[' + (options.pageName || 'Unknown') + '] MQTT 초기화 완료 이벤트 수신');
        initializeMQTTStandard(options);
    });
    
    // 페이지 로딩 완료 시에도 초기화 (MQTT 이벤트가 없는 경우 대비)
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            console.log('[' + (options.pageName || 'Unknown') + '] DOM 로딩 완료 - MQTT 초기화 대기');
        });
    } else {
        console.log('[' + (options.pageName || 'Unknown') + '] DOM 이미 로딩 완료 - MQTT 초기화 대기');
    }
}

/**
 * 페이지 이름 가져오기 (URL 기반)
 * @returns {string} 페이지 이름
 */
function getPageName() {
    var path = window.location.pathname;
    var pageName = 'Unknown';
    
    if (path.includes('/main/main')) {
        pageName = 'Main';
    } else if (path.includes('/admin/sensorSetting')) {
        pageName = 'SensorSetting';
    } else if (path.includes('/chart/chart')) {
        pageName = 'Chart';
    } else if (path.includes('/admin/userList')) {
        pageName = 'UserList';
    } else if (path.includes('/login/login')) {
        pageName = 'Login';
    }
    
    return pageName;
}

// 전역 함수로 노출
window.initializeMQTTStandard = initializeMQTTStandard;
window.initializePageVariables = initializePageVariables;
window.registerMQTTInitialization = registerMQTTInitialization;
window.getPageName = getPageName;

console.log('표준 MQTT 초기화 템플릿 로드 완료');



