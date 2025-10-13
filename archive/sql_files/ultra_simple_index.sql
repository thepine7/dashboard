-- =====================================================
-- 초간단 인덱스 생성 쿼리
-- 기존 인덱스가 있으면 넘어가고, 없으면 생성
-- =====================================================

-- 연결 타임아웃 설정
SET SESSION wait_timeout = 28800;
SET SESSION interactive_timeout = 28800;

-- =====================================================
-- 인덱스 1: 장치별 시간 범위 조회용
-- =====================================================
SELECT '인덱스 1 확인 중...' as status;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = 'hnt' AND TABLE_NAME = 'hnt_sensor_data' 
     AND INDEX_NAME = 'idx_hnt_sensor_data_uuid_inst_dtm') = 0,
    'CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm ON hnt_sensor_data (uuid, inst_dtm)',
    'SELECT ''인덱스 1이 이미 존재합니다'' as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 인덱스 2: 사용자별 장치 조회용
-- =====================================================
SELECT '인덱스 2 확인 중...' as status;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = 'hnt' AND TABLE_NAME = 'hnt_sensor_data' 
     AND INDEX_NAME = 'idx_hnt_sensor_data_user_id_uuid') = 0,
    'CREATE INDEX idx_hnt_sensor_data_user_id_uuid ON hnt_sensor_data (user_id, uuid)',
    'SELECT ''인덱스 2가 이미 존재합니다'' as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 인덱스 3: 시간 기반 조회용
-- =====================================================
SELECT '인덱스 3 확인 중...' as status;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
     WHERE TABLE_SCHEMA = 'hnt' AND TABLE_NAME = 'hnt_sensor_data' 
     AND INDEX_NAME = 'idx_hnt_sensor_data_inst_dtm') = 0,
    'CREATE INDEX idx_hnt_sensor_data_inst_dtm ON hnt_sensor_data (inst_dtm)',
    'SELECT ''인덱스 3이 이미 존재합니다'' as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================
-- 완료 확인
-- =====================================================
SELECT '모든 인덱스 생성 완료!' as status;

SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data_%'
GROUP BY INDEX_NAME
ORDER BY INDEX_NAME;
