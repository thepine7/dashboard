-- =====================================================
-- 사용자 관리 센서 조회 쿼리
-- =====================================================

-- 1. 주계정의 모든 센서 조회 (사용자 관리용)
SELECT 
    s.user_id as '사용자ID',
    s.sensor_id as '센서소유자ID',
    s.sensor_uuid as '센서UUID',
    s.sensor_name as '센서명',
    s.sensor_loc as '센서위치',
    s.sensor_type as '센서타입',
    s.sensor_gu as '센서구분',
    s.chart_type as '차트타입',
    s.inst_dtm as '등록일시'
FROM hnt_sensor_info s
WHERE s.sensor_id = 'thepine'  -- 주계정 ID로 변경
  AND s.user_id = s.sensor_id
ORDER BY s.inst_dtm DESC;

-- 2. 부계정의 접근 가능한 센서 조회
SELECT 
    s.user_id as '부계정ID',
    s.sensor_id as '주계정ID',
    s.sensor_uuid as '센서UUID',
    s.sensor_name as '센서명',
    s.sensor_loc as '센서위치',
    s.sensor_type as '센서타입',
    s.sensor_gu as '센서구분',
    s.chart_type as '차트타입',
    s.inst_dtm as '등록일시'
FROM hnt_sensor_info s
INNER JOIN hnt_user u ON s.user_id = u.user_id
WHERE u.user_id = 'thepine7'  -- 부계정 ID로 변경
  AND u.parent_user_id = s.sensor_id
  AND u.del_yn = 'N'
ORDER BY s.inst_dtm DESC;

-- 3. 사용자별 센서 통계
SELECT 
    u.user_id as '사용자ID',
    u.user_nm as '사용자명',
    u.user_grade as '등급',
    u.parent_user_id as '부모사용자ID',
    CASE 
        WHEN u.parent_user_id IS NULL THEN '주계정'
        ELSE '부계정'
    END as '계정유형',
    COUNT(s.sensor_uuid) as '센서수',
    GROUP_CONCAT(
        CONCAT(s.sensor_name, '(', s.sensor_uuid, ')')
        ORDER BY s.sensor_uuid
        SEPARATOR ', '
    ) as '센서목록'
FROM hnt_user u
LEFT JOIN hnt_sensor_info s ON u.user_id = s.sensor_id AND s.user_id = s.sensor_id
WHERE u.del_yn = 'N'
GROUP BY u.user_id, u.user_nm, u.user_grade, u.parent_user_id
ORDER BY 
    CASE WHEN u.parent_user_id IS NULL THEN 0 ELSE 1 END,
    u.user_id;

-- 4. 주계정별 부계정과 센서 관계
SELECT 
    main_user.user_id as '주계정ID',
    main_user.user_nm as '주계정명',
    COUNT(DISTINCT sub_user.user_id) as '부계정수',
    COUNT(DISTINCT s.sensor_uuid) as '센서수',
    GROUP_CONCAT(
        DISTINCT CONCAT(sub_user.user_id, '(', sub_user.user_nm, ')')
        ORDER BY sub_user.user_id
        SEPARATOR ', '
    ) as '부계정목록',
    GROUP_CONCAT(
        DISTINCT CONCAT(s.sensor_name, '(', s.sensor_uuid, ')')
        ORDER BY s.sensor_uuid
        SEPARATOR ', '
    ) as '센서목록'
FROM hnt_user main_user
LEFT JOIN hnt_user sub_user ON main_user.user_id = sub_user.parent_user_id AND sub_user.del_yn = 'N'
LEFT JOIN hnt_sensor_info s ON main_user.user_id = s.sensor_id AND s.user_id = s.sensor_id
WHERE main_user.del_yn = 'N'
  AND main_user.parent_user_id IS NULL
GROUP BY main_user.user_id, main_user.user_nm
HAVING 부계정수 > 0 OR 센서수 > 0
ORDER BY 센서수 DESC, 부계정수 DESC;

-- 5. 특정 사용자의 센서 상세 정보
SELECT 
    s.sensor_uuid as '센서UUID',
    s.sensor_name as '센서명',
    s.sensor_loc as '센서위치',
    s.sensor_type as '센서타입',
    s.sensor_gu as '센서구분',
    s.chart_type as '차트타입',
    s.inst_dtm as '등록일시',
    s.mdf_dtm as '수정일시',
    CASE 
        WHEN s.user_id = s.sensor_id THEN '소유'
        ELSE '공유'
    END as '센서관계'
FROM hnt_sensor_info s
WHERE s.sensor_id = 'thepine'  -- 사용자 ID로 변경
ORDER BY s.inst_dtm DESC;
