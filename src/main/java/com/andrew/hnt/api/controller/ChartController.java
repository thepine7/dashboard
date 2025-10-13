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
            
            // 센서 이름 설정 (기본값)
            String sensorName = "장치 " + sensorUuid.substring(0, Math.min(8, sensorUuid.length()));
            model.addAttribute("sensorName", sensorName);
            logger.info("차트 페이지 센서 이름 설정: {}", sensorName);
            
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
                        model.addAttribute("sensorName", defaultSensorName != null ? defaultSensorName : "장치 " + defaultSensorUuid.substring(0, Math.min(8, defaultSensorUuid.length())));
                        logger.info("차트 페이지 기본 센서 설정: sensorUuid={}, sensorName={}", defaultSensorUuid, defaultSensorName);
                        
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
