-- =====================================================
-- 부계정 관리 쿼리 모음 (parent_user_id 컬럼 사용)
-- =====================================================

-- 1. 부계정 생성 (parent_user_id 사용)
-- INSERT INTO hnt_user (
--     user_id, user_nm, user_grade, parent_user_id, 
--     user_email, user_tel, del_yn, inst_dtm
-- ) VALUES (
--     'sub_user_id', '부계정명', 'B', 'parent_user_id',
--     'email@example.com', '010-1234-5678', 'N', NOW()
-- );

-- 2. 부계정 여부 확인 (간단한 방식)
SELECT 
    user_id as '사용자ID',
    user_nm as '사용자명',
    user_grade as '등급',
    parent_user_id as '부모사용자ID',
    CASE 
        WHEN parent_user_id IS NULL THEN '주계정'
        ELSE '부계정'
    END as '계정유형'
FROM hnt_user 
WHERE user_id = '사용자ID'
  AND del_yn = 'N';

-- 3. 특정 사용자의 모든 부계정 조회
SELECT 
    user_id as '부계정ID',
    user_nm as '부계정명',
    user_grade as '등급',
    user_email as '이메일',
    user_tel as '전화번호',
    inst_dtm as '생성일시'
FROM hnt_user 
WHERE parent_user_id = '주계정ID'
  AND del_yn = 'N'
ORDER BY inst_dtm DESC;

-- 4. 부계정의 주계정 정보 조회
SELECT 
    sub_user.user_id as '부계정ID',
    sub_user.user_nm as '부계정명',
    parent_user.user_id as '주계정ID',
    parent_user.user_nm as '주계정명',
    parent_user.user_grade as '주계정등급'
FROM hnt_user sub_user
INNER JOIN hnt_user parent_user ON sub_user.parent_user_id = parent_user.user_id
WHERE sub_user.user_id = '부계정ID'
  AND sub_user.del_yn = 'N'
  AND parent_user.del_yn = 'N';

-- 5. 부계정이 접근 가능한 센서 목록 조회
SELECT 
    sub_user.user_id as '부계정ID',
    sub_user.user_nm as '부계정명',
    s.sensor_uuid as '센서UUID',
    s.sensor_name as '센서명',
    s.sensor_loc as '센서위치',
    s.sensor_type as '센서타입',
    parent_user.user_id as '주계정ID',
    parent_user.user_nm as '주계정명'
FROM hnt_user sub_user
INNER JOIN hnt_user parent_user ON sub_user.parent_user_id = parent_user.user_id
INNER JOIN hnt_sensor_info s ON parent_user.user_id = s.sensor_id
WHERE sub_user.user_id = '부계정ID'
  AND sub_user.del_yn = 'N'
  AND parent_user.del_yn = 'N'
ORDER BY s.sensor_uuid;

-- 6. 주계정별 부계정 통계
SELECT 
    parent_user.user_id as '주계정ID',
    parent_user.user_nm as '주계정명',
    parent_user.user_grade as '주계정등급',
    COUNT(sub_user.user_id) as '부계정수',
    COUNT(s.sensor_uuid) as '센서수',
    GROUP_CONCAT(
        DISTINCT CONCAT(sub_user.user_id, '(', sub_user.user_nm, ')')
        ORDER BY sub_user.user_id
        SEPARATOR ', '
    ) as '부계정목록'
FROM hnt_user parent_user
LEFT JOIN hnt_user sub_user ON parent_user.user_id = sub_user.parent_user_id AND sub_user.del_yn = 'N'
LEFT JOIN hnt_sensor_info s ON parent_user.user_id = s.sensor_id AND s.user_id = s.sensor_id
WHERE parent_user.del_yn = 'N'
GROUP BY parent_user.user_id, parent_user.user_nm, parent_user.user_grade
HAVING 부계정수 > 0 OR 센서수 > 0
ORDER BY 부계정수 DESC, 센서수 DESC;

-- 7. 부계정 권한 확인 (센서 접근 권한)
SELECT 
    sub_user.user_id as '부계정ID',
    sub_user.user_nm as '부계정명',
    sub_user.user_grade as '부계정등급',
    parent_user.user_id as '주계정ID',
    parent_user.user_nm as '주계정명',
    COUNT(s.sensor_uuid) as '접근가능센서수',
    GROUP_CONCAT(
        CONCAT(s.sensor_name, '(', s.sensor_uuid, ')')
        ORDER BY s.sensor_uuid
        SEPARATOR ', '
    ) as '접근가능센서목록'
FROM hnt_user sub_user
INNER JOIN hnt_user parent_user ON sub_user.parent_user_id = parent_user.user_id
LEFT JOIN hnt_sensor_info s ON parent_user.user_id = s.sensor_id AND s.user_id = s.sensor_id
WHERE sub_user.del_yn = 'N'
  AND parent_user.del_yn = 'N'
GROUP BY sub_user.user_id, sub_user.user_nm, sub_user.user_grade, 
         parent_user.user_id, parent_user.user_nm
ORDER BY sub_user.user_id;

-- 8. 부계정 생성 시 센서 접근 권한 자동 설정
-- 부계정 생성 후 hnt_sensor_info 테이블에 부계정이 주계정의 센서에 접근할 수 있도록 설정
-- INSERT INTO hnt_sensor_info (
--     user_id, sensor_id, sensor_uuid, sensor_name, sensor_loc, sensor_type, sensor_gu, chart_type, inst_dtm
-- )
-- SELECT 
--     '부계정ID' as user_id,
--     s.sensor_id,
--     s.sensor_uuid,
--     s.sensor_name,
--     s.sensor_loc,
--     s.sensor_type,
--     s.sensor_gu,
--     s.chart_type,
--     NOW() as inst_dtm
-- FROM hnt_sensor_info s
-- WHERE s.sensor_id = '주계정ID'
--   AND s.user_id = s.sensor_id;

-- 9. 부계정 삭제 시 관련 데이터 정리
-- DELETE FROM hnt_sensor_info WHERE user_id = '부계정ID';
-- UPDATE hnt_user SET del_yn = 'Y', updt_dtm = NOW() WHERE user_id = '부계정ID';

-- 10. 부계정 관계 변경 (부모 변경)
-- UPDATE hnt_user 
-- SET parent_user_id = '새로운주계정ID', updt_dtm = NOW()
-- WHERE user_id = '부계정ID';

-- 11. 부계정 등급 변경 (B → U 또는 A)
-- UPDATE hnt_user 
-- SET user_grade = 'U', updt_dtm = NOW()
-- WHERE user_id = '부계정ID';

-- 12. 부계정 관계 검증 (데이터 정합성 확인)
SELECT 
    '부계정 관계 검증' as '검증항목',
    COUNT(*) as '총사용자수',
    COUNT(CASE WHEN parent_user_id IS NULL THEN 1 END) as '주계정수',
    COUNT(CASE WHEN parent_user_id IS NOT NULL THEN 1 END) as '부계정수',
    COUNT(CASE WHEN parent_user_id IS NOT NULL AND user_grade = 'B' THEN 1 END) as 'B등급부계정수',
    COUNT(CASE WHEN parent_user_id IS NOT NULL AND user_grade != 'B' THEN 1 END) as '비B등급부계정수'
FROM hnt_user 
WHERE del_yn = 'N';
