package com.andrew.hnt.api.scheduler;

import com.andrew.hnt.api.service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 사용자 활동 상태 자동 관리 스케줄러
 * 
 * 주요 기능:
 * - 3분마다 비활성 사용자 자동 로그아웃 처리
 * - 마지막 활동(mdf_dtm)으로부터 3분 이상 지난 사용자를 비활성 처리
 * 
 * 동작 방식:
 * 1. 하트비트: 2분마다 프론트엔드에서 활동 신호 전송 → mdf_dtm 갱신
 * 2. 스케줄러: 3분마다 mdf_dtm이 3분 이상 지난 사용자를 비활성 처리
 * 3. 결과: 브라우저/앱 종료 후 3분 이내 "비활성" 표시
 */
@Component
@EnableScheduling
public class UserActivityScheduler {
  
  @Autowired
  private LoginService loginService;
  
  private static final Logger logger = LoggerFactory.getLogger(UserActivityScheduler.class);
  
  /**
   * 3분마다 비활성 웹 사용자 자동 로그아웃 처리 (앱 사용자 제외)
   * - 마지막 활동(mdf_dtm)으로부터 3분 이상 지난 웹 사용자를 비활성 처리
   * - 앱 사용자는 하트비트를 보내지 않으므로 타임아웃 체크에서 제외
   * - 앱 사용자는 30분 세션 타임아웃만 적용
   * - 초기 지연: 1분 후 시작
   * - 실행 간격: 3분마다
   */
  @Scheduled(fixedDelay = 180000, initialDelay = 60000) // 3분마다, 초기 1분 후 시작
  public void checkInactiveUsers() {
    try {
      logger.info("=== 비활성 사용자 체크 시작 (웹 사용자만) ===");
      // 앱 사용자는 하트비트를 보내지 않으므로 타임아웃 체크에서 제외
      // 앱 사용자는 30분 세션 타임아웃만 적용
      loginService.checkAndUpdateSessionTimeoutForWebOnly();
      logger.info("=== 비활성 사용자 체크 완료 ===");
    } catch (Exception e) {
      logger.error("비활성 사용자 체크 실패", e);
    }
  }
}

