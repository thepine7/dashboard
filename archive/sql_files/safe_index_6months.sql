-- 6개월치 데이터에만 인덱스 추가 (가장 안전한 방법)
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
-- 2. 6개월치 데이터만 복사하여 임시 테이블 생성
-- ========================================

-- 6개월치 데이터만 복사
CREATE TABLE hnt_sensor_data_6months AS
SELECT * FROM hnt_sensor_data 
WHERE inst_dtm >= DATE_SUB(NOW(), INTERVAL 6 MONTH);

-- 임시 테이블에 인덱스 추가 (빠름)
ALTER TABLE hnt_sensor_data_6months 
ADD INDEX idx_sensor_data_uuid_inst_dtm (uuid, inst_dtm),
ADD INDEX idx_sensor_data_inst_dtm (inst_dtm),
ADD INDEX idx_sensor_data_user_uuid (user_id, uuid);

-- ========================================
-- 3. 성능 확인
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
-- 4. 정리 (필요시)
-- ========================================

-- 임시 테이블 삭제 (필요시)
-- DROP TABLE IF EXISTS hnt_sensor_data_6months;
