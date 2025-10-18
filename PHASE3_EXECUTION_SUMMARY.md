# Phase 3 인덱스 최적화 실행 가이드

**작성일**: 2025-10-17  
**상태**: Phase 1+2 완료, Phase 3 준비 완료  
**다음 단계**: 즉시 실행 가능

---

## 🎯 빠른 실행 가이드

### 1️⃣ 즉시 실행 (오늘 - 2025-10-17)

#### Step 1: no 인덱스 삭제 (100MB 절감)
```bash
# Windows PowerShell에서 실행
cd D:\Project\SW\CursorAI\tomcat22

# MySQL 접속 및 삭제 실행
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 -e "ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_no_IDX;"

# 결과 확인
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 -e "SHOW INDEX FROM hnt_sensor_data WHERE Key_name = 'hnt_sensor_data_no_IDX';"
```

**예상 결과**: Empty set (인덱스 삭제 성공)

---

#### Step 2: 24시간 모니터링 설정
```bash
# 슬로우 쿼리 로그 활성화
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 << 'EOF'
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.5;
SET GLOBAL log_queries_not_using_indexes = 'ON';
SHOW VARIABLES LIKE 'slow_query_log%';
EOF
```

---

#### Step 3: 현재 상태 스냅샷 저장
```bash
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 << 'EOF'
CREATE TABLE IF NOT EXISTS hnt_index_stats_before (
    snapshot_time DATETIME,
    index_name VARCHAR(100),
    cardinality BIGINT,
    index_type VARCHAR(50),
    columns TEXT
);

INSERT INTO hnt_index_stats_before
SELECT 
    NOW() as snapshot_time,
    INDEX_NAME,
    MAX(CARDINALITY) as cardinality,
    INDEX_TYPE,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hnt' 
  AND TABLE_NAME = 'hnt_sensor_data'
GROUP BY INDEX_NAME, INDEX_TYPE;

SELECT * FROM hnt_index_stats_before;
EOF
```

---

### 2️⃣ 24시간 후 실행 (2025-10-18)

#### Step 4: 모니터링 결과 분석
```bash
# 인덱스 사용 통계 확인
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 << 'EOF'
SELECT 
    INDEX_NAME,
    COUNT_STAR as total_access,
    COUNT_READ as total_read,
    ROUND(SUM_TIMER_WAIT/1000000000000, 2) as total_latency_sec
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_SCHEMA = 'hnt' 
  AND OBJECT_NAME = 'hnt_sensor_data'
  AND INDEX_NAME IS NOT NULL
ORDER BY total_access DESC;
EOF
```

---

#### Step 5: Phase 3 실행 결정

**시나리오 A: 최소 최적화 (안전)**
- 중복 inst_dtm 인덱스 하나만 삭제
- 예상 효과: +300MB 절감

```bash
# 사용 빈도 낮은 인덱스 삭제 (모니터링 결과 기반)
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 -e "ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_inst_dtm_IDX;"
```

**시나리오 B: 중간 최적화 (권장)**
- 중복 inst_dtm 인덱스 하나 삭제
- sensor_value 인덱스 삭제 (미사용 시)
- 예상 효과: +400MB 절감

```bash
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 << 'EOF'
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_inst_dtm_IDX;
ALTER TABLE hnt_sensor_data DROP INDEX hnt_sensor_data_sensor_value_IDX;
EOF
```

**시나리오 C: 최대 최적화 (공격적)**
- 중복 inst_dtm 인덱스 하나 삭제
- sensor_value 인덱스 삭제
- idx_sensor_data_performance 재설계
- 예상 효과: +2,300MB 절감

---

## 📊 현재 상태 요약

### Phase 1+2 완료 (2025-10-17)
| 항목 | 값 |
|------|-----|
| 삭제된 인덱스 | 5개 |
| 절감 크기 | 3,270 MB |
| 남은 인덱스 | 11개 |
| 현재 총 크기 | 8,499 MB |

### Phase 3 예상 (2025-10-18)
| 시나리오 | 삭제 인덱스 | 절감 크기 | 최종 크기 |
|---------|------------|----------|----------|
| **최소** | 1개 | +300 MB | 8,199 MB |
| **중간** | 2개 | +400 MB | 8,099 MB |
| **최대** | 4개 | +2,300 MB | 6,199 MB |

---

## 🔍 핵심 발견사항

### ✅ 최적 활용 인덱스 (유지)
1. `idx_hnt_sensor_data_uuid_inst_dtm` (uuid, inst_dtm)
2. `idx_hnt_sensor_data_user_id_uuid` (user_id, uuid)
3. `PRIMARY`, `hnt_sensor_data_UN`, `hnt_sensor_data_2`

### ❌ 즉시 삭제 (완료/예정)
1. ✅ `hnt_sensor_data_no_IDX` (PRIMARY KEY와 중복) - 100MB

### ⚠️ 검토 후 삭제 (24시간 후)
1. `hnt_sensor_data_inst_dtm_IDX` 또는 `idx_hnt_sensor_data_inst_dtm` - 300MB
2. `hnt_sensor_data_sensor_value_IDX` (조건부) - 100MB
3. `idx_sensor_data_performance` (재설계 검토) - 1,800MB

---

## 📋 체크리스트

### 오늘 (2025-10-17)
- [x] Phase 1+2 분석 보고서 작성
- [x] 코드 인덱스 활용 현황 검토
- [x] Phase 3 실행 계획 수립
- [ ] no 인덱스 삭제 실행
- [ ] 24시간 모니터링 설정

### 내일 (2025-10-18)
- [ ] 모니터링 결과 분석
- [ ] Phase 3 시나리오 결정
- [ ] Phase 3 인덱스 삭제 실행
- [ ] 성능 검증 및 최종 보고서 작성

### 2주 내
- [ ] raw_data LIKE 쿼리 최적화
- [ ] idx_sensor_data_performance 재설계 검토
- [ ] 최종 성능 측정 및 문서화

---

## 🎯 성공 기준

### 정량적 목표
- ✅ Phase 1+2: 3.3 GB 절감 (달성)
- ⏳ Phase 3: 추가 0.3~2.3 GB 절감 (예정)
- 📊 최종 목표: 총 3.6~5.6 GB 절감 (-31~48%)

### 정성적 목표
- ✅ 코드 주석 업데이트
- ✅ 인덱스 활용 현황 문서화
- ⏳ 쿼리 성능 개선
- ⏳ 유지보수성 향상

---

## 📞 문제 발생 시

### 롤백 방법
```bash
# no 인덱스 복구
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 -e "CREATE INDEX hnt_sensor_data_no_IDX ON hnt_sensor_data(no);"

# inst_dtm 인덱스 복구
mysql -h hntsolution.co.kr -u root -pHntRoot123! hnt --default-character-set=utf8 -e "CREATE INDEX hnt_sensor_data_inst_dtm_IDX ON hnt_sensor_data(inst_dtm);"
```

### 성능 문제 발생 시
1. 슬로우 쿼리 로그 확인
2. EXPLAIN으로 쿼리 실행 계획 분석
3. 필요 시 인덱스 재생성

---

## 📚 관련 문서

1. **CODE_INDEX_OPTIMIZATION_REVIEW_REPORT.md** - 종합 보고서
2. **INDEX_USAGE_ANALYSIS_REPORT.md** - 상세 분석
3. **archive/sql_files/phase3_no_index_deletion_20251017.sql** - 삭제 스크립트
4. **archive/sql_files/phase3_monitoring_setup_20251017.sql** - 모니터링 스크립트
5. **archive/sql_files/optimize_raw_data_queries.sql** - 쿼리 최적화 가이드

---

**준비 완료!** 이제 Step 1부터 실행하시면 됩니다. 🚀

