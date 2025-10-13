-- =====================================================
-- 계정 관계 확인 쿼리 모음
-- =====================================================

-- 1. 전체 사용자 계정 관계 확인
SELECT 
    u1.user_id as '사용자ID',
    u1.user_nm as '사용자명',
    u1.user_grade as '등급',
    u1.login_user_id as '로그인사용자ID',
    CASE 
        WHEN u1.user_id = u1.login_user_id THEN '주계정'
        ELSE '부계정'
    END as '계정유형',
    CASE 
        WHEN u1.user_id = u1.login_user_id THEN u1.user_id
        ELSE u1.login_user_id
    END as '실제센서소유자'
FROM hnt_user u1
ORDER BY 
    CASE WHEN u1.user_id = u1.login_user_id THEN u1.user_id ELSE u1.login_user_id END,
    u1.user_id;

-- 2. thepine 관련 계정들 상세 확인
SELECT 
    u1.user_id as '사용자ID',
    u1.user_nm as '사용자명',
    u1.user_grade as '등급',
    u1.login_user_id as '로그인사용자ID',
    u1.user_email as '이메일',
    u1.user_tel as '전화번호',
    u1.inst_dtm as '생성일시',
    u1.updt_dtm as '수정일시',
    CASE 
        WHEN u1.user_id = u1.login_user_id THEN '주계정'
        ELSE '부계정'
    END as '계정유형'
FROM hnt_user u1
WHERE u1.user_id LIKE '%thepine%' OR u1.login_user_id LIKE '%thepine%'
ORDER BY u1.login_user_id, u1.user_id;

-- 3. 주계정별 부계정 목록 확인
SELECT 
    main_user.user_id as '주계정ID',
    main_user.user_nm as '주계정명',
    main_user.user_grade as '주계정등급',
    COUNT(sub_user.user_id) as '부계정수',
    GROUP_CONCAT(
        CONCAT(sub_user.user_id, '(', sub_user.user_nm, ')')
        ORDER BY sub_user.user_id
        SEPARATOR ', '
    ) as '부계정목록'
FROM hnt_user main_user
LEFT JOIN hnt_user sub_user ON main_user.user_id = sub_user.login_user_id
WHERE main_user.user_id = main_user.login_user_id  -- 주계정만
GROUP BY main_user.user_id, main_user.user_nm, main_user.user_grade
ORDER BY main_user.user_id;

-- 4. 센서 소유권 확인 (주계정 vs 부계정)
SELECT 
    s.user_id as '센서소유자ID',
    s.sensor_id as '센서ID',
    s.sensor_uuid as '센서UUID',
    s.sensor_name as '센서명',
    s.sensor_loc as '센서위치',
    s.sensor_type as '센서타입',
    u.user_nm as '소유자명',
    u.user_grade as '소유자등급',
    CASE 
        WHEN u.user_id = u.login_user_id THEN '주계정소유'
        ELSE '부계정소유'
    END as '소유유형'
FROM hnt_sensor_info s
LEFT JOIN hnt_user u ON s.user_id = u.user_id
ORDER BY s.user_id, s.sensor_uuid;

-- 5. thepine 계정의 센서와 부계정 관계 확인
SELECT 
    s.user_id as '센서소유자',
    s.sensor_uuid as '센서UUID',
    s.sensor_name as '센서명',
    s.sensor_loc as '센서위치',
    u_owner.user_nm as '소유자명',
    u_owner.user_grade as '소유자등급',
    u_sub.user_id as '부계정ID',
    u_sub.user_nm as '부계정명',
    u_sub.user_grade as '부계정등급'
FROM hnt_sensor_info s
LEFT JOIN hnt_user u_owner ON s.user_id = u_owner.user_id
LEFT JOIN hnt_user u_sub ON s.user_id = u_sub.login_user_id
WHERE s.user_id = 'thepine' OR u_sub.user_id = 'thepine7'
ORDER BY s.sensor_uuid, u_sub.user_id;

-- 6. 부계정이 접근 가능한 센서 목록 확인
SELECT 
    sub_user.user_id as '부계정ID',
    sub_user.user_nm as '부계정명',
    sub_user.user_grade as '부계정등급',
    main_user.user_id as '주계정ID',
    main_user.user_nm as '주계정명',
    s.sensor_uuid as '접근가능센서UUID',
    s.sensor_name as '센서명',
    s.sensor_loc as '센서위치'
FROM hnt_user sub_user
LEFT JOIN hnt_user main_user ON sub_user.login_user_id = main_user.user_id
LEFT JOIN hnt_sensor_info s ON main_user.user_id = s.user_id
WHERE sub_user.user_id != sub_user.login_user_id  -- 부계정만
ORDER BY sub_user.user_id, s.sensor_uuid;

-- 7. 특정 사용자(thepine7)의 계정 정보 상세 확인
SELECT 
    'thepine7' as '확인대상',
    u.user_id,
    u.user_nm,
    u.user_grade,
    u.login_user_id,
    u.user_email,
    u.user_tel,
    u.inst_dtm,
    u.updt_dtm,
    CASE 
        WHEN u.user_id = u.login_user_id THEN '주계정'
        ELSE '부계정'
    END as '계정유형',
    CASE 
        WHEN u.user_id = u.login_user_id THEN '자신의 센서 사용'
        ELSE CONCAT('주계정(', u.login_user_id, ')의 센서 공유 사용')
    END as '센서사용방식'
FROM hnt_user u
WHERE u.user_id = 'thepine7';
