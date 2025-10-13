-- =====================================================
-- 간단한 인덱스 생성 쿼리 (MySQL 5.7 호환)
-- 기존 인덱스가 있으면 넘어가고, 없으면 생성
-- =====================================================

-- 연결 타임아웃 설정 (실행 전 권장)
SET SESSION wait_timeout = 28800;
SET SESSION interactive_timeout = 28800;
SET SESSION net_read_timeout = 600;
SET SESSION net_write_timeout = 600;

-- =====================================================
-- 1. 현재 인덱스 상태 확인
-- =====================================================
SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    NON_UNIQUE,
    INDEX_COMMENT
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN (
    'idx_hnt_sensor_data_uuid_inst_dtm',
    'idx_hnt_sensor_data_user_id_uuid', 
    'idx_hnt_sensor_data_inst_dtm'
  )
GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_COMMENT
ORDER BY INDEX_NAME;

-- =====================================================
-- 2. 인덱스 1: 장치별 시간 범위 조회용 (uuid, inst_dtm)
-- =====================================================
-- 기존 인덱스 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스가 이미 존재합니다'
        ELSE '인덱스를 생성합니다'
    END as status
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME = 'idx_hnt_sensor_data_uuid_inst_dtm';

-- 인덱스 생성 (없을 때만)
SET @index1_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_sensor_data'
      AND INDEX_NAME = 'idx_hnt_sensor_data_uuid_inst_dtm'
);

SET @sql1 = IF(@index1_exists = 0, 
    'CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm ON hnt_sensor_data (uuid, inst_dtm) COMMENT ''장치별 시간 범위 조회용 복합 인덱스''',
    'SELECT ''인덱스 idx_hnt_sensor_data_uuid_inst_dtm가 이미 존재합니다'' as message'
);

PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- =====================================================
-- 3. 인덱스 2: 사용자별 장치 조회용 (user_id, uuid)
-- =====================================================
-- 기존 인덱스 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스가 이미 존재합니다'
        ELSE '인덱스를 생성합니다'
    END as status
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME = 'idx_hnt_sensor_data_user_id_uuid';

-- 인덱스 생성 (없을 때만)
SET @index2_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_sensor_data'
      AND INDEX_NAME = 'idx_hnt_sensor_data_user_id_uuid'
);

SET @sql2 = IF(@index2_exists = 0, 
    'CREATE INDEX idx_hnt_sensor_data_user_id_uuid ON hnt_sensor_data (user_id, uuid) COMMENT ''사용자별 장치 조회용 복합 인덱스''',
    'SELECT ''인덱스 idx_hnt_sensor_data_user_id_uuid가 이미 존재합니다'' as message'
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- =====================================================
-- 4. 인덱스 3: 시간 기반 조회용 (inst_dtm)
-- =====================================================
-- 기존 인덱스 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스가 이미 존재합니다'
        ELSE '인덱스를 생성합니다'
    END as status
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME = 'idx_hnt_sensor_data_inst_dtm';

-- 인덱스 생성 (없을 때만)
SET @index3_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_sensor_data'
      AND INDEX_NAME = 'idx_hnt_sensor_data_inst_dtm'
);

SET @sql3 = IF(@index3_exists = 0, 
    'CREATE INDEX idx_hnt_sensor_data_inst_dtm ON hnt_sensor_data (inst_dtm) COMMENT ''시간 범위 조회용 인덱스''',
    'SELECT ''인덱스 idx_hnt_sensor_data_inst_dtm가 이미 존재합니다'' as message'
);

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- =====================================================
-- 5. 최종 인덱스 상태 확인
-- =====================================================
SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    NON_UNIQUE,
    INDEX_COMMENT,
    '생성 완료' as status
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN (
    'idx_hnt_sensor_data_uuid_inst_dtm',
    'idx_hnt_sensor_data_user_id_uuid', 
    'idx_hnt_sensor_data_inst_dtm'
  )
GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_COMMENT
ORDER BY INDEX_NAME;

-- =====================================================
-- 실행 가이드
-- =====================================================
-- 
-- 1. 전체 스크립트를 한 번에 실행하세요
-- 2. 각 인덱스는 자동으로 존재 여부를 확인하고 생성합니다
-- 3. 기존 인덱스가 있으면 "이미 존재합니다" 메시지가 표시됩니다
-- 4. 인덱스가 없으면 자동으로 생성됩니다
-- 5. 연결 타임아웃이 발생하면 타임아웃 설정을 먼저 실행하세요
-- 
-- 예상 소요 시간:
-- - 인덱스 1: 5-10분
-- - 인덱스 2: 3-5분  
-- - 인덱스 3: 10-15분
-- 
-- =====================================================
