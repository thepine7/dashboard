package com.andrew.hnt.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.andrew.hnt.api.mapper.AdminMapper;

/**
 * 부계정 관리 통합 서비스
 * 주계정과 부계정의 관계를 일관되게 관리
 */
@Service
public class SubAccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(SubAccountService.class);
    
    @Autowired
    private AdminMapper adminMapper;
    
    /**
     * 부계정 여부 확인 (통일된 로직)
     * @param userId 사용자 ID
     * @return 부계정 여부
     */
    public boolean isSubAccount(String userId) {
        try {
            // 1차: parent_user_id 컬럼 확인 (기본 방식)
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("userId", userId);
            
            boolean isSubByParent = adminMapper.isSubAccount(param);
            if (isSubByParent) {
                logger.info("부계정 확인 (parent_user_id 기반) - userId: {}", userId);
                return true;
            }
            
            // 2차: hnt_sensor_info 테이블에서 user_id != sensor_id 확인 (보조 방식)
            boolean isSubBySensor = adminMapper.isSubAccountBySensorInfo(param);
            if (isSubBySensor) {
                logger.info("부계정 확인 (sensor_info 기반) - userId: {}", userId);
                return true;
            }
            
            logger.info("주계정 확인 - userId: {}", userId);
            return false;
            
        } catch (Exception e) {
            logger.error("부계정 여부 확인 실패 - userId: {}, error: {}", userId, e.toString());
            return false;
        }
    }
    
    /**
     * 부계정의 메인 사용자 ID 조회 (통일된 로직)
     * @param subUserId 부계정 사용자 ID
     * @return 메인 사용자 ID
     */
    public String getMainUserIdForSubUser(String subUserId) {
        try {
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("subUserId", subUserId);
            
            // 1차: parent_user_id 컬럼에서 조회 (기본 방식)
            Map<String, Object> result = adminMapper.getMainUserIdForSubUser(param);
            if (result != null && result.size() > 0) {
                String mainUserId = String.valueOf(result.get("parent_user_id"));
                if (mainUserId != null && !"null".equals(mainUserId) && !mainUserId.isEmpty()) {
                    logger.info("메인 사용자 ID 조회 성공 (parent_user_id 기반) - 부계정: {}, 메인: {}", subUserId, mainUserId);
                    return mainUserId;
                }
            }
            
            // 2차: hnt_sensor_info 테이블에서 sensor_id 조회 (보조 방식)
            result = adminMapper.getMainUserIdForSubUserBySensorInfo(param);
            if (result != null && result.size() > 0) {
                String mainUserId = String.valueOf(result.get("sensor_id"));
                if (mainUserId != null && !"null".equals(mainUserId) && !mainUserId.isEmpty()) {
                    logger.info("메인 사용자 ID 조회 성공 (sensor_info 기반) - 부계정: {}, 메인: {}", subUserId, mainUserId);
                    return mainUserId;
                }
            }
            
            logger.warn("부계정의 메인 사용자 ID를 찾을 수 없음 - 부계정: {}", subUserId);
            return null;
            
        } catch (Exception e) {
            logger.error("부계정의 메인 사용자 ID 조회 실패 - 부계정: {}, error: {}", subUserId, e.toString());
            return null;
        }
    }
    
    /**
     * 부계정이 접근 가능한 센서 리스트 조회
     * @param mainUserId 메인 사용자 ID
     * @param subUserId 부계정 사용자 ID
     * @return 센서 리스트
     */
    public List<Map<String, Object>> getSubSensorList(String mainUserId, String subUserId) {
        try {
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("mainUserId", mainUserId);
            param.put("subUserId", subUserId);
            
            List<Map<String, Object>> sensorList = adminMapper.getSubSensorList(param);
            logger.info("부계정 센서 리스트 조회 - 메인: {}, 부계정: {}, 개수: {}", 
                mainUserId, subUserId, sensorList != null ? sensorList.size() : 0);
            
            return sensorList != null ? sensorList : new java.util.ArrayList<Map<String, Object>>();
            
        } catch (Exception e) {
            logger.error("부계정 센서 리스트 조회 실패 - 메인: {}, 부계정: {}, error: {}", 
                mainUserId, subUserId, e.toString());
            return new java.util.ArrayList<Map<String, Object>>();
        }
    }
    
    /**
     * 주계정의 센서 리스트 조회
     * @param userId 사용자 ID
     * @return 센서 리스트
     */
    public List<Map<String, Object>> getMainSensorList(String userId) {
        try {
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("userId", userId);
            
            List<Map<String, Object>> sensorList = adminMapper.getSensorList(param);
            logger.info("주계정 센서 리스트 조회 - 사용자: {}, 개수: {}", 
                userId, sensorList != null ? sensorList.size() : 0);
            
            return sensorList != null ? sensorList : new java.util.ArrayList<Map<String, Object>>();
            
        } catch (Exception e) {
            logger.error("주계정 센서 리스트 조회 실패 - 사용자: {}, error: {}", userId, e.toString());
            return new java.util.ArrayList<Map<String, Object>>();
        }
    }
    
    /**
     * 사용자 등급별 센서 리스트 조회 (통합)
     * @param userId 사용자 ID
     * @return 센서 리스트
     */
    public List<Map<String, Object>> getSensorListByUserType(String userId) {
        try {
            // 부계정 여부 확인
            boolean isSubAccount = isSubAccount(userId);
            
            if (isSubAccount) {
                // 부계정인 경우: 메인 사용자의 센서 리스트 조회
                String mainUserId = getMainUserIdForSubUser(userId);
                if (mainUserId != null) {
                    return getSubSensorList(mainUserId, userId);
                } else {
                    logger.warn("부계정의 메인 사용자 ID를 찾을 수 없어 빈 리스트 반환 - 부계정: {}", userId);
                    return new java.util.ArrayList<Map<String, Object>>();
                }
            } else {
                // 주계정인 경우: 일반 센서 리스트 조회
                return getMainSensorList(userId);
            }
            
        } catch (Exception e) {
            logger.error("사용자 등급별 센서 리스트 조회 실패 - 사용자: {}, error: {}", userId, e.toString());
            return new java.util.ArrayList<Map<String, Object>>();
        }
    }
}

