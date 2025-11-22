package com.andrew.hnt.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.andrew.hnt.api.service.UnifiedSessionService;
import com.andrew.hnt.api.service.UnifiedSessionService.SessionValidationResult;
import com.andrew.hnt.api.service.DataService;
import com.andrew.hnt.api.service.AdminService;
import com.andrew.hnt.api.util.RedirectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Controller
@RequestMapping(value = "/chart")
public class ChartController extends DefaultController {

    @Autowired
    private CommonController commonController;
    
    @Autowired
    private UnifiedSessionService unifiedSessionService;
    
    @Autowired
    private DataService dataService;
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private com.andrew.hnt.api.mapper.AdminMapper adminMapper;
    
    private static final Logger logger = LoggerFactory.getLogger(ChartController.class);

    /**
     * 챠트 메인화면 (통합 세션 검증 적용)
     * @return
     */
    @RequestMapping(value = "/chart", method = RequestMethod.GET)
    public String chartMain(
            @RequestParam(value = "sensorUuid", required = false) String sensorUuid,
            HttpServletRequest req,
            Model model
            ) {
        String result = "";

        HttpSession session = req.getSession();
        
        // 통합 세션 검증 (기본 + 보안 + 권한 + 모델 설정)
        SessionValidationResult validationResult = unifiedSessionService.validateSession(
            session, req, model, "B"); // 부계정 이상 권한 필요
        
        if (!validationResult.isValid()) {
            logger.warn("차트 페이지 접근 - 통합 세션 검증 실패, 리다이렉트: {}, 오류: {}", 
                validationResult.getRedirectUrl(), validationResult.getErrorMessage());
            return validationResult.getRedirectUrl();
        }
        
        // 검증된 사용자 정보 사용
        String sessionUserId = validationResult.getUserId();
        String sessionUserGrade = validationResult.getUserGrade();
        String sessionUserNm = validationResult.getUserNm();
        
        logger.info("ChartController 통합 세션 검증 성공 - userId: {}, userGrade: {}, userNm: {}", 
                   sessionUserId, sessionUserGrade, sessionUserNm);

        // 세션 userId 사용하여 사이드바 데이터 설정
        commonController.addSidebarData(sessionUserId, model, session);
        
        // sensorUuid 파라미터를 모델에 추가
        if (sensorUuid != null && !sensorUuid.isEmpty()) {
            model.addAttribute("sensorUuid", sensorUuid);
            logger.info("차트 페이지 sensorUuid 설정: {}", sensorUuid);
            
            // 센서 정보 조회 (UUID로 직접 조회 - 간단한 방식)
            try {
                // UUID로 직접 센서 정보 조회
                Map<String, Object> sensorParam = new HashMap<>();
                sensorParam.put("sensorUuid", sensorUuid);
                sensorParam.put("userId", sessionUserId);
                
                Map<String, Object> sensorInfo = adminMapper.getSensorInfoByUuid(sensorParam);
                String sensorName = sensorUuid; // 기본값: UUID
                
                if (sensorInfo != null && !sensorInfo.isEmpty()) {
                    // DB에서 센서 이름 조회
                    String dbSensorName = (String) sensorInfo.get("sensor_name");
                    if (dbSensorName != null && !dbSensorName.isEmpty()) {
                        sensorName = dbSensorName; // DB에서 가져온 이름 그대로 사용
                    }
                    
                    // 센서의 실제 소유자 ID(sensor_id)로 MQTT 토픽 설정 (부계정 지원)
                    String sensorOwnerId = (String) sensorInfo.get("sensor_id");
                    if (sensorOwnerId != null && !sensorOwnerId.isEmpty()) {
                        String topicStr = "HBEE/" + sensorOwnerId + "/TC/" + sensorUuid + "/SER";
                        model.addAttribute("topicStr", topicStr);
                        model.addAttribute("sensorId", sensorOwnerId); // 센서 실제 소유자 ID
                        logger.info("차트 페이지 토픽 설정: topicStr={}, sensorId={}", topicStr, sensorOwnerId);
                    }
                }
                
                // 초기 표시명은 "장치이름(장치종류)" 형태 (device_type은 MQTT로 실시간 수신 후 정확한 값으로 업데이트)
                String displayName = sensorName + "(장치종류)";
                model.addAttribute("sensorName", displayName);
                model.addAttribute("sensor_name", sensorName);
                model.addAttribute("deviceType", ""); // 빈 문자열로 초기화 (MQTT로 실시간 수신 대기)
                logger.info("차트 페이지 센서 정보 설정: sensorName={} (device_type은 MQTT로 실시간 수신 대기)", displayName);
            } catch (Exception e) {
                logger.error("차트 페이지 센서 정보 조회 중 오류 발생", e);
                String sensorName = sensorUuid; // 기본값: 전체 UUID 사용
                String displayName = sensorName + "(장치종류)";
                model.addAttribute("sensorName", displayName);
                model.addAttribute("sensor_name", sensorName);
                model.addAttribute("deviceType", ""); // 빈 문자열로 초기화
            }
            
            // 차트 데이터 조회 (최적화된 쿼리 사용)
            try {
                Map<String, Object> param = new HashMap<>();
                param.put("userId", sessionUserId);
                param.put("sensorUuid", sensorUuid);
                
                // 날짜 범위 설정 (오늘 하루)
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String today = sdf.format(new java.util.Date());
                param.put("startDateTime", today + " 00:00:00");
                param.put("endDateTime", today + " 23:59:59");
                
                // 최적화된 일간 데이터 조회 (30분 단위 그룹화)
                List<Map<String, Object>> dailyList = dataService.selectDailyData(param);
                String daily = "";
                if (dailyList != null && !dailyList.isEmpty()) {
                    StringBuilder dailyBuilder = new StringBuilder();
                    for (Map<String, Object> item : dailyList) {
                        if (dailyBuilder.length() > 0) {
                            dailyBuilder.append(",");
                        }
                        dailyBuilder.append(item.get("inst_dtm")).append("^").append(item.get("sensor_value"));
                    }
                    daily = dailyBuilder.toString();
                }
                
                model.addAttribute("daily", daily);
                model.addAttribute("monthly", ""); // 월간 데이터는 일단 빈 문자열
                model.addAttribute("yearly", ""); // 연간 데이터는 일단 빈 문자열
                
                logger.info("최적화된 차트 데이터 조회 완료 - daily 데이터 개수: {}", dailyList != null ? dailyList.size() : 0);
                
            } catch (Exception e) {
                logger.error("차트 데이터 조회 중 오류 발생", e);
                model.addAttribute("daily", "");
                model.addAttribute("monthly", "");
                model.addAttribute("yearly", "");
            }
        } else {
            logger.warn("차트 페이지 sensorUuid 파라미터가 없습니다. 기본 센서 설정 시도.");
            
            // 기본 센서 UUID 설정 (사용자의 첫 번째 센서)
            try {
                Map<String, Object> param = new HashMap<>();
                param.put("userId", sessionUserId);
                
                // 사용자의 센서 목록 조회
                List<Map<String, Object>> sensorList = adminService.getSensorList(sessionUserId);
                if (sensorList != null && !sensorList.isEmpty()) {
                    Map<String, Object> firstSensor = sensorList.get(0);
                    String defaultSensorUuid = (String) firstSensor.get("sensor_uuid");
                    String defaultSensorName = (String) firstSensor.get("sensor_name");
                    
                    if (defaultSensorUuid != null && !defaultSensorUuid.isEmpty()) {
                        model.addAttribute("sensorUuid", defaultSensorUuid);
                        
                        // 기본 센서의 상세 정보 조회 (UUID로 직접 조회 - 간단한 방식)
                        try {
                            // UUID로 직접 센서 정보 조회
                            Map<String, Object> defaultSensorParam = new HashMap<>();
                            defaultSensorParam.put("sensorUuid", defaultSensorUuid);
                            defaultSensorParam.put("userId", sessionUserId);
                            
                            Map<String, Object> sensorInfo = adminMapper.getSensorInfoByUuid(defaultSensorParam);
                            String sensorName = defaultSensorName != null ? defaultSensorName : defaultSensorUuid; // 기본값
                            
                            if (sensorInfo != null && !sensorInfo.isEmpty()) {
                                // DB에서 센서 이름 조회
                                String dbSensorName = (String) sensorInfo.get("sensor_name");
                                if (dbSensorName != null && !dbSensorName.isEmpty()) {
                                    sensorName = dbSensorName; // DB에서 가져온 이름 그대로 사용
                                }
                            }
                            
                            // 초기 표시명은 "장치이름(장치종류)" 형태 (device_type은 MQTT로 실시간 수신 후 정확한 값으로 업데이트)
                            String displayName = sensorName + "(장치종류)";
                            model.addAttribute("sensorName", displayName);
                            model.addAttribute("sensor_name", sensorName);
                            model.addAttribute("deviceType", ""); // 빈 문자열로 초기화 (MQTT로 실시간 수신 대기)
                            logger.info("차트 페이지 기본 센서 설정: sensorUuid={}, sensorName={} (device_type은 MQTT로 실시간 수신 대기)", 
                                       defaultSensorUuid, displayName);
                        } catch (Exception e) {
                            logger.error("기본 센서 정보 조회 중 오류 발생", e);
                            String sensorName = defaultSensorName != null ? defaultSensorName : defaultSensorUuid; // 기본값: 전체 UUID 사용
                            String displayName = sensorName + "(장치종류)";
                            model.addAttribute("sensorName", displayName);
                            model.addAttribute("sensor_name", sensorName);
                            model.addAttribute("deviceType", ""); // 빈 문자열로 초기화
                        }
                        
                        // 기본 센서의 차트 데이터 조회
                        param.put("sensorUuid", defaultSensorUuid);
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                        String today = sdf.format(new java.util.Date());
                        param.put("startDateTime", today + " 00:00:00");
                        param.put("endDateTime", today + " 23:59:59");
                        
                        List<Map<String, Object>> dailyList = dataService.selectDailyData(param);
                        String daily = "";
                        if (dailyList != null && !dailyList.isEmpty()) {
                            StringBuilder dailyBuilder = new StringBuilder();
                            for (Map<String, Object> item : dailyList) {
                                if (dailyBuilder.length() > 0) {
                                    dailyBuilder.append(",");
                                }
                                dailyBuilder.append(item.get("inst_dtm")).append("^").append(item.get("sensor_value"));
                            }
                            daily = dailyBuilder.toString();
                        }
                        
                        model.addAttribute("daily", daily);
                        model.addAttribute("monthly", "");
                        model.addAttribute("yearly", "");
                        logger.info("기본 센서 차트 데이터 조회 완료 - daily 데이터 개수: {}", dailyList != null ? dailyList.size() : 0);
                    } else {
                        logger.warn("기본 센서 UUID가 비어있습니다.");
                        model.addAttribute("daily", "");
                        model.addAttribute("monthly", "");
                        model.addAttribute("yearly", "");
                    }
                } else {
                    logger.warn("사용자 센서 목록이 비어있습니다.");
                    model.addAttribute("daily", "");
                    model.addAttribute("monthly", "");
                    model.addAttribute("yearly", "");
                }
            } catch (Exception e) {
                logger.error("기본 센서 설정 중 오류 발생", e);
                model.addAttribute("daily", "");
                model.addAttribute("monthly", "");
                model.addAttribute("yearly", "");
            }
        }

        if(null != sessionUserId && !"".equals(sessionUserId)) {
            result = "chart/chart";
        } else {
            // 표준화된 리다이렉트 로직 사용
            RedirectUtil.RedirectResult redirectResult = RedirectUtil.redirectToLogin(session, req, "세션 정보 누락");
            logger.info(RedirectUtil.createRedirectLog(redirectResult, req));
            result = redirectResult.getRedirectUrl();
        }

        return result;
    }
}
