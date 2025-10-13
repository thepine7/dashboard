package com.andrew.hnt.api.service.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.andrew.hnt.api.common.Constants;
import com.andrew.hnt.api.service.SessionManagementService;
import com.andrew.hnt.api.service.SessionSecurityService;
import com.andrew.hnt.api.service.AdminService;
import com.andrew.hnt.api.service.LoginService;
import com.andrew.hnt.api.util.StringUtil;

/**
 * 세션 관리 공통 서비스 구현체
 */
@Service
public class SessionManagementServiceImpl implements SessionManagementService {
    
    @Autowired
    @Lazy
    private AdminService adminService;
    
    @Autowired
    @Lazy
    private LoginService loginService;
    
    @Autowired
    @Lazy
    private SessionSecurityService sessionSecurityService;
    
    private static final Logger logger = LoggerFactory.getLogger(SessionManagementServiceImpl.class);
    
    
    @Override
    public SessionInfo validateAndGetSessionInfo(HttpSession session, Object req) {
        // 세션에서 사용자 정보 확인
        String sessionUserId = (String) session.getAttribute("userId");
        String sessionUserGrade = (String) session.getAttribute("userGrade");
        String sessionUserNm = (String) session.getAttribute("userNm");
        
        logger.info("=== 세션 정보 확인 ===");
        logger.info("세션 userId: {}, userGrade: {}, userNm: {}", sessionUserId, sessionUserGrade, sessionUserNm);
        
        // URL 파라미터 확인 (HttpServletRequest인 경우)
        String urlUserId = null;
        String urlUserGrade = null;
        String urlUserNm = null;
        
        if (req instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) req;
            urlUserId = request.getParameter("userId");
            urlUserGrade = request.getParameter("userGrade");
            urlUserNm = request.getParameter("userNm");
            
            logger.info("URL 파라미터 userId: {}, userGrade: {}, userNm: {}", 
                       urlUserId != null ? urlUserId : "null", 
                       urlUserGrade != null ? urlUserGrade : "null", 
                       urlUserNm != null ? urlUserNm : "null");
        }
        
        // 앱에서 전달받은 사용자 정보가 있으면 즉시 세션에 설정 (앱 로그인 예외 처리)
        if (!StringUtil.isEmpty(urlUserId) && !StringUtil.isEmpty(urlUserGrade)) {
            logger.info("앱에서 전달받은 사용자 정보를 세션에 즉시 설정 - userId: {}, userGrade: {}, userNm: {}", 
                       urlUserId, urlUserGrade, urlUserNm);
            
            // 앱에서 전달받은 사용자 정보를 세션에 설정 (신뢰)
            session.setAttribute("userId", urlUserId);
            session.setAttribute("userGrade", urlUserGrade);
            if (!StringUtil.isEmpty(urlUserNm)) {
                session.setAttribute("userNm", urlUserNm);
            }
            
            // Constants 키로도 설정 (호환성)
            session.setAttribute(Constants.SESSION_USER_ID, urlUserId);
            session.setAttribute(Constants.SESSION_USER_GRADE, urlUserGrade);
            if (!StringUtil.isEmpty(urlUserNm)) {
                session.setAttribute(Constants.SESSION_USER_NM, urlUserNm);
            }
            
            // 로컬 변수 업데이트
            sessionUserId = urlUserId;
            sessionUserGrade = urlUserGrade;
            sessionUserNm = urlUserNm;
            
            logger.info("세션 설정 완료 - sessionUserId: {}, sessionUserGrade: {}, sessionUserNm: {}", 
                       sessionUserId, sessionUserGrade, sessionUserNm);
        } else {
            // URL 파라미터가 없으면 세션 정보를 사용
            logger.info("URL 파라미터가 없으므로 세션 정보 사용 - userId: {}, userGrade: {}, userNm: {}", 
                       sessionUserId, sessionUserGrade, sessionUserNm);
        }
        
        // 세션 유효성 검사
        if (!isValidSession(session)) {
            // 소프트 복구 시도
            String fallbackUserId = (String) session.getAttribute("loginUserId");
            if (!StringUtil.isEmpty(fallbackUserId)) {
                // 앱/웹 공통: 로그인 직후 세션 보완 전 단계에서 loginUserId가 존재할 수 있음
                session.setAttribute("userId", fallbackUserId);
                if (session.getAttribute("userGrade") == null) {
                    // 기본값 U, 추후 CommonController.addSidebarData에서 보완
                    session.setAttribute("userGrade", "U");
                }
                
                // Constants 키로도 설정 (호환성)
                session.setAttribute(Constants.SESSION_USER_ID, fallbackUserId);
                if (session.getAttribute("userGrade") != null) {
                    session.setAttribute(Constants.SESSION_USER_GRADE, session.getAttribute("userGrade"));
                }
                logger.info("isValidSession 실패, loginUserId로 세션 보완 후 진행: {}", fallbackUserId);
                
                // 보완된 정보로 다시 설정
                sessionUserId = fallbackUserId;
                sessionUserGrade = "U";
            } else {
                logger.warn("세션이 유효하지 않음 - 로그인 페이지로 리다이렉트");
                return new SessionInfo(null, null, null, false, "redirect:/login/login");
            }
        }
        
        return new SessionInfo(sessionUserId, sessionUserGrade, sessionUserNm, true, null);
    }
    
    @Override
    public boolean hasPermission(HttpSession session, String requiredGrade) {
        String userGrade = (String) session.getAttribute("userGrade");
        
        if (StringUtil.isEmpty(userGrade)) {
            logger.warn("사용자 등급이 없음 - 권한 없음");
            return false;
        }
        
        // 등급별 권한 체크
        switch (requiredGrade) {
            case "A":
                return "A".equals(userGrade);
            case "U":
                return "A".equals(userGrade) || "U".equals(userGrade);
            case "B":
                return "A".equals(userGrade) || "U".equals(userGrade) || "B".equals(userGrade);
            default:
                logger.warn("알 수 없는 권한 등급: {}", requiredGrade);
                return false;
        }
    }
    
    @Override
    public void restoreSubAccountInfo(HttpSession session, String userId) {
        try {
            // 부계정 여부 확인
            boolean isSubAccount = adminService.isSubAccount(userId);
            
            if (isSubAccount) {
                // 부계정인 경우 메인 사용자 ID 조회
                String mainUserId = adminService.getMainUserIdForSubUser(userId);
                
                if (mainUserId != null && !mainUserId.isEmpty()) {
                    // 세션에 부계정 정보 복구
                    session.setAttribute("isSubAccount", true);
                    session.setAttribute("mainUserId", mainUserId);
                    session.setAttribute("loginUserId", mainUserId);
                    
                    logger.info("부계정 정보 복구 완료 - 부계정: {}, 메인 사용자: {}", userId, mainUserId);
                } else {
                    logger.warn("부계정의 메인 사용자 ID를 찾을 수 없음 - 부계정: {}", userId);
                    session.setAttribute("isSubAccount", false);
                    session.setAttribute("mainUserId", userId);
                    session.setAttribute("loginUserId", userId);
                }
            } else {
                // 주계정인 경우
                session.setAttribute("isSubAccount", false);
                session.setAttribute("mainUserId", userId);
                session.setAttribute("loginUserId", userId);
                
                logger.info("주계정 정보 복구 완료 - 사용자: {}", userId);
            }
        } catch (Exception e) {
            logger.error("부계정 정보 복구 실패 - userId: {}, error: {}", userId, e.getMessage());
            // 복구 실패 시 기본값 설정
            session.setAttribute("isSubAccount", false);
            session.setAttribute("mainUserId", userId);
            session.setAttribute("loginUserId", userId);
        }
    }
    
    @Override
    public boolean isAdmin(HttpSession session) {
        return hasPermission(session, "A");
    }
    
    @Override
    public boolean isUser(HttpSession session) {
        return hasPermission(session, "U");
    }
    
    @Override
    public boolean isSubAccount(HttpSession session) {
        if (!isValidSession(session)) {
            return false;
        }
        
        String userGrade = (String) session.getAttribute("userGrade");
        return "B".equals(userGrade);
    }
    
    @Override
    public boolean canManageDevices(HttpSession session) {
        // A, U 등급만 장치 삭제/수정 가능
        return hasPermission(session, "U");
    }
    
    @Override
    public boolean canManageUsers(HttpSession session) {
        // A 등급만 사용자 관리 가능
        return hasPermission(session, "A");
    }
    
    /**
     * 세션 보안 검증 (SessionSecurityService 위임)
     * @param session HttpSession
     * @param request HttpServletRequest
     * @return boolean
     */
    public boolean validateSessionSecurity(HttpSession session, HttpServletRequest request) {
        return sessionSecurityService.validateSessionSecurity(session, request);
    }
    
    /**
     * 세션 보안 검증 (기존 호환성 유지)
     * @param session HttpSession
     * @return boolean
     */
    public boolean validateSessionSecurity(HttpSession session) {
        return sessionSecurityService.validateSessionSecurity(session, null);
    }
    
    
    /**
     * 세션 갱신
     * @param session HttpSession
     */
    public void refreshSession(HttpSession session) {
        // SessionSecurityService를 통한 세션 갱신
        sessionSecurityService.refreshSession(session);
    }
    
    @Override
    public boolean isValidSession(HttpSession session) {
        if (session == null) {
            logger.warn("세션이 null임");
            return false;
        }
        
        // 세션 타임아웃 체크 (30분) - 먼저 체크하여 불필요한 로직 실행 방지
        long lastAccessedTime = session.getLastAccessedTime();
        long currentTime = System.currentTimeMillis();
        long sessionTimeout = 30 * 60 * 1000; // 30분
        
        if (currentTime - lastAccessedTime > sessionTimeout) {
            logger.warn("세션 타임아웃 - 마지막 접근: {}, 현재: {}", lastAccessedTime, currentTime);
            return false;
        }
        
        // Constants 키만 사용 (일관성 보장)
        String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
        String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
        
        if (StringUtil.isEmpty(userId) || StringUtil.isEmpty(userGrade)) {
            logger.warn("세션에 필수 정보가 없음 - SESSION_USER_ID: {}, SESSION_USER_GRADE: {}", userId, userGrade);
            logger.warn("기본 키 확인 - userId: {}, userGrade: {}", 
                session.getAttribute("userId"), session.getAttribute("userGrade"));
            return false;
        }
        
        return true;
    }
    
    @Override
    public void setSessionInfo(HttpSession session, String userId, String userGrade, String userNm) {
        if (session == null) {
            logger.warn("세션이 null이므로 정보 설정 불가");
            return;
        }
        
        // Constants 키로만 설정 (일관성 보장)
        session.setAttribute(Constants.SESSION_USER_ID, userId);
        session.setAttribute(Constants.SESSION_USER_GRADE, userGrade);
        if (!StringUtil.isEmpty(userNm)) {
            session.setAttribute(Constants.SESSION_USER_NM, userNm);
        }
        
        // 호환성을 위해 기본 키도 설정
        session.setAttribute("userId", userId);
        session.setAttribute("userGrade", userGrade);
        if (!StringUtil.isEmpty(userNm)) {
            session.setAttribute("userNm", userNm);
        }
        
        logger.info("세션 정보 설정 완료 - userId: {}, userGrade: {}, userNm: {}", userId, userGrade, userNm);
    }
    
    @Override
    public boolean setSessionInfoToModel(HttpSession session, Model model) {
        try {
            if (session == null || model == null) {
                logger.warn("세션이나 모델이 null입니다.");
                return false;
            }
            
            // 세션에서 사용자 정보 읽기
            String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
            String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
            String userNm = (String) session.getAttribute(Constants.SESSION_USER_NM);
            String userEmail = (String) session.getAttribute(Constants.SESSION_USER_EMAIL);
            String userTel = (String) session.getAttribute(Constants.SESSION_USER_TEL);
            String sensorId = (String) session.getAttribute(Constants.SESSION_SENSOR_ID);
            String token = (String) session.getAttribute(Constants.SESSION_TOKEN);
            
            logger.debug("세션에서 읽은 정보 - userId: {}, userGrade: {}, userNm: {}", userId, userGrade, userNm);
            
            // 세션에 핵심 정보가 없으면 DB에서 조회
            if (StringUtil.isEmpty(userId) || StringUtil.isEmpty(userGrade)) {
                logger.warn("세션에 핵심 사용자 정보가 없습니다. DB에서 조회 시도");
                return false;
            }
            
            // 모델에 세션 정보 설정 (JSP에서 사용할 키로 설정)
            model.addAttribute("userId", userId);
            model.addAttribute("userGrade", userGrade);
            model.addAttribute("userNm", userNm);
            model.addAttribute("sensorId", StringUtil.isNotEmpty(sensorId) ? sensorId : userId);
            model.addAttribute("loginUserId", userId); // loginUserId도 userId와 동일하게 설정
            
            // Constants 키로도 설정 (호환성)
            model.addAttribute(Constants.SESSION_USER_ID, userId);
            model.addAttribute(Constants.SESSION_USER_GRADE, userGrade);
            model.addAttribute(Constants.SESSION_USER_NM, userNm);
            model.addAttribute(Constants.SESSION_SENSOR_ID, StringUtil.isNotEmpty(sensorId) ? sensorId : userId);
            model.addAttribute(Constants.SESSION_LOGIN_USER_ID, userId);
            
            logger.info("세션 정보를 모델에 설정 완료 - userId: {}, userGrade: {}, userNm: {}", 
                       userId, userGrade, userNm);
            
            if (StringUtil.isNotEmpty(userNm)) {
                model.addAttribute(Constants.SESSION_USER_NM, userNm);
            } else if (loginService != null) {
                // 세션에 사용자 이름이 없으면 DB에서 조회
                try {
                    com.andrew.hnt.api.model.UserInfo userInfo = loginService.getUserInfoByUserId(userId);
                    if (userInfo != null && StringUtil.isNotEmpty(userInfo.getUserNm())) {
                        model.addAttribute(Constants.SESSION_USER_NM, userInfo.getUserNm());
                        // 세션에도 업데이트
                        session.setAttribute(Constants.SESSION_USER_NM, userInfo.getUserNm());
                        logger.debug("DB에서 사용자 이름 조회하여 모델에 설정: {}", userInfo.getUserNm());
                    }
                } catch (Exception e) {
                    logger.warn("DB에서 사용자 정보 조회 실패: {}", e.getMessage());
                }
            }
            
            if (StringUtil.isNotEmpty(userEmail)) {
                model.addAttribute(Constants.SESSION_USER_EMAIL, userEmail);
            }
            
            if (StringUtil.isNotEmpty(userTel)) {
                model.addAttribute(Constants.SESSION_USER_TEL, userTel);
            }
            
            if (StringUtil.isNotEmpty(token)) {
                model.addAttribute(Constants.SESSION_TOKEN, token);
            }
            
            logger.info("세션 정보를 모델에 설정 완료 - userId: {}, userGrade: {}, userNm: {}", 
                       userId, userGrade, model.asMap().get(Constants.SESSION_USER_NM));
            
            return true;
            
        } catch (Exception e) {
            logger.error("세션 정보를 모델에 설정하는 중 오류 발생", e);
            return false;
        }
    }
    
    @Override
    public boolean setSessionInfoToModel(HttpSession session, Model model, String userId) {
        try {
            if (session == null || model == null || StringUtil.isEmpty(userId)) {
                logger.warn("세션이나 모델이 null이거나 사용자 ID가 없습니다.");
                return false;
            }
            
            // 세션에서 사용자 정보 읽기
            String sessionUserId = (String) session.getAttribute(Constants.SESSION_USER_ID);
            String sessionUserGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
            String sessionUserNm = (String) session.getAttribute(Constants.SESSION_USER_NM);
            String sessionUserEmail = (String) session.getAttribute(Constants.SESSION_USER_EMAIL);
            String sessionUserTel = (String) session.getAttribute(Constants.SESSION_USER_TEL);
            String sessionSensorId = (String) session.getAttribute(Constants.SESSION_SENSOR_ID);
            String sessionToken = (String) session.getAttribute(Constants.SESSION_TOKEN);
            
            logger.debug("세션에서 읽은 정보 - sessionUserId: {}, sessionUserGrade: {}, sessionUserNm: {}", 
                        sessionUserId, sessionUserGrade, sessionUserNm);
            
            // 세션에 핵심 정보가 없으면 DB에서 조회
            if (StringUtil.isEmpty(sessionUserId) || StringUtil.isEmpty(sessionUserGrade)) {
                logger.warn("세션에 핵심 사용자 정보가 없습니다. DB에서 조회 시도 - userId: {}", userId);
                
                try {
                    com.andrew.hnt.api.model.UserInfo userInfo = loginService.getUserInfoByUserId(userId);
                    if (userInfo != null) {
                        // DB에서 조회한 정보로 세션 설정
                        session.setAttribute(Constants.SESSION_USER_ID, userInfo.getUserId());
                        session.setAttribute(Constants.SESSION_USER_GRADE, userInfo.getUserGrade());
                        session.setAttribute(Constants.SESSION_USER_NM, userInfo.getUserNm());
                        session.setAttribute(Constants.SESSION_USER_EMAIL, userInfo.getUserEmail());
                        session.setAttribute(Constants.SESSION_USER_TEL, userInfo.getUserTel());
                        
                        // 모델에 설정
                        model.addAttribute(Constants.SESSION_USER_ID, userInfo.getUserId());
                        model.addAttribute(Constants.SESSION_USER_GRADE, userInfo.getUserGrade());
                        model.addAttribute(Constants.SESSION_USER_NM, userInfo.getUserNm());
                        model.addAttribute(Constants.SESSION_USER_EMAIL, userInfo.getUserEmail());
                        model.addAttribute(Constants.SESSION_USER_TEL, userInfo.getUserTel());
                        model.addAttribute(Constants.SESSION_SENSOR_ID, userInfo.getUserId());
                        
                        logger.info("DB에서 사용자 정보 조회하여 세션 및 모델에 설정 - userId: {}, userGrade: {}, userNm: {}", 
                                   userInfo.getUserId(), userInfo.getUserGrade(), userInfo.getUserNm());
                        return true;
                    } else {
                        logger.warn("DB에서 사용자 정보를 찾을 수 없습니다 - userId: {}", userId);
                        return false;
                    }
                } catch (Exception e) {
                    logger.error("DB에서 사용자 정보 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
                    return false;
                }
            }
            
            // 세션에 정보가 있으면 모델에 설정
            model.addAttribute(Constants.SESSION_USER_ID, sessionUserId);
            model.addAttribute(Constants.SESSION_USER_GRADE, sessionUserGrade);
            model.addAttribute(Constants.SESSION_SENSOR_ID, StringUtil.isNotEmpty(sessionSensorId) ? sessionSensorId : sessionUserId);
            
            if (StringUtil.isNotEmpty(sessionUserNm)) {
                model.addAttribute(Constants.SESSION_USER_NM, sessionUserNm);
            }
            
            if (StringUtil.isNotEmpty(sessionUserEmail)) {
                model.addAttribute(Constants.SESSION_USER_EMAIL, sessionUserEmail);
            }
            
            if (StringUtil.isNotEmpty(sessionUserTel)) {
                model.addAttribute(Constants.SESSION_USER_TEL, sessionUserTel);
            }
            
            if (StringUtil.isNotEmpty(sessionToken)) {
                model.addAttribute(Constants.SESSION_TOKEN, sessionToken);
            }
            
            logger.info("세션 정보를 모델에 설정 완료 - userId: {}, userGrade: {}, userNm: {}", 
                       sessionUserId, sessionUserGrade, sessionUserNm);
            
            return true;
            
        } catch (Exception e) {
            logger.error("세션 정보를 모델에 설정하는 중 오류 발생 - userId: {}", userId, e);
            return false;
        }
    }
    
    @Override
    public String handleSessionExpired(HttpSession session, HttpServletRequest request) {
        try {
            if (session != null) {
                String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
                if (userId != null) {
                    logger.info("세션 만료 처리: userId={}", userId);
                    
                    // 세션 보안 정보 제거
                    sessionSecurityService.removeSessionInfo(userId);
                }
                
                // 세션 무효화
                session.invalidate();
            }
            
            // 로그인 페이지로 리다이렉트
            return "redirect:/login/login";
            
        } catch (Exception e) {
            logger.error("세션 만료 처리 중 오류 발생", e);
            return "redirect:/login/login";
        }
    }
    
    @Override
    public void invalidateSession(HttpSession session, String reason) {
        try {
            if (session != null) {
                String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
                if (userId != null) {
                    logger.info("세션 무효화: userId={}, reason={}", userId, reason);
                }
                
                // SessionSecurityService를 통한 세션 무효화
                sessionSecurityService.invalidateSession(session);
            }
        } catch (Exception e) {
            logger.error("세션 무효화 중 오류 발생: reason={}", reason, e);
        }
    }
    
    
}
