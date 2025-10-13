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
                window.location.href = '/admin/userDetail?userId=' + userId;
            },
            goUserModify: function(userId) {
                console.log('PageNavigation.goUserModify() 호출됨:', userId);
                window.location.href = '/admin/userModify?userId=' + userId;
            },
            goBack: function() {
                console.log('PageNavigation.goBack() 호출됨');
                window.history.back();
            }
        };
        
        function logoutToLogin() {
            console.log('logoutToLogin() 호출됨');
            window.location.href = '/login/logout';
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
                                      <h1><span style="color: #f0f8ff; " id="pageTitle">사용자정보</span></h1>
             <p><span style="color: #f0f8ff; " id="pageDescription">사용자정보 화면입니다.</span></p>

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
                                            <td align="center" valign="middle" style="background-color: #ffffff;" width="70%" height="25"><strong><span style="font-size:10pt;">${userInfo.userTel}</span></strong></td>
                                        </tr>
                                        <tr>
                                            <td align="center" valign="middle" style="background-color: #ffffff;" width="30%"  height="25"><strong><span style="font-size:10pt;">메일주소</span></strong></td>
                                            <td align="center" valign="middle" style="background-color: #ffffff;" width="70%" height="25"><strong><span style="font-size:10pt;">${userInfo.userEmail}</span></strong></td>
                                        </tr>
                                        <tr>
                                            <td align="center" valign="middle" style="background-color: #ffffff;" width="30%"  height="25"><strong><span style="font-size:10pt;">등급</span></strong></td>
                                            <td align="center" valign="middle" style="background-color: #ffffff;" width="70%" height="25">
                                                <strong><span style="font-size:10pt;">
                                                    <c:choose>
                                                        <c:when test="${userInfo.userGrade eq 'U'}">사용자</c:when>
                                                        <c:when test="${userInfo.userGrade eq 'A'}">관리자</c:when>
                                                        <c:when test="${userInfo.userGrade eq 'B'}">부계정</c:when>
                                                    </c:choose>
                                                </span></strong>
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
                                            <td align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;" width="30%" height="25">장치명</span></strong></td>
                                            <td align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;" width="30%" height="25">장치고유ID</span></strong></td>
                                            <td align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;" width="30%" height="25">챠트유형</span></strong></td>
                                            <td align="center" valign="middle" style="background-color: #c7254e;"><strong><span style="color: #f0f8ff; font-size:10pt;" width="30%" height="25">위치</span></strong></td>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:choose>
                                            <c:when test="${sensorList eq null}"></c:when>
                                            <c:otherwise>
                                                <c:forEach var="item" items="${sensorList}">
                                                    <tr>
                                                        <td align="center" valign="middle" style="background-color: #ffffff;" height="25"><strong><span style="font-size:10pt;">${item.sensor_name}</span></strong></td>
                                                        <td align="center" valign="middle" style="background-color: #ffffff;" height="25"><strong><span style="font-size:10pt;">${item.sensor_uuid}</span></strong></td>
                                                        <td align="center" valign="middle" style="background-color: #ffffff;" height="25"><strong><span style="font-size:10pt;">${item.chart_type}</span></strong></td>
                                                        <td align="center" valign="middle" style="background-color: #ffffff;" height="25"><strong><span style="font-size:10pt;">${item.sensor_loc}</span></strong></td>
                                                    </tr>
                                                </c:forEach>
                                            </c:otherwise>
                                        </c:choose>
                                    </tbody>
                                </table>
                            </div>
                            <div>
                                <p align="center"><button id="modify" name="modify" style="width:100px; height:30px;" onclick="modify();">정보수정</button>&nbsp;&nbsp;<button id="goList" name="goList" style="width:100px; height:30px;" onclick="goback();">이전으로</button></p>
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
<!-- jQuery 먼저 로딩 (의존성 해결) -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>

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
    function goMain() {
        // 공통 페이지 이동 함수 사용 (세션 기반)
        PageNavigation.goMain();
    }

    function goback() {
        // 공통 페이지 이동 함수 사용 (뒤로가기)
        PageNavigation.goBack();
    }

              function modify() {
         // 공통 페이지 이동 함수 사용 (AJAX 기반)
         PageNavigation.goUserModify('${userInfo.userId}');
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
        
        if(newGrade === currentGrade) {
            alert("현재 등급과 동일합니다.");
            return;
        }
        
        var confirmMessage = "사용자 '" + targetUserId + "'의 등급을\n";
        confirmMessage += "현재: " + gradeNames[currentGrade] + " → 변경: " + gradeNames[newGrade] + "\n\n";
        confirmMessage += "변경하시겠습니까?";
        
        if(confirm(confirmMessage)) {
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
                success: function(result) {
                    if(result.resultCode == "200") {
                        alert("사용자 등급 변경이 완료되었습니다.");
                        location.reload();
                    } else {
                        alert("사용자 등급 변경에 실패했습니다.\n" + (result.resultMsg || ""));
                        $('#changeGrade').prop('disabled', false).text('등급변경');
                    }
                },
                error: function(result) {
                    alert("사용자 등급 변경에 실패했습니다.");
                    $('#changeGrade').prop('disabled', false).text('등급변경');
                }
            });
        }
    }
    
    	// 사용자 상세 페이지 뒤로가기 처리 설정
	function setupUserDetailBackNavigation() {
		// 페이지 로드 시 히스토리 상태 추가 (메모리 최적화)
		history.pushState({page: 'userDetail'}, '사용자상세', window.location.href);
		
		// 뒤로가기 시도 시 메인 페이지로 이동
		window.addEventListener('popstate', function(event) {
			console.log('사용자 상세 페이지에서 뒤로가기 감지 - 메인 페이지로 이동');
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
			console.log('사용자 상세 페이지 이탈 - 히스토리 정리');
			history.replaceState({page: 'main'}, '메인', '/main');
		});
	}
	
	// 사용자 상세 페이지 뒤로가기 처리 설정 실행
	setupUserDetailBackNavigation();
    
    // 즉시 페이지 모드 설정 (페이지 로딩보다 빠름)
    (function() {
        // 세션에서 gu 파라미터 확인 (URL 파라미터 대신 세션 사용)
        var guParam = '${gu}';
        if(guParam === 'm') {
            console.log('정보수정 모드 감지됨');
            document.getElementById('pageTitle').textContent = '사용자정보수정';
            document.getElementById('pageDescription').textContent = '사용자정보수정 화면입니다.';
            document.getElementById('pageButton').textContent = '사용자정보수정';
        } else {
            console.log('일반 모드 감지됨');
            document.getElementById('pageTitle').textContent = '사용자정보';
            document.getElementById('pageDescription').textContent = '사용자정보 화면입니다.';
            document.getElementById('pageButton').textContent = '사용자정보';
        }
    })();
</script>
</body>
</html>
