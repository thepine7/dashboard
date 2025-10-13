# MySQL Workbench 인덱스 생성 가이드

## 📋 실행 순서

### 1단계: 기존 인덱스 확인
```sql
-- 이 쿼리를 먼저 실행하여 현재 인덱스 상태를 확인
SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    NON_UNIQUE,
    INDEX_COMMENT
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN (
    'idx_hnt_sensor_data_uuid_inst_dtm',
    'idx_hnt_sensor_data_user_id_uuid', 
    'idx_hnt_sensor_data_inst_dtm'
  )
GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_COMMENT
ORDER BY INDEX_NAME;
```

**결과 해석:**
- 결과가 없으면 → 인덱스가 없음 (생성 필요)
- 결과가 있으면 → 인덱스가 이미 존재 (생성 건너뛰기)

### 2단계: 인덱스 생성 (필요한 것만)

#### 2-1. 장치별 시간 범위 조회용 복합 인덱스
```sql
-- 위 쿼리 결과에서 'idx_hnt_sensor_data_uuid_inst_dtm'이 없으면 실행
-- 예상 소요 시간: 5-10분
CREATE INDEX idx_hnt_sensor_data_uuid_inst_dtm 
ON hnt_sensor_data (uuid, inst_dtm) 
COMMENT '장치별 시간 범위 조회용 복합 인덱스';
```

#### 2-2. 사용자별 장치 조회용 인덱스
```sql
-- 위 쿼리 결과에서 'idx_hnt_sensor_data_user_id_uuid'가 없으면 실행
-- 예상 소요 시간: 3-5분
CREATE INDEX idx_hnt_sensor_data_user_id_uuid 
ON hnt_sensor_data (user_id, uuid) 
COMMENT '사용자별 장치 조회용 복합 인덱스';
```

#### 2-3. 시간 기반 조회용 인덱스
```sql
-- 위 쿼리 결과에서 'idx_hnt_sensor_data_inst_dtm'이 없으면 실행
-- 예상 소요 시간: 10-15분
CREATE INDEX idx_hnt_sensor_data_inst_dtm 
ON hnt_sensor_data (inst_dtm) 
COMMENT '시간 범위 조회용 인덱스';
```

### 3단계: 최종 확인
```sql
-- 모든 인덱스 생성 완료 후 최종 확인
SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    NON_UNIQUE,
    INDEX_COMMENT,
    '생성 완료' as STATUS
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN (
    'idx_hnt_sensor_data_uuid_inst_dtm',
    'idx_hnt_sensor_data_user_id_uuid', 
    'idx_hnt_sensor_data_inst_dtm'
  )
GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_COMMENT
ORDER BY INDEX_NAME;
```

## ⚠️ 주의사항

1. **연결 유지**: 각 인덱스 생성은 시간이 오래 걸리므로 연결이 끊어지지 않도록 주의
2. **순차 실행**: 한 번에 하나씩 실행하여 안전하게 처리
3. **모니터링**: 작업 관리자에서 MySQL 프로세스 상태 확인
4. **백업**: 인덱스 생성 전 데이터베이스 백업 권장

## 📊 예상 성능 개선

- **차트 데이터 조회**: 10-50배 성능 향상
- **실시간 데이터 조회**: 5-20배 성능 향상
- **사용자별 장치 조회**: 3-10배 성능 향상
