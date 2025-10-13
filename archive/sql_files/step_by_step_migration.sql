-- =====================================================
-- 단계별 안전한 마이그레이션 쿼리
-- =====================================================

-- 1단계: 현재 부계정 관계 확인
SELECT 
    '1단계: 현재 부계정 관계 확인' as '단계',
    s.user_id as '부계정ID',
    s.sensor_id as '주계정ID',
    u1.user_nm as '부계정명',
    u2.user_nm as '주계정명'
FROM hnt_sensor_info s
INNER JOIN hnt_user u1 ON s.user_id = u1.user_id
INNER JOIN hnt_user u2 ON s.sensor_id = u2.user_id
WHERE s.user_id != s.sensor_id
  AND u1.del_yn = 'N'
  AND u2.del_yn = 'N'
ORDER BY s.sensor_id, s.user_id;

-- 2단계: 마이그레이션 대상 사용자 확인
SELECT 
    '2단계: 마이그레이션 대상' as '단계',
    u.user_id as '사용자ID',
    u.user_nm as '사용자명',
    u.user_grade as '등급',
    s.sensor_id as '예상주계정ID',
    u2.user_nm as '예상주계정명'
FROM hnt_user u
INNER JOIN hnt_sensor_info s ON u.user_id = s.user_id AND s.user_id != s.sensor_id
INNER JOIN hnt_user u2 ON s.sensor_id = u2.user_id
WHERE u.del_yn = 'N'
  AND u2.del_yn = 'N'
ORDER BY s.sensor_id, u.user_id;

-- 3단계: Safe Update Mode 비활성화
SET SQL_SAFE_UPDATES = 0;

-- 4단계: 개별 사용자별 마이그레이션 (예시 - thepine7)
-- 실제 사용자 ID로 변경하여 실행
UPDATE hnt_user 
SET parent_user_id = 'thepine'
WHERE user_id = 'thepine7'
  AND del_yn = 'N';

-- 5단계: 다른 부계정이 있다면 개별적으로 실행
-- UPDATE hnt_user 
-- SET parent_user_id = '주계정ID'
-- WHERE user_id = '부계정ID'
--   AND del_yn = 'N';

-- 6단계: Safe Update Mode 재활성화
SET SQL_SAFE_UPDATES = 1;

-- 7단계: 마이그레이션 결과 확인
SELECT 
    '7단계: 마이그레이션 결과' as '단계',
    user_id as '사용자ID',
    user_nm as '사용자명',
    user_grade as '등급',
    parent_user_id as '부모사용자ID',
    CASE 
        WHEN parent_user_id IS NULL THEN '주계정'
        ELSE '부계정'
    END as '계정유형'
FROM hnt_user 
WHERE del_yn = 'N'
ORDER BY 
    CASE WHEN parent_user_id IS NULL THEN 0 ELSE 1 END,
    user_id;

-- 8단계: 부계정 관계 검증
SELECT 
    '8단계: 부계정 관계 검증' as '단계',
    COUNT(*) as '총사용자수',
    COUNT(CASE WHEN parent_user_id IS NULL THEN 1 END) as '주계정수',
    COUNT(CASE WHEN parent_user_id IS NOT NULL THEN 1 END) as '부계정수',
    COUNT(CASE WHEN parent_user_id IS NOT NULL AND user_grade = 'B' THEN 1 END) as 'B등급부계정수'
FROM hnt_user 
WHERE del_yn = 'N';

-- 9단계: 부계정별 센서 접근 권한 확인
SELECT 
    '9단계: 부계정 센서 접근 권한' as '단계',
    sub_user.user_id as '부계정ID',
    sub_user.user_nm as '부계정명',
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
GROUP BY sub_user.user_id, sub_user.user_nm, parent_user.user_id, parent_user.user_nm
ORDER BY sub_user.user_id;
