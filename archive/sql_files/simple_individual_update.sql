-- =====================================================
-- 개별 사용자별 간단한 업데이트 쿼리
-- =====================================================

-- 1. 현재 부계정 관계 확인
SELECT 
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

-- 2. 개별 사용자별 업데이트 (실제 사용자 ID로 변경하여 실행)
-- 예시: thepine7 사용자의 경우
UPDATE hnt_user 
SET parent_user_id = 'thepine'
WHERE user_id = 'thepine7'
  AND del_yn = 'N';

-- 3. 다른 부계정이 있다면 개별적으로 실행
-- UPDATE hnt_user 
-- SET parent_user_id = '주계정ID'
-- WHERE user_id = '부계정ID'
--   AND del_yn = 'N';

-- 4. 결과 확인
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
WHERE del_yn = 'N'
ORDER BY 
    CASE WHEN parent_user_id IS NULL THEN 0 ELSE 1 END,
    user_id;
