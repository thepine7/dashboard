<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>

<html lang="ko" class="">
<head>
    <meta charset="UTF-8">
    <!--[if IE]><meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1"><![endif]-->
    <title>H&T Technology</title>
    	<link rel="icon" href="/images/hntbi.png" type="image/png">
    <meta name="keywords" content="" />
    <meta name="description" content="" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <link rel="stylesheet" href="/css/templatemo_main.css">
    <link rel="stylesheet" href="/css/responsive-common.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" media="print" onload="this.media='all'">
    
    <!-- 앱 레이아웃 개선 스타일 -->
    <style>
        /* 상태표시등 스타일 */
        .status-indicator {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 25px;
            height: 25px;
            border-radius: 50%;
            margin: 2px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.2);
        }
        .status-indicator i {
            font-size: 16px;
            color: white;
        }
        .status-indicator.green { 
            background: #4CAF50; 
            box-shadow: 0 0 10px rgba(76, 175, 80, 0.3); 
        }
        .status-indicator.gray { 
            background: #9e9e9e; 
        }
        .status-indicator.red { 
            background: #f44336; 
            box-shadow: 0 0 10px rgba(244, 67, 54, 0.3); 
        }
        .status-indicator.active { 
            animation: pulse 2s infinite; 
        }
        
        @keyframes pulse {
            0% { transform: scale(1); }
            50% { transform: scale(1.1); }
            100% { transform: scale(1); }
        }
        
        /* 모바일에서 현재온도 폰트 크기만 조정 */
        @media (max-width: 768px) {
            #sensorVal {
                font-size: 18px !important;
                line-height: 1.2 !important;
            }
            
            /* 레이아웃 개선 (색상 변경 없음) */
            .templatemo-content-wrapper {
                margin-left: 0 !important;
                padding: 10px !important;
            }
            
            .templatemo-sidebar {
                display: none !important;
            }
            
            .col-md-6, .col-sm-6 {
                width: 100% !important;
                float: none !important;
                margin-bottom: 15px !important;
            }
            
            .panel-body {
                padding: 10px !important;
            }
            
            .table td {
                padding: 8px 4px !important;
                font-size: 12px !important;
            }
            
            .table thead td {
                font-size: 11px !important;
                padding: 6px 4px !important;
            }
            
            .btn {
                font-size: 11px !important;
                padding: 6px 8px !important;
                margin: 2px !important;
            }
            
            input[type="date"] {
                font-size: 11px !important;
                padding: 4px 6px !important;
            }
            
            /* 차트 컨테이너 높이 조정 */
            .panel-body[style*="height: 320px"] {
                height: 280px !important;
            }
        }
        
        /* 강제 적용을 위한 추가 스타일 */
        @media screen and (max-width: 768px) {
            .templatemo-content-wrapper {
                margin-left: 0 !important;
                padding: 10px !important;
            }
            
            .templatemo-sidebar {
                display: none !important;
            }
            
            .col-md-6, .col-sm-6 {
                width: 100% !important;
                float: none !important;
                margin-bottom: 15px !important;
            }
        }
    </style>
    
    <!-- PageNavigation 객체 및 공통 함수 정의 -->
    <script>
        window.PageNavigation = {
            goMain: function() {
                console.log('PageNavigation.goMain() 호출됨');
                // 세션 기반 메인 페이지로 이동 (URL 파라미터 제거)
                window.location.href = '/main/main';
            },
            goLogin: function() {
                console.log('PageNavigation.goLogin() 호출됨');
                window.location.href = '/login/login';
            },
            goUserList: function() {
                console.log('PageNavigation.goUserList() 호출됨');
                // 세션 기반 사용자 목록 페이지로 이동 (URL 파라미터 제거)
                window.location.href = '/admin/userList';
            },
            goCreateSub: function() {
                console.log('PageNavigation.goCreateSub() 호출됨');
                // 세션 기반 부계정 생성 페이지로 이동 (URL 파라미터 제거)
                window.location.href = '/admin/createSub';
            },
            goChartSetting: function(uuid) {
                console.log('PageNavigation.goChartSetting() 호출됨', uuid);
                // 세션 기반 차트 설정 페이지로 이동 (URL 파라미터 제거)
                var url = '/admin/chartSetting';
                if (uuid) {
                    url += "?sensorUuid=" + uuid;
                }
                window.location.href = url;
            },
            goChartData: function(uuid) {
                console.log('PageNavigation.goChartData() 호출됨', uuid);
                // 세션 기반 차트 데이터 페이지로 이동 (URL 파라미터 제거)
                var url = '/chart/chart';
                if (uuid) {
                    url += "?sensorUuid=" + uuid;
                }
                window.location.href = url;
            },
            goUserDetail: function(userId) {
                console.log('PageNavigation.goUserDetail() 호출됨', userId);
                // 세션 기반 사용자 상세 페이지로 이동 (URL 파라미터 제거)
                var url = '/admin/userDetail';
                if (userId) {
                    url += "?dtlUser=" + userId;
                }
                window.location.href = url;
            },
            goUserModify: function(userId) {
                console.log('PageNavigation.goUserModify() 호출됨', userId);
                // 세션 기반 사용자 수정 페이지로 이동 (URL 파라미터 제거)
                var url = '/admin/userModify';
                if (userId) {
                    url += "?dtlUser=" + userId + "&gu=m";
                }
                window.location.href = url;
            },
            goBack: function() {
                console.log('PageNavigation.goBack() 호출됨');
                window.history.back();
            }
        };
        
        // 로그아웃 함수 (main.jsp와 동일한 로직)
        function logoutToLogin() {
            console.log('logoutToLogin() 호출됨');
            // jQuery가 로드된 후에 실행되도록 확인
            if (typeof $ === 'undefined') {
                console.warn('jQuery가 로드되지 않음 - 로그인 페이지로 이동');
                window.location.href = '/login/login';
                return;
            }
            
            var currentUserId = window.SessionData ? window.SessionData.userId : $('#userId').val();
            if (!currentUserId) {
                console.warn('사용자 ID가 없어 로그아웃 처리 건너뜀');
                window.location.href = '/login/login';
                return;
            }
            
            $.ajax({
                url: '/login/logoutProcess',
                type: 'POST',
                data: JSON.stringify({
                    userId: currentUserId
                }),
                contentType: 'application/json',
                success: function(response) {
                    console.log('로그아웃 성공:', response);
                    // 세션 데이터 초기화
                    if (window.SessionData) {
                        window.SessionData = {};
                    }
                    window.location.href = '/login/login';
                },
                error: function(xhr, status, error) {
                    console.error('로그아웃 실패:', error);
                    // 에러가 발생해도 로그인 페이지로 이동
                    if (window.SessionData) {
                        window.SessionData = {};
                    }
                    window.location.href = '/login/login';
                }
            });
        }
    </script>
    
    <!-- 공통 세션 정보 템플릿 -->
    <jsp:include page="/WEB-INF/jsp/common/session-info.jsp" />
</head>
<body>
<!-- jQuery 먼저 로딩 (의존성 해결) -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>

<!-- 디버깅용 출력 -->
<!-- 모델 sensorId 값: ${model.sensorId} -->
<!-- 세션 sensorId 값: ${sessionScope.sensorId} -->
<!-- 요청 sensorId 값: ${param.sensorId} -->
<input type="hidden" id="sensorId" name="sensorId" value="${sensorId}" />
<input type="hidden" id="sensorUuid" name="sensorUuid" value="${sensorUuid}" />
<input type="hidden" id="sensorName" name="sensorName" value="${sensorName}" />
<input type="hidden" id="topicStr" name="topicStr" value="${topicStr}" />
<input type="hidden" id="dailyList" name="dailyList" value="${dailyList}" />
<input type="hidden" id="monthlyList" name="monthlyList" value="${monthlyList}" />
<input type="hidden" id="yearlyList" name="yearlyList" value="${yearlyList}" />
<input type="hidden" id="daily" name="daily" value="${daily}" />
<input type="hidden" id="monthly" name="monthly" value="${monthly}" />
<input type="hidden" id="yearly" name="yearly" value="${yearly}" />

<div id="main-wrapper">
    <div class="navbar navbar-inverse" role="navigation" style="background-color: #ffffff">
        <div class="navbar-header">
            		<div class="logo"><h1><a href="javascript:PageNavigation.goMain();"><img src="/images/hntbi.png" width="70" height="32"></a></h1></div>
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
                <li class="active"><a href="javascript:PageNavigation.goMain();"><i class="fa fa-home"></i>Dashboard</a></li>
                <li class="sub open">
                    <ul style="background-color: #afd9ee; height: 40px; padding-top: 10px">
                        <strong>${userNm}님 안녕하세요.</strong>
                    </ul>
                    <c:if test="${userGrade eq 'A' || userGrade eq 'U'}">
                        <a href="javascript:;">
                            <i class="fa fa-database"></i> 관리 메뉴보기 <div class="pull-right"><span class="caret"></span></div>
                        </a>
                        <ul class="templatemo-submenu">
                            <li><a href="javascript:PageNavigation.goUserList();">사용자 관리</a></li>
                        </ul>
                        <ul class="templatemo-submenu">
                            <li><a href="javascript:PageNavigation.goCreateSub();">부계정 생성</a></li>
                        </ul>
                    </c:if>
                </li>
                <li><a href="" data-toggle="modal" data-target="#confirmModal"><i class="fa fa-sign-out"></i>로그아웃</a></li>
            </ul>
        </div><!--/.navbar-collapse -->

        <div class="templatemo-content-wrapper" style="background-color: #333333">
            <div class="templatemo-content" style="background-color: #333333">
                <ol class="breadcrumb">
                    <li><a href="javascript:PageNavigation.goMain();">Main</a></li>
                </ol>
                <h1><span style="color: #f0f8ff; ">챠트 데이터</span></h1>
                <p><span style="color: #f0f8ff; ">챠트 데이터 조회 화면입니다.</span></p>

                <div class="templatemo-charts">
                    <div class="row">
                                                 <div class="col-md-6 col-sm-6 margin-bottom-30">
                             <div class="panel panel-primary">
                                 <div class="panel-heading">${sensorName}</div>
                                 <div class="panel-body">
                                     <!-- 온도 정보 테이블 -->
                                     <table class="table table-striped" style="width: 100%; table-layout: fixed; margin-bottom: 10px;">
                                         <thead>
                                             <tr>
                                                 <td align="center" style="background-color: #c7254e; width: 30%;"><strong><span style="color: #f0f8ff; ">구분</span></strong></td>
                                                 <td align="center" style="background-color: #c7254e; width: 70%;"><strong><span style="color: #f0f8ff; ">온도</span></strong></td>
                                             </tr>
                                         </thead>
                                         <tbody>
                                             <tr>
                                                 <td align="center" style="width: 30%;">설정온도</td>
                                                 <td align="center" style="width: 70%;">
                                                     <strong><span align="center" id="setTmp" name="setTmp" style="color: #4cae4c"></span></strong>
                                                 </td>
                                             </tr>
                                             <tr>
                                                 <td align="center" valign="middle" style="width: 30%;">현재온도</td>
                                                 <td align="center" style="width: 70%;">
                                                     <strong><span align="center" id="sensorVal" name="sensorVal" style="color: #c7254e"></span></strong>
                                                 </td>
                                             </tr>
                                         </tbody>
                                     </table>
                                     
                                     <!-- 상태표시등 테이블 -->
                                     <table class="table table-striped" style="width: 100%; table-layout: fixed; margin-bottom: 10px;">
                                         <tbody>
                                             <tr>
                                                 <td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">운전</span></strong></td>
                                                 <td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">콤프</span></strong></td>
                                                 <td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span id="defrostLabelChart" style="color: #f0f8ff; font-size:10pt;">제상</span></strong></td>
                                                 <td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">FAN</span></strong></td>
                                                 <td width="20%" align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;">이상</span></strong></td>
                                             </tr>
                                             <tr>
                                                 <td align="center"><div id="statusChart" class="status-indicator green"><i class="bi bi-play-circle-fill"></i></div></td>
                                                 <td align="center"><div id="compChart" class="status-indicator gray"><i class="bi bi-gear-fill"></i></div></td>
                                                 <td align="center"><div id="defrChart" class="status-indicator gray"><i class="bi bi-snow"></i></div></td>
                                                 <td align="center"><div id="fanChart" class="status-indicator gray"><i class="bi bi-fan"></i></div></td>
                                                 <td align="center"><div id="errorChart" class="status-indicator gray"><i class="bi bi-exclamation-triangle-fill"></i></div></td>
                                             </tr>
                                         </tbody>
                                     </table>
                                     
                                     <!-- 날짜 선택 테이블 -->
                                     <table class="table table-striped" style="width: 100%; table-layout: fixed;">
                                         <thead>
                                             <tr>
                                                 <td colspan="5" align="center" style="background-color: #c7254e;">
                                                     <strong><span style="color: #f0f8ff; font-size:10pt;">날짜</span></strong>
                                                 </td>
                                             </tr>
                                         </thead>
                                         <tbody>
                                             <tr>
                                                 <td colspan="5" align="center" style="width: 100%;">
                                                     <div style="display: flex; align-items: center; justify-content: center; gap: 5px;">
                                                         <button type="button" class="btn btn-sm btn-default" id="prevDay" title="이전 날짜" style="padding: 2px 6px; max-width: 100%;">
                                                             <i class="bi bi-chevron-left"></i>
                                                         </button>
                                                         <input type="date" id="startDate" name="startDate" value="${todayStr}" style="width: 110px; height: 22px; text-align: center; max-width: 100%;"> ~ <input type="date" id="endDate" name="endDate" value="${todayStr}" style="width: 110px; height: 22px; text-align: center; max-width: 100%;">
                                                         <button type="button" class="btn btn-sm btn-default" id="nextDay" title="다음 날짜" style="padding: 2px 6px; max-width: 100%;">
                                                             <i class="bi bi-chevron-right"></i>
                                                         </button>
                                                     </div>
                                                 </td>
                                             </tr>
                                             <tr>
                                                 <td colspan="5" align="center" style="width: 100%;">
                                                     <button type="button" class="btn btn-default" id="1mon" name="1mon" style="max-width: 100%; margin: 2px;">1개월</button>
                                                     <button type="button" class="btn btn-default" id="3mon" name="3mon" style="max-width: 100%; margin: 2px;">3개월</button>
                                                     <button type="button" class="btn btn-default" id="6mon" name="6mon" style="max-width: 100%; margin: 2px;">6개월</button>
                                                     <button type="button" class="btn btn-default" id="12mon" name="12mon" style="max-width: 100%; margin: 2px;">1년</button>
                                                 </td>
                                             </tr>
                                             <tr>
                                                 <td colspan="5" align="center" style="width: 100%;"><button type="button" class="btn btn-default" id="excelDownload" name="excelDownload" style="max-width: 100%;">다운로드</button></td>
                                             </tr>
                                         </tbody>
                                     </table>
                                 </div>
                             </div>
                         </div>
                                                                                                     <div class="col-md-6 col-sm-6 margin-bottom-30">
                               <div class="panel panel-primary">
                                   <div class="panel-heading">장치 챠트</div>
                                                                       <div class="panel-body" style="height: 320px;">
                                        <div style="position: relative; height: 100%; width: 100%;">
                                            <canvas id="dailyChart" style="max-height: 100%; max-width: 100%;"></canvas>
                                            <div id="chartMessage" style="position: absolute; top: 10px; left: 10px; display: none;"></div>
                                        </div>
                                    </div>
                               </div>
                           </div>
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

</div>


<!-- 세션 관리자 -->
<script src="/js/session-manager.js"></script>
<script src="/js/heartbeat-manager.js"></script>

<!-- 공통 에러 차단 시스템 -->
<script src="/js/error-blocking-system.js"></script>

<!-- 공통 유틸리티 파일들 -->
<!-- 공통 파라미터 유틸리티 -->
<script src="/js/common-parameter-utils.js"></script>
<!-- 공통 상태 표시 유틸리티 -->
<script src="/js/common-status-utils.js"></script>
<!-- 표준 MQTT 초기화 템플릿 -->
<script src="/js/mqtt-init-template.js"></script>
<!-- 통합 AJAX 및 검증 관리자 -->
<script src="/js/unified-ajax-manager.js"></script>
<script src="/js/unified-validation-manager.js"></script>

<!-- MQTT 관련 스크립트 -->
<script src="/js/mqttws31-min.js"></script>
    <script src="/js/unified-mqtt-manager.js?v=20251018003"></script>
    <script src="/js/mqtt-message-validator.js"></script>
    <script src="/js/session-timeout-manager.js"></script>

<!-- 기타 스크립트들 -->
<script src="/js/bootstrap.min.js"></script>
<script src="/js/templatemo_script.js"></script>

<!-- 로컬 Chart.js 2.9.4 사용 (외부 의존성 제거) -->
	<script src="/js/chart-2.9.4.min.js"></script>
<script type="text/javascript">
    // 세션 관리 모듈 초기화 (main.jsp와 동일한 로직)
    console.log('=== 세션 관리 모듈 초기화 ===');
    if (typeof SessionManager !== 'undefined') {
        if (!SessionManager.initialize()) {
            console.error('세션 관리 모듈 초기화 실패');
            window.location.href = '/login/login';
        }
    } else {
        console.warn('SessionManager 모듈을 찾을 수 없습니다. 기본 세션 처리 사용');
        window.SessionData = {
            userId: document.getElementById('userId') ? document.getElementById('userId').value : '',
            userGrade: document.getElementById('userGrade') ? document.getElementById('userGrade').value : 'U',
            userNm: document.getElementById('userNm') ? document.getElementById('userNm').value : '',
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

    // 차트 페이지용 allowedSensorIds 설정 (부계정 지원)
    var sensorId = $('#sensorId').val();
    var currentUserId = $('#userId').val();
    window.allowedSensorIds = sensorId ? [sensorId] : (currentUserId ? [currentUserId] : []);
    console.log('차트 페이지 allowedSensorIds 설정:', window.allowedSensorIds);

    var setp01 = 0;
    
    // 상태 관리 변수 초기화 (main.jsp와 동일한 로직)
    var deviceErrorStates = {}; // 객체로 초기화
    var deviceDinErrorStates = {}; // 객체로 초기화
    var deviceLastDataTime = {}; // 객체로 초기화
    var deviceErrorCounters = {}; // 객체로 초기화

    // MQTT 실시간 데이터만 사용하므로 getData AJAX 함수 비활성화
    // getData 함수는 더 이상 사용하지 않음 (MQTT 실시간 데이터 사용)

    function getParam() {
        // 차트 페이지에서는 MQTT로 실시간 데이터를 받으므로 별도 파라미터 조회 불필요
        console.log('차트 페이지 getParam 호출 - MQTT 실시간 데이터 사용');
        
        // 설정온도 조회 (DB에서 기존 설정값 가져오기)
        loadSensorSettings();
        
        // MQTT로 설정값 요청 (GET&type=1)
        setSensor();
        
        // 차트 초기화 실행
        initializeChart();
        
        // 날짜 초기화 실행
        initializeDate();
    }
    
    // 센서 설정값 조회 함수
    function loadSensorSettings() {
        var sensorUuid = $('#sensorUuid').val();
        if (!sensorUuid) {
            console.warn('센서 UUID가 없습니다.');
            return;
        }
        
        
        $.ajax({
            url: '/data/getSensorSettings',
            method: 'POST',
            data: {
                sensorUuid: sensorUuid
            },
            success: function(response) {
                if (response.resultCode === '200' && response.settings) {
                    console.log('센서 설정값 조회 성공:', response.settings);
                    
                    // 설정온도 표시 (p01 파라미터)
                    if (response.settings.p01) {
                        var setTemp = decodeTemperature(response.settings.p01);
                        $('#setTmp').html(setTemp + '°C');
                        console.log('설정온도 표시:', setTemp + '°C');
                    }
                    
                    // 기타 파라미터들도 표시 가능
                    if (response.settings.p02) {
                        $('#p02').val(decodeTemperature(response.settings.p02));
                    }
                    if (response.settings.p03) {
                        $('#p03').val(response.settings.p03);
                    }
                    // 필요한 다른 파라미터들도 추가 가능
                    
                } else {
                    console.warn('센서 설정값 조회 실패:', response.resultMessage);
                }
            },
            error: function(xhr, status, error) {
                console.error('센서 설정값 조회 오류:', error);
            }
        });
    }

    function setSensor() {
        // MQTT로 설정값 요청 (GET&type=1)
        console.log('차트 페이지 setSensor 호출 - 설정값 요청');
        if (typeof UnifiedMQTTManager !== 'undefined' && UnifiedMQTTManager.isConnected()) {
            UnifiedMQTTManager.publishSensorRequest('GET&type=1');
            console.log('MQTT 설정값 요청 전송: GET&type=1');
        } else {
            console.warn('MQTT 연결되지 않음 - 설정값 요청 불가');
        }
    }

    function getStatus() {
        // MQTT로 상태값 요청 (GET&type=2)
        console.log('차트 페이지 getStatus 호출 - 상태값 요청');
        if (typeof UnifiedMQTTManager !== 'undefined' && UnifiedMQTTManager.isConnected()) {
            UnifiedMQTTManager.publishSensorRequest('GET&type=2');
            console.log('MQTT 상태값 요청 전송: GET&type=2');
        } else {
            console.warn('MQTT 연결되지 않음 - 상태값 요청 불가');
        }
    }

    // 표준 MQTT 초기화 등록
    registerMQTTInitialization({
        pageName: 'Chart',
        requestSettings: function() {
            console.log('Chart 페이지: 설정값 요청 (GET&type=1)');
                // MQTT로 설정값 요청
                if (typeof UnifiedMQTTManager !== 'undefined' && UnifiedMQTTManager.isConnected()) {
                    UnifiedMQTTManager.publishSensorRequest('GET&type=1');
                    console.log('MQTT 설정값 요청 전송: GET&type=1');
                } else {
                    console.warn('MQTT 연결되지 않음 - 설정값 요청 불가');
                }
        },
        requestStatus: function() {
            console.log('Chart 페이지: 상태값 요청 (GET&type=2)');
            // MQTT로 상태값 요청
            if (typeof UnifiedMQTTManager !== 'undefined' && UnifiedMQTTManager.isConnected()) {
                UnifiedMQTTManager.publishSensorRequest('GET&type=2');
                console.log('MQTT 상태값 요청 전송: GET&type=2');
            } else {
                console.warn('MQTT 연결되지 않음 - 상태값 요청 불가');
            }
        },
        startErrorCheck: function() {
            console.log('Chart 페이지: 에러 체크 시작');
            // 차트 페이지는 실시간 데이터 기반 에러 체크
        },
        initializePageSpecific: function() {
            console.log('Chart 페이지: 페이지별 초기화');
            // 차트 페이지별 추가 초기화 로직
        }
    });

    // startInterval 함수는 common-utils.js에서 제공

    // 날짜 변경 이벤트 핸들러
    $('#startDate, #endDate').on('change', function() {
        console.log('날짜 변경 감지 - startDate:', $('#startDate').val(), 'endDate:', $('#endDate').val());
        loadChartData();
    });
    
    // 이전/다음 날짜 버튼 이벤트 핸들러
    $('button[onclick*="prev"]').on('click', function() {
        prevDate();
    });
    
    $('button[onclick*="next"]').on('click', function() {
        nextDate();
    });
    
    // 기간 버튼 이벤트 핸들러
    $('button:contains("1개월")').on('click', function() {
        setPeriod('1month');
    });
    
    $('button:contains("3개월")').on('click', function() {
        setPeriod('3month');
    });
    
    $('button:contains("6개월")').on('click', function() {
        setPeriod('6month');
    });
    
    $('button:contains("1년")').on('click', function() {
        setPeriod('1year');
    });

    // 다운로드 중 플래그
    var isDownloading = false;
    
    $('#excelDownload').click(function() {
        // 다운로드 중이면 무시
        if (isDownloading) {
            console.log('이미 다운로드 중입니다. 중복 요청 차단.');
            return;
        }
        
        var params = {
            userId: $('#userId').val(),
            sensorId: $('#sensorId').val(),
            sensorUuid: $('#sensorUuid').val(),
            sensorName: $('#sensorName').val(),
            startDate: $('#startDate').val(),
            endDate: $('#endDate').val()
        };

        // 엑셀 다운로드 파라미터
        downloadExcel(params);
    });

    // 엑셀 다운로드 함수 구현
    function downloadExcel(params) {
        console.log('엑셀 다운로드 시작:', params);
        
        // 파라미터 검증
        if (!params.sensorUuid) {
            alert('센서 정보가 없습니다.');
            return;
        }
        
        if (!params.startDate || !params.endDate) {
            alert('날짜 범위를 선택해주세요.');
            return;
        }
        
        // 다운로드 플래그 설정
        isDownloading = true;
        
        // 버튼 비활성화 및 텍스트 변경
        var $downloadBtn = $('#excelDownload');
        $downloadBtn.prop('disabled', true);
        var originalText = $downloadBtn.text();
        $downloadBtn.text('다운로드 중...');
        
        console.log('다운로드 버튼 비활성화 - 중복 클릭 방지');
        
        // URL 파라미터 생성
        var urlParams = new URLSearchParams();
        urlParams.append('userId', params.userId || '');
        urlParams.append('sensorId', params.sensorId || '');
        urlParams.append('sensorUuid', params.sensorUuid);
        urlParams.append('sensorName', params.sensorName || '');
        urlParams.append('startDate', params.startDate);
        urlParams.append('endDate', params.endDate);
        urlParams.append('gu', 'd'); // 일간 데이터
        
        // 다운로드 URL 생성
        var downloadUrl = '/data/excelDownload?' + urlParams.toString();
        
        console.log('다운로드 URL:', downloadUrl);
        
        // 현재 창에서 다운로드 실행
        location.href = downloadUrl;
        
        // 3초 후 버튼 재활성화 (다운로드 완료 예상 시간)
        setTimeout(function() {
            isDownloading = false;
            $downloadBtn.prop('disabled', false);
            $downloadBtn.text(originalText);
            console.log('다운로드 버튼 재활성화');
        }, 3000);
    }

    function goMain() {
    // 공통 페이지 이동 함수 사용 (세션 기반)
    PageNavigation.goMain();
}

// 인터벌 시작 함수 (main.jsp와 동일한 구현)
function startInterval(seconds, callback, gu) {
    callback();
    return setInterval(callback, seconds * 1000);
}

// 즉시 초기화 (페이지 로딩보다 빠름)
    (function() {
        // 에러 체크 변수 초기화 (객체 키 방식으로 수정)
        var sensorUuid = $('#sensorUuid').val() || '0008DC7553A4';
        deviceLastDataTime[sensorUuid] = Date.now();
        deviceErrorCounters[sensorUuid] = 0;
        deviceErrorStates[sensorUuid] = false;
        deviceStatusStates = 'gray';
        deviceErrorDisplayStates = 'gray';
        window.deviceDinErrorStates = false;

        // MQTT 실시간 데이터만 사용하므로 getData AJAX 호출 제거
        // startInterval(0.5, getData);
        getParam();
        // MQTT 연결 확인 (공통 핸들러 사용)
        if (typeof handlePageMQTT === 'function') {
          handlePageMQTT('chart');
        } else {
          console.warn('공통 MQTT 핸들러를 찾을 수 없음');
          // 기존 방식 호환성
          if (typeof startConnect === 'function') {
            startConnect();
          }
        }
        
        // 통합된 초기 동기화 사용
        if (typeof UnifiedMQTTManager !== 'undefined' && typeof UnifiedMQTTManager.executeInitialSync === 'function') {
          console.log('차트 페이지 - 통합 초기 동기화 사용');
          // MQTT 초기화 완료 후 자동으로 초기 동기화가 실행됨
          UnifiedMQTTManager.executeInitialSync();
        } else {
          console.warn('UnifiedMQTTManager를 찾을 수 없음 - 기존 방식으로 실행');
          
          // 기존 방식 (fallback)
          setTimeout(function() {
            // 챠트데이터 파라미터 확인
            setSensor();
            // 에러 해제 후 GET&type=1 요청
          }, 2000);
          
          setTimeout(function() {
            getStatus();
            // 에러 해제 후 GET&type=2 요청
          }, 4000); // 2초 + 2초 = 4초 후
        }
        
        // 에러 체크 시작 (3초마다 - 더 빠른 에러 감지를 위해 최적화)
        // MQTT 데이터는 인터럽트 방식으로 즉시 수신되며, 에러 체크만 주기적으로 수행
        setTimeout(function() {
            startInterval(3, chkError);
        }, 3000);

        // 아이들(가시성 숨김) 시 에러 체크 일시 정지 / 복귀 시 재개 및 초기 동기화 (main.jsp와 동일한 로직)
        document.addEventListener('visibilitychange', function() {
          if (document.hidden) {
            // 에러 체크 일시 정지: 오탐 방지
            if (window.__errorTimer) {
              clearInterval(window.__errorTimer);
              window.__errorTimer = null;
            }
            // MQTT 연결 상태 확인 및 재연결 시도
            if (typeof UnifiedMQTTManager !== 'undefined' && !UnifiedMQTTManager.isConnected()) {
              console.log('페이지 숨김 상태에서 MQTT 연결 끊어짐 감지, 재연결 시도');
              UnifiedMQTTManager.connect()
                .then(function(client) {
                  console.log('MQTT 재연결 성공:', client);
                  window.dispatchEvent(new CustomEvent('mqtt:connected'));
                })
                .catch(function(error) {
                  console.error('MQTT 재연결 실패:', error);
                });
            }
          } else {
            // 복귀: 차트는 사용자가 선택한 날짜를 유지하고, 불필요한 재조회 방지
            var sensorUuid = $('#sensorUuid').val() || '0008DC7553A4';
            deviceLastDataTime[sensorUuid] = Date.now();
            window.__errorTimer = startInterval(5, chkError);
            
            // MQTT 연결 상태 확인 및 재연결 시도
            if (typeof UnifiedMQTTManager !== 'undefined' && !UnifiedMQTTManager.isConnected()) {
              console.log('페이지 복귀 시 MQTT 연결 끊어짐 감지, 재연결 시도');
              UnifiedMQTTManager.connect()
                .then(function(client) {
                  console.log('MQTT 재연결 성공:', client);
                  window.dispatchEvent(new CustomEvent('mqtt:connected'));
                })
                .catch(function(error) {
                  console.error('MQTT 재연결 실패:', error);
                });
            }
            
            // 상태/설정 재동기화는 차트 데이터에 영향 없음 (주석 처리)
            // setTimeout(function() { getStatus(); }, 2000);
            // setTimeout(function() { setSensor(); }, 4000);
            // 만약 이전에 선택한 날짜 데이터가 있다면 그대로 유지하여 재그리기 없음
            // 필요 시 아래 한 줄로 강제 유지 가능: updateDailyChart(window.dailyChart.data.labels, window.dailyChart.data.datasets[0].data);
          }
        });
    })();

    // 차트 초기화 함수
    function initializeChart() {
        var dailyArr = [];
        var monthlyArr = [];
        var yearlyArr = [];
        var dailyX = [];
        var dailyY = [];
        var monthlyX = [];
        var monthlyY = [];
        var yearlyX = [];
        var yearlyY = [];

        var daily = $('#daily').val();
        var monthly = $('#monthly').val();
        var yearly = $('#yearly').val();

        // 빈 데이터 처리
        if (!daily || daily === '' || daily === '[]') {
            console.warn('일간 데이터가 없습니다. 빈 차트를 표시합니다.');
            daily = ''; // 빈 문자열로 초기화
        }

        console.log('차트 초기화 - daily 데이터:', daily);
        console.log('차트 초기화 - monthly 데이터:', monthly);
        console.log('차트 초기화 - yearly 데이터:', yearly);

        // 현재 차트 날짜/데이터 전역 보관 (툴팁/복구용)
        window._currentChartDate = ($('#endDate').val() || selectedEndDate);
        // dailyArr 길이 확인

        if (daily && daily.length > 0) {
            daily = daily.replace("[", "");
            daily = daily.replace("]", "");
            dailyArr = daily.split(",");
        } else {
            dailyArr = [];
        }
        
        // 전역 dailyArr 설정 (차트 생성용)
        window.dailyArr = dailyArr;
        console.log('window.dailyArr 설정 완료:', window.dailyArr.length, '개 데이터');
        
        // 디버깅: 서버에서 전달받은 데이터 확인
        // 챠트 데이터 디버깅

        if (monthly && monthly.length > 0) {
            monthly = monthly.replace("[", "");
            monthly = monthly.replace("]", "");
            monthlyArr = monthly.split(",");
        } else {
            monthlyArr = [];
        }

        if (yearly && yearly.length > 0) {
            yearly = yearly.replace("[", "");
            yearly = yearly.replace("]", "");
            yearlyArr = yearly.split(",");
        } else {
            yearlyArr = [];
        }

        // 선택된 날짜(없으면 오늘) 기준으로 00:00~23:59 데이터만 표시
        var today = new Date();
        var todayStr = today.getFullYear() + '-' + 
                      String(today.getMonth() + 1).padStart(2, '0') + '-' + 
                      String(today.getDate()).padStart(2, '0');
        var selectedEndDate = ($('#endDate').val() && $('#endDate').val().length === 10) ? $('#endDate').val() : todayStr;
        
        // 24시간 X축 라벨 생성 (00:00 ~ 23:30, 30분 간격)
        var timeLabels = [];
        for(var hour = 0; hour < 24; hour++) {
            for(var minute = 0; minute < 60; minute += 30) {
                var timeStr = String(hour).padStart(2, '0') + ':' + String(minute).padStart(2, '0');
                timeLabels.push(timeStr);
            }
        }
        
                 // DB에서 읽어온 데이터를 시간별로 매핑
         var dataMap = {};
         
         // 데이터 매핑 디버깅
         // dailyArr 처리 시작
         
         for(var i=0; i < dailyArr.length; i++) {
             if(dailyArr[i] && dailyArr[i].trim() !== '') {
                 var tmp = [];
                 tmp = dailyArr[i].split("^");
                 
                 // 데이터 파싱
                 
                 if(tmp.length >= 2) {
                     var fullDateTime = tmp[0].trim();
                     var sensorValue = tmp[1].trim();
                     
                     // fullDateTime과 sensorValue 파싱
                     
                     // 시간 추출: "2025-09-23 00:30" -> "00:30"
                     var timeOnly = "";
                     if(fullDateTime.length > 10) {
                         // "2025-09-23 00:30" -> "00:30"
                         var timePart = fullDateTime.substring(11);
                         timeOnly = timePart;
                     } else {
                         timeOnly = fullDateTime;
                     }
                     
                     // 추출된 시간 확인
                     
                     if(timeOnly && !isNaN(parseFloat(sensorValue))) {
                         dataMap[timeOnly] = parseFloat(sensorValue);
                         // 데이터 매핑 완료
                     }
                 }
             }
         }
         
         // 최종 dataMap 생성 완료
         
                 // 24시간 X축에 맞춰 데이터 배열 생성
        // 차트 데이터 생성 시작
        
        for(var i=0; i < timeLabels.length; i++) {
            dailyX.push(timeLabels[i]);
            
            if(dataMap[timeLabels[i]]) {
                dailyY.push(dataMap[timeLabels[i]]);
                // 시간별 데이터 매핑
            } else {
                dailyY.push(null); // 데이터가 없는 시간대는 null로 표시
                // 데이터 없음
            }
        }
        
        // 최종 dailyX, dailyY 배열 생성 완료
        // dailyY에서 null이 아닌 데이터 개수 확인
        var validDataCount = dailyY.filter(y => y !== null).length;
        console.log('차트 데이터 생성 완료:', dailyX.length, '개 라벨,', validDataCount, '개 유효 데이터');
        console.log('데이터 맵 키들:', Object.keys(dataMap));
        console.log('샘플 데이터:', dailyY.slice(0, 10));

        // 전역에 현재 raw daily 배열 보관 (툴팁에서 사용)
        window._currentDailyArr = dailyArr.slice();
        // 챠트데이터 생성 완료

        for(var j=0; j < monthlyArr.length; j++) {
            var tmp = [];
            tmp = monthlyArr[j].split("^");
            monthlyX.push(tmp[0]);
            monthlyY.push(tmp[1]);
        }

        for(var k=0; k < yearlyArr.length; k++) {
            var tmp = [];
            tmp = yearlyArr[k].split("^");
            yearlyX.push(tmp[0]);
            yearlyY.push(tmp[1]);
        }

                
        
        // 차트 생성 전 캔버스 확인
        var canvas = document.getElementById("dailyChart");
        if (!canvas) {
            console.error("캔버스 요소를 찾을 수 없습니다!");
            return;
        }
        
        var ctx = canvas.getContext("2d");
        if (!ctx) {
            console.error("캔버스 컨텍스트를 가져올 수 없습니다!");
            return;
        }
        
        // 중복 인스턴스 방지: 기존 차트 모두 제거
        try { if (window.dailyChart && typeof window.dailyChart.destroy === 'function') { window.dailyChart.destroy(); } } catch(e) {}
        try { if (window.myChart && typeof window.myChart.destroy === 'function') { window.myChart.destroy(); } } catch(e) {}

        // 차트 생성 전 데이터 확인
        console.log('차트 생성 전 데이터 확인:');
        console.log('  dailyX 길이:', dailyX.length);
        console.log('  dailyY 길이:', dailyY.length);
        console.log('  dailyY 유효 데이터:', dailyY.filter(y => y !== null).length);
        console.log('  dailyX 샘플:', dailyX.slice(0, 5));
        console.log('  dailyY 샘플:', dailyY.slice(0, 5));

        window.dailyChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: dailyX,
                datasets: [{
                    label: '일간 데이터',
                    data: dailyY,
                     spanGaps: true,
                     borderColor: "rgba(255, 201, 14, 1)",
                     backgroundColor: "rgba(255, 201, 14, 0.5)",
                     fill: false,
                     lineTension: 0.1,
                     pointRadius: 0,
                     pointHoverRadius: 0,
                     pointBackgroundColor: "rgba(255, 201, 14, 1)",
                     pointBorderColor: "rgba(255, 201, 14, 1)",
                     pointBorderWidth: 1
                }]
            },
            options: {
                 responsive: true,
                // 마우스 상호작용이 차트 데이터 변경을 유발하지 않도록 유지
                 maintainAspectRatio: false,
                 aspectRatio: 2,
                 title: {
                     display: true,
                     text: '일간 데이터'
                 },
                tooltips: {
                    mode: 'index',
                    intersect: false,
                    callbacks: {
                        title: function(tooltipItems, data) {
                            // 선택된 날짜 고정 + 현재 라벨 시간 결합
                            var idx = tooltipItems[0].index;
                            var dateStr = window._currentChartDate;
                            var timeStr = data.labels[idx] || '';
                            if (dateStr && timeStr) return dateStr + ' ' + timeStr;
                            return timeStr || dateStr || '';
                        },
                        label: function(tooltipItem, data) {
                            if (tooltipItem.yLabel !== null) {
                                return '온도: ' + tooltipItem.yLabel + '°C';
                            }
                            return '데이터 없음';
                        }
                    }
                },
                hover: {
                    mode: 'nearest',
                    intersect: true
                },
                scales: {
                    xAxes: [{
                        display: true,
                        scaleLabel: {
                            display: true,
                            labelString: '시간 (24시간)'
                        },
                                                 ticks: {
                             maxTicksLimit: 12, // 12시간 표시 (더 조밀하게)
                             maxRotation: 45,   // 라벨 회전
                             minRotation: 0,
                             callback: function(value, index, values) {
                                 // 1시간마다 라벨 표시 (00:00, 01:00, 02:00, ...)
                                 if(index % 2 === 0) {
                                     return value;
                                 }
                                 return '';
                             }
                         }
                    }],
                    yAxes: [{
                        display: true,
                        ticks: {
                            // DB 데이터에 맞게 자동 범위 조정
                            beginAtZero: false,
                            // 최소값과 최대값을 자동으로 계산
                            callback: function(value, index, values) {
                                return value + '°C';
                            }
                        },
                        scaleLabel: {
                            display: true,
                            labelString: '온도 (°C)'
                        }
                    }]
                }
            }
        });
        
        console.log('차트 생성 완료 - 데이터 포인트:', dailyY.filter(y => y !== null).length, '개');
        console.log('차트 인스턴스:', window.dailyChart);
    }

    // 차트 초기화 함수 호출
    initializeChart();

    $('#1mon').click(function() {
        var now = new Date();	// 현재 날짜 및 시간
        var oneMon = new Date(now.setMonth(now.getMonth() - 1));	// 한달 전
        $('#startDate').val(dateFormat(oneMon));
    });

    $('#3mon').click(function() {
        var now = new Date();	// 현재 날짜 및 시간
        var oneMon = new Date(now.setMonth(now.getMonth() - 3));	// 한달 전
        $('#startDate').val(dateFormat(oneMon));
    });

    $('#6mon').click(function() {
        var now = new Date();	// 현재 날짜 및 시간
        var oneMon = new Date(now.setMonth(now.getMonth() - 6));	// 한달 전
        $('#startDate').val(dateFormat(oneMon));
    });

    $('#12mon').click(function() {
        var now = new Date();	// 현재 날짜 및 시간
        var oneMon = new Date(now.setMonth(now.getMonth() - 12));	// 한달 전
        $('#startDate').val(dateFormat(oneMon));
    });

    function dateFormat(date) {
        let dateFormat2 = date.getFullYear() +
            '-' + ( (date.getMonth()+1) < 10 ? "0" + (date.getMonth()+1) : (date.getMonth()+1) )+
            '-' + ( (date.getDate()) < 10 ? "0" + (date.getDate()) : (date.getDate()) );
        return dateFormat2;
    }

    function IsJsonString(str) {
        try {
            var json = JSON.parse(str);
            return (typeof json === 'object');
        } catch (e) {
            return false;
        }
    }

    function validateJson(str) {
        try {
            var json = JSON.parse(str);
            return (typeof json === 'object');
        } catch (e) {
            return false;
        }
    }

    // updateCurrentTemperature 함수는 mqtt_lib.js에서 제공됨 (중복 제거)

    // 상태표시등 업데이트 함수들은 common-utils.js에서 제공됨
    // 차트 페이지 전용 상태표시등 업데이트 함수들 (common-utils.js의 updateStatusIndicator 사용)
    // 상태 표시등 업데이트 함수들 (공통 유틸리티 사용)
    // 온도 디코딩 함수 (p01 파라미터용)
    function decodeTemperature(encodedValue) {
        if (!encodedValue || encodedValue === '') {
            return '0.0';
        }
        
        var value = parseFloat(encodedValue);
        if (isNaN(value)) {
            return '0.0';
        }
        
        // 10배 인코딩된 값을 디코딩 (100 -> 10.0)
        var decodedValue = value / 10;
        return decodedValue.toFixed(1);
    }

    // 상태표시등 업데이트 함수
function updateStatusIndicatorSingle(type, status) {
    var elementId = type + 'Chart';
    var element = document.getElementById(elementId);
    if (element) {
        // 기존 클래스 제거
        element.className = 'status-indicator';
        
        // 상태에 따른 클래스 추가
        if (status === 'green') {
            element.classList.add('green');
        } else if (status === 'red') {
            element.classList.add('red');
        } else {
            element.classList.add('gray');
        }
        
        console.log('상태표시등 업데이트:', elementId, status);
    } else {
        console.warn('상태표시등 요소를 찾을 수 없음:', elementId);
    }
}

// 상태표시등 업데이트 함수 (output 메시지용) - 장치설정페이지 방식 참조
function updateStatusIndicatorFromOutput(type, value) {
    var status = 'gray'; // 기본값
    
    if (value === '1' || value === 1) {
        status = 'red'; // ON 상태
    } else if (value === '0' || value === 0) {
        status = 'gray'; // OFF 상태
    }
    
    updateStatusIndicatorSingle(type, status);
}

    function updateStatusChart(status) {
        updateStatusIndicatorSingle('status', status);
    }

    function updateCompChart(status) {
        updateStatusIndicatorSingle('comp', status);
    }

    function updateDefrChart(status) {
        updateStatusIndicatorSingle('defr', status);
    }

    function updateFanChart(status) {
        updateStatusIndicatorSingle('fan', status);
    }

    function updateErrorChart(status) {
        updateStatusIndicatorSingle('error', status);
    }

    function rcvMsg(topic, message) {
        // 챠트데이터 - MQTT 메시지 수신 (main.jsp와 동일한 로직)
        
        if(topic) {
            var topicArr = new Array();
            topicArr = topic.split("/");
            var uuid = topicArr[3];
            var userId = topicArr[1];

            // 현재 사용자의 장치만 처리 (부계정 사용자 고려) - allowedSensorIds 확인
            var currentUserId = $('#userId').val();
            var allowedSensorIds = window.allowedSensorIds || [];
            
            // 현재 사용자 또는 allowedSensorIds에 포함된 userId만 처리
            var isAllowedUser = (userId === currentUserId) || (allowedSensorIds.indexOf(userId) >= 0);
            if (!isAllowedUser) {
                console.log('차트 페이지: 사용자 ID 불일치로 메시지 필터링됨:', {
                    messageUserId: userId,
                    currentUserId: currentUserId,
                    allowedSensorIds: allowedSensorIds
                });
                return;
            }

            // 현재 장치만 처리 (sensor_uuid만 확인)
            if(uuid == $('#sensorUuid').val()) {
                if(message) {
                    if(validateJson(message)) {
                        var msg = JSON.parse(message);
                        if (msg.actcode == 'setres') {
                            // 설정 응답 처리 - 설정온도 표시
                            console.log('차트 페이지 설정 응답 수신:', msg);
                            if (msg.p01) {
                                var setTemp = parseFloat(msg.p01) / 10; // p01은 10배 인코딩된 값
                                $('#setTmp').text(setTemp.toFixed(1) + '°C');
                                console.log('설정온도 업데이트:', setTemp.toFixed(1) + '°C');
                            }
                        } else if (msg.actcode == 'live' && msg.name == 'output') {
                            // output 메시지 처리 - 상태표시등 업데이트 (장치설정페이지 방식 참조)
                            console.log('차트 페이지 output 메시지 수신:', msg);
                            
                            // Output 타입에 따른 상태표시등 업데이트
                            var outputType = msg.type;
                            var outputValue = msg.value;
                            
                            if (outputType == '1') {  // COMP
                                updateStatusIndicatorFromOutput('comp', outputValue);
                            } else if (outputType == '2') {  // DEF (제상/히터)
                                updateStatusIndicatorFromOutput('defr', outputValue);
                            } else if (outputType == '3') {  // FAN
                                updateStatusIndicatorFromOutput('fan', outputValue);
                            }
                        } else if (msg.actcode == 'live') {
                            if (msg.name == 'ain') {
                                // 현재 온도 알림 - 직접 처리
                                var isError = (msg.value == 'Error');
                                
                                // 현재온도 직접 업데이트
                                if (isError) {
                                    $('#sensorVal').text('Error');
                                } else {
                                    $('#sensorVal').text(msg.value + '°C');
                                }
                                
                                if (isError) {
                                    updateStatusChart('gray');
                                    updateErrorChart('red');
                                } else {
                                    updateStatusChart('green');
                                    
                                    // 정상 온도 데이터 수신 시 에러 상태 해제
                                    if (deviceErrorStates['']) {
                                        deviceErrorStates[''] = false;
                                        deviceErrorCounters[''] = 0;
                                        
                                        // 상태표시등 업데이트
                                        // DIN 이상이 활성 중이면 통신정상이어도 이상표시는 유지한다
                                        if (!deviceDinErrorStates['']) {
                                            updateErrorChart('gray');
                                        }
                                        updateStatusChart('green');
                                        
                                        
                                        
                                        // 에러 해제 후 GET&type=1, GET&type=2 2초 간격으로 한 번만 요청
                                        setTimeout(function() {
                                            setSensor();
                       
                                        }, 2000);
                                        
                                        setTimeout(function() {
                                            getStatus();
                   
                                        }, 4000); // 2초 + 2초 = 4초 후
                                    }
                                    
                                    // 마지막 데이터 수신 시간 업데이트
                                    var sensorUuid = $('#sensorUuid').val() || '0008DC7553A4';
                                    deviceLastDataTime[sensorUuid] = Date.now();
                                    
                                    // 온도 데이터 수신 시점에 바로 에러 체크
                                    chkError();
                                }
                            } else if (msg.name == 'din') {
                                // input 상태 변화 알림
                                if (msg.type == '1' || msg.type == 1) {
                                    if (msg.value == '1' || msg.value == 1) {
                                        deviceDinErrorStates[''] = true;
                                        updateErrorChart('red');
                                    } else {
                                        deviceDinErrorStates[''] = false;
                                        // 통신이상이 아닐 때만 회색으로 변경
                                        if (!deviceErrorStates['']) {
                                            updateErrorChart('gray');
                                        }
                                    }
                                }
                            } else if (msg.name == 'output') {
                                // output 상태 변화 알림
                                var type = msg.type;
                                var value = msg.value;
                                
                                console.log('Output 상태 변화:', type, value);
                                
                                // 상태표시등 업데이트
                                if (type == '1' || type == 1) { // COMP
                                    updateStatusIndicatorFromOutput('comp', value);
                                } else if (type == '2' || type == 2) { // DEF
                                    updateStatusIndicatorFromOutput('defr', value);
                                } else if (type == '3' || type == 3) { // FAN
                                    updateStatusIndicatorFromOutput('fan', value);
                                }
                            }
                        } else if (msg.actcode == "actres") {

                        }
                    }
                }
            }
        }
    }

    // 에러 체크 함수 (main.jsp와 동일한 로직)
    function chkError() {
        var currentTime = Date.now();
        var sensorUuid = $('#sensorUuid').val() || '0008DC7553A4'; // 현재 센서 UUID 가져오기
        
        // deviceLastDataTime이 0이면 아직 데이터를 받지 않은 상태
        var timeDiff = deviceLastDataTime[sensorUuid] === 0 ? 999999999 : currentTime - deviceLastDataTime[sensorUuid];
        
        // 디버깅: 변수 상태 확인 (상세 로그)
        var lastDataTimeStr = deviceLastDataTime[sensorUuid] === 0 ? "미수신" : new Date(deviceLastDataTime[sensorUuid]).toLocaleTimeString();
        var timeDiffStr = deviceLastDataTime[sensorUuid] === 0 ? "무한대" : (timeDiff / 1000).toFixed(1) + "초";
        
        console.log(`[Chart] 에러 체크 변수 상태:`, {
          deviceLastDataTime: deviceLastDataTime[sensorUuid],
          lastDataTimeStr: lastDataTimeStr,
          deviceErrorCounters: deviceErrorCounters[sensorUuid],
          deviceErrorStates: deviceErrorStates[sensorUuid],
          currentTime: currentTime,
          timeDiff: timeDiff,
          timeDiffStr: timeDiffStr,
          threshold: 5000,
          isOverThreshold: timeDiff > 5000
        });
        
        // 5초 동안 온도 데이터 미수신 시 에러 체크 (main.jsp와 동일)
        if (deviceLastDataTime[sensorUuid] === 0 || timeDiff > 5000) {
            if (!deviceErrorStates[sensorUuid]) {
                deviceErrorCounters[sensorUuid] = (deviceErrorCounters[sensorUuid] || 0) + 1;
                
                console.log(`[Chart] 에러 체크: deviceLastDataTime=${lastDataTimeStr}, timeDiff=${timeDiffStr}, counter=${deviceErrorCounters[sensorUuid]}`);
                
                // 3번 연속 미수신 시 에러 상태로 변경
                if (deviceErrorCounters[sensorUuid] >= 3) {
                    console.log(`[Chart] 통신에러 상태로 변경 시작: ${deviceErrorCounters[sensorUuid]}번 연속 미수신`);
                    deviceErrorStates[sensorUuid] = true;
                    deviceStatusStates = 'gray';
                    deviceErrorDisplayStates = 'red';
                    
                    console.log(`[Chart] 통신에러 상태로 변경: ${deviceErrorCounters[sensorUuid]}번 연속 미수신`);
                    
                    // 상태표시등 업데이트 - 통신이상시 이상만 빨간색, 나머지는 회색
                    updateStatusChart('gray');
                    updateCompChart('gray');
                    updateDefrChart('gray');
                    updateFanChart('gray');
                    updateErrorChart('red');
                    
                    // 현재온도 표시를 Error로 변경
                    $('#sensorVal').text('Error');
                    console.log('[Chart] 현재온도 UI를 Error로 변경');
                }
            }
        } else if (deviceLastDataTime[sensorUuid] > 0) {
            // 정상 데이터 수신 시 (시간 제한 없음)
            if (deviceErrorStates[sensorUuid]) {
                console.log(`[Chart] 에러 상태 해제: 정상 데이터 수신`);
                
                // 에러 상태 해제
                deviceErrorStates[sensorUuid] = false;
                deviceErrorCounters[sensorUuid] = 0;
                deviceStatusStates = 'green';
                deviceErrorDisplayStates = 'gray';
                
                // 상태 복구
                updateStatusChart('green');
                updateErrorChart('gray');
            } else {
                // 정상 상태에서도 카운터 리셋 (연속 에러 방지)
                deviceErrorCounters[sensorUuid] = 0;
            }
        }
    }
    
    // 날짜 이동 함수들
    $(document).ready(function() {
        // URL 파라미터에서 날짜 정보 읽어오기
        setDateFromUrl();
        
        // 이전 날짜 버튼 클릭 이벤트
        $('#prevDay').click(function() {
            moveDate(-1);
        });
        
        // 다음 날짜 버튼 클릭 이벤트
        $('#nextDay').click(function() {
            moveDate(1);
        });
    });
    
    // 날짜 초기화 함수
    function initializeDate() {
        // 오늘 날짜를 YYYY-MM-DD 형식으로 설정
        var today = new Date();
        var todayStr = today.getFullYear() + '-' + 
                      String(today.getMonth() + 1).padStart(2, '0') + '-' + 
                      String(today.getDate()).padStart(2, '0');
        
        // 날짜 입력 필드에 오늘 날짜 설정
        $('#startDate').val(todayStr);
        $('#endDate').val(todayStr);
        
        console.log('날짜 초기화 완료:', todayStr);
    }
    
    // 세션에서 날짜 정보를 읽어와서 입력 필드에 설정 (URL 파라미터 대신 세션 사용)
    function setDateFromUrl() {
        var startDate = '${startDate}';
        var endDate = '${endDate}';
        
        // URL 파라미터에서 읽은 날짜
        
        if(startDate) {
            $('#startDate').val(startDate);
        }
        if(endDate) {
            $('#endDate').val(endDate);
        }
        
        // 입력 필드 설정 완료
    }
    
    // 날짜 이동 함수
    function moveDate(direction) {
        var startDate = new Date($('#startDate').val());
        var endDate = new Date($('#endDate').val());
        var today = new Date();
        
        // 날짜 이동 시작
        
        // 오늘 날짜를 YYYY-MM-DD 형식으로 변환
        var todayStr = today.getFullYear() + '-' + 
                      String(today.getMonth() + 1).padStart(2, '0') + '-' + 
                      String(today.getDate()).padStart(2, '0');
        
        // 날짜 이동
        startDate.setDate(startDate.getDate() + direction);
        endDate.setDate(endDate.getDate() + direction);
        
        // 오늘 날짜를 넘어가지 않도록 제한
        if (endDate > today) {
            endDate = new Date(today);
            startDate = new Date(today);
        }
        
        // 날짜를 YYYY-MM-DD 형식으로 변환
        var newStartDate = startDate.getFullYear() + '-' + 
                          String(startDate.getMonth() + 1).padStart(2, '0') + '-' + 
                          String(startDate.getDate()).padStart(2, '0');
        var newEndDate = endDate.getFullYear() + '-' + 
                        String(endDate.getMonth() + 1).padStart(2, '0') + '-' + 
                        String(endDate.getDate()).padStart(2, '0');
        
        // 날짜 이동 후
        
        // 입력 필드 업데이트
        $('#startDate').val(newStartDate);
        $('#endDate').val(newEndDate);
        
        // 입력 필드 값 확인
        // 입력 필드 업데이트 완료
        
        // 다음 날짜 버튼 비활성화/활성화
        if (newEndDate >= todayStr) {
            $('#nextDay').prop('disabled', true).css('opacity', '0.5');
        } else {
            $('#nextDay').prop('disabled', false).css('opacity', '1');
        }
        
        // 날짜 변경 후 데이터 새로고침
        refreshChartData();
    }
    
    // 차트 데이터 새로고침 함수 (AJAX 방식)
    function refreshChartData() {
        var userId = $('#userId').val();
        var sensorId = $('#sensorId').val();
        var sensorUuid = $('#sensorUuid').val();
        var endDate = $('#endDate').val();
        
        // 값이 비어있으면 경고
        if(!userId || !sensorId || !sensorUuid || !endDate) {
            console.error('필수 파라미터가 비어있습니다!');
            return;
        }
        
        // AJAX로 데이터 요청 (성능 최적화)
        console.log("=== 날짜 변경 AJAX 요청 ===");
        console.log("요청 파라미터:", {
            userId: userId,
            sensorId: sensorId,
            sensorUuid: sensorUuid,
            startDate: endDate,
            endDate: endDate
        });
        
        $.ajax({
            url: '/data/getDailyData',
            type: 'POST',
            cache: false,
            timeout: 30000,
            data: {
                userId: userId,
                sensorId: sensorId,
                sensorUuid: sensorUuid,
                startDate: endDate,  // endDate를 startDate로 전달
                endDate: endDate
            },
            success: function(response) {
                console.log("=== AJAX 응답 수신 ===");
                console.log("응답 전체:", response);
                console.log("resultCode:", response.resultCode);
                console.log("dailyData 길이:", response.dailyData ? response.dailyData.length : 0);
                console.log("dailyData 내용:", response.dailyData);
                
                if(response.resultCode === '200') {
                    updateChartWithNewData(response.dailyData);
                } else {
                    console.error('데이터 조회 실패:', response.resultMessage);
                }
            },
            error: function(xhr, status, error) {
                console.error('AJAX 요청 실패:', error);
                console.error('상태:', status);
                console.error('응답 텍스트:', xhr.responseText);
            }
        });
    }
    
    // 새로운 데이터로 차트 업데이트
    function updateChartWithNewData(dailyData) {
        console.log("=== updateChartWithNewData 시작 ===");
        console.log("받은 dailyData:", dailyData);
        console.log("dailyData 길이:", dailyData ? dailyData.length : 0);
        
        if(!dailyData || dailyData.length === 0) {
            console.warn('차트 데이터가 없습니다. 빈 차트를 표시합니다.');
            showEmptyChart();
            return;
        }
        
        // 데이터 파싱
        var dailyArr = dailyData.split(',');
        console.log("파싱된 dailyArr:", dailyArr);
        console.log("dailyArr 길이:", dailyArr.length);
        
        // 선택된 날짜 상태 고정
        window._currentChartDate = $('#endDate').val();
        window._currentDailyArr = dailyArr.slice();
        
        // dailyX, dailyY 배열 초기화
        var dailyX = [];
        var dailyY = [];
        
        // 실제 데이터가 있는 시간대만 추출
        console.log("=== 데이터 처리 시작 ===");
        for(var i = 0; i < dailyArr.length; i++) {
            if(dailyArr[i] && dailyArr[i].includes('^')) {
                var parts = dailyArr[i].split('^');
                var timeStr = parts[0].trim();
                var tempValue = parseFloat(parts[1]);
                
                console.log("데이터[" + i + "]: timeStr='" + timeStr + "', tempValue=" + tempValue);
                
                // 시간 추출: "2025-09-22 00" -> "00:00"
                var timeLabel = "";
                if(timeStr.length > 10) {
                    // "2025-09-22 00" -> "00:00"
                    var hourPart = timeStr.substring(11);
                    timeLabel = hourPart + ":00";
                } else {
                    timeLabel = timeStr;
                }
                
                console.log("추출된 시간: '" + timeLabel + "'");
                
                if(timeLabel && !isNaN(tempValue)) {
                    dailyX.push(timeLabel);
                    dailyY.push(tempValue);
                    console.log("차트 데이터 추가: " + timeLabel + " -> " + tempValue);
                }
            }
        }
        
        console.log("최종 dailyX:", dailyX);
        console.log("최종 dailyY:", dailyY);
        console.log("dailyY에서 null이 아닌 데이터 개수:", dailyY.filter(val => val !== null).length);
        
        // 차트 업데이트
        updateDailyChart(dailyX, dailyY);
        
        // 데이터가 있으면 메시지 숨기기
        $('#chartMessage').hide();
    }
    
    // 빈 차트 표시 함수
    function showEmptyChart() {
        // 완전히 빈 차트 (데이터가 전혀 없는 경우)
        var dailyX = [];
        var dailyY = [];
        
        // 빈 차트 생성
        updateDailyChart(dailyX, dailyY);
        
        // 사용자에게 메시지 표시
        $('#chartMessage').html('<div class="alert alert-info">선택한 날짜에 데이터가 없습니다.</div>').show();
    }
    
    // 차트 업데이트 함수
    function updateDailyChart(dailyX, dailyY) {
        // 기존 차트가 있으면 제거
        if(window.dailyChart && typeof window.dailyChart.destroy === 'function') {
            try {
                window.dailyChart.destroy();
            } catch(e) {
                // 차트 제거 오류 무시
            }
        }
        // 예전 레거시 인스턴스가 남아있을 경우 대비
        if(window.myChart && typeof window.myChart.destroy === 'function') {
            try { window.myChart.destroy(); } catch(e) {}
        }
        
        // 새로운 차트 생성
        var ctx = document.getElementById('dailyChart').getContext('2d');
        window.dailyChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: dailyX,
                datasets: [{
                    label: '일간 데이터',
                    data: dailyY,
                    borderColor: 'rgb(255, 205, 86)',
                    backgroundColor: 'rgba(255, 205, 86, 0.2)',
                    borderWidth: 2,
                    fill: false,
                    tension: 0.1
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: '온도 (°C)'
                        }
                    },
                    x: {
                        title: {
                            display: true,
                            text: '시간 (24시간)'
                        },
                        ticks: {
                            maxTicksLimit: 10,  // 최대 10개의 틱만 표시
                            maxRotation: 45,    // 라벨 회전 각도
                            minRotation: 0
                        }
                    }
                }
            }
        });
    }
    
    // ===== 간단한 앱 복구 기능 =====
    
    // 앱 포커스 시 간단한 새로고침
    document.addEventListener('visibilitychange', function() {
        if (!document.hidden) {
            console.log("앱 포커스됨");
        }
    });
    
    // 차트 페이지 뒤로가기 처리 설정
    function setupChartBackNavigation() {
        // 페이지 로드 시 히스토리 상태 추가
        history.pushState({page: 'chart'}, '차트', window.location.href);
        
        // 뒤로가기 시도 시 메인 페이지로 이동
        window.addEventListener('popstate', function(event) {
            console.log('차트 페이지에서 뒤로가기 감지 - 메인 페이지로 이동');
            PageNavigation.goMain();
        });
        
        // 키보드 뒤로가기 단축키 처리
        document.addEventListener('keydown', function(event) {
            if (event.altKey && event.keyCode === 37) { // Alt + Left Arrow
                event.preventDefault();
                event.stopPropagation();
                console.log('Alt+Left 감지 - 메인 페이지로 이동');
                PageNavigation.goMain();
                return false;
            }
        });
        
        // 마우스 뒤로가기 버튼 처리
        document.addEventListener('mousedown', function(event) {
            if (event.button === 3 || event.button === 4) { // 뒤로가기/앞으로가기 버튼
                event.preventDefault();
                event.stopPropagation();
                console.log('마우스 뒤로가기 버튼 감지 - 메인 페이지로 이동');
                PageNavigation.goMain();
                return false;
            }
        });
        
        // 페이지 이동 시에는 MQTT 연결 유지 (로그아웃 시에만 해제)
        window.addEventListener('beforeunload', function(event) {
            console.log('차트 페이지 이동 - MQTT 연결 유지');
            // 페이지 이동 시에는 MQTT 연결을 끊지 않음
            // 로그아웃 시에만 disconnectOnLogout() 호출
        });
    }
    
    // 차트 페이지 뒤로가기 처리 설정 실행
    setupChartBackNavigation();
    
    // 날짜 변경 관련 함수들
    function loadChartData() {
        var startDate = $('#startDate').val();
        var endDate = $('#endDate').val();
        var sensorUuid = $('#sensorUuid').val();
        
        if (!startDate || !endDate || !sensorUuid) {
            console.warn('날짜 또는 센서 UUID가 없습니다.');
            return;
        }
        
        console.log('차트 데이터 로드 시작 - startDate:', startDate, 'endDate:', endDate, 'sensorUuid:', sensorUuid);
        
        // 로딩 표시
        showLoading();
        
        $.ajax({
            url: '/data/getDailyData',
            method: 'POST',
            data: {
                sensorUuid: sensorUuid,
                startDate: startDate,
                endDate: endDate
            },
            success: function(response) {
                console.log('차트 데이터 로드 성공:', response);
                hideLoading();
                
                if (response.resultCode === '200' && response.dailyData) {
                    // 새 데이터로 차트 업데이트
                    updateChartWithNewData(response.dailyData);
                } else {
                    console.warn('차트 데이터 로드 실패:', response.resultMessage);
                    showError('데이터를 불러올 수 없습니다: ' + response.resultMessage);
                }
            },
            error: function(xhr, status, error) {
                console.error('차트 데이터 로드 오류:', error);
                hideLoading();
                showError('데이터 로드 중 오류가 발생했습니다: ' + error);
            }
        });
    }
    
    function updateChartWithNewData(dailyData) {
        console.log('차트 업데이트 시작 - 데이터 길이:', dailyData ? dailyData.length : 0);
        
        // daily 데이터 업데이트
        $('#daily').val(dailyData);
        
        // 데이터 파싱
        if (dailyData && dailyData.length > 0) {
            dailyData = dailyData.replace("[", "");
            dailyData = dailyData.replace("]", "");
            window.dailyArr = dailyData.split(",");
        } else {
            window.dailyArr = [];
        }
        
        console.log('파싱된 dailyArr 길이:', window.dailyArr ? window.dailyArr.length : 0);
        
        // 차트 재생성
        if (window.dailyChart) {
            window.dailyChart.destroy();
        }
        
        // 차트 초기화
        initializeChart();
        
        console.log('차트 업데이트 완료');
    }
    
    function prevDate() {
        var startDate = $('#startDate').val();
        var endDate = $('#endDate').val();
        
        if (startDate && endDate) {
            var start = new Date(startDate);
            var end = new Date(endDate);
            
            // 하루 전으로 이동
            start.setDate(start.getDate() - 1);
            end.setDate(end.getDate() - 1);
            
            $('#startDate').val(formatDate(start));
            $('#endDate').val(formatDate(end));
            
            loadChartData();
        }
    }
    
    function nextDate() {
        var startDate = $('#startDate').val();
        var endDate = $('#endDate').val();
        
        if (startDate && endDate) {
            var start = new Date(startDate);
            var end = new Date(endDate);
            
            // 하루 후로 이동
            start.setDate(start.getDate() + 1);
            end.setDate(end.getDate() + 1);
            
            $('#startDate').val(formatDate(start));
            $('#endDate').val(formatDate(end));
            
            loadChartData();
        }
    }
    
    function setPeriod(period) {
        var today = new Date();
        var startDate, endDate;
        
        switch(period) {
            case '1month':
                startDate = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate());
                endDate = new Date(today);
                break;
            case '3month':
                startDate = new Date(today.getFullYear(), today.getMonth() - 3, today.getDate());
                endDate = new Date(today);
                break;
            case '6month':
                startDate = new Date(today.getFullYear(), today.getMonth() - 6, today.getDate());
                endDate = new Date(today);
                break;
            case '1year':
                startDate = new Date(today.getFullYear() - 1, today.getMonth(), today.getDate());
                endDate = new Date(today);
                break;
            default:
                return;
        }
        
        $('#startDate').val(formatDate(startDate));
        $('#endDate').val(formatDate(endDate));
        
        loadChartData();
    }
    
    function formatDate(date) {
        var year = date.getFullYear();
        var month = String(date.getMonth() + 1).padStart(2, '0');
        var day = String(date.getDate()).padStart(2, '0');
        return year + '-' + month + '-' + day;
    }
    
    function showLoading() {
        // 로딩 표시 (간단한 구현)
        console.log('로딩 중...');
    }
    
    function hideLoading() {
        // 로딩 숨김
        console.log('로딩 완료');
    }
    
    function showError(message) {
        // 에러 표시 (간단한 구현)
        alert(message);
    }


</script>
</body>
</html>