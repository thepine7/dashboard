-- 데이터베이스 쿼리 최적화를 위한 인덱스 생성 스크립트
-- HnT Sensor API 프로젝트 전용
-- 작성일: 2025-10-01

-- =============================================
-- 1. hnt_sensor_data 테이블 인덱스 최적화
-- =============================================

-- 기존 인덱스 확인
SHOW INDEX FROM hnt_sensor_data;

-- 복합 인덱스 생성 (uuid + inst_dtm) - 가장 자주 사용되는 조합
CREATE INDEX IF NOT EXISTS idx_sensor_data_uuid_time 
ON hnt_sensor_data (uuid, inst_dtm);

-- inst_dtm 단일 인덱스 (시간 범위 조회용)
CREATE INDEX IF NOT EXISTS idx_sensor_data_inst_dtm 
ON hnt_sensor_data (inst_dtm);

-- uuid 단일 인덱스 (센서별 조회용)
CREATE INDEX IF NOT EXISTS idx_sensor_data_uuid 
ON hnt_sensor_data (uuid);

-- user_id + uuid 복합 인덱스 (사용자별 센서 조회용)
CREATE INDEX IF NOT EXISTS idx_sensor_data_user_uuid 
ON hnt_sensor_data (user_id, uuid);

-- =============================================
-- 2. hnt_sensor_info 테이블 인덱스 최적화
-- =============================================

-- 기존 인덱스 확인
SHOW INDEX FROM hnt_sensor_info;

-- user_id 인덱스 (사용자별 센서 목록 조회용)
CREATE INDEX IF NOT EXISTS idx_sensor_info_user_id 
ON hnt_sensor_info (user_id);

-- sensor_uuid 인덱스 (센서별 정보 조회용)
CREATE INDEX IF NOT EXISTS idx_sensor_info_sensor_uuid 
ON hnt_sensor_info (sensor_uuid);

-- user_id + sensor_uuid 복합 인덱스 (사용자별 센서 정보 조회용)
CREATE INDEX IF NOT EXISTS idx_sensor_info_user_sensor 
ON hnt_sensor_info (user_id, sensor_uuid);

-- =============================================
-- 3. hnt_user 테이블 인덱스 최적화
-- =============================================

-- 기존 인덱스 확인
SHOW INDEX FROM hnt_user;

-- user_id 인덱스 (사용자 조회용)
CREATE INDEX IF NOT EXISTS idx_user_user_id 
ON hnt_user (user_id);

-- user_grade 인덱스 (등급별 조회용)
CREATE INDEX IF NOT EXISTS idx_user_user_grade 
ON hnt_user (user_grade);

-- =============================================
-- 4. hnt_config 테이블 인덱스 최적화
-- =============================================

-- 기존 인덱스 확인
SHOW INDEX FROM hnt_config;

-- user_id 인덱스 (사용자별 설정 조회용)
CREATE INDEX IF NOT EXISTS idx_config_user_id 
ON hnt_config (user_id);

-- uuid 인덱스 (센서별 설정 조회용)
CREATE INDEX IF NOT EXISTS idx_config_uuid 
ON hnt_config (uuid);

-- user_id + uuid 복합 인덱스 (사용자별 센서 설정 조회용)
CREATE INDEX IF NOT EXISTS idx_config_user_uuid 
ON hnt_config (user_id, uuid);

-- =============================================
-- 5. hnt_alarm 테이블 인덱스 최적화
-- =============================================

-- 기존 인덱스 확인
SHOW INDEX FROM hnt_alarm;

-- user_id 인덱스 (사용자별 알림 조회용)
CREATE INDEX IF NOT EXISTS idx_alarm_user_id 
ON hnt_alarm (user_id);

-- uuid 인덱스 (센서별 알림 조회용)
CREATE INDEX IF NOT EXISTS idx_alarm_uuid 
ON hnt_alarm (uuid);

-- alarm_time 인덱스 (시간별 알림 조회용)
CREATE INDEX IF NOT EXISTS idx_alarm_time 
ON hnt_alarm (alarm_time);

-- =============================================
-- 6. 인덱스 성능 확인 쿼리
-- =============================================

-- 인덱스 사용률 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    PACKED,
    NULLABLE,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
AND TABLE_NAME IN ('hnt_sensor_data', 'hnt_sensor_info', 'hnt_user', 'hnt_config', 'hnt_alarm')
ORDER BY TABLE_NAME, INDEX_NAME;

-- 테이블 크기 확인
SELECT 
    TABLE_NAME,
    ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) AS 'Size (MB)',
    TABLE_ROWS
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'hnt'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;

-- =============================================
-- 7. 쿼리 성능 테스트
-- =============================================

-- selectSensorData 쿼리 성능 테스트
EXPLAIN SELECT 
    date_format(inst_dtm, '%Y-%m-%d') as getDate,
    date_format(inst_dtm, '%Y-%m-%d %H') as inst_dtm,
    round(avg(sensor_value), 1) as sensor_value
FROM hnt_sensor_data USE INDEX (idx_sensor_data_uuid_time)
WHERE uuid = 'test_uuid'
AND inst_dtm BETWEEN '2025-10-01 00:00:00' AND '2025-10-01 23:59:59'
GROUP BY date_format(inst_dtm, '%Y-%m-%d'), date_format(inst_dtm, '%Y-%m-%d %H')
ORDER BY getDate ASC, inst_dtm ASC
LIMIT 200;

-- =============================================
-- 8. 인덱스 최적화 권장사항
-- =============================================

/*
1. 복합 인덱스 순서: 가장 선택성이 높은 컬럼을 앞에 배치
   - idx_sensor_data_uuid_time: uuid (선택성 높음) + inst_dtm (범위 조회)

2. 인덱스 크기 관리: 불필요한 인덱스는 정기적으로 제거
   - 사용하지 않는 인덱스는 DROP INDEX로 제거

3. 통계 정보 업데이트: 정기적으로 ANALYZE TABLE 실행
   - ANALYZE TABLE hnt_sensor_data;
   - ANALYZE TABLE hnt_sensor_info;

4. 쿼리 최적화: EXPLAIN으로 실행 계획 확인
   - 인덱스 사용 여부 확인
   - 전체 테이블 스캔 방지

5. 파티셔닝 고려: 대용량 데이터의 경우 테이블 파티셔닝 검토
   - 날짜별 파티셔닝으로 성능 향상 가능
*/
