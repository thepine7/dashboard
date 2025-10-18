-- =====================================================
-- HnT 데이터베이스 현재 인덱스 상태 확인
-- =====================================================

-- 1. hnt_sensor_data 테이블 인덱스 확인
-- =====================================================
SHOW INDEX FROM hnt_sensor_data;

-- 2. hnt_alarm 테이블 인덱스 확인
-- =====================================================
SHOW INDEX FROM hnt_alarm;

-- 3. hnt_config 테이블 인덱스 확인
-- =====================================================
SHOW INDEX FROM hnt_config;

-- 4. hnt_sensor_info 테이블 인덱스 확인
-- =====================================================
SHOW INDEX FROM hnt_sensor_info;

-- 5. hnt_user 테이블 인덱스 확인
-- =====================================================
SHOW INDEX FROM hnt_user;

-- 6. 테이블별 레코드 수 확인
-- =====================================================
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

-- 7. 인덱스 사용 통계 확인 (MySQL 5.7+)
-- =====================================================
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

-- 8. 주요 쿼리 실행 계획 확인
-- =====================================================
-- 현재 온도 조회 쿼리
EXPLAIN SELECT sensor_value 
FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4' 
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR)
  AND raw_data LIKE '%ain%'
ORDER BY inst_dtm DESC 
LIMIT 1;

-- 에러 체크 쿼리
EXPLAIN SELECT COUNT(*) as cnt 
FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4' 
  AND raw_data LIKE '%ain%'
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -5 MINUTE);

-- 센서 데이터 조회 쿼리 (차트용)
EXPLAIN SELECT 
    date_format(inst_dtm, '%Y-%m-%d %H') as inst_dtm,
    round(avg(sensor_value), 1) as sensor_value
FROM hnt_sensor_data
WHERE uuid = '0008DC7553A4'
  AND inst_dtm BETWEEN '2025-09-29 00:00:00' AND '2025-09-29 23:59:59'
GROUP BY date_format(inst_dtm, '%Y-%m-%d %H')
ORDER BY inst_dtm ASC
LIMIT 200;



























