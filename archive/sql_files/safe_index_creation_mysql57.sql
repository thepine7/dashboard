-- MySQL 5.7 호환 안전한 인덱스 생성 스크립트
-- 중복 생성 방지를 위한 조건부 인덱스 생성

-- 1. hnt_sensor_info 테이블 인덱스 생성
-- 부계정 여부 확인 쿼리 최적화 (user_id, sensor_id, del_yn)
SET @sql = 'CREATE INDEX idx_sensor_info_user_sensor_del ON hnt_sensor_info (user_id, sensor_id, del_yn)';
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_sensor_info' 
      AND INDEX_NAME = 'idx_sensor_info_user_sensor_del'
);
SET @sql = IF(@index_exists = 0, @sql, 'SELECT "인덱스 idx_sensor_info_user_sensor_del 이미 존재" as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 부계정의 메인 사용자 ID 조회 최적화 (user_id, del_yn)
SET @sql = 'CREATE INDEX idx_sensor_info_user_del ON hnt_sensor_info (user_id, del_yn)';
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_sensor_info' 
      AND INDEX_NAME = 'idx_sensor_info_user_del'
);
SET @sql = IF(@index_exists = 0, @sql, 'SELECT "인덱스 idx_sensor_info_user_del 이미 존재" as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 센서 목록 조회 최적화 (sensor_id, user_id, del_yn)
SET @sql = 'CREATE INDEX idx_sensor_info_sensor_user_del ON hnt_sensor_info (sensor_id, user_id, del_yn)';
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_sensor_info' 
      AND INDEX_NAME = 'idx_sensor_info_sensor_user_del'
);
SET @sql = IF(@index_exists = 0, @sql, 'SELECT "인덱스 idx_sensor_info_sensor_user_del 이미 존재" as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. hnt_user 테이블 인덱스 생성
-- 부계정 관계 확인 최적화 (parent_user_id, del_yn)
SET @sql = 'CREATE INDEX idx_user_parent_del ON hnt_user (parent_user_id, del_yn)';
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_user' 
      AND INDEX_NAME = 'idx_user_parent_del'
);
SET @sql = IF(@index_exists = 0, @sql, 'SELECT "인덱스 idx_user_parent_del 이미 존재" as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 사용자 등급별 조회 최적화 (user_grade, del_yn)
SET @sql = 'CREATE INDEX idx_user_grade_del ON hnt_user (user_grade, del_yn)';
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_user' 
      AND INDEX_NAME = 'idx_user_grade_del'
);
SET @sql = IF(@index_exists = 0, @sql, 'SELECT "인덱스 idx_user_grade_del 이미 존재" as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. hnt_sensor_data 테이블 인덱스 생성
-- 센서 데이터 조회 최적화 (user_id, sensor_id, inst_dtm)
SET @sql = 'CREATE INDEX idx_sensor_data_user_sensor_time ON hnt_sensor_data (user_id, sensor_id, inst_dtm)';
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_sensor_data' 
      AND INDEX_NAME = 'idx_sensor_data_user_sensor_time'
);
SET @sql = IF(@index_exists = 0, @sql, 'SELECT "인덱스 idx_sensor_data_user_sensor_time 이미 존재" as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. hnt_config 테이블 인덱스 생성
-- 설정 정보 조회 최적화 (user_id, sensor_uuid, del_yn)
SET @sql = 'CREATE INDEX idx_config_user_uuid_del ON hnt_config (user_id, sensor_uuid, del_yn)';
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_config' 
      AND INDEX_NAME = 'idx_config_user_uuid_del'
);
SET @sql = IF(@index_exists = 0, @sql, 'SELECT "인덱스 idx_config_user_uuid_del 이미 존재" as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. hnt_alarm 테이블 인덱스 생성
-- 알림 데이터 조회 최적화 (user_id, sensor_uuid, del_yn)
SET @sql = 'CREATE INDEX idx_alarm_user_uuid_del ON hnt_alarm (user_id, sensor_uuid, del_yn)';
SET @index_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_alarm' 
      AND INDEX_NAME = 'idx_alarm_user_uuid_del'
);
SET @sql = IF(@index_exists = 0, @sql, 'SELECT "인덱스 idx_alarm_user_uuid_del 이미 존재" as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 인덱스 생성 완료 후 통계 업데이트
ANALYZE TABLE hnt_sensor_info;
ANALYZE TABLE hnt_user;
ANALYZE TABLE hnt_sensor_data;
ANALYZE TABLE hnt_config;
ANALYZE TABLE hnt_alarm;

-- 인덱스 생성 결과 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND INDEX_NAME LIKE 'idx_%'
ORDER BY TABLE_NAME, INDEX_NAME;