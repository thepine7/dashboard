<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>

<html lang="ko" class="">
<head>
  <meta charset="UTF-8">
  <!--[if IE]><meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1"><![endif]-->
  <title>H&T Solutions</title>
  <link rel="icon" href="/images/hntbi.png" type="image/png">
  <meta name="keywords" content="" />
  <meta name="description" content="" />
  <meta name="viewport" content="width=device-width">
  <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
  <meta http-equiv="Pragma" content="no-cache">
  <meta http-equiv="Expires" content="0">
  <link rel="stylesheet" href="/css/templatemo_main.css">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
  <style>
    /* 인라인 스타일 최소화 - 성능 향상 */
    .material-symbols-outlined {
      font-variation-settings: 'FILL' 0, 'wght' 500, 'GRAD' 0, 'opsz' 24;
    }
    
    /* 자주 사용되는 스타일을 미리 정의 */
    .navbar-bg { background-color: #ffffff; }
    .content-bg { background-color: #333333; }
    .text-light { color: #f0f8ff; }
    .btn-toggle-bg { background-color: #cccccc; }
    .sidebar-bg { background-color: #afd9ee; }
    
    /* 이미지 최적화 */
    img { max-width: 100%; height: auto; }
    
    /* 폰트 최적화 */
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
    
    /* 상태표시등 스타일 */
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
    
    /* 아이콘 스타일 */
    .icon-btn {
      display: inline-block;
      padding: 8px;
      margin: 2px;
      border-radius: 5px;
      transition: all 0.3s ease;
      background-color: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      text-decoration: none;
      color: #fff;
    }
    .icon-btn:hover {
      background-color: rgba(255, 255, 255, 0.2);
      transform: scale(1.05);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
      text-decoration: none;
      color: #fff;
    }
    .icon-btn i {
      font-size: 20px;
      vertical-align: middle;
    }
    .icon-btn img {
      vertical-align: middle;
      border-radius: 2px;
    }
    
    /* 버튼 스타일 강화 */
    .btn-primary {
      background-color: #337ab7;
      border-color: #2e6da4;
      color: white;
      padding: 6px 12px;
      border-radius: 4px;
      text-decoration: none;
    }
    .btn-primary:hover {
      background-color: #286090;
      border-color: #204d74;
      color: white;
      text-decoration: none;
    }
  </style>
  
  <!-- 통일된 세션 관리 모듈 사용 -->
  <script src="/js/session-manager.js"></script>
  <!-- 하트비트 관리자 (사용자 활동 추적) -->
  <script src="/js/heartbeat-manager.js"></script>
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
  <script src="/js/unified-mqtt-manager.js?v=20251018"></script>
</head>
<body>
<input type="hidden" id="userId" name="userId" value="${not empty userId ? userId : ''}">
<input type="hidden" id="loginUserId" name="loginUserId" value="${not empty loginUserId ? loginUserId : ''}">
<input type="hidden" id="userGrade" name="userGrade" value="${not empty userGrade ? userGrade : 'U'}">
<input type="hidden" id="userNm" name="userNm" value="${not empty userNm ? userNm : ''}">
<input type="hidden" id="sensorId" name="sensorId" value="${not empty sensorId ? sensorId : ''}">
<input type="hidden" id="userEmail" name="userEmail" value="${not empty userEmail ? userEmail : ''}">
<input type="hidden" id="userTel" name="userTel" value="${not empty userTel ? userTel : ''}">
<input type="hidden" id="token" name="token" value="${not empty token ? token : ''}">
<input type="hidden" id="saveId" name="saveId" value="${not empty saveId ? saveId : ''}">

<!-- 세션 정보 디버깅 (개발 환경에서만 표시) -->
<c:if test="${pageContext.request.serverName == 'localhost' || pageContext.request.serverName == '127.0.0.1' || pageContext.request.serverName == 'iot.hntsolution.co.kr'}">
<div style="position: fixed; top: 10px; right: 10px; background: rgba(0,0,0,0.8); color: white; padding: 10px; border-radius: 5px; font-size: 12px; z-index: 9999; max-width: 300px;">
  <strong>세션 정보 디버깅:</strong><br>
  userId: ${userId}<br>
  userGrade: ${userGrade}<br>
  userNm: ${userNm}<br>
  sensorId: ${sensorId}<br>
  loginUserId: ${loginUserId}<br>
  userEmail: ${userEmail}<br>
  userTel: ${userTel}<br>
  token: ${token}<br>
  <button onclick="this.parentElement.style.display='none'" style="margin-top: 5px; padding: 2px 5px; font-size: 10px;">닫기</button>
</div>
</c:if>

<script>
  console.log('=== 세션 관리 모듈 초기화 ===');
  
  // 세션 정보 디버깅 로그 추가
  console.log('=== JSP EL 표현식 값 확인 ===');
  console.log('userId (EL):', '${userId}');
  console.log('userGrade (EL):', '${userGrade}');
  console.log('userNm (EL):', '${userNm}');
  console.log('sensorId (EL):', '${sensorId}');
  console.log('loginUserId (EL):', '${loginUserId}');
  console.log('userEmail (EL):', '${userEmail}');
  console.log('userTel (EL):', '${userTel}');
  console.log('token (EL):', '${token}');
  
  // Hidden input 값 확인
  console.log('=== Hidden Input 값 확인 ===');
  console.log('userId (input):', document.getElementById('userId') ? document.getElementById('userId').value : 'null');
  console.log('userGrade (input):', document.getElementById('userGrade') ? document.getElementById('userGrade').value : 'null');
  console.log('userNm (input):', document.getElementById('userNm') ? document.getElementById('userNm').value : 'null');
  console.log('sensorId (input):', document.getElementById('sensorId') ? document.getElementById('sensorId').value : 'null');
  console.log('loginUserId (input):', document.getElementById('loginUserId') ? document.getElementById('loginUserId').value : 'null');
  console.log('userEmail (input):', document.getElementById('userEmail') ? document.getElementById('userEmail').value : 'null');
  console.log('userTel (input):', document.getElementById('userTel') ? document.getElementById('userTel').value : 'null');
  console.log('token (input):', document.getElementById('token') ? document.getElementById('token').value : 'null');
  
  // SessionManager 초기화
  if (typeof SessionManager !== 'undefined') {
    if (!SessionManager.initialize()) {
      console.error('세션 관리 모듈 초기화 실패');
            window.location.href = '/login/login';
    }
            } else {
    console.warn('SessionManager 모듈을 찾을 수 없습니다. 기본 세션 처리 사용');
    
    // 기본 세션 정보 처리 (fallback)
    window.SessionData = {
      userId: document.getElementById('userId') ? document.getElementById('userId').value : '',
      userGrade: document.getElementById('userGrade') ? document.getElementById('userGrade').value : 'U',
      userNm: document.getElementById('userNm') ? document.getElementById('userNm').value : '',
      loginUserId: document.getElementById('loginUserId') ? document.getElementById('loginUserId').value : '',
      sensorId: document.getElementById('sensorId') ? document.getElementById('sensorId').value : '',
      userEmail: document.getElementById('userEmail') ? document.getElementById('userEmail').value : '',
      userTel: document.getElementById('userTel') ? document.getElementById('userTel').value : '',
      token: document.getElementById('token') ? document.getElementById('token').value : '',
      saveId: document.getElementById('saveId') ? document.getElementById('saveId').value : ''
    };
    
    if (!window.SessionData.userId) {
      console.warn('세션 정보가 없습니다. 로그인 페이지로 이동합니다.');
          window.location.href = '/login/login';
    }
  }
  
  console.log('=== 세션 정보 설정 완료 ===');
  console.log('window.SessionData:', window.SessionData);
  
  // MQTT 연결 함수 정의 (강화된 중복 방지 + 로드 대기)
  function startConnect() {
    console.log('=== startConnect 함수 호출됨 ===');
    
    // UnifiedMQTTManager 로드 대기 (최대 5초)
    var maxAttempts = 50; // 50 * 100ms = 5초
    var attempts = 0;
    
    function checkAndConnect() {
      attempts++;
      
      if (typeof UnifiedMQTTManager !== 'undefined') {
        console.log('UnifiedMQTTManager 로드 완료 (시도 ' + attempts + '회)');
        console.log('UnifiedMQTTManager를 사용하여 MQTT 연결 시작');
        
        // 중복 방지 상태 확인
        var duplicateState = UnifiedMQTTManager.getDuplicatePreventionState();
        console.log('중복 방지 상태:', duplicateState);
        
        // 중복 방지 체크
        var duplicateCheck = UnifiedMQTTManager.checkDuplicateInitialization();
        if (!duplicateCheck.allowed) {
          console.log('중복 초기화 방지:', duplicateCheck.reason);
          return;
        }
        
        // 표준 MQTT 초기화 템플릿 사용
        registerMQTTInitialization({
          pageName: 'Main',
          requestSettings: function() {
            // 설정값 요청 (GET&type=1) - main.jsp는 다중 장치이므로 각 장치별로 요청
            console.log('=== Main 페이지: 설정값 요청 시작 ===');
            
            // 센서 UUID 목록 확인
            var sensorUuids = [];
            $('input[id^="sensorUuid"]').each(function(){
              var uuid = $(this).val();
              if (uuid) {
                sensorUuids.push(uuid);
              }
            });
            console.log('발견된 센서 UUID 목록:', sensorUuids);
            
            // 각 센서별로 설정값 요청
            $('input[id^="sensorUuid"]').each(function(){
              var uuid = $(this).val();
              if (uuid && typeof window['setSensor_' + uuid] === 'function') {
                console.log('✅ setSensor_' + uuid + ' 함수 호출');
                try {
                  window['setSensor_' + uuid]();
                } catch(e) {
                  console.error('❌ setSensor_' + uuid + ' 함수 실행 실패:', e);
                }
              } else {
                console.warn('⚠️ setSensor_' + uuid + ' 함수가 정의되지 않음');
              }
            });
            
            console.log('=== Main 페이지: 설정값 요청 완료 ===');
          },
          requestStatus: function() {
            // 상태값 요청 (GET&type=2) - main.jsp는 다중 장치이므로 각 장치별로 요청
            console.log('=== Main 페이지: 상태값 요청 시작 ===');
            
            // 각 센서별로 상태값 요청 (2초 후 실행)
            setTimeout(function() {
              console.log('=== 상태값 요청 실행 (2초 후) ===');
              
              // 센서 UUID 목록 확인
              var sensorUuids = [];
              $('input[id^="sensorUuid"]').each(function(){
                var uuid = $(this).val();
                if (uuid) {
                  sensorUuids.push(uuid);
                }
              });
              console.log('상태값 요청할 센서 UUID 목록:', sensorUuids);
              
              $('input[id^="sensorUuid"]').each(function(){
                var uuid = $(this).val();
                if (uuid && typeof window['getStatus_' + uuid] === 'function') {
                  console.log('✅ getStatus_' + uuid + ' 함수 호출');
                  try {
                    window['getStatus_' + uuid]();
                  } catch(e) {
                    console.error('❌ getStatus_' + uuid + ' 함수 실행 실패:', e);
                  }
                } else {
                  console.warn('⚠️ getStatus_' + uuid + ' 함수가 정의되지 않음');
                }
              });
              
              console.log('=== Main 페이지: 상태값 요청 완료 ===');
            }, 2000);
          },
          startErrorCheck: function() {
            // 에러 체크 시작
            console.log('Main 페이지: 에러 체크 시작');
            // 기존 로직 유지
          },
          initializePageSpecific: function() {
            // Main 페이지별 추가 초기화
            console.log('Main 페이지: 페이지별 초기화');
            // 기존 로직 유지
          }
        });
        
      } else if (attempts < maxAttempts) {
        console.log('UnifiedMQTTManager 로드 대기 중... (' + attempts + '/' + maxAttempts + ')');
        setTimeout(checkAndConnect, 100); // 100ms 후 재시도
      } else {
        console.error('UnifiedMQTTManager 로드 타임아웃 (' + (maxAttempts * 100) + 'ms)');
        console.error('MQTT 연결 실패 - 페이지를 새로고침해주세요.');
        alert('MQTT 연결 실패\n페이지를 새로고침해주세요.');
      }
    }
    
    // 즉시 첫 번째 시도
    checkAndConnect();
  }
  
  // 전역 함수로 등록
  window.startConnect = startConnect;
    </script>
<c:choose>
  <c:when test="${sensorList eq null}"></c:when>
  <c:otherwise>
    <c:forEach var="item" items="${sensorList}">

      <c:choose>
        <c:when test="${empty sensorId }">
          <input type="hidden" id="sensorUuid${item.sensor_uuid}" name="sensorUuid${item.sensor_uuid}" value="${item.sensor_uuid}" />
        </c:when>
        <c:otherwise>
          <input type="hidden" id="sensorUuid${item.sensor_uuid}" name="sensorUuid${item.sensor_uuid}" value="${item.sensor_uuid}" />
        </c:otherwise>
      </c:choose>
      <input type="hidden" id="sensorType" name="sensorType" value="${item.sensor_type}" />
      <input type="hidden" id="topicStr${item.sensor_uuid}" name="topicStr${item.sensor_uuid}" value="HBEE/${item.sensor_id}/TC/${item.sensor_uuid}/SER" />
      <input type="hidden" id="sensorList" name="sensorList" value="${sensorList}" />
      <input type="hidden" id="sensorArr" name="sensorArr" value="${sensorArr}" />
    </c:forEach>
  </c:otherwise>
</c:choose>

<div class="navbar navbar-inverse navbar-bg" role="navigation">
  <div class="navbar-header">
    		<div class="logo navbar-bg"><h1><a href="javascript:goMain();"><img src="/images/hntbi.png" width="70" height="32" alt="H&T Logo"></a></h1></div>
    <button type="button" class="navbar-toggle btn-toggle-bg" data-toggle="collapse" data-target=".navbar-collapse">
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
          <ul class="sidebar-bg" style="height: 40px; padding-top: 10px">
            <strong>${userNm}님 안녕하세요.</strong>
          </ul>
        <c:if test="${userGrade eq 'A' || userGrade eq 'U'}">
          <a href="javascript:;">
            <i class="fa fa-database"></i> 관리 메뉴보기 <div class="pull-right"><span class="caret"></span></div>
          </a>
          <ul class="templatemo-submenu">
            <li><a href="javascript:goUserList();">사용자 관리</a></li>
          </ul>
          <ul class="templatemo-submenu">
            <li><a href="javascript:goCreateSub();">부계정 생성</a></li>
          </ul>
        </c:if>
      </li>
      <li><a href="javascript:void(0);" onclick="prepareLogout();"><i class="fa fa-sign-out"></i>로그아웃</a></li>
    </ul>
  </div><!--/.navbar-collapse -->

  <div class="templatemo-content-wrapper content-bg">
    <div class="templatemo-content content-bg">
      <h1><span class="text-light">Main</span></h1>
      
      <div class="templatemo-charts">
        <div class="row">
          <!--여기부터-->

          
          <c:choose>
            <c:when test="${sensorList eq null}">
              <div class="col-md-12">
                <div class="alert alert-warning">
                  <strong>경고!</strong> sensorList가 null입니다.
                </div>
              </div>
            </c:when>
            <c:when test="${fn:length(sensorList) eq 0}">
              <div class="col-md-12">
                <div class="alert alert-warning">
                  <strong>경고!</strong> sensorList가 비어있습니다. (크기: 0)
                </div>
              </div>
            </c:when>
            <c:otherwise>
              <c:forEach var="item" items="${sensorList}">
              <div class="col-md-6 col-sm-6">
                <span class="btn btn-primary">
                  <c:if test="${userGrade ne 'B'}">
                    <a href="javascript:goSensorSetting_${item.sensor_uuid}();" class="icon-btn" title="장치설정"><i class="bi bi-gear-fill"></i></a>
                  </c:if>
                  <c:if test="${item.chart_type eq 'none'}">
                    <c:if test="${userGrade ne 'B'}">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</c:if>
                    <a href="javascript:goChartSetting_${item.sensor_uuid}();" class="icon-btn" title="차트설정"><i class="bi bi-graph-up"></i></a>
                  </c:if>
                  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="javascript:goChartData_${item.sensor_uuid}();" class="icon-btn" title="차트데이터"><i class="bi bi-bar-chart-fill"></i></a>
                </span>
                <div class="panel panel-primary">
                  <div class="panel-heading"><strong>${item.sensor_name} <span id="deviceType${item.sensor_uuid}" style="font-size: 12px; color: #ccc;">(장치종류)</span></strong></div>
                  <input type="hidden" id="name${item.sensor_uuid}" name="name${item.sensor_uuid}" value="${item.sensor_name}">
                  <input type="hidden" id="sensorUuid${item.sensor_uuid}" name="sensorUuid${item.sensor_uuid}" value="${item.sensor_uuid}">
                  <input type="hidden" id="topicStr${item.sensor_uuid}" name="topicStr${item.sensor_uuid}" value="HBEE/${item.sensor_id}/TC/${item.sensor_uuid}/SER">
                  <div class="panel-body">
                    <table class="table table-striped">
                      <thead>
                      <tr>
                        <td align="center" width="30%" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; ">구분</span></strong></td>
                        <td align="center" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; ">온도</span></strong></td>
                      </tr>
                      </thead>
                      <tbody>
                      <tr>
                        <td align="center">설정온도</td>
                        <td align="center">
                          <strong><span align="center" id="setTmp${item.sensor_uuid}" name="setTmp${item.sensor_uuid}" style="color: #4cae4c"></span></strong>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" valign="middle">현재온도</td>
                        <td align="center">
                          <strong><span align="center" id="sensorVal${item.sensor_uuid}" name="sensorVal${item.sensor_uuid}" style="color: #c7254e">-</span></strong>
                        </td>
                      </tr>
                      </tbody>
                    </table>
                    <table class="table table-striped">
                      <thead>
                      <tr>
                        <td align="center"><div id="status${item.sensor_uuid}" class="status-indicator green"><i class="bi bi-play-circle-fill"></i></div></td>
                        <td align="center"><div id="comp${item.sensor_uuid}" class="status-indicator gray"><i class="bi bi-gear-fill"></i></div></td>
                        <td align="center"><div id="defr${item.sensor_uuid}" class="status-indicator gray"><i class="bi bi-snow defrost-icon"></i><i class="bi bi-thermometer-half heater-icon" style="display:none;"></i></div></td>

                        <td align="center"><div id="fan${item.sensor_uuid}" class="status-indicator gray"><i class="bi bi-fan"></i></div></td>
                        <td align="center"><div id="error${item.sensor_uuid}" class="status-indicator gray"><i class="bi bi-exclamation-triangle-fill"></i></div></td>
                      </tr>
                      </thead>
                      <tbody>
                      <tr>
                                   <td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">운전</span></strong></td>
           <td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">콤프</span></strong></td>
           <td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span id="defrostLabel${item.sensor_uuid}" style="color: #f0f8ff; font-size:10pt;">제상</span></strong></td>
              
           <td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">FAN</span></strong></td>
           <td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">이상</span></strong></td>
                      </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
                  <c:choose>
                    <c:when test="${item.chart_type eq 'none'}">
                    </c:when>
                    <c:otherwise>
                      <div class="col-md-6 col-sm-6">
                        <span class="btn btn-primary">
                          <c:if test="${userGrade ne 'B'}">
                            <a href="javascript:goChartSetting_${item.sensor_uuid}();" class="icon-btn" title="차트설정"><i class="bi bi-graph-up"></i></a>
                          </c:if>
                        </span>
                        <div class="panel panel-primary">
                          <div class="panel-heading">그래프</div>
                          <canvas id="sensorChart${item.sensor_uuid}" height=300" style="height:100%; width:100%"></canvas>
                        </div>
                      </div>
                    </c:otherwise>
                  </c:choose>
              </c:forEach>
            </c:otherwise>
          </c:choose>
          <!--여기까지-->
        </div>
      </div>
    </div>
  </div>
  <!-- Modal -->
  <div class="modal fade" id="confirmModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
          <h4 class="modal-title" id="myModalLabel">로그아웃 하시겠습니까?</h4>
        </div>
        <div class="modal-footer">
          <a href="javascript:logoutToLogin();" class="btn btn-primary">Yes</a>
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

<!--script src="/js/jquery.min.js"></script-->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
	<script src="/js/bootstrap.min.js"></script>
	<script src="/js/templatemo_script.js"></script>

<!--mqtt js-->
<!-- MQTT 라이브러리 파일들이 존재하지 않으므로 제거됨 -->

<script src="https://cdn.jsdelivr.net/npm/moment@2.29.1/min/moment.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@2.9.4"></script>
<script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-streaming@1.9.0"></script>
<script>
  // 페이지 로드 완료 후 MQTT 연결 시작
  // startConnect() 함수가 자체적으로 UnifiedMQTTManager 로드를 대기하므로
  // 추가 지연 없이 즉시 호출
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
      console.log('DOMContentLoaded 이벤트 발생 - startConnect 호출');
      startConnect(); // 로드 대기 로직이 내장되어 있음
    });
  } else {
    console.log('문서 이미 로드됨 - startConnect 즉시 호출');
    startConnect(); // 로드 대기 로직이 내장되어 있음
  }
  // 페이지 로드 완료 상태 관리
  var __pageReady = false;
  window.addEventListener('load', function(){ __pageReady = true; });

  // 페이지/연결 안정화 대기 후 동기화 (앱 복귀용)
  function waitForReadyThenSync(maxWaitMs) {
    var start = Date.now();
    (function poll(){
      var pageReady = (document.readyState === 'complete');
      var mqttReady = isMqttConnectedSafe();
      if (pageReady && mqttReady) {
        // 통합된 초기 동기화 사용
        if (typeof UnifiedMQTTManager !== 'undefined' && typeof UnifiedMQTTManager.executeInitialSync === 'function') {
          console.log('앱 복귀 시 통합 초기 동기화 실행');
          UnifiedMQTTManager.executeInitialSync();
        }
        return;
      }
      if (Date.now() - start > (maxWaitMs || 6000)) {
        // 타임아웃: MQTT 미연결이어도 초기 동기화 강제 수행
        if (typeof UnifiedMQTTManager !== 'undefined' && typeof UnifiedMQTTManager.executeInitialSync === 'function') {
          console.log('타임아웃 시 통합 초기 동기화 실행');
          UnifiedMQTTManager.executeInitialSync();
        }
        return;
      }
      setTimeout(poll, 200);
    })();
  }
  
  // MQTT 연결 상태 확인 함수
  function isMqttConnectedSafe() {
    try {
      return (typeof client !== 'undefined' && client && typeof client.isConnected === 'function' && client.isConnected());
    } catch(e) { return false; }
  }
  // 아이들(가시성 숨김) 시 에러 체크 일시 정지 / 복귀 시 재개 및 초기 동기화
  document.addEventListener('visibilitychange', function() {
      if (document.hidden) {
        // 백그라운드 진입 즉시: 활성 플래그 내림, 모든 에러체크 타이머/카운터 일시 정지
        if (typeof isPageActive !== 'undefined') { isPageActive = false; }
      if (window.__errorTimers && window.__errorTimers.length) {
        window.__errorTimers.forEach(function(t){ clearInterval(t); });
        window.__errorTimers = [];
      }
        // 배경 진입 시점 기록
        try { window.backgroundStartTime = Date.now(); } catch(e) {}
        // 모든 장치에 에러 체크 유예 부여(앱은 여유 증가)
        var saveY = ($('#saveId').val() || 'N') === 'Y';
        var graceMsBg = saveY ? 40000 : 15000; // 앱은 더 길게 유예
        applyToAllUuids(function(uuid){ pauseError(uuid, graceMsBg); });
    } else {
      if (typeof checkTimeoutAndRedirect === 'function') {
        checkTimeoutAndRedirect();
      }
      // 복귀: 최근 수신 기준 초기화 후 에러 체크 재개
      if (typeof deviceLastDataTime !== 'undefined') {
        deviceLastDataTime = Date.now();
      }
        if (!window.__errorTimers) window.__errorTimers = [];
        // 복귀 즉시: 에러 카운터/상태 리셋 + MQTT 재연결
        // saveId=Y && 백그라운드 30초 초과 시: 첫 ain 수신까지 에러체크 완전 중지(최대 60초 타임아웃)
        var bgDur = 0; try { bgDur = Date.now() - (window.backgroundStartTime || 0); } catch(e) { bgDur = 0; }
        var isApp = ($('#saveId').val() || 'N') === 'Y';
        if (isApp && bgDur > 30000) {
          applyToAllUuids(function(uuid){ pauseError(uuid, 60000); }); // ain 수신 시 즉시 해제, 최대 60초
        } else {
          applyToAllUuids(function(uuid){ pauseError(uuid, 15000); });
        }
        if (typeof startConnect === 'function') { startConnect(); }
        // 복귀 직후 페이지/연결 안정화 대기 후 동기화
        waitForReadyThenSync(6000);
      // 에러 체크 타이머 시작 함수
      function startErrorCheckTimers() {
        if (typeof startInterval === 'function') {
          // 기존 타이머 정리 (중복 방지)
          if (window.__errorTimers && window.__errorTimers.length > 0) {
            console.log('기존 에러 체크 타이머 정리:', window.__errorTimers.length, '개');
            window.__errorTimers.forEach(function(handle) {
              try {
                if (typeof clearInterval === 'function') {
                  clearInterval(handle);
                }
              } catch(e) {
                console.warn('타이머 정리 실패:', e);
              }
            });
            window.__errorTimers = [];
          }
          
          // 에러 타이머 배열 초기화
          if (!window.__errorTimers) {
            window.__errorTimers = [];
          }
          
          // 센서 리스트가 있을 때만 에러 체크 시작
          $('input[id^="sensorUuid"]').each(function(){
            var uuid = $(this).val();
            if (uuid && typeof window['chkError_' + uuid] === 'function') {
              try {
                var handle = startInterval(5, function() { window['chkError_' + uuid](); });
                window.__errorTimers.push(handle);
                console.log('에러 체크 타이머 시작 (5초):', uuid);
              } catch(e) {
                console.error('에러 체크 타이머 시작 실패:', uuid, e);
              }
            }
          });
        }
      }
      
      // 메인은 각 장치별 chkError_uuuu가 있으므로 5초 주기로 재개 (존재 시)
      startErrorCheckTimers();
      // 2초/4초 1회 초기 동기화는 센서설정/차트에 한정, 메인은 생략
    }
  });

    // 페이지 숨김 이벤트 보강: pagehide/blur에서도 즉시 비활성 처리
    window.addEventListener('pagehide', function(){
      if (typeof isPageActive !== 'undefined') { isPageActive = false; }
      if (window.__errorTimers && window.__errorTimers.length) { window.__errorTimers.forEach(function(t){ clearInterval(t); }); window.__errorTimers = []; }
      applyToAllUuids(function(uuid){ pauseError(uuid, 15000); });
    });
    window.addEventListener('blur', function(){
      if (typeof isPageActive !== 'undefined') { isPageActive = false; }
      if (window.__errorTimers && window.__errorTimers.length) { window.__errorTimers.forEach(function(t){ clearInterval(t); }); window.__errorTimers = []; }
      applyToAllUuids(function(uuid){ pauseError(uuid, 15000); });
    });

    // 장치별 에러 체크 일시정지 관리
    var errorPauseUntil = {};
    var resumeGate = {}; // 복귀 후 첫 ain 수신 전까지 에러 무시
    function pauseError(uuid, ms) {
      try {
        errorPauseUntil[uuid] = Date.now() + (ms || 15000);
        // 카운터/상태 리셋
        window['deviceErrorCounters_' + uuid] = 0;
        window['deviceErrorStates_' + uuid] = false;
        resumeGate[uuid] = true; // 복귀 게이트 활성화
      } catch(e) {}
    }
    function applyToAllUuids(callback) {
      try {
        // 간단한 방법: DOM에서 sensorUuid input 찾기
        $('input[id^="sensorUuid"]').each(function(){
          var uuid = $(this).val();
          if (uuid) { 
              callback(uuid); 
          }
        });
      } catch(e) {
        console.error('applyToAllUuids 오류:', e);
      }
    }
</script>

<!-- 센서 리스트가 있을 때만 JavaScript 코드 생성 -->
    <c:choose>
        <c:when test="${sensorList eq null}"></c:when>
        <c:otherwise>
            <c:forEach var="item" items="${sensorList}">
    <script type="text/javascript">
      var chartColors_${item.sensor_uuid} = {
        red: 'rgb(255, 99, 132)',
        orange: 'rgb(255, 159, 64)',
        yellow: 'rgb(255, 205, 86)',
        green: 'rgb(75, 192, 192)',
        blue: 'rgb(54, 162, 235)',
        purple: 'rgb(153, 102, 255)',
        grey: 'rgb(201, 203, 207)'
      };

      var titleStr_${item.sensor_uuid} = "";
      var xval_${item.sensor_uuid} = 0;
      var yval_${item.sensor_uuid} = 0;
      var setp01_${item.sensor_uuid} = 0;
      var outputVal_${item.sensor_uuid} = "";
      var name_${item.sensor_uuid} = "";

      // MQTT 실시간 데이터만 사용하므로 getData AJAX 함수 제거됨

      function chkError_${item.sensor_uuid}() {
        // 실시간 데이터 기반 에러 체크 (기존 chkError 함수와 동일한 로직)
        chkError('${item.sensor_uuid}');
      }
  
      function setSensor_${item.sensor_uuid}() {
        // 토픽 유효성 검사: 와일드카드(+,#) 포함 시 호출 보류 후 재시도
        var currentTopic_${item.sensor_uuid} = $('#topicStr${item.sensor_uuid}').val();
        if (!currentTopic_${item.sensor_uuid} || currentTopic_${item.sensor_uuid}.indexOf('+') >= 0 || currentTopic_${item.sensor_uuid}.indexOf('#') >= 0) {
          window['topicRetry_${item.sensor_uuid}'] = (window['topicRetry_${item.sensor_uuid}'] || 0) + 1;
          if (window['topicRetry_${item.sensor_uuid}'] <= 5) {
            setTimeout(function(){ setSensor_${item.sensor_uuid}(); }, 1000);
          } else {
            console.warn('setSensor - 잘못된 topicStr 감지, 재시도 중단:', currentTopic_${item.sensor_uuid});
          }
          return;
        }

        var sendData2 = {
          userId: $('#userId').val(),
          topicStr: currentTopic_${item.sensor_uuid},
          setGu: "readparam",
          type: "1"  // type=1: parameter 요청
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

              if(result.resultMsg) {
                if(IsJsonString(result.resultMsg)) {
                  var jsonObj = JSON.parse(result.resultMsg);
                  var rcvTopic = result.rcvTopic;
                  ////console.log(rcvTopic);
                  var rcvTopicArr = Array();

                  if(rcvTopic) {
                    rcvTopicArr = rcvTopic.split("/");
                    if(rcvTopicArr[3] == '${item.sensor_uuid}') {
                      setp01_${item.sensor_uuid} = jsonObj.p01;
                    }
                  }
                }
              }

            }
          },
          error: function (reuslt) {

          }
        });
      }

      function getStatus_${item.sensor_uuid}() {
        // 토픽 유효성 검사: 와일드카드(+,#) 포함 시 호출 보류 후 재시도
        var currentTopic_${item.sensor_uuid} = $('#topicStr${item.sensor_uuid}').val();
        if (!currentTopic_${item.sensor_uuid} || currentTopic_${item.sensor_uuid}.indexOf('+') >= 0 || currentTopic_${item.sensor_uuid}.indexOf('#') >= 0) {
          window['topicRetry_${item.sensor_uuid}'] = (window['topicRetry_${item.sensor_uuid}'] || 0) + 1;
          if (window['topicRetry_${item.sensor_uuid}'] <= 5) {
            setTimeout(function(){ getStatus_${item.sensor_uuid}(); }, 1000);
          } else {
            console.warn('getStatus - 잘못된 topicStr 감지, 재시도 중단:', currentTopic_${item.sensor_uuid});
          }
              return;
          }
          
        var sendData2 = {
          userId: $('#userId').val(),
          topicStr: currentTopic_${item.sensor_uuid},
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
            }
          },
          error: function (result) {

          }
        });
      }

      function onRefresh_${item.sensor_uuid}(chart) {
          var now = Date.now();
        var currentValue = 0;

        // MQTT에서 받은 현재 온도 값 사용
        if (typeof yval_${item.sensor_uuid} !== 'undefined' && yval_${item.sensor_uuid} !== null) {
          // Error 상태가 아닐 때만 그래프에 데이터 추가
          if (yval_${item.sensor_uuid} !== 'Error' && yval_${item.sensor_uuid} !== 'error') {
            currentValue = parseFloat(yval_${item.sensor_uuid});
            
            chart.data.datasets.forEach(function(dataset) {
              dataset.data.push({
                x: now,
                y: currentValue
              });
              
              // 데이터 포인트가 너무 많아지면 오래된 데이터 제거 (최대 50개 유지)
              if (dataset.data.length > 50) {
                dataset.data.shift();
              }
            });
          } else {
            // Error 상태일 때는 그래프에 데이터 추가하지 않음
            console.log("그래프 업데이트 스킵 - 장치: ${item.sensor_uuid}, 상태: Error");
          }
        }
      }

      function setSensorId_${item.sensor_uuid}(str) {
        if(str !== "") {
          $('#sensorUuid${item.sensor_uuid}').val(str);
          // 세션 정보를 포함한 페이지 이동
          var url = "/main/main?sensorId=" + str;
          if (window.SessionData && window.SessionData.userId) {
            url += "&userId=" + window.SessionData.userId + "&userGrade=" + window.SessionData.userGrade;
          }
          location.href = url;
        }
      }

      function goSensorSetting_${item.sensor_uuid}() {
        var sensorUuid = $('#sensorUuid${item.sensor_uuid}').val();
        //startDisconnect();

        // 세션 기반 장치설정 페이지로 이동 (URL 파라미터 제거)
        var url = "/admin/sensorSetting?sensorUuid=" + sensorUuid;
        
        console.log('센서 설정 페이지로 이동:', url);
        location.href = url;
      }

      function goChartSetting_${item.sensor_uuid}() {
        var sensorUuid = $('#sensorUuid${item.sensor_uuid}').val();
        //startDisconnect();

        // 세션 기반 차트설정 페이지로 이동 (URL 파라미터 제거)
        var url = "/admin/chartSetting?sensorUuid=" + sensorUuid;
        
        console.log('차트 설정 페이지로 이동:', url);
        location.href = url;
      }

      function goChartData_${item.sensor_uuid}() {
        var sensorUuid = $('#sensorUuid${item.sensor_uuid}').val();
        //startDisconnect();

        // 세션 기반 차트데이터 페이지로 이동 (URL 파라미터 제거)
        var url = "/chart/chart?sensorUuid=" + sensorUuid;
        
        console.log('차트 데이터 페이지로 이동:', url);
        location.href = url;
      }

      var chart_type;
      chart_type = '${item.chart_type}';

      if(chart_type != "none") {
        var color_${item.sensor_uuid} = Chart.helpers.color;
        var config_${item.sensor_uuid} = {
          type: '${item.chart_type}',
          data: {
            datasets: [{
              label: titleStr_${item.sensor_uuid},
              backgroundColor: color_${item.sensor_uuid}(chartColors_${item.sensor_uuid}.red).alpha(0.5).rgbString(),
              borderColor: chartColors_${item.sensor_uuid}.red,
              fill: false,
              lineTension: 0,
              pointRadius: 3,
              data: []
            }
            ]
          },
          options: {
            responsive: false,
            title: {
              display: true,
              text: '${item.chart_type} chart'
            },
            scales: {
              xAxes: [{
                type: 'realtime',
                realtime: {
                  duration: 45000,  // 45초로 조정하여 더 촘촘한 간격
                  refresh: 5000,
                  delay: 0,
                  onRefresh: onRefresh_${item.sensor_uuid}
                },
                ticks: {
                  maxTicksLimit: 15,  // 15개의 틱 표시 (더 촘촘한 간격)
                  maxRotation: 45,    // 라벨 회전 각도
                  minRotation: 0
                }
              }],
              yAxes: [{
                scaleLabel: {
                  display: true,
                  labelString: 'value'
                }
              }]
            },
            tooltips: {
              enabled: true,
              mode: 'nearest',
              intersect: false
            },
            hover: {
              mode: 'nearest',
              intersect: false
            }
          }
        };

        var ctx_${item.sensor_uuid} = document.getElementById('sensorChart${item.sensor_uuid}').getContext('2d');
        window.myChart_${item.sensor_uuid} = new Chart(ctx_${item.sensor_uuid}, config_${item.sensor_uuid});

        $('#sensorChart${item.sensor_uuid}').show();
      }

    </script>
    </c:forEach>
  </c:otherwise>
</c:choose>

    <script>
      // 함수 호이스팅을 위한 함수 선언
      function validateJson(str) {
        try {
          var json = JSON.parse(str);
          return (typeof json === 'object');
        } catch (e) {
          return false;
        }
      }

      function IsJsonString(str) {
        try {
          var json = JSON.parse(str);
          return (typeof json === 'object');
            } catch (e) {
          return false;
        }
      }

      function startInterval(seconds, callback, gu) {
        callback();
        return setInterval(callback, seconds * 1000);
      }

      function sendNoti(sensorVal, gu, uuid, type) {
        // saveid 설정 확인
        var currentSaveId = $('#saveId').val() || 'N';
        
        // saveid가 N인 경우 알림 전송하지 않음
        if (currentSaveId !== 'Y') {
          console.log('saveid N 설정: 알림 전송 건너뜀');
          return;
        }
        
        var userId = $('#userId').val();
        var sensorId = $('#sensorId').val();
        var token = $('#token').val();
        var sensorName = $('#name'+uuid).val();

        var sendData2 = {
          userId: userId,
          sensorId: sensorId,
          sensorUuid: uuid,
          sensorValue: sensorVal,
          token: token,
          name: gu,
          type: type
        }

        console.log('saveid Y 설정: 알림 전송 시작 - ' + gu + ' / ' + uuid);

        $.ajax({
          url: '/main/sendAlarm',
          async: true,
          type: 'POST',
          data: JSON.stringify(sendData2),
          dataType: 'json',
          contentType: 'application/json',
          success: function (result) {
            if (result.resultCode == "200") {
              console.log('알림 전송 성공');
            }
          },
          error: function (result) {
            console.log('알림 전송 실패');
          }
        });
      }

      function goMain() {
        var userId = window.SessionData ? window.SessionData.userId : ($('#loginUserId').val() || $('#userId').val());
        var userGrade = window.SessionData ? window.SessionData.userGrade : $('#userGrade').val();
        
        if (!userId || userId === "" || userId === "null") {
          location.href = "/main/main";
          return;
        }
        
        // 세션 정보를 포함한 메인 페이지로 이동
        var url = "/main/main";
        if (userId && userGrade) {
          url += "?userId=" + userId + "&userGrade=" + userGrade;
        }
        location.href = url;
      }

      // PageNavigation 객체 정의
      window.PageNavigation = {
        goUserList: function() {
          console.log('PageNavigation.goUserList() 호출됨');
          // 세션 기반 사용자 관리 페이지로 이동 (URL 파라미터 제거)
          window.location.href = '/admin/userList';
        },
        goCreateSub: function() {
          console.log('PageNavigation.goCreateSub() 호출됨');
          // 세션 기반 부계정 생성 페이지로 이동 (URL 파라미터 제거)
          window.location.href = '/admin/createSub';
        }
      };

      function goUserList() {
        // PageNavigation 객체 사용
        window.PageNavigation.goUserList();
      }

      function goCreateSub() {
        // PageNavigation 객체 사용
        window.PageNavigation.goCreateSub();
      }

      // 공통 현재온도 처리 함수
      function updateCurrentTemperature(sensorUuid, value, isError) {
        if(isError) {
          $('#sensorVal'+sensorUuid).html('<font size="50px">Error</font>');
          // 에러 상태일 때 yval 변수도 Error로 설정
          window['yval_' + sensorUuid] = 'Error';
        } else {
          $('#sensorVal'+sensorUuid).html('<font size="50px">' + value + '°C</font>');
          // 정상 온도일 때 yval 변수에 실제 값 저장
          window['yval_' + sensorUuid] = value;
          // 데이터 수신 시간 업데이트
          window['deviceLastDataTime_' + sensorUuid] = Date.now();
        }
      }
      
      // 상태표시등 업데이트 함수 (Bootstrap Icons 사용)
      // updateStatusIndicator 함수는 공통 유틸리티에서 제공됨

      // 장치별 에러 체크 함수 (실시간 온도 데이터 기반)
      function chkError(sensor_uuid) {
        // 복귀/백그라운드/연결상태/유예 게이트
        try {
          if (typeof isPageActive !== 'undefined' && isPageActive === false) return;
          if (typeof client !== 'undefined' && client && typeof client.isConnected === 'function' && !client.isConnected()) return;
          if (errorPauseUntil && errorPauseUntil[sensor_uuid] && Date.now() < errorPauseUntil[sensor_uuid]) return;
          if (resumeGate && resumeGate[sensor_uuid] === true) return; // 첫 ain 수신 전까지 무시
          if (typeof navigator !== 'undefined' && navigator && navigator.onLine === false) return;
        } catch(e) {}
        var deviceLastDataTime = window['deviceLastDataTime_' + sensor_uuid] || 0;
        var deviceErrorCounters = window['deviceErrorCounters_' + sensor_uuid] || 0;
        var deviceErrorStates = window['deviceErrorStates_' + sensor_uuid] || false;
        
        var currentTime = Date.now();
        var timeDiff = currentTime - deviceLastDataTime;
        
        // 9초 동안 온도 데이터 미수신 시 에러 체크 (3초 × 3회 = 9초)
        if (timeDiff > 9000 && !deviceErrorStates) {
          deviceErrorCounters++;
          
          // 3번 연속 미수신 시 에러 상태로 변경
          if (deviceErrorCounters >= 3) {
            deviceErrorStates = true;
            window['deviceErrorStates_' + sensor_uuid] = true;
            
            // 상태표시등 업데이트 (Bootstrap Icons 사용)
            updateStatusIndicator('status'+sensor_uuid, 'gray', 'status');
            $('#sensorVal'+sensor_uuid).html('<font size="50px">Error</font>');
            
            // 에러 상태일 때 그래프에도 Error 표시
            if (typeof window['yval_' + sensor_uuid] !== 'undefined') {
              window['yval_' + sensor_uuid] = 'Error';
            }
            
            sendNoti("0", "error", sensor_uuid, "0");

            // 상태 표시등 업데이트
            updateStatusIndicator('error'+sensor_uuid, 'red', 'error');
            updateStatusIndicator('comp'+sensor_uuid, 'gray', 'comp');
            updateStatusIndicator('defr'+sensor_uuid, 'gray', 'defr');
            updateStatusIndicator('fan'+sensor_uuid, 'gray', 'fan');
            
            // 상태 변수 업데이트
            window['deviceErrorStates_' + sensor_uuid] = true;
            window['deviceErrorCounters_' + sensor_uuid] = deviceErrorCounters;
          } else {
            // 카운터만 업데이트
            window['deviceErrorCounters_' + sensor_uuid] = deviceErrorCounters;
          }
        }
      }

      // MQTT 초기화 완료 후 페이지별 처리
      document.addEventListener('mqtt:initialization-complete', function(event) {
        console.log('MQTT 초기화 완료 - 페이지별 초기화 시작');
        
        <c:choose>
            <c:when test="${sensorList eq null}"></c:when>
            <c:otherwise>
                <c:forEach var="item" items="${sensorList}">
                    // 장치별 에러 체크 변수 초기화
                    window['deviceLastDataTime_${item.sensor_uuid}'] = Date.now();
                    window['deviceErrorCounters_${item.sensor_uuid}'] = 0;
                    window['deviceErrorStates_${item.sensor_uuid}'] = false;
                    window['deviceStatusStates_${item.sensor_uuid}'] = 'gray';
                    window['deviceErrorDisplayStates_${item.sensor_uuid}'] = 'gray';
                    window['deviceDinErrorStates_${item.sensor_uuid}'] = false;
                    
                    // 장치별 에러 체크 시작 (3초마다 - 더 빠른 에러 감지를 위해 최적화)
                    // MQTT 데이터는 인터럽트 방식으로 즉시 수신되며, 에러 체크만 주기적으로 수행
                    setTimeout(function() {
                      if (!window.__errorTimers) window.__errorTimers = [];
                      var handle = startInterval(3, function() { chkError('${item.sensor_uuid}'); }, "2");
                      window.__errorTimers.push(handle);
                    }, 2000); // 초기 지연 2초 후 시작 (빠른 초기 에러 감지)

                    var yval_${item.sensor_uuid} = 0;
                </c:forEach>
            </c:otherwise>
        </c:choose>
      });

      // 장치 리스트에서 sensor_id와 sensor_uuid 목록 추출 (페이지 로딩 시 한 번만 생성)
      var allowedSensorIds = [];
      var allowedSensorUuids = [];
      
      // 서버에서 전달된 센서 목록을 먼저 배열로 저장
      var tempSensorIds = [<c:forEach items="${sensorList}" var="item" varStatus="status">'${item.sensor_id}'<c:if test="${!status.last}">,</c:if></c:forEach>];
      var tempSensorUuids = [<c:forEach items="${sensorList}" var="item" varStatus="status">'${item.sensor_uuid}'<c:if test="${!status.last}">,</c:if></c:forEach>];
      
      // 중복 제거 (Set 사용)
      allowedSensorIds = [...new Set(tempSensorIds)];
      allowedSensorUuids = [...new Set(tempSensorUuids)];
      
      console.log('센서 ID 목록 (중복 제거):', allowedSensorIds);
      console.log('센서 UUID 목록 (중복 제거):', allowedSensorUuids);

      function rcvMsg(topic, message) {
        if(topic) {
          var topicArr = new Array();
          topicArr = topic.split("/");
          let uuid = topicArr[3];
          let topicUserId = topicArr[1];  // 토픽의 userId (장치 소유자)

          // 현재 사용자의 장치만 처리 (부계정 사용자 고려)
          var currentUserId = $('#userId').val();  // 현재 로그인 사용자
          
          // 토픽의 userId가 허용된 sensor_id 중 하나이거나 현재 로그인 사용자이면 처리
          if (!allowedSensorIds.includes(topicUserId) && topicUserId !== currentUserId) {
            return;  // 현재 사용자의 장치가 아닌 경우 처리 중단
          }
      
          ////console.log("topic : " + topic);
          ////console.log("message : " + message);

          if(message) {
            if(validateJson(message)) {
              var msg = JSON.parse(message);
              ////console.log("msg : " + msg);

              if(msg.actcode == 'live') {
                // 이미 필터링 완료되었으므로 추가 체크 불필요
                // 센서 정보 입력 처리 제거 - 불필요한 API 호출 방지 (404 에러)
                // 실시간 데이터 수신 시 별도의 센서 정보 저장 불필요
                // (MQTT 메시지 수신만으로 충분)

                if(msg.name == 'ain') {
                  // 현재 온도 알림 - 공통 함수 사용
                  var sensorUuid = topicArr[3];
                  // 데이터 수신 시점에 에러 유예 즉시 해제(소폭 유예 유지 가능)
                  try {
                    if (errorPauseUntil && errorPauseUntil[sensorUuid]) { delete errorPauseUntil[sensorUuid]; }
                    if (resumeGate && resumeGate[sensorUuid]) { resumeGate[sensorUuid] = false; }
                  } catch(e) {}
                  var isError = (msg.value == 'Error');
                  
                  // 공통 현재온도 처리 함수 호출
                  updateCurrentTemperature(sensorUuid, msg.value, isError);
                  
                  if(isError) {
                    // 에러 상태일 때 그래프에도 Error 표시
                    if (typeof window['yval_' + sensorUuid] !== 'undefined') {
                      window['yval_' + sensorUuid] = 'Error';
                    }
                    
                    if(currentUserId == topicUserId) {
                      sendNoti("Error", 'ain', topicArr[3], msg.type);
                    }
      } else {
                    
                    // MQTT에서 정상 온도 데이터 수신 시 에러 상태 해제
                    var sensorUuid = topicArr[3];
                    var deviceErrorStates = window['deviceErrorStates_' + sensorUuid] || false;
                    
                    if (deviceErrorStates) {
                      deviceErrorStates = false;
                      window['deviceErrorStates_' + sensorUuid] = false;
                      window['deviceErrorCounters_' + sensorUuid] = 0;
                      
                      // 에러 해제 후 GET&type=1, GET&type=2 2초 간격으로 한 번만 요청
                      setTimeout(function() {
            // 토픽 유효성 검사 후 호출
            var t = $('#topicStr' + sensorUuid).val();
            if (t && t.indexOf('+') < 0 && t.indexOf('#') < 0 && typeof window['setSensor_' + sensorUuid] === 'function') {
              window['setSensor_' + sensorUuid]();
                        }
                      }, 2000);
                      
                      setTimeout(function() {
            var t = $('#topicStr' + sensorUuid).val();
            if (t && t.indexOf('+') < 0 && t.indexOf('#') < 0 && typeof window['getStatus_' + sensorUuid] === 'function') {
              window['getStatus_' + sensorUuid]();
                        }
                      }, 4000); // 2초 + 2초 = 4초 후
                    }
                    
                    // 정상 온도 데이터 수신 시 상태 업데이트 (Bootstrap Icons 사용)
                    // DIN 이상이 활성 중이면 이상표시는 유지
                    if (!window['deviceDinErrorStates_' + topicArr[3]]) {
                      updateStatusIndicator('error'+topicArr[3], 'gray', 'error');
                    }
                    updateStatusIndicator('status'+topicArr[3], 'green', 'status');
                    
                    // 마지막 데이터 수신 시간 업데이트
                    window['deviceLastDataTime_' + sensorUuid] = Date.now();
                    
                    // 온도 데이터 수신 시점에 바로 에러 체크
                    chkError(sensorUuid);
                    
                    if(currentUserId == topicUserId) {
                      // 온도 알람이 사용 설정된 경우에만 알림 전송
                      // 메인 페이지에서는 알람 설정을 DB에서 확인할 수 없으므로
                      // 서버 측에서만 알람 조건을 체크하도록 함
                      // 클라이언트에서는 무조건 sendNoti 호출 (서버에서 알람 설정 확인)
                      sendNoti(msg.value, 'ain', topicArr[3], msg.type);
                    }
                    
                    // 정상 온도 데이터일 때만 그래프 업데이트
                    if (typeof window['yval_' + sensorUuid] !== 'undefined') {
                      window['yval_' + sensorUuid] = parseFloat(msg.value);
                    }
                  }
                } else if(msg.name == 'din') {
                  // digital input 상태 변화 알림
                  if(msg.type == '1' && msg.ch == '1') {
                    if(msg.value == '1') {
                      // din, type 1, ch1, value 1인 경우: 운전으로 표시하지만 이상으로 표시
                      updateStatusIndicator('status'+topicArr[3], 'green', 'status');
                      window['deviceDinErrorStates_' + topicArr[3]] = true;
                      updateStatusIndicator('error'+topicArr[3], 'red', 'error');
                      if(currentUserId == topicUserId) {
                        sendNoti(msg.value, 'din', topicArr[3], msg.type);
                      }
                    } else {
                      // din, type 1, ch1, value 0인 경우: 이상 해제
                      window['deviceDinErrorStates_' + topicArr[3]] = false;
                      updateStatusIndicator('error'+topicArr[3], 'gray', 'error');
                      if(currentUserId == topicUserId) {
                        sendNoti(msg.value, 'din', topicArr[3], msg.type);
                      }
                    }
                  }
                } else if(msg.name == 'output') {
                  console.log('=== output 메시지 수신 (상태표시등) ===');
                  console.log('UUID:', topicArr[3]);
                  console.log('Type:', msg.type, '(1:comp, 2:def, 3:fan)');
                  console.log('Value:', msg.value, '(1:ON, 0:OFF)');
                  
                  // output 상태 변화 알림
                  if(msg.value == '1') {
                    if(msg.type == '1') {
                      // comp 이상
                      console.log('✅ comp 상태표시등 ON (빨간색)');
                      updateStatusIndicator('comp'+topicArr[3], 'red', 'comp');
                    } else if(msg.type == '2') {
                      // def 이상
                      console.log('✅ defr 상태표시등 ON (빨간색)');
                      updateStatusIndicator('defr'+topicArr[3], 'red', 'defr');
                    } else if(msg.type == '3') {
                      // fan 이상
                      console.log('✅ fan 상태표시등 ON (빨간색)');
                      updateStatusIndicator('fan'+topicArr[3], 'red', 'fan');
                    }
                  } else if(msg.value == '0') {
                    if(msg.type == '1') {
                      console.log('✅ comp 상태표시등 OFF (회색)');
                      updateStatusIndicator('comp'+topicArr[3], 'gray', 'comp');
                    } else if(msg.type == '2') {
                      console.log('✅ defr 상태표시등 OFF (회색)');
                      updateStatusIndicator('defr'+topicArr[3], 'gray', 'defr');
                    } else if(msg.type == '3') {
                      console.log('✅ fan 상태표시등 OFF (회색)');
                      updateStatusIndicator('fan'+topicArr[3], 'gray', 'fan');
                    }
                  }
                }
              } else if(msg.actcode == "setres") {
                console.log('=== setres 메시지 수신 (설정온도) ===');
                console.log('UUID:', topicArr[3]);
                console.log('p01 (원본):', msg.p01);
                
                // 파라미터 디코딩 (음수 처리 지원)
                if(msg.p01 && msg.p01.length > 0) {
                  msg.p01 = decodeTemperature(msg.p01);
                  console.log('p01 (디코딩 후):', msg.p01);
                }
                
                var setTmpElement = $('#setTmp'+topicArr[3]);
                if (setTmpElement.length > 0) {
                  setTmpElement.html(msg.p01 + '°C');
                  console.log('✅ 설정온도 업데이트 완료:', msg.p01 + '°C');
                } else {
                  console.error('❌ setTmp'+topicArr[3]+' 요소를 찾을 수 없음');
                }
                
                // 장치종류 업데이트
                if(msg.p16) {
                  var deviceTypeText = "";
                  if(msg.p16 == "0") {
                    deviceTypeText = "Cooler";
                  } else if(msg.p16 == "1") {
                    deviceTypeText = "Heater";
            } else {
                    deviceTypeText = "Cooler"; // 기본값
                  }
                  $('#deviceType'+topicArr[3]).text('(' + deviceTypeText + ')');
                  
                  // 장치종류를 전역 변수에 저장 (updateStatusIndicator 함수에서 사용)
                  window['deviceType_' + topicArr[3]] = msg.p16;
                  
                  // 장치종류에 따른 상태표시등 텍스트와 아이콘 변경
                  var deviceType = msg.p16 || '0';
                  updateDefrostLabel('defrostLabel'+topicArr[3], deviceType);
                  updateDefrostIndicator('defr'+topicArr[3], 'gray', deviceType);
                }
              } else if(msg.actcode == "actres") {

              }
            }
          }
        }
      }

      // JSP EL 값을 JavaScript 변수로 저장
      var currentUserId = '${userId}';
      var currentUserGrade = '${userGrade}';
      
      // 웹뷰 환경 감지 함수
      function isWebView() {
          return (typeof Android !== 'undefined' && Android.finish) || 
                 (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.closeApp);
      }
      
      // 메인 페이지 뒤로가기 처리 설정
      function setupMainPageBackNavigation() {
                    // 히스토리 관리는 DOM 로드 완료 후 지연 실행 (성능 최적화)
          setTimeout(function() {
              // 로그인 후 첫 진입인지 확인 (더 정확한 로직)
              var isFirstLogin = false;
              
              // 세션 스토리지를 사용하여 첫 진입 여부 확인
              var firstVisit = sessionStorage.getItem('mainPageFirstVisit');
              if (firstVisit === null || firstVisit === 'true') {
                  isFirstLogin = true;
                  sessionStorage.setItem('mainPageFirstVisit', 'false');
              }
              
              console.log('=== 히스토리 관리 디버깅 ===');
              console.log('isFirstLogin:', isFirstLogin);
              console.log('firstVisit:', firstVisit);
              console.log('history.length:', history.length);
              console.log('현재 URL:', window.location.href);
              
              // 히스토리 정리 (history.go() 제거하고 replaceState만 사용)
              if (!isFirstLogin) {
                  console.log('히스토리 정리 실행 - replaceState 사용');
                  // 히스토리를 메인 페이지만 남기고 정리
                  history.replaceState({page: 'main'}, '메인', '/main/main');
                  
                  // 추가로 강제 히스토리 정리 실행
                  setTimeout(function() {
                      forceCleanHistory();
                  }, 50);
              } else {
                  console.log('히스토리 정리 건너뛰기 - 첫 진입');
              }
              console.log('히스토리 상태 설정 완료');
          }, 200); // 200ms 지연으로 페이지 로딩 우선 (시간 증가)
          
          // 뒤로가기 시도 시 처리
          window.addEventListener('popstate', function(event) {
              // 웹뷰 환경 확인 후 처리
              if (isWebView()) {
                  if (typeof Android !== 'undefined' && Android.finish) {
                      // Android 앱 종료
                      Android.finish();
                  } else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.closeApp) {
                      // iOS 앱 종료
                      window.webkit.messageHandlers.closeApp.postMessage('close');
                  }
              } else {
                  // 일반 브라우저에서는 페이지 새로고침으로 실시간 데이터 초기화
                  window.location.reload(true);
              }
          });
          
          // 키보드 뒤로가기 단축키 처리
          document.addEventListener('keydown', function(event) {
              if (event.altKey && event.keyCode === 37) { // Alt + Left Arrow
                  event.preventDefault();
                  event.stopPropagation();
                  
                  if (isWebView()) {
                      if (typeof Android !== 'undefined' && Android.finish) {
                          Android.finish();
                      } else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.closeApp) {
                          window.webkit.messageHandlers.closeApp.postMessage('close');
                      }
            } else {
                      window.location.reload(true);
                  }
                  return false;
              }
          });
          
          // 마우스 뒤로가기 버튼 처리
          document.addEventListener('mousedown', function(event) {
              if (event.button === 3 || event.button === 4) { // 뒤로가기/앞으로가기 버튼
                  event.preventDefault();
                  event.stopPropagation();
                  
                  if (isWebView()) {
                      if (typeof Android !== 'undefined' && Android.finish) {
                          Android.finish();
                      } else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.closeApp) {
                          window.webkit.messageHandlers.closeApp.postMessage('close');
                      }
          } else {
                      window.location.reload(true);
                  }
                  return false;
              }
          });
      }
      
      // 메인 페이지 뒤로가기 처리 설정 실행 (모든 리소스 로드 완료 후)
      window.addEventListener('load', function() {
          console.log('=== 페이지 완전 로드 완료 ===');
          setupMainPageBackNavigation();
          
          // 페이지 로드 시 에러 체크 타이머 시작 (새로고침이나 다른 페이지에서 돌아올 때)
          startErrorCheckTimers();
      });
      
      // 페이지 이동 시에는 MQTT 연결 유지 (로그아웃 시에만 해제)
      window.addEventListener('beforeunload', function() {
          console.log('=== 페이지 이동 - MQTT 연결 유지 ===');
          // 페이지 이동 시에는 MQTT 연결을 끊지 않음
          // 로그아웃 시에만 disconnectOnLogout() 호출
      });
      
      // 페이지 표시 시 에러 체크 타이머 시작 (HBEE 배너 클릭, 뒤로가기 등)
      window.addEventListener('pageshow', function(event) {
          console.log('=== 페이지 표시 이벤트 ===', event.persisted ? '(캐시에서 복원)' : '(새로 로드)');
          // 페이지가 표시될 때마다 에러 체크 타이머 시작
          startErrorCheckTimers();
      });
      
      // Main 버튼 클릭으로 이동한 경우를 위한 추가 체크
      window.addEventListener('DOMContentLoaded', function() {
          // Main 버튼 클릭으로 이동한 경우 플래그 확인
          if (sessionStorage.getItem('mainButtonClicked') === 'true') {
              console.log('=== Main 버튼 클릭으로 이동 감지 ===');
              sessionStorage.removeItem('mainButtonClicked'); // 플래그 제거
              // 약간의 지연 후 에러 체크 시작 (페이지 로딩 완료 대기)
              setTimeout(function() {
                  startErrorCheckTimers();
              }, 1000);
          }
      });
      
      // 다른 페이지로 이동 시 히스토리 정리 (필요시 사용)
      function navigateToPage(url) {
          // 현재 히스토리 상태를 메인으로 정리
          history.replaceState({page: 'main'}, '메인', '/main/main');
          // 새 페이지로 이동
          window.location.href = url;
      }
      
      // 강력한 히스토리 정리 함수
      function forceCleanHistory() {
          console.log('=== 강제 히스토리 정리 시작 ===');
          console.log('현재 히스토리 길이:', history.length);
          
          // 히스토리를 메인 페이지만 남기고 완전히 정리
          if (history.length > 1) {
              // 모든 히스토리 엔트리를 메인으로 교체
              for (var i = 0; i < history.length; i++) {
                  history.replaceState({page: 'main'}, '메인', '/main/main');
              }
              console.log('히스토리 강제 정리 완료');
          }
      }
      
      // 로그아웃 시 세션 스토리지 정리
      function clearMainPageSession() {
          sessionStorage.removeItem('mainPageFirstVisit');
      }
      
      // 페이지 이탈 시 강제 로그아웃을 수행하지 않습니다.
      // 내부 네비게이션 중 세션이 끊기는 문제를 방지하기 위함입니다.
      
      // 페이지 포커스 감지 (사용자가 다른 탭으로 이동하는 경우)
      var isPageActive = true;
      var lastActivityTime = Date.now();
      var backgroundStartTime = null;
      var currentSaveId = $('#saveId').val() || 'N'; // saveid 설정 확인
      
      console.log('현재 saveid 설정: ' + currentSaveId);
      
      document.addEventListener('visibilitychange', function() {
          if (document.hidden) {
              // 백그라운드 진입
              isPageActive = false;
              backgroundStartTime = Date.now();
              console.log('페이지 비활성화됨 - MQTT 연결 유지');
          } else {
              // 페이지 복귀
              isPageActive = true;
              lastActivityTime = Date.now();
              console.log('페이지 활성화됨');
              
              // 연결 상태 확인 (연결 끊어진 경우에만 재연결)
              if (typeof UnifiedMQTTManager !== 'undefined') {
                  if (!UnifiedMQTTManager.isConnected()) {
                      console.log('MQTT 연결 끊김 감지 - 재연결 시도');
                      UnifiedMQTTManager.connect();
                  } else {
                      console.log('MQTT 연결 상태 정상');
                  }
              }
              
              // 백그라운드 시간 초기화
              backgroundStartTime = null;
          }
      });
      
      // 앱 포커스 관련 커스텀 로직 제거 (기본 동작만 유지)
      
      // 앱 인터페이스 함수 (앱에서 호출)
      window.appResume = function() {
          console.log('앱에서 resume 이벤트 호출됨');
          if (currentSaveId === 'Y') {
              console.log('saveid Y 설정: 앱 resume 시 MQTT 연결 복구');
              
              // 즉시 MQTT 연결 상태 확인 및 복구
              setTimeout(function() {
                  if (typeof client !== 'undefined') {
                      if (!client.isConnected()) {
                          console.log('앱 resume 시 MQTT 연결 끊어짐 - 즉시 재연결');
                          if (typeof startConnect === 'function') {
                              startConnect();
                          }
                      } else {
                          console.log('앱 resume 시 MQTT 연결 상태 정상');
                      }
                  } else {
                      console.log('앱 resume 시 MQTT 클라이언트 미정의 - 연결 시도');
                      if (typeof startConnect === 'function') {
                          startConnect();
                      }
                  }
              }, 500); // 0.5초 후 확인
          }
      };
      
      // 주기적으로 페이지 활성 상태 확인 (saveid 설정에 따라 다르게 처리)
      setInterval(function() {
          if (!isPageActive) {
              var backgroundDuration = Date.now() - (backgroundStartTime || Date.now());
              
              if (currentSaveId === 'Y') {
                  // saveid가 Y인 경우: 30분 이상 비활성 상태일 때만 로그아웃 처리
                  if (backgroundDuration > 1800000) { // 30분
                      console.log('saveid Y 설정: 60분 이상 비활성 상태로 인한 로그아웃 처리');
                      performLogout();
                  }
              } else {
                  // saveid가 N인 경우: 기존 로직 (5분)
                  if (backgroundDuration > 300000) { // 5분
                      console.log('saveid N 설정: 5분 이상 비활성 상태로 인한 로그아웃 처리');
                      performLogout();
                  }
              }
          }
      }, currentSaveId === 'Y' ? 1800000 : 300000); // saveid Y: 30분, saveid N: 5분
      
      // 로그아웃 처리 함수
      function performLogout() {
          console.log('=== 로그아웃 처리 시작 ===');
          console.log('로그아웃 시간: ' + new Date().toLocaleString());
          
          // MQTT 연결 해제 추가
          if (typeof UnifiedMQTTManager !== 'undefined' && 
              typeof UnifiedMQTTManager.disconnectOnLogout === 'function') {
              console.log('로그아웃으로 인한 MQTT 연결 해제');
              UnifiedMQTTManager.disconnectOnLogout();
          }
          
          // 기존 MQTT 연결 종료 (로그아웃 시에만)
          if (typeof logoutMqttDisconnect === 'function') {
              console.log('MQTT 연결 종료 함수 호출');
              logoutMqttDisconnect();
      } else {
              console.log('MQTT 연결 종료 함수가 정의되지 않음');
          }
          
          var xhr = new XMLHttpRequest();
          xhr.open('POST', '/login/logoutProcess', false);
          xhr.setRequestHeader('Content-Type', 'application/json');
          
          var logoutData = {
              userId: currentUserId
          };
          
          try {
              xhr.send(JSON.stringify(logoutData));
              console.log('페이지 비활성 상태로 인한 로그아웃 처리 완료');
              // 앱/브라우저 공통: 로그인 페이지로 이동
              window.location.href = '/login/login';
          } catch(e) {
              console.log('비활성 상태 로그아웃 처리 실패:', e);
          }
      }

      // 로그아웃 준비 함수
      function prepareLogout() {
        $('#confirmModal').modal('show');
      }
      
      // 사용자 클릭 로그아웃 시: 로그인 페이지로 이동
      function logoutToLogin() {
        // MQTT 연결 해제
        if (typeof UnifiedMQTTManager !== 'undefined' && 
            typeof UnifiedMQTTManager.disconnectOnLogout === 'function') {
            UnifiedMQTTManager.disconnectOnLogout();
        }
        
        var currentUserId = $('#loginUserId').val() || $('#userId').val() || window.currentUserId || '';
        
        $.ajax({
          url: '/login/logoutProcess',
          type: 'POST',
          async: true,
          data: JSON.stringify({ userId: currentUserId }),
          contentType: 'application/json',
          success: function(response) {
            window.location.href = '/login/login';
          },
          error: function(xhr, status, error) {
            // 에러 발생 시에도 로그인 페이지로 이동
            window.location.href = '/login/login';
          }
        });
      }
      
      // MQTT 초기 동기화 강제 실행 함수 (강화된 중복 방지)
      function forceInitialSync() {
        console.log('=== MQTT 초기 동기화 강제 실행 ===');
        
        // 통합된 초기 동기화 사용
        if (typeof UnifiedMQTTManager !== 'undefined' && typeof UnifiedMQTTManager.executeInitialSync === 'function') {
          console.log('통합 초기 동기화 실행');
          
          // 중복 방지 상태 확인
          var duplicateState = UnifiedMQTTManager.getDuplicatePreventionState();
          console.log('강제 실행 전 중복 방지 상태:', duplicateState);
          
          // 강제 실행이므로 중복 방지 상태 리셋
          UnifiedMQTTManager.resetDuplicatePreventionState();
          console.log('강제 실행을 위해 중복 방지 상태 리셋');
          
          UnifiedMQTTManager.executeInitialSync();
        } else {
          console.warn('UnifiedMQTTManager를 찾을 수 없음 - 기존 방식으로 실행');
          
          // 기존 방식 (fallback)
          applyToAllUuids(function(uuid) {
            console.log('장치 ' + uuid + ' 초기 동기화 시작');
            
            // 1. 설정값 요청 (GET&type=1)
            if (typeof window['setSensor_' + uuid] === 'function') {
              console.log('setSensor_' + uuid + ' 함수 호출');
              window['setSensor_' + uuid]();
            } else {
              console.warn('setSensor_' + uuid + ' 함수가 정의되지 않음');
            }
            
            // 2. 상태정보 요청 (GET&type=2) - 2초 후 실행
            setTimeout(function() {
              if (typeof window['getStatus_' + uuid] === 'function') {
                console.log('getStatus_' + uuid + ' 함수 호출');
                window['getStatus_' + uuid]();
              } else {
                console.warn('getStatus_' + uuid + ' 함수가 정의되지 않음');
              }
            }, 2000);
          });
        }
        
        console.log('MQTT 초기 동기화 강제 실행 완료');
      }
      
      // 중복 방지 설정 관리 함수들
      function configureDuplicatePrevention(settings) {
        if (typeof UnifiedMQTTManager !== 'undefined' && typeof UnifiedMQTTManager.updateDuplicatePreventionSettings === 'function') {
          console.log('중복 방지 설정 업데이트:', settings);
          UnifiedMQTTManager.updateDuplicatePreventionSettings(settings);
        } else {
          console.warn('UnifiedMQTTManager를 찾을 수 없음 - 설정 업데이트 불가');
        }
      }
      
      function getDuplicatePreventionStatus() {
        if (typeof UnifiedMQTTManager !== 'undefined' && typeof UnifiedMQTTManager.getDuplicatePreventionState === 'function') {
          var state = UnifiedMQTTManager.getDuplicatePreventionState();
          console.log('중복 방지 상태:', state);
          return state;
        } else {
          console.warn('UnifiedMQTTManager를 찾을 수 없음 - 상태 조회 불가');
          return null;
        }
      }
      
      function resetDuplicatePrevention() {
        if (typeof UnifiedMQTTManager !== 'undefined' && typeof UnifiedMQTTManager.resetDuplicatePreventionState === 'function') {
          console.log('중복 방지 상태 리셋');
          UnifiedMQTTManager.resetDuplicatePreventionState();
        } else {
          console.warn('UnifiedMQTTManager를 찾을 수 없음 - 상태 리셋 불가');
        }
      }
      
      function addPageToBlacklist(pageType, reason) {
        if (typeof UnifiedMQTTManager !== 'undefined' && typeof UnifiedMQTTManager.addPageToBlacklist === 'function') {
          console.log('페이지 블랙리스트 추가:', pageType, '사유:', reason);
          UnifiedMQTTManager.addPageToBlacklist(pageType, reason);
        } else {
          console.warn('UnifiedMQTTManager를 찾을 수 없음 - 블랙리스트 추가 불가');
        }
      }
      
      function removePageFromBlacklist(pageType) {
        if (typeof UnifiedMQTTManager !== 'undefined' && typeof UnifiedMQTTManager.removePageFromBlacklist === 'function') {
          console.log('페이지 블랙리스트 제거:', pageType);
          UnifiedMQTTManager.removePageFromBlacklist(pageType);
        } else {
          console.warn('UnifiedMQTTManager를 찾을 수 없음 - 블랙리스트 제거 불가');
        }
      }
      
      // 전역 함수로 등록
      window.configureDuplicatePrevention = configureDuplicatePrevention;
      window.getDuplicatePreventionStatus = getDuplicatePreventionStatus;
      window.resetDuplicatePrevention = resetDuplicatePrevention;
      window.addPageToBlacklist = addPageToBlacklist;
      window.removePageFromBlacklist = removePageFromBlacklist;

      // MQTT 상태 확인 함수
      function checkMqttStatus() {
        console.log('=== MQTT 상태 확인 ===');
        
        // MQTT 클라이언트 상태 확인
        if (typeof client !== 'undefined') {
          console.log('MQTT 클라이언트 상태:', client.isConnected() ? '연결됨' : '연결 안됨');
          console.log('MQTT 클라이언트 ID:', client.clientId);
            } else {
          console.log('MQTT 클라이언트가 정의되지 않음');
        }
        
        // 토픽 정보 확인
        applyToAllUuids(function(uuid) {
          var topicStr = $('#topicStr' + uuid).val();
          console.log('장치 ' + uuid + ' 토픽:', topicStr);
        });
        
        // 세션 정보 확인
        console.log('현재 사용자 ID:', $('#userId').val());
        console.log('현재 센서 ID:', $('#sensorId').val());
      }

      // p16에 따른 제상/히터 라벨 업데이트 함수
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

      // p16에 따른 제상/히터 아이콘 업데이트 함수
      function updateDefrostIndicator(elementId, status, deviceType) {
        var element = document.getElementById(elementId);
        if (element) {
          var defrostIcon = element.querySelector('.defrost-icon');
          var heaterIcon = element.querySelector('.heater-icon');
          
          if (deviceType === '1') {
            // 히터 모드
            if (defrostIcon) defrostIcon.style.display = 'none';
            if (heaterIcon) heaterIcon.style.display = 'inline';
          } else {
            // 제상 모드
            if (defrostIcon) defrostIcon.style.display = 'inline';
            if (heaterIcon) heaterIcon.style.display = 'none';
          }
          
          // 상태에 따른 클래스 업데이트
          element.className = 'status-indicator ' + status;
        }
      }

      // 전역 함수로 등록
      window.updateDefrostLabel = updateDefrostLabel;
      window.updateDefrostIndicator = updateDefrostIndicator;
    </script>
      </body>
  </html>