/**
 * 통합 토픽 관리 시스템
 * PageTopicManager와 PageTopicDefinitions를 통합하여 관리
 */
var UnifiedTopicManager = (function() {
    'use strict';
    
    // 초기화 상태
    var isInitialized = false;
    
    // 현재 페이지 정보
    var currentPage = null;
    var currentUserId = null;
    
    /**
     * 시스템 초기화
     */
    function initialize() {
        if (isInitialized) {
            console.log('UnifiedTopicManager 이미 초기화됨');
            return;
        }
        
        console.log('UnifiedTopicManager 초기화 시작');
        
        try {
            // 페이지 정의 등록
            registerAllPageDefinitions();
            
            // 전역 이벤트 리스너 등록
            setupGlobalEventListeners();
            
            isInitialized = true;
            console.log('UnifiedTopicManager 초기화 완료');
            
        } catch (error) {
            console.error('UnifiedTopicManager 초기화 실패:', error);
        }
    }
    
    /**
     * 모든 페이지 정의 등록
     */
    function registerAllPageDefinitions() {
        if (typeof PageTopicDefinitions === 'undefined') {
            console.warn('PageTopicDefinitions가 로드되지 않았습니다.');
            return;
        }
        
        var allDefinitions = PageTopicDefinitions.getAllPageDefinitions();
        for (var pageName in allDefinitions) {
            if (allDefinitions.hasOwnProperty(pageName)) {
                var definition = allDefinitions[pageName];
                PageTopicManager.registerPage(pageName, definition.topics, definition.callbacks);
            }
        }
        
        console.log('모든 페이지 정의 등록 완료');
    }
    
    /**
     * 전역 이벤트 리스너 설정
     */
    function setupGlobalEventListeners() {
        // 페이지 가시성 변경 이벤트
        document.addEventListener('visibilitychange', function() {
            if (document.hidden) {
                console.log('페이지 숨김 - 토픽 구독 유지');
            } else {
                console.log('페이지 표시 - 토픽 구독 상태 확인');
                if (currentPage) {
                    refreshCurrentPageSubscriptions();
                }
            }
        });
        
        // 페이지 이동 시에는 토픽 구독 유지 (로그아웃 시에만 해제)
        window.addEventListener('beforeunload', function() {
            console.log('페이지 이동 - 토픽 구독 유지');
            // 페이지 이동 시에는 토픽 구독을 해제하지 않음
        });
        
        // MQTT 연결 상태 변경 이벤트
        document.addEventListener('mqtt:connected', function() {
            console.log('MQTT 연결됨 - 현재 페이지 토픽 재구독');
            if (currentPage) {
                refreshCurrentPageSubscriptions();
            }
        });
        
        document.addEventListener('mqtt:disconnected', function() {
            console.log('MQTT 연결 끊김 - 토픽 구독 상태 초기화');
            // 연결이 끊어져도 구독 상태는 유지 (재연결 시 복구)
        });
    }
    
    /**
     * 페이지 활성화
     * @param {string} pageName 페이지 이름
     * @param {string} userId 사용자 ID
     */
    function activatePage(pageName, userId) {
        console.log('페이지 활성화 요청:', pageName, '사용자 ID:', userId);
        
        if (!isInitialized) {
            console.warn('UnifiedTopicManager가 초기화되지 않았습니다.');
            return false;
        }
        
        if (!pageName || !userId) {
            console.warn('페이지 이름 또는 사용자 ID가 없습니다.');
            return false;
        }
        
        currentPage = pageName;
        currentUserId = userId;
        
        // PageTopicManager를 통한 페이지 활성화
        var success = PageTopicManager.activatePage(pageName, userId);
        
        if (success) {
            console.log('페이지 활성화 성공:', pageName);
        } else {
            console.warn('페이지 활성화 실패:', pageName);
        }
        
        return success;
    }
    
    /**
     * 페이지 비활성화
     * @param {string} pageName 페이지 이름
     */
    function deactivatePage(pageName) {
        console.log('페이지 비활성화 요청:', pageName);
        
        if (!isInitialized) {
            console.warn('UnifiedTopicManager가 초기화되지 않았습니다.');
            return false;
        }
        
        var success = PageTopicManager.deactivatePage(pageName);
        
        if (pageName === currentPage) {
            currentPage = null;
            currentUserId = null;
        }
        
        if (success) {
            console.log('페이지 비활성화 성공:', pageName);
        } else {
            console.warn('페이지 비활성화 실패:', pageName);
        }
        
        return success;
    }
    
    /**
     * 현재 페이지 토픽 구독 새로고침
     */
    function refreshCurrentPageSubscriptions() {
        if (!currentPage || !currentUserId) {
            console.log('현재 페이지 정보가 없어서 토픽 구독 새로고침 불가');
            return false;
        }
        
        console.log('현재 페이지 토픽 구독 새로고침:', currentPage);
        
        // 기존 구독 해제
        PageTopicManager.deactivatePage(currentPage);
        
        // 새로 구독
        var success = PageTopicManager.activatePage(currentPage, currentUserId);
        
        if (success) {
            console.log('토픽 구독 새로고침 성공:', currentPage);
        } else {
            console.warn('토픽 구독 새로고침 실패:', currentPage);
        }
        
        return success;
    }
    
    /**
     * 특정 토픽 구독
     * @param {string} topic 토픽
     * @param {Function} callback 콜백 함수
     */
    function subscribeToTopic(topic, callback) {
        console.log('토픽 구독 요청:', topic);
        
        if (!isInitialized) {
            console.warn('UnifiedTopicManager가 초기화되지 않았습니다.');
            return false;
        }
        
        return PageTopicManager.subscribeToTopic(topic, callback);
    }
    
    /**
     * 특정 토픽 구독 해제
     * @param {string} topic 토픽
     */
    function unsubscribeFromTopic(topic) {
        console.log('토픽 구독 해제 요청:', topic);
        
        if (!isInitialized) {
            console.warn('UnifiedTopicManager가 초기화되지 않았습니다.');
            return false;
        }
        
        return PageTopicManager.unsubscribeFromTopic(topic);
    }
    
    /**
     * 모든 토픽 구독 해제
     */
    function unsubscribeAll() {
        console.log('모든 토픽 구독 해제 요청');
        
        if (!isInitialized) {
            console.warn('UnifiedTopicManager가 초기화되지 않았습니다.');
            return false;
        }
        
        return PageTopicManager.unsubscribeAll();
    }
    
    /**
     * 구독 상태 조회
     * @returns {Object} 구독 상태 정보
     */
    function getSubscriptionStatus() {
        if (!isInitialized) {
            return {
                initialized: false,
                currentPage: null,
                currentUserId: null,
                activeTopics: [],
                totalActiveTopics: 0
            };
        }
        
        var status = PageTopicManager.getSubscriptionStatus();
        status.initialized = true;
        status.currentUserId = currentUserId;
        
        return status;
    }
    
    /**
     * 현재 페이지 정보 조회
     * @returns {Object} 현재 페이지 정보
     */
    function getCurrentPageInfo() {
        return {
            pageName: currentPage,
            userId: currentUserId,
            initialized: isInitialized
        };
    }
    
    /**
     * 페이지 정의 조회
     * @param {string} pageName 페이지 이름
     * @returns {Object} 페이지 정의
     */
    function getPageDefinition(pageName) {
        if (typeof PageTopicDefinitions === 'undefined') {
            console.warn('PageTopicDefinitions가 로드되지 않았습니다.');
            return null;
        }
        
        return PageTopicDefinitions.getPageDefinition(pageName);
    }
    
    /**
     * 메시지 처리
     * @param {string} topic 토픽
     * @param {string} message 메시지
     */
    function handleMessage(topic, message) {
        if (!isInitialized) {
            console.warn('UnifiedTopicManager가 초기화되지 않았습니다.');
            return;
        }
        
        PageTopicManager.handleMessage(topic, message);
    }
    
    // Public API
    return {
        initialize: initialize,
        activatePage: activatePage,
        deactivatePage: deactivatePage,
        refreshCurrentPageSubscriptions: refreshCurrentPageSubscriptions,
        subscribeToTopic: subscribeToTopic,
        unsubscribeFromTopic: unsubscribeFromTopic,
        unsubscribeAll: unsubscribeAll,
        getSubscriptionStatus: getSubscriptionStatus,
        getCurrentPageInfo: getCurrentPageInfo,
        getPageDefinition: getPageDefinition,
        handleMessage: handleMessage
    };
})();
