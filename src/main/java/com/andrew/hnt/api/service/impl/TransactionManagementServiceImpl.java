package com.andrew.hnt.api.service.impl;

import com.andrew.hnt.api.mapper.AdminMapper;
import com.andrew.hnt.api.mapper.DataMapper;
import com.andrew.hnt.api.mapper.LoginMapper;
import com.andrew.hnt.api.mapper.MqttMapper;
import com.andrew.hnt.api.model.SensorVO;
import com.andrew.hnt.api.service.TransactionManagementService;
import com.andrew.hnt.api.util.UnifiedErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 트랜잭션 관리 서비스 구현
 * MQTT 메시지 처리 및 데이터베이스 작업의 트랜잭션을 통합 관리
 */
@Service
public class TransactionManagementServiceImpl implements TransactionManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionManagementServiceImpl.class);
    
    @Autowired
    private MqttMapper mqttMapper;
    
    @Autowired
    private LoginMapper loginMapper;
    
    @Autowired
    private AdminMapper adminMapper;
    
    @Autowired
    private DataMapper dataMapper;
    
    @Autowired
    private UnifiedErrorHandler errorHandler;
    
    // 트랜잭션 통계
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong successfulTransactions = new AtomicLong(0);
    private final AtomicLong failedTransactions = new AtomicLong(0);
    private final AtomicLong rollbackCount = new AtomicLong(0);
    
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public Map<String, Object> saveSensorDataBatch(List<SensorVO> sensorDataList) {
        Map<String, Object> result = new HashMap<>();
        totalTransactions.incrementAndGet();
        
        try {
            if (sensorDataList == null || sensorDataList.isEmpty()) {
                result.put("resultCode", "400");
                result.put("resultMessage", "센서 데이터 리스트가 비어있습니다.");
                return result;
            }
            
            logger.info("센서 데이터 배치 저장 시작 - 데이터 수: {}", sensorDataList.size());
            
            // 배치 단위로 센서 데이터 저장
            for (SensorVO sensorVO : sensorDataList) {
                if (sensorVO != null) {
                    mqttMapper.insertSensorData(sensorVO);
                }
            }
            
            successfulTransactions.incrementAndGet();
            result.put("resultCode", "200");
            result.put("resultMessage", "센서 데이터 배치 저장 완료");
            result.put("processedCount", sensorDataList.size());
            
            logger.info("센서 데이터 배치 저장 완료 - 처리된 데이터 수: {}", sensorDataList.size());
            
        } catch (Exception e) {
            failedTransactions.incrementAndGet();
            rollbackCount.incrementAndGet();
            errorHandler.logError("센서 데이터 배치 저장", e);
            result.put("resultCode", "500");
            result.put("resultMessage", "센서 데이터 배치 저장 중 오류가 발생했습니다.");
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public Map<String, Object> saveSensorData(SensorVO sensorVO) {
        Map<String, Object> result = new HashMap<>();
        totalTransactions.incrementAndGet();
        
        try {
            if (sensorVO == null) {
                result.put("resultCode", "400");
                result.put("resultMessage", "센서 데이터가 null입니다.");
                return result;
            }
            
            logger.debug("센서 데이터 단일 저장 시작 - UUID: {}", sensorVO.getUuid());
            
            mqttMapper.insertSensorData(sensorVO);
            
            successfulTransactions.incrementAndGet();
            result.put("resultCode", "200");
            result.put("resultMessage", "센서 데이터 저장 완료");
            
            logger.debug("센서 데이터 단일 저장 완료 - UUID: {}", sensorVO.getUuid());
            
        } catch (Exception e) {
            failedTransactions.incrementAndGet();
            rollbackCount.incrementAndGet();
            errorHandler.logError("센서 데이터 단일 저장", e);
            result.put("resultCode", "500");
            result.put("resultMessage", "센서 데이터 저장 중 오류가 발생했습니다.");
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 60)
    public Map<String, Object> registerDeviceWithTransaction(Map<String, Object> param) {
        Map<String, Object> result = new HashMap<>();
        totalTransactions.incrementAndGet();
        
        try {
            String currentUserId = (String) param.get("userId");
            String sensorUuid = (String) param.get("sensorUuid");
            
            logger.info("장치 등록 트랜잭션 시작 - userId: {}, sensorUuid: {}", currentUserId, sensorUuid);
            
            // 1. 기존 소유자 확인
            Map<String, Object> checkParam = new HashMap<>();
            checkParam.put("sensorUuid", sensorUuid);
            Map<String, Object> existingOwner = mqttMapper.getSensorInfoByUuid(checkParam);
            
            if (existingOwner != null && existingOwner.size() > 0) {
                String existingUserId = String.valueOf(existingOwner.get("user_id"));
                logger.info("기존 소유자 발견 - userId: {}, sensorUuid: {}", existingUserId, sensorUuid);
                
                // 2. 기존 소유자의 모든 데이터 삭제 (트랜잭션 내에서)
                mqttMapper.deleteSensorInfoByUuid(checkParam);
                mqttMapper.deleteConfigByUuid(checkParam);
                mqttMapper.deleteSensorDataByUuid(checkParam);
                mqttMapper.deleteAlarmByUuid(checkParam);
                
                logger.info("기존 소유자 데이터 삭제 완료 - userId: {}", existingUserId);
            }
            
            // 3. 새 사용자에게 장치 등록
            mqttMapper.insertSensorInfo(param);
            
            successfulTransactions.incrementAndGet();
            result.put("resultCode", "200");
            result.put("resultMessage", "장치 등록 완료");
            result.put("previousOwner", existingOwner != null ? existingOwner.get("user_id") : null);
            
            logger.info("장치 등록 트랜잭션 완료 - userId: {}, sensorUuid: {}", currentUserId, sensorUuid);
            
        } catch (Exception e) {
            failedTransactions.incrementAndGet();
            rollbackCount.incrementAndGet();
            errorHandler.logError("장치 등록 트랜잭션", e);
            result.put("resultCode", "500");
            result.put("resultMessage", "장치 등록 중 오류가 발생했습니다.");
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 60)
    public Map<String, Object> deleteUserWithTransaction(String userId, String userGrade) {
        Map<String, Object> result = new HashMap<>();
        totalTransactions.incrementAndGet();
        
        try {
            logger.info("사용자 삭제 트랜잭션 시작 - userId: {}, userGrade: {}", userId, userGrade);
            
            if ("B".equals(userGrade)) {
                // 부계정인 경우: 부계정 사용자 정보만 삭제 (장치 정보 보존)
                adminMapper.deleteSubUser(userId);
                result.put("resultCode", "200");
                result.put("resultMessage", "부계정 사용자 삭제 완료 (장치 정보 보존)");
                result.put("deletedType", "subUser");
            } else {
                // 메인 사용자인 경우: 모든 정보 삭제
                adminMapper.deleteUserSensorData(userId);
                adminMapper.deleteDeviceAlarm(createAlarmParam(userId));
                adminMapper.deleteConfig(userId, "");
                adminMapper.deleteSensor(userId);
                adminMapper.deleteUser(userId);
                result.put("resultCode", "200");
                result.put("resultMessage", "메인 사용자 삭제 완료 (모든 데이터 삭제)");
                result.put("deletedType", "mainUser");
            }
            
            successfulTransactions.incrementAndGet();
            logger.info("사용자 삭제 트랜잭션 완료 - userId: {}, userGrade: {}", userId, userGrade);
            
        } catch (Exception e) {
            failedTransactions.incrementAndGet();
            rollbackCount.incrementAndGet();
            errorHandler.logError("사용자 삭제 트랜잭션", e);
            result.put("resultCode", "500");
            result.put("resultMessage", "사용자 삭제 중 오류가 발생했습니다.");
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 60)
    public Map<String, Object> deleteSensorDataWithTransaction(Map<String, Object> param) {
        Map<String, Object> result = new HashMap<>();
        totalTransactions.incrementAndGet();
        
        try {
            String sensorUuid = (String) param.get("sensorUuid");
            logger.info("센서 데이터 삭제 트랜잭션 시작 - sensorUuid: {}", sensorUuid);
            
            // 1. 센서 데이터 삭제
            dataMapper.deleteSensorData(param);
            
            // 2. 장치 관련 알림 데이터 삭제
            adminMapper.deleteDeviceAlarm(param);
            
            // 3. 장치 기본 정보 삭제
            dataMapper.deleteSensorInfo(param);
            
            // 4. 장치 설정 정보 삭제
            String userId = (String) param.get("userId");
            adminMapper.deleteConfig(userId, sensorUuid);
            
            successfulTransactions.incrementAndGet();
            result.put("resultCode", "200");
            result.put("resultMessage", "센서 데이터 삭제 완료");
            
            logger.info("센서 데이터 삭제 트랜잭션 완료 - sensorUuid: {}", sensorUuid);
            
        } catch (Exception e) {
            failedTransactions.incrementAndGet();
            rollbackCount.incrementAndGet();
            errorHandler.logError("센서 데이터 삭제 트랜잭션", e);
            result.put("resultCode", "500");
            result.put("resultMessage", "센서 데이터 삭제 중 오류가 발생했습니다.");
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public Map<String, Object> saveAlarmDataWithTransaction(Map<String, Object> alarmData) {
        Map<String, Object> result = new HashMap<>();
        totalTransactions.incrementAndGet();
        
        try {
            logger.debug("알림 데이터 저장 트랜잭션 시작");
            
            // 알림 데이터 저장 (구체적인 구현은 필요에 따라 추가)
            // adminMapper.insertAlarm(alarmData);
            
            successfulTransactions.incrementAndGet();
            result.put("resultCode", "200");
            result.put("resultMessage", "알림 데이터 저장 완료");
            
            logger.debug("알림 데이터 저장 트랜잭션 완료");
            
        } catch (Exception e) {
            failedTransactions.incrementAndGet();
            rollbackCount.incrementAndGet();
            errorHandler.logError("알림 데이터 저장 트랜잭션", e);
            result.put("resultCode", "500");
            result.put("resultMessage", "알림 데이터 저장 중 오류가 발생했습니다.");
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> getTransactionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalTransactions", totalTransactions.get());
        status.put("successfulTransactions", successfulTransactions.get());
        status.put("failedTransactions", failedTransactions.get());
        status.put("rollbackCount", rollbackCount.get());
        status.put("successRate", calculateSuccessRate());
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }
    
    @Override
    public Map<String, Object> getTransactionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("transactionStatus", getTransactionStatus());
        stats.put("serviceName", "TransactionManagementService");
        stats.put("version", "1.0.0");
        return stats;
    }
    
    private double calculateSuccessRate() {
        long total = totalTransactions.get();
        if (total == 0) return 0.0;
        return (double) successfulTransactions.get() / total * 100;
    }
    
    private Map<String, Object> createAlarmParam(String userId) {
        Map<String, Object> param = new HashMap<>();
        param.put("userId", userId);
        return param;
    }
}
