/**
 * 페이지별 토픽 정의
 * 각 페이지에서 구독해야 할 토픽들을 정의
 */
var PageTopicDefinitions = (function() {
    'use strict';
    
    // 페이지별 토픽 정의
    var pageDefinitions = {
        // 메인 페이지
        'main': {
            topics: [
                'HBEE/{userId}/+/+/DEV',  // 모든 장치 메시지
                'HBEE/{userId}/+/+/SER'   // 모든 장치 응답
            ],
            callbacks: {
                'HBEE/{userId}/+/+/DEV': function(topic, message) {
                    console.log('메인 페이지 DEV 메시지 수신:', topic, message);
                    // 기존 MQTT 메시지 처리 로직 호출
                    if (typeof updateStatusFromMqttMessage === 'function') {
                        updateStatusFromMqttMessage(topic, message);
                    }
                },
                'HBEE/{userId}/+/+/SER': function(topic, message) {
                    console.log('메인 페이지 SER 메시지 수신:', topic, message);
                    // 기존 MQTT 메시지 처리 로직 호출
                    if (typeof updateStatusFromMqttMessage === 'function') {
                        updateStatusFromMqttMessage(topic, message);
                    }
                }
            }
        },
        
        // 센서 설정 페이지
        'sensorSetting': {
            topics: [
                'HBEE/{userId}/+/+/DEV',  // 모든 장치 메시지
                'HBEE/{userId}/+/+/SER'   // 모든 장치 응답
            ],
            callbacks: {
                'HBEE/{userId}/+/+/DEV': function(topic, message) {
                    console.log('센서 설정 페이지 DEV 메시지 수신:', topic, message);
                    // 센서 설정 관련 메시지 처리
                    if (typeof handleSensorSettingMessage === 'function') {
                        handleSensorSettingMessage(topic, message);
                    }
                },
                'HBEE/{userId}/+/+/SER': function(topic, message) {
                    console.log('센서 설정 페이지 SER 메시지 수신:', topic, message);
                    // 센서 설정 응답 처리
                    if (typeof handleSensorSettingResponse === 'function') {
                        handleSensorSettingResponse(topic, message);
                    }
                }
            }
        },
        
        // 차트 페이지
        'chart': {
            topics: [
                'HBEE/{userId}/+/+/DEV'   // 모든 장치 메시지 (차트 업데이트용)
            ],
            callbacks: {
                'HBEE/{userId}/+/+/DEV': function(topic, message) {
                    console.log('차트 페이지 DEV 메시지 수신:', topic, message);
                    // 차트 업데이트 처리
                    if (typeof updateChartFromMqttMessage === 'function') {
                        updateChartFromMqttMessage(topic, message);
                    }
                }
            }
        },
        
        // 사용자 목록 페이지
        'userList': {
            topics: [],  // 실시간 데이터 불필요
            callbacks: {}
        },
        
        // 사용자 상세 페이지
        'userDetail': {
            topics: [],  // 실시간 데이터 불필요
            callbacks: {}
        },
        
        // 사용자 생성 페이지
        'createSub': {
            topics: [],  // 실시간 데이터 불필요
            callbacks: {}
        },
        
        // 차트 설정 페이지
        'chartSetting': {
            topics: [],  // 실시간 데이터 불필요
            callbacks: {}
        },
        
        // 사용자 수정 페이지
        'userModify': {
            topics: [],  // 실시간 데이터 불필요
            callbacks: {}
        },
        
        // 로그인 페이지
        'login': {
            topics: [],  // 실시간 데이터 불필요
            callbacks: {}
        },
        
        // 회원가입 페이지
        'join': {
            topics: [],  // 실시간 데이터 불필요
            callbacks: {}
        }
    };
    
    /**
     * 페이지 정의 조회
     * @param {string} pageName 페이지 이름
     * @returns {Object} 페이지 정의
     */
    function getPageDefinition(pageName) {
        return pageDefinitions[pageName] || {
            topics: [],
            callbacks: {}
        };
    }
    
    /**
     * 모든 페이지 정의 조회
     * @returns {Object} 모든 페이지 정의
     */
    function getAllPageDefinitions() {
        return pageDefinitions;
    }
    
    /**
     * 페이지 정의 등록
     * @param {string} pageName 페이지 이름
     * @param {Object} definition 페이지 정의
     */
    function registerPageDefinition(pageName, definition) {
        pageDefinitions[pageName] = definition;
        console.log('페이지 정의 등록:', pageName);
    }
    
    /**
     * 페이지 정의 해제
     * @param {string} pageName 페이지 이름
     */
    function unregisterPageDefinition(pageName) {
        delete pageDefinitions[pageName];
        console.log('페이지 정의 해제:', pageName);
    }
    
    /**
     * 페이지별 토픽 수 조회
     * @returns {Object} 페이지별 토픽 수
     */
    function getPageTopicCounts() {
        var counts = {};
        for (var pageName in pageDefinitions) {
            if (pageDefinitions.hasOwnProperty(pageName)) {
                counts[pageName] = pageDefinitions[pageName].topics.length;
            }
        }
        return counts;
    }
    
    /**
     * 특정 토픽을 사용하는 페이지 조회
     * @param {string} topic 토픽
     * @returns {Array} 페이지 목록
     */
    function getPagesUsingTopic(topic) {
        var pages = [];
        for (var pageName in pageDefinitions) {
            if (pageDefinitions.hasOwnProperty(pageName)) {
                var pageTopics = pageDefinitions[pageName].topics;
                for (var i = 0; i < pageTopics.length; i++) {
                    if (pageTopics[i] === topic) {
                        pages.push(pageName);
                        break;
                    }
                }
            }
        }
        return pages;
    }
    
    // Public API
    return {
        getPageDefinition: getPageDefinition,
        getAllPageDefinitions: getAllPageDefinitions,
        registerPageDefinition: registerPageDefinition,
        unregisterPageDefinition: unregisterPageDefinition,
        getPageTopicCounts: getPageTopicCounts,
        getPagesUsingTopic: getPagesUsingTopic
    };
})();
