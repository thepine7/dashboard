package com.andrew.hnt.api.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.andrew.hnt.api.model.LoginVO;
import com.andrew.hnt.api.model.UserInfo;
import com.andrew.hnt.api.service.LoginService;
import com.andrew.hnt.api.service.AdminService;
import com.andrew.hnt.api.service.SessionSecurityService;
import org.slf4j.Logger;
import com.andrew.hnt.api.util.StringUtil;
import com.andrew.hnt.api.util.UnifiedErrorHandler;
import com.andrew.hnt.api.util.ValidationUtil;
import com.andrew.hnt.api.common.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.andrew.hnt.api.service.SessionManagementService;
import com.andrew.hnt.api.util.ResponseUtil;
import org.slf4j.LoggerFactory;

@Controller
public class LoginController {
	
	@Autowired
	private LoginService loginService;
	
	@Autowired
	private AdminService adminService;
	
	@Autowired
	private SessionManagementService sessionManagementService;
	
	@Autowired
	private UnifiedErrorHandler unifiedErrorHandler;
	
	@Autowired
	private SessionSecurityService sessionSecurityService;
	
	private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

	/**
	 * 로그인 페이지 호출
	 * @param req
	 * @param res
	 * @return
	 */
	@RequestMapping(value = "/login/login", method = RequestMethod.GET)
	public String login(
			HttpServletRequest req
			, HttpServletResponse res
	        ) {

		logger.info("login");

		return "login/login";
	}

	@RequestMapping(value = "/logout/logout",method = RequestMethod.GET)
    public String logout(
        HttpServletRequest req
        , HttpServletResponse res
        ) {
  String result = "redirect:/login/login";

  HttpSession session = req.getSession();

  String sessionUserId = null;
  if (session != null) {
    sessionUserId = (String) session.getAttribute(Constants.SESSION_USER_ID);

    if (sessionUserId != null && !sessionUserId.isEmpty()) {
      // 세션 보안 서비스를 통한 세션 무효화
      sessionSecurityService.invalidateSession(session);
      
      session.removeAttribute(Constants.SESSION_USER_ID);
      session.invalidate();
      result = "redirect:/login/login";
    }
  }

  // 로그아웃 시 로그아웃 일시 업데이트
  LoginVO loginVO = new LoginVO();
  if (sessionUserId != null && !sessionUserId.isEmpty()) {
    loginVO.setUserId(sessionUserId);
  }

  try {
    loginService.updateLogoutDtm(loginVO);
  } catch(Exception e) {
    unifiedErrorHandler.logError("로그아웃 시간 업데이트", e);
  }

  return result;
}

	@RequestMapping(value = "/logout/logoutProcess", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> logoutProcess(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody LoginVO loginVO
	) {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		HttpSession session = req.getSession();

		if(null != session) {
			if(null != String.valueOf(session.getAttribute(Constants.SESSION_USER_ID)) && !"".equals(String.valueOf(session.getAttribute(Constants.SESSION_USER_ID)))) {
				if(loginVO.getUserId().equals(String.valueOf(loginVO.getUserId()))) {
					session.removeAttribute(Constants.SESSION_USER_ID);
					session.invalidate();
				}
			}
		}

		try {
			loginService.updateLogoutDtm(loginVO);
			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "로그아웃 성공");
		} catch(Exception e) {
			unifiedErrorHandler.logError("로그아웃 처리", e);
			return unifiedErrorHandler.createInternalServerErrorResponse("로그아웃 처리 중 오류가 발생했습니다.");
		}

		return resultMap;
	}
	
	/**
	 * 로그인 처리 요청
	 * @param req
	 * @param res
	 * @return
	 */
	@RequestMapping(value = "/login/loginProcess", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> loginProcess(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody LoginVO loginVO
			) {

		logger.info("=== 로그인 처리 시작 ===");
		logger.info("요청 데이터: userId={}, userPass={}", 
			loginVO != null ? loginVO.getUserId() : "null", 
			loginVO != null ? "***" : "null");

		Map<String, Object> resultMap = new HashMap<String, Object>();

		// 기존 세션 재사용 (세션 정보 손실 방지)
		HttpSession session = req.getSession(true);
		
		// 세션 고정 공격 방지를 위한 세션 ID 재발급 (세션 무효화 없이)
		if (session.getAttribute("userId") != null) {
			// 기존 로그인된 사용자가 있으면 로그아웃 처리
			logger.info("기존 로그인된 사용자 발견 - 로그아웃 처리");
			session.removeAttribute("userId");
			session.removeAttribute("userGrade");
			session.removeAttribute("userNm");
			session.removeAttribute(Constants.SESSION_USER_ID);
			session.removeAttribute(Constants.SESSION_USER_GRADE);
			session.removeAttribute(Constants.SESSION_USER_NM);
		}

		if(null != loginVO) {
			if(null != loginVO.getUserId() && !"".equals(loginVO.getUserId()) && 0 < loginVO.getUserId().length()) {
				if(null != loginVO.getUserPass() && !"".equals(loginVO.getUserPass()) && 0 < loginVO.getUserPass().length()) {
					try {
						Map<String, Object> loginMap = loginService.getUserInfo(loginVO);

						if(null != loginMap && 0 < loginMap.size()) {
							if("success".equals(String.valueOf(loginMap.get("result")))) {
								UserInfo userInfo = (UserInfo) loginMap.get("userInfo");
								resultMap.put("resultCode", "200");
								resultMap.put("resultMessage", "회원 로그인 성공");
								resultMap.put("userInfo", userInfo);
								resultMap.put("redirectUrl", "/main/main");

								// 로그인 성공 시 로그인 일시 업데이트 및 로그아웃 일시 삭제
								loginService.updateLoginDtm(loginVO);

								if(null != userInfo) {
									logger.info(userInfo.getUserId());
									// 센서가 없는 사용자의 경우 userId를 sensorId로 사용
									String sensorId = (userInfo.getSensorId() != null && !"".equals(userInfo.getSensorId())) 
										? userInfo.getSensorId() : userInfo.getUserId();
									
								// 세션 정보 설정 (통일된 방식)
								sessionManagementService.setSessionInfo(session, userInfo.getUserId(), userInfo.getUserGrade(), userInfo.getUserNm());
								
								// 세션 보안 서비스를 통한 세션 정보 설정
								sessionSecurityService.setSessionInfo(session, req, userInfo.getUserId(), userInfo.getUserGrade(), userInfo.getUserNm());
								
								// Constants 키로 세션에 직접 설정 (JSP에서 접근 가능)
								session.setAttribute(Constants.SESSION_USER_ID, userInfo.getUserId());
								session.setAttribute(Constants.SESSION_USER_GRADE, userInfo.getUserGrade());
								session.setAttribute(Constants.SESSION_USER_NM, userInfo.getUserNm());
								session.setAttribute(Constants.SESSION_SENSOR_ID, sensorId);
								session.setAttribute(Constants.SESSION_USER_EMAIL, userInfo.getUserEmail());
								session.setAttribute(Constants.SESSION_USER_TEL, userInfo.getUserTel());
								session.setAttribute(Constants.SESSION_TOKEN, userInfo.getToken());
								
								// 세션 설정 완료 로그
								logger.info("=== LoginController 세션 설정 완료 ===");
								logger.info("세션 ID: {}", session.getId());
								logger.info("설정된 사용자 정보:");
								logger.info("- SESSION_USER_ID: {}", session.getAttribute(Constants.SESSION_USER_ID));
								logger.info("- SESSION_USER_GRADE: {}", session.getAttribute(Constants.SESSION_USER_GRADE));
								logger.info("- SESSION_USER_NM: {}", session.getAttribute(Constants.SESSION_USER_NM));
								logger.info("- userId: {}", session.getAttribute("userId"));
								logger.info("- userGrade: {}", session.getAttribute("userGrade"));
								logger.info("- userNm: {}", session.getAttribute("userNm"));
								
								// 기본 키로도 설정 (호환성)
								session.setAttribute("userId", userInfo.getUserId());
								session.setAttribute("userGrade", userInfo.getUserGrade());
								session.setAttribute("userNm", userInfo.getUserNm());
								session.setAttribute("sensorId", sensorId);
									
									// 부계정 여부 확인 및 설정
									try {
										boolean isSubAccount = adminService.isSubAccount(userInfo.getUserId());
										if(isSubAccount) {
											// 부계정인 경우 메인 사용자 ID를 loginUserId로 설정
											String mainUserId = adminService.getMainUserIdForSubUser(userInfo.getUserId());
											if(mainUserId != null && !mainUserId.isEmpty()) {
												session.setAttribute(Constants.SESSION_LOGIN_USER_ID, mainUserId);
												session.setAttribute("isSubAccount", true);
												session.setAttribute("mainUserId", mainUserId);
												logger.info("부계정 로그인 - 부계정: {}, 메인 사용자: {}", userInfo.getUserId(), mainUserId);
											} else {
												session.setAttribute(Constants.SESSION_LOGIN_USER_ID, userInfo.getUserId());
												session.setAttribute("isSubAccount", false);
												logger.warn("부계정의 메인 사용자 ID를 찾을 수 없음 - 부계정: {}", userInfo.getUserId());
											}
										} else {
											// 메인 계정인 경우 자신의 ID를 loginUserId로 설정
											session.setAttribute(Constants.SESSION_LOGIN_USER_ID, userInfo.getUserId());
											session.setAttribute("isSubAccount", false);
											session.setAttribute("mainUserId", userInfo.getUserId());
										}
									} catch(Exception e) {
										logger.error("부계정 여부 확인 실패 - userId: {}, error: {}", userInfo.getUserId(), e.getMessage());
										session.setAttribute(Constants.SESSION_LOGIN_USER_ID, userInfo.getUserId());
										session.setAttribute("isSubAccount", false);
									}
								}

								// 로그인 성공 시 고객 정보의 토큰값 업데이트
								if(null != loginVO.getToken() && !"".equals(loginVO.getToken())) {
									loginService.updateUserInfo(loginVO);
								}

							} else {
								resultMap.put("resultCode", "999");
								resultMap.put("resultMessage", "회원 로그인 실패 - 사용자 정보 없음");
					}
				}
	} catch(Exception e) {
		unifiedErrorHandler.logError("로그아웃 처리", e);
	}
			} else {
					resultMap.put("resultCode", "999");
					resultMap.put("resultMessage", "회원 로그인 실패 - 사용자 아이디 없음");
				}
			} else {
				resultMap.put("resultCode", "999");
				resultMap.put("resultMessage", "회원 로그인 실패 - 사용자 비밀번호 없음");
			}
		} else {

		}
		
		return resultMap;
	}
	
	/**
	 * 회원 가입 페이지 호출
	 * @param req
	 * @param res
	 * @return
	 */
	@RequestMapping(value = "/login/join", method = RequestMethod.GET)
	public String join(
			HttpServletRequest req
			, HttpServletResponse res
	        , Model model) {

		logger.info("join");

		return "login/join";
	}
	
	/**
	 * 회원 가입 처리 요청
	 * @param req
	 * @param res
	 * @return
	 */
	@RequestMapping(value = "/login/joinProcess", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> joinProcess(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody UserInfo userInfo
			) {

		Map<String, Object> resultMap = new HashMap<String, Object>();

		HttpSession session = req.getSession();
		
		// 공통 검증 유틸리티 사용
		ValidationUtil.ValidationResult validationResult = ValidationUtil.validateUserInfo(
			userInfo.getUserId(), 
			userInfo.getUserNm(), 
			userInfo.getUserPass(), 
			userInfo.getUserEmail(), 
			userInfo.getUserTel()
		);
		
		if (!validationResult.isValid()) {
			return ResponseUtil.error(ResponseUtil.BAD_REQUEST_CODE, validationResult.getErrorMessage());
		}
		
		// XSS 및 SQL Injection 방지 검증
		ValidationUtil.ValidationResult securityResult = ValidationUtil.validateXssPrevention(userInfo.getUserId(), "사용자 ID");
		if (!securityResult.isValid()) {
			return ResponseUtil.error(ResponseUtil.BAD_REQUEST_CODE, securityResult.getErrorMessage());
		}
		
		securityResult = ValidationUtil.validateSqlInjectionPrevention(userInfo.getUserId(), "사용자 ID");
		if (!securityResult.isValid()) {
			return ResponseUtil.error(ResponseUtil.BAD_REQUEST_CODE, securityResult.getErrorMessage());
		}
		
		// 기존 개별 검증 로직 제거됨 - ValidationUtil로 통합
		Map<String, Object> joinMap;
		try {
			joinMap = loginService.insertUser(userInfo);
		} catch(Exception e) {
			unifiedErrorHandler.logError("회원 가입", e);
			return unifiedErrorHandler.createInternalServerErrorResponse("회원 가입 중 오류가 발생했습니다.");
		}
		
		if(null != joinMap && 0 < joinMap.size()) {
			if(String.valueOf(joinMap.get("result")).equals("success")) {
				UserInfo joinInfo = (UserInfo)joinMap.get("userInfo");
				resultMap.put("resultCode", "200");
				resultMap.put("resultMessage", "회원 가입 성공");
				resultMap.put("userInfo", joinInfo);

				// 회원 가입 성공 시 세션에 회원 정보 세팅
				sessionManagementService.setSessionInfo(session, joinInfo.getUserId(), joinInfo.getUserGrade(), joinInfo.getUserNm());
				session.setAttribute(Constants.SESSION_USER_TEL, joinInfo.getUserTel());
				session.setAttribute(Constants.SESSION_USER_EMAIL, joinInfo.getUserEmail());
			} else {
				resultMap.put("resultCode", "999");
				resultMap.put("resultMessage", "회원 가입 실패");
			}
		} else {
			resultMap.put("resultCode", "999");
			resultMap.put("resultMessage", "회원 가입 실패");
		}
		
		return resultMap;
	}
	
	/**
	 * 회원 아이디 찾기 페이지 호출
	 * @param req
	 * @param res
	 * @return
	 */
	@RequestMapping(value = "/login/findUserId", method = RequestMethod.GET)
	public String findUserId(
			HttpServletRequest req
			, HttpServletResponse res
			, Model model
	        ) {

		return "";
	}
	
	/**
	 * 회원 아이디 찾기 처리
	 * @param req
	 * @param res
	 * @return
	 */
	@RequestMapping(value = "/login/findUserIdProcess", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> findUserIdProcess(
			HttpServletRequest req
			, HttpServletResponse res
	        ) {

		Map<String, Object> resultMap = new HashMap<String, Object>();

		return resultMap;
	}
	
	/**
	 * 세션 연장 API
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @return Map<String, Object>
	 */
	@RequestMapping(value = "/extendSession", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> extendSession(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> resultMap = new HashMap<>();
		
		try {
			HttpSession session = request.getSession(false);
			
			if (session == null) {
				logger.warn("세션 연장 요청 - 세션이 존재하지 않음");
				resultMap.put("resultCode", "401");
				resultMap.put("resultMessage", "세션이 존재하지 않습니다.");
				return resultMap;
			}
			
			// 세션 유효성 검증
			if (!sessionManagementService.isValidSession(session)) {
				logger.warn("세션 연장 요청 - 유효하지 않은 세션");
				resultMap.put("resultCode", "401");
				resultMap.put("resultMessage", "세션이 만료되었습니다.");
				return resultMap;
			}
			
			// 세션 갱신
			sessionManagementService.refreshSession(session);
			
			// 마지막 로그인 시간 업데이트
			String userId = (String) session.getAttribute("userId");
			if (!StringUtil.isEmpty(userId)) {
				loginService.updateLastLoginTime(userId);
			}
			
			logger.info("세션 연장 완료 - userId: {}", userId);
			
			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "세션이 연장되었습니다.");
			resultMap.put("extendedAt", new java.util.Date());
			
		} catch (Exception e) {
			unifiedErrorHandler.logError("세션 연장", e);
			resultMap.put("resultCode", "500");
			resultMap.put("resultMessage", "세션 연장 중 오류가 발생했습니다.");
		}
		
		return resultMap;
	}
	
	/**
	 * 세션 확인 API (네트워크 기반 세션 검증)
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/api/auth/me", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> checkSession(HttpServletRequest req) {
		logger.info("=== 세션 확인 API 호출 ===");
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		try {
			HttpSession session = req.getSession(false);
			
			if (session == null) {
				logger.warn("세션이 존재하지 않음");
				resultMap.put("resultCode", "401");
				resultMap.put("resultMessage", "세션이 존재하지 않습니다.");
				return resultMap;
			}
			
			// 세션에서 사용자 정보 조회 (Constants 키 우선, 없으면 기본 키 사용)
			String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
			String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
			String userNm = (String) session.getAttribute(Constants.SESSION_USER_NM);
			
			// Constants 키에서 찾지 못하면 기본 키로 재시도
			if (StringUtil.isEmpty(userId)) {
				userId = (String) session.getAttribute("userId");
				logger.info("Constants 키에서 찾지 못해 기본 키로 재시도 - userId: {}", userId);
			}
			if (StringUtil.isEmpty(userGrade)) {
				userGrade = (String) session.getAttribute("userGrade");
				logger.info("Constants 키에서 찾지 못해 기본 키로 재시도 - userGrade: {}", userGrade);
			}
			if (StringUtil.isEmpty(userNm)) {
				userNm = (String) session.getAttribute("userNm");
				logger.info("Constants 키에서 찾지 못해 기본 키로 재시도 - userNm: {}", userNm);
			}
			
			if (StringUtil.isEmpty(userId)) {
				logger.warn("세션에 사용자 ID가 없음 - Constants: {}, 기본: {}", 
					session.getAttribute(Constants.SESSION_USER_ID), session.getAttribute("userId"));
				resultMap.put("resultCode", "401");
				resultMap.put("resultMessage", "인증되지 않은 사용자입니다.");
				return resultMap;
			}
			
			// 세션 유효성 확인 (세션 ID 존재 여부로 간단히 확인)
			if (session.getId() == null || session.getId().isEmpty()) {
				logger.warn("세션 ID가 없음 - userId: {}", userId);
				resultMap.put("resultCode", "401");
				resultMap.put("resultMessage", "세션이 유효하지 않습니다.");
				return resultMap;
			}
			
			// 세션 정보 반환
			Map<String, Object> userInfo = new HashMap<String, Object>();
			userInfo.put("userId", userId);
			userInfo.put("userGrade", userGrade);
			userInfo.put("userNm", userNm);
			userInfo.put("sessionId", session.getId());
			userInfo.put("lastAccessedTime", new java.util.Date(session.getLastAccessedTime()));
			
			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "세션이 유효합니다.");
			resultMap.put("userInfo", userInfo);
			
			logger.info("세션 확인 성공 - userId: {}, userGrade: {}, userNm: {}", userId, userGrade, userNm);
			
		} catch (Exception e) {
			unifiedErrorHandler.logError("세션 확인", e);
			resultMap.put("resultCode", "500");
			resultMap.put("resultMessage", "세션 확인 중 오류가 발생했습니다.");
		}
		
		return resultMap;
	}
}
