<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
        <%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
            <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
                <%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
                    <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

                        <html>

                        <head>
                            <meta charset="UTF-8">
                            <!--[if IE]><meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1"><![endif]-->
                            <title>H&T Solutions</title>
                            <link rel="icon" href="/images/hntbi.png" type="image/png">
                            <meta name="keywords" content="" />
                            <meta name="description" content="" />
                            <meta name="viewport"
                                content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <link rel="stylesheet" href="/css/templatemo_main.css">
                            <link rel="stylesheet" href="/css/common-buttons.css">
                            <link rel="stylesheet" href="/css/responsive-common.css">
                            <link rel="stylesheet"
                                href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css"
                                media="print" onload="this.media='all'">

                            <!-- PageNavigation 객체 및 공통 함수 정의 -->
                            <script>
                                window.PageNavigation = {
                                    goMain: function () {
                                        console.log('PageNavigation.goMain() 호출됨');
                                        // Main 버튼 클릭 플래그 설정
                                        sessionStorage.setItem('mainButtonClicked', 'true');
                                        window.location.href = '/main/main';
                                    },
                                    goUserList: function () {
                                        console.log('PageNavigation.goUserList() 호출됨');
                                        window.location.href = '/admin/userList';
                                    },
                                    goCreateSub: function () {
                                        console.log('PageNavigation.goCreateSub() 호출됨');
                                        window.location.href = '/admin/createSub';
                                    },
                                    goSensorSetting: function () {
                                        console.log('PageNavigation.goSensorSetting() 호출됨');
                                        window.location.href = '/admin/sensorSetting';
                                    }
                                };

                                function showError(message) {
                                    console.log('에러:', message);
                                    alert('오류: ' + message);
                                }
                                function showSuccess(message) {
                                    console.log('성공:', message);
                                    alert('성공: ' + message);
                                }
                            </script>

                            <!-- 앱 레이아웃 개선 스타일 -->
                            <style>
                                /* 모바일에서 레이아웃 개선 */
                                @media (max-width: 768px) {
                                    .templatemo-content-wrapper {
                                        margin-left: 0 !important;
                                        padding: 10px !important;
                                    }

                                    .templatemo-sidebar {
                                        display: none !important;
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

                                    /* 이미지 크기 조정 */
                                    .table img {
                                        max-width: 100% !important;
                                        height: auto !important;
                                    }

                                    /* 라디오 버튼 크기 조정 */
                                    input[type="radio"] {
                                        transform: scale(1.2);
                                        margin: 5px;
                                    }

                                    /* 텍스트 크기 조정 */
                                    .table strong span {
                                        font-size: 10px !important;
                                    }

                                    /* 날짜 입력 필드 스타일 */
                                    input[type="date"] {
                                        text-align: center !important;
                                        font-size: 11px !important;
                                        padding: 4px 6px !important;
                                        border: 1px solid #ccc !important;
                                        border-radius: 3px !important;
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
                                }
                            </style>

                            <!-- 공통 세션 정보 템플릿 -->
                            <jsp:include page="/WEB-INF/jsp/common/session-info.jsp" />
                        </head>

                        <body>
                            <!-- jQuery 먼저 로딩 (의존성 해결) -->
                            <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>

                            <!-- 브라우저 확장 프로그램 에러 차단 (엣지/크롬 확장 프로그램 에러 무시) -->
                            <script>
                                (function () {
                                    'use strict';

                                    // 확장 프로그램 에러 무시
                                    var originalError = window.onerror;
                                    window.onerror = function (message, source, lineno, colno, error) {
                                        if (source && (source.includes('content.js') || source.includes('chrome-extension') || source.includes('moz-extension'))) {
                                            return true; // 에러 무시
                                        }
                                        if (originalError) {
                                            return originalError(message, source, lineno, colno, error);
                                        }
                                        return false;
                                    };

                                    // Promise rejection 에러 무시 (확장 프로그램 관련)
                                    window.addEventListener('unhandledrejection', function (event) {
                                        if (event.reason && (
                                            event.reason.name === 'i' ||
                                            event.reason.code === 403 ||
                                            (event.reason.message && event.reason.message.includes('not valid JSON'))
                                        )) {
                                            event.stopImmediatePropagation();
                                            event.preventDefault();
                                            return true;
                                        }
                                    }, true);
                                })();

                                // 로그아웃 함수
                                function logoutToLogin() {
                                    var currentUserId = $('#loginUserId').val() || $('#userId').val() || window.currentUserId || '';

                                    $.ajax({
                                        url: '/login/logoutProcess',
                                        type: 'POST',
                                        async: true,
                                        data: JSON.stringify({ userId: currentUserId }),
                                        contentType: 'application/json',
                                        success: function (response) {
                                            window.location.href = '/login/login';
                                        },
                                        error: function (xhr, status, error) {
                                            window.location.href = '/login/login';
                                        }
                                    });
                                }
                            </script>

                            <div class="navbar navbar-inverse" role="navigation" style="background-color: #ffffff">
                                <div class="navbar-header">
                                    <div class="logo">
                                        <h1><a href="javascript:PageNavigation.goMain();"><img src="/images/hntbi.png"
                                                    width="70" height="32"></a></h1>
                                    </div>
                                    <button type="button" class="navbar-toggle" data-toggle="collapse"
                                        data-target=".navbar-collapse" style="background-color: #cccccc">
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
                                        <li class="active"><a href="javascript:goMain();"><i
                                                    class="fa fa-home"></i>대시보드</a></li>
                                        <li class="sub open">
                                            <ul style="background-color: #afd9ee; height: 40px; padding-top: 10px">
                                                <strong>${userNm}님 안녕하세요.</strong>
                                            </ul>
                                            <c:if test="${userGrade eq 'A' || userGrade eq 'U'}">
                                                <a href="javascript:;">
                                                    <i class="fa fa-database"></i> 관리 메뉴보기 <div class="pull-right"><span
                                                            class="caret"></span></div>
                                                </a>
                                                <ul class="templatemo-submenu">
                                                    <li><a href="javascript:PageNavigation.goUserList();">사용자 관리</a>
                                                    </li>
                                                </ul>
                                                <ul class="templatemo-submenu">
                                                    <li><a href="javascript:PageNavigation.goCreateSub();">부계정 생성</a>
                                                    </li>
                                                </ul>
                                            </c:if>
                                        </li>
                                        <li><a href="" data-toggle="modal" data-target="#confirmModal"><i
                                                    class="fa fa-sign-out"></i>로그아웃</a></li>
                                    </ul>
                                </div><!--/.navbar-collapse -->

                                <div class="templatemo-content-wrapper" style="background-color: #333333">
                                    <div class="templatemo-content" style="background-color: #333333">
                                        <ol class="breadcrumb">
                                            <li><a href="#" onclick="goMain()">메인</a></li>
                                        </ol>
                                        <h1><span style="color: #f0f8ff; ">챠트 설정</span></h1>
                                        <p><span style="color: #f0f8ff; ">챠트 설정 화면입니다.</span></p>

                                        <div class="templatemo-panels">
                                            <div class="row">
                                                <div class="" style="margin-right: 10px; margin-left: 10px;">
                                                    <span class="btn btn-primary"><a
                                                            href="javascript:PageNavigation.goSensorSetting();">챠트설정</a></span>

                                                    <!-- Hidden inputs for session and sensor data -->
                                                    <input type="hidden" id="loginUserId" name="loginUserId"
                                                        value="${userId}">
                                                    <input type="hidden" id="userId" name="userId" value="${userId}">
                                                    <input type="hidden" id="sensorUuid" name="sensorUuid"
                                                        value="${sensorUuid}">
                                                    <input type="hidden" id="chartType" name="chartType"
                                                        value="${chartType}">
                                                    <input type="hidden" id="topicStr" name="topicStr"
                                                        value="${topicStr}">

                                                    <div class="panel panel-primary" width="100%">
                                                        <div class="panel-heading">챠트 종류</div>
                                                        <div class="panel-body">
                                                            <table class="table table-striped" width="100%">
                                                                <tbody>

                                                                    <tr>
                                                                        <td width="50%" align="center" valign="middle"
                                                                            style="background-color: #c7254e;">
                                                                            <strong><span
                                                                                    style="color: #f0f8ff; font-size:10pt;">Line
                                                                                    Chart</span></strong>
                                                                        </td>
                                                                        <td width="50%" align="center" valign="middle"
                                                                            style="background-color: #c7254e;">
                                                                            <strong><span
                                                                                    style="color: #f0f8ff; font-size:10pt;">Bar
                                                                                    Chart</span></strong>
                                                                        </td>
                                                                    </tr>
                                                                    <tr>
                                                                        <td width="50%" align="center" valign="middle">
                                                                            <img src="/images/linechart.svg" width="70%"
                                                                                height="75%">
                                                                        </td>
                                                                        <td width="50%" align="center" valign="middle">
                                                                            <img src="/images/barchart.svg" width="70%"
                                                                                height="75%">
                                                                        </td>
                                                                    </tr>
                                                                    <tr>
                                                                        <td width="50%" align="center" valign="middle">
                                                                            <input type="radio" id="line" name="chart"
                                                                                value="line"></td>
                                                                        <td width="50%" align="center" valign="middle">
                                                                            <input type="radio" id="bar" name="chart"
                                                                                value="bar"></td>
                                                                    </tr>
                                                                    <tr>
                                                                        <td colspan="2" align="center" valign="middle"
                                                                            style="background-color: #f8f9fa;">
                                                                            <strong><span
                                                                                    style="font-size:10pt;">사용안함</span></strong>
                                                                        </td>
                                                                    </tr>
                                                                    <tr>
                                                                        <td colspan="2" align="center" valign="middle">
                                                                            <input type="radio" id="none" name="chart"
                                                                                value="none">
                                                                        </td>
                                                                    </tr>


                                                                    <tr>
                                                                        <td colspan="2" align="center"
                                                                            style="padding-top: 15px; padding-bottom: 15px;">
                                                                            <strong><span
                                                                                    style="font-size: 12px;"><button
                                                                                        id="save" name="save">설정
                                                                                        저장</button></span></strong></td>
                                                                    </tr>
                                                                </tbody>
                                                            </table>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class="modal fade" id="confirmModal" tabindex="-1" role="dialog"
                                    aria-labelledby="myModalLabel" aria-hidden="true">
                                    <div class="modal-dialog">
                                        <div class="modal-content">
                                            <div class="modal-header">
                                                <button type="button" class="close" data-dismiss="modal"><span
                                                        aria-hidden="true">&times;</span><span
                                                        class="sr-only">Close</span></button>
                                                <h4 class="modal-title" id="myModalLabel">로그아웃 하시겠습니까?</h4>
                                            </div>
                                            <div class="modal-footer">
                                                <a href="javascript:logoutToLogin();" class="btn btn-primary">Yes</a>
                                                <button type="button" class="btn btn-default"
                                                    data-dismiss="modal">No</button>
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


                            <!-- 공통 에러 차단 시스템 -->
                            <script src="/js/error-blocking-system.js"></script>

                            <!-- 공통 유틸리티 파일들 -->
                            <!-- 통합 AJAX 및 검증 관리자 -->
                            <script src="/js/unified-ajax-manager.js"></script>
                            <script src="/js/unified-validation-manager.js"></script>

                            <!-- 기타 스크립트들 -->
                            <script src="/js/bootstrap.min.js"></script>
                            <script src="/js/templatemo_script.js"></script>
                            <script src="/js/session-manager.js"></script>
                            <script src="/js/heartbeat-manager.js"></script>
                            <script src="https://cdn.jsdelivr.net/npm/moment@2.29.1/min/moment.min.js"></script>
                            <script src="https://cdn.jsdelivr.net/npm/chart.js@2.9.4"></script>
                            <script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-streaming@1.9.0"></script>

                            <script>
                                function goMain() {
                                    // 공통 페이지 이동 함수 사용 (세션 기반)
                                    PageNavigation.goMain();
                                }

                                // 공통 알림 함수
                                function showError(message) {
                                    console.log('에러:', message);
                                    alert('오류: ' + message);
                                }

                                function showSuccess(message) {
                                    console.log('성공:', message);
                                    alert('성공: ' + message);
                                }

                                $('#save').click(function () {
                                    var userId = $('#loginUserId').val();
                                    var sensorId = $('#userId').val();
                                    var sensorUuid = $('#sensorUuid').val();

                                    var checked = $("input[name='chart']:checked").val();

                                    var sendData = {
                                        userId: userId,
                                        sensorId: sensorId,
                                        sensorUuid: sensorUuid,
                                        chartType: checked
                                    }

                                    $.ajax({
                                        url: '/admin/setChart',
                                        async: true,
                                        type: 'POST',
                                        data: JSON.stringify(sendData),
                                        dataType: 'json',
                                        contentType: 'application/json',
                                        success: function (result) {
                                            if (null != result) {
                                                if ("200" == result.resultCode) {
                                                    showSuccess("챠트 설정 저장 성공");
                                                } else {
                                                    showError("챠트 설정 저장 실패");
                                                }
                                            }
                                        },
                                        error: function (result) {
                                            showError("챠트 설정 저장 실패");
                                        },
                                        complete: function (result) {

                                        }
                                    });
                                });

                                // 차트 설정 페이지 뒤로가기 처리 설정
                                function setupChartSettingBackNavigation() {
                                    // 페이지 로드 시 히스토리 상태 추가
                                    history.pushState({ page: 'chartSetting' }, '차트설정', window.location.href);

                                    // 뒤로가기 시도 시 메인 페이지로 이동
                                    window.addEventListener('popstate', function (event) {
                                        console.log('차트 설정 페이지에서 뒤로가기 감지 - 메인 페이지로 이동');
                                        PageNavigation.goMain();
                                    });

                                    // 키보드 뒤로가기 단축키 처리
                                    document.addEventListener('keydown', function (event) {
                                        if (event.altKey && event.keyCode === 37) { // Alt + Left Arrow
                                            event.preventDefault();
                                            event.stopPropagation();
                                            console.log('Alt+Left 감지 - 메인 페이지로 이동');
                                            PageNavigation.goMain();
                                            return false;
                                        }
                                    });

                                    // 마우스 뒤로가기 버튼 처리
                                    document.addEventListener('mousedown', function (event) {
                                        if (event.button === 3 || event.button === 4) { // 뒤로가기/앞으로가기 버튼
                                            event.preventDefault();
                                            event.stopPropagation();
                                            console.log('마우스 뒤로가기 버튼 감지 - 메인 페이지로 이동');
                                            PageNavigation.goMain();
                                            return false;
                                        }
                                    });

                                    // 페이지 이탈 시 히스토리 정리
                                    window.addEventListener('beforeunload', function (event) {
                                        console.log('차트 설정 페이지 이탈 - 히스토리 정리');
                                        history.replaceState({ page: 'main' }, '메인', '/main/main');
                                    });
                                }

                                $(window).on({
                                    load: function () {
                                        $.fn.radioSelect = function (val) {
                                            this.each(function () {
                                                var $this = $(this);
                                                if ($this.val() == val)
                                                    $this.attr('checked', true);
                                            });
                                            return this;
                                        };

                                        var chartType;
                                        chartType = $('#chartType').val();

                                        if (chartType != "") {
                                            $(":radio[name='chart']").radioSelect(chartType);
                                        }

                                        // 차트 설정 페이지 뒤로가기 처리 설정 실행
                                        setupChartSettingBackNavigation();
                                    }
                                });

                            </script>

                        </body>

                        </html>