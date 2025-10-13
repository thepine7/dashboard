package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 권한 체크 유틸리티 클래스
 * HnT Sensor API 프로젝트 전용
 * 
 * 사용자 권한 체크 로직을 표준화하고 일관된 권한 검증을 제공
 * 
 * 권한 등급:
 * - A: 관리자 (모든 권한)
 * - U: 일반 사용자 (일반 사용자 + 부계정 권한)
 * - B: 부계정 (읽기 전용 권한)
 */
@Component
public class PermissionUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionUtil.class);
    
    // 권한 등급 상수
    public static final String ADMIN_GRADE = "A";
    public static final String USER_GRADE = "U";
    public static final String SUB_ACCOUNT_GRADE = "B";
    
    // 권한 레벨 상수
    public static final int ADMIN_LEVEL = 3;
    public static final int USER_LEVEL = 2;
    public static final int SUB_ACCOUNT_LEVEL = 1;
    
    /**
     * 사용자 권한 등급을 레벨로 변환
     * @param userGrade 사용자 권한 등급
     * @return 권한 레벨 (0: 무효, 1: 부계정, 2: 일반사용자, 3: 관리자)
     */
    public static int getPermissionLevel(String userGrade) {
        if (userGrade == null || userGrade.isEmpty()) {
            return 0;
        }
        
        switch (userGrade) {
            case ADMIN_GRADE:
                return ADMIN_LEVEL;
            case USER_GRADE:
                return USER_LEVEL;
            case SUB_ACCOUNT_GRADE:
                return SUB_ACCOUNT_LEVEL;
            default:
                logger.warn("알 수 없는 권한 등급: {}", userGrade);
                return 0;
        }
    }
    
    /**
     * 사용자가 필요한 권한을 가지고 있는지 확인
     * @param userGrade 사용자 권한 등급
     * @param requiredGrade 필요한 권한 등급
     * @return 권한 보유 여부
     */
    public static boolean hasPermission(String userGrade, String requiredGrade) {
        int userLevel = getPermissionLevel(userGrade);
        int requiredLevel = getPermissionLevel(requiredGrade);
        
        if (userLevel == 0 || requiredLevel == 0) {
            logger.warn("권한 검증 실패 - 사용자: {}, 필요: {}", userGrade, requiredGrade);
            return false;
        }
        
        boolean hasPermission = userLevel >= requiredLevel;
        
        if (!hasPermission) {
            logger.warn("권한 부족 - 사용자: {} (레벨: {}), 필요: {} (레벨: {})", 
                       userGrade, userLevel, requiredGrade, requiredLevel);
        } else {
            logger.debug("권한 확인 - 사용자: {} (레벨: {}), 필요: {} (레벨: {})", 
                        userGrade, userLevel, requiredGrade, requiredLevel);
        }
        
        return hasPermission;
    }
    
    /**
     * 부계정 권한 제한 확인
     * @param userGrade 사용자 권한 등급
     * @return 부계정 여부
     */
    public static boolean isSubAccount(String userGrade) {
        return SUB_ACCOUNT_GRADE.equals(userGrade);
    }
    
    /**
     * 관리자 권한 확인
     * @param userGrade 사용자 권한 등급
     * @return 관리자 여부
     */
    public static boolean isAdmin(String userGrade) {
        return ADMIN_GRADE.equals(userGrade);
    }
    
    /**
     * 일반 사용자 이상 권한 확인
     * @param userGrade 사용자 권한 등급
     * @return 일반 사용자 이상 여부
     */
    public static boolean isUserOrAbove(String userGrade) {
        return hasPermission(userGrade, USER_GRADE);
    }
    
    /**
     * 부계정 이상 권한 확인
     * @param userGrade 사용자 권한 등급
     * @return 부계정 이상 여부
     */
    public static boolean isSubAccountOrAbove(String userGrade) {
        return hasPermission(userGrade, SUB_ACCOUNT_GRADE);
    }
    
    /**
     * 권한 검증 결과 클래스
     */
    public static class PermissionResult {
        private final boolean valid;
        private final String errorMessage;
        private final String userGrade;
        private final String requiredGrade;
        
        public PermissionResult(boolean valid, String errorMessage, String userGrade, String requiredGrade) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.userGrade = userGrade;
            this.requiredGrade = requiredGrade;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getUserGrade() {
            return userGrade;
        }
        
        public String getRequiredGrade() {
            return requiredGrade;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> result = new HashMap<>();
            result.put("valid", valid);
            result.put("errorMessage", errorMessage);
            result.put("userGrade", userGrade);
            result.put("requiredGrade", requiredGrade);
            return result;
        }
    }
    
    /**
     * 권한 검증 수행
     * @param userGrade 사용자 권한 등급
     * @param requiredGrade 필요한 권한 등급
     * @return 권한 검증 결과
     */
    public static PermissionResult validatePermission(String userGrade, String requiredGrade) {
        if (userGrade == null || userGrade.isEmpty()) {
            return new PermissionResult(false, "사용자 권한 정보가 없습니다.", userGrade, requiredGrade);
        }
        
        if (requiredGrade == null || requiredGrade.isEmpty()) {
            return new PermissionResult(false, "필요한 권한 정보가 없습니다.", userGrade, requiredGrade);
        }
        
        boolean hasPermission = hasPermission(userGrade, requiredGrade);
        
        if (!hasPermission) {
            String errorMessage = String.format("권한이 부족합니다. 현재: %s, 필요: %s", 
                                               getGradeDescription(userGrade), 
                                               getGradeDescription(requiredGrade));
            return new PermissionResult(false, errorMessage, userGrade, requiredGrade);
        }
        
        return new PermissionResult(true, "권한 검증 성공", userGrade, requiredGrade);
    }
    
    /**
     * 권한 등급 설명 반환
     * @param userGrade 사용자 권한 등급
     * @return 권한 등급 설명
     */
    public static String getGradeDescription(String userGrade) {
        if (userGrade == null || userGrade.isEmpty()) {
            return "알 수 없음";
        }
        
        switch (userGrade) {
            case ADMIN_GRADE:
                return "관리자";
            case USER_GRADE:
                return "일반 사용자";
            case SUB_ACCOUNT_GRADE:
                return "부계정";
            default:
                return "알 수 없는 권한 (" + userGrade + ")";
        }
    }
    
    /**
     * 권한 등급별 허용 작업 목록 반환
     * @param userGrade 사용자 권한 등급
     * @return 허용 작업 목록
     */
    public static String[] getAllowedOperations(String userGrade) {
        if (isAdmin(userGrade)) {
            return new String[]{"READ", "WRITE", "DELETE", "CREATE", "ADMIN"};
        } else if (isUserOrAbove(userGrade)) {
            return new String[]{"READ", "WRITE", "DELETE", "CREATE"};
        } else if (isSubAccountOrAbove(userGrade)) {
            return new String[]{"READ"};
        } else {
            return new String[]{};
        }
    }
    
    /**
     * 특정 작업 권한 확인
     * @param userGrade 사용자 권한 등급
     * @param operation 작업명
     * @return 작업 권한 여부
     */
    public static boolean canPerformOperation(String userGrade, String operation) {
        String[] allowedOps = getAllowedOperations(userGrade);
        for (String allowedOp : allowedOps) {
            if (allowedOp.equals(operation)) {
                return true;
            }
        }
        return false;
    }
}
