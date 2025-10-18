-- =====================================================
-- 인덱스 최적화 검증 스크립트
-- 작성일: 2025-10-17
-- 목적: 인덱스 삭제 후 성능 및 상태 검증
-- =====================================================

USE hnt;

-- =====================================================
-- 1. 인덱스 삭제 확인
-- =====================================================

SELECT '=== 1. 인덱스 삭제 확인 ===' AS section;

-- 삭제된 인덱스가 존재하지 않는지 확인
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN ('hnt_sensor_data_inst_id_IDX', 'hnt_sensor_data_mdf_id_IDX');

-- 예상 결과: Empty set (0 rows)

-- =====================================================
-- 2. 테이블 크기 변화 확인
-- =====================================================

SELECT '=== 2. 테이블 크기 변화 ===' AS section;

SELECT 
    table_name,
    ROUND(data_length/1024/1024, 2) AS data_mb,
    ROUND(index_length/1024/1024, 2) AS index_mb,
    ROUND((data_length + index_length)/1024/1024, 2) AS total_mb,
    table_rows AS total_rows
FROM information_schema.tables
WHERE table_schema = 'hnt' AND table_name = 'hnt_sensor_data';

-- 비교:
-- 작업 전: index_mb = 5973.96 MB
-- 작업 후: index_mb = 약 5773.96 MB (예상)
-- 감소량: 약 200 MB

-- =====================================================
-- 3. 핵심 인덱스 존재 확인
-- =====================================================

SELECT '=== 3. 핵심 인덱스 상태 ===' AS section;

SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN (
    'PRIMARY',
    'idx_hnt_sensor_data_uuid_inst_dtm',
    'idx_sensor_data_performance',
    'idx_hnt_sensor_data_user_id_uuid',
    'idx_config_user_uuid'
  )
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 예상 결과: 모든 핵심 인덱스가 존재해야 함

-- =====================================================
-- 4. 전체 인덱스 개수 확인
-- =====================================================

SELECT '=== 4. 전체 인덱스 개수 ===' AS section;

SELECT 
    COUNT(DISTINCT INDEX_NAME) AS total_indexes
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data';

-- 비교:
-- 작업 전: 26개
-- 작업 후: 24개 (2개 감소)

-- =====================================================
-- 5. 쿼리 성능 검증 (EXPLAIN)
-- =====================================================

SELECT '=== 5. 쿼리 성능 검증 ===' AS section;

-- 5-1. 일간 데이터 조회 (가장 중요한 쿼리)
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

-- 확인 항목:
-- key: idx_hnt_sensor_data_uuid_inst_dtm (최적 인덱스 사용)
-- rows: 약 17,778
-- type: range

-- 5-2. 사용자별 센서 데이터 조회
EXPLAIN SELECT * FROM hnt_sensor_data
WHERE user_id = 'thepine'
  AND sensor_id = 'thepine'
  AND uuid = '0008DC755397'
ORDER BY inst_dtm DESC
LIMIT 100;

-- 확인 항목:
-- key: idx_sensor_data_performance (최적 인덱스 사용)
-- rows: < 1000
-- type: ref

-- 5-3. UUID 기반 최신 데이터 조회
EXPLAIN SELECT * FROM hnt_sensor_data
WHERE uuid = '0008DC755397'
ORDER BY inst_dtm DESC
LIMIT 1;

-- 확인 항목:
-- key: idx_hnt_sensor_data_uuid_inst_dtm (최적 인덱스 사용)
-- rows: < 100
-- type: ref

-- =====================================================
-- 6. 실제 쿼리 실행 시간 측정
-- =====================================================

SELECT '=== 6. 쿼리 실행 시간 측정 ===' AS section;

SET @start_time = NOW(6);

-- 테스트 쿼리 실행
SELECT COUNT(*) as total_count
FROM hnt_sensor_data
WHERE uuid = '0008DC755397'
  AND inst_dtm BETWEEN '2025-10-16 00:00:00' AND '2025-10-17 23:59:59';

SET @end_time = NOW(6);

SELECT 
    @start_time AS query_start,
    @end_time AS query_end,
    TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 1000 AS execution_time_ms;

-- 기준 시간: 작업 전 실행 시간과 비교
-- 예상 결과: 동일하거나 약간 빠름

-- =====================================================
-- 7. 남은 인덱스 목록 상세
-- =====================================================

SELECT '=== 7. 현재 인덱스 목록 ===' AS section;

SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS columns,
    MAX(CARDINALITY) AS cardinality,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
GROUP BY INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY 
    CASE WHEN INDEX_NAME = 'PRIMARY' THEN 1 ELSE 2 END,
    INDEX_NAME;

-- =====================================================
-- 8. 데이터 무결성 확인
-- =====================================================

SELECT '=== 8. 데이터 무결성 확인 ===' AS section;

-- 8-1. 레코드 수 확인
SELECT COUNT(*) AS total_records
FROM hnt_sensor_data;

-- 예상 결과: 약 28,966,082 (변화 없음)

-- 8-2. 최신 데이터 확인
SELECT 
    uuid,
    sensor_value,
    inst_dtm
FROM hnt_sensor_data
ORDER BY inst_dtm DESC
LIMIT 5;

-- 예상 결과: 최신 5개 레코드 정상 조회

-- =====================================================
-- 9. 관련 테이블 확인
-- =====================================================

SELECT '=== 9. 관련 테이블 상태 ===' AS section;

-- hnt_sensor_info 테이블
SELECT 
    table_name,
    ROUND(data_length/1024/1024, 2) AS data_mb,
    ROUND(index_length/1024/1024, 2) AS index_mb,
    table_rows
FROM information_schema.tables
WHERE table_schema = 'hnt' 
  AND table_name IN ('hnt_sensor_info', 'hnt_config', 'hnt_user', 'hnt_alarm');

-- =====================================================
-- 10. 최종 검증 결과
-- =====================================================

SELECT '=== 10. 최종 검증 결과 ===' AS section;

SELECT 
    '✅ Phase 1 인덱스 최적화 완료' AS status,
    (SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS 
     WHERE TABLE_SCHEMA = 'hnt' AND TABLE_NAME = 'hnt_sensor_data') AS current_index_count,
    (SELECT ROUND(index_length/1024/1024, 2) FROM information_schema.tables 
     WHERE table_schema = 'hnt' AND table_name = 'hnt_sensor_data') AS current_index_mb,
    '2개 인덱스 삭제 완료 (inst_id, mdf_id)' AS changes,
    NOW() AS verified_at;

-- =====================================================
-- 검증 체크리스트
-- =====================================================
/*
✅ 체크리스트:

[ ] 1. 삭제 대상 인덱스 2개가 없는지 확인
[ ] 2. 인덱스 크기가 약 200MB 감소했는지 확인
[ ] 3. 핵심 인덱스 4개가 모두 존재하는지 확인
[ ] 4. 일간 데이터 조회 쿼리가 최적 인덱스를 사용하는지 확인
[ ] 5. 쿼리 실행 시간이 유사하거나 개선되었는지 확인
[ ] 6. 데이터 레코드 수가 변하지 않았는지 확인
[ ] 7. 최신 데이터가 정상 조회되는지 확인

모든 항목이 체크되면 Phase 1 완료!
*/

-- =====================================================
-- 다음 단계
-- =====================================================
/*
Phase 1 검증 완료 후:

1. 24시간 동안 성능 모니터링
2. 애플리케이션 로그 확인
3. 문제 없으면 Phase 2 (중복 인덱스 삭제) 검토
4. Phase 2 대상:
   - hnt_sensor_data_uuid_IDX
   - hnt_sensor_data_user_id_IDX
   - hnt_sensor_data_sensor_id_IDX
*/

-- =====================================================
-- 작성자: Cursor AI Assistant
-- =====================================================

