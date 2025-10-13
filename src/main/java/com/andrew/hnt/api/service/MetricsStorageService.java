package com.andrew.hnt.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 메트릭 저장 서비스 인터페이스
 * 수집된 성능 메트릭을 영구 저장하고 조회하는 기능 제공
 */
public interface MetricsStorageService {
    
    /**
     * 메트릭 저장
     * @param metrics 수집된 메트릭 데이터
     * @param timestamp 수집 시간
     */
    void saveMetrics(Map<String, Object> metrics, LocalDateTime timestamp);
    
    /**
     * 메트릭 조회 (시간 범위)
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 메트릭 데이터 목록
     */
    List<Map<String, Object>> getMetrics(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 최신 메트릭 조회
     * @return 최신 메트릭 데이터
     */
    Map<String, Object> getLatestMetrics();
    
    /**
     * 메트릭 통계 조회
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 메트릭 통계 데이터
     */
    Map<String, Object> getMetricsStatistics(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 오래된 메트릭 데이터 정리
     * @param retentionDays 보존 기간 (일)
     * @return 정리된 레코드 수
     */
    int cleanupOldMetrics(int retentionDays);
    
    /**
     * 메트릭 저장소 상태 조회
     * @return 저장소 상태 정보
     */
    Map<String, Object> getStorageStatus();
}
