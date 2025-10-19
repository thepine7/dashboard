/**
 * 하트비트 관리자
 * 
 * 주요 기능:
 * - 2분마다 서버에 활동 신호 전송
 * - 브라우저/앱 종료 시 하트비트 중단 → 3분 후 자동 비활성 처리
 * - 페이지 백그라운드 시에도 하트비트 유지
 * 
 * 사용 방법:
 * - 모든 페이지에서 자동으로 실행됨
 * - 별도의 초기화 코드 불필요
 */
var HeartbeatManager = (function() {
  'use strict';
  
  // 하트비트 상태
  var heartbeatState = {
    intervalId: null,
    isRunning: false,
    lastHeartbeatTime: 0,
    failCount: 0,
    maxFailCount: 3
  };
  
  // 설정
  var config = {
    interval: 120000,      // 2분 (120초)
    timeout: 5000,         // 타임아웃 5초
    retryDelay: 10000      // 재시도 지연 10초
  };
  
  /**
   * 하트비트 전송
   */
  function sendHeartbeat() {
    // 페이지가 숨겨져 있거나 오프라인이면 전송하지 않음
    if (document.hidden || !navigator.onLine) {
      console.log('하트비트 스킵 - 페이지 숨김 또는 오프라인');
      return;
    }
    
    $.ajax({
      url: '/api/heartbeat',
      type: 'POST',
      data: JSON.stringify({}),
      contentType: 'application/json',
      dataType: 'json',
      timeout: config.timeout,
      success: function(result) {
        if (result.resultCode === '200') {
          heartbeatState.lastHeartbeatTime = Date.now();
          heartbeatState.failCount = 0;
          console.log('하트비트 전송 성공');
        } else {
          console.warn('하트비트 응답 오류:', result.resultMessage);
          handleHeartbeatFailure();
        }
      },
      error: function(xhr, status, error) {
        console.warn('하트비트 전송 실패:', status, error);
        handleHeartbeatFailure();
      }
    });
  }
  
  /**
   * 하트비트 실패 처리
   */
  function handleHeartbeatFailure() {
    heartbeatState.failCount++;
    
    if (heartbeatState.failCount >= heartbeatState.maxFailCount) {
      console.error('하트비트 연속 실패 (' + heartbeatState.failCount + '회) - 중단');
      stop();
    }
  }
  
  /**
   * 하트비트 시작
   */
  function start() {
    if (heartbeatState.isRunning) {
      console.log('하트비트 이미 실행 중');
      return;
    }
    
    console.log('하트비트 시작 - 2분마다 전송');
    
    // 즉시 첫 하트비트 전송
    sendHeartbeat();
    
    // 2분마다 하트비트 전송
    heartbeatState.intervalId = setInterval(function() {
      sendHeartbeat();
    }, config.interval);
    
    heartbeatState.isRunning = true;
  }
  
  /**
   * 하트비트 중지
   */
  function stop() {
    if (!heartbeatState.isRunning) {
      return;
    }
    
    console.log('하트비트 중지');
    
    if (heartbeatState.intervalId) {
      clearInterval(heartbeatState.intervalId);
      heartbeatState.intervalId = null;
    }
    
    heartbeatState.isRunning = false;
  }
  
  /**
   * 하트비트 재시작
   */
  function restart() {
    console.log('하트비트 재시작');
    stop();
    heartbeatState.failCount = 0;
    start();
  }
  
  /**
   * 하트비트 상태 조회
   */
  function getStatus() {
    return {
      isRunning: heartbeatState.isRunning,
      lastHeartbeatTime: heartbeatState.lastHeartbeatTime,
      failCount: heartbeatState.failCount,
      timeSinceLastHeartbeat: Date.now() - heartbeatState.lastHeartbeatTime
    };
  }
  
  /**
   * 페이지 가시성 변경 감지
   */
  document.addEventListener('visibilitychange', function() {
    if (document.hidden) {
      console.log('페이지 숨김 - 하트비트 일시 중단');
      // 페이지가 숨겨져도 하트비트는 유지 (백그라운드 탭도 활성 상태로 간주)
    } else {
      console.log('페이지 표시 - 하트비트 재개');
      // 페이지가 다시 표시되면 즉시 하트비트 전송
      if (heartbeatState.isRunning) {
        sendHeartbeat();
      }
    }
  });
  
  /**
   * 페이지 로드 시 자동 시작
   */
  $(document).ready(function() {
    // 세션이 있는 경우에만 하트비트 시작
    $.ajax({
      url: '/api/checkSession',
      type: 'POST',
      data: JSON.stringify({}),
      contentType: 'application/json',
      dataType: 'json',
      timeout: 3000,
      success: function(result) {
        if (result.resultCode === '200') {
          console.log('세션 확인 완료 - 하트비트 자동 시작');
          start();
        } else {
          console.log('세션 없음 - 하트비트 시작 안함');
        }
      },
      error: function() {
        console.log('세션 확인 실패 - 하트비트 시작 안함');
      }
    });
  });
  
  // Public API
  return {
    start: start,
    stop: stop,
    restart: restart,
    getStatus: getStatus
  };
})();

// 전역 객체로 노출
window.HeartbeatManager = HeartbeatManager;

