package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.common.Constants;

import com.andrew.hnt.api.model.LoginVO;
import com.andrew.hnt.api.service.LoginService;
import com.andrew.hnt.api.util.StringUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.andrew.hnt.api.model.UserInfo;
import com.andrew.hnt.api.mqtt.common.MQTT;
import com.andrew.hnt.api.service.AdminService;
import com.andrew.hnt.api.service.SessionManagementService;
import com.andrew.hnt.api.service.UnifiedSessionService;
import com.andrew.hnt.api.service.UnifiedSessionService.SessionValidationResult;
import com.andrew.hnt.api.util.ResponseUtil;
import com.andrew.hnt.api.util.ExceptionUtil;
import com.andrew.hnt.api.util.UnifiedErrorHandler;
import com.andrew.hnt.api.util.SecurityUtil;
import com.andrew.hnt.api.service.UnifiedPermissionValidationService;
import com.andrew.hnt.api.util.PermissionValidationStrategy;
import com.andrew.hnt.api.service.TransactionManagementService;
import com.andrew.hnt.api.util.PermissionUtil;
import com.andrew.hnt.api.util.RedirectUtil;
import com.andrew.hnt.api.service.SubAccountPermissionService;
import com.andrew.hnt.api.util.JsonValidationUtil;
import com.andrew.hnt.api.util.ArrayMessageProcessor;
import com.andrew.hnt.api.util.TopicParserUtil;
import com.andrew.hnt.api.util.ParsingErrorHandler;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * AdminController
 * 관리자 화면 처리, 각종 설정 화면 처리 컨트롤러
 */
@Controller
@RequestMapping("/admin")
public class AdminController extends DefaultController {
	
	@Autowired
	private AdminService adminService;


	@Autowired
	private LoginService loginService;
	
	@Autowired
	private CommonController commonController;
	
	@Autowired
	private UnifiedSessionService unifiedSessionService;
	
	@Autowired
	private com.andrew.hnt.api.service.SessionManagementService sessionManagementService;
	
	@Autowired
	private TransactionManagementService transactionManagementService;
	
	@Autowired
	private SubAccountPermissionService subAccountPermissionService;
	
	@Autowired
	private UnifiedErrorHandler unifiedErrorHandler;
	
	@Autowired
	private SecurityUtil securityUtil;
	
	@Autowired
	private UnifiedPermissionValidationService unifiedPermissionValidationService;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * 장치 설정 화면
	 * @param req
	 * @param res
	 * @param userId
	 * @param sensorUuid
	 * @param model
	 * @return
	 */
    @RequestMapping(value = "/sensorSetting", method = RequestMethod.GET)
    public String sensorSettiong(
            HttpServletRequest req
            , HttpServletResponse res
            , Model model
    		) {

        String result = "admin/sensorSetting";
        
        HttpSession session = req.getSession();
        
        // 통합 세션 검증 (기본 + 보안 + 권한 + 모델 설정)
        SessionValidationResult validationResult = unifiedSessionService.validateSession(session, req, model, "B"); // 부계정 이상 권한 필요
        
        if (!validationResult.isValid()) {
            logger.warn("센서 설정 페이지 접근 - 통합 세션 검증 실패, 리다이렉트: {}, 오류: {}", 
                validationResult.getRedirectUrl(), validationResult.getErrorMessage());
            return validationResult.getRedirectUrl();
        }
        
        // 검증 성공 시 사용자 정보 사용
        String sessionUserId = validationResult.getUserId();
        String sessionUserGrade = validationResult.getUserGrade();
        String sessionUserNm = validationResult.getUserNm();
        
        logger.info("센서 설정 페이지 - 통합 세션 검증 성공: userId={}, userGrade={}, userNm={}", 
                   sessionUserId, sessionUserGrade, sessionUserNm);
        
        // URL 파라미터에서 sensorUuid 가져오기 (세션 기반으로 변경 예정)
        String sensorUuid = req.getParameter("sensorUuid");
        logger.info("센서 설정 페이지 접근 - 요청 URL: {}, UUID 파라미터: {}", req.getRequestURL(), sensorUuid);
        
        if (StringUtil.isEmpty(sensorUuid)) {
            logger.warn("센서 설정 페이지 접근 - sensorUuid 파라미터 없음, 메인으로 리다이렉트");
            return "redirect:/main/main";
        }
        
        logger.info("센서 설정 페이지 접근 - 세션 userId: {}, sensorUuid: {}, userGrade: {}", 
                   sessionUserId, sensorUuid, sessionUserGrade);

        // 권한 검사 (A, U, B 등급 모두 접근 가능)
        if (!sessionManagementService.hasPermission(session, "B")) {
            logger.warn("센서 설정 페이지 접근 - 권한 없음, 메인으로 리다이렉트");
            return "redirect:/main/main";
        }
        
        // 세션 정보를 모델에 설정
        sessionManagementService.setSessionInfoToModel(session, model);
        
        // 모델에 설정된 값 디버깅 로그 추가
        logger.info("모델에 설정된 세션 정보 확인:");
        logger.info("  model.userId = {}", model.asMap().get("userId"));
        logger.info("  model.userGrade = {}", model.asMap().get("userGrade"));
        logger.info("  model.userNm = {}", model.asMap().get("userNm"));
        logger.info("  model.sensorId = {}", model.asMap().get("sensorId"));
        logger.info("  model.loginUserId = {}", model.asMap().get("loginUserId"));
        
        // 세션에서 사용자 정보를 모델에 직접 설정
        model.addAttribute("userId", sessionUserId);
        model.addAttribute("loginUserId", sessionUserId);
        
        // 세션에서 parentUserId 가져오기 (부계정인 경우 메인 사용자 ID)
        String parentUserId = (String) session.getAttribute("parentUserId");
        if(!StringUtil.isEmpty(parentUserId)) {
            model.addAttribute("parentUserId", parentUserId);
        } else {
            model.addAttribute("parentUserId", sessionUserId); // 주계정인 경우 자기 자신
        }
        
        // sensorId는 DB에서 조회한 후에 설정 (중복 설정 방지)
        // model.addAttribute("sensorId", StringUtil.isEmpty(sessionUserId) ? "" : sessionUserId); // 주석 처리
        model.addAttribute("sensorUuid", sensorUuid); // 센서 UUID 추가
        
        if(!StringUtil.isEmpty(sessionUserNm)) {
            model.addAttribute("userNm", sessionUserNm);
        }
        
        if(!StringUtil.isEmpty(sessionUserGrade)) {
            model.addAttribute("userGrade", sessionUserGrade);
        }
        
        // topicStr은 센서 정보 조회 후에 설정됨
        String topicStr = "";
        
        // 센서 정보 조회 (센서의 실제 소유자 ID 확인용)
        String actualSensorOwnerId = sessionUserId; // 기본값
        try {
            Map<String, Object> sensorInfo = adminService.getSensorInfoByUuid(sensorUuid);
            if (sensorInfo != null && !sensorInfo.isEmpty()) {
                model.addAttribute("sensorInfo", sensorInfo);
                model.addAttribute("sensor_name", sensorInfo.get("sensor_name"));
                model.addAttribute("sensorUuid", sensorUuid);
                
                // 센서의 실제 소유자 ID 추출 (부계정 지원)
                if (sensorInfo.get("sensor_id") != null) {
                    actualSensorOwnerId = String.valueOf(sensorInfo.get("sensor_id"));
                    logger.info("센서의 실제 소유자 ID: {}", actualSensorOwnerId);
                }
                
                // 센서 정보 초기값 설정
                String sensorType = String.valueOf(sensorInfo.get("sensor_type") != null ? sensorInfo.get("sensor_type") : "0");
                String deviceType = String.valueOf(sensorInfo.get("p16") != null ? sensorInfo.get("p16") : "0");
                
                // 센서 타입에 따른 정보 설정
                String sensorDisplayName = "";
                String tempRange = "";
                if("2".equals(sensorType)) { // T3
                    sensorDisplayName = "PT100(T3)";
                    tempRange = "-200°C ~ 850°C";
                } else if("0".equals(sensorType)) { // T1
                    sensorDisplayName = "NTC 10K(T1)";
                    tempRange = "-50°C ~ 125°C";
                } else if("1".equals(sensorType)) { // T2
                    sensorDisplayName = "NTC 5K(T2)";
                    tempRange = "-50°C ~ 125°C";
                } else {
                    sensorDisplayName = "NTC 10K(T1)"; // Default
                    tempRange = "-50°C ~ 125°C"; // Default
                }
                
                // 장치종류 설정
                String deviceTypeText = "";
                if("0".equals(deviceType)) {
                    deviceTypeText = "Cooler (쿨러)";
                } else if("1".equals(deviceType)) {
                    deviceTypeText = "Heater (히터)";
                } else {
                    deviceTypeText = "Cooler (쿨러)";
                }
                
                model.addAttribute("sensorDisplayName", sensorDisplayName);
                model.addAttribute("tempRange", tempRange);
                model.addAttribute("deviceTypeText", deviceTypeText);
                logger.info("센서 정보 조회 성공 - sensorName: {}, sensorUuid: {}", 
                           sensorInfo.get("sensor_name"), sensorUuid);
            } else {
                logger.warn("센서 정보 조회 실패 - sensorUuid: {}", sensorUuid);
                model.addAttribute("sensor_name", "알 수 없는 장치");
                model.addAttribute("sensorUuid", sensorUuid);
                
                // 센서 정보가 없을 때 기본값 설정
                model.addAttribute("sensorDisplayName", "-");
                model.addAttribute("tempRange", "-");
                model.addAttribute("deviceTypeText", "-");
            }
        } catch (Exception e) {
            unifiedErrorHandler.logError("센서 정보 조회", e);
            model.addAttribute("sensor_name", "알 수 없는 장치");
            model.addAttribute("sensorUuid", sensorUuid);
            
            // 예외 발생 시에도 기본값 설정
            model.addAttribute("sensorDisplayName", "-");
            model.addAttribute("tempRange", "-");
			model.addAttribute("deviceTypeText", "-");
		}
		
		// 세션 우선: DB에서 직접 사이드바 데이터 조회 (세션 userId 사용)
		// commonController.addSidebarData(sessionUserId, model, session); // 주석 처리 - 339번째 줄에서 호출
		
		UserInfo user = new UserInfo();

		try {
			// 사용자 아이디가 있을 경우 사용자 아이디로 사용자 정보와 연결된 센서 정보를 가져온다.
			// 부계정인 경우 센서의 실제 소유자 ID(actualSensorOwnerId)를 사용하여 조회
			// userId와 sensorUuid는 이미 위에서 설정했으므로 중복 설정 제거

			Map<String, Object> userInfo = new HashMap<String, Object>();
			logger.info("getUserInfo 호출 - actualSensorOwnerId: {}, sensorUuid: {}", actualSensorOwnerId, sensorUuid);
			userInfo = adminService.getUserInfo(actualSensorOwnerId, sensorUuid);

			if(null != userInfo && !"".equals(userInfo)) {
				user = (UserInfo) userInfo.get("userInfo");
				Map<String, Object> sensor = (Map<String, Object>) userInfo.get("sensorInfo");

				if(null != sensor && 0 < sensor.size()) {
					// 출력 제어 및 설정 전송 토픽은 'TC' 고정 세그먼트를 사용
					String sensorId = String.valueOf(sensor.get("sensor_id"));
					String sensorUuidFromSensor = String.valueOf(sensor.get("sensor_uuid"));
					
					// null 체크 추가
					if (!"null".equals(sensorId) && !"null".equals(sensorUuidFromSensor)) {
						topicStr = "HBEE/" + sensorId + "/TC/" + sensorUuidFromSensor + "/SER";
					} else {
						// fallback: sessionUserId와 sensorUuid 사용
						topicStr = "HBEE/" + sessionUserId + "/TC/" + sensorUuid + "/SER";
					}
				} else {
					// 센서 정보가 없는 경우 fallback
					topicStr = "HBEE/" + sessionUserId + "/TC/" + sensorUuid + "/SER";
				}
				
				// 토픽 구조 검증
				TopicParserUtil.TopicParseResult topicValidation = TopicParserUtil.parseTopic(topicStr);
				if (!topicValidation.isValid()) {
					logger.warn("생성된 토픽이 유효하지 않음: {} - {}", topicStr, topicValidation.getReason());
					
					// 파싱 오류 처리 및 로깅
					ParsingErrorHandler.handleTopicParseError(
						topicValidation.getReason(), 
						topicStr, 
						"AdminController.sensorSetting"
					);
					
					// 기본 토픽으로 대체
					topicStr = "HBEE/" + sessionUserId + "/TC/" + sensorUuid + "/SER";
				} else {
					logger.debug("토픽 생성 성공: {} (타입: {})", topicStr, topicValidation.getTopicType());
				}
				
				// topicStr을 모델에 추가 (한 번만)
				model.addAttribute("topicStr", topicStr);

				String sensorName = "";
				// DB에서 센서 이름 가져오기
				if(sensor != null && sensor.containsKey("sensor_name")) {
					sensorName = String.valueOf(sensor.get("sensor_name"));
					logger.debug("DB에서 센서 이름 조회 성공: {}", sensorName);
				} else {
					// 기본값: "장치" + UUID 앞 8자리
					sensorName = "장치 " + sensorUuid.substring(0, Math.min(8, sensorUuid.length()));
					logger.warn("DB에서 센서 이름 조회 실패, 기본값 사용: {}", sensorName);
				}

				model.addAttribute("user", user);
				model.addAttribute("sensorUuid", sensorUuid);
				model.addAttribute("sensorName", sensorName);
				model.addAttribute("token", session.getAttribute("token"));
				
				// sensorId 설정 (센서 정보가 있는 경우만, DB에서 조회한 실제 sensor_id 사용)
				// CommonController보다 먼저 설정하여 덮어쓰기 방지
				logger.info("=== sensorId 설정 시작 ===");
				logger.info("sensor null 여부: {}", (sensor == null));
				if(sensor != null) {
					logger.info("sensor.size(): {}", sensor.size());
					logger.info("sensor.get(\"sensor_id\"): {}", sensor.get("sensor_id"));
				}
				
				if(null != sensor && 0 < sensor.size() && sensor.get("sensor_id") != null) {
					String dbSensorId = String.valueOf(sensor.get("sensor_id"));
					logger.info("dbSensorId: {}", dbSensorId);
					if (!"null".equals(dbSensorId)) {
						model.addAttribute("sensorId", dbSensorId);
						logger.info("[OK] 센서 소유자 ID 설정 완료: {}", dbSensorId);
					} else {
						logger.warn("[ERROR] dbSensorId가 'null' 문자열임");
					}
				} else {
					logger.warn("[ERROR] sensor 조건 실패 - sensorId 설정 불가");
				}
				
				// CommonController를 통한 세션 정보 추가 (sensorId가 이미 설정되어 있으면 덮어쓰지 않음)
				commonController.addSidebarData(sessionUserId, model, session);
				
				// 센서 정보 추가
				if(null != sensor && 0 < sensor.size()) {
					model.addAttribute("sensorInfo", sensor);
					
					// 센서 타입에 따른 온도 범위 설정
					String sensorType = String.valueOf(sensor.get("sensor_type"));
					String tempRange = "";
					String sensorDisplayName = "";
					
					if("PT100".equals(sensorType)) {
						tempRange = "-200°C ~ 850°C";
						sensorDisplayName = "PT100(T3)";
					} else if("NTC".equals(sensorType)) {
						tempRange = "-50°C ~ 125°C";
						sensorDisplayName = "NTC(T1)";
					} else if("T3".equals(sensorType)) {
						tempRange = "-200°C ~ 850°C";  // T3는 PT100과 동일한 범위
						sensorDisplayName = "PT100(T3)";
					} else if("T1".equals(sensorType)) {
						tempRange = "-50°C ~ 125°C";  // T1은 NTC와 동일한 범위
						sensorDisplayName = "NTC(T1)";
					} else if("T2".equals(sensorType)) {
						tempRange = "-50°C ~ 125°C";  // T2는 NTC와 동일한 범위
						sensorDisplayName = "NTC(T2)";
					} else {
						tempRange = "-50°C ~ 125°C"; // 기본값
						sensorDisplayName = "NTC(T1)";
					}
					model.addAttribute("tempRange", tempRange);
					model.addAttribute("sensorType", sensorType);
					model.addAttribute("sensorDisplayName", sensorDisplayName);
				}

			// DB에 저장된 알람 설정 정보를 가져온다.
			Map<String, Object> alarmMap = new HashMap<String, Object>();
			Map<String, Object> param = new HashMap<String, Object>();
			// 부계정이 메인 계정의 장치 알람설정을 조회할 때는 sensorId를 실제 장치 소유자 ID로 설정
			// MyBatis HashMap은 언더스코어 그대로 반환하므로 sensor_id로 조회
			String actualSensorId = (sensor != null && sensor.get("sensor_id") != null) 
				? String.valueOf(sensor.get("sensor_id")) 
				: sessionUserId; // sensor가 null이면 sessionUserId 사용
			param.put("userId", sessionUserId);
			param.put("sensorId", actualSensorId);  // 실제 장치 소유자 ID 사용
			param.put("sensorUuid", sensorUuid);
				logger.info("알람 설정 조회 파라미터 - userId: {}", sessionUserId);
				logger.info("알람 설정 조회 파라미터 - sensor_id from DB: {}", actualSensorId);
				logger.info("알람 설정 조회 파라미터 - sensorUuid: {}", sensorUuid);
				logger.info("loginUserId : " + session.getAttribute("loginUserId"));

				try {
					alarmMap = adminService.getAlarmSetting(param);
					
					logger.info("알람 설정 조회 결과 - userId: {}, sensorId: {}, sensorUuid: {}", 
						sessionUserId, actualSensorId, sensorUuid);
					logger.info("alarmMap 크기: {}", alarmMap != null ? alarmMap.size() : 0);
					logger.info("alarmMap 내용: {}", alarmMap);

					if(null != alarmMap && 0 < alarmMap.size()) {
						JSONObject alarm = new JSONObject(alarmMap);
						model.addAttribute("alarmMap", alarm);
						logger.info("alarmMap을 모델에 추가 완료: {}", alarm.toString());
					} else {
						logger.warn("알람 설정 데이터가 없음 - 기본값으로 초기화");
						
						// 기본 알람 설정 생성
						Map<String, Object> defaultAlarmMap = new HashMap<String, Object>();
						defaultAlarmMap.put("userId", sessionUserId);
						defaultAlarmMap.put("sensorId", actualSensorId);
						defaultAlarmMap.put("sensorUuid", sensorUuid);
						defaultAlarmMap.put("alarm_yn1", "N");
						defaultAlarmMap.put("alarm_yn2", "N");
						defaultAlarmMap.put("alarm_yn3", "N");
						defaultAlarmMap.put("alarm_yn4", "N");
						defaultAlarmMap.put("alarm_yn5", "N");
						defaultAlarmMap.put("set_val1", "");
						defaultAlarmMap.put("set_val2", "");
						defaultAlarmMap.put("set_val3", "");
						defaultAlarmMap.put("set_val4", "");
						defaultAlarmMap.put("delay_hour1", "0");
						defaultAlarmMap.put("delay_min1", "0");
						defaultAlarmMap.put("delay_hour2", "0");
						defaultAlarmMap.put("delay_min2", "0");
						defaultAlarmMap.put("delay_hour4", "0");
						defaultAlarmMap.put("delay_min4", "0");
						defaultAlarmMap.put("delay_hour5", "0");
						defaultAlarmMap.put("delay_min5", "0");
						defaultAlarmMap.put("re_delay_hour1", "0");
						defaultAlarmMap.put("re_delay_min1", "0");
						defaultAlarmMap.put("re_delay_hour2", "0");
						defaultAlarmMap.put("re_delay_min2", "0");
						defaultAlarmMap.put("re_delay_hour4", "0");
						defaultAlarmMap.put("re_delay_min4", "0");
						defaultAlarmMap.put("re_delay_hour5", "0");
						defaultAlarmMap.put("re_delay_min5", "0");
						
						JSONObject alarm = new JSONObject(defaultAlarmMap);
						model.addAttribute("alarmMap", alarm);
						logger.info("기본 알람 설정을 모델에 추가 완료: {}", alarm.toString());
					}
					

				} catch(Exception e) {
					unifiedErrorHandler.logError("알람 설정 조회", e);
					// 에러 발생 시에도 빈 JSONObject 추가
					model.addAttribute("alarmMap", new JSONObject());
				}

			}
		} catch(Exception e) {
			unifiedErrorHandler.logError("센서 설정", e);
			// 에러 발생 시에도 기본 topicStr 설정
			model.addAttribute("topicStr", "HBEE/" + sessionUserId + "/TC/" + sensorUuid + "/SER");
		}

        return result;
    }

	/**
	 * 장치로 명령어 전송 처리
	 * @param req
	 * @param res
	 * @param sensorMap
	 * @return
	 */
	@RequestMapping(value = "/setSensor", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> setSensor(
    		HttpServletRequest req
    		, HttpServletResponse res
    		, @RequestBody Map<String, Object> sensorMap
    		) {
    	try {
    		// 입력값 검증
    		if(sensorMap == null || sensorMap.isEmpty()) {
    			return unifiedErrorHandler.createBadRequestResponse("센서 설정 데이터가 없습니다.");
    		}
    		
    		HttpSession session = req.getSession();
    		
		// 세션 검증
		if(!sessionManagementService.isValidSession(session)) {
			return unifiedErrorHandler.createUnauthorizedResponse();
		}
		
		// 부계정 권한 확인 - 설정 변경은 불가, 조회는 가능
		String sessionUserId = (String) session.getAttribute("userId");
		String parentUserId = (String) session.getAttribute("parentUserId");
		boolean isSubAccount = (parentUserId != null && !parentUserId.isEmpty() && !parentUserId.equals(sessionUserId));
		
		// setGu 값 확인 (조회 vs 설정 변경 구분)
		String setGu = sensorMap != null ? String.valueOf(sensorMap.get("setGu")) : "";
		
		// 부계정은 설정 변경 불가 (param, defrost, stopDefrost, initdevice, output 등)
		if(isSubAccount) {
			boolean isReadOnlyRequest = "readparam".equals(setGu) || "readstatus".equals(setGu);
			
			if(!isReadOnlyRequest) {
				Map<String, Object> errorMap = new HashMap<>();
				errorMap.put("resultCode", "403");
				errorMap.put("resultMessage", "부계정 사용자는 장치 설정을 변경할 수 없습니다. (읽기 전용)");
				logger.warn("부계정 사용자 장치 설정 변경 시도 차단 - userId: {}, parentUserId: {}, setGu: {}", 
					sessionUserId, parentUserId, setGu);
				return errorMap;
			}
		}
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		if(null != sensorMap && 0 < sensorMap.size()) {
		String sendTopic = "";
		String payload = "";
		String userId = "";
   		String p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12, p13, p14, p15, p16;
    		
    		sendTopic = String.valueOf(sensorMap.get("topicStr"));
    		setGu = String.valueOf(sensorMap.get("setGu"));
    		userId = String.valueOf(sensorMap.get("userId"));
    		p01 = String.valueOf(sensorMap.get("p01"));
    		p02 = String.valueOf(sensorMap.get("p02"));
    		p03 = String.valueOf(sensorMap.get("p03"));
    		p04 = String.valueOf(sensorMap.get("p04"));
    		p05 = String.valueOf(sensorMap.get("p05"));
    		p06 = String.valueOf(sensorMap.get("p06"));
    		p07 = String.valueOf(sensorMap.get("p07"));
    		p08 = String.valueOf(sensorMap.get("p08"));
    		p09 = String.valueOf(sensorMap.get("p09"));
    		p10 = String.valueOf(sensorMap.get("p10"));
			p11 = String.valueOf(sensorMap.get("p11"));
			p12 = String.valueOf(sensorMap.get("p12"));
			p13 = String.valueOf(sensorMap.get("p13"));
			p14 = String.valueOf(sensorMap.get("p14"));
			p15 = String.valueOf(sensorMap.get("p15"));
			p16 = String.valueOf(sensorMap.get("p16"));
    		
    		if(null != setGu && !"".equals(setGu)) {
    			if("param".equals(setGu)) {
    				payload = Constants.Mqtt.MESSAGE_TYPE_SET + "&p01="+p01+"&p02="+p02+"&p03="+p03+"&p04="+p04+"&p05="+p05+"&p06="+p06+"&p07="+p07+"&p08="+p08
							+"&p09="+p09+"&p10="+p10+"&p11="+p11+"&p12="+p12+"&p13="+p13+"&p14="+p14+"&p15="+p15+"&p16="+p16;
    				
				// 센서 설정값을 DB에 저장 (기존 hnt_config 테이블 구조 사용)
				// TODO: AdminMapper 의존성 문제로 임시 주석 처리
				/*
				try {
					String sensorUuid = String.valueOf(sensorMap.get("sensorUuid"));
					
					// 기존 설정 삭제
					adminMapper.deleteConfig(userId, sensorUuid);
					
					// 기존 hnt_config 테이블 구조에 맞게 저장
					Map<String, Object> configParam = new HashMap<>();
					configParam.put("userId", userId);
					configParam.put("sensorId", userId);
					configParam.put("sensorUuid", sensorUuid);
					configParam.put("setVal1", p01);  // p01 = 설정온도
					configParam.put("setVal2", p02);  // p02 = 히스테리시스
					configParam.put("setVal3", p03);  // p03 = 지연시간
					configParam.put("instId", userId);
					configParam.put("mdfId", userId);
					
					adminMapper.insertConfig(configParam);
					
					logger.info("센서 설정값 DB 저장 완료: userId={}, sensorUuid={}, setVal1={}", userId, sensorUuid, p01);
				} catch (Exception e) {
					logger.error("센서 설정값 DB 저장 실패: userId={}, sensorUuid={}, error={}", userId, sensorMap.get("sensorUuid"), e.getMessage());
				}
				*/
    			} else if("defrost".equals(setGu)) {
					payload = Constants.Mqtt.MESSAGE_TYPE_ACT + "&name=" + Constants.Mqtt.ACTION_FORCEDEF + "&value=1";    // 강제제상 시작
				} else if("stopDefrost".equals(setGu)) {
    				payload = Constants.Mqtt.MESSAGE_TYPE_ACT + "&name=" + Constants.Mqtt.ACTION_FORCEDEF + "&value=0";    // 강제제상 종료
    			} else if("readparam".equals(setGu)) {
    				payload = Constants.Mqtt.MESSAGE_TYPE_GET + "&type=1";
            } else if("initdevice".equals(setGu)) {
                // 장치 삭제 프로토콜: name은 고정 키 'userId'
                payload = Constants.Mqtt.MESSAGE_TYPE_ACT + "&name=" + Constants.Mqtt.ACTION_USER_ID + "&value=0";    // 기기 초기화(삭제)
            } else if("readstatus".equals(setGu)) {
    				payload = Constants.Mqtt.MESSAGE_TYPE_GET + "&type=2";
            } else if ("output".equals(setGu)) {
                // 출력 단자 수동 제어: ACT&name=output&type={1~99}&ch={1~99}&value=0|1
                String outType = String.valueOf(sensorMap.get("outType"));
                String outCh = String.valueOf(sensorMap.get("outCh"));
                String outValue = String.valueOf(sensorMap.get("outValue"));
                if (outType == null || outType.isEmpty()) { outType = "1"; }
                if (outCh == null || outCh.isEmpty()) { outCh = "1"; }
                if (!"1".equals(outValue)) { outValue = "0"; }
                payload = Constants.Mqtt.MESSAGE_TYPE_ACT + "&name=" + Constants.Mqtt.ACTION_OUTPUT + "&type=" + outType + "&ch=" + outCh + "&value=" + outValue;
				}
    		}
    		
    		String MqttServer1 = Constants.Mqtt.SERVER;
    		String MqttServer2 = "";
    		String client_id = "";
    		String userName = Constants.Mqtt.USERNAME;
    		String password = Constants.Mqtt.PASSWORD;
        String topic = Constants.Mqtt.TOPIC_WILDCARD;
    		String msg = "";
    		String readMsg = "";
    		
    		client_id = UUID.randomUUID().toString();
    		
        MQTT client = new MQTT(MqttServer1, client_id, userName, password);
        if(null != sendTopic && !"".equals(sendTopic)) {
            // 응답 구독 토픽은 DEV로 전환
            if (sendTopic.endsWith("/SER")) {
                topic = sendTopic.substring(0, sendTopic.length() - 3) + "DEV";
            } else {
                topic = sendTopic;
            }
        }

        client.init(topic, "Y"); // 응답 수신을 위해 구독
        client.publish(payload, 0, sendTopic); // 요청은 SER로 발행
    		String resultMsg = client.getMsg();
    		String rcvTopic = client.getRcvTopic();
    		
    		// JSON 유효성 검사 및 배열 형태 메시지 처리 적용
    		if (resultMsg != null && !resultMsg.trim().isEmpty()) {
    			// 배열 형태 메시지인지 확인
    			if (ArrayMessageProcessor.isArrayMessage(resultMsg)) {
    				logger.info("배열 형태 MQTT 응답 감지, 배열 처리 시작");
    				
    				ArrayMessageProcessor.ArrayProcessingResult arrayResult = 
    					ArrayMessageProcessor.processArrayMessage(resultMsg);
    				
				if (!arrayResult.isSuccess()) {
					logger.warn("배열 형태 MQTT 응답 처리 실패: {}", arrayResult.getReason());
					
					// 파싱 오류 처리 및 로깅
					ParsingErrorHandler.handleArrayParseError(
						arrayResult.getReason(), 
						resultMsg, 
						"AdminController.setSensor"
					);
					
					resultMap.put("resultCode", "400");
					resultMap.put("resultMessage", "배열 형태 MQTT 응답 처리 실패: " + arrayResult.getReason());
					resultMap.put("resultMsg", resultMsg);
					resultMap.put("rcvTopic", rcvTopic);
					return resultMap;
				}
    				
    				logger.info("배열 형태 MQTT 응답 처리 성공: {}", arrayResult.getReason());
    				resultMap.put("arrayProcessing", arrayResult.getAdditionalInfo());
    				resultMap.put("originalArraySize", arrayResult.getOriginalArraySize());
    				resultMap.put("processedElementCount", arrayResult.getProcessedElementCount());
    				
			} else {
				// 일반 JSON 메시지 처리
				JsonValidationUtil.JsonValidationResult jsonValidation = 
					JsonValidationUtil.validateAndParseJson(resultMsg);
				
				if (!jsonValidation.isValid()) {
					logger.warn("MQTT 응답 JSON 유효성 검사 실패: {}", jsonValidation.getReason());
					
					// 파싱 오류 처리 및 로깅
					ParsingErrorHandler.handleValidationError(
						jsonValidation.getReason(), 
						resultMsg, 
						"AdminController.setSensor"
					);
					
					resultMap.put("resultCode", "400");
					resultMap.put("resultMessage", "MQTT 응답 JSON 유효성 검사 실패: " + jsonValidation.getReason());
					resultMap.put("resultMsg", resultMsg);
					resultMap.put("rcvTopic", rcvTopic);
					return resultMap;
				}
				
				logger.debug("MQTT 응답 JSON 유효성 검사 통과: {}", jsonValidation.getReason());
				resultMap.put("jsonValidation", jsonValidation.getAdditionalInfo());
			}
		}

    		resultMap.put("resultCode", "200");
    		resultMap.put("resultMsg", resultMsg);
    		resultMap.put("rcvTopic", rcvTopic);
    	}
    	
    	return resultMap;
    	} catch (Exception e) {
    		unifiedErrorHandler.logError("센서 설정", e);
    		return unifiedErrorHandler.createInternalServerErrorResponse(e);
    	}
    }

	/**
	 * 장치 설정 저장
	 * @param req
	 * @param res
	 * @param settingMap
	 * @return
	 */
	@RequestMapping(value = "/saveSensorSetting", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> saveSensorSetting(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> settingMap
		) {
    	Map<String, Object> resultMap = new HashMap<String, Object>();

    	if(null != settingMap && 0 < settingMap.size()) {
    		try {
    			// 기존 알람설정 조회
    			Map<String, Object> existingSetting = adminService.selectSetting(settingMap);
    			
    			// 변경된 항목 확인
    			StringBuilder changedItems = new StringBuilder();
    			boolean hasChanges = false;
    			
    			if(existingSetting != null && existingSetting.size() > 0) {
    				// 알람 사용 여부 비교
    				if(!String.valueOf(existingSetting.get("alarm_yn1")).equals(String.valueOf(settingMap.get("alarmYn1")))) {
    					String oldValue = "Y".equals(String.valueOf(existingSetting.get("alarm_yn1"))) ? "사용" : "미사용";
    					String newValue = "Y".equals(String.valueOf(settingMap.get("alarmYn1"))) ? "사용" : "미사용";
    					changedItems.append("• 고온알람 사용여부: ").append(oldValue).append(" → ").append(newValue).append("\n");
    					hasChanges = true;
    				}
    				if(!String.valueOf(existingSetting.get("alarm_yn2")).equals(String.valueOf(settingMap.get("alarmYn2")))) {
    					String oldValue = "Y".equals(String.valueOf(existingSetting.get("alarm_yn2"))) ? "사용" : "미사용";
    					String newValue = "Y".equals(String.valueOf(settingMap.get("alarmYn2"))) ? "사용" : "미사용";
    					changedItems.append("• 저온알람 사용여부: ").append(oldValue).append(" → ").append(newValue).append("\n");
    					hasChanges = true;
    				}
    				if(!String.valueOf(existingSetting.get("alarm_yn3")).equals(String.valueOf(settingMap.get("alarmYn3")))) {
    					String oldValue = "Y".equals(String.valueOf(existingSetting.get("alarm_yn3"))) ? "사용" : "미사용";
    					String newValue = "Y".equals(String.valueOf(settingMap.get("alarmYn3"))) ? "사용" : "미사용";
    					changedItems.append("• 특정온도알람 사용여부: ").append(oldValue).append(" → ").append(newValue).append("\n");
    					hasChanges = true;
    				}
    				if(!String.valueOf(existingSetting.get("alarm_yn4")).equals(String.valueOf(settingMap.get("alarmYn4")))) {
    					String oldValue = "Y".equals(String.valueOf(existingSetting.get("alarm_yn4"))) ? "사용" : "미사용";
    					String newValue = "Y".equals(String.valueOf(settingMap.get("alarmYn4"))) ? "사용" : "미사용";
    					changedItems.append("• DI알람 사용여부: ").append(oldValue).append(" → ").append(newValue).append("\n");
    					hasChanges = true;
    				}
    				if(!String.valueOf(existingSetting.get("alarm_yn5")).equals(String.valueOf(settingMap.get("alarmYn5")))) {
    					String oldValue = "Y".equals(String.valueOf(existingSetting.get("alarm_yn5"))) ? "사용" : "미사용";
    					String newValue = "Y".equals(String.valueOf(settingMap.get("alarmYn5"))) ? "사용" : "미사용";
    					changedItems.append("• 통신이상알람 사용여부: ").append(oldValue).append(" → ").append(newValue).append("\n");
    					hasChanges = true;
    				}
    				
    				// 설정값 비교
    				if(!String.valueOf(existingSetting.get("set_val1")).equals(String.valueOf(settingMap.get("setVal1")))) {
    					changedItems.append("• 고온 설정값: ").append(existingSetting.get("set_val1")).append(" → ").append(settingMap.get("setVal1")).append("\n");
    					hasChanges = true;
    				}
    				if(!String.valueOf(existingSetting.get("set_val2")).equals(String.valueOf(settingMap.get("setVal2")))) {
    					changedItems.append("• 저온 설정값: ").append(existingSetting.get("set_val2")).append(" → ").append(settingMap.get("setVal2")).append("\n");
    					hasChanges = true;
    				}
    				if(!String.valueOf(existingSetting.get("set_val3")).equals(String.valueOf(settingMap.get("setVal3")))) {
    					changedItems.append("• 특정온도 설정값: ").append(existingSetting.get("set_val3")).append(" → ").append(settingMap.get("setVal3")).append("\n");
    					hasChanges = true;
    				}
    				
    				// 지연시간 비교 (시간+분을 분으로 변환하여 비교)
    				int existingDelay1 = Integer.parseInt(String.valueOf(existingSetting.get("delay_time1")));
    				int newDelay1 = Integer.parseInt(String.valueOf(settingMap.get("delayHour1"))) * 60 + Integer.parseInt(String.valueOf(settingMap.get("delayMin1")));
    				if(existingDelay1 != newDelay1) {
    					int oldHour = existingDelay1 / 60;
    					int oldMin = existingDelay1 % 60;
    					int newHour = newDelay1 / 60;
    					int newMin = newDelay1 % 60;
    					changedItems.append("• 고온알람 지연시간: ").append(oldHour).append("시간").append(oldMin).append("분 → ").append(newHour).append("시간").append(newMin).append("분\n");
    					hasChanges = true;
    				}
    				
    				int existingDelay2 = Integer.parseInt(String.valueOf(existingSetting.get("delay_time2")));
    				int newDelay2 = Integer.parseInt(String.valueOf(settingMap.get("delayHour2"))) * 60 + Integer.parseInt(String.valueOf(settingMap.get("delayMin2")));
    				if(existingDelay2 != newDelay2) {
    					int oldHour = existingDelay2 / 60;
    					int oldMin = existingDelay2 % 60;
    					int newHour = newDelay2 / 60;
    					int newMin = newDelay2 % 60;
    					changedItems.append("• 저온알람 지연시간: ").append(oldHour).append("시간").append(oldMin).append("분 → ").append(newHour).append("시간").append(newMin).append("분\n");
    					hasChanges = true;
    				}
    				
    				int existingDelay3 = Integer.parseInt(String.valueOf(existingSetting.get("delay_time3")));
    				int newDelay3 = Integer.parseInt(String.valueOf(settingMap.get("delayHour3"))) * 60 + Integer.parseInt(String.valueOf(settingMap.get("delayMin3")));
    				if(existingDelay3 != newDelay3) {
    					int oldHour = existingDelay3 / 60;
    					int oldMin = existingDelay3 % 60;
    					int newHour = newDelay3 / 60;
    					int newMin = newDelay3 % 60;
    					changedItems.append("• 특정온도알람 지연시간: ").append(oldHour).append("시간").append(oldMin).append("분 → ").append(newHour).append("시간").append(newMin).append("분\n");
    					hasChanges = true;
    				}
    				
    				int existingDelay4 = Integer.parseInt(String.valueOf(existingSetting.get("delay_time4")));
    				int newDelay4 = Integer.parseInt(String.valueOf(settingMap.get("delayHour4"))) * 60 + Integer.parseInt(String.valueOf(settingMap.get("delayMin4")));
    				if(existingDelay4 != newDelay4) {
    					int oldHour = existingDelay4 / 60;
    					int oldMin = existingDelay4 % 60;
    					int newHour = newDelay4 / 60;
    					int newMin = newDelay4 % 60;
    					changedItems.append("• DI알람 지연시간: ").append(oldHour).append("시간").append(oldMin).append("분 → ").append(newHour).append("시간").append(newMin).append("분\n");
    					hasChanges = true;
    				}
    				
    				int existingDelay5 = Integer.parseInt(String.valueOf(existingSetting.get("delay_time5")));
    				int newDelay5 = Integer.parseInt(String.valueOf(settingMap.get("delayHour5"))) * 60 + Integer.parseInt(String.valueOf(settingMap.get("delayMin5")));
    				if(existingDelay5 != newDelay5) {
    					int oldHour = existingDelay5 / 60;
    					int oldMin = existingDelay5 % 60;
    					int newHour = newDelay5 / 60;
    					int newMin = newDelay5 % 60;
    					changedItems.append("• 통신이상알람 지연시간: ").append(oldHour).append("시간").append(oldMin).append("분 → ").append(newHour).append("시간").append(newMin).append("분\n");
    					hasChanges = true;
    				}
    				
    				// 재전송지연시간 비교
    				int existingReDelay1 = Integer.parseInt(String.valueOf(existingSetting.get("re_delay_time1")));
    				int newReDelay1 = Integer.parseInt(String.valueOf(settingMap.get("reDelayHour1"))) * 60 + Integer.parseInt(String.valueOf(settingMap.get("reDelayMin1")));
    				if(existingReDelay1 != newReDelay1) {
    					int oldHour = existingReDelay1 / 60;
    					int oldMin = existingReDelay1 % 60;
    					int newHour = newReDelay1 / 60;
    					int newMin = newReDelay1 % 60;
    					changedItems.append("• 고온알람 재전송지연시간: ").append(oldHour).append("시간").append(oldMin).append("분 → ").append(newHour).append("시간").append(newMin).append("분\n");
    					hasChanges = true;
    				}
    				
    				int existingReDelay2 = Integer.parseInt(String.valueOf(existingSetting.get("re_delay_time2")));
    				int newReDelay2 = Integer.parseInt(String.valueOf(settingMap.get("reDelayHour2"))) * 60 + Integer.parseInt(String.valueOf(settingMap.get("reDelayMin2")));
    				if(existingReDelay2 != newReDelay2) {
    					int oldHour = existingReDelay2 / 60;
    					int oldMin = existingReDelay2 % 60;
    					int newHour = newReDelay2 / 60;
    					int newMin = newReDelay2 % 60;
    					changedItems.append("• 저온알람 재전송지연시간: ").append(oldHour).append("시간").append(oldMin).append("분 → ").append(newHour).append("시간").append(newMin).append("분\n");
    					hasChanges = true;
    				}
    				
    				int existingReDelay3 = Integer.parseInt(String.valueOf(existingSetting.get("re_delay_time3")));
    				int newReDelay3 = Integer.parseInt(String.valueOf(settingMap.get("reDelayHour3"))) * 60 + Integer.parseInt(String.valueOf(settingMap.get("reDelayMin3")));
    				if(existingReDelay3 != newReDelay3) {
    					int oldHour = existingReDelay3 / 60;
    					int oldMin = existingReDelay3 % 60;
    					int newHour = newReDelay3 / 60;
    					int newMin = newReDelay3 % 60;
    					changedItems.append("• 특정온도알람 재전송지연시간: ").append(oldHour).append("시간").append(oldMin).append("분 → ").append(newHour).append("시간").append(newMin).append("분\n");
    					hasChanges = true;
    				}
    				
    				int existingReDelay4 = Integer.parseInt(String.valueOf(existingSetting.get("re_delay_time4")));
    				int newReDelay4 = Integer.parseInt(String.valueOf(settingMap.get("reDelayHour4"))) * 60 + Integer.parseInt(String.valueOf(settingMap.get("reDelayMin4")));
    				if(existingReDelay4 != newReDelay4) {
    					int oldHour = existingReDelay4 / 60;
    					int oldMin = existingReDelay4 % 60;
    					int newHour = newReDelay4 / 60;
    					int newMin = newReDelay4 % 60;
    					changedItems.append("• DI알람 재전송지연시간: ").append(oldHour).append("시간").append(oldMin).append("분 → ").append(newHour).append("시간").append(newMin).append("분\n");
    					hasChanges = true;
    				}
    				
    				int existingReDelay5 = Integer.parseInt(String.valueOf(existingSetting.get("re_delay_time5")));
    				int newReDelay5 = Integer.parseInt(String.valueOf(settingMap.get("reDelayHour5"))) * 60 + Integer.parseInt(String.valueOf(settingMap.get("reDelayMin5")));
    				if(existingReDelay5 != newReDelay5) {
    					int oldHour = existingReDelay5 / 60;
    					int oldMin = existingReDelay5 % 60;
    					int newHour = newReDelay5 / 60;
    					int newMin = newReDelay5 % 60;
    					changedItems.append("• 통신이상알람 재전송지연시간: ").append(oldHour).append("시간").append(oldMin).append("분 → ").append(newHour).append("시간").append(newMin).append("분\n");
    					hasChanges = true;
    				}
    			}
    			
				if(hasChanges) {
					// 변경사항이 있을 때만 저장
					adminService.insertSetting(settingMap);
					
					// 마지막 줄바꿈 제거
					String changedItemsStr = changedItems.toString();
					if(changedItemsStr.length() > 1) {
						changedItemsStr = changedItemsStr.substring(0, changedItemsStr.length() - 1);
					}
					resultMap.put("resultCode", "200");
					resultMap.put("resultMessage", "알람설정이 저장되었습니다.\n\n변경된 항목:\n" + changedItemsStr);
				} else {
					// 변경사항이 없으면 저장하지 않음
					resultMap.put("resultCode", "200");
					resultMap.put("resultMessage", "변경된 항목이 없습니다. 저장하지 않았습니다.");
				}
			} catch(Exception e) {
    			e.printStackTrace();
    			logger.error("ERROR : " + e.toString());
				resultMap.put("resultCode", "999");
				resultMap.put("resultMessage", "fail");
			}
		} else {
			resultMap.put("resultCode", "998");
			resultMap.put("resultMessage", "fail");
		}
    	
    	return resultMap;
    }

	/**
	 * 챠트 설정 화면
	 * @param req
	 * @param res
	 * @param userId
	 * @param sensorUuid
	 * @param model
	 * @return
	 */
    @RequestMapping(value = "/chartSetting", method = RequestMethod.GET)
	public String chartSetting(
			HttpServletRequest req
			, HttpServletResponse res
			, Model model
		) {

    	String result = "admin/chartSetting";
    	HttpSession session = req.getSession();
    	
    	// URL 파라미터에서 sensorUuid 가져오기
    	String sensorUuid = req.getParameter("sensorUuid");

        // 공통 세션 관리 서비스 사용
        SessionManagementService.SessionInfo sessionInfo = sessionManagementService.validateAndGetSessionInfo(session, req);
        
        if (!sessionInfo.isValid()) {
            logger.warn("차트 설정 페이지 접근 - 세션 검증 실패, 리다이렉트: {}", sessionInfo.getRedirectUrl());
            return sessionInfo.getRedirectUrl();
        }
        
        // 세션 정보를 모델에 설정
        sessionManagementService.setSessionInfoToModel(session, model);
        
        // 세션에서 사용자 정보 가져오기
        String sessionUserId = sessionInfo.getUserId();
        
        // 세션 우선: DB에서 직접 사이드바 데이터 조회 (세션 userId 사용)
        commonController.addSidebarData(sessionUserId, model, session);

		UserInfo user = new UserInfo();
		String topicStr = "";
		String chartType = "";

		if(null != sessionUserId && !"".equals(sessionUserId)) {
			// 사용자 아이디가 있을 경우 사용자 아이디로 사용자 정보와 연결된 센서 정보를 가져온다.
			model.addAttribute("userId", sessionUserId);
			model.addAttribute("loginUserId", sessionUserId);
			
			// 세션에서 parentUserId 가져오기
			String parentUserId = (String) session.getAttribute("parentUserId");
			if(!StringUtil.isEmpty(parentUserId)) {
				model.addAttribute("parentUserId", parentUserId);
			} else {
				model.addAttribute("parentUserId", sessionUserId);
			}
			
			model.addAttribute("sensorUuid", sensorUuid);

			Map<String, Object> userInfo = new HashMap<String, Object>();
			userInfo = adminService.getUserInfo(sessionUserId, sensorUuid);

			if(null != userInfo && !"".equals(userInfo)) {
				user = (UserInfo) userInfo.get("userInfo");
				Map<String, Object> sensor = (Map<String, Object>) userInfo.get("sensorInfo");

                if(null != sensor && 0 < sensor.size()) {
                    // 설정/출력 제어 요청 토픽은 'TC' 세그먼트 사용
                    topicStr = "HBEE/"+String.valueOf(sensor.get("sensor_id"))+"/TC/"+String.valueOf(sensor.get("sensor_uuid"))+"/SER";
                    chartType = String.valueOf(sensor.get("chart_type"));
                }

				model.addAttribute("user", user);
				model.addAttribute("topicStr", topicStr);
				model.addAttribute("sensorUuid", sensorUuid);
				model.addAttribute("chartType", chartType);
			}
		} else {
			result = "redirect:/login/login";
		}

    	return result;
	}

	/**
	 * 챠트 설정 저장 처리
	 * @param req
	 * @param res
	 * @param settingMap
	 * @return
	 */
	@RequestMapping(value = "/setChart", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> setChart(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> settingMap
		) {
    	Map<String, Object> resultMap = new HashMap<String, Object>();

    	if(null != settingMap && 0 < settingMap.size()) {
    		try {
    			adminService.updateSensorInfo(settingMap);

    			resultMap.put("resultCode", "200");
    			resultMap.put("resultMsg", "챠트 설정 저장 성공");
			} catch(Exception e) {
				resultMap.put("resultCode", "999");
				resultMap.put("resultMsg", "챠트 설정 저장 실패");
			}
		} else {
			resultMap.put("resultCode", "998");
			resultMap.put("resultMsg", "챠트 설정 저장에 필요한 필수 정보 누락");
		}

    	return resultMap;
	}

	/**
	 * 통신 에러 체크
	 * @param req
	 * @param res
	 * @param chkMap
	 * @return
	 */
	@RequestMapping(value = "/chkError", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> chkError(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> chkMap
		) {
    	Map<String, Object> resultMap = new HashMap<String, Object>();

    	if(null != chkMap && 0 < chkMap.size()) {
    		try {
    			int result = 0;
    			result = adminService.chkError(chkMap);

    			if(result > 0) {
    				resultMap.put("resultCode", "200");
    				resultMap.put("resultMessage", "ok");
				} else {
					resultMap.put("resultCode", "200");
					resultMap.put("resultMessage", "Error");
				}
			} catch(Exception e) {
				resultMap.put("resultCode", "200");
				resultMap.put("resultMessage", "Error");
			}
		}

    	return resultMap;
	}

	/**
	 * 사용자 목록 (관리자 화면)
	 * @param req
	 * @param res
	 * @param userId
	 * @param userGrade
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/userList", method = RequestMethod.GET)
	public String userList(
			HttpServletRequest req
			, HttpServletResponse res
			, Model model
	    ) {
    	String result = "admin/userList";
    	
    	HttpSession session = req.getSession();
    	
    	// 통합 세션 검증 (기본 + 보안 + 권한 + 모델 설정)
    	SessionValidationResult validationResult = unifiedSessionService.validateSession(session, req, model, "U"); // 일반 사용자 이상 권한 필요
    	
    	if (!validationResult.isValid()) {
    		logger.warn("사용자 목록 페이지 접근 - 통합 세션 검증 실패, 리다이렉트: {}, 오류: {}", 
    			validationResult.getRedirectUrl(), validationResult.getErrorMessage());
    		return validationResult.getRedirectUrl();
    	}
    	
    	// 검증 성공 시 사용자 정보 사용
    	String sessionUserId = validationResult.getUserId();
    	String sessionUserGrade = validationResult.getUserGrade();
    	String sessionUserNm = validationResult.getUserNm();
    	
    	logger.info("사용자 목록 페이지 - 통합 세션 검증 성공: userId={}, userGrade={}, userNm={}", 
    			sessionUserId, sessionUserGrade, sessionUserNm);
    	
    	// 통합 세션 검증에서 이미 권한 검사 완료 (U 등급 이상)
    	// A(관리자) 또는 U(일반사용자)인 경우 사용자 목록 조회 및 화면 표시
		List<UserInfo> userList = new ArrayList<UserInfo>();
		Map<String, Object> userMap = new HashMap<String, Object>();

		try {
			if("A".equals(sessionUserGrade)) {
				// A(관리자)인 경우 모든 사용자 목록 조회 (최적화된 쿼리 사용)
				userMap = loginService.getUserListWithActivityStatus(sessionUserId);
			} else {
				// U(일반사용자)인 경우 자신과 자신이 생성한 B계정 조회 (최적화된 쿼리 사용)
				userMap = loginService.getUserAndSubUserListWithActivityStatus(sessionUserId, sessionUserId);
			}
		} catch(Exception e) {
			unifiedErrorHandler.logError("데이터 처리", e);
		}

		if(null != userMap && 0 < userMap.size()) {
			userList = (List<UserInfo>) userMap.get("userList");
			
			// 최적화된 쿼리에서 이미 활동 상태가 설정되어 있음
			// 추가 처리 없이 바로 모델에 추가
			model.addAttribute("userList", userList);
			
			logger.info("최적화된 사용자 목록 조회 완료 - 사용자 수: {}", userList != null ? userList.size() : 0);
		}

    	return result;
	}

	@RequestMapping(value = "getChangeList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> getChangeList(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> changeMap
		) {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != changeMap && 0 < changeMap.size()) {
			String userId = String.valueOf(changeMap.get("userId"));
			String userGrade = String.valueOf(changeMap.get("userGrade"));

			if(null != userId && !"".equals(userId) && 0 < userId.length()) {
				if(null != userGrade && !"".equals(userGrade) && 0 < userGrade.length()) {
					if("A".equals(userGrade) || "U".equals(userGrade)) {
						List<UserInfo> userList = new ArrayList<UserInfo>();
						Map<String, Object> userMap = new HashMap<String, Object>();

						try {
							HttpSession session = req.getSession();
							String currentUserId = (String) session.getAttribute("userId");
							
							if("A".equals(userGrade)) {
								// A(관리자)인 경우 모든 사용자 목록 조회 (최적화된 쿼리 사용)
								userMap = loginService.getUserListWithActivityStatus(currentUserId);
							} else {
								// U(일반사용자)인 경우 자신과 자신이 생성한 B계정 조회 (최적화된 쿼리 사용)
								userMap = loginService.getUserAndSubUserListWithActivityStatus(userId, currentUserId);
							}
						} catch(Exception e) {
							unifiedErrorHandler.logError("데이터 처리", e);
						}

						if(null != userMap && 0 < userMap.size()) {
							userList = (List<UserInfo>) userMap.get("userList");
							
							// 최적화된 쿼리에서 이미 활동 상태가 설정되어 있음
							// 추가 처리 없이 바로 결과에 추가
							resultMap.put("resultCode", "200");
							resultMap.put("userList", userList);
							
							logger.info("최적화된 사용자 목록 조회 완료 - 사용자 수: {}", userList != null ? userList.size() : 0);
						}
					} else {
						resultMap.put("resultCode", "999");
						resultMap.put("resultMessage", "등급 오류");
					}
				} else {
					resultMap.put("resultCode", "999");
					resultMap.put("resultMessage", "등급 오류");
				}
			} else {
				resultMap.put("resultCode", "999");
				resultMap.put("resultMessage", "아이디 없음");
			}
		} else {
			resultMap.put("resultCode", "999");
			resultMap.put("resultMessage", "필수정보 누락");
		}

		return resultMap;
	}

	/**
	 * 사용자 삭제 (관리자 화면)
	 * @param req
	 * @param res
	 * @param deleteMap
	 * @return
	 */
	@RequestMapping(value = "/deleteUser", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> deleteUser(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> deleteMap
		) {
    	Map<String, Object> resultMap = new HashMap<String, Object>();

    	if(null != deleteMap && 0 < deleteMap.size()) {
    		String userId = String.valueOf(deleteMap.get("userId"));

    		if(null != userId && !"".equals(userId) && 0 < userId.length()) {
    			try {
    				// 삭제할 사용자의 등급 확인
    				UserInfo userInfo = null;
    				try {
    					userInfo = loginService.getUserInfoByUserId(userId);
    				} catch(Exception e) {
    					logger.warn("사용자 정보 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
    				}
    				
    				if(userInfo != null) {
    					String userGrade = userInfo.getUserGrade();
    					
    					if("B".equals(userGrade)) {
    						// 부계정인 경우: 부계정 사용자 정보만 삭제 (장치 정보 보존)
    						adminService.deleteSubUser(userId);
    						logger.info("부계정 삭제 완료 - userId: {}, userGrade: {}", userId, userGrade);
    					} else {
    						// 메인 사용자인 경우: 모든 정보 삭제
    						adminService.deleteUser(userId);
    						logger.info("메인 사용자 삭제 완료 - userId: {}, userGrade: {}", userId, userGrade);
    					}
    				} else {
    					// 사용자 정보가 없는 경우 기본 삭제 메서드 사용
    					adminService.deleteUser(userId);
    					logger.warn("사용자 정보 없음, 기본 삭제 메서드 사용 - userId: {}", userId);
    				}
    				
    				resultMap.put("resultCode", "200");
				} catch(Exception e) {
    				unifiedErrorHandler.logError("데이터 처리", e);
    				resultMap.put("resultCode", "999");
    				resultMap.put("resultMessage", "사용자 삭제 중 오류가 발생했습니다.");
				}
			}
		} else {
			resultMap.put("resultCode", "999");
			resultMap.put("resultMessage", "삭제할 사용자 정보가 없습니다.");
		}

    	return resultMap;
	}

	/**
	 * 사용자 수정 (관리자 화면)
	 * @param req
	 * @param res
	 * @param modifyMap
	 * @return
	 */
	@RequestMapping(value = "/modifyUser", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modifyUser(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> modifyMap
	) {
    	Map<String, Object> resultMap = new HashMap<String, Object>();
    	
    	logger.info("=== 사용자 정보 수정 요청 시작 ===");
    	logger.info("수정 데이터: {}", modifyMap);

    	if(null != modifyMap && 0 < modifyMap.size()) {
    		try {
    			adminService.updateUser(modifyMap);
    			resultMap.put("resultCode", "200");
    			resultMap.put("resultMessage", "사용자 정보 수정 성공");
    			logger.info("사용자 정보 수정 성공 - userId: {}", modifyMap.get("userId"));
		} catch(Exception e) {
    			unifiedErrorHandler.logError("사용자 정보 수정", e);
    			resultMap.put("resultCode", "500");
    			resultMap.put("resultMessage", "사용자 정보 수정 실패: " + e.getMessage());
    			logger.error("사용자 정보 수정 실패 - userId: {}, error: {}", modifyMap.get("userId"), e.toString());
		}
	} else {
		resultMap.put("resultCode", "400");
		resultMap.put("resultMessage", "수정 데이터가 없습니다");
		logger.warn("사용자 정보 수정 실패 - 수정 데이터가 비어있음");
	}

    	logger.info("=== 사용자 정보 수정 요청 종료 - resultCode: {} ===", resultMap.get("resultCode"));
    	return resultMap;
	}

	/**
	 * 사용자 상세
	 * @param req
	 * @param res
	 * @param userId
	 * @param userGrade
	 * @param dtlUser
	 * @param gu
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/userDetail", method = {RequestMethod.GET, RequestMethod.POST})
	public String userDetail(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestParam(name = "dtlUser", required = false) String dtlUser
			, Model model
		) {
    	String result = "admin/userDetail";
    	HttpSession session = req.getSession();

    // 공통 세션 관리 서비스 사용
    SessionManagementService.SessionInfo sessionInfo = sessionManagementService.validateAndGetSessionInfo(session, req);
    
    if (!sessionInfo.isValid()) {
        logger.warn("사용자 상세 페이지 접근 - 세션 검증 실패, 리다이렉트: {}", sessionInfo.getRedirectUrl());
        return sessionInfo.getRedirectUrl();
    }
    
    // 세션 정보를 모델에 설정
    sessionManagementService.setSessionInfoToModel(session, model);
    
    // 세션에서 사용자 정보 가져오기
    String sessionUserId = sessionInfo.getUserId();
    String sessionUserGrade = sessionInfo.getUserGrade();
    String sessionUserNm = sessionInfo.getUserNm();

    // 사이드바 데이터는 세션의 userId로 보강
    commonController.addSidebarData(sessionUserId, model, session);
    
    // dtlUser가 없으면 세션에서 가져오기 (새로고침 대응)
    if(dtlUser == null || dtlUser.isEmpty()) {
        dtlUser = (String) session.getAttribute("targetDetailUserId");
        logger.info("파라미터 dtlUser 없음, 세션에서 복구: {}", dtlUser);
    } else {
        // 새로운 dtlUser가 있으면 세션에 저장
        session.setAttribute("targetDetailUserId", dtlUser);
        logger.info("targetDetailUserId 세션에 저장: {}", dtlUser);
    }
		
		

		if(null != sessionUserId && !"".equals(sessionUserId) && 0 < sessionUserId.length()) {
			if(null != sessionUserGrade && !"".equals(sessionUserGrade) && 0 < sessionUserGrade.length()) {
				if("A".equals(sessionUserGrade) || "U".equals(sessionUserGrade)) {
					if(null != dtlUser && !"".equals(dtlUser) && 0 < dtlUser.length()) {
						// 사용자 정보가 있고 등급이 관리자(A) 또는 일반사용자(U) 일때 사용자 상세 정보 표시
						UserInfo userInfo = null;

						try {
							// 비밀번호 검증 없이 사용자 정보만 조회
							userInfo = loginService.getUserInfoByUserId(dtlUser);
						} catch (Exception e) {
							logger.error("사용자 상세 정보 조회 실패 - dtlUser: {}, error: {}", dtlUser, e.getMessage());
						}

					if(null != userInfo) {
						logger.info("사용자 상세 정보 조회 성공 - dtlUser: {}, userTel: {}, userEmail: {}", 
							dtlUser, userInfo.getUserTel(), userInfo.getUserEmail());
						model.addAttribute("userInfo", userInfo);
						// 사용자 정보가 있으면 해당 사용자의 장치 정보를 가져온다.
						List<Map<String, Object>> sensorList = new ArrayList<Map<String, Object>>();
						
						// UserInfo에서 parent_user_id 가져오기
						String parentUserId = userInfo.getParentUserId();
						if(parentUserId == null || parentUserId.length() == 0 || "null".equalsIgnoreCase(parentUserId)) {
							parentUserId = dtlUser;  // parent_user_id가 없으면 자기 자신이 주계정
						}
						
						try {
							sensorList = adminService.getFullSensorList(dtlUser, parentUserId);
							logger.info("장치 조회 - targetUserId: {}, parentUserId: {}, 조회된 장치 수: {}", 
								dtlUser, parentUserId, (sensorList != null ? sensorList.size() : 0));
						} catch(Exception e) {
							logger.error("사용자 장치 정보 조회 실패 - targetUserId: {}, parentId: {}, error: {}", 
								dtlUser, parentUserId, e.toString());
						}

						if(null != sensorList && 0 < sensorList.size()) {
							model.addAttribute("sensorList", sensorList);
						}
						} else {
							logger.warn("사용자 정보를 찾을 수 없습니다 - dtlUser: {}", dtlUser);
						}
					} else {

					}

					model.addAttribute("userId", sessionUserId);
					model.addAttribute("userGrade", sessionUserGrade);
					model.addAttribute("loginUserId", sessionUserId);
					
					// 세션에서 parentUserId 가져오기
					String parentUserId_userList = (String) session.getAttribute("parentUserId");
					if(!StringUtil.isEmpty(parentUserId_userList)) {
						model.addAttribute("parentUserId", parentUserId_userList);
					} else {
						model.addAttribute("parentUserId", sessionUserId);
					}
				} else {
					result = "redirect:/main/main";
				}
			} else {
				result = "redirect:/main/main";
			}
		} else {
			result = "redirect:/login/login";
		}

    	return result;
	}

	/**
	 * 사용자 정보 수정 화면
	 * @param req
	 * @param res
	 * @param userId
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/userModify", method = {RequestMethod.GET, RequestMethod.POST})
	public String userModify(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestParam(name = "userId", required = false) String userId
			, Model model
		) {
		String result = "admin/userModify";
		HttpSession session = req.getSession();

		// 공통 세션 관리 서비스 사용
		SessionManagementService.SessionInfo sessionInfo = sessionManagementService.validateAndGetSessionInfo(session, req);
		
		if (!sessionInfo.isValid()) {
			logger.warn("사용자 수정 페이지 접근 - 세션 검증 실패, 리다이렉트: {}", sessionInfo.getRedirectUrl());
			return sessionInfo.getRedirectUrl();
		}
		
		// 세션 정보를 모델에 설정
		sessionManagementService.setSessionInfoToModel(session, model);
		
		// 세션에서 사용자 정보 가져오기
		String sessionUserId = sessionInfo.getUserId();
		String sessionUserGrade = sessionInfo.getUserGrade();

		// 사이드바 데이터는 세션의 userId로 보강
		commonController.addSidebarData(sessionUserId, model, session);

		// userId가 없으면 세션에서 가져오기 (새로고침 대응)
		if(userId == null || userId.isEmpty()) {
			userId = (String) session.getAttribute("targetUserId");
			logger.info("파라미터 userId 없음, 세션에서 복구: {}", userId);
		} else {
			// 새로운 userId가 있으면 세션에 저장
			session.setAttribute("targetUserId", userId);
			logger.info("targetUserId 세션에 저장: {}", userId);
		}

		if(null != sessionUserId && !"".equals(sessionUserId) && 0 < sessionUserId.length()) {
			if(null != sessionUserGrade && !"".equals(sessionUserGrade) && 0 < sessionUserGrade.length()) {
				if("A".equals(sessionUserGrade) || "U".equals(sessionUserGrade)) {
					if(null != userId && !"".equals(userId) && 0 < userId.length()) {
						// 사용자 정보가 있고 등급이 관리자(A) 또는 일반사용자(U) 일때 사용자 수정 화면 표시
						UserInfo userInfo = null;

						try {
							// 비밀번호 검증 없이 사용자 정보만 조회
							userInfo = loginService.getUserInfoByUserId(userId);
						} catch (Exception e) {
							logger.error("사용자 수정 정보 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
						}

						if(null != userInfo) {
							model.addAttribute("userInfo", userInfo);
						} else {
							logger.warn("사용자 정보를 찾을 수 없습니다 - userId: {}", userId);
						}
					}

					model.addAttribute("userId", sessionUserId);
					model.addAttribute("userGrade", sessionUserGrade);
					model.addAttribute("loginUserId", sessionUserId);
					
					// 세션에서 parentUserId 가져오기
					String parentUserId_userDetail = (String) session.getAttribute("parentUserId");
					if(!StringUtil.isEmpty(parentUserId_userDetail)) {
						model.addAttribute("parentUserId", parentUserId_userDetail);
					} else {
						model.addAttribute("parentUserId", sessionUserId);
					}
				} else {
					result = "redirect:/main/main";
				}
			} else {
				result = "redirect:/main/main";
			}
		} else {
			result = "redirect:/login/login";
		}

		return result;
	}

	/**
	 * 부계정 생성 화면
	 * @param req
	 * @param res
	 * @param userId
	 * @param userGrade
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/createSub", method = RequestMethod.GET)
	public String createSub(
			HttpServletRequest req
			, HttpServletResponse res
			, Model model
		) {
    	String result = "admin/createSub";
    	
    	HttpSession session = req.getSession();
    	
    	// 공통 세션 관리 서비스 사용
    	SessionManagementService.SessionInfo sessionInfo = sessionManagementService.validateAndGetSessionInfo(session, req);
    	
    	if (!sessionInfo.isValid()) {
    		logger.warn("하위 사용자 생성 페이지 접근 - 세션 검증 실패, 리다이렉트: {}", sessionInfo.getRedirectUrl());
    		return sessionInfo.getRedirectUrl();
    	}
    	
    	// 세션 정보를 모델에 설정
    	sessionManagementService.setSessionInfoToModel(session, model);
    	
    	// 세션에서 사용자 정보 가져오기
    	String sessionUserId = sessionInfo.getUserId();
    	String sessionUserGrade = sessionInfo.getUserGrade();
    	String sessionUserNm = sessionInfo.getUserNm();
    	
    	// 표준화된 권한 검사 (A, U 등급만 접근 가능, B 등급은 제외)
    	if (PermissionUtil.isSubAccount(sessionUserGrade)) {
    		// 표준화된 리다이렉트 로직 사용
    		RedirectUtil.RedirectResult redirectResult = RedirectUtil.redirectToMain(session, req, "부계정은 하위 사용자 생성 권한이 없음");
    		logger.info(RedirectUtil.createRedirectLog(redirectResult, req));
    		return redirectResult.getRedirectUrl();
    	}
    	
    	// 세션에서 사용자 정보를 모델에 직접 설정
    	if(!StringUtil.isEmpty(sessionUserId)) {
    		model.addAttribute("userId", sessionUserId);
    		model.addAttribute("loginUserId", sessionUserId);
    		
    		// 세션에서 parentUserId 가져오기
    		String parentUserId_userModify = (String) session.getAttribute("parentUserId");
    		if(!StringUtil.isEmpty(parentUserId_userModify)) {
    			model.addAttribute("parentUserId", parentUserId_userModify);
    		} else {
    			model.addAttribute("parentUserId", sessionUserId);
    		}
    	}
    	
    	if(!StringUtil.isEmpty(sessionUserGrade)) {
    		model.addAttribute("userGrade", sessionUserGrade);
    	}
    	
    	// 세션에서 사용자 정보를 모델에 직접 설정
    	if(!StringUtil.isEmpty(sessionUserNm)) {
    		model.addAttribute("userNm", sessionUserNm);
    	}

    	// 새로운 방식: DB에서 직접 사이드바 데이터 조회
    	commonController.addSidebarData(sessionUserId, model, session);
    	

    	// 사용자 아이디가 있을 경우
    	if(null != sessionUserId && !"".equals(sessionUserId) && 0 < sessionUserId.length()) {
    		// 해당 사용자 등급이 U, A 인 경우에만 부계정 생성 가능
    		if(null != sessionUserGrade && !"".equals(sessionUserGrade) && 0 < sessionUserGrade.length()) {
    			// A(관리자) 또는 U(일반사용자)인 경우에만 부계정 생성 가능
    			if("A".equals(sessionUserGrade) || "U".equals(sessionUserGrade)) {
    				model.addAttribute("userId", sessionUserId);
    				model.addAttribute("userGrade", sessionUserGrade);
    				model.addAttribute("loginUserId", sessionUserId);
    				
    				// 세션에서 parentUserId 가져오기
    				String parentUserId_createSub = (String) session.getAttribute("parentUserId");
    				if(!StringUtil.isEmpty(parentUserId_createSub)) {
    					model.addAttribute("parentUserId", parentUserId_createSub);
    				} else {
    					model.addAttribute("parentUserId", sessionUserId);
    				}
    				
    				// U 등급 사용자의 경우 부계정 목록 확인
    				if("U".equals(sessionUserGrade)) {
    					try {
    						Map<String, Object> subUserMap = loginService.getSubUserList(sessionUserId);
    						if(null != subUserMap && subUserMap.size() > 0) {
    							List<UserInfo> subUserList = (List<UserInfo>) subUserMap.get("userList");
    							if(null != subUserList && subUserList.size() > 0) {
    								model.addAttribute("hasSubUsers", true);
    							} else {
    								model.addAttribute("hasSubUsers", false);
    							}
    						} else {
    							model.addAttribute("hasSubUsers", false);
    						}
    					} catch(Exception e) {
    						unifiedErrorHandler.logError("하위 사용자 조회", e);
    						model.addAttribute("hasSubUsers", false);
    					}
    				} else {
    					model.addAttribute("hasSubUsers", true); // A 등급은 항상 true
    				}
    			} else {
    				result = "redirect:/main/main";
    			}
    		}
    	} else {
    		result = "redirect:/login/login";
    	}

    	return result;
	}

	/**
	 * 부계정 생성 처리
	 * @param req
	 * @param res
	 * @param createMap
	 * @return
	 */
	@RequestMapping(value = "/createSubProc", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> createSubProc(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> createMap
		) {
    	Map<String, Object> resultMap = new HashMap<String, Object>();

    	if(null != createMap && 0 < createMap.size()) {
    		try {
    			// 세션에서 userId 가져오기
    			HttpSession session = req.getSession();
    			String sessionUserId = (String) session.getAttribute("userId");
    			
    			// createMap에 userId가 없으면 세션에서 설정
    			if(!createMap.containsKey("userId") || createMap.get("userId") == null || 
    			   "null".equalsIgnoreCase(String.valueOf(createMap.get("userId"))) ||
    			   String.valueOf(createMap.get("userId")).isEmpty()) {
    				logger.warn("createMap에 userId 없음, 세션에서 설정: {}", sessionUserId);
    				createMap.put("userId", sessionUserId);
    			}
    			
    			logger.info("부계정 생성 요청 - createMap: {}", createMap);
    			adminService.createSubProc(createMap);

    			resultMap.put("resultCode", "200");
    			resultMap.put("resultMessage", "부계정 생성 성공");
		} catch(Exception e) {
    			unifiedErrorHandler.logError("부계정 생성", e);
    			resultMap.put("resultCode", "500");
    			resultMap.put("resultMessage", "부계정 생성 실패: " + e.getMessage());
		}
	} else {
		resultMap.put("resultCode", "400");
		resultMap.put("resultMessage", "요청 데이터가 없습니다");
	}

    	return resultMap;
	}

	/**
	 * 사용자 등급 변경
	 * @param req
	 * @param res
	 * @param changeMap
	 * @return
	 */
	@RequestMapping(value = "/changeUserGrade", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> changeUserGrade(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody Map<String, Object> changeMap
		) {
    	Map<String, Object> resultMap = new HashMap<String, Object>();

    	if(null != changeMap && 0 < changeMap.size()) {
    		String targetUserId = String.valueOf(changeMap.get("targetUserId"));
    		String newGrade = String.valueOf(changeMap.get("newGrade"));
    		String adminUserId = String.valueOf(changeMap.get("adminUserId"));

    		if(null != targetUserId && !"".equals(targetUserId) && 0 < targetUserId.length()) {
    			if(null != newGrade && !"".equals(newGrade) && 0 < newGrade.length()) {
    				if("A".equals(newGrade) || "U".equals(newGrade) || "B".equals(newGrade)) {
    					try {
    						// 현재 사용자 권한 확인
    						LoginVO loginVO = new LoginVO();
    						loginVO.setUserId(adminUserId);
    						
    						Map<String, Object> adminInfo = loginService.getUserInfo(loginVO);
    						
    						if(null != adminInfo && 0 < adminInfo.size()) {
    							UserInfo adminUser = (UserInfo) adminInfo.get("userInfo");
    							
    							if(null != adminUser) {
    								String currentUserGrade = adminUser.getUserGrade();
    								boolean canChange = false;
    								
    								// 현재 사용자의 등급에 따라 조정 가능한 등급 확인
    								if("A".equals(currentUserGrade)) {
    									// A 등급은 모든 등급 조정 가능
    									canChange = true;
    								} else if("U".equals(currentUserGrade)) {
    									// U 등급은 U, B 등급만 조정 가능
    									if("U".equals(newGrade) || "B".equals(newGrade)) {
    										canChange = true;
    									}
    								}
    								
    								if(canChange) {
    									// 등급 변경 처리
    									Map<String, Object> updateMap = new HashMap<String, Object>();
    									updateMap.put("userId", targetUserId);
    									updateMap.put("userGrade", newGrade);
    									
    									adminService.updateUserGrade(updateMap);
    									
    									resultMap.put("resultCode", "200");
    									resultMap.put("resultMessage", "사용자 등급 변경이 완료되었습니다.");
    								} else {
    									resultMap.put("resultCode", "999");
    									resultMap.put("resultMessage", "해당 등급으로 변경할 권한이 없습니다.");
    								}
    							} else {
    								resultMap.put("resultCode", "999");
    								resultMap.put("resultMessage", "사용자 정보를 찾을 수 없습니다.");
    							}
    						} else {
    							resultMap.put("resultCode", "999");
    							resultMap.put("resultMessage", "사용자 정보를 찾을 수 없습니다.");
    						}
    					} catch(Exception e) {
    						unifiedErrorHandler.logError("데이터 처리", e);
    						resultMap.put("resultCode", "999");
    						resultMap.put("resultMessage", "등급 변경 중 오류가 발생했습니다.");
    					}
    				} else {
    					resultMap.put("resultCode", "999");
    					resultMap.put("resultMessage", "유효하지 않은 등급입니다.");
    				}
    			} else {
    				resultMap.put("resultCode", "999");
    				resultMap.put("resultMessage", "새 등급이 입력되지 않았습니다.");
    			}
    		} else {
    			resultMap.put("resultCode", "999");
    			resultMap.put("resultMessage", "대상 사용자 ID가 입력되지 않았습니다.");
    		}
    	} else {
    		resultMap.put("resultCode", "999");
    		resultMap.put("resultMessage", "요청 데이터가 없습니다.");
    	}

    	return resultMap;
	}

	/**
	 * 기존 사용자들의 활동 상태를 모두 비활성으로 초기화
	 */
	@RequestMapping(value = "/resetUserActivityStatus", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> resetUserActivityStatus(
			HttpServletRequest req,
			HttpServletResponse res
	) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		try {
			// 모든 사용자를 비활성 상태로 초기화
			adminService.resetAllUserActivityStatus();
			
			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "모든 사용자의 활동 상태가 비활성으로 초기화되었습니다.");
			logger.info("사용자 활동 상태 초기화 완료");
			
		} catch(Exception e) {
			logger.error("사용자 활동 상태 초기화 실패: " + e.toString());
			resultMap.put("resultCode", "999");
			resultMap.put("resultMessage", "사용자 활동 상태 초기화 실패 - " + e.toString());
		}
		
		return resultMap;
	}
    
    @RequestMapping(value = "/resetSpecificUserActivityStatus", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> resetSpecificUserActivityStatus(
            HttpServletRequest req,
            HttpServletResponse res,
            @RequestBody Map<String, Object> requestData
    ) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String userId = (String) requestData.get("userId");
            if (userId != null && !userId.isEmpty()) {
                adminService.resetSpecificUserActivityStatus(userId);
                resultMap.put("resultCode", "200");
                resultMap.put("resultMessage", "사용자 " + userId + "의 활동 상태가 비활성으로 초기화되었습니다.");
                logger.info("특정 사용자 활동 상태 초기화 완료 - userId: {}", userId);
            } else {
                resultMap.put("resultCode", "400");
                resultMap.put("resultMessage", "사용자 ID가 제공되지 않았습니다.");
            }
        } catch(Exception e) {
            logger.error("특정 사용자 활동 상태 초기화 실패: " + e.toString());
            resultMap.put("resultCode", "999");
            resultMap.put("resultMessage", "특정 사용자 활동 상태 초기화 실패 - " + e.toString());
        }
        return resultMap;
    }
    
    /**
     * 알람 설정 조회 처리
     * @param req
     * @param res
     * @param alarmData
     * @return
     */
    @RequestMapping(value = "/getAlarmSetting", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> getAlarmSetting(
            HttpServletRequest req
            , HttpServletResponse res
            , @RequestBody Map<String, Object> alarmData
        ) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        
        try {
            logger.info("알람 설정 조회 요청: {}", alarmData);
            
            if(null != alarmData && 0 < alarmData.size()) {
                // 알람 설정 조회 서비스 호출
                Map<String, Object> alarmMap = adminService.getAlarmSetting(alarmData);
                
                if(alarmMap != null && alarmMap.size() > 0) {
                    resultMap.put("resultCode", "200");
                    resultMap.put("resultMessage", "알람 설정 조회 성공");
                    resultMap.put("data", alarmMap);
                    logger.info("알람 설정 조회 완료: {}", alarmMap);
                } else {
                    resultMap.put("resultCode", "404");
                    resultMap.put("resultMessage", "알람 설정을 찾을 수 없습니다.");
                    logger.info("알람 설정 조회 결과 없음");
                }
            } else {
                resultMap.put("resultCode", "400");
                resultMap.put("resultMessage", "알람 설정 조회 파라미터가 없습니다.");
            }
        } catch(Exception e) {
            logger.error("알람 설정 조회 실패: " + e.toString());
            resultMap.put("resultCode", "999");
            resultMap.put("resultMessage", "알람 설정 조회 실패 - " + e.toString());
        }
        
        return resultMap;
    }
    
    /**
     * 알람 설정 저장 처리
     * @param req
     * @param res
     * @param alarmData
     * @return
     */
    @RequestMapping(value = "/saveAlarmSetting", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> saveAlarmSetting(
            HttpServletRequest req
            , HttpServletResponse res
            , @RequestBody Map<String, Object> alarmData
        ) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        
        try {
            HttpSession session = req.getSession();
            
            // 세션 검증
            if(!sessionManagementService.isValidSession(session)) {
                return unifiedErrorHandler.createUnauthorizedResponse();
            }
            
            // 부계정 권한 확인 - 알람 설정 수정 불가
            // 조건: parentUserId가 null이 아니고 userId와 다르면 부계정
            String userId = (String) session.getAttribute("userId");
            String parentUserId = (String) session.getAttribute("parentUserId");
            boolean isSubAccount = (parentUserId != null && !parentUserId.isEmpty() && !parentUserId.equals(userId));
            
            if(isSubAccount) {
                resultMap.put("resultCode", "403");
                resultMap.put("resultMessage", "부계정 사용자는 알람 설정을 변경할 수 없습니다. (읽기 전용)");
                logger.warn("부계정 사용자 알람 설정 변경 시도 차단 - userId: {}, parentUserId: {}", userId, parentUserId);
                return resultMap;
            }
            
            logger.info("알람 설정 저장 요청: {}", alarmData);
            
            if(null != alarmData && 0 < alarmData.size()) {
                // 알람 설정 저장 서비스 호출
                adminService.saveAlarmSetting(alarmData);
                
                resultMap.put("resultCode", "200");
                resultMap.put("resultMessage", "알람 설정이 성공적으로 저장되었습니다.");
                logger.info("알람 설정 저장 완료");
            } else {
                resultMap.put("resultCode", "400");
                resultMap.put("resultMessage", "알람 설정 데이터가 없습니다.");
            }
        } catch(Exception e) {
            logger.error("알람 설정 저장 실패: " + e.toString());
            resultMap.put("resultCode", "999");
            resultMap.put("resultMessage", "알람 설정 저장 실패 - " + e.toString());
        }
        
        return resultMap;
    }
}
