-- =====================================================
-- 연결 타임아웃 해결 및 인덱스 생성 SQL
-- =====================================================

-- =====================================================
-- 1. 연결 타임아웃 설정 (최대값으로 설정)
-- =====================================================
SET SESSION wait_timeout = 28800;
SET SESSION interactive_timeout = 28800;
SET SESSION net_read_timeout = 1800;
SET SESSION net_write_timeout = 1800;
SET SESSION lock_wait_timeout = 300;

-- 타임아웃 설정 확인
SELECT 
    @@wait_timeout as wait_timeout,
    @@interactive_timeout as interactive_timeout,
    @@net_read_timeout as net_read_timeout,
    @@net_write_timeout as net_write_timeout,
    @@lock_wait_timeout as lock_wait_timeout;

-- =====================================================
-- 2. 현재 인덱스 상태 확인
-- =====================================================
SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    NON_UNIQUE,
    INDEX_COMMENT
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data_%'
GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_COMMENT
ORDER BY INDEX_NAME;

-- =====================================================
-- 3. 인덱스 1: 장치별 시간 범위 조회용 (uuid, inst_dtm)
-- =====================================================
SELECT '=== 인덱스 1 확인 시작 ===' as status;

-- 기존 인덱스 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스 1이 이미 존재합니다'
        ELSE '인덱스 1을 생성합니다'
    END as index1_status
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

-- 디버깅: 인덱스 존재 여부 확인
SELECT @index1_exists as index1_exists_count;

SET @sql1 = IF(@index1_exists = 0, 
    'CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm ON hnt_sensor_data (uuid, inst_dtm) COMMENT ''장치별 시간 범위 조회용 복합 인덱스''',
    'SELECT ''인덱스 1이 이미 존재합니다'' as message'
);

-- 디버깅: 생성될 SQL 확인
SELECT @sql1 as sql1_to_execute;

PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SELECT '=== 인덱스 1 완료 ===' as status;

-- =====================================================
-- 4. 인덱스 2: 사용자별 장치 조회용 (user_id, uuid)
-- =====================================================
SELECT '=== 인덱스 2 확인 시작 ===' as status;

-- 기존 인덱스 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스 2가 이미 존재합니다'
        ELSE '인덱스 2를 생성합니다'
    END as index2_status
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

-- 디버깅: 인덱스 존재 여부 확인
SELECT @index2_exists as index2_exists_count;

SET @sql2 = IF(@index2_exists = 0, 
    'CREATE INDEX idx_hnt_sensor_data_user_id_uuid ON hnt_sensor_data (user_id, uuid) COMMENT ''사용자별 장치 조회용 복합 인덱스''',
    'SELECT ''인덱스 2가 이미 존재합니다'' as message'
);

-- 디버깅: 생성될 SQL 확인
SELECT @sql2 as sql2_to_execute;

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SELECT '=== 인덱스 2 완료 ===' as status;

-- =====================================================
-- 5. 인덱스 3: 시간 기반 조회용 (inst_dtm) - 개별 실행
-- =====================================================
SELECT '=== 인덱스 3 확인 시작 ===' as status;

-- 기존 인덱스 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스 3이 이미 존재합니다'
        ELSE '인덱스 3을 생성합니다'
    END as index3_status
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

-- 디버깅: 인덱스 존재 여부 확인
SELECT @index3_exists as index3_exists_count;

SET @sql3 = IF(@index3_exists = 0, 
    'CREATE INDEX idx_hnt_sensor_data_inst_dtm ON hnt_sensor_data (inst_dtm) COMMENT ''시간 범위 조회용 인덱스''',
    'SELECT ''인덱스 3이 이미 존재합니다'' as message'
);

-- 디버깅: 생성될 SQL 확인
SELECT @sql3 as sql3_to_execute;

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

SELECT '=== 인덱스 3 완료 ===' as status;

-- =====================================================
-- 6. 최종 인덱스 상태 확인
-- =====================================================
SELECT '=== 최종 인덱스 상태 확인 ===' as status;

SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    NON_UNIQUE,
    INDEX_COMMENT,
    '생성 완료' as status
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data_%'
GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_COMMENT
ORDER BY INDEX_NAME;

-- =====================================================
-- 7. 테이블 통계 정보
-- =====================================================
SELECT '=== 테이블 통계 정보 ===' as status;

SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    DATA_LENGTH,
    INDEX_LENGTH,
    (DATA_LENGTH + INDEX_LENGTH) as TOTAL_SIZE
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data';

-- =====================================================
-- 실행 가이드
-- =====================================================
-- 
-- 1. 전체 스크립트를 한 번에 실행하세요
-- 2. 각 인덱스는 자동으로 존재 여부를 확인하고 생성합니다
-- 3. 연결 타임아웃이 발생하면 MySQL Workbench 설정을 변경하세요:
--    - Edit → Preferences → SQL Editor
--    - DBMS connection read time out: 1800 (30분)
--    - DBMS connection write time out: 1800 (30분)
-- 
-- 예상 소요 시간:
-- - 인덱스 1: 5-10분
-- - 인덱스 2: 3-5분  
-- - 인덱스 3: 10-15분
-- 
-- =====================================================
