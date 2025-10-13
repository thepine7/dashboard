-- 1개월씩 나누어서 인덱스 추가 (단계별 실행)
-- 실행 전 반드시 백업을 수행하세요

-- ========================================
-- 1. 전체 데이터 현황 확인
-- ========================================
SELECT 
    COUNT(*) as total_records,
    MIN(inst_dtm) as oldest_record,
    MAX(inst_dtm) as newest_record,
    COUNT(DISTINCT DATE_FORMAT(inst_dtm, '%Y-%m')) as total_months
FROM hnt_sensor_data;

-- ========================================
-- 2. 월별 데이터 개수 확인
-- ========================================
SELECT 
    DATE_FORMAT(inst_dtm, '%Y-%m') as month,
    COUNT(*) as record_count
FROM hnt_sensor_data 
GROUP BY DATE_FORMAT(inst_dtm, '%Y-%m')
ORDER BY month DESC;

-- ========================================
-- 3. 단계별 인덱스 추가 (1개월씩)
-- ========================================

-- 2025년 9월 데이터에 인덱스 추가
CREATE INDEX idx_sensor_data_202509_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-09-01 00:00:00' AND inst_dtm < '2025-10-01 00:00:00';

CREATE INDEX idx_sensor_data_202509_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
WHERE inst_dtm >= '2025-09-01 00:00:00' AND inst_dtm < '2025-10-01 00:00:00';

CREATE INDEX idx_sensor_data_202509_user_uuid 
ON hnt_sensor_data (user_id, uuid) 
WHERE inst_dtm >= '2025-09-01 00:00:00' AND inst_dtm < '2025-10-01 00:00:00';

-- ========================================
-- 4. 2025년 8월 데이터에 인덱스 추가
-- ========================================
CREATE INDEX idx_sensor_data_202508_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-08-01 00:00:00' AND inst_dtm < '2025-09-01 00:00:00';

CREATE INDEX idx_sensor_data_202508_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
WHERE inst_dtm >= '2025-08-01 00:00:00' AND inst_dtm < '2025-09-01 00:00:00';

CREATE INDEX idx_sensor_data_202508_user_uuid 
ON hnt_sensor_data (user_id, uuid) 
WHERE inst_dtm >= '2025-08-01 00:00:00' AND inst_dtm < '2025-09-01 00:00:00';

-- ========================================
-- 5. 2025년 7월 데이터에 인덱스 추가
-- ========================================
CREATE INDEX idx_sensor_data_202507_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-07-01 00:00:00' AND inst_dtm < '2025-08-01 00:00:00';

CREATE INDEX idx_sensor_data_202507_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
WHERE inst_dtm >= '2025-07-01 00:00:00' AND inst_dtm < '2025-08-01 00:00:00';

CREATE INDEX idx_sensor_data_202507_user_uuid 
ON hnt_sensor_data (user_id, uuid) 
WHERE inst_dtm >= '2025-07-01 00:00:00' AND inst_dtm < '2025-08-01 00:00:00';

-- ========================================
-- 6. 2025년 6월 데이터에 인덱스 추가
-- ========================================
CREATE INDEX idx_sensor_data_202506_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-06-01 00:00:00' AND inst_dtm < '2025-07-01 00:00:00';

CREATE INDEX idx_sensor_data_202506_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
WHERE inst_dtm >= '2025-06-01 00:00:00' AND inst_dtm < '2025-07-01 00:00:00';

CREATE INDEX idx_sensor_data_202506_user_uuid 
ON hnt_sensor_data (user_id, uuid) 
WHERE inst_dtm >= '2025-06-01 00:00:00' AND inst_dtm < '2025-07-01 00:00:00';

-- ========================================
-- 7. 2025년 5월 데이터에 인덱스 추가
-- ========================================
CREATE INDEX idx_sensor_data_202505_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-05-01 00:00:00' AND inst_dtm < '2025-06-01 00:00:00';

CREATE INDEX idx_sensor_data_202505_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
WHERE inst_dtm >= '2025-05-01 00:00:00' AND inst_dtm < '2025-06-01 00:00:00';

CREATE INDEX idx_sensor_data_202505_user_uuid 
ON hnt_sensor_data (user_id, uuid) 
WHERE inst_dtm >= '2025-05-01 00:00:00' AND inst_dtm < '2025-06-01 00:00:00';

-- ========================================
-- 8. 2025년 4월 데이터에 인덱스 추가
-- ========================================
CREATE INDEX idx_sensor_data_202504_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-04-01 00:00:00' AND inst_dtm < '2025-05-01 00:00:00';

CREATE INDEX idx_sensor_data_202504_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
WHERE inst_dtm >= '2025-04-01 00:00:00' AND inst_dtm < '2025-05-01 00:00:00';

CREATE INDEX idx_sensor_data_202504_user_uuid 
ON hnt_sensor_data (user_id, uuid) 
WHERE inst_dtm >= '2025-04-01 00:00:00' AND inst_dtm < '2025-05-01 00:00:00';

-- ========================================
-- 9. 인덱스 생성 상태 확인
-- ========================================
SHOW INDEX FROM hnt_sensor_data;

-- ========================================
-- 10. 성능 테스트
-- ========================================
EXPLAIN SELECT 
    date_format(inst_dtm, '%Y-%m-%d') as getDate,
    date_format(inst_dtm, '%Y-%m-%d %H') as inst_dtm,
    round(avg(sensor_value), 1) as sensor_value
FROM hnt_sensor_data
WHERE uuid = '0008DC7553A4'
  AND inst_dtm BETWEEN '2025-09-20 00:00:00' AND '2025-09-20 23:59:59'
GROUP BY date_format(inst_dtm, '%Y-%m-%d'), date_format(inst_dtm, '%Y-%m-%d %H')
ORDER BY getDate asc, inst_dtm asc
LIMIT 50;
