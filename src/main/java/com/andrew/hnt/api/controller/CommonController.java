package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.common.Constants;
import com.andrew.hnt.api.model.UserInfo;
import com.andrew.hnt.api.service.LoginService;
import com.andrew.hnt.api.util.StringUtil;
import com.andrew.hnt.api.util.UnifiedErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import javax.servlet.http.HttpSession;

/**
 * 공통 컨트롤러
 * 각 페이지에서 공통으로 사용되는 기능을 제공
 */
@Component
public class CommonController {
    
    private static final Logger logger = LoggerFactory.getLogger(CommonController.class);
    
    @Autowired
    private LoginService loginService;
    
    @Autowired
    private UnifiedErrorHandler errorHandler;
    
    /**
     * 사이드바 데이터를 위한 공통 메서드
     * 각 페이지에서 호출하여 사이드바 관련 데이터만 Model에 추가
     * 세션 정보는 setUserInfoFromSession에서 이미 설정되었으므로 여기서는 사이드바 전용 데이터만 처리
     */
    public void addSidebarData(String userId, Model model, HttpSession session) {
        try {
            if (userId != null && !userId.isEmpty()) {
                // 세션에서 사용자 정보 확인
                String sessionUserNm = (String) session.getAttribute(Constants.SESSION_USER_NM);
                String sessionUserGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
                String sessionUserEmail = (String) session.getAttribute(Constants.SESSION_USER_EMAIL);
                String sessionUserTel = (String) session.getAttribute(Constants.SESSION_USER_TEL);
                String sessionToken = (String) session.getAttribute(Constants.SESSION_TOKEN);
                
                logger.info("=== 사이드바 데이터 추가 시작 ===");
                logger.info("userId: {}, userNm: {}, userGrade: {}", 
                           userId, sessionUserNm, sessionUserGrade);
                
                // JSP에서 사용할 세션 정보를 모델에 설정 (중복 설정 방지)
                if (!model.containsAttribute("userId")) {
                    model.addAttribute("userId", userId);
                }
                if (!model.containsAttribute("userGrade")) {
                    model.addAttribute("userGrade", sessionUserGrade);
                }
                if (!model.containsAttribute("userNm")) {
                    model.addAttribute("userNm", sessionUserNm);
                }
                if (!model.containsAttribute("sensorId")) {
                    model.addAttribute("sensorId", userId);
                }
                if (!model.containsAttribute("loginUserId")) {
                    model.addAttribute("loginUserId", userId);
                }
                if (!model.containsAttribute("userEmail")) {
                    model.addAttribute("userEmail", sessionUserEmail != null ? sessionUserEmail : "");
                }
                if (!model.containsAttribute("userTel")) {
                    model.addAttribute("userTel", sessionUserTel != null ? sessionUserTel : "");
                }
                if (!model.containsAttribute("token")) {
                    model.addAttribute("token", sessionToken != null ? sessionToken : "");
                }
                if (!model.containsAttribute("saveId")) {
                    model.addAttribute("saveId", "");
                }
                
                logger.info("사이드바 데이터 설정 완료 - userId: {}, userGrade: {}, userNm: {}", 
                           userId, sessionUserGrade, sessionUserNm);
                
                // TODO: 사이드바 전용 데이터 추가 로직 구현
                // 예: 메뉴 권한, 알림 개수, 사용자 설정 등
                
            } else {
                logger.warn("사용자 ID가 없어 사이드바 데이터를 추가할 수 없습니다.");
            }
        } catch (Exception e) {
            errorHandler.logError("사이드바 데이터 추가", e);
        }
    }
    
    /**
     * 사용자 정보를 세션에서 가져와서 모델에 설정
     * @param session HttpSession
     * @param model Model
     * @return 성공 여부
     */
    public boolean setUserInfoFromSession(HttpSession session, Model model) {
        try {
            if (session == null || model == null) {
                logger.warn("세션이나 모델이 null입니다.");
                return false;
            }
            
            // 세션에서 사용자 정보 읽기
            String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
            String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
            String userNm = (String) session.getAttribute(Constants.SESSION_USER_NM);
            String sensorId = (String) session.getAttribute(Constants.SESSION_SENSOR_ID);
            String userEmail = (String) session.getAttribute(Constants.SESSION_USER_EMAIL);
            String userTel = (String) session.getAttribute(Constants.SESSION_USER_TEL);
            String token = (String) session.getAttribute(Constants.SESSION_TOKEN);
            
            if (StringUtil.isEmpty(userId) || StringUtil.isEmpty(userGrade)) {
                logger.warn("세션에 핵심 사용자 정보가 없습니다.");
                return false;
            }
            
            // 모델에 세션 정보 설정 (JSP에서 사용할 키로 설정)
            model.addAttribute("userId", userId);
            model.addAttribute("userGrade", userGrade);
            model.addAttribute("userNm", userNm);
            model.addAttribute("sensorId", StringUtil.isNotEmpty(sensorId) ? sensorId : userId);
            model.addAttribute("loginUserId", userId);
            model.addAttribute("userEmail", userEmail != null ? userEmail : "");
            model.addAttribute("userTel", userTel != null ? userTel : "");
            model.addAttribute("token", token != null ? token : "");
            model.addAttribute("saveId", "");
            
            // Constants 키로도 설정 (호환성)
            model.addAttribute(Constants.SESSION_USER_ID, userId);
            model.addAttribute(Constants.SESSION_USER_GRADE, userGrade);
            model.addAttribute(Constants.SESSION_USER_NM, userNm);
            model.addAttribute(Constants.SESSION_SENSOR_ID, StringUtil.isNotEmpty(sensorId) ? sensorId : userId);
            model.addAttribute(Constants.SESSION_LOGIN_USER_ID, userId);
            
            logger.info("세션 정보를 모델에 설정 완료 - userId: {}, userGrade: {}, userNm: {}", 
                       userId, userGrade, userNm);
            
            return true;
            
        } catch (Exception e) {
            errorHandler.logError("세션 정보 모델 설정", e);
            return false;
        }
    }
    
    /**
     * 사용자 정보를 DB에서 조회하여 모델에 설정
     * @param userId 사용자 ID
     * @param model Model
     * @return 성공 여부
     */
    public boolean setUserInfoFromDatabase(String userId, Model model) {
        try {
            if (StringUtil.isEmpty(userId) || model == null) {
                logger.warn("사용자 ID가 없거나 모델이 null입니다.");
                return false;
            }
            
            // DB에서 사용자 정보 조회
            UserInfo userInfo = loginService.getUserInfoByUserId(userId);
            if (userInfo == null) {
                logger.warn("사용자 정보를 찾을 수 없습니다 - userId: {}", userId);
                return false;
            }
            
            // 모델에 사용자 정보 설정 (JSP에서 사용할 키로 설정)
            model.addAttribute("userId", userInfo.getUserId());
            model.addAttribute("userGrade", userInfo.getUserGrade());
            model.addAttribute("userNm", userInfo.getUserNm());
            model.addAttribute("sensorId", userInfo.getUserId());
            model.addAttribute("loginUserId", userInfo.getUserId());
            
            // Constants 키로도 설정 (호환성)
            model.addAttribute(Constants.SESSION_USER_ID, userInfo.getUserId());
            model.addAttribute(Constants.SESSION_USER_GRADE, userInfo.getUserGrade());
            model.addAttribute(Constants.SESSION_USER_NM, userInfo.getUserNm());
            model.addAttribute(Constants.SESSION_SENSOR_ID, userInfo.getUserId());
            model.addAttribute(Constants.SESSION_LOGIN_USER_ID, userInfo.getUserId());
            
            logger.info("DB에서 사용자 정보 조회하여 모델에 설정 완료 - userId: {}, userGrade: {}, userNm: {}", 
                       userInfo.getUserId(), userInfo.getUserGrade(), userInfo.getUserNm());
            
            return true;
            
        } catch (Exception e) {
            errorHandler.logError("DB 사용자 정보 조회", e);
            return false;
        }
    }
}