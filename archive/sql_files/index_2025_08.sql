-- 2025년 8월 데이터에 인덱스 추가
-- 실행 전 반드시 백업을 수행하세요

-- 8월 데이터 개수 확인
SELECT 
    COUNT(*) as aug_2025_records,
    MIN(inst_dtm) as min_date,
    MAX(inst_dtm) as max_date
FROM hnt_sensor_data 
WHERE inst_dtm >= '2025-08-01 00:00:00' AND inst_dtm < '2025-09-01 00:00:00';

-- 8월 데이터에 인덱스 추가
CREATE INDEX idx_sensor_data_202508_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-08-01 00:00:00' AND inst_dtm < '2025-09-01 00:00:00';

CREATE INDEX idx_sensor_data_202508_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
WHERE inst_dtm >= '2025-08-01 00:00:00' AND inst_dtm < '2025-09-01 00:00:00';

CREATE INDEX idx_sensor_data_202508_user_uuid 
ON hnt_sensor_data (user_id, uuid) 
WHERE inst_dtm >= '2025-08-01 00:00:00' AND inst_dtm < '2025-09-01 00:00:00';

-- 완료 확인
SELECT '2025년 8월 인덱스 생성 완료' as status;
