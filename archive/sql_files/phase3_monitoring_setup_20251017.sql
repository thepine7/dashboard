-- =====================================================
-- Phase 3: 24시간 모니터링 설정
-- =====================================================
-- 작성일: 2025-10-17
-- 목적: 실제 인덱스 사용 패턴 모니터링
-- 기간: 24시간
-- =====================================================

-- 1. 슬로우 쿼리 로그 활성화
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.5;  -- 0.5초 이상 걸리는 쿼리 기록
SET GLOBAL log_queries_not_using_indexes = 'ON';  -- 인덱스 미사용 쿼리 기록

-- 2. 로그 파일 위치 확인
SHOW VARIABLES LIKE 'slow_query_log_file';
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';

-- 3. 현재 인덱스 통계 저장 (모니터링 시작)
CREATE TABLE IF NOT EXISTS hnt_index_stats_before (
    snapshot_time DATETIME,
    index_name VARCHAR(100),
    cardinality BIGINT,
    index_type VARCHAR(50),
    columns TEXT
);

INSERT INTO hnt_index_stats_before
SELECT 
    NOW() as snapshot_time,
    INDEX_NAME,
    MAX(CARDINALITY) as cardinality,
    INDEX_TYPE,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
GROUP BY INDEX_NAME, INDEX_TYPE
ORDER BY INDEX_NAME;

-- 4. 인덱스 사용 통계 조회 (MySQL 5.7 지원)
SELECT 
    OBJECT_SCHEMA as db_name,
    OBJECT_NAME as table_name,
    INDEX_NAME,
    COUNT_STAR as total_access,
    COUNT_READ as total_read,
    COUNT_WRITE as total_write,
    COUNT_FETCH as rows_fetched,
    SUM_TIMER_WAIT/1000000000000 as total_latency_sec
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_SCHEMA = 'hnt' 
  AND OBJECT_NAME = 'hnt_sensor_data'
ORDER BY total_access DESC;

-- 5. 현재 쿼리 실행 상태 확인
SELECT 
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    LEFT(info, 100) as query_preview
FROM information_schema.processlist
WHERE db = 'hnt' 
  AND command != 'Sleep'
ORDER BY time DESC;

-- =====================================================
-- 24시간 후 실행할 분석 쿼리
-- =====================================================

-- 6. 인덱스 통계 비교 (24시간 후)
CREATE TABLE IF NOT EXISTS hnt_index_stats_after (
    snapshot_time DATETIME,
    index_name VARCHAR(100),
    cardinality BIGINT,
    index_type VARCHAR(50),
    columns TEXT
);

INSERT INTO hnt_index_stats_after
SELECT 
    NOW() as snapshot_time,
    INDEX_NAME,
    MAX(CARDINALITY) as cardinality,
    INDEX_TYPE,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
GROUP BY INDEX_NAME, INDEX_TYPE
ORDER BY INDEX_NAME;

-- 7. Cardinality 변화 확인
SELECT 
    b.index_name,
    b.cardinality as before_cardinality,
    a.cardinality as after_cardinality,
    (a.cardinality - b.cardinality) as cardinality_change,
    ROUND((a.cardinality - b.cardinality) * 100.0 / NULLIF(b.cardinality, 0), 2) as change_percent
FROM hnt_index_stats_before b
LEFT JOIN hnt_index_stats_after a ON b.index_name = a.index_name
ORDER BY ABS(cardinality_change) DESC;

-- 8. 인덱스 사용 빈도 확인 (24시간 후)
SELECT 
    INDEX_NAME,
    COUNT_STAR as total_access,
    COUNT_READ as total_read,
    COUNT_WRITE as total_write,
    COUNT_FETCH as rows_fetched,
    ROUND(SUM_TIMER_WAIT/1000000000000, 2) as total_latency_sec,
    ROUND(SUM_TIMER_WAIT/1000000000000/NULLIF(COUNT_STAR, 0), 4) as avg_latency_sec
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_SCHEMA = 'hnt' 
  AND OBJECT_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IS NOT NULL
ORDER BY total_access DESC;

-- 9. 슬로우 쿼리 분석 가이드
-- 슬로우 쿼리 로그 파일 위치:
-- - Windows: C:\ProgramData\MySQL\MySQL Server 5.7\Data\slow-query.log
-- - Linux: /var/log/mysql/slow-query.log
-- 
-- 분석 도구:
-- 1. mysqldumpslow (기본 제공)
-- 2. pt-query-digest (Percona Toolkit)
-- 3. 수동 분석: grep, awk 등

-- 10. 모니터링 종료 (24시간 후)
-- SET GLOBAL slow_query_log = 'OFF';
-- DROP TABLE hnt_index_stats_before;
-- DROP TABLE hnt_index_stats_after;

-- =====================================================
-- 검증 대상 인덱스
-- =====================================================
-- 1. idx_hnt_sensor_data_inst_dtm (inst_dtm)
-- 2. hnt_sensor_data_inst_dtm_IDX (inst_dtm) - 중복 의심
-- 3. hnt_sensor_data_sensor_value_IDX (sensor_value)
-- 4. idx_sensor_data_download_date_range (inst_dtm, user_id, uuid)
-- 5. idx_sensor_data_performance (user_id, sensor_id, uuid, inst_dtm)
-- =====================================================

