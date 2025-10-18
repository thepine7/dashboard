-- =====================================================
-- Phase 2: 중복 인덱스 분석 및 삭제 계획
-- 작성일: 2025-10-17
-- 목적: Phase 1 완료 후 중복 인덱스 삭제 검토
-- 주의: Phase 1 안정화 확인 후 진행
-- =====================================================

USE hnt;

-- =====================================================
-- 1. Phase 2 삭제 대상 인덱스 분석
-- =====================================================

SELECT '=== Phase 2 삭제 대상 인덱스 ===' AS section;

-- 1-1. uuid 단일 인덱스 vs 복합 인덱스
SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS columns,
    MAX(CARDINALITY) AS cardinality,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND (INDEX_NAME = 'hnt_sensor_data_uuid_IDX' 
       OR INDEX_NAME LIKE '%uuid%')
GROUP BY INDEX_NAME, INDEX_TYPE
ORDER BY INDEX_NAME;

/*
예상 결과:
1. hnt_sensor_data_uuid_IDX (uuid) ← 삭제 대상
2. idx_hnt_sensor_data_uuid_inst_dtm (uuid, inst_dtm) ← 유지 (복합)
3. idx_sensor_data_performance (user_id, sensor_id, uuid, inst_dtm) ← 유지 (복합)
4. idx_hnt_sensor_data_user_id_uuid (user_id, uuid) ← 유지 (복합)

결론: uuid 단일 인덱스는 불필요 (복합 인덱스로 대체 가능)
*/

-- 1-2. user_id 단일 인덱스 vs 복합 인덱스
SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS columns,
    MAX(CARDINALITY) AS cardinality,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND (INDEX_NAME = 'hnt_sensor_data_user_id_IDX' 
       OR INDEX_NAME LIKE '%user_id%')
GROUP BY INDEX_NAME, INDEX_TYPE
ORDER BY INDEX_NAME;

/*
예상 결과:
1. hnt_sensor_data_user_id_IDX (user_id) ← 삭제 대상
2. idx_sensor_data_performance (user_id, sensor_id, uuid, inst_dtm) ← 유지 (복합)
3. idx_hnt_sensor_data_user_id_uuid (user_id, uuid) ← 유지 (복합)

결론: user_id 단일 인덱스는 불필요 (복합 인덱스로 대체 가능)
*/

-- 1-3. sensor_id 단일 인덱스 vs 복합 인덱스
SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS columns,
    MAX(CARDINALITY) AS cardinality,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND (INDEX_NAME = 'hnt_sensor_data_sensor_id_IDX' 
       OR INDEX_NAME LIKE '%sensor_id%')
GROUP BY INDEX_NAME, INDEX_TYPE
ORDER BY INDEX_NAME;

/*
예상 결과:
1. hnt_sensor_data_sensor_id_IDX (sensor_id) ← 삭제 대상
2. idx_sensor_data_performance (user_id, sensor_id, uuid, inst_dtm) ← 유지 (복합)

결론: sensor_id 단일 인덱스는 불필요 (복합 인덱스로 대체 가능)
*/

-- =====================================================
-- 2. 쿼리 영향도 분석
-- =====================================================

SELECT '=== 2. 쿼리 영향도 분석 ===' AS section;

-- 2-1. uuid만 사용하는 쿼리 (복합 인덱스로 대체 가능)
EXPLAIN SELECT * FROM hnt_sensor_data
WHERE uuid = '0008DC755397'
ORDER BY inst_dtm DESC
LIMIT 1;

/*
현재: hnt_sensor_data_uuid_IDX 또는 idx_hnt_sensor_data_uuid_inst_dtm
삭제 후: idx_hnt_sensor_data_uuid_inst_dtm 사용 (성능 동일 또는 향상)
*/

-- 2-2. user_id만 사용하는 쿼리
EXPLAIN SELECT COUNT(*) FROM hnt_sensor_data
WHERE user_id = 'thepine';

/*
현재: hnt_sensor_data_user_id_IDX 또는 idx_sensor_data_performance
삭제 후: idx_sensor_data_performance 사용 (성능 동일)
*/

-- 2-3. sensor_id만 사용하는 쿼리
EXPLAIN SELECT COUNT(*) FROM hnt_sensor_data
WHERE sensor_id = 'thepine';

/*
현재: hnt_sensor_data_sensor_id_IDX 또는 idx_sensor_data_performance
삭제 후: idx_sensor_data_performance 사용 (성능 동일)
*/

-- =====================================================
-- 3. Phase 2 실행 계획
-- =====================================================

/*
단계별 삭제 계획:

1단계 (Day 1): uuid 인덱스 삭제
   ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_uuid_IDX;
   → 24시간 모니터링

2단계 (Day 2): user_id 인덱스 삭제
   ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_user_id_IDX;
   → 24시간 모니터링

3단계 (Day 3): sensor_id 인덱스 삭제
   ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_sensor_id_IDX;
   → 24시간 모니터링

예상 효과:
- 인덱스 크기: 약 500MB 감소
- INSERT 성능: 10% 향상
- SELECT 성능: 영향 없음 (복합 인덱스 사용)
*/

-- =====================================================
-- 4. 롤백 SQL (Phase 2용)
-- =====================================================

/*
문제 발생 시 인덱스 재생성:

-- uuid 인덱스 재생성
ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_uuid_IDX (uuid);

-- user_id 인덱스 재생성
ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_user_id_IDX (user_id);

-- sensor_id 인덱스 재생성
ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_sensor_id_IDX (sensor_id);

재생성 시간: 각 약 30-60분 예상
*/

-- =====================================================
-- 5. Phase 2 실행 조건
-- =====================================================

/*
Phase 2를 진행하기 전 확인 사항:

✅ Phase 1 완료 확인:
   [ ] inst_id, mdf_id 인덱스 삭제 완료
   [ ] 인덱스 크기 약 200MB 감소 확인
   [ ] 애플리케이션 정상 동작 (24시간 이상)
   [ ] 쿼리 성능 저하 없음
   [ ] 에러 로그 없음

⚠️ Phase 2 진행 조건:
   [ ] 모든 Phase 1 확인 사항 통과
   [ ] 개발팀 승인
   [ ] 백업 완료
   [ ] 트래픽이 적은 시간대 (새벽 2-4시)
   [ ] 롤백 계획 준비

❌ Phase 2 진행 불가 조건:
   - Phase 1에서 문제 발생
   - 쿼리 성능 저하 발견
   - 애플리케이션 에러 발생
   - 트래픽 증가 시기
*/

-- =====================================================
-- 6. 예상 최종 결과
-- =====================================================

SELECT '=== Phase 1 + Phase 2 완료 후 예상 결과 ===' AS section;

SELECT 
    '현재 (Phase 0)' AS phase,
    26 AS index_count,
    5973.96 AS index_mb,
    11501.64 AS total_mb
UNION ALL
SELECT 
    'Phase 1 완료',
    24,
    5773.96,
    11301.64
UNION ALL
SELECT 
    'Phase 2 완료 (예상)',
    21,
    5273.96,
    10801.64;

/*
최종 효과:
- 인덱스 개수: 26개 → 21개 (5개 삭제)
- 인덱스 크기: 5973.96MB → 5273.96MB (700MB 감소, 11.7%)
- 전체 크기: 11501.64MB → 10801.64MB (700MB 감소, 6.1%)
- INSERT 성능: 15% 향상 예상
- SELECT 성능: 영향 없음 또는 약간 향상
*/

-- =====================================================
-- 7. 모니터링 쿼리 (Phase 2용)
-- =====================================================

-- 7-1. 인덱스 사용 현황 확인
SELECT 
    INDEX_NAME,
    COUNT(*) AS usage_count
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
GROUP BY INDEX_NAME
ORDER BY usage_count DESC;

-- 7-2. 슬로우 쿼리 로그 확인 (MySQL 설정 필요)
-- SHOW VARIABLES LIKE 'slow_query%';
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 1;

-- 7-3. 인덱스 크기 추이 모니터링
SELECT 
    CURRENT_TIMESTAMP() AS check_time,
    table_name,
    ROUND(data_length/1024/1024, 2) AS data_mb,
    ROUND(index_length/1024/1024, 2) AS index_mb,
    ROUND((data_length + index_length)/1024/1024, 2) AS total_mb
FROM information_schema.tables
WHERE table_schema = 'hnt' AND table_name = 'hnt_sensor_data';

-- =====================================================
-- 작성자: Cursor AI Assistant
-- 검토 필요: HnT Solutions 개발팀
-- =====================================================

