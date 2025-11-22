package com.andrew.hnt.api.mqtt;

import com.andrew.hnt.api.model.SensorVO;
import com.andrew.hnt.api.service.impl.MqttServiceImpl;
import com.andrew.hnt.api.service.TransactionManagementService;
import com.andrew.hnt.api.service.RealtimeSyncService;
import com.andrew.hnt.api.service.UnifiedDataConsistencyService;
import com.andrew.hnt.api.service.NotificationService;
import com.andrew.hnt.api.service.AdminService;
import com.andrew.hnt.api.model.NotificationRequest;
import com.andrew.hnt.api.util.DataConsistencyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
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
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AdminService adminService;
    
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
    
    // 알람 중복 발송 방지 (userId_sensorUuid_alarmType -> 마지막 발송 시간)
    private final ConcurrentHashMap<String, Long> lastAlarmSentTime = new ConcurrentHashMap<>();
    private static final long ALARM_COOLDOWN_MS = 10000; // 10초 (테스트용, 운영에서는 300000으로 변경)
    
    // 배치 처리 스레드
    private volatile boolean running = false;
    private Thread batchProcessorThread;
    
    /**
     * 초기화 시 배치 처리 자동 시작
     */
    @javax.annotation.PostConstruct
    public void init() {
        logger.info("=== MqttMessageProcessor 초기화 시작 ===");
        startProcessing();
        logger.info("=== MqttMessageProcessor 초기화 완료 ===");
    }
    
    /**
     * 메시지 처리 시작
     */
    public void startProcessing() {
        if (running) {
            logger.warn("배치 처리가 이미 실행 중입니다");
            return;
        }
        
        running = true;
        batchProcessorThread = new Thread(this::processBatches, "MQTT-BatchProcessor");
        batchProcessorThread.setDaemon(true);
        batchProcessorThread.start();
        
        logger.info("🚀 MQTT 메시지 배치 처리 시작 (스레드: {})", batchProcessorThread.getName());
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
            // thepine 사용자 메시지가 있는지 확인
            boolean hasThepine = batch.stream().anyMatch(s -> "thepine".equals(s.getUserId()));
            if (hasThepine) {
                logger.info("🔔 [thepine] 배치 처리 시작 - 배치 크기: {}", batch.size());
            } else {
                logger.debug("배치 처리 시작 - 배치 크기: {}", batch.size());
            }
            
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
                
                if (hasThepine) {
                    logger.info("🔔 [thepine] 배치 처리 완료 - 처리된 메시지: {}/{}", result.get("processedCount"), batch.size());
                } else {
                    logger.debug("배치 처리 완료 - 처리된 메시지: {}/{}", result.get("processedCount"), batch.size());
                }
                
                // 알림 조건 체크 (각 센서 데이터마다)
                for (SensorVO sensor : batch) {
                    try {
                        checkAndSendNotification(sensor);
                    } catch (Exception e) {
                        logger.error("알림 체크 중 오류 - sensorUuid: {}", sensor.getUuid(), e);
                    }
                }
                
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
     * 알림 조건 체크 및 발송
     * @param sensor 센서 데이터
     */
    private void checkAndSendNotification(SensorVO sensor) {
        try {
            // thepine 사용자만 로그 출력
            boolean isThepine = "thepine".equals(sensor.getUserId());
            
            if (isThepine) {
                logger.info("===============================================");
                logger.info("🔔 [thepine] 알림 조건 체크 시작");
                logger.info("   - UUID: {}", sensor.getUuid());
                logger.info("   - UserId: {}", sensor.getUserId());
                logger.info("   - SensorId: {}", sensor.getSensorId());
                logger.info("   - Value: {}", sensor.getSensorValue());
                logger.info("===============================================");
            }
            
            // 센서 설정 조회
            Map<String, Object> configParam = new HashMap<>();
            configParam.put("userId", sensor.getUserId());
            configParam.put("sensorId", sensor.getSensorId());
            configParam.put("sensorUuid", sensor.getUuid());
            
            if (isThepine) {
                logger.info("[thepine] 센서 설정 조회 시도 - configParam: {}", configParam);
            }
            Map<String, Object> config = adminService.selectSetting(configParam);
            
            if (config == null || config.isEmpty()) {
                if (isThepine) {
                    logger.warn("❌ [thepine] 센서 설정이 없음 - UUID: {}", sensor.getUuid());
                    logger.warn("   - userId: {}", sensor.getUserId());
                    logger.warn("   - sensorId: {}", sensor.getSensorId());
                }
                return; // 설정이 없으면 알림 발송 안 함
            }
            
            if (isThepine) {
                logger.info("✅ [thepine] 센서 설정 조회 성공");
                logger.info("   - UUID: {}", sensor.getUuid());
                logger.info("   - config size: {}", config.size());
                logger.info("   - config keys: {}", config.keySet());
            }
            
            // 알림 설정 확인
            String highAlarmYn = String.valueOf(config.get("alarm_yn1"));
            String lowAlarmYn = String.valueOf(config.get("alarm_yn2"));
            String specificAlarmYn = String.valueOf(config.get("alarm_yn3"));
            String diAlarmYn = String.valueOf(config.get("alarm_yn4"));
            String networkAlarmYn = String.valueOf(config.get("alarm_yn5"));
            
            if (isThepine) {
                logger.info("[thepine] 알림 설정 확인:");
                logger.info("   - 고온 알림: {} (alarm_yn1)", highAlarmYn);
                logger.info("   - 저온 알림: {} (alarm_yn2)", lowAlarmYn);
                logger.info("   - 특정온도 알림: {} (alarm_yn3)", specificAlarmYn);
                logger.info("   - DI 알림: {} (alarm_yn4)", diAlarmYn);
                logger.info("   - 통신이상 알림: {} (alarm_yn5)", networkAlarmYn);
            }
            
            if (!"Y".equals(highAlarmYn) && !"Y".equals(lowAlarmYn) && 
                !"Y".equals(specificAlarmYn) && !"Y".equals(diAlarmYn) && !"Y".equals(networkAlarmYn)) {
                if (isThepine) {
                    logger.warn("❌ [thepine] 알림 설정이 모두 비활성화됨");
                }
                return; // 알림 설정이 비활성화되어 있으면 발송 안 함
            }
            
            // 온도 값 확인
            double currentTemp;
            try {
                currentTemp = Double.parseDouble(sensor.getSensorValue());
                if (isThepine) {
                    logger.info("✅ [thepine] 온도 값 파싱 성공: {}°C", currentTemp);
                }
            } catch (NumberFormatException e) {
                if (isThepine) {
                    logger.warn("❌ [thepine] 온도 값 파싱 실패: {}", sensor.getSensorValue());
                }
                return; // 온도 값이 숫자가 아니면 무시
            }
            
            // 고온 알림 체크
            if ("Y".equals(highAlarmYn)) {
                String highTempStr = String.valueOf(config.get("set_val1"));
                if (isThepine) {
                    logger.info("🔥 [thepine] 고온 알림 체크 시작");
                    logger.info("   - 현재 온도: {}°C", currentTemp);
                    logger.info("   - 설정 온도: {}°C (set_val1)", highTempStr);
                }
                
                try {
                    double highTemp = Double.parseDouble(highTempStr);
                    String alarmKey = sensor.getUserId() + "_" + sensor.getUuid() + "_온도 높음";
                    
                    if (currentTemp > highTemp) {
                        if (isThepine) {
                            logger.info("🔥🔥🔥 [thepine] 고온 알림 조건 충족! 알림 발송");
                        }
                        sendNotification(sensor, "온도 높음", currentTemp, highTemp);
                    } else {
                        // 조건 미충족 시 알람 시간 초기화 (다음 조건 충족 시 즉시 발송)
                        if (lastAlarmSentTime.containsKey(alarmKey)) {
                            lastAlarmSentTime.remove(alarmKey);
                            if (isThepine) {
                                logger.info("   - 조건 미충족 (현재 {} <= 설정 {}) - 알람 시간 초기화", currentTemp, highTemp);
                            }
                        } else {
                            if (isThepine) {
                                logger.info("   - 조건 미충족 (현재 {} <= 설정 {})", currentTemp, highTemp);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (isThepine) {
                        logger.error("[thepine] 고온 알림 체크 실패", e);
                    }
                }
            }
            
            // 저온 알림 체크
            if ("Y".equals(lowAlarmYn)) {
                String lowTempStr = String.valueOf(config.get("set_val2"));
                if (isThepine) {
                    logger.info("❄️ [thepine] 저온 알림 체크 시작");
                    logger.info("   - 현재 온도: {}°C", currentTemp);
                    logger.info("   - 설정 온도: {}°C (set_val2)", lowTempStr);
                }
                
                try {
                    double lowTemp = Double.parseDouble(lowTempStr);
                    String alarmKey = sensor.getUserId() + "_" + sensor.getUuid() + "_온도 낮음";
                    
                    if (currentTemp < lowTemp) {
                        if (isThepine) {
                            logger.info("❄️❄️❄️ [thepine] 저온 알림 조건 충족! 알림 발송");
                        }
                        sendNotification(sensor, "온도 낮음", currentTemp, lowTemp);
                    } else {
                        // 조건 미충족 시 알람 시간 초기화 (다음 조건 충족 시 즉시 발송)
                        if (lastAlarmSentTime.containsKey(alarmKey)) {
                            lastAlarmSentTime.remove(alarmKey);
                            if (isThepine) {
                                logger.info("   - 조건 미충족 (현재 {} >= 설정 {}) - 알람 시간 초기화", currentTemp, lowTemp);
                            }
                        } else {
                            if (isThepine) {
                                logger.info("   - 조건 미충족 (현재 {} >= 설정 {})", currentTemp, lowTemp);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (isThepine) {
                        logger.error("[thepine] 저온 알림 체크 실패", e);
                    }
                }
            }
            
            // 특정온도 알림 체크
            if ("Y".equals(specificAlarmYn)) {
                String specificTempStr = String.valueOf(config.get("set_val3"));
                if (isThepine) {
                    logger.info("🎯 [thepine] 특정온도 알림 체크 시작");
                    logger.info("   - 현재 온도: {}°C", currentTemp);
                    logger.info("   - 설정 온도: {}°C (set_val3)", specificTempStr);
                }
                
                try {
                    double specificTemp = Double.parseDouble(specificTempStr);
                    String alarmKey = sensor.getUserId() + "_" + sensor.getUuid() + "_특정온도";
                    
                    // 특정온도와 일치하면 알람 (오차 범위 ±0.5°C)
                    if (Math.abs(currentTemp - specificTemp) <= 0.5) {
                        if (isThepine) {
                            logger.info("🎯🎯🎯 [thepine] 특정온도 알림 조건 충족! 알림 발송");
                        }
                        sendNotification(sensor, "특정온도", currentTemp, specificTemp);
                    } else {
                        // 조건 미충족 시 알람 시간 초기화
                        if (lastAlarmSentTime.containsKey(alarmKey)) {
                            lastAlarmSentTime.remove(alarmKey);
                            if (isThepine) {
                                logger.info("   - 조건 미충족 (현재 {} ≠ 설정 {}) - 알람 시간 초기화", currentTemp, specificTemp);
                            }
                        } else {
                            if (isThepine) {
                                logger.info("   - 조건 미충족 (현재 {} ≠ 설정 {})", currentTemp, specificTemp);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (isThepine) {
                        logger.error("[thepine] 특정온도 알림 체크 실패", e);
                    }
                }
            }
            
            // DI 알림 체크 (센서 타입이 din인 경우)
            if ("Y".equals(diAlarmYn) && "din".equals(sensor.getSensorType())) {
                String alarmKey = sensor.getUserId() + "_" + sensor.getUuid() + "_DI이상";
                
                if (isThepine) {
                    logger.info("⚠️ [thepine] DI 알림 체크 시작");
                    logger.info("   - 센서 값: {}", sensor.getSensorValue());
                }
                
                try {
                    int dinValue = Integer.parseInt(sensor.getSensorValue());
                    
                    // DIN 값이 1이면 이상 (알람 발송)
                    if (dinValue == 1) {
                        if (isThepine) {
                            logger.info("⚠️⚠️⚠️ [thepine] DI 이상 알림 조건 충족! 알림 발송");
                        }
                        sendNotification(sensor, "DI이상", dinValue, 0);
                    } else {
                        // 조건 미충족 시 알람 시간 초기화
                        if (lastAlarmSentTime.containsKey(alarmKey)) {
                            lastAlarmSentTime.remove(alarmKey);
                            if (isThepine) {
                                logger.info("   - 조건 미충족 (DIN 정상) - 알람 시간 초기화");
                            }
                        } else {
                            if (isThepine) {
                                logger.info("   - 조건 미충족 (DIN 정상)");
                            }
                        }
                    }
                } catch (Exception e) {
                    if (isThepine) {
                        logger.error("[thepine] DI 알림 체크 실패", e);
                    }
                }
            }
            
            // 통신이상 알림 체크 (센서 값이 "Error"인 경우)
            if ("Y".equals(networkAlarmYn)) {
                String alarmKey = sensor.getUserId() + "_" + sensor.getUuid() + "_통신이상";
                
                if (isThepine) {
                    logger.info("📡 [thepine] 통신이상 알림 체크 시작");
                    logger.info("   - 센서 값: {}", sensor.getSensorValue());
                }
                
                // 센서 값이 "Error"이면 통신이상
                if ("Error".equals(sensor.getSensorValue())) {
                    if (isThepine) {
                        logger.info("📡📡📡 [thepine] 통신이상 알림 조건 충족! 알림 발송");
                    }
                    sendNotification(sensor, "통신이상", 0, 0);
                } else {
                    // 조건 미충족 시 알람 시간 초기화
                    if (lastAlarmSentTime.containsKey(alarmKey)) {
                        lastAlarmSentTime.remove(alarmKey);
                        if (isThepine) {
                            logger.info("   - 조건 미충족 (통신 정상) - 알람 시간 초기화");
                        }
                    } else {
                        if (isThepine) {
                            logger.info("   - 조건 미충족 (통신 정상)");
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("알림 조건 체크 실패 - sensorUuid: {}", sensor.getUuid(), e);
        }
    }
    
    /**
     * 알림 발송
     * @param sensor 센서 데이터
     * @param alarmType 알림 타입
     * @param currentTemp 현재 온도
     * @param setTemp 설정 온도
     */
    private void sendNotification(SensorVO sensor, String alarmType, double currentTemp, double setTemp) {
        try {
            boolean isThepine = "thepine".equals(sensor.getUserId());
            
            // DB에서 알람 설정 조회 (지연시간, 재전송간격)
            Map<String, Object> configParam = new HashMap<>();
            configParam.put("userId", sensor.getUserId());
            configParam.put("sensorUuid", sensor.getUuid());
            Map<String, Object> config = adminService.selectSetting(configParam);
            
            // 알람 타입에 따른 지연시간과 재전송간격 추출
            long delayTimeMs = 0; // 지연시간 (초기 알람 발송까지 대기 시간)
            long repeatIntervalMs = 600000; // 재전송반복간격 (기본값: 10분)
            
            if (config != null && !config.isEmpty()) {
                if ("온도 높음".equals(alarmType)) {
                    // 고온 알람 (alarm_yn1, delay_time1, re_delay_time1)
                    String delayTimeStr = String.valueOf(config.get("delay_time1"));
                    String repeatTimeStr = String.valueOf(config.get("re_delay_time1"));
                    
                    if (delayTimeStr != null && !"null".equals(delayTimeStr)) {
                        delayTimeMs = Long.parseLong(delayTimeStr) * 60 * 1000; // 분 -> 밀리초
                    }
                    if (repeatTimeStr != null && !"null".equals(repeatTimeStr)) {
                        repeatIntervalMs = Long.parseLong(repeatTimeStr) * 60 * 1000; // 분 -> 밀리초
                    }
                    
                    if (isThepine) {
                        logger.info("[thepine] 고온 알람 설정: 지연시간={}분, 재전송간격={}분", 
                            delayTimeMs / 60000, repeatIntervalMs / 60000);
                    }
                } else if ("온도 낮음".equals(alarmType)) {
                    // 저온 알람 (alarm_yn2, delay_time2, re_delay_time2)
                    String delayTimeStr = String.valueOf(config.get("delay_time2"));
                    String repeatTimeStr = String.valueOf(config.get("re_delay_time2"));
                    
                    if (delayTimeStr != null && !"null".equals(delayTimeStr)) {
                        delayTimeMs = Long.parseLong(delayTimeStr) * 60 * 1000; // 분 -> 밀리초
                    }
                    if (repeatTimeStr != null && !"null".equals(repeatTimeStr)) {
                        repeatIntervalMs = Long.parseLong(repeatTimeStr) * 60 * 1000; // 분 -> 밀리초
                    }
                    
                    if (isThepine) {
                        logger.info("[thepine] 저온 알람 설정: 지연시간={}분, 재전송간격={}분", 
                            delayTimeMs / 60000, repeatIntervalMs / 60000);
                    }
                }
            }
            
            // 중복 발송 방지 체크 (재전송반복간격 적용)
            String alarmKey = sensor.getUserId() + "_" + sensor.getUuid() + "_" + alarmType;
            Long lastSentTime = lastAlarmSentTime.get(alarmKey);
            long currentTime = System.currentTimeMillis();
            
            if (lastSentTime == null) {
                // 첫 번째 알람: 지연시간 체크
                if (delayTimeMs > 0) {
                    // 지연시간이 설정되어 있으면 대기
                    if (isThepine) {
                        logger.info("[thepine] 첫 알람 - 지연시간 {}분 대기 중 (다음 체크에서 발송)", delayTimeMs / 60000);
                    }
                    lastAlarmSentTime.put(alarmKey, currentTime);
                    return; // 지연시간 동안 대기
                } else {
                    // 지연시간이 0분이면 즉시 발송
                    if (isThepine) {
                        logger.info("[thepine] 첫 알람 - 지연시간 0분, 즉시 발송");
                    }
                    // 발송 후 시간 기록은 아래에서 처리
                }
            } else {
                // 두 번째 이후 알람: 재전송반복간격 체크
                long timeSinceLastAlarm = currentTime - lastSentTime;
                
                if (timeSinceLastAlarm < repeatIntervalMs) {
                    // 재전송간격 이내면 발송하지 않음
                    if (isThepine) {
                        long remainingTime = (repeatIntervalMs - timeSinceLastAlarm) / 60000;
                        logger.info("[thepine] 알람 중복 방지 - 재전송간격 대기 시간: {}분", remainingTime);
                    }
                    return;
                } else {
                    // 재전송간격이 지났으면 발송
                    if (isThepine) {
                        logger.info("[thepine] 재전송간격 {}분 경과 - 알람 재발송", repeatIntervalMs / 60000);
                    }
                    // 발송 후 시간 기록은 아래에서 처리
                }
            }
            
            String message = String.format("%s 장치 - %s (현재: %.1f°C, 설정: %.1f°C)", 
                sensor.getName() != null ? sensor.getName() : sensor.getUuid(),
                alarmType, currentTemp, setTemp);
            
            // FCM 토큰은 hnt_user 테이블에서 조회
            String fcmToken = null;
            try {
                fcmToken = adminService.getUserToken(sensor.getUserId());
                
                if (isThepine) {
                    if (fcmToken != null) {
                        logger.info("[thepine] FCM 토큰 조회 성공 (길이: {})", fcmToken.length());
                    } else {
                        logger.warn("[thepine] FCM 토큰이 없습니다");
                    }
                }
            } catch (Exception e) {
                logger.warn("FCM 토큰 조회 실패 - userId: {}", sensor.getUserId(), e);
            }
            
            NotificationRequest request = new NotificationRequest();
            request.setUserId(sensor.getUserId());
            request.setFcmToken(fcmToken);
            request.setSensorUuid(sensor.getUuid());
            request.setMessage(message);
            request.setAlarmType(alarmType);
            
            boolean success = notificationService.sendDualNotification(request);
            
            if (success) {
                // 알람 발송 성공 시 시간 기록
                lastAlarmSentTime.put(alarmKey, currentTime);
                logger.info("알림 발송 성공 - userId: {}, sensorUuid: {}, type: {}", 
                    sensor.getUserId(), sensor.getUuid(), alarmType);
            } else {
                logger.warn("알림 발송 실패 - userId: {}, sensorUuid: {}, type: {}", 
                    sensor.getUserId(), sensor.getUuid(), alarmType);
            }
            
        } catch (Exception e) {
            logger.error("알림 발송 중 오류 - sensorUuid: {}", sensor.getUuid(), e);
        }
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

