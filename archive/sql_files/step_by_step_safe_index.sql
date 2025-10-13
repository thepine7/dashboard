-- =====================================================
-- 단계별 안전한 인덱스 생성 (MySQL 5.7 호환)
-- 각 단계를 개별적으로 실행하여 안전하게 처리
-- =====================================================

-- =====================================================
-- STEP 1: 기존 인덱스 상태 확인
-- =====================================================
-- 이 쿼리를 먼저 실행하여 현재 인덱스 상태를 확인하세요
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
-- STEP 2-1: 장치별 시간 범위 조회용 복합 인덱스 생성
-- =====================================================
-- 위 쿼리 결과에서 'idx_hnt_sensor_data_uuid_inst_dtm'이 없으면 실행
-- 예상 소요 시간: 5-10분
CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
COMMENT '장치별 시간 범위 조회용 복합 인덱스';

-- =====================================================
-- STEP 2-2: 사용자별 장치 조회용 인덱스 생성
-- =====================================================
-- 위 쿼리 결과에서 'idx_hnt_sensor_data_user_id_uuid'가 없으면 실행
-- 예상 소요 시간: 3-5분
CREATE INDEX idx_hnt_sensor_data_user_id_uuid 
ON hnt_sensor_data (user_id, uuid) 
COMMENT '사용자별 장치 조회용 복합 인덱스';

-- =====================================================
-- STEP 2-3: 시간 기반 조회용 인덱스 생성
-- =====================================================
-- 위 쿼리 결과에서 'idx_hnt_sensor_data_inst_dtm'이 없으면 실행
-- 예상 소요 시간: 10-15분
CREATE INDEX idx_hnt_sensor_data_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
COMMENT '시간 범위 조회용 인덱스';

-- =====================================================
-- STEP 3: 최종 인덱스 상태 확인
-- =====================================================
-- 모든 인덱스 생성 완료 후 최종 확인
SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    NON_UNIQUE,
    INDEX_COMMENT,
    '생성 완료' as STATUS
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
