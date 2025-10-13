package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.common.Constants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.andrew.hnt.api.model.DeviceVO;
import com.andrew.hnt.api.mqtt.common.MQTT;
import com.andrew.hnt.api.service.AdminService;
import com.andrew.hnt.api.service.UnifiedSessionService;
import com.andrew.hnt.api.service.UnifiedSessionService.SessionValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.andrew.hnt.api.util.ExcelUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.andrew.hnt.api.model.LoginVO;
import com.andrew.hnt.api.service.DataService;
import com.andrew.hnt.api.util.ResponseUtil;
import com.andrew.hnt.api.util.UnifiedErrorHandler;
import com.andrew.hnt.api.util.ValidationUtil;
import com.andrew.hnt.api.service.SubAccountPermissionService;

@Controller
@RequestMapping("/data")
public class DataController {
	
	@Autowired
	private DataService dataService;

	@Autowired
	private AdminService adminService;
	
	@Autowired
	private UnifiedSessionService unifiedSessionService;
	
	@Autowired
	private UnifiedErrorHandler unifiedErrorHandler;
	
	@Autowired
	private SubAccountPermissionService subAccountPermissionService;



	private static final Logger logger = LoggerFactory.getLogger(DataController.class);

	@RequestMapping(value = "/getSensorList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> getSensorList(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody LoginVO loginVO
			) {
		List<Map<String, Object>> deviceList = new ArrayList<Map<String, Object>>();
		
		try {
			if(null != loginVO) {
				String userId = loginVO.getUserId();

				deviceList = dataService.getDeviceList(userId);
				
				Map<String, Object> resultMap = ResponseUtil.success("장치 목록 조회 성공");
				if(null != deviceList && 0 < deviceList.size()) {
					resultMap.put("deviceList", deviceList);
				} else {
					resultMap.put("deviceList", null);
				}
				return resultMap;
			} else {
				return ResponseUtil.error(ResponseUtil.BAD_REQUEST_CODE, "잘못된 요청입니다.");
			}
		} catch(Exception e) {
			unifiedErrorHandler.logError("장치 목록 조회", e);
			return ResponseUtil.error(ResponseUtil.INTERNAL_ERROR_CODE, "장치 목록 조회 실패");
		}
	}

	@RequestMapping(value = "/updateSensorInfo", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> updateSensorInfo(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody DeviceVO deviceVO
			) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		// 공통 검증 유틸리티 사용
		ValidationUtil.ValidationResult deviceValidation = ValidationUtil.validateNotNull(deviceVO, "장치 정보");
		if (!deviceValidation.isValid()) {
			resultMap.put("resultCode", "400");
			resultMap.put("resultMessage", deviceValidation.getErrorMessage());
			return resultMap;
		}

		// 중복 검증 제거됨 - 위에서 이미 검증 완료
		try {
			// 통합 세션 검증 (모델 설정 없음)
			HttpSession session = req.getSession();
			SessionValidationResult validationResult = unifiedSessionService.validateSession(session, req, "U"); // 일반 사용자 이상 권한 필요
			
			if (!validationResult.isValid()) {
				logger.warn("통합 세션 검증 실패 - 장치 수정 시도 차단");
				resultMap.put("resultCode", "401");
				resultMap.put("resultMessage", "세션이 만료되었거나 로그인이 필요합니다.");
				return resultMap;
			}
			
			String sessionUserId = validationResult.getUserId();
			String sessionUserGrade = validationResult.getUserGrade();
			
			logger.info("통합 세션 검증 성공 - 장치 수정 권한 확인: userId={}, userGrade={}", sessionUserId, sessionUserGrade);

			// 부계정 권한 제한 강화 체크
			SubAccountPermissionService.SubAccountPermissionResult permissionResult = 
				subAccountPermissionService.validateSubAccountPermission(session, "UPDATE_SENSOR");
			
			if (!permissionResult.isAllowed()) {
				logger.warn(subAccountPermissionService.createPermissionDeniedLog(session, "UPDATE_SENSOR", permissionResult.getReason()));
				resultMap.put("resultCode", "403");
				resultMap.put("resultMessage", permissionResult.getReason());
				resultMap.put("operation", permissionResult.getOperation());
				resultMap.put("userGrade", permissionResult.getUserGrade());
				return resultMap;
			}

			dataService.updateSensorInfo(deviceVO);

			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "장치명 변경 성공");
			logger.info("장치 수정 성공 - userId: {}, sensorUuid: {}, userGrade: {}", sessionUserId, deviceVO.getSensorUuid(), sessionUserGrade);
		} catch(Exception e) {
			unifiedErrorHandler.logError("장치명 변경", e);
			resultMap.put("resultCode", "999");
			resultMap.put("resultMessage", "장치명 변경 실패");
		}

		return resultMap;
	}

	@RequestMapping(value = "/deleteSensorInfo", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> deleteSensorInfo(
			HttpServletRequest req
			, HttpServletResponse res
			, @RequestBody DeviceVO deviceVO
		) {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != deviceVO) {
			try {
				// 통합 세션 검증 (모델 설정 없음)
				HttpSession session = req.getSession();
				SessionValidationResult validationResult = unifiedSessionService.validateSession(session, req, "U"); // 일반 사용자 이상 권한 필요
				
				if (!validationResult.isValid()) {
					logger.warn("통합 세션 검증 실패 - 장치 삭제 시도 차단");
					resultMap.put("resultCode", "401");
					resultMap.put("resultMessage", "세션이 만료되었거나 로그인이 필요합니다.");
					return resultMap;
				}
				
				String sessionUserId = validationResult.getUserId();
				String sessionUserGrade = validationResult.getUserGrade();
				String sessionUserNm = validationResult.getUserNm();
				
				logger.info("통합 세션 검증 성공 - 장치 삭제 권한 확인: userId={}, userGrade={}, userNm={}", 
					sessionUserId, sessionUserGrade, sessionUserNm);

				// 부계정 권한 제한 강화 체크
				SubAccountPermissionService.SubAccountPermissionResult permissionResult = 
					subAccountPermissionService.validateSubAccountPermission(session, "DELETE_SENSOR");
				
				if (!permissionResult.isAllowed()) {
					logger.warn(subAccountPermissionService.createPermissionDeniedLog(session, "DELETE_SENSOR", permissionResult.getReason()));
					resultMap.put("resultCode", "403");
					resultMap.put("resultMessage", permissionResult.getReason());
					resultMap.put("operation", permissionResult.getOperation());
					resultMap.put("userGrade", permissionResult.getUserGrade());
					return resultMap;
				}
				
				dataService.deleteSensorInfo(deviceVO);

				Map<String, Object> param = new HashMap<String, Object>();
				param.put("userId", deviceVO.getUserId());
				param.put("setGu", "deleteDevice");

				// DB 데이터 삭제 후 센서 자체 삭제 처리 추가 2023-02-24
				String payload = Constants.Mqtt.MESSAGE_TYPE_ACT + "&name=" + Constants.Mqtt.ACTION_USER_ID + "&value=0";
				String sendTopic = "HBEE/" + deviceVO.getUserId() + "/TC/" + deviceVO.getSensorUuid() + "/SER";

				param.put("topicStr", sendTopic);

                String MqttServer1 = Constants.Mqtt.SERVER;
				String clientId = "";
				String userName = Constants.Mqtt.USERNAME;
				String password = Constants.Mqtt.PASSWORD;
                String topic = Constants.Mqtt.TOPIC_WILDCARD;

				clientId = UUID.randomUUID().toString();

				MQTT client = new MQTT(MqttServer1, clientId, userName, password);

                if(null != sendTopic && !"".equals(sendTopic) && 0 < sendTopic.length()) {
                    // 응답은 DEV 토픽으로 수신하도록 구독 토픽 설정
                    if (sendTopic.endsWith("/SER")) {
                        topic = sendTopic.substring(0, sendTopic.length() - 3) + "DEV";
                    } else {
                        topic = sendTopic;
                    }
                }
                client.init(topic, "Y"); // DEV 구독
                client.publish(payload, 0, sendTopic); // SER 발행

				resultMap.put("resultCode", "200");
				resultMap.put("resultMessage", "장치 삭제 성공");
				logger.info("장치 삭제 성공 - userId: {}, sensorUuid: {}, userGrade: {}", sessionUserId, deviceVO.getSensorUuid(), sessionUserGrade);
			} catch(Exception e) {
				unifiedErrorHandler.logError("데이터 처리", e);
				resultMap.put("resultCode", "999");
				resultMap.put("resultMessage", "장치 삭제 실패");
			}
		}

		return resultMap;
	}

	// insertSensorInfo 메서드는 MainController로 이동하여 중복 제거
	// 장치 등록은 /main/insertSensorInfo 엔드포인트를 사용하세요
	// 중복 제거 완료: 2025-01-27

	/**
	 * 상세 센서 데이터를 보여주는 화면
	 * ChartController로 이동됨 - /chart/chart 사용
	 */




	@RequestMapping(value = "/excelDownload", method = {RequestMethod.GET, RequestMethod.POST})
	public void excelDownload (
			HttpServletRequest req
			, HttpServletResponse res
		) throws IOException {

		HttpSession session = req.getSession();
		
		// URL 파라미터에서 값들 가져오기
		String sensorId = req.getParameter("sensorId");
		String sensorUuid = req.getParameter("sensorUuid");
		String setDate = req.getParameter("setDate");
		String startDate = req.getParameter("startDate");
		String endDate = req.getParameter("endDate");
		String sensorName = req.getParameter("sensorName");
		
		// 통합 세션 검증 (모델 설정 없음)
		SessionValidationResult validationResult = unifiedSessionService.validateSession(session, req, "B"); // 부계정 이상 권한 필요
		
		if (!validationResult.isValid()) {
			logger.warn("엑셀 다운로드 - 통합 세션 검증 실패");
			res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "세션이 만료되었거나 로그인이 필요합니다.");
			return;
		}
		
		String sessionUserId = validationResult.getUserId();
		
		logger.info("엑셀 다운로드 시작 - sensorId: {}, userId: {}, sensorUuid: {}, sensorName: {}", 
			sensorId, sessionUserId, sensorUuid, sensorName);

		if(null != session) {

			if(null != sessionUserId && !"".equals(sessionUserId) && null != sensorId && !"".equals(sensorId) && null != sensorUuid && !"".equals(sensorUuid)) {
				String todayStr = "";
				String setDate1 = "";
				String setDate2 = "";

				if(null != setDate && !"".equals(setDate) && 0 < setDate.length()) {
					todayStr = setDate;
					if(setDate.contains("-")) {
						setDate = setDate.replace("-", "");
					}
					setDate1 = setDate + "000000";
					setDate2 = setDate + "235959";
				} else {
					Date today = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					todayStr = sdf.format(today);
				}

				if(null != startDate && !"".equals(startDate) && 0 < startDate.length()) {
					if(startDate.contains("-")) {
						startDate = startDate.replace("-", "");
					}
					setDate1 = startDate + "000000";
				}

				if(null != endDate && !"".equals(endDate) && 0 < endDate.length()) {
					if(endDate.contains("-")) {
						endDate = endDate.replace("-", "");
					}
					setDate2 = endDate + "235959";
				}

				logger.info("엑셀 다운로드 파라미터 - todayStr: {}, setDate1: {}, setDate2: {}", 
					todayStr, setDate1, setDate2);

				res.setHeader("Content-Disposition", "attachment;filename=report_"+todayStr+".xls");
				res.setContentType("application/vnd.ms-excel");

				List<Map<String, Object>> dailyList = new ArrayList<Map<String, Object>>();
				List<Map<String, Object>> monthlyList = new ArrayList<Map<String, Object>>();
				List<Map<String, Object>> yearlyList = new ArrayList<Map<String, Object>>();

				List<String> monthly = new ArrayList<String>();
				List<String> yearly = new ArrayList<String>();

				List<String> header = new ArrayList<String>();
				List<String> header2 = new ArrayList<String>();
				List<String> header3 = new ArrayList<String>();

				// === 실제 장치 소유자 ID 조회 (차트 페이지와 동일한 로직) ===
				String actualSensorId = sensorId;  // 기본값
				Map<String, Object> userInfo = new HashMap<String, Object>();
				userInfo = adminService.getUserInfo(sessionUserId, sensorUuid);

				if(null != userInfo && 0 < userInfo.size()) {
					@SuppressWarnings("unchecked")
					Map<String, Object> sensor = (Map<String, Object>) userInfo.get("sensorInfo");
					if (null != sensor && 0 < sensor.size()) {
						// 실제 장치 소유자 ID로 sensorId 설정
						actualSensorId = String.valueOf(sensor.get("sensorId"));
						logger.info("엑셀 다운로드 - URL 파라미터 sensorId: {}", sensorId);
						logger.info("엑셀 다운로드 - 실제 장치 소유자 sensorId: {}", actualSensorId);
					}
				}

				Map<String, Object> param = new HashMap<String, Object>();
				// 부계정이 메인 계정의 장치 데이터를 조회할 때는 user_id를 sensor_id와 동일하게 설정
				param.put("userId", actualSensorId);  // 실제 장치 소유자 ID 사용
				param.put("sensorId", actualSensorId);
				param.put("sensorUuid", sensorUuid);
				param.put("setDate", setDate);
				param.put("setDate1", setDate1);
				param.put("setDate2", setDate2);

				logger.info("데이터 조회 파라미터 - param: {}", param);

				try {
					// 일간 데이터 조회
					param.put("gu", "d");
					dailyList = dataService.selectSensorData(param);
					logger.info("일간 데이터 조회 완료 - 데이터 수: {}, sensorUuid: {}", 
						dailyList != null ? dailyList.size() : 0, sensorUuid);

					// 데이터 샘플 로깅
					if(dailyList != null && !dailyList.isEmpty()) {
						logger.info("일간 데이터 샘플 - 첫 번째 데이터: {}", dailyList.get(0));
					}

					header.add("00:00");
					header.add("00:30");
					header.add("01:00");
					header.add("01:30");
					header.add("02:00");
					header.add("02:30");
					header.add("03:00");
					header.add("03:30");
					header.add("04:00");
					header.add("04:30");
					header.add("05:00");
					header.add("05:30");
					header.add("06:00");
					header.add("06:30");
					header.add("07:00");
					header.add("07:30");
					header.add("08:00");
					header.add("08:30");
					header.add("09:00");
					header.add("09:30");
					header.add("10:00");
					header.add("10:30");
					header.add("11:00");
					header.add("11:30");
					header.add("12:00");
					header.add("12:30");
					header.add("13:00");
					header.add("13:30");
					header.add("14:00");
					header.add("14:30");
					header.add("15:00");
					header.add("15:30");
					header.add("16:00");
					header.add("16:30");
					header.add("17:00");
					header.add("17:30");
					header.add("18:00");
					header.add("18:30");
					header.add("19:00");
					header.add("19:30");
					header.add("20:00");
					header.add("20:30");
					header.add("21:00");
					header.add("21:30");
					header.add("22:00");
					header.add("22:30");
					header.add("23:00");
					header.add("23:30");

					// 월간 데이터 조회
					param.remove("gu");
					param.put("gu", "w");
					monthlyList = dataService.selectSensorData(param);
					logger.info("월간 데이터 조회 완료 - 데이터 수: {}", monthlyList != null ? monthlyList.size() : 0);

					if(null != monthlyList && 0 < monthlyList.size()) {
						for(int i =0; i < monthlyList.size(); i++) {
							monthly.add(String.valueOf(monthlyList.get(i).get("sensor_value")));
							header2.add(String.valueOf(monthlyList.get(i).get("inst_dtm")));
						}
					}

					// 연간 데이터 조회
					param.remove("gu");
					param.put("gu", "y");
					yearlyList = dataService.selectSensorData(param);
					logger.info("연간 데이터 조회 완료 - 데이터 수: {}", yearlyList != null ? yearlyList.size() : 0);

					if(null != yearlyList && 0 < yearlyList.size()) {
						for(int i =0; i < yearlyList.size(); i++) {
							yearly.add(String.valueOf(yearlyList.get(i).get("sensor_value")));
							header3.add(String.valueOf(yearlyList.get(i).get("inst_dtm")));
						}
					}

					logger.info("엑셀 파일 생성 시작 - 일간: {}, 월간: {}, 연간: {}", 
						dailyList != null ? dailyList.size() : 0,
						monthlyList != null ? monthlyList.size() : 0,
						yearlyList != null ? yearlyList.size() : 0);

					ByteArrayInputStream stream = ExcelUtils.createDataExcel(header, header2, header3, dailyList, monthlyList, yearlyList, sensorName);
					IOUtils.copy(stream, res.getOutputStream());
					
					logger.info("엑셀 파일 생성 완료");
				} catch (Exception e) {
					logger.error("엑셀 다운로드 실패: {}", e.getMessage(), e);
				}
			}
		}

	}
	
	@RequestMapping(value = "/getDailyData", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> getDailyData(
			HttpServletRequest req
			, HttpServletResponse res
	) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		try {
			// URL 파라미터에서 값들 가져오기
			String sensorId = req.getParameter("sensorId");
			String sensorUuid = req.getParameter("sensorUuid");
			String startDate = req.getParameter("startDate");
			String endDate = req.getParameter("endDate");
			// 통합 세션 검증 (모델 설정 없음)
			HttpSession session = req.getSession();
			SessionValidationResult validationResult = unifiedSessionService.validateSession(session, req, "B"); // 부계정 이상 권한 필요
			
			if (!validationResult.isValid()) {
				logger.warn("일간 데이터 조회 - 통합 세션 검증 실패");
				resultMap.put("resultCode", "401");
				resultMap.put("resultMessage", "세션이 만료되었거나 로그인이 필요합니다.");
				return resultMap;
			}
			
			String sessionUserId = validationResult.getUserId();
			
			logger.info("DataController.getDailyData - 요청 파라미터: sensorId={}, userId={}, sensorUuid={}, startDate={}, endDate={}", 
				sensorId, sessionUserId, sensorUuid, startDate, endDate);
			
			Map<String, Object> param = new HashMap<String, Object>();
			// 온도데이터는 sensor_uuid에만 종속됨
			param.put("sensorUuid", sensorUuid);
			
			// 날짜 파라미터 추가 (endDate만 사용)
			if(endDate != null && !endDate.isEmpty()) {
				param.put("startDateTime", endDate + " 00:00:00");
				param.put("endDateTime", endDate + " 23:59:59");
				logger.info("DataController.getDailyData - 날짜 범위: {} ~ {}", endDate + " 00:00:00", endDate + " 23:59:59");
			}
			
			// 최적화된 일간 데이터 조회 (30분 단위 그룹화)
			List<Map<String, Object>> dailyList = null;
			List<String> daily = new ArrayList<String>();
			
			try {
				// 새로운 최적화된 쿼리 사용
				dailyList = dataService.selectDailyData(param);
				logger.info("최적화된 일간 데이터 조회 사용 - 30분 단위 그룹화");
			} catch(Exception queryException) {
				logger.warn("최적화된 쿼리 실패, 기존 쿼리로 대체: " + queryException.getMessage());
				try {
					// 기존 쿼리로 대체
					param.put("gu", "d");
					dailyList = dataService.selectSensorData(param);
					logger.info("기존 쿼리로 대체 실행");
				} catch(Exception fallbackException) {
					logger.warn("기존 쿼리도 실패, 빈 데이터로 처리: " + fallbackException.getMessage());
				}
			}
			
			logger.info("DataController.getDailyData - DB 조회 결과: dailyList.size()={}", 
				dailyList != null ? dailyList.size() : 0);
			
			if(null != dailyList && 0 < dailyList.size()) {
				for(int i=0; i < dailyList.size(); i++) {
					Map<String, Object> item = dailyList.get(i);
					if(item != null && item.get("inst_dtm") != null && item.get("sensor_value") != null) {
						String dataStr = item.get("inst_dtm") + "^" + item.get("sensor_value");
						daily.add(dataStr);
						logger.debug("DataController.getDailyData - 데이터[{}]: {}", i, dataStr);
					}
				}
			}
			
			// 결과 데이터를 쉼표로 구분된 문자열로 변환
			String dailyData = String.join(",", daily);
			
			logger.info("DataController.getDailyData - 최종 응답: dailyData 길이={}, 내용={}", 
				dailyData.length(), dailyData);
			
			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "데이터 조회 성공");
			resultMap.put("dailyData", dailyData);
			
		} catch(Exception e) {
			unifiedErrorHandler.logError("데이터 처리", e);
			resultMap.put("resultCode", "500");
			resultMap.put("resultMessage", "데이터 조회 실패: " + e.getMessage());
		}
		
		return resultMap;
	}
	
	/**
	 * 센서 설정값 조회 (차트 페이지용)
	 */
	@PostMapping("/getSensorSettings")
	@ResponseBody
	public Map<String, Object> getSensorSettings(@RequestParam Map<String, Object> param, HttpSession session, HttpServletRequest req) {
		Map<String, Object> resultMap = new HashMap<>();
		
		try {
			// 세션 검증
			SessionValidationResult sessionResult = unifiedSessionService.validateSession(session, req, "B");
			if (!sessionResult.isValid()) {
				resultMap.put("resultCode", "401");
				resultMap.put("resultMessage", "세션이 만료되었습니다. 다시 로그인해주세요.");
				return resultMap;
			}
			
			String sensorUuid = (String) param.get("sensorUuid");
			if (sensorUuid == null || sensorUuid.trim().isEmpty()) {
				resultMap.put("resultCode", "400");
				resultMap.put("resultMessage", "센서 UUID가 필요합니다.");
				return resultMap;
			}
			
			logger.info("센서 설정값 조회 요청 - sensorUuid: {}", sensorUuid);
			
			// 센서 설정값 조회
			Map<String, Object> settings = adminService.getSensorSettings(sensorUuid);
			
			if (settings != null && !settings.isEmpty()) {
				logger.info("센서 설정값 조회 성공 - sensorUuid: {}, 설정값: {}", sensorUuid, settings);
				resultMap.put("resultCode", "200");
				resultMap.put("resultMessage", "설정값 조회 성공");
				resultMap.put("settings", settings);
			} else {
				logger.warn("센서 설정값이 없음 - sensorUuid: {}", sensorUuid);
				resultMap.put("resultCode", "404");
				resultMap.put("resultMessage", "설정값을 찾을 수 없습니다.");
			}
			
		} catch (Exception e) {
			unifiedErrorHandler.logError("센서 설정값 조회", e);
			resultMap.put("resultCode", "500");
			resultMap.put("resultMessage", "설정값 조회 실패: " + e.getMessage());
		}
		
		return resultMap;
	}
	
}
