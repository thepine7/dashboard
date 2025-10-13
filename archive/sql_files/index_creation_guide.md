# HnT 센서 데이터 인덱스 생성 가이드 📊

## 📈 데이터 현황 분석
- **총 레코드 수**: 36,509,835건
- **데이터 기간**: 2023-11-09 ~ 2025-09-23 (22개월)
- **월별 최대 데이터**: 2025-07월 (4,512,326건)
- **현재 월 데이터**: 2025-09월 (2,081,294건)

## 🎯 인덱스 생성 전략

### 1단계: 핵심 복합 인덱스 (우선 생성)
```sql
-- 가장 중요한 쿼리 패턴용
CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm);
```

### 2단계: 사용자별 조회용 인덱스
```sql
-- 사용자별 장치 조회용
CREATE INDEX idx_hnt_sensor_data_user_id_uuid 
ON hnt_sensor_data (user_id, uuid);
```

### 3단계: 시간 기반 조회용 인덱스
```sql
-- 차트 데이터 조회용
CREATE INDEX idx_hnt_sensor_data_inst_dtm 
ON hnt_sensor_data (inst_dtm);
```

### 4단계: 월별 부분 인덱스 (최근 6개월)
- **2025-09월**: 2,081,294건
- **2025-08월**: 3,213,504건  
- **2025-07월**: 4,512,326건
- **2025-06월**: 2,248,645건
- **2025-05월**: 935,386건
- **2025-03월**: 406,063건

## ⚡ 실행 순서 (권장)

### 1. 즉시 실행 (성능 향상)
```sql
-- 1. 핵심 복합 인덱스 (5-10분 소요)
CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm);

-- 2. 사용자별 인덱스 (3-5분 소요)
CREATE INDEX idx_hnt_sensor_data_user_id_uuid 
ON hnt_sensor_data (user_id, uuid);

-- 3. 시간 기반 인덱스 (10-15분 소요)
CREATE INDEX idx_hnt_sensor_data_inst_dtm 
ON hnt_sensor_data (inst_dtm);
```

### 2. 단계적 실행 (월별)
```sql
-- 최근 월부터 순차적으로 실행
-- 각 월별 인덱스는 2-5분 소요

-- 2025-09월 (2,081,294건)
CREATE INDEX idx_hnt_sensor_data_2025_09 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-09-01 00:00:00' 
  AND inst_dtm < '2025-10-01 00:00:00';

-- 2025-08월 (3,213,504건)
CREATE INDEX idx_hnt_sensor_data_2025_08 
ON hnt_sensor_data (uuid, inst_dtm) 
WHERE inst_dtm >= '2025-08-01 00:00:00' 
  AND inst_dtm < '2025-09-01 00:00:00';

-- ... 나머지 월별 인덱스
```

## 🔍 성능 모니터링

### 인덱스 생성 확인
```sql
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data%'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
```

### 쿼리 성능 테스트
```sql
-- 장치별 최근 1일 데이터 조회
EXPLAIN SELECT * FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4' 
  AND inst_dtm >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY inst_dtm DESC;

-- 차트용 일간 데이터 조회
EXPLAIN SELECT 
    DATE_FORMAT(inst_dtm, '%Y-%m-%d %H') as time_group,
    ROUND(AVG(sensor_value), 1) as avg_value
FROM hnt_sensor_data 
WHERE uuid = '0008DC7553A4'
  AND inst_dtm >= DATE_SUB(NOW(), INTERVAL 1 DAY)
GROUP BY DATE_FORMAT(inst_dtm, '%Y-%m-%d %H')
ORDER BY time_group;
```

## ⚠️ 주의사항

1. **인덱스 생성 시간**: 각 인덱스마다 2-15분 소요
2. **디스크 공간**: 인덱스로 인해 약 20-30% 추가 공간 필요
3. **동시 접속**: 인덱스 생성 중에는 쿼리 성능이 일시적으로 저하될 수 있음
4. **백업**: 인덱스 생성 전 데이터베이스 백업 권장

## 🚀 예상 성능 향상

- **장치별 데이터 조회**: 10-100배 향상
- **차트 데이터 조회**: 5-50배 향상  
- **사용자별 장치 조회**: 5-20배 향상
- **전체 쿼리 응답 시간**: 평균 70-90% 단축

## 📝 실행 로그

인덱스 생성 시 다음과 같은 로그를 확인하세요:
- `Query OK, 0 rows affected (2.34 sec)` - 정상 완료
- `ERROR 1061 (42000): Duplicate key name` - 이미 존재하는 인덱스
- `ERROR 1072 (42000): Key column doesn't exist` - 컬럼명 오류
