# 데이터베이스 최적화 가이드

## 개요
부계정 관련 쿼리의 성능을 최적화하기 위한 인덱스 생성 및 성능 검증 가이드입니다.

## 1. 인덱스 생성

### 1.1 기본 인덱스 생성
```bash
# MySQL에 접속하여 인덱스 생성
mysql -h hntsolution.co.kr -u root -p hnt < optimize_subaccount_queries.sql
```

### 1.2 생성되는 인덱스 목록
- **hnt_sensor_info**: 3개 인덱스
  - `idx_sensor_info_user_sensor_del`: 부계정 여부 확인 최적화
  - `idx_sensor_info_user_del`: 부계정의 메인 사용자 ID 조회 최적화
  - `idx_sensor_info_sensor_user_del`: 센서 목록 조회 최적화
- **hnt_user**: 2개 인덱스
  - `idx_user_parent_del`: 부계정 관계 확인 최적화
  - `idx_user_grade_del`: 사용자 등급별 조회 최적화
- **hnt_sensor_data**: 1개 인덱스
  - `idx_sensor_data_user_sensor_time`: 센서 데이터 조회 최적화
- **hnt_config**: 1개 인덱스
  - `idx_config_user_uuid_del`: 설정 정보 조회 최적화
- **hnt_alarm**: 1개 인덱스
  - `idx_alarm_user_uuid_del`: 알림 데이터 조회 최적화

## 2. 성능 검증

### 2.1 인덱스 사용 확인
```bash
# 인덱스 사용 통계 확인
mysql -h hntsolution.co.kr -u root -p hnt < verify_index_performance.sql
```

### 2.2 주요 검증 항목
1. **EXPLAIN 결과 확인**: 인덱스 사용 여부 확인
2. **실행 시간 측정**: 쿼리 성능 개선 확인
3. **인덱스 통계**: 인덱스 크기 및 사용률 확인
4. **카디널리티**: 인덱스 효율성 확인

## 3. 예상 성능 개선 효과

### 3.1 부계정 관련 쿼리
- **부계정 여부 확인**: 50-80% 성능 개선
- **메인 사용자 ID 조회**: 60-90% 성능 개선
- **센서 목록 조회**: 40-70% 성능 개선

### 3.2 데이터 조회 쿼리
- **센서 데이터 조회**: 30-60% 성능 개선
- **설정 정보 조회**: 50-80% 성능 개선
- **알림 데이터 조회**: 40-70% 성능 개선

## 4. 인덱스 관리

### 4.1 정기적 유지보수
```sql
-- 월별 통계 업데이트
ANALYZE TABLE hnt_sensor_info;
ANALYZE TABLE hnt_user;
ANALYZE TABLE hnt_sensor_data;
ANALYZE TABLE hnt_config;
ANALYZE TABLE hnt_alarm;
```

### 4.2 인덱스 모니터링
```sql
-- 인덱스 사용률 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    INDEX_TYPE
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
  AND INDEX_NAME LIKE 'idx_%'
ORDER BY TABLE_NAME, INDEX_NAME;
```

## 5. 주의사항

### 5.1 인덱스 생성 시
- **서버 부하**: 인덱스 생성 시 서버 부하 증가
- **테이블 락**: 대용량 테이블의 경우 테이블 락 발생 가능
- **디스크 공간**: 인덱스로 인한 디스크 공간 사용량 증가

### 5.2 인덱스 유지보수
- **정기적 통계 업데이트**: 쿼리 최적화를 위한 통계 정보 갱신
- **인덱스 재구성**: 필요시 인덱스 재구성 고려
- **사용하지 않는 인덱스**: 사용하지 않는 인덱스는 제거 고려

## 6. 문제 해결

### 6.1 인덱스 생성 실패
```sql
-- 기존 인덱스 확인
SHOW INDEX FROM hnt_sensor_info;

-- 인덱스 삭제 후 재생성
DROP INDEX idx_sensor_info_user_sensor_del ON hnt_sensor_info;
CREATE INDEX idx_sensor_info_user_sensor_del ON hnt_sensor_info (user_id, sensor_id, del_yn);
```

### 6.2 성능 개선 미흡
- **쿼리 최적화**: 인덱스 외에 쿼리 자체 최적화 검토
- **테이블 구조**: 테이블 구조 개선 검토
- **하드웨어**: 서버 하드웨어 성능 검토

## 7. 모니터링

### 7.1 성능 모니터링
- **쿼리 실행 시간**: 정기적 쿼리 성능 측정
- **인덱스 사용률**: 인덱스 사용 통계 모니터링
- **서버 리소스**: CPU, 메모리, 디스크 사용률 모니터링

### 7.2 알림 설정
- **쿼리 실행 시간**: 임계값 초과 시 알림
- **인덱스 사용률**: 낮은 사용률 인덱스 알림
- **서버 리소스**: 높은 리소스 사용률 알림
