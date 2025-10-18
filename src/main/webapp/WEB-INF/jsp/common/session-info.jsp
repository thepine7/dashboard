<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!-- 공통 세션 정보 처리 템플릿 -->
<!-- 모든 JSP 페이지에서 include하여 사용 -->

<!-- JSP EL 디버깅 정보 (개발 환경에서만 표시) -->
<c:if test="${pageContext.request.serverName == 'localhost' || pageContext.request.serverName == '127.0.0.1'}">
<div style="display:none; background: #f0f0f0; padding: 5px; margin: 5px 0; font-size: 12px; border: 1px solid #ccc;">
    <strong>세션 정보 디버깅:</strong><br>
    userId: ${userId}<br>
    userGrade: ${userGrade}<br>
    userNm: ${userNm}<br>
    sensorId: ${sensorId}<br>
    loginUserId: ${loginUserId}<br>
    parentUserId: ${parentUserId}<br>
    userEmail: ${userEmail}<br>
    userTel: ${userTel}<br>
    token: ${token}<br>
    sensorUuid: ${sensorUuid}<br>
    topicStr: ${topicStr}
</div>
</c:if>

<!-- 세션 정보 Hidden Input -->
<input type="hidden" id="userId" name="userId" value="${userId}" />
<input type="hidden" id="userGrade" name="userGrade" value="${userGrade}" />
<input type="hidden" id="userNm" name="userNm" value="${userNm}" />
<input type="hidden" id="sensorId" name="sensorId" value="${sensorId}" />
<input type="hidden" id="loginUserId" name="loginUserId" value="${loginUserId}" />
<input type="hidden" id="parentUserId" name="parentUserId" value="${parentUserId}" />
<input type="hidden" id="token" name="token" value="${token}" />

<!-- 선택적 세션 정보 (있는 경우만) -->
<c:if test="${not empty userEmail}">
<input type="hidden" id="userEmail" name="userEmail" value="${userEmail}" />
</c:if>

<c:if test="${not empty userTel}">
<input type="hidden" id="userTel" name="userTel" value="${userTel}" />
</c:if>

<!-- 센서 관련 정보 (항상 생성) -->
<input type="hidden" id="sensorUuid" name="sensorUuid" value="${sensorUuid}" />
<input type="hidden" id="topicStr" name="topicStr" value="${topicStr}" />

<!-- 세션 정보 초기화 스크립트 -->
<script>
// jQuery가 로드된 후에 실행되도록 확인
function initializeSessionInfo() {
    console.log('=== session-info.jsp 세션 정보 초기화 시작 ===');
    
    // 세션 정보 디버깅 로그
    console.log('JSP EL 표현식 값:');
    console.log('  userId: ${userId}');
    console.log('  userGrade: ${userGrade}');
    console.log('  userNm: ${userNm}');
    console.log('  sensorId: ${sensorId}');
    console.log('  loginUserId: ${loginUserId}');
    console.log('  parentUserId: ${parentUserId}');
    console.log('  userEmail: ${userEmail}');
    console.log('  userTel: ${userTel}');
    console.log('  token: ${token}');
    
    // Hidden input 값 확인
    console.log('Hidden input 값:');
    console.log('  userId:', $('#userId').val());
    console.log('  userGrade:', $('#userGrade').val());
    console.log('  userNm:', $('#userNm').val());
    console.log('  sensorId:', $('#sensorId').val());
    console.log('  loginUserId:', $('#loginUserId').val());
    console.log('  parentUserId:', $('#parentUserId').val());
    console.log('  userEmail:', $('#userEmail').val());
    console.log('  userTel:', $('#userTel').val());
    console.log('  token:', $('#token').val());
    
    // SessionManager 초기화 (이미 초기화되었다면 스킵)
    if (typeof SessionManager !== 'undefined') {
        // SessionManager가 이미 초기화되었는지 확인
        if (!SessionManager.isInitialized || !SessionManager.isInitialized()) {
            if (!SessionManager.initialize()) {
                console.error('SessionManager 초기화 실패');
                return false;
            }
            console.log('SessionManager 초기화 성공 (session-info.jsp)');
        } else {
            console.log('SessionManager 이미 초기화됨 - 스킵');
        }
    } else {
        console.warn('SessionManager가 로드되지 않았습니다. session-manager.js를 포함해주세요.');
    }
    
    // 세션 정보 유효성 검증
    var userId = $('#userId').val();
    var userGrade = $('#userGrade').val();
    var userNm = $('#userNm').val();
    var sensorId = $('#sensorId').val();
    var loginUserId = $('#loginUserId').val();
    
    console.log('세션 정보 유효성 검증:');
    console.log('  userId:', userId, userId ? 'OK' : 'MISSING');
    console.log('  userGrade:', userGrade, userGrade ? 'OK' : 'MISSING');
    console.log('  userNm:', userNm, userNm ? 'OK' : 'MISSING');
    console.log('  sensorId:', sensorId, sensorId ? 'OK' : 'MISSING');
    console.log('  loginUserId:', loginUserId, loginUserId ? 'OK' : 'MISSING');
    
    if (!userId || !userGrade) {
        console.error('세션 정보가 없습니다. 로그인 페이지로 이동합니다.');
        console.error('누락된 정보 - userId:', userId, 'userGrade:', userGrade);
        alert('세션 정보가 없습니다. 다시 로그인해주세요.');
        window.location.href = '/login/login';
        return false;
    }
    
    // 전역 변수 설정 (하위 호환성)
    window.currentUserId = userId;
    window.currentUserGrade = userGrade;
    window.currentUserNm = userNm || '';
    window.currentSensorId = sensorId || userId;
    window.currentLoginUserId = loginUserId || userId;
    window.currentParentUserId = $('#parentUserId').val() || userId;
    window.currentUserEmail = $('#userEmail').val() || '';
    window.currentUserTel = $('#userTel').val() || '';
    window.currentToken = $('#token').val() || '';
    
    // SessionData 객체도 설정 (통일성)
    window.SessionData = {
        userId: userId,
        userGrade: userGrade,
        userNm: userNm || '',
        sensorId: sensorId || userId,
        loginUserId: loginUserId || userId,
        parentUserId: $('#parentUserId').val() || userId,
        userEmail: $('#userEmail').val() || '',
        userTel: $('#userTel').val() || '',
        token: $('#token').val() || '',
        saveId: $('#saveId').val() || ''
    };
    
    console.log('세션 정보 초기화 완료:');
    console.log('  window.currentUserId:', window.currentUserId);
    console.log('  window.currentUserGrade:', window.currentUserGrade);
    console.log('  window.currentUserNm:', window.currentUserNm);
    console.log('  window.currentSensorId:', window.currentSensorId);
    console.log('  window.SessionData:', window.SessionData);
    
    return true;
}

// jQuery가 로드된 후에 실행
if (typeof $ !== 'undefined') {
    $(document).ready(function() {
        console.log('jQuery 로드 완료 - session-info 초기화 시작');
        initializeSessionInfo();
    });
} else {
    // jQuery가 아직 로드되지 않은 경우, 로드 후 실행
    window.addEventListener('load', function() {
        if (typeof $ !== 'undefined') {
            console.log('jQuery 로드 완료 (load 이벤트) - session-info 초기화 시작');
            $(document).ready(function() {
                initializeSessionInfo();
            });
        } else {
            console.warn('jQuery가 로드되지 않았습니다.');
        }
    });
}
</script>
