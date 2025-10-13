package com.andrew.hnt.api.service.impl;

import com.andrew.hnt.api.mapper.MqttMapper;
import com.andrew.hnt.api.model.SensorVO;
import com.andrew.hnt.api.service.MqttService;
import com.andrew.hnt.api.util.MqttMessageValidator;
import com.andrew.hnt.api.service.UnifiedDataConsistencyService;
import com.andrew.hnt.api.mqtt.MqttHealthChecker;
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
	private UnifiedDataConsistencyService unifiedDataConsistencyService;
	
	@Autowired
	private MqttHealthChecker healthChecker;

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
    
    /**
     * 메시지를 SensorVO로 파싱
     * @param message MQTT 메시지
     * @return SensorVO 객체
     */
    private SensorVO parseMessageToSensorVO(String message) {
        try {
            // JSON 메시지 파싱 로직
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(message);
            
            SensorVO sensorVO = new SensorVO();
            sensorVO.setInstId("hnt");
            sensorVO.setMdfId("hnt");
            
            // JSON에서 필요한 필드 추출
            if (jsonNode.has("uuid")) {
                sensorVO.setUuid(jsonNode.get("uuid").asText());
            }
            if (jsonNode.has("sensorValue")) {
                sensorVO.setSensorValue(jsonNode.get("sensorValue").asText());
            }
            if (jsonNode.has("userId")) {
                sensorVO.setUserId(jsonNode.get("userId").asText());
            }
            
            return sensorVO;
            
        } catch (Exception e) {
            logger.warn("메시지 파싱 중 오류 발생 - message: {}", message, e);
            return null;
        }
    }
    
    /**
     * 메시지 ID 생성
     * @param sensorVO 센서 데이터
     * @return 메시지 ID
     */
    private String generateMessageId(SensorVO sensorVO) {
        return String.format("%s_%d", sensorVO.getUuid(), System.currentTimeMillis());
    }

    @Override
    public void receiveData(String str) {
        // 입력값 검증
        if (str == null || str.trim().isEmpty()) {
            logger.warn("MQTT 메시지가 비어있습니다.");
            return;
        }
        
        // 메시지 길이 검증
        if (!messageValidator.isValidMessageLength(str)) {
            logger.warn("MQTT 메시지가 너무 깁니다: {} bytes", str.length());
            return;
        }
        
        // 보안 위협 검증
        if (messageValidator.hasSecurityThreat(str)) {
            logger.warn("보안 위협이 감지된 MQTT 메시지: {}", str);
            return;
        }
        
        // 메시지 정제 (보안 강화)
        str = messageValidator.sanitizeMessage(str);
        
        // 헬스 체커에 메시지 수신 기록
        if (healthChecker != null) {
            healthChecker.recordMessageReceived();
        }
        
        // 통합 데이터 일관성 서비스를 통한 메시지 처리
        try {
            // 1. 메시지 파싱 및 SensorVO 생성
            SensorVO sensorVO = parseMessageToSensorVO(str);
            if (sensorVO == null) {
                logger.warn("메시지 파싱 실패 - 메시지: {}", str);
                return;
            }
            
            // 2. 메시지 ID 생성
            String messageId = generateMessageId(sensorVO);
            
            // 3. 일관성 있는 메시지 처리
            Map<String, Object> result = unifiedDataConsistencyService.processMessageWithConsistency(
                messageId, sensorVO, System.currentTimeMillis());
            
            if (!"200".equals(result.get("resultCode"))) {
                logger.warn("메시지 일관성 처리 실패 - messageId: {}, error: {}", 
                           messageId, result.get("resultMessage"));
            } else {
                logger.debug("메시지 일관성 처리 성공 - messageId: {}, uuid: {}", 
                           messageId, sensorVO.getUuid());
            }
            
        } catch (Exception e) {
            logger.error("통합 데이터 일관성 서비스 처리 중 오류 발생", e);
            
            // 기존 방식으로 폴백 처리
            if (loginService != null) {
                loginService.insertData(str);
            }
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
    			mqttMapper.insertSensorData(sensorVO);
    		} catch(Exception e) {
    			logger.error("Error : " + e.toString());
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

}


