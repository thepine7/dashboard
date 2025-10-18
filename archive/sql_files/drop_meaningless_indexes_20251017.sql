-- =====================================================
-- 무의미한 인덱스 삭제 스크립트
-- 작성일: 2025-10-17
-- 대상: hnt_sensor_data 테이블
-- 목적: Cardinality 1인 무의미한 인덱스 삭제
-- =====================================================

-- 데이터베이스 선택
USE hnt;

-- =====================================================
-- Phase 1: 삭제 전 확인
-- =====================================================

-- 1. 현재 테이블 크기 확인
SELECT 
    table_name,
    ROUND(data_length/1024/1024, 2) AS data_mb,
    ROUND(index_length/1024/1024, 2) AS index_mb,
    ROUND((data_length + index_length)/1024/1024, 2) AS total_mb
FROM information_schema.tables
WHERE table_schema = 'hnt' AND table_name = 'hnt_sensor_data';

-- 2. 삭제 대상 인덱스 확인
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY,
    INDEX_TYPE,
    COMMENT
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN ('hnt_sensor_data_inst_id_IDX', 'hnt_sensor_data_mdf_id_IDX')
ORDER BY INDEX_NAME;

-- 예상 결과:
-- INDEX_NAME: hnt_sensor_data_inst_id_IDX
-- COLUMN_NAME: inst_id
-- CARDINALITY: 1 (무의미!)
-- 
-- INDEX_NAME: hnt_sensor_data_mdf_id_IDX
-- COLUMN_NAME: mdf_id
-- CARDINALITY: 1 (무의미!)

-- =====================================================
-- Phase 2: 인덱스 삭제 실행
-- =====================================================

-- 주의: 실행 전 반드시 백업 파일 확인!
-- 백업 파일: archive/sql_files/backup_before_index_optimization_20251017.sql

-- 1. inst_id 인덱스 삭제
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_inst_id_IDX;

-- 성공 메시지 확인
SELECT 'inst_id 인덱스 삭제 완료' AS status;

-- 2. mdf_id 인덱스 삭제
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_mdf_id_IDX;

-- 성공 메시지 확인
SELECT 'mdf_id 인덱스 삭제 완료' AS status;

-- =====================================================
-- Phase 3: 삭제 후 검증
-- =====================================================

-- 1. 인덱스 삭제 확인
SELECT 
    INDEX_NAME,
    COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN ('hnt_sensor_data_inst_id_IDX', 'hnt_sensor_data_mdf_id_IDX');

-- 예상 결과: Empty set (인덱스가 삭제되었음을 의미)

-- 2. 테이블 크기 확인 (삭제 후)
SELECT 
    table_name,
    ROUND(data_length/1024/1024, 2) AS data_mb,
    ROUND(index_length/1024/1024, 2) AS index_mb,
    ROUND((data_length + index_length)/1024/1024, 2) AS total_mb
FROM information_schema.tables
WHERE table_schema = 'hnt' AND table_name = 'hnt_sensor_data';

-- 예상 결과:
-- index_mb: 약 5773.96 MB (5973.96 - 200 = 5773.96)
-- 약 200MB 감소 예상

-- 3. 남은 인덱스 목록 확인
SHOW INDEX FROM hnt_sensor_data;

-- 4. 핵심 인덱스가 여전히 존재하는지 확인
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN (
    'idx_hnt_sensor_data_uuid_inst_dtm',
    'idx_sensor_data_performance',
    'idx_hnt_sensor_data_user_id_uuid'
  )
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 예상 결과: 3개 핵심 인덱스 모두 존재해야 함

-- =====================================================
-- Phase 4: 성능 검증
-- =====================================================

-- 1. 일간 데이터 조회 쿼리 실행 계획 확인
EXPLAIN SELECT 
    date_format(inst_dtm, '%Y-%m-%d') as getDate,
    concat(date_format(inst_dtm, '%Y-%m-%d %H:'), lpad(minute(inst_dtm), 2, '0')) as inst_dtm,
    round(avg(sensor_value), 1) as sensor_value
FROM hnt_sensor_data
WHERE uuid = '0008DC755397'
  AND inst_dtm BETWEEN '2025-10-16 00:00:00' AND '2025-10-17 23:59:59'
GROUP BY date_format(inst_dtm, '%Y-%m-%d'), 
         concat(date_format(inst_dtm, '%Y-%m-%d %H:'), lpad(minute(inst_dtm), 2, '0'))
ORDER BY inst_dtm ASC;

-- 예상 결과:
-- key: idx_hnt_sensor_data_uuid_inst_dtm (여전히 최적 인덱스 사용)
-- rows: 약 17,778 (변화 없음)

-- 2. 센서 정보 조회 쿼리 실행 계획 확인
EXPLAIN SELECT * FROM hnt_sensor_data
WHERE user_id = 'thepine'
  AND sensor_id = 'thepine'
  AND uuid = '0008DC755397'
ORDER BY inst_dtm DESC
LIMIT 100;

-- 예상 결과:
-- key: idx_sensor_data_performance (여전히 최적 인덱스 사용)

-- =====================================================
-- 작업 완료 보고
-- =====================================================

SELECT 
    '인덱스 최적화 Phase 1 완료' AS status,
    '삭제된 인덱스: inst_id, mdf_id' AS deleted_indexes,
    '예상 효과: 인덱스 크기 약 200MB 감소, INSERT 성능 5% 향상' AS expected_effect,
    NOW() AS completed_at;

-- =====================================================
-- 롤백 방법 (문제 발생 시)
-- =====================================================
-- 
-- backup_before_index_optimization_20251017.sql 파일의 
-- ALTER TABLE 문을 실행하여 인덱스를 재생성하세요.
--
-- ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_inst_id_IDX (inst_id);
-- ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_mdf_id_IDX (mdf_id);
--

-- =====================================================
-- 모니터링 권장사항
-- =====================================================
-- 1. 작업 후 24시간 동안 성능 모니터링
-- 2. 애플리케이션 로그에서 에러 확인
-- 3. 쿼리 응답 시간 변화 확인
-- 4. INSERT 처리량 변화 확인
-- 5. 문제 없으면 Phase 2 (중복 인덱스 삭제) 진행 검토

-- =====================================================
-- 작성자: Cursor AI Assistant
-- 실행자: HnT Solutions 개발팀
-- =====================================================

