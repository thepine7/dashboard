package com.andrew.hnt.api.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.andrew.hnt.api.model.LoginVO;
import com.andrew.hnt.api.model.SensorVO;
import com.andrew.hnt.api.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andrew.hnt.api.mapper.LoginMapper;
import com.andrew.hnt.api.mapper.MqttMapper;
import com.andrew.hnt.api.service.LoginService;
import com.andrew.hnt.api.util.StringUtil;
import com.andrew.hnt.api.util.AES256Util;
import com.andrew.hnt.api.common.BaseService;

@Service
public class LoginServiceImpl extends BaseService implements LoginService {
	
	@Autowired
	private LoginMapper loginMapper;
	
		@Autowired
	private MqttMapper mqttMapper;

	

	private AES256Util aes256;

	private SensorVO sensorVO2;
	
	private static final Logger logger = LoggerFactory.getLogger(LoginServiceImpl.class);
	
	// 비동기 처리를 위한 Executor (5개 스레드)
	private final java.util.concurrent.Executor asyncExecutor = java.util.concurrent.Executors.newFixedThreadPool(5);
	
	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	public Map<String, Object> getUserList() throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		List<UserInfo> userList = new ArrayList<UserInfo>();

		try {
			userList = loginMapper.getUserList();;

		if(null != userList && 0 < userList.size()) {
			for(int i=0; i < userList.size(); i++) {
				// 중복 로직 제거: 최적화된 쿼리(getUserListWithActivityStatus)를 사용하세요
				// 이 메서드는 레거시 호환성을 위해 유지되지만, loginYn은 쿼리에서 계산됩니다

				String user_id = String.valueOf(userList.get(i).getUserId());
					Map<String, Object> param = new HashMap<String, Object>();
					param.put("userId", user_id);

					Map<String, Object> infoMap = new HashMap<String, Object>();

					try {
						infoMap = loginMapper.getMainId(param);

						if(null != infoMap && 0 < infoMap.size()) {
							userList.get(i).setMainId(String.valueOf(infoMap.get("mainId")));
						}
					} catch(Exception e) {
						logger.error("Error : " + e.toString());
					}
				}

				resultMap.put("result", "success");
				resultMap.put("resultMessage", "사용자 목록 조회 성공했습니다.");
				resultMap.put("userList", userList);
			} else {
				resultMap.put("result", "fail");
				resultMap.put("resultMessage", "사용자 목록 조회 실패했습니다.");
			}
		} catch(Exception e) {
			logger.error("Error : " + e.toString());
			resultMap.put("result", "fail");
			resultMap.put("resultMessage", "사용자 목록 조회 과정에서 오류가 발생되었습니다. - " + e.toString());
		}

		return resultMap;
	}

	@Override
	public Map<String, Object> getSubUserList(String userId) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		List<UserInfo> userList = new ArrayList<UserInfo>();

		try {
			// U(일반사용자)가 생성한 B계정만 조회
			userList = loginMapper.getSubUserList(userId);

			if(null != userList && 0 < userList.size()) {
				for(int i=0; i < userList.size(); i++) {
					String loginDtm = "";
					String logoutDtm = "";

					loginDtm = String.valueOf(userList.get(i).getLoginDtm());
					logoutDtm = String.valueOf(userList.get(i).getLogoutDtm());

					// 새로운 활동 상태 확인 로직 적용
					try {
						if(null != loginDtm && !"".equals(loginDtm) && "Y".equals(loginDtm)) {
							if(null != logoutDtm && !"".equals(logoutDtm) && "A".equals(logoutDtm)) {
								// 활동 상태가 "Y"이고 포커스 상태가 "A"이면 활성
								userList.get(i).setLoginYn("활성");
							} else {
								userList.get(i).setLoginYn("비활성");
							}
						} else {
							userList.get(i).setLoginYn("비활성");
						}
					} catch(Exception e) {
						logger.error("활동 상태 확인 실패 - userId: {}, error: {}", userList.get(i).getUserId(), e.getMessage());
						userList.get(i).setLoginYn("비활성");
					}

					String user_id = String.valueOf(userList.get(i).getUserId());
					Map<String, Object> param = new HashMap<String, Object>();
					param.put("userId", user_id);

					Map<String, Object> infoMap = new HashMap<String, Object>();

					try {
						infoMap = loginMapper.getMainId(param);

						if(null != infoMap && 0 < infoMap.size()) {
							userList.get(i).setMainId(String.valueOf(infoMap.get("mainId")));
						}
					} catch(Exception e) {
						logger.error("Error : " + e.toString());
					}
				}

				resultMap.put("result", "success");
				resultMap.put("resultMessage", "부계정 목록 조회 성공했습니다.");
				resultMap.put("userList", userList);
			} else {
				resultMap.put("result", "fail");
				resultMap.put("resultMessage", "부계정 목록 조회 실패했습니다.");
			}
		} catch(Exception e) {
			logger.error("Error : " + e.toString());
			resultMap.put("result", "fail");
			resultMap.put("resultMessage", "부계정 목록 조회 과정에서 오류가 발생되었습니다. - " + e.toString());
		}

		return resultMap;
	}

	@Override
	public Map<String, Object> getUserAndSubUserList(String userId) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		List<UserInfo> userList = new ArrayList<UserInfo>();

		try {
		// U(일반사용자) 자신과 자신이 생성한 B계정 조회
		userList = loginMapper.getUserAndSubUserList(userId);

		if(null != userList && 0 < userList.size()) {
			for(int i=0; i < userList.size(); i++) {
				// 중복 로직 제거: 최적화된 쿼리(getUserAndSubUserListWithActivityStatus)를 사용하세요
				// 이 메서드는 레거시 호환성을 위해 유지되지만, loginYn은 쿼리에서 계산됩니다

				String user_id = String.valueOf(userList.get(i).getUserId());
					Map<String, Object> param = new HashMap<String, Object>();
					param.put("userId", user_id);

					Map<String, Object> infoMap = new HashMap<String, Object>();

					try {
						infoMap = loginMapper.getMainId(param);

						if(null != infoMap && 0 < infoMap.size()) {
							userList.get(i).setMainId(String.valueOf(infoMap.get("mainId")));
						}
					} catch(Exception e) {
						logger.error("Error : " + e.toString());
					}
				}

				resultMap.put("result", "success");
				resultMap.put("resultMessage", "사용자 및 부계정 목록 조회 성공했습니다.");
				resultMap.put("userList", userList);
			} else {
				resultMap.put("result", "fail");
				resultMap.put("resultMessage", "사용자 및 부계정 목록 조회 실패했습니다.");
			}
		} catch(Exception e) {
			logger.error("Error : " + e.toString());
			resultMap.put("result", "fail");
			resultMap.put("resultMessage", "사용자 및 부계정 목록 조회 과정에서 오류가 발생되었습니다. - " + e.toString());
		}

		return resultMap;
	}

	@Override
	public Map<String, Object> getUserInfo(LoginVO loginVO) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != loginVO) {
			UserInfo userInfo = new UserInfo();
			String userPass = "";
			userPass = loginVO.getUserPass();

			if(null != userPass && !"".equals(userPass) && 0 < userPass.length()) {
				try {
					aes256 = new AES256Util();
					userPass = aes256.encrypt(userPass);
				} catch(Exception e) {
					logger.error("암호화 중 에러가 발생되었습니다. - userId: {}, error: {}", loginVO.getUserId(), e.toString(), e);
					resultMap.put("result", "fail");
					resultMap.put("resultMsg", "암호화 중 에러가 발생되었습니다.");
					return resultMap;
				}

				loginVO.setUserPass(userPass);
			} else {
				logger.warn("비밀번호가 없습니다. - userId: {}", loginVO.getUserId());
				resultMap.put("result", "fail");
				resultMap.put("resultMsg", "비밀번호가 없습니다.");
				return resultMap;
			}

			try {
				userInfo = loginMapper.getUserInfo(loginVO);

				if(null != userInfo) {
					resultMap.put("result", "success");
					resultMap.put("resultMsg", "회원 로그인에 성공하였습니다.");
					resultMap.put("userInfo", userInfo);
				} else {
					logger.warn("사용자 정보가 없습니다. - userId: {}", loginVO.getUserId());
					resultMap.put("result", "fail");
					resultMap.put("resultMsg", "회원 정보가 없습니다.");
				}
			} catch(Exception e) {
				logger.error("로그인 과정에서 에러가 발생되었습니다. - userId: {}, error: {}", loginVO.getUserId(), e.toString(), e);
				resultMap.put("result", "fail");
				resultMap.put("resultMsg", "로그인 과정에서 에러가 발생되었습니다.");
			}
		} else {
			logger.warn("loginVO가 null입니다.");
			resultMap.put("result", "fail");
			resultMap.put("resultMsg", "로그인 정보가 없습니다.");
		}

		return resultMap;
	}

	/**
	 * 회원 가입 처리
	 * @param userInfo
	 * @return
	 * @throws Exception
	 */
	@Override
	public Map<String, Object> insertUser(UserInfo userInfo) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != userInfo) {
			userInfo.setDelYn("N");
			userInfo.setUseYn("Y");
			userInfo.setUserGrade("U");
			userInfo.setInstId("hnt");
			userInfo.setMdfId("hnt");
			// 신규 가입 시 parent_user_id를 자기 자신으로 설정
			userInfo.setParentUserId(userInfo.getUserId());

			String userPass = userInfo.getUserPass();
			String userId = userInfo.getUserId();

			// 사용자 비밀번호는 암호화하여 입력
			if(null != userPass && !"".equals(userPass) && 0 < userPass.length()) {
				try {
					aes256 = new AES256Util();
					userPass = aes256.encrypt(userPass);
				} catch(Exception e) {
					logger.error("Error : 암호화 중 에러가 발생되었습니다. - " + e.toString(), e);
					resultMap.put("result", "fail");
					resultMap.put("resultMsg", "암호화 중 에러가 발생되었습니다.");
				}

				userInfo.setUserPass(userPass);
			}

			// 사용자 아이디에 @가 포함되어 있을 경우 메일 주소이므로 @ 기준으로 앞 자리를 가져와 사용자 아이디로 사용
			if(null != userId && !"".equals(userId) && 0 < userId.length()) {
				if(userId.contains("@")) {
					String[] userIdArr = userId.split("@");

					if(null != userIdArr && 0 < userIdArr.length) {
						userInfo.setUserId(userIdArr[0]);
					}
				}
			}

			try {
				loginMapper.insertUser(userInfo);
				resultMap.put("result", "success");
				resultMap.put("resultMsg", "회원 가입에 성공하였습니다.");
				resultMap.put("userInfo", userInfo);
			} catch(Exception e) {
				logger.error("Error : 회원 가입 중 오류가 발생되었습니다. - " + e.toString());
				resultMap.put("result", "fail");
				resultMap.put("resultMsg", "회원 가입 중 오류가 발생되었습니다.");
			}
		} else {
			logger.error("Error : 회원 가입에 필요한 정보가 없습니다.");
			resultMap.put("result", "fail");
			resultMap.put("resultMsg", "회원 가입에 필요한 정보가 없습니다.");
		}
		
		return resultMap;
	}

	public void setData(SensorVO sensorVO) {
		this.sensorVO2 = sensorVO;
	}

	public void insertSensorData2() {
		mqttMapper.insertSensorData(sensorVO2);
	}
	
	@Override
    public void insertSensorData(SensorVO sensorVO) throws Exception {
    	if(null != sensorVO) {
    		if(null != sensorVO.getUserId() && !"".equals(sensorVO.getUserId())) {
				try {
					if(null != sensorVO.getSensorValue() && !"".equals(sensorVO.getSensorValue())) {
						mqttMapper.insertSensorData(sensorVO);
					}
				} catch (Exception e) {
					logger.error("Error : " + e.toString());
				}
			}
    	}
    }

    	@Override
	public void insertSensorInfo(Map<String, Object> param) throws Exception {
		if(null != param && 0 < param.size()) {
			try {
				Map<String, Object> sensorInfo = new HashMap<String, Object>();
				sensorInfo = mqttMapper.getSensorInfo(param);

				if(null != sensorInfo && 0 < param.size()) {
					int cnt = Integer.parseInt(String.valueOf(sensorInfo.get("cnt")));

					if(0 < cnt) {
					} else {
						mqttMapper.insertSensorInfo(param);
					}
				} else {
					mqttMapper.insertSensorInfo(param);
				}
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
			}
		}
	}

	@Override
	public Map<String, Object> insertSensorInfo2(Map<String, Object> param) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != param && 0 < param.size()) {
			try {
				String currentUserId = String.valueOf(param.get("userId"));
				String sensorUuid = String.valueOf(param.get("sensorUuid"));
				
			// 요청 타입 구분: 장치 이름 변경 vs 장치 등록
			String requestType = String.valueOf(param.get("requestType"));
			
			// sensorName 파라미터 안전하게 추출
			Object sensorNameObj = param.get("sensorName");
			String sensorName = null;
			if(sensorNameObj != null && !"null".equals(String.valueOf(sensorNameObj)) && !"".equals(String.valueOf(sensorNameObj))) {
				sensorName = String.valueOf(sensorNameObj);
			}
			
			// 1. 기존 장치 확인 (중복등록 체크 전에 수행)
			Map<String, Object> currentSensorInfo = new HashMap<String, Object>();
			currentSensorInfo = mqttMapper.getSensorInfo(param);
			
			// 기존 장치가 있는지 확인
			boolean existingSensor = false;
			if(null != currentSensorInfo && 0 < currentSensorInfo.size()) {
				int cnt = Integer.parseInt(String.valueOf(currentSensorInfo.get("cnt")));
				existingSensor = (cnt > 0);
			}
			
			// 기존 장치가 있고 sensorName이 제공된 경우 → 이름 변경 요청
			if(existingSensor && sensorName != null) {
				logger.info("장치 이름 변경 요청 감지 - userId: {}, sensorUuid: {}, newName: {}", 
					currentUserId, sensorUuid, sensorName);
				
				// 장치 이름만 변경
				Map<String, Object> updateParam = new HashMap<String, Object>();
				updateParam.put("sensorUuid", sensorUuid);
				updateParam.put("sensorName", sensorName);
				updateParam.put("mdfId", param.get("mdfId") != null ? param.get("mdfId") : "hnt");
				
				// 직접 UPDATE 쿼리 실행
				mqttMapper.updateSensorNameDirect(updateParam);
				
				resultMap.put("result", "true");
				resultMap.put("message", "장치 이름이 성공적으로 변경되었습니다.");
				
				logger.info("장치 이름 변경 완료 - sensorUuid: {}, newName: {}", sensorUuid, sensorName);
				
				return resultMap;
			}
			
			// 기존 장치가 있지만 sensorName이 없는 경우 → 중복 등록 시도
			if(existingSensor) {
				resultMap.put("result", "false");
				resultMap.put("message", "이미 등록된 장치입니다.");
				return resultMap;
			}
			
			// 기존 장치가 없는 경우 → 신규 장치 등록 요청
			logger.info("신규 장치 등록 요청으로 인식 - userId: {}, sensorUuid: {}", currentUserId, sensorUuid);
		
		// 3. 다른 사용자가 해당 장치를 소유하고 있는지 확인 (장치 전송 기능)
		Map<String, Object> checkParam = new HashMap<String, Object>();
		checkParam.put("sensorUuid", sensorUuid);
		Map<String, Object> existingOwner = mqttMapper.getSensorInfoByUuid(checkParam);
		
		if(existingOwner != null && existingOwner.size() > 0) {
			String existingUserId = String.valueOf(existingOwner.get("user_id"));
			
			// 다른 사용자가 소유하고 있음 - 장치 전송 처리
			logger.info("장치 전송 시작 - 기존 소유자: {}, 새 소유자: {}, sensorUuid: {}", existingUserId, currentUserId, sensorUuid);
			
			// 4. 기존 소유자의 장치 정보, 설정, 알림 삭제 (동기)
			mqttMapper.deleteSensorInfoByUuid(checkParam);
			mqttMapper.deleteConfigByUuid(checkParam);
			mqttMapper.deleteAlarmByUuid(checkParam);
			
			logger.info("기존 소유자 장치 정보 삭제 완료 - userId: {}, sensorUuid: {}", existingUserId, sensorUuid);
			
			// 5. 기존 소유자의 센서 데이터 비동기 삭제 (대용량 데이터)
			final String finalSensorUuid = sensorUuid;
			final String finalExistingUserId = existingUserId;
			java.util.concurrent.CompletableFuture.runAsync(() -> {
				try {
					logger.info("장치 이전 - 센서 데이터 비동기 삭제 시작 - 기존 소유자: {}, sensorUuid: {}", finalExistingUserId, finalSensorUuid);
					
					int batchSize = 1000; // 한 번에 1,000개씩 삭제
					int deletedCount = 0;
					int totalDeleted = 0;
					
					do {
						Map<String, Object> asyncParam = new HashMap<>();
						asyncParam.put("sensorUuid", finalSensorUuid);
						asyncParam.put("batchSize", batchSize);
						
						deletedCount = mqttMapper.deleteSensorDataBatch(asyncParam);
						totalDeleted += deletedCount;
						
						if (totalDeleted % 10000 == 0 && totalDeleted > 0) {
							logger.info("장치 이전 - 센서 데이터 비동기 삭제 진행 중 - 삭제된 개수: {}, 총 삭제: {}, uuid: {}", 
								deletedCount, totalDeleted, finalSensorUuid);
						}
						
						// DB 부하 방지
						if (deletedCount > 0) {
							try { Thread.sleep(10); } catch (InterruptedException ie) {}
						}
					} while (deletedCount > 0);
					
					logger.info("장치 이전 - 센서 데이터 비동기 삭제 완료 - 기존 소유자: {}, sensorUuid: {}, 총 삭제: {}", 
						finalExistingUserId, finalSensorUuid, totalDeleted);
						
				} catch (Exception e) {
					logger.error("장치 이전 - 센서 데이터 비동기 삭제 중 오류 발생 - uuid: " + finalSensorUuid, e);
				}
			}, asyncExecutor);
		}
		
		// 5. 새 사용자에게 장치 등록
		logger.info("장치등록 시작 - userId: {}, sensorUuid: {}", currentUserId, sensorUuid);
		mqttMapper.insertSensorInfo(param);
		logger.info("장치등록 완료 - userId: {}, sensorUuid: {}", currentUserId, sensorUuid);
		
		// 6. 알람설정 초기값 저장
		try {
			Map<String, Object> defaultSettings = new HashMap<>();
			boolean alarmSuccess = true;
			
			if (alarmSuccess) {
				logger.info("알람설정 저장 완료 - userId: {}, sensorUuid: {}", currentUserId, sensorUuid);
			} else {
				logger.error("알람설정 저장 실패: userId={}, sensorUuid={}", currentUserId, sensorUuid);
			}
		} catch (Exception e) {
			logger.error("알람설정 저장 실패: userId={}, sensorUuid={}, error={}", currentUserId, sensorUuid, e.getMessage());
		}
		
		resultMap.put("result", "true");
		resultMap.put("message", "장치가 성공적으로 등록되었습니다.");
		
		if(existingOwner != null && existingOwner.size() > 0) {
			String existingUserId = String.valueOf(existingOwner.get("user_id"));
			resultMap.put("message", "기존 소유자(" + existingUserId + ")의 장치가 새 사용자(" + currentUserId + ")에게 전송되었습니다.");
		}
		
		logger.info("=== 장치 등록 완료 ===");
		logger.info("최종 결과: {}", resultMap);
				
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
				resultMap.put("result", "false");
				resultMap.put("message", "장치 등록 중 오류가 발생했습니다.");
			}
		}

		return resultMap;
	}

	@Override
	public void updateUserInfo(LoginVO loginVO) throws Exception {
		if(null != loginVO) {
			try {
				loginMapper.updateUserInfo(loginVO);
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
			}
		}
	}

	@Override
	public void updateLoginDtm(LoginVO loginVO) throws Exception {
		if(null != loginVO) {
			try {
				logger.info("로그인 상태 업데이트 시작 - userId: {}", loginVO.getUserId());
				loginMapper.updateLoginDtm(loginVO);
				loginMapper.removeLogoutDtm(loginVO);
				logger.info("로그인 상태 업데이트 완료 - userId: {}", loginVO.getUserId());
			} catch(Exception e) {
				logger.error("로그인 상태 업데이트 실패 - userId: {}, error: {}", loginVO.getUserId(), e.getMessage());
				throw e;
			}
		}
	}

	@Override
	public void updateLogoutDtm(LoginVO loginVO) throws Exception {
		logger.info("=== updateLogoutDtm 호출됨 ===");
		logger.info("loginVO null 여부: {}", (loginVO == null));
		
		if(null != loginVO) {
			logger.info("loginVO.getUserId(): {}", loginVO.getUserId());
			try {
				logger.info("DB 로그아웃 업데이트 시작 - userId: {}", loginVO.getUserId());
				loginMapper.updateLogoutDtm(loginVO);
				logger.info("DB 로그아웃 업데이트 성공 - userId: {}", loginVO.getUserId());

				// 로그아웃 시 연결되어 있는 모든 알람 정보 삭제 처리
				loginMapper.deleteAlarm(loginVO);
				logger.info("알람 정보 삭제 완료 - userId: {}", loginVO.getUserId());
			} catch(Exception e) {
				logger.error("로그아웃 DB 업데이트 에러 - userId: {}, error: {}", loginVO.getUserId(), e.toString(), e);
				throw e;
			}
		} else {
			logger.error("loginVO가 null입니다!");
		}
	}

	@Override
	public void insertData(String str) {
		//logger.info("receive data : " + str);
		// 여기서 들어오는 데이터 uuid로 구분하여 db에 입력 처리
	}
	
	@Override
	public UserInfo getUserInfoByUserId(String userId) throws Exception {
		try {
			// 비밀번호 검증 없이 사용자 정보만 조회
			UserInfo userInfo = loginMapper.getUserInfoByUserId(userId);
			return userInfo;
		} catch (Exception e) {
			logger.error("Error getting user info by userId: {}", userId, e);
			throw e;
		}
	}

	@Override
	public void updateUserActivityStatus(String userId, String activityStatus, String focusStatus) throws Exception {
		try {
			Map<String, Object> param = new HashMap<>();
			param.put("userId", userId);
			param.put("activityStatus", activityStatus);
			param.put("focusStatus", focusStatus);
			loginMapper.updateUserActivityStatus(param);
		} catch (Exception e) {
			logger.error("DB 업데이트 실패 - userId: {}, activityStatus: {}, focusStatus: {}, error: {}", userId, activityStatus, focusStatus, e.getMessage());
			throw e;
		}
	}

	@Override
	public void updateUserFocusStatus(String userId, String focusStatus) throws Exception {
		try {
			Map<String, Object> param = new HashMap<>();
			param.put("userId", userId);
			param.put("focusStatus", focusStatus);
			loginMapper.updateUserFocusStatus(param);
		} catch (Exception e) {
			logger.error("Error updating user focus status: " + e.toString());
			throw e;
		}
	}

	@Override
	public String getUserActivityStatus(String userId, javax.servlet.http.HttpSession session) throws Exception {
		try {
			// DB에서 사용자 정보 조회
			UserInfo userInfo = getUserInfoByUserId(userId);
			if (userInfo == null) {
				return "N";
			}
			
			// 현재 세션의 사용자와 비교
			String currentUserId = (String) session.getAttribute("userId");
			if (userId.equals(currentUserId)) {
				// 현재 로그인한 사용자가 활동 중이므로 DB 상태를 활성으로 업데이트
				try {
					updateUserActivityStatus(userId, "Y", "N");
					logger.info("사용자 활동 상태 업데이트 - userId: {}, status: 활성", userId);
				} catch (Exception e) {
					logger.error("사용자 활동 상태 업데이트 실패 - userId: {}, error: {}", userId, e.getMessage());
				}
				return "Y";
			}
			
			// 다른 사용자의 경우 DB의 로그인 상태 확인
			String loginStatus = userInfo.getLoginDtm();
			String logoutStatus = userInfo.getLogoutDtm();
			
			// 로그인 상태가 'Y'이고 로그아웃 상태가 'N'이면 활성
			if ("Y".equals(loginStatus) && "N".equals(logoutStatus)) {
				// 추가로 마지막 수정 시간 확인 (2분 이상 지났으면 비활성)
				// 여기서는 간단히 로그인 상태만 확인
				return "Y";
			} else {
				return "N";
			}
		} catch (Exception e) {
			logger.error("Error getting user activity status for userId: {}, error: {}", userId, e.getMessage());
			return "N"; // 에러 시 비활성으로 처리
		}
	}

	/**
	 * 세션 타임아웃 체크 및 자동 비활성 처리
	 */
	@Override
	public void checkAndUpdateSessionTimeout() throws Exception {
		try {
			logger.info("세션 타임아웃 체크 시작");
			
			// 타임아웃 대상 사용자 조회 (로그용)
			List<UserInfo> timeoutUsers = loginMapper.getTimeoutUsers();
			if (timeoutUsers != null && timeoutUsers.size() > 0) {
				logger.info("타임아웃 대상 사용자 수: {}", timeoutUsers.size());
				for (UserInfo user : timeoutUsers) {
					logger.info("타임아웃 처리 예정 사용자: {}", user.getUserId());
				}
				
				// 각 사용자별로 개별 처리하여 다른 사용자에게 영향 주지 않도록 함
				for (UserInfo user : timeoutUsers) {
					try {
						// 개별 사용자 타임아웃 처리
						Map<String, Object> param = new HashMap<>();
						param.put("userId", user.getUserId());
						loginMapper.updateSpecificUserTimeout(param);
						logger.info("사용자 {} 타임아웃 처리 완료", user.getUserId());
					} catch (Exception e) {
						logger.error("사용자 {} 타임아웃 처리 실패: {}", user.getUserId(), e.getMessage());
					}
				}
			} else {
				logger.info("타임아웃 대상 사용자 없음");
			}
			
			logger.info("세션 타임아웃 체크 완료");
		} catch (Exception e) {
			logger.error("세션 타임아웃 체크 실패: {}", e.getMessage());
		}
	}

	/**
	 * 타임아웃 대상 사용자 목록 조회
	 */
	@Override
	public List<UserInfo> getTimeoutUsersList() throws Exception {
		try {
			return loginMapper.getTimeoutUsers();
		} catch (Exception e) {
			logger.error("타임아웃 대상 사용자 조회 실패: {}", e.getMessage());
			throw e;
		}
	}

	/**
	 * 특정 사용자 타임아웃 처리
	 */
	@Override
	public void processUserTimeout(String userId) throws Exception {
		try {
			// 개별 사용자 타임아웃 처리
			Map<String, Object> param = new HashMap<>();
			param.put("userId", userId);
			loginMapper.updateSpecificUserTimeout(param);
			logger.info("사용자 {} 타임아웃 처리 완료", userId);
		} catch (Exception e) {
			logger.error("사용자 {} 타임아웃 처리 실패: {}", userId, e.getMessage());
			throw e;
		}
	}

	/**
	 * 마지막 로그인 시간 업데이트
	 */
	@Override
	public void updateLastLoginTime(String userId) throws Exception {
		try {
			if (StringUtil.isEmpty(userId)) {
				logger.warn("사용자 ID가 비어있어 로그인 시간 업데이트 불가");
				return;
			}
			
			Map<String, Object> param = new HashMap<>();
			param.put("userId", userId);
			param.put("loginDtm", new java.util.Date());
			
			loginMapper.updateLastLoginTime(param);
			logger.debug("사용자 {} 마지막 로그인 시간 업데이트 완료", userId);
			
		} catch (Exception e) {
			logger.error("사용자 {} 마지막 로그인 시간 업데이트 실패: {}", userId, e.getMessage());
			throw e;
		}
	}

	@Override
	public String getMainUserIdForSubUser(String subUserId) throws Exception {
		try {
			// 부계정의 센서 정보에서 sensor_id를 조회 (sensor_id가 메인 사용자 ID)
			Map<String, Object> param = new HashMap<>();
			param.put("subUserId", subUserId);
			Map<String, Object> result = loginMapper.getMainUserIdForSubUser(param);
			
			if (result != null && result.size() > 0) {
				String mainUserId = String.valueOf(result.get("sensor_id"));
				logger.info("부계정 {}의 메인 사용자 ID 조회 성공: {}", subUserId, mainUserId);
				return mainUserId;
			} else {
				logger.warn("부계정 {}의 메인 사용자 ID를 찾을 수 없습니다.", subUserId);
				return null;
			}
		} catch (Exception e) {
			logger.error("부계정 {}의 메인 사용자 ID 조회 실패: {}", subUserId, e.getMessage());
			throw e;
		}
	}

	/**
	 * 사용자 활동 시간 업데이트 (하트비트)
	 */
	@Override
	public void updateUserActivity(String userId) throws Exception {
		try {
			if (StringUtil.isEmpty(userId)) {
				logger.warn("사용자 ID가 비어있어 활동 시간 업데이트 불가");
				return;
			}
			
			Map<String, Object> param = new HashMap<>();
			param.put("userId", userId);
			
			loginMapper.updateUserActivity(param);
			logger.debug("사용자 {} 활동 시간 업데이트 완료 (하트비트)", userId);
			
		} catch (Exception e) {
			logger.error("사용자 {} 활동 시간 업데이트 실패: {}", userId, e.getMessage());
			throw e;
		}
	}
	
	/**
	 * 사용자 설정 업데이트 (User-Agent, Auto Login)
	 */
	@Override
	public void updateUserPreferences(LoginVO loginVO) throws Exception {
		try {
			if (loginVO != null && loginVO.getUserId() != null) {
				loginMapper.updateUserPreferences(loginVO);
				
				String clientType = com.andrew.hnt.api.util.UserAgentUtil.getClientType(loginVO.getUserAgent());
				logger.info("사용자 설정 업데이트 성공 - userId: {}, autoLogin: {}, 접속유형: {}", 
					loginVO.getUserId(), 
					loginVO.getSaveId(),
					clientType);
			}
		} catch (Exception e) {
			logger.error("사용자 설정 업데이트 실패", e);
			// 설정 저장 실패는 로그인 자체를 막지 않음
		}
	}
	
	/**
	 * 웹 사용자만 타임아웃 체크 (앱 사용자 제외)
	 */
	@Override
	public void checkAndUpdateSessionTimeoutForWebOnly() throws Exception {
		try {
			logger.info("=== 웹 사용자 타임아웃 체크 시작 ===");
			
			// 3분(180초) 이상 활동이 없는 웹 사용자만 비활성 처리
			// 앱 사용자는 제외 (앱 사용자는 user_agent 컬럼으로 구분)
			int timeoutCount = loginMapper.updateInactiveWebUsers();
			
			if (timeoutCount > 0) {
				logger.info("타임아웃 처리된 웹 사용자 수: {}", timeoutCount);
			} else {
				logger.debug("타임아웃 처리할 웹 사용자 없음");
			}
			
			logger.info("=== 웹 사용자 타임아웃 체크 완료 ===");
		} catch (Exception e) {
			logger.error("웹 사용자 타임아웃 체크 실패", e);
			throw e;
		}
	}

	/**
	 * 최적화된 사용자 목록 조회 (활동 상태 포함) - N+1 쿼리 문제 해결
	 */
	@Override
	public Map<String, Object> getUserListWithActivityStatus(String currentUserId) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		List<UserInfo> userList = new ArrayList<UserInfo>();
		
		try {
			Map<String, Object> param = new HashMap<>();
			param.put("currentUserId", currentUserId);
			
			userList = loginMapper.getUserListWithActivityStatus(param);
			
			// 최적화된 쿼리에서 이미 loginYn이 계산되어 있으므로 추가 처리 불필요
			// 중복 로직 제거됨
			
			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "사용자 목록 조회 성공");
			resultMap.put("userList", userList);
			
			logger.info("최적화된 사용자 목록 조회 완료 - 사용자 수: {}", userList != null ? userList.size() : 0);
			
		} catch (Exception e) {
			logger.error("최적화된 사용자 목록 조회 실패: {}", e.getMessage());
			resultMap.put("resultCode", "500");
			resultMap.put("resultMessage", "사용자 목록 조회 실패: " + e.getMessage());
			resultMap.put("userList", new ArrayList<UserInfo>());
		}
		
		return resultMap;
	}

	/**
	 * 최적화된 사용자 및 부계정 목록 조회 (활동 상태 포함) - N+1 쿼리 문제 해결
	 */
	@Override
	public Map<String, Object> getUserAndSubUserListWithActivityStatus(String userId, String currentUserId) throws Exception {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		List<UserInfo> userList = new ArrayList<UserInfo>();
		
		try {
			Map<String, Object> param = new HashMap<>();
			param.put("userId", userId);
			param.put("currentUserId", currentUserId);
			
			userList = loginMapper.getUserAndSubUserListWithActivityStatus(param);
			
			// 최적화된 쿼리에서 이미 loginYn이 계산되어 있으므로 추가 처리 불필요
			// 중복 로직 제거됨
			
			resultMap.put("resultCode", "200");
			resultMap.put("resultMessage", "사용자 및 부계정 목록 조회 성공");
			resultMap.put("userList", userList);
			
			logger.info("최적화된 사용자 및 부계정 목록 조회 완료 - 사용자 수: {}", userList != null ? userList.size() : 0);
			
		} catch (Exception e) {
			logger.error("최적화된 사용자 및 부계정 목록 조회 실패: {}", e.getMessage());
			resultMap.put("resultCode", "500");
			resultMap.put("resultMessage", "사용자 및 부계정 목록 조회 실패: " + e.getMessage());
			resultMap.put("userList", new ArrayList<UserInfo>());
		}
		
		return resultMap;
	}

}
