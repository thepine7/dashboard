-- ============================================================================
-- 차트설정 & 사용자관리 페이지 인덱스 최적화 Phase 1
-- ============================================================================
-- 작성일: 2025-10-17
-- 대상 테이블: hnt_sensor_info, hnt_user
-- 목적: 필수 인덱스 추가로 Full table scan 제거
-- 실행 시점: 즉시
-- ============================================================================

-- ============================================================================
-- 1. 백업 정보 출력
-- ============================================================================
SELECT 'Phase 1: 필수 인덱스 추가 시작' as status;
SELECT NOW() as start_time;

-- ============================================================================
-- 2. 현재 인덱스 상태 확인
-- ============================================================================
SELECT '=== 현재 hnt_sensor_info 인덱스 ===' as info;
SHOW INDEX FROM hnt_sensor_info;

SELECT '=== 현재 hnt_user 인덱스 ===' as info;
SHOW INDEX FROM hnt_user;

-- ============================================================================
-- 3. 필수 인덱스 추가
-- ============================================================================

-- 3.1. hnt_sensor_info: sensor_uuid 단독 인덱스 추가
SELECT '=== Step 1: sensor_uuid 인덱스 추가 ===' as info;

-- 기존 인덱스 존재 여부 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '이미 존재하는 인덱스입니다. 건너뜁니다.'
        ELSE '인덱스 생성을 시작합니다.'
    END as check_result
FROM information_schema.STATISTICS
WHERE table_schema = 'hnt'
AND table_name = 'hnt_sensor_info'
AND index_name = 'idx_sensor_info_sensor_uuid';

-- 인덱스 생성 (존재하지 않는 경우만)
-- 실행 전 위 SELECT 결과 확인 필수
CREATE INDEX idx_sensor_info_sensor_uuid ON hnt_sensor_info(sensor_uuid);

SELECT '✅ sensor_uuid 인덱스 생성 완료' as status;

-- ============================================================================

-- 3.2. hnt_user: del_yn + no 복합 인덱스 추가
SELECT '=== Step 2: del_yn + no 복합 인덱스 추가 ===' as info;

-- 기존 인덱스 존재 여부 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '이미 존재하는 인덱스입니다. 건너뜁니다.'
        ELSE '인덱스 생성을 시작합니다.'
    END as check_result
FROM information_schema.STATISTICS
WHERE table_schema = 'hnt'
AND table_name = 'hnt_user'
AND index_name = 'idx_user_del_no';

-- 인덱스 생성 (존재하지 않는 경우만)
-- 실행 전 위 SELECT 결과 확인 필수
CREATE INDEX idx_user_del_no ON hnt_user(del_yn, no DESC);

SELECT '✅ del_yn + no 인덱스 생성 완료' as status;

-- ============================================================================
-- 4. 인덱스 생성 결과 확인
-- ============================================================================

SELECT '=== 인덱스 생성 후 hnt_sensor_info 인덱스 ===' as info;
SHOW INDEX FROM hnt_sensor_info;

SELECT '=== 인덱스 생성 후 hnt_user 인덱스 ===' as info;
SHOW INDEX FROM hnt_user;

-- ============================================================================
-- 5. 인덱스 사용 검증 (EXPLAIN)
-- ============================================================================

-- 5.1. getSensorInfoByUuid 쿼리 검증
SELECT '=== 검증 1: getSensorInfoByUuid (sensor_uuid 인덱스 사용 확인) ===' as info;
EXPLAIN SELECT * FROM hnt_sensor_info WHERE sensor_uuid = '0008DC755397' LIMIT 1;

-- 예상 결과:
-- key: idx_sensor_info_sensor_uuid
-- type: ref (또는 eq_ref)
-- rows: 1

-- ============================================================================

-- 5.2. getUserListWithActivityStatus 쿼리 검증
SELECT '=== 검증 2: getUserListWithActivityStatus (del_yn 인덱스 사용 확인) ===' as info;
EXPLAIN SELECT u.no, u.user_nm, u.user_id, u.user_grade 
FROM hnt_user u 
WHERE u.del_yn = 'N' 
ORDER BY u.no DESC 
LIMIT 100;

-- 예상 결과:
-- key: idx_user_del_no
-- type: range (또는 ref)
-- Extra: Using index (또는 Using where만)
-- Using filesort 제거 확인

-- ============================================================================
-- 6. 테이블 크기 및 인덱스 크기 확인
-- ============================================================================

SELECT '=== 테이블 및 인덱스 크기 확인 ===' as info;

SELECT 
    table_name,
    ROUND(((data_length) / 1024 / 1024), 2) AS data_size_mb,
    ROUND(((index_length) / 1024 / 1024), 2) AS index_size_mb,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS total_size_mb,
    table_rows
FROM information_schema.TABLES
WHERE table_schema = 'hnt'
AND table_name IN ('hnt_sensor_info', 'hnt_user')
ORDER BY table_name;

-- ============================================================================
-- 7. 실행 완료
-- ============================================================================

SELECT '✅ Phase 1 인덱스 추가 완료' as status;
SELECT NOW() as end_time;

SELECT '다음 단계:' as info;
SELECT '1. 애플리케이션 재시작 (톰캣2)' as step1;
SELECT '2. 차트 페이지 및 사용자 목록 페이지 테스트' as step2;
SELECT '3. 24시간 성능 모니터링' as step3;
SELECT '4. 1주일 후 Phase 2 실행 (중복 인덱스 정리)' as step4;

-- ============================================================================
-- 롤백 스크립트 (문제 발생 시)
-- ============================================================================
-- 아래 명령어는 주석 처리되어 있습니다.
-- 문제 발생 시 주석 제거 후 실행하세요.

-- ALTER TABLE hnt_sensor_info DROP INDEX idx_sensor_info_sensor_uuid;
-- ALTER TABLE hnt_user DROP INDEX idx_user_del_no;

-- SELECT '⚠️ 롤백 완료 - 인덱스 삭제됨' as status;
-- ============================================================================

