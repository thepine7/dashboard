-- =====================================================
-- MySQL 5.7 호환 인덱스 생성 스크립트
-- =====================================================
-- MySQL 5.7에서는 DROP INDEX IF EXISTS 문법을 지원하지 않음
-- 안전한 인덱스 생성 방법 사용

-- 1. 기존 인덱스 확인
-- =====================================================
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data%'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 2. 기존 인덱스 삭제 (안전한 방법)
-- =====================================================
-- 각 인덱스를 개별적으로 삭제 (존재하지 않으면 에러 무시)

-- 핵심 복합 인덱스 삭제
DROP INDEX idx_hnt_sensor_data_uuid_inst_dtm ON hnt_sensor_data;
DROP INDEX idx_hnt_sensor_data_user_id_uuid ON hnt_sensor_data;
DROP INDEX idx_hnt_sensor_data_inst_dtm ON hnt_sensor_data;

-- 3. 핵심 인덱스 생성 (단계별)
-- =====================================================

-- 3-1. 가장 중요한 복합 인덱스 (5-10분 소요)
CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
COMMENT '장치별 시간 범위 조회용 복합 인덱스';

-- 3-2. 사용자별 장치 조회용 인덱스 (3-5분 소요)
CREATE INDEX idx_hnt_sensor_data_user_id_uuid 
ON hnt_sensor_data (user_id, uuid) 
COMMENT '사용자별 장치 조회용 복합 인덱스';

-- 3-3. 시간 기반 조회용 인덱스 (10-15분 소요)
CREATE INDEX idx_hnt_sensor_data_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
COMMENT '시간 범위 조회용 인덱스';

-- 4. 인덱스 생성 완료 확인
-- =====================================================
SELECT 
    '인덱스 생성 완료' as status,
    COUNT(*) as total_indexes,
    NOW() as completion_time
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data%';

-- 5. 성능 테스트 쿼리
-- =====================================================
-- 인덱스 생성 후 이 쿼리들의 성능을 확인하세요

-- 테스트 1: 장치별 최근 1일 데이터 조회
EXPLAIN SELECT * FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4' 
  AND inst_dtm >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY inst_dtm DESC
LIMIT 100;

-- 테스트 2: 사용자별 장치 목록 조회
EXPLAIN SELECT DISTINCT uuid FROM hnt_sensor_data 
WHERE user_id = 'thepine';

-- 테스트 3: 차트용 일간 데이터 조회 (30분 단위)
EXPLAIN SELECT 
    DATE_FORMAT(inst_dtm, '%Y-%m-%d %H') as time_group,
    ROUND(AVG(sensor_value), 1) as avg_value
FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4'
  AND inst_dtm >= DATE_SUB(NOW(), INTERVAL 1 DAY)
GROUP BY DATE_FORMAT(inst_dtm, '%Y-%m-%d %H')
ORDER BY time_group;
