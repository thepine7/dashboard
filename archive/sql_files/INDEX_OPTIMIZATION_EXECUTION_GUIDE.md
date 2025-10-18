# 데이터베이스 인덱스 최적화 실행 가이드

**작성일**: 2025-10-17  
**대상 데이터베이스**: hnt (MySQL 5.7.9)  
**대상 테이블**: hnt_sensor_data  
**작성자**: Cursor AI Assistant

---

## 📋 목차

1. [개요](#개요)
2. [Phase 1: 무의미한 인덱스 삭제](#phase-1-무의미한-인덱스-삭제)
3. [Phase 2: 중복 인덱스 삭제](#phase-2-중복-인덱스-삭제)
4. [롤백 절차](#롤백-절차)
5. [모니터링 가이드](#모니터링-가이드)
6. [FAQ](#faq)

---

## 개요

### 현재 상태
- **인덱스 크기**: 5,973.96 MB (데이터보다 큼)
- **총 인덱스 개수**: 26개
- **문제점**: 중복/무의미한 인덱스로 인한 INSERT 성능 저하

### 최적화 목표
- **인덱스 크기 감소**: 약 700MB (11.7%)
- **INSERT 성능 향상**: 15%
- **SELECT 성능**: 영향 없음 (핵심 인덱스 유지)

### 파일 구조
```
archive/sql_files/
├── backup_before_index_optimization_20251017.sql     # 롤백용 백업
├── drop_meaningless_indexes_20251017.sql             # Phase 1 실행 스크립트
├── verify_index_optimization_20251017.sql            # 검증 스크립트
├── phase2_duplicate_indexes_analysis_20251017.sql    # Phase 2 분석
└── INDEX_OPTIMIZATION_EXECUTION_GUIDE.md             # 이 문서
```

---

## Phase 1: 무의미한 인덱스 삭제

### 대상 인덱스
| 인덱스명 | 컬럼 | Cardinality | 이유 |
|---------|------|-------------|------|
| `hnt_sensor_data_inst_id_IDX` | inst_id | 1 | 완전 무의미 |
| `hnt_sensor_data_mdf_id_IDX` | mdf_id | 1 | 완전 무의미 |

### 실행 절차

#### Step 1: 사전 준비 (5분)

```bash
# 1. 현재 디렉토리 확인
cd d:\Project\SW\CursorAI\tomcat22\archive\sql_files

# 2. 백업 파일 존재 확인
ls backup_before_index_optimization_20251017.sql
```

#### Step 2: MySQL 접속 (1분)

```bash
# MySQL 접속
mysql -h hntsolution.co.kr -u root -p hnt

# 비밀번호 입력: HntRoot123!
```

#### Step 3: 사전 확인 (2분)

```sql
-- 현재 테이블 크기 확인
SELECT 
    table_name,
    ROUND(data_length/1024/1024, 2) AS data_mb,
    ROUND(index_length/1024/1024, 2) AS index_mb
FROM information_schema.tables
WHERE table_schema = 'hnt' AND table_name = 'hnt_sensor_data';

-- 삭제 대상 인덱스 확인
SELECT INDEX_NAME, COLUMN_NAME, CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IN ('hnt_sensor_data_inst_id_IDX', 'hnt_sensor_data_mdf_id_IDX');
```

**예상 결과**:
```
index_mb: 5973.96
Cardinality: 1 (두 인덱스 모두)
```

#### Step 4: 인덱스 삭제 실행 (5-10분)

```sql
-- 스크립트 파일 실행
source drop_meaningless_indexes_20251017.sql;

-- 또는 직접 실행
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_inst_id_IDX;
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_mdf_id_IDX;
```

**진행 상황**:
- 인덱스 삭제는 즉시 완료됩니다.
- 대용량 테이블이므로 5-10분 소요 예상

#### Step 5: 검증 (5분)

```sql
-- 검증 스크립트 실행
source verify_index_optimization_20251017.sql;
```

**확인 사항**:
- ✅ 삭제 대상 인덱스 2개가 없는지 확인
- ✅ 인덱스 크기가 약 200MB 감소했는지 확인
- ✅ 핵심 인덱스가 여전히 존재하는지 확인
- ✅ 쿼리 실행 계획이 변하지 않았는지 확인

### 예상 결과

```
작업 전:
- 인덱스 개수: 26개
- 인덱스 크기: 5973.96 MB

작업 후:
- 인덱스 개수: 24개
- 인덱스 크기: 5773.96 MB (약 200MB 감소)
```

### 예상 소요 시간

| 단계 | 소요 시간 | 비고 |
|-----|----------|------|
| 사전 준비 | 5분 | 파일 확인 |
| MySQL 접속 | 1분 | - |
| 사전 확인 | 2분 | 쿼리 실행 |
| 인덱스 삭제 | 5-10분 | 테이블 크기에 따라 다름 |
| 검증 | 5분 | 쿼리 실행 |
| **총 소요 시간** | **18-23분** | - |

---

## Phase 2: 중복 인덱스 삭제

⚠️ **주의**: Phase 1 완료 및 24시간 안정화 확인 후 진행

### 대상 인덱스

| 인덱스명 | 컬럼 | 대체 인덱스 | 삭제 순서 |
|---------|------|------------|----------|
| `hnt_sensor_data_uuid_IDX` | uuid | idx_hnt_sensor_data_uuid_inst_dtm | 1단계 |
| `hnt_sensor_data_user_id_IDX` | user_id | idx_sensor_data_performance | 2단계 |
| `hnt_sensor_data_sensor_id_IDX` | sensor_id | idx_sensor_data_performance | 3단계 |

### 실행 조건 체크리스트

✅ **Phase 1 완료 확인**:
- [ ] inst_id, mdf_id 인덱스 삭제 완료
- [ ] 인덱스 크기 약 200MB 감소 확인
- [ ] 애플리케이션 정상 동작 (24시간 이상)
- [ ] 쿼리 성능 저하 없음
- [ ] 에러 로그 없음

✅ **Phase 2 진행 준비**:
- [ ] 개발팀 승인 완료
- [ ] 백업 완료 확인
- [ ] 트래픽이 적은 시간대 (새벽 2-4시)
- [ ] 롤백 계획 준비 완료

### 단계별 실행 (3일 소요)

#### 1단계: uuid 인덱스 삭제 (Day 1)

```sql
-- 1. 사전 확인
EXPLAIN SELECT * FROM hnt_sensor_data
WHERE uuid = '0008DC755397'
ORDER BY inst_dtm DESC LIMIT 1;
-- 현재 사용 인덱스 확인

-- 2. 삭제 실행
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_uuid_IDX;

-- 3. 사후 확인
EXPLAIN SELECT * FROM hnt_sensor_data
WHERE uuid = '0008DC755397'
ORDER BY inst_dtm DESC LIMIT 1;
-- key: idx_hnt_sensor_data_uuid_inst_dtm 사용 확인

-- 4. 24시간 모니터링
```

#### 2단계: user_id 인덱스 삭제 (Day 2)

```sql
-- 1단계에서 문제 없으면 진행

-- 1. 사전 확인
EXPLAIN SELECT COUNT(*) FROM hnt_sensor_data
WHERE user_id = 'thepine';

-- 2. 삭제 실행
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_user_id_IDX;

-- 3. 사후 확인
EXPLAIN SELECT COUNT(*) FROM hnt_sensor_data
WHERE user_id = 'thepine';
-- key: idx_sensor_data_performance 사용 확인

-- 4. 24시간 모니터링
```

#### 3단계: sensor_id 인덱스 삭제 (Day 3)

```sql
-- 2단계에서 문제 없으면 진행

-- 1. 사전 확인
EXPLAIN SELECT COUNT(*) FROM hnt_sensor_data
WHERE sensor_id = 'thepine';

-- 2. 삭제 실행
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_sensor_id_IDX;

-- 3. 사후 확인
EXPLAIN SELECT COUNT(*) FROM hnt_sensor_data
WHERE sensor_id = 'thepine';
-- key: idx_sensor_data_performance 사용 확인

-- 4. 최종 검증
```

### Phase 2 예상 결과

```
Phase 1 완료 후:
- 인덱스 개수: 24개
- 인덱스 크기: 5773.96 MB

Phase 2 완료 후:
- 인덱스 개수: 21개
- 인덱스 크기: 5273.96 MB (추가 500MB 감소)

총 효과:
- 인덱스 크기: 700MB 감소 (11.7%)
- INSERT 성능: 15% 향상 예상
```

---

## 롤백 절차

### Phase 1 롤백 (문제 발생 시)

```sql
-- backup_before_index_optimization_20251017.sql 파일에서 실행

-- inst_id 인덱스 재생성
ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_inst_id_IDX (inst_id);

-- mdf_id 인덱스 재생성
ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_mdf_id_IDX (mdf_id);

-- 검증
SHOW INDEX FROM hnt_sensor_data;
```

**재생성 소요 시간**: 각 약 30-60분 (2,900만 건 처리)

### Phase 2 롤백

```sql
-- uuid 인덱스 재생성
ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_uuid_IDX (uuid);

-- user_id 인덱스 재생성
ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_user_id_IDX (user_id);

-- sensor_id 인덱스 재생성
ALTER TABLE hnt_sensor_data ADD INDEX hnt_sensor_data_sensor_id_IDX (sensor_id);
```

### 롤백이 필요한 경우

❌ **즉시 롤백 필요**:
- 쿼리 응답 시간이 50% 이상 증가
- 애플리케이션 에러 급증
- 사용자 불만 접수

⚠️ **모니터링 강화**:
- 쿼리 응답 시간이 20-50% 증가
- 간헐적 타임아웃 발생
- CPU 사용률 증가

✅ **정상 상태**:
- 쿼리 응답 시간 변화 없음
- 에러 로그 없음
- 애플리케이션 정상 동작

---

## 모니터링 가이드

### 모니터링 기간

- **Phase 1**: 삭제 후 24-48시간
- **Phase 2**: 각 단계별 24시간

### 모니터링 지표

#### 1. 인덱스 크기 변화

```sql
-- 1시간마다 실행
SELECT 
    CURRENT_TIMESTAMP() AS check_time,
    ROUND(index_length/1024/1024, 2) AS index_mb
FROM information_schema.tables
WHERE table_schema = 'hnt' AND table_name = 'hnt_sensor_data';
```

#### 2. 쿼리 성능 측정

```sql
-- 주요 쿼리 실행 시간 측정
SET @start_time = NOW(6);

-- 일간 데이터 조회 (가장 중요)
SELECT COUNT(*) FROM hnt_sensor_data
WHERE uuid = '0008DC755397'
  AND inst_dtm BETWEEN '2025-10-16' AND '2025-10-17';

SET @end_time = NOW(6);

SELECT 
    TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 1000 AS execution_time_ms;
```

**기준 시간**: 작업 전 실행 시간 기록 후 비교

#### 3. 애플리케이션 로그 확인

```bash
# 에러 로그 확인
tail -f logs/hnt-sensor-api-error.log

# DB 관련 에러 필터링
grep -i "sql\|database\|timeout" logs/hnt-sensor-api-error.log
```

#### 4. INSERT 성능 측정

```sql
-- MQTT로 인입되는 센서 데이터 INSERT 시간 모니터링
-- logs/hnt-sensor-api-performance.log 확인
```

### 모니터링 체크리스트

✅ **정상 지표**:
- [ ] 인덱스 크기 예상대로 감소
- [ ] 쿼리 응답 시간 ±10% 이내
- [ ] 에러 로그 없음
- [ ] CPU/메모리 사용률 정상
- [ ] INSERT 처리량 유지 또는 증가

⚠️ **주의 지표**:
- [ ] 쿼리 응답 시간 +20% 이상
- [ ] 간헐적 타임아웃 발생
- [ ] CPU 사용률 +30% 이상

❌ **경고 지표**:
- [ ] 쿼리 응답 시간 +50% 이상
- [ ] 빈번한 타임아웃
- [ ] 애플리케이션 에러 급증
- [ ] 사용자 불만 접수

---

## FAQ

### Q1. Phase 1과 Phase 2를 동시에 진행할 수 없나요?

**A**: 안전을 위해 단계적 진행을 권장합니다.
- Phase 1은 리스크가 거의 없는 무의미한 인덱스 삭제입니다.
- Phase 2는 중복 인덱스 삭제로, 쿼리 영향도를 확인해야 합니다.
- Phase 1 안정화 확인 후 Phase 2를 진행하세요.

### Q2. 인덱스 삭제 중 서비스에 영향이 있나요?

**A**: 인덱스 삭제는 테이블 Lock을 유발할 수 있습니다.
- 삭제 시간: 5-10분 (Phase 1), 각 5-10분 (Phase 2)
- 트래픽이 적은 시간대 (새벽 2-4시) 작업 권장
- MyISAM 엔진은 테이블 Lock이 발생하므로 주의 필요

### Q3. 롤백이 어렵나요?

**A**: 롤백은 간단하지만 시간이 오래 걸립니다.
- SQL문 실행만으로 롤백 가능
- 인덱스 재생성 시간: 각 30-60분 (2,900만 건 처리)
- 백업 파일에 롤백 SQL이 준비되어 있습니다.

### Q4. Phase 2는 꼭 해야 하나요?

**A**: Phase 1만으로도 충분한 효과가 있습니다.
- Phase 1: 무의미한 인덱스 삭제 (200MB 감소, 리스크 없음)
- Phase 2: 중복 인덱스 삭제 (500MB 추가 감소, 약간의 리스크)
- Phase 1 완료 후 성능 개선이 충분하다면 Phase 2는 선택사항입니다.

### Q5. 인덱스 크기가 왜 데이터보다 큰가요?

**A**: 인덱스가 너무 많고 VARCHAR 컬럼이 길어서입니다.
- 현재 26개 인덱스 (너무 많음)
- VARCHAR(100) 컬럼이 여러 인덱스에 중복 포함
- 복합 인덱스의 총 크기가 데이터를 초과

### Q6. 작업 중 문제가 발생하면?

**A**: 즉시 작업을 중단하고 롤백하세요.
1. 현재 단계 중단
2. 롤백 SQL 실행
3. 로그 확인 및 원인 분석
4. 필요시 개발팀 문의

---

## 연락처

**문의**: HnT Solutions 개발팀  
**작성자**: Cursor AI Assistant  
**최종 검토**: 2025-10-17

---

## 작업 이력

| 날짜 | Phase | 작업 내용 | 결과 | 작업자 |
|-----|-------|----------|------|--------|
| 2025-10-17 | 준비 | 백업 및 스크립트 생성 | 완료 | AI |
| | Phase 1 | 무의미한 인덱스 2개 삭제 | 대기 | - |
| | Phase 2-1 | uuid 인덱스 삭제 | 대기 | - |
| | Phase 2-2 | user_id 인덱스 삭제 | 대기 | - |
| | Phase 2-3 | sensor_id 인덱스 삭제 | 대기 | - |

---

**⚠️ 주의사항**: 
- 모든 작업은 백업 후 진행하세요.
- 트래픽이 적은 시간대에 작업하세요.
- 문제 발생 시 즉시 롤백하세요.
- 각 단계 완료 후 충분한 모니터링을 수행하세요.

**✅ 성공 기준**:
- 인덱스 크기 약 700MB 감소
- INSERT 성능 15% 향상
- SELECT 성능 영향 없음
- 애플리케이션 안정성 유지

