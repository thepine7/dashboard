-- =====================================================
-- HnT 센서 데이터 효율적 인덱스 생성 쿼리
-- =====================================================
-- 총 레코드: 36,509,835건 (22개월간)
-- 최적화 전략: 월별 단계적 인덱스 생성 + 복합 인덱스

-- 1. 기존 인덱스 확인 및 삭제
-- =====================================================
-- 기존 인덱스가 있다면 삭제 (안전하게)
DROP INDEX IF EXISTS idx_hnt_sensor_data_uuid_inst_dtm ON hnt_sensor_data;
DROP INDEX IF EXISTS idx_hnt_sensor_data_inst_dtm ON hnt_sensor_data;
DROP INDEX IF EXISTS idx_hnt_sensor_data_user_id_uuid ON hnt_sensor_data;
DROP INDEX IF EXISTS idx_hnt_sensor_data_uuid ON hnt_sensor_data;
DROP INDEX IF EXISTS idx_hnt_sensor_data_inst_dtm_uuid ON hnt_sensor_data;

-- 2. 핵심 복합 인덱스 생성 (가장 중요한 쿼리용)
-- =====================================================
-- 쿼리 패턴: WHERE uuid = ? AND inst_dtm BETWEEN ? AND ?
-- 이 인덱스가 가장 자주 사용될 것
CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
COMMENT '장치별 시간 범위 조회용 복합 인덱스';

-- 3. 사용자별 장치 조회용 인덱스
-- =====================================================
-- 쿼리 패턴: WHERE user_id = ? AND uuid = ?
CREATE INDEX idx_hnt_sensor_data_user_id_uuid 
ON hnt_sensor_data (user_id, uuid) 
COMMENT '사용자별 장치 조회용 복합 인덱스';

-- 4. 시간 기반 조회용 인덱스 (차트 데이터용)
-- =====================================================
-- 쿼리 패턴: WHERE inst_dtm BETWEEN ? AND ? ORDER BY inst_dtm
CREATE INDEX idx_hnt_sensor_data_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
COMMENT '시간 범위 조회용 인덱스';

-- 5. 월별 단계적 인덱스 생성 (최근 6개월부터)
-- =====================================================
-- 최근 데이터부터 인덱스 생성하여 즉시 성능 향상

-- 2025-09월 데이터용 인덱스 (2,081,294건)
CREATE INDEX idx_hnt_sensor_data_2025_09 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-09-01 00:00:00' 
  AND inst_dtm < '2025-10-01 00:00:00'
COMMENT '2025년 9월 데이터용 부분 인덱스';

-- 2025-08월 데이터용 인덱스 (3,213,504건)
CREATE INDEX idx_hnt_sensor_data_2025_08 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-08-01 00:00:00' 
  AND inst_dtm < '2025-09-01 00:00:00'
COMMENT '2025년 8월 데이터용 부분 인덱스';

-- 2025-07월 데이터용 인덱스 (4,512,326건)
CREATE INDEX idx_hnt_sensor_data_2025_07 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-07-01 00:00:00' 
  AND inst_dtm < '2025-08-01 00:00:00'
COMMENT '2025년 7월 데이터용 부분 인덱스';

-- 2025-06월 데이터용 인덱스 (2,248,645건)
CREATE INDEX idx_hnt_sensor_data_2025_06 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-06-01 00:00:00' 
  AND inst_dtm < '2025-07-01 00:00:00'
COMMENT '2025년 6월 데이터용 부분 인덱스';

-- 2025-05월 데이터용 인덱스 (935,386건)
CREATE INDEX idx_hnt_sensor_data_2025_05 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-05-01 00:00:00' 
  AND inst_dtm < '2025-06-01 00:00:00'
COMMENT '2025년 5월 데이터용 부분 인덱스';

-- 2025-04월 데이터용 인덱스 (데이터 없음 - 건너뜀)
-- 2025-03월 데이터용 인덱스 (406,063건)
CREATE INDEX idx_hnt_sensor_data_2025_03 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-03-01 00:00:00' 
  AND inst_dtm < '2025-04-01 00:00:00'
COMMENT '2025년 3월 데이터용 부분 인덱스';

-- 6. 인덱스 생성 진행 상황 확인
-- =====================================================
-- 인덱스 생성 완료 후 실행하여 확인
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

-- 7. 쿼리 성능 테스트
-- =====================================================
-- 인덱스 생성 후 이 쿼리들의 성능을 테스트해보세요

-- 테스트 쿼리 1: 장치별 최근 1일 데이터 조회
EXPLAIN SELECT * FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4' 
  AND inst_dtm >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY inst_dtm DESC;

-- 테스트 쿼리 2: 사용자별 장치 목록 조회
EXPLAIN SELECT DISTINCT uuid FROM hnt_sensor_data 
WHERE user_id = 'thepine';

-- 테스트 쿼리 3: 차트용 일간 데이터 조회 (30분 단위)
EXPLAIN SELECT 
    DATE_FORMAT(inst_dtm, '%Y-%m-%d %H') as time_group,
    ROUND(AVG(sensor_value), 1) as avg_value
FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4'
  AND inst_dtm >= DATE_SUB(NOW(), INTERVAL 1 DAY)
GROUP BY DATE_FORMAT(inst_dtm, '%Y-%m-%d %H')
ORDER BY time_group;

-- 8. 인덱스 사용률 모니터링
-- =====================================================
-- 인덱스가 실제로 사용되고 있는지 확인
SELECT 
    OBJECT_SCHEMA,
    OBJECT_NAME,
    INDEX_NAME,
    COUNT_FETCH,
    COUNT_INSERT,
    COUNT_UPDATE,
    COUNT_DELETE
FROM performance_schema.table_io_waits_summary_by_index_usage 
WHERE OBJECT_SCHEMA = 'hnt' 
  AND OBJECT_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data%'
ORDER BY COUNT_FETCH DESC;
