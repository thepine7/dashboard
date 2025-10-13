package com.andrew.hnt.api.service;

import com.andrew.hnt.api.model.SensorVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 트랜잭션 관리 서비스
 * MQTT 메시지 처리 및 데이터베이스 작업의 트랜잭션을 통합 관리
 * 
 * 주요 기능:
 * - MQTT 메시지 배치 처리 트랜잭션
 * - 센서 데이터 저장 트랜잭션
 * - 장치 등록 트랜잭션
 * - 트랜잭션 롤백 정책 관리
 */
public interface TransactionManagementService {
    
    /**
     * MQTT 센서 데이터 배치 저장 (트랜잭션)
     * @param sensorDataList 센서 데이터 리스트
     * @return 처리 결과
     */
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    Map<String, Object> saveSensorDataBatch(List<SensorVO> sensorDataList);
    
    /**
     * MQTT 센서 데이터 단일 저장 (트랜잭션)
     * @param sensorVO 센서 데이터
     * @return 처리 결과
     */
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    Map<String, Object> saveSensorData(SensorVO sensorVO);
    
    /**
     * 장치 등록 트랜잭션 (기존 소유자 데이터 삭제 + 새 소유자 등록)
     * @param param 장치 등록 파라미터
     * @return 처리 결과
     */
    @Transactional(rollbackFor = Exception.class, timeout = 60)
    Map<String, Object> registerDeviceWithTransaction(Map<String, Object> param);
    
    /**
     * 사용자 삭제 트랜잭션 (부계정 vs 메인 사용자 구분)
     * @param userId 사용자 ID
     * @param userGrade 사용자 등급
     * @return 처리 결과
     */
    @Transactional(rollbackFor = Exception.class, timeout = 60)
    Map<String, Object> deleteUserWithTransaction(String userId, String userGrade);
    
    /**
     * 센서 데이터 삭제 트랜잭션 (완전 삭제)
     * @param param 삭제 파라미터
     * @return 처리 결과
     */
    @Transactional(rollbackFor = Exception.class, timeout = 60)
    Map<String, Object> deleteSensorDataWithTransaction(Map<String, Object> param);
    
    /**
     * 알림 데이터 저장 트랜잭션
     * @param alarmData 알림 데이터
     * @return 처리 결과
     */
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    Map<String, Object> saveAlarmDataWithTransaction(Map<String, Object> alarmData);
    
    /**
     * 트랜잭션 상태 확인
     * @return 트랜잭션 상태 정보
     */
    Map<String, Object> getTransactionStatus();
    
    /**
     * 트랜잭션 통계 조회
     * @return 트랜잭션 통계 정보
     */
    Map<String, Object> getTransactionStats();
}
