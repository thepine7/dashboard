package com.andrew.hnt.api.service.impl;

import com.andrew.hnt.api.mapper.MqttMapper;
import com.andrew.hnt.api.model.SensorVO;
import com.andrew.hnt.api.service.MqttService;
import com.andrew.hnt.api.util.MqttMessageValidator;
import com.andrew.hnt.api.mqtt.MqttHealthChecker;
import com.andrew.hnt.api.mqtt.MqttMessageProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.util.concurrent.locks.ReentrantLock;

@Service
@Transactional(timeout = 30, rollbackFor = Exception.class)
public class MqttServiceImpl implements MqttService {

    @Autowired
    private MqttMapper mqttMapper;
    
	@Autowired
	private MqttMessageValidator messageValidator;
	
	@Autowired
	private MqttHealthChecker healthChecker;
	
	@Autowired(required = false)
	@Lazy
	private MqttMessageProcessor messageProcessor;

    @Autowired
    @Lazy
    private LoginServiceImpl loginService;

    private static final Logger logger = LoggerFactory.getLogger(MqttServiceImpl.class);

    public SensorVO sensorVO1 = new SensorVO();
    public static ArrayList<String> data = new ArrayList<String>();
    public static String title = "";
    public static String name = "";
    public static String type = "";
    public static String dinVal = "";
    public static String outputVal = "";
    public static String sensorId = "";
    public static String sensorUuid = "";
    public static String sensorType = "";
    public static SensorVO sensorVO = new SensorVO();

    private String apiKey = "AAAAoUCvVY0:APA91bFhv_a-RRU0OOJPmGk4MBri_Aqu0MW4r1CDfar4GrhQf3H9XPTWRhoul86dfhLTomTn-WsTrKJ-qPAakoap9vMl7JHmrj8WniVnTQE3y5mhxKFDPp09bAmjaAuDx8qUXH1qhO05";
    private String senderId = "692574967181";

    private ReentrantLock listLock = new ReentrantLock();

    public String setSensorValue(ArrayList<String> data, SensorVO sensorVO) throws Exception {
        MqttServiceImpl.data = data;
        MqttServiceImpl.title = sensorVO.getUserId();
        MqttServiceImpl.name = sensorVO.getName();
        MqttServiceImpl.type = sensorVO.getType();
        MqttServiceImpl.dinVal = sensorVO.getDinVal();
        MqttServiceImpl.outputVal = sensorVO.getOutputVal();
        MqttServiceImpl.sensorId = sensorVO.getSensorId();
        MqttServiceImpl.sensorUuid = sensorVO.getUuid();
        MqttServiceImpl.sensorType = sensorVO.getSensorType();
        MqttServiceImpl.sensorVO = sensorVO;

        Iterator<String> itrData = data.iterator();
        while(null != data && itrData.hasNext()) {
            listLock.lock();
            try {
                return data.toString();
            } finally {
                listLock.unlock();
            }
        }

        return null;
    }

    @Override
    public Map<String, Object> getData() throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();

        resultMap.put("data", data.toString());
        resultMap.put("title", title);
        resultMap.put("sensorId", sensorId);
        resultMap.put("sensorUuid", sensorUuid);
        resultMap.put("sensorType", sensorType);
        resultMap.put("sensorVO", sensorVO);
        resultMap.put("name", name);
        resultMap.put("type", type);
        resultMap.put("dinVal", dinVal);
        resultMap.put("outputVal", outputVal);

        return resultMap;
    }
    
    @Override
    public void receiveData(String str) {
        logger.info("=== MqttServiceImpl.receiveData() 호출됨 ===");
        logger.info("원본 메시지: {}", str);
        
        // 입력값 검증
        if (str == null || str.trim().isEmpty()) {
            logger.warn("MQTT 메시지가 비어있습니다.");
            return;
        }
        logger.info("✅ 입력값 검증 통과");
        
        // 메시지 길이 검증
        if (!messageValidator.isValidMessageLength(str)) {
            logger.warn("MQTT 메시지가 너무 깁니다: {} bytes", str.length());
            return;
        }
        logger.info("✅ 메시지 길이 검증 통과");
        
        // 보안 위협 검증
        if (messageValidator.hasSecurityThreat(str)) {
            logger.warn("보안 위협이 감지된 MQTT 메시지: {}", str);
            return;
        }
        logger.info("✅ 보안 위협 검증 통과");
        
        // JSON 구조가 필요한 메시지는 sanitize 건너뛰기
        boolean isDeviceRegistration = str.contains("\"actcode\":\"reg\"") || str.contains("actcode:reg");
        boolean isLiveMessage = str.contains("\"actcode\":\"live\"") || str.contains("actcode:live");
        
        if (!isDeviceRegistration && !isLiveMessage) {
            // 메시지 정제 (보안 강화) - JSON 구조가 필요 없는 메시지만
            str = messageValidator.sanitizeMessage(str);
            logger.info("정제된 메시지: {}", str);
        } else {
            if (isDeviceRegistration) {
                logger.info("장치 등록 메시지 - sanitize 건너뛰기 (JSON 구조 보존)");
            }
            if (isLiveMessage) {
                logger.info("실시간 데이터 메시지 - sanitize 건너뛰기 (JSON 구조 보존)");
            }
        }
        
        // 헬스 체커에 메시지 수신 기록
        if (healthChecker != null) {
            healthChecker.recordMessageReceived();
        }
        
        logger.info("=== 검증 완료 - 메시지 처리 시작 ===");
        
        // actcode: "live" 메시지 처리 (MqttMessageProcessor로 전달)
        if (str.contains("\"actcode\":\"live\"") || str.contains("actcode:live")) {
            logger.info("실시간 데이터 메시지 감지 - MqttMessageProcessor로 전달");
            handleLiveMessage(str);
            return;
        }
        
        // 장치 등록 메시지 처리 (actcode: "reg" 또는 actcode:reg)
        if (str.contains("\"actcode\":\"reg\"") || str.contains("actcode:reg")) {
            logger.info("=== 장치 등록 메시지 감지 ===");
            logger.info("메시지: {}", str);
            handleDeviceRegistration(str);
            return;
        }
        
        logger.info("기타 메시지 처리 - 폴백 로직");
        
        // 기타 메시지는 기존 방식으로 폴백 처리
        if (loginService != null) {
            loginService.insertData(str);
        }
        
        if(null != str && !"".equals(str) && 0 < str.length()) {
            if(str.contains("@")) {
                ObjectMapper mapper = new ObjectMapper();

                String[] strArr = str.split("@");

                if(null != strArr && 0 < strArr.length) {
                	sensorVO.setTopic(strArr[0]);
                	sensorVO.setRawData(strArr[1]);
                    try {
                        String[] topicArr = strArr[0].split("/");
                        
                        // 토픽 검증
                        if (!messageValidator.isValidTopic(strArr[0])) {
                            logger.warn("잘못된 MQTT 토픽 형식: {}", strArr[0]);
                            return;
                        }
                        
                        Map<String, Object> valueMap = new HashMap<String, Object>();
                        for(int i=0; i < topicArr.length; i++) {
                        }

                        if(!str.contains("temperature")) {
                        	if(isValidJson(strArr[1])) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> tempValueMap = mapper.readValue(strArr[1], Map.class);
                                
                                // JSON 메시지 검증
                                if (!messageValidator.isValidJsonMessage(tempValueMap)) {
                                    logger.warn("잘못된 JSON 메시지 형식: {}", strArr[1]);
                                    return;
                                }
                                
                                valueMap = tempValueMap;
                                if(topicArr.length > 2) { sensorVO.setSensorType(topicArr[2]); }
                        	} else {
                        		topicArr = null;
                        	}
                        } else {
                            String[] tempArr = strArr[0].split("/");
                            sensorVO.setSensorValue(strArr[1]);    // Set sensor value
                            if(null != tempArr) {
                                if(null != tempArr && tempArr.length > 3) { sensorVO.setSensorType(tempArr[3]); }
                            }
                        }

                        if(null != topicArr && 0 < topicArr.length && !str.contains("register")) {
                            if(topicArr.length > 1) { sensorVO.setUserId(topicArr[1]); }    // Set user ID
                            if(topicArr.length > 1) { sensorVO.setSensorId(topicArr[1]); }    // Set sensor ID
                            if(topicArr.length > 3) { sensorVO.setUuid(topicArr[3]); }    // Set UUID
                        }

                        if(null != valueMap && 0 < valueMap.size()) {
                            // value 변수를 미리 선언하여 모든 조건문에서 사용 가능하도록 함
                            Object value = valueMap.get("value");
                            
                        	if(valueMap.containsKey("actcode")) {
                        		if("setres".equals(valueMap.get("actcode"))) {
                        			if(2 < valueMap.size()) {
                        				sensorVO.setDataJson(strArr[1]);
                        			}
                        		} else if("live".equals(valueMap.get("actcode"))) {
                                    if(null != value && !"".equals(String.valueOf(value))) {
                                        if(!"null".equals(String.valueOf(value))) {
                                            sensorVO.setSensorValue(String.valueOf(value));    // Set sensor value
                                        } else {
                                            sensorVO.setSensorValue("0");
                                        }
                                    }

                                    sensorVO.setName(String.valueOf(valueMap.get("name")));
                                    sensorVO.setType(String.valueOf(valueMap.get("type")));

                        		} else if("din".equals(valueMap.get("name"))) {
                        		    if("1".equals(valueMap.get("type"))) {
                                        sensorVO.setDinVal(String.valueOf(value));
                                    }
                                } else if("output".equals(valueMap.get("name"))) {
                        		    if("1".equals(String.valueOf(valueMap.get("type")))) {
                                                // COMP setting
                                        sensorVO.setOutputVal("comp/" + String.valueOf(value));
                                    } else if("2".equals(String.valueOf(valueMap.get("type")))) {
                        		        // DEF setting
                                        sensorVO.setOutputVal("def/" + String.valueOf(value));
                                    } else if("3".equals(String.valueOf(valueMap.get("type")))) {
                        		        // FAN setting
                                        sensorVO.setOutputVal("fan/" + String.valueOf(value));
                                    }
                                }
                        	}
                        }

                        if(null != sensorVO.getSensorValue() && !"".equals(sensorVO.getSensorValue())) {
                            listLock.lock();
                            try {
                                data = new ArrayList<>();
                                data.add(sensorVO.getSensorValue());
                                this.setSensorValue(data, sensorVO);
                                if (loginService != null) {
                                    loginService.setData(sensorVO);
                                }
                            } finally {
                                listLock.unlock();
                            }
                        }

                    } catch(Exception e) {
                        logger.error("Error processing MQTT message", e);
                        logger.error("Error : " + e.toString());
                    }
                }
            }
        }
    }
    
    public boolean isValidJson(String str) {
    	boolean result = false;
    	
    	ObjectMapper mapper = new ObjectMapper();
    	
    	try {
    		mapper.readTree(str);
    		result = true;
    	} catch(Exception e) {
    		result = false;
    	}
    	
    	return result;
    }
    
    @Override
    public void insertSensorData(SensorVO sensorVO) throws Exception {
    	if(null != sensorVO) {
    		try {
    			// 1. DB에 센서 데이터 저장
    			mqttMapper.insertSensorData(sensorVO);
    			
    			// 2. MqttMessageProcessor로 메시지 전달 (알람 체크용)
    			if(messageProcessor != null && "ain".equals(sensorVO.getSensorType())) {
    				try {
    					messageProcessor.addMessage(sensorVO);
    					logger.debug("센서 데이터를 MqttMessageProcessor로 전달 - UUID: {}, Value: {}", 
    						sensorVO.getUuid(), sensorVO.getSensorValue());
    				} catch(Exception e) {
    					logger.error("MqttMessageProcessor 전달 실패 - UUID: {}", sensorVO.getUuid(), e);
    				}
    			}
    		} catch(Exception e) {
    			logger.error("센서 데이터 저장 실패: " + e.toString());
    		}
    	}
    }

    public void sendNoti(Map<String, Object> noti) {
        if(null != noti && 0 < noti.size()) {
            OkHttpClient client = new OkHttpClient.Builder().build();

            okhttp3.RequestBody body = new FormBody.Builder()
                    .add("to", String.valueOf(noti.get("token")))
                    .add("project_id", senderId)
                    .add("notification", "")
                    .add("data", String.valueOf(noti.get("sensor_uuid")) + " device error occurred : " + String.valueOf(noti.get("type")))
                    .build();

            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .addHeader("Authorization", "key=" + apiKey)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    logger.error("Error");
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        logger.info("Success : " + response.code() + "/" + response.body().string());
                    } else {
                        logger.info("Fail : " + response.code() + "/" + response.body().string());
                    }
                }
            });
        }
    }
    
    /**
     * 장치 등록 메시지 처리 (actcode: "reg")
     * @param message MQTT 메시지 (JSON 형식)
     */
    private void handleDeviceRegistration(String message) {
        try {
            logger.info("=== 장치 등록 처리 시작 ===");
            logger.debug("수신 메시지: {}", message);
            
            // 1. JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(message);
            
            // 필수 필드 추출 (userid 또는 userId 모두 허용)
            String userId = null;
            if (json.has("userid")) {
                userId = json.get("userid").asText();
            } else if (json.has("userId")) {
                userId = json.get("userId").asText();
            }
            
            if (userId == null || !json.has("model") || !json.has("mac")) {
                logger.error("장치 등록 실패 - 필수 필드 누락: {}", message);
                return;
            }
            
            String model = json.get("model").asText();
            String mac = json.get("mac").asText();
            
            logger.info("장치 등록 정보 - userId: {}, model: {}, mac: {}", userId, model, mac);
            
            // 2. 중복 등록 체크 (현재 사용자 + UUID 조합)
            Map<String, Object> checkParam = new HashMap<>();
            checkParam.put("userId", userId);
            checkParam.put("sensorUuid", mac);
            Map<String, Object> existing = mqttMapper.getSensorInfo(checkParam);
            
            logger.debug("중복 체크 결과 - existing: {}", existing);
            
            if (existing != null && existing.size() > 0) {
                Object cntObj = existing.get("cnt");
                logger.debug("cnt 값: {}, 타입: {}", cntObj, cntObj != null ? cntObj.getClass().getName() : "null");
                
                int cnt = 0;
                if (cntObj instanceof Integer) {
                    cnt = (Integer) cntObj;
                } else if (cntObj instanceof Long) {
                    cnt = ((Long) cntObj).intValue();
                } else if (cntObj instanceof String) {
                    cnt = Integer.parseInt((String) cntObj);
                }
                
                if (cnt > 0) {
                    // 이미 등록됨 - REG&value=1 응답만 전송, mdf_dtm 업데이트
                    logger.info("이미 등록된 장치 - userId: {}, mac: {}, cnt: {}", userId, mac, cnt);
                    mqttMapper.updateSensorLastAccessTime(checkParam);
                    sendRegResponse(userId, model, mac);
                    return;
                }
            }
            
            logger.info("신규 장치 등록 진행 - userId: {}, mac: {}", userId, mac);
            
            // 3. 다른 사용자 소유 여부 확인 (장치 이전)
            Map<String, Object> ownerCheck = new HashMap<>();
            ownerCheck.put("sensorUuid", mac);
            Map<String, Object> existingOwner = mqttMapper.getSensorInfoByUuid(ownerCheck);
            
            if (existingOwner != null && existingOwner.size() > 0) {
                String oldUserId = String.valueOf(existingOwner.get("user_id"));
                
                if (!oldUserId.equals(userId)) {
                    // 다른 사용자가 소유하고 있음 - 장치 전송 처리
                    logger.info("장치 이전 감지 - 기존: {}, 신규: {}, mac: {}", oldUserId, userId, mac);
                    
                    // 기존 소유자 데이터 삭제 (동기)
                    mqttMapper.deleteSensorInfoByUuid(ownerCheck);
                    mqttMapper.deleteConfigByUuid(ownerCheck);
                    mqttMapper.deleteAlarmByUuid(ownerCheck);
                    
                    logger.info("기존 소유자 장치 정보 삭제 완료 - userId: {}, mac: {}", oldUserId, mac);
                    
                    // 센서 데이터 비동기 삭제 (대용량 데이터)
                    final String finalMac = mac;
                    final String finalOldUserId = oldUserId;
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            logger.info("장치 이전 - 센서 데이터 비동기 삭제 시작 - 기존 소유자: {}, mac: {}", finalOldUserId, finalMac);
                            
                            int batchSize = 1000; // 한 번에 1,000개씩 삭제
                            int deletedCount = 0;
                            int totalDeleted = 0;
                            
                            do {
                                Map<String, Object> asyncParam = new HashMap<>();
                                asyncParam.put("sensorUuid", finalMac);
                                asyncParam.put("batchSize", batchSize);
                                
                                deletedCount = mqttMapper.deleteSensorDataBatch(asyncParam);
                                totalDeleted += deletedCount;
                                
                                if (totalDeleted % 10000 == 0 && totalDeleted > 0) {
                                    logger.info("장치 이전 - 센서 데이터 비동기 삭제 진행 중 - 삭제된 개수: {}, 총 삭제: {}, mac: {}", 
                                        deletedCount, totalDeleted, finalMac);
                                }
                                
                                // DB 부하 방지
                                if (deletedCount > 0) {
                                    try { Thread.sleep(10); } catch (InterruptedException ie) {}
                                }
                            } while (deletedCount > 0);
                            
                            logger.info("장치 이전 - 센서 데이터 비동기 삭제 완료 - 기존 소유자: {}, mac: {}, 총 삭제: {}", 
                                finalOldUserId, finalMac, totalDeleted);
                                
                        } catch (Exception e) {
                            logger.error("장치 이전 - 센서 데이터 비동기 삭제 중 오류 발생 - mac: " + finalMac, e);
                        }
                    });
                }
            }
            
            // 4. 신규 장치 등록
            Map<String, Object> param = new HashMap<>();
            param.put("userId", userId);
            param.put("sensorId", userId);
            param.put("sensorUuid", mac);
            param.put("sensorType", model);
            param.put("instId", "mqtt_auto");
            param.put("mdfId", "mqtt_auto");
            
            logger.info("장치 기본 정보 저장 시작 - userId: {}, mac: {}", userId, mac);
            mqttMapper.insertSensorInfo(param);
            logger.info("장치 기본 정보 저장 완료 - userId: {}, mac: {}", userId, mac);
            
            // 5. 기본 설정 저장 (hnt_config)
            param.put("topic", "HBEE/" + userId + "/" + model + "/" + mac + "/SER");
            
            logger.info("장치 기본 설정 저장 시작 - userId: {}, mac: {}", userId, mac);
            mqttMapper.insertDefaultConfig(param);
            logger.info("장치 기본 설정 저장 완료 - userId: {}, mac: {}", userId, mac);
            
            // 6. REG&value=1 응답 전송
            sendRegResponse(userId, model, mac);
            
            // 7. 장치 등록 완료 알림 전송 (앱 자동 갱신용)
            sendDeviceRegisteredNotification(userId, model, mac);
            
            logger.info("=== 장치 등록 완료 - userId: {}, model: {}, mac: {} ===", userId, model, mac);
            
        } catch (Exception e) {
            logger.error("장치 등록 실패", e);
        }
    }
    
    /**
     * 장치 등록 응답 전송 (REG&value=1)
     * @param userId 사용자 ID
     * @param model 장치 모델 (TC, HTC, WIO, EIO 등)
     * @param mac 장치 MAC 주소 (UUID)
     */
    private void sendRegResponse(String userId, String model, String mac) {
        try {
            String topic = "HBEE/" + userId + "/" + model + "/" + mac + "/SER";
            String payload = "REG&value=1";
            
            logger.info("REG 응답 전송 시작 - topic: {}, payload: {}", topic, payload);
            
            // MQTT 클라이언트 생성 및 전송
            com.andrew.hnt.api.mqtt.common.MQTT client = new com.andrew.hnt.api.mqtt.common.MQTT(
                "tcp://iot.hntsolution.co.kr:1883",
                java.util.UUID.randomUUID().toString(),
                "hnt1",
                "abcde"
            );
            
            client.init(topic, "N"); // 구독 불필요
            
            // 연결 확인
            if (client.isConnected()) {
                client.publish(payload, 0, topic);
                logger.info("REG 응답 전송 완료 - topic: {}, payload: {}", topic, payload);
            } else {
                logger.error("REG 응답 전송 실패 - MQTT 연결 실패");
            }
            
            // 연결 해제
            client.disconnect();
            
        } catch (Exception e) {
            logger.error("REG 응답 전송 실패", e);
        }
    }
    
    /**
     * 장치 등록 완료 알림 전송 (앱 자동 갱신용)
     * @param userId 사용자 ID
     * @param model 장치 모델 (TC, HTC, WIO, EIO 등)
     * @param mac 장치 MAC 주소 (UUID)
     */
    private void sendDeviceRegisteredNotification(String userId, String model, String mac) {
        try {
            String notificationTopic = String.format("HBEE/%s/DEVICE_REGISTERED", userId);
            String payload = String.format(
                "{\"actcode\":\"device_registered\",\"mac\":\"%s\",\"model\":\"%s\",\"timestamp\":%d}",
                mac, model, System.currentTimeMillis()
            );
            
            logger.info("=== 장치 등록 알림 전송 시작 ===");
            logger.info("Topic: {}", notificationTopic);
            logger.info("Payload: {}", payload);
            
            // MQTT 클라이언트 생성 (별도 스레드에서 실행)
            new Thread(() -> {
                com.andrew.hnt.api.mqtt.common.MQTT client = null;
                try {
                    logger.info("MQTT 클라이언트 생성 시작...");
                    
                    client = new com.andrew.hnt.api.mqtt.common.MQTT(
                        "tcp://iot.hntsolution.co.kr:1883",
                        "notification_" + System.currentTimeMillis(),
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
                        logger.info("=== 장치 등록 알림 전송 완료 ===");
                        logger.info("Topic: {}", notificationTopic);
                        logger.info("Payload: {}", payload);
                    } else {
                        logger.error("=== 장치 등록 알림 전송 실패 - MQTT 연결 타임아웃 ===");
                    }
                    
                } catch (Exception e) {
                    logger.error("=== 장치 등록 알림 전송 중 예외 발생 ===", e);
                } finally {
                    // 연결 해제
                    if (client != null) {
                        try {
                            Thread.sleep(500); // 메시지 전송 완료 대기
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
            logger.error("=== 장치 등록 알림 전송 실패 ===", e);
            logger.error("userId: {}, model: {}, mac: {}", userId, model, mac);
        }
    }
    
    /**
     * 실시간 데이터 메시지 처리 (actcode: live)
     * @param messageWithTopic 토픽 정보 포함 메시지 (형식: topic@message)
     */
    private void handleLiveMessage(String messageWithTopic) {
        try {
            // 토픽과 메시지 분리
            String[] parts = messageWithTopic.split("@", 2);
            if (parts.length < 2) {
                return;
            }
            
            String topic = parts[0];
            String message = parts[1];
            
            // 토픽에서 userId와 sensorUuid 추출
            // 토픽 형식: HBEE/{userId}/TC/{sensorUuid}/DEV
            String[] topicParts = topic.split("/");
            if (topicParts.length < 4) {
                return;
            }
            
            String userId = topicParts[1];
            String sensorUuid = topicParts[3];
            
            // thepine 사용자만 로그 출력
            boolean isThepine = "thepine".equals(userId);
            
            if (isThepine) {
                logger.info("=== [thepine] 실시간 데이터 메시지 처리 시작 ===");
                logger.info("Topic: {}", topic);
                logger.info("Message: {}", message);
                logger.info("추출된 정보 - userId: {}, sensorUuid: {}", userId, sensorUuid);
            }
            
            // JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonData = mapper.readValue(message, Map.class);
            
            String actcode = String.valueOf(jsonData.get("actcode"));
            String name = String.valueOf(jsonData.get("name"));
            String value = String.valueOf(jsonData.get("value"));
            
            if (isThepine) {
                logger.info("actcode: {}, name: {}, value: {}", actcode, name, value);
            }
            
            // ain (온도) 데이터만 알람 체크
            if ("live".equals(actcode) && "ain".equals(name) && !"Error".equals(value)) {
                if (isThepine) {
                    logger.info("🌡️ [thepine] 온도 데이터 감지 - MqttMessageProcessor로 전달");
                }
                
                // SensorVO 생성
                SensorVO sensorVO = new SensorVO();
                sensorVO.setUserId(userId);
                sensorVO.setSensorId(userId); // 기본값으로 userId 사용
                sensorVO.setUuid(sensorUuid);
                sensorVO.setSensorValue(value);
                sensorVO.setSensorType("ain"); // 온도 센서 타입
                sensorVO.setTopic(topic);
                sensorVO.setRawData(message);
                sensorVO.setInstId(userId); // inst_id 설정 (필수)
                sensorVO.setMdfId(userId);  // mdf_id 설정 (필수)
                
                // MqttMessageProcessor로 전달
                if (messageProcessor != null) {
                    messageProcessor.addMessage(sensorVO);
                    if (isThepine) {
                        logger.info("✅ [thepine] MqttMessageProcessor로 메시지 전달 완료 - UUID: {}, Value: {}", sensorUuid, value);
                    }
                } else {
                    if (isThepine) {
                        logger.warn("[thepine] MqttMessageProcessor가 null입니다");
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("실시간 데이터 메시지 처리 실패", e);
        }
    }

}


