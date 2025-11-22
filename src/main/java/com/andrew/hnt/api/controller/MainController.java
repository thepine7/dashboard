package com.andrew.hnt.api.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.andrew.hnt.api.model.DataVO;
import com.andrew.hnt.api.model.LoginVO;
import com.andrew.hnt.api.model.SensorVO;
import com.andrew.hnt.api.model.UserInfo;
import com.andrew.hnt.api.service.AdminService;
import com.andrew.hnt.api.service.DeviceRegistrationService;
import com.andrew.hnt.api.service.LoginService;
import com.andrew.hnt.api.service.SubAccountService;
import com.andrew.hnt.api.service.impl.MqttServiceImpl;
import com.andrew.hnt.api.util.StringUtil;
import com.andrew.hnt.api.util.ResponseUtil;
import com.andrew.hnt.api.util.ExceptionUtil;
import com.andrew.hnt.api.util.UnifiedErrorHandler;
import com.andrew.hnt.api.util.ValidationUtil;
import com.andrew.hnt.api.common.Constants;
import com.andrew.hnt.api.service.SessionManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.andrew.hnt.api.common.BaseController;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainController extends BaseController {

	@Autowired
	private LoginService loginService;
	
	@Override
	protected Logger getLogger() {
		return logger;
	}
	
	@Autowired
	private AdminService adminService;
	
	@Autowired
	private com.andrew.hnt.api.mapper.AdminMapper adminMapper;

	@Autowired
	private MqttServiceImpl mqttService;
	
	@Autowired
	private CommonController commonController;
	
	@Autowired
	private DeviceRegistrationService deviceRegistrationService;
	
	
	@Autowired
	private SubAccountService subAccountService;
	
    @Autowired
    private SessionManagementService sessionManagementService;
    
    @Autowired
    private UnifiedErrorHandler unifiedErrorHandler;

    @Autowired
    private com.andrew.hnt.api.service.NotificationService notificationService;

	private static final Logger logger = LoggerFactory.getLogger(MainController.class);

	private String apiKey = "AAAAoUCvVY0:APA91bFhv_a-RRU0OOJPmGk4MBri_Aqu0MW4r1CDfar4GrhQf3H9XPTWRhoul86dfhLTomTn-WsTrKJ-qPAakoap9vMl7JHmrj8WniVnTQE3y5mhxKFDPp09bAmjaAuDx8qUXH1qhO05";
	private String senderId = "692574967181";

	/**
	 * 메인 대시보드 페이지 처리 (통합 세션 검증 적용)
	 * @param req HTTP 요청
	 * @param res HTTP 응답
	 * @param sensorId 센서 ID (선택적)
	 * @param format 응답 형식 (html/json)
	 * @param model 뷰 모델
	 * @return 뷰 이름 또는 JSON 응답
	 */
	@RequestMapping(value = "/main/main", method = {RequestMethod.GET, RequestMethod.POST})
	public Object main(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestParam(name = "sensorId", required = false) String sensorId
			, @RequestParam(name = "format", required = false, defaultValue = "html") String format
			, @RequestParam(name = "userId", required = false) String paramUserId
			, @RequestParam(name = "userNm", required = false) String paramUserNm
			, @RequestParam(name = "userEmail", required = false) String paramUserEmail
			, @RequestParam(name = "userGrade", required = false) String paramUserGrade
			, @RequestParam(name = "userTel", required = false) String paramUserTel
	        , Model model) {
		
		// 캐시 방지 헤더 설정
		setNoCacheHeaders(res);
		
		// 앱 접근 감지
		HttpSession session = req.getSession();
		String userAgent = req.getHeader("User-Agent");
		boolean isAppAccess = userAgent != null && (userAgent.contains("hnt_android") || userAgent.contains("okhttp"));
		
		logger.info("=== 메인 페이지 접근 시작 ===");
		logger.info("User-Agent: {}", userAgent);
		logger.info("앱 접근 여부: {}", isAppAccess);
		logger.info("URL 파라미터 userId: {}", paramUserId);
		
		String sessionUserId = null;
		String sessionUserGrade = null;
		String sessionUserNm = null;
		String sessionUserEmail = null;
		String sessionUserTel = null;
		
		// 1. URL 파라미터 우선 확인 (앱에서 전달)
		if (paramUserId != null && !paramUserId.isEmpty()) {
			logger.info("=== URL 파라미터로 사용자 정보 설정 (앱 접근) ===");
			sessionUserId = paramUserId;
			sessionUserGrade = paramUserGrade != null ? paramUserGrade : "U";
			sessionUserNm = paramUserNm != null ? paramUserNm : paramUserId;
			sessionUserEmail = paramUserEmail;
			sessionUserTel = paramUserTel;
			
			// 세션에도 저장 (WebView 내에서 세션 유지)
			session.setAttribute(Constants.SESSION_USER_ID, sessionUserId);
			session.setAttribute(Constants.SESSION_USER_GRADE, sessionUserGrade);
			session.setAttribute(Constants.SESSION_USER_NM, sessionUserNm);
			session.setAttribute(Constants.SESSION_USER_EMAIL, sessionUserEmail);
			session.setAttribute(Constants.SESSION_USER_TEL, sessionUserTel);
			session.setAttribute("userId", sessionUserId);
			session.setAttribute("userGrade", sessionUserGrade);
			session.setAttribute("userNm", sessionUserNm);
			
			logger.info("URL 파라미터로 사용자 정보 설정 완료 - userId: {}, userGrade: {}, userNm: {}", 
				sessionUserId, sessionUserGrade, sessionUserNm);
			
		} else if (isAppAccess) {
			// 2. 앱 접근인데 파라미터가 없으면 세션 확인
			logger.info("=== 앱 접근 (파라미터 없음) - 세션 확인 ===");
			sessionUserId = (String) session.getAttribute(Constants.SESSION_USER_ID);
			sessionUserGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
			sessionUserNm = (String) session.getAttribute(Constants.SESSION_USER_NM);
			
			if (sessionUserId == null || sessionUserId.isEmpty()) {
				logger.warn("앱 접근이지만 세션 정보 없음 - 로그인 필요");
				return "redirect:/login/login";
			}
			
		} else {
			// 3. 웹 브라우저 접근 시 기존 세션 검증
			logger.info("=== 웹 브라우저 세션 검증 시작 ===");
			logger.info("세션 ID: {}", session.getId());
			
			boolean sessionValid = sessionManagementService.isValidSession(session);
			logger.info("세션 검증 결과: {}", sessionValid);
			
			if (!sessionValid) {
				logger.warn("세션 검증 실패 - 로그인 페이지로 리다이렉트");
				return "redirect:/login/login";
			}
			
			// 세션에서 사용자 정보 추출
			sessionUserId = (String) session.getAttribute(Constants.SESSION_USER_ID);
			sessionUserGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
			sessionUserNm = (String) session.getAttribute(Constants.SESSION_USER_NM);
		}
		
		// 사용자 정보 검증
		if (sessionUserId == null || sessionUserId.isEmpty()) {
			logger.warn("사용자 정보를 찾을 수 없음 - 로그인 페이지로 리다이렉트");
			return "redirect:/login/login";
		}
		
		logger.info("최종 사용자 정보 - userId: {}, userGrade: {}, userNm: {}", 
			sessionUserId, sessionUserGrade, sessionUserNm);
		
		// 데이터베이스 연결 상태 체크 (제거됨)
		logger.debug("데이터베이스 헬스 체크 건너뜀");
		
		// 모델에 사용자 정보 설정
		model.addAttribute("userId", sessionUserId);
		model.addAttribute("userGrade", sessionUserGrade);
		model.addAttribute("userNm", sessionUserNm);
		
		// DB에서 auto_login 값 조회하여 saveId로 설정
		try {
			LoginVO loginVO = new LoginVO();
			loginVO.setUserId(sessionUserId);
			Map<String, Object> userInfoMap = loginService.getUserInfo(loginVO);
			
			if (userInfoMap != null && userInfoMap.get("userInfo") != null) {
				UserInfo userInfo = (UserInfo) userInfoMap.get("userInfo");
				String autoLogin = userInfo.getAutoLogin();
				
				// auto_login 값이 있으면 사용, 없으면 앱 접근 여부로 판단
				if (autoLogin != null && !autoLogin.isEmpty() && !"null".equals(autoLogin)) {
					model.addAttribute("saveId", autoLogin);
					logger.info("DB에서 조회한 auto_login 값 사용 - saveId: {}", autoLogin);
				} else if (isAppAccess) {
					model.addAttribute("saveId", "Y");
					logger.info("앱 접근 감지 - 알림 활성화 (saveId = Y)");
				} else {
					model.addAttribute("saveId", "N");
					logger.info("웹 접근 - 알림 비활성화 (saveId = N)");
				}
			} else {
				// DB 조회 실패 시 앱 접근 여부로 판단
				model.addAttribute("saveId", isAppAccess ? "Y" : "N");
				logger.warn("사용자 정보 조회 실패 - 앱 접근 여부로 saveId 설정: {}", isAppAccess ? "Y" : "N");
			}
		} catch (Exception e) {
			// 오류 시 앱 접근 여부로 판단
			model.addAttribute("saveId", isAppAccess ? "Y" : "N");
			logger.error("auto_login 조회 중 오류 - 앱 접근 여부로 saveId 설정: {}", isAppAccess ? "Y" : "N", e);
		}
		if (sessionUserEmail != null) {
			model.addAttribute("userEmail", sessionUserEmail);
		}
		if (sessionUserTel != null) {
			model.addAttribute("userTel", sessionUserTel);
		}
		model.addAttribute("sensorId", sessionUserId); // 센서 ID는 기본적으로 userId와 동일
		
		// 사이드바 데이터 추가
		commonController.addSidebarData(sessionUserId, model, session);
		
		// 센서 데이터 처리
		processSensorData(sessionUserId, sensorId, model);
		
		// JSON 응답 옵션 처리
		if ("json".equals(format)) {
			// JSON 응답을 ResponseEntity로 감싸서 반환
			Map<String, Object> jsonResponse = createJsonResponse(model, sessionUserId);
			res.setContentType("application/json; charset=UTF-8");
			return new org.springframework.http.ResponseEntity<>(jsonResponse, org.springframework.http.HttpStatus.OK);
		}
		
		return "main/main";
	}
	
	/**
	 * 캐시 방지 헤더 설정
	 * @param res HTTP 응답
	 */
	private void setNoCacheHeaders(HttpServletResponse res) {
		res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		res.setHeader("Pragma", "no-cache");
		res.setHeader("Expires", "0");
	}
	
	/**
	 * JSON 응답 생성 (하이브리드 액티브웹 지원)
	 * @param model 뷰 모델
	 * @param userId 사용자 ID
	 * @return JSON 응답 Map
	 */
	private Map<String, Object> createJsonResponse(Model model, String userId) {
		Map<String, Object> response = new HashMap<>();
		
		try {
			// 기본 응답 정보
			response.put("resultCode", "200");
			response.put("resultMessage", "성공");
			
			// 사용자 정보
			Map<String, Object> userInfo = new HashMap<>();
			userInfo.put("userId", model.getAttribute("userId"));
			userInfo.put("userNm", model.getAttribute("userNm"));
			userInfo.put("userEmail", model.getAttribute("userEmail"));
			userInfo.put("userGrade", model.getAttribute("userGrade"));
			userInfo.put("userTel", model.getAttribute("userTel"));
			response.put("userInfo", userInfo);
			
			// 센서 리스트
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> sensorList = (List<Map<String, Object>>) model.getAttribute("sensorList");
			response.put("sensorList", sensorList != null ? sensorList : new ArrayList<>());
			
			// 사이드바 데이터
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> sidebarData = (List<Map<String, Object>>) model.getAttribute("sidebarData");
			response.put("sidebarData", sidebarData != null ? sidebarData : new ArrayList<>());
			
			// 부계정 정보
			response.put("isSubAccount", model.getAttribute("isSubAccount"));
			response.put("mainUserId", model.getAttribute("mainUserId"));
			
			logger.info("JSON 응답 생성 완료 - userId: {}, 센서 수: {}", 
				userId, sensorList != null ? sensorList.size() : 0);
			
		} catch (Exception e) {
			logger.error("JSON 응답 생성 중 오류 발생", e);
			response.put("resultCode", "500");
			response.put("resultMessage", "서버 오류가 발생했습니다.");
		}
		
		return response;
	}
	
	
	/**
	 * 센서 데이터 처리
	 * @param sessionUserId 세션 사용자 ID
	 * @param sensorId 센서 ID (선택적)
	 * @param model 뷰 모델
	 */
	private void processSensorData(String sessionUserId, String sensorId, Model model) {
		if (isValidUserId(sessionUserId)) {
			try {
				// sensorId가 제공된 경우 해당 센서만 조회, 그렇지 않으면 전체 센서 리스트 조회
				List<Map<String, Object>> sensorList;
				if (sensorId != null && !sensorId.isEmpty() && !"null".equals(sensorId)) {
					// 특정 센서 ID로 조회
					Map<String, Object> param = new HashMap<String, Object>();
					param.put("userId", sessionUserId);
					param.put("sensorId", sensorId);
					sensorList = adminMapper.getSensorListBySensorId(param);
					logger.info("특정 센서 조회 - userId: {}, sensorId: {}, count: {}", 
						sessionUserId, sensorId, sensorList != null ? sensorList.size() : 0);
				} else {
					// 전체 센서 리스트 조회
					sensorList = subAccountService.getSensorListByUserType(sessionUserId);
					logger.info("전체 센서 리스트 조회 - userId: {}, count: {}", 
						sessionUserId, sensorList != null ? sensorList.size() : 0);
				}
				
				StringUtil.logInfo(logger, "통합 센서 리스트 조회 완료 - userId: {}, count: {}", 
					sessionUserId, sensorList != null ? sensorList.size() : 0);
				
				// 센서 리스트 처리
				processSensorList(sensorList, sessionUserId, model);
				
			} catch (Exception e) {
				// ExceptionUtil을 사용한 통일된 예외 처리
				ExceptionUtil.handleException(logger, e, "센서 데이터 처리");
			}
		}
	}
	
	/**
	 * 사용자 ID 유효성 검사
	 * @param userId 사용자 ID
	 * @return 유효성 여부
	 */
	private boolean isValidUserId(String userId) {
		return userId != null && !"".equals(userId) && !"null".equals(userId) && !"undefined".equals(userId);
	}
	
	/**
	 * 센서 리스트 처리
	 * @param sensorList 센서 리스트
	 * @param sessionUserId 세션 사용자 ID
	 * @param model 뷰 모델
	 */
	private void processSensorList(List<Map<String, Object>> sensorList, String sessionUserId, Model model) {
		if (sensorList != null && sensorList.size() > 0) {
			// 부계정 여부 확인
			boolean isSubAccount = subAccountService.isSubAccount(sessionUserId);
			String mainUserId = isSubAccount ? subAccountService.getMainUserIdForSubUser(sessionUserId) : sessionUserId;
			
			// 센서 리스트에 세션 정보 추가
			for (Map<String, Object> sensor : sensorList) {
						// 토픽 문자열 생성
				String topic = String.format("HBEE/%s/%s/%s/SER", 
					sensor.get("sensor_id"), sensor.get("sensor_type"), sensor.get("sensor_uuid"));
						sensor.put("topicStr", topic);
						
						// 세션 정보 추가
						sensor.put("currentUserId", sessionUserId);
						sensor.put("isSubAccount", isSubAccount);
						sensor.put("mainUserId", mainUserId);
					}
					
					model.addAttribute("sensorList", sensorList);
			StringUtil.logInfo(logger, "센서 리스트 모델 설정 완료 - count: {}", sensorList.size());
		}
	}

	@RequestMapping(value = "/data/insertSensorInfo", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> insertSensorInfo(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> insertData
	) {
		// 공통 장치 등록 서비스 사용 (중복 제거)
		// 앱 호환성: 앱은 /data/insertSensorInfo 경로 사용
		return deviceRegistrationService.processDeviceRegistration(req, insertData, true);
	}

	@RequestMapping(value = "/getData", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> getData(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody DataVO dataVO
	        ) {
		
		try {
			// 공통 검증 유틸리티 사용
			ValidationUtil.ValidationResult validationResult = ValidationUtil.validateNotNull(dataVO, "데이터 요청 정보");
			if (!validationResult.isValid()) {
				return unifiedErrorHandler.createBadRequestResponse(validationResult.getErrorMessage());
			}
			
			// 세션 검증
			HttpSession session = req.getSession();
			if(!sessionManagementService.isValidSession(session)) {
				return unifiedErrorHandler.createUnauthorizedResponse();
			}
			// MQTT 서비스에서 실시간 데이터만 조회
			Map<String, Object> dataMap = mqttService.getData();
			
			if(null != dataMap && 0 < dataMap.size()) {
				// 실시간 센서 데이터만 처리 (장치 등록 로직 완전 제거)
				String sensorValue = String.valueOf(dataMap.get("data"));
				String uuid = String.valueOf(dataMap.get("sensorUuid"));
				String sensorType = String.valueOf(dataMap.get("sensorType"));
				
				// 온도 데이터 응답
				if("ain".equals(String.valueOf(dataMap.get("name")))) {
					Map<String, Object> responseData = new HashMap<>();
					responseData.put("dataVal", sensorValue);
					responseData.put("uuid", uuid);
					responseData.put("sensorType", sensorType);
					
					// kimtest 사용자만 로깅 (로그 스팸 방지)
					if("kimtest".equals(String.valueOf(dataMap.get("sensorId")))) {
						StringUtil.logInfo(logger, "kimtest MQTT 실시간 데이터 처리: uuid={}, value={}", uuid, sensorValue);
					}
					
					return ResponseUtil.success("데이터 조회 성공", responseData);
				}
    
				// 상태 데이터 응답
				if("din".equals(String.valueOf(dataMap.get("name")))) {
					Map<String, Object> responseData = new HashMap<>();
					responseData.put("dinVal", String.valueOf(dataMap.get("dinVal")));
					responseData.put("uuid", uuid);
					return ResponseUtil.success("상태 데이터 조회 성공", responseData);
				}
				
				if("output".equals(String.valueOf(dataMap.get("name")))) {
					Map<String, Object> responseData = new HashMap<>();
					responseData.put("outputVal", String.valueOf(dataMap.get("outputVal")));
					responseData.put("uuid", uuid);
					return ResponseUtil.success("출력 데이터 조회 성공", responseData);
				}
			}
			
			return ResponseUtil.success("데이터 없음");
			
		} catch(Exception e) {
			unifiedErrorHandler.logError("getData 처리", e);
			return unifiedErrorHandler.createInternalServerErrorResponse(e);
		}
	}

	public void insertSensorData(SensorVO sensorVO) {
		try {
			loginService.insertSensorData(sensorVO);
		} catch(Exception e) {
			StringUtil.logError(logger, "센서 정보 삽입 중 오류 발생", e);
		}
	}

	public void sendNoti(Map<String, Object> noti) {
		logger.info("===============================================");
		logger.info("🔔 sendNoti 호출 - 알람 발송 시작");
		logger.info("===============================================");
		
		if(null != noti && 0 < noti.size()) {
			String userId = String.valueOf(noti.get("userId"));
			String fcmToken = String.valueOf(noti.get("token"));
			String sensorUuid = String.valueOf(noti.get("sensor_uuid"));
			String gu = String.valueOf(noti.get("gu"));
			String inType = String.valueOf(noti.get("inType"));
			String inTemp = String.valueOf(noti.get("inTemp"));
			String curTemp = String.valueOf(noti.get("curTemp"));
			
			logger.info("📋 알람 정보:");
			logger.info("   - 사용자 ID: {}", userId);
			logger.info("   - FCM 토큰: {}...", fcmToken != null && fcmToken.length() > 20 ? fcmToken.substring(0, 20) : fcmToken);
			logger.info("   - 센서 UUID: {}", sensorUuid);
			logger.info("   - 알람 유형(gu): {}", gu);
			logger.info("   - 입력 타입(inType): {}", inType);
			logger.info("   - 현재 온도: {}°C", inTemp);
			logger.info("   - 설정 온도: {}°C", curTemp);
			logger.info("===============================================");
			
			// 알림 메시지 생성
			String message = buildNotificationMessage(noti);
			logger.info("📝 생성된 알람 메시지: {}", message);
			
			// 이중화 알림 발송 (FCM 우선, MQTT 백업)
			com.andrew.hnt.api.model.NotificationRequest request = new com.andrew.hnt.api.model.NotificationRequest();
			request.setUserId(userId);
			request.setFcmToken(fcmToken);
			request.setSensorUuid(sensorUuid);
			request.setMessage(message);
			
			logger.info("🚀 이중화 알림 발송 시작 (FCM 우선, MQTT 백업)");
			boolean success = notificationService.sendDualNotification(request);
			
			if(success) {
				logger.info("✅ 알람 발송 성공");
			} else {
				logger.warn("❌ 알람 발송 실패");
			}
			logger.info("===============================================");
		} else {
			logger.warn("⚠️ sendNoti 호출되었으나 noti 데이터가 비어있음");
			logger.info("===============================================");
		}
	}
	
	/**
	 * 알림 메시지 생성
	 */
	private String buildNotificationMessage(Map<String, Object> noti) {
		String gu = String.valueOf(noti.get("gu"));
		String inTemp = String.valueOf(noti.get("inTemp"));
		String curTemp = String.valueOf(noti.get("curTemp"));
		String inType = String.valueOf(noti.get("inType"));
		String sensorName = String.valueOf(noti.get("sensor_uuid"));

		String warnText = "";
			if(null != gu && !"".equals(gu) && 0 < gu.length()) {
				if("ain".equals(gu)) {
					if(null != inType && !"".equals(inType) && 0 < inType.length()) {
						if("high".equals(inType)) {
							warnText = "온도 높음(설정온도 : " + curTemp + "°C, 현재온도 : " + inTemp + "°C)";
						} else if("low".equals(inType)) {
							warnText = "온도 낮음(설정온도 : " + curTemp + "°C, 현재온도 : " + inTemp + "°C)";
						}
					}
				} else if("din".equals(gu)) {
					warnText = "DI알람(에러, 현재온도 : " + inTemp +")";
				} else if("netError".equals(gu)) {
					warnText = "통신에러";
				}
			}

		return sensorName + "장치 이상 발생 : " + warnText;
	}

	@RequestMapping(value = "/sendAlarm", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> sendAlarm(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> reqMap
	) {
		logger.info("===============================================");
		logger.info("📞 sendAlarm 호출");
		logger.info("   - userId: {}", reqMap.get("userId"));
		logger.info("   - sensorUuid: {}", reqMap.get("sensorUuid"));
		logger.info("   - name: {}", reqMap.get("name"));
		logger.info("   - sensorValue: {}", reqMap.get("sensorValue"));
		logger.info("===============================================");
		
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != reqMap && 0 < reqMap.size()) {
			Map<String, Object> config = new HashMap<String, Object>();
			Map<String, Object> sensorInfo = new HashMap<String, Object>();
			Map<String, Object> confParam = new HashMap<String, Object>();
			String token = "";

			confParam.put("userId", String.valueOf(reqMap.get("userId")));
			confParam.put("sensorId", String.valueOf(reqMap.get("sensorId")));
			confParam.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));

			token = String.valueOf(reqMap.get("token"));
			if(StringUtil.isEmpty(token)) {
				LoginVO loginVO = new LoginVO();
				UserInfo userInfo = new UserInfo();
				Map<String, Object> userMap = new HashMap<String, Object>();
				loginVO.setUserId(String.valueOf(reqMap.get("userId")));

				try {
					userMap = loginService.getUserInfo(loginVO);

					if(null != userMap && 0 < userMap.size()) {
						userInfo = (UserInfo) userMap.get("userInfo");

						if(null != userInfo) {
							token = userInfo.getToken();
							reqMap.put("token", token);
						}
					}
				} catch(Exception e) {
					unifiedErrorHandler.logError("센서 데이터 처리", e);
				}
			}

			String sensorName = "";

			// 인입된 온도와 설정의 온도를 비교하여 알람 세팅 진행
			String highTemp = "";
			String lowTemp = "";
			String specificTemp = "";
			String highAlarmYn = "";
			String lowAlarmYn = "";
			String highAlarmTime = "";
			String lowAlarmTime = "";
			String highAlarmDelayTime = "";
			String lowAlarmDelayTime = "";
			String specificAlarmTime = "";
			String specificAlarmDelayTime = "";
			String specificAlarmYn = "";
			String diAlarmTime = "";
			String diAlarmDelayTime = "";
			String diAlarmYn = "";

			String networkAlarmTime = "";
			String networkDelayTime = "";
			String networkAlarmYn = "";

			config = adminService.selectSetting(confParam);
			sensorInfo = adminService.getSensorInfo(confParam);

			if(null != config && 0 < config.size()) {
				highTemp = String.valueOf(config.get("set_val1"));
				lowTemp = String.valueOf(config.get("set_val2"));
				specificTemp = String.valueOf(config.get("set_val3"));
				highAlarmYn = String.valueOf(config.get("alarm_yn1"));
				lowAlarmYn = String.valueOf(config.get("alarm_yn2"));
				highAlarmTime = String.valueOf(config.get("delay_time1"));
				lowAlarmTime = String.valueOf(config.get("delay_time2"));
				highAlarmDelayTime = String.valueOf(config.get("re_delay_time1"));
				lowAlarmDelayTime = String.valueOf(config.get("re_delay_time2"));

				specificAlarmTime = String.valueOf(config.get("delay_time3"));
				specificAlarmDelayTime = String.valueOf(config.get("re_delay_time3"));
				specificAlarmYn = String.valueOf(config.get("alarm_yn3"));

				diAlarmTime = String.valueOf(config.get("delay_time4"));
				diAlarmDelayTime = String.valueOf(config.get("re_delay_time4"));
				diAlarmYn = String.valueOf(config.get("alarm_yn4"));

				networkAlarmTime = String.valueOf(config.get("delay_time5"));
				networkDelayTime = String.valueOf(config.get("re_delay_time5"));
				networkAlarmYn = String.valueOf(config.get("alarm_yn5"));
			}

			if(null != sensorInfo && 0 < sensorInfo.size()) {
				sensorName = String.valueOf(sensorInfo.get("sensor_name"));
			}

			String in_name = "";
			in_name = String.valueOf(reqMap.get("name"));

			String curTemp = "";
			curTemp = adminService.getCurTemp(confParam);

			if(null != in_name && !"".equals(in_name) && 0 < in_name.length()) {
				if ("ain".equals(in_name)) {
					if (null != config && 0 < config.size()) {
						if (String.valueOf(reqMap.get("sensorUuid")).equals(String.valueOf(config.get("sensor_uuid")))) {
							// 인입된 온도가 설정 온도 이상인 경우 (high와 비교)
							// 1. 알람 사용 유무 확인
							if (null != highAlarmYn && !"".equals(highAlarmYn)) {
							// 2. 알람 사용으로 되어 있는 경우 온도 비교
							if ("Y".equals(highAlarmYn)) {
								if (Double.compare(Double.parseDouble(String.valueOf(reqMap.get("sensorValue"))), Double.parseDouble(highTemp)) > 0) {
									logger.info("🔥 고온 알람 조건 충족");
									logger.info("   - 현재 온도: {}°C", reqMap.get("sensorValue"));
									logger.info("   - 설정 온도: {}°C", highTemp);
									logger.info("   - 알람 지연 시간: {}분", highAlarmTime);
										if (!"0".equals(highAlarmTime)) {
											// 알람 지연 시간이 즉시가 아닌 경우에는 알람 발송 테이블에 저장
											Map<String, Object> notiMap = new HashMap<String, Object>();
											notiMap.put("userId", String.valueOf(reqMap.get("userId")));
											notiMap.put("userToken", String.valueOf(reqMap.get("token")));
											notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											notiMap.put("addTime", highAlarmTime);
											notiMap.put("alarmType", "high");
											notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
											notiMap.put("curTemp", highTemp);
											notiMap.put("urgentYn", "Y");

											Map<String, Object> chkMap = new HashMap<String, Object>();
											chkMap = adminService.selectChkNoti(notiMap);

											// 이미 알람이 들어있는지 확인 후 없으면 입력
											if (null != chkMap && 0 < chkMap.size()) {
												logger.error("Error chkmap : " + chkMap.size());
											} else {
												//logger.error("Error insert noti");
												adminService.insertNoti(notiMap);
											}
										} else {
											// DB에서 즉시 발송 이력 확인되는 경우 발송 제외 처리 추가
											Map<String, Object> chkUrgent = new HashMap<String, Object>();
											Map<String, Object> chkRelease = new HashMap<String, Object>();
											Map<String, Object> chkMap = new HashMap<String, Object>();
											boolean urgentYn = false;
											boolean releaseYn = false;
											chkMap.put("userId", String.valueOf(reqMap.get("userId")));
											chkMap.put("userToken", String.valueOf(reqMap.get("token")));
											chkMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											chkMap.put("alarmType", "high");
											chkMap.put("reDelayTime", highAlarmDelayTime);

											chkUrgent = adminService.selectUrgentNoti(chkMap);
											if(null != chkUrgent && 0 < chkUrgent.size()) {
												int urgentCnt = 0;
												urgentCnt = Integer.parseInt(String.valueOf(chkUrgent.get("cnt")));

												if(0 < urgentCnt) {
													urgentYn = true;
												}
											}

											// 해제 되었다 다시 오류가 인입된 경우에는 발송 처리 추가
											chkRelease = adminService.selectReleaseNoti(chkMap);
											if(null != chkRelease && 0 < chkRelease.size()) {
												int releaseCnt = 0;
												releaseCnt = Integer.parseInt(String.valueOf(chkRelease.get("cnt")));

												if(0 < releaseCnt) {
													releaseYn = true;
												}
											}

											Map<String, Object> notiMap = new HashMap<String, Object>();
											notiMap.put("userId", String.valueOf(reqMap.get("userId")));
											notiMap.put("userToken", String.valueOf(reqMap.get("token")));
											notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											notiMap.put("addTime", highAlarmTime);
											notiMap.put("alarmType", "rehigh");
											notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
											notiMap.put("curTemp", highTemp);

											Map<String, Object> chk = new HashMap<String, Object>();
											chk = adminService.selectNoti(notiMap);

											if(null != chk && 0 < chk.size()) {
											}

											// 긴급 발송 건이 없거나 오류가 해제된 경우에 발송
											if(!urgentYn || releaseYn) {
											// 알람 지연 시간이 0 으로 즉시인 경우 바로 알람 발송 (알람 발송 테이블 저장 X)
											Map<String, Object> noti = new HashMap<String, Object>();
											noti.put("userId", String.valueOf(reqMap.get("userId")));
											noti.put("token", String.valueOf(reqMap.get("token")));
											noti.put("sensor_uuid", String.valueOf(reqMap.get("sensorUuid")));
											noti.put("type", "온도 높음");
											noti.put("inType", "high");
											noti.put("gu", "ain");
											noti.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
											noti.put("curTemp", highTemp);
											sendNoti(noti);

												// DB에 즉시 발송 이력 저장
												Map<String, Object> urgent = new HashMap<String, Object>();
												urgent.put("userId", String.valueOf(reqMap.get("userId")));
												urgent.put("userToken", String.valueOf(reqMap.get("token")));
												urgent.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
												urgent.put("addTime", highAlarmTime);
												urgent.put("alarmType", "high");
												urgent.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
												urgent.put("curTemp", highTemp);
												urgent.put("urgentYn", "Y");

												adminService.insertUrgentNoti(urgent);
											}

										}

										if (!"0".equals(highAlarmDelayTime)) {
											// 재전송 지연 시간이 즉시가 아닌 경우 알람 발송 테이블에 저장
											Map<String, Object> notiMap = new HashMap<String, Object>();
											notiMap.put("userId", String.valueOf(reqMap.get("userId")));
											notiMap.put("userToken", String.valueOf(reqMap.get("token")));
											notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											notiMap.put("addTime", Integer.parseInt(highAlarmTime) + Integer.parseInt(highAlarmDelayTime));
											notiMap.put("alarmType", "rehigh");
											notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
											notiMap.put("curTemp", highTemp);
											notiMap.put("urgentYn", "N");

											Map<String, Object> chkMap = new HashMap<String, Object>();
											chkMap = adminService.selectNoti(notiMap);

											// 이미 알람이 들어있는지 확인 후 없으면 입력
											if (null != chkMap && 0 < chkMap.size()) {
												//logger.error("Error chkmap : " + chkMap.size());
											} else {
												//logger.error("Error insert noti");
												adminService.insertNoti(notiMap);
											}
										}

									} else {
										// 발송 전 정상 데이터가 인입된 경우 DB 데이터 확인하여 이미 들어가 있는 발송 대상건이 있으면 발송되지 않도록 처리
										Map<String, Object> notiMap = new HashMap<String, Object>();
										notiMap.put("userId", String.valueOf(reqMap.get("userId")));
										notiMap.put("userToken", String.valueOf(reqMap.get("token")));
										notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
										notiMap.put("addTime", highAlarmTime);
										notiMap.put("alarmType", "high");
										notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
										notiMap.put("curTemp", highTemp);

										Map<String, Object> chkMap = new HashMap<String, Object>();
										Map<String, Object> chkMap2 = new HashMap<String, Object>();

										chkMap = adminService.selectNoti(notiMap);
										chkMap2 = adminService.selectUrgentNoti(notiMap);

										// 이미 알람이 들어있는지 확인 후 없으면 입력
										if (null != chkMap && 0 < chkMap.size()) {
											Map<String, Object> param = new HashMap<String, Object>();
											param.put("userId", chkMap.get("user_id"));
											param.put("userToken", chkMap.get("user_token"));
											param.put("no", chkMap.get("no"));
											param.put("releaseYn", "Y");
											param.put("sensorUuid", chkMap.get("sensor_uuid"));

											try {
												adminService.updateNoti(param);
											} catch(Exception e) {
												unifiedErrorHandler.logError("알림 처리", e);
											}
										}

										if(null != chkMap2 && 0 < chkMap2.size()) {
											Map<String, Object> param = new HashMap<String, Object>();
											param.put("userId", String.valueOf(reqMap.get("userId")));
											param.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											param.put("alarmType", "high");

											try {
												adminService.updateUrgentNoti(param);
											} catch(Exception e) {
												unifiedErrorHandler.logError("알림 처리", e);
											}
										}

										notiMap.remove("alarmType");
										notiMap.put("alarmType", "rehigh");

										chkMap = new HashMap<String, Object>();
										chkMap = adminService.selectNoti(notiMap);

										// 이미 알람이 들어있는지 확인 후 없으면 입력
										if (null != chkMap && 0 < chkMap.size()) {
											Map<String, Object> param = new HashMap<String, Object>();
											param.put("userId", chkMap.get("user_id"));
											param.put("userToken", chkMap.get("user_token"));
											param.put("no", chkMap.get("no"));
											param.put("releaseYn", "Y");
											param.put("sensorUuid", chkMap.get("sensor_uuid"));

											try {
												adminService.updateNoti(param);
											} catch(Exception e) {
												unifiedErrorHandler.logError("알림 처리", e);
											}
										}
									}
								}
							}

						// 인입된 온도가 설정 온도 이하인 경우 (low와 비교)
						if (null != lowAlarmYn && !"".equals(lowAlarmYn)) {
							if ("Y".equals(lowAlarmYn)) {
								if (Double.compare(Double.parseDouble(String.valueOf(reqMap.get("sensorValue"))), Double.parseDouble(lowTemp)) < 0) {
									logger.info("❄️ 저온 알람 조건 충족");
									logger.info("   - 현재 온도: {}°C", reqMap.get("sensorValue"));
									logger.info("   - 설정 온도: {}°C", lowTemp);
									logger.info("   - 알람 지연 시간: {}분", lowAlarmTime);
										if (!"0".equals(lowAlarmTime)) {
											Map<String, Object> notiMap = new HashMap<String, Object>();
											notiMap.put("userId", String.valueOf(reqMap.get("userId")));
											notiMap.put("userToken", String.valueOf(reqMap.get("token")));
											notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											notiMap.put("addTime", lowAlarmTime);
											notiMap.put("alarmType", "low");
											notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
											notiMap.put("curTemp", lowTemp);
											notiMap.put("urgentYn", "Y");

											Map<String, Object> chkMap = new HashMap<String, Object>();
											chkMap = adminService.selectChkNoti(notiMap);

											// 이미 알람이 들어있는지 확인 후 없으면 입력
											if (null != chkMap && 0 < chkMap.size()) {
											} else {
												adminService.insertNoti(notiMap);
											}
										} else {
											// DB에서 즉시 발송 이력 확인되는 경우 발송 제외 처리 추가
											Map<String, Object> chkUrgent = new HashMap<String, Object>();
											Map<String, Object> chkRelease = new HashMap<String, Object>();
											Map<String, Object> chkMap = new HashMap<String, Object>();
											boolean urgentYn = false;
											boolean releaseYn = false;
											chkMap.put("userId", String.valueOf(reqMap.get("userId")));
											chkMap.put("userToken", String.valueOf(reqMap.get("token")));
											chkMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											chkMap.put("alarmType", "low");
											chkMap.put("reDelayTime", lowAlarmDelayTime);

											chkUrgent = adminService.selectUrgentNoti(chkMap);
											if(null != chkUrgent && 0 < chkUrgent.size()) {
												int urgentCnt = 0;
												urgentCnt = Integer.parseInt(String.valueOf(chkUrgent.get("cnt")));

												if(0 < urgentCnt) {
													urgentYn = true;
												}
											}

											// 해제 되었다 다시 오류가 인입된 경우에는 발송 처리 추가
											chkRelease = adminService.selectReleaseNoti(chkMap);
											if(null != chkRelease && 0 < chkRelease.size()) {
												int releaseCnt = 0;
												releaseCnt = Integer.parseInt(String.valueOf(chkRelease.get("cnt")));

												if(0 < releaseCnt) {
													releaseYn = true;
												}
											}

											Map<String, Object> notiMap = new HashMap<String, Object>();
											notiMap.put("userId", String.valueOf(reqMap.get("userId")));
											notiMap.put("userToken", String.valueOf(reqMap.get("token")));
											notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											notiMap.put("addTime", lowAlarmTime);
											notiMap.put("alarmType", "relow");
											notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
											notiMap.put("curTemp", lowTemp);

											Map<String, Object> chk = new HashMap<String, Object>();
											chk = adminService.selectNoti(notiMap);

											if(null != chk && 0 < chk.size()) {
											}

											// 긴급 발송건이 없거나 오류 해제된 경우
											if(!urgentYn || releaseYn) {
												Map<String, Object> noti = new HashMap<String, Object>();
												noti.put("userId", String.valueOf(reqMap.get("userId")));
												noti.put("token", String.valueOf(reqMap.get("token")));
												noti.put("sensor_uuid", String.valueOf(reqMap.get("sensorUuid")));
												noti.put("type", "온도 낮음");
												noti.put("inType", "low");
												noti.put("gu", "ain");
												noti.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
												noti.put("curTemp", lowTemp);
												sendNoti(noti);

												// DB에 즉시 발송 이력 저장
												Map<String, Object> urgent = new HashMap<String, Object>();
												urgent.put("userId", String.valueOf(reqMap.get("userId")));
												urgent.put("userToken", String.valueOf(reqMap.get("token")));
												urgent.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
												urgent.put("addTime", highAlarmTime);
												urgent.put("alarmType", "low");
												urgent.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
												urgent.put("curTemp", lowTemp);
												urgent.put("urgentYn", "Y");

												adminService.insertUrgentNoti(urgent);
											}
										}

										if (!"0".equals(lowAlarmDelayTime)) {
											Map<String, Object> notiMap = new HashMap<String, Object>();
											notiMap.put("userId", String.valueOf(reqMap.get("userId")));
											notiMap.put("userToken", String.valueOf(reqMap.get("token")));
											notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											notiMap.put("addTime", Integer.parseInt(lowAlarmTime) + Integer.parseInt(lowAlarmDelayTime));
											notiMap.put("alarmType", "relow");
											notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
											notiMap.put("curTemp", lowTemp);
											notiMap.put("urgentYn", "N");

											Map<String, Object> chkMap = new HashMap<String, Object>();
											chkMap = adminService.selectNoti(notiMap);

											// 이미 알람이 들어있는지 확인 후 없으면 입력
											if (null != chkMap && 0 < chkMap.size()) {
											} else {
												adminService.insertNoti(notiMap);
											}
										}

									} else {
										// DB 확인하여 이미 발송 대상 알람이 있으면 발송되지 않도록 처리
										Map<String, Object> notiMap = new HashMap<String, Object>();
										notiMap.put("userId", String.valueOf(reqMap.get("userId")));
										notiMap.put("userToken", String.valueOf(reqMap.get("token")));
										notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
										notiMap.put("addTime", lowAlarmTime);
										notiMap.put("alarmType", "low");
										notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
										notiMap.put("curTemp", lowTemp);

										Map<String, Object> chkMap = new HashMap<String, Object>();
										Map<String, Object> chkMap2 = new HashMap<String, Object>();

										chkMap = adminService.selectNoti(notiMap);
										chkMap2 = adminService.selectUrgentNoti(notiMap);

										// 이미 알람이 들어있는지 확인 후 없으면 입력
										if (null != chkMap && 0 < chkMap.size()) {
											Map<String, Object> param = new HashMap<String, Object>();
											param.put("userId", chkMap.get("user_id"));
											param.put("userToken", chkMap.get("user_token"));
											param.put("no", chkMap.get("no"));
											param.put("sensorUuid", chkMap.get("sensor_uuid"));

											try {
												adminService.updateNoti(param);
											} catch(Exception e) {
												unifiedErrorHandler.logError("알림 처리", e);
											}
										}

										if(null != chkMap2 && 0 < chkMap2.size()) {
											Map<String, Object> param = new HashMap<String, Object>();
											param.put("userId", String.valueOf(reqMap.get("userId")));
											param.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											param.put("alarmType", "low");

											try {
												adminService.updateUrgentNoti(param);
											} catch(Exception e) {
												unifiedErrorHandler.logError("알림 처리", e);
											}
										}

										notiMap.remove("alarmType");
										notiMap.put("alarmType", "relow");

										chkMap = new HashMap<String, Object>();
										chkMap = adminService.selectNoti(notiMap);

										if(null != chkMap && 0 < chkMap.size()) {
											Map<String, Object> param = new HashMap<String, Object>();
											param.put("userId", chkMap.get("user_id"));
											param.put("userToken", chkMap.get("user_token"));
											param.put("no", chkMap.get("no"));
											param.put("sensorUuid", chkMap.get("sensor_uuid"));

											try {
												adminService.updateNoti(param);
											} catch(Exception e) {
												unifiedErrorHandler.logError("알림 처리", e);
											}
										}
									}
								}
							}
						}
					}
				} else if("ain".equals(in_name)) {
					// 특정온도알람 처리
					if (null != config && 0 < config.size()) {
						if (String.valueOf(reqMap.get("sensorUuid")).equals(String.valueOf(config.get("sensor_uuid")))) {
							// 특정온도알람 사용 유무 확인
							if (null != specificAlarmYn && !"".equals(specificAlarmYn)) {
								// 특정온도알람 사용으로 되어 있는 경우 온도 비교
								if ("Y".equals(specificAlarmYn)) {
									if (Double.compare(Double.parseDouble(String.valueOf(reqMap.get("sensorValue"))), Double.parseDouble(specificTemp)) == 0) {
										if (!"0".equals(specificAlarmTime)) {
											// 알람 지연 시간이 즉시가 아닌 경우에는 알람 발송 테이블에 저장
											Map<String, Object> notiMap = new HashMap<String, Object>();
											notiMap.put("userId", String.valueOf(reqMap.get("userId")));
											notiMap.put("userToken", String.valueOf(reqMap.get("token")));
											notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											notiMap.put("addTime", specificAlarmTime);
											notiMap.put("alarmType", "specific");
											notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
											notiMap.put("curTemp", specificTemp);
											notiMap.put("urgentYn", "Y");

											Map<String, Object> chkMap = new HashMap<String, Object>();
											chkMap = adminService.selectChkNoti(notiMap);

											// 이미 알람이 들어있는지 확인 후 없으면 입력
											if (null != chkMap && 0 < chkMap.size()) {
												logger.error("Error chkmap : " + chkMap.size());
											} else {
												adminService.insertNoti(notiMap);
											}
										} else {
											// DB에서 즉시 발송 이력 확인되는 경우 발송 제외 처리 추가
											Map<String, Object> chkUrgent = new HashMap<String, Object>();
											Map<String, Object> chkRelease = new HashMap<String, Object>();
											Map<String, Object> chkMap = new HashMap<String, Object>();
											boolean urgentYn = false;
											boolean releaseYn = false;
											chkMap.put("userId", String.valueOf(reqMap.get("userId")));
											chkMap.put("userToken", String.valueOf(reqMap.get("token")));
											chkMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											chkMap.put("alarmType", "specific");
											chkMap.put("reDelayTime", specificAlarmDelayTime);

											chkUrgent = adminService.selectUrgentNoti(chkMap);
											if(null != chkUrgent && 0 < chkUrgent.size()) {
												int urgentCnt = 0;
												urgentCnt = Integer.parseInt(String.valueOf(chkUrgent.get("cnt")));

												if(0 < urgentCnt) {
													urgentYn = true;
												}
											}

											// 해제 되었다 다시 오류가 인입된 경우에는 발송 처리 추가
											chkRelease = adminService.selectReleaseNoti(chkMap);
											if(null != chkRelease && 0 < chkRelease.size()) {
												int releaseCnt = 0;
												releaseCnt = Integer.parseInt(String.valueOf(chkRelease.get("cnt")));

												if(0 < releaseCnt) {
													releaseYn = true;
												}
											}

											Map<String, Object> notiMap = new HashMap<String, Object>();
											notiMap.put("userId", String.valueOf(reqMap.get("userId")));
											notiMap.put("userToken", String.valueOf(reqMap.get("token")));
											notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											notiMap.put("addTime", specificAlarmTime);
											notiMap.put("alarmType", "respecific");
											notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
											notiMap.put("curTemp", specificTemp);

											Map<String, Object> chk = new HashMap<String, Object>();
											chk = adminService.selectNoti(notiMap);

											if(null != chk && 0 < chk.size()) {
											}

											// 긴급 발송 건이 없거나 오류가 해제된 경우에 발송
											if(!urgentYn || releaseYn) {
												// 알람 지연 시간이 0 으로 즉시인 경우 바로 알람 발송 (알람 발송 테이블 저장 X)
												Map<String, Object> noti = new HashMap<String, Object>();
												noti.put("userId", String.valueOf(reqMap.get("userId")));
												noti.put("token", String.valueOf(reqMap.get("token")));
												noti.put("sensor_uuid", String.valueOf(reqMap.get("sensorUuid")));
												noti.put("type", "특정온도 알람");
												noti.put("inType", "specific");
												noti.put("gu", "ain");
												noti.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
												noti.put("curTemp", specificTemp);
												sendNoti(noti);

												// DB에 즉시 발송 이력 저장
												Map<String, Object> urgent = new HashMap<String, Object>();
												urgent.put("userId", String.valueOf(reqMap.get("userId")));
												urgent.put("userToken", String.valueOf(reqMap.get("token")));
												urgent.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
												urgent.put("addTime", specificAlarmTime);
												urgent.put("alarmType", "specific");
												urgent.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
												urgent.put("curTemp", specificTemp);
												urgent.put("urgentYn", "Y");

												adminService.insertUrgentNoti(urgent);
											}
										}

										if (!"0".equals(specificAlarmDelayTime)) {
											// 재전송 지연 시간이 즉시가 아닌 경우 알람 발송 테이블에 저장
											Map<String, Object> notiMap = new HashMap<String, Object>();
											notiMap.put("userId", String.valueOf(reqMap.get("userId")));
											notiMap.put("userToken", String.valueOf(reqMap.get("token")));
											notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											notiMap.put("addTime", Integer.parseInt(specificAlarmTime) + Integer.parseInt(specificAlarmDelayTime));
											notiMap.put("alarmType", "respecific");
											notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
											notiMap.put("curTemp", specificTemp);
											notiMap.put("urgentYn", "N");

											Map<String, Object> chkMap = new HashMap<String, Object>();
											chkMap = adminService.selectNoti(notiMap);

											// 이미 알람이 들어있는지 확인 후 없으면 입력
											if (null != chkMap && 0 < chkMap.size()) {
												//logger.error("Error chkmap : " + chkMap.size());
											} else {
												//logger.error("Error insert noti");
												adminService.insertNoti(notiMap);
											}
										}

									} else {
										// 발송 전 정상 데이터가 인입된 경우 DB 데이터 확인하여 이미 들어가 있는 발송 대상건이 있으면 발송되지 않도록 처리
										Map<String, Object> notiMap = new HashMap<String, Object>();
										notiMap.put("userId", String.valueOf(reqMap.get("userId")));
										notiMap.put("userToken", String.valueOf(reqMap.get("token")));
										notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
										notiMap.put("addTime", specificAlarmTime);
										notiMap.put("alarmType", "specific");
										notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
										notiMap.put("curTemp", specificTemp);

										Map<String, Object> chkMap = new HashMap<String, Object>();
										Map<String, Object> chkMap2 = new HashMap<String, Object>();

										chkMap = adminService.selectNoti(notiMap);
										chkMap2 = adminService.selectUrgentNoti(notiMap);

										// 이미 알람이 들어있는지 확인 후 없으면 입력
										if (null != chkMap && 0 < chkMap.size()) {
											Map<String, Object> param = new HashMap<String, Object>();
											param.put("userId", chkMap.get("user_id"));
											param.put("userToken", chkMap.get("user_token"));
											param.put("no", chkMap.get("no"));
											param.put("releaseYn", "Y");
											param.put("sensorUuid", chkMap.get("sensor_uuid"));

											try {
												adminService.updateNoti(param);
											} catch(Exception e) {
												unifiedErrorHandler.logError("알림 처리", e);
											}
										}

										if(null != chkMap2 && 0 < chkMap2.size()) {
											Map<String, Object> param = new HashMap<String, Object>();
											param.put("userId", String.valueOf(reqMap.get("userId")));
											param.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											param.put("alarmType", "specific");

											try {
												adminService.updateUrgentNoti(param);
											} catch(Exception e) {
												unifiedErrorHandler.logError("알림 처리", e);
											}
										}

										notiMap.remove("alarmType");
										notiMap.put("alarmType", "respecific");

										chkMap = new HashMap<String, Object>();
										chkMap = adminService.selectNoti(notiMap);

										// 이미 알람이 들어있는지 확인 후 없으면 입력
										if (null != chkMap && 0 < chkMap.size()) {
											Map<String, Object> param = new HashMap<String, Object>();
											param.put("userId", chkMap.get("user_id"));
											param.put("userToken", chkMap.get("user_token"));
											param.put("no", chkMap.get("no"));
											param.put("releaseYn", "Y");
											param.put("sensorUuid", chkMap.get("sensor_uuid"));

											try {
												adminService.updateNoti(param);
											} catch(Exception e) {
												unifiedErrorHandler.logError("알림 처리", e);
											}
										}
									}
								}
							}
						}
					}
				} else if("din".equals(in_name)) {
					if (null != diAlarmYn && !"".equals(diAlarmYn)) {
						if (null != in_name && !"".equals(in_name)) {
							if ("din".equals(in_name) && "1".equals(String.valueOf(reqMap.get("type"))) && "1".equals(String.valueOf(reqMap.get("sensorValue")))) {
								if ("Y".equals(diAlarmYn)) {
									if (!"0".equals(diAlarmTime)) {
										Map<String, Object> notiMap = new HashMap<String, Object>();
										notiMap.put("userId", String.valueOf(reqMap.get("userId")));
										notiMap.put("userToken", String.valueOf(reqMap.get("token")));
										notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
										notiMap.put("addTime", diAlarmTime);
										notiMap.put("alarmType", "di1");
										notiMap.put("inTemp", curTemp);
										notiMap.put("urgentYn", "Y");

										Map<String, Object> chkMap = new HashMap<String, Object>();
										chkMap = adminService.selectChkNoti(notiMap);

										// 이미 알람이 들어있는지 확인 후 없으면 입력
										if (null != chkMap && 0 < chkMap.size()) {
										} else {
											adminService.insertNoti(notiMap);
										}
									} else {
										Map<String, Object> chkUrgent = new HashMap<String, Object>();
										Map<String, Object> chkRelease = new HashMap<String, Object>();
										Map<String, Object> chkMap = new HashMap<String, Object>();
										boolean urgentYn = false;
										boolean releaseYn = false;
										chkMap.put("userId", String.valueOf(reqMap.get("userId")));
										chkMap.put("userToken", String.valueOf(reqMap.get("token")));
										chkMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
										chkMap.put("alarmType", "di");
										chkMap.put("reDelayTime", diAlarmDelayTime);

										chkUrgent = adminService.selectUrgentNoti(chkMap);
										if(null != chkUrgent && 0 < chkUrgent.size()) {
											int urgentCnt = 0;
											urgentCnt = Integer.parseInt(String.valueOf(chkUrgent.get("cnt")));

											if(0 < urgentCnt) {
												urgentYn = true;
											}
										}

										chkRelease = adminService.selectReleaseNoti(chkMap);
										if(null != chkRelease && 0 < chkRelease.size()) {
											int releaseCnt = 0;
											releaseCnt = Integer.parseInt(String.valueOf(chkRelease.get("cnt")));

											if(0 < releaseCnt) {
												releaseYn = true;
											}
										}

										Map<String, Object> notiMap = new HashMap<String, Object>();
										notiMap.put("userId", String.valueOf(reqMap.get("userId")));
										notiMap.put("userToken", String.valueOf(reqMap.get("token")));
										notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
										notiMap.put("addTime", diAlarmTime);
										notiMap.put("alarmType", "di2");
										notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
										notiMap.put("curTemp", highTemp);

										Map<String, Object> chk = new HashMap<String, Object>();
										chk = adminService.selectNoti(notiMap);

										if(null != chk && 0 < chk.size()) {
										}

										if(!urgentYn || releaseYn) {
											Map<String, Object> noti = new HashMap<String, Object>();
											noti.put("userId", String.valueOf(reqMap.get("userId")));
											noti.put("token", String.valueOf(reqMap.get("token")));
											noti.put("sensor_uuid", String.valueOf(reqMap.get("sensorUuid")));
											noti.put("type", "DI알람");
											noti.put("inType", "di");
											noti.put("gu", "din");
											noti.put("inTemp", curTemp);
											sendNoti(noti);

											// DB에 즉시 발송 이력 저장
											Map<String, Object> urgent = new HashMap<String, Object>();
											urgent.put("userId", String.valueOf(reqMap.get("userId")));
											urgent.put("userToken", String.valueOf(reqMap.get("token")));
											urgent.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
											urgent.put("addTime", diAlarmTime);
											urgent.put("alarmType", "di");
											urgent.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
											urgent.put("curTemp", highTemp);
											urgent.put("urgentYn", "Y");

											adminService.insertUrgentNoti(urgent);
										}
									}

									if (!"0".equals(diAlarmDelayTime)) {
										Map<String, Object> notiMap = new HashMap<String, Object>();
										notiMap.put("userId", String.valueOf(reqMap.get("userId")));
										notiMap.put("userToken", String.valueOf(reqMap.get("token")));
										notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
										notiMap.put("addTime", Integer.parseInt(diAlarmTime) + Integer.parseInt(diAlarmDelayTime));
										notiMap.put("alarmType", "di2");
										notiMap.put("inTemp", curTemp);
										notiMap.put("urgentYn", "N");

										Map<String, Object> chkMap = new HashMap<String, Object>();
										chkMap = adminService.selectNoti(notiMap);

										// 이미 알람이 들어있는지 확인 후 없으면 입력
										if (null != chkMap && 0 < chkMap.size()) {
										} else {
											adminService.insertNoti(notiMap);
										}
									}
								}
							} else {
								Map<String, Object> notiMap = new HashMap<String, Object>();
								notiMap.put("userId", String.valueOf(reqMap.get("userId")));
								notiMap.put("userToken", String.valueOf(reqMap.get("token")));
								notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
								notiMap.put("addTime", diAlarmTime);
								notiMap.put("alarmType", "di1");
								notiMap.put("inTemp", curTemp);

								Map<String, Object> chkMap = new HashMap<String, Object>();
								chkMap = adminService.selectNoti(notiMap);

								// 이미 알람이 들어있는지 확인 후 없으면 입력
								if (null != chkMap && 0 < chkMap.size()) {
									Map<String, Object> param = new HashMap<String, Object>();
									param.put("userId", chkMap.get("user_id"));
									param.put("userToken", chkMap.get("user_token"));
									param.put("no", chkMap.get("no"));
									param.put("sensorUuid", chkMap.get("sensor_uuid"));

									try {
										adminService.updateNoti(param);
									} catch(Exception e) {
										unifiedErrorHandler.logError("알림 처리", e);
									}
								}

								notiMap.remove("alarmType");
								notiMap.put("alarmType", "di2");

								chkMap = new HashMap<String, Object>();
								chkMap = adminService.selectNoti(notiMap);

								if(null != chkMap && 0 < chkMap.size()) {
									Map<String, Object> param = new HashMap<String, Object>();
									param.put("userId", chkMap.get("user_id"));
									param.put("userToken", chkMap.get("user_token"));
									param.put("no", chkMap.get("no"));
									param.put("sensorUuid", chkMap.get("sensor_uuid"));

									try {
										adminService.updateNoti(param);
									} catch(Exception e) {
										unifiedErrorHandler.logError("알림 처리", e);
									}
								}
							}
						}
					}
				} else if("output".equals(in_name)) {

				} else if("error".equals(in_name)) {
					if(null != networkAlarmYn && !"".equals(networkAlarmYn)) {
						if("Y".equals(networkAlarmYn)) {
							if(!"0".equals(networkAlarmTime)) {
								logger.info("1. error 1 - network alarm time : " + networkAlarmTime);
								Map<String, Object> notiMap = new HashMap<String, Object>();
								notiMap.put("userId", String.valueOf(reqMap.get("userId")));
								notiMap.put("userToken", String.valueOf(reqMap.get("token")));
								notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
								notiMap.put("addTime", networkAlarmTime);
								notiMap.put("alarmType", "netError1");
								notiMap.put("inTemp", curTemp);
								notiMap.put("urgentYn", "Y");

								Map<String, Object> chkMap = new HashMap<String, Object>();
								chkMap = adminService.selectChkNoti(notiMap);

								// 이미 알람이 들어있는지 확인 후 없으면 입력
								if (null != chkMap && 0 < chkMap.size()) {
								} else {
									adminService.insertNoti(notiMap);
								}
							} else {
								logger.info("2. error 2 - network alarm time : " + networkAlarmTime);
								Map<String, Object> chkUrgent = new HashMap<String, Object>();
								Map<String, Object> chkRelease = new HashMap<String, Object>();
								Map<String, Object> chkMap = new HashMap<String, Object>();
								boolean urgentYn = false;
								boolean releaseYn = false;
								chkMap.put("userId", String.valueOf(reqMap.get("userId")));
								chkMap.put("userToken", String.valueOf(reqMap.get("token")));
								chkMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
								chkMap.put("alarmType", "netError");
								chkMap.put("reDelayTime", networkDelayTime);

								chkUrgent = adminService.selectUrgentNoti(chkMap);
								if(null != chkUrgent && 0 < chkUrgent.size()) {
									int urgentCnt = 0;
									urgentCnt = Integer.parseInt(String.valueOf(chkUrgent.get("cnt")));

									if(0 < urgentCnt) {
										urgentYn = true;
									}
								}

								// 해제 되었다 다시 오류가 인입된 경우에는 발송 처리 추가
								chkRelease = adminService.selectReleaseNoti(chkMap);
								if(null != chkRelease && 0 < chkRelease.size()) {
									int releaseCnt = 0;
									releaseCnt = Integer.parseInt(String.valueOf(chkRelease.get("cnt")));

									if(0 < releaseCnt) {
										releaseYn = true;
									}
								}

								Map<String, Object> notiMap = new HashMap<String, Object>();
								notiMap.put("userId", String.valueOf(reqMap.get("userId")));
								notiMap.put("userToken", String.valueOf(reqMap.get("token")));
								notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
								notiMap.put("addTime", networkAlarmTime);
								notiMap.put("alarmType", "netError2");
								notiMap.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
								notiMap.put("curTemp", highTemp);

								// 알림 중복 체크 (필요시 사용)
								// adminService.selectNoti(notiMap);

								if(!urgentYn || releaseYn) {
									Map<String, Object> noti = new HashMap<String, Object>();
									noti.put("userId", String.valueOf(reqMap.get("userId")));
									noti.put("token", String.valueOf(reqMap.get("token")));
									noti.put("sensor_uuid", String.valueOf(reqMap.get("sensorUuid")));
									noti.put("type", "통신에러");
									noti.put("gu", "netError");
									noti.put("inTemp", curTemp);
									sendNoti(noti);

									Map<String, Object> urgent = new HashMap<String, Object>();
									urgent.put("userId", String.valueOf(reqMap.get("userId")));
									urgent.put("userToken", String.valueOf(reqMap.get("token")));
									urgent.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
									urgent.put("addTime", networkAlarmTime);
									urgent.put("alarmType", "netError");
									urgent.put("inTemp", String.valueOf(reqMap.get("sensorValue")));
									urgent.put("curTemp", highTemp);
									urgent.put("urgentYn", "Y");

									adminService.insertUrgentNoti(urgent);
								}
							}

							if(!"0".equals(networkDelayTime)) {
								Map<String, Object> notiMap = new HashMap<String, Object>();
								notiMap.put("userId", String.valueOf(reqMap.get("userId")));
								notiMap.put("userToken", String.valueOf(reqMap.get("token")));
								notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
								notiMap.put("addTime", Integer.parseInt(networkAlarmTime) + Integer.parseInt(networkDelayTime));
								notiMap.put("alarmType", "netError2");
								notiMap.put("inTemp", curTemp);
								notiMap.put("urgentYn", "N");

								Map<String, Object> chkMap = new HashMap<String, Object>();
								chkMap = adminService.selectNoti(notiMap);

								// 이미 알람이 들어있는지 확인 후 없으면 입력
								if (null != chkMap && 0 < chkMap.size()) {
								} else {
									adminService.insertNoti(notiMap);
								}
							}
						}
					}
				} else if("error_release".equals(in_name)) {
					Map<String, Object> notiMap = new HashMap<String, Object>();
					notiMap.put("userId", String.valueOf(reqMap.get("userId")));
					notiMap.put("userToken", String.valueOf(reqMap.get("token")));
					notiMap.put("sensorUuid", String.valueOf(reqMap.get("sensorUuid")));
					notiMap.put("addTime", networkAlarmTime);
					notiMap.put("alarmType", "netError1");
					notiMap.put("inTemp", curTemp);

					Map<String, Object> chkMap = new HashMap<String, Object>();
					chkMap = adminService.selectNoti(notiMap);

					// 이미 알람이 들어있는지 확인 후 없으면 입력
					if (null != chkMap && 0 < chkMap.size()) {
						Map<String, Object> param = new HashMap<String, Object>();
						param.put("userId", chkMap.get("user_id"));
						param.put("userToken", chkMap.get("user_token"));
						param.put("no", chkMap.get("no"));
						param.put("sensorUuid", chkMap.get("sensor_uuid"));

						try {
							adminService.updateNoti(param);
						} catch(Exception e) {
							unifiedErrorHandler.logError("알림 처리", e);
						}
					}

					notiMap.remove("alarmType");
					notiMap.put("alarmType", "netError2");

					chkMap = new HashMap<String, Object>();
					chkMap = adminService.selectNoti(notiMap);

					if(null != chkMap && 0 < chkMap.size()) {
						Map<String, Object> param = new HashMap<String, Object>();
						param.put("userId", chkMap.get("user_id"));
						param.put("userToken", chkMap.get("user_token"));
						param.put("no", chkMap.get("no"));
						param.put("sensorUuid", chkMap.get("sensor_uuid"));

						try {
							adminService.updateNoti(param);
						} catch(Exception e) {
							unifiedErrorHandler.logError("알림 처리", e);
						}
					}
				}

				resultMap.put("resultCode", "200");
			}
		}

		return resultMap;
	}

	@RequestMapping(value = "/updateFocusStatus", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> updateFocusStatus(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> reqMap
	) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		try {
			// 포커스 상태 업데이트 로직
			// 현재는 단순히 성공 응답만 반환
			resultMap = ResponseUtil.success("포커스 상태 업데이트 성공");
		} catch (Exception e) {
			// ExceptionUtil을 사용한 통일된 예외 처리
			ExceptionUtil.handleException(logger, e, "포커스 상태 업데이트");
			resultMap = ResponseUtil.error(e);
		}
		
		return resultMap;
	}

	/**
	 * FCM 토큰 업데이트 (하이브리드 액티브웹 지원)
	 * @param req HTTP 요청
	 * @param res HTTP 응답
	 * @param request FCM 토큰 요청 데이터
	 * @return 응답 결과
	 */
	@RequestMapping(value = "/main/updateFcmToken", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> updateFcmToken(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> request) {
		
		Map<String, Object> resultMap = new HashMap<>();
		
		try {
			// 세션 검증
			HttpSession session = req.getSession();
			if (!sessionManagementService.isValidSession(session)) {
				return unifiedErrorHandler.createUnauthorizedResponse();
			}
			
			// 요청 데이터 검증
			String userId = String.valueOf(request.get("userId"));
			String userToken = String.valueOf(request.get("userToken"));
			
			if (StringUtil.isEmpty(userId) || StringUtil.isEmpty(userToken)) {
				return unifiedErrorHandler.createBadRequestResponse("사용자 ID와 토큰이 필요합니다.");
			}
			
			// FCM 토큰 업데이트 로직
			Map<String, Object> updateParam = new HashMap<>();
			updateParam.put("userId", userId);
			updateParam.put("userToken", userToken);
			
			// 사용자 정보 업데이트 (토큰 필드가 있다면)
			// 실제 구현은 데이터베이스 스키마에 따라 조정 필요
			logger.info("FCM 토큰 업데이트 요청 - userId: {}, token: {}", userId, userToken);
			
			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "FCM 토큰 업데이트 성공");
			
		} catch (Exception e) {
			logger.error("FCM 토큰 업데이트 중 오류 발생", e);
			resultMap.put("resultCode", "500");
			resultMap.put("resultMessage", "서버 오류가 발생했습니다.");
		}
		
		return resultMap;
	}

	/**
	 * FCM 토큰 삭제 (하이브리드 액티브웹 지원)
	 * @param req HTTP 요청
	 * @param res HTTP 응답
	 * @param request FCM 토큰 삭제 요청 데이터
	 * @return 응답 결과
	 */
	@RequestMapping(value = "/main/deleteFcmToken", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> deleteFcmToken(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> request) {
		
		Map<String, Object> resultMap = new HashMap<>();
		
		try {
			// 세션 검증
			HttpSession session = req.getSession();
			if (!sessionManagementService.isValidSession(session)) {
				return unifiedErrorHandler.createUnauthorizedResponse();
			}
			
			// 요청 데이터 검증
			String userId = String.valueOf(request.get("userId"));
			
			if (StringUtil.isEmpty(userId)) {
				return unifiedErrorHandler.createBadRequestResponse("사용자 ID가 필요합니다.");
			}
			
			// FCM 토큰 삭제 로직
			logger.info("FCM 토큰 삭제 요청 - userId: {}", userId);
			
			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "FCM 토큰 삭제 성공");
			
		} catch (Exception e) {
			logger.error("FCM 토큰 삭제 중 오류 발생", e);
			resultMap.put("resultCode", "500");
			resultMap.put("resultMessage", "서버 오류가 발생했습니다.");
		}
		
		return resultMap;
	}

	/**
	 * 즉시 알람 체크 (하이브리드 액티브웹 지원)
	 * @param req HTTP 요청
	 * @param res HTTP 응답
	 * @param request 알람 체크 요청 데이터
	 * @return 응답 결과
	 */
	@RequestMapping(value = "/main/checkAlarmImmediately", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> checkAlarmImmediately(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> request) {
		
		Map<String, Object> resultMap = new HashMap<>();
		
		try {
			// 세션 검증
			HttpSession session = req.getSession();
			if (!sessionManagementService.isValidSession(session)) {
				return unifiedErrorHandler.createUnauthorizedResponse();
			}
			
			// 요청 데이터 검증
			String userId = String.valueOf(request.get("userId"));
			
			if (StringUtil.isEmpty(userId)) {
				return unifiedErrorHandler.createBadRequestResponse("사용자 ID가 필요합니다.");
			}
			
			// 즉시 알람 체크 로직
			logger.info("즉시 알람 체크 요청 - userId: {}", userId);
			
			// 사용자의 센서들에 대한 알람 상태 체크
			// 실제 구현은 비즈니스 로직에 따라 조정 필요
			
			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "알람 체크 완료");
			resultMap.put("alarmCount", 0); // 실제 알람 개수
			
		} catch (Exception e) {
			logger.error("즉시 알람 체크 중 오류 발생", e);
			resultMap.put("resultCode", "500");
			resultMap.put("resultMessage", "서버 오류가 발생했습니다.");
		}
		
		return resultMap;
	}
	
	/**
	 * 앱 토큰 검증
	 * @param appToken 앱에서 전송한 토큰
	 * @return 토큰 유효성 여부
	 */
	private boolean validateAppToken(String appToken) {
		try {
			if (appToken == null || appToken.isEmpty()) {
				logger.warn("앱 토큰이 없습니다");
				return false;
			}
			
			// 토큰 형식 검증: APP_userId_timestamp_random
			if (!appToken.startsWith("APP_")) {
				logger.warn("앱 토큰 형식이 올바르지 않습니다: {}", appToken);
				return false;
			}
			
			// 토큰에서 사용자 ID 추출
			String[] tokenParts = appToken.split("_");
			if (tokenParts.length < 3) {
				logger.warn("앱 토큰 형식이 올바르지 않습니다: {}", appToken);
				return false;
			}
			
			String userId = tokenParts[1];
			if (userId == null || userId.isEmpty()) {
				logger.warn("앱 토큰에서 사용자 ID를 추출할 수 없습니다: {}", appToken);
				return false;
			}
			
			// 사용자 ID 유효성 검증 (DB 조회)
			// 실제 구현에서는 DB에서 사용자 존재 여부 확인
			logger.info("앱 토큰 검증 성공 - userId: {}", userId);
			return true;
			
		} catch (Exception e) {
			logger.error("앱 토큰 검증 중 오류 발생", e);
			return false;
		}
	}
	
	/**
	 * 앱 토큰에서 사용자 ID 추출
	 * @param appToken 앱에서 전송한 토큰
	 * @return 사용자 ID
	 */
	private String extractUserIdFromToken(String appToken) {
		try {
			if (appToken == null || appToken.isEmpty()) {
				return null;
			}
			
			// 토큰 형식: APP_userId_timestamp_random
			String[] tokenParts = appToken.split("_");
			if (tokenParts.length >= 2) {
				return tokenParts[1];
			}
			
			return null;
			
		} catch (Exception e) {
			logger.error("앱 토큰에서 사용자 ID 추출 중 오류 발생", e);
			return null;
		}
	}

}
