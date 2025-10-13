# MySQL 5.7 호환 인덱스 생성 가이드 📊

## 🚨 문제 해결

### 1. **MySQL 5.7 호환성 문제**
- `DROP INDEX IF EXISTS` 문법 미지원
- `SOURCE` 명령어 미지원
- 연결 끊김 문제 발생

### 2. **해결 방법**
- MySQL 5.7 호환 문법 사용
- 타임아웃 설정 연장
- 단계별 인덱스 생성

## ⚡ 단계별 실행 방법

### 1단계: 기존 인덱스 확인
```sql
-- 현재 인덱스 상태 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data%'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
```

### 2단계: 기존 인덱스 삭제 (개별 실행)
```sql
-- 각 인덱스를 개별적으로 삭제
-- 존재하지 않으면 에러가 발생하지만 무시하고 진행

DROP INDEX idx_hnt_sensor_data_uuid_inst_dtm ON hnt_sensor_data;
DROP INDEX idx_hnt_sensor_data_user_id_uuid ON hnt_sensor_data;
DROP INDEX idx_hnt_sensor_data_inst_dtm ON hnt_sensor_data;
```

### 3단계: 핵심 인덱스 생성 (순차 실행)

#### 3-1. 가장 중요한 복합 인덱스
```sql
-- 예상 소요 시간: 5-10분
-- 성능 향상: 10-100배
CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
COMMENT '장치별 시간 범위 조회용 복합 인덱스';
```

#### 3-2. 사용자별 장치 조회용 인덱스
```sql
-- 예상 소요 시간: 3-5분
-- 성능 향상: 5-20배
CREATE INDEX idx_hnt_sensor_data_user_id_uuid 
ON hnt_sensor_data (user_id, uuid) 
COMMENT '사용자별 장치 조회용 복합 인덱스';
```

#### 3-3. 시간 기반 조회용 인덱스
```sql
-- 예상 소요 시간: 10-15분
-- 성능 향상: 5-50배
CREATE INDEX idx_hnt_sensor_data_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
COMMENT '시간 범위 조회용 인덱스';
```

### 4단계: 인덱스 생성 완료 확인
```sql
-- 생성된 인덱스 확인
SELECT 
    '인덱스 생성 완료' as status,
    COUNT(*) as total_indexes,
    NOW() as completion_time
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME LIKE 'idx_hnt_sensor_data%';
```

## ⚠️ 주의사항

### 1. **연결 끊김 방지**
- 각 인덱스 생성 후 완료 확인
- 연결이 끊어지면 다시 연결 후 다음 단계 진행
- 타임아웃 설정이 10분으로 연장됨

### 2. **실행 순서**
- **반드시 순차적으로 실행** (동시 실행 금지)
- 각 단계 완료 후 다음 단계 진행
- 에러 발생 시 해당 단계만 재실행

### 3. **성능 모니터링**
- 인덱스 생성 중 다른 쿼리 성능 저하 가능
- 생성 완료 후 성능 테스트 권장

## 🚀 예상 성능 향상

- **장치별 데이터 조회**: 10-100배 향상
- **차트 데이터 조회**: 5-50배 향상
- **사용자별 장치 조회**: 5-20배 향상
- **전체 쿼리 응답 시간**: 평균 70-90% 단축

## 📝 실행 로그 예시

```sql
-- 정상 완료
Query OK, 0 rows affected (2.34 sec)

-- 인덱스가 존재하지 않는 경우 (무시하고 진행)
ERROR 1091 (42000): Can't DROP 'idx_hnt_sensor_data_uuid_inst_dtm'; check that column/key exists

-- 연결 끊김 (재연결 후 재시도)
ERROR 2013 (HY000): Lost connection to MySQL server during query
```

## 🔧 문제 해결

### 연결 끊김 시
1. MySQL Workbench 재연결
2. 해당 단계부터 다시 실행
3. 타임아웃 설정 확인

### 문법 에러 시
1. MySQL 버전 확인 (`SELECT VERSION();`)
2. 호환 문법 사용
3. 단계별 실행 확인
