-- 부계정 관련 쿼리 성능 최적화를 위한 인덱스 생성
-- 실행 전 기존 인덱스 확인 후 중복 생성 방지

-- 1. hnt_sensor_info 테이블 최적화 인덱스
-- 부계정 여부 확인 쿼리 최적화 (user_id, sensor_id, del_yn)
CREATE INDEX idx_sensor_info_user_sensor_del 
ON hnt_sensor_info (user_id, sensor_id, del_yn);

-- 부계정의 메인 사용자 ID 조회 최적화 (user_id, del_yn)
CREATE INDEX idx_sensor_info_user_del 
ON hnt_sensor_info (user_id, del_yn);

-- 센서 목록 조회 최적화 (sensor_id, user_id, del_yn)
CREATE INDEX idx_sensor_info_sensor_user_del 
ON hnt_sensor_info (sensor_id, user_id, del_yn);

-- 2. hnt_user 테이블 최적화 인덱스
-- 부계정 관계 확인 최적화 (parent_user_id, del_yn)
CREATE INDEX idx_user_parent_del 
ON hnt_user (parent_user_id, del_yn);

-- 사용자 등급별 조회 최적화 (user_grade, del_yn)
CREATE INDEX idx_user_grade_del 
ON hnt_user (user_grade, del_yn);

-- 3. hnt_sensor_data 테이블 최적화 인덱스 (이미 생성된 인덱스 확인)
-- 센서 데이터 조회 최적화 (user_id, sensor_id, inst_dtm)
CREATE INDEX idx_sensor_data_user_sensor_time 
ON hnt_sensor_data (user_id, sensor_id, inst_dtm);

-- 4. hnt_config 테이블 최적화 인덱스
-- 설정 정보 조회 최적화 (user_id, sensor_uuid, del_yn)
CREATE INDEX idx_config_user_uuid_del 
ON hnt_config (user_id, sensor_uuid, del_yn);

-- 5. hnt_alarm 테이블 최적화 인덱스
-- 알림 데이터 조회 최적화 (user_id, sensor_uuid, del_yn)
CREATE INDEX idx_alarm_user_uuid_del 
ON hnt_alarm (user_id, sensor_uuid, del_yn);

-- 인덱스 생성 완료 후 통계 업데이트
ANALYZE TABLE hnt_sensor_info;
ANALYZE TABLE hnt_user;
ANALYZE TABLE hnt_sensor_data;
ANALYZE TABLE hnt_config;
ANALYZE TABLE hnt_alarm;

-- 인덱스 사용 통계 확인 쿼리
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    PACKED,
    NULLABLE,
    INDEX_TYPE
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME IN ('hnt_sensor_info', 'hnt_user', 'hnt_sensor_data', 'hnt_config', 'hnt_alarm')
ORDER BY TABLE_NAME, INDEX_NAME;
