-- =====================================================
-- hnt_user 테이블 구조 확인
-- =====================================================

-- 1. hnt_user 테이블의 컬럼 구조 확인
DESCRIBE hnt_user;

-- 2. hnt_user 테이블의 모든 컬럼 정보 확인
SELECT 
    COLUMN_NAME as '컬럼명',
    DATA_TYPE as '데이터타입',
    IS_NULLABLE as 'NULL허용',
    COLUMN_DEFAULT as '기본값',
    COLUMN_COMMENT as '주석'
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_user'
ORDER BY ORDINAL_POSITION;

-- 3. hnt_user 테이블의 실제 데이터 샘플 확인
SELECT * FROM hnt_user LIMIT 5;

-- 4. thepine 관련 사용자 데이터 확인
SELECT * FROM hnt_user WHERE user_id LIKE '%thepine%';

-- 5. 부계정 관련 컬럼이 있는지 확인
SHOW COLUMNS FROM hnt_user LIKE '%login%';
SHOW COLUMNS FROM hnt_user LIKE '%parent%';
SHOW COLUMNS FROM hnt_user LIKE '%main%';
SHOW COLUMNS FROM hnt_user LIKE '%sub%';
