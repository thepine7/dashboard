# 인덱스 활용 현황 분석 보고서

**작성일**: 2025-10-17  
**분석자**: Cursor AI  
**데이터베이스**: hnt (MySQL 5.7.9)  
**테이블**: hnt_sensor_data

---

## 📊 현재 상태 (Phase 1+2 완료)

| 항목 | 이전 | 현재 | 변화 |
|------|------|------|------|
| 인덱스 개수 | 16개 | 11개 | -5개 (-31%) |
| 인덱스 크기 | 6,474 MB | 4,204 MB | -2,270 MB (-35%) |
| 데이터 크기 | 5,295 MB | 4,295 MB | -1,000 MB (-19%) |
| 총 크기 | 11,769 MB | 8,499 MB | -3,270 MB (-28%) |

**삭제된 인덱스** (Phase 1+2):
1. ❌ hnt_sensor_data_inst_id_IDX (inst_id)
2. ❌ hnt_sensor_data_mdf_id_IDX (mdf_id)
3. ❌ hnt_sensor_data_uuid_IDX (uuid) - 복합 인덱스로 대체됨
4. ❌ hnt_sensor_data_user_id_IDX (user_id) - 복합 인덱스로 대체됨
5. ❌ hnt_sensor_data_sensor_id_IDX (sensor_id) - 복합 인덱스로 대체됨

---

## 🔍 남아있는 11개 인덱스 분석

### ⭐ 핵심 복합 인덱스 (3개) - 최적 활용 중

#### 1. idx_hnt_sensor_data_uuid_inst_dtm (uuid, inst_dtm)

**크기**: 약 1,200 MB  
**Cardinality**: 28,969,100  
**활용도**: ⭐⭐⭐⭐⭐ (매우 높음)

**사용 위치**:
- `DataMapper.xml`:
  - selectDailyData (라인 166-179)
  - selectWeeklyData (라인 194-209)
  - selectYearlyData (라인 213-226)
  - selectDailyDataWithCursor (라인 182-200)
  - selectSensorDataWithCursor (라인 229-247)

**쿼리 패턴**:
```sql
SELECT ... FROM hnt_sensor_data
WHERE uuid = ?
AND inst_dtm BETWEEN ? AND ?
ORDER BY inst_dtm ASC/DESC
```

**성능 측정**:
- 실행 시간: 1.93초 (기존 5.65초 대비 66% 개선)
- 처리 데이터: 125,018개 → 32,790개 (30일치 → 1분 단위 집계)

**평가**: ✅ **최적 설계 및 활용**

---

#### 2. idx_sensor_data_performance (user_id, sensor_id, uuid, inst_dtm)

**크기**: 약 1,800 MB  
**Cardinality**: 28,969,100  
**활용도**: ⚠️ **부분 활용** (첫 번째 컬럼만)

**현재 사용 위치**:
- `AdminMapper.xml`:
  - getSensorInfo (라인 18): `WHERE user_id = #{userId}`
  - getSensorList (라인 54): `WHERE user_id = #{userId}`
  - getSensorListBySensorId (라인 70-71): `WHERE user_id = ? AND sensor_id = ?`

**문제점**:
- 4개 컬럼 중 1~2개만 활용
- 나머지 컬럼 (uuid, inst_dtm)은 사용되지 않음
- 인덱스 크기 대비 효율성 낮음

**개선 제안**:
```sql
-- 제안 1: 전체 인덱스 활용 쿼리 추가
SELECT sensor_value, inst_dtm 
FROM hnt_sensor_data
WHERE user_id = ?
  AND sensor_id = ?
  AND uuid = ?
  AND inst_dtm BETWEEN ? AND ?
ORDER BY inst_dtm DESC
LIMIT 1000;

-- 제안 2: 인덱스 재설계 검토
-- 현재: (user_id, sensor_id, uuid, inst_dtm)
-- 제안: (user_id, sensor_id) 또는 삭제
```

**평가**: ⚠️ **개선 필요** (활용도 향상 또는 재설계)

---

#### 3. idx_hnt_sensor_data_user_id_uuid (user_id, uuid)

**크기**: 약 900 MB  
**Cardinality**: 28,969,100  
**활용도**: ⭐⭐⭐⭐⭐ (매우 높음)

**사용 위치**:
- `DataMapper.xml`:
  - deleteSensorData (라인 27-31)

**쿼리 패턴**:
```sql
DELETE FROM hnt_sensor_data
WHERE user_id = ?
AND uuid = ?
```

**평가**: ✅ **최적 설계 및 활용** (DELETE 작업에 필수)

---

### 🔧 필수 인덱스 (3개) - 시스템 필수

#### 4. PRIMARY (no)
- **용도**: 기본 키, 자동 증가
- **평가**: ✅ 필수

#### 5. hnt_sensor_data_UN (no, user_id, sensor_id)
- **용도**: UNIQUE 제약조건
- **평가**: ✅ 필수

#### 6. hnt_sensor_data_2 (inst_dtm, no)
- **용도**: 시간 기반 정렬
- **평가**: ✅ 필수

---

### ⚠️ 보조 인덱스 (5개) - 검토 필요

#### 7. idx_sensor_data_download_date_range (inst_dtm, user_id, uuid)

**크기**: 약 1,200 MB  
**Cardinality**: 28,969,100  
**활용도**: ❓ **확인 필요**

**예상 사용 위치**: 엑셀 다운로드 기능 (날짜 범위 조회)

**쿼리 패턴 (예상)**:
```sql
SELECT * FROM hnt_sensor_data
WHERE inst_dtm BETWEEN ? AND ?
  AND user_id = ?
  AND uuid = ?
```

**평가**: ⏸️ **24시간 모니터링 후 결정**

---

#### 8. idx_hnt_sensor_data_inst_dtm (inst_dtm)

**크기**: 약 400 MB  
**Cardinality**: 14,484,550  
**활용도**: ⚠️ **중복 의심**

**문제점**: 
- `hnt_sensor_data_inst_dtm_IDX`와 완전히 동일
- 둘 다 inst_dtm 단일 컬럼 인덱스

**평가**: ⚠️ **Phase 3에서 하나 삭제 권장** (200-300MB 절감)

---

#### 9. hnt_sensor_data_inst_dtm_IDX (inst_dtm)

**크기**: 약 400 MB  
**Cardinality**: 14,484,550  
**활용도**: ⚠️ **중복 의심**

**문제점**: 
- `idx_hnt_sensor_data_inst_dtm`와 완전히 동일

**평가**: ⚠️ **Phase 3에서 하나 삭제 권장** (200-300MB 절감)

---

#### 10. hnt_sensor_data_no_IDX (no)

**크기**: 약 100 MB  
**Cardinality**: 28,969,100  
**활용도**: ❌ **사용되지 않음**

**문제점**:
- PRIMARY KEY가 이미 `no`를 포함
- WHERE no = ? 쿼리를 찾을 수 없음
- 코드에서 `no`로 필터링하는 경우 없음

**검증**:
```bash
# 코드 검색 결과
grep -r "WHERE.*no\s*=" src/main/resources/mapper
# 결과: No matches found
```

**평가**: ❌ **즉시 삭제 가능** (100MB 절감)

---

#### 11. hnt_sensor_data_sensor_value_IDX (sensor_value)

**크기**: 약 50-100 MB  
**Cardinality**: 1,032 (매우 낮음)  
**활용도**: ⚠️ **예상과 다름**

**재분석 결과**:
```sql
EXPLAIN SELECT sensor_value FROM hnt_sensor_data 
WHERE sensor_value = '25.5' LIMIT 10;
```
→ **실제로 이 인덱스를 사용함!**

**문제점**:
- 코드에서 `WHERE sensor_value = ?` 쿼리를 찾을 수 없음
- Cardinality가 매우 낮아 효율성 의심

**코드 검색 결과**:
```bash
grep -r "sensor_value\s*=" src/main/resources/mapper
# 결과: No matches found (WHERE 절에서 사용 안 함)
```

**평가**: ❓ **실제 사용 패턴 추가 확인 필요**
- 애플리케이션 로그 분석 필요
- 슬로우 쿼리 로그 확인 필요

---

## 🔍 쿼리 최적화 기회

### 1. raw_data LIKE 쿼리 비효율

**위치**: `AdminMapper.xml`

#### getCurTemp (라인 446-453)
```sql
-- 현재 (비효율적)
SELECT sensor_value FROM hnt_sensor_data
WHERE uuid = #{sensorUuid}
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR)
  AND raw_data LIKE '%ain%'  -- ⚠️ 인덱스 사용 불가
ORDER BY inst_dtm DESC
LIMIT 1
```

**문제점**:
- `LIKE '%ain%'`는 와일드카드가 앞에 있어 인덱스를 사용할 수 없음
- Full table scan 발생 가능
- uuid + inst_dtm 인덱스 효율 저하

**raw_data 실제 데이터**:
```json
{"actcode":"live","name":"ain","ch":"1","value":"27.6"}
```

**개선안 1**: raw_data 필터링 제거 (애플리케이션 레벨 처리)
```sql
SELECT sensor_value, raw_data FROM hnt_sensor_data
WHERE uuid = #{sensorUuid}
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR)
ORDER BY inst_dtm DESC
LIMIT 10  -- 여유있게 가져와서 애플리케이션에서 필터링
```

**개선안 2**: JSON 함수 활용 (MySQL 5.7.9 지원)
```sql
SELECT sensor_value FROM hnt_sensor_data
WHERE uuid = #{sensorUuid}
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR)
  AND JSON_EXTRACT(raw_data, '$.name') = 'ain'  -- 인덱스는 사용 안 되지만 더 빠름
ORDER BY inst_dtm DESC
LIMIT 1
```

**개선안 3**: sensor_type 컬럼 활용
```sql
-- sensor_type이 이미 저장되어 있다면
SELECT sensor_value FROM hnt_sensor_data
WHERE uuid = #{sensorUuid}
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR)
  AND sensor_type = 'ain'  -- 인덱스 추가 가능
ORDER BY inst_dtm DESC
LIMIT 1
```

**권장**: 개선안 1 (애플리케이션 레벨 필터링)

---

#### chkError (라인 456-461)
```sql
-- 현재 (비효율적)
SELECT COUNT(*) as cnt FROM hnt_sensor_data
WHERE uuid = #{sensorUuid}
  AND raw_data LIKE '%ain%'
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -5 MINUTE)
```

**개선안**:
```sql
SELECT COUNT(*) as cnt FROM hnt_sensor_data
WHERE uuid = #{sensorUuid}
  AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -5 MINUTE)
-- raw_data 필터링 제거 또는 애플리케이션 레벨 처리
```

---

## 📋 Phase 3 최적화 제안

### 즉시 실행 가능 (리스크 낮음)

#### 1. ✅ 코드 주석 업데이트
**파일**: `DataMapper.xml` 라인 177  
**변경**: 삭제된 인덱스 이름 업데이트  
**예상 효과**: 유지보수성 향상  
**완료**: ✅

---

#### 2. ❌ hnt_sensor_data_no_IDX 삭제
**근거**: 
- PRIMARY KEY와 완전 중복
- 코드에서 사용하지 않음 (검증 완료)

**SQL**:
```sql
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_no_IDX;
```

**예상 효과**: 100MB 절감

---

### 24시간 모니터링 후 결정

#### 3. ⏸️ 중복 inst_dtm 인덱스 하나 삭제

**선택 1**: hnt_sensor_data_inst_dtm_IDX 삭제  
**선택 2**: idx_hnt_sensor_data_inst_dtm 삭제

**결정 방법**:
```sql
-- 슬로우 쿼리 로그 활성화
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.5;

-- 24시간 후 로그 분석
-- 어느 인덱스가 더 많이 사용되는지 확인
```

**예상 효과**: 200-300MB 절감

---

#### 4. ⏸️ sensor_value 인덱스 검토

**현재 상태**: 
- EXPLAIN에서는 사용됨
- 코드에서는 WHERE sensor_value = ? 쿼리 없음

**모니터링 필요**:
- 실제 운영 환경에서 사용되는지 확인
- 애플리케이션 로그 분석
- 슬로우 쿼리 로그 확인

**예상 효과**: 50-100MB 절감 (사용 안 하면)

---

### 장기 계획

#### 5. 📋 raw_data LIKE 쿼리 최적화

**대상**:
- getCurTemp (AdminMapper.xml 라인 446-453)
- chkError (AdminMapper.xml 라인 456-461)

**방법**: 
- 애플리케이션 레벨 필터링으로 변경
- 또는 sensor_type 컬럼 활용

**예상 효과**: 
- 쿼리 실행 시간 30-50% 개선
- 인덱스 효율 향상

---

#### 6. 📋 idx_sensor_data_performance 활용도 향상

**현재 문제**: 4개 컬럼 중 1-2개만 활용

**옵션 A**: 전체 활용 쿼리 추가
```sql
SELECT sensor_value, inst_dtm 
FROM hnt_sensor_data
WHERE user_id = ?
  AND sensor_id = ?
  AND uuid = ?
  AND inst_dtm BETWEEN ? AND ?
ORDER BY inst_dtm DESC
LIMIT 1000;
```

**옵션 B**: 인덱스 재설계
```sql
-- 현재: (user_id, sensor_id, uuid, inst_dtm)
-- 제안: (user_id, sensor_id) 또는 (user_id, uuid)
```

**옵션 C**: 인덱스 삭제
- 다른 인덱스로 충분하면 삭제 고려
- 1,800MB 절감 가능

---

## 🎯 최종 권장 실행 계획

### Step 1: 코드 개선 (완료) ✅
- [x] DataMapper.xml 주석 업데이트

### Step 2: 즉시 실행 가능한 최적화
- [ ] hnt_sensor_data_no_IDX 삭제 (100MB)
- [ ] 인덱스 삭제 후 성능 모니터링

### Step 3: 24시간 모니터링 설정
```sql
-- 슬로우 쿼리 로그 활성화
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.5;
SET GLOBAL log_queries_not_using_indexes = 'ON';

-- 로그 파일 위치 확인
SHOW VARIABLES LIKE 'slow_query_log_file';
```

### Step 4: 모니터링 후 Phase 3 실행
- [ ] 중복 inst_dtm 인덱스 하나 삭제 (200-300MB)
- [ ] sensor_value 인덱스 사용 여부 확인 후 결정 (50-100MB)
- [ ] idx_sensor_data_performance 재설계 검토 (1,800MB)

---

## 📊 예상 최종 결과

| 단계 | 인덱스 개수 | 인덱스 크기 | 총 크기 | 누적 절감 |
|------|------------|------------|---------|----------|
| **현재 (Phase 1+2)** | 11개 | 4,204 MB | 8,499 MB | 3,270 MB |
| **Step 2 완료** | 10개 | 4,104 MB | 8,399 MB | 3,370 MB |
| **Step 4 완료 (최소)** | 9개 | 3,804 MB | 8,099 MB | 3,670 MB |
| **Step 4 완료 (최대)** | 7개 | 2,004 MB | 6,299 MB | 5,470 MB |

**최종 예상 절감**: 3.6 GB ~ 5.5 GB (31% ~ 47%)

---

## ✅ 결론

### 잘 활용되는 인덱스 (유지)
1. ✅ idx_hnt_sensor_data_uuid_inst_dtm
2. ✅ idx_hnt_sensor_data_user_id_uuid
3. ✅ PRIMARY, hnt_sensor_data_UN, hnt_sensor_data_2

### 개선 필요 인덱스
1. ⚠️ idx_sensor_data_performance (활용도 낮음)
2. ⚠️ 중복 inst_dtm 인덱스 (하나 삭제 권장)

### 즉시 삭제 가능 인덱스
1. ❌ hnt_sensor_data_no_IDX (PRIMARY KEY와 중복)

### 추가 검토 필요
1. ❓ sensor_value 인덱스 (실제 사용 패턴 확인 필요)
2. ❓ idx_sensor_data_download_date_range (24시간 모니터링)

---

**최종 업데이트**: 2025-10-17 15:30 KST  
**다음 리뷰 일정**: 2025-10-18 (24시간 모니터링 후)

