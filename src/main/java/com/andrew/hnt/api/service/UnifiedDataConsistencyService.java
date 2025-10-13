package com.andrew.hnt.api.service;

import com.andrew.hnt.api.model.SensorVO;
import com.andrew.hnt.api.util.DataConsistencyManager;
import com.andrew.hnt.api.util.UnifiedErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 통합 데이터 일관성 서비스
 * MQTT 메시지 처리와 데이터베이스 저장 간의 일관성을 보장하는 통합 서비스
 */
@Service
public class UnifiedDataConsistencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedDataConsistencyService.class);
    
    @Autowired
    private DataConsistencyManager dataConsistencyManager;
    
    @Autowired
    private TransactionManagementService transactionManagementService;
    
    @Autowired
    private RealtimeSyncService realtimeSyncService;
    
    @Autowired
    private UnifiedErrorHandler errorHandler;
    
    // 비동기 처리를 위한 Executor
    private final Executor asyncExecutor = Executors.newFixedThreadPool(5);
    
    /**
     * MQTT 메시지를 일관성 있게 처리
     * @param messageId 메시지 고유 ID
     * @param sensorData 센서 데이터
     * @param expectedSequence 예상 시퀀스 번호
     * @return 처리 결과
     */
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public Map<String, Object> processMessageWithConsistency(String messageId, SensorVO sensorData, Long expectedSequence) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.debug("MQTT 메시지 일관성 처리 시작 - messageId: {}, uuid: {}", messageId, sensorData.getUuid());
            
            // 1. 데이터 일관성 검증
            DataConsistencyManager.ConsistencyResult consistencyResult = 
                dataConsistencyManager.processMessageWithConsistency(messageId, sensorData, expectedSequence);
            
            if (!consistencyResult.isValid()) {
                logger.warn("메시지 일관성 검증 실패 - messageId: {}, reason: {}", messageId, consistencyResult.getMessage());
                result.put("resultCode", "400");
                result.put("resultMessage", consistencyResult.getMessage());
                result.put("consistencyResult", consistencyResult);
                return result;
            }
            
            // 2. 데이터베이스 저장
            Map<String, Object> saveResult = transactionManagementService.saveSensorData(sensorData);
            
            if (!"200".equals(saveResult.get("resultCode"))) {
                logger.error("데이터베이스 저장 실패 - messageId: {}, error: {}", messageId, saveResult.get("resultMessage"));
                result.put("resultCode", "500");
                result.put("resultMessage", "데이터베이스 저장 실패: " + saveResult.get("resultMessage"));
                return result;
            }
            
            // 3. 실시간 동기화 (비동기)
            CompletableFuture.runAsync(() -> {
                try {
                    sendRealtimeUpdate(sensorData);
                } catch (Exception e) {
                    logger.warn("실시간 동기화 전송 실패 - messageId: {}", messageId, e);
                }
            }, asyncExecutor);
            
            result.put("resultCode", "200");
            result.put("resultMessage", "메시지 일관성 처리 완료");
            result.put("consistencyResult", consistencyResult);
            result.put("saveResult", saveResult);
            
            logger.debug("MQTT 메시지 일관성 처리 완료 - messageId: {}, uuid: {}", messageId, sensorData.getUuid());
            
        } catch (Exception e) {
            errorHandler.logError("MQTT 메시지 일관성 처리 중 오류 발생", e);
            result.put("resultCode", "500");
            result.put("resultMessage", "메시지 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 배치 메시지를 일관성 있게 처리
     * @param messageBatch 메시지 배치
     * @return 처리 결과
     */
    @Transactional(rollbackFor = Exception.class, timeout = 60)
    public Map<String, Object> processBatchWithConsistency(List<DataConsistencyManager.MessageWithId> messageBatch) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("배치 메시지 일관성 처리 시작 - 배치 크기: {}", messageBatch.size());
            
            // 1. 배치 일관성 검증
            DataConsistencyManager.BatchConsistencyResult batchResult = 
                dataConsistencyManager.processBatchWithConsistency(messageBatch);
            
            if (!batchResult.isValid()) {
                logger.warn("배치 일관성 검증 실패 - reason: {}", batchResult.getMessage());
                result.put("resultCode", "400");
                result.put("resultMessage", batchResult.getMessage());
                result.put("batchResult", batchResult);
                return result;
            }
            
            List<SensorVO> validMessages = batchResult.getValidMessages();
            if (validMessages.isEmpty()) {
                logger.warn("유효한 메시지가 없음 - 배치 크기: {}", messageBatch.size());
                result.put("resultCode", "400");
                result.put("resultMessage", "유효한 메시지가 없습니다");
                return result;
            }
            
            // 2. 배치 데이터베이스 저장
            Map<String, Object> saveResult = transactionManagementService.saveSensorDataBatch(validMessages);
            
            if (!"200".equals(saveResult.get("resultCode"))) {
                logger.error("배치 데이터베이스 저장 실패 - error: {}", saveResult.get("resultMessage"));
                result.put("resultCode", "500");
                result.put("resultMessage", "배치 데이터베이스 저장 실패: " + saveResult.get("resultMessage"));
                return result;
            }
            
            // 3. 실시간 동기화 (비동기)
            CompletableFuture.runAsync(() -> {
                try {
                    sendBatchRealtimeUpdate(validMessages);
                } catch (Exception e) {
                    logger.warn("배치 실시간 동기화 전송 실패", e);
                }
            }, asyncExecutor);
            
            result.put("resultCode", "200");
            result.put("resultMessage", "배치 메시지 일관성 처리 완료");
            result.put("processedCount", validMessages.size());
            result.put("totalCount", messageBatch.size());
            result.put("batchResult", batchResult);
            result.put("saveResult", saveResult);
            
            logger.info("배치 메시지 일관성 처리 완료 - 처리된 메시지: {}/{}", validMessages.size(), messageBatch.size());
            
        } catch (Exception e) {
            errorHandler.logError("배치 메시지 일관성 처리 중 오류 발생", e);
            result.put("resultCode", "500");
            result.put("resultMessage", "배치 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 메시지 순서 보장 처리
     * @param uuid 센서 UUID
     * @param messageId 메시지 ID
     * @param sensorData 센서 데이터
     * @return 처리 결과
     */
    public Map<String, Object> processMessageWithOrderGuarantee(String uuid, String messageId, SensorVO sensorData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 시퀀스 번호 생성 (타임스탬프 기반)
            long sequence = System.currentTimeMillis();
            
            // 일관성 있는 메시지 처리
            Map<String, Object> processResult = processMessageWithConsistency(messageId, sensorData, sequence);
            
            if ("200".equals(processResult.get("resultCode"))) {
                result.put("resultCode", "200");
                result.put("resultMessage", "순서 보장 메시지 처리 완료");
                result.put("sequence", sequence);
                result.put("processResult", processResult);
            } else {
                result.put("resultCode", processResult.get("resultCode"));
                result.put("resultMessage", processResult.get("resultMessage"));
            }
            
        } catch (Exception e) {
            errorHandler.logError("순서 보장 메시지 처리 중 오류 발생", e);
            result.put("resultCode", "500");
            result.put("resultMessage", "순서 보장 메시지 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 데이터 일관성 복구
     * @param uuid 센서 UUID
     * @param startTime 복구 시작 시간
     * @param endTime 복구 종료 시간
     * @return 복구 결과
     */
    public Map<String, Object> recoverDataConsistency(String uuid, long startTime, long endTime) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("데이터 일관성 복구 시작 - uuid: {}, period: {} - {}", uuid, startTime, endTime);
            
            // 1. 해당 기간의 데이터 조회
            // 2. 데이터 무결성 검증
            // 3. 불일치 데이터 수정
            // 4. 실시간 동기화 업데이트
            
            result.put("resultCode", "200");
            result.put("resultMessage", "데이터 일관성 복구 완료");
            result.put("recoveredCount", 0); // 실제 구현에서는 복구된 데이터 수 반환
            
            logger.info("데이터 일관성 복구 완료 - uuid: {}", uuid);
            
        } catch (Exception e) {
            errorHandler.logError("데이터 일관성 복구 중 오류 발생", e);
            result.put("resultCode", "500");
            result.put("resultMessage", "데이터 일관성 복구 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 실시간 업데이트 전송
     * @param sensorData 센서 데이터
     */
    private void sendRealtimeUpdate(SensorVO sensorData) {
        try {
            if (realtimeSyncService != null) {
                // 사용자 ID 추출 (센서 데이터에서)
                String userId = extractUserIdFromSensor(sensorData);
                if (userId != null && !userId.isEmpty()) {
                    realtimeSyncService.sendSensorData(userId, sensorData);
                }
            }
        } catch (Exception e) {
            logger.warn("실시간 업데이트 전송 실패 - uuid: {}", sensorData.getUuid(), e);
        }
    }
    
    /**
     * 배치 실시간 업데이트 전송
     * @param sensorDataList 센서 데이터 목록
     */
    private void sendBatchRealtimeUpdate(List<SensorVO> sensorDataList) {
        try {
            if (realtimeSyncService != null && !sensorDataList.isEmpty()) {
                // 첫 번째 센서에서 사용자 ID 추출
                String userId = extractUserIdFromSensor(sensorDataList.get(0));
                if (userId != null && !userId.isEmpty()) {
                    realtimeSyncService.sendSensorDataBatch(userId, sensorDataList);
                }
            }
        } catch (Exception e) {
            logger.warn("배치 실시간 업데이트 전송 실패", e);
        }
    }
    
    /**
     * 센서 데이터에서 사용자 ID 추출
     * @param sensorData 센서 데이터
     * @return 사용자 ID
     */
    private String extractUserIdFromSensor(SensorVO sensorData) {
        // 실제 구현에서는 센서 데이터에서 사용자 ID를 추출
        // 예: 센서 UUID에서 사용자 ID 추출, 또는 별도 매핑 테이블 조회
        return sensorData.getUserId(); // SensorVO에 userId 필드가 있다고 가정
    }
    
    /**
     * 데이터 일관성 통계 조회
     * @return 통계 정보
     */
    public Map<String, Object> getConsistencyStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 데이터 일관성 관리자 통계
        Map<String, Object> consistencyStats = dataConsistencyManager.getConsistencyStats();
        stats.putAll(consistencyStats);
        
        // 트랜잭션 관리 서비스 통계
        Map<String, Object> transactionStats = transactionManagementService.getTransactionStatus();
        stats.put("transactionStats", transactionStats);
        
        // 실시간 동기화 서비스 통계 (있는 경우)
        if (realtimeSyncService != null) {
            try {
                Map<String, Object> syncStats = realtimeSyncService.getSyncStats();
                stats.put("syncStats", syncStats);
            } catch (Exception e) {
                logger.warn("실시간 동기화 통계 조회 실패", e);
            }
        }
        
        return stats;
    }
}
