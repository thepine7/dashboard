-- =====================================================
-- Phase 3: hnt_sensor_data_no_IDX 인덱스 삭제
-- =====================================================
-- 작성일: 2025-10-17
-- 목적: PRIMARY KEY와 중복되는 no 인덱스 삭제
-- 예상 효과: 약 100MB 절감
-- 리스크: 낮음 (PRIMARY KEY로 충분)
-- =====================================================

-- 1. 현재 상태 확인
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN ('PRIMARY', 'hnt_sensor_data_no_IDX')
ORDER BY INDEX_NAME;

-- 2. 코드에서 사용 여부 확인 (이미 검증 완료)
-- grep -r "WHERE.*no\s*=" src/main/resources/mapper
-- 결과: No matches found

-- 3. 인덱스 삭제 실행
-- ⚠️ 주의: 운영 DB에서는 피크 시간대를 피해서 실행하세요
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_no_IDX;

-- 4. 삭제 후 상태 확인
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
ORDER BY INDEX_NAME;

-- 5. 테이블 크기 확인
SELECT 
    table_name,
    ROUND((data_length) / (1024 * 1024), 2) AS data_size_mb,
    ROUND((index_length) / (1024 * 1024), 2) AS index_size_mb,
    ROUND((data_length + index_length) / (1024 * 1024), 2) AS total_size_mb
FROM information_schema.TABLES
WHERE table_schema = 'hnt' 
  AND table_name = 'hnt_sensor_data';

-- =====================================================
-- 롤백 스크립트 (필요 시 사용)
-- =====================================================
-- CREATE INDEX hnt_sensor_data_no_IDX ON hnt_sensor_data(no);
-- =====================================================

