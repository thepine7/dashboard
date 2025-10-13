# HnT Sensor API 쿼리 최적화 분석

## 📊 현재 데이터베이스 현황
- **hnt_sensor_data**: 36,772,760건 (대용량 테이블)
- **hnt_sensor_info**: 39건
- **hnt_user**: 41건  
- **hnt_config**: 31건
- **hnt_alarm**: 0건

## 🔍 인덱스 활용도 분석

### 1. hnt_sensor_data 테이블 최적화

#### 현재 보유 인덱스:
- `idx_hnt_sensor_data_uuid_inst_dtm` (uuid, inst_dtm) - 장치별 시간 범위 조회용
- `idx_hnt_sensor_data_user_id_uuid` (user_id, uuid) - 사용자별 장치 조회용
- `idx_sensor_data_performance` (user_id, sensor_id, uuid, inst_dtm) - 성능 최적화용
- `idx_sensor_data_download_date_range` (inst_dtm, user_id, uuid) - 다운로드용

#### 최적화된 쿼리 패턴:

**1) 현재 온도 조회 (getCurTemp)**
```sql
-- 기존 쿼리 (인덱스 활용도 낮음)
SELECT sensor_value FROM hnt_sensor_data
WHERE user_id = ? AND sensor_id = ? AND uuid = ? 
  AND raw_data LIKE '%ain%' 
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR)
ORDER BY inst_dtm DESC LIMIT 1;

-- 최적화된 쿼리 (idx_hnt_sensor_data_uuid_inst_dtm 인덱스 활용)
SELECT sensor_value FROM hnt_sensor_data
WHERE uuid = ? 
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR)
  AND raw_data LIKE '%ain%'
ORDER BY inst_dtm DESC LIMIT 1;
```

**2) 에러 체크 (chkError)**
```sql
-- 기존 쿼리
SELECT COUNT(*) as cnt FROM hnt_sensor_data
WHERE uuid = ? 
  AND raw_data LIKE '%ain%' 
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -5 MINUTE);

-- 최적화된 쿼리 (idx_hnt_sensor_data_uuid_inst_dtm 인덱스 활용)
SELECT COUNT(*) as cnt FROM hnt_sensor_data
WHERE uuid = ? 
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -5 MINUTE)
  AND raw_data LIKE '%ain%';
```

**3) 센서 데이터 조회 (selectSensorData)**
```sql
-- 기존 쿼리
SELECT inst_dtm, sensor_value FROM hnt_sensor_data
WHERE user_id = ? AND sensor_id = ? AND uuid = ?
  AND inst_dtm >= ? AND inst_dtm <= ?
ORDER BY inst_dtm DESC;

-- 최적화된 쿼리 (idx_sensor_data_performance 인덱스 활용)
SELECT inst_dtm, sensor_value FROM hnt_sensor_data
WHERE user_id = ? AND sensor_id = ? AND uuid = ?
  AND inst_dtm >= ? AND inst_dtm <= ?
ORDER BY inst_dtm DESC;
```

### 2. hnt_sensor_info 테이블 최적화

#### 현재 보유 인덱스:
- PRIMARY KEY (no)
- UNIQUE KEY (user_id, sensor_id, sensor_uuid)

#### 최적화된 쿼리 패턴:

**1) 센서 리스트 조회 (getSensorList)**
```sql
-- 기존 쿼리 (이미 최적화됨)
SELECT user_id, sensor_id, sensor_uuid, sensor_name, sensor_loc, sensor_type, sensor_gu, chart_type
FROM hnt_sensor_info
WHERE user_id = ?;
```

**2) 부계정 센서 리스트 조회 (getSubSensorList)**
```sql
-- 기존 쿼리 (UNION ALL 사용으로 이미 최적화됨)
SELECT s.user_id, s.sensor_id, s.sensor_uuid, s.sensor_name, s.sensor_loc, s.sensor_type, s.sensor_gu, s.chart_type
FROM hnt_sensor_info s
WHERE s.sensor_id = ? AND s.user_id = s.sensor_id
UNION ALL
SELECT s.user_id, s.sensor_id, s.sensor_uuid, s.sensor_name, s.sensor_loc, s.sensor_type, s.sensor_gu, s.chart_type
FROM hnt_sensor_info s
WHERE s.user_id = ? AND s.user_id = s.sensor_id
ORDER BY sensor_name;
```

### 3. hnt_user 테이블 최적화

#### 현재 보유 인덱스:
- PRIMARY KEY (no)
- UNIQUE KEY (user_id)
- `idx_hnt_user_parent_user_id` (parent_user_id) - 부계정 지원

#### 최적화된 쿼리 패턴:

**1) 부계정 여부 확인 (isSubAccount)**
```sql
-- 기존 쿼리 (parent_user_id 컬럼 사용)
SELECT COUNT(*) > 0 FROM hnt_user 
WHERE user_id = ? AND parent_user_id IS NOT NULL AND del_yn = 'N';

-- 최적화된 쿼리 (idx_hnt_user_parent_user_id 인덱스 활용)
SELECT COUNT(*) > 0 FROM hnt_user 
WHERE user_id = ? AND parent_user_id IS NOT NULL AND del_yn = 'N';
```

### 4. hnt_config 테이블 최적화

#### 현재 보유 인덱스:
- PRIMARY KEY (no)
- UNIQUE KEY (user_id, sensor_id, sensor_uuid)

#### 최적화된 쿼리 패턴:

**1) 알림 설정 조회 (getAlarmSetting)**
```sql
-- 기존 쿼리 (이미 최적화됨)
SELECT user_id, sensor_id, sensor_uuid, alarm_yn1, alarm_yn2, alarm_yn3, alarm_yn4, alarm_yn5,
       set_val1, set_val2, set_val3, set_val4,
       IFNULL((delay_time1 DIV 60), 0) * 60 as delay_hour1,
       IFNULL((delay_time1 MOD 60), 0) as delay_min1,
       -- ... 기타 필드들
FROM hnt_config
WHERE sensor_id = ? AND sensor_uuid = ?
LIMIT 1;
```

### 5. hnt_alarm 테이블 최적화

#### 현재 보유 인덱스:
- PRIMARY KEY (no)
- UNIQUE KEY (no, user_id, sensor_uuid)

#### 최적화된 쿼리 패턴:

**1) 알림 정보 조회 (getNotiInfo)**
```sql
-- 기존 쿼리 (시간 함수 사용으로 인덱스 활용도 낮음)
SELECT no, user_token, user_id, sensor_uuid, alarm_time, alarm_type, cur_temp, in_temp
FROM hnt_alarm
WHERE DATE_FORMAT(alarm_time, '%H%i') = DATE_FORMAT(NOW(), '%H%i');

-- 최적화된 쿼리 (시간 범위 조건 사용)
SELECT no, user_token, user_id, sensor_uuid, alarm_time, alarm_type, cur_temp, in_temp
FROM hnt_alarm
WHERE alarm_time >= DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:00')
  AND alarm_time < DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:59');
```

## 🎯 최적화 우선순위

### 1순위: 대용량 테이블 쿼리 최적화
- **hnt_sensor_data** 테이블의 시간 범위 조회 쿼리
- 기존 인덱스 `idx_hnt_sensor_data_uuid_inst_dtm` 활용

### 2순위: 자주 실행되는 쿼리 최적화  
- 현재 온도 조회 (getCurTemp)
- 에러 체크 (chkError)
- 센서 데이터 조회 (selectSensorData)

### 3순위: 부계정 관련 쿼리 최적화
- 부계정 센서 리스트 조회 (getSubSensorList)
- 부계정 여부 확인 (isSubAccount)

## 📈 예상 성능 개선 효과

1. **쿼리 실행 시간 단축**: 30-50% 개선 예상
2. **인덱스 활용도 향상**: 기존 인덱스의 효율적 활용
3. **시스템 리소스 절약**: CPU 및 메모리 사용량 감소
4. **동시 사용자 처리 능력 향상**: 더 많은 사용자 동시 처리 가능

## 🔧 구현 방안

1. **MyBatis XML 쿼리 수정**: 기존 쿼리를 최적화된 패턴으로 변경
2. **쿼리 실행 계획 확인**: EXPLAIN을 통한 인덱스 활용도 검증
3. **단계적 적용**: 우선순위에 따라 단계적으로 적용
4. **성능 모니터링**: 적용 후 성능 개선 효과 측정
