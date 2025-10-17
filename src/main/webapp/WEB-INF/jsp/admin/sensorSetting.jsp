<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="utf-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <meta charset="utf-8">
    <!--[if IE]><meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1"><![endif]-->
    <title>H&T Solutions</title>
    	<link rel="icon" href="/images/hntbi.png" type="image/png">
    <meta name="keywords" content="" />
    <meta name="description" content="" />
    <meta name="viewport" content="width=device-width">
    	<link rel="stylesheet" href="/css/templatemo_main.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
    <style>
        /* 상태표시등 스타일 (메인 페이지와 동일) */
        .status-indicator {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 25px;
            height: 25px;
            border-radius: 50%;
            transition: all 0.3s ease;
            box-shadow: 0 2px 4px rgba(0,0,0,0.2);
        }
        .status-indicator i {
            font-size: 16px;
            color: white;
        }
        .status-indicator.green { background: #4CAF50; box-shadow: 0 0 10px rgba(76, 175, 80, 0.3); }
        .status-indicator.gray { background: #9e9e9e; }
        .status-indicator.red { background: #f44336; box-shadow: 0 0 10px rgba(244, 67, 54, 0.3); }
        .status-indicator.active { animation: pulse 2s infinite; }
        
        @keyframes pulse {
            0% { transform: scale(1); }
            50% { transform: scale(1.1); }
            100% { transform: scale(1); }
        }
        
        /* 출력 제어 버튼 스타일 */
        .btn-soft {
            background-color: #f8f9fa;
            border: 1px solid #dee2e6;
            color: #495057;
            padding: 4px 8px;
            font-size: 12px;
            border-radius: 3px;
            cursor: pointer;
            transition: all 0.2s ease;
            margin: 2px;
        }
        .btn-soft:hover {
            background-color: #e9ecef;
            border-color: #adb5bd;
        }
        .btn-soft:disabled {
            background-color: #e9ecef;
            border-color: #dee2e6;
            color: #6c757d;
            cursor: not-allowed;
            opacity: 0.5;
        }
      /* 활성 상태 버튼 스타일 - 기존 색상 유지하면서 더 진하게 */
      .btn-soft.is-active {
        font-weight: bold;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
        transform: scale(1.05);
        border-width: 2px;
      }
      
      /* ON/OFF 구분용 아주 연한 색상 버튼 */
      .btn-on-soft {
        background-color: #e3f6ed;  /* 약간 더 진한 연한 그린 */
        border-color: #bfead6;
        color: #1f5535;
      }
      .btn-on-soft:hover { 
        background-color: #daf1e6; 
        box-shadow: 0 0 0 3px rgba(46,204,113,0.12); 
      }

      .btn-off-soft {
        background-color: #ffecec; /* 약간 더 진한 연한 레드 */
        border-color: #ffcfcf;
        color: #732020;
      }
      .btn-off-soft:hover { 
        background-color: #ffe3e3; 
        box-shadow: 0 0 0 3px rgba(231,76,60,0.12); 
      }

      /* 실행 상태 하이라이트 (명확한 구분) */
      .btn-on-soft.is-active {
        background-color: #28a745 !important; /* 진한 녹색 */
        border-color: #1e7e34 !important;
        color: white !important;
        font-weight: 900 !important;
        box-shadow: 0 4px 8px rgba(40, 167, 69, 0.4) !important;
        transform: scale(1.05) !important;
        border-width: 2px !important;
      }
      .btn-off-soft.is-active {
        background-color: #dc3545 !important; /* 진한 빨간색 */
        border-color: #bd2130 !important;
        color: white !important;
        font-weight: 900 !important;
        box-shadow: 0 4px 8px rgba(220, 53, 69, 0.4) !important;
        transform: scale(1.05) !important;
        border-width: 2px !important;
      }
      
      /* 비활성 상태는 기본 색상 유지 (투명도 제거) */
      .btn-on-soft:not(.is-active) {
        background-color: #e3f6ed !important;
        border-color: #bfead6 !important;
        color: #1f5535 !important;
      }
      .btn-off-soft:not(.is-active) {
        background-color: #ffecec !important;
        border-color: #ffcfcf !important;
        color: #732020 !important;
      }
        .btn-soft.is-active {
            background-color: #007bff;
            border-color: #007bff;
            color: white;
            box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
        }
        .out-btn {
            width: 64px;
            height: 30px;
            margin-right: 6px;
            border-radius: 4px;
        }
    </style>
  <!-- 통일된 세션 관리 모듈 사용 -->
<script src="/js/session-manager.js"></script>
<!-- 부계정 권한 제한 모듈 사용 -->
<script src="/js/sub-account-permission.js"></script>
<!-- 공통 파라미터 유틸리티 -->
<script src="/js/common-parameter-utils.js"></script>
<!-- 공통 상태 표시 유틸리티 -->
<script src="/js/common-status-utils.js"></script>
<!-- 표준 MQTT 초기화 템플릿 -->
<script src="/js/mqtt-init-template.js"></script>
<!-- Paho MQTT 라이브러리 -->
<script src="/js/mqttws31-min.js"></script>
<!-- 통일된 MQTT 관리 모듈 사용 (버전 쿼리로 캐시 무효화) -->
<script src="/js/unified-mqtt-manager.js?v=20251007"></script>
<!-- 센서 설정 페이지 전용 JavaScript -->
<script>
// sensor-setting.js에서 사용할 변수들을 JSP에서 설정
window.SensorSettingPage = window.SensorSettingPage || {};
window.SensorSettingPage.currentUserId = '${userId}';
window.SensorSettingPage.currentSensorUuid = '${sensorUuid}';
window.SensorSettingPage.currentTopic = 'HBEE/' + window.SensorSettingPage.currentUserId + '/TC/' + window.SensorSettingPage.currentSensorUuid + '/SER';

console.log('센서설정 페이지 변수 설정:', {
    userId: window.SensorSettingPage.currentUserId,
    sensorUuid: window.SensorSettingPage.currentSensorUuid,
    topic: window.SensorSettingPage.currentTopic
});

// 온도 값 포맷팅 함수 (전역 함수로 선언 - 페이지 초기 로딩 시에도 사용 가능)
function formatTemperatureValue(value) {
    if(value === null || value === undefined || value === '') {
        return '';
    }
    
    // 문자열을 숫자로 변환
    var numValue = parseFloat(value);
    
    // NaN 체크
    if(isNaN(numValue)) {
        return value; // 숫자가 아니면 원본 반환
    }
    
    // 소숫점 1자리로 고정
    return numValue.toFixed(1);
}

console.log('formatTemperatureValue 함수 정의 완료');
</script>
<script src="/js/sensor-setting.js"></script>

</head>
<body>
<input type="hidden" id="userId" name="userId" value="${userId}" />
<input type="hidden" id="userNm" name="userNm" value="${userNm}" />
<input type="hidden" id="userGrade" name="userGrade" value="${userGrade}" />
<input type="hidden" id ="topicStr" name="topicStr" value="${topicStr}" />
<input type="hidden" id="sensorUuid" name="sensorUuid" value="${sensorUuid}" />
<input type="hidden" id="sensor_name" name="sensor_name" value="${sensor_name}" />
<input type="hidden" id="alarmMap" name="alarmMap" value='${alarmMap}' />
<input type="hidden" id="token" name="token" value="${token}" />
<input type="hidden" id="sensorId" name="sensorId" value="${sensorId}" />
<input type="hidden" id="loginUserId" name="loginUserId" value="${loginUserId}"/>

<div class="navbar navbar-inverse" role="navigation" style="background-color: #ffffff">
    <div class="navbar-header">
		<div class="logo"><h1><a href="/main/main"><img src="/images/hntbi.png" width="70" height="32"></a></h1></div>
        <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse" style="background-color: #cccccc">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
        </button>
    </div>
</div>

<div class="template-page-wrapper">
    <div class="navbar-collapse collapse templatemo-sidebar">
        <ul class="templatemo-sidebar-menu">
            <li class="active"><a href="#"><i class="fa fa-home"></i>Dashboard</a></li>
            <li class="sub open">
                <ul style="background-color: #afd9ee; height: 40px; padding-top: 10px">
                    <strong>${userNm}님 안녕하세요.</strong>
                </ul>
				<c:if test="${userGrade eq 'A' || userGrade eq 'U'}">
					<a href="javascript:;">
						<i class="fa fa-database"></i> 관리 메뉴보기 <div class="pull-right"><span class="caret"></span></div>
					</a>
					<ul class="templatemo-submenu">
						<li><a href="/admin/userList?userId=${userId}&userGrade=${userGrade}">사용자 관리</a></li>
					</ul>
					<ul class="templatemo-submenu">
						<li><a href="/admin/createSub?userId=${userId}&userGrade=${userGrade}">부계정 생성</a></li>
					</ul>
            </c:if>
            </li>
            <li><a href="" data-toggle="modal" data-target="#confirmModal"><i class="fa fa-sign-out"></i>로그아웃</a></li>
        </ul>
    </div><!--/.navbar-collapse -->

    <div class="templatemo-content-wrapper" style="background-color: #333333">
        <div class="templatemo-content" style="background-color: #333333">
            <ol class="breadcrumb">
                <li><a href="/main/main" onclick="goMain()">Main</a></li>
            </ol>
            <h1><span style="color: #f0f8ff; ">장치 설정</span></h1>
            <p><span style="color: #f0f8ff; ">장치 설정 화면입니다.</span></p>
            <div class="templatemo-panels">
                <div class="row">
                    <div class="" style="margin-right: 10px; margin-left: 10px;">
                        <span class="btn btn-primary"><a href="javascript:void(0);" onclick="refreshDeviceSettings()">장치설정</a></span>
                        <div class="panel panel-primary">
                            <div class="panel-heading"><strong><span id="deviceName${sensorUuid}">${sensor_name}</span> <span id="deviceType${sensorUuid}" style="font-size: 12px; color: #ccc;">(장치)</span></strong></div>
                            <div class="panel-body">
                                                            <!-- 센서 정보 표시 -->
                                <div style="background-color: #d9edf7; padding: 10px; margin-bottom: 15px; border-radius: 5px;">
                                    <div style="font-weight: bold; margin-bottom: 5px;">센서 정보</div>
                                    <div style="font-size: 12px; line-height: 1.4;">
                                        <div><strong>센서 정보:</strong> <span id="deviceSensorInfoDisplay">-</span></div>
                                        <div><strong>온도 범위:</strong> <span id="deviceTempRangeDisplay">-</span></div>
                                        <div><strong>장치 종류:</strong> <span id="deviceDeviceTypeDisplay">-</span></div>
                                    </div>
                                </div>
                                <table class="table table-striped">
                                    <thead>
                                        <tr>
	                                        <td align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">현재온도</span></strong></td>
	                                        <td align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">설정온도</span></strong></td>
	                                        <td align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">강제재상</span></strong></td>
                                        </tr>
                                    </thead>
									<tbody>
									    <tr>
											<td align="center" valign="middle"><strong><p align="center" id="curTemp" name="curTemp" style="font-size: 30px; color: #c9302c"></p></strong></td>
											<td align="center" valign="middle"><strong><span id="setTemp" name="setTemp" style="font-size: 30px; color: #3c763d"></span></strong></td>
                                            <td align="center" valign="middle" style="padding-right:12px;">
                                              <div style="display:flex; flex-direction:column; justify-content:center; align-items:center; gap:8px;">
                                                <button id="defrost" name="defrost" class="btn-soft btn-on-soft defrost-btn" style="font-size: 13px; width:140px;">강제제상</button>
                                                <button id="stopDefrost" name="defrost" class="btn-soft btn-off-soft defrost-btn" style="font-size: 13px; width:140px;">강제제상종료</button>
                                              </div>
                                            </td>
										</tr>
									    <tr>
											<td align="center" valign="middle" style="background-color: #c7254e;">
												<strong><span style="color: #f0f8ff; font-size:10pt;">상태</span></strong>
											</td>
											<td colspan="2">
												<!-- 상태표시 아이콘들 (메인 페이지와 동일한 2행 구조) -->
												<table class="table table-striped">
													<thead>
													<tr>
                                            <td align="center"><div id="status${sensorUuid != null ? sensorUuid : ''}" class="status-indicator green"><i class="bi bi-play-circle-fill"></i></div></td>
                                            <td align="center"><div id="comp${sensorUuid != null ? sensorUuid : ''}" class="status-indicator gray"><i class="bi bi-gear-fill"></i></div></td>
                                            <td align="center"><div id="defr${sensorUuid != null ? sensorUuid : ''}" class="status-indicator gray"><i class="bi bi-snow defrost-icon"></i><i class="bi bi-thermometer-half heater-icon" style="display:none;"></i></div></td>
                                            <td align="center"><div id="fan${sensorUuid != null ? sensorUuid : ''}" class="status-indicator gray"><i class="bi bi-fan"></i></div></td>
                                            <td align="center"><div id="error${sensorUuid != null ? sensorUuid : ''}" class="status-indicator gray"><i class="bi bi-exclamation-triangle-fill"></i></div></td>
													</tr>
													</thead>
													<tbody>
													<tr>
														<td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">운전</span></strong></td>
														<td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">콤프</span></strong></td>
														<td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span id="defrostLabel${sensorUuid != null ? sensorUuid : ''}" style="color: #f0f8ff; font-size:10pt;">제상</span></strong></td>
														<td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">FAN</span></strong></td>
														<td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">이상</span></strong></td>
													</tr>
													</tbody>
												</table>
                                        </td>
	                                    </tr>
                                    </tbody>
                                </table>
                                <table class="table table-striped">
                                    <thead>
	                                    <tr>
	                                        <td width="25%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">항목</span></strong></td>
	                                        <td width="25%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">설정값</span></strong></td>
	                                        <td width="25%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">항목</span></strong></td>
	                                        <td width="25%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">설정값</span></strong></td>
	                                    </tr>
                                    </thead>
                                    <tbody>
	                                    <tr>
	                                        <td align="center" valign="middle" style="font-size:10pt;">설정온도</td>
	                                        <td align="left" valign="middle" style="font-size:10pt;">
	                                            <c:if test="${userGrade ne 'B'}">
	                                                <input type="text" id="p01" name="p01" value="" style="width:50px;" onblur="validateTemperatureInput(this)" />°C
	                                            </c:if>
	                                            <c:if test="${userGrade eq 'B'}">
	                                                <input type="text" id="p01" name="p01" value="" style="width:50px; background-color: #f5f5f5;" readonly />°C
	                                            </c:if>
	                                        </td>
	                                        <td align="center" valign="middle" style="font-size:10pt;">장치종류</td>
	                                        <td align="left" valign="middle" style="font-size:10pt;">
												<select id="p16" name="p16" style="width:60px; font-size:10pt;">
													<option value="">선택</option>
													<option value="0">Cooler</option>
													<option value="1">Heater</option>
												</select>
											</td>
	                                    </tr>
	                                    <tr>
	                                        <td align="center" valign="middle" style="font-size:10pt;">히스테리시스<br>편차</td>
	                                        <td align="left" valign="middle" style="font-size:10pt;"><input type="text" id="p02" name="p02" value="" style="width:50px;" onblur="validateHysteresisInput(this)" />°C</td>
	                                        <td align="center" valign="middle" style="font-size:10pt;">제상 시간<br>(min)</td>
	                                        <td align="left" valign="middle" style="font-size:10pt;"><input type="text" id="p06" name="p06" value="" style="width:50px;" onblur="validateMinuteInput(this)" />분</td>
	                                    </tr>
	                                    <tr>
	                                        <td align="center" valign="middle" style="font-size:10pt;">COMP 출력<br>지연시간(sec)</td>
	                                        <td align="left" valign="middle" style="font-size:10pt;"><input type="text" id="p03" name="p03" value="" style="width:50px;" onblur="validateDelayInput(this)" />초</td>
	                                        <td align="center" valign="middle" style="font-size:10pt;">팬설정</td>
	                                        <td align="left" valign="middle" style="font-size:10pt;">
												<select id="p07" name="p07" style="width:60px; font-size:10pt;">
													<option value="">선택</option>
													<option value="0">F1</option>
													<option value="1">F2</option>
													<option value="2">F3</option>
													<option value="3">F4</option>
												</select>
											</td>
	                                    </tr>
	                                    <tr>
	                                        <td align="center" valign="middle" style="font-size:10pt;">온도보정</td>
	                                        <td align="left" valign="middle" style="font-size:10pt;"><input type="text" id="p04" name="p04" value="" style="width:50px;" onblur="validateTemperatureCompensationInput(this)" />°C</td>
	                                        <td align="center" valign="middle" style="font-size:10pt;">제상 후 FAN ON<br>지연시간(sec)</td>
	                                        <td align="left" valign="middle" style="font-size:10pt;"><input type="text" id="p08" name="p08" value="" style="width:50px;" onblur="validateDelayInput(this)" />초</td>
	                                    </tr>
	                                    <tr>
	                                        <td align="center" valign="middle" style="font-size:10pt;">제상 정지시간<br>(hour)</td>
	                                        <td align="left" valign="middle" style="font-size:10pt;"><input type="text" id="p05" name="p05" value="" style="width:50px;" onblur="validateHourInput(this)" />시간</td>
	                                        <td align="center" valign="middle" style="font-size:10pt;">FAN OFF<br>지연시간(sec)</td>
	                                        <td align="left" valign="middle" style="font-size:10pt;"><input type="text" id="p09" name="p09" value="" style="width:50px;" onblur="validateDelayInput(this)" />초</td>
	                                    </tr>
										<tr>
											<td align="center" valign="middle" style="font-size:10pt;">저온방지<br>온도편차</td>
											<td align="left" valign="middle" style="font-size:10pt;"><input type="text" id="p10" name="p10" value="" style="width:50px;" onblur="validateLowTempPreventionInput(this)" />°C</td>
											<td align="center" valign="middle" style="font-size:10pt;">COMP 누적 시간<br>제상 선택</td>
											<td align="left" valign="middle" style="font-size:10pt;">
												<select id="p11" name="p11" style="width:60px; font-size:10pt;">
													<option value="">선택</option>
													<option value="1">On</option>
													<option value="0">Off</option>
												</select>
											</td>
										</tr>
										<tr>
											<td align="center" valign="middle" style="font-size:10pt;">온도 센서<br>타입</td>
											<td align="left" valign="middle" style="font-size:10pt;">
												<select id="p12" name="p12" style="width:60px; font-size:10pt;">
													<option value="">선택</option>
													<option value="0">T1</option>
													<option value="1">T2</option>
													<option value="2">T3</option>
												</select>
											</td>
											<td align="center" valign="middle" style="font-size:10pt;">수동조작<br>on/off</td>
											<td align="left" valign="middle" style="font-size:10pt;">
												<select id="p13" name="p13" style="width:60px; font-size:10pt;">
													<option value="">선택</option>
													<option value="1">On</option>
													<option value="0">Off</option>
												</select>
											</td>
										</tr>
										<tr>
											<td align="center" valign="middle" style="font-size:10pt;">통신 국번</td>
											<td align="left" valign="middle" style="font-size:10pt;">
												<select id="p14" name="p14" style="width:60px; font-size:10pt;">
													<option value="">선택</option>
													<option value="1">1</option>
													<option value="2">2</option>
													<option value="3">3</option>
													<option value="4">4</option>
													<option value="5">5</option>
													<option value="6">6</option>
													<option value="7">7</option>
													<option value="8">8</option>
													<option value="9">9</option>
													<option value="10">10</option>
												</select>
											</td>
											<td align="center" valign="middle" style="font-size:10pt;">통신 속도</td>
											<td align="left" valign="middle" style="font-size:10pt;">
												<select id="p15" name="p15" style="width:60px; font-size:10pt;">
													<option value="">선택</option>
													<option value="0">1200</option>
													<option value="1">2400</option>
													<option value="2">4800</option>
													<option value="3">9600</option>
													<option value="4">19200</option>
												</select>
											</td>
										</tr>
	                                    <tr>
                                        <td colspan="4" align="center" style="font-size:10pt;">
                                            <div style="margin-bottom:8px;">
                                              <c:if test="${userGrade ne 'B'}">
                                                <!-- COMP(Type=1) -->
                                                <div style="display:flex; justify-content:center; align-items:center; margin:6px 0; gap:10px;">
                                                  <span style="width:60px; text-align:right; color:#333; font-weight:bold;">콤프 :</span>
                                                  <div style="display:inline-flex; gap:8px;">
                                                    <button class="out-btn btn-soft btn-on-soft device-output-btn" data-type="1" data-ch="1" data-val="1">ON</button>
                                                    <button class="out-btn btn-soft btn-off-soft device-output-btn" data-type="1" data-ch="1" data-val="0">OFF</button>
                                                  </div>
                                                </div>
                                                <!-- DEF(Type=2) -->
                                                <div style="display:flex; justify-content:center; align-items:center; margin:6px 0; gap:10px;">
                                                  <span style="width:60px; text-align:right; color:#333; font-weight:bold;">제상 :</span>
                                                  <div style="display:inline-flex; gap:8px;">
                                                    <button class="out-btn btn-soft btn-on-soft device-output-btn" data-type="2" data-ch="1" data-val="1">ON</button>
                                                    <button class="out-btn btn-soft btn-off-soft device-output-btn" data-type="2" data-ch="1" data-val="0">OFF</button>
                                                  </div>
                                                </div>
                                                <!-- FAN(Type=3) -->
                                                <div style="display:flex; justify-content:center; align-items:center; margin:6px 0; gap:10px;">
                                                  <span style="width:60px; text-align:right; color:#333; font-weight:bold;">팬 :</span>
                                                  <div style="display:inline-flex; gap:8px;">
                                                    <button class="out-btn btn-soft btn-on-soft device-output-btn" data-type="3" data-ch="1" data-val="1">ON</button>
                                                    <button class="out-btn btn-soft btn-off-soft device-output-btn" data-type="3" data-ch="1" data-val="0">OFF</button>
                                                  </div>
                                                </div>
                                              </c:if>
                                              <c:if test="${userGrade eq 'B'}">
                                                <div style="text-align:center;"><span style="color:#999; font-size:10pt;">읽기 전용 (출력 제어 불가)</span></div>
                                              </c:if>
                                            </div>
                                            <c:if test="${userGrade ne 'B'}">
                                                <button id="saveSensor" name="saveSensor" style="width:120px; height:30px;">장치설정저장</button>
                                            </c:if>
                                            <c:if test="${userGrade eq 'B'}">
                                                <span style="color: #999; font-size:10pt;">읽기 전용 (설정 변경 불가)</span>
                                            </c:if>
                                        </td>
                                    </tr>
                                    </tbody>
                                								</table>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="row">
                    <div class="" style="margin-right: 10px; margin-left: 10px;">
                        <span class="btn btn-primary"><a href="javascript:void(0);" onclick="refreshAlarmSettings()">알람설정</a></span>
                        <div class="panel panel-primary">
                            <div class="panel-heading"><strong><span id="deviceName2${sensorUuid}">${sensor_name}</span> <span id="deviceType2${sensorUuid}" style="font-size: 12px; color: #ccc;">(쿨러)</span></strong></div>
                            <div class="panel-body">
                                <!-- 센서 정보 표시 -->
                                <div style="background-color: #d9edf7; padding: 10px; margin-bottom: 15px; border-radius: 5px;">
                                    <div style="font-weight: bold; margin-bottom: 5px;">센서 정보</div>
                                    <div style="font-size: 12px; line-height: 1.4;">
                                        <div><strong>센서 정보:</strong> <span id="alarmSensorInfoDisplay">${sensorDisplayName != null ? sensorDisplayName : '-'}</span></div>
                                        <div><strong>온도 범위:</strong> <span id="alarmTempRangeDisplay">${tempRange != null ? tempRange : '-'}</span></div>
                                        <div><strong>장치 종류:</strong> <span id="alarmDeviceTypeDisplay">${deviceTypeText != null ? deviceTypeText : '-'}</span></div>
                                    </div>
                                </div>
								<table class="table table-striped">
									<tr>
										<th width="15%" align="center" rowspan="2" style="font-size: 12pt;">고온알람</th>
									</tr>
									<tr valign="middle">
										<td width="80%">
											<table border="1" style="border-color: #ffffff" width="100%">
												<tr>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">알람</span></strong></td>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">설정값</span></strong></td>
												</tr>
												<tr>
													<td align="center">
														<select id="alarmYn1" name="alarmYn1" style="width:65px; font-size:10pt;">
															<option value="Y">사용</option>
															<option value="N">미사용</option>
														</select>
													</td>
													<td align="center" style="font-size:10pt;">
														<input type="text" id="setVal1" name="setVal1" style="width:60px; font-size:10pt;">이상
													</td>
												</tr>
											</table>
											<table border="1" style="border-color: #ffffff" width="100%">
												<tr>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">지연시간</span></strong></td>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">재전송지연시간</span></strong></td>
												</tr>
												<tr>
													<td align="center">
														<select id="delayHour1" name="delayHour1" style="width:60px; font-size:10pt;">
															<option value="0">0시간</option>
															<option value="60">1시간</option>
															<option value="120">2시간</option>
															<option value="180">3시간</option>
															<option value="240">4시간</option>
															<option value="300">5시간</option>
															<option value="360">6시간</option>
															<option value="420">7시간</option>
															<option value="480">8시간</option>
															<option value="540">9시간</option>
															<option value="600">10시간</option>
															<option value="660">11시간</option>
															<option value="720">12시간</option>
															<option value="780">13시간</option>
															<option value="840">14시간</option>
															<option value="900">15시간</option>
															<option value="960">16시간</option>
															<option value="1020">17시간</option>
															<option value="1080">18시간</option>
															<option value="1140">19시간</option>
															<option value="1200">20시간</option>
															<option value="1260">21시간</option>
															<option value="1320">22시간</option>
															<option value="1380">23시간</option>
															<option value="1440">24시간</option>
														</select>
														<select id="delayMin1" name="delayMin1" style="width:60px; font-size:10pt;">
															<option value="0">0분</option>
															<option value="10">10분</option>
															<option value="20">20분</option>
															<option value="30">30분</option>
															<option value="40">40분</option>
															<option value="50">50분</option>
														</select>
													</td>
													<td align="center">
														<select id="reDelayHour1" name="reDelayHour1" style="width:60px; font-size:10pt;">
															<option value="0">0시간</option>
															<option value="60">1시간</option>
															<option value="120">2시간</option>
															<option value="180">3시간</option>
															<option value="240">4시간</option>
															<option value="300">5시간</option>
															<option value="360">6시간</option>
															<option value="420">7시간</option>
															<option value="480">8시간</option>
															<option value="540">9시간</option>
															<option value="600">10시간</option>
															<option value="660">11시간</option>
															<option value="720">12시간</option>
															<option value="780">13시간</option>
															<option value="840">14시간</option>
															<option value="900">15시간</option>
															<option value="960">16시간</option>
															<option value="1020">17시간</option>
															<option value="1080">18시간</option>
															<option value="1140">19시간</option>
															<option value="1200">20시간</option>
															<option value="1260">21시간</option>
															<option value="1320">22시간</option>
															<option value="1380">23시간</option>
															<option value="1440">24시간</option>
														</select>
														<select id="reDelayMin1" name="reDelayMin1" style="width:60px; font-size:10pt;">
															<option value="0">0분</option>
															<option value="10">10분</option>
															<option value="20">20분</option>
															<option value="30">30분</option>
															<option value="40">40분</option>
															<option value="50">50분</option>
														</select>
													</td>
												</tr>
											</table>
										</td>
										<td rowspan="8" width="5%" valign="middle">
											<table border="0" width="100%" height="500" cellpadding="0" cellspacing="0">
												<tr valign="middle">
													<td align="center">
														<c:if test="${userGrade eq 'B'}">
															<span style="color: #999; font-size:8pt;">읽기전용</span>
														</c:if>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									<tr>
										<th align="center" rowspan="2" style="font-size: 12pt;">저온알람</th>
									</tr>
									<tr>
										<td>
											<table border="1" style="border-color: #ffffff" width="100%">
												<tr>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">알람</span></strong></td>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">설정값</span></strong></td>
												</tr>
												<tr>
													<td align="center">
														<select id="alarmYn2" name="alarmYn2" style="width:65px; font-size:10pt;">
															<option value="Y">사용</option>
															<option value="N">미사용</option>
														</select>
													</td>
													<td align="center" style="font-size:10pt;">
														<input type="text" id="setVal2" name="setVal2" value="" style="width:60px; font-size:10pt;" />이하
													</td>
												</tr>
											</table>
											<table border="1" style="border-color: #ffffff" width="100%">
												<tr>
													<td height="30" width="50%" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">지연시간</span></strong></td>
													<td height="30" width="50%" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">재전송지연시간</span></strong></td>
												</tr>
												<tr>
													<td align="center">
														<select id="delayHour2" name="delayHour2" style="width:60px; font-size:10pt;">
															<option value="0">0시간</option>
															<option value="60">1시간</option>
															<option value="120">2시간</option>
															<option value="180">3시간</option>
															<option value="240">4시간</option>
															<option value="300">5시간</option>
															<option value="360">6시간</option>
															<option value="420">7시간</option>
															<option value="480">8시간</option>
															<option value="540">9시간</option>
															<option value="600">10시간</option>
															<option value="660">11시간</option>
															<option value="720">12시간</option>
															<option value="780">13시간</option>
															<option value="840">14시간</option>
															<option value="900">15시간</option>
															<option value="960">16시간</option>
															<option value="1020">17시간</option>
															<option value="1080">18시간</option>
															<option value="1140">19시간</option>
															<option value="1200">20시간</option>
															<option value="1260">21시간</option>
															<option value="1320">22시간</option>
															<option value="1380">23시간</option>
															<option value="1440">24시간</option>
														</select>
														<select id="delayMin2" name="delayMin2" style="width:60px; font-size:10pt;">
															<option value="0">0분</option>
															<option value="10">10분</option>
															<option value="20">20분</option>
															<option value="30">30분</option>
															<option value="40">40분</option>
															<option value="50">50분</option>
														</select>
													</td>
													<td align="center">
														<select id="reDelayHour2" name="reDelayHour2" style="width:60px; font-size:10pt;">
															<option value="0">0시간</option>
															<option value="60">1시간</option>
															<option value="120">2시간</option>
															<option value="180">3시간</option>
															<option value="240">4시간</option>
															<option value="300">5시간</option>
															<option value="360">6시간</option>
															<option value="420">7시간</option>
															<option value="480">8시간</option>
															<option value="540">9시간</option>
															<option value="600">10시간</option>
															<option value="660">11시간</option>
															<option value="720">12시간</option>
															<option value="780">13시간</option>
															<option value="840">14시간</option>
															<option value="900">15시간</option>
															<option value="960">16시간</option>
															<option value="1020">17시간</option>
															<option value="1080">18시간</option>
															<option value="1140">19시간</option>
															<option value="1200">20시간</option>
															<option value="1260">21시간</option>
															<option value="1320">22시간</option>
															<option value="1380">23시간</option>
															<option value="1440">24시간</option>
														</select>
														<select id="reDelayMin2" name="reDelayMin2" style="width:60px; font-size:10pt;">
															<option value="0">0분</option>
															<option value="10">10분</option>
															<option value="20">20분</option>
															<option value="30">30분</option>
															<option value="40">40분</option>
															<option value="50">50분</option>
														</select>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									<tr>
										<th align="center" rowspan="2" style="font-size: 12pt;">특정온도알람</th>
									</tr>
									<tr>
										<td>
											<table border="1" style="border-color: #ffffff" width="100%">
												<tr>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">알람</span></strong></td>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">설정값</span></strong></td>
												</tr>
												<tr>
													<td align="center">
														<select id="alarmYn3" name="alarmYn3" style="width:65px; font-size:10pt;">
															<option value="Y">사용</option>
															<option value="N">미사용</option>
														</select>
													</td>
													<td align="center" style="font-size:10pt;">
														<input type="text" id="setVal3" name="setVal3" value="" style="width:60px; font-size:10pt;" />
													</td>
												</tr>
											</table>
											<table border="1" style="border-color: #ffffff" width="100%">
												<tr>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">지연시간</span></strong></td>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">재전송지연시간</span></strong></td>
												</tr>
												<tr>
													<td align="center">
														<select id="delayHour3" name="delayHour3" style="width:60px; font-size:10pt;">
															<option value="0">0시간</option>
															<option value="60">1시간</option>
															<option value="120">2시간</option>
															<option value="180">3시간</option>
															<option value="240">4시간</option>
															<option value="300">5시간</option>
															<option value="360">6시간</option>
															<option value="420">7시간</option>
															<option value="480">8시간</option>
															<option value="540">9시간</option>
															<option value="600">10시간</option>
															<option value="660">11시간</option>
															<option value="720">12시간</option>
															<option value="780">13시간</option>
															<option value="840">14시간</option>
															<option value="900">15시간</option>
															<option value="960">16시간</option>
															<option value="1020">17시간</option>
															<option value="1080">18시간</option>
															<option value="1140">19시간</option>
															<option value="1200">20시간</option>
															<option value="1260">21시간</option>
															<option value="1320">22시간</option>
															<option value="1380">23시간</option>
															<option value="1440">24시간</option>
														</select>
														<select id="delayMin3" name="delayMin3" style="width:60px; font-size:10pt;">
															<option value="0">0분</option>
															<option value="10">10분</option>
															<option value="20">20분</option>
															<option value="30">30분</option>
															<option value="40">40분</option>
															<option value="50">50분</option>
														</select>
													</td>
													<td align="center">
														<select id="reDelayHour3" name="reDelayHour3" style="width:60px; font-size:10pt;">
															<option value="0">0시간</option>
															<option value="60">1시간</option>
															<option value="120">2시간</option>
															<option value="180">3시간</option>
															<option value="240">4시간</option>
															<option value="300">5시간</option>
															<option value="360">6시간</option>
															<option value="420">7시간</option>
															<option value="480">8시간</option>
															<option value="540">9시간</option>
															<option value="600">10시간</option>
															<option value="660">11시간</option>
															<option value="720">12시간</option>
															<option value="780">13시간</option>
															<option value="840">14시간</option>
															<option value="900">15시간</option>
															<option value="960">16시간</option>
															<option value="1020">17시간</option>
															<option value="1080">18시간</option>
															<option value="1140">19시간</option>
															<option value="1200">20시간</option>
															<option value="1260">21시간</option>
															<option value="1320">22시간</option>
															<option value="1380">23시간</option>
															<option value="1440">24시간</option>
														</select>
														<select id="reDelayMin3" name="reDelayMin3" style="width:60px; font-size:10pt;">
															<option value="0">0분</option>
															<option value="10">10분</option>
															<option value="20">20분</option>
															<option value="30">30분</option>
															<option value="40">40분</option>
															<option value="50">50분</option>
														</select>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									<tr>
										<th align="center" rowspan="2" style="font-size: 12pt;">DIN알람</th>
									</tr>
									<tr>
										<td>
											<table border="1" style="border-color: #ffffff" width="100%">
												<tr>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">알람</span></strong></td>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">상태</span></strong></td>
												</tr>
												<tr>
													<td align="center">
														<select id="alarmYn4" name="alarmYn4" style="width:65px; font-size:10pt;">
															<option value="Y">사용</option>
															<option value="N">미사용</option>
														</select>
													</td>
													<td align="center" style="font-size:10pt;">
														<span id="status4" style="color: #28a745; font-weight: bold;">정상</span>
													</td>
												</tr>
											</table>
											<table border="1" style="border-color: #ffffff" width="100%">
												<tr>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">지연시간</span></strong></td>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">재전송지연시간</span></strong></td>
												</tr>
												<tr>
													<td align="center">
														<select id="delayHour4" name="delayHour4" style="width:60px; font-size:10pt;">
															<option value="0">0시간</option>
															<option value="60">1시간</option>
															<option value="120">2시간</option>
															<option value="180">3시간</option>
															<option value="240">4시간</option>
															<option value="300">5시간</option>
															<option value="360">6시간</option>
															<option value="420">7시간</option>
															<option value="480">8시간</option>
															<option value="540">9시간</option>
															<option value="600">10시간</option>
															<option value="660">11시간</option>
															<option value="720">12시간</option>
															<option value="780">13시간</option>
															<option value="840">14시간</option>
															<option value="900">15시간</option>
															<option value="960">16시간</option>
															<option value="1020">17시간</option>
															<option value="1080">18시간</option>
															<option value="1140">19시간</option>
															<option value="1200">20시간</option>
															<option value="1260">21시간</option>
															<option value="1320">22시간</option>
															<option value="1380">23시간</option>
															<option value="1440">24시간</option>
														</select>
														<select id="delayMin4" name="delayMin4" style="width:60px; font-size:10pt;">
															<option value="0">0분</option>
															<option value="10">10분</option>
															<option value="20">20분</option>
															<option value="30">30분</option>
															<option value="40">40분</option>
															<option value="50">50분</option>
														</select>
													</td>
													<td align="center">
														<select id="reDelayHour4" name="reDelayHour4" style="width:60px; font-size:10pt;">
															<option value="0">0시간</option>
															<option value="60">1시간</option>
															<option value="120">2시간</option>
															<option value="180">3시간</option>
															<option value="240">4시간</option>
															<option value="300">5시간</option>
															<option value="360">6시간</option>
															<option value="420">7시간</option>
															<option value="480">8시간</option>
															<option value="540">9시간</option>
															<option value="600">10시간</option>
															<option value="660">11시간</option>
															<option value="720">12시간</option>
															<option value="780">13시간</option>
															<option value="840">14시간</option>
															<option value="900">15시간</option>
															<option value="960">16시간</option>
															<option value="1020">17시간</option>
															<option value="1080">18시간</option>
															<option value="1140">19시간</option>
															<option value="1200">20시간</option>
															<option value="1260">21시간</option>
															<option value="1320">22시간</option>
															<option value="1380">23시간</option>
															<option value="1440">24시간</option>
														</select>
														<select id="reDelayMin4" name="reDelayMin4" style="width:60px; font-size:10pt;">
															<option value="0">0분</option>
															<option value="10">10분</option>
															<option value="20">20분</option>
															<option value="30">30분</option>
															<option value="40">40분</option>
															<option value="50">50분</option>
														</select>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									<tr>
										<th align="center" rowspan="2" style="font-size: 12pt;">통신이상</th>
									</tr>
									<tr>
										<td>
											<table border="1" style="border-color: #ffffff" width="100%">
												<tr>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">알람</span></strong></td>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">상태</span></strong></td>
												</tr>
												<tr>
													<td align="center">
														<select id="alarmYn5" name="alarmYn5" style="width:65px; font-size:10pt;">
															<option value="Y">사용</option>
															<option value="N">미사용</option>
														</select>
													</td>
													<td height="30" align="center" style="font-size:10pt;">
														<span id="status5" style="color: #28a745; font-weight: bold;">정상</span>
													</td>
												</tr>
											</table>
											<table border="1" style="border-color: #ffffff" width="100%">
												<tr>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">지연시간</span></strong></td>
													<td width="50%" height="30" style="background-color: #c7254e; font-size:10pt;" align="center"><strong><span style="color: #f0f8ff; font-size:10pt;">재전송지연시간</span></strong></td>
												</tr>
												<tr>
													<td align="center">
														<select id="delayHour5" name="delayHour5" style="width:60px; font-size:10pt;">
															<option value="0">0시간</option>
															<option value="60">1시간</option>
															<option value="120">2시간</option>
															<option value="180">3시간</option>
															<option value="240">4시간</option>
															<option value="300">5시간</option>
															<option value="360">6시간</option>
															<option value="420">7시간</option>
															<option value="480">8시간</option>
															<option value="540">9시간</option>
															<option value="600">10시간</option>
															<option value="660">11시간</option>
															<option value="720">12시간</option>
															<option value="780">13시간</option>
															<option value="840">14시간</option>
															<option value="900">15시간</option>
															<option value="960">16시간</option>
															<option value="1020">17시간</option>
															<option value="1080">18시간</option>
															<option value="1140">19시간</option>
															<option value="1200">20시간</option>
															<option value="1260">21시간</option>
															<option value="1320">22시간</option>
															<option value="1380">23시간</option>
															<option value="1440">24시간</option>
														</select>
														<select id="delayMin5" name="delayMin5" style="width:60px; font-size:10pt;">
															<option value="0">0분</option>
															<option value="10">10분</option>
															<option value="20">20분</option>
															<option value="30">30분</option>
															<option value="40">40분</option>
															<option value="50">50분</option>
														</select>
													</td>
													<td align="center">
														<select id="reDelayHour5" name="reDelayHour5" style="width:60px; font-size:10pt;">
															<option value="0">0시간</option>
															<option value="60">1시간</option>
															<option value="120">2시간</option>
															<option value="180">3시간</option>
															<option value="240">4시간</option>
															<option value="300">5시간</option>
															<option value="360">6시간</option>
															<option value="420">7시간</option>
															<option value="480">8시간</option>
															<option value="540">9시간</option>
															<option value="600">10시간</option>
															<option value="660">11시간</option>
															<option value="720">12시간</option>
															<option value="780">13시간</option>
															<option value="840">14시간</option>
															<option value="900">15시간</option>
															<option value="960">16시간</option>
															<option value="1020">17시간</option>
															<option value="1080">18시간</option>
															<option value="1140">19시간</option>
															<option value="1200">20시간</option>
															<option value="1260">21시간</option>
															<option value="1320">22시간</option>
															<option value="1380">23시간</option>
															<option value="1440">24시간</option>
														</select>
														<select id="reDelayMin5" name="reDelayMin5" style="width:60px; font-size:10pt;">
															<option value="0">0분</option>
															<option value="10">10분</option>
															<option value="20">20분</option>
															<option value="30">30분</option>
															<option value="40">40분</option>
															<option value="50">50분</option>
														</select>
													</td>
												</tr>
											</table>
										</td>
									</tr>
								</table>
                                <div style="text-align: center; margin-top: 15px;">
                                    <c:if test="${userGrade ne 'B'}">
                                        <button id="saveAlarm" name="saveAlarm" style="width:120px; height:30px;">알람설정저장</button>
                                    </c:if>
                                    <c:if test="${userGrade eq 'B'}">
                                        <span style="color: #999; font-size:10pt;">읽기 전용 (알람 설정 변경 불가)</span>
                                    </c:if>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="confirmModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
                    <h4 class="modal-title" id="myModalLabel">로그아웃 하시겠습니까?</h4>
                </div>
                <div class="modal-footer">
                    <a href="/login/logout?userId=${loginUserId}" class="btn btn-primary">Yes</a>
                    <button type="button" class="btn btn-default" data-dismiss="modal">No</button>
                </div>
            </div>
        </div>
    </div>
    <footer class="templatemo-footer">
        <div class="templatemo-copyright">
            <p>Copyright &copy; 2022 H&T Solutions</p>
        </div>
    </footer>
</div>

<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
<script src="/js/bootstrap.min.js"></script>
<script src="/js/templatemo_script.js"></script>

<!-- MQTT 라이브러리 파일들이 존재하지 않으므로 제거됨 -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/paho-mqtt/1.0.1/mqttws31.js" type="text/javascript"></script>

<script src="https://cdn.jsdelivr.net/npm/moment@2.29.1/min/moment.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@2.9.4"></script>
<script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-streaming@1.9.0"></script>
<script>
    // 화면 갱신 함수들
    function refreshDeviceSettings() {
        console.log("장치설정 버튼 클릭 - 화면 갱신");
        location.reload();
    }
    
    	function refreshAlarmSettings() {
		console.log("알람설정 버튼 클릭 - 화면 갱신");
		location.reload();
	}
	
	// 장치 응답 대기 및 비교 함수
	function waitForDeviceResponse(changedValues) {
		var responseTimeout = 20000; // 20초 타임아웃 (장치 응답 시간 여유 확보)
		var checkInterval = 500; // 0.5초마다 체크 (더 빠른 응답 감지)
		var elapsedTime = 0;
		
		console.log("장치 응답 대기 시작 (최대 20초):", changedValues);
		
		var checkResponse = setInterval(function() {
			elapsedTime += checkInterval;
			
			// 진행 상황 로그 (5초마다)
			if(elapsedTime % 5000 === 0) {
				console.log("장치 응답 대기 중... (" + (elapsedTime/1000) + "초 경과)");
			}
			
			// 타임아웃 체크
			if(elapsedTime >= responseTimeout) {
				clearInterval(checkResponse);
				console.error("장치 응답 시간 초과 (20초)");
				alert("장치 응답 시간 초과\n장치가 응답하지 않습니다.\n\n설정이 저장되었는지 확인하려면\n페이지를 새로고침하세요.");
				return;
			}
			
			// MQTT 메시지에서 장치 응답 확인
			// rcvMsg 함수에서 처리된 응답을 확인
			if(window.deviceResponseReceived) {
				clearInterval(checkResponse);
				console.log("장치 응답 수신 완료 (" + (elapsedTime/1000) + "초 경과)");
				compareDeviceResponse(changedValues);
			}
		}, checkInterval);
	}
	
	// 장치 응답과 저장값 비교 함수
	function compareDeviceResponse(changedValues) {
		console.log("장치 응답과 변경된 값 비교:", changedValues);
		
		// 장치에서 받은 응답값들 (rcvMsg에서 설정됨)
		var deviceResponse = window.deviceResponseData || {};
		
		var isSuccess = true;
		var mismatchDetails = [];
		
		// 설정값 이름 매핑
		var settingNames = {
			'p01': '설정온도',
			'p02': '히스테리시스 편차', 
			'p03': 'COMP 출력 지연시간',
			'p04': '온도보정',
			'p05': '제상 정지시간',
			'p06': '제상 시간',
			'p07': '팬설정',
			'p08': '제상 후 FAN ON 지연시간',
			'p09': 'FAN OFF 지연시간',
			'p10': '저온방지 온도편차',
			'p11': 'COMP 누적 시간 제상 선택',
			'p12': '온도 센서 타입',
			'p13': '수동조작 on/off',
			'p14': '통신 국번',
			'p15': '통신 속도',
			'p16': '장치종류'
		};
		
		// 변경된 값들만 비교
		for(var key in changedValues) {
			var change = changedValues[key];
			if(deviceResponse[key]) {
				// 소수점 디코딩을 고려한 값 비교
				var savedValue = String(change.new);
				var deviceValue = String(deviceResponse[key]);
				
				// p01, p02, p04, p10의 경우 장치 응답값을 정규화
				if(key === 'p02' || key === 'p04' || key === 'p10') {
					var deviceValueNum = parseInt(deviceResponse[key]) / 10;
					deviceValue = deviceValueNum.toString() + ".0";
				} else if(key === 'p01') {
					// p01은 항상 10으로 나누어 처리 (1000 → 100.0, 999 → 99.9)
					var deviceValueNum = parseInt(deviceResponse[key]);
					var normalizedValue = deviceValueNum / 10;
					if(normalizedValue === Math.floor(normalizedValue)) {
						deviceValue = normalizedValue.toString() + ".0";
					} else {
						deviceValue = normalizedValue.toString();
					}
				}
				
				// 숫자 비교를 위해 parseFloat 사용
				var savedValueNum = parseFloat(savedValue) || 0;
				var deviceValueNum = parseFloat(deviceValue) || 0;
				
				console.log("값 비교:", key, "저장값:", savedValue, "(" + savedValueNum + ")", "장치값:", deviceValue, "(" + deviceValueNum + ")");
				
				// 부동소수점 비교를 위해 작은 오차 허용
				if(Math.abs(savedValueNum - deviceValueNum) > 0.001) {
					isSuccess = false;
					var settingName = settingNames[key] || key;
					mismatchDetails.push(settingName + ": 저장값(" + savedValue + ") != 장치값(" + deviceValue + ")");
				}
			}
		}
		
		if(isSuccess) {
			alert("장치설정 저장 성공!\n변경된 설정값이 장치에 정상적으로 적용되었습니다.");
			
			// 성공 후 화면 갱신된 값을 다시 저장하여 다음 변경 시 올바른 비교가 가능하도록 함
			updateOriginalValues();
		} else {
			var errorMessage = "장치설정 저장 실패!\n\n불일치 항목:\n";
			for(var i = 0; i < mismatchDetails.length; i++) {
				errorMessage += mismatchDetails[i] + "\n";
			}
			alert(errorMessage);
			
			// 실패 시에도 장치에서 받은 실제 값으로 originalValues 업데이트
			updateOriginalValuesFromDeviceResponse(deviceResponse);
		}
		
			// 응답 상태 초기화
	window.deviceResponseReceived = false;
	window.deviceResponseData = null;
}

// 화면 갱신된 값을 다시 저장하는 함수
function updateOriginalValues() {
	console.log("화면 갱신된 값을 다시 저장합니다.");
	
	// 현재 화면에 표시된 값들을 다시 저장
	var p01_display = $('#setTemp').text().replace('°C', '');
	if(!p01_display || p01_display.trim() === '') {
		p01_display = $('#p01').val();
	}
	
	// 각 설정값의 현재 화면 값을 다시 저장
	window.originalValues = {
		p01: p01_display, // 이미 정규화된 값이므로 그대로 사용
		p02: $('#p02').val(), // 이미 정규화된 값이므로 그대로 사용
		p03: $('#p03').val(),
		p04: $('#p04').val(), // 이미 정규화된 값이므로 그대로 사용
		p05: $('#p05').val(),
		p06: $('#p06').val(),
		p07: $('#p07 option:selected').val(),
		p08: $('#p08').val(), // 시간 관련 값이므로 그대로 사용
		p09: $('#p09').val(),
		p10: $('#p10').val(), // 이미 정규화된 값이므로 그대로 사용
		p11: $('#p11 option:selected').val(),
		p12: $('#p12 option:selected').val(),
		p13: $('#p13 option:selected').val(),
		p14: $('#p14 option:selected').val(),
		p15: $('#p15 option:selected').val(),
		p16: $('#p16 option:selected').val()
	};
	
	console.log("업데이트된 originalValues:", window.originalValues);
}

// 장치 응답에서 받은 값으로 originalValues 업데이트하는 함수
function updateOriginalValuesFromDeviceResponse(deviceResponse) {
	console.log("장치 응답에서 받은 값으로 originalValues 업데이트합니다.");
	
	// 장치에서 받은 값들을 정규화하여 저장
	window.originalValues = {
		p01: deviceResponse.p01 ? ((parseInt(deviceResponse.p01) / 10).toString() + ".0") : "",
		p02: deviceResponse.p02 ? ((parseInt(deviceResponse.p02) / 10).toString() + ".0") : "",
		p03: deviceResponse.p03 || "",
		p04: deviceResponse.p04 ? ((parseInt(deviceResponse.p04) / 10).toString() + ".0") : "",
		p05: deviceResponse.p05 || "",
		p06: deviceResponse.p06 || "",
		p07: deviceResponse.p07 || "",
		p08: deviceResponse.p08 ? deviceResponse.p08.toString() : "",
		p09: deviceResponse.p09 || "",
		p10: deviceResponse.p10 ? (parseInt(deviceResponse.p10) === 0 ? "0.0" : ((parseInt(deviceResponse.p10) / 10).toString() + ".0")) : "",
		p11: deviceResponse.p11 || "",
		p12: deviceResponse.p12 || "",
		p13: deviceResponse.p13 || "",
		p14: deviceResponse.p14 || "",
		p15: deviceResponse.p15 || "",
		p16: deviceResponse.p16 || ""
	};
	
	console.log("장치 응답으로 업데이트된 originalValues:", window.originalValues);
}
	
	// Chrome extension 에러 핸들링
    window.addEventListener('error', function(e) {
        // Chrome extension 에러는 무시
        if (e.message && (e.message.includes('runtime.lastError') || 
            e.message.includes('message port closed'))) {
            e.preventDefault();
            return false;
        }
    });
    
    // Promise 에러 핸들링
    window.addEventListener('unhandledrejection', function(e) {
        // Chrome extension 관련 Promise 에러는 무시
        if (e.reason && (e.reason.message && e.reason.message.includes('message port closed'))) {
            e.preventDefault();
            return false;
        }
    });
    
	$(window).on({
		load: function() {
			console.log('센서설정 페이지 로딩 시작');
			
			// 페이지 로딩 완료 대기 (DOM 요소들이 완전히 렌더링될 때까지)
			setTimeout(function() {
				console.log('센서설정 페이지 초기화 시작');
				
				// CSS 로딩 완료 확인
				var cssLoaded = false;
				var cssCheckCount = 0;
				var maxCssChecks = 10;
				
				function checkCSSLoaded() {
					cssCheckCount++;
					var currentSensorUuid = $('#sensorUuid').val();
					var statusElement = document.getElementById('status' + currentSensorUuid);
					var templateWrapper = document.querySelector('.template-page-wrapper');
					
					console.log('CSS 로딩 확인 시도:', cssCheckCount, 'statusElement:', statusElement ? '존재' : '없음', 'templateWrapper:', templateWrapper ? '존재' : '없음');
					
					if (statusElement && statusElement.offsetHeight > 0 && templateWrapper && templateWrapper.offsetHeight > 0) {
						cssLoaded = true;
						console.log('CSS 로딩 완료 확인됨 - 상태표시 아이콘과 메인 콘텐츠 모두 렌더링됨');
						initializeSensorSetting();
					} else if (cssCheckCount < maxCssChecks) {
						setTimeout(checkCSSLoaded, 500); // 0.5초 간격으로 확인
					} else {
						console.warn('CSS 로딩 확인 시간 초과, 강제 초기화 진행');
						initializeSensorSetting();
					}
				}
				
				function initializeSensorSetting() {
					// B 등급 사용자의 경우 모든 입력 필드를 읽기 전용으로 설정
					if('${userGrade}' === 'B') {
						$('input[type="text"], select').prop('readonly', true).prop('disabled', true).css('background-color', '#f5f5f5');
					}
					
					// 센서설정 페이지 에러 체크 변수 초기화 (메인 페이지와 동일한 방식)
					var sensorUuid = $('#sensorUuid').val();
					if (sensorUuid) {
						window['deviceLastDataTime_' + sensorUuid] = Date.now();
						window['deviceErrorCounters_' + sensorUuid] = 0;
						window['deviceErrorStates_' + sensorUuid] = false;
						window['deviceStatusStates_' + sensorUuid] = 'gray';
						window['deviceErrorDisplayStates_' + sensorUuid] = 'gray';
						window.deviceDinErrorStates = false;
						
						console.log('센서설정 페이지 에러 체크 변수 초기화 완료:', sensorUuid);
						
						// 에러 체크 타이머 시작 (3초마다 실행)
						window.__errorTimer = startInterval(3, chkError);
					}
					
					// UnifiedMQTTManager를 통한 MQTT 초기화 (메인 페이지와 동일한 방식)
					if (typeof UnifiedMQTTManager !== 'undefined') {
						UnifiedMQTTManager.executeInitialSync();
					} else {
						console.warn('UnifiedMQTTManager가 로드되지 않았습니다.');
					}
					
					// 센서 정보 요청 (페이지 로딩 시)
			setTimeout(function() {
						console.log('페이지 로딩 완료 - 센서 정보 요청');
        setSensor();
					}, 3000); // MQTT 연결 후 3초 뒤에 센서 정보 요청
					
					console.log('센서설정 페이지 초기화 완료');
				}
				
				// CSS 로딩 확인 시작
				checkCSSLoaded();
			}, 2000); // 2초 대기 (페이지 로딩 시간 증가)
			
					// 장치 이름 설정 (main 페이지와 동일한 형식)
		var sensorUuid = $('#sensorUuid').val();
		var deviceName = '';

		// sensorUuid를 기반으로 장치 이름 생성
		if(sensorUuid) {
			if(sensorUuid.includes('50EE')) {
				deviceName = '사무실50EE';
			} else if(sensorUuid.includes('53A4')) {
				deviceName = '냉동53A4';
			} else if(sensorUuid.includes('5575')) {
				deviceName = '사무실5575';
        } else {
				deviceName = sensorUuid; // 기본값
			}

		// 장치 이름 표시 (기존 로직 유지)
		$('#deviceName').text(deviceName);
		}
		
		// p16 초기 설정 (페이지 로딩 시)
		console.log("페이지 로딩 시 p16 초기값:", $('#p16').val());
		if($('#p16').val() == "" || $('#p16').val() == null) {
			$('#p16').val("0").prop("selected", true);
			console.log("페이지 로딩 시 p16 Cooler 설정");
		}

    setTimeout(function() {
				startInterval(60, chkError);
			}, 30000);

			// getData AJAX 호출 제거 - MQTT를 통한 실시간 데이터 수신으로 대체

    		// 페이지 로딩 완료 후 type 1 요청 (0.5초 후)
    		setTimeout(function() {
    			var sendData= {
    			    userId: $('#userId').val(),
    			    topicStr: $('#topicStr').val(),
    			    setGu: "readparam",
    			    type: "1"  // type=1: parameter 요청
    			}

    			var p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12, p13, p14, p15, p16;
    			var sensorUuid = $('#sensorUuid').val();

    			$.ajax({
        			url: '/admin/setSensor',
                    async: true,
                    type: 'POST',
                    data: JSON.stringify(sendData),
                    dataType: 'json',
                    contentType: 'application/json',
                    success: function(result) {
                    	console.log("=== 페이지 로드 시 readparam AJAX 응답 ===");
                    	console.log("result.resultCode:", result.resultCode);
                    	console.log("result:", result);
                    	if(result.resultCode == "200") {
                    		console.log("readparam 성공 - result.resultMsg:", result.resultMsg);
                    		console.log("sensorUuid:", sensorUuid);
                    		console.log("result.rcvTopic:", result.rcvTopic);
							var jsonObj = JSON.parse(result.resultMsg);
							var rcvTopic = result.rcvTopic;
							var rcvTopicArr = Array();
							if(rcvTopic) {
								rcvTopicArr = rcvTopic.split("/");

								if (rcvTopicArr[3] == sensorUuid) {

									p01 = jsonObj.p01;
									if(p01 != "" && p01 != null && p01 != undefined) {
										if(p01.length == 1) {
											p01 = "0." + p01;
										} else if(p01.length == 2) {
											p01 = p01.substr(0, 1) + "." + p01.substr(1, 2);
										} else if(p01.length == 3) {
											p01 = p01.substr(0, 2) + "." + p01.substr(2, 3);
										} else if(p01.length == 4) {
											p01 = p01.substr(0, 3) + "." + p01.substr(3, 4);
										}
									}

									p02 = jsonObj.p02;
									if (p02 != "" && p02 != null && p02 != undefined) {
										if(p02.length == 1) {
											p02 = "0." + p02;
										} else if(p02.length == 2) {
											p02 = p02.substr(0, 1) + "." + p02.substr(1, 2);
										} else if(p02.length == 3) {
											p02 = p02.substr(0, 2) + "." + p02.substr(2, 3);
										} else if(p02.length == 4) {
											p02 = p02.substr(0, 3) + "." + p02.substr(3, 4);
										}
									}

									p03 = jsonObj.p03;
									p04 = jsonObj.p04;
									if (p04 != "" && p04 != null && p04 != undefined) {
										if (p04.length == 4) {
											p04 = p04.substr(0, 3) + "." + p04.substr(3, 4);
										} else if (p04.length == 3) {
											p04 = p04.substr(0, 2) + "." + p04.substr(2, 3);
										} else if (p04.length == 2) {
											if (p04.indexOf("-") >= 0) {
												p04 = p04.substr(0, 1) + "0." + p04.substr(1, 2);
											} else {
												p04 = p04.substr(0, 1) + "." + p04.substr(1, 2);
      	    }
      	  } else {
											p04 = "0." + p04;
										}
									}

								p05 = jsonObj.p05;
								p06 = jsonObj.p06;
								p07 = jsonObj.p07;
								p08 = jsonObj.p08;
								p09 = jsonObj.p09;
								p10 = jsonObj.p10;
								if (p10 != "" && p10 != null && p10 != undefined) {
									if (p10.length == 2) {
										p10 = p10.substr(0, 1) + "." + p10.substr(1, 2);
      	    } else {
										p10 = p10 + ".0";
									}
								}

								p11 = jsonObj.p11;
								p12 = jsonObj.p12;
								p13 = jsonObj.p13;
								p14 = jsonObj.p14;
								p15 = jsonObj.p15;
								p16 = jsonObj.p16 || "0"; // 기본값 설정
								console.log("p16 원본값:", p16, "타입:", typeof p16);
								
								// p16 값이 없으면 기본값 0 (Cooler) 설정
								if(p16 == null || p16 == undefined || p16 == "") {
									p16 = "0";
									console.log("p16 기본값 설정:", p16);
								}

								$('#setTemp').html(p01);
								$('#p01').val(p01 + "°C");
								$('#p02').val(p02);
								$('#p03').val(p03);
								$('#p04').val(p04);
								$('#p05').val(p05);
								$('#p06').val(p06);
								$('#p07').val(p07).prop("selected", true);
								$('#p08').val(p08);
								$('#p09').val(p09);
								$('#p10').val(p10);
								$('#p11').val(p11).prop("selected", true);
								$('#p12').val(p12).prop("selected", true);
								$('#p13').val(p13).prop("selected", true);
								$('#p14').val(p14).prop("selected", true);
								$('#p15').val(p15).prop("selected", true);
								console.log("p16 설정 전:", $('#p16').val(), "선택된 옵션:", $('#p16 option:selected').text());
								$('#p16').val(p16).prop("selected", true);
								console.log("p16 설정 후:", $('#p16').val(), "선택된 옵션:", $('#p16 option:selected').text());
								
								// p16이 설정되지 않았으면 강제로 Cooler 설정
								if($('#p16 option:selected').val() == "" || $('#p16 option:selected').val() == null) {
									$('#p16').val("0").prop("selected", true);
									console.log("p16 강제 Cooler 설정 완료");
								}
								
								// 원래 값들을 data-original 속성에 저장 (변경 감지용)
								$('#p01').attr('data-original', p01);
								$('#p02').attr('data-original', p02);
								$('#p03').attr('data-original', p03);
								$('#p04').attr('data-original', p04);
								$('#p05').attr('data-original', p05);
								$('#p06').attr('data-original', p06);
								$('#p07').attr('data-original', p07);
								$('#p08').attr('data-original', p08);
								$('#p09').attr('data-original', p09);
								$('#p10').attr('data-original', p10);
								$('#p11').attr('data-original', p11);
								$('#p12').attr('data-original', p12);
								$('#p13').attr('data-original', p13);
								$('#p14').attr('data-original', p14);
								$('#p15').attr('data-original', p15);
								$('#p16').attr('data-original', p16);
								
								// readparam AJAX에서는 화면 업데이트만 수행
								// originalValues는 setres MQTT 메시지에서 설정됨
								console.log("=== readparam AJAX 성공 - 화면 업데이트 완료 ===");

							}
						}
                	}
                },
                error: function(result) {

                },
                complete: function(result) {

                }
    		});
    	}, 500);  // 페이지 로딩 완료 후 0.5초 후 type 1 요청

    	// 페이지 로딩 완료 후 type 2 요청 (2.5초 후)
    	setTimeout(function() {
    		var sendData2 = {
    			userId: $('#userId').val(),
    			topicStr: $('#topicStr').val(),
    			setGu: "readstatus",
    			type: "2"  // type=2: status 요청
    		}

    		$.ajax({
    			url: '/admin/setSensor',
    			async: true,
    			type: 'POST',
    			data: JSON.stringify(sendData2),
    			dataType: 'json',
    			contentType: 'application/json',
    			success: function (result) {
    				if (result.resultCode == "200") {
    					var jsonObj = JSON.parse(result.resultMsg);
    					var rcvTopic = result.rcvTopic;
    					var rcvTopicArr = Array();

    					if(rcvTopic) {
    						rcvTopicArr = rcvTopic.split("/");

    						if(rcvTopicArr[3] == $('#sensorUuid').val()) {
    							// 상태 정보 처리
    						}
    					}
    				}
    			},
    			error: function (result) {

    			}
    		});
    	}, 2500);  // 페이지 로딩 완료 후 2.5초 후 type 2 요청


    		var alarmData = $('#alarmMap').val();
    		console.log(alarmData);

    		if(alarmData) {
				var alarmMap = JSON.parse(alarmData);

				if (alarmMap.sensor_uuid == $('#sensorUuid').val()) {
					var alarmYn1, alarmYn2, alarmYn3, alarmYn4, alarmYn5;
					var setVal1, setVal2, setVal3, setVal4;
					var delay_hour1, delay_hour2, delay_hour4, delay_hour5;
					var delay_min1, delay_min2, delay_min4, delay_min5;
					var re_delay_hour1, re_delay_hour2, re_delay_hour4, re_delay_hour5;
					var re_delay_min1, re_delay_min2, re_delay_min4, re_delay_min5;

					alarmYn1 = alarmMap.alarm_yn1 || "N";
					alarmYn2 = alarmMap.alarm_yn2 || "N";
					alarmYn3 = alarmMap.alarm_yn3 || "N";
					alarmYn4 = alarmMap.alarm_yn4 || "N";
					alarmYn5 = alarmMap.alarm_yn5 || "N";

					$('#alarmYn1').val(alarmYn1).prop("selected", true);
					$('#alarmYn2').val(alarmYn2).prop("selected", true);
					$('#alarmYn3').val(alarmYn3).prop("selected", true);
					$('#alarmYn4').val(alarmYn4).prop("selected", true);
					$('#alarmYn5').val(alarmYn5).prop("selected", true);

				setVal1 = alarmMap.set_val1 || "";
				setVal2 = alarmMap.set_val2 || "";
				setVal3 = alarmMap.set_val3 || "";
				setVal4 = alarmMap.set_val4 || "";

				// 온도 값에 소숫점 포맷팅 적용 (페이지 초기 로딩 시)
				$('#setVal1').val(formatTemperatureValue(setVal1));
				$('#setVal2').val(formatTemperatureValue(setVal2));
				$('#setVal3').val(formatTemperatureValue(setVal3));
				$('#setVal4').val(setVal4); // setVal4는 온도가 아니므로 포맷팅 제외
				
				console.log('초기 알람 설정값 로드 (소숫점 포맷팅 적용):', {
					setVal1: formatTemperatureValue(setVal1),
					setVal2: formatTemperatureValue(setVal2),
					setVal3: formatTemperatureValue(setVal3)
				});

					delay_hour1 = alarmMap.delay_hour1 || "0";
					delay_hour2 = alarmMap.delay_hour2 || "0";
					delay_hour4 = alarmMap.delay_hour4 || "0";
					delay_hour5 = alarmMap.delay_hour5 || "0";

					$('#delayHour1').val(delay_hour1).prop("selected", true);
					$('#delayHour2').val(delay_hour2).prop("selected", true);
					$('#delayHour4').val(delay_hour4).prop("selected", true);
					$('#delayHour5').val(delay_hour5).prop("selected", true);

					delay_min1 = alarmMap.delay_min1 || "0";
					delay_min2 = alarmMap.delay_min2 || "0";
					delay_min4 = alarmMap.delay_min4 || "0";
					delay_min5 = alarmMap.delay_min5 || "0";

					$('#delayMin1').val(delay_min1).prop("selected", true);
					$('#delayMin2').val(delay_min2).prop("selected", true);
					$('#delayMin4').val(delay_min4).prop("selected", true);
					$('#delayMin5').val(delay_min5).prop("selected", true);

					re_delay_hour1 = alarmMap.re_delay_hour1 || "0";
					re_delay_hour2 = alarmMap.re_delay_hour2 || "0";
					re_delay_hour4 = alarmMap.re_delay_hour4 || "0";
					re_delay_hour5 = alarmMap.re_delay_hour5 || "0";

					$('#reDelayHour1').val(re_delay_hour1).prop("selected", true);
					$('#reDelayHour2').val(re_delay_hour2).prop("selected", true);
					$('#reDelayHour4').val(re_delay_hour4).prop("selected", true);
					$('#reDelayHour5').val(re_delay_hour5).prop("selected", true);

					re_delay_min1 = alarmMap.re_delay_min1 || "0";
					re_delay_min2 = alarmMap.re_delay_min2 || "0";
					re_delay_min4 = alarmMap.re_delay_min4 || "0";
					re_delay_min5 = alarmMap.re_delay_min5 || "0";

					$('#reDelayMin1').val(re_delay_min1).prop("selected", true);
					$('#reDelayMin2').val(re_delay_min2).prop("selected", true);
					$('#reDelayMin4').val(re_delay_min4).prop("selected", true);
					$('#reDelayMin5').val(re_delay_min5).prop("selected", true);
				}
					} else {
				$('#alarmYn1').val("N").prop("selected", true);
				$('#alarmYn2').val("N").prop("selected", true);
				$('#alarmYn3').val("N").prop("selected", true);
				$('#alarmYn4').val("N").prop("selected", true);
				$('#alarmYn5').val("N").prop("selected", true);
			}
		}
    });

	// MQTT를 통한 실시간 데이터 수신 함수
	function updateCurrentTemperature(sensorUuid, value) {
		if (sensorUuid === $('#sensorUuid').val()) {
			if (value === 'Error') {
				$('#curTemp').html('Error');
    	} else {
				$('#curTemp').html(value + '°C');
			}
    }
}

	// MQTT 메시지 처리 함수
function rcvMsg(topic, message) {
    try {
        var jsonObj = JSON.parse(message);
        var topicArr = topic.split('/');
        
        if (topicArr.length >= 5 && topicArr[4] === 'DEV') {
            var userId = topicArr[1];
            var sensorUuid = topicArr[3];
            
            // 현재 센서와 일치하는지 확인
            if (sensorUuid === $('#sensorUuid').val()) {
                if (jsonObj.actcode === 'live') {
                    if (jsonObj.name === 'ain') {
                        updateCurrentTemperature(sensorUuid, jsonObj.value);
						} else if (jsonObj.name === 'output') {
							// 출력 상태 변화 알림 → 버튼 하이라이트 반영
							console.log('출력 상태 변화 수신:', jsonObj);
							updateOutputButtonState(jsonObj);
                    }
                } else if (jsonObj.actcode === 'setres') {
						// 설정 응답 수신 시 센서 정보 업데이트
                    updateSensorInfo(jsonObj);
                }
            }
        }
    } catch (e) {
        console.error('MQTT 메시지 파싱 에러:', e);
    }
}

	// handleSetresMessage 함수는 sensor-setting.js에서 정의됨
	
	// 제상/히터 라벨 업데이트 함수 (메인 페이지와 동일)
	function updateDefrostLabel(elementId, deviceType) {
		var element = document.getElementById(elementId);
		if (element) {
			if (deviceType === '1') {
				element.textContent = '히터';
			} else {
				element.textContent = '제상';
			}
		}
	}
	
	// 제상/히터 아이콘 업데이트 함수 (메인 페이지와 동일)
	function updateDefrostIndicator(elementId, status, deviceType) {
		var element = document.getElementById(elementId);
		if (element) {
			var defrostIcon = element.querySelector('.defrost-icon');
			var heaterIcon = element.querySelector('.heater-icon');
			if (deviceType === '1') {
				if (defrostIcon) defrostIcon.style.display = 'none';
				if (heaterIcon) heaterIcon.style.display = 'inline';
			} else {
				if (defrostIcon) defrostIcon.style.display = 'inline';
				if (heaterIcon) heaterIcon.style.display = 'none';
			}
			element.className = 'status-indicator ' + status;
		}
	}
	
	// 전역 함수로 등록
	window.updateDefrostLabel = updateDefrostLabel;
	window.updateDefrostIndicator = updateDefrostIndicator;

	// 센서 정보 업데이트 함수
function updateSensorInfo(msg) {
    var sensorType = msg.p12; // 온도 센서 타입
    var deviceType = msg.p16; // 장치종류
    
    // 전역 변수 업데이트
    currentSensorType = sensorType;
    
    var sensorInfo = "";
    var tempRange = "";
    var deviceTypeText = "";
    
    // 센서 타입에 따른 정보 설정
    if(sensorType == "2") { // T3
        sensorInfo = "PT100(T3)";
        tempRange = "-200°C ~ 850°C";
    } else if(sensorType == "0") { // T1
        sensorInfo = "NTC 10K(T1)";
        tempRange = "-50°C ~ 125°C";
    } else if(sensorType == "1") { // T2
        sensorInfo = "NTC 5K(T2)";
        tempRange = "-50°C ~ 125°C";
                } else {
        sensorInfo = "NTC 10K(T1)"; // Default
        tempRange = "-50°C ~ 125°C"; // Default
    }
    
    // 장치종류 설정 (센서 정보 섹션용 - 상세 정보)
    var deviceTypeTextDetail = "";
    if(deviceType == "0") {
        deviceTypeTextDetail = "Cooler (쿨러)";
    } else if(deviceType == "1") {
        deviceTypeTextDetail = "Heater (히터)";
    } else {
        deviceTypeTextDetail = "Cooler (쿨러)";
    }
    
    // 장치종류 설정 (패널 헤더용 - 간단한 정보)
    var deviceTypeText = "";
    if(deviceType == "0") {
        deviceTypeText = "쿨러";
    } else if(deviceType == "1") {
        deviceTypeText = "히터";
    } else {
        deviceTypeText = "쿨러";
    }
		
		// 화면 업데이트 (알람설정섹션)
		$('#alarmSensorInfoDisplay').text(sensorInfo);
		$('#alarmTempRangeDisplay').text(tempRange);
		$('#alarmDeviceTypeDisplay').text(deviceTypeTextDetail);
		
		// 화면 업데이트 (장치설정섹션)
		$('#deviceSensorInfoDisplay').text(sensorInfo);
		$('#deviceTempRangeDisplay').text(tempRange);
		$('#deviceDeviceTypeDisplay').text(deviceTypeTextDetail);
		
		// 장치종류 표시 업데이트 (패널 헤더)
		var currentSensorUuid = $('#sensorUuid').val();
		if(currentSensorUuid) {
			// 장치설정 섹션 헤더 업데이트
			$('#deviceType' + currentSensorUuid).text('(' + deviceTypeText + ')');
			// 알람설정 섹션 헤더 업데이트
			$('#deviceType2' + currentSensorUuid).text('(' + deviceTypeText + ')');
		}
		
		// 장치이름 업데이트 (센서 이름이 변경된 경우)
		var sensorName = $('#sensor_name').val() || '센서';
		if(currentSensorUuid) {
			$('#deviceName' + currentSensorUuid).text(sensorName);
			$('#deviceName2' + currentSensorUuid).text(sensorName);
		}
		
		// 제상/히터 라벨 및 아이콘 업데이트 (메인 페이지와 동일한 방식)
		if(typeof updateDefrostLabel === 'function') {
			updateDefrostLabel('defrostLabel' + currentSensorUuid, deviceType);
		}
		
		// 상태표시등 아이콘 업데이트 (제상/히터 아이콘 변경)
		if(typeof updateDefrostIndicator === 'function') {
			updateDefrostIndicator('defr' + currentSensorUuid, 'gray', deviceType);
		}
		
		// p16 설정값 업데이트 (MQTT에서 수신된 값으로 설정)
		$('#p16').val(deviceType);
		console.log('p16 설정값 업데이트:', deviceType, '->', deviceTypeText);
		
		// 장치종류 변경 시 버튼 상태 업데이트
		if(typeof updateOutputButtonsEnabled === 'function') {
			updateOutputButtonsEnabled();
    }
    
    console.log('센서 정보 업데이트:', {
        sensorInfo: sensorInfo,
        tempRange: tempRange,
        deviceTypeText: deviceTypeText
    });
}
	
	// setSensor 함수 - 센서 설정 요청
	function setSensor() {
		var message = 'GET&type=1';
		console.log('setSensor 요청:', message);
		
		// 통합 MQTT 관리자를 통한 메시지 발행
		if (typeof UnifiedMQTTManager !== 'undefined' && typeof UnifiedMQTTManager.publishSensorRequest === 'function') {
			UnifiedMQTTManager.publishSensorRequest(message);
		}
	}
	
	// getStatus 함수 - 센서 상태 요청
	function getStatus() {
		var message = 'GET&type=2';
		console.log('getStatus 요청:', message);
		
		// 통합 MQTT 관리자를 통한 메시지 발행
		if (typeof UnifiedMQTTManager !== 'undefined' && typeof UnifiedMQTTManager.publishSensorRequest === 'function') {
			UnifiedMQTTManager.publishSensorRequest(message);
		}
	}

    $('#defrost').click(function() {
		var topicStr = $('#topicStr').val();
		var userId = $('#userId').val();
		var setGu = "defrost";

		var sendData = {
			topicStr: topicStr,
			setGu: setGu,
			userId: userId
		}

		$.ajax({
			url: '/admin/setSensor',
			async: true,
			type: 'POST',
			data: JSON.stringify(sendData),
			dataType: 'json',
			contentType: 'application/json',
            success: function(result) {
				if(null != result) {
					if("200" == result.resultCode) {
						alert("강제제상 성공");
					}
                }
			},
			error: function(result) {
			},
			complete: function(result) {

			}
		});
	});

    $('#stopDefrost').click(function() {
		var topicStr = $('#topicStr').val();
		var userId = $('#userId').val();
		var setGu = "stopDefrost";

		var sendData = {
			topicStr: topicStr,
			setGu: setGu,
			userId: userId
		}

		$.ajax({
			url: '/admin/setSensor',
			async: true,
			type: 'POST',
			data: JSON.stringify(sendData),
			dataType: 'json',
			contentType: 'application/json',
            success: function(result) {
				if(null != result) {
					if("200" == result.resultCode) {
						alert("강제제상 종료 성공");
					}
                }
			},
			error: function(result) {
			},
			complete: function(result) {

			}
		});
	});

	// ============================================
	// 레거시 코드 정리 완료 (2025-10-17)
	// - save2~save5 버튼: HTML에 존재하지 않는 버튼들의 이벤트 핸들러 제거
	// - saveSensorSetting() 함수: 사용되지 않는 함수 제거
	// - 현재 사용 중인 버튼: saveSensor, saveAlarm
	// ============================================

    $('#saveSensor').click(function() {
    	if('${userGrade}' === 'B') {
    		alert("부계정은 설정을 변경할 수 없습니다.");
    		return;
    	}
    	// 저장 전 원래 값들을 저장 (장치에서 받아온 값 또는 화면에 표시된 값 기준)
    	console.log("=== 저장 시 originalValues 확인 ===");
    	console.log("window.originalValues:", window.originalValues);
    	console.log("window.originalValues 타입:", typeof window.originalValues);
    	console.log("window.originalValues가 undefined인가?", window.originalValues === undefined);
    	console.log("window.originalValues가 null인가?", window.originalValues === null);
    	console.log("window.originalValues가 빈 객체인가?", window.originalValues && Object.keys(window.originalValues).length === 0);
    	
    	// p01_display 변수 정의
    	var p01_display = $('#setTemp').text().replace('°C', '');
    	if(!p01_display || p01_display.trim() === '') {
    		p01_display = $('#p01').val();
    	}
    	
    	var originalValues = window.originalValues || {
    		p01: normalizeValue(p01_display), // 화면에 표시된 값 (20.0)
    		p02: normalizeValue($('#p02').attr('data-original') || $('#p02').val()),
    		p03: $('#p03').attr('data-original') || $('#p03').val(),
    		p04: normalizeValue($('#p04').attr('data-original') || $('#p04').val()),
    		p05: $('#p05').attr('data-original') || $('#p05').val(),
    		p06: $('#p06').attr('data-original') || $('#p06').val(),
    		p07: $('#p07').attr('data-original') || $('#p07').val(),
    		p08: normalizeValue($('#p08').attr('data-original') || $('#p08').val()),
    		p09: $('#p09').attr('data-original') || $('#p09').val(),
    		p10: normalizeValue($('#p10').attr('data-original') || $('#p10').val()),
    		p11: $('#p11').attr('data-original') || $('#p11').val(),
    		p12: $('#p12').attr('data-original') || $('#p12').val(),
    		p13: $('#p13').attr('data-original') || $('#p13').val(),
    		p14: $('#p14').attr('data-original') || $('#p14').val(),
    		p15: $('#p15').attr('data-original') || $('#p15').val(),
    		p16: $('#p16').attr('data-original') || $('#p16').val()
    	};
    	
    	console.log("사용할 originalValues:", originalValues);
    	console.log("================================");
    	
    	var topicStr = $('#topicStr').val();
    	var userId = $('#userId').val();
    	var setGu = "param";
    	var p01_input = $('#p01').val(); // 사용자 입력값
    	var p01_display = $('#setTemp').text().replace('°C', ''); // 화면에 표시된 값 (20.0)
    	// p01_display가 비어있거나 undefined인 경우 처리
    	if(!p01_display || p01_display.trim() === '') {
    		p01_display = p01_input; // 사용자 입력값을 기본값으로 사용
    	}
    	var p01_original = p01_input; // 사용자 입력값은 이미 정규화된 형태이므로 그대로 사용
    	var p01 = p01_input; // 장치 전송용 값
    	
    	// p01 정규화 (0, 0.0, 00 모두 동일하게 처리)
    	if(p01 == "0" || p01 == "0.0" || p01 == "00") {
    		p01_original = "0"; // 팝업 표시용으로는 "0"으로 통일
    		p01 = "00"; // 장치 전송용으로는 "00"으로 통일
    	} else {
    		// 100 이상일 때는 10배로 전송
    		var p01_num = parseFloat(p01);
    		if(p01_num >= 100) {
    			p01 = (p01_num * 10).toString(); // 100 → 1000, 150 → 1500
    		} else {
    			// 기존 로직 유지 (99.9 이하)
    			if(p01.indexOf("-") > -1) {
					if (p01.length == 2 || p01.length == 3) {
						p01 = p01 + ".0";
					}
				} else {
					if (p01.length == 1 || p01.length == 2) {
						p01 = p01 + ".0";
					}
				}
    			if(p01.indexOf(".") > -1) {
    				p01 = p01.replace(".", ""); // 장치 전송용으로만 소수점 제거
				}
    		}
    	}
    	var p02_input = $('#p02').val(); // 사용자 입력값
    	var p02_original = p02_input; // 사용자 입력값은 이미 정규화된 형태이므로 그대로 사용
    	var p02 = p02_input; // 장치 전송용 값
    	
    	// p02 정규화 (0, 0.0, 00 모두 동일하게 처리)
    	if(p02 == "0" || p02 == "0.0" || p02 == "00") {
    		p02_original = "0"; // 팝업 표시용으로는 "0"으로 통일
    		p02 = "00"; // 장치 전송용으로는 "00"으로 통일
    	} else {
    		// 기존 로직 유지
    		if(p02.length == 1 || p02.length == 2) {
    			p02 = p02 + ".0";
			}
    		if(p02.indexOf(".") > -1) {
    			p02 = p02.replace(".", ""); // 장치 전송용으로만 소수점 제거
			}
    	}
    	var p03 = $('#p03').val();
		if(p03.indexOf(".") > -1) {
			p03 = p03.replace(".", "");
		}
    	var p04_input = $('#p04').val(); // 사용자 입력값
    	var p04_original = p04_input; // 사용자 입력값은 이미 정규화된 형태이므로 그대로 사용
	// p04가 "0"인 경우 "0.0"으로 통일
	if(p04_original === "0") {
		p04_original = "0.0";
	}
    	var p04 = p04_input; // 장치 전송용 값
    	
    	// p04 정규화 (0, 0.0, 00 모두 동일하게 처리)
    	if(p04 == "0" || p04 == "0.0" || p04 == "00") {
    		p04_original = "0"; // 팝업 표시용으로는 "0"으로 통일
    		p04 = "00"; // 장치 전송용으로는 "00"으로 통일
    	} else {
    		// 기존 로직 유지
    		if(p04.length ==  1 || p04.length == 2) {
    			p04 = p04 + ".0";
			}
			if(p04.indexOf(".") > -1) {
				p04 = p04.replace(".", ""); // 장치 전송용으로만 소수점 제거
			}
    	}
    	var p05 = $('#p05').val();
    	var p06 = $('#p06').val();
    	var p07 = $('#p07 option:selected').val();
    	var p08_input = $('#p08').val(); // 사용자 입력값
    	var p08_original = p08_input; // 시간 관련 값은 그대로 사용
    	var p08 = p08_input; // 장치 전송용 값
    	
    	// p08 정규화 (0, 0.0, 00 모두 동일하게 처리)
    	if(p08 == "0" || p08 == "0.0" || p08 == "00") {
    		p08_original = "0"; // 팝업 표시용으로는 "0"으로 통일
    		p08 = "00"; // 장치 전송용으로는 "00"으로 통일
    	} else {
    		// 기존 로직 유지
    		if(p08.length == 1) {
    			p08 = p08 + ".0";
			}
			if(p08.indexOf(".") > -1) {
				p08 = p08.replace(".", ""); // 장치 전송용으로만 소수점 제거
			}
    	}
    	var p09 = $('#p09').val();
		if(p09.indexOf(".") > -1) {
			p09 = p09.replace(".", "");
		}
    	var p10_input = $('#p10').val(); // 사용자 입력값
    	var p10_original = p10_input; // 사용자 입력값은 이미 정규화된 형태이므로 그대로 사용
    	var p10 = p10_input; // 장치 전송용 값
    	
    	// p10 정규화 (0, 0.0, 00 모두 동일하게 처리)
    	if(p10 == "0" || p10 == "0.0" || p10 == "00") {
    		p10_original = "0"; // 팝업 표시용으로는 "0"으로 통일
    		p10 = "00"; // 장치 전송용으로는 "00"으로 통일
				} else {
    		// 기존 로직 유지
    		if(p10.length == 1 || p10.length == 2) {
    			p10 = p10 + ".0";
			}
			if(p10.indexOf(".") > -1) {
				p10 = p10.replace(".", ""); // 장치 전송용으로만 소수점 제거
			}
    	}
		var p11 = $('#p11 option:selected').val();
		var p12 = $('#p12 option:selected').val();
		var p13 = $('#p13 option:selected').val();
		var p14 = $('#p14 option:selected').val();
		var p15 = $('#p15 option:selected').val();
		var p16 = $('#p16 option:selected').val();

		// 변경된 값들만 수집
		var changedValues = {};
		var settingNames = {
			p01: "설정온도",
			p02: "히스테리시스 편차",
			p03: "COMP 출력 지연시간",
			p04: "온도보정",
			p05: "제상 정지시간",
			p06: "제상 시간",
			p07: "팬설정",
			p08: "제상 후 FAN ON 지연시간",
			p09: "FAN OFF 지연시간",
			p10: "저온방지 온도편차",
			p11: "COMP 누적 시간 제상 선택",
			p12: "온도 센서 타입",
			p13: "수동조작 on/off",
			p14: "통신 국번",
			p15: "통신 속도",
			p16: "장치종류"
		};
		
		// 각 설정값이 변경되었는지 확인 (화면에 표시된 값과 사용자 입력값 비교)
		console.log("=== 변경 감지 디버깅 ===");
		console.log("p01 - 화면표시값:", p01_display, "사용자입력값:", p01_input, "정규화된입력값:", p01_original);
		console.log("p01 - 원래값:", originalValues.p01, "새값:", p01_original, "변경됨:", originalValues.p01 !== p01_original);
		console.log("p04 - 원래값:", originalValues.p04, "새값:", p04_original, "변경됨:", originalValues.p04 !== p04_original);
		console.log("p04 - 원래값 타입:", typeof originalValues.p04, "새값 타입:", typeof p04_original);
		console.log("p04 - 숫자 비교:", Math.abs(parseFloat(p04_original) - parseFloat(originalValues.p04)));
		console.log("p08 - 원래값:", originalValues.p08, "새값:", p08_original, "변경됨:", originalValues.p08 !== p08_original);
		console.log("p10 - 원래값:", originalValues.p10, "새값:", p10_original, "변경됨:", Math.abs(parseFloat(p10_original) - parseFloat(originalValues.p10)) > 0.001);
		console.log("p10 - 원래값 타입:", typeof originalValues.p10, "새값 타입:", typeof p10_original);
		console.log("p10 - 숫자 비교:", Math.abs(parseFloat(p10_original) - parseFloat(originalValues.p10)));
		console.log("p01 - setTemp 텍스트:", $('#setTemp').text());
		console.log("=========================");
		
		// 사용자 입력값을 소수점 형태로 표시하기 위한 함수
		function formatDisplayValue(value) {
			if(value == "0" || value == "0.0" || value == "00") {
				return "0.0";
			}
			// 정수인 경우 .0 추가
			if(value.indexOf(".") === -1) {
				return value + ".0";
			}
			return value;
		}
		
		if(originalValues.p01 !== p01_original) changedValues.p01 = { original: originalValues.p01, new: formatDisplayValue(p01_original) };
		if(originalValues.p02 !== p02_original) changedValues.p02 = { original: originalValues.p02, new: formatDisplayValue(p02_original) };
		if(originalValues.p03 !== p03) changedValues.p03 = { original: originalValues.p03, new: p03 };
		// p04는 온도 관련 값이므로 숫자 비교 사용
		var p04_original_num = parseFloat(p04_original) || 0;
		var p04_original_values_num = parseFloat(originalValues.p04) || 0;
		if(Math.abs(p04_original_num - p04_original_values_num) > 0.001) {
			changedValues.p04 = { original: originalValues.p04, new: formatDisplayValue(p04_original) };
		}
		if(originalValues.p05 !== p05) changedValues.p05 = { original: originalValues.p05, new: p05 };
		if(originalValues.p06 !== p06) changedValues.p06 = { original: originalValues.p06, new: p06 };
		if(originalValues.p07 !== p07) changedValues.p07 = { original: originalValues.p07, new: p07 };
		// p08은 시간 관련 값이므로 문자열 비교 사용
		if(originalValues.p08 !== p08_original) {
			changedValues.p08 = { original: originalValues.p08, new: p08_original }; // 시간 관련 값은 formatDisplayValue 사용하지 않음
		}
		if(originalValues.p09 !== p09) changedValues.p09 = { original: originalValues.p09, new: p09 };
		// p10은 온도 관련 값이므로 숫자 비교 사용
		var p10_original_num = parseFloat(p10_original) || 0;
		var p10_original_values_num = parseFloat(originalValues.p10) || 0;
		if(Math.abs(p10_original_num - p10_original_values_num) > 0.001) {
			changedValues.p10 = { original: originalValues.p10, new: formatDisplayValue(p10_original) };
		}
		if(originalValues.p11 !== p11) changedValues.p11 = { original: originalValues.p11, new: p11 };
		if(originalValues.p12 !== p12) changedValues.p12 = { original: originalValues.p12, new: p12 };
		if(originalValues.p13 !== p13) changedValues.p13 = { original: originalValues.p13, new: p13 };
		if(originalValues.p14 !== p14) changedValues.p14 = { original: originalValues.p14, new: p14 };
		if(originalValues.p15 !== p15) changedValues.p15 = { original: originalValues.p15, new: p15 };
		if(originalValues.p16 !== p16) changedValues.p16 = { original: originalValues.p16, new: p16 };

		// 변경된 값이 있는지 확인
		var hasChanges = false;
		for(var key in changedValues) {
			hasChanges = true;
			break;
		}
		
		if(hasChanges) {
			// 변경된 값만 팝업에 표시
			var popupMessage = "변경된 설정값:\n\n";
			
			// 변경된 값들만 팝업에 추가
			for(var key in changedValues) {
				var change = changedValues[key];
				var settingName = settingNames[key] || key;
				var originalValue = change.original;
				var newValue = change.new;
				
				// 셀렉트박스 항목들은 텍스트로 표시
				if(key === 'p07') {
					// 원래 값도 해당 옵션의 텍스트로 변환
					var originalOption = $('#p07 option[value="' + change.original + '"]').text();
					originalValue = originalOption || change.original;
					newValue = $('#p07 option:selected').text();
				} else if(key === 'p11') {
					var originalOption = $('#p11 option[value="' + change.original + '"]').text();
					originalValue = originalOption || change.original;
					newValue = $('#p11 option:selected').text();
				} else if(key === 'p12') {
					var originalOption = $('#p12 option[value="' + change.original + '"]').text();
					originalValue = originalOption || change.original;
					newValue = $('#p12 option:selected').text();
				} else if(key === 'p13') {
					var originalOption = $('#p13 option[value="' + change.original + '"]').text();
					originalValue = originalOption || change.original;
					newValue = $('#p13 option:selected').text();
				} else if(key === 'p14') {
					var originalOption = $('#p14 option[value="' + change.original + '"]').text();
					originalValue = originalOption || change.original;
					newValue = $('#p14 option:selected').text();
				} else if(key === 'p15') {
					var originalOption = $('#p15 option[value="' + change.original + '"]').text();
					originalValue = originalOption || change.original;
					newValue = $('#p15 option:selected').text();
				} else if(key === 'p16') {
					var originalOption = $('#p16 option[value="' + change.original + '"]').text();
					originalValue = originalOption || change.original;
					newValue = $('#p16 option:selected').text();
				}
				
				popupMessage += settingName + ": " + originalValue + " → " + newValue + "\n";
			}
			
			popupMessage += "\n이 설정값들을 저장하시겠습니까?";
			
			// 확인 팝업 표시
			if(confirm(popupMessage)) {
				// 사용자가 확인을 누르면 실제 전송
				sendDeviceSettings(changedValues, p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12, p13, p14, p15, p16, topicStr, userId, setGu);
			}
				} else {
			alert("변경된 설정값이 없습니다.");
		}
	});
	
	// 전송 함수 정의
	function sendDeviceSettings(changedValues, p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12, p13, p14, p15, p16, topicStr, userId, setGu) {
		var sendData = {
			topicStr: topicStr,
			setGu: setGu,
			userId: userId,
			p01: p01,
			p02: p02,
			p03: p03,
			p04: p04,
			p05: p05,
			p06: p06,
			p07: p07,
			p08: p08,
			p09: p09,
			p10: p10,
			p11: p11,
			p12: p12,
			p13: p13,
			p14: p14,
			p15: p15,
			p16: p16,
			changedValues: changedValues  // 변경된 값들도 함께 전송
		}
		
		$.ajax({
    		url: '/admin/setSensor',
            async: true,
            type: 'POST',
            data: JSON.stringify(sendData),
            dataType: 'json',
            contentType: 'application/json',
            success: function(result) {
                if(result.resultCode == "200") {
					// 응답 플래그 초기화 (새로운 응답을 기다리기 위해)
					window.deviceResponseReceived = false;
					window.deviceResponseData = null;
					console.log("장치 설정 전송 성공 - 응답 플래그 초기화 완료");
					
					// 장치 응답 대기 및 비교 로직
					waitForDeviceResponse(changedValues);


					setTimeout(function() {

						var sendData2 = {
							userId: $('#userId').val(),
							topicStr: $('#topicStr').val(),
							setGu: "readstatus",
							type: "2"  // type=2: status 요청
						}

						$.ajax({
							url: '/admin/setSensor',
							async: true,
							type: 'POST',
							data: JSON.stringify(sendData2),
							dataType: 'json',
							contentType: 'application/json',
							success: function (result) {
								if (result.resultCode == "200") {
									var jsonObj = JSON.parse(result.resultMsg);
									var rcvTopic = result.rcvTopic;
									var rcvTopicArr = Array();

									if(rcvTopic) {
										rcvTopicArr = rcvTopic.split("/");

										if(rcvTopicArr[3] == $('#sensorUuid').val()) {
										}
									}
								}
							},
							error: function (result) {

							}
						});
					}, 3000);  // type 1 요청(1000ms) + 2초 간격 = 3초 후 실행
                } else {
                    alert("센서설정 저장 실패");
                }
            },
            error: function(result) {
                alert("센서설정 저장 실패");
            },
            complete: function(result) {
            }
    	});
    }

	function rcvMsg(topic, message) {
		if(topic) {
			var topicArr = new Array();
			topicArr = topic.split("/");
			var uuid = topicArr[3];
			var userId = topicArr[1];

			// 현재 장치의 센서 값만 출력 (msg 변수 정의 후에 출력)
			if($('#userId').val() == topicArr[1]) {
				// msg 변수는 JSON.parse 이후에 정의되므로 여기서는 출력하지 않음
			}
				
				if(message) {
					if(validateJson(message)) {
						var msg = JSON.parse(message);

					if(uuid == $('#sensorUuid').val()) {
										if (msg.actcode == 'live') {
					// 현재 장치의 센서 값만 출력
					if($('#userId').val() == topicArr[1]) {
						console.log("sensor value : " + msg.value + " (device: " + uuid + ")");
					}
					
				if (msg.name == 'ain') {
				// 현재 온도 알림
						
						// 메인 페이지 방식으로 데이터 수신 시간 업데이트 (ain 메시지에서만)
						var currentSensorUuid = $('#sensorUuid').val();
						if (currentSensorUuid) {
							window['deviceLastDataTime_' + currentSensorUuid] = Date.now();
							console.log('센서설정 페이지 ain 메시지 데이터 수신 시간 업데이트:', currentSensorUuid);
							
							// 정상 데이터 수신 시 에러 상태 해제
							if (window['deviceErrorStates_' + currentSensorUuid]) {
								window['deviceErrorStates_' + currentSensorUuid] = false;
								window['deviceErrorCounters_' + currentSensorUuid] = 0;
								console.log('센서설정 페이지 에러 상태 해제:', currentSensorUuid);
							}
						}
						
						if (msg.value == 'Error') {
									// 실시간 온도데이터가 일정시간 이상 들어오지 않으면 이상표시하고 Error표시
									if($('#status img').attr('src') !== '/images/gray.png') {
										$('#status').html('<img src="/images/gray.png" width="25" height="25">');
									}
									if($('#error img').attr('src') !== '/images/red.png') {
										$('#error').html('<img src="/images/red.png" width="25" height="25">');
									}
									$('#curTemp').html('Error');
									if($('#userId').val() == topicArr[1]) {
												sendNoti("Error", 'ain', topicArr[3], msg.type);
									}
						} else {
									$('#curTemp').html(msg.value + '°C');
									// 정상 온도 데이터 수신 시 이상 해제
									if($('#error img').attr('src') !== '/images/gray.png') {
										$('#error').html('<img src="/images/gray.png" width="25" height="25">');
									}
									if($('#status img').attr('src') !== '/images/green.png') {
										$('#status').html('<img src="/images/green.png" width="25" height="25">');
									}
									if($('#userId').val() == topicArr[1]) {
												sendNoti(msg.value, 'ain', topicArr[3], msg.type);
									}
								}
                            } else if (msg.name == 'din') {
								// input 상태 변화 알림
								if (msg.type == '1' && msg.ch == '1') {
									if (msg.value == '1') {
										// din, type 1, ch1, value 1인 경우: 운전으로 표시하지만 이상으로 표시
										updateStatusIndicator('status' + uuid, 'green', 'status');
										updateStatusIndicator('error' + uuid, 'red', 'error');
										if($('#userId').val() == topicArr[1]) {
											sendNoti(msg.value, 'din', topicArr[3], msg.type);
										}
                                    } else {
										// din, type 1, ch1, value 0인 경우: 이상 해제
										updateStatusIndicator('error' + uuid, 'gray', 'error');
										if($('#userId').val() == topicArr[1]) {
													sendNoti(msg.value, 'din', topicArr[3], msg.type);
										}
									}
								}
                            } else if (msg.name == 'output') {
								// output 상태 변화 알림
								if (msg.value == '1') {
									if (msg.type == '1') {
										// comp 이상
										updateStatusIndicator('comp' + uuid, 'red', 'comp');
									} else if (msg.type == '2') {
										// def 이상
										updateStatusIndicator('defr' + uuid, 'red', 'defr');
									} else if (msg.type == '3') {
										// fan 이상
										updateStatusIndicator('fan' + uuid, 'red', 'fan');
                                    }
								} else if (msg.value == '0') {
									if (msg.type == '1') {
										updateStatusIndicator('comp' + uuid, 'gray', 'comp');
									} else if (msg.type == '2') {
										updateStatusIndicator('defr' + uuid, 'gray', 'defr');
									} else if (msg.type == '3') {
										updateStatusIndicator('fan' + uuid, 'gray', 'fan');
                                    }
								}
							}
						} else if (msg.actcode == "setres") {
							// 장치 응답 처리 - 저장 성공/실패 확인
							console.log("장치 응답 수신:", msg);
							
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
							// 장치에서 받은 원본 값들을 화면 표시용으로 변환하여 저장
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
								p02: msg.p02 ? ((parseInt(msg.p02) / 10).toString() + ".0") : "",
								p03: msg.p03 || "",
								p04: msg.p04 ? ((parseInt(msg.p04) / 10).toString() + ".0") : "",
								p05: msg.p05 || "",
								p06: msg.p06 || "",
								p07: msg.p07 || "",
								p08: msg.p08 ? msg.p08.toString() : "",
								p09: msg.p09 || "",
								p10: msg.p10 ? (parseInt(msg.p10) === 0 ? "0.0" : ((parseInt(msg.p10) / 10).toString() + ".0")) : "",
								p11: msg.p11 || "",
								p12: msg.p12 || "",
                                p13: msg.p13 || "",
								p14: msg.p14 || "",
								p15: msg.p15 || "",
								p16: msg.p16 || ""
							};
							
							console.log("=== setres 메시지에서 originalValues 설정 ===");
							console.log("장치에서 받은 원본 값들:", msg);
							console.log("정규화된 originalValues:", window.originalValues);
							
							// p01 처리 - 1000 이상일 때는 10으로 나누어 처리
							if(msg.p01) {
								var p01Value = parseInt(msg.p01);
								var normalizedValue = p01Value / 10;
								if(normalizedValue === Math.floor(normalizedValue)) {
									// 정수인 경우에만 ".0" 추가
									msg.p01 = normalizedValue.toString() + ".0";
								} else {
									// 소수점이 있는 경우 그대로 사용
									msg.p01 = normalizedValue.toString();
								}
							}
							$('#setTemp').html(msg.p01 + '°C');
							$('#p01').val(msg.p01);
							// p02도 다른 파라미터들처럼 10으로 나누어 처리
							if(msg.p02) {
								var p02Value = parseInt(msg.p02) / 10;
									msg.p02 = p02Value.toString() + ".0";
							}
							$('#p02').val(msg.p02);
							$('#p03').val(msg.p03);
							// p04도 다른 파라미터들처럼 10으로 나누어 처리
							if(msg.p04) {
								var p04Value = parseInt(msg.p04) / 10;
								msg.p04 = p04Value.toString() + ".0";
								// p04가 "0"인 경우 "0.0"으로 통일
								if(p04Value === 0) {
									msg.p04 = "0.0";
								}
							}
							$('#p04').val(msg.p04);
							$('#p05').val(msg.p05);
							$('#p06').val(msg.p06);
							$('#p07').val(msg.p07).prop("selected", true);
							$('#p08').val(msg.p08);
							$('#p09').val(msg.p09);
							// p10도 다른 파라미터들처럼 10으로 나누어 처리
							if(msg.p10) {
								var p10Value = parseInt(msg.p10) / 10;
									msg.p10 = p10Value.toString() + ".0";
								// p10이 "0"인 경우 "0.0"으로 통일
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
						
						// 센서 정보 업데이트 (장치 종류 포함)
						updateSensorInfo(msg);
						
						// 버튼 상태 업데이트 (장치 종류 변경 시)
						if(typeof updateOutputButtonsEnabled === 'function') {
							updateOutputButtonsEnabled();
						}
					} else if (msg.actcode == "actres") {

						}
					}
				}
			}
		}
	}

	function sendNoti(sensorVal, gu, uuid, type) {
		var userId = $('#userId').val();
		var sensorId = $('#sensorId').val();
		var token = $('#token').val();

		var sendData2 = {
			userId: userId,
			sensorId: sensorId,
			sensorUuid: uuid,
			sensorValue: sensorVal,
			token: token,
			name: gu,
			type: type
		}

		$.ajax({
			url: '/sendAlarm',
			async: true,
			type: 'POST',
			data: JSON.stringify(sendData2),
			dataType: 'json',
			contentType: 'application/json',
			success: function (result) {
				if (result.resultCode == "200") {
					console.log('알람 발송 성공 - uuid: ' + uuid + ', value: ' + sensorVal);
				}
			},
			error: function (result) {
				console.error('알람 발송 실패 - uuid: ' + uuid + ', error:', result);
			}
		});
	}

	// 센서설정 페이지 에러 체크 함수 (메인 페이지 방식)
	function chkError() {
		var sensorUuid = $('#sensorUuid').val();
		if (!sensorUuid) return;
		
		// 메인 페이지와 동일한 방식으로 변수 접근
		var deviceLastDataTime = window['deviceLastDataTime_' + sensorUuid] || 0;
		var deviceErrorCounters = window['deviceErrorCounters_' + sensorUuid] || 0;
		var deviceErrorStates = window['deviceErrorStates_' + sensorUuid] || false;
		
		var currentTime = Date.now();
		var timeDiff = currentTime - deviceLastDataTime;
		
		console.log(`[SensorSetting] 에러 체크: sensorUuid=${sensorUuid}, timeDiff=${timeDiff}ms, counter=${deviceErrorCounters}, errorState=${deviceErrorStates}`);
		
		// 9초 동안 온도 데이터 미수신 시 에러 체크 (메인 페이지와 동일)
		if (timeDiff > 9000 && !deviceErrorStates) {
			deviceErrorCounters++;
			window['deviceErrorCounters_' + sensorUuid] = deviceErrorCounters;
            
			console.log(`[SensorSetting] 에러 카운터 증가: ${deviceErrorCounters}`);
			
			// 3번 연속 미수신 시 에러 상태로 변경
			if (deviceErrorCounters >= 3) {
				deviceErrorStates = true;
				window['deviceErrorStates_' + sensorUuid] = true;
				
				console.log(`[SensorSetting] 에러 상태 변경: ${deviceErrorStates}`);
				
				// 상태표시등 업데이트
				updateStatusIndicator('status' + sensorUuid, 'gray', 'status');
				updateStatusIndicator('comp' + sensorUuid, 'gray', 'comp');
				updateStatusIndicator('defr' + sensorUuid, 'gray', 'defr');
				updateStatusIndicator('fan' + sensorUuid, 'gray', 'fan');
				updateStatusIndicator('error' + sensorUuid, 'red', 'error');
				
				// 현재온도 Error 표시
				$('#curTemp').html('Error');
                
				// 알람 전송
				sendNoti("0", "error", sensorUuid, "0");
			} else {
				window['deviceErrorCounters_' + sensorUuid] = deviceErrorCounters;
			}
		} else if (timeDiff < 5000) {
			// 정상 상태에서도 카운터 리셋 (연속 에러 방지)
			if (deviceErrorCounters > 0) {
				window['deviceErrorCounters_' + sensorUuid] = 0;
				console.log(`[SensorSetting] 에러 카운터 리셋`);
			}
		}
	}
	
	// 상태표시등 업데이트 함수 (센서설정 페이지용 - 메인 페이지와 동일한 이미지 방식)
	function updateStatusIndicator(elementId, color, type) {
		console.log(`[SensorSetting] 상태표시등 업데이트: ${elementId} -> ${color}`);
		var element = document.getElementById(elementId);
		if (element) {
			// 메인 페이지와 동일한 방식: className만 변경하고 innerHTML은 그대로 유지
			element.className = 'status-indicator ' + color;
			console.log(`[SensorSetting] 상태표시등 업데이트 완료: ${elementId} -> ${color}`);
		} else {
			console.warn(`[SensorSetting] 상태표시등 요소를 찾을 수 없음: ${elementId}`);
		}
	}

	function startInterval(seconds, callback) {
		callback();
		return setInterval(callback, seconds * 1000);
	}

	function isEmpty(str){

		if(typeof str == "undefined" || str == null || str == "")
			return true;
		else
			return false ;
	}

	// 값 정규화 함수 (장치에서 받은 원본값을 화면 표시용으로 변환)
	function normalizeValue(value) {
		if(!value || value == "0" || value == "0.0" || value == "00") {
			return "0";
		}
		
		// 장치에서 받은 원본값을 화면 표시용으로 변환
		// 예: "200" → "20.0", "10" → "1.0", "90" → "9.0"
		if(value && !isNaN(value) && value.length >= 2) {
			// 온도 관련 값들 (p01, p02, p04, p10)은 10으로 나누어서 표시
			var numValue = parseInt(value);
			if(numValue >= 10) {
				var displayValue = (numValue / 10).toString();
				if(displayValue.indexOf(".") === -1) {
					displayValue = displayValue + ".0";
				}
				return displayValue;
			}
		}
		
		return value;
	}

	function validateJson(str) {
		if(!str || typeof str !== 'string') {
			return false;
		}
		try {
			var json = JSON.parse(str);
			return (typeof json === 'object');
		} catch (e) {
			return false;
		}
	}

	// 출력 제어 버튼 관련 함수들
	function getStoredManualState(){
		var v = '';
		// MQTT에서 받아온 실제 수동조작 값 우선 사용
		if (window.originalValues && window.originalValues.p13 !== undefined && window.originalValues.p13 !== null && String(window.originalValues.p13) !== '') {
			v = String(window.originalValues.p13);
			console.log('getStoredManualState: MQTT 값 사용 - p13 =', v);
		} else if ($('#p13').attr('data-original') && $('#p13').attr('data-original') !== '') {
			// data-original 속성에 저장된 값 사용
			v = String($('#p13').attr('data-original'));
			console.log('getStoredManualState: data-original 사용 - p13 =', v);
		} else {
			// 마지막 수단으로 DOM 값 사용
			v = String($('#p13').val() || '0');
			console.log('getStoredManualState: DOM 값 사용 - p13 =', v);
		}
		console.log('getStoredManualState: 최종 반환값 =', v);
		return v;
	}

	function updateOutputButtonsEnabled(){
		var manualStored = getStoredManualState();
		// MQTT에서 받아온 실제 장치종류 값 사용 (window.originalValues.p16)
		var deviceType = (window.originalValues && window.originalValues.p16) ? window.originalValues.p16 : '0';
		
		// 디버깅 로그 추가
		console.log('=== updateOutputButtonsEnabled 디버깅 ===');
		console.log('manualStored (p13):', manualStored);
		console.log('deviceType (p16):', deviceType);
		console.log('window.originalValues:', window.originalValues);
		console.log('window.originalValues.p13:', window.originalValues ? window.originalValues.p13 : 'undefined');
		console.log('window.originalValues.p16:', window.originalValues ? window.originalValues.p16 : 'undefined');
		
		// 수동조작 상태에 따른 버튼 활성화/비활성화만 처리 (CSS는 단순하게 .is-active 클래스로 처리)
		
		// 장치종류 확인
		var isHeater = (deviceType === '1');
		var isCooler = (deviceType === '0');
		console.log('히터 여부:', isHeater, '쿨러 여부:', isCooler);
		
		if(isHeater) {
			console.log('히터 장치: 모든 버튼 비활성화');
			// 히터인 경우: 모든 버튼 비활성화 (수동조작 상태와 관계없이)
			$('.device-output-btn').prop('disabled', true).css('opacity', '0.5');
			$('#defrost').prop('disabled', true);
			$('#stopDefrost').prop('disabled', true);
			$('#defrost, #stopDefrost').removeClass('is-active');
			
		} else if(isCooler) {
			console.log('쿨러 장치: 수동조작 상태에 따라 출력 제어 버튼 활성화/비활성화');
			// 쿨러인 경우: 수동조작 ON일 때만 버튼 클릭 가능, OFF일 때는 표시만 하고 클릭 불가
			var enabled = (manualStored === '1');
			console.log('쿨러 장치 - 출력 제어 버튼 클릭 가능 여부:', enabled);
			
			// 버튼 클릭 가능 여부만 설정하고, 상태 표시는 항상 가능하도록 함
			$('.device-output-btn').prop('disabled', !enabled);
			// 투명도는 클릭 불가능할 때만 약간 낮춤 (상태 표시는 명확하게)
			$('.device-output-btn').css('opacity', enabled ? '1' : '0.8');
			
			// 강제제상/종료 버튼은 수동조작 ON일 때 비활성
			var defrostDisabled = (manualStored === '1');
			$('#defrost').prop('disabled', defrostDisabled);
			$('#stopDefrost').prop('disabled', defrostDisabled);
		} else {
			console.log('알 수 없는 장치종류:', deviceType);
			// 기본적으로 모든 버튼 비활성화
			$('.device-output-btn').prop('disabled', true).css('opacity', '0.5');
			$('#defrost').prop('disabled', true);
			$('#stopDefrost').prop('disabled', true);
		}
		
		console.log('=== 디버깅 완료 ===');
	}

	// 출력 버튼 상태 업데이트 함수
	function updateOutputButtonState(msg) {
		var type = msg.type;
		var value = msg.value;
		
		console.log('출력 버튼 상태 업데이트:', type, value);
		
		// 타입에 따른 버튼 선택자 매핑
		var buttonSelectors = {
			'1': '.device-output-btn[data-type="1"]', // 콤프
			'2': '.device-output-btn[data-type="2"]', // 제상
			'3': '.device-output-btn[data-type="3"]'  // 팬
		};
		
		var selector = buttonSelectors[type];
		if (selector) {
			// 해당 타입의 모든 버튼에서 is-active 클래스 제거
			$(selector).removeClass('is-active');
			
			// 현재 값에 해당하는 버튼에 is-active 클래스 추가
			$(selector + '[data-val="' + value + '"]').addClass('is-active');
			
			console.log('출력 버튼 상태 업데이트 완료 - 타입:', type, '값:', value);
		} else {
			console.warn('알 수 없는 출력 타입:', type);
		}
	}

	// 출력 제어 버튼 이벤트 핸들러
	$(function(){
		// 저장된 값 기준으로 버튼 활성화 상태 초기화
		updateOutputButtonsEnabled();
		// 초기 setres 기반 활성 버튼 표시
		var manualStoredInit = getStoredManualState();
		if(manualStoredInit === '1'){
			// 상태 메시지로는 output의 현재값을 수신하지 않으니, 기본적으로 두 버튼 모두 비활성 상태 클래스 제거
			$('.device-output-btn').removeClass('is-active');
		} else {
			$('.device-output-btn').removeClass('is-active');
		}
		// 초기 로드 시에도 강제제상 버튼 비활성 반영
		var defrostDisabledInit = (manualStoredInit === '1');
		$('#defrost').prop('disabled', defrostDisabledInit);
		$('#stopDefrost').prop('disabled', defrostDisabledInit);
		
		// p16(장치종류) 변경 이벤트 핸들러 추가
		$('#p16').on('change', function() {
			updateOutputButtonsEnabled();
		});

		$('.device-output-btn').off('click.sensorOutput').on('click.sensorOutput', function(){
			if($('#userGrade').val() === 'B') { alert('부계정은 변경할 수 없습니다.'); return; }
			var manualStored = getStoredManualState();
			
			if(manualStored !== '1') { alert('장치에 저장된 수동조작이 ON일 때만 사용할 수 있습니다. (저장 p13=1)'); return; }
			var type = $(this).data('type');
			var ch = $(this).data('ch');
			var val = $(this).data('val');
			var topicStr = $('#topicStr').val();
			
			if(!topicStr || topicStr.indexOf('+')>=0 || topicStr.indexOf('#')>=0){ alert('잘못된 토픽입니다.'); return; }
			var sendData = {
				userId: $('#userId').val(),
				topicStr: topicStr,
				setGu: 'output',
				outType: String(type),
				outCh: String(ch),
				outValue: String(val)
			};
			var $btn = $(this);
			$.ajax({
				url: '/admin/setSensor',
				type: 'POST',
				async: true,
				data: JSON.stringify(sendData),
				dataType: 'json',
				contentType: 'application/json',
				success: function(res){
					if(res && res.resultCode==='200'){
						console.log('출력제어 요청 성공 - MQTT 응답 대기 중...');
						// MQTT 응답을 기다리므로 즉시 버튼 상태 변경하지 않음
						// 장치에서 실제 상태 변경 후 MQTT output 메시지로 버튼 상태 업데이트
					} else {
						alert('출력제어 실패');
					}
				},
				error: function(xhr){ console.error('출력제어 실패', xhr); alert('출력제어 실패'); }
			});
		});
		
		// 알람설정저장 버튼 이벤트 핸들러
		$('#saveAlarm').off('click.alarmSave').on('click.alarmSave', function(){
			if($('#userGrade').val() === 'B') { 
				alert('부계정은 알람 설정을 변경할 수 없습니다.'); 
				return; 
			}
			
			// 알람 설정 데이터 수집
			var alarmData = {
				userId: $('#userId').val(),
				sensorUuid: $('#sensorUuid').val(),
				topicStr: $('#topicStr').val(),  // topicStr 추가
				alarmYn1: $('#alarmYn1').val(),
				setVal1: $('#setVal1').val(),
				delayHour1: $('#delayHour1').val(),
				delayMin1: $('#delayMin1').val(),
				reDelayHour1: $('#reDelayHour1').val(),
				reDelayMin1: $('#reDelayMin1').val(),
				alarmYn2: $('#alarmYn2').val(),
				setVal2: $('#setVal2').val(),
				delayHour2: $('#delayHour2').val(),
				delayMin2: $('#delayMin2').val(),
				reDelayHour2: $('#reDelayHour2').val(),
				reDelayMin2: $('#reDelayMin2').val(),
				alarmYn3: $('#alarmYn3').val(),
				setVal3: $('#setVal3').val(),
				delayHour3: $('#delayHour3').val(),
				delayMin3: $('#delayMin3').val(),
				reDelayHour3: $('#reDelayHour3').val(),
				reDelayMin3: $('#reDelayMin3').val(),
				alarmYn4: $('#alarmYn4').val(),
				setVal4: $('#setVal4').val() || "",  // setVal4 추가
				delayHour4: $('#delayHour4').val(),
				delayMin4: $('#delayMin4').val(),
				reDelayHour4: $('#reDelayHour4').val(),
				reDelayMin4: $('#reDelayMin4').val(),
				alarmYn5: $('#alarmYn5').val(),
				delayHour5: $('#delayHour5').val(),
				delayMin5: $('#delayMin5').val(),
				reDelayHour5: $('#reDelayHour5').val(),
				reDelayMin5: $('#reDelayMin5').val()
			};
			
			console.log('알람설정 저장 요청:', alarmData);
			
			$.ajax({
				url: '/admin/saveSensorSetting',  // 올바른 URL로 변경
				type: 'POST',
				async: true,
				data: JSON.stringify(alarmData),
				dataType: 'json',
				contentType: 'application/json',
				success: function(res){
					if(res && res.resultCode === '200'){
						// 변경 내역 알림
						alert(res.resultMessage || '알람설정이 성공적으로 저장되었습니다.');
						console.log('알람설정 저장 성공:', res);
						
						// DB에서 저장된 설정을 다시 읽어와서 UI 갱신
						reloadAlarmSettings();
					} else {
						alert('알람설정 저장에 실패했습니다: ' + (res.resultMessage || '알 수 없는 오류'));
						console.error('알람설정 저장 실패:', res);
					}
				},
				error: function(xhr, status, error){
					console.error('알람설정 저장 실패:', xhr, status, error);
					alert('알람설정 저장 중 오류가 발생했습니다.');
				}
			});
		});
		
		// 알람 설정을 DB에서 다시 읽어와서 UI를 갱신하는 함수
		function reloadAlarmSettings() {
			var userId = $('#userId').val();
			var sensorUuid = $('#sensorUuid').val();
			
			console.log('알람설정 재조회 요청: userId=' + userId + ', sensorUuid=' + sensorUuid);
			
			$.ajax({
				url: '/data/getSensorSettings',
				type: 'POST',
				async: true,
				data: { sensorUuid: sensorUuid },
				dataType: 'json',
				success: function(result) {
					if(result && result.resultCode === '200' && result.settings) {
						var settings = result.settings;
						console.log('알람설정 재조회 성공:', settings);
						
						// UI 갱신
						updateAlarmUI(settings);
						
						// 성공 메시지
						console.log('✅ 알람설정이 DB에서 다시 로드되어 UI가 갱신되었습니다.');
						
						// 사용자에게 알림 (선택적)
						setTimeout(function() {
							alert('알람설정 변경이 완료되었습니다.\n설정값이 정상적으로 저장되었습니다.');
						}, 500);
						
					} else {
						console.warn('알람설정 재조회 실패:', result);
					}
				},
				error: function(xhr, status, error) {
					console.error('알람설정 재조회 중 오류:', xhr, status, error);
				}
			});
		}
		
		// 알람 설정 UI를 업데이트하는 함수
		function updateAlarmUI(settings) {
			console.log('=== 알람 설정 UI 갱신 시작 ===');
			
			// 알람1 (고온알람)
			if(settings.alarm_yn1) {
				$('#alarmYn1').val(settings.alarm_yn1);
				console.log('고온알람 사용여부: ' + settings.alarm_yn1);
			}
			if(settings.set_val1) {
				// 소숫점 포맷팅: 정수인 경우 .0 추가
				var setVal1 = formatTemperatureValue(settings.set_val1);
				$('#setVal1').val(setVal1);
				console.log('고온 설정값: ' + setVal1 + ' (원본: ' + settings.set_val1 + ')');
			}
			if(settings.delay_time1 !== undefined && settings.delay_time1 !== null) {
				var delayTime1 = parseInt(settings.delay_time1);
				var hour1 = Math.floor(delayTime1 / 60);
				var min1 = delayTime1 % 60;
				$('#delayHour1').val(hour1);
				$('#delayMin1').val(min1);
				console.log('고온알람 지연시간: ' + hour1 + '시간 ' + min1 + '분');
			}
			if(settings.re_delay_time1 !== undefined && settings.re_delay_time1 !== null) {
				var reDelayTime1 = parseInt(settings.re_delay_time1);
				var reHour1 = Math.floor(reDelayTime1 / 60);
				var reMin1 = reDelayTime1 % 60;
				$('#reDelayHour1').val(reHour1);
				$('#reDelayMin1').val(reMin1);
				console.log('고온알람 재전송지연시간: ' + reHour1 + '시간 ' + reMin1 + '분');
			}
			
			// 알람2 (저온알람)
			if(settings.alarm_yn2) {
				$('#alarmYn2').val(settings.alarm_yn2);
			}
			if(settings.set_val2) {
				// 소숫점 포맷팅: 정수인 경우 .0 추가
				var setVal2 = formatTemperatureValue(settings.set_val2);
				$('#setVal2').val(setVal2);
				console.log('저온 설정값: ' + setVal2 + ' (원본: ' + settings.set_val2 + ')');
			}
			if(settings.delay_time2 !== undefined && settings.delay_time2 !== null) {
				var delayTime2 = parseInt(settings.delay_time2);
				$('#delayHour2').val(Math.floor(delayTime2 / 60));
				$('#delayMin2').val(delayTime2 % 60);
			}
			if(settings.re_delay_time2 !== undefined && settings.re_delay_time2 !== null) {
				var reDelayTime2 = parseInt(settings.re_delay_time2);
				$('#reDelayHour2').val(Math.floor(reDelayTime2 / 60));
				$('#reDelayMin2').val(reDelayTime2 % 60);
			}
			
			// 알람3 (특정온도알람)
			if(settings.alarm_yn3) {
				$('#alarmYn3').val(settings.alarm_yn3);
			}
			if(settings.set_val3) {
				// 소숫점 포맷팅: 정수인 경우 .0 추가
				var setVal3 = formatTemperatureValue(settings.set_val3);
				$('#setVal3').val(setVal3);
				console.log('특정온도 설정값: ' + setVal3 + ' (원본: ' + settings.set_val3 + ')');
			}
			if(settings.delay_time3 !== undefined && settings.delay_time3 !== null) {
				var delayTime3 = parseInt(settings.delay_time3);
				$('#delayHour3').val(Math.floor(delayTime3 / 60));
				$('#delayMin3').val(delayTime3 % 60);
			}
			if(settings.re_delay_time3 !== undefined && settings.re_delay_time3 !== null) {
				var reDelayTime3 = parseInt(settings.re_delay_time3);
				$('#reDelayHour3').val(Math.floor(reDelayTime3 / 60));
				$('#reDelayMin3').val(reDelayTime3 % 60);
			}
			
			// 알람4 (DI알람)
			if(settings.alarm_yn4) {
				$('#alarmYn4').val(settings.alarm_yn4);
			}
			if(settings.set_val4) {
				$('#setVal4').val(settings.set_val4);
			}
			if(settings.delay_time4 !== undefined && settings.delay_time4 !== null) {
				var delayTime4 = parseInt(settings.delay_time4);
				$('#delayHour4').val(Math.floor(delayTime4 / 60));
				$('#delayMin4').val(delayTime4 % 60);
			}
			if(settings.re_delay_time4 !== undefined && settings.re_delay_time4 !== null) {
				var reDelayTime4 = parseInt(settings.re_delay_time4);
				$('#reDelayHour4').val(Math.floor(reDelayTime4 / 60));
				$('#reDelayMin4').val(reDelayTime4 % 60);
			}
			
			// 알람5 (통신이상알람)
			if(settings.alarm_yn5) {
				$('#alarmYn5').val(settings.alarm_yn5);
			}
			if(settings.delay_time5 !== undefined && settings.delay_time5 !== null) {
				var delayTime5 = parseInt(settings.delay_time5);
				$('#delayHour5').val(Math.floor(delayTime5 / 60));
				$('#delayMin5').val(delayTime5 % 60);
			}
			if(settings.re_delay_time5 !== undefined && settings.re_delay_time5 !== null) {
				var reDelayTime5 = parseInt(settings.re_delay_time5);
				$('#reDelayHour5').val(Math.floor(reDelayTime5 / 60));
				$('#reDelayMin5').val(reDelayTime5 % 60);
			}
			
			console.log('=== 알람 설정 UI 갱신 완료 ===');
		}
	});

	function goMain() {
		//startDisconnect();
		location.href = "/main/main";
}
</script>

<!-- MQTT 스크립트 추가 -->
<script src="/js/unified-mqtt-manager.js?v=20251007"></script>
<script>
	// MQTT 연결 함수 정의 (메인 페이지와 동일)
	function startConnect() {
		console.log('=== 센서설정 페이지 startConnect 함수 호출됨 ===');
		
		// UnifiedMQTTManager 로드 대기 (최대 5초)
		var maxAttempts = 50; // 50 * 100ms = 5초
		var attempts = 0;
		
		function checkAndConnect() {
			attempts++;
			
			if (typeof UnifiedMQTTManager !== 'undefined') {
				console.log('UnifiedMQTTManager 로드 완료 (시도 ' + attempts + '회)');
				
				// MQTT 연결 시작
				UnifiedMQTTManager.startMQTTConnection();
				
				// 연결 성공 후 초기 동기화 실행
				setTimeout(function() {
					UnifiedMQTTManager.executeInitialSync();
				}, 1000); // 1초 후 초기 동기화
				
			} else if (attempts < maxAttempts) {
				console.log('UnifiedMQTTManager 로드 대기 중... (시도 ' + attempts + '/' + maxAttempts + ')');
				setTimeout(checkAndConnect, 100); // 100ms 후 재시도
			} else {
				console.error('UnifiedMQTTManager 로드 실패 - 최대 시도 횟수 초과');
			}
		}
		
		// 즉시 첫 번째 시도
		checkAndConnect();
	}
	
	// 전역 함수로 등록
	window.startConnect = startConnect;
	
	// MQTT 초기화
	$(document).ready(function() {
		// 센서 정보 초기화 (서버에서 전달받은 값으로 설정)
		initializeSensorInfo();

		// MQTT 연결 시작
		startConnect();

// 전역 함수 등록 (센서설정 페이지용)
window.rcvMsg = rcvMsg;
window.setSensor = setSensor;
window.getStatus = getStatus;
		// handleSetresMessage는 sensor-setting.js에서 window 객체에 직접 할당됨
	});

	// 센서 정보 초기화 함수
	function initializeSensorInfo() {
		console.log('센서 정보 초기화 시작');
		
		// 서버에서 전달받은 값들
		var sensorDisplayName = '${sensorDisplayName}';
		var tempRange = '${tempRange}';
		var deviceTypeText = '${deviceTypeText}';
		
		console.log('서버 값들:', {
			sensorDisplayName: sensorDisplayName,
			tempRange: tempRange,
			deviceTypeText: deviceTypeText
		});
		
		// JSP EL 변수가 제대로 렌더링되지 않은 경우 기본값 사용
		if (sensorDisplayName === '${sensorDisplayName}' || sensorDisplayName === '') {
			sensorDisplayName = '-';
		}
		if (tempRange === '${tempRange}' || tempRange === '') {
			tempRange = '-';
		}
		if (deviceTypeText === '${deviceTypeText}' || deviceTypeText === '') {
			deviceTypeText = '-';
		}
		
		// 장치설정 섹션 업데이트
		$('#deviceSensorInfoDisplay').text(sensorDisplayName);
		$('#deviceTempRangeDisplay').text(tempRange);
		$('#deviceDeviceTypeDisplay').text(deviceTypeText);
		
		// 알람설정 섹션도 동일하게 업데이트
		$('#alarmSensorInfoDisplay').text(sensorDisplayName);
		$('#alarmTempRangeDisplay').text(tempRange);
		$('#alarmDeviceTypeDisplay').text(deviceTypeText);
		
		console.log('센서 정보 초기화 완료:', {
			deviceSensorInfo: $('#deviceSensorInfoDisplay').text(),
			deviceTempRange: $('#deviceTempRangeDisplay').text(),
			deviceDeviceType: $('#deviceDeviceTypeDisplay').text()
		});
	}
</script>

</body>
</html>
