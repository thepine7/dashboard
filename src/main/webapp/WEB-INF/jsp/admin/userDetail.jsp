<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
        <%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
            <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
                <%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
                    <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
                        <% // 브라우저 캐시 무효화 response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate" );
                            response.setHeader("Pragma", "no-cache" ); response.setDateHeader("Expires", 0); %>

                            <!-- 공통 세션 정보 -->
                            <jsp:include page="/WEB-INF/jsp/common/session-info.jsp" />

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

                                <!-- PageNavigation 객체 및 공통 함수 정의 -->
                                <script>
                                    window.PageNavigation = {
                                        goMain: function () {
                                            console.log('PageNavigation.goMain() 호출됨');
                                            // Main 버튼 클릭 플래그 설정
                                            sessionStorage.setItem('mainButtonClicked', 'true');
                                            window.location.href = '/main/main';
                                        },
                                        goLogin: function () {
                                            console.log('PageNavigation.goLogin() 호출됨');
                                            window.location.href = '/login/login';
                                        },
                                        goUserList: function () {
                                            console.log('PageNavigation.goUserList() 호출됨');
                                            window.location.href = '/admin/userList';
                                        },
                                        goCreateSub: function () {
                                            console.log('PageNavigation.goCreateSub() 호출됨');
                                            window.location.href = '/admin/createSub';
                                        },
                                        goUserDetail: function (userId) {
                                            console.log('PageNavigation.goUserDetail() 호출됨:', userId);
                                            window.location.href = '/admin/userDetail?userId=' + userId;
                                        },
                                        goUserModify: function (userId) {
                                            console.log('PageNavigation.goUserModify() 호출됨:', userId);
                                            window.location.href = '/admin/userModify?userId=' + userId;
                                        },
                                        goBack: function () {
                                            console.log('PageNavigation.goBack() 호출됨');
                                            window.history.back();
                                        }
                                    };

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

                            </head>

                            <body>

                                <div class="navbar navbar-inverse" role="navigation" style="background-color: #ffffff">
                                    <div class="navbar-header">
                                        <div class="logo">
                                            <h1><a href="javascript:PageNavigation.goMain();"><img
                                                        src="/images/hntbi.png" width="70" height="32"></a></h1>
                                        </div>
                                        <button type="button" class="navbar-toggle" data-toggle="collapse"
                                            data-target=".navbar-collapse">
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
                                            <li class="active"><a href="#"><i class="fa fa-home"></i>대시보드</a></li>
                                            <li class="sub open">
                                                <c:if test="${userGrade eq 'A' || userGrade eq 'U'}">
                                                    <a href="javascript:;">
                                                        <i class="fa fa-database"></i> 관리 메뉴보기 <div class="pull-right">
                                                            <span class="caret"></span></div>
                                                    </a>
                                                    <ul class="templatemo-submenu">
                                                        <li><a href="javascript:PageNavigation.goUserList();">사용자 관리</a>
                                                        </li>
                                                    </ul>
                                                    <ul class="templatemo-submenu">
                                                        <li><a href="javascript:PageNavigation.goCreateSub();">부계정
                                                                생성</a></li>
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
                                                <li><a href="javascript:PageNavigation.goMain();">메인</a></li>
                                            </ol>
                                            <h1><span style="color: #f0f8ff; " id="pageTitle">사용자정보</span></h1>
                                            <p><span style="color: #f0f8ff; " id="pageDescription">사용자정보 화면입니다.</span>
                                            </p>

                                            <div class="templatemo-panels">
                                                <div class="row">
                                                    <div class="" style="margin-right: 10px; margin-left: 10px;">
                                                        <span class="btn btn-primary" id="pageButton">사용자정보</span>
                                                        <div class="panel panel-primary">
                                                            <div class="panel-heading">사용자 정보</div>
                                                            <div class="panel-body">

                                                                <table class="table table-striped">
                                                                    <thead>
                                                                        <tr>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #c7254e;">
                                                                                <strong><span
                                                                                        style="color: #f0f8ff; font-size:10pt;"
                                                                                        width="30%"
                                                                                        height="25">구분</span></strong>
                                                                            </td>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #c7254e;">
                                                                                <strong><span
                                                                                        style="color: #f0f8ff; font-size:10pt;"
                                                                                        width="70%"
                                                                                        height="25">정보</span></strong>
                                                                            </td>
                                                                        </tr>
                                                                    </thead>
                                                                    <tbody>
                                                                        <tr>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="30%" height="25"><strong><span
                                                                                        style="font-size:10pt;">아이디</span></strong>
                                                                            </td>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="70%" height="25"><strong><span
                                                                                        style="font-size:10pt;">${userInfo.userId}</span></strong>
                                                                            </td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="30%" height="25"><strong><span
                                                                                        style="font-size:10pt;">이름</span></strong>
                                                                            </td>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="70%" height="25"><strong><span
                                                                                        style="font-size:10pt;">${userInfo.userNm}</span></strong>
                                                                            </td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="30%" height="25"><strong><span
                                                                                        style="font-size:10pt;">전화번호</span></strong>
                                                                            </td>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="70%" height="25"><strong><span
                                                                                        style="font-size:10pt;">${userInfo.userTel}</span></strong>
                                                                            </td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="30%" height="25"><strong><span
                                                                                        style="font-size:10pt;">메일주소</span></strong>
                                                                            </td>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="70%" height="25"><strong><span
                                                                                        style="font-size:10pt;">${userInfo.userEmail}</span></strong>
                                                                            </td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="30%" height="25"><strong><span
                                                                                        style="font-size:10pt;">등급</span></strong>
                                                                            </td>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="70%" height="25">
                                                                                <strong><span style="font-size:10pt;">
                                                                                        <c:choose>
                                                                                            <c:when
                                                                                                test="${userInfo.userGrade eq 'U'}">
                                                                                                사용자</c:when>
                                                                                            <c:when
                                                                                                test="${userInfo.userGrade eq 'A'}">
                                                                                                관리자</c:when>
                                                                                            <c:when
                                                                                                test="${userInfo.userGrade eq 'B'}">
                                                                                                부계정</c:when>
                                                                                        </c:choose>
                                                                                    </span></strong>
                                                                            </td>
                                                                        </tr>
                                                                        <tr>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="30%" height="25"><strong><span
                                                                                        style="font-size:10pt;">주계정ID</span></strong>
                                                                            </td>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #ffffff;"
                                                                                width="70%" height="25"><strong><span
                                                                                        style="font-size:10pt;">${userInfo.parentUserId}</span></strong>
                                                                            </td>
                                                                        </tr>
                                                                    </tbody>
                                                                </table>
                                                            </div>

                                                            <div class="panel-heading">사용자 장치 목록</div>
                                                            <div class="panel-body">
                                                                <table class="table table-striped">
                                                                    <thead>
                                                                        <tr>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #c7254e;">
                                                                                <strong><span
                                                                                        style="color: #f0f8ff; font-size:10pt;"
                                                                                        width="30%"
                                                                                        height="25">장치명</span></strong>
                                                                            </td>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #c7254e;">
                                                                                <strong><span
                                                                                        style="color: #f0f8ff; font-size:10pt;"
                                                                                        width="30%"
                                                                                        height="25">장치고유ID</span></strong>
                                                                            </td>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #c7254e;">
                                                                                <strong><span
                                                                                        style="color: #f0f8ff; font-size:10pt;"
                                                                                        width="30%"
                                                                                        height="25">챠트유형</span></strong>
                                                                            </td>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #c7254e;">
                                                                                <strong><span
                                                                                        style="color: #f0f8ff; font-size:10pt;"
                                                                                        width="30%"
                                                                                        height="25">위치</span></strong>
                                                                            </td>
                                                                            <td align="center" valign="middle"
                                                                                style="background-color: #c7254e;">
                                                                                <strong><span
                                                                                        style="color: #f0f8ff; font-size:10pt;"
                                                                                        width="10%"
                                                                                        height="25">삭제</span></strong>
                                                                            </td>
                                                                        </tr>
                                                                    </thead>
                                                                    <tbody>
                                                                        <c:choose>
                                                                            <c:when test="${sensorList eq null}">
                                                                            </c:when>
                                                                            <c:otherwise>
                                                                                <c:forEach var="item"
                                                                                    items="${sensorList}">
                                                                                    <tr>
                                                                                        <td align="center"
                                                                                            valign="middle"
                                                                                            style="background-color: #ffffff;"
                                                                                            height="25"><strong><span
                                                                                                    style="font-size:10pt;">${item.sensor_name}</span></strong>
                                                                                        </td>
                                                                                        <td align="center"
                                                                                            valign="middle"
                                                                                            style="background-color: #ffffff;"
                                                                                            height="25"><strong><span
                                                                                                    style="font-size:10pt;">${item.sensor_uuid}</span></strong>
                                                                                        </td>
                                                                                        <td align="center"
                                                                                            valign="middle"
                                                                                            style="background-color: #ffffff;"
                                                                                            height="25"><strong><span
                                                                                                    style="font-size:10pt;">${item.chart_type}</span></strong>
                                                                                        </td>
                                                                                        <td align="center"
                                                                                            valign="middle"
                                                                                            style="background-color: #ffffff;"
                                                                                            height="25"><strong><span
                                                                                                    style="font-size:10pt;">${item.sensor_loc}</span></strong>
                                                                                        </td>
                                                                                        <td align="center"
                                                                                            valign="middle"
                                                                                            style="background-color: #ffffff;"
                                                                                            height="25">
                                                                                            <c:if
                                                                                                test="${userGrade eq 'A'}">
                                                                                                <button
                                                                                                    class="btn btn-danger btn-sm"
                                                                                                    onclick="deleteDevice('${item.sensor_uuid}', '${item.sensor_name}', '${userInfo.userId}')">삭제</button>
                                                                                            </c:if>
                                                                                        </td>
                                                                                    </tr>
                                                                                </c:forEach>
                                                                            </c:otherwise>
                                                                        </c:choose>
                                                                    </tbody>
                                                                </table>
                                                            </div>
                                                            <div>
                                                                <p align="center"><button id="modify" name="modify"
                                                                        style="width:100px; height:30px;"
                                                                        onclick="goUserModify('${userInfo.userId}');">정보수정</button>&nbsp;&nbsp;<button
                                                                        id="goList" name="goList"
                                                                        style="width:100px; height:30px;"
                                                                        onclick="goback();">이전으로</button></p>
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
                                                    <a href="javascript:logoutToLogin();"
                                                        class="btn btn-primary">Yes</a>
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
                                <!-- jQuery 먼저 로딩 (의존성 해결) -->
                                <script
                                    src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>

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
                                </script>

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
                                <script>
                                    function goMain() {
                                        // 공통 페이지 이동 함수 사용 (세션 기반)
                                        PageNavigation.goMain();
                                    }

                                    function goback() {
                                        // 공통 페이지 이동 함수 사용 (뒤로가기)
                                        PageNavigation.goBack();
                                    }

                                    function goUserModify(userId) {
                                        // POST 방식으로 정보수정 페이지 이동 (URL에 userId 노출 방지)
                                        var form = document.createElement('form');
                                        form.method = 'POST';
                                        form.action = '/admin/userModify';

                                        var input = document.createElement('input');
                                        input.type = 'hidden';
                                        input.name = 'userId';
                                        input.value = userId;

                                        form.appendChild(input);
                                        document.body.appendChild(form);
                                        form.submit();
                                    }

                                    // 장치 삭제 함수
                                    function deleteDevice(sensorUuid, sensorName, userId) {
                                        console.log('deleteDevice 호출됨:', { sensorUuid: sensorUuid, sensorName: sensorName, userId: userId });

                                        if (!confirm('장치 "' + sensorName + '"을(를) 삭제하시겠습니까?\n\n삭제 시 모든 센서 데이터가 영구적으로 삭제됩니다.')) {
                                            console.log('장치 삭제 취소됨.');
                                            return;
                                        }

                                        console.log('장치 삭제 확인됨 - AJAX 요청 시작');

                                        $.ajax({
                                            url: '/data/deleteSensorInfo',
                                            type: 'POST',
                                            contentType: 'application/json',
                                            data: JSON.stringify({
                                                userId: userId,
                                                sensorUuid: sensorUuid,
                                                sensorName: sensorName
                                            }),
                                            success: function (response) {
                                                console.log('장치 삭제 응답:', response);
                                                if (response.resultCode === '200') {
                                                    alert('장치가 성공적으로 삭제되었습니다.');
                                                    location.reload();
                                                } else {
                                                    alert('장치 삭제 실패: ' + response.resultMessage);
                                                }
                                            },
                                            error: function (xhr, status, error) {
                                                console.error('장치 삭제 AJAX 오류:', status, error);
                                                console.error('응답 텍스트:', xhr.responseText);
                                                console.error('상태 코드:', xhr.status);
                                                alert('장치 삭제 중 오류가 발생했습니다.\n상태: ' + status + '\n에러: ' + error);
                                            }
                                        });
                                    }

                                    function changeUserGrade() {
                                        var newGrade = $('#userGradeSelect').val();
                                        var targetUserId = '${userInfo.userId}';
                                        var currentGrade = '${userInfo.userGrade}';
                                        var gradeNames = {
                                            'A': '관리자',
                                            'U': '사용자',
                                            'B': '부계정'
                                        };

                                        if (newGrade === currentGrade) {
                                            alert("현재 등급과 동일합니다.");
                                            return;
                                        }

                                        var confirmMessage = "사용자 '" + targetUserId + "'의 등급을\n";
                                        confirmMessage += "현재: " + gradeNames[currentGrade] + " → 변경: " + gradeNames[newGrade] + "\n\n";
                                        confirmMessage += "변경하시겠습니까?";

                                        if (confirm(confirmMessage)) {
                                            var sendData = {
                                                targetUserId: targetUserId,
                                                newGrade: newGrade,
                                                adminUserId: '${loginUserId}'
                                            };

                                            // 버튼 비활성화
                                            $('#changeGrade').prop('disabled', true).text('처리중...');

                                            $.ajax({
                                                url: '/admin/changeUserGrade',
                                                async: true,
                                                type: 'POST',
                                                data: JSON.stringify(sendData),
                                                dataType: 'json',
                                                contentType: 'application/json',
                                                success: function (result) {
                                                    if (result.resultCode == "200") {
                                                        alert("사용자 등급 변경이 완료되었습니다.");
                                                        location.reload();
                                                    } else {
                                                        alert("사용자 등급 변경에 실패했습니다.\n" + (result.resultMsg || ""));
                                                        $('#changeGrade').prop('disabled', false).text('등급변경');
                                                    }
                                                },
                                                error: function (result) {
                                                    alert("사용자 등급 변경에 실패했습니다.");
                                                    $('#changeGrade').prop('disabled', false).text('등급변경');
                                                }
                                            });
                                        }
                                    }

                                    // 사용자 상세 페이지 뒤로가기 처리 설정
                                    function setupUserDetailBackNavigation() {
                                        // 페이지 로드 시 히스토리 상태 추가 (메모리 최적화)
                                        history.pushState({ page: 'userDetail' }, '사용자상세', window.location.href);

                                        // 뒤로가기 시도 시 메인 페이지로 이동
                                        window.addEventListener('popstate', function (event) {
                                            console.log('사용자 상세 페이지에서 뒤로가기 감지 - 메인 페이지로 이동');
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
                                            console.log('사용자 상세 페이지 이탈 - 히스토리 정리');
                                            history.replaceState({ page: 'main' }, '메인', '/main/main');
                                        });
                                    }

                                    // 사용자 상세 페이지 뒤로가기 처리 설정 실행
                                    setupUserDetailBackNavigation();

                                </script>
                            </body>

                            </html>