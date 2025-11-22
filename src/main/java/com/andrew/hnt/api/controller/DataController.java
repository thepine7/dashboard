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
			
			// 세션이 유효하지 않은 경우 (기존 앱 호환성 - userId 기반 처리)
			if (!validationResult.isValid()) {
				logger.warn("세션 검증 실패 - 기존 앱 호환성 모드 (userId 기반 장치명 변경)");
				
				// deviceVO에서 userId 확인
				String requestUserId = deviceVO.getUserId();
				if (requestUserId == null || requestUserId.isEmpty()) {
					logger.error("세션도 없고 userId도 없음 - 장치 수정 차단");
					resultMap.put("resultCode", "401");
					resultMap.put("resultMessage", "세션이 만료되었거나 로그인이 필요합니다.");
					return resultMap;
				}
				
				// 기존 앱 호환성: userId가 있으면 장치명 변경 허용
				logger.info("기존 앱 호환성 모드 - userId 기반 장치명 변경: userId={}, sensorUuid={}", 
					requestUserId, deviceVO.getSensorUuid());
				
				dataService.updateSensorInfo(deviceVO);
				
				resultMap.put("resultCode", "200");
				resultMap.put("resultMessage", "장치명 변경 성공");
				logger.info("기존 앱 - 장치 수정 성공: userId={}, sensorUuid={}", requestUserId, deviceVO.getSensorUuid());
				
				return resultMap;
			}
			
			// 정상 세션 처리
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

		logger.info("=== deleteSensorInfo 호출됨 ===");
		logger.info("deviceVO: {}", deviceVO);
		if(deviceVO != null) {
			logger.info("deviceVO.getUserId(): {}", deviceVO.getUserId());
			logger.info("deviceVO.getSensorUuid(): {}", deviceVO.getSensorUuid());
			logger.info("deviceVO.getSensorName(): {}", deviceVO.getSensorName());
		}

		if(null != deviceVO) {
			try {
				// 통합 세션 검증 (모델 설정 없음)
				HttpSession session = req.getSession();
				SessionValidationResult validationResult = unifiedSessionService.validateSession(session, req, "U"); // 일반 사용자 이상 권한 필요
				
				logger.info("세션 검증 결과: {}", validationResult.isValid());
				
				// 세션이 유효하지 않은 경우 (기존 앱 호환성 - userId 기반 처리)
				if (!validationResult.isValid()) {
					logger.warn("세션 검증 실패 - 기존 앱 호환성 모드 (userId 기반 장치 삭제)");
					
					// deviceVO에서 userId 확인
					String requestUserId = deviceVO.getUserId();
					logger.info("requestUserId: {}", requestUserId);
					logger.info("requestUserId == null: {}", requestUserId == null);
					logger.info("requestUserId.isEmpty(): {}", requestUserId != null ? requestUserId.isEmpty() : "null이라 isEmpty 불가");
					
					if (requestUserId == null || requestUserId.isEmpty()) {
						logger.error("세션도 없고 userId도 없음 - 장치 삭제 차단");
						resultMap.put("resultCode", "401");
						resultMap.put("resultMessage", "세션이 만료되었거나 로그인이 필요합니다.");
						return resultMap;
					}
					
					// 기존 앱 호환성: userId가 있으면 장치 삭제 허용
					logger.info("기존 앱 호환성 모드 - userId 기반 장치 삭제: userId={}, sensorUuid={}", 
						requestUserId, deviceVO.getSensorUuid());
					
					logger.info("=== 장치 삭제 시작 (앱 모드) ===");
					logger.info("삭제 요청 - userId: {}, sensorUuid: {}, sensorName: {}", 
						deviceVO.getUserId(), deviceVO.getSensorUuid(), deviceVO.getSensorName());
					
					dataService.deleteSensorInfo(deviceVO);
					
					logger.info("dataService.deleteSensorInfo() 호출 완료");

					Map<String, Object> param = new HashMap<String, Object>();
					param.put("userId", deviceVO.getUserId());
					param.put("setGu", "deleteDevice");

					// DB 데이터 삭제 후 센서 자체 삭제 처리 추가 2023-02-24
					String payload = Constants.Mqtt.MESSAGE_TYPE_ACT + "&name=" + Constants.Mqtt.ACTION_USER_ID + "&value=0";
					String sendTopic = "HBEE/" + deviceVO.getUserId() + "/TC/" + deviceVO.getSensorUuid() + "/SER";

					param.put("topicStr", sendTopic);
					
					// 장치 삭제 알림 전송 (PC 대시보드 자동 갱신용) - 앱 모드
					sendDeviceDeletedNotification(deviceVO.getUserId(), deviceVO.getSensorUuid());

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
					logger.info("=== 장치 삭제 완료 (앱 모드) ===");
					logger.info("기존 앱 - 장치 삭제 성공: userId={}, sensorUuid={}", requestUserId, deviceVO.getSensorUuid());
					
					return resultMap;
				}
				
				// 정상 세션 처리
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
			
			logger.info("=== 장치 삭제 시작 ===");
			logger.info("삭제 요청 - userId: {}, sensorUuid: {}, sensorName: {}", 
				deviceVO.getUserId(), deviceVO.getSensorUuid(), deviceVO.getSensorName());
			
			dataService.deleteSensorInfo(deviceVO);
			
			logger.info("dataService.deleteSensorInfo() 호출 완료");

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
				
				// 장치 삭제 알림 전송 (PC 대시보드 자동 갱신용)
				sendDeviceDeletedNotification(deviceVO.getUserId(), deviceVO.getSensorUuid());

			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "장치 삭제 성공");
			logger.info("=== 장치 삭제 완료 ===");
			logger.info("장치 삭제 성공 - userId: {}, sensorUuid: {}, userGrade: {}", sessionUserId, deviceVO.getSensorUuid(), sessionUserGrade);
		} catch(Exception e) {
			logger.error("=== 장치 삭제 실패 ===");
			logger.error("에러 발생 - userId: {}, sensorUuid: {}", deviceVO.getUserId(), deviceVO.getSensorUuid());
			logger.error("에러 상세:", e);
			unifiedErrorHandler.logError("데이터 처리", e);
			resultMap.put("resultCode", "999");
			resultMap.put("resultMessage", "장치 삭제 실패: " + e.getMessage());
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

		logger.info("=== 엑셀 다운로드 메서드 호출됨 ===");
		HttpSession session = req.getSession();
		
		// URL 파라미터에서 값들 가져오기 (오타 방지)
		String userId = req.getParameter("userId");
		String sensorId = req.getParameter("sensorId");
		if (sensorId == null) {
			sensorId = req.getParameter("sensorld"); // 오타된 파라미터명도 처리
		}
		String sensorUuid = req.getParameter("sensorUuid");
		String setDate = req.getParameter("setDate");
		String startDate = req.getParameter("startDate");
		String endDate = req.getParameter("endDate");
		String sensorName = req.getParameter("sensorName");
		
		// 파라미터 로깅 추가
		logger.info("엑셀 다운로드 요청 파라미터 - userId: {}, sensorId: {}, sensorUuid: {}, sensorName: {}, startDate: {}, endDate: {}", 
			userId, sensorId, sensorUuid, sensorName, startDate, endDate);
		
		// 통합 세션 검증 (모델 설정 없음)
		SessionValidationResult validationResult = unifiedSessionService.validateSession(session, req, "B"); // 부계정 이상 권한 필요
		
		String sessionUserId;
		if (!validationResult.isValid()) {
			logger.warn("엑셀 다운로드 - 통합 세션 검증 실패, 파라미터에서 userId 사용");
			// 세션 검증 실패 시 파라미터에서 userId 사용
			sessionUserId = userId != null ? userId : "thepine"; // 기본값 설정
		} else {
			sessionUserId = validationResult.getUserId();
		}
		
		logger.info("=== 엑셀 다운로드 시작 ===");
		logger.info("요청 파라미터 - sensorId: {}, userId: {}, sensorUuid: {}, sensorName: {}", 
			sensorId, sessionUserId, sensorUuid, sensorName);
		logger.info("날짜 파라미터 - startDate: {}, endDate: {}", startDate, endDate);
		logger.info("전체 요청 파라미터 - req.getParameterMap(): {}", req.getParameterMap());

		// 파라미터 검증 강화
		if (sensorUuid == null || sensorUuid.trim().isEmpty()) {
			logger.warn("엑셀 다운로드 - 센서 UUID가 없음");
			res.sendError(HttpServletResponse.SC_BAD_REQUEST, "센서 정보가 필요합니다.");
			return;
		}
		
		if (startDate == null || startDate.trim().isEmpty() || endDate == null || endDate.trim().isEmpty()) {
			logger.warn("엑셀 다운로드 - 날짜 정보가 없음");
			res.sendError(HttpServletResponse.SC_BAD_REQUEST, "날짜 범위가 필요합니다.");
			return;
		}
		
		// sensorId가 없으면 세션의 userId 사용
		if (sensorId == null || sensorId.trim().isEmpty()) {
			sensorId = sessionUserId; // 세션의 userId 사용
			logger.info("엑셀 다운로드 - sensorId가 없어서 세션 userId 사용: {}", sensorId);
		}

		if(null != session) {
			if(null != sessionUserId && !"".equals(sessionUserId) && null != sensorId && !"".equals(sensorId) && null != sensorUuid && !"".equals(sensorUuid)) {
				
				// === 1. 날짜 파라미터 정리 ===
				String todayStr;
				String setDate1;
				String setDate2;
				
				if(null != setDate && !"".equals(setDate) && setDate.length() > 0) {
					// setDate 사용 (하위 호환성)
					todayStr = setDate;
					String dateWithoutHyphens = removeDateHyphens(setDate);
					setDate1 = dateWithoutHyphens + "000000";
					setDate2 = dateWithoutHyphens + "235959";
				} else {
					// startDate/endDate 사용 (현재 방식)
					Date today = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					todayStr = sdf.format(today);
					
					if(startDate != null && !startDate.isEmpty()) {
						String dateWithoutHyphens = removeDateHyphens(startDate);
						setDate1 = dateWithoutHyphens + "000000";
					} else {
						setDate1 = "";
					}
					
					if(endDate != null && !endDate.isEmpty()) {
						String dateWithoutHyphens = removeDateHyphens(endDate);
						setDate2 = dateWithoutHyphens + "235959";
					} else {
						setDate2 = "";
					}
				}
				
				logger.info("날짜 파라미터 처리 완료 - todayStr: {}, setDate1: {}, setDate2: {}", 
					todayStr, setDate1, setDate2);
				
				// === 2. 파일 응답 헤더 설정 ===
				String fileName = "report_" + todayStr + ".xlsx";
				String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8")
					.replaceAll("\\+", "%20");
				
				res.setHeader("Content-Disposition", 
					"attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName);
				res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
				res.setCharacterEncoding("UTF-8");
				res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
				res.setHeader("Pragma", "no-cache");
				res.setHeader("Expires", "0");
				res.setHeader("X-Content-Type-Options", "nosniff");
				res.setHeader("X-Download-Options", "noopen");
				
				// === 3. 데이터 조회용 파라미터 구성 ===
				Map<String, Object> userInfo = adminService.getUserInfo(sessionUserId, sensorUuid);
				String actualSensorId = sensorId;
				
				if(null != userInfo && userInfo.size() > 0) {
					@SuppressWarnings("unchecked")
					Map<String, Object> sensor = (Map<String, Object>) userInfo.get("sensorInfo");
					if(null != sensor && sensor.size() > 0) {
						// DB 컬럼명은 sensor_id (언더스코어)
						actualSensorId = String.valueOf(sensor.get("sensor_id"));
						logger.info("실제 장치 소유자 ID: {}", actualSensorId);
					}
				}
				
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("userId", actualSensorId != null ? actualSensorId : sessionUserId);
				param.put("sensorId", actualSensorId != null ? actualSensorId : sessionUserId);
				param.put("sensorUuid", sensorUuid);
				param.put("setDate", setDate);
				param.put("setDate1", setDate1);
				param.put("setDate2", setDate2);
				
				// === 4. SQL용 YYYY-MM-DD HH:MM:SS 형식 생성 ===
				String finalStartDate = startDate;
				String finalEndDate = endDate;
				
				// startDate가 없으면 setDate1에서 추출
				if(finalStartDate == null || finalStartDate.trim().isEmpty()) {
					if(setDate1 != null && setDate1.length() >= 8) {
						finalStartDate = convertDateFormat(setDate1.substring(0, 8));
					}
				}
				
				// endDate가 없으면 setDate2에서 추출
				if(finalEndDate == null || finalEndDate.trim().isEmpty()) {
					if(setDate2 != null && setDate2.length() >= 8) {
						finalEndDate = convertDateFormat(setDate2.substring(0, 8));
					}
				}
				
				param.put("startDateTime", finalStartDate + " 00:00:00");
				param.put("endDateTime", finalEndDate + " 23:59:59");
				
				logger.info("SQL 조회 파라미터 - startDateTime: {}, endDateTime: {}", 
					finalStartDate + " 00:00:00", finalEndDate + " 23:59:59");
				
				// === 5. 데이터 리스트 초기화 ===
				List<Map<String, Object>> dailyList = new ArrayList<Map<String, Object>>();
				List<Map<String, Object>> monthlyList = new ArrayList<Map<String, Object>>();
				List<Map<String, Object>> yearlyList = new ArrayList<Map<String, Object>>();
				
				List<String> monthly = new ArrayList<String>();
				List<String> yearly = new ArrayList<String>();
				
				List<String> header = new ArrayList<String>();
				List<String> header2 = new ArrayList<String>();
				List<String> header3 = new ArrayList<String>();

			try {
					// === 성능 최적화: 일간 데이터 조회 ===
					long dailyStartTime = System.currentTimeMillis();
					param.put("gu", "d");
					dailyList = dataService.selectDailyData(param); // 최적화된 쿼리 사용
					logger.info("일간 데이터 조회 완료 - 데이터 수: {}, sensorUuid: {}, 소요시간: {}ms", 
						dailyList != null ? dailyList.size() : 0, sensorUuid, System.currentTimeMillis() - dailyStartTime);

					// 데이터 검증 및 샘플 로깅
					if(dailyList != null && !dailyList.isEmpty()) {
						logger.info("일간 데이터 샘플 - 첫 번째 데이터: {}", dailyList.get(0));
						
						// 데이터 유효성 검증
						boolean hasValidData = false;
						for (Map<String, Object> item : dailyList) {
							if (item != null && item.get("inst_dtm") != null && item.get("sensor_value") != null) {
								hasValidData = true;
								break;
							}
						}
						
						if (!hasValidData) {
							logger.warn("엑셀 다운로드 - 유효한 데이터가 없음");
							res.sendError(HttpServletResponse.SC_NOT_FOUND, "선택한 날짜 범위에 데이터가 없습니다.");
							return;
						}
					} else {
						logger.warn("엑셀 다운로드 - 데이터가 없음");
						res.sendError(HttpServletResponse.SC_NOT_FOUND, "선택한 날짜 범위에 데이터가 없습니다.");
						return;
					}

					// === 성능 최적화: 1분 단위 헤더 생성 ===
					for (int hour = 0; hour < 24; hour++) {
						for (int minute = 0; minute < 60; minute += 1) { // 1분 단위
							header.add(String.format("%02d:%02d", hour, minute));
						}
					}

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
					
					if (stream != null) {
						try {
							IOUtils.copy(stream, res.getOutputStream());
							res.getOutputStream().flush();
							logger.info("엑셀 파일 생성 및 전송 완료");
						} finally {
							stream.close();
						}
					} else {
						logger.error("엑셀 파일 스트림이 null입니다");
						res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "엑셀 파일 생성 실패");
						return;
					}
				} catch (Exception e) {
					logger.error("엑셀 다운로드 실패: {}", e.getMessage(), e);
					try {
						res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "엑셀 파일 생성 중 오류가 발생했습니다: " + e.getMessage());
					} catch (IOException ioException) {
						logger.error("에러 응답 전송 실패: {}", ioException.getMessage(), ioException);
					}
					return;
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
			
			// 최적화된 일간 데이터 조회 (1분 단위 그룹화)
			List<Map<String, Object>> dailyList = null;
			List<String> daily = new ArrayList<String>();
			
			try {
				// 새로운 최적화된 쿼리 사용 (1분 단위)
				dailyList = dataService.selectDailyData(param);
				logger.info("최적화된 일간 데이터 조회 사용 - 1분 단위 그룹화");
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
	
	/**
	 * YYYYMMDD 형식을 YYYY-MM-DD 형식으로 변환
	 * @param dateStr 변환할 날짜 문자열
	 * @return YYYY-MM-DD 형식의 날짜 문자열
	 */
	private String convertDateFormat(String dateStr) {
		if (dateStr == null || dateStr.isEmpty()) {
			return null;
		}
		
		// 이미 YYYY-MM-DD 형식이면 그대로 반환
		if (dateStr.contains("-")) {
			return dateStr;
		}
		
		// YYYYMMDD -> YYYY-MM-DD 변환
		if (dateStr.length() >= 8) {
			return dateStr.substring(0, 4) + "-" + 
				   dateStr.substring(4, 6) + "-" + 
				   dateStr.substring(6, 8);
		}
		
		return dateStr;
	}
	
	/**
	 * YYYY-MM-DD 형식을 YYYYMMDD 형식으로 변환
	 * @param dateStr 변환할 날짜 문자열
	 * @return YYYYMMDD 형식의 날짜 문자열
	 */
	private String removeDateHyphens(String dateStr) {
		if (dateStr == null || dateStr.isEmpty()) {
			return null;
		}
		return dateStr.replace("-", "");
	}
	
	/**
	 * 장치 삭제 알림 전송 (PC 대시보드 자동 갱신용)
	 * @param userId 사용자 ID
	 * @param sensorUuid 장치 UUID
	 */
	private void sendDeviceDeletedNotification(String userId, String sensorUuid) {
		try {
			String notificationTopic = String.format("HBEE/%s/DEVICE_DELETED", userId);
			String payload = String.format(
				"{\"actcode\":\"device_deleted\",\"uuid\":\"%s\",\"timestamp\":%d}",
				sensorUuid, System.currentTimeMillis()
			);
			
			logger.info("=== 장치 삭제 알림 전송 시작 ===");
			logger.info("Topic: {}", notificationTopic);
			logger.info("Payload: {}", payload);
			
			// MQTT 클라이언트 생성 (별도 스레드에서 실행)
			new Thread(() -> {
				com.andrew.hnt.api.mqtt.common.MQTT client = null;
				try {
					client = new com.andrew.hnt.api.mqtt.common.MQTT(
						"tcp://iot.hntsolution.co.kr:1883",
						"notification_delete_" + System.currentTimeMillis(),
						"hnt1",
						"abcde"
					);
					
					logger.info("MQTT 클라이언트 init() 호출...");
					client.init(notificationTopic, "N"); // 구독 불필요
					
					// 연결 대기 (최대 3초)
					int retryCount = 0;
					while (!client.isConnected() && retryCount < 30) {
						Thread.sleep(100);
						retryCount++;
					}
					
					if (client.isConnected()) {
						logger.info("MQTT 연결 성공 - 메시지 전송 시작");
						client.publish(payload, 0, notificationTopic);
						logger.info("=== 장치 삭제 알림 전송 완료 ===");
						logger.info("Topic: {}", notificationTopic);
						logger.info("Payload: {}", payload);
					} else {
						logger.error("=== 장치 삭제 알림 전송 실패 - MQTT 연결 타임아웃 ===");
					}
					
				} catch (Exception e) {
					logger.error("=== 장치 삭제 알림 전송 중 예외 발생 ===", e);
				} finally {
					// 연결 해제
					if (client != null) {
						try {
							client.disconnect();
							logger.debug("MQTT 클라이언트 연결 해제 완료");
						} catch (Exception e) {
							// Paho 라이브러리 내부 NullPointerException 무시 (정상 동작)
							logger.debug("MQTT 클라이언트 연결 해제 중 예외 (무시 가능): {}", e.getMessage());
						}
					}
				}
			}).start();
			
		} catch (Exception e) {
			logger.error("장치 삭제 알림 전송 실패", e);
		}
	}
	
}
