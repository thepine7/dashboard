/**
 * 통합 검증 관리자
 * HnT Sensor API 프로젝트 전용
 * 기존 common-utils.js, session-utils.js의 검증 함수들을 통합
 */

// ============================================================================
// 통합 검증 관리자 객체
// ============================================================================

var UnifiedValidationManager = {
    
    // ============================================================================
    // 사용자 정보 검증
    // ============================================================================
    
    /**
     * 사용자 정보 검증 (통합)
     * @returns {boolean} 검증 결과
     */
    validateUserInfo: function() {
        // 세션 만료 체크
        if (this.isSessionExpired()) {
            this.errorLog('세션이 만료되었습니다.');
            this.showError('세션이 만료되었습니다. 다시 로그인해주세요.', function() {
                window.location.href = '/login/login';
            });
            return false;
        }
        
        // 사용자 ID 검증
        var userId = this.getUserId();
        if (!userId) {
            this.errorLog('사용자 ID가 없습니다.');
            this.showError('세션이 만료되었습니다. 다시 로그인해주세요.', function() {
                window.location.href = '/login/login';
            });
            return false;
        }
        
        // 사용자 등급 검증
        var userGrade = this.getUserGrade();
        if (!userGrade) {
            this.errorLog('사용자 등급이 없습니다.');
            this.showError('사용자 정보가 올바르지 않습니다. 다시 로그인해주세요.', function() {
                window.location.href = '/login/login';
            });
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
     * 현재 사용자 정보 조회
     * @returns {object} 사용자 정보 객체
     */
    getCurrentUserInfo: function() {
        return {
            userId: this.getUserId(),
            userGrade: this.getUserGrade(),
            userNm: this.getValueById('userNm'),
            userEmail: this.getValueById('userEmail'),
            userTel: this.getValueById('userTel'),
            loginUserId: this.getValueById('loginUserId'),
            sensorId: this.getValueById('sensorId'),
            token: this.getValueById('token')
        };
    },
    
    // ============================================================================
    // 입력 데이터 검증
    // ============================================================================
    
    /**
     * 필수 필드 검증
     * @param {object} data - 검증할 데이터
     * @param {array} requiredFields - 필수 필드 목록
     * @returns {object} 검증 결과
     */
    validateRequiredFields: function(data, requiredFields) {
        var result = {
            isValid: true,
            errors: []
        };
        
        for (var i = 0; i < requiredFields.length; i++) {
            var field = requiredFields[i];
            if (!data[field] || data[field].toString().trim() === '') {
                result.isValid = false;
                result.errors.push(field + '은(는) 필수 입력 항목입니다.');
            }
        }
        
        return result;
    },
    
    /**
     * 이메일 형식 검증
     * @param {string} email - 이메일 주소
     * @returns {boolean} 검증 결과
     */
    validateEmail: function(email) {
        var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },
    
    /**
     * 전화번호 형식 검증
     * @param {string} phone - 전화번호
     * @returns {boolean} 검증 결과
     */
    validatePhone: function(phone) {
        var phoneRegex = /^[0-9-+().\s]+$/;
        return phoneRegex.test(phone);
    },
    
    /**
     * 사용자 ID 형식 검증
     * @param {string} userId - 사용자 ID
     * @returns {boolean} 검증 결과
     */
    validateUserId: function(userId) {
        // 영문, 숫자, 언더스코어만 허용, 3-20자
        var userIdRegex = /^[a-zA-Z0-9_]{3,20}$/;
        return userIdRegex.test(userId);
    },
    
    /**
     * 비밀번호 강도 검증
     * @param {string} password - 비밀번호
     * @returns {object} 검증 결과
     */
    validatePassword: function(password) {
        var result = {
            isValid: true,
            errors: []
        };
        
        if (password.length < 8) {
            result.isValid = false;
            result.errors.push('비밀번호는 8자 이상이어야 합니다.');
        }
        
        if (!/[A-Z]/.test(password)) {
            result.isValid = false;
            result.errors.push('비밀번호는 대문자를 포함해야 합니다.');
        }
        
        if (!/[a-z]/.test(password)) {
            result.isValid = false;
            result.errors.push('비밀번호는 소문자를 포함해야 합니다.');
        }
        
        if (!/[0-9]/.test(password)) {
            result.isValid = false;
            result.errors.push('비밀번호는 숫자를 포함해야 합니다.');
        }
        
        if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
            result.isValid = false;
            result.errors.push('비밀번호는 특수문자를 포함해야 합니다.');
        }
        
        return result;
    },
    
    // ============================================================================
    // 권한 검증
    // ============================================================================
    
    /**
     * 관리자 권한 확인
     * @returns {boolean} 관리자 여부
     */
    isAdmin: function() {
        var userGrade = this.getUserGrade();
        return userGrade === 'A';
    },
    
    /**
     * 일반 사용자 권한 확인
     * @returns {boolean} 일반 사용자 여부
     */
    isUser: function() {
        var userGrade = this.getUserGrade();
        return userGrade === 'U';
    },
    
    /**
     * 부계정 권한 확인
     * @returns {boolean} 부계정 여부
     */
    isSubAccount: function() {
        var userGrade = this.getUserGrade();
        return userGrade === 'B';
    },
    
    /**
     * 권한 확인
     * @param {string} requiredGrade - 필요한 권한 등급
     * @returns {boolean} 권한 여부
     */
    hasPermission: function(requiredGrade) {
        var userGrade = this.getUserGrade();
        
        if (requiredGrade === 'A') {
            return userGrade === 'A';
        } else if (requiredGrade === 'U') {
            return userGrade === 'A' || userGrade === 'U';
        } else if (requiredGrade === 'B') {
            return userGrade === 'A' || userGrade === 'U' || userGrade === 'B';
        }
        
        return false;
    },
    
    // ============================================================================
    // MQTT 관련 검증
    // ============================================================================
    
    /**
     * MQTT 토픽 형식 검증
     * @param {string} topic - MQTT 토픽
     * @returns {boolean} 검증 결과
     */
    validateMqttTopic: function(topic) {
        if (!topic || typeof topic !== 'string') {
            return false;
        }
        
        // HBEE/{userId}/{sensorType}/{sensorUuid}/DEV 형식 검증
        var topicRegex = /^HBEE\/[^\/]+\/[^\/]+\/[^\/]+\/DEV$/;
        return topicRegex.test(topic);
    },
    
    /**
     * MQTT 메시지 형식 검증
     * @param {string} message - MQTT 메시지
     * @returns {boolean} 검증 결과
     */
    validateMqttMessage: function(message) {
        try {
            var data = JSON.parse(message);
            return data.hasOwnProperty('actcode');
        } catch (e) {
            return false;
        }
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
            console.error('[VALIDATION ERROR] ' + message, data);
        } else {
            console.error('[VALIDATION ERROR] ' + message);
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
    }
};

// ============================================================================
// 전역 함수로 노출 (기존 코드 호환성)
// ============================================================================

/**
 * 사용자 정보 검증 (전역 함수)
 * @returns {boolean} 검증 결과
 */
function validateUserInfo() {
    return UnifiedValidationManager.validateUserInfo();
}

/**
 * 세션 만료 여부 확인 (전역 함수)
 * @returns {boolean} 세션 만료 여부
 */
function isSessionExpired() {
    return UnifiedValidationManager.isSessionExpired();
}

/**
 * 현재 사용자 정보 조회 (전역 함수)
 * @returns {object} 사용자 정보 객체
 */
function getCurrentUserInfo() {
    return UnifiedValidationManager.getCurrentUserInfo();
}

/**
 * 관리자 권한 확인 (전역 함수)
 * @returns {boolean} 관리자 여부
 */
function isAdmin() {
    return UnifiedValidationManager.isAdmin();
}

/**
 * 일반 사용자 권한 확인 (전역 함수)
 * @returns {boolean} 일반 사용자 여부
 */
function isUser() {
    return UnifiedValidationManager.isUser();
}

/**
 * 부계정 권한 확인 (전역 함수)
 * @returns {boolean} 부계정 여부
 */
function isSubAccount() {
    return UnifiedValidationManager.isSubAccount();
}

/**
 * 권한 확인 (전역 함수)
 * @param {string} requiredGrade - 필요한 권한 등급
 * @returns {boolean} 권한 여부
 */
function hasPermission(requiredGrade) {
    return UnifiedValidationManager.hasPermission(requiredGrade);
}

// ============================================================================
// 초기화
// ============================================================================

/**
 * 통합 검증 관리자 초기화
 */
function initializeUnifiedValidationManager() {
    console.log('통합 검증 관리자 초기화 완료');
}

// 페이지 로드 시 초기화
$(document).ready(function() {
    initializeUnifiedValidationManager();
});
