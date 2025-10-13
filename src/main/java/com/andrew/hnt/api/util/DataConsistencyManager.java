package com.andrew.hnt.api.util;

import com.andrew.hnt.api.model.SensorVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 데이터 일관성 관리자
 * MQTT 메시지 처리와 데이터베이스 저장 간의 일관성을 보장
 */
@Component
public class DataConsistencyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DataConsistencyManager.class);
    
    @Autowired
    private UnifiedErrorHandler errorHandler;
    
    // 메시지 중복 방지를 위한 처리된 메시지 ID 추적
    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();
    
    // 메시지 순서 보장을 위한 시퀀스 번호 관리
    private final Map<String, AtomicLong> messageSequences = new ConcurrentHashMap<>();
    
    // 동시성 제어를 위한 락 관리
    private final Map<String, ReentrantLock> resourceLocks = new ConcurrentHashMap<>();
    
    // 일관성 검증을 위한 통계
    private final AtomicLong totalProcessedMessages = new AtomicLong(0);
    private final AtomicLong duplicateMessages = new AtomicLong(0);
    private final AtomicLong outOfOrderMessages = new AtomicLong(0);
    private final AtomicLong consistencyViolations = new AtomicLong(0);
    private final AtomicLong successfulConsistencyChecks = new AtomicLong(0);
    
    // 메시지 ID 만료 시간 (24시간)
    private static final long MESSAGE_ID_EXPIRY_TIME = 24 * 60 * 60 * 1000;
    
    // 메시지 ID 정리 주기 (1시간)
    private static final long CLEANUP_INTERVAL = 60 * 60 * 1000;
    
    // 마지막 정리 시간
    private volatile long lastCleanupTime = System.currentTimeMillis();
    
    /**
     * 메시지 일관성 검증 및 처리
     * @param messageId 메시지 고유 ID
     * @param sensorData 센서 데이터
     * @param expectedSequence 예상 시퀀스 번호
     * @return 처리 결과
     */
    public ConsistencyResult processMessageWithConsistency(String messageId, SensorVO sensorData, Long expectedSequence) {
        logger.debug("메시지 일관성 검증 시작 - messageId: {}, expectedSequence: {}", messageId, expectedSequence);
        
        try {
            // 1. 메시지 중복 검사
            if (isDuplicateMessage(messageId)) {
                duplicateMessages.incrementAndGet();
                logger.warn("중복 메시지 감지 - messageId: {}", messageId);
                return new ConsistencyResult(false, "중복 메시지", ConsistencyResult.ResultType.DUPLICATE);
            }
            
            // 2. 메시지 순서 검증
            if (!isMessageInOrder(sensorData.getUuid(), expectedSequence)) {
                outOfOrderMessages.incrementAndGet();
                logger.warn("순서가 맞지 않는 메시지 - uuid: {}, expectedSequence: {}, actualSequence: {}", 
                           sensorData.getUuid(), expectedSequence, getCurrentSequence(sensorData.getUuid()));
                return new ConsistencyResult(false, "메시지 순서 오류", ConsistencyResult.ResultType.OUT_OF_ORDER);
            }
            
            // 3. 데이터 무결성 검증
            if (!validateDataIntegrity(sensorData)) {
                consistencyViolations.incrementAndGet();
                logger.warn("데이터 무결성 위반 - sensorData: {}", sensorData);
                return new ConsistencyResult(false, "데이터 무결성 위반", ConsistencyResult.ResultType.INTEGRITY_VIOLATION);
            }
            
            // 4. 리소스 락 획득
            String resourceKey = sensorData.getUuid();
            ReentrantLock lock = getResourceLock(resourceKey);
            
            try {
                lock.lock();
                
                // 5. 메시지 처리
                processedMessageIds.add(messageId);
                updateMessageSequence(sensorData.getUuid(), expectedSequence);
                totalProcessedMessages.incrementAndGet();
                successfulConsistencyChecks.incrementAndGet();
                
                logger.debug("메시지 일관성 검증 성공 - messageId: {}, uuid: {}", messageId, sensorData.getUuid());
                
                return new ConsistencyResult(true, "일관성 검증 성공", ConsistencyResult.ResultType.SUCCESS, sensorData);
                
            } finally {
                lock.unlock();
            }
            
        } catch (Exception e) {
            consistencyViolations.incrementAndGet();
            errorHandler.logError("메시지 일관성 검증 중 오류 발생", e);
            return new ConsistencyResult(false, "일관성 검증 중 오류: " + e.getMessage(), ConsistencyResult.ResultType.ERROR);
        }
    }
    
    /**
     * 배치 메시지 일관성 검증 및 처리
     * @param messageBatch 메시지 배치
     * @return 처리 결과
     */
    public BatchConsistencyResult processBatchWithConsistency(List<MessageWithId> messageBatch) {
        logger.info("배치 메시지 일관성 검증 시작 - 배치 크기: {}", messageBatch.size());
        
        List<SensorVO> validMessages = new ArrayList<>();
        List<ConsistencyResult> results = new ArrayList<>();
        
        try {
            // 1. 배치 내 중복 검사
            Set<String> batchMessageIds = new HashSet<>();
            for (MessageWithId message : messageBatch) {
                if (batchMessageIds.contains(message.getMessageId())) {
                    results.add(new ConsistencyResult(false, "배치 내 중복 메시지", ConsistencyResult.ResultType.DUPLICATE));
                    continue;
                }
                batchMessageIds.add(message.getMessageId());
            }
            
            // 2. 각 메시지별 일관성 검증
            for (MessageWithId message : messageBatch) {
                ConsistencyResult result = processMessageWithConsistency(
                    message.getMessageId(), 
                    message.getSensorData(), 
                    message.getSequence()
                );
                
                results.add(result);
                
                if (result.isValid()) {
                    validMessages.add(message.getSensorData());
                }
            }
            
            // 3. 배치 전체 일관성 검증
            if (!validateBatchConsistency(validMessages)) {
                logger.warn("배치 전체 일관성 검증 실패");
                return new BatchConsistencyResult(false, "배치 일관성 검증 실패", validMessages, results);
            }
            
            logger.info("배치 메시지 일관성 검증 완료 - 유효한 메시지: {}/{}", validMessages.size(), messageBatch.size());
            
            return new BatchConsistencyResult(true, "배치 일관성 검증 성공", validMessages, results);
            
        } catch (Exception e) {
            errorHandler.logError("배치 메시지 일관성 검증 중 오류 발생", e);
            return new BatchConsistencyResult(false, "배치 일관성 검증 중 오류: " + e.getMessage(), validMessages, results);
        }
    }
    
    /**
     * 중복 메시지 검사
     * @param messageId 메시지 ID
     * @return 중복 여부
     */
    private boolean isDuplicateMessage(String messageId) {
        // 정리 작업 수행 (주기적으로)
        cleanupExpiredMessageIds();
        
        return processedMessageIds.contains(messageId);
    }
    
    /**
     * 메시지 순서 검증
     * @param uuid 센서 UUID
     * @param expectedSequence 예상 시퀀스 번호
     * @return 순서 정확성
     */
    private boolean isMessageInOrder(String uuid, Long expectedSequence) {
        if (expectedSequence == null) {
            return true; // 시퀀스 번호가 없으면 순서 검증 생략
        }
        
        AtomicLong currentSequence = messageSequences.get(uuid);
        if (currentSequence == null) {
            messageSequences.put(uuid, new AtomicLong(expectedSequence));
            return true;
        }
        
        return expectedSequence > currentSequence.get();
    }
    
    /**
     * 현재 시퀀스 번호 조회
     * @param uuid 센서 UUID
     * @return 현재 시퀀스 번호
     */
    private long getCurrentSequence(String uuid) {
        AtomicLong sequence = messageSequences.get(uuid);
        return sequence != null ? sequence.get() : 0;
    }
    
    /**
     * 메시지 시퀀스 업데이트
     * @param uuid 센서 UUID
     * @param sequence 시퀀스 번호
     */
    private void updateMessageSequence(String uuid, Long sequence) {
        if (sequence != null) {
            messageSequences.computeIfAbsent(uuid, k -> new AtomicLong(0)).set(sequence);
        }
    }
    
    /**
     * 데이터 무결성 검증
     * @param sensorData 센서 데이터
     * @return 무결성 여부
     */
    private boolean validateDataIntegrity(SensorVO sensorData) {
        if (sensorData == null) {
            return false;
        }
        
        // 필수 필드 검증
        if (sensorData.getUuid() == null || sensorData.getUuid().trim().isEmpty()) {
            return false;
        }
        
        if (sensorData.getSensorValue() == null) {
            return false;
        }
        
        // 센서 값 범위 검증
        try {
            double value = Double.parseDouble(sensorData.getSensorValue());
            if (value < -200 || value > 1000) {
                logger.warn("센서 값이 범위를 벗어남 - uuid: {}, value: {}", sensorData.getUuid(), value);
                return false;
            }
        } catch (NumberFormatException e) {
            if (!"Error".equals(sensorData.getSensorValue())) {
                logger.warn("센서 값이 숫자가 아님 - uuid: {}, value: {}", sensorData.getUuid(), sensorData.getSensorValue());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 배치 일관성 검증
     * @param validMessages 유효한 메시지 목록
     * @return 일관성 여부
     */
    private boolean validateBatchConsistency(List<SensorVO> validMessages) {
        if (validMessages.isEmpty()) {
            return false;
        }
        
        // 배치 내 센서별 메시지 수 검증
        Map<String, Integer> sensorMessageCounts = new HashMap<>();
        for (SensorVO sensor : validMessages) {
            sensorMessageCounts.merge(sensor.getUuid(), 1, Integer::sum);
        }
        
        // 각 센서별로 최대 1개의 메시지만 허용 (실시간 데이터 특성상)
        for (Map.Entry<String, Integer> entry : sensorMessageCounts.entrySet()) {
            if (entry.getValue() > 1) {
                logger.warn("배치 내 센서별 메시지 중복 - uuid: {}, count: {}", entry.getKey(), entry.getValue());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 리소스 락 획득
     * @param resourceKey 리소스 키
     * @return 락 객체
     */
    private ReentrantLock getResourceLock(String resourceKey) {
        return resourceLocks.computeIfAbsent(resourceKey, k -> new ReentrantLock());
    }
    
    /**
     * 만료된 메시지 ID 정리
     */
    private void cleanupExpiredMessageIds() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime < CLEANUP_INTERVAL) {
            return;
        }
        
        // 간단한 정리 로직 (실제로는 더 정교한 구현 필요)
        if (processedMessageIds.size() > 10000) {
            processedMessageIds.clear();
            lastCleanupTime = currentTime;
            logger.info("만료된 메시지 ID 정리 완료");
        }
    }
    
    /**
     * 일관성 통계 조회
     * @return 통계 정보
     */
    public Map<String, Object> getConsistencyStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProcessedMessages", totalProcessedMessages.get());
        stats.put("duplicateMessages", duplicateMessages.get());
        stats.put("outOfOrderMessages", outOfOrderMessages.get());
        stats.put("consistencyViolations", consistencyViolations.get());
        stats.put("successfulConsistencyChecks", successfulConsistencyChecks.get());
        stats.put("activeMessageSequences", messageSequences.size());
        stats.put("activeResourceLocks", resourceLocks.size());
        stats.put("processedMessageIdsSize", processedMessageIds.size());
        return stats;
    }
    
    /**
     * 일관성 검증 결과 클래스
     */
    public static class ConsistencyResult {
        private final boolean valid;
        private final String message;
        private final ResultType resultType;
        private final SensorVO sensorData;
        
        public enum ResultType {
            SUCCESS, DUPLICATE, OUT_OF_ORDER, INTEGRITY_VIOLATION, ERROR
        }
        
        public ConsistencyResult(boolean valid, String message, ResultType resultType) {
            this(valid, message, resultType, null);
        }
        
        public ConsistencyResult(boolean valid, String message, ResultType resultType, SensorVO sensorData) {
            this.valid = valid;
            this.message = message;
            this.resultType = resultType;
            this.sensorData = sensorData;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public ResultType getResultType() { return resultType; }
        public SensorVO getSensorData() { return sensorData; }
    }
    
    /**
     * 배치 일관성 검증 결과 클래스
     */
    public static class BatchConsistencyResult {
        private final boolean valid;
        private final String message;
        private final List<SensorVO> validMessages;
        private final List<ConsistencyResult> results;
        
        public BatchConsistencyResult(boolean valid, String message, List<SensorVO> validMessages, List<ConsistencyResult> results) {
            this.valid = valid;
            this.message = message;
            this.validMessages = validMessages;
            this.results = results;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<SensorVO> getValidMessages() { return validMessages; }
        public List<ConsistencyResult> getResults() { return results; }
    }
    
    /**
     * 메시지와 ID를 포함하는 클래스
     */
    public static class MessageWithId {
        private final String messageId;
        private final SensorVO sensorData;
        private final Long sequence;
        
        public MessageWithId(String messageId, SensorVO sensorData, Long sequence) {
            this.messageId = messageId;
            this.sensorData = sensorData;
            this.sequence = sequence;
        }
        
        public String getMessageId() { return messageId; }
        public SensorVO getSensorData() { return sensorData; }
        public Long getSequence() { return sequence; }
    }
}
