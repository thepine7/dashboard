/**
 * 에러 차단 시스템
 * HnT Sensor API 프로젝트 전용
 */

// ============================================================================
// 에러 차단 시스템 설정
// ============================================================================

var ErrorBlockingSystem = {
    
    // 차단할 에러 메시지 패턴들 (임시로 모두 비활성화)
    blockedPatterns: [
        // 모든 차단 패턴을 주석 처리하여 에러 블로킹 완전 비활성화
        // /github\.com/,
        // /githubusercontent\.com/,
        // /raw\.githubusercontent\.com/,
        
        // /cdnjs\.cloudflare\.com/,
        // /unpkg\.com/,
        // /jsdelivr\.net/,
        
        // /googleapis\.com/,
        // /gstatic\.com/,
        // /google\.com/,
        // /youtube\.com/,
        // /youtube-nocookie\.com/,
        
        // /net::ERR_/,
        // /Failed to load resource/,
        // /Mixed Content/,
        // /CORS/,
        
        // /favicon\.ico/,
        // /robots\.txt/,
        // /sitemap\.xml/
    ],
    
    // 허용할 에러 메시지 패턴들 (MQTT 관련 + 세션 관련)
    allowedPatterns: [
        /MQTT/,
        /연결/,
        /호스트/,
        /포트/,
        /클라이언트/,
        /=== MQTT/,
        /연결 성공/,
        /연결 실패/,
        /연결 끊김/,
        /토픽/,
        /메시지/,
        /구독/,
        /발행/,
        // 세션 관련 디버깅 메시지 허용
        /세션/,
        /JSP/,
        /스크립틀릿/,
        /디버깅/,
        /동기화/,
        /사용자/,
        /userId/,
        /userGrade/,
        /userNm/,
        /loginUserId/,
        /sensorId/
    ],
    
    // 에러 차단 활성화 여부 (임시로 완전 비활성화)
    isEnabled: false,
    
    // 디버그 모드 (모든 에러 표시)
    debugMode: true
};

// ============================================================================
// 에러 차단 함수들
// ============================================================================

/**
 * 에러 메시지 차단 여부 확인
 * @param {string} message - 에러 메시지
 * @param {string} source - 에러 소스
 * @param {number} lineno - 라인 번호
 * @param {number} colno - 컬럼 번호
 * @param {Error} error - 에러 객체
 * @returns {boolean} 차단 여부
 */
function shouldBlockError(message, source, lineno, colno, error) {
    // 임시로 모든 에러 블로킹 완전 비활성화
    return false;
    
    // 기존 로직 (주석 처리)
    /*
    // 에러 차단 시스템이 비활성화된 경우
    if (!ErrorBlockingSystem.isEnabled) {
        return false;
    }
    
    // 디버그 모드인 경우 모든 에러 허용
    if (ErrorBlockingSystem.debugMode) {
        return false;
    }
    
    // 메시지가 없는 경우 차단
    if (!message) {
        return true;
    }
    
    // 허용 패턴 확인 (MQTT 관련 메시지는 허용)
    for (var i = 0; i < ErrorBlockingSystem.allowedPatterns.length; i++) {
        if (ErrorBlockingSystem.allowedPatterns[i].test(message)) {
            return false;
        }
    }
    
    // 차단 패턴 확인
    for (var i = 0; i < ErrorBlockingSystem.blockedPatterns.length; i++) {
        if (ErrorBlockingSystem.blockedPatterns[i].test(message)) {
            return true;
        }
    }
    
    // 소스 파일명 확인
    if (source) {
        for (var i = 0; i < ErrorBlockingSystem.blockedPatterns.length; i++) {
            if (ErrorBlockingSystem.blockedPatterns[i].test(source)) {
                return true;
            }
        }
    }
    
    return false;
    */
}

/**
 * 콘솔 에러 차단
 * @param {string} message - 에러 메시지
 * @param {string} source - 에러 소스
 * @param {number} lineno - 라인 번호
 * @param {number} colno - 컬럼 번호
 * @param {Error} error - 에러 객체
 */
function blockConsoleError(message, source, lineno, colno, error) {
    if (shouldBlockError(message, source, lineno, colno, error)) {
        // 에러 차단 (아무것도 하지 않음)
        return;
    }
    
    // 원본 console.error 호출 (첫 번째 파라미터만 전달)
    if (originalConsoleError) {
        originalConsoleError.call(console, message);
    }
}

/**
 * 콘솔 경고 차단
 * @param {string} message - 경고 메시지
 * @param {string} source - 경고 소스
 * @param {number} lineno - 라인 번호
 * @param {number} colno - 컬럼 번호
 * @param {Error} error - 에러 객체
 */
function blockConsoleWarn(message, source, lineno, colno, error) {
    if (shouldBlockError(message, source, lineno, colno, error)) {
        // 경고 차단 (아무것도 하지 않음)
        return;
    }
    
    // 원본 console.warn 호출 (첫 번째 파라미터만 전달)
    if (originalConsoleWarn) {
        originalConsoleWarn.call(console, message);
    }
}

/**
 * 콘솔 로그 차단
 * @param {string} message - 로그 메시지
 * @param {string} source - 로그 소스
 * @param {number} lineno - 라인 번호
 * @param {number} colno - 컬럼 번호
 * @param {Error} error - 에러 객체
 */
function blockConsoleLog(message, source, lineno, colno, error) {
    if (shouldBlockError(message, source, lineno, colno, error)) {
        // 로그 차단 (아무것도 하지 않음)
        return;
    }
    
    // 원본 console.log 호출 (첫 번째 파라미터만 전달)
    if (originalConsoleLog) {
        originalConsoleLog.call(console, message);
    }
}

// ============================================================================
// 원본 콘솔 함수 저장
// ============================================================================

var originalConsoleError = console.error;
var originalConsoleWarn = console.warn;
var originalConsoleLog = console.log;

// ============================================================================
// 콘솔 함수 오버라이드
// ============================================================================

// 임시로 콘솔 함수 오버라이드 비활성화
/*
console.error = function(message, source, lineno, colno, error) {
    blockConsoleError(message, source, lineno, colno, error);
};

console.warn = function(message, source, lineno, colno, error) {
    blockConsoleWarn(message, source, lineno, colno, error);
};

console.log = function(message, source, lineno, colno, error) {
    blockConsoleLog(message, source, lineno, colno, error);
};
*/

// ============================================================================
// 전역 에러 이벤트 차단
// ============================================================================

/**
 * 전역 에러 이벤트 핸들러
 * @param {Event} event - 에러 이벤트
 */
function handleGlobalError(event) {
    var message = event.message || '';
    var source = event.filename || '';
    var lineno = event.lineno || 0;
    var colno = event.colno || 0;
    var error = event.error || null;
    
    if (shouldBlockError(message, source, lineno, colno, error)) {
        // 에러 차단
        event.preventDefault();
        event.stopPropagation();
        return false;
    }
    
    // 에러 허용 (원본 동작 유지)
    return true;
}

/**
 * 전역 Promise 에러 이벤트 핸들러
 * @param {Event} event - Promise 에러 이벤트
 */
function handleUnhandledRejection(event) {
    var message = event.reason ? event.reason.toString() : '';
    var source = '';
    var lineno = 0;
    var colno = 0;
    var error = event.reason;
    
    if (shouldBlockError(message, source, lineno, colno, error)) {
        // 에러 차단
        event.preventDefault();
        return false;
    }
    
    // 에러 허용 (원본 동작 유지)
    return true;
}

// 전역 에러 이벤트 리스너 등록
window.addEventListener('error', handleGlobalError, true);
window.addEventListener('unhandledrejection', handleUnhandledRejection, true);

// ============================================================================
// 네트워크 요청 차단
// ============================================================================

/**
 * XMLHttpRequest 오버라이드
 */
(function() {
    var originalXHR = window.XMLHttpRequest;
    
    window.XMLHttpRequest = function() {
        var xhr = new originalXHR();
        var originalOpen = xhr.open;
        var originalSend = xhr.send;
        
        xhr.open = function(method, url, async, user, password) {
            // 차단할 URL 패턴 확인
            if (shouldBlockError(url, '', 0, 0, null)) {
                // 요청 차단
                xhr.readyState = 4;
                xhr.status = 0;
                xhr.statusText = 'Blocked';
                return;
            }
            
            return originalOpen.call(this, method, url, async, user, password);
        };
        
        xhr.send = function(data) {
            // 차단된 요청인 경우 전송하지 않음
            if (xhr.status === 0 && xhr.statusText === 'Blocked') {
                return;
            }
            
            return originalSend.call(this, data);
        };
        
        return xhr;
    };
})();

// ============================================================================
// 에러 차단 시스템 제어 함수들
// ============================================================================

/**
 * 에러 차단 시스템 활성화/비활성화
 * @param {boolean} enabled - 활성화 여부
 */
function setErrorBlockingEnabled(enabled) {
    ErrorBlockingSystem.isEnabled = enabled;
    debugLog('에러 차단 시스템 ' + (enabled ? '활성화' : '비활성화'));
}

/**
 * 디버그 모드 설정
 * @param {boolean} enabled - 디버그 모드 여부
 */
function setDebugMode(enabled) {
    ErrorBlockingSystem.debugMode = enabled;
    debugLog('디버그 모드 ' + (enabled ? '활성화' : '비활성화'));
}

/**
 * 차단 패턴 추가
 * @param {RegExp} pattern - 차단할 패턴
 */
function addBlockedPattern(pattern) {
    if (pattern instanceof RegExp) {
        ErrorBlockingSystem.blockedPatterns.push(pattern);
        debugLog('차단 패턴 추가', pattern);
    }
}

/**
 * 허용 패턴 추가
 * @param {RegExp} pattern - 허용할 패턴
 */
function addAllowedPattern(pattern) {
    if (pattern instanceof RegExp) {
        ErrorBlockingSystem.allowedPatterns.push(pattern);
        debugLog('허용 패턴 추가', pattern);
    }
}

// ============================================================================
// 초기화 함수
// ============================================================================

/**
 * 디버그 로그 함수 정의
 */
function debugLog(message, ...args) {
    if (typeof console !== 'undefined' && console.debug) {
        console.debug('[ErrorBlockingSystem]', message, ...args);
    }
}

/**
 * 에러 차단 시스템 초기화
 */
function initializeErrorBlockingSystem() {
    debugLog('에러 차단 시스템 초기화 완료');
    debugLog('차단 패턴 수:', ErrorBlockingSystem.blockedPatterns.length);
    debugLog('허용 패턴 수:', ErrorBlockingSystem.allowedPatterns.length);
}

// ============================================================================
// 전역 함수로 노출
// ============================================================================

window.ErrorBlockingSystem = ErrorBlockingSystem;
window.setErrorBlockingEnabled = setErrorBlockingEnabled;
window.setDebugMode = setDebugMode;
window.addBlockedPattern = addBlockedPattern;
window.addAllowedPattern = addAllowedPattern;

// ============================================================================
// 초기화
// ============================================================================

// 임시로 초기화 비활성화
/*
// 페이지 로드 시 초기화
$(document).ready(function() {
    initializeErrorBlockingSystem();
});
*/
