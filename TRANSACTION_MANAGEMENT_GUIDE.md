# 🔄 트랜잭션 관리 가이드

## 📋 개요

HnT Sensor API 프로젝트의 데이터베이스 트랜잭션 시퀀스 문제를 해결하기 위한 통합 트랜잭션 관리 시스템입니다.

## 🎯 해결된 문제점

### **1. 트랜잭션 경계 불명확 문제**
- **기존**: MQTT 메시지 처리 시 트랜잭션 없음
- **해결**: `TransactionManagementService`를 통한 통합 트랜잭션 관리

### **2. 배치 처리 트랜잭션 문제**
- **기존**: 개별 메시지 처리로 인한 부분 실패 시 롤백 불가
- **해결**: 배치 단위 트랜잭션으로 전체 롤백 보장

### **3. 트랜잭션 설정 불일치**
- **기존**: 서비스별로 다른 트랜잭션 설정
- **해결**: 통합 트랜잭션 서비스로 일관된 설정

## 🏗️ 아키텍처

### **트랜잭션 관리 계층**

```
┌─────────────────────────────────────┐
│           Controller Layer          │
├─────────────────────────────────────┤
│     TransactionManagementService    │ ← 통합 트랜잭션 관리
├─────────────────────────────────────┤
│         Service Layer               │
├─────────────────────────────────────┤
│         Mapper Layer                │
├─────────────────────────────────────┤
│        Database Layer               │
└─────────────────────────────────────┘
```

### **주요 컴포넌트**

| 컴포넌트 | 역할 | 트랜잭션 설정 |
|----------|------|---------------|
| **TransactionManagementService** | 통합 트랜잭션 관리 | `@Transactional(rollbackFor = Exception.class)` |
| **MqttMessageProcessor** | MQTT 메시지 배치 처리 | 트랜잭션 서비스 사용 |
| **AdminController** | 관리자 기능 | 트랜잭션 서비스 사용 |
| **DataController** | 데이터 처리 | 트랜잭션 서비스 사용 |

## 🔧 트랜잭션 설정

### **1. 트랜잭션 타임아웃 설정**

| 작업 유형 | 타임아웃 | 이유 |
|-----------|----------|------|
| **센서 데이터 저장** | 30초 | 일반적인 데이터 저장 작업 |
| **장치 등록** | 60초 | 기존 데이터 삭제 + 새 데이터 등록 |
| **사용자 삭제** | 60초 | 다중 테이블 삭제 작업 |
| **센서 데이터 삭제** | 60초 | 완전 삭제 작업 |

### **2. 롤백 정책**

```java
@Transactional(rollbackFor = Exception.class, timeout = 30)
```

- **모든 예외에 대해 롤백**: `rollbackFor = Exception.class`
- **런타임 예외 자동 롤백**: Spring 기본 동작
- **체크 예외도 롤백**: 명시적 설정

### **3. 트랜잭션 전파**

| 메서드 | 전파 설정 | 설명 |
|--------|-----------|------|
| **saveSensorDataBatch** | `REQUIRED` (기본) | 새 트랜잭션 생성 또는 기존 트랜잭션 참여 |
| **registerDeviceWithTransaction** | `REQUIRED` (기본) | 장치 등록 전체를 하나의 트랜잭션으로 처리 |
| **deleteUserWithTransaction** | `REQUIRED` (기본) | 사용자 삭제 전체를 하나의 트랜잭션으로 처리 |

## 📊 트랜잭션 모니터링

### **1. 통계 정보**

```json
{
  "totalTransactions": 1250,
  "successfulTransactions": 1245,
  "failedTransactions": 5,
  "rollbackCount": 3,
  "successRate": 99.6,
  "timestamp": 1703123456789
}
```

### **2. 모니터링 API**

| API | 경로 | 설명 |
|-----|------|------|
| **상태 조회** | `GET /api/transaction/status` | 현재 트랜잭션 상태 |
| **통계 조회** | `GET /api/transaction/stats` | 트랜잭션 통계 정보 |
| **헬스 체크** | `GET /api/transaction/health` | 트랜잭션 서비스 상태 |

### **3. 로그 레벨**

```yaml
logging:
  level:
    com.andrew.hnt.api.service.TransactionManagementService: DEBUG
    com.andrew.hnt.api.mqtt.MqttMessageProcessor: INFO
    org.springframework.transaction: DEBUG
```

## 🚀 사용 방법

### **1. MQTT 메시지 배치 처리**

```java
// MqttMessageProcessor에서 자동 호출
Map<String, Object> result = transactionManagementService.saveSensorDataBatch(batch);
```

### **2. 장치 등록**

```java
// AdminController에서 호출
Map<String, Object> result = transactionManagementService.registerDeviceWithTransaction(param);
```

### **3. 사용자 삭제**

```java
// AdminController에서 호출
Map<String, Object> result = transactionManagementService.deleteUserWithTransaction(userId, userGrade);
```

## ⚠️ 주의사항

### **1. 트랜잭션 경계**

- **트랜잭션 내에서 외부 API 호출 금지**: 롤백 불가능
- **대용량 데이터 처리 시 배치 크기 조정**: 메모리 사용량 고려
- **장시간 실행 작업은 비동기 처리**: 타임아웃 방지

### **2. 성능 최적화**

- **배치 크기 조정**: `BATCH_SIZE = 200` (현재 설정)
- **타임아웃 설정**: 작업 유형별 적절한 타임아웃
- **연결 풀 설정**: HikariCP 최적화

### **3. 에러 처리**

- **트랜잭션 롤백 시 로그 기록**: 디버깅을 위한 상세 로그
- **재시도 로직**: 일시적 오류에 대한 재시도
- **알림 시스템**: 트랜잭션 실패 시 관리자 알림

## 🔍 문제 해결

### **1. 트랜잭션 타임아웃**

```java
// 로그 확인
logger.error("트랜잭션 타임아웃 발생 - 작업: {}", operation);

// 해결 방법
// 1. 타임아웃 시간 증가
// 2. 배치 크기 감소
// 3. 작업 분할
```

### **2. 데드락 발생**

```java
// 로그 확인
logger.error("데드락 발생 - 테이블: {}, 락: {}", table, lock);

// 해결 방법
// 1. 테이블 접근 순서 통일
// 2. 인덱스 최적화
// 3. 트랜잭션 범위 축소
```

### **3. 메모리 부족**

```java
// 로그 확인
logger.error("메모리 부족 - 배치 크기: {}", batchSize);

// 해결 방법
// 1. 배치 크기 감소
// 2. JVM 힙 메모리 증가
// 3. 가비지 컬렉션 최적화
```

## 📈 성능 모니터링

### **1. 트랜잭션 성능 지표**

- **평균 처리 시간**: 배치당 처리 시간
- **처리량**: 초당 처리 메시지 수
- **에러율**: 실패한 트랜잭션 비율
- **롤백율**: 롤백된 트랜잭션 비율

### **2. 데이터베이스 성능**

- **연결 풀 사용률**: HikariCP 모니터링
- **쿼리 실행 시간**: MyBatis 로그 분석
- **락 대기 시간**: 데이터베이스 락 모니터링

### **3. 시스템 리소스**

- **CPU 사용률**: 트랜잭션 처리 중 CPU 사용량
- **메모리 사용률**: 힙 메모리 사용량
- **디스크 I/O**: 데이터베이스 I/O 성능

## 🎯 향후 개선 계획

### **1. 트랜잭션 최적화**

- **분산 트랜잭션**: 마이크로서비스 환경 대응
- **비동기 처리**: 대용량 데이터 처리 최적화
- **캐싱 전략**: 자주 사용되는 데이터 캐싱

### **2. 모니터링 강화**

- **실시간 대시보드**: 트랜잭션 상태 실시간 모니터링
- **알림 시스템**: 이상 상황 자동 알림
- **성능 분석**: 트랜잭션 성능 분석 도구

### **3. 확장성 개선**

- **수평 확장**: 다중 인스턴스 지원
- **로드 밸런싱**: 트랜잭션 부하 분산
- **장애 복구**: 자동 장애 복구 시스템

---

## 📚 참고 자료

- [Spring Transaction Management](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [MyBatis Transaction](https://mybatis.org/spring/transactions.html)
