package com.andrew.hnt.api.mqtt;

import com.andrew.hnt.api.model.SensorVO;
import com.andrew.hnt.api.service.impl.MqttServiceImpl;
import com.andrew.hnt.api.service.TransactionManagementService;
import com.andrew.hnt.api.service.RealtimeSyncService;
import com.andrew.hnt.api.service.UnifiedDataConsistencyService;
import com.andrew.hnt.api.util.DataConsistencyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

/**
 * MQTT 메시지 처리 최적화
 * 배치 처리 및 메모리 사용량 최적화
 */
@Component
public class MqttMessageProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttMessageProcessor.class);
    
    @Autowired
    private MqttServiceImpl mqttService;
    
    @Autowired
    private MqttHealthChecker healthChecker;
    
    @Autowired
    private TransactionManagementService transactionManagementService;
    
    @Autowired
    private RealtimeSyncService realtimeSyncService;
    
    @Autowired
    private UnifiedDataConsistencyService unifiedDataConsistencyService;
    
    // 메시지 처리 큐 (동적 크기 조정)
    private final BlockingQueue<SensorVO> messageQueue = new LinkedBlockingQueue<>(20000);
    
    // 배치 처리 설정 (성능 최적화)
    private static final int BATCH_SIZE = 200; // 배치 크기 증가
    private static final long BATCH_TIMEOUT_MS = 3000; // 3초로 단축
    private static final int MAX_QUEUE_SIZE = 20000; // 최대 큐 크기
    private static final int QUEUE_WARNING_THRESHOLD = 15000; // 큐 경고 임계값
    
    // 통계 정보
    private final AtomicLong processedMessages = new AtomicLong(0);
    private final AtomicLong batchCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong droppedMessages = new AtomicLong(0);
    private final AtomicLong maxQueueSize = new AtomicLong(0);
    
    // 배치 처리 스레드
    private volatile boolean running = false;
    private Thread batchProcessorThread;
    
    /**
     * 메시지 처리 시작
     */
    public void startProcessing() {
        if (running) {
            return;
        }
        
        running = true;
        batchProcessorThread = new Thread(this::processBatches, "MQTT-BatchProcessor");
        batchProcessorThread.setDaemon(true);
        batchProcessorThread.start();
        
        logger.info("MQTT 메시지 배치 처리 시작");
    }
    
    /**
     * 메시지 처리 중지
     */
    public void stopProcessing() {
        running = false;
        if (batchProcessorThread != null) {
            batchProcessorThread.interrupt();
        }
        logger.info("MQTT 메시지 배치 처리 중지");
    }
    
    /**
     * 메시지 큐에 추가 (개선된 버전)
     * @param sensorVO 센서 데이터
     */
    public void addMessage(SensorVO sensorVO) {
        if (sensorVO == null) {
            logger.warn("null 메시지 무시됨");
            return;
        }
        
        // 큐 크기 모니터링
        int currentQueueSize = messageQueue.size();
        if (currentQueueSize > maxQueueSize.get()) {
            maxQueueSize.set(currentQueueSize);
        }
        
        // 큐 경고 임계값 체크
        if (currentQueueSize > QUEUE_WARNING_THRESHOLD) {
            logger.warn("메시지 큐 크기 경고: {} (임계값: {})", currentQueueSize, QUEUE_WARNING_THRESHOLD);
        }
        
        // 큐가 가득 찬 경우 처리
        if (currentQueueSize >= MAX_QUEUE_SIZE) {
            droppedMessages.incrementAndGet();
            logger.error("메시지 큐가 가득 참 - 메시지 드롭됨 (큐 크기: {}, 드롭된 메시지: {})", 
                currentQueueSize, droppedMessages.get());
            return;
        }
        
        // 메시지 추가
        if (!messageQueue.offer(sensorVO)) {
            droppedMessages.incrementAndGet();
            logger.warn("메시지 큐 추가 실패 - 메시지 드롭됨");
        } else {
            // Health Checker에 메시지 수신 기록
            if (healthChecker != null) {
                healthChecker.recordMessageReceived();
            }
        }
    }
    
    /**
     * 배치 처리 메인 루프
     */
    private void processBatches() {
        List<SensorVO> batch = new ArrayList<>(BATCH_SIZE);
        
        while (running) {
            try {
                // 배치 수집
                SensorVO message = messageQueue.poll(BATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                
                if (message != null) {
                    batch.add(message);
                    
                    // 배치 크기만큼 수집하거나 타임아웃까지 대기
                    while (batch.size() < BATCH_SIZE && running) {
                        SensorVO nextMessage = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                        if (nextMessage != null) {
                            batch.add(nextMessage);
                        } else {
                            break; // 타임아웃
                        }
                    }
                }
                
                // 배치 처리
                if (!batch.isEmpty()) {
                    processBatch(batch);
                    batch.clear();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("배치 처리 중 오류 발생", e);
                errorCount.incrementAndGet();
            }
        }
        
        // 남은 메시지 처리
        if (!batch.isEmpty()) {
            processBatch(batch);
        }
    }
    
    /**
     * 배치 처리 실행 (데이터 일관성 보장)
     * @param batch 처리할 메시지 배치
     */
    private void processBatch(List<SensorVO> batch) {
        if (batch.isEmpty()) {
            return;
        }
        
        try {
            logger.debug("배치 처리 시작 - 배치 크기: {}", batch.size());
            
            // 1. 메시지 배치를 일관성 있는 형태로 변환
            List<DataConsistencyManager.MessageWithId> messageBatch = new ArrayList<>();
            for (int i = 0; i < batch.size(); i++) {
                SensorVO sensor = batch.get(i);
                String messageId = generateMessageId(sensor, i);
                Long sequence = System.currentTimeMillis() + i; // 순서 보장을 위한 시퀀스
                
                messageBatch.add(new DataConsistencyManager.MessageWithId(messageId, sensor, sequence));
            }
            
            // 2. 통합 데이터 일관성 서비스를 통한 배치 처리
            Map<String, Object> result = unifiedDataConsistencyService.processBatchWithConsistency(messageBatch);
            
            if ("200".equals(result.get("resultCode"))) {
                // 통계 업데이트
                processedMessages.addAndGet(batch.size());
                batchCount.incrementAndGet();
                
                logger.debug("배치 처리 완료 - 처리된 메시지: {}/{}", result.get("processedCount"), batch.size());
                
                if (batchCount.get() % 100 == 0) {
                    logger.info("배치 처리 통계 - 처리된 메시지: {}, 배치 수: {}, 오류 수: {}", 
                        processedMessages.get(), batchCount.get(), errorCount.get());
                }
            } else {
                errorCount.addAndGet(batch.size());
                logger.error("배치 처리 실패 - 오류: {}", result.get("resultMessage"));
            }
            
        } catch (Exception e) {
            logger.error("배치 처리 중 예외 발생 - 배치 크기: {}", batch.size(), e);
            errorCount.addAndGet(batch.size());
        }
    }
    
    /**
     * 처리 통계 정보 반환 (개선된 버전)
     * @return 통계 정보
     */
    public String getProcessingStats() {
        return String.format("처리된 메시지: %d, 배치 수: %d, 오류 수: %d, 드롭된 메시지: %d, 큐 크기: %d, 최대 큐 크기: %d", 
            processedMessages.get(), batchCount.get(), errorCount.get(), droppedMessages.get(), 
            messageQueue.size(), maxQueueSize.get());
    }
    
    /**
     * 센서 데이터에서 사용자 ID 추출
     * @param sensor 센서 데이터
     * @return 사용자 ID
     */
    private String extractUserIdFromSensor(SensorVO sensor) {
        // SensorVO에서 사용자 ID 추출 로직
        // 실제 구현은 SensorVO 구조에 따라 달라질 수 있음
        return sensor.getUserId(); // SensorVO에 getUserId() 메서드가 있다고 가정
    }
    
    /**
     * 메시지 ID 생성
     * @param sensor 센서 데이터
     * @param index 배치 내 인덱스
     * @return 메시지 ID
     */
    private String generateMessageId(SensorVO sensor, int index) {
        return String.format("%s_%d_%d", sensor.getUuid(), System.currentTimeMillis(), index);
    }
    
    /**
     * MQTT 메시지 생성
     * @param sensor 센서 데이터
     * @return MQTT 메시지 JSON 문자열
     */
    private String createMqttMessage(SensorVO sensor) {
        // 센서 데이터를 MQTT 메시지 형식으로 변환
        Map<String, Object> message = new HashMap<>();
        message.put("actcode", "live");
        message.put("name", "ain");
        message.put("value", sensor.getSensorValue());
        message.put("timestamp", System.currentTimeMillis());
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message);
        } catch (Exception e) {
            logger.warn("MQTT 메시지 생성 실패", e);
            return "{}";
        }
    }
    
    /**
     * 상세 통계 정보 반환
     * @return 상세 통계 정보
     */
    public Map<String, Object> getDetailedStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("processedMessages", processedMessages.get());
        stats.put("batchCount", batchCount.get());
        stats.put("errorCount", errorCount.get());
        stats.put("droppedMessages", droppedMessages.get());
        stats.put("currentQueueSize", messageQueue.size());
        stats.put("maxQueueSize", maxQueueSize.get());
        stats.put("isProcessing", running);
        stats.put("queueUtilization", (double) messageQueue.size() / MAX_QUEUE_SIZE * 100);
        return stats;
    }
    
    /**
     * 큐 크기 반환
     * @return 현재 큐 크기
     */
    public int getQueueSize() {
        return messageQueue.size();
    }
    
    /**
     * 처리 중인지 확인
     * @return 처리 중 여부
     */
    public boolean isProcessing() {
        return running;
    }
    
    /**
     * 외부에서 호출 가능한 cleanup 메서드
     */
    @PreDestroy
    public void cleanup() {
        logger.info("MQTT 메시지 프로세서 정리 시작");
        running = false;
        
        // 남은 메시지 처리
        if (!messageQueue.isEmpty()) {
            logger.info("남은 메시지 {}개 처리 중...", messageQueue.size());
            processBatches();
        }
        
        logger.info("MQTT 메시지 프로세서 정리 완료");
    }
}

