-- 챠트데이터 페이지 성능 최적화를 위한 인덱스 추가
-- MySQL 5.7.9 버전용

-- 1. hnt_sensor_data 테이블 기본 인덱스 (이미 존재할 수 있음)
-- CREATE INDEX idx_sensor_data_uuid ON hnt_sensor_data(uuid);
-- CREATE INDEX idx_sensor_data_inst_dtm ON hnt_sensor_data(inst_dtm);

-- 2. 복합 인덱스 추가 (UUID + 날짜 범위 검색 최적화)
CREATE INDEX idx_sensor_data_uuid_inst_dtm ON hnt_sensor_data(uuid, inst_dtm);

-- 3. 사용자별 데이터 검색 최적화
CREATE INDEX idx_sensor_data_user_id_inst_dtm ON hnt_sensor_data(user_id, inst_dtm);

-- 4. 센서 타입별 검색 최적화 (필요시)
CREATE INDEX idx_sensor_data_uuid_type_inst_dtm ON hnt_sensor_data(uuid, sensor_type, inst_dtm);

-- 5. 기존 인덱스 확인 쿼리
-- SHOW INDEX FROM hnt_sensor_data;

-- 6. 쿼리 실행 계획 확인 (성능 테스트용)
-- EXPLAIN SELECT date_format(inst_dtm, '%Y-%m-%d') as getDate,
--        concat(date_format(inst_dtm, '%Y-%m-%d %H'), ':',
--               lpad(floor(date_format(inst_dtm, '%i')/30)*30, 2, '0')) as inst_dtm,
--        round(avg(sensor_value), 1) as sensor_value
-- FROM hnt_sensor_data
-- WHERE uuid = '0008DC7553A4' 
--   AND inst_dtm BETWEEN '2025-09-19 00:00:00' AND '2025-09-19 23:59:59'
-- GROUP BY date_format(inst_dtm, '%Y-%m-%d %H'), floor(date_format(inst_dtm, '%i')/30)
-- ORDER BY getDate ASC, inst_dtm ASC;







