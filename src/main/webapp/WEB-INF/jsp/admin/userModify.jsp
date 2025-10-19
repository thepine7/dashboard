<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%
    // 브라우저 캐시 무효화
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
%>

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
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <link rel="stylesheet" href="/css/templatemo_main.css">
    <link rel="stylesheet" href="/css/common-buttons.css">
    <link rel="stylesheet" href="/css/responsive-common.css">
    
    <!-- 공통 세션 정보 템플릿 -->
    <jsp:include page="/WEB-INF/jsp/common/session-info.jsp" />
    
    <!-- PageNavigation 객체 및 공통 함수 정의 -->
    <script>
        window.PageNavigation = {
            goMain: function() {
                console.log('PageNavigation.goMain() 호출됨');
                window.location.href = '/main/main';
            },
            goLogin: function() {
                console.log('PageNavigation.goLogin() 호출됨');
                window.location.href = '/login/login';
            },
            goUserList: function() {
                console.log('PageNavigation.goUserList() 호출됨');
                window.location.href = '/admin/userList';
            },
            goCreateSub: function() {
                console.log('PageNavigation.goCreateSub() 호출됨');
                window.location.href = '/admin/createSub';
            },
            goUserDetail: function(userId) {
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
            goBack: function() {
                console.log('PageNavigation.goBack() 호출됨');
                window.history.back();
            }
        };
        
        function logoutToLogin() {
            console.log('logoutToLogin() 호출됨');
            window.location.href = '/login/login';
        }
    </script>
</head>
<body>

<div class="navbar navbar-inverse" role="navigation" style="background-color: #ffffff">
    <div class="navbar-header">
        		<div class="logo"><h1><a href="javascript:PageNavigation.goMain();"><img src="/images/hntbi.png" width="70" height="32"></a></h1></div>
        <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
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
                <a href="javascript:;">
                    <i class="fa fa-database"></i> 관리 메뉴보기 <div class="pull-right"><span class="caret"></span></div>
                </a>
                <ul class="templatemo-submenu">
                    <li><a href="javascript:PageNavigation.goUserList();">사용자 관리</a></li>
                </ul>
                <ul class="templatemo-submenu">
                    <li><a href="javascript:PageNavigation.goCreateSub();">부계정 생성</a></li>
                </ul>
            </li>
            <li><a href="" data-toggle="modal" data-target="#confirmModal"><i class="fa fa-sign-out"></i>로그아웃</a></li>
        </ul>
    </div><!--/.navbar-collapse -->

    <div class="templatemo-content-wrapper" style="background-color: #333333">
        <div class="templatemo-content" style="background-color: #333333">
            <ol class="breadcrumb">
                <li><a href="javascript:PageNavigation.goMain();">Main</a></li>
            </ol>
            <h1><span style="color: #f0f8ff; ">사용자 정보수정</span></h1>
            <p><span style="color: #f0f8ff; ">사용자 정보수정 화면입니다.</span></p>

            <div class="templatemo-panels">
                <div class="row">
                    <div class="" style="margin-right: 10px; margin-left: 10px;">
                        <span class="btn btn-primary">사용자 정보수정</span>
                        <div class="panel panel-primary">
                            <div class="panel-heading">사용자 정보수정</div>
                            <div class="panel-body">
                                <table class="table table-striped">
                                    <thead>
                                    <tr>
                                        <td align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;" width="30%" height="25">구분</span></strong></td>
                                        <td align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;" width="70%" height="25">정보</span></strong></td>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <tr>
                                        <td align="center" valign="middle" style="background-color: #ffffff;" width="30%"  height="25"><strong><span style="font-size:10pt;">아이디</span></strong></td>
                                        <td align="center" valign="middle" style="background-color: #ffffff;" width="70%" height="25"><strong><span style="font-size:10pt;">${userInfo.userId}</span></strong></td>
                                    </tr>
                                    <tr>
                                        <td align="center" valign="middle" style="background-color: #ffffff;" width="30%"  height="25"><strong><span style="font-size:10pt;">이름</span></strong></td>
                                        <td align="center" valign="middle" style="background-color: #ffffff;" width="70%" height="25"><strong><span style="font-size:10pt;">${userInfo.userNm}</span></strong></td>
                                    </tr>
                                    <tr>
                                        <td align="center" valign="middle" style="background-color: #ffffff;" width="30%"  height="25"><strong><span style="font-size:10pt;">전화번호</span></strong></td>
                                        <td align="center" valign="middle" style="background-color: #ffffff;" width="70%" height="25"><input type="text" id="userTel" name="userTel" value="${userInfo.userTel}" autocomplete="off" style="font-size:10pt;"></td>
                                    </tr>
                                    <tr>
                                        <td align="center" valign="middle" style="background-color: #ffffff;" width="30%"  height="25"><strong><span style="font-size:10pt;">메일주소</span></strong></td>
                                        <td align="center" valign="middle" style="background-color: #ffffff;" width="70%" height="25"><input type="text" id="userEmail" name="userEmail" value="${userInfo.userEmail}" autocomplete="off" style="font-size:10pt;"></td>
                                    </tr>
                                    <tr>
                                        <td align="center" valign="middle" style="background-color: #ffffff;" width="30%"  height="25"><strong><span style="font-size:10pt;">등급</span></strong></td>
                                        <td align="center" valign="middle" style="background-color: #ffffff;" width="70%" height="25">
                                            <select id="grade" name="grade">
                                                <option value="">선택</option>
                                                <option value="U">사용자</option>
                                                <option value="A">관리자</option>
                                                <option value="B">부계정</option>
                                            </select>
                                        </td>
                                    </tr>
                                    </tbody>
                                </table>
                            </div>

                            <div>
                                <p align="center"><button id="modify" name="modify" style="width:100px; height:30px;" onclick="modify();">확인</button>&nbsp;&nbsp;<button id="cancel" name="cancel" style="width:100px; height:30px;" onclick="cancel();">취소</button></p>
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
                    <a href="/login/login" class="btn btn-primary">Yes</a>
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

<!-- jQuery 먼저 로딩 (의존성 해결) -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>

<!-- 브라우저 확장 프로그램 에러 차단 (엣지/크롬 확장 프로그램 에러 무시) -->
<script>
(function() {
    'use strict';
    
    // 확장 프로그램 에러 무시
    var originalError = window.onerror;
    window.onerror = function(message, source, lineno, colno, error) {
        if (source && (source.includes('content.js') || source.includes('chrome-extension') || source.includes('moz-extension'))) {
            return true; // 에러 무시
        }
        if (originalError) {
            return originalError(message, source, lineno, colno, error);
        }
        return false;
    };
    
    // Promise rejection 에러 무시 (확장 프로그램 관련)
    window.addEventListener('unhandledrejection', function(event) {
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
<script>
    // 페이지 로딩 시 등급 선택 초기화
    $(document).ready(function() {
        // 등급 선택
        var initialGrade = '${userInfo.userGrade}';
        $('#grade').val(initialGrade);
    });

    function goMain() {
        // 공통 페이지 이동 함수 사용 (세션 기반)
        PageNavigation.goMain();
    }

    function modify() {
        console.log("=== modify() 함수 시작 ===");
        
        var userId = '${userInfo.userId}';
        
        // DOM에서 직접 값 읽기 전 디버깅
        console.log("DOM 요소 확인:");
        console.log("$('#userTel') 요소:", $('#userTel'));
        console.log("$('#userTel').val():", $('#userTel').val());
        console.log("$('#userEmail').val():", $('#userEmail').val());
        console.log("$('#grade').val():", $('#grade').val());
        
        // DOM에서 직접 값 읽기 (단일 소스)
        var userTel = $('#userTel').val().trim();
        var userEmail = $('#userEmail').val().trim();
        var userGrade = $('#grade').val();
        
        console.log("읽어온 값:");
        console.log("userId:", userId);
        console.log("userTel:", userTel);
        console.log("userEmail:", userEmail);
        console.log("userGrade:", userGrade);
        
        // 검증
        if(!userTel || userTel.length < 10) {
            alert("핸드폰 번호가 잘못되었습니다.");
            return false;
        }
        
        if(!userEmail || userEmail.indexOf("@") < 0) {
            alert("메일 주소가 잘못되었습니다.");
            return false;
        }
        
        if(!userGrade) {
            alert("회원 등급을 선택해주세요.");
            return false;
        }

        var sendData = {
            userId: userId,
            userEmail: userEmail,
            userTel: userTel,
            userGrade: userGrade
        };
        
        console.log("=== 사용자 정보 수정 요청 ===");
        console.log("전송 데이터:", sendData);

        $.ajax({
            url: '/admin/modifyUser',
            type: 'POST',
            data: JSON.stringify(sendData),
            dataType: 'json',
            contentType: 'application/json',
            success: function(result) {
                console.log("=== 수정 응답 ===");
                console.log("resultCode:", result.resultCode);
                console.log("resultMessage:", result.resultMessage);
                
                if(result.resultCode == "200") {
                    alert("정보 수정 완료");
                    PageNavigation.goUserDetail(userId);
                } else {
                    alert("정보 수정 실패: " + (result.resultMessage || "알 수 없는 오류"));
                }
            },
            error: function(xhr, status, error) {
                console.error("=== 수정 에러 ===");
                console.error("status:", status);
                console.error("error:", error);
                console.error("xhr:", xhr);
                alert("정보 수정 실패: " + error);
            }
        });
    }

    function cancel() {
        // 취소 시 메인 페이지로 이동
        PageNavigation.goMain();
    }

    // 사용자 수정 페이지 뒤로가기 처리 설정
    function setupUserModifyBackNavigation() {
        // 페이지 로드 시 히스토리 상태 추가
        history.pushState({page: 'userModify'}, '사용자수정', window.location.href);
        
        // 뒤로가기 시도 시 메인 페이지로 이동
        window.addEventListener('popstate', function(event) {
            console.log('사용자 수정 페이지에서 뒤로가기 감지 - 메인 페이지로 이동');
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
        
        // 페이지 이탈 시 히스토리 정리
        window.addEventListener('beforeunload', function(event) {
            console.log('사용자 수정 페이지 이탈 - 히스토리 정리');
            history.replaceState({page: 'main'}, '메인', '/main/main');
        });
    }
    
    $(window).on({
       load: function() {
           var userGrade = '${userInfo.userGrade}';

           if(userGrade) {
               if (userGrade == "U") {
                   $('#grade').val('U').prop("selected", true);
               } else if (userGrade == "A") {
                   $('#grade').val('A').prop("selected", true);
               }
           }
           
           // 사용자 수정 페이지 뒤로가기 처리 설정 실행
           setupUserModifyBackNavigation();
       }
    });
</script>
</body>
</html>
