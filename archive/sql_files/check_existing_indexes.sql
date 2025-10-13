-- 기존 인덱스 확인 스크립트
-- HnT Sensor API 프로젝트 전용
-- 작성일: 2025-10-01

-- =============================================
-- 1. 모든 테이블의 기존 인덱스 확인
-- =============================================

-- hnt_sensor_data 테이블 인덱스 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
AND TABLE_NAME = 'hnt_sensor_data'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- hnt_sensor_info 테이블 인덱스 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
AND TABLE_NAME = 'hnt_sensor_info'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- hnt_user 테이블 인덱스 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
AND TABLE_NAME = 'hnt_user'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- hnt_config 테이블 인덱스 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
AND TABLE_NAME = 'hnt_config'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- hnt_alarm 테이블 인덱스 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
AND TABLE_NAME = 'hnt_alarm'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- =============================================
-- 2. 기존 인덱스 활용을 위한 쿼리 최적화
-- =============================================

-- 기존 인덱스 기반 쿼리 실행 계획 확인
EXPLAIN SELECT 
    date_format(inst_dtm, '%Y-%m-%d') as getDate,
    date_format(inst_dtm, '%Y-%m-%d %H') as inst_dtm,
    round(avg(sensor_value), 1) as sensor_value
FROM hnt_sensor_data
WHERE uuid = 'test_uuid'
AND inst_dtm BETWEEN '2025-10-01 00:00:00' AND '2025-10-01 23:59:59'
GROUP BY date_format(inst_dtm, '%Y-%m-%d'), date_format(inst_dtm, '%Y-%m-%d %H')
ORDER BY getDate ASC, inst_dtm ASC
LIMIT 200;

-- =============================================
-- 3. 인덱스 사용률 확인
-- =============================================

-- 인덱스 사용 통계 확인
SELECT 
    TABLE_SCHEMA,
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    PACKED,
    NULLABLE,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
ORDER BY TABLE_NAME, INDEX_NAME;