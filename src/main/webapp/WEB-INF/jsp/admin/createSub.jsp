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
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <link rel="stylesheet" href="/css/templatemo_main.css">
    <link rel="stylesheet" href="/css/common-buttons.css">
    <link rel="stylesheet" href="/css/responsive-common.css">
    
</head>
<body>
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
            <h1><span style="color: #f0f8ff; ">부계정 생성</span></h1>
            <p><span style="color: #f0f8ff; ">부계정 생성 화면입니다.</span></p>

            <div class="templatemo-panels">
                <div class="row">
                    <div class="" style="margin-right: 10px; margin-left: 10px;">
                        <span class="btn btn-primary">부계정 생성</span>
                        <div class="panel panel-primary">
                            <div class="panel-heading">정보 입력</div>
                            <div class="panel-body form-horizontal templatemo-signin-form">
                                <div class="form-group">
                                    <div class="col-md-12">
                                        <label for="subNm" class="col-sm-2 control-label">이름</label>
                                        <div class="col-sm-10">
                                            <input type="text" class="form-control" id="subNm" placeholder="이름">
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <div class="col-md-12">
                                        <label for="subId" class="col-sm-2 control-label">아이디</label>
                                        <div class="col-sm-10">
                                            <input type="text" class="form-control" id="subId" placeholder="사용자 아이디">
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <div class="col-md-12">
                                        <label for="subPass" class="col-sm-2 control-label">비밀번호</label>
                                        <div class="col-sm-10">
                                            <input type="text" class="form-control" id="subPass" placeholder="비밀번호">
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <div class="col-md-12">
                                        <label for="subUserTel" class="col-sm-2 control-label">전화번호</label>
                                        <div class="col-sm-10">
                                            <input type="text" class="form-control" id="subUserTel" placeholder="전화번호">
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <div class="col-md-12">
                                        <label for="subUserEmail" class="col-sm-2 control-label">메일주소</label>
                                        <div class="col-sm-10">
                                            <input type="text" class="form-control" id="subUserEmail" placeholder="메일주소">
                                        </div>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <div class="col-md-12">
                                        <div class="col-sm-offset-2 col-sm-10" align="right">
                                            <input type="submit" id="join" name="join" value="부계정생성" class="btn btn-default">
                                        </div>
                                    </div>
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
    // PageNavigation 객체 정의
    window.PageNavigation = {
        goMain: function() {
            console.log('PageNavigation.goMain() 호출됨');
            // Main 버튼 클릭 플래그 설정
            sessionStorage.setItem('mainButtonClicked', 'true');
            // 세션 기반 메인 페이지로 이동 (URL 파라미터 제거)
            window.location.href = '/main/main';
        },
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

    function goMain() {
        // 공통 페이지 이동 함수 사용 (세션 기반)
        window.PageNavigation.goMain();
    }

    // 로그아웃 함수
    function logoutToLogin() {
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
                window.location.href = '/login/login';
            }
        });
    }

    $(window).on({
        load: function() {
            var isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry/i.test(navigator.userAgent) ? true : false;

            $('#join').click(function() {
                //console.log("join");
                var subNm = $('#subNm').val();
                var subId = $('#subId').val();
                var subPass = $('#subPass').val();
                var userTel = $('#subUserTel').val();  // 수정: subUserTel로 변경
                var userEmail = $('#subUserEmail').val();  // 수정: subUserEmail로 변경
                
                console.log('부계정 생성 데이터:', {
                    subNm: subNm,
                    subId: subId,
                    userTel: userTel,
                    userEmail: userEmail
                });

                // 필수 입력 확인
                if(subId === '' || subPass === '' || subNm === '') {
                    alert("회원가입에 필요한 정보가 없습니다.");
                    return;
                }
                
		// 전화번호 형식 검증 및 통일 (10-11자리만 허용)
		if(userTel && userTel !== '') {
			// 하이픈 제거
			var cleanPhone = userTel.replace(/-/g, '');
			
			// 숫자만 있는지 확인 및 길이 체크 (10-11자리만 허용)
			if(!/^01[0-9]{8,9}$/.test(cleanPhone)) {
				alert("올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)\n10자리 또는 11자리여야 합니다.");
				return;
			}
			
			// 맨 뒤 4자리를 기준으로 형식 통일
			var len = cleanPhone.length;
			var firstPart = cleanPhone.substring(0, 3);  // 010, 011 등
			var lastPart = cleanPhone.substring(len - 4);  // 마지막 4자리
			var middlePart = cleanPhone.substring(3, len - 4);  // 중간 부분 (3-4자리)
			
			userTel = firstPart + '-' + middlePart + '-' + lastPart;
		}
                
                // 이메일 형식 검증
                var emailPattern = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/;
                if(userEmail && userEmail !== '' && !emailPattern.test(userEmail)) {
                    alert("올바른 이메일 형식이 아닙니다. (예: user@example.com)");
                    return;
                }

                var sendData = {
                    subNm: subNm,
                    subId: subId,
                    subPass: subPass,
                    userTel: userTel,
                    userEmail: userEmail
                    // userId 제거 - 백엔드에서 세션 값 사용
                }

                $.ajax({
                    url: '/admin/createSubProc',
                    async: true,
                    type: 'POST',
                    data: JSON.stringify(sendData),
                    dataType: 'json',
                    contentType: 'application/json',
                    success: function(result) {
                        if(result.resultCode == "200") {
                            alert("부계정 생성 성공");
                            PageNavigation.goMain();
                        } else {
                            alert(result.resultMessage || "부계정 생성 실패: 알 수 없는 오류");
                        }
                    },
                    error: function(xhr, status, error) {
                        console.error("부계정 생성 에러:", xhr, status, error);
                        alert("부계정 생성 중 서버 오류가 발생했습니다");
                    }
                });
            });
        }
    });

    function saveUserInfo(str) {
        window.hntInterface.saveUserInfo(str);
    }
    
    // 부계정 생성 페이지 뒤로가기 처리 설정
    function setupCreateSubBackNavigation() {
        // 페이지 로드 시 히스토리 상태 추가
        history.pushState({page: 'createSub'}, '부계정생성', window.location.href);
        
        // 뒤로가기 시도 시 메인 페이지로 이동
        window.addEventListener('popstate', function(event) {
            console.log('부계정 생성 페이지에서 뒤로가기 감지 - 메인 페이지로 이동');
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
            console.log('부계정 생성 페이지 이탈 - 히스토리 정리');
            history.replaceState({page: 'main'}, '메인', '/main/main');
        });
    }
    
    // 부계정 생성 페이지 뒤로가기 처리 설정 실행
    setupCreateSubBackNavigation();
</script>
</body>
</html>
