/**
 * 부계정 권한 제한 JavaScript 유틸리티
 * HnT Sensor API 프로젝트 전용
 * 
 * 부계정(B 등급) 사용자의 UI 권한을 강화하고
 * 읽기 전용 권한을 엄격하게 적용
 */

// 부계정 권한 제한 관리자
var SubAccountPermissionManager = {
    
    // 부계정 제한 작업 목록
    RESTRICTED_OPERATIONS: [
        'CREATE_USER',      // 사용자 생성
        'DELETE_USER',      // 사용자 삭제
        'UPDATE_USER',      // 사용자 수정
        'CREATE_SENSOR',    // 센서 생성
        'DELETE_SENSOR',    // 센서 삭제
        'UPDATE_SENSOR',    // 센서 수정
        'UPDATE_SENSOR_SETTING', // 센서 설정 변경
        'CREATE_SUB_ACCOUNT', // 부계정 생성
        'DELETE_SUB_ACCOUNT', // 부계정 삭제
        'UPDATE_SUB_ACCOUNT', // 부계정 수정
        'ADMIN_OPERATIONS', // 관리자 작업
        'SYSTEM_SETTINGS'   // 시스템 설정
    ],
    
    // 부계정 허용 작업 목록
    ALLOWED_OPERATIONS: [
        'READ_SENSOR_DATA',     // 센서 데이터 조회
        'READ_USER_INFO',       // 사용자 정보 조회
        'READ_SENSOR_LIST',     // 센서 목록 조회
        'READ_CHART_DATA',      // 차트 데이터 조회
        'READ_ALARM_DATA',      // 알람 데이터 조회
        'EXPORT_DATA',          // 데이터 내보내기
        'VIEW_DASHBOARD'        // 대시보드 조회
    ],
    
    /**
     * 현재 사용자가 부계정인지 확인
     * @return {boolean} 부계정 여부
     */
    isSubAccount: function() {
        var userGrade = this.getUserGrade();
        return userGrade === 'B';
    },
    
    /**
     * 현재 사용자 등급 조회
     * @return {string} 사용자 등급
     */
    getUserGrade: function() {
        var userGradeElement = document.getElementById('userGrade');
        return userGradeElement ? userGradeElement.value : '';
    },
    
    /**
     * 현재 사용자 ID 조회
     * @return {string} 사용자 ID
     */
    getUserId: function() {
        var userIdElement = document.getElementById('userId');
        return userIdElement ? userIdElement.value : '';
    },
    
    /**
     * 작업 권한 검증
     * @param {string} operation 수행하려는 작업
     * @return {boolean} 권한 여부
     */
    validateOperation: function(operation) {
        if (!this.isSubAccount()) {
            return true; // 부계정이 아닌 경우 통과
        }
        
        // 제한된 작업인지 확인
        for (var i = 0; i < this.RESTRICTED_OPERATIONS.length; i++) {
            if (operation === this.RESTRICTED_OPERATIONS[i]) {
                console.warn('부계정 권한 제한 - 작업:', operation, '사용자:', this.getUserId());
                return false;
            }
        }
        
        // 허용된 작업인지 확인
        for (var i = 0; i < this.ALLOWED_OPERATIONS.length; i++) {
            if (operation === this.ALLOWED_OPERATIONS[i]) {
                console.debug('부계정 권한 허용 - 작업:', operation, '사용자:', this.getUserId());
                return true;
            }
        }
        
        // 알 수 없는 작업은 기본적으로 제한
        console.warn('알 수 없는 작업 - 부계정 권한 제한:', operation, '사용자:', this.getUserId());
        return false;
    },
    
    /**
     * 작업 설명 반환
     * @param {string} operation 작업명
     * @return {string} 작업 설명
     */
    getOperationDescription: function(operation) {
        var descriptions = {
            'CREATE_USER': '사용자 생성',
            'DELETE_USER': '사용자 삭제',
            'UPDATE_USER': '사용자 수정',
            'CREATE_SENSOR': '센서 생성',
            'DELETE_SENSOR': '센서 삭제',
            'UPDATE_SENSOR': '센서 수정',
            'UPDATE_SENSOR_SETTING': '센서 설정 변경',
            'CREATE_SUB_ACCOUNT': '부계정 생성',
            'DELETE_SUB_ACCOUNT': '부계정 삭제',
            'UPDATE_SUB_ACCOUNT': '부계정 수정',
            'ADMIN_OPERATIONS': '관리자 작업',
            'SYSTEM_SETTINGS': '시스템 설정',
            'READ_SENSOR_DATA': '센서 데이터 조회',
            'READ_USER_INFO': '사용자 정보 조회',
            'READ_SENSOR_LIST': '센서 목록 조회',
            'READ_CHART_DATA': '차트 데이터 조회',
            'READ_ALARM_DATA': '알람 데이터 조회',
            'EXPORT_DATA': '데이터 내보내기',
            'VIEW_DASHBOARD': '대시보드 조회'
        };
        
        return descriptions[operation] || operation;
    },
    
    /**
     * 권한 제한 경고 메시지 표시
     * @param {string} operation 수행하려던 작업
     */
    showPermissionDeniedWarning: function(operation) {
        var message = '부계정은 ' + this.getOperationDescription(operation) + ' 권한이 없습니다.';
        
        if (typeof showWarning === 'function') {
            showWarning(message);
        } else if (typeof alert === 'function') {
            alert(message);
        } else {
            console.warn(message);
        }
    },
    
    /**
     * 부계정 권한 제한 로그 생성
     * @param {string} operation 수행하려던 작업
     * @param {string} reason 제한 이유
     * @return {string} 로그 메시지
     */
    createPermissionDeniedLog: function(operation, reason) {
        return '부계정 권한 제한 - 사용자: ' + this.getUserId() + 
               ', 등급: ' + this.getUserGrade() + 
               ', 작업: ' + operation + 
               ', 이유: ' + reason;
    },
    
    /**
     * 부계정 UI 요소 비활성화
     * @param {string} selector CSS 선택자
     * @param {string} operation 작업명
     */
    disableUIElement: function(selector, operation) {
        if (!this.isSubAccount()) {
            return; // 부계정이 아닌 경우 통과
        }
        
        var elements = document.querySelectorAll(selector);
        for (var i = 0; i < elements.length; i++) {
            var element = elements[i];
            
            // 버튼 비활성화
            if (element.tagName === 'BUTTON' || element.tagName === 'INPUT') {
                element.disabled = true;
                element.style.opacity = '0.5';
                element.style.cursor = 'not-allowed';
                
                // 툴팁 추가
                element.title = '부계정은 ' + this.getOperationDescription(operation) + ' 권한이 없습니다';
            }
            
            // 링크 비활성화
            if (element.tagName === 'A') {
                element.style.pointerEvents = 'none';
                element.style.opacity = '0.5';
                element.style.cursor = 'not-allowed';
                
                // 툴팁 추가
                element.title = '부계정은 ' + this.getOperationDescription(operation) + ' 권한이 없습니다';
            }
        }
    },
    
    /**
     * 부계정 UI 요소 숨김
     * @param {string} selector CSS 선택자
     * @param {string} operation 작업명
     */
    hideUIElement: function(selector, operation) {
        if (!this.isSubAccount()) {
            return; // 부계정이 아닌 경우 통과
        }
        
        var elements = document.querySelectorAll(selector);
        for (var i = 0; i < elements.length; i++) {
            var element = elements[i];
            element.style.display = 'none';
        }
    },
    
    /**
     * 부계정 읽기 전용 메시지 표시
     * @param {string} selector CSS 선택자
     * @param {string} operation 작업명
     */
    showReadOnlyMessage: function(selector, operation) {
        if (!this.isSubAccount()) {
            return; // 부계정이 아닌 경우 통과
        }
        
        var elements = document.querySelectorAll(selector);
        for (var i = 0; i < elements.length; i++) {
            var element = elements[i];
            var message = '읽기 전용 (' + this.getOperationDescription(operation) + ' 불가)';
            
            // 기존 내용을 읽기 전용 메시지로 교체
            element.innerHTML = '<span style="color: #999; font-size: 10pt;">' + message + '</span>';
        }
    },
    
    /**
     * 부계정 권한 초기화
     * 페이지 로딩 시 부계정 권한에 따른 UI 제한 적용
     */
    initializeSubAccountPermissions: function() {
        if (!this.isSubAccount()) {
            console.log('부계정이 아닙니다. 권한 제한을 적용하지 않습니다.');
            return;
        }
        
        console.log('부계정 권한 제한 초기화 - 사용자:', this.getUserId(), '등급:', this.getUserGrade());
        
        // 센서 설정 버튼 비활성화
        this.disableUIElement('button[id*="save"]', 'UPDATE_SENSOR_SETTING');
        this.disableUIElement('button[id*="delete"]', 'DELETE_SENSOR');
        this.disableUIElement('button[id*="create"]', 'CREATE_SENSOR');
        
        // 관리자 메뉴 숨김
        this.hideUIElement('a[href*="/admin/"]', 'ADMIN_OPERATIONS');
        this.hideUIElement('button[onclick*="deleteUser"]', 'DELETE_USER');
        this.hideUIElement('button[onclick*="createUser"]', 'CREATE_USER');
        
        // 읽기 전용 메시지 표시
        this.showReadOnlyMessage('.read-only-message', 'READ_ONLY');
        
        console.log('부계정 권한 제한 초기화 완료');
    },
    
    /**
     * 부계정 권한 통계 반환
     * @return {Object} 권한 통계
     */
    getPermissionStats: function() {
        return {
            isSubAccount: this.isSubAccount(),
            userId: this.getUserId(),
            userGrade: this.getUserGrade(),
            allowedOperations: this.ALLOWED_OPERATIONS,
            restrictedOperations: this.RESTRICTED_OPERATIONS,
            totalAllowedOperations: this.ALLOWED_OPERATIONS.length,
            totalRestrictedOperations: this.RESTRICTED_OPERATIONS.length
        };
    }
};

// 페이지 로딩 완료 시 부계정 권한 초기화
document.addEventListener('DOMContentLoaded', function() {
    SubAccountPermissionManager.initializeSubAccountPermissions();
});

// 전역 함수로 등록
window.SubAccountPermissionManager = SubAccountPermissionManager;
