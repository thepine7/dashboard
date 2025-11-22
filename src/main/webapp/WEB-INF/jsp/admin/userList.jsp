<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
        <%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
            <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
                <%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
                    <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

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
                                        // POST 방식으로 변경 (URL에 userId 노출 방지)
                                        var form = document.createElement('form');
                                        form.method = 'POST';
                                        form.action = '/admin/userDetail';

                                        var input = document.createElement('input');
                                        input.type = 'hidden';
                                        input.name = 'dtlUser';
                                        input.value = userId;

                                        form.appendChild(input);
                                        document.body.appendChild(form);
                                        form.submit();
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

                            <style>
                                /* 사용자 관리 테이블 하이라이트 효과 */
                                .user-table-row {
                                    transition: all 0.3s ease;
                                    cursor: pointer;
                                }

                                .user-table-row:hover {
                                    background-color: #e3f2fd !important;
                                    transform: translateY(-1px);
                                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
                                }

                                .user-table-row:hover td {
                                    background-color: #e3f2fd !important;
                                    color: #1976d2;
                                    font-weight: 600;
                                }

                                .user-table-row:hover .btn {
                                    background-color: #f44336;
                                    color: white;
                                    border-color: #f44336;
                                    transform: scale(1.05);
                                }

                                /* 삭제 버튼 스타일 개선 */
                                .delete-btn {
                                    transition: all 0.3s ease;
                                    border-radius: 4px;
                                    font-size: 11px;
                                    font-weight: 500;
                                }

                                .delete-btn:hover {
                                    background-color: #f44336 !important;
                                    color: white !important;
                                    border-color: #f44336 !important;
                                    transform: scale(1.05);
                                }

                                /* 테이블 헤더 스타일 */
                                .table thead tr {
                                    background: linear-gradient(135deg, #c7254e, #a0183a);
                                }

                                /* 테이블 전체 스타일 */
                                .table {
                                    border-radius: 8px;
                                    overflow: hidden;
                                    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                                    width: 100%;
                                    table-layout: fixed;
                                    max-width: 100%;
                                }

                                /* 테이블 컨테이너 */
                                .table-container {
                                    overflow-x: auto;
                                    width: 100%;
                                    min-width: 800px;
                                }

                                /* 컬럼 너비 최적화 */
                                .table td,
                                .table th {
                                    padding: 4px 2px;
                                    font-size: 8pt;
                                    white-space: nowrap;
                                    overflow: hidden;
                                    text-overflow: ellipsis;
                                }

                                /* 특정 컬럼 너비 조정 */
                                .table td:nth-child(1),
                                .table th:nth-child(1) {
                                    width: 15%;
                                }

                                /* 이름 */
                                .table td:nth-child(2),
                                .table th:nth-child(2) {
                                    width: 15%;
                                }

                                /* 아이디 */
                                .table td:nth-child(3),
                                .table th:nth-child(3) {
                                    width: 15%;
                                }

                                /* 주계정아이디 */
                                .table td:nth-child(4),
                                .table th:nth-child(4) {
                                    width: 8%;
                                }

                                /* 등급 */
                                .table td:nth-child(5),
                                .table th:nth-child(5) {
                                    width: 15%;
                                }

                                /* 로그인상태 */
                                .table td:nth-child(6),
                                .table th:nth-child(6) {
                                    width: 12%;
                                }

                                /* 삭제 */

                                /* 패널 여백 줄임 */
                                .panel-body {
                                    padding: 8px;
                                }

                                .panel-primary {
                                    margin-bottom: 15px;
                                }

                                .templatemo-panels {
                                    padding: 0 5px;
                                }
                            </style>

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
                            </script>

                            <div class="navbar navbar-inverse" role="navigation" style="background-color: #ffffff">
                                <div class="navbar-header">
                                    <div class="logo">
                                        <h1><a href="javascript:PageNavigation.goMain();"><img src="/images/hntbi.png"
                                                    width="70" height="32"></a></h1>
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
                                            <li><a href="javascript:PageNavigation.goMain();">메인</a></li>
                                        </ol>
                                        <h1><span style="color: #f0f8ff; ">사용자 관리</span></h1>
                                        <p><span style="color: #f0f8ff; ">사용자 관리 화면입니다.</span></p>

                                        <div class="templatemo-panels">
                                            <div class="row">
                                                <div class="" style="margin-right: 10px; margin-left: 10px;">
                                                    <span class="btn btn-primary"><a
                                                            href="javascript:PageNavigation.goUserList();">사용자
                                                            관리</a></span>
                                                    <div class="panel panel-primary">
                                                        <div class="panel-heading">사용자 목록</div>
                                                        <div class="panel-body">
                                                            <table class="table table-striped">
                                                                <thead>
                                                                    <tr>
                                                                        <td align="center" valign="middle"
                                                                            style="background-color: #c7254e;">
                                                                            <strong><span
                                                                                    style="color: #f0f8ff; font-size:10pt;"
                                                                                    height="25">이름</span></strong>
                                                                        </td>
                                                                        <td align="center" valign="middle"
                                                                            style="background-color: #c7254e;">
                                                                            <strong><span
                                                                                    style="color: #f0f8ff; font-size:10pt;"
                                                                                    height="25">아이디</span></strong>
                                                                        </td>
                                                                        <td align="center" valign="middle"
                                                                            style="background-color: #c7254e;">
                                                                            <strong><span
                                                                                    style="color: #f0f8ff; font-size:10pt;"
                                                                                    height="25">주계정아이디</span></strong>
                                                                        </td>
                                                                        <td align="center" valign="middle"
                                                                            style="background-color: #c7254e;">
                                                                            <strong><span
                                                                                    style="color: #f0f8ff; font-size:10pt;"
                                                                                    height="25">등급</span></strong>
                                                                        </td>
                                                                        <td align="center" valign="middle"
                                                                            style="background-color: #c7254e;">
                                                                            <strong><span
                                                                                    style="color: #f0f8ff; font-size:10pt;"
                                                                                    height="25">로그인상태</span></strong>
                                                                        </td>
                                                                        <td align="center" valign="middle"
                                                                            style="background-color: #c7254e;">
                                                                            <strong><span
                                                                                    style="color: #f0f8ff; font-size:10pt;"
                                                                                    height="25">삭제</span></strong>
                                                                        </td>
                                                                    </tr>
                                                                </thead>
                                                                <tbody id="userList">
                                                                    <c:choose>
                                                                        <c:when test="${userList eq null}">
                                                                            <tr>
                                                                                <td colspan="6">사용자가 없습니다.</td>
                                                                            </tr>
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            <c:forEach var="item" items="${userList}">
                                                                                <tr class="user-table-row">
                                                                                    <td onclick="userDetail('${item.userId}');"
                                                                                        align="center" valign="middle"
                                                                                        style="background-color: #ffffff;"
                                                                                        height="25"><strong><span
                                                                                                style="font-size:10pt;">${item.userNm}</span></strong>
                                                                                    </td>
                                                                                    <td onclick="userDetail('${item.userId}');"
                                                                                        align="center" valign="middle"
                                                                                        style="background-color: #ffffff;"
                                                                                        height="25"><strong><span
                                                                                                style="font-size:10pt;">${item.userId}</span></strong>
                                                                                    </td>
                                                                                    <td onclick="userDetail('${item.userId}');"
                                                                                        align="center" valign="middle"
                                                                                        style="background-color: #ffffff;"
                                                                                        height="25"><strong><span
                                                                                                style="font-size:10pt;">${item.mainId}</span></strong>
                                                                                    </td>
                                                                                    <td onclick="userDetail('${item.userId}');"
                                                                                        align="center" valign="middle"
                                                                                        style="background-color: #ffffff;"
                                                                                        height="25"><strong><span
                                                                                                style="font-size:10pt;">${item.userGrade}</span></strong>
                                                                                    </td>
                                                                                    <td onclick="userDetail('${item.userId}');"
                                                                                        align="center" valign="middle"
                                                                                        style="background-color: #ffffff;"
                                                                                        height="25">
                                                                                        <c:choose>
                                                                                            <c:when
                                                                                                test="${item.loginYn eq '활성'}">
                                                                                                <c:set var="statusColor"
                                                                                                    value="#4CAF50" />
                                                                                            </c:when>
                                                                                            <c:otherwise>
                                                                                                <c:set var="statusColor"
                                                                                                    value="#9e9e9e" />
                                                                                            </c:otherwise>
                                                                                        </c:choose>
                                                                                        <strong>
                                                                                            <span
                                                                                                style="font-size:10pt; padding: 4px 8px; border-radius: 4px; color: white; font-weight: bold; background-color: ${statusColor};">
                                                                                                ${item.loginYn}
                                                                                            </span>
                                                                                        </strong>
                                                                                    </td>
                                                                                    <td align="center" valign="middle"
                                                                                        style="background-color: #f0f0f0;"
                                                                                        height="25">
                                                                                        <strong>
                                                                                            <span
                                                                                                style="font-size:10pt;">
                                                                                                <c:choose>
                                                                                                    <c:when
                                                                                                        test="${userGrade eq 'A' && item.userId eq loginUserId}">
                                                                                                        <!-- A등급 사용자 자기 자신: "---" 텍스트 직접 표시 -->
                                                                                                        <span
                                                                                                            style="color: #666666; font-style: italic;">---</span>
                                                                                                    </c:when>
                                                                                                    <c:otherwise>
                                                                                                        <button
                                                                                                            id="delete"
                                                                                                            name="delete"
                                                                                                            class="delete-btn"
                                                                                                            style="width:50px; height:30px;"
                                                                                                            onclick="deleteUser('${item.userId}');">삭제</button>
                                                                                                    </c:otherwise>
                                                                                                </c:choose>
                                                                                            </span>
                                                                                        </strong>
                                                                                    </td>
                                                                                </tr>
                                                                            </c:forEach>
                                                                        </c:otherwise>
                                                                    </c:choose>
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

                            <script src="/js/bootstrap.min.js"></script>
                            <script src="/js/templatemo_script.js"></script>
                            <script src="/js/session-manager.js"></script>
                            <script src="/js/heartbeat-manager.js"></script>
                            <script>
                                // 성공/에러 메시지 표시 함수
                                function showSuccess(message) {
                                    console.log('성공:', message);
                                    alert(message);
                                }

                                function showError(message) {
                                    console.log('에러:', message);
                                    alert('오류: ' + message);
                                }

                                function goMain() {
                                    // 공통 페이지 이동 함수 사용 (세션 기반)
                                    PageNavigation.goMain();
                                }

                                function deleteUser(userId) {

                                    var result = confirm("삭제하시겠습니까?");

                                    if (result) {
                                        var sendData = {
                                            userId: userId
                                        }

                                        $.ajax({
                                            url: '/admin/deleteUser',
                                            async: true,
                                            type: 'POST',
                                            data: JSON.stringify(sendData),
                                            dataType: 'json',
                                            contentType: 'application/json',
                                            success: function (result) {
                                                if (result.resultCode == "200") {
                                                    showSuccess("삭제되었습니다.");
                                                    PageNavigation.goUserList();
                                                } else {
                                                    showError("삭제 실패하였습니다.");
                                                }
                                            },
                                            error: function (result) {
                                                showError("삭제 과정에서 오류가 발생되었습니다.");
                                            }
                                        });
                                    } else {

                                    }
                                }

                                function getChangeList() {
                                    var sendData = {
                                        userId: window.currentUserId || $('#userId').val(),
                                        userGrade: window.currentUserGrade || $('#userGrade').val()
                                    }

                                    $.ajax({
                                        url: '/admin/getChangeList',
                                        async: true,
                                        type: 'POST',
                                        data: JSON.stringify(sendData),
                                        dataType: 'json',
                                        contentType: 'application/json',
                                        success: function (result) {
                                            if (result.resultCode == "200") {
                                                // 디버깅: thepine 사용자의 loginYn 값 콘솔 출력
                                                var thepineUser = result.userList.find(function (user) {
                                                    return user.userId === 'thepine';
                                                });
                                                if (thepineUser) {
                                                    console.log('=== getChangeList 응답에서 thepine 사용자 ===');
                                                    console.log('userId:', thepineUser.userId);
                                                    console.log('loginYn:', thepineUser.loginYn);
                                                    console.log('loginDtm:', thepineUser.loginDtm);
                                                    console.log('logoutDtm:', thepineUser.logoutDtm);
                                                }

                                                var html = "";
                                                for (var i = 0; i < result.userList.length; i++) {
                                                    html += "<tr class='user-table-row'>";
                                                    html += "<td onclick=userDetail('" + result.userList[i].userId + "'); align='center' valign='middle' style='background-color: #ffffff;' height='25'><strong><span style='font-size:10pt;'>" + result.userList[i].userNm + "</span></strong></td>";
                                                    html += "<td onclick=userDetail('" + result.userList[i].userId + "'); align='center' valign='middle' style='background-color: #ffffff;' height='25'><strong><span style='font-size:10pt;'>" + result.userList[i].userId + "</span></strong></td>";
                                                    html += "<td onclick=userDetail('" + result.userList[i].userId + "'); align='center' valign='middle' style='background-color: #ffffff;' height='25'><strong><span style='font-size:10pt;'>" + result.userList[i].mainId + "</span></strong></td>";
                                                    html += "<td onclick=userDetail('" + result.userList[i].userId + "'); align='center' valign='middle' style='background-color: #ffffff;' height='25'><strong><span style='font-size:10pt;'>" + result.userList[i].userGrade + "</span></strong></td>";
                                                    // 활동 상태에 따른 스타일 적용
                                                    var statusStyle = result.userList[i].loginYn === '활성' ?
                                                        "background-color: #4CAF50;" : "background-color: #9e9e9e;";
                                                    html += "<td onclick=userDetail('" + result.userList[i].userId + "'); align='center' valign='middle' style='background-color: #ffffff;' height='25'>";
                                                    html += "<strong><span style='font-size:10pt; padding: 4px 8px; border-radius: 4px; color: white; font-weight: bold; " + statusStyle + "'>" + result.userList[i].loginYn + "</span></strong></td>";

                                                    // A등급 관리자가 자신을 삭제하지 못하도록 조건부 버튼 생성
                                                    var currentUserId = $('#loginUserId').val();
                                                    var currentUserGrade = $('#userGrade').val();
                                                    if (currentUserGrade === 'A' && result.userList[i].userId === currentUserId) {
                                                        // A등급 사용자 자기 자신: "---" 텍스트 표시
                                                        html += "<td align='center' valign='middle' style='background-color: #f0f0f0;' height='25'><strong><span style='font-size:10pt; color: #666666; font-style: italic;'>---</span></strong></td>";
                                                    } else {
                                                        html += "<td align='center' valign='middle' style='background-color: #ffffff;' height='25'><strong><span style='font-size:10pt;'><button id='delete' name='delete' class='delete-btn' style='width:50px; height:30px;' onclick=deleteUser('" + result.userList[i].userId + "');>삭제</button></span></strong></td>";
                                                    }
                                                    html += "</tr>";
                                                }
                                                $('#userList').html(html);
                                            } else {
                                                console.log(result.resultCode);
                                            }
                                        },
                                        error: function (result) {
                                            console.log("Error : " + result);
                                        }
                                    });
                                }

                                function userDetail(userId) {
                                    // 공통 페이지 이동 함수 사용 (AJAX 기반)
                                    PageNavigation.goUserDetail(userId);
                                }

                                // 더 정확한 모바일/웹뷰 감지 함수
                                function isMobileOrWebview() {
                                    // 1. User Agent 체크
                                    var userAgent = navigator.userAgent.toLowerCase();
                                    var isMobileUA = /mobile|android|iphone|ipad|ipod|blackberry|windows phone/i.test(userAgent);

                                    // 2. 화면 크기 체크 (더 엄격한 기준)
                                    var isSmallScreen = window.innerWidth <= 768;

                                    // 3. 터치 지원 체크
                                    var isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;

                                    // 4. 웹뷰 특수성 체크
                                    var isWebview = /wv|webview|mobile safari/i.test(userAgent);

                                    console.log("=== 모바일/웹뷰 감지 상세 정보 ===");
                                    console.log("User Agent:", userAgent);
                                    console.log("Mobile UA:", isMobileUA);
                                    console.log("화면 너비:", window.innerWidth);
                                    console.log("작은 화면:", isSmallScreen);
                                    console.log("터치 지원:", isTouchDevice);
                                    console.log("웹뷰:", isWebview);

                                    // 모바일/웹뷰로 판단하는 조건들
                                    var result = isMobileUA || isSmallScreen || isTouchDevice || isWebview;
                                    console.log("최종 결과:", result);

                                    return result;
                                }

                                window.onload = function () {
                                    // 5초 후 시작, 이후 5초마다 사용자 목록 갱신
                                    setTimeout(() => {
                                        setInterval(getChangeList, 5000);
                                    }, 5000);
                                };

                                // 사용자 목록 페이지 뒤로가기 처리 설정
                                function setupUserListBackNavigation() {
                                    // 페이지 로드 시 히스토리 상태 추가
                                    history.pushState({ page: 'userList' }, '사용자목록', window.location.href);

                                    // 뒤로가기 시도 시 메인 페이지로 이동
                                    window.addEventListener('popstate', function (event) {
                                        console.log('사용자 목록 페이지에서 뒤로가기 감지 - 메인 페이지로 이동');
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
                                        console.log('사용자 목록 페이지 이탈 - 히스토리 정리');
                                        history.replaceState({ page: 'main' }, '메인', '/main/main');
                                    });
                                }

                                // 사용자 목록 페이지 뒤로가기 처리 설정 실행
                                setupUserListBackNavigation();

                                // 페이지 포커스 감지 (사용자가 다른 탭으로 이동하는 경우)
                                var isPageActive = true;
                                var lastActivityTime = Date.now();

                                document.addEventListener('visibilitychange', function () {
                                    if (document.hidden) {
                                        // 페이지가 숨겨졌을 때 (다른 탭으로 이동)
                                        isPageActive = false;
                                        console.log('페이지 비활성화됨');
                                    } else {
                                        // 페이지가 다시 활성화되었을 때
                                        isPageActive = true;
                                        lastActivityTime = Date.now();
                                        console.log('페이지 활성화됨');
                                    }
                                });

                                // 주기적으로 페이지 활성 상태 확인 (5분마다)
                                setInterval(function () {
                                    if (!isPageActive) {
                                        // 페이지가 5분 이상 비활성 상태면 로그아웃 처리
                                        var xhr = new XMLHttpRequest();
                                        xhr.open('POST', '/login/logoutProcess', false);
                                        xhr.setRequestHeader('Content-Type', 'application/json');

                                        var logoutData = {
                                            userId: $('#loginUserId').val()
                                        };

                                        try {
                                            xhr.send(JSON.stringify(logoutData));
                                            console.log('페이지 비활성 상태로 인한 로그아웃 처리 완료');
                                        } catch (e) {
                                            console.log('비활성 상태 로그아웃 처리 실패:', e);
                                        }
                                    }
                                }, 300000); // 5분 (300초)
                            </script>
                        </body>

                        </html>