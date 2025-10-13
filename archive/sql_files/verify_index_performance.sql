-- 인덱스 성능 검증 쿼리
-- 부계정 관련 쿼리의 성능을 측정하고 인덱스 효과를 확인

-- 1. 부계정 여부 확인 쿼리 성능 테스트
EXPLAIN SELECT 
    COUNT(*) > 0 as is_sub_account
FROM hnt_sensor_info 
WHERE user_id = 'thepine7' 
  AND user_id != sensor_id 
  AND del_yn = 'N';

-- 2. 부계정의 메인 사용자 ID 조회 성능 테스트
EXPLAIN SELECT sensor_id
FROM hnt_sensor_info
WHERE user_id = 'thepine7' 
  AND user_id != sensor_id 
  AND del_yn = 'N'
LIMIT 1;

-- 3. 센서 목록 조회 성능 테스트
EXPLAIN SELECT
    s.user_id,
    s.sensor_id,
    s.sensor_uuid,
    s.sensor_name,
    s.sensor_loc,
    s.sensor_type,
    s.sensor_gu,
    s.chart_type
FROM hnt_sensor_info s
WHERE s.sensor_id = 'thepine'
  AND s.user_id = s.sensor_id
  AND s.del_yn = 'N'
ORDER BY s.sensor_name;

-- 4. 센서 데이터 조회 성능 테스트 (최근 1일)
EXPLAIN SELECT 
    DATE_FORMAT(inst_dtm, '%Y-%m-%d %H:%i') as time_key,
    ROUND(AVG(sensor_value), 1) as avg_value
FROM hnt_sensor_data 
WHERE user_id = 'thepine' 
  AND sensor_id = 'thepine'
  AND inst_dtm >= DATE_SUB(NOW(), INTERVAL 1 DAY)
GROUP BY DATE_FORMAT(inst_dtm, '%Y-%m-%d %H:%i')
ORDER BY time_key DESC;

-- 5. 설정 정보 조회 성능 테스트
EXPLAIN SELECT *
FROM hnt_config 
WHERE user_id = 'thepine' 
  AND sensor_uuid = '0008DC7553A4'
  AND del_yn = 'N';

-- 6. 알림 데이터 조회 성능 테스트
EXPLAIN SELECT *
FROM hnt_alarm 
WHERE user_id = 'thepine' 
  AND sensor_uuid = '0008DC7553A4'
  AND del_yn = 'N'
ORDER BY inst_dtm DESC
LIMIT 10;

-- 7. 인덱스 사용 통계 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    PACKED,
    NULLABLE,
    INDEX_TYPE,
    COMMENT
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME IN ('hnt_sensor_info', 'hnt_user', 'hnt_sensor_data', 'hnt_config', 'hnt_alarm')
  AND INDEX_NAME LIKE 'idx_%'
ORDER BY TABLE_NAME, INDEX_NAME;

-- 8. 테이블별 인덱스 개수 확인
SELECT 
    TABLE_NAME,
    COUNT(*) as index_count,
    GROUP_CONCAT(INDEX_NAME ORDER BY INDEX_NAME SEPARATOR ', ') as index_names
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME IN ('hnt_sensor_info', 'hnt_user', 'hnt_sensor_data', 'hnt_config', 'hnt_alarm')
GROUP BY TABLE_NAME
ORDER BY TABLE_NAME;

-- 9. 인덱스 크기 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    ROUND(((INDEX_LENGTH) / 1024 / 1024), 2) AS 'Index Size (MB)'
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME IN ('hnt_sensor_info', 'hnt_user', 'hnt_sensor_data', 'hnt_config', 'hnt_alarm')
ORDER BY INDEX_LENGTH DESC;

-- 10. 쿼리 성능 비교 (인덱스 사용 전후)
-- 실제 실행 시간을 측정하기 위한 쿼리
SET @start_time = NOW(6);

-- 부계정 여부 확인 쿼리 실행
SELECT 
    COUNT(*) > 0 as is_sub_account
FROM hnt_sensor_info 
WHERE user_id = 'thepine7' 
  AND user_id != sensor_id 
  AND del_yn = 'N';

SET @end_time = NOW(6);
SELECT 
    TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 1000 as execution_time_ms;
