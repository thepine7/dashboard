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
        <title>H&T Technology</title>
        <meta name="keywords" content="" />
        <meta name="description" content="" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <link rel="stylesheet" href="/css/templatemo_main.css">
        <link rel="stylesheet" href="/css/common-buttons.css">
        <link rel="stylesheet" href="/css/responsive-common.css">
            
        <!-- 회원가입 페이지는 세션 정보가 없어야 하므로 session-info.jsp 제외 -->

    <!-- 로컬 jQuery 사용 -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
        
        <!-- 공통 에러 차단 시스템 -->
        <script src="/js/error-blocking-system.js"></script>
        
        <!-- 공통 유틸리티 파일들 -->
        <!-- 통합 AJAX 및 검증 관리자 -->
        <script src="/js/unified-ajax-manager.js"></script>
        <script src="/js/unified-validation-manager.js"></script>
        
        <script>
        $(window).on({
			   load: function() {

                   var isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry/i.test(navigator.userAgent) ? true : false;

			       // 회원가입
			       $('#join').click(function() {
			           //console.log("join");
			           var userNm = $('#userNm').val();
			           var userId = $('#userId').val();
			           var userPass = $('#userPass').val();
			           var userTel = $('#userTel').val();
			           var userEmail = $('#userEmail').val();
			           var result = "";
			
			           if(userId !== '' && userPass !== '' && userNm !== '') {
			               var sendData = {
			                   userNm: userNm,
			                   userId: userId,
			                   userPass: userPass,
			                   userTel: userTel,
			                   userEmail: userEmail
			               }
			
			               $.ajax({
			                   url: '/login/joinProcess',
			                   async: true,
			                   type: 'POST',
			                   data: JSON.stringify(sendData),
			                   dataType: 'json',
			                   contentType: 'application/json',
			                   success: function(result) {
			                       if(result.resultCode == "200") {
			                           showSuccess("회원가입 성공");
                                       if(isMobile) { saveUserInfo(userId);}
			                           if (typeof PageNavigation !== 'undefined' && PageNavigation.goMain) {
			                               PageNavigation.goMain();
			                           } else {
			                               location.href = '/main/main';
			                           }
			                       } else {
			                           showError("회원가입 실패");
			                       }
			                   },
			                   error: function(result) {
			                       showError("회원가입 실패");
			                       PageNavigation.goLogin();
			                   },
			                   complete: function(result) {
			                       PageNavigation.goLogin();
			                   }
			               });
			           } else {
			               showWarning("회원가입에 필요한 정보가 없습니다.");
			           }
			       });
			
			       $('#joinBtn').click(function() {
			          PageNavigation.goLogin();
			       });
			
			       // 로그인
			       $('#login').click(function() {
			           //console.log("login");
			           var userId = $('#userId').val();
			           var userPass = $('#userPass').val();
			           var saveId = $('#saveId').is(':checked') ? 'Y' : 'N'; // 체크박스 체크 상태 확인
			           var result = "";
			           //console.log("userId : " + userId);
			           //console.log("userPass : " + userPass);
			           console.log("saveId 설정: " + saveId); // 디버깅용 로그 추가

			           if(userId !== '' && userPass !== '') {
			               var sendData = {
			                   userId: userId,
			                   userPass: userPass,
			                   saveId: saveId
			               }
			
			               $.ajax({
			                   url: '/login/loginProcess',
			                   async: true,
			                   type: 'POST',
			                   data: JSON.stringify(sendData),
			                   dataType: 'json',
			                   contentType: 'application/json',
			                   success: function(result) {
			                       if(result.resultCode == "200") {
			                           showSuccess("로그인 성공");
			                           saveUserInfo(userId);
			                           if (typeof PageNavigation !== 'undefined' && PageNavigation.goMain) {
			                               PageNavigation.goMain();
			                           } else {
			                               location.href = '/main/main';
			                           }
			                       } else {
			                           showError("로그인 실패");
			                       }
			                   },
			                   error: function(result) {
			                       showError("로그인 실패");
			                       PageNavigation.goLogin();
			                   }
			               });
			           } else {
			               showWarning("로그인에 필요한 정보가 없습니다.");
			           }
			       });
			   }
			});

			function saveUserInfo(str) {
        		window.hntInterface.saveUserInfo(str);
        	}
        </script>
    </head>

    <body style="background-color: #ffffff">
        <div class="navbar navbar-inverse" role="navigation">
            <div class="navbar-header">
                <div class="logo"><h1><a href="javascript:if(typeof PageNavigation !== 'undefined' && PageNavigation.goMain) { PageNavigation.goMain(); } else { location.href = '/main'; }">H&T Solutions</a></h1></div>
            </div>
        </div>

        <div class="template-page-wrapper">
            <div class="form-horizontal templatemo-signin-form">
                <div>
                    		<p align="center"><img src="/images/hntbi.png" width="200" height="90"></p>
                    <br/><br/>
                </div>
                <div class="form-group">
                    <div class="col-md-12">
                        <label for="userNm" class="col-sm-2 control-label">이름</label>
                        <div class="col-sm-10">
                            <input type="text" class="form-control" id="userNm" placeholder="이름">
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <div class="col-md-12">
                        <label for="userId" class="col-sm-2 control-label">아이디</label>
                        <div class="col-sm-10">
                            <input type="text" class="form-control" id="userId" placeholder="사용자 아이디">
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <div class="col-md-12">
                        <label for="userPass" class="col-sm-2 control-label">비밀번호</label>
                        <div class="col-sm-10">
                            <input type="text" class="form-control" id="userPass" placeholder="비밀번호">
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <div class="col-md-12">
                        <label for="userTel" class="col-sm-2 control-label">전화번호</label>
                        <div class="col-sm-10">
                            <input type="text" class="form-control" id="userTel" placeholder="전화번호">
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <div class="col-md-12">
                        <label for="userEmail" class="col-sm-2 control-label">메일주소</label>
                        <div class="col-sm-10">
                            <input type="text" class="form-control" id="userEmail" placeholder="메일주소">
                        </div>
                    </div>
                </div>

                <div class="form-group">
                    <div class="col-md-12">
                        <div class="col-sm-offset-2 col-sm-10" align="right">
                            <input type="submit" id="join" name="join" value="회원가입" class="btn btn-default">
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </body>

</html>