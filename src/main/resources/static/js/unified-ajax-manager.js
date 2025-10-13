/**
 * 통합 AJAX 관리자
 * HnT Sensor API 프로젝트 전용
 * 기존 ajax-session-manager.js, session-utils.js, common-utils.js의 AJAX 함수들을 통합
 */

// ============================================================================
// 통합 AJAX 관리자 객체
// ============================================================================

var UnifiedAjaxManager = {
    
    // 기본 설정
    defaults: {
        timeout: 30000,
        cache: false,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8'
    },
    
    // 중복 리다이렉트 방지
    isRedirecting: false,
    
    // ============================================================================
    // AJAX 요청 메서드
    // ============================================================================
    
    /**
     * AJAX 요청 실행
     * @param {object} options - AJAX 옵션
     * @returns {jQuery.Deferred} jQuery Deferred 객체
     */
    ajax: function(options) {
        // 기본 설정과 사용자 옵션 병합
        var finalOptions = $.extend({}, this.defaults, options);
        
        // 세션 검증
        if (!this.validateSessionBeforeAjax(finalOptions)) {
            return $.Deferred().reject('세션 검증 실패');
        }
        
        // 에러 처리 함수 추가
        var originalError = finalOptions.error;
        finalOptions.error = function(xhr, status, error) {
            // 세션 만료 처리
            if (UnifiedAjaxManager.handleSessionExpiry(xhr, status, error)) {
                return;
            }
            
            // 원본 에러 처리 함수 실행
            if (typeof originalError === 'function') {
                originalError(xhr, status, error);
            } else {
                // 기본 에러 처리
                UnifiedAjaxManager.handleAjaxError(xhr, status, error);
            }
        };
        
        // AJAX 요청 실행
        return $.ajax(finalOptions);
    },
    
    /**
     * GET 요청
     * @param {string} url - 요청 URL
     * @param {object} data - 요청 데이터
     * @param {object} options - 추가 옵션
     * @returns {jQuery.Deferred} jQuery Deferred 객체
     */
    get: function(url, data, options) {
        var ajaxOptions = $.extend({
            url: url,
            type: 'GET',
            data: data
        }, options || {});
        
        return this.ajax(ajaxOptions);
    },
    
    /**
     * POST 요청
     * @param {string} url - 요청 URL
     * @param {object} data - 요청 데이터
     * @param {object} options - 추가 옵션
     * @returns {jQuery.Deferred} jQuery Deferred 객체
     */
    post: function(url, data, options) {
        var ajaxOptions = $.extend({
            url: url,
            type: 'POST',
            data: data
        }, options || {});
        
        return this.ajax(ajaxOptions);
    },
    
    /**
     * PUT 요청
     * @param {string} url - 요청 URL
     * @param {object} data - 요청 데이터
     * @param {object} options - 추가 옵션
     * @returns {jQuery.Deferred} jQuery Deferred 객체
     */
    put: function(url, data, options) {
        var ajaxOptions = $.extend({
            url: url,
            type: 'PUT',
            data: data
        }, options || {});
        
        return this.ajax(ajaxOptions);
    },
    
    /**
     * DELETE 요청
     * @param {string} url - 요청 URL
     * @param {object} data - 요청 데이터
     * @param {object} options - 추가 옵션
     * @returns {jQuery.Deferred} jQuery Deferred 객체
     */
    delete: function(url, data, options) {
        var ajaxOptions = $.extend({
            url: url,
            type: 'DELETE',
            data: data
        }, options || {});
        
        return this.ajax(ajaxOptions);
    },
    
    // ============================================================================
    // 세션 관리 메서드
    // ============================================================================
    
    /**
     * AJAX 요청 전 세션 검증
     * @param {object} options - AJAX 옵션
     * @returns {boolean} 검증 결과
     */
    validateSessionBeforeAjax: function(options) {
        // 임시로 세션 만료 체크 비활성화 (디버깅용)
        console.warn('=== 세션 만료 체크 비활성화 (디버깅용) ===');
        /*
        if (this.isSessionExpired()) {
            this.errorLog('세션이 만료되었습니다.');
            this.showError('세션이 만료되었습니다. 다시 로그인해주세요.', function() {
                window.location.href = '/login/login';
            });
            return false;
        }
        */
        
        // 사용자 정보 검증
        if (!this.validateUserInfo()) {
            return false;
        }
        
        return true;
    },
    
    /**
     * 세션 만료 여부 확인
     * @returns {boolean} 세션 만료 여부
     */
    isSessionExpired: function() {
        var userId = this.getUserId();
        var userGrade = this.getUserGrade();
        
        return !userId || !userGrade;
    },
    
    /**
     * 사용자 정보 검증
     * @returns {boolean} 검증 결과
     */
    validateUserInfo: function() {
        var userId = this.getUserId();
        var userGrade = this.getUserGrade();
        
        // 임시로 사용자 ID 검증 비활성화 (디버깅용)
        console.warn('=== 사용자 ID 검증 비활성화 (디버깅용) ===');
        /*
        if (!userId) {
            this.errorLog('사용자 ID가 없습니다.');
            this.showError('세션이 만료되었습니다. 다시 로그인해주세요.', function() {
                window.location.href = '/login/login';
            });
            return false;
        }
        */
        
        // 임시로 사용자 등급 검증 비활성화 (디버깅용)
        console.warn('=== 사용자 등급 검증 비활성화 (디버깅용) ===');
        /*
        if (!userGrade) {
            this.errorLog('사용자 등급이 없습니다.');
            this.showError('사용자 정보가 올바르지 않습니다. 다시 로그인해주세요.', function() {
                window.location.href = '/login/login';
            });
            return false;
        }
        */
        
        return true;
    },
    
    /**
     * 세션 만료 처리
     * @param {object} xhr - XMLHttpRequest 객체
     * @param {string} status - 상태
     * @param {string} error - 에러 메시지
     * @returns {boolean} 세션 만료 여부
     */
    handleSessionExpiry: function(xhr, status, error) {
        if (xhr.status === 401 || xhr.status === 403) {
            this.errorLog('세션 만료 감지', {status: xhr.status, error: error});
            
            // 임시로 세션 만료 리다이렉트 비활성화 (디버깅용)
            console.warn('=== 세션 만료 리다이렉트 비활성화 (디버깅용) ===');
            /*
            if (!this.isRedirecting) {
                this.isRedirecting = true;
                this.showError('세션이 만료되었습니다. 다시 로그인해주세요.', function() {
                    window.location.href = '/login/login';
                });
            }
            */
            
            return true;
        }
        
        return false;
    },
    
    /**
     * AJAX 에러 처리
     * @param {object} xhr - XMLHttpRequest 객체
     * @param {string} status - 상태
     * @param {string} error - 에러 메시지
     */
    handleAjaxError: function(xhr, status, error) {
        this.errorLog('AJAX 요청 실패', {status: status, error: error, xhr: xhr});
        
        var errorMessage = '요청 처리 중 오류가 발생했습니다.';
        
        if (xhr.status === 0) {
            errorMessage = '네트워크 연결을 확인해주세요.';
        } else if (xhr.status === 404) {
            errorMessage = '요청한 페이지를 찾을 수 없습니다.';
        } else if (xhr.status === 500) {
            errorMessage = '서버 내부 오류가 발생했습니다.';
        } else if (xhr.status === 503) {
            errorMessage = '서비스가 일시적으로 사용할 수 없습니다.';
        }
        
        this.showError(errorMessage);
    },
    
    // ============================================================================
    // 유틸리티 메서드
    // ============================================================================
    
    /**
     * 사용자 ID 조회
     * @returns {string} 사용자 ID
     */
    getUserId: function() {
        return this.getValueById('userId') || this.getValueById('loginUserId');
    },
    
    /**
     * 사용자 등급 조회
     * @returns {string} 사용자 등급
     */
    getUserGrade: function() {
        return this.getValueById('userGrade');
    },
    
    /**
     * 사용자 이름 조회
     * @returns {string} 사용자 이름
     */
    getUserNm: function() {
        return this.getValueById('userNm');
    },
    
    /**
     * ID로 값 조회
     * @param {string} id - 요소 ID
     * @returns {string} 값
     */
    getValueById: function(id) {
        var element = document.getElementById(id);
        return element ? element.value : null;
    },
    
    /**
     * 에러 로그 출력
     * @param {string} message - 메시지
     * @param {any} data - 추가 데이터
     */
    errorLog: function(message, data) {
        if (typeof data !== 'undefined') {
            console.error('[ERROR] ' + message, data);
        } else {
            console.error('[ERROR] ' + message);
        }
    },
    
    /**
     * 에러 메시지 표시
     * @param {string} message - 메시지
     * @param {function} callback - 콜백 함수
     */
    showError: function(message, callback) {
        if (typeof callback === 'function') {
            alert(message);
            callback();
        } else {
            alert(message);
        }
    },
    
    /**
     * 성공 메시지 표시
     * @param {string} message - 메시지
     */
    showSuccess: function(message) {
        alert(message);
    },
    
    /**
     * 경고 메시지 표시
     * @param {string} message - 메시지
     */
    showWarning: function(message) {
        alert(message);
    },
    
    /**
     * 정보 메시지 표시
     * @param {string} message - 메시지
     */
    showInfo: function(message) {
        alert(message);
    }
};

// ============================================================================
// 전역 함수로 노출 (기존 코드 호환성)
// ============================================================================

/**
 * AJAX 요청 실행 (전역 함수)
 * @param {object} options - AJAX 옵션
 * @returns {jQuery.Deferred} jQuery Deferred 객체
 */
function ajaxRequest(options) {
    return UnifiedAjaxManager.ajax(options);
}

/**
 * GET 요청 (전역 함수)
 * @param {string} url - 요청 URL
 * @param {object} data - 요청 데이터
 * @param {object} options - 추가 옵션
 * @returns {jQuery.Deferred} jQuery Deferred 객체
 */
function ajaxGet(url, data, options) {
    return UnifiedAjaxManager.get(url, data, options);
}

/**
 * POST 요청 (전역 함수)
 * @param {string} url - 요청 URL
 * @param {object} data - 요청 데이터
 * @param {object} options - 추가 옵션
 * @returns {jQuery.Deferred} jQuery Deferred 객체
 */
function ajaxPost(url, data, options) {
    return UnifiedAjaxManager.post(url, data, options);
}

/**
 * PUT 요청 (전역 함수)
 * @param {string} url - 요청 URL
 * @param {object} data - 요청 데이터
 * @param {object} options - 추가 옵션
 * @returns {jQuery.Deferred} jQuery Deferred 객체
 */
function ajaxPut(url, data, options) {
    return UnifiedAjaxManager.put(url, data, options);
}

/**
 * DELETE 요청 (전역 함수)
 * @param {string} url - 요청 URL
 * @param {object} data - 요청 데이터
 * @param {object} options - 추가 옵션
 * @returns {jQuery.Deferred} jQuery Deferred 객체
 */
function ajaxDelete(url, data, options) {
    return UnifiedAjaxManager.delete(url, data, options);
}

/**
 * 사용자 정보 검증 (전역 함수)
 * @returns {boolean} 검증 결과
 */
function validateUserInfo() {
    return UnifiedAjaxManager.validateUserInfo();
}

/**
 * 세션 만료 여부 확인 (전역 함수)
 * @returns {boolean} 세션 만료 여부
 */
function isSessionExpired() {
    return UnifiedAjaxManager.isSessionExpired();
}

// ============================================================================
// 초기화
// ============================================================================

/**
 * 통합 AJAX 관리자 초기화
 */
function initializeUnifiedAjaxManager() {
    console.log('통합 AJAX 관리자 초기화 완료');
}

// 페이지 로드 시 초기화
$(document).ready(function() {
    initializeUnifiedAjaxManager();
});
