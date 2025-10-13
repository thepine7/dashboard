-- 데이터베이스 인덱스 최적화 스크립트
-- 실행 전 반드시 백업을 수행하세요

-- 1. hnt_sensor_data 테이블 인덱스 최적화
-- 기존 인덱스 확인
SHOW INDEX FROM hnt_sensor_data;

-- 복합 인덱스 생성 (user_id, sensor_id, inst_dtm)
CREATE INDEX IF NOT EXISTS idx_sensor_data_user_sensor_time 
ON hnt_sensor_data (user_id, sensor_id, inst_dtm);

-- inst_dtm 단일 인덱스 (시간 범위 쿼리용)
CREATE INDEX IF NOT EXISTS idx_sensor_data_inst_dtm 
ON hnt_sensor_data (inst_dtm);

-- sensor_uuid 인덱스 (센서별 조회용)
CREATE INDEX IF NOT EXISTS idx_sensor_data_sensor_uuid 
ON hnt_sensor_data (sensor_uuid);

-- 2. hnt_sensor_info 테이블 인덱스 최적화
-- user_id 인덱스 (사용자별 센서 조회용)
CREATE INDEX IF NOT EXISTS idx_sensor_info_user_id 
ON hnt_sensor_info (user_id);

-- sensor_id 인덱스 (센서 소유자 조회용)
CREATE INDEX IF NOT EXISTS idx_sensor_info_sensor_id 
ON hnt_sensor_info (sensor_id);

-- uuid 인덱스 (센서 UUID 조회용)
CREATE INDEX IF NOT EXISTS idx_sensor_info_uuid 
ON hnt_sensor_info (uuid);

-- 3. hnt_user 테이블 인덱스 최적화
-- user_id 인덱스 (기본키이지만 명시적으로 생성)
CREATE INDEX IF NOT EXISTS idx_user_user_id 
ON hnt_user (user_id);

-- user_grade 인덱스 (등급별 조회용)
CREATE INDEX IF NOT EXISTS idx_user_user_grade 
ON hnt_user (user_grade);

-- del_yn 인덱스 (삭제되지 않은 사용자 조회용)
CREATE INDEX IF NOT EXISTS idx_user_del_yn 
ON hnt_user (del_yn);

-- 4. hnt_config 테이블 인덱스 최적화
-- 복합 인덱스 (user_id, sensor_uuid, config_type)
CREATE INDEX IF NOT EXISTS idx_config_user_sensor_type 
ON hnt_config (user_id, sensor_uuid, config_type);

-- config_type 인덱스 (설정 타입별 조회용)
CREATE INDEX IF NOT EXISTS idx_config_config_type 
ON hnt_config (config_type);

-- 5. hnt_alarm 테이블 인덱스 최적화
-- user_id 인덱스 (사용자별 알림 조회용)
CREATE INDEX IF NOT EXISTS idx_alarm_user_id 
ON hnt_alarm (user_id);

-- sensor_uuid 인덱스 (센서별 알림 조회용)
CREATE INDEX IF NOT EXISTS idx_alarm_sensor_uuid 
ON hnt_alarm (sensor_uuid);

-- alarm_time 인덱스 (시간별 알림 조회용)
CREATE INDEX IF NOT EXISTS idx_alarm_alarm_time 
ON hnt_alarm (alarm_time);

-- 6. 사용하지 않는 인덱스 제거 (필요시)
-- 중복되거나 사용되지 않는 인덱스 확인 후 제거
-- DROP INDEX IF EXISTS index_name ON table_name;

-- 7. 테이블 통계 업데이트
ANALYZE TABLE hnt_sensor_data;
ANALYZE TABLE hnt_sensor_info;
ANALYZE TABLE hnt_user;
ANALYZE TABLE hnt_config;
ANALYZE TABLE hnt_alarm;

-- 8. 인덱스 사용률 확인 쿼리
-- 실행 계획 확인을 위한 예시 쿼리들
EXPLAIN SELECT * FROM hnt_sensor_data 
WHERE user_id = 'test_user' AND inst_dtm >= '2024-01-01' 
ORDER BY inst_dtm DESC LIMIT 100;

EXPLAIN SELECT * FROM hnt_sensor_info 
WHERE user_id = 'test_user' AND del_yn = 'N';

EXPLAIN SELECT * FROM hnt_config 
WHERE user_id = 'test_user' AND sensor_uuid = 'test_uuid' 
AND config_type = 'alarm';
