-- =====================================================
-- hnt_user 테이블에 parent_user_id 컬럼 추가
-- 부계정 관계를 명확하게 관리하기 위한 스키마 변경
-- =====================================================

-- 1. 현재 hnt_user 테이블 구조 확인
DESCRIBE hnt_user;

-- 2. parent_user_id 컬럼 추가
ALTER TABLE hnt_user 
ADD COLUMN parent_user_id VARCHAR(50) NULL COMMENT '부모 사용자 ID (부계정인 경우)';

-- 3. 인덱스 추가 (성능 최적화)
CREATE INDEX idx_hnt_user_parent_user_id ON hnt_user (parent_user_id);

-- 4. 기존 부계정 관계 데이터 마이그레이션
-- hnt_sensor_info 테이블의 관계를 기반으로 parent_user_id 설정
UPDATE hnt_user u
SET parent_user_id = (
    SELECT DISTINCT s.sensor_id 
    FROM hnt_sensor_info s 
    WHERE s.user_id = u.user_id 
      AND s.user_id != s.sensor_id 
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1 
    FROM hnt_sensor_info s 
    WHERE s.user_id = u.user_id 
      AND s.user_id != s.sensor_id
);

-- 5. 마이그레이션 결과 확인
SELECT 
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

-- 6. 부계정 관계 요약
SELECT 
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

-- 7. 부계정 관계 검증
SELECT 
    '부계정 관계 검증' as '검증항목',
    COUNT(*) as '총사용자수',
    COUNT(CASE WHEN parent_user_id IS NULL THEN 1 END) as '주계정수',
    COUNT(CASE WHEN parent_user_id IS NOT NULL THEN 1 END) as '부계정수',
    COUNT(CASE WHEN parent_user_id IS NOT NULL AND user_grade = 'B' THEN 1 END) as 'B등급부계정수'
FROM hnt_user 
WHERE del_yn = 'N';

-- 8. 외래키 제약조건 추가 (선택사항)
-- 주의: 기존 데이터가 정합성을 만족하는지 확인 후 실행
-- ALTER TABLE hnt_user 
-- ADD CONSTRAINT fk_hnt_user_parent_user_id 
-- FOREIGN KEY (parent_user_id) REFERENCES hnt_user(user_id) 
-- ON DELETE SET NULL ON UPDATE CASCADE;

-- 9. 부계정 생성 시 parent_user_id 설정 예시
-- INSERT INTO hnt_user (user_id, user_nm, user_grade, parent_user_id, ...)
-- VALUES ('sub_user_id', '부계정명', 'B', 'parent_user_id', ...);

-- 10. 부계정 여부 확인 쿼리 (새로운 방식)
-- SELECT 
--     user_id,
--     user_nm,
--     CASE 
--         WHEN parent_user_id IS NULL THEN '주계정'
--         ELSE '부계정'
--     END as account_type,
--     parent_user_id
-- FROM hnt_user 
-- WHERE user_id = '사용자ID';
