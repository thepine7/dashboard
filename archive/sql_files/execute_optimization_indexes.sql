-- =====================================================
-- HnT Sensor API 최적화 인덱스 실행 스크립트
-- =====================================================
-- 실행 전 반드시 백업을 수행하세요
-- 실행 순서: 1. 백업 → 2. 인덱스 생성 → 3. 성능 확인

-- 1. 기존 인덱스 확인
-- =====================================================
SHOW INDEX FROM hnt_sensor_data;
SHOW INDEX FROM hnt_alarm;
SHOW INDEX FROM hnt_config;

-- 2. raw_data 컬럼 인덱스 생성 (LIKE 검색 최적화)
-- =====================================================
-- 쿼리 패턴: WHERE raw_data LIKE '%ain%'
-- 예상 성능 향상: 5-10배
CREATE INDEX idx_hnt_sensor_data_raw_data 
ON hnt_sensor_data (raw_data) 
COMMENT 'raw_data LIKE 검색 최적화용 인덱스';

-- 3. 복합 인덱스 최적화 (uuid + raw_data + inst_dtm)
-- =====================================================
-- 쿼리 패턴: WHERE uuid = ? AND raw_data LIKE '%ain%' AND inst_dtm >= ?
-- 예상 성능 향상: 10-50배
CREATE INDEX idx_hnt_sensor_data_uuid_raw_inst 
ON hnt_sensor_data (uuid, raw_data, inst_dtm) 
COMMENT 'uuid + raw_data + inst_dtm 복합 인덱스';

-- 4. 알림 테이블 인덱스 최적화
-- =====================================================
-- 사용자별 센서별 알림 조회 최적화
CREATE INDEX idx_hnt_alarm_user_sensor 
ON hnt_alarm (user_id, sensor_uuid) 
COMMENT '사용자별 센서별 알림 조회용 인덱스';

-- 알림 시간 기반 조회 최적화
CREATE INDEX idx_hnt_alarm_time 
ON hnt_alarm (alarm_time) 
COMMENT '알림 시간 기반 조회용 인덱스';

-- 복합 인덱스 (user_id + sensor_uuid + alarm_time)
CREATE INDEX idx_hnt_alarm_user_sensor_time 
ON hnt_alarm (user_id, sensor_uuid, alarm_time) 
COMMENT '사용자별 센서별 시간 기반 알림 조회용 복합 인덱스';

-- 5. 설정 테이블 인덱스 최적화
-- =====================================================
-- 센서별 설정 조회 최적화
CREATE INDEX idx_hnt_config_sensor_uuid 
ON hnt_config (sensor_uuid) 
COMMENT '센서별 설정 조회용 인덱스';

-- 사용자별 설정 조회 최적화
CREATE INDEX idx_hnt_config_user_id 
ON hnt_config (user_id) 
COMMENT '사용자별 설정 조회용 인덱스';

-- 6. 센서 정보 테이블 인덱스 최적화
-- =====================================================
-- 센서 UUID 조회 최적화
CREATE INDEX idx_hnt_sensor_info_uuid 
ON hnt_sensor_info (sensor_uuid) 
COMMENT '센서 UUID 조회용 인덱스';

-- 센서 소유자 조회 최적화
CREATE INDEX idx_hnt_sensor_info_sensor_id 
ON hnt_sensor_info (sensor_id) 
COMMENT '센서 소유자 조회용 인덱스';

-- 7. 사용자 테이블 인덱스 최적화
-- =====================================================
-- 사용자 등급별 조회 최적화
CREATE INDEX idx_hnt_user_grade 
ON hnt_user (user_grade) 
COMMENT '사용자 등급별 조회용 인덱스';

-- 삭제되지 않은 사용자 조회 최적화
CREATE INDEX idx_hnt_user_del_yn 
ON hnt_user (del_yn) 
COMMENT '삭제되지 않은 사용자 조회용 인덱스';

-- 8. 인덱스 생성 완료 후 확인
-- =====================================================
SHOW INDEX FROM hnt_sensor_data;
SHOW INDEX FROM hnt_alarm;
SHOW INDEX FROM hnt_config;
SHOW INDEX FROM hnt_sensor_info;
SHOW INDEX FROM hnt_user;

-- 9. 쿼리 실행 계획 확인 (성능 테스트용)
-- =====================================================
-- 현재 온도 조회 쿼리 최적화 확인
EXPLAIN SELECT sensor_value 
FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4' 
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR)
  AND raw_data LIKE '%ain%'
ORDER BY inst_dtm DESC 
LIMIT 1;

-- 에러 체크 쿼리 최적화 확인
EXPLAIN SELECT COUNT(*) as cnt 
FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4' 
  AND raw_data LIKE '%ain%'
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -5 MINUTE);

-- 센서 데이터 조회 쿼리 최적화 확인
EXPLAIN SELECT 
    date_format(inst_dtm, '%Y-%m-%d %H') as inst_dtm,
    round(avg(sensor_value), 1) as sensor_value
FROM hnt_sensor_data
WHERE uuid = '0008DC7553A4'
  AND inst_dtm BETWEEN '2025-09-29 00:00:00' AND '2025-09-29 23:59:59'
GROUP BY date_format(inst_dtm, '%Y-%m-%d %H')
ORDER BY inst_dtm ASC
LIMIT 200;

-- 10. 성능 모니터링 쿼리
-- =====================================================
-- 테이블별 레코드 수 확인
SELECT 
    'hnt_sensor_data' as table_name, 
    COUNT(*) as record_count 
FROM hnt_sensor_data
UNION ALL
SELECT 
    'hnt_sensor_info' as table_name, 
    COUNT(*) as record_count 
FROM hnt_sensor_info
UNION ALL
SELECT 
    'hnt_user' as table_name, 
    COUNT(*) as record_count 
FROM hnt_user
UNION ALL
SELECT 
    'hnt_config' as table_name, 
    COUNT(*) as record_count 
FROM hnt_config
UNION ALL
SELECT 
    'hnt_alarm' as table_name, 
    COUNT(*) as record_count 
FROM hnt_alarm;

-- 11. 인덱스 사용률 확인 (MySQL 5.7+)
-- =====================================================
-- 인덱스 사용 통계 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    PACKED,
    NULLABLE,
    INDEX_TYPE,
    COMMENT
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME IN ('hnt_sensor_data', 'hnt_alarm', 'hnt_config', 'hnt_sensor_info', 'hnt_user')
ORDER BY TABLE_NAME, SEQ_IN_INDEX;

























