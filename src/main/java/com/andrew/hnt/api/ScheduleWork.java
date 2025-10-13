package com.andrew.hnt.api;

import com.andrew.hnt.api.mapper.AdminMapper;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@EnableAsync
@Service
public class ScheduleWork {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleWork.class);

    @Autowired
    private AdminMapper adminMapper;

    private String apiKey = "AAAAoUCvVY0:APA91bFhv_a-RRU0OOJPmGk4MBri_Aqu0MW4r1CDfar4GrhQf3H9XPTWRhoul86dfhLTomTn-WsTrKJ-qPAakoap9vMl7JHmrj8WniVnTQE3y5mhxKFDPp09bAmjaAuDx8qUXH1qhO05";
    private String senderId = "692574967181";

    @Async("taskExecutor")
    @Scheduled(fixedRate = 60000)
    public void runSchedule() throws Exception {
        logger.info("스케줄러 테스트 실행");

        List<Map<String, Object>> notiList = new ArrayList<Map<String, Object>>();
        Map<String, Object> sensorInfo = new HashMap<String, Object>();
        String sensorName = "";

        try {
            notiList = adminMapper.getNotiInfo();
        } catch(Exception e) {
            logger.error("Error : " + e.toString(), e);
        }

        if(null != notiList && 0 < notiList.size()) {

            for(int i=0; i < notiList.size(); i++) {
                Map<String, Object> param = new HashMap<String, Object>();
                param.put("userId", notiList.get(i).get("user_id"));
                param.put("userToken", notiList.get(i).get("user_token"));
                param.put("no", notiList.get(i).get("no"));
                param.put("sensorUuid", notiList.get(i).get("sensor_uuid"));
                param.put("sensorId", String.valueOf(param.get("userId")));

                Map<String, Object> temp = new HashMap<String, Object>();
                temp = adminMapper.getSensorVal(param);

                Map<String, Object> configMap = new HashMap<String, Object>();
                configMap = adminMapper.selectSetting(param);

                String highAlarmYn = "";
                String lowAlarmYn = "";
                String diAlarmYn = "";
                String networkAlarmYn = "";

                String highTemp = "";
                String lowTemp = "";

                String inTemp = "";
                String curTemp = "";

                if (null != temp && 0 < temp.size()) {
                    inTemp = String.valueOf(temp.get("sensor_value"));
                    param.put("inTemp", inTemp);
                } else {
                    inTemp = String.valueOf(notiList.get(i).get("in_temp"));
                    param.put("inTemp", inTemp);
                }

                try {
                    sensorInfo = adminMapper.getSensorInfo(param);
                } catch (Exception e) {
                    logger.error("Error : " + e.toString());
                }

                if (null != sensorInfo && 0 < sensorInfo.size()) {
                    sensorName = String.valueOf(sensorInfo.get("sensor_name"));
                }

                String warnText = "";
                String alarmYn = "";
                String type = String.valueOf(notiList.get(i).get("alarm_type"));

                if (null != configMap && 0 < configMap.size()) {
                    highAlarmYn = String.valueOf(configMap.get("alarm_yn1"));
                    lowAlarmYn = String.valueOf(configMap.get("alarm_yn2"));
                    diAlarmYn = String.valueOf(configMap.get("alarm_yn4"));
                    networkAlarmYn = String.valueOf(configMap.get("alarm_yn5"));

                    highTemp = String.valueOf(configMap.get("set_val1"));
                    lowTemp = String.valueOf(configMap.get("set_val2"));

                    if (type.contains("high")) {
                        curTemp = String.valueOf(configMap.get("set_val1"));
                    } else if (type.contains("low")) {
                        curTemp = String.valueOf(configMap.get("set_val2"));
                    }
                } else {
                    curTemp = String.valueOf(notiList.get(i).get("cur_temp"));
                }

                if (null != type && !"".equals(type) && 0 < type.length()) {
                    if (type.contains("high")) {
                        warnText = "온도 높음(설정온도 : " + curTemp + "°C, 현재온도 : " + inTemp + "°C)";
                        if(null != highAlarmYn && !"".equals(highAlarmYn) && "Y".equals(highAlarmYn)) {
                            if (Double.compare(Double.parseDouble(inTemp), Double.parseDouble(highTemp)) > 0) {
                                alarmYn = "Y";
                            }
                        }
                    } else if (type.contains("low")) {
                        warnText = "온도 낮음(설정온도 : " + curTemp + "°C, 현재온도 : " + inTemp + "°C)";
                        if(null != lowAlarmYn && !"".equals(lowAlarmYn) && "Y".equals(lowAlarmYn)) {
                            if (Double.compare(Double.parseDouble(inTemp), Double.parseDouble(lowTemp)) < 0) {
                                alarmYn = "Y";
                            }
                        }
                    } else if (type.contains("di")) {
                        warnText = "DI알람(에러, 현재온도 : " + inTemp + ")";
                        if(null != diAlarmYn && !"".equals(diAlarmYn) && "Y".equals(diAlarmYn)) {
                            alarmYn = "Y";
                        }
                    } else if (type.contains("net")) {
                        warnText = "통신에러";
                        if(null != networkAlarmYn && !"".equals(networkAlarmYn) && "Y".equals(networkAlarmYn)) {
                            alarmYn = "Y";
                        }
                    }
                }

                if(null != alarmYn && !"".equals(alarmYn) && "Y".equals(alarmYn)) {
                    OkHttpClient client = new OkHttpClient.Builder().build();
                    RequestBody body = new FormBody.Builder()
                            .add("to", String.valueOf(notiList.get(i).get("user_token")))
                            .add("project_id", senderId)
                            .add("notification", "")
                            .add("data", sensorName + "장치 이상 발생 : " + warnText)
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
                                logger.info("data : " + param.get("userId") + "/" + param.get("sensorUuid") + "/" + type);

                                try {
                                    adminMapper.deleteNoti(param);
                                    adminMapper.updateUrgentNoti2(param);

                                    param.put("sensorId", String.valueOf(param.get("userId")));

                                    Map<String, Object> config = new HashMap<String, Object>();
                                    config = adminMapper.selectSetting(param);

                                    String highAlarmYn = "";
                                    String lowAlarmYn = "";
                                    String diAlarmYn = "";
                                    String networkAlarmYn = "";

                                    String highTemp = "";
                                    String lowTemp = "";

                                    if (type != null && type.contains("high")) {
                                        param.put("alarmType", "rehigh");
                                    } else if (type != null && type.contains("low")) {
                                        param.put("alarmType", "relow");
                                    } else if (type != null && type.contains("di")) {
                                        param.put("alarmType", "di2");
                                    } else if (type != null && type.contains("net")) {
                                        param.put("alarmType", "netError2");
                                    }

                                    if (null != config && 0 < config.size()) {
                                        highAlarmYn = String.valueOf(config.get("alarm_yn1"));
                                        lowAlarmYn = String.valueOf(config.get("alarm_yn2"));
                                        diAlarmYn = String.valueOf(config.get("alarm_yn4"));
                                        networkAlarmYn = String.valueOf(config.get("alarm_yn5"));

                                        highTemp = String.valueOf(config.get("set_val1"));
                                        lowTemp = String.valueOf(config.get("set_val2"));

                                        if (type != null && type.contains("high")) {
                                            if (null != highAlarmYn && !"".equals(highAlarmYn) && "Y".equals(highAlarmYn)) {
                                                logger.info("INFO : " + highAlarmYn);
                                                param.put("addTime", String.valueOf(config.get("re_delay_time1")));
                                                param.put("curTemp", highTemp);
                                                adminMapper.insertNoti(param);
                                            }
                                        } else if (type != null && type.contains("low")) {
                                            if (null != lowAlarmYn && !"".equals(lowAlarmYn) && "Y".equals(lowAlarmYn)) {
                                                logger.info("INFO : " + lowAlarmYn);
                                                param.put("addTime", String.valueOf(config.get("re_delay_time2")));
                                                param.put("curTemp", lowTemp);
                                                adminMapper.insertNoti(param);
                                            }
                                        } else if (type != null && type.contains("di")) {
                                            if (null != diAlarmYn && !"".equals(diAlarmYn) && "Y".equals(diAlarmYn)) {
                                                logger.info("INFO : " + diAlarmYn);
                                                param.put("addTime", String.valueOf(config.get("re_delay_time4")));
                                                adminMapper.insertNoti(param);
                                            }
                                        } else if (type != null && type.contains("net")) {
                                            if (null != networkAlarmYn && !"".equals(networkAlarmYn) && "Y".equals(networkAlarmYn)) {
                                                logger.info("INFO : " + networkAlarmYn);
                                                param.put("addTime", String.valueOf(config.get("re_delay_time5")));
                                                adminMapper.insertNoti(param);
                                            }
                                        }
                                    }

                                } catch (Exception e) {
                                    logger.error("Error : " + e.toString(), e);
                                }
                            } else {
                                logger.info("Fail : " + response.code() + "/" + response.body().string());
                            }
                        }
                    });
                }
            }
        }
    }
}
