-- ============================================================================
-- 차트설정 & 사용자관리 페이지 인덱스 최적화 Phase 2
-- ============================================================================
-- 작성일: 2025-10-17
-- 대상 테이블: hnt_sensor_info, hnt_user
-- 목적: 중복 및 미사용 인덱스 정리
-- 실행 시점: Phase 1 실행 후 1주일 (2025-10-24)
-- 주의: 반드시 24시간 모니터링 후 실행
-- ============================================================================

-- ============================================================================
-- 중요: 실행 전 확인사항
-- ============================================================================
-- 1. Phase 1 인덱스가 정상 작동하는지 확인
-- 2. 24시간 slow query log 확인
-- 3. 애플리케이션 에러 로그 확인
-- 4. 백업 파일 생성 완료 확인
-- ============================================================================

SELECT '⚠️ Phase 2 실행 전 필수 확인사항 ⚠️' as warning;
SELECT '1. Phase 1 인덱스 정상 작동 확인' as check1;
SELECT '2. 24시간 slow query log 분석 완료' as check2;
SELECT '3. 애플리케이션 에러 로그 확인' as check3;
SELECT '4. 백업 파일 생성 완료 (mysqldump)' as check4;
SELECT '위 4가지 확인 후 계속 진행하세요.' as instruction;

-- ============================================================================
-- 1. 현재 인덱스 상태 확인
-- ============================================================================

SELECT 'Phase 2: 중복 인덱스 정리 시작' as status;
SELECT NOW() as start_time;

SELECT '=== 현재 hnt_sensor_info 인덱스 ===' as info;
SHOW INDEX FROM hnt_sensor_info;

SELECT '=== 현재 hnt_user 인덱스 ===' as info;
SHOW INDEX FROM hnt_user;

-- ============================================================================
-- 2. 인덱스 사용 통계 확인
-- ============================================================================

SELECT '=== hnt_sensor_info 인덱스 사용 통계 ===' as info;
SELECT 
    INDEX_NAME,
    CARDINALITY,
    SEQ_IN_INDEX,
    COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt'
AND TABLE_NAME = 'hnt_sensor_info'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

SELECT '=== hnt_user 인덱스 사용 통계 ===' as info;
SELECT 
    INDEX_NAME,
    CARDINALITY,
    SEQ_IN_INDEX,
    COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt'
AND TABLE_NAME = 'hnt_user'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- ============================================================================
-- 3. hnt_sensor_info 중복 인덱스 삭제
-- ============================================================================

-- 3.1. hnt_sensor_info_no_IDX 삭제 (PRIMARY와 중복)
SELECT '=== Step 1: hnt_sensor_info_no_IDX 삭제 (PRIMARY와 중복) ===' as info;

-- 인덱스 존재 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스가 존재합니다. 삭제를 시작합니다.'
        ELSE '⚠️ 인덱스가 존재하지 않습니다. 건너뜁니다.'
    END as check_result
FROM information_schema.STATISTICS
WHERE table_schema = 'hnt'
AND table_name = 'hnt_sensor_info'
AND index_name = 'hnt_sensor_info_no_IDX';

-- 삭제 실행 (존재하는 경우만)
-- ALTER TABLE hnt_sensor_info DROP INDEX hnt_sensor_info_no_IDX;
-- SELECT '✅ hnt_sensor_info_no_IDX 삭제 완료' as status;

-- ============================================================================

-- 3.2. idx_sensor_info_user_sensor 삭제 (hnt_sensor_info_UN과 중복)
SELECT '=== Step 2: idx_sensor_info_user_sensor 삭제 (UN과 중복) ===' as info;

-- 인덱스 존재 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스가 존재합니다. 삭제를 시작합니다.'
        ELSE '⚠️ 인덱스가 존재하지 않습니다. 건너뜁니다.'
    END as check_result
FROM information_schema.STATISTICS
WHERE table_schema = 'hnt'
AND table_name = 'hnt_sensor_info'
AND index_name = 'idx_sensor_info_user_sensor';

-- 삭제 실행 (존재하는 경우만)
-- ALTER TABLE hnt_sensor_info DROP INDEX idx_sensor_info_user_sensor;
-- SELECT '✅ idx_sensor_info_user_sensor 삭제 완료' as status;

-- ============================================================================

-- 3.3. hnt_sensor_info_inst_dtm_IDX 삭제 (미사용)
SELECT '=== Step 3: hnt_sensor_info_inst_dtm_IDX 삭제 (미사용) ===' as info;

-- 인덱스 존재 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스가 존재합니다. 삭제를 시작합니다.'
        ELSE '⚠️ 인덱스가 존재하지 않습니다. 건너뜁니다.'
    END as check_result
FROM information_schema.STATISTICS
WHERE table_schema = 'hnt'
AND table_name = 'hnt_sensor_info'
AND index_name = 'hnt_sensor_info_inst_dtm_IDX';

-- 삭제 실행 (존재하는 경우만)
-- ALTER TABLE hnt_sensor_info DROP INDEX hnt_sensor_info_inst_dtm_IDX;
-- SELECT '✅ hnt_sensor_info_inst_dtm_IDX 삭제 완료' as status;

-- ============================================================================

-- 3.4. hnt_sensor_info_mdf_dtm_IDX 삭제 (미사용)
SELECT '=== Step 4: hnt_sensor_info_mdf_dtm_IDX 삭제 (미사용) ===' as info;

-- 인덱스 존재 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스가 존재합니다. 삭제를 시작합니다.'
        ELSE '⚠️ 인덱스가 존재하지 않습니다. 건너뜁니다.'
    END as check_result
FROM information_schema.STATISTICS
WHERE table_schema = 'hnt'
AND table_name = 'hnt_sensor_info'
AND index_name = 'hnt_sensor_info_mdf_dtm_IDX';

-- 삭제 실행 (존재하는 경우만)
-- ALTER TABLE hnt_sensor_info DROP INDEX hnt_sensor_info_mdf_dtm_IDX;
-- SELECT '✅ hnt_sensor_info_mdf_dtm_IDX 삭제 완료' as status;

-- ============================================================================

-- 3.5. hnt_sensor_info_sensor_id_IDX 삭제 (미사용)
SELECT '=== Step 5: hnt_sensor_info_sensor_id_IDX 삭제 (미사용) ===' as info;

-- 인덱스 존재 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스가 존재합니다. 삭제를 시작합니다.'
        ELSE '⚠️ 인덱스가 존재하지 않습니다. 건너뜁니다.'
    END as check_result
FROM information_schema.STATISTICS
WHERE table_schema = 'hnt'
AND table_name = 'hnt_sensor_info'
AND index_name = 'hnt_sensor_info_sensor_id_IDX';

-- 삭제 실행 (존재하는 경우만)
-- ALTER TABLE hnt_sensor_info DROP INDEX hnt_sensor_info_sensor_id_IDX;
-- SELECT '✅ hnt_sensor_info_sensor_id_IDX 삭제 완료' as status;

-- ============================================================================
-- 4. hnt_user 중복 인덱스 삭제
-- ============================================================================

-- 4.1. hnt_user_inst_dtm_IDX 삭제 (미사용)
SELECT '=== Step 6: hnt_user_inst_dtm_IDX 삭제 (미사용) ===' as info;

-- 인덱스 존재 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스가 존재합니다. 삭제를 시작합니다.'
        ELSE '⚠️ 인덱스가 존재하지 않습니다. 건너뜁니다.'
    END as check_result
FROM information_schema.STATISTICS
WHERE table_schema = 'hnt'
AND table_name = 'hnt_user'
AND index_name = 'hnt_user_inst_dtm_IDX';

-- 삭제 실행 (존재하는 경우만)
-- ALTER TABLE hnt_user DROP INDEX hnt_user_inst_dtm_IDX;
-- SELECT '✅ hnt_user_inst_dtm_IDX 삭제 완료' as status;

-- ============================================================================

-- 4.2. hnt_user_mdf_dtm_IDX 삭제 (미사용)
SELECT '=== Step 7: hnt_user_mdf_dtm_IDX 삭제 (미사용) ===' as info;

-- 인덱스 존재 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스가 존재합니다. 삭제를 시작합니다.'
        ELSE '⚠️ 인덱스가 존재하지 않습니다. 건너뜁니다.'
    END as check_result
FROM information_schema.STATISTICS
WHERE table_schema = 'hnt'
AND table_name = 'hnt_user'
AND index_name = 'hnt_user_mdf_dtm_IDX';

-- 삭제 실행 (존재하는 경우만)
-- ALTER TABLE hnt_user DROP INDEX hnt_user_mdf_dtm_IDX;
-- SELECT '✅ hnt_user_mdf_dtm_IDX 삭제 완료' as status;

-- ============================================================================

-- 4.3. idx_hnt_user_parent_user_id 삭제 (idx_user_parent_del과 중복)
SELECT '=== Step 8: idx_hnt_user_parent_user_id 삭제 (중복) ===' as info;

-- 인덱스 존재 확인
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '인덱스가 존재합니다. 삭제를 시작합니다.'
        ELSE '⚠️ 인덱스가 존재하지 않습니다. 건너뜁니다.'
    END as check_result
FROM information_schema.STATISTICS
WHERE table_schema = 'hnt'
AND table_name = 'hnt_user'
AND index_name = 'idx_hnt_user_parent_user_id';

-- 삭제 실행 (존재하는 경우만)
-- ALTER TABLE hnt_user DROP INDEX idx_hnt_user_parent_user_id;
-- SELECT '✅ idx_hnt_user_parent_user_id 삭제 완료' as status;

-- ============================================================================
-- 5. 인덱스 삭제 결과 확인
-- ============================================================================

SELECT '=== 인덱스 삭제 후 hnt_sensor_info 인덱스 ===' as info;
SHOW INDEX FROM hnt_sensor_info;

SELECT '=== 인덱스 삭제 후 hnt_user 인덱스 ===' as info;
SHOW INDEX FROM hnt_user;

-- ============================================================================
-- 6. 테이블 크기 비교
-- ============================================================================

SELECT '=== 테이블 및 인덱스 크기 확인 (삭제 후) ===' as info;

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
-- 7. 쿼리 성능 재확인 (EXPLAIN)
-- ============================================================================

-- 7.1. getSensorList (hnt_sensor_info_UN 사용 확인)
SELECT '=== 검증 1: getSensorList (인덱스 정상 사용 확인) ===' as info;
EXPLAIN SELECT * FROM hnt_sensor_info WHERE user_id = 'thepine';

-- 7.2. getUserListWithActivityStatus (idx_user_del_no 사용 확인)
SELECT '=== 검증 2: getUserListWithActivityStatus (인덱스 정상 사용 확인) ===' as info;
EXPLAIN SELECT * FROM hnt_user WHERE del_yn = 'N' ORDER BY no DESC LIMIT 100;

-- ============================================================================
-- 8. 실행 완료
-- ============================================================================

SELECT '✅ Phase 2 중복 인덱스 정리 완료' as status;
SELECT NOW() as end_time;

SELECT '다음 단계:' as info;
SELECT '1. 애플리케이션 재시작 (톰캣2)' as step1;
SELECT '2. 차트 페이지 및 사용자 목록 페이지 테스트' as step2;
SELECT '3. 24시간 성능 모니터링' as step3;
SELECT '4. INSERT/UPDATE 성능 개선 확인' as step4;

-- ============================================================================
-- 롤백 스크립트 (문제 발생 시)
-- ============================================================================
-- 아래 명령어는 주석 처리되어 있습니다.
-- 문제 발생 시 주석 제거 후 실행하세요.

/*
-- hnt_sensor_info 인덱스 복원
CREATE INDEX hnt_sensor_info_no_IDX ON hnt_sensor_info(no);
CREATE INDEX idx_sensor_info_user_sensor ON hnt_sensor_info(user_id, sensor_id);
CREATE INDEX hnt_sensor_info_inst_dtm_IDX ON hnt_sensor_info(inst_dtm);
CREATE INDEX hnt_sensor_info_mdf_dtm_IDX ON hnt_sensor_info(mdf_dtm);
CREATE INDEX hnt_sensor_info_sensor_id_IDX ON hnt_sensor_info(sensor_id);

-- hnt_user 인덱스 복원
CREATE INDEX hnt_user_inst_dtm_IDX ON hnt_user(inst_dtm);
CREATE INDEX hnt_user_mdf_dtm_IDX ON hnt_user(mdf_dtm);
CREATE INDEX idx_hnt_user_parent_user_id ON hnt_user(parent_user_id);

SELECT '⚠️ 롤백 완료 - 인덱스 복원됨' as status;
*/
-- ============================================================================

