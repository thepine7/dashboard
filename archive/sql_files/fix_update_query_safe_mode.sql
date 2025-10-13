-- =====================================================
-- Safe Update Mode 문제 해결 쿼리
-- =====================================================

-- 1. Safe Update Mode 비활성화
SET SQL_SAFE_UPDATES = 0;

-- 2. 기존 부계정 관계 데이터 마이그레이션 (수정된 버전)
-- 더 안전한 방법으로 개별 사용자별 업데이트
UPDATE hnt_user 
SET parent_user_id = (
    SELECT DISTINCT s.sensor_id 
    FROM hnt_sensor_info s 
    WHERE s.user_id = hnt_user.user_id 
      AND s.user_id != s.sensor_id 
    LIMIT 1
)
WHERE user_id IN (
    SELECT DISTINCT s.user_id 
    FROM hnt_sensor_info s 
    WHERE s.user_id != s.sensor_id
      AND s.user_id IS NOT NULL
)
AND del_yn = 'N';

-- 3. Safe Update Mode 재활성화
SET SQL_SAFE_UPDATES = 1;

-- 4. 마이그레이션 결과 확인
SELECT 
    '마이그레이션 결과 확인' as '단계',
    user_id as '사용자ID',
    user_nm as '사용자명',
    user_grade as '등급',
    parent_user_id as '부모사용자ID',
    CASE 
        WHEN parent_user_id IS NULL THEN '주계정'
        ELSE '부계정'
    END as '계정유형',
    inst_dtm as '생성일시'
FROM hnt_user 
WHERE del_yn = 'N'
ORDER BY 
    CASE WHEN parent_user_id IS NULL THEN 0 ELSE 1 END,
    user_id;

-- 5. 부계정 관계 요약
SELECT 
    '부계정 관계 요약' as '단계',
    parent_user_id as '주계정ID',
    COUNT(*) as '부계정수',
    GROUP_CONCAT(
        CONCAT(user_id, '(', user_nm, ')')
        ORDER BY user_id
        SEPARATOR ', '
    ) as '부계정목록'
FROM hnt_user 
WHERE parent_user_id IS NOT NULL
  AND del_yn = 'N'
GROUP BY parent_user_id
ORDER BY 부계정수 DESC;

-- 6. 부계정 관계 검증
SELECT 
    '부계정 관계 검증' as '단계',
    COUNT(*) as '총사용자수',
    COUNT(CASE WHEN parent_user_id IS NULL THEN 1 END) as '주계정수',
    COUNT(CASE WHEN parent_user_id IS NOT NULL THEN 1 END) as '부계정수',
    COUNT(CASE WHEN parent_user_id IS NOT NULL AND user_grade = 'B' THEN 1 END) as 'B등급부계정수'
FROM hnt_user 
WHERE del_yn = 'N';
