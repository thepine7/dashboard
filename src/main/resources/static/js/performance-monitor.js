/**
 * 프론트엔드 성능 모니터링 도구
 * HnT Sensor API 프로젝트 전용
 * 
 * 주요 기능:
 * - DOM 조작 성능 모니터링
 * - 메모리 사용량 추적
 * - 이벤트 리스너 누수 감지
 * - 성능 병목 지점 식별
 */

var PerformanceMonitor = (function() {
    'use strict';
    
    // 성능 데이터 저장
    var performanceData = {
        domManipulations: 0,
        eventListeners: 0,
        memoryUsage: 0,
        renderTime: 0,
        lastUpdate: Date.now()
    };
    
    // 설정
    var config = {
        monitoringInterval: 5000, // 5초마다 모니터링
        maxDomManipulations: 100, // 초당 최대 DOM 조작 횟수
        maxEventListeners: 50,    // 최대 이벤트 리스너 수
        memoryThreshold: 50 * 1024 * 1024 // 50MB 메모리 임계값
    };
    
    var monitoringInterval = null;
    var isMonitoring = false;
    
    /**
     * 성능 모니터링 시작
     */
    function startMonitoring() {
        if (isMonitoring) {
            console.log('성능 모니터링이 이미 시작됨');
            return;
        }
        
        console.log('프론트엔드 성능 모니터링 시작');
        
        // DOM 조작 모니터링
        monitorDOMManipulations();
        
        // 이벤트 리스너 모니터링
        monitorEventListeners();
        
        // 메모리 사용량 모니터링
        monitorMemoryUsage();
        
        // 정기적인 성능 체크
        monitoringInterval = setInterval(checkPerformance, config.monitoringInterval);
        
        isMonitoring = true;
    }
    
    /**
     * 성능 모니터링 중지
     */
    function stopMonitoring() {
        if (monitoringInterval) {
            clearInterval(monitoringInterval);
            monitoringInterval = null;
        }
        
        isMonitoring = false;
        console.log('프론트엔드 성능 모니터링 중지');
    }
    
    /**
     * DOM 조작 모니터링
     */
    function monitorDOMManipulations() {
        var originalAppendChild = Node.prototype.appendChild;
        var originalRemoveChild = Node.prototype.removeChild;
        var originalInsertBefore = Node.prototype.insertBefore;
        
        // appendChild 모니터링
        Node.prototype.appendChild = function(child) {
            performanceData.domManipulations++;
            return originalAppendChild.call(this, child);
        };
        
        // removeChild 모니터링
        Node.prototype.removeChild = function(child) {
            performanceData.domManipulations++;
            return originalRemoveChild.call(this, child);
        };
        
        // insertBefore 모니터링
        Node.prototype.insertBefore = function(newNode, referenceNode) {
            performanceData.domManipulations++;
            return originalInsertBefore.call(this, newNode, referenceNode);
        };
    }
    
    /**
     * 이벤트 리스너 모니터링
     */
    function monitorEventListeners() {
        var originalAddEventListener = EventTarget.prototype.addEventListener;
        var originalRemoveEventListener = EventTarget.prototype.removeEventListener;
        
        // addEventListener 모니터링
        EventTarget.prototype.addEventListener = function(type, listener, options) {
            performanceData.eventListeners++;
            return originalAddEventListener.call(this, type, listener, options);
        };
        
        // removeEventListener 모니터링
        EventTarget.prototype.removeEventListener = function(type, listener, options) {
            performanceData.eventListeners--;
            return originalRemoveEventListener.call(this, type, listener, options);
        };
    }
    
    /**
     * 메모리 사용량 모니터링
     */
    function monitorMemoryUsage() {
        if (performance.memory) {
            performanceData.memoryUsage = performance.memory.usedJSHeapSize;
        }
    }
    
    /**
     * 성능 체크
     */
    function checkPerformance() {
        var currentTime = Date.now();
        var timeDiff = currentTime - performanceData.lastUpdate;
        
        // DOM 조작 빈도 체크
        var domManipulationRate = performanceData.domManipulations / (timeDiff / 1000);
        if (domManipulationRate > config.maxDomManipulations) {
            console.warn('높은 DOM 조작 빈도 감지:', domManipulationRate.toFixed(2) + '/초');
        }
        
        // 이벤트 리스너 수 체크
        if (performanceData.eventListeners > config.maxEventListeners) {
            console.warn('과도한 이벤트 리스너 수 감지:', performanceData.eventListeners);
        }
        
        // 메모리 사용량 체크
        if (performance.memory) {
            var currentMemory = performance.memory.usedJSHeapSize;
            if (currentMemory > config.memoryThreshold) {
                console.warn('높은 메모리 사용량 감지:', (currentMemory / 1024 / 1024).toFixed(2) + 'MB');
            }
            performanceData.memoryUsage = currentMemory;
        }
        
        // 성능 데이터 리셋
        performanceData.domManipulations = 0;
        performanceData.lastUpdate = currentTime;
        
        // 성능 리포트 로깅
        logPerformanceReport();
    }
    
    /**
     * 성능 리포트 로깅
     */
    function logPerformanceReport() {
        console.log('=== 프론트엔드 성능 리포트 ===');
        console.log('이벤트 리스너 수:', performanceData.eventListeners);
        console.log('메모리 사용량:', (performanceData.memoryUsage / 1024 / 1024).toFixed(2) + 'MB');
        
        if (performance.memory) {
            console.log('총 메모리:', (performance.memory.totalJSHeapSize / 1024 / 1024).toFixed(2) + 'MB');
            console.log('메모리 한계:', (performance.memory.jsHeapSizeLimit / 1024 / 1024).toFixed(2) + 'MB');
        }
        
        // 렌더링 성능 체크
        if (performance.getEntriesByType) {
            var paintEntries = performance.getEntriesByType('paint');
            if (paintEntries.length > 0) {
                console.log('First Paint:', paintEntries[0].startTime.toFixed(2) + 'ms');
            }
        }
    }
    
    /**
     * 성능 최적화 제안
     */
    function getOptimizationSuggestions() {
        var suggestions = [];
        
        if (performanceData.eventListeners > config.maxEventListeners) {
            suggestions.push('이벤트 리스너 수가 많습니다. 이벤트 위임을 사용하세요.');
        }
        
        if (performanceData.memoryUsage > config.memoryThreshold) {
            suggestions.push('메모리 사용량이 높습니다. 불필요한 객체를 정리하세요.');
        }
        
        if (performanceData.domManipulations > config.maxDomManipulations) {
            suggestions.push('DOM 조작이 빈번합니다. 배치 처리나 DocumentFragment를 사용하세요.');
        }
        
        return suggestions;
    }
    
    /**
     * 성능 데이터 가져오기
     */
    function getPerformanceData() {
        return {
            domManipulations: performanceData.domManipulations,
            eventListeners: performanceData.eventListeners,
            memoryUsage: performanceData.memoryUsage,
            suggestions: getOptimizationSuggestions()
        };
    }
    
    // 공개 API
    return {
        startMonitoring: startMonitoring,
        stopMonitoring: stopMonitoring,
        getPerformanceData: getPerformanceData,
        getOptimizationSuggestions: getOptimizationSuggestions
    };
})();

// 자동 초기화 (개발 환경에서만)
if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
    document.addEventListener('DOMContentLoaded', function() {
        PerformanceMonitor.startMonitoring();
    });
}

// 전역 함수로 노출
window.PerformanceMonitor = PerformanceMonitor;
