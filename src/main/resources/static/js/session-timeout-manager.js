/**
 * 세션 타임아웃 관리자
 * HnT Sensor API 프로젝트 전용
 * 
 * 주요 기능:
 * - 세션 상태 실시간 모니터링
 * - 세션 만료 전 경고 표시
 * - 세션 만료 시 자동 로그아웃 처리
 * - AJAX 요청 시 세션 만료 감지
 */

var SessionTimeoutManager = (function() {
    'use strict';
    
    // 세션 상태 관리 (성능 최적화)
    var sessionState = {
        isActive: true,
        lastActivity: Date.now(),
        warningShown: false,
        checkInterval: null,
        warningInterval: null,
        isInitialized: false
    };
    
    // 설정
    var config = {
        sessionTimeout: 30 * 60 * 1000,        // 30분 (밀리초)
        warningTime: 5 * 60 * 1000,            // 5분 전 경고 (밀리초)
        checkInterval: 60 * 1000,              // 1분마다 체크 (밀리초)
        warningInterval: 30 * 1000,            // 30초마다 경고 체크 (밀리초)
        activityEvents: ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click']
    };
    
    /**
     * 세션 타임아웃 관리자 초기화 (성능 최적화)
     */
    function init() {
        // 중복 초기화 방지
        if (sessionState.isInitialized) {
            console.log('세션 타임아웃 관리자는 이미 초기화됨');
            return;
        }
        
        console.log('세션 타임아웃 관리자 초기화 (최적화 버전)');
        
        // 사용자 활동 이벤트 리스너 등록 (이벤트 위임 사용)
        registerActivityListeners();
        
        // 정기적인 세션 상태 체크 시작
        startSessionCheck();
        
        // AJAX 요청 인터셉터 설정
        setupAjaxInterceptor();
        
        sessionState.isInitialized = true;
        console.log('세션 타임아웃 관리자 초기화 완료');
    }
    
    /**
     * 사용자 활동 이벤트 리스너 등록 (이벤트 위임 최적화)
     */
    function registerActivityListeners() {
        // 이벤트 위임을 사용하여 성능 최적화
        var throttledUpdate = throttle(updateLastActivity, 1000); // 1초마다 최대 1회 실행
        
        // 모든 활동 이벤트를 하나의 리스너로 처리
        document.addEventListener('click', throttledUpdate, true);
        document.addEventListener('keypress', throttledUpdate, true);
        document.addEventListener('mousemove', throttledUpdate, true);
        document.addEventListener('scroll', throttledUpdate, true);
        
        // 페이지 가시성 변경 이벤트
        document.addEventListener('visibilitychange', function() {
            if (!document.hidden) {
                updateLastActivity();
            }
        });
        
        // 윈도우 포커스 이벤트
        window.addEventListener('focus', function() {
            updateLastActivity();
        });
    }
    
    /**
     * 함수 실행을 제한하는 throttle 함수
     */
    function throttle(func, limit) {
        var inThrottle;
        return function() {
            var args = arguments;
            var context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(function() {
                    inThrottle = false;
                }, limit);
            }
        };
    }
    
    /**
     * 마지막 활동 시간 업데이트
     */
    function updateLastActivity() {
        sessionState.lastActivity = Date.now();
        sessionState.warningShown = false;
        
        // 경고 메시지가 표시되어 있다면 제거
        hideWarningMessage();
        
        console.log('사용자 활동 감지 - 세션 갱신');
    }
    
    /**
     * 세션 상태 정기 체크 시작
     */
    function startSessionCheck() {
        if (sessionState.checkInterval) {
            clearInterval(sessionState.checkInterval);
        }
        
        sessionState.checkInterval = setInterval(function() {
            checkSessionStatus();
        }, config.checkInterval);
        
        console.log('세션 상태 체크 시작 - 간격:', config.checkInterval + 'ms');
    }
    
    /**
     * 세션 상태 체크
     */
    function checkSessionStatus() {
        var currentTime = Date.now();
        var timeSinceLastActivity = currentTime - sessionState.lastActivity;
        var timeUntilTimeout = config.sessionTimeout - timeSinceLastActivity;
        
        console.log('세션 상태 체크 - 마지막 활동:', timeSinceLastActivity + 'ms 전, 남은 시간:', timeUntilTimeout + 'ms');
        
        // 세션 만료 체크
        if (timeSinceLastActivity >= config.sessionTimeout) {
            console.warn('세션 만료 감지');
            handleSessionTimeout();
            return;
        }
        
        // 경고 표시 체크
        if (timeUntilTimeout <= config.warningTime && !sessionState.warningShown) {
            console.warn('세션 만료 경고 표시');
            showWarningMessage(timeUntilTimeout);
            sessionState.warningShown = true;
        }
    }
    
    /**
     * 세션 만료 경고 메시지 표시
     */
    function showWarningMessage(timeUntilTimeout) {
        var minutes = Math.ceil(timeUntilTimeout / (60 * 1000));
        
        // 기존 경고 메시지 제거
        hideWarningMessage();
        
        // 경고 메시지 생성
        var warningDiv = document.createElement('div');
        warningDiv.id = 'session-timeout-warning';
        warningDiv.style.cssText = `
            position: fixed;
            top: 20px;
            left: 50%;
            transform: translateX(-50%);
            background: #ff9800;
            color: white;
            padding: 15px 25px;
            border-radius: 8px;
            z-index: 10001;
            font-family: Arial, sans-serif;
            font-size: 14px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.3);
            text-align: center;
            min-width: 300px;
            animation: slideDown 0.3s ease-out;
        `;
        
        warningDiv.innerHTML = `
            <div style="font-weight: bold; margin-bottom: 8px;">
                <i class="fas fa-exclamation-triangle" style="margin-right: 8px;"></i>
                세션 만료 경고
            </div>
            <div style="margin-bottom: 12px;">
                세션이 ${minutes}분 후에 만료됩니다.<br>
                계속 사용하려면 아무 키나 누르거나 마우스를 움직여주세요.
            </div>
            <div>
                <button onclick="SessionTimeoutManager.extendSession()" style="
                    background: rgba(255,255,255,0.2);
                    border: 1px solid rgba(255,255,255,0.3);
                    color: white;
                    padding: 8px 16px;
                    border-radius: 4px;
                    cursor: pointer;
                    margin-right: 10px;
                    font-size: 13px;
                ">세션 연장</button>
                <button onclick="SessionTimeoutManager.logout()" style="
                    background: rgba(255,255,255,0.1);
                    border: 1px solid rgba(255,255,255,0.3);
                    color: white;
                    padding: 8px 16px;
                    border-radius: 4px;
                    cursor: pointer;
                    font-size: 13px;
                ">로그아웃</button>
            </div>
        `;
        
        // CSS 애니메이션 추가
        if (!document.getElementById('session-timeout-styles')) {
            var style = document.createElement('style');
            style.id = 'session-timeout-styles';
            style.textContent = `
                @keyframes slideDown {
                    from { transform: translateX(-50%) translateY(-100%); opacity: 0; }
                    to { transform: translateX(-50%) translateY(0); opacity: 1; }
                }
            `;
            document.head.appendChild(style);
        }
        
        document.body.appendChild(warningDiv);
        
        // 자동 갱신 타이머 시작
        startWarningTimer(timeUntilTimeout);
    }
    
    /**
     * 경고 타이머 시작
     */
    function startWarningTimer(timeUntilTimeout) {
        if (sessionState.warningInterval) {
            clearInterval(sessionState.warningInterval);
        }
        
        var remainingTime = timeUntilTimeout;
        
        sessionState.warningInterval = setInterval(function() {
            remainingTime -= 1000; // 1초씩 감소
            
            if (remainingTime <= 0) {
                clearInterval(sessionState.warningInterval);
                sessionState.warningInterval = null;
                return;
            }
            
            // 남은 시간 업데이트
            var minutes = Math.ceil(remainingTime / (60 * 1000));
            var warningDiv = document.getElementById('session-timeout-warning');
            if (warningDiv) {
                var messageDiv = warningDiv.querySelector('div:nth-child(2)');
                if (messageDiv) {
                    messageDiv.innerHTML = `세션이 ${minutes}분 후에 만료됩니다.<br>계속 사용하려면 아무 키나 누르거나 마우스를 움직여주세요.`;
                }
            }
        }, 1000);
    }
    
    /**
     * 경고 메시지 숨기기
     */
    function hideWarningMessage() {
        var warningDiv = document.getElementById('session-timeout-warning');
        if (warningDiv) {
            warningDiv.remove();
        }
        
        if (sessionState.warningInterval) {
            clearInterval(sessionState.warningInterval);
            sessionState.warningInterval = null;
        }
    }
    
    /**
     * 세션 연장
     */
    function extendSession() {
        console.log('세션 연장 요청');
        
        // AJAX로 세션 연장 요청
        fetch('/login/extendSession', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(function(response) {
            if (response.ok) {
                updateLastActivity();
                hideWarningMessage();
                showSuccessMessage('세션이 연장되었습니다.');
            } else {
                throw new Error('세션 연장 실패');
            }
        })
        .catch(function(error) {
            console.error('세션 연장 실패:', error);
            showErrorMessage('세션 연장에 실패했습니다. 다시 로그인해주세요.');
        });
    }
    
    /**
     * 세션 만료 처리
     */
    function handleSessionTimeout() {
        console.warn('세션 만료 처리 시작');
        
        // MQTT 연결 해제 추가
        if (typeof UnifiedMQTTManager !== 'undefined' && 
            typeof UnifiedMQTTManager.disconnectOnLogout === 'function') {
            console.log('세션 만료로 인한 MQTT 연결 해제');
            UnifiedMQTTManager.disconnectOnLogout();
        }
        
        // 모든 타이머 정리
        if (sessionState.checkInterval) {
            clearInterval(sessionState.checkInterval);
            sessionState.checkInterval = null;
        }
        
        if (sessionState.warningInterval) {
            clearInterval(sessionState.warningInterval);
            sessionState.warningInterval = null;
        }
        
        // 경고 메시지 제거
        hideWarningMessage();
        
        // 세션 만료 메시지 표시
        showSessionExpiredMessage();
        
        // 3초 후 로그인 페이지로 이동
        setTimeout(function() {
            window.location.href = '/login?timeout=true&reason=expired';
        }, 3000);
    }
    
    /**
     * 세션 만료 메시지 표시
     */
    function showSessionExpiredMessage() {
        var expiredDiv = document.createElement('div');
        expiredDiv.id = 'session-expired-message';
        expiredDiv.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.8);
            z-index: 10002;
            display: flex;
            justify-content: center;
            align-items: center;
            font-family: Arial, sans-serif;
        `;
        
        expiredDiv.innerHTML = `
            <div style="
                background: white;
                padding: 30px;
                border-radius: 10px;
                text-align: center;
                box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                max-width: 400px;
                margin: 20px;
            ">
                <div style="font-size: 48px; color: #f44336; margin-bottom: 20px;">
                    <i class="fas fa-clock"></i>
                </div>
                <div style="font-size: 18px; font-weight: bold; margin-bottom: 15px; color: #333;">
                    세션이 만료되었습니다
                </div>
                <div style="color: #666; margin-bottom: 20px;">
                    보안을 위해 세션이 자동으로 만료되었습니다.<br>
                    잠시 후 로그인 페이지로 이동합니다.
                </div>
                <div style="color: #999; font-size: 14px;">
                    <i class="fas fa-spinner fa-spin"></i> 3초 후 자동 이동...
                </div>
            </div>
        `;
        
        document.body.appendChild(expiredDiv);
    }
    
    /**
     * AJAX 요청 인터셉터 설정
     */
    function setupAjaxInterceptor() {
        // jQuery AJAX 인터셉터
        if (typeof $ !== 'undefined') {
            $(document).ajaxComplete(function(event, xhr, settings) {
                if (xhr.status === 401 || xhr.status === 403) {
                    var responseText = xhr.responseText;
                    if (responseText && responseText.includes('세션이 만료')) {
                        console.warn('AJAX 요청에서 세션 만료 감지');
                        handleSessionTimeout();
                    }
                }
            });
        }
        
        // Fetch API 인터셉터
        var originalFetch = window.fetch;
        window.fetch = function() {
            return originalFetch.apply(this, arguments).then(function(response) {
                if (response.status === 401 || response.status === 403) {
                    return response.text().then(function(text) {
                        if (text && text.includes('세션이 만료')) {
                            console.warn('Fetch 요청에서 세션 만료 감지');
                            handleSessionTimeout();
                        }
                        return response;
                    });
                }
                return response;
            });
        };
    }
    
    /**
     * 성공 메시지 표시
     */
    function showSuccessMessage(message) {
        showMessage(message, 'success');
    }
    
    /**
     * 에러 메시지 표시
     */
    function showErrorMessage(message) {
        showMessage(message, 'error');
    }
    
    /**
     * 메시지 표시
     */
    function showMessage(message, type) {
        var color = type === 'success' ? '#4caf50' : '#f44336';
        var icon = type === 'success' ? 'check-circle' : 'exclamation-circle';
        
        var messageDiv = document.createElement('div');
        messageDiv.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: ${color};
            color: white;
            padding: 12px 20px;
            border-radius: 5px;
            z-index: 10003;
            font-family: Arial, sans-serif;
            font-size: 14px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.3);
            max-width: 300px;
        `;
        
        messageDiv.innerHTML = `
            <i class="fas fa-${icon}" style="margin-right: 8px;"></i>
            ${message}
        `;
        
        document.body.appendChild(messageDiv);
        
        // 3초 후 자동 제거
        setTimeout(function() {
            if (messageDiv.parentElement) {
                messageDiv.remove();
            }
        }, 3000);
    }
    
    /**
     * 로그아웃 처리
     */
    function logout() {
        console.log('사용자 요청 로그아웃');
        
        // 로그아웃 요청
        fetch('/login/logout', {
            method: 'GET'
        })
        .then(function() {
            window.location.href = '/login/login';
        })
        .catch(function(error) {
            console.error('로그아웃 실패:', error);
            window.location.href = '/login/login';
        });
    }
    
    /**
     * 세션 상태 정보 반환
     */
    function getSessionState() {
        return {
            isActive: sessionState.isActive,
            lastActivity: sessionState.lastActivity,
            timeSinceLastActivity: Date.now() - sessionState.lastActivity,
            timeUntilTimeout: config.sessionTimeout - (Date.now() - sessionState.lastActivity)
        };
    }
    
    /**
     * 세션 타임아웃 관리자 중지
     */
    function stop() {
        if (sessionState.checkInterval) {
            clearInterval(sessionState.checkInterval);
            sessionState.checkInterval = null;
        }
        
        if (sessionState.warningInterval) {
            clearInterval(sessionState.warningInterval);
            sessionState.warningInterval = null;
        }
        
        hideWarningMessage();
        
        console.log('세션 타임아웃 관리자 중지');
    }
    
    // 공개 API
    return {
        init: init,
        extendSession: extendSession,
        logout: logout,
        getSessionState: getSessionState,
        stop: stop
    };
})();

// 자동 초기화
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        SessionTimeoutManager.init();
    });
} else {
    SessionTimeoutManager.init();
}

// 전역 함수로도 사용 가능하도록 설정
window.SessionTimeoutManager = SessionTimeoutManager;
