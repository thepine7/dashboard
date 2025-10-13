-- 6개월치 데이터에만 인덱스 추가 (성능 최적화)
-- 실행 전 반드시 백업을 수행하세요

-- ========================================
-- 1. 기존 인덱스 삭제 (있다면)
-- ========================================
DROP INDEX IF EXISTS idx_sensor_data_uuid_inst_dtm ON hnt_sensor_data;
DROP INDEX IF EXISTS idx_sensor_data_inst_dtm ON hnt_sensor_data;
DROP INDEX IF EXISTS idx_sensor_data_user_uuid ON hnt_sensor_data;

-- ========================================
-- 2. 6개월치 데이터 확인
-- ========================================
-- 6개월 전 날짜 확인
SELECT 
    DATE_SUB(NOW(), INTERVAL 6 MONTH) as six_months_ago,
    COUNT(*) as total_records,
    MIN(inst_dtm) as oldest_record,
    MAX(inst_dtm) as newest_record
FROM hnt_sensor_data;

-- 6개월치 데이터 개수 확인
SELECT 
    COUNT(*) as six_month_records
FROM hnt_sensor_data 
WHERE inst_dtm >= DATE_SUB(NOW(), INTERVAL 6 MONTH);

-- ========================================
-- 3. 6개월치 데이터에만 인덱스 추가
-- ========================================

-- 방법 1: 파티션 테이블로 변경 (권장)
-- 기존 테이블을 파티션 테이블로 변경하여 6개월치 데이터만 인덱스 적용
ALTER TABLE hnt_sensor_data 
PARTITION BY RANGE (YEAR(inst_dtm) * 100 + MONTH(inst_dtm)) (
    PARTITION p202401 VALUES LESS THAN (202402),
    PARTITION p202402 VALUES LESS THAN (202403),
    PARTITION p202403 VALUES LESS THAN (202404),
    PARTITION p202404 VALUES LESS THAN (202405),
    PARTITION p202405 VALUES LESS THAN (202406),
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
    PARTITION p202506 VALUES LESS THAN (202507),
    PARTITION p202507 VALUES LESS THAN (202508),
    PARTITION p202508 VALUES LESS THAN (202509),
    PARTITION p202509 VALUES LESS THAN (202510),
    PARTITION p202510 VALUES LESS THAN (202511),
    PARTITION p202511 VALUES LESS THAN (202512),
    PARTITION p202512 VALUES LESS THAN (202601),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 파티션 테이블에 인덱스 추가 (6개월치 파티션에만 적용)
ALTER TABLE hnt_sensor_data 
ADD INDEX idx_sensor_data_uuid_inst_dtm (uuid, inst_dtm),
ADD INDEX idx_sensor_data_inst_dtm (inst_dtm),
ADD INDEX idx_sensor_data_user_uuid (user_id, uuid);

-- ========================================
-- 4. 대안: 임시 테이블 방식 (파티션 실패 시)
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
-- 5. 쿼리 수정 (6개월치 데이터만 조회)
-- ========================================

-- 기존 쿼리를 6개월치 데이터만 조회하도록 수정
-- DataMapper.xml에서 사용할 쿼리
/*
SELECT
    date_format(inst_dtm, '%Y-%m-%d') as getDate,
    date_format(inst_dtm, '%Y-%m-%d %H') as inst_dtm,
    round(avg(sensor_value), 1) as sensor_value
FROM hnt_sensor_data_6months  -- 임시 테이블 사용
WHERE uuid = #{sensorUuid}
  AND inst_dtm BETWEEN #{startDateTime} AND #{endDateTime}
GROUP BY date_format(inst_dtm, '%Y-%m-%d'), date_format(inst_dtm, '%Y-%m-%d %H')
ORDER BY getDate asc, inst_dtm asc
LIMIT 50;
*/

-- ========================================
-- 6. 성능 확인
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
-- 7. 정리 (필요시)
-- ========================================

-- 임시 테이블 삭제 (필요시)
-- DROP TABLE IF EXISTS hnt_sensor_data_6months;

-- 파티션 테이블 원복 (필요시)
-- ALTER TABLE hnt_sensor_data REMOVE PARTITIONING;
