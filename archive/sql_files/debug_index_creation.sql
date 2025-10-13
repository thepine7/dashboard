-- =====================================================
-- 디버깅용 안전한 인덱스 생성 쿼리
-- 중복 인덱스 생성 에러 방지 및 상세 디버깅
-- =====================================================

-- 연결 타임아웃 설정 (필요시 주석 해제 후 실행)
SET SESSION wait_timeout = 28800;        -- 8시간
SET SESSION interactive_timeout = 28800; -- 8시간
SET SESSION net_read_timeout = 1800;     -- 30분
SET SESSION net_write_timeout = 1800;    -- 30분
SET SESSION lock_wait_timeout = 300;     -- 5분

-- =====================================================
-- 1. 현재 인덱스 상태 확인
-- =====================================================
SELECT '=== 현재 인덱스 상태 확인 ===' as status;

SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    NON_UNIQUE,
    INDEX_TYPE,
    INDEX_COMMENT
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data_%'
GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_TYPE, INDEX_COMMENT
ORDER BY INDEX_NAME;

-- =====================================================
-- 2. 인덱스 1: 장치별 시간 범위 조회용 (uuid, inst_dtm)
-- =====================================================
SELECT '=== 인덱스 1 처리 시작 ===' as status;

-- 인덱스 존재 여부 확인
SET @index1_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_sensor_data'
      AND INDEX_NAME = 'idx_hnt_sensor_data_uuid_inst_dtm'
);

-- 디버깅: 인덱스 존재 여부 확인
SELECT @index1_exists as index1_exists_count;

-- 조건부 SQL 생성
SET @sql1 = IF(@index1_exists = 0, 
    'CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm ON hnt_sensor_data (uuid, inst_dtm) COMMENT ''장치별 시간 범위 조회용 복합 인덱스''',
    'SELECT ''인덱스 1이 이미 존재합니다'' as message'
);

-- 디버깅: 생성될 SQL 확인
SELECT @sql1 as sql1_to_execute;

-- SQL 실행
PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SELECT '=== 인덱스 1 완료 ===' as status;

-- =====================================================
-- 3. 인덱스 2: 사용자별 장치 조회용 (user_id, uuid)
-- =====================================================
SELECT '=== 인덱스 2 처리 시작 ===' as status;

-- 인덱스 존재 여부 확인
SET @index2_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_sensor_data'
      AND INDEX_NAME = 'idx_hnt_sensor_data_user_id_uuid'
);

-- 디버깅: 인덱스 존재 여부 확인
SELECT @index2_exists as index2_exists_count;

-- 조건부 SQL 생성
SET @sql2 = IF(@index2_exists = 0, 
    'CREATE INDEX idx_hnt_sensor_data_user_id_uuid ON hnt_sensor_data (user_id, uuid) COMMENT ''사용자별 장치 조회용 복합 인덱스''',
    'SELECT ''인덱스 2가 이미 존재합니다'' as message'
);

-- 디버깅: 생성될 SQL 확인
SELECT @sql2 as sql2_to_execute;

-- SQL 실행
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SELECT '=== 인덱스 2 완료 ===' as status;

-- =====================================================
-- 4. 인덱스 3: 시간 기반 조회용 (inst_dtm)
-- =====================================================
SELECT '=== 인덱스 3 처리 시작 ===' as status;

-- 인덱스 존재 여부 확인
SET @index3_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'hnt' 
      AND TABLE_NAME = 'hnt_sensor_data'
      AND INDEX_NAME = 'idx_hnt_sensor_data_inst_dtm'
);

-- 디버깅: 인덱스 존재 여부 확인
SELECT @index3_exists as index3_exists_count;

-- 조건부 SQL 생성
SET @sql3 = IF(@index3_exists = 0, 
    'CREATE INDEX idx_hnt_sensor_data_inst_dtm ON hnt_sensor_data (inst_dtm) COMMENT ''시간 범위 조회용 인덱스''',
    'SELECT ''인덱스 3이 이미 존재합니다'' as message'
);

-- 디버깅: 생성될 SQL 확인
SELECT @sql3 as sql3_to_execute;

-- SQL 실행
PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

SELECT '=== 인덱스 3 완료 ===' as status;

-- =====================================================
-- 5. 최종 인덱스 상태 확인
-- =====================================================
SELECT '=== 최종 인덱스 상태 확인 ===' as status;

SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    NON_UNIQUE,
    INDEX_TYPE,
    INDEX_COMMENT,
    '생성 완료' as status
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data_%'
GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_TYPE, INDEX_COMMENT
ORDER BY INDEX_NAME;

-- =====================================================
-- 6. 테이블 통계 정보
-- =====================================================
SELECT '=== 테이블 통계 정보 ===' as status;

SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    DATA_LENGTH,
    INDEX_LENGTH,
    (DATA_LENGTH + INDEX_LENGTH) as TOTAL_SIZE,
    ROUND((INDEX_LENGTH / (DATA_LENGTH + INDEX_LENGTH)) * 100, 2) as INDEX_RATIO_PERCENT
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data';

SELECT '=== 모든 작업 완료! ===' as status;
