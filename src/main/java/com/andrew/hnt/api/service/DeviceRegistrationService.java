package com.andrew.hnt.api.service;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.andrew.hnt.api.util.ResponseUtil;

/**
 * 장치 등록 공통 서비스
 * MainController와 DataController에서 중복 사용되던 insertSensorInfo 로직을 통합
 */
@Service
public class DeviceRegistrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistrationService.class);
    
    @Autowired
    private LoginService loginService;
    
    @Autowired
    private SessionManagementService sessionManagementService;
    
    /**
     * 장치 등록 처리 (공통 로직)
     * @param req HTTP 요청 객체
     * @param insertData 장치 등록 데이터
     * @param isFromMainController MainController에서 호출된 경우 true
     * @return 처리 결과 Map
     */
    public Map<String, Object> processDeviceRegistration(
            HttpServletRequest req, 
            Map<String, Object> insertData, 
            boolean isFromMainController) {
        HttpSession session = req.getSession();
        
        // 세션 검증
        if (session == null) {
            return isFromMainController ? 
                ResponseUtil.unauthorized("세션이 없습니다.") : 
                createErrorResponse("401", "세션이 만료되었거나 로그인이 필요합니다.");
        }
        
        // 데이터 검증
        if (insertData == null || insertData.size() == 0) {
            return isFromMainController ? 
                ResponseUtil.badRequest("장치 정보가 없습니다.") : 
                createErrorResponse("400", "장치 정보가 없습니다.");
        }
        
        try {
            // 세션 정보 검증 및 추출
            SessionManagementService.SessionInfo sessionInfo = sessionManagementService.validateAndGetSessionInfo(session, req);
            
            if (!sessionInfo.isValid()) {
                logger.warn("세션 검증 실패 - 장치 등록 시도 차단");
                return isFromMainController ? 
                    ResponseUtil.unauthorized("세션이 만료되었습니다.") : 
                    createErrorResponse("401", "세션이 만료되었거나 로그인이 필요합니다.");
            }
            
            String sessionUserId = sessionInfo.getUserId();
            String sessionUserGrade = sessionInfo.getUserGrade();
            
            // 장치 관리 권한 체크 (A, U 등급만 가능)
            if (!sessionManagementService.canManageDevices(session)) {
                logger.warn("권한 없는 사용자 장치 등록 시도 차단 - userId: {}, userGrade: {}", sessionUserId, sessionUserGrade);
                return isFromMainController ? 
                    ResponseUtil.badRequest("장치 등록 권한이 없습니다.") : 
                    createErrorResponse("403", "장치 등록 권한이 없습니다.");
            }
            
            // 요청 타입 자동 구분 (앱 수정 불가로 인해)
            String sensorName = String.valueOf(insertData.get("sensorName"));
            String uuid = String.valueOf(insertData.get("uuid"));
            String userId = String.valueOf(insertData.get("userId"));
            
            // kimtest 사용자만 로깅 (로그 스팸 방지)
            if ("kimtest".equals(userId)) {
                logger.info("=== kimtest 자동 요청 타입 구분 시작 ===");
                logger.info("sensorName: '{}', uuid: '{}'", sensorName, uuid);
                logger.info("호출 컨트롤러: {}", isFromMainController ? "MainController" : "DataController");
            }
            
            // 장치 상태 확인 vs 장치 등록/이름 변경 구분
            if (String.valueOf(insertData.get("userId")).equals(String.valueOf(insertData.get("sensorId")))) {
                // 장치 상태 확인 요청
                if ("kimtest".equals(userId)) {
                    logger.info("kimtest 장치 상태 확인 요청 감지");
                }
                
                // 장치 상태 확인 로직 (현재는 단순 성공 응답)
                return isFromMainController ? 
                    ResponseUtil.success("장치 상태 확인 완료") : 
                    createSuccessResponse("200", "장치 상태 확인 완료");
                
            } else {
                // 장치 등록/이름 변경 요청
                Map<String, Object> param = new HashMap<String, Object>();
                param.put("userId", insertData.get("userId"));
                param.put("sensorId", insertData.get("sensorId"));
                param.put("sensorUuid", insertData.get("uuid"));
                param.put("sensorType", insertData.get("sensorType"));
                param.put("instId", "hnt");
                param.put("mdfId", "hnt");
                
                // 장치 이름 파라미터 추가
                if (insertData.get("sensorName") != null && !"".equals(String.valueOf(insertData.get("sensorName")))) {
                    param.put("sensorName", String.valueOf(insertData.get("sensorName")));
                    if ("kimtest".equals(userId)) {
                        logger.info("kimtest 장치 이름 변경 요청 감지 - sensorName: {}", insertData.get("sensorName"));
                    }
                } else {
                    if ("kimtest".equals(userId)) {
                        logger.info("kimtest sensorName 파라미터가 없음 - insertData.get('sensorName'): {}", insertData.get("sensorName"));
                    }
                }
                
                if ("kimtest".equals(userId)) {
                    logger.info("kimtest 장치 등록 요청으로 인식 - LoginServiceImpl로 전달할 param: {}", param);
                }
                
                // LoginService를 통한 실제 장치 등록 처리
                Map<String, Object> chkMap = loginService.insertSensorInfo2(param);
                
                if (chkMap != null && chkMap.size() > 0) {
                    if ("true".equals(String.valueOf(chkMap.get("result")))) {
                        String message = chkMap.get("message") != null ? 
                            String.valueOf(chkMap.get("message")) : "success";
                        return isFromMainController ? 
                            ResponseUtil.success(message) : 
                            createSuccessResponse("200", message);
                    } else {
                        // 중복 등록 또는 기타 오류
                        String message = chkMap.get("message") != null ? 
                            String.valueOf(chkMap.get("message")) : "장치 등록 실패";
                        return isFromMainController ? 
                            ResponseUtil.badRequest(message) : 
                            createErrorResponse("400", message);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("장치 등록 처리 중 오류 발생: {}", e.toString(), e);
            return isFromMainController ? 
                ResponseUtil.error(e) : 
                createErrorResponse("999", "장치 등록 중 오류가 발생했습니다.");
        }
        
        return isFromMainController ? 
            ResponseUtil.badRequest("처리할 수 없는 요청입니다.") : 
            createErrorResponse("400", "처리할 수 없는 요청입니다.");
    }
    
    /**
     * MainController용 성공 응답 생성
     */
    private Map<String, Object> createSuccessResponse(String resultCode, String resultMessage) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("resultCode", resultCode);
        resultMap.put("resultMessage", resultMessage);
        return resultMap;
    }
    
    /**
     * MainController용 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String resultCode, String resultMessage) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("resultCode", resultCode);
        resultMap.put("resultMessage", resultMessage);
        return resultMap;
    }
}