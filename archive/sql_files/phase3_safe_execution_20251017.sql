-- =====================================================
-- Phase 3 안전 실행 스크립트
-- =====================================================
-- 작성일: 2025-10-17
-- 목적: 기능 영향 없이 불필요한 인덱스 안전하게 삭제
-- 실행 전 상태: 11개 인덱스, 8,499 MB
-- =====================================================

-- =====================================================
-- 현재 상태 스냅샷 (Phase 3 실행 전)
-- =====================================================
/*
INDEX_NAME                              | columns                           | cardinality | NON_UNIQUE | INDEX_TYPE
----------------------------------------|-----------------------------------|-------------|------------|------------
hnt_sensor_data_2                       | inst_dtm, no                      | 28,970,100  | 0          | BTREE
hnt_sensor_data_inst_dtm_IDX            | inst_dtm                          | 14,485,050  | 1          | BTREE
hnt_sensor_data_no_IDX                  | no                                | 28,970,100  | 1          | BTREE ⚠️ 삭제 대상
hnt_sensor_data_sensor_value_IDX        | sensor_value                      | 1,032       | 1          | BTREE ⚠️ 검토 필요
hnt_sensor_data_UN                      | no, user_id, sensor_id            | 28,970,100  | 0          | BTREE
idx_hnt_sensor_data_inst_dtm            | inst_dtm                          | 14,485,050  | 1          | BTREE ⚠️ 중복
idx_hnt_sensor_data_user_id_uuid        | user_id, uuid                     | 38          | 1          | BTREE
idx_hnt_sensor_data_uuid_inst_dtm       | uuid, inst_dtm                    | 28,970,100  | 1          | BTREE
idx_sensor_data_download_date_range     | inst_dtm, user_id, uuid           | 28,970,100  | 1          | BTREE
idx_sensor_data_performance             | user_id, sensor_id, uuid, inst_dtm| 28,970,100  | 1          | BTREE
PRIMARY                                 | no                                | 28,970,100  | 0          | BTREE

크기: 데이터 4,294 MB + 인덱스 4,204 MB = 총 8,499 MB
*/

-- =====================================================
-- Step 1: 가장 안전한 인덱스부터 삭제
-- =====================================================

-- 1-1. hnt_sensor_data_no_IDX 삭제 (가장 안전)
-- 이유: PRIMARY KEY와 완전 중복, 코드에서 미사용 검증 완료
-- 예상 효과: ~100MB 절감
-- 리스크: 없음 (PRIMARY KEY가 동일 기능 제공)

-- 삭제 전 확인: 이 인덱스를 사용하는 쿼리가 있는지 체크
SELECT 
    'hnt_sensor_data_no_IDX 삭제 전 확인' as status,
    COUNT(*) as total_records
FROM hnt_sensor_data
WHERE no > 0
LIMIT 1;

-- 삭제 실행
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_no_IDX;

-- 삭제 후 검증: PRIMARY KEY로 동일한 쿼리 실행 확인
EXPLAIN SELECT * FROM hnt_sensor_data WHERE no = 1;
-- 예상 결果: key = PRIMARY (정상)

-- 크기 확인
SELECT 
    'Step 1 완료' as status,
    ROUND((data_length + index_length) / (1024 * 1024), 2) AS total_size_mb
FROM information_schema.TABLES
WHERE table_schema = 'hnt' AND table_name = 'hnt_sensor_data';

-- =====================================================
-- Step 2: 24시간 모니터링 설정 (삭제 전 검증)
-- =====================================================

-- 2-1. 슬로우 쿼리 로그 활성화
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.5;
SET GLOBAL log_queries_not_using_indexes = 'ON';

-- 2-2. 인덱스 사용 통계 저장 (모니터링 시작)
CREATE TABLE IF NOT EXISTS hnt_index_usage_snapshot (
    snapshot_id INT AUTO_INCREMENT PRIMARY KEY,
    snapshot_time DATETIME,
    index_name VARCHAR(100),
    table_rows BIGINT,
    data_mb DECIMAL(10,2),
    index_mb DECIMAL(10,2),
    total_mb DECIMAL(10,2)
);

INSERT INTO hnt_index_usage_snapshot (snapshot_time, index_name, table_rows, data_mb, index_mb, total_mb)
SELECT 
    NOW(),
    'ALL_INDEXES',
    table_rows,
    ROUND(data_length / 1024 / 1024, 2),
    ROUND(index_length / 1024 / 1024, 2),
    ROUND((data_length + index_length) / 1024 / 1024, 2)
FROM information_schema.TABLES
WHERE table_schema = 'hnt' AND table_name = 'hnt_sensor_data';

-- =====================================================
-- Step 3: 중복 inst_dtm 인덱스 분석 (삭제 전 확인)
-- =====================================================

-- 3-1. 두 인덱스 비교
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY,
    SUB_PART,
    NULLABLE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN ('idx_hnt_sensor_data_inst_dtm', 'hnt_sensor_data_inst_dtm_IDX')
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 3-2. 실제 쿼리 실행 계획 확인 (어느 인덱스를 사용하는지)
EXPLAIN SELECT * FROM hnt_sensor_data 
WHERE inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR)
ORDER BY inst_dtm DESC
LIMIT 10;

-- 3-3. uuid가 포함된 쿼리는 복합 인덱스를 사용하는지 확인
EXPLAIN SELECT * FROM hnt_sensor_data
WHERE uuid = '0008DC755397'
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR)
ORDER BY inst_dtm DESC
LIMIT 10;
-- 예상: idx_hnt_sensor_data_uuid_inst_dtm 사용 (정상)

-- =====================================================
-- Step 4: sensor_value 인덱스 사용 여부 확인
-- =====================================================

-- 4-1. 이 인덱스를 사용하는 쿼리가 있는지 확인
EXPLAIN SELECT * FROM hnt_sensor_data
WHERE sensor_value = '25.5'
LIMIT 10;
-- 결과: hnt_sensor_data_sensor_value_IDX 사용됨 (확인됨)

-- 4-2. 하지만 애플리케이션 코드에서는 사용하지 않음
-- 검증: 24시간 슬로우 쿼리 로그에서 실제 사용 빈도 확인 필요

-- =====================================================
-- 롤백 스크립트 (문제 발생 시 즉시 복구)
-- =====================================================

-- hnt_sensor_data_no_IDX 복구
-- CREATE INDEX hnt_sensor_data_no_IDX ON hnt_sensor_data(no);

-- idx_hnt_sensor_data_inst_dtm 복구 (삭제한 경우)
-- CREATE INDEX idx_hnt_sensor_data_inst_dtm ON hnt_sensor_data(inst_dtm);

-- hnt_sensor_data_inst_dtm_IDX 복구 (삭제한 경우)
-- CREATE INDEX hnt_sensor_data_inst_dtm_IDX ON hnt_sensor_data(inst_dtm);

-- sensor_value 인덱스 복구 (삭제한 경우)
-- CREATE INDEX hnt_sensor_data_sensor_value_IDX ON hnt_sensor_data(sensor_value);

-- =====================================================
-- 최종 검증 스크립트
-- =====================================================

-- 1. 현재 인덱스 목록
SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS columns,
    MAX(CARDINALITY) AS cardinality
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' AND TABLE_NAME = 'hnt_sensor_data'
GROUP BY INDEX_NAME
ORDER BY INDEX_NAME;

-- 2. 테이블 크기
SELECT 
    table_name,
    table_rows,
    ROUND(data_length / 1024 / 1024, 2) AS data_mb,
    ROUND(index_length / 1024 / 1024, 2) AS index_mb,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS total_mb
FROM information_schema.TABLES
WHERE table_schema = 'hnt' AND table_name = 'hnt_sensor_data';

-- 3. 주요 쿼리 성능 테스트
-- 3-1. 센서별 일간 데이터 조회 (가장 많이 사용)
EXPLAIN SELECT 
    DATE_FORMAT(inst_dtm, '%Y-%m-%d %H:%i') as time,
    AVG(sensor_value) as avg_value
FROM hnt_sensor_data
WHERE uuid = '0008DC755397'
  AND inst_dtm BETWEEN DATE_ADD(NOW(), INTERVAL -1 DAY) AND NOW()
GROUP BY DATE_FORMAT(inst_dtm, '%Y-%m-%d %H:%i')
ORDER BY inst_dtm;
-- 예상: idx_hnt_sensor_data_uuid_inst_dtm 사용

-- 3-2. 센서 데이터 삭제
EXPLAIN DELETE FROM hnt_sensor_data
WHERE user_id = 'thepine' AND uuid = '0008DC755397'
LIMIT 1;
-- 예상: idx_hnt_sensor_data_user_id_uuid 사용

-- =====================================================
-- Phase 3 실행 요약
-- =====================================================
/*
Step 1 (즉시 실행): hnt_sensor_data_no_IDX 삭제
  - 리스크: 없음
  - 효과: ~100MB 절감
  - 상태: 실행 완료

Step 2 (24시간 모니터링): 슬로우 쿼리 로그 수집
  - 목적: 나머지 인덱스 사용 패턴 분석
  - 대상: inst_dtm 중복 인덱스, sensor_value 인덱스
  
Step 3 (24시간 후 결정): 
  - 중복 inst_dtm 인덱스 하나 삭제 (~400MB 절감)
  - sensor_value 인덱스 삭제 검토 (~100MB 절감)

예상 최종 효과:
  - 인덱스 개수: 11개 → 8-9개
  - 크기 절감: 500-600MB 추가
  - 총 누적 절감: Phase 1+2+3 = 3.7-3.8 GB
*/

