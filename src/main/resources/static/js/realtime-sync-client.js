/**
 * 실시간 동기화 클라이언트
 * Server-Sent Events (SSE) 기반 실시간 통신
 * 
 * 주요 기능:
 * - SSE 연결 관리
 * - 실시간 데이터 수신
 * - 자동 재연결
 * - 메시지 큐 관리
 */
var RealtimeSyncClient = (function() {
    'use strict';
    
    // 연결 상태
    var connectionState = {
        connected: false,
        connecting: false,
        eventSource: null,
        reconnectAttempts: 0,
        maxReconnectAttempts: 10,
        reconnectDelay: 2000,
        lastReconnectTime: 0
    };
    
    // 메시지 큐
    var messageQueue = [];
    var maxQueueSize = 1000;
    
    // 통계 정보
    var stats = {
        totalMessages: 0,
        failedMessages: 0,
        lastMessageTime: 0,
        connectionStartTime: 0
    };
    
    // 이벤트 리스너
    var eventListeners = {};
    
    /**
     * 실시간 동기화 클라이언트 초기화
     */
    function init() {
        console.log('RealtimeSyncClient 초기화 시작');
        
        // 페이지 가시성 변경 감지
        document.addEventListener('visibilitychange', handleVisibilityChange);
        
        // 페이지 이동 시에는 SSE 연결 유지 (로그아웃 시에만 해제)
        window.addEventListener('beforeunload', function() {
            console.log('페이지 이동 - SSE 연결 유지');
            // 페이지 이동 시에는 SSE 연결을 끊지 않음
        });
        
        // 자동 연결 시작
        connect();
        
        console.log('RealtimeSyncClient 초기화 완료');
    }
    
    /**
     * SSE 연결 시작
     */
    function connect() {
        if (connectionState.connecting || connectionState.connected) {
            console.log('이미 연결 중이거나 연결됨');
            return;
        }
        
        connectionState.connecting = true;
        connectionState.lastReconnectTime = Date.now();
        
        try {
            console.log('SSE 연결 시작');
            
            // fetch를 사용하여 Accept 헤더 설정
            fetch('/api/realtime/connect', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(data => {
                console.log('SSE 연결 성공');
                connectionState.connected = true;
                connectionState.connecting = false;
                connectionState.reconnectAttempts = 0;
                stats.connectionStartTime = Date.now();
                
                // 연결 성공 이벤트 발생
                if (typeof onConnectionSuccess === 'function') {
                    onConnectionSuccess(data);
                }
            })
            .catch(error => {
                console.error('SSE 연결 실패:', error);
                connectionState.connected = false;
                connectionState.connecting = false;
                connectionState.lastError = error.message;
                
                // 연결 실패 이벤트 발생
                if (typeof onConnectionError === 'function') {
                    onConnectionError(error);
                }
                
                // 재연결 시도
                scheduleReconnect();
            });
            
            // EventSource 대신 fetch 사용하므로 eventSource는 null로 설정
            connectionState.eventSource = null;
            
        } catch (error) {
            console.error('SSE 연결 생성 실패:', error);
            connectionState.connecting = false;
            stats.failedMessages++;
            
            // 에러 이벤트 발생
            triggerEvent('error', {
                error: error,
                timestamp: Date.now()
            });
            
            // 자동 재연결 시도
            scheduleReconnect();
        }
    }
    
    /**
     * SSE 연결 해제
     */
    function disconnect() {
        console.log('SSE 연결 해제 시작');
        
        if (connectionState.eventSource) {
            connectionState.eventSource.close();
            connectionState.eventSource = null;
        }
        
        connectionState.connected = false;
        connectionState.connecting = false;
        
        // 연결 해제 이벤트 발생
        triggerEvent('disconnected', {
            timestamp: Date.now()
        });
        
        console.log('SSE 연결 해제 완료');
    }
    
    /**
     * 자동 재연결 스케줄링
     */
    function scheduleReconnect() {
        if (connectionState.reconnectAttempts >= connectionState.maxReconnectAttempts) {
            console.error('최대 재연결 시도 횟수 초과');
            triggerEvent('maxReconnectAttemptsReached', {
                attempts: connectionState.reconnectAttempts,
                timestamp: Date.now()
            });
            return;
        }
        
        var now = Date.now();
        var timeSinceLastReconnect = now - connectionState.lastReconnectTime;
        
        if (timeSinceLastReconnect < connectionState.reconnectDelay) {
            var delay = connectionState.reconnectDelay - timeSinceLastReconnect;
            console.log('재연결 대기 중:', delay + 'ms');
            setTimeout(connect, delay);
        } else {
            connect();
        }
        
        connectionState.reconnectAttempts++;
    }
    
    /**
     * 메시지 처리
     */
    function handleMessage(event) {
        try {
            var data = JSON.parse(event.data);
            stats.totalMessages++;
            stats.lastMessageTime = Date.now();
            
            // 메시지 큐에 추가
            addToQueue({
                type: 'message',
                data: data,
                timestamp: Date.now()
            });
            
        } catch (error) {
            console.error('메시지 파싱 실패:', error);
            stats.failedMessages++;
        }
    }
    
    /**
     * 센서 데이터 처리
     */
    function handleSensorData(data) {
        console.log('센서 데이터 수신:', data);
        
        // 메시지 큐에 추가
        addToQueue({
            type: 'sensor_data',
            data: data,
            timestamp: Date.now()
        });
        
        // 센서 데이터 이벤트 발생
        triggerEvent('sensorData', data);
    }
    
    /**
     * MQTT 메시지 처리
     */
    function handleMqttMessage(data) {
        console.log('MQTT 메시지 수신:', data);
        
        // 메시지 큐에 추가
        addToQueue({
            type: 'mqtt_message',
            data: data,
            timestamp: Date.now()
        });
        
        // MQTT 메시지 이벤트 발생
        triggerEvent('mqttMessage', data);
    }
    
    /**
     * 센서 설정 처리
     */
    function handleSensorSettings(data) {
        console.log('센서 설정 수신:', data);
        
        // 메시지 큐에 추가
        addToQueue({
            type: 'sensor_settings',
            data: data,
            timestamp: Date.now()
        });
        
        // 센서 설정 이벤트 발생
        triggerEvent('sensorSettings', data);
    }
    
    /**
     * 알림 처리
     */
    function handleAlarm(data) {
        console.log('알림 수신:', data);
        
        // 메시지 큐에 추가
        addToQueue({
            type: 'alarm',
            data: data,
            timestamp: Date.now()
        });
        
        // 알림 이벤트 발생
        triggerEvent('alarm', data);
    }
    
    /**
     * 시스템 상태 처리
     */
    function handleSystemStatus(data) {
        console.log('시스템 상태 수신:', data);
        
        // 메시지 큐에 추가
        addToQueue({
            type: 'system_status',
            data: data,
            timestamp: Date.now()
        });
        
        // 시스템 상태 이벤트 발생
        triggerEvent('systemStatus', data);
    }
    
    /**
     * 하트비트 처리
     */
    function handleHeartbeat(data) {
        console.log('하트비트 수신:', data);
        
        // 하트비트 이벤트 발생
        triggerEvent('heartbeat', data);
    }
    
    /**
     * 브로드캐스트 처리
     */
    function handleBroadcast(data) {
        console.log('브로드캐스트 수신:', data);
        
        // 메시지 큐에 추가
        addToQueue({
            type: 'broadcast',
            data: data,
            timestamp: Date.now()
        });
        
        // 브로드캐스트 이벤트 발생
        triggerEvent('broadcast', data);
    }
    
    /**
     * 메시지 큐에 추가
     */
    function addToQueue(message) {
        // 큐 크기 제한
        if (messageQueue.length >= maxQueueSize) {
            messageQueue.shift(); // 오래된 메시지 제거
        }
        
        messageQueue.push(message);
    }
    
    /**
     * 페이지 가시성 변경 처리
     */
    function handleVisibilityChange() {
        if (document.hidden) {
            console.log('페이지 숨김 - 연결 유지');
        } else {
            console.log('페이지 표시 - 연결 상태 확인');
            if (!connectionState.connected && !connectionState.connecting) {
                connect();
            }
        }
    }
    
    /**
     * 이벤트 리스너 등록
     */
    function addEventListener(eventType, callback) {
        if (!eventListeners[eventType]) {
            eventListeners[eventType] = [];
        }
        eventListeners[eventType].push(callback);
    }
    
    /**
     * 이벤트 리스너 제거
     */
    function removeEventListener(eventType, callback) {
        if (eventListeners[eventType]) {
            var index = eventListeners[eventType].indexOf(callback);
            if (index > -1) {
                eventListeners[eventType].splice(index, 1);
            }
        }
    }
    
    /**
     * 이벤트 발생
     */
    function triggerEvent(eventType, data) {
        if (eventListeners[eventType]) {
            eventListeners[eventType].forEach(function(callback) {
                try {
                    callback(data);
                } catch (error) {
                    console.error('이벤트 리스너 실행 오류:', error);
                }
            });
        }
    }
    
    /**
     * 연결 상태 조회
     */
    function getConnectionStatus() {
        return {
            connected: connectionState.connected,
            connecting: connectionState.connecting,
            reconnectAttempts: connectionState.reconnectAttempts,
            stats: stats
        };
    }
    
    /**
     * 메시지 큐 조회
     */
    function getMessageQueue() {
        return messageQueue.slice(); // 복사본 반환
    }
    
    /**
     * 메시지 큐 초기화
     */
    function clearMessageQueue() {
        messageQueue = [];
    }
    
    /**
     * 통계 정보 조회
     */
    function getStats() {
        return {
            totalMessages: stats.totalMessages,
            failedMessages: stats.failedMessages,
            successRate: stats.totalMessages > 0 ? 
                ((stats.totalMessages - stats.failedMessages) / stats.totalMessages * 100).toFixed(2) + '%' : '0%',
            lastMessageTime: stats.lastMessageTime,
            connectionStartTime: stats.connectionStartTime,
            queueSize: messageQueue.length
        };
    }
    
    // 자동 초기화
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
    
    // Public API
    return {
        connect: connect,
        disconnect: disconnect,
        addEventListener: addEventListener,
        removeEventListener: removeEventListener,
        getConnectionStatus: getConnectionStatus,
        getMessageQueue: getMessageQueue,
        clearMessageQueue: clearMessageQueue,
        getStats: getStats
    };
})();
