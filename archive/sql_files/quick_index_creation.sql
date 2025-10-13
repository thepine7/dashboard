-- =====================================================
-- HnT 센서 데이터 빠른 인덱스 생성 (우선순위 순)
-- =====================================================
-- 총 레코드: 36,509,835건
-- 실행 시간: 약 20-30분 (전체)
-- 예상 성능 향상: 70-90%

-- 1. 핵심 복합 인덱스 (가장 중요) ⭐⭐⭐
-- =====================================================
-- 쿼리 패턴: WHERE uuid = ? AND inst_dtm BETWEEN ? AND ?
-- 예상 소요 시간: 5-10분
-- 성능 향상: 10-100배
CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
COMMENT '장치별 시간 범위 조회용 복합 인덱스';

-- 2. 사용자별 장치 조회용 인덱스 ⭐⭐
-- =====================================================
-- 쿼리 패턴: WHERE user_id = ? AND uuid = ?
-- 예상 소요 시간: 3-5분
-- 성능 향상: 5-20배
CREATE INDEX idx_hnt_sensor_data_user_id_uuid 
ON hnt_sensor_data (user_id, uuid) 
COMMENT '사용자별 장치 조회용 복합 인덱스';

-- 3. 시간 기반 조회용 인덱스 (차트용) ⭐⭐
-- =====================================================
-- 쿼리 패턴: WHERE inst_dtm BETWEEN ? AND ? ORDER BY inst_dtm
-- 예상 소요 시간: 10-15분
-- 성능 향상: 5-50배
CREATE INDEX idx_hnt_sensor_data_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
COMMENT '시간 범위 조회용 인덱스';

-- 4. 최근 월별 부분 인덱스 (선택적) ⭐
-- =====================================================
-- 최근 데이터부터 단계적으로 생성

-- 2025-09월 (2,081,294건) - 2-3분
CREATE INDEX idx_hnt_sensor_data_2025_09 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-09-01 00:00:00' 
  AND inst_dtm < '2025-10-01 00:00:00'
COMMENT '2025년 9월 데이터용 부분 인덱스';

-- 2025-08월 (3,213,504건) - 3-5분
CREATE INDEX idx_hnt_sensor_data_2025_08 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-08-01 00:00:00' 
  AND inst_dtm < '2025-09-01 00:00:00'
COMMENT '2025년 8월 데이터용 부분 인덱스';

-- 2025-07월 (4,512,326건) - 4-6분
CREATE INDEX idx_hnt_sensor_data_2025_07 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-07-01 00:00:00' 
  AND inst_dtm < '2025-08-01 00:00:00'
COMMENT '2025년 7월 데이터용 부분 인덱스';

-- 5. 인덱스 생성 완료 확인
-- =====================================================
SELECT 
    '인덱스 생성 완료' as status,
    COUNT(*) as total_indexes,
    NOW() as completion_time
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data%';

-- 6. 성능 테스트 쿼리
-- =====================================================
-- 인덱스 생성 후 이 쿼리들의 성능을 확인하세요

-- 테스트 1: 장치별 최근 1일 데이터 (가장 중요한 쿼리)
EXPLAIN SELECT * FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4' 
  AND inst_dtm >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY inst_dtm DESC
LIMIT 100;

-- 테스트 2: 사용자별 장치 목록
EXPLAIN SELECT DISTINCT uuid FROM hnt_sensor_data 
WHERE user_id = 'thepine';

-- 테스트 3: 차트용 일간 데이터 (30분 단위)
EXPLAIN SELECT 
    DATE_FORMAT(inst_dtm, '%Y-%m-%d %H') as time_group,
    ROUND(AVG(sensor_value), 1) as avg_value
FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4'
  AND inst_dtm >= DATE_SUB(NOW(), INTERVAL 1 DAY)
GROUP BY DATE_FORMAT(inst_dtm, '%Y-%m-%d %H')
ORDER BY time_group;
