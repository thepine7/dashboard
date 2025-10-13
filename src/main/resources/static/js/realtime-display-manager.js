/**
 * 실시간 표시 관리 통합 모듈
 * 온도, 상태표시등, 설정값 표시의 깜빡임 방지 및 동기화 관리
 */

window.RealtimeDisplayManager = {
    
    // 상태 추적 변수들
    deviceStates: {},
    updateLocks: {},
    lastUpdateTimes: {},
    
    // 설정 상수
    CONSTANTS: {
        UPDATE_THROTTLE_MS: 100, // 업데이트 스로틀링 (100ms)
        LOCK_TIMEOUT_MS: 1000,   // 락 타임아웃 (1초)
        ERROR_THRESHOLD_TIME: 15000, // 에러 임계값 (15초)
        ERROR_RETRY_COUNT: 3     // 에러 재시도 횟수
    },
    
    /**
     * 장치 상태 초기화
     * @param {String} uuid 장치 UUID
     */
    initializeDevice: function(uuid) {
        this.deviceStates[uuid] = {
            lastDataTime: Date.now(),
            errorCounters: 0,
            errorStates: false,
            statusStates: 'gray',
            errorDisplayStates: 'gray',
            dinErrorStates: false,
            hasTemperatureData: false,
            deviceType: '0',
            lastUpdateTime: 0
        };
        
        console.log(`[${uuid}] 장치 상태 초기화 완료`);
    },
    
    /**
     * 락 메커니즘으로 동시 업데이트 방지
     * @param {String} lockKey 락 키
     * @returns {Boolean} 락 획득 성공 여부
     */
    acquireLock: function(lockKey) {
        if (this.updateLocks[lockKey]) {
            return false;
        }
        
        this.updateLocks[lockKey] = Date.now();
        return true;
    },
    
    /**
     * 락 해제
     * @param {String} lockKey 락 키
     */
    releaseLock: function(lockKey) {
        delete this.updateLocks[lockKey];
    },
    
    /**
     * 락 타임아웃 체크 및 정리
     */
    cleanupLocks: function() {
        var now = Date.now();
        Object.keys(this.updateLocks).forEach(function(lockKey) {
            if (now - window.RealtimeDisplayManager.updateLocks[lockKey] > window.RealtimeDisplayManager.CONSTANTS.LOCK_TIMEOUT_MS) {
                console.warn('락 타임아웃 해제:', lockKey);
                window.RealtimeDisplayManager.releaseLock(lockKey);
            }
        });
    },
    
    /**
     * 실시간 온도 업데이트 (깜빡임 방지)
     * @param {String} uuid 장치 UUID
     * @param {String} value 온도값
     * @param {Boolean} isError 에러 상태 여부
     */
    updateCurrentTemperature: function(uuid, value, isError) {
        try {
            var lockKey = 'temperature_' + uuid;
            
            // 락 획득 시도
            if (!this.acquireLock(lockKey)) {
                console.log(`[${uuid}] 온도 업데이트 대기 중...`);
                setTimeout(() => this.updateCurrentTemperature(uuid, value, isError), 50);
                return;
            }
            
            // 스로틀링 체크
            var now = Date.now();
            var lastUpdate = this.lastUpdateTimes[uuid] || 0;
            if (now - lastUpdate < this.CONSTANTS.UPDATE_THROTTLE_MS) {
                this.releaseLock(lockKey);
                setTimeout(() => this.updateCurrentTemperature(uuid, value, isError), 50);
                return;
            }
            
            // 장치 상태 초기화 (필요시)
            if (!this.deviceStates[uuid]) {
                this.initializeDevice(uuid);
            }
            
            var sensorValElement = $('#sensorVal' + uuid);
            if (sensorValElement.length === 0) {
                console.warn(`[${uuid}] 온도 표시 요소를 찾을 수 없음`);
                this.releaseLock(lockKey);
                return;
            }
            
            // 현재 표시된 값과 비교하여 중복 업데이트 방지
            var currentDisplay = sensorValElement.html();
            var newDisplay = isError ? 
                '<font size="50px" color="red">Error</font>' : 
                '<font size="50px">' + value + '°C</font>';
            
            if (currentDisplay === newDisplay) {
                console.log(`[${uuid}] 온도 표시 중복 방지: ${value}`);
                this.releaseLock(lockKey);
                return;
            }
            
            // 온도 업데이트
            sensorValElement.html(newDisplay);
            
            // 상태 업데이트
            this.deviceStates[uuid].lastDataTime = now;
            this.deviceStates[uuid].hasTemperatureData = !isError;
            this.lastUpdateTimes[uuid] = now;
            
            // 그래프 데이터 업데이트
            if (typeof window['yval_' + uuid] !== 'undefined') {
                window['yval_' + uuid] = isError ? 'Error' : parseFloat(value);
            }
            
            console.log(`[${uuid}] 온도 업데이트 완료: ${value}°C (에러: ${isError})`);
            
            this.releaseLock(lockKey);
            
        } catch (e) {
            console.error(`[${uuid}] 온도 업데이트 오류:`, e);
            this.releaseLock(lockKey);
        }
    },
    
    // updateStatusIndicator 함수는 src/main/resources/static/js/common-utils.js에서 제공됨 (중복 제거)
    
    /**
     * 상태표시등 요소 ID 생성
     * @param {String} type 상태 타입
     * @param {String} uuid 장치 UUID
     * @returns {String} 요소 ID
     */
    getStatusElementId: function(type, uuid) {
        var elementMap = {
            'status': '#status' + uuid,
            'error': '#error' + uuid,
            'comp': '#comp' + uuid,
            'defr': '#defr' + uuid,
            'fan': '#fan' + uuid
        };
        return elementMap[type] || '#status' + uuid;
    },
    
    /**
     * 상태표시등 이미지 경로 생성
     * @param {String} color 색상
     * @returns {String} 이미지 경로
     */
    getStatusImageSrc: function(color) {
        var imageMap = {
            'green': '/images/green.png',
            'red': '/images/red.png',
            'gray': '/images/gray.png'
        };
        return imageMap[color] || '/images/gray.png';
    },
    
    /**
     * 설정값 업데이트 (깜빡임 방지)
     * @param {String} uuid 장치 UUID
     * @param {Object} settings 설정값 객체
     */
    updateSensorSettings: function(uuid, settings) {
        try {
            var lockKey = 'settings_' + uuid;
            
            if (!this.acquireLock(lockKey)) {
                console.log(`[${uuid}] 설정값 업데이트 대기 중...`);
                setTimeout(() => this.updateSensorSettings(uuid, settings), 100);
                return;
            }
            
            console.log(`[${uuid}] 설정값 업데이트 시작:`, settings);
            
            // 설정온도 (p01) 처리
            if (settings.p01 !== undefined) {
                var setTemp = parseFloat(settings.p01) / 10;
                var setTempElement = $('#setTmp' + uuid);
                
                if (setTempElement.length > 0) {
                    var newDisplay = '<font size="20px" style="font-size: 20px !important;">' + setTemp + '°C</font>';
                    var currentDisplay = setTempElement.html();
                    
                    if (currentDisplay !== newDisplay) {
                        setTempElement.html(newDisplay);
                        console.log(`[${uuid}] 설정온도 업데이트: ${setTemp}°C`);
                    }
                } else {
                    console.warn(`[${uuid}] 설정온도 표시 요소를 찾을 수 없음`);
                }
            }
            
            // 장치 타입 (p16) 처리
            if (settings.p16 !== undefined) {
                this.deviceStates[uuid].deviceType = settings.p16;
                console.log(`[${uuid}] 장치 타입 업데이트: ${settings.p16}`);
            }
            
            this.releaseLock(lockKey);
            console.log(`[${uuid}] 설정값 업데이트 완료`);
            
        } catch (e) {
            console.error(`[${uuid}] 설정값 업데이트 오류:`, e);
            this.releaseLock(lockKey);
        }
    },
    
    /**
     * 에러 상태 체크 및 업데이트
     * @param {String} uuid 장치 UUID
     */
    checkErrorStatus: function(uuid) {
        try {
            if (!this.deviceStates[uuid]) {
                this.initializeDevice(uuid);
            }
            
            var device = this.deviceStates[uuid];
            var now = Date.now();
            var timeDiff = now - device.lastDataTime;
            
            // 15초 동안 데이터 미수신 시 에러 체크
            if (timeDiff > this.CONSTANTS.ERROR_THRESHOLD_TIME) {
                if (!device.errorStates) {
                    device.errorCounters++;
                    
                    console.log(`[${uuid}] 에러 체크: timeDiff=${timeDiff}ms, counter=${device.errorCounters}`);
                    
                    // 3번 연속 미수신 시 에러 상태로 변경
                    if (device.errorCounters >= this.CONSTANTS.ERROR_RETRY_COUNT) {
                        device.errorStates = true;
                        
                        // 현재온도 표시를 Error로 변경
                        this.updateCurrentTemperature(uuid, "Error", true);
                        
                        // 상태표시등 업데이트
                        this.updateStatusIndicator(uuid, 'status', 'gray');
                        this.updateStatusIndicator(uuid, 'error', 'red');
                        
                        console.log(`[${uuid}] 통신에러 상태로 변경: ${device.errorCounters}번 연속 미수신`);
                    }
                }
            } else {
                // 정상 데이터 수신 시 에러 카운터 리셋
                if (device.errorCounters > 0) {
                    device.errorCounters = 0;
                    console.log(`[${uuid}] 에러 카운터 리셋`);
                }
            }
            
        } catch (e) {
            console.error(`[${uuid}] 에러 상태 체크 오류:`, e);
        }
    },
    
    /**
     * 에러 상태 해제
     * @param {String} uuid 장치 UUID
     */
    clearError: function(uuid) {
        try {
            if (!this.deviceStates[uuid]) {
                return;
            }
            
            var device = this.deviceStates[uuid];
            device.errorStates = false;
            device.errorCounters = 0;
            
            // 상태표시등 업데이트
            this.updateStatusIndicator(uuid, 'status', 'green');
            this.updateStatusIndicator(uuid, 'error', 'gray');
            
            console.log(`[${uuid}] 에러 상태 해제`);
            
        } catch (e) {
            console.error(`[${uuid}] 에러 상태 해제 오류:`, e);
        }
    },
    
    /**
     * 모든 장치의 에러 상태 체크 (주기적 호출)
     */
    checkAllDevicesErrorStatus: function() {
        Object.keys(this.deviceStates).forEach(function(uuid) {
            window.RealtimeDisplayManager.checkErrorStatus(uuid);
        });
    },
    
    /**
     * 락 정리 (주기적 호출)
     */
    cleanup: function() {
        this.cleanupLocks();
    }
};

// 주기적 정리 작업
setInterval(function() {
    window.RealtimeDisplayManager.cleanup();
    window.RealtimeDisplayManager.checkAllDevicesErrorStatus();
}, 5000); // 5초마다 실행

// 전역 함수로 노출 (기존 코드 호환성)
window.updateCurrentTemperature = function(uuid, value, isError) {
    window.RealtimeDisplayManager.updateCurrentTemperature(uuid, value, isError);
};

// updateStatusIndicator 함수는 src/main/resources/static/js/common-utils.js에서 제공됨 (중복 제거)

window.updateSensorSettings = function(uuid, settings) {
    window.RealtimeDisplayManager.updateSensorSettings(uuid, settings);
};

window.clearError = function(uuid) {
    window.RealtimeDisplayManager.clearError(uuid);
};
