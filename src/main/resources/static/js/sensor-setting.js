/**
 * 장치설정페이지 전용 JavaScript
 * HnT Sensor API 프로젝트
 */

// 장치설정페이지 전용 변수들
var SensorSettingPage = {
    // 페이지 초기화 상태
    initialized: false,
    
    // MQTT 관련 변수들
    mqttClient: null,
    currentTopic: '',
    currentUserId: '',
    currentSensorUuid: '',
    
    // 장치 상태 변수들
    deviceStatusStates: {},
    deviceErrorDisplayStates: {},
    deviceTempStates: {},
    deviceDinErrorStates: {},
    
    // 파라미터 설정 변수들
    parameterValues: {},
    
    // 알람 설정 변수들
    alarmSettings: {}
};

/**
 * 페이지 초기화
 */
function initializeSensorSettingPage() {
    console.log('=== 장치설정페이지 초기화 시작 ===');
    
    if (SensorSettingPage.initialized) {
        console.log('이미 초기화됨 - 재초기화 건너뜀');
        return;
    }
    
    // 세션 데이터 설정
    setupSessionData();
    
    // MQTT 연결 초기화
    initializeMqttConnection();
    
    // 이벤트 리스너 설정
    setupEventListeners();
    
    // UI 초기화
    initializeUI();
    
    SensorSettingPage.initialized = true;
    console.log('=== 장치설정페이지 초기화 완료 ===');
}

/**
 * 세션 데이터 설정
 */
function setupSessionData() {
    // 전역 세션 정보 객체 설정
    window.SessionData = {
        userId: '${userId}',
        userGrade: '${userGrade}',
        userNm: '${userNm}',
        userEmail: '${userEmail}',
        sensorUuid: '${sensorUuid}',
        sensorId: '${sensorId}',
        topicStr: 'HBEE/${userId}/TC/${sensorUuid}/SER'
    };
    
    // MQTT 메시지 필터링을 위한 현재 사용자 ID 설정
    window.currentUserId = '${userId}';
    
    // 페이지별 변수 설정
    SensorSettingPage.currentUserId = '${userId}';
    SensorSettingPage.currentSensorUuid = '${sensorUuid}';
    SensorSettingPage.currentTopic = 'HBEE/${userId}/TC/${sensorUuid}/SER';
    
    console.log('세션 데이터 설정 완료:', window.SessionData);
}

/**
 * MQTT 연결 초기화
 */
function initializeMqttConnection() {
    console.log('MQTT 연결 초기화 시작');
    
    // MQTT 클라이언트가 이미 있는지 확인
    if (typeof client !== 'undefined' && client) {
        SensorSettingPage.mqttClient = client;
        console.log('기존 MQTT 클라이언트 사용');
    } else {
        console.log('새 MQTT 클라이언트 생성 필요');
        // MQTT 연결은 mqtt_lib.js에서 처리
    }
}

/**
 * 이벤트 리스너 설정
 */
function setupEventListeners() {
    console.log('이벤트 리스너 설정 시작');
    
    // 페이지 로드 완료 이벤트
    $(document).ready(function() {
        console.log('jQuery ready 이벤트');
        onPageLoadComplete();
    });
    
    // MQTT 연결 성공 이벤트
    document.addEventListener('mqtt:connected', function(event) {
        console.log('MQTT 연결 성공 이벤트 수신');
        onMqttConnected();
    });
    
    // MQTT 메시지 수신 이벤트
    document.addEventListener('mqtt:message', function(event) {
        console.log('MQTT 메시지 이벤트 수신:', event.detail);
        handleMqttMessage(event.detail.topic, event.detail.payload);
    });
    
    // 버튼 클릭 이벤트들
    setupButtonEventListeners();
}

/**
 * 버튼 이벤트 리스너 설정
 */
function setupButtonEventListeners() {
    // 강제제상 버튼들
    $('#defrost').on('click', function() {
        startDefrost();
    });
    
    $('#stopDefrost').on('click', function() {
        stopDefrost();
    });
    
    // 저장 버튼들
    $('#save1').on('click', function() {
        saveSensorSetting();
    });
    
    $('#save2').on('click', function() {
        saveAlarmSetting();
    });
    
    // 새로고침 버튼
    $('a[onclick="refreshDeviceSettings()"]').on('click', function() {
        refreshDeviceSettings();
    });
    
    // 출력 제어 버튼들
    $('.output-control-btn').on('click', function() {
        var type = $(this).data('type');
        var channel = $(this).data('channel');
        var value = $(this).data('value');
        controlOutput(type, channel, value);
    });
}

/**
 * UI 초기화
 */
function initializeUI() {
    console.log('UI 초기화 시작');
    
    // 권한 체크
    checkUserPermissions();
    
    // 장치 정보 표시
    displayDeviceInfo();
    
    // 알람 설정 로드
    loadAlarmSettings();
    
    // 파라미터 설정 로드
    loadParameterSettings();
}

/**
 * 사용자 권한 체크
 */
function checkUserPermissions() {
    var userGrade = window.SessionData?.userGrade || '${userGrade}';
    
    if (userGrade === 'B') {
        // 부계정은 설정 변경 불가
        $('.admin-only').hide();
        $('.save-btn').prop('disabled', true);
        console.log('부계정 사용자 - 설정 변경 기능 비활성화');
    }
}

/**
 * 장치 정보 표시
 */
function displayDeviceInfo() {
    var sensorName = '${sensorName}' || '알 수 없는 장치';
    var sensorType = '${sensorType}' || 'NTC';
    var tempRange = '${tempRange}' || '-50°C ~ 125°C';
    
    $('#deviceName').text(sensorName);
    $('#deviceSensorInfoDisplay').text('${sensorDisplayName}' || 'NTC(T1)');
    $('#deviceTempRangeDisplay').text(tempRange);
    $('#deviceTypeDisplay').text('Cooler (냉각기)');
    
    console.log('장치 정보 표시 완료:', {
        sensorName: sensorName,
        sensorType: sensorType,
        tempRange: tempRange
    });
}

/**
 * 알람 설정 로드
 */
function loadAlarmSettings() {
    try {
        var alarmMapStr = $('#alarmMap').val();
        if (alarmMapStr && alarmMapStr !== '') {
            var alarmMap = JSON.parse(alarmMapStr);
            SensorSettingPage.alarmSettings = alarmMap;
            
            // 알람 설정 폼에 값 설정
            populateAlarmForm(alarmMap);
            
            console.log('알람 설정 로드 완료:', alarmMap);
        }
    } catch (error) {
        console.error('알람 설정 로드 실패:', error);
    }
}

/**
 * 알람 설정 폼에 값 설정
 */
function populateAlarmForm(alarmMap) {
    // 알람 사용 여부
    $('input[name="alarmYn1"]').prop('checked', alarmMap.alarm_yn1 === 'Y');
    $('input[name="alarmYn2"]').prop('checked', alarmMap.alarm_yn2 === 'Y');
    $('input[name="alarmYn3"]').prop('checked', alarmMap.alarm_yn3 === 'Y');
    $('input[name="alarmYn4"]').prop('checked', alarmMap.alarm_yn4 === 'Y');
    $('input[name="alarmYn5"]').prop('checked', alarmMap.alarm_yn5 === 'Y');
    
    // 설정값
    $('input[name="setVal1"]').val(alarmMap.set_val1 || '');
    $('input[name="setVal2"]').val(alarmMap.set_val2 || '');
    $('input[name="setVal3"]').val(alarmMap.set_val3 || '');
    
    // 지연시간 (분 단위로 변환)
    var delay1 = parseInt(alarmMap.delay_time1 || 0);
    $('input[name="delayHour1"]').val(Math.floor(delay1 / 60));
    $('input[name="delayMin1"]').val(delay1 % 60);
    
    var delay2 = parseInt(alarmMap.delay_time2 || 0);
    $('input[name="delayHour2"]').val(Math.floor(delay2 / 60));
    $('input[name="delayMin2"]').val(delay2 % 60);
    
    var delay3 = parseInt(alarmMap.delay_time3 || 0);
    $('input[name="delayHour3"]').val(Math.floor(delay3 / 60));
    $('input[name="delayMin3"]').val(delay3 % 60);
    
    var delay4 = parseInt(alarmMap.delay_time4 || 0);
    $('input[name="delayHour4"]').val(Math.floor(delay4 / 60));
    $('input[name="delayMin4"]').val(delay4 % 60);
    
    var delay5 = parseInt(alarmMap.delay_time5 || 0);
    $('input[name="delayHour5"]').val(Math.floor(delay5 / 60));
    $('input[name="delayMin5"]').val(delay5 % 60);
    
    // 재전송 지연시간
    var reDelay1 = parseInt(alarmMap.re_delay_time1 || 0);
    $('input[name="reDelayHour1"]').val(Math.floor(reDelay1 / 60));
    $('input[name="reDelayMin1"]').val(reDelay1 % 60);
    
    var reDelay2 = parseInt(alarmMap.re_delay_time2 || 0);
    $('input[name="reDelayHour2"]').val(Math.floor(reDelay2 / 60));
    $('input[name="reDelayMin2"]').val(reDelay2 % 60);
    
    var reDelay3 = parseInt(alarmMap.re_delay_time3 || 0);
    $('input[name="reDelayHour3"]').val(Math.floor(reDelay3 / 60));
    $('input[name="reDelayMin3"]').val(reDelay3 % 60);
    
    var reDelay4 = parseInt(alarmMap.re_delay_time4 || 0);
    $('input[name="reDelayHour4"]').val(Math.floor(reDelay4 / 60));
    $('input[name="reDelayMin4"]').val(reDelay4 % 60);
    
    var reDelay5 = parseInt(alarmMap.re_delay_time5 || 0);
    $('input[name="reDelayHour5"]').val(Math.floor(reDelay5 / 60));
    $('input[name="reDelayMin5"]').val(reDelay5 % 60);
}

/**
 * 파라미터 설정 로드
 */
function loadParameterSettings() {
    // 파라미터 설정은 MQTT 응답에서 받아옴
    console.log('파라미터 설정 로드 대기 중...');
}

/**
 * 페이지 로드 완료 처리
 */
function onPageLoadComplete() {
    console.log('페이지 로드 완료');
    
    // MQTT 연결 상태 확인
    if (typeof checkAndReconnectIfNeeded === 'function') {
        checkAndReconnectIfNeeded();
    }
}

/**
 * MQTT 연결 성공 처리
 */
function onMqttConnected() {
    console.log('MQTT 연결 성공 - 초기 설정 요청');
    
    // 초기 설정 요청
    setTimeout(function() {
        sendMqttCommand('GET&type=1');
    }, 500);
    
    setTimeout(function() {
        sendMqttCommand('GET&type=2');
    }, 2500);
}

/**
 * MQTT 메시지 처리
 */
function handleMqttMessage(topic, message) {
    console.log('MQTT 메시지 처리:', topic, message);
    
    // 토픽 파싱
    var topicParts = topic.split('/');
    if (topicParts.length < 4) {
        console.log('토픽 형식이 올바르지 않음:', topic);
        return;
    }
    
    var topicUserId = topicParts[1];
    var topicUuid = topicParts[3];
    
    // 현재 사용자와 장치 확인
    if (topicUserId !== SensorSettingPage.currentUserId || 
        topicUuid !== SensorSettingPage.currentSensorUuid) {
        console.log('다른 사용자 또는 장치의 메시지 - 무시:', topicUserId, topicUuid);
        return;
    }
    
    // JSON 파싱
    try {
        var messageObj = JSON.parse(message);
        processMqttMessage(messageObj);
    } catch (error) {
        console.error('JSON 파싱 실패:', error);
    }
}

/**
 * MQTT 메시지 처리 (기존 로직 유지)
 */
function processMqttMessage(messageObj) {
    var actcode = messageObj.actcode;
    
    if (actcode === 'live') {
        // 실시간 데이터 처리
        handleLiveMessage(messageObj);
    } else if (actcode === 'setres') {
        // 설정 응답 처리
        handleSetResMessage(messageObj);
    } else if (actcode === 'actres') {
        // 액션 응답 처리
        handleActResMessage(messageObj);
    }
}

/**
 * 실시간 데이터 처리
 */
function handleLiveMessage(messageObj) {
    var name = messageObj.name;
    var value = messageObj.value;
    
    if (name === 'ain') {
        // 현재온도 업데이트
        updateCurrentTemperature(value);
    } else if (name === 'din') {
        // DIN 상태 처리
        handleDinStatus(messageObj);
    } else if (name === 'output') {
        // 출력 상태 처리
        handleOutputStatus(messageObj);
    }
}

/**
 * 현재온도 업데이트
 * @param {string} sensorUuid - 센서 UUID
 * @param {string} value - 온도 값
 * @param {boolean} isError - 에러 상태 (선택적)
 */
function updateCurrentTemperature(sensorUuid, value, isError) {
    // 센서 설정 페이지는 단일 센서만 처리
    var currentSensorUuid = document.getElementById('sensorUuid') ? document.getElementById('sensorUuid').value : null;
    
    // 현재 페이지의 센서 UUID와 일치하는지 확인
    if (currentSensorUuid && sensorUuid !== currentSensorUuid) {
        console.log('현재온도 업데이트 - 다른 장치의 메시지 필터링됨:', sensorUuid, '!==', currentSensorUuid);
        return;
    }
    
    if (value === 'Error' || isError) {
        $('#curTemp').html('<font size="30px" style="color: #c9302c;">Error</font>');
    } else {
        $('#curTemp').html('<font size="30px" style="color: #c9302c;">' + value + '</font> °C');
    }
    
    console.log('현재온도 업데이트:', sensorUuid, value);
}

/**
 * 설정 응답(setres) 메시지 처리
 * @param {object} msg - setres 메시지 객체
 */
window.handleSetresMessage = function(msg) {
    console.log('handleSetresMessage 호출됨:', msg);
    
    // 장치 응답 데이터 저장 (문자열 형태로 저장)
    window.deviceResponseData = {
        p01: String(msg.p01 || ""),
        p02: String(msg.p02 || ""),
        p03: String(msg.p03 || ""),
        p04: String(msg.p04 || ""),
        p05: String(msg.p05 || ""),
        p06: String(msg.p06 || ""),
        p07: String(msg.p07 || ""),
        p08: String(msg.p08 || ""),
        p09: String(msg.p09 || ""),
        p10: String(msg.p10 || ""),
        p11: String(msg.p11 || ""),
        p12: String(msg.p12 || ""),
        p13: String(msg.p13 || ""),
        p14: String(msg.p14 || ""),
        p15: String(msg.p15 || ""),
        p16: String(msg.p16 || "")
    };
    
    // 응답 수신 플래그 설정
    window.deviceResponseReceived = true;
    
    // setres 메시지에서 받은 값들을 originalValues에 저장 (페이지 로드 시 초기값으로 사용)
    window.originalValues = {
        p01: msg.p01 ? (function() {
            var p01Value = parseInt(msg.p01);
            var normalizedValue = p01Value / 10;
            if(normalizedValue === Math.floor(normalizedValue)) {
                return normalizedValue.toString() + ".0";
            } else {
                return normalizedValue.toString();
            }
        })() : "",
        p02: msg.p02 ? (function() {
            var p02Value = parseInt(msg.p02) / 10;
            if(p02Value === Math.floor(p02Value)) {
                return p02Value.toString() + ".0";
            } else {
                return p02Value.toString();
            }
        })() : "",
        p03: msg.p03 || "",
        p04: msg.p04 ? ((parseInt(msg.p04) / 10).toString() + ".0") : "",
        p05: msg.p05 || "",
        p06: msg.p06 || "",
        p07: msg.p07 || "",
        p08: msg.p08 ? msg.p08.toString() : "",
        p09: msg.p09 || "",
        p10: msg.p10 ? (function() {
            var p10Value = parseInt(msg.p10) / 10;
            if(p10Value === 0) {
                return "0.0";
            } else if(p10Value === Math.floor(p10Value)) {
                return p10Value.toString() + ".0";
            } else {
                return p10Value.toString();
            }
        })() : "",
        p11: msg.p11 || "",
        p12: msg.p12 || "",
        p13: msg.p13 || "",
        p14: msg.p14 || "",
        p15: msg.p15 || "",
        p16: msg.p16 || ""
    };
    
    // 출력 버튼 상태 리셋
    try {
        if (typeof updateOutputButtonsEnabled === 'function') {
            updateOutputButtonsEnabled();
        }
        $('.out-btn').removeClass('is-active');
    } catch(e) {
        console.error('출력 버튼 상태 업데이트 실패:', e);
    }
    
    // p01 처리 - 단순히 10으로 나누어 디코딩
    if(msg.p01) {
        var p01Value = parseInt(msg.p01);
        var normalizedValue = p01Value / 10;
        if(normalizedValue === Math.floor(normalizedValue)) {
            msg.p01 = normalizedValue.toString() + ".0";
        } else {
            msg.p01 = normalizedValue.toString();
        }
    }
    $('#setTemp').html(msg.p01 + '°C');
    $('#p01').val(msg.p01);
    
    // p02 처리
    if(msg.p02) {
        var p02Value = parseInt(msg.p02) / 10;
        if(p02Value === Math.floor(p02Value)) {
            msg.p02 = p02Value.toString() + ".0";
        } else {
            msg.p02 = p02Value.toString();
        }
    }
    $('#p02').val(msg.p02);
    
    $('#p03').val(msg.p03);
    
    // p04 처리
    if(msg.p04) {
        var p04Value = parseInt(msg.p04);
        var normalizedValue = p04Value / 10;
        if(normalizedValue === Math.floor(normalizedValue)) {
            msg.p04 = normalizedValue.toString() + ".0";
        } else {
            msg.p04 = normalizedValue.toString();
        }
        if(normalizedValue === 0) {
            msg.p04 = "0.0";
        }
    }
    $('#p04').val(msg.p04);
    
    $('#p05').val(msg.p05);
    $('#p06').val(msg.p06);
    $('#p07').val(msg.p07).prop("selected", true);
    $('#p08').val(msg.p08);
    $('#p09').val(msg.p09);
    
    // p10 처리
    if(msg.p10) {
        var p10Value = parseInt(msg.p10) / 10;
        if(p10Value === Math.floor(p10Value)) {
            msg.p10 = p10Value.toString() + ".0";
        } else {
            msg.p10 = p10Value.toString();
        }
        if(p10Value === 0) {
            msg.p10 = "0.0";
        }
    }
    $('#p10').val(msg.p10);
    
    $('#p11').val(msg.p11).prop("selected", true);
    $('#p12').val(msg.p12).prop("selected", true);
    $('#p13').val(msg.p13).prop("selected", true);
    $('#p14').val(msg.p14).prop("selected", true);
    $('#p15').val(msg.p15).prop("selected", true);
    $('#p16').val(msg.p16).prop("selected", true);
    
    // 장치종류에 따른 상태표시등 텍스트 및 아이콘 변경
    if(msg.p16) {
        if(msg.p16 == "1") {
            // Heater
            $('#defrostLabel').text("히터");
            $('#defr .defrost-icon').hide();
            $('#defr .heater-icon').show();
        } else {
            // Cooler
            $('#defrostLabel').text("제상");
            $('#defr .defrost-icon').show();
            $('#defr .heater-icon').hide();
        }
    }
    
    // 센서 정보 업데이트
    if (typeof updateSensorInfo === 'function') {
        updateSensorInfo(msg);
    }
    
    console.log('setres 메시지 처리 완료');
}

/**
 * DIN 상태 처리
 */
function handleDinStatus(messageObj) {
    var value = parseInt(messageObj.value);
    var uuid = SensorSettingPage.currentSensorUuid;
    
    if (value === 1) {
        // DIN 이상 상태
        SensorSettingPage.deviceDinErrorStates[uuid] = true;
        updateStatusIndicator('error', 'red');
        updateStatusIndicator('operation', 'green');
    } else if (value === 0) {
        // DIN 정상 상태
        SensorSettingPage.deviceDinErrorStates[uuid] = false;
        updateStatusIndicator('error', 'gray');
        updateStatusIndicator('operation', 'green');
    }
}

/**
 * 출력 상태 처리
 */
function handleOutputStatus(messageObj) {
    var type = messageObj.type;
    var channel = messageObj.ch;
    var value = parseInt(messageObj.value);
    
    // 출력 상태에 따른 UI 업데이트
    var statusClass = value === 1 ? 'red' : 'gray';
    
    if (type === '1') { // COMP
        updateStatusIndicator('comp', statusClass);
    } else if (type === '2') { // DEF
        updateStatusIndicator('defrost', statusClass);
    } else if (type === '3') { // FAN
        updateStatusIndicator('fan', statusClass);
    }
}

/**
 * 상태표시등 업데이트
 * @param {string} sensorUuid - 센서 UUID (선택적, 단일 센서 페이지에서는 무시)
 * @param {string} type - 상태 타입 ('status', 'comp', 'defr', 'fan', 'error')
 * @param {string} status - 상태 색상 ('gray', 'green', 'red')
 */
function updateStatusIndicator(sensorUuid, type, status) {
    // 3개 파라미터 모드 (센서설정 페이지)
    if (arguments.length === 3) {
        // sensorUuid는 무시 (단일 센서 페이지)
        var actualType = type;
        var actualStatus = status;
    } 
    // 2개 파라미터 모드 (레거시 호환)
    else if (arguments.length === 2) {
        var actualType = sensorUuid; // 첫 번째 인자가 type
        var actualStatus = type;      // 두 번째 인자가 status
    } else {
        console.error('updateStatusIndicator: 잘못된 파라미터 개수', arguments);
        return;
    }
    
    // 센서 설정 페이지에서는 UUID를 포함한 요소 ID 사용
    var sensorUuidElement = document.getElementById('sensorUuid');
    var sensorUuid = sensorUuidElement ? sensorUuidElement.value : '';
    var elementId = actualType + sensorUuid;
    var selector = '#' + elementId;
    var element = $(selector);
    
    if (element.length > 0) {
        // 기존 색상 클래스 제거
        element.removeClass('green red gray');
        
        // 새로운 색상 클래스 추가
        element.addClass(actualStatus);
        
        console.log('상태표시등 업데이트:', elementId, actualStatus, '클래스:', element.attr('class'));
    } else {
        console.warn('상태표시등 요소를 찾을 수 없음:', elementId);
    }
}

/**
 * 설정 응답 처리
 */
function handleSetResMessage(messageObj) {
    console.log('설정 응답 수신:', messageObj);
    
    // 설정온도 업데이트
    if (messageObj.p01) {
        var setTemp = parseFloat(messageObj.p01) / 10;
        $('#setTemp').html('<font size="30px" style="color: #3c763d;">' + setTemp + '</font> °C');
    }
    
    // 파라미터 값들 저장
    for (var key in messageObj) {
        if (key.startsWith('p')) {
            SensorSettingPage.parameterValues[key] = messageObj[key];
        }
    }
}

/**
 * 액션 응답 처리
 */
function handleActResMessage(messageObj) {
    console.log('액션 응답 수신:', messageObj);
    
    var name = messageObj.name;
    
    if (name === 'forcedef') {
        console.log('강제제상 액션 응답');
    } else if (name === 'output') {
        console.log('출력 제어 액션 응답');
    }
}

/**
 * 강제제상 시작
 */
function startDefrost() {
    console.log('강제제상 시작');
    sendMqttCommand('ACT&name=forcedef&value=1');
}

/**
 * 강제제상 종료
 */
function stopDefrost() {
    console.log('강제제상 종료');
    sendMqttCommand('ACT&name=forcedef&value=0');
}

/**
 * 센서 정보 업데이트
 */
function updateSensorInfo(msg) {
    console.log('센서 정보 업데이트:', msg);
    // 센서 정보 업데이트 로직 (필요시 구현)
}

/**
 * 온도 입력 검증
 */
function validateTemperatureInput(input) {
    var value = parseFloat(input.value);
    if (isNaN(value) || value < -50 || value > 125) {
        alert('온도는 -50°C ~ 125°C 범위 내에서 입력해주세요.');
        input.focus();
        return false;
    }
    return true;
}

/**
 * 센서 설정 저장
 */
function saveSensorSetting() {
    var userGrade = window.SessionData?.userGrade || '${userGrade}';
    
    if (userGrade === 'B') {
        alert("부계정은 설정을 변경할 수 없습니다.");
        return;
    }
    
    console.log('센서 설정 저장');
    // 실제 저장 로직은 JSP의 saveSensorSetting 함수에서 처리
    // 여기서는 중복 호출 방지만 처리
}

/**
 * 알람 설정 저장
 */
function saveAlarmSetting() {
    console.log('알람 설정 저장');
    // 기존 saveAlarmSetting 로직 사용
    if (typeof saveAlarmSetting === 'function') {
        saveAlarmSetting();
    }
}

/**
 * 장치 설정 새로고침
 */
function refreshDeviceSettings() {
    console.log('장치 설정 새로고침');
    
    // 설정값 읽기
    sendMqttCommand('GET&type=1');
    
    // 상태 읽기
    setTimeout(function() {
        sendMqttCommand('GET&type=2');
    }, 2000);
}

/**
 * 출력 제어
 */
function controlOutput(type, channel, value) {
    console.log('출력 제어:', type, channel, value);
    sendMqttCommand('ACT&name=output&type=' + type + '&ch=' + channel + '&value=' + value);
}

/**
 * MQTT 명령 전송
 */
function sendMqttCommand(command) {
    console.log('MQTT 명령 전송:', command);
    
    if (typeof MQTTConnectionPool !== 'undefined' && MQTTConnectionPool.publish) {
        MQTTConnectionPool.publish(SensorSettingPage.currentTopic, command);
    } else if (typeof client !== 'undefined' && client && client.isConnected()) {
        var message = new Paho.MQTT.Message(command);
        message.destinationName = SensorSettingPage.currentTopic;
        client.send(message);
    } else {
        console.warn('MQTT 클라이언트가 연결되지 않음');
    }
}

// 페이지 로드 시 자동 초기화
if (typeof $ !== 'undefined') {
    $(document).ready(function() {
        initializeSensorSettingPage();
    });
} else {
    document.addEventListener('DOMContentLoaded', function() {
        initializeSensorSettingPage();
    });
}

// 전역 함수로 노출 (기존 코드와의 호환성 유지)
window.SensorSettingPage = SensorSettingPage;
window.initializeSensorSettingPage = initializeSensorSettingPage;
window.startDefrost = startDefrost;
window.stopDefrost = stopDefrost;
window.saveSensorSetting = saveSensorSetting;
window.saveAlarmSetting = saveAlarmSetting;
window.refreshDeviceSettings = refreshDeviceSettings;
window.controlOutput = controlOutput;
