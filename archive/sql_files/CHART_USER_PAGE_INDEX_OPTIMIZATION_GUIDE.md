# 차트설정 & 사용자관리 페이지 인덱스 최적화 실행 가이드

**작성일**: 2025-10-17  
**대상 테이블**: hnt_sensor_info, hnt_user  
**예상 소요 시간**: Phase 1 (10분), Phase 2 (15분)

---

## 📋 목차

1. [개요](#개요)
2. [사전 준비](#사전-준비)
3. [Phase 1: 필수 인덱스 추가](#phase-1-필수-인덱스-추가)
4. [Phase 2: 중복 인덱스 정리](#phase-2-중복-인덱스-정리)
5. [검증 및 모니터링](#검증-및-모니터링)
6. [롤백 절차](#롤백-절차)
7. [FAQ](#faq)

---

## 개요

### 최적화 목적

차트설정 페이지와 사용자관리 페이지의 DB 쿼리 성능을 개선하기 위해 다음 작업을 수행합니다:

1. **Phase 1**: Full table scan을 제거하기 위한 필수 인덱스 추가
2. **Phase 2**: INSERT/UPDATE 성능 향상을 위한 중복 인덱스 정리

### 예상 효과

| 항목 | 현재 | 최적화 후 | 개선율 |
|------|------|-----------|--------|
| 차트 페이지 (센서 100개 시) | 0.5초 | 0.01초 | **98%** ⬇️ |
| 사용자 목록 (1,000명 시) | 3초 | 0.1초 | **97%** ⬇️ |
| 인덱스 크기 | 15 MB | 0 MB | **15 MB** 감소 |
| INSERT/UPDATE 성능 | 기준 | 기준 +15% | **15%** ⬆️ |

---

## 사전 준비

### 1. 백업 생성 (필수)

```bash
# 현재 디렉토리: D:\Project\SW\CursorAI\tomcat22

# Step 1: 전체 데이터베이스 백업
mysqldump -h hntsolution.co.kr -u root -pHntRoot123! hnt \
  --single-transaction --routines --triggers --events \
  > archive/sql_files/backup_hnt_full_before_chart_user_optimization_$(date +%Y%m%d_%H%M%S).sql

# Step 2: 테이블 구조만 백업
mysqldump -h hntsolution.co.kr -u root -pHntRoot123! hnt \
  hnt_sensor_info hnt_user --no-data \
  > archive/sql_files/backup_tables_structure_$(date +%Y%m%d_%H%M%S).sql

# Step 3: 현재 인덱스 상태 백업
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 \
  -e "SHOW INDEX FROM hnt_sensor_info; SHOW INDEX FROM hnt_user;" \
  > archive/sql_files/backup_current_indexes_$(date +%Y%m%d_%H%M%S).txt
```

**백업 파일 확인**:
```bash
ls -lh archive/sql_files/backup_*
```

### 2. 디스크 공간 확인

```bash
# Windows PowerShell
Get-Volume

# 또는 MySQL에서 확인
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 -e "
SELECT 
    table_schema as 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'hnt'
GROUP BY table_schema;
"
```

**최소 필요 공간**: 500 MB (인덱스 생성 시 임시 공간)

### 3. 실행 시점 선택

**권장 시점**: 
- 사용자 트래픽이 적은 시간대 (예: 새벽 2~4시)
- 주말 또는 공휴일

**피해야 할 시점**:
- 업무 시간 (09:00 ~ 18:00)
- 데이터 백업 시간
- 시스템 점검 시간

---

## Phase 1: 필수 인덱스 추가

### 실행 시점
**즉시 실행 가능** (사용자 트래픽 적은 시간대 권장)

### Step 1: SQL 파일 확인

```bash
# SQL 파일 위치 확인
cat archive/sql_files/chart_user_page_index_optimization_phase1_20251017.sql
```

### Step 2: MySQL 접속

```bash
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8
```

### Step 3: 인덱스 추가 실행

**방법 A: SQL 파일 직접 실행 (권장)**
```bash
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 \
  < archive/sql_files/chart_user_page_index_optimization_phase1_20251017.sql \
  > archive/sql_files/phase1_execution_log_$(date +%Y%m%d_%H%M%S).txt 2>&1
```

**방법 B: 단계별 수동 실행**

**MySQL 접속 후 실행**:

```sql
-- 1. 현재 인덱스 확인
SHOW INDEX FROM hnt_sensor_info;
SHOW INDEX FROM hnt_user;

-- 2. hnt_sensor_info: sensor_uuid 인덱스 추가
CREATE INDEX idx_sensor_info_sensor_uuid ON hnt_sensor_info(sensor_uuid);

-- 예상 소요 시간: 1~2초
-- 확인: Query OK, 0 rows affected (1.23 sec)

-- 3. hnt_user: del_yn + no 복합 인덱스 추가
CREATE INDEX idx_user_del_no ON hnt_user(del_yn, no DESC);

-- 예상 소요 시간: 1~2초
-- 확인: Query OK, 0 rows affected (0.98 sec)

-- 4. 인덱스 생성 확인
SHOW INDEX FROM hnt_sensor_info WHERE Key_name = 'idx_sensor_info_sensor_uuid';
SHOW INDEX FROM hnt_user WHERE Key_name = 'idx_user_del_no';
```

### Step 4: 검증

**4.1. 인덱스 사용 확인 (EXPLAIN)**

```sql
-- getSensorInfoByUuid 쿼리 검증
EXPLAIN SELECT * FROM hnt_sensor_info WHERE sensor_uuid = '0008DC755397' LIMIT 1;

-- 예상 결과:
-- key: idx_sensor_info_sensor_uuid ✅
-- type: ref (또는 eq_ref) ✅
-- rows: 1 ✅

-- getUserListWithActivityStatus 쿼리 검증
EXPLAIN SELECT u.no, u.user_nm, u.user_id 
FROM hnt_user u 
WHERE u.del_yn = 'N' 
ORDER BY u.no DESC 
LIMIT 100;

-- 예상 결과:
-- key: idx_user_del_no ✅
-- type: range (또는 ref) ✅
-- Extra: Using index (또는 Using where만) ✅
-- Using filesort 제거 확인 ✅
```

**4.2. 실제 쿼리 실행 테스트**

```sql
-- 차트 페이지 센서 정보 조회
SELECT user_id, sensor_id, sensor_uuid, sensor_name 
FROM hnt_sensor_info 
WHERE sensor_uuid = '0008DC755397' 
LIMIT 1;

-- 사용자 목록 조회
SELECT user_id, user_nm, user_grade 
FROM hnt_user 
WHERE del_yn = 'N' 
ORDER BY no DESC 
LIMIT 10;
```

**예상 결과**: 정상 실행, 데이터 형식 정상

### Step 5: 애플리케이션 재시작

```powershell
# Synology NAS Container Manager에서 톰캣2 재시작
# 또는 SSH 접속 후:
# docker restart tomcat2
```

### Step 6: 기능 테스트

**테스트 항목**:
1. ✅ 차트 페이지 로딩 (http://iot.hntsolution.co.kr:8888/chart/chart)
2. ✅ 센서 선택 시 차트 표시
3. ✅ 사용자 목록 페이지 (http://iot.hntsolution.co.kr:8888/admin/userList)
4. ✅ 사용자 검색 및 필터링

**예상 결과**: 모든 기능 정상 작동, 로딩 속도 개선

---

## Phase 2: 중복 인덱스 정리

### 실행 시점
**Phase 1 실행 후 1주일** (2025-10-24)

### 사전 확인사항 (필수)

1. ✅ **Phase 1 인덱스 정상 작동 확인**
   ```sql
   EXPLAIN SELECT * FROM hnt_sensor_info WHERE sensor_uuid = '0008DC755397';
   -- key: idx_sensor_info_sensor_uuid 확인
   
   EXPLAIN SELECT * FROM hnt_user WHERE del_yn = 'N' ORDER BY no DESC;
   -- key: idx_user_del_no 확인
   ```

2. ✅ **24시간 slow query log 분석**
   ```sql
   -- Slow query log 활성화
   SET GLOBAL slow_query_log = 'ON';
   SET GLOBAL long_query_time = 1;
   
   -- 24시간 후 확인
   SHOW VARIABLES LIKE 'slow_query_log_file';
   ```

3. ✅ **애플리케이션 에러 로그 확인**
   ```bash
   tail -n 100 logs/hnt-sensor-api.log | grep -i error
   tail -n 100 logs/hnt-sensor-api-db.log | grep -i error
   ```

4. ✅ **백업 파일 생성 완료 확인**
   ```bash
   ls -lh archive/sql_files/backup_* | tail -n 5
   ```

### Step 1: 인덱스 사용 통계 확인

```sql
-- hnt_sensor_info 인덱스 카디널리티 확인
SELECT 
    INDEX_NAME,
    CARDINALITY,
    SEQ_IN_INDEX,
    COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt'
AND TABLE_NAME = 'hnt_sensor_info'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- hnt_user 인덱스 카디널리티 확인
SELECT 
    INDEX_NAME,
    CARDINALITY,
    SEQ_IN_INDEX,
    COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt'
AND TABLE_NAME = 'hnt_user'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
```

### Step 2: 중복 인덱스 삭제

**방법 A: SQL 파일 직접 실행 (권장)**

⚠️ **주의**: 주석을 제거한 후 실행하세요!

```bash
# SQL 파일 편집: ALTER TABLE 명령어 주석 제거
# 편집기에서 archive/sql_files/chart_user_page_index_optimization_phase2_20251017.sql 열기
# 각 ALTER TABLE 명령어 앞의 "--" 제거

# 실행
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 \
  < archive/sql_files/chart_user_page_index_optimization_phase2_20251017.sql \
  > archive/sql_files/phase2_execution_log_$(date +%Y%m%d_%H%M%S).txt 2>&1
```

**방법 B: 단계별 수동 실행 (안전)**

```sql
-- 1. hnt_sensor_info 중복 인덱스 삭제
ALTER TABLE hnt_sensor_info DROP INDEX hnt_sensor_info_no_IDX;
-- 예상 소요 시간: 1초

ALTER TABLE hnt_sensor_info DROP INDEX idx_sensor_info_user_sensor;
-- 예상 소요 시간: 1초

ALTER TABLE hnt_sensor_info DROP INDEX hnt_sensor_info_inst_dtm_IDX;
-- 예상 소요 시간: 1초

ALTER TABLE hnt_sensor_info DROP INDEX hnt_sensor_info_mdf_dtm_IDX;
-- 예상 소요 시간: 1초

ALTER TABLE hnt_sensor_info DROP INDEX hnt_sensor_info_sensor_id_IDX;
-- 예상 소요 시간: 1초

-- 2. hnt_user 중복 인덱스 삭제
ALTER TABLE hnt_user DROP INDEX hnt_user_inst_dtm_IDX;
-- 예상 소요 시간: 1초

ALTER TABLE hnt_user DROP INDEX hnt_user_mdf_dtm_IDX;
-- 예상 소요 시간: 1초

ALTER TABLE hnt_user DROP INDEX idx_hnt_user_parent_user_id;
-- 예상 소요 시간: 1초
```

### Step 3: 삭제 결과 확인

```sql
-- 인덱스 목록 확인
SHOW INDEX FROM hnt_sensor_info;
SHOW INDEX FROM hnt_user;

-- 테이블 크기 확인
SELECT 
    table_name,
    ROUND(((data_length) / 1024 / 1024), 2) AS data_size_mb,
    ROUND(((index_length) / 1024 / 1024), 2) AS index_size_mb,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS total_size_mb
FROM information_schema.TABLES
WHERE table_schema = 'hnt'
AND table_name IN ('hnt_sensor_info', 'hnt_user')
ORDER BY table_name;
```

**예상 결과**:
- hnt_sensor_info 인덱스 크기: 약 5-10 MB 감소
- hnt_user 인덱스 크기: 약 2-5 MB 감소

### Step 4: 쿼리 성능 재확인

```sql
-- getSensorList 쿼리 검증
EXPLAIN SELECT * FROM hnt_sensor_info WHERE user_id = 'thepine';
-- key: hnt_sensor_info_UN ✅

-- getUserListWithActivityStatus 쿼리 검증
EXPLAIN SELECT * FROM hnt_user WHERE del_yn = 'N' ORDER BY no DESC LIMIT 100;
-- key: idx_user_del_no ✅
```

### Step 5: 애플리케이션 재시작 및 테스트

```powershell
# 톰캣2 재시작
# Synology NAS Container Manager에서 재시작
```

**테스트 항목**:
1. ✅ 차트 페이지 로딩
2. ✅ 사용자 목록 페이지
3. ✅ 센서 추가/수정
4. ✅ 사용자 추가/수정

---

## 검증 및 모니터링

### 성능 모니터링 (24시간)

**1. Slow Query Log 확인**
```sql
-- Slow query 설정 확인
SHOW VARIABLES LIKE 'slow_query_log%';
SHOW VARIABLES LIKE 'long_query_time';

-- Slow query 개수 확인
SHOW GLOBAL STATUS LIKE 'Slow_queries';
```

**2. 인덱스 사용 통계**
```sql
-- 인덱스 히트율 확인
SHOW GLOBAL STATUS LIKE 'Handler_read%';
```

**3. 테이블 크기 모니터링**
```sql
SELECT 
    table_name,
    ROUND(((index_length) / 1024 / 1024), 2) AS index_size_mb
FROM information_schema.TABLES
WHERE table_schema = 'hnt'
AND table_name IN ('hnt_sensor_info', 'hnt_user');
```

### 애플리케이션 로그 모니터링

```bash
# 에러 로그 확인
tail -f logs/hnt-sensor-api.log | grep -i error

# DB 쿼리 로그 확인
tail -f logs/hnt-sensor-api-db.log

# 성능 로그 확인
tail -f logs/hnt-sensor-api-performance.log
```

---

## 롤백 절차

### Phase 1 롤백 (문제 발생 시)

```sql
-- 인덱스 삭제
ALTER TABLE hnt_sensor_info DROP INDEX idx_sensor_info_sensor_uuid;
ALTER TABLE hnt_user DROP INDEX idx_user_del_no;

-- 확인
SHOW INDEX FROM hnt_sensor_info;
SHOW INDEX FROM hnt_user;
```

**롤백 후 조치**:
1. 애플리케이션 재시작
2. 기능 테스트
3. 문제 원인 분석
4. 재시도 여부 결정

### Phase 2 롤백 (문제 발생 시)

```sql
-- hnt_sensor_info 인덱스 복원
CREATE INDEX hnt_sensor_info_no_IDX ON hnt_sensor_info(no);
CREATE INDEX idx_sensor_info_user_sensor ON hnt_sensor_info(user_id, sensor_id);
CREATE INDEX hnt_sensor_info_inst_dtm_IDX ON hnt_sensor_info(inst_dtm);
CREATE INDEX hnt_sensor_info_mdf_dtm_IDX ON hnt_sensor_info(mdf_dtm);
CREATE INDEX hnt_sensor_info_sensor_id_IDX ON hnt_sensor_info(sensor_id);

-- hnt_user 인덱스 복원
CREATE INDEX hnt_user_inst_dtm_IDX ON hnt_user(inst_dtm);
CREATE INDEX hnt_user_mdf_dtm_IDX ON hnt_user(mdf_dtm);
CREATE INDEX idx_hnt_user_parent_user_id ON hnt_user(parent_user_id);

-- 확인
SHOW INDEX FROM hnt_sensor_info;
SHOW INDEX FROM hnt_user;
```

### 전체 복원 (긴급 상황)

```bash
# 백업 파일에서 테이블 구조 복원
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 \
  < archive/sql_files/backup_tables_structure_YYYYMMDD_HHMMSS.sql
```

---

## FAQ

### Q1: Phase 1 실행 중 오류가 발생하면?

**A**: 
1. 오류 메시지 확인
2. 인덱스 생성 중단 시 롤백 불필요 (생성되지 않음)
3. 인덱스 생성 완료 후 오류 시 롤백 스크립트 실행
4. 애플리케이션 재시작
5. 문제 원인 분석 후 재시도

### Q2: Phase 2 실행 후 성능이 저하되면?

**A**:
1. 즉시 롤백 스크립트 실행 (인덱스 복원)
2. 애플리케이션 재시작
3. Slow query log 분석
4. 삭제한 인덱스 중 실제로 사용 중인 것이 있는지 확인
5. 필요한 인덱스만 선별적으로 복원

### Q3: 인덱스 추가 시 디스크 공간 부족 오류가 발생하면?

**A**:
1. 인덱스 생성 즉시 중단 (자동 롤백)
2. 디스크 공간 확보
3. 불필요한 파일 삭제
4. 재시도

### Q4: Phase 1과 Phase 2를 한 번에 실행해도 되나요?

**A**:
- **권장하지 않음**: Phase 1의 안정성을 1주일간 확인 후 Phase 2 실행
- 급한 경우: Phase 1 실행 후 24시간 모니터링 후 Phase 2 실행

### Q5: 백업 파일은 언제 삭제해도 되나요?

**A**:
- Phase 1 백업: Phase 2 완료 후 1개월
- Phase 2 백업: 완료 후 3개월
- 전체 백업: 최소 6개월 보관 권장

### Q6: 인덱스 추가로 INSERT/UPDATE 성능이 저하되나요?

**A**:
- Phase 1: 인덱스 2개 추가로 약 5% 성능 저하 (무시 가능)
- Phase 2: 인덱스 8개 삭제로 약 10-15% 성능 향상
- 최종 효과: 약 5-10% INSERT/UPDATE 성능 향상

### Q7: 테이블 락(lock)이 발생하나요?

**A**:
- MySQL MyISAM 엔진: 테이블 락 발생 (2~5초)
- InnoDB 엔진: 온라인 DDL (락 최소화)
- 권장: 트래픽 적은 시간대 실행

---

## 체크리스트

### Phase 1 실행 전

- [ ] 백업 파일 생성 완료
- [ ] 디스크 공간 확인 (최소 500 MB)
- [ ] 실행 시점 선택 (트래픽 적은 시간)
- [ ] SQL 파일 확인
- [ ] 롤백 스크립트 준비

### Phase 1 실행 후

- [ ] 인덱스 생성 확인 (SHOW INDEX)
- [ ] 쿼리 성능 확인 (EXPLAIN)
- [ ] 애플리케이션 재시작
- [ ] 기능 테스트 (차트, 사용자 목록)
- [ ] 24시간 모니터링

### Phase 2 실행 전

- [ ] Phase 1 정상 작동 확인 (1주일)
- [ ] 24시간 slow query log 분석
- [ ] 애플리케이션 에러 로그 확인
- [ ] 백업 파일 생성 완료
- [ ] 롤백 스크립트 준비

### Phase 2 실행 후

- [ ] 인덱스 삭제 확인 (SHOW INDEX)
- [ ] 테이블 크기 확인 (감소 확인)
- [ ] 쿼리 성능 확인 (EXPLAIN)
- [ ] 애플리케이션 재시작
- [ ] 기능 테스트 (모든 페이지)
- [ ] 24시간 모니터링
- [ ] INSERT/UPDATE 성능 확인

---

**최종 업데이트**: 2025-10-17  
**문의**: 시스템 관리자

