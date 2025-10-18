-- =====================================================
-- 데이터베이스 인덱스 최적화 전 백업 파일
-- 작성일: 2025-10-17
-- 목적: 무의미한 인덱스 삭제 전 롤백용 백업
-- =====================================================

-- 데이터베이스 선택
USE hnt;

-- =====================================================
-- 현재 인덱스 상태 백업 (hnt_sensor_data)
-- =====================================================

-- 테이블 구조 백업
SHOW CREATE TABLE hnt_sensor_data;

-- 인덱스 정보 백업
SHOW INDEX FROM hnt_sensor_data;

-- =====================================================
-- 롤백용 CREATE INDEX 문
-- =====================================================

-- 삭제 대상 인덱스 재생성 SQL (롤백 시 사용)

-- 1. inst_id 인덱스 재생성
-- Cardinality: 1 (무의미한 인덱스)
ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_inst_id_IDX (inst_id);

-- 2. mdf_id 인덱스 재생성
-- Cardinality: 1 (무의미한 인덱스)
ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_mdf_id_IDX (mdf_id);

-- =====================================================
-- 인덱스 삭제 전 크기 확인 쿼리
-- =====================================================

SELECT 
    table_name,
    ROUND(data_length/1024/1024, 2) AS data_mb,
    ROUND(index_length/1024/1024, 2) AS index_mb,
    ROUND((data_length + index_length)/1024/1024, 2) AS total_mb,
    table_rows
FROM information_schema.tables
WHERE table_schema = 'hnt' AND table_name = 'hnt_sensor_data';

-- 예상 결과:
-- table_name: hnt_sensor_data
-- data_mb: 5527.68
-- index_mb: 5973.96
-- total_mb: 11501.64
-- table_rows: 28966082

-- =====================================================
-- 삭제할 인덱스 정보
-- =====================================================

-- inst_id 인덱스 정보
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME = 'hnt_sensor_data_inst_id_IDX';

-- mdf_id 인덱스 정보
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME = 'hnt_sensor_data_mdf_id_IDX';

-- =====================================================
-- 주의사항
-- =====================================================
-- 1. 이 파일은 롤백 시 사용됩니다.
-- 2. 인덱스 삭제 후 문제 발생 시 위의 ALTER TABLE 문을 실행하세요.
-- 3. 인덱스 재생성은 시간이 오래 걸릴 수 있습니다 (약 2,900만 건 처리).
-- 4. 백업 파일은 삭제하지 마세요.

-- =====================================================
-- 롤백 절차
-- =====================================================
-- 1. 이 파일의 ALTER TABLE 문을 실행
-- 2. 인덱스 재생성 완료까지 대기 (약 30-60분 소요 예상)
-- 3. SHOW INDEX FROM hnt_sensor_data; 로 인덱스 복구 확인
-- 4. 애플리케이션 정상 동작 확인

-- =====================================================
-- 작성자: Cursor AI Assistant
-- 검토자: HnT Solutions 개발팀
-- =====================================================

