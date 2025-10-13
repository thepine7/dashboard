package com.andrew.hnt.api.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.andrew.hnt.api.util.PermissionUtil;
import com.andrew.hnt.api.util.RedirectUtil;
import com.andrew.hnt.api.common.Constants;

/**
 * 부계정 권한 제한 서비스
 * HnT Sensor API 프로젝트 전용
 * 
 * 부계정(B 등급) 사용자의 권한을 강화하고
 * 읽기 전용 권한을 엄격하게 적용
 */
@Service
public class SubAccountPermissionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SubAccountPermissionService.class);
    
    @Autowired
    private AdminService adminService;
    
    // 부계정 제한 작업 목록
    private static final String[] RESTRICTED_OPERATIONS = {
        "CREATE_USER",      // 사용자 생성
        "DELETE_USER",      // 사용자 삭제
        "UPDATE_USER",      // 사용자 수정
        "CREATE_SENSOR",    // 센서 생성
        "DELETE_SENSOR",    // 센서 삭제
        "UPDATE_SENSOR",    // 센서 수정
        "UPDATE_SENSOR_SETTING", // 센서 설정 변경
        "CREATE_SUB_ACCOUNT", // 부계정 생성
        "DELETE_SUB_ACCOUNT", // 부계정 삭제
        "UPDATE_SUB_ACCOUNT", // 부계정 수정
        "ADMIN_OPERATIONS", // 관리자 작업
        "SYSTEM_SETTINGS"   // 시스템 설정
    };
    
    // 부계정 허용 작업 목록
    private static final String[] ALLOWED_OPERATIONS = {
        "READ_SENSOR_DATA",     // 센서 데이터 조회
        "READ_USER_INFO",       // 사용자 정보 조회
        "READ_SENSOR_LIST",     // 센서 목록 조회
        "READ_CHART_DATA",      // 차트 데이터 조회
        "READ_ALARM_DATA",      // 알람 데이터 조회
        "EXPORT_DATA",          // 데이터 내보내기
        "VIEW_DASHBOARD"        // 대시보드 조회
    };
    
    /**
     * 부계정 권한 검증 결과 클래스
     */
    public static class SubAccountPermissionResult {
        private final boolean allowed;
        private final String reason;
        private final String operation;
        private final String userGrade;
        private final Map<String, Object> additionalInfo;
        
        public SubAccountPermissionResult(boolean allowed, String reason, String operation, String userGrade) {
            this.allowed = allowed;
            this.reason = reason;
            this.operation = operation;
            this.userGrade = userGrade;
            this.additionalInfo = new HashMap<>();
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public String getReason() {
            return reason;
        }
        
        public String getOperation() {
            return operation;
        }
        
        public String getUserGrade() {
            return userGrade;
        }
        
        public Map<String, Object> getAdditionalInfo() {
            return additionalInfo;
        }
        
        public void addInfo(String key, Object value) {
            this.additionalInfo.put(key, value);
        }
    }
    
    /**
     * 부계정 권한 검증
     * @param session HttpSession
     * @param operation 수행하려는 작업
     * @return 권한 검증 결과
     */
    public SubAccountPermissionResult validateSubAccountPermission(HttpSession session, String operation) {
        if (session == null) {
            return new SubAccountPermissionResult(false, "세션이 null입니다", operation, "UNKNOWN");
        }
        
        String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
        String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
        
        if (userGrade == null || userGrade.isEmpty()) {
            return new SubAccountPermissionResult(false, "사용자 등급 정보가 없습니다", operation, "UNKNOWN");
        }
        
        // 부계정이 아닌 경우 통과
        if (!PermissionUtil.isSubAccount(userGrade)) {
            return new SubAccountPermissionResult(true, "부계정이 아닙니다", operation, userGrade);
        }
        
        // 부계정인 경우 작업별 권한 검증
        return validateOperationPermission(operation, userGrade, userId);
    }
    
    /**
     * 작업별 권한 검증
     * @param operation 수행하려는 작업
     * @param userGrade 사용자 등급
     * @param userId 사용자 ID
     * @return 권한 검증 결과
     */
    private SubAccountPermissionResult validateOperationPermission(String operation, String userGrade, String userId) {
        // 제한된 작업인지 확인
        for (String restrictedOp : RESTRICTED_OPERATIONS) {
            if (operation.equals(restrictedOp)) {
                logger.warn("부계정 권한 제한 - 작업: {}, 사용자: {}, 등급: {}", operation, userId, userGrade);
                return new SubAccountPermissionResult(false, 
                    "부계정은 " + getOperationDescription(operation) + " 권한이 없습니다", 
                    operation, userGrade);
            }
        }
        
        // 허용된 작업인지 확인
        for (String allowedOp : ALLOWED_OPERATIONS) {
            if (operation.equals(allowedOp)) {
                logger.debug("부계정 권한 허용 - 작업: {}, 사용자: {}, 등급: {}", operation, userId, userGrade);
                return new SubAccountPermissionResult(true, 
                    "부계정 " + getOperationDescription(operation) + " 권한 허용", 
                    operation, userGrade);
            }
        }
        
        // 알 수 없는 작업은 기본적으로 제한
        logger.warn("알 수 없는 작업 - 부계정 권한 제한: {}, 사용자: {}, 등급: {}", operation, userId, userGrade);
        return new SubAccountPermissionResult(false, 
            "알 수 없는 작업입니다", 
            operation, userGrade);
    }
    
    /**
     * 작업 설명 반환
     * @param operation 작업명
     * @return 작업 설명
     */
    private String getOperationDescription(String operation) {
        switch (operation) {
            case "CREATE_USER": return "사용자 생성";
            case "DELETE_USER": return "사용자 삭제";
            case "UPDATE_USER": return "사용자 수정";
            case "CREATE_SENSOR": return "센서 생성";
            case "DELETE_SENSOR": return "센서 삭제";
            case "UPDATE_SENSOR": return "센서 수정";
            case "UPDATE_SENSOR_SETTING": return "센서 설정 변경";
            case "CREATE_SUB_ACCOUNT": return "부계정 생성";
            case "DELETE_SUB_ACCOUNT": return "부계정 삭제";
            case "UPDATE_SUB_ACCOUNT": return "부계정 수정";
            case "ADMIN_OPERATIONS": return "관리자 작업";
            case "SYSTEM_SETTINGS": return "시스템 설정";
            case "READ_SENSOR_DATA": return "센서 데이터 조회";
            case "READ_USER_INFO": return "사용자 정보 조회";
            case "READ_SENSOR_LIST": return "센서 목록 조회";
            case "READ_CHART_DATA": return "차트 데이터 조회";
            case "READ_ALARM_DATA": return "알람 데이터 조회";
            case "EXPORT_DATA": return "데이터 내보내기";
            case "VIEW_DASHBOARD": return "대시보드 조회";
            default: return operation;
        }
    }
    
    /**
     * 부계정 센서 접근 권한 검증
     * @param session HttpSession
     * @param sensorUuid 센서 UUID
     * @return 접근 권한 여부
     */
    public boolean validateSensorAccess(HttpSession session, String sensorUuid) {
        if (session == null || sensorUuid == null || sensorUuid.isEmpty()) {
            return false;
        }
        
        String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
        String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
        
        if (userId == null || userGrade == null) {
            return false;
        }
        
        // 부계정이 아닌 경우 통과
        if (!PermissionUtil.isSubAccount(userGrade)) {
            return true;
        }
        
        try {
            // 부계정의 경우 접근 가능한 센서 목록 확인
            List<Map<String, Object>> accessibleSensors = adminService.getSubSensorList(userId, userId);
            
            for (Map<String, Object> sensor : accessibleSensors) {
                String accessibleUuid = (String) sensor.get("sensor_uuid");
                if (sensorUuid.equals(accessibleUuid)) {
                    logger.debug("부계정 센서 접근 허용 - 사용자: {}, 센서: {}", userId, sensorUuid);
                    return true;
                }
            }
            
            logger.warn("부계정 센서 접근 거부 - 사용자: {}, 센서: {}", userId, sensorUuid);
            return false;
            
        } catch (Exception e) {
            logger.error("부계정 센서 접근 권한 검증 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 부계정 사용자 접근 권한 검증
     * @param session HttpSession
     * @param targetUserId 대상 사용자 ID
     * @return 접근 권한 여부
     */
    public boolean validateUserAccess(HttpSession session, String targetUserId) {
        if (session == null || targetUserId == null || targetUserId.isEmpty()) {
            return false;
        }
        
        String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
        String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
        
        if (userId == null || userGrade == null) {
            return false;
        }
        
        // 부계정이 아닌 경우 통과
        if (!PermissionUtil.isSubAccount(userGrade)) {
            return true;
        }
        
        // 부계정은 자신의 정보만 접근 가능
        boolean allowed = userId.equals(targetUserId);
        
        if (!allowed) {
            logger.warn("부계정 사용자 접근 거부 - 사용자: {}, 대상: {}", userId, targetUserId);
        } else {
            logger.debug("부계정 사용자 접근 허용 - 사용자: {}, 대상: {}", userId, targetUserId);
        }
        
        return allowed;
    }
    
    /**
     * 부계정 권한 제한 로그 생성
     * @param session HttpSession
     * @param operation 수행하려던 작업
     * @param reason 제한 이유
     * @return 로그 메시지
     */
    public String createPermissionDeniedLog(HttpSession session, String operation, String reason) {
        String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
        String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
        
        return String.format("부계정 권한 제한 - 사용자: %s, 등급: %s, 작업: %s, 이유: %s", 
                           userId, userGrade, operation, reason);
    }
    
    /**
     * 부계정 허용 작업 목록 반환
     * @return 허용 작업 목록
     */
    public List<String> getAllowedOperations() {
        List<String> operations = new ArrayList<>();
        for (String operation : ALLOWED_OPERATIONS) {
            operations.add(operation);
        }
        return operations;
    }
    
    /**
     * 부계정 제한 작업 목록 반환
     * @return 제한 작업 목록
     */
    public List<String> getRestrictedOperations() {
        List<String> operations = new ArrayList<>();
        for (String operation : RESTRICTED_OPERATIONS) {
            operations.add(operation);
        }
        return operations;
    }
    
    /**
     * 부계정 권한 통계 반환
     * @param session HttpSession
     * @return 권한 통계
     */
    public Map<String, Object> getSubAccountPermissionStats(HttpSession session) {
        Map<String, Object> stats = new HashMap<>();
        
        String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
        String userGrade = (String) session.getAttribute(Constants.SESSION_USER_GRADE);
        
        stats.put("userId", userId);
        stats.put("userGrade", userGrade);
        stats.put("isSubAccount", PermissionUtil.isSubAccount(userGrade));
        stats.put("allowedOperations", getAllowedOperations());
        stats.put("restrictedOperations", getRestrictedOperations());
        stats.put("totalAllowedOperations", ALLOWED_OPERATIONS.length);
        stats.put("totalRestrictedOperations", RESTRICTED_OPERATIONS.length);
        
        return stats;
    }
}
