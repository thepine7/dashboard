/**
 * 페이지별 토픽 관리자
 * 페이지 이동 시 토픽 구독/해제를 체계적으로 관리
 */
var PageTopicManager = (function() {
    'use strict';
    
    // 페이지별 토픽 구독 정보
    var pageTopics = new Map();
    
    // 현재 페이지 정보
    var currentPage = null;
    
    // 토픽 구독 상태 추적
    var subscriptionState = {
        activeTopics: new Set(),
        pageCallbacks: new Map(),
        globalCallbacks: new Map()
    };
    
    /**
     * 페이지 등록
     * @param {string} pageName 페이지 이름
     * @param {Array} topics 구독할 토픽 목록
     * @param {Object} callbacks 토픽별 콜백 함수
     */
    function registerPage(pageName, topics, callbacks) {
        console.log('페이지 등록:', pageName, '토픽 수:', topics.length);
        
        pageTopics.set(pageName, {
            topics: topics || [],
            callbacks: callbacks || {},
            registeredAt: Date.now()
        });
        
        console.log('페이지 등록 완료:', pageName);
    }
    
    /**
     * 페이지 활성화
     * @param {string} pageName 페이지 이름
     * @param {string} userId 사용자 ID
     */
    function activatePage(pageName, userId) {
        console.log('페이지 활성화:', pageName, '사용자 ID:', userId);
        
        // 이전 페이지 비활성화
        if (currentPage && currentPage !== pageName) {
            deactivatePage(currentPage);
        }
        
        // 새 페이지 활성화
        var pageInfo = pageTopics.get(pageName);
        if (!pageInfo) {
            console.warn('등록되지 않은 페이지:', pageName);
            return false;
        }
        
        currentPage = pageName;
        
        // 토픽 구독
        var successCount = 0;
        for (var i = 0; i < pageInfo.topics.length; i++) {
            var topic = pageInfo.topics[i];
            var fullTopic = buildTopic(topic, userId);
            
            if (subscribeToTopic(fullTopic, pageInfo.callbacks[topic])) {
                successCount++;
            }
        }
        
        console.log('페이지 활성화 완료:', pageName, '성공:', successCount, '전체:', pageInfo.topics.length);
        return successCount > 0;
    }
    
    /**
     * 페이지 비활성화
     * @param {string} pageName 페이지 이름
     */
    function deactivatePage(pageName) {
        console.log('페이지 비활성화:', pageName);
        
        var pageInfo = pageTopics.get(pageName);
        if (!pageInfo) {
            console.warn('등록되지 않은 페이지:', pageName);
            return false;
        }
        
        // 토픽 구독 해제
        var successCount = 0;
        for (var i = 0; i < pageInfo.topics.length; i++) {
            var topic = pageInfo.topics[i];
            var fullTopic = buildTopic(topic, getCurrentUserId());
            
            if (unsubscribeFromTopic(fullTopic)) {
                successCount++;
            }
        }
        
        console.log('페이지 비활성화 완료:', pageName, '성공:', successCount, '전체:', pageInfo.topics.length);
        return successCount > 0;
    }
    
    /**
     * 토픽 구독
     * @param {string} topic 토픽
     * @param {Function} callback 콜백 함수
     */
    function subscribeToTopic(topic, callback) {
        if (!topic) {
            console.warn('토픽이 비어있어서 구독 불가');
            return false;
        }
        
        try {
            // UnifiedMQTTManager를 통한 구독
            if (typeof UnifiedMQTTManager !== 'undefined' && UnifiedMQTTManager.subscribe) {
                var success = UnifiedMQTTManager.subscribe(topic, callback);
                if (success) {
                    subscriptionState.activeTopics.add(topic);
                    if (callback) {
                        subscriptionState.pageCallbacks.set(topic, callback);
                    }
                    console.log('토픽 구독 성공:', topic);
                    return true;
                }
            } else {
                console.warn('UnifiedMQTTManager가 로드되지 않았습니다.');
            }
            
            return false;
        } catch (error) {
            console.error('토픽 구독 실패:', topic, error);
            return false;
        }
    }
    
    /**
     * 토픽 구독 해제
     * @param {string} topic 토픽
     */
    function unsubscribeFromTopic(topic) {
        if (!topic) {
            console.warn('토픽이 비어있어서 구독 해제 불가');
            return false;
        }
        
        try {
            // UnifiedMQTTManager를 통한 구독 해제
            if (typeof UnifiedMQTTManager !== 'undefined' && UnifiedMQTTManager.unsubscribe) {
                var success = UnifiedMQTTManager.unsubscribe(topic);
                if (success) {
                    subscriptionState.activeTopics.delete(topic);
                    subscriptionState.pageCallbacks.delete(topic);
                    console.log('토픽 구독 해제 성공:', topic);
                    return true;
                }
            } else {
                console.warn('UnifiedMQTTManager가 로드되지 않았습니다.');
            }
            
            return false;
        } catch (error) {
            console.error('토픽 구독 해제 실패:', topic, error);
            return false;
        }
    }
    
    /**
     * 토픽 빌드
     * @param {string} topicTemplate 토픽 템플릿
     * @param {string} userId 사용자 ID
     * @returns {string} 완성된 토픽
     */
    function buildTopic(topicTemplate, userId) {
        if (!topicTemplate || !userId) {
            return topicTemplate;
        }
        
        // 토픽 템플릿에서 {userId} 치환
        return topicTemplate.replace('{userId}', userId);
    }
    
    /**
     * 현재 사용자 ID 조회
     * @returns {string} 사용자 ID
     */
    function getCurrentUserId() {
        // JSP에서 설정된 사용자 ID 조회
        var userId = window.currentUserId || 
                    (typeof getUserId === 'function' ? getUserId() : null) ||
                    (typeof getCurrentUserId === 'function' ? getCurrentUserId() : null);
        
        return userId;
    }
    
    /**
     * 모든 토픽 구독 해제
     */
    function unsubscribeAll() {
        console.log('모든 토픽 구독 해제 시작');
        
        var successCount = 0;
        var totalCount = subscriptionState.activeTopics.size;
        
        subscriptionState.activeTopics.forEach(function(topic) {
            if (unsubscribeFromTopic(topic)) {
                successCount++;
            }
        });
        
        console.log('모든 토픽 구독 해제 완료:', '성공:', successCount, '전체:', totalCount);
        return successCount;
    }
    
    /**
     * 현재 구독 상태 조회
     * @returns {Object} 구독 상태 정보
     */
    function getSubscriptionStatus() {
        return {
            currentPage: currentPage,
            activeTopics: Array.from(subscriptionState.activeTopics),
            totalActiveTopics: subscriptionState.activeTopics.size,
            registeredPages: Array.from(pageTopics.keys()),
            totalRegisteredPages: pageTopics.size
        };
    }
    
    /**
     * 페이지 등록 해제
     * @param {string} pageName 페이지 이름
     */
    function unregisterPage(pageName) {
        console.log('페이지 등록 해제:', pageName);
        
        if (currentPage === pageName) {
            deactivatePage(pageName);
            currentPage = null;
        }
        
        pageTopics.delete(pageName);
        console.log('페이지 등록 해제 완료:', pageName);
    }
    
    /**
     * 전역 콜백 등록
     * @param {string} topic 토픽
     * @param {Function} callback 콜백 함수
     */
    function registerGlobalCallback(topic, callback) {
        subscriptionState.globalCallbacks.set(topic, callback);
        console.log('전역 콜백 등록:', topic);
    }
    
    /**
     * 전역 콜백 해제
     * @param {string} topic 토픽
     */
    function unregisterGlobalCallback(topic) {
        subscriptionState.globalCallbacks.delete(topic);
        console.log('전역 콜백 해제:', topic);
    }
    
    /**
     * 메시지 처리
     * @param {string} topic 토픽
     * @param {string} message 메시지
     */
    function handleMessage(topic, message) {
        // 페이지별 콜백 처리
        var pageCallback = subscriptionState.pageCallbacks.get(topic);
        if (pageCallback && typeof pageCallback === 'function') {
            try {
                pageCallback(topic, message);
            } catch (error) {
                console.error('페이지 콜백 실행 오류:', topic, error);
            }
        }
        
        // 전역 콜백 처리
        var globalCallback = subscriptionState.globalCallbacks.get(topic);
        if (globalCallback && typeof globalCallback === 'function') {
            try {
                globalCallback(topic, message);
            } catch (error) {
                console.error('전역 콜백 실행 오류:', topic, error);
            }
        }
    }
    
    // Public API
    return {
        registerPage: registerPage,
        activatePage: activatePage,
        deactivatePage: deactivatePage,
        subscribeToTopic: subscribeToTopic,
        unsubscribeFromTopic: unsubscribeFromTopic,
        unsubscribeAll: unsubscribeAll,
        getSubscriptionStatus: getSubscriptionStatus,
        unregisterPage: unregisterPage,
        registerGlobalCallback: registerGlobalCallback,
        unregisterGlobalCallback: unregisterGlobalCallback,
        handleMessage: handleMessage
    };
})();
