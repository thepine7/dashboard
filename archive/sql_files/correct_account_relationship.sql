-- =====================================================
-- 올바른 계정 관계 확인 쿼리 (실제 DB 구조 기반)
-- =====================================================

-- 1. 전체 사용자 계정 관계 확인 (sensor_id 기반)
SELECT 
    u.user_id as '사용자ID',
    u.user_nm as '사용자명',
    u.user_grade as '등급',
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id = u.user_id
        ) THEN '주계정'
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id != u.user_id
        ) THEN '부계정'
        ELSE '센서없음'
    END as '계정유형',
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id = u.user_id
        ) THEN u.user_id
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id != u.user_id
        ) THEN (
            SELECT DISTINCT s2.sensor_id 
            FROM hnt_sensor_info s2 
            WHERE s2.user_id = u.user_id 
            LIMIT 1
        )
        ELSE 'N/A'
    END as '실제센서소유자'
FROM hnt_user u
WHERE u.del_yn = 'N'
ORDER BY 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id = u.user_id
        ) THEN u.user_id
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id != u.user_id
        ) THEN (
            SELECT DISTINCT s2.sensor_id 
            FROM hnt_sensor_info s2 
            WHERE s2.user_id = u.user_id 
            LIMIT 1
        )
        ELSE u.user_id
    END,
    u.user_id;

-- 2. thepine 관련 계정들 상세 확인
SELECT 
    u.user_id as '사용자ID',
    u.user_nm as '사용자명',
    u.user_grade as '등급',
    u.user_email as '이메일',
    u.user_tel as '전화번호',
    u.inst_dtm as '생성일시',
    u.updt_dtm as '수정일시',
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id = u.user_id
        ) THEN '주계정'
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id != u.user_id
        ) THEN '부계정'
        ELSE '센서없음'
    END as '계정유형',
    (
        SELECT DISTINCT s.sensor_id 
        FROM hnt_sensor_info s 
        WHERE s.user_id = u.user_id 
        LIMIT 1
    ) as '센서소유자ID'
FROM hnt_user u
WHERE (u.user_id LIKE '%thepine%' OR EXISTS (
    SELECT 1 FROM hnt_sensor_info s 
    WHERE s.sensor_id LIKE '%thepine%' AND s.user_id = u.user_id
))
AND u.del_yn = 'N'
ORDER BY u.user_id;

-- 3. 주계정별 부계정 목록 확인 (sensor_id 기반)
SELECT 
    main_user.user_id as '주계정ID',
    main_user.user_nm as '주계정명',
    main_user.user_grade as '주계정등급',
    COUNT(DISTINCT sub_user.user_id) as '부계정수',
    GROUP_CONCAT(
        DISTINCT CONCAT(sub_user.user_id, '(', sub_user.user_nm, ')')
        ORDER BY sub_user.user_id
        SEPARATOR ', '
    ) as '부계정목록'
FROM hnt_user main_user
LEFT JOIN hnt_sensor_info s ON main_user.user_id = s.sensor_id
LEFT JOIN hnt_user sub_user ON s.user_id = sub_user.user_id AND sub_user.user_id != main_user.user_id
WHERE main_user.del_yn = 'N'
  AND EXISTS (
      SELECT 1 FROM hnt_sensor_info s2 
      WHERE s2.user_id = main_user.user_id AND s2.sensor_id = main_user.user_id
  )  -- 주계정만
GROUP BY main_user.user_id, main_user.user_nm, main_user.user_grade
ORDER BY main_user.user_id;

-- 4. 센서 소유권 확인 (주계정 vs 부계정)
SELECT 
    s.user_id as '센서사용자ID',
    s.sensor_id as '센서소유자ID',
    s.sensor_uuid as '센서UUID',
    s.sensor_name as '센서명',
    s.sensor_loc as '센서위치',
    s.sensor_type as '센서타입',
    u_user.user_nm as '사용자명',
    u_user.user_grade as '사용자등급',
    u_owner.user_nm as '소유자명',
    u_owner.user_grade as '소유자등급',
    CASE 
        WHEN s.user_id = s.sensor_id THEN '주계정소유'
        ELSE '부계정사용'
    END as '소유유형'
FROM hnt_sensor_info s
LEFT JOIN hnt_user u_user ON s.user_id = u_user.user_id
LEFT JOIN hnt_user u_owner ON s.sensor_id = u_owner.user_id
ORDER BY s.sensor_id, s.user_id, s.sensor_uuid;

-- 5. 부계정이 접근 가능한 센서 목록 확인
SELECT 
    sub_user.user_id as '부계정ID',
    sub_user.user_nm as '부계정명',
    sub_user.user_grade as '부계정등급',
    main_user.user_id as '주계정ID',
    main_user.user_nm as '주계정명',
    s.sensor_uuid as '접근가능센서UUID',
    s.sensor_name as '센서명',
    s.sensor_loc as '센서위치'
FROM hnt_sensor_info s
LEFT JOIN hnt_user sub_user ON s.user_id = sub_user.user_id
LEFT JOIN hnt_user main_user ON s.sensor_id = main_user.user_id
WHERE s.user_id != s.sensor_id  -- 부계정만
  AND sub_user.del_yn = 'N'
ORDER BY sub_user.user_id, s.sensor_uuid;

-- 6. 특정 사용자(thepine7)의 계정 정보 상세 확인
SELECT 
    'thepine7' as '확인대상',
    u.user_id,
    u.user_nm,
    u.user_grade,
    u.user_email,
    u.user_tel,
    u.inst_dtm,
    u.updt_dtm,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id = u.user_id
        ) THEN '주계정'
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id != u.user_id
        ) THEN '부계정'
        ELSE '센서없음'
    END as '계정유형',
    (
        SELECT DISTINCT s.sensor_id 
        FROM hnt_sensor_info s 
        WHERE s.user_id = u.user_id 
        LIMIT 1
    ) as '센서소유자ID',
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id = u.user_id
        ) THEN '자신의 센서 사용'
        WHEN EXISTS (
            SELECT 1 FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id AND s.sensor_id != u.user_id
        ) THEN CONCAT('주계정(', (
            SELECT DISTINCT s.sensor_id 
            FROM hnt_sensor_info s 
            WHERE s.user_id = u.user_id 
            LIMIT 1
        ), ')의 센서 공유 사용')
        ELSE '센서 없음'
    END as '센서사용방식'
FROM hnt_user u
WHERE u.user_id = 'thepine7';
