/**
 * 통일된 세션 관리 JavaScript 모듈
 * 모든 페이지에서 일관된 방식으로 세션 정보를 처리
 */
var SessionManager = (function() {
    'use strict';
    
    // 전역 변수
    var currentUserId = null;
    var currentUserGrade = null;
    var currentUserNm = null;
    var currentSensorId = null;
    var currentToken = null;
    
    /**
     * 세션 정보 초기화
     * 페이지 로드 시 호출
     */
    function initialize() {
        console.log('=== SessionManager 초기화 시작 ===');
        
        // DOM에서 세션 정보 읽기
        currentUserId = getValueFromDOM('userId');
        currentUserGrade = getValueFromDOM('userGrade');
        currentUserNm = getValueFromDOM('userNm');
        currentSensorId = getValueFromDOM('sensorId');
        currentToken = getValueFromDOM('token');
        
        // 전역 변수로 설정
        window.currentUserId = currentUserId;
        window.currentUserGrade = currentUserGrade;
        window.currentUserNm = currentUserNm;
        window.currentSensorId = currentSensorId;
        window.currentToken = currentToken;
        
        console.log('SessionManager 초기화 완료:', {
            userId: currentUserId,
            userGrade: currentUserGrade,
            userNm: currentUserNm,
            sensorId: currentSensorId,
            token: currentToken
        });
        
        // 세션 정보 유효성 검증
        if (!isValidSession()) {
            console.warn('세션 정보가 유효하지 않습니다. 로그인 페이지로 이동합니다.');
            redirectToLogin();
            return false;
        }
        
        return true;
    }
    
    /**
     * DOM에서 값 읽기
     * @param {string} elementId DOM 요소 ID
     * @returns {string|null} 값 또는 null
     */
    function getValueFromDOM(elementId) {
        var element = document.getElementById(elementId);
        if (element && element.value) {
            return element.value.trim();
        }
        return null;
    }
    
    /**
     * 세션 정보 유효성 검증
     * @returns {boolean} 유효성 여부
     */
    function isValidSession() {
        return currentUserId && currentUserId !== '' && 
               currentUserGrade && currentUserGrade !== '';
    }
    
    /**
     * 현재 사용자 ID 반환
     * @returns {string|null} 사용자 ID
     */
    function getCurrentUserId() {
        return currentUserId;
    }
    
    /**
     * 현재 사용자 등급 반환
     * @returns {string|null} 사용자 등급
     */
    function getCurrentUserGrade() {
        return currentUserGrade;
    }
    
    /**
     * 현재 사용자 이름 반환
     * @returns {string|null} 사용자 이름
     */
    function getCurrentUserNm() {
        return currentUserNm;
    }
    
    /**
     * 현재 센서 ID 반환
     * @returns {string|null} 센서 ID
     */
    function getCurrentSensorId() {
        return currentSensorId;
    }
    
    /**
     * 현재 토큰 반환
     * @returns {string|null} 토큰
     */
    function getCurrentToken() {
        return currentToken;
    }
    
    /**
     * MQTT 토픽 생성
     * @param {string} sensorUuid 센서 UUID
     * @returns {string} MQTT 토픽
     */
    function createMqttTopic(sensorUuid) {
        if (!currentUserId || !sensorUuid) {
            console.error('MQTT 토픽 생성 실패 - userId 또는 sensorUuid가 없습니다.');
            return null;
        }
        return "HBEE/" + currentUserId + "/TC/" + sensorUuid + "/SER";
    }
    
    /**
     * 페이지 이동 시 세션 기반으로 이동 (URL 파라미터 없음)
     * @param {string} url 이동할 URL
     */
    function navigateWithSession(url) {
        if (!isValidSession()) {
            console.warn('세션 정보가 유효하지 않아 기본 URL로 이동합니다.');
            window.location.href = url;
            return;
        }
        
        // URL 파라미터 없이 세션 기반으로 이동
        console.log('세션 기반 페이지 이동:', url);
        window.location.href = url;
    }
    
    /**
     * 로그인 페이지로 리다이렉트 (임시 비활성화)
     */
    function redirectToLogin() {
        // 임시로 리다이렉트 비활성화 (디버깅용)
        console.warn('=== redirectToLogin 비활성화 (디버깅용) ===');
        /*
        window.location.href = '/login/login';
        */
    }
    
    /**
     * 세션 정보 디버깅
     */
    function debugSessionInfo() {
        console.log('=== 세션 정보 디버깅 ===');
        console.log('currentUserId:', currentUserId);
        console.log('currentUserGrade:', currentUserGrade);
        console.log('currentUserNm:', currentUserNm);
        console.log('currentSensorId:', currentSensorId);
        console.log('currentToken:', currentToken);
        console.log('isValidSession:', isValidSession());
        console.log('========================');
    }
    
    /**
     * 세션 정보 업데이트
     * @param {Object} sessionData 세션 데이터
     */
    function updateSessionInfo(sessionData) {
        if (sessionData.userId) {
            currentUserId = sessionData.userId;
            window.currentUserId = currentUserId;
        }
        if (sessionData.userGrade) {
            currentUserGrade = sessionData.userGrade;
            window.currentUserGrade = currentUserGrade;
        }
        if (sessionData.userNm) {
            currentUserNm = sessionData.userNm;
            window.currentUserNm = currentUserNm;
        }
        if (sessionData.sensorId) {
            currentSensorId = sessionData.sensorId;
            window.currentSensorId = currentSensorId;
        }
        if (sessionData.token) {
            currentToken = sessionData.token;
            window.currentToken = currentToken;
        }
        
        console.log('세션 정보 업데이트 완료:', sessionData);
    }
    
    // 공개 API
    return {
        initialize: initialize,
        getCurrentUserId: getCurrentUserId,
        getCurrentUserGrade: getCurrentUserGrade,
        getCurrentUserNm: getCurrentUserNm,
        getCurrentSensorId: getCurrentSensorId,
        getCurrentToken: getCurrentToken,
        createMqttTopic: createMqttTopic,
        navigateWithSession: navigateWithSession,
        isValidSession: isValidSession,
        debugSessionInfo: debugSessionInfo,
        updateSessionInfo: updateSessionInfo
    };
})();

// 페이지 로드 시 자동 초기화
if (typeof $ !== 'undefined') {
    $(document).ready(function() {
        SessionManager.initialize();
    });
} else {
    // jQuery가 없을 경우 DOMContentLoaded 이벤트 사용
    document.addEventListener('DOMContentLoaded', function() {
        SessionManager.initialize();
    });
}

// 전역 함수로도 사용 가능하도록 설정
window.SessionManager = SessionManager;
