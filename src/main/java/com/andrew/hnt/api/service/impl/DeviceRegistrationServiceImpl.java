package com.andrew.hnt.api.service.impl;

import com.andrew.hnt.api.mapper.AdminMapper;
import com.andrew.hnt.api.mapper.MqttMapper;
import com.andrew.hnt.api.mqtt.common.MQTT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Device 등록 서비스 구현체
 */
@Service
public class DeviceRegistrationServiceImpl {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistrationServiceImpl.class);
    
    @Autowired
    private MqttMapper mqttMapper;
    
    @Autowired
    private AdminMapper adminMapper;
    
	
    
    
    // MQTT 서버 설정
    private static final String MQTT_SERVER = "tcp://hntsolution.co.kr:1883";
    private static final String MQTT_USERNAME = "hnt1";
    private static final String MQTT_PASSWORD = "abcde";
    
	@Transactional
	public boolean processDeviceRegistration(String userId, String model, String mac) {
		try {
			// kimtest 사용자만 로깅 (로그 스팸 방지)
			if("kimtest".equals(userId)) {
				logger.info("=== kimtest DeviceRegistrationService 호출 감지 ===");
				logger.info("Device 등록 시작: userId={}, model={}, mac={}", userId, model, mac);
				logger.info("이 호출은 register 토픽으로 인한 자동 등록일 가능성이 높습니다!");
			}

			// 중복등록 체크: 이미 등록된 장치인지 확인
			Map<String, Object> existingDevice = mqttMapper.getSensorInfoByUuid(createParamMap("sensorUuid", mac));
			if(existingDevice != null && existingDevice.size() > 0) {
				String existingUserId = String.valueOf(existingDevice.get("user_id"));
				if(userId.equals(existingUserId)) {
					logger.info("중복등록 방지: 장치가 이미 {} 사용자에게 등록되어 있음. mac={}", userId, mac);
					// 즉시 응답만 전송하고 등록은 건너뛰기
					sendImmediateRegistrationResponse(userId, model, mac);
					return true;
				} else {
					logger.info("장치 전송 감지: 기존 소유자 {} → 새 소유자 {}. mac={}", existingUserId, userId, mac);
				}
			}

			// 즉시 응답 전송 (수신 즉시)
			boolean immediateResponse = sendImmediateRegistrationResponse(userId, model, mac);
			if (!immediateResponse) {
				logger.error("즉시 응답 전송 실패: userId={}, model={}, mac={}", userId, model, mac);
				// 즉시 응답 실패해도 등록은 계속 진행
			}

			// 1단계: 기존 소유자 데이터 삭제
			boolean deleteSuccess = deleteExistingOwnerData(mac);
			if (!deleteSuccess) {
				logger.error("기존 소유자 데이터 삭제 실패: mac={}", mac);
				return false;
			}

			// 2단계: 새 사용자에게 장치 등록
			boolean transferSuccess = transferDevice(userId, model, mac);
			if (!transferSuccess) {
				logger.error("장치 전송 실패: userId={}, model={}, mac={}", userId, model, mac);
				return false;
			}

			// 3단계: MQTT 응답 전송
			boolean responseSuccess = sendRegistrationResponse(userId, model, mac);
			if (!responseSuccess) {
				logger.error("MQTT 응답 전송 실패: userId={}, model={}, mac={}", userId, model, mac);
				return false;
			}

			// 4단계: 알람설정 초기값 설정 (장치전송 시에도 새로 설정)
			if("kimtest".equals(userId) || "thepine".equals(userId)) {
				logger.info("=== 알람설정 초기값 설정 시작 전: userId={}, mac={} ===", userId, mac);
			}
			
			// 메서드 호출 전 파라미터 확인
			logger.info("=== setAlarmSettingDefaults 호출 전 파라미터 확인: userId={}, mac={} ===", userId, mac);
			
			boolean alarmSettingSuccess = setAlarmSettingDefaults(userId, mac);
			
			// 메서드 호출 후 결과 확인
			logger.info("=== setAlarmSettingDefaults 호출 후 결과: success={}, userId={}, mac={} ===", alarmSettingSuccess, userId, mac);
			
			if (!alarmSettingSuccess) {
				logger.error("알람설정 초기값 설정 실패: userId={}, mac={}", userId, mac);
				// 알람설정 실패해도 장치 등록은 성공으로 처리
			} else {
				if("kimtest".equals(userId) || "thepine".equals(userId)) {
					logger.info("=== 알람설정 초기값 설정 성공: userId={}, mac={} ===", userId, mac);
				}
			}

			// 5단계: 장치 등록 완료 후 자동으로 설정값과 상태 읽기 (비동기로 실행)
			if("kimtest".equals(userId)) {
				logger.info("장치 등록 완료 후 자동 설정값/상태 읽기 시작: userId={}, mac={}", userId, mac);
			}
			
			// 비동기로 실행하여 메인 스레드 블로킹 방지 (스레드 풀 사용)
			CompletableFuture.runAsync(() -> {
				try {
					if("kimtest".equals(userId)) {
						logger.info("장치 읽기 스레드 시작: userId={}, mac={}", userId, mac);
					}
					
					// GET&type=1 (설정값 읽기) - 2초 후 실행
					if("kimtest".equals(userId)) {
						logger.info("2초 후 GET&type=1 실행 대기 중...");
					}
					Thread.sleep(2000);
					
					if("kimtest".equals(userId)) {
						logger.info("GET&type=1 실행 시작");
					}
					executeDeviceRead(userId, mac, "GET&type=1", "설정값");
					
					// GET&type=2 (상태 읽기) - 2초 후 실행
					if("kimtest".equals(userId)) {
						logger.info("2초 후 GET&type=2 실행 대기 중...");
					}
					Thread.sleep(2000);
					
					if("kimtest".equals(userId)) {
						logger.info("GET&type=2 실행 시작");
					}
					executeDeviceRead(userId, mac, "GET&type=2", "상태");
					
					if("kimtest".equals(userId)) {
						logger.info("장치 읽기 스레드 완료: userId={}, mac={}", userId, mac);
					}
					
				} catch (Exception e) {
					if("kimtest".equals(userId)) {
						logger.error("장치 읽기 중 예외 발생: userId={}, mac={}", userId, mac, e);
					}
				}
			});

			if("kimtest".equals(userId)) {
				logger.info("kimtest Device 등록 완료: userId={}, model={}, mac={}", userId, model, mac);
			}
			return true;

		} catch (Exception e) {
			logger.error("Device 등록 중 오류 발생: userId={}, model={}, mac={}", userId, model, mac, e);
			return false;
		}
	}
    
    @Transactional
    public boolean transferDevice(String newUserId, String model, String mac) {
        try {
            logger.info("장치 전송 시작: newUserId={}, model={}, mac={}", newUserId, model, mac);
            
            // 새 사용자에게 장치 등록
            Map<String, Object> param = new HashMap<>();
            param.put("userId", newUserId);
            param.put("sensorId", newUserId);
            param.put("sensorUuid", mac);
            param.put("sensorType", model);
            param.put("instId", "hnt");
            param.put("mdfId", "hnt");
            
            logger.info("장치 기본 정보 삽입 시작: param={}", param);
            mqttMapper.insertSensorInfo(param);
            logger.info("장치 기본 정보 삽입 완료");
            
            // 기본 설정 정보 생성
            Map<String, Object> configParam = new HashMap<>();
            configParam.put("userId", newUserId);
            configParam.put("sensorId", newUserId);
            configParam.put("sensorUuid", mac);
            configParam.put("topic", String.format("HBEE/%s/%s/%s", newUserId, model, mac));
            
            logger.info("기본 설정 정보 삽입 시작: configParam={}", configParam);
            mqttMapper.insertDefaultConfig(configParam);
            logger.info("기본 설정 정보 삽입 완료");
            
            logger.info("장치 전송 완료: newUserId={}, model={}, mac={}", newUserId, model, mac);
            return true;
            
        } catch (Exception e) {
            logger.error("장치 전송 중 오류 발생: newUserId={}, model={}, mac={}", newUserId, model, mac, e);
            return false;
        }
    }
    
    @Transactional
    public boolean deleteExistingOwnerData(String mac) {
        try {
            logger.info("기존 소유자 데이터 삭제 시작: mac={}", mac);
            
            // 1. 기존 사용자의 장치 기본 정보 삭제
            mqttMapper.deleteSensorInfoByUuid(createParamMap("sensorUuid", mac));
            
            // 2. 기존 사용자의 장치 설정 정보 삭제
            mqttMapper.deleteConfigByUuid(createParamMap("sensorUuid", mac));
            
            // 3. 기존 사용자의 센서 데이터 삭제
            mqttMapper.deleteSensorDataByUuid(createParamMap("sensorUuid", mac));
            
            // 4. 기존 사용자의 알림 데이터 삭제
            mqttMapper.deleteAlarmByUuid(createParamMap("sensorUuid", mac));
            
            logger.info("기존 소유자 데이터 삭제 완료: mac={}", mac);
            return true;
            
        } catch (Exception e) {
            logger.error("기존 소유자 데이터 삭제 중 오류 발생: mac={}", mac, e);
            return false;
        }
    }
    
    	/**
	 * 즉시 응답 전송 (수신 즉시)
	 */
	private boolean sendImmediateRegistrationResponse(String userId, String model, String mac) {
		try {
			// 응답 메시지 생성: REG&value=1 (JSON이 아닌 일반 문자열)
			String responseMessage = "REG&value=1";
			
			// 응답 토픽: HBEE/($userId)/TC(model)/($mac)/SER
			String responseTopic = String.format("HBEE/%s/%s/%s/SER", userId, model, mac);
			
			// MQTT 클라이언트 생성 및 즉시 응답 전송
			String clientId = "hnt-server-immediate-" + System.currentTimeMillis();
			MQTT mqttClient = new MQTT(MQTT_SERVER, clientId, MQTT_USERNAME, MQTT_PASSWORD);
			
			// MQTT 클라이언트 초기화 (구독 토픽은 필요 없으므로 빈 문자열)
			mqttClient.init("", "N");
			
			// 즉시 응답 전송
			mqttClient.publish(responseMessage, 0, responseTopic);
			boolean sendSuccess = true; // publish는 void이므로 성공으로 간주
			
			if (sendSuccess) {
				logger.info("즉시 응답 전송 성공: topic={}, message={}", responseTopic, responseMessage);
			} else {
				logger.error("즉시 응답 전송 실패: topic={}, message={}", responseTopic, responseMessage);
			}
			
			return sendSuccess;
			
		} catch (Exception e) {
			logger.error("즉시 응답 전송 중 오류 발생: userId={}, model={}, mac={}", userId, model, mac, e);
			return false;
		}
	}

	/**
	 * MQTT 응답 전송 (등록 완료)
	 */
	private boolean sendRegistrationResponse(String userId, String model, String mac) {
        try {
            // 응답 메시지 생성: REG&value=1
            String responseMessage = "REG&value=1";
            
            // 응답 토픽: HBEE/($userId)/TC(model)/($mac)/SER
            String responseTopic = String.format("HBEE/%s/%s/%s/SER", userId, model, mac);
            
            // MQTT 클라이언트 생성 및 응답 전송
            String clientId = "hnt-server-" + System.currentTimeMillis();
            MQTT mqttClient = new MQTT(MQTT_SERVER, clientId, MQTT_USERNAME, MQTT_PASSWORD);
            
            // MQTT 클라이언트 초기화 (구독 토픽은 필요 없으므로 빈 문자열)
            mqttClient.init("", "N");
            
            // 응답 전송
            mqttClient.publish(responseMessage, 0, responseTopic);
            
            if("kimtest".equals(userId)) {
                logger.info("MQTT 응답 전송 성공: topic={}, message={}", responseTopic, responseMessage);
            }
            
            return true;
            
        } catch (Exception e) {
            if("kimtest".equals(userId)) {
                logger.error("MQTT 응답 전송 중 오류 발생: userId={}, model={}, mac={}", userId, model, mac, e);
            }
            return false;
        }
    }
    
    /**
     * 파라미터 맵 생성 헬퍼 메서드
     */
    private Map<String, Object> createParamMap(String key, Object value) {
        Map<String, Object> param = new HashMap<>();
        param.put(key, value);
        return param;
    }

	/**
	 * 장치 설정값/상태 읽기 실행 (동기 방식)
	 */
	private void executeDeviceRead(String userId, String mac, String payload, String readType) {
		try {
			String sendTopic = String.format("HBEE/%s/TC/%s/SER", userId, mac);
			String subscribeTopic = String.format("HBEE/%s/TC/%s/DEV", userId, mac);
			
			// MQTT 클라이언트 생성 및 메시지 전송
			com.andrew.hnt.api.mqtt.common.MQTT client = new com.andrew.hnt.api.mqtt.common.MQTT(
				"tcp://hntsolution.co.kr:1883", 
				UUID.randomUUID().toString(), 
				"hnt1", 
				"abcde"
			);
			
			client.init(subscribeTopic, "Y");
			client.publish(payload, 0, sendTopic);
			
			if("kimtest".equals(userId)) {
				logger.info("자동 {} 읽기 완료: {} → {}", readType, payload, sendTopic);
			}
			
			// 응답 수신 (선택적)
			String response = client.getMsg();
			if(response != null && !response.isEmpty()) {
				if("kimtest".equals(userId)) {
					logger.info("{} 읽기 응답: {}", readType, response);
				}
			}
			
		} catch (Exception e) {
			if("kimtest".equals(userId)) {
				logger.error("자동 {} 읽기 실패: userId={}, mac={}", readType, userId, mac, e);
			}
		}
	}

	/**
	 * 알람설정 초기값 설정 (장치전송 시에도 새로 설정)
	 */
	private boolean setAlarmSettingDefaults(String userId, String mac) {
		logger.info("=== setAlarmSettingDefaults 메서드 진입: userId={}, mac={} ===", userId, mac);
		
		try {
			// 알람설정 초기값 설정
			Map<String, Object> defaultSettings = new HashMap<>();
			defaultSettings.put("userId", userId);
			defaultSettings.put("sensorId", userId);
			defaultSettings.put("sensorUuid", mac);
			
			// 알람 사용 여부 (기본값: 미사용)
			defaultSettings.put("alarmYn1", "N");  // 고온알람
			defaultSettings.put("alarmYn2", "N");  // 저온알람
			defaultSettings.put("alarmYn3", "N");  // 특정온도알람
			defaultSettings.put("alarmYn4", "N");  // DI알람
			defaultSettings.put("alarmYn5", "N");  // 통신이상알람
			
			// 알람 설정값 (기본값)
			defaultSettings.put("setVal1", "20");  // 고온설정값
			defaultSettings.put("setVal2", "10");  // 저온설정값
			defaultSettings.put("setVal3", "15");  // 특정온도설정값
			defaultSettings.put("setVal4", "");    // DI설정값 (빈문자열)
			
			// 지연시간 (기본값: 0분)
			defaultSettings.put("delay_time1", 0);  // 고온지연시간
			defaultSettings.put("delay_time2", 0);  // 저온지연시간
			defaultSettings.put("delay_time3", 0);  // 특정온도지연시간
			defaultSettings.put("delay_time4", 0);  // DI지연시간
			defaultSettings.put("delay_time5", 0);  // 통신이상지연시간
			
			// 재지연시간 (기본값: 0분)
			defaultSettings.put("re_delay_time1", 0);  // 고온재지연시간
			defaultSettings.put("re_delay_time2", 0);  // 저온재지연시간
			defaultSettings.put("re_delay_time3", 0);  // 특정온도재지연시간
			defaultSettings.put("re_delay_time4", 0);  // DI재지연시간
			defaultSettings.put("re_delay_time5", 0);  // 통신이상재지연시간
			
			logger.info("알람설정 초기값 생성 완료: {}", defaultSettings);
			
			// DB에 알람설정 초기값 저장
			adminMapper.saveAlarmSetting(defaultSettings);
			
			logger.info("알람설정 초기값 DB 저장 완료: userId={}, mac={}", userId, mac);
			return true;
			
		} catch (Exception e) {
			logger.error("알람설정 초기값 설정 실패: userId={}, mac={}, error={}", userId, mac, e.getMessage(), e);
			return false;
		}
	}
	
}
