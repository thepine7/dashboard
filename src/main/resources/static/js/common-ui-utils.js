/**
 * 공통 UI 유틸리티
 * HnT Sensor API 프로젝트
 * 
 * 주요 기능:
 * - 버튼 상태 관리
 * - 로딩 상태 표시
 * - 알림 메시지 표시
 * - 폼 검증
 */

// 전역 변수
window.UIUtils = {
    
    // 버튼 상태 관리
    buttonStates: new Map(),
    
    // 로딩 상태 관리
    loadingStates: new Map(),
    
    /**
     * 버튼 로딩 상태 설정
     */
    setButtonLoading: function(buttonId, isLoading) {
        const button = document.getElementById(buttonId);
        if (!button) return;
        
        if (isLoading) {
            button.classList.add('btn-loading');
            button.disabled = true;
            this.loadingStates.set(buttonId, true);
        } else {
            button.classList.remove('btn-loading');
            button.disabled = false;
            this.loadingStates.set(buttonId, false);
        }
    },
    
    /**
     * 버튼 활성화/비활성화
     */
    setButtonEnabled: function(buttonId, enabled) {
        const button = document.getElementById(buttonId);
        if (!button) return;
        
        button.disabled = !enabled;
        if (enabled) {
            button.classList.remove('btn-disabled');
        } else {
            button.classList.add('btn-disabled');
        }
    },
    
    /**
     * 버튼 텍스트 변경
     */
    setButtonText: function(buttonId, text) {
        const button = document.getElementById(buttonId);
        if (!button) return;
        
        button.textContent = text;
    },
    
    /**
     * 버튼 클래스 변경
     */
    setButtonClass: function(buttonId, className) {
        const button = document.getElementById(buttonId);
        if (!button) return;
        
        // 기존 버튼 클래스 제거
        button.classList.remove('btn-primary', 'btn-success', 'btn-danger', 'btn-warning', 'btn-info', 'btn-secondary');
        
        // 새 클래스 추가
        if (className) {
            button.classList.add(className);
        }
    },
    
    /**
     * 상태표시등 업데이트
     */
    updateStatusIndicator: function(elementId, status) {
        const element = document.getElementById(elementId);
        if (!element) return;
        
        // 기존 상태 클래스 제거
        element.classList.remove('green', 'gray', 'red', 'active');
        
        // 새 상태 클래스 추가
        if (status) {
            element.classList.add(status);
        }
    },
    
    /**
     * 로딩 스피너 표시/숨김
     */
    showLoading: function(elementId, show) {
        const element = document.getElementById(elementId);
        if (!element) return;
        
        if (show) {
            element.style.display = 'block';
            element.innerHTML = '<div class="spinner-border" role="status"><span class="sr-only">Loading...</span></div>';
        } else {
            element.style.display = 'none';
        }
    },
    
    /**
     * 알림 메시지 표시
     */
    showAlert: function(message, type = 'info', duration = 3000) {
        // 기존 알림 제거
        this.hideAlert();
        
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
        alertDiv.style.position = 'fixed';
        alertDiv.style.top = '20px';
        alertDiv.style.right = '20px';
        alertDiv.style.zIndex = '9999';
        alertDiv.style.minWidth = '300px';
        
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="close" data-dismiss="alert">
                <span>&times;</span>
            </button>
        `;
        
        document.body.appendChild(alertDiv);
        
        // 자동 제거
        if (duration > 0) {
            setTimeout(() => {
                this.hideAlert();
            }, duration);
        }
    },
    
    /**
     * 알림 메시지 숨김
     */
    hideAlert: function() {
        const existingAlert = document.querySelector('.alert');
        if (existingAlert) {
            existingAlert.remove();
        }
    },
    
    /**
     * 성공 메시지 표시
     */
    showSuccess: function(message, duration = 3000) {
        this.showAlert(message, 'success', duration);
    },
    
    /**
     * 에러 메시지 표시
     */
    showError: function(message, duration = 5000) {
        this.showAlert(message, 'danger', duration);
    },
    
    /**
     * 경고 메시지 표시
     */
    showWarning: function(message, duration = 4000) {
        this.showAlert(message, 'warning', duration);
    },
    
    /**
     * 정보 메시지 표시
     */
    showInfo: function(message, duration = 3000) {
        this.showAlert(message, 'info', duration);
    },
    
    /**
     * 폼 검증
     */
    validateForm: function(formId) {
        const form = document.getElementById(formId);
        if (!form) return false;
        
        const inputs = form.querySelectorAll('input[required], select[required], textarea[required]');
        let isValid = true;
        
        inputs.forEach(input => {
            if (!input.value.trim()) {
                this.showError(`${input.name || input.id}은(는) 필수 입력 항목입니다.`);
                input.focus();
                isValid = false;
                return false;
            }
        });
        
        return isValid;
    },
    
    /**
     * 이메일 형식 검증
     */
    validateEmail: function(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },
    
    /**
     * 전화번호 형식 검증
     */
    validatePhone: function(phone) {
        const phoneRegex = /^[0-9-+().\s]+$/;
        return phoneRegex.test(phone) && phone.replace(/[^0-9]/g, '').length >= 10;
    },
    
    /**
     * 숫자 형식 검증
     */
    validateNumber: function(value, min = null, max = null) {
        const num = parseFloat(value);
        if (isNaN(num)) return false;
        if (min !== null && num < min) return false;
        if (max !== null && num > max) return false;
        return true;
    },
    
    /**
     * 문자열 길이 검증
     */
    validateLength: function(value, min = 0, max = Infinity) {
        const length = value.length;
        return length >= min && length <= max;
    },
    
    /**
     * 모달 표시
     */
    showModal: function(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        
        modal.style.display = 'block';
        modal.classList.add('show');
        document.body.classList.add('modal-open');
    },
    
    /**
     * 모달 숨김
     */
    hideModal: function(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        
        modal.style.display = 'none';
        modal.classList.remove('show');
        document.body.classList.remove('modal-open');
    },
    
    /**
     * 확인 대화상자
     */
    confirm: function(message, callback) {
        if (confirm(message)) {
            if (typeof callback === 'function') {
                callback();
            }
        }
    },
    
    /**
     * 커스텀 확인 대화상자
     */
    showConfirm: function(message, confirmText = '확인', cancelText = '취소') {
        return new Promise((resolve) => {
            const modal = document.createElement('div');
            modal.className = 'modal fade show';
            modal.style.display = 'block';
            modal.innerHTML = `
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-body">
                            ${message}
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-dismiss="modal">${cancelText}</button>
                            <button type="button" class="btn btn-primary" data-confirm="true">${confirmText}</button>
                        </div>
                    </div>
                </div>
            `;
            
            document.body.appendChild(modal);
            document.body.classList.add('modal-open');
            
            modal.querySelector('[data-confirm="true"]').addEventListener('click', () => {
                document.body.removeChild(modal);
                document.body.classList.remove('modal-open');
                resolve(true);
            });
            
            modal.querySelector('[data-dismiss="modal"]').addEventListener('click', () => {
                document.body.removeChild(modal);
                document.body.classList.remove('modal-open');
                resolve(false);
            });
        });
    },
    
    /**
     * 페이지 새로고침
     */
    reload: function() {
        window.location.reload();
    },
    
    /**
     * 페이지 이동
     */
    redirect: function(url) {
        window.location.href = url;
    },
    
    /**
     * 스크롤을 맨 위로
     */
    scrollToTop: function() {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    
    /**
     * 요소로 스크롤
     */
    scrollToElement: function(elementId) {
        const element = document.getElementById(elementId);
        if (element) {
            element.scrollIntoView({ behavior: 'smooth' });
        }
    },
    
    /**
     * 로컬 스토리지에 데이터 저장
     */
    setStorage: function(key, value) {
        try {
            localStorage.setItem(key, JSON.stringify(value));
        } catch (e) {
            console.error('로컬 스토리지 저장 실패:', e);
        }
    },
    
    /**
     * 로컬 스토리지에서 데이터 가져오기
     */
    getStorage: function(key, defaultValue = null) {
        try {
            const value = localStorage.getItem(key);
            return value ? JSON.parse(value) : defaultValue;
        } catch (e) {
            console.error('로컬 스토리지 읽기 실패:', e);
            return defaultValue;
        }
    },
    
    /**
     * 로컬 스토리지에서 데이터 삭제
     */
    removeStorage: function(key) {
        try {
            localStorage.removeItem(key);
        } catch (e) {
            console.error('로컬 스토리지 삭제 실패:', e);
        }
    },
    
    /**
     * 디바운스 함수
     */
    debounce: function(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },
    
    /**
     * 스로틀 함수
     */
    throttle: function(func, limit) {
        let inThrottle;
        return function() {
            const args = arguments;
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }
};

// 페이지 로딩 완료 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    console.log('공통 UI 유틸리티 로드 완료');
});
