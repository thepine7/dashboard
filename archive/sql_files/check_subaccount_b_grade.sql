-- =====================================================
-- 부계정 B 등급 계정 확인 쿼리
-- =====================================================

-- 1. B 등급 사용자 목록 (등급 기반)
SELECT 
    user_id as '사용자ID',
    user_nm as '사용자명',
    user_grade as '등급',
    user_email as '이메일',
    user_tel as '전화번호',
    inst_dtm as '생성일시',
    updt_dtm as '수정일시',
    CASE 
        WHEN del_yn = 'Y' THEN '삭제됨'
        WHEN del_yn = 'N' THEN '활성'
        ELSE del_yn
    END as '상태'
FROM hnt_user 
WHERE user_grade = 'B'
ORDER BY inst_dtm DESC;

-- 2. B 등급 사용자 수 통계
SELECT 
    COUNT(*) as 'B등급_총수',
    COUNT(CASE WHEN del_yn = 'N' THEN 1 END) as 'B등급_활성수',
    COUNT(CASE WHEN del_yn = 'Y' THEN 1 END) as 'B등급_삭제수'
FROM hnt_user 
WHERE user_grade = 'B';

-- 3. B 등급 사용자와 센서 관계 확인
SELECT 
    u.user_id as 'B등급사용자ID',
    u.user_nm as 'B등급사용자명',
    u.user_grade as '등급',
    s.sensor_id as '센서소유자ID',
    s.sensor_uuid as '센서UUID',
    s.sensor_name as '센서명',
    s.sensor_loc as '센서위치',
    CASE 
        WHEN s.user_id = s.sensor_id THEN '자신소유'
        WHEN s.user_id != s.sensor_id THEN '공유사용'
        ELSE 'N/A'
    END as '센서관계'
FROM hnt_user u
LEFT JOIN hnt_sensor_info s ON u.user_id = s.user_id
WHERE u.user_grade = 'B'
  AND u.del_yn = 'N'
ORDER BY u.user_id, s.sensor_uuid;

-- 4. B 등급 사용자의 주계정 확인
SELECT 
    u.user_id as 'B등급사용자ID',
    u.user_nm as 'B등급사용자명',
    u.user_grade as '등급',
    s.sensor_id as '주계정ID',
    main_user.user_nm as '주계정명',
    main_user.user_grade as '주계정등급',
    COUNT(s.sensor_uuid) as '공유센서수'
FROM hnt_user u
LEFT JOIN hnt_sensor_info s ON u.user_id = s.user_id AND s.user_id != s.sensor_id
LEFT JOIN hnt_user main_user ON s.sensor_id = main_user.user_id
WHERE u.user_grade = 'B'
  AND u.del_yn = 'N'
GROUP BY u.user_id, u.user_nm, u.user_grade, s.sensor_id, main_user.user_nm, main_user.user_grade
ORDER BY u.user_id;

-- 5. B 등급 사용자 상세 정보 (센서 접근 권한 포함)
SELECT 
    u.user_id as 'B등급사용자ID',
    u.user_nm as 'B등급사용자명',
    u.user_grade as '등급',
    u.user_email as '이메일',
    u.user_tel as '전화번호',
    u.inst_dtm as '생성일시',
    main_user.user_id as '주계정ID',
    main_user.user_nm as '주계정명',
    main_user.user_grade as '주계정등급',
    COUNT(s.sensor_uuid) as '접근가능센서수',
    GROUP_CONCAT(
        CONCAT(s.sensor_name, '(', s.sensor_uuid, ')')
        ORDER BY s.sensor_uuid
        SEPARATOR ', '
    ) as '접근가능센서목록'
FROM hnt_user u
LEFT JOIN hnt_sensor_info s ON u.user_id = s.user_id AND s.user_id != s.sensor_id
LEFT JOIN hnt_user main_user ON s.sensor_id = main_user.user_id
WHERE u.user_grade = 'B'
  AND u.del_yn = 'N'
GROUP BY u.user_id, u.user_nm, u.user_grade, u.user_email, u.user_tel, u.inst_dtm, 
         main_user.user_id, main_user.user_nm, main_user.user_grade
ORDER BY u.inst_dtm DESC;

-- 6. B 등급 사용자별 센서 접근 상세 정보
SELECT 
    u.user_id as 'B등급사용자ID',
    u.user_nm as 'B등급사용자명',
    s.sensor_uuid as '센서UUID',
    s.sensor_name as '센서명',
    s.sensor_loc as '센서위치',
    s.sensor_type as '센서타입',
    main_user.user_id as '주계정ID',
    main_user.user_nm as '주계정명',
    s.inst_dtm as '센서등록일시'
FROM hnt_user u
INNER JOIN hnt_sensor_info s ON u.user_id = s.user_id AND s.user_id != s.sensor_id
INNER JOIN hnt_user main_user ON s.sensor_id = main_user.user_id
WHERE u.user_grade = 'B'
  AND u.del_yn = 'N'
ORDER BY u.user_id, s.sensor_uuid;

-- 7. B 등급 사용자 생성 통계 (월별)
SELECT 
    DATE_FORMAT(inst_dtm, '%Y-%m') as '생성월',
    COUNT(*) as 'B등급생성수',
    GROUP_CONCAT(
        CONCAT(user_id, '(', user_nm, ')')
        ORDER BY inst_dtm
        SEPARATOR ', '
    ) as '생성된B등급사용자'
FROM hnt_user 
WHERE user_grade = 'B'
  AND del_yn = 'N'
GROUP BY DATE_FORMAT(inst_dtm, '%Y-%m')
ORDER BY 생성월 DESC;

-- 8. B 등급 사용자와 주계정 관계 요약
SELECT 
    main_user.user_id as '주계정ID',
    main_user.user_nm as '주계정명',
    main_user.user_grade as '주계정등급',
    COUNT(DISTINCT sub_user.user_id) as '부계정수',
    GROUP_CONCAT(
        DISTINCT CONCAT(sub_user.user_id, '(', sub_user.user_nm, ')')
        ORDER BY sub_user.user_id
        SEPARATOR ', '
    ) as '부계정목록',
    COUNT(DISTINCT s.sensor_uuid) as '공유센서수'
FROM hnt_user main_user
INNER JOIN hnt_sensor_info s ON main_user.user_id = s.sensor_id
INNER JOIN hnt_user sub_user ON s.user_id = sub_user.user_id AND sub_user.user_grade = 'B'
WHERE main_user.del_yn = 'N'
  AND sub_user.del_yn = 'N'
GROUP BY main_user.user_id, main_user.user_nm, main_user.user_grade
ORDER BY 부계정수 DESC, main_user.user_id;
