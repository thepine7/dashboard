-- 6개월치 데이터에만 인덱스 추가 (조건부 인덱스)
-- 실행 전 반드시 백업을 수행하세요

-- ========================================
-- 1. 6개월치 데이터 개수 확인
-- ========================================
SELECT 
    COUNT(*) as total_records,
    COUNT(CASE WHEN inst_dtm >= DATE_SUB(NOW(), INTERVAL 6 MONTH) THEN 1 END) as six_month_records,
    MIN(inst_dtm) as oldest_record,
    MAX(inst_dtm) as newest_record
FROM hnt_sensor_data;

-- ========================================
-- 2. 6개월치 데이터에만 인덱스 추가 (조건부 인덱스)
-- ========================================

-- 방법 1: 조건부 인덱스 (MySQL 8.0+)
-- 6개월치 데이터에만 인덱스 적용
CREATE INDEX idx_sensor_data_uuid_inst_dtm_6m 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= DATE_SUB(NOW(), INTERVAL 6 MONTH);

CREATE INDEX idx_sensor_data_inst_dtm_6m 
ON hnt_sensor_data (inst_dtm) 
WHERE inst_dtm >= DATE_SUB(NOW(), INTERVAL 6 MONTH);

CREATE INDEX idx_sensor_data_user_uuid_6m 
ON hnt_sensor_data (user_id, uuid) 
WHERE inst_dtm >= DATE_SUB(NOW(), INTERVAL 6 MONTH);

-- ========================================
-- 3. 대안: 파티션 테이블 (MySQL 5.7+)
-- ========================================

-- 기존 테이블을 파티션 테이블로 변경
ALTER TABLE hnt_sensor_data 
PARTITION BY RANGE (YEAR(inst_dtm) * 100 + MONTH(inst_dtm)) (
    PARTITION p202406 VALUES LESS THAN (202407),
    PARTITION p202407 VALUES LESS THAN (202408),
    PARTITION p202408 VALUES LESS THAN (202409),
    PARTITION p202409 VALUES LESS THAN (202410),
    PARTITION p202410 VALUES LESS THAN (202411),
    PARTITION p202411 VALUES LESS THAN (202412),
    PARTITION p202412 VALUES LESS THAN (202501),
    PARTITION p202501 VALUES LESS THAN (202502),
    PARTITION p202502 VALUES LESS THAN (202503),
    PARTITION p202503 VALUES LESS THAN (202504),
    PARTITION p202504 VALUES LESS THAN (202505),
    PARTITION p202505 VALUES LESS THAN (202506),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 파티션 테이블에 인덱스 추가 (6개월치 파티션에만 적용)
ALTER TABLE hnt_sensor_data 
ADD INDEX idx_sensor_data_uuid_inst_dtm (uuid, inst_dtm),
ADD INDEX idx_sensor_data_inst_dtm (inst_dtm),
ADD INDEX idx_sensor_data_user_uuid (user_id, uuid);

-- ========================================
-- 4. 대안: 임시 테이블 방식 (가장 안전)
-- ========================================

-- 6개월치 데이터만 복사하여 임시 테이블 생성
CREATE TABLE hnt_sensor_data_6months AS
SELECT * FROM hnt_sensor_data 
WHERE inst_dtm >= DATE_SUB(NOW(), INTERVAL 6 MONTH);

-- 임시 테이블에 인덱스 추가
ALTER TABLE hnt_sensor_data_6months 
ADD INDEX idx_sensor_data_uuid_inst_dtm (uuid, inst_dtm),
ADD INDEX idx_sensor_data_inst_dtm (inst_dtm),
ADD INDEX idx_sensor_data_user_uuid (user_id, uuid);

-- ========================================
-- 5. 성능 확인
-- ========================================

-- 인덱스 사용 여부 확인
EXPLAIN SELECT 
    date_format(inst_dtm, '%Y-%m-%d') as getDate,
    date_format(inst_dtm, '%Y-%m-%d %H') as inst_dtm,
    round(avg(sensor_value), 1) as sensor_value
FROM hnt_sensor_data_6months
WHERE uuid = '0008DC7553A4'
  AND inst_dtm BETWEEN '2025-09-20 00:00:00' AND '2025-09-20 23:59:59'
GROUP BY date_format(inst_dtm, '%Y-%m-%d'), date_format(inst_dtm, '%Y-%m-%d %H')
ORDER BY getDate asc, inst_dtm asc
LIMIT 50;

-- ========================================
-- 6. 정리 (필요시)
-- ========================================

-- 임시 테이블 삭제 (필요시)
-- DROP TABLE IF EXISTS hnt_sensor_data_6months;

-- 조건부 인덱스 삭제 (필요시)
-- DROP INDEX IF EXISTS idx_sensor_data_uuid_inst_dtm_6m ON hnt_sensor_data;
-- DROP INDEX IF EXISTS idx_sensor_data_inst_dtm_6m ON hnt_sensor_data;
-- DROP INDEX IF EXISTS idx_sensor_data_user_uuid_6m ON hnt_sensor_data;
