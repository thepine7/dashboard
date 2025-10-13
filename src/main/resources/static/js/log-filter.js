/**
 * JavaScript 로깅 필터
 * HnT Sensor API 프로젝트
 * 
 * 주요 기능:
 * - 콘솔 로그 필터링
 * - 성능 최적화
 * - 디버그 모드 관리
 */

window.LogFilter = {
    
    // 로그 카운터
    counters: new Map(),
    
    // 로그 제한 설정
    MAX_LOGS_PER_MINUTE: 10,
    LOG_RESET_INTERVAL: 60000, // 1분
    
    // 디버그 모드
    debugMode: false,
    
    // 필터링할 키워드 (MQTT 디버깅을 위해 일시적으로 비활성화)
    filteredKeywords: [
        // 'MQTT', '연결', '호스트', '포트', '클라이언트',
        // '=== MQTT', '연결 성공', '연결 실패', '연결 끊김'
    ],
    
    /**
     * 문자열 해시 함수
     */
    hashCode: function(str) {
        let hash = 0;
        if (str.length === 0) return hash;
        for (let i = 0; i < str.length; i++) {
            const char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash; // 32bit integer로 변환
        }
        return hash;
    },
    
    /**
     * 로그 출력 여부 결정
     */
    shouldLog: function(level, message) {
        const key = level + ':' + LogFilter.hashCode(message);
        let counter = LogFilter.counters.get(key);
        
        if (!counter) {
            counter = { count: 0, lastReset: Date.now() };
            LogFilter.counters.set(key, counter);
        }
        
        // 1분마다 카운터 리셋
        if (Date.now() - counter.lastReset > LogFilter.LOG_RESET_INTERVAL) {
            counter.count = 0;
            counter.lastReset = Date.now();
        }
        
        counter.count++;
        
        // 첫 번째 로그는 항상 출력
        if (counter.count === 1) {
            return true;
        }
        
        // 제한 횟수 초과 시 출력하지 않음
        if (counter.count > LogFilter.MAX_LOGS_PER_MINUTE) {
            return false;
        }
        
        return true;
    },
    
    /**
     * 메시지 필터링
     */
    isFiltered: function(message) {
        if (!LogFilter.debugMode) {
            return LogFilter.filteredKeywords.some(keyword => 
                message.includes(keyword)
            );
        }
        return false;
    },
    
    /**
     * 디버그 로그
     */
    debug: function(message) {
        if (LogFilter.isFiltered(message)) return;
        if (LogFilter.shouldLog('DEBUG', message)) {
            console.debug(message);
        }
    },
    
    /**
     * 정보 로그
     */
    info: function(message) {
        if (LogFilter.isFiltered(message)) return;
        if (LogFilter.shouldLog('INFO', message)) {
            console.info(message);
        }
    },
    
    /**
     * 경고 로그
     */
    warn: function(message) {
        if (LogFilter.shouldLog('WARN', message)) {
            console.warn(message);
        }
    },
    
    /**
     * 에러 로그
     */
    error: function(message) {
        if (LogFilter.shouldLog('ERROR', message)) {
            console.error(message);
        }
    },
    
    /**
     * 디버그 모드 설정
     */
    setDebugMode: function(enabled) {
        LogFilter.debugMode = enabled;
        console.log('디버그 모드:', enabled ? '활성화' : '비활성화');
    },
    
    /**
     * 필터링 키워드 추가
     */
    addFilterKeyword: function(keyword) {
        if (!LogFilter.filteredKeywords.includes(keyword)) {
            LogFilter.filteredKeywords.push(keyword);
        }
    },
    
    /**
     * 필터링 키워드 제거
     */
    removeFilterKeyword: function(keyword) {
        const index = LogFilter.filteredKeywords.indexOf(keyword);
        if (index > -1) {
            LogFilter.filteredKeywords.splice(index, 1);
        }
    },
    
    /**
     * 카운터 초기화
     */
    resetCounters: function() {
        LogFilter.counters.clear();
    }
};

// 기존 console 메서드 오버라이드
(function() {
    const originalConsole = {
        log: console.log,
        debug: console.debug,
        info: console.info,
        warn: console.warn,
        error: console.error
    };
    
    console.log = function(message) {
        if (LogFilter.isFiltered(message)) return;
        if (LogFilter.shouldLog('LOG', message)) {
            originalConsole.log(message);
        }
    };
    
    console.debug = function(message) {
        LogFilter.debug(message);
    };
    
    console.info = function(message) {
        LogFilter.info(message);
    };
    
    console.warn = function(message) {
        LogFilter.warn(message);
    };
    
    console.error = function(message) {
        LogFilter.error(message);
    };
})();

// 페이지 로딩 완료 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    console.log('로깅 필터 로드 완료');
});
