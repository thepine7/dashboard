package com.andrew.hnt.api.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.andrew.hnt.api.util.AES256Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andrew.hnt.api.mapper.AdminMapper;
import com.andrew.hnt.api.mapper.LoginMapper;
import com.andrew.hnt.api.model.UserInfo;
import com.andrew.hnt.api.service.AdminService;
import com.andrew.hnt.api.common.BaseService;

@Service
public class AdminServiceImpl extends BaseService implements AdminService {
	
	@Autowired
	private LoginMapper loginMapper;
	
	@Autowired
	private AdminMapper adminMapper;

	private AES256Util aes256;

	private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);
	
	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> getUserInfo(String userId, String sensorUuid) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		logInfo("getUserInfo", "사용자 정보 조회 시작 - userId: " + userId + ", sensorUuid: " + sensorUuid);
		
        if(null != userId && !"".equals(userId)) {
			try {
                UserInfo userInfo = new UserInfo();
				Map<String, Object> sensorMap = new HashMap<String, Object>();
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("userId", userId);
				param.put("sensorUuid", sensorUuid);
				
				logInfo("getUserInfo", "DB 조회 파라미터: " + param.toString());
				
                // 비밀번호가 없는 조회 경로에서는 userId만으로 조회
                userInfo = loginMapper.getUserInfoByUserId(userId);
                logInfo("getUserInfo", "사용자 정보 조회 결과: " + (userInfo != null ? userInfo.toString() : "null"));

				sensorMap = adminMapper.getSensorInfoByUuid(param);
				logInfo("getUserInfo", "센서 정보 조회 결과: " + (sensorMap != null ? sensorMap.toString() : "null"));
				
				resultMap.put("userInfo", userInfo);
				resultMap.put("sensorInfo", sensorMap);
			} catch(Exception e) {
				logError("getUserInfo", "사용자 정보 조회 중 오류 발생", e);
			}
		}
		
		return resultMap;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> getSensorInfo(Map<String, Object> param) {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != param && 0 < param.size()) {
			try {
				resultMap = adminMapper.getSensorInfo(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
			}
		}

		return resultMap;
	}
	
	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> getSensorInfoByUuid(String sensorUuid) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("sensorUuid", sensorUuid);
			// userId는 현재 세션에서 가져와야 하지만, 메서드 시그니처가 String sensorUuid만 받음
			// 임시로 null로 설정하고 쿼리에서 userId 조건을 제거
			param.put("userId", null);
			resultMap = adminMapper.getSensorInfoByUuid(param);
			logger.info("AdminServiceImpl.getSensorInfoByUuid - 센서 정보 조회 결과: {}", resultMap);
		} catch(Exception e) {
			logger.error("AdminServiceImpl.getSensorInfoByUuid - 오류 발생: {}", e.getMessage());
		}
		
		return resultMap;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getSensorList(String userId) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		if(null != userId && !"".equals(userId)) {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("userId", userId);

			resultList = adminMapper.getSensorList(param);

			if(null != resultList && 0 < resultList.size()) {
				for(int i=0; i < resultList.size(); i++) {
					String chk = String.valueOf(resultList.get(i).get("sensor_name"));
					int j = 0;
					if(null != chk && !"".equals(chk) && 0 < chk.length()) {

					} else {
						resultList.get(i).remove("sensor_name");
						j = i + 1;
						resultList.get(i).put("sensor_name", "장치"+j);
					}
				}
			}
		}

		return resultList;
	}

	@Override
	public List<Map<String, Object>> getSubSensorList(String mainUserId, String subUserId) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		if(null != mainUserId && !"".equals(mainUserId) && null != subUserId && !"".equals(subUserId)) {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("mainUserId", mainUserId);
			param.put("subUserId", subUserId);

			try {
				resultList = adminMapper.getSubSensorList(param);

				if(null != resultList && 0 < resultList.size()) {
					for(int i=0; i < resultList.size(); i++) {
						String chk = String.valueOf(resultList.get(i).get("sensor_name"));
						int j = 0;
						if(null != chk && !"".equals(chk) && 0 < chk.length()) {

						} else {
							resultList.get(i).remove("sensor_name");
							j = i + 1;
							resultList.get(i).put("sensor_name", "장치"+j);
						}
					}
				}
			} catch(Exception e) {
				logger.error("Error in getSubSensorList: " + e.toString());
			}
		}

		return resultList;
	}

	@Override
	public List<Map<String, Object>> getFullSensorList(String targetUserId, String parentUserId) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		if(null != targetUserId && targetUserId.length() > 0) {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("targetUserId", targetUserId);
			param.put("parentUserId", parentUserId);

			try {
				resultList = adminMapper.getFullSensorList(param);

				if(null != resultList && 0 < resultList.size()) {
					for(int i=0; i < resultList.size(); i++) {
						String name = String.valueOf(resultList.get(i).get("sensor_name"));
						if(name == null || name.length() == 0 || "null".equalsIgnoreCase(name)) {
							resultList.get(i).put("sensor_name", "장치" + (i + 1));
						}
					}
				}
			} catch(Exception e) {
				logger.error("Error in getFullSensorList: {}", e.toString());
			}
		}

		return resultList;
	}

	@Override
	public void insertSetting(Map<String, Object> settingMap) {
		if(null != settingMap && 0 < settingMap.size()) {
			logger.info("userId : " + String.valueOf(settingMap.get("userId")));
			logger.info("sensorId : " + String.valueOf(settingMap.get("userId")));
			logger.info("sensorUuid : " + String.valueOf(settingMap.get("sensorUuid")));
			try {
				// 이미 입력된 설정이 있을 경우에는 업데이트, 없으면 입력 처리
				Map<String, Object> chkMap = new HashMap<String, Object>();
				Map<String, Object> alarmMap = new HashMap<String, Object>();
				chkMap.put("userId", String.valueOf(settingMap.get("userId")));
				chkMap.put("sensorId", String.valueOf(settingMap.get("userId")));
				chkMap.put("sensorUuid", String.valueOf(settingMap.get("sensorUuid")));

				Map<String, Object> sensorMap = new HashMap<String, Object>();
				sensorMap = adminMapper.selectSensorId(chkMap);

				logger.info("sensorMap size : " + sensorMap.size());

				if(null != sensorMap && 0 < sensorMap.size()) {
					logger.info("sensorMap info : " + sensorMap.get("sensor_id"));
					chkMap.remove("sensorId");
					chkMap.put("sensorId", String.valueOf(sensorMap.get("sensor_id")));
					logger.info("chkMap info : " + chkMap.get("sensorId"));
					settingMap.remove("sensorId");
					settingMap.put("sensorId", String.valueOf(sensorMap.get("sensor_id")));
					logger.info("settingMap info : " + settingMap.get("sensorId"));
				}

				alarmMap = adminMapper.selectSetting(chkMap);

				if(null != alarmMap && 0 < alarmMap.size()) {
					adminMapper.updateSetting(settingMap);
				} else {
					adminMapper.insertSetting(settingMap);
				}
			} catch(Exception e) {
				logger.error("Error : " + e.toString(), e);
			}
		}
	}

	@Override
	public Map<String, Object> selectSetting(Map<String, Object> param) {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != param && 0 < param.size()) {
			try {
				Map<String, Object> sensorMap = new HashMap<String, Object>();
				sensorMap = adminMapper.selectSensorId(param);

				if(null != sensorMap && 0 < sensorMap.size()) {
					param.remove("sensorId");
					param.put("sensorId", String.valueOf(sensorMap.get("sensor_id")));
				}

				resultMap = adminMapper.selectSetting(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString(), e);
			}
		}

		return resultMap;
	}

	@Override
	public Map<String, Object> selectNoti(Map<String, Object> param) {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != param && 0 < param.size()) {
			try {
				resultMap = adminMapper.selectNoti(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString(), e);
			}
		}

		return resultMap;
	}

	@Override
	public Map<String, Object> selectChkNoti(Map<String, Object> param) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		if(null != param && 0 < param.size()) {
			try {
				resultList = adminMapper.selectChkNoti(param);

				if(null != resultList && 0 < resultList.size()) {
					resultMap.put("result", "200");
					resultMap.put("cnt", resultList.size());
				}
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
			}
		}

		return resultMap;
	}

	@Override
	public void deleteNoti2(Map<String, Object> param) {
		if(null != param && 0 < param.size()) {
			try {
				adminMapper.deleteNoti2(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString(), e);
			}
		}
	}

	@Override
	public void insertNoti(Map<String, Object> param) {
		if(null != param && 0 < param.size()) {
			try {
				adminMapper.insertNoti(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString(), e);
			}
		}
	}

	@Override
	public void updateSensorInfo(Map<String, Object> param) {
		if(null != param && 0 < param.size()) {
			try {
				adminMapper.updateSensorInfo(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString(), e);
			}
		}
	}

	@Override
	public Map<String, Object> getAlarmSetting(Map<String, Object> param) {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != param && 0 < param.size()) {
			try {
				resultMap = adminMapper.getAlarmSetting(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
			}
		}

		return resultMap;
	}



	@Override
	public String getCurTemp(Map<String, Object> param) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String result = "";

		if(null != param && 0 < param.size()) {
			try {
				// 타임아웃 방지를 위한 빠른 조회
				resultMap = adminMapper.getCurTemp(param);

				if(null != resultMap && 0 < resultMap.size()) {
					result = String.valueOf(resultMap.get("sensor_value"));
				}
			} catch(Exception e) {
				logger.error("Error in getCurTemp: " + e.toString());
				// 타임아웃 발생 시 기본값 반환
				result = "0";
			}
		}

		return result;
	}

	@Override
	public int chkError(Map<String, Object> param) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		int result = 0;

		if(null != param && 0 < param.size()) {
			try {
				resultMap = adminMapper.chkError(param);

				if(null != resultMap && 0 < resultMap.size()) {
					result = Integer.parseInt(String.valueOf(resultMap.get("cnt")));

					if(result < 1) {
						result = 0;
					} else {
						// 통신 오류가 해소된 경우 이미 입력되어 있는 발송 대상 정보가 있을 경우 발송되지 않도록 처리
						param.put("alarmType", "netError1");

						Map<String, Object> chkMap = new HashMap<String, Object>();
						chkMap = adminMapper.selectNoti(param);

						if(null != chkMap && 0 < chkMap.size()) {
							param.put("no", String.valueOf(chkMap.get("no")));

							try {
								adminMapper.updateNoti(param);
							} catch(Exception e) {
								logger.error("Error : " + e.toString());
							}
						}

						param.remove("no");
						param.remove("alarmType");

						param.put("alarmType", "netError2");
						chkMap = new HashMap<String, Object>();
						chkMap = adminMapper.selectNoti(param);

						if(null != chkMap && 0 < chkMap.size()) {
							param.put("no", String.valueOf(chkMap.get("no")));

							try {
								adminMapper.updateNoti(param);
							} catch(Exception e) {
								logger.error("Error : " + e.toString());
							}
						}
					}
				}
			} catch(Exception e) {
				logger.error("Error :  "+ e.toString());
			}
		}

		return result;
	}

	@Override
	public void updateNoti(Map<String, Object> param) {
		if(null != param && 0 < param.size()) {
			try {
				adminMapper.updateNoti(param);
				adminMapper.updateUrgentNoti(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
			}
		}
	}

	@Override
	public Map<String, Object> selectUrgentNoti(Map<String, Object> param) {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != param && 0 < param.size()) {
			try {
				resultMap = adminMapper.selectUrgentNoti(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
			}
		}

		return resultMap;
	}

	@Override
	public Map<String, Object> selectReleaseNoti(Map<String, Object> param) {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		if(null != param && 0 < param.size()) {
			try {
				resultMap = adminMapper.selectReleaseNoti(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
			}
		}

		return resultMap;
	}

	@Override
	public void insertUrgentNoti(Map<String, Object> param) {
		if(null != param && 0 < param.size()) {
			try {
				adminMapper.insertUrgentNoti(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString(), e);
			}
		}
	}

	@Override
	public void updateUrgentNoti(Map<String, Object> param) {
		if(null != param && 0 < param.size()) {
			try {
				adminMapper.updateUrgentNoti(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
			}
		}
	}

	@Override
	public void deleteUser(String userId) {
		if(null != userId && !"".equals(userId) && 0 < userId.length()) {
			try {
				// 1. 사용자 센서 데이터 삭제 (모든 센서 데이터 완전 삭제)
				adminMapper.deleteUserSensorData(userId);
				logger.info("사용자 센서 데이터 삭제 완료 - userId: {}", userId);
				
				// 2. 사용자 알림 데이터 삭제
				Map<String, Object> alarmParam = new HashMap<String, Object>();
				alarmParam.put("userId", userId);
				adminMapper.deleteDeviceAlarm(alarmParam);
				logger.info("사용자 알림 데이터 삭제 완료 - userId: {}", userId);
				
				// 3. 사용자 장치 설정 정보 삭제
				adminMapper.deleteConfig(userId, "");
				logger.info("사용자 장치 설정 정보 삭제 완료 - userId: {}", userId);
				
				// 4. 사용자 장치 기본 정보 삭제
				adminMapper.deleteSensor(userId);
				logger.info("사용자 장치 기본 정보 삭제 완료 - userId: {}", userId);
				
				// 5. 사용자 정보 삭제
				adminMapper.deleteUser(userId);
				logger.info("사용자 정보 삭제 완료 - userId: {}", userId);
				
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
			}
		}
	}

	@Override
	public void deleteSubUser(String userId) {
		if(null != userId && !"".equals(userId) && 0 < userId.length()) {
			try {
				// 부계정 사용자 정보만 삭제 (장치 정보는 보존)
				adminMapper.deleteSubUser(userId);
				logger.info("부계정 사용자 정보 삭제 완료 - userId: {}", userId);
				
			} catch(Exception e) {
				logger.error("Error : " + e.toString());
			}
		}
	}

	@Override
	public void updateUser(Map<String, Object> param) {
		if(null != param && 0 < param.size()) {
			try {
				logger.info("=== updateUser 실행 직전 ===");
				logger.info("수정할 userId: {}", param.get("userId"));
				logger.info("수정할 userTel: {}", param.get("userTel"));
				logger.info("수정할 userEmail: {}", param.get("userEmail"));
				logger.info("수정할 userGrade: {}", param.get("userGrade"));
				
				int updateCount = adminMapper.updateUser(param);
				
				logger.info("=== updateUser 실행 완료 ===");
				logger.info("DB UPDATE 영향받은 행 수: {}", updateCount);
				
				if(updateCount == 0) {
					logger.warn("⚠️ DB UPDATE 실패 - 영향받은 행이 0개입니다. userId가 존재하지 않거나 권한이 없습니다.");
					throw new RuntimeException("사용자 정보 업데이트 실패 - 해당 사용자를 찾을 수 없습니다.");
				}
			} catch(Exception e) {
				logger.error("updateUser 실패 - userId: {}, error: {}", param.get("userId"), e.toString());
				throw new RuntimeException("사용자 정보 업데이트 실패", e);
			}
		}
	}

	@Override
	public void createSubProc(Map<String, Object> param) throws Exception {
		if(null != param && 0 < param.size()) {
		String subId = String.valueOf(param.get("subId"));
		String subPass = String.valueOf(param.get("subPass"));
		String subNm = String.valueOf(param.get("subNm"));
		String userTel = String.valueOf(param.get("userTel"));
		String userEmail = String.valueOf(param.get("userEmail"));

		// 서브 사용자 정보 입력
			if(null != subId && !"".equals(subId) && 0 < subId.length()) {
				if(subId.contains("@")) {
					String[] userIdArr = subId.split("@");

					if(null != userIdArr && 0 < userIdArr.length) {
						subId = userIdArr[0];
						param.remove("subId");
						param.put("subId", subId);
					}
				}
			}

			if(null != subPass && !"".equals(subPass) && 0 < subPass.length()) {
				try {
					aes256 = new AES256Util();
					subPass = aes256.encrypt(subPass);
					param.remove("subPass");
					param.put("subPass", subPass);
				} catch(Exception e) {
					logger.error("Error : 암호화 중 에러가 발생되었습니다. - " + e.toString(), e);
					throw new Exception("비밀번호 암호화 중 오류가 발생했습니다");
				}
			}

			// parent_user_id 유효성 검사 강화
			String parentUserId = String.valueOf(param.get("userId"));
			if(parentUserId == null || parentUserId.isEmpty() || 
			   "null".equalsIgnoreCase(parentUserId) || "undefined".equalsIgnoreCase(parentUserId)) {
				logger.error("부계정 생성 실패 - parentUserId가 유효하지 않음: {}", param);
				throw new Exception("주계정 ID가 유효하지 않습니다");
			}
			logger.info("부계정 생성 시작 - subId: {}, parentUserId: {}", subId, parentUserId);

			UserInfo userInfo = new UserInfo();
			userInfo.setUserId(subId);
			userInfo.setUserPass(subPass);
			userInfo.setUserNm(subNm);
			userInfo.setUserGrade("B");
			userInfo.setUserEmail(String.valueOf(param.get("userEmail")));
			userInfo.setUserTel(String.valueOf(param.get("userTel")));
			userInfo.setUseYn("Y");
			userInfo.setDelYn("N");
			userInfo.setInstId("hnt");
			userInfo.setMdfId("hnt");
			userInfo.setParentUserId(parentUserId);

			try {
				loginMapper.insertUser(userInfo);
				logger.info("부계정 생성 성공 - subId: {}, parentUserId: {}", subId, parentUserId);
				logger.info("DB 저장 확인 - UserInfo.parentUserId: {}", userInfo.getParentUserId());
			} catch(Exception e) {
				String errorMsg = e.getMessage();
				
				// MySQL Duplicate entry 에러 감지
				if(errorMsg != null && errorMsg.contains("Duplicate entry")) {
					logger.error("중복 사용자 ID - subId: {}", subId);
					throw new Exception("이미 존재하는 사용자 아이디입니다");
				}
				
				logger.error("부계정 생성 실패 - subId: {}, parentUserId: {}, error: {}", subId, parentUserId, e.toString());
				throw new Exception("부계정 생성 중 오류가 발생했습니다");
			}

			// 부계정은 장치를 직접 소유하지 않으며 parent_user_id 기반으로 장치를 조회한다.
			if(param.containsKey("sensorInfoCopy")) {
				logger.warn("sensorInfoCopy 파라미터는 더 이상 사용되지 않습니다 - userId: {}", subId);
			}
		}
	}

	@Override
	public void updateUserGrade(Map<String, Object> param) throws Exception {
		if(null != param && 0 < param.size()) {
			String userId = String.valueOf(param.get("userId"));
			String userGrade = String.valueOf(param.get("userGrade"));

			if(null != userId && !"".equals(userId) && 0 < userId.length()) {
				if(null != userGrade && !"".equals(userGrade) && 0 < userGrade.length()) {
					try {
						adminMapper.updateUserGrade(param);
					} catch(Exception e) {
						logger.error("Error : " + e.toString());
						throw new Exception();
					}
				}
			}
		}
	}

	@Override
	public void resetAllUserActivityStatus() throws Exception {
		try {
			adminMapper.resetAllUserActivityStatus();
			logger.info("모든 사용자의 활동 상태 초기화 완료");
		} catch (Exception e) {
			logger.error("사용자 활동 상태 초기화 실패: " + e.toString());
			throw e;
		}
	}
	
	@Override
	public void resetSpecificUserActivityStatus(String userId) throws Exception {
		try {
			adminMapper.resetSpecificUserActivityStatus(userId);
			logger.info("특정 사용자 활동 상태 초기화 완료 - userId: {}", userId);
		} catch (Exception e) {
			logger.error("특정 사용자 활동 상태 초기화 실패 - userId: {}, error: {}", userId, e.toString());
			throw e;
		}
	}
	
	@Override
	public boolean isSubAccount(String userId) {
		try {
			// hnt_user 테이블의 parent_user_id 확인
			UserInfo userInfo = loginMapper.getUserInfoByUserId(userId);
			if(userInfo != null) {
				String parentId = userInfo.getParentUserId();
				// parent_user_id가 자기 자신이 아니면 부계정
				boolean isSub = parentId != null && !parentId.equals(userId) && !parentId.equals("null");
				logger.info("부계정 여부 확인 (parent_user_id 기반) - userId: {}, parentId: {}, isSubAccount: {}", 
					userId, parentId, isSub);
				return isSub;
			}
		} catch(Exception e) {
			logger.error("부계정 여부 확인 실패 - userId: {}, error: {}", userId, e.toString());
		}
		return false;
	}
	
	@Override
	public String getMainUserIdForSubUser(String subUserId) {
		try {
			// hnt_user 테이블의 parent_user_id 직접 반환
			UserInfo userInfo = loginMapper.getUserInfoByUserId(subUserId);
			if(userInfo != null) {
				String parentId = userInfo.getParentUserId();
				String resultId = parentId != null && !parentId.equals("null") ? parentId : subUserId;
				logger.info("부계정의 메인 사용자 ID 조회 (parent_user_id 기반) - subUserId: {}, parentId: {}, resultId: {}", 
					subUserId, parentId, resultId);
				return resultId;
			}
		} catch(Exception e) {
			logger.error("부계정의 메인 사용자 ID 조회 실패 - subUserId: {}, error: {}", subUserId, e.toString());
		}
		return subUserId;
	}

	@Override
	public List<Map<String, Object>> getUserSensorList(String userId) {
		try {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("userId", userId);
			
			List<Map<String, Object>> sensorList = adminMapper.getUserSensorList(param);
			logger.info("사용자 센서 리스트 조회 - userId: {}, count: {}", userId, sensorList != null ? sensorList.size() : 0);
			return sensorList;
		} catch (Exception e) {
			logger.error("사용자 센서 리스트 조회 실패 - userId: {}, error: {}", userId, e.toString());
			return new ArrayList<Map<String, Object>>();
		}
	}
	
	/**
	 * 알람 설정 저장
	 * @param alarmData 알람 설정 데이터
	 */
	@Override
	public void saveAlarmSetting(Map<String, Object> alarmData) {
		try {
			logger.info("알람 설정 저장 시작: {}", alarmData);
			
			// delay_time 계산 (시간 + 분) - 모든 값이 분 단위로 저장됨
			if(alarmData.get("delayHour1") != null && alarmData.get("delayMin1") != null) {
				int delayHour1 = Integer.parseInt(alarmData.get("delayHour1").toString());
				int delayMin1 = Integer.parseInt(alarmData.get("delayMin1").toString());
				alarmData.put("delay_time1", delayHour1 + delayMin1);
			}
			
			if(alarmData.get("delayHour2") != null && alarmData.get("delayMin2") != null) {
				int delayHour2 = Integer.parseInt(alarmData.get("delayHour2").toString());
				int delayMin2 = Integer.parseInt(alarmData.get("delayMin2").toString());
				alarmData.put("delay_time2", delayHour2 + delayMin2);
			}
			
			if(alarmData.get("delayHour3") != null && alarmData.get("delayMin3") != null) {
				int delayHour3 = Integer.parseInt(alarmData.get("delayHour3").toString());
				int delayMin3 = Integer.parseInt(alarmData.get("delayMin3").toString());
				alarmData.put("delay_time3", delayHour3 + delayMin3);
			}
			
			if(alarmData.get("delayHour4") != null && alarmData.get("delayMin4") != null) {
				int delayHour4 = Integer.parseInt(alarmData.get("delayHour4").toString());
				int delayMin4 = Integer.parseInt(alarmData.get("delayMin4").toString());
				alarmData.put("delay_time4", delayHour4 + delayMin4);
			}
			
			if(alarmData.get("delayHour5") != null && alarmData.get("delayMin5") != null) {
				int delayHour5 = Integer.parseInt(alarmData.get("delayHour5").toString());
				int delayMin5 = Integer.parseInt(alarmData.get("delayMin5").toString());
				alarmData.put("delay_time5", delayHour5 + delayMin5);
			}
			
			// re_delay_time 계산 (시간 + 분) - 모든 값이 분 단위로 저장됨
			if(alarmData.get("reDelayHour1") != null && alarmData.get("reDelayMin1") != null) {
				int reDelayHour1 = Integer.parseInt(alarmData.get("reDelayHour1").toString());
				int reDelayMin1 = Integer.parseInt(alarmData.get("reDelayMin1").toString());
				alarmData.put("re_delay_time1", reDelayHour1 + reDelayMin1);
			}
			
			if(alarmData.get("reDelayHour2") != null && alarmData.get("reDelayMin2") != null) {
				int reDelayHour2 = Integer.parseInt(alarmData.get("reDelayHour2").toString());
				int reDelayMin2 = Integer.parseInt(alarmData.get("reDelayMin2").toString());
				alarmData.put("re_delay_time2", reDelayHour2 + reDelayMin2);
			}
			
			if(alarmData.get("reDelayHour3") != null && alarmData.get("reDelayMin3") != null) {
				int reDelayHour3 = Integer.parseInt(alarmData.get("reDelayHour3").toString());
				int reDelayMin3 = Integer.parseInt(alarmData.get("reDelayMin3").toString());
				alarmData.put("re_delay_time3", reDelayHour3 + reDelayMin3);
			}
			
			if(alarmData.get("reDelayHour4") != null && alarmData.get("reDelayMin4") != null) {
				int reDelayHour4 = Integer.parseInt(alarmData.get("reDelayHour4").toString());
				int reDelayMin4 = Integer.parseInt(alarmData.get("reDelayMin4").toString());
				alarmData.put("re_delay_time4", reDelayHour4 + reDelayMin4);
			}
			
			if(alarmData.get("reDelayHour5") != null && alarmData.get("reDelayMin5") != null) {
				int reDelayHour5 = Integer.parseInt(alarmData.get("reDelayHour5").toString());
				int reDelayMin5 = Integer.parseInt(alarmData.get("reDelayMin5").toString());
				alarmData.put("re_delay_time5", reDelayHour5 + reDelayMin5);
			}
			
			// 알람 설정 저장
			adminMapper.saveAlarmSetting(alarmData);
			
			logger.info("알람 설정 저장 완료");
		} catch(Exception e) {
			logger.error("알람 설정 저장 실패: {}", e.toString(), e);
			throw new RuntimeException("알람 설정 저장 실패: " + e.getMessage());
		}
	}
	
	@Override
	public Map<String, Object> getSensorSettings(String sensorUuid) {
		try {
			logger.info("센서 설정값 조회 시작 - sensorUuid: {}", sensorUuid);
			
			Map<String, Object> param = new HashMap<>();
			param.put("sensorUuid", sensorUuid);
			
			// 센서 설정값 조회
			Map<String, Object> settings = adminMapper.selectSetting(param);
			
			if (settings != null && !settings.isEmpty()) {
				logger.info("센서 설정값 조회 성공 - sensorUuid: {}, 설정값: {}", sensorUuid, settings);
				return settings;
			} else {
				logger.warn("센서 설정값이 없음 - sensorUuid: {}", sensorUuid);
				return new HashMap<>();
			}
			
		} catch (Exception e) {
			logger.error("센서 설정값 조회 실패 - sensorUuid: {}, 오류: {}", sensorUuid, e.getMessage(), e);
			return new HashMap<>();
		}
	}
}
