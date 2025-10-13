-- =====================================================
-- raw_data 컬럼 LIKE 검색 최적화를 위한 인덱스 생성
-- =====================================================
-- 실행 전 반드시 백업을 수행하세요

-- 1. raw_data 컬럼 인덱스 생성 (LIKE 검색 최적화)
-- =====================================================
-- 쿼리 패턴: WHERE raw_data LIKE '%ain%'
-- 예상 성능 향상: 5-10배
CREATE INDEX idx_hnt_sensor_data_raw_data 
ON hnt_sensor_data (raw_data) 
COMMENT 'raw_data LIKE 검색 최적화용 인덱스';

-- 2. 복합 인덱스 최적화 (uuid + raw_data + inst_dtm)
-- =====================================================
-- 쿼리 패턴: WHERE uuid = ? AND raw_data LIKE '%ain%' AND inst_dtm >= ?
-- 예상 성능 향상: 10-50배
CREATE INDEX idx_hnt_sensor_data_uuid_raw_inst 
ON hnt_sensor_data (uuid, raw_data, inst_dtm) 
COMMENT 'uuid + raw_data + inst_dtm 복합 인덱스';

-- 3. 알림 테이블 인덱스 최적화
-- =====================================================
-- 사용자별 센서별 알림 조회 최적화
CREATE INDEX idx_hnt_alarm_user_sensor 
ON hnt_alarm (user_id, sensor_uuid) 
COMMENT '사용자별 센서별 알림 조회용 인덱스';

-- 알림 시간 기반 조회 최적화
CREATE INDEX idx_hnt_alarm_time 
ON hnt_alarm (alarm_time) 
COMMENT '알림 시간 기반 조회용 인덱스';

-- 복합 인덱스 (user_id + sensor_uuid + alarm_time)
CREATE INDEX idx_hnt_alarm_user_sensor_time 
ON hnt_alarm (user_id, sensor_uuid, alarm_time) 
COMMENT '사용자별 센서별 시간 기반 알림 조회용 복합 인덱스';

-- 4. 기존 인덱스 확인
-- =====================================================
SHOW INDEX FROM hnt_sensor_data;
SHOW INDEX FROM hnt_alarm;

-- 5. 쿼리 실행 계획 확인 (성능 테스트용)
-- =====================================================
-- 현재 온도 조회 쿼리 최적화 확인
EXPLAIN SELECT sensor_value 
FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4' 
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR)
  AND raw_data LIKE '%ain%'
ORDER BY inst_dtm DESC 
LIMIT 1;

-- 에러 체크 쿼리 최적화 확인
EXPLAIN SELECT COUNT(*) as cnt 
FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4' 
  AND raw_data LIKE '%ain%'
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -5 MINUTE);



















