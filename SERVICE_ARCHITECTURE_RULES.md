# 서비스 아키텍처 구조 룰

## 개요
HnT Sensor API 프로젝트의 서비스 계층 구조와 사용 규칙을 정의합니다.

## 서비스 계층 구조

### 1. 서비스 인터페이스 (Interface Layer)
```
com.andrew.hnt.api.service/
├── LoginService.java                    # 로그인/사용자 관리
├── DataService.java                     # 데이터 조회/처리
├── AdminService.java                    # 관리자 기능
├── MqttService.java                     # MQTT 통신
├── SubAccountService.java               # 부계정 관리
├── SessionManagementService.java        # 세션 관리 (기본)
├── SessionSecurityService.java          # 세션 보안
├── SessionValidationService.java        # 세션 검증
└── UnifiedSessionService.java           # 통합 세션 관리 (신규)
```

### 2. 서비스 구현체 (Implementation Layer)
```
com.andrew.hnt.api.service.impl/
├── LoginServiceImpl.java                # 로그인 서비스 구현
├── DataServiceImpl.java                 # 데이터 서비스 구현
├── AdminServiceImpl.java                # 관리자 서비스 구현
├── MqttServiceImpl.java                 # MQTT 서비스 구현
├── SubAccountServiceImpl.java           # 부계정 서비스 구현
├── SessionManagementServiceImpl.java    # 세션 관리 구현
├── SessionSecurityServiceImpl.java      # 세션 보안 구현
├── SessionValidationServiceImpl.java    # 세션 검증 구현
└── UnifiedSessionServiceImpl.java       # 통합 세션 관리 구현
```

### 3. 컨트롤러 계층 (Controller Layer)
```
com.andrew.hnt.api.controller/
├── MainController.java                  # 메인 대시보드
├── AdminController.java                 # 관리자 기능
├── LoginController.java                 # 로그인/회원가입
├── DataController.java                  # 데이터 처리
├── ChartController.java                 # 차트 관련
├── CommonController.java                # 공통 기능 (Component)
├── ApiController.java                   # REST API (신규)
├── UnifiedSessionController.java        # 통합 세션 예제 (신규)
├── DefaultController.java               # 기본 컨트롤러
└── TestController.java                  # 테스트용
```

### 4. 매퍼 계층 (Mapper Layer)
```
com.andrew.hnt.api.mapper/
├── LoginMapper.java                     # 사용자 관련 DB 작업
├── DataMapper.java                      # 센서 데이터 DB 작업
├── AdminMapper.java                     # 관리자 기능 DB 작업
└── MqttMapper.java                      # MQTT 관련 DB 작업
```

## 서비스 의존성 관계

### 1. 통합 세션 관리 서비스 (UnifiedSessionService)
- **역할**: 모든 세션 관련 기능을 통합하여 제공
- **의존성**: 
  - `SessionManagementService` (기본 세션 관리)
  - `SessionSecurityService` (세션 보안)
  - `SessionValidationService` (세션 검증)
- **사용처**: 모든 컨트롤러에서 세션 검증 및 권한 관리

### 2. 세션 관리 서비스들 (기존)
- **SessionManagementService**: 기본 세션 정보 관리
- **SessionSecurityService**: 세션 보안 검증 (IP, User-Agent 등)
- **SessionValidationService**: 세션 유효성 검증 및 권한 체크

### 3. 비즈니스 서비스들
- **LoginService**: 사용자 인증, 회원가입, 사용자 관리
- **DataService**: 센서 데이터 조회, 통계, 엑셀 다운로드
- **AdminService**: 관리자 기능, 센서 설정, 사용자 관리
- **MqttService**: MQTT 통신, 실시간 데이터 처리
- **SubAccountService**: 부계정 관리

### 4. 공통 서비스들
- **CommonController**: 사이드바 데이터, 세션 정보 설정
- **UnifiedErrorHandler**: 통합 에러 처리 및 응답 표준화

## 서비스 사용 패턴

### 1. 컨트롤러에서 서비스 사용
```java
@Controller
public class ExampleController {
    @Autowired
    private UnifiedSessionService unifiedSessionService;  // 통합 세션 관리
    @Autowired
    private LoginService loginService;                    // 사용자 관리
    @Autowired
    private DataService dataService;                      // 데이터 처리
    @Autowired
    private CommonController commonController;            // 공통 기능
}
```

### 2. 서비스 간 의존성
```java
@Service
public class UnifiedSessionServiceImpl implements UnifiedSessionService {
    @Autowired
    private SessionManagementService sessionManagementService;
    @Autowired
    private SessionSecurityService sessionSecurityService;
    @Autowired
    private SessionValidationService sessionValidationService;
}
```

### 3. 매퍼 사용 패턴
```java
@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private LoginMapper loginMapper;
    
    // 서비스 로직에서 매퍼 호출
    public UserInfo getUserInfoByUserId(String userId) {
        return loginMapper.getUserInfoByUserId(userId);
    }
}
```

## 서비스 계층별 책임

### 1. 컨트롤러 계층 (Controller Layer)
- **책임**: HTTP 요청/응답 처리, 뷰 렌더링
- **특징**: 
  - `@Controller` 또는 `@RestController` 어노테이션
  - 서비스 계층에 비즈니스 로직 위임
  - 세션 검증 및 권한 체크

### 2. 서비스 계층 (Service Layer)
- **책임**: 비즈니스 로직 처리, 트랜잭션 관리
- **특징**:
  - `@Service` 어노테이션
  - 인터페이스와 구현체 분리
  - 매퍼 계층에 데이터 접근 위임

### 3. 매퍼 계층 (Mapper Layer)
- **책임**: 데이터베이스 접근, SQL 실행
- **특징**:
  - `@Mapper` 어노테이션
  - MyBatis XML 매퍼와 연결
  - 단순한 CRUD 작업

## 서비스 통합 규칙

### 1. 통합 세션 관리 서비스 우선 사용
- **새로운 컨트롤러**: `UnifiedSessionService` 사용
- **기존 컨트롤러**: 점진적으로 `UnifiedSessionService`로 마이그레이션
- **세션 검증**: `validateSession()` 한 번 호출로 모든 처리

### 2. 서비스 간 의존성 최소화
- **순환 의존성 방지**: A → B → A 형태의 의존성 금지
- **인터페이스 의존**: 구현체가 아닌 인터페이스에 의존
- **단방향 의존성**: 상위 계층에서 하위 계층으로만 의존

### 3. 에러 처리 표준화
- **GlobalExceptionHandler**: 모든 예외를 중앙에서 처리
- **UnifiedErrorHandler**: 표준화된 에러 응답 형식
- **커스텀 예외**: 비즈니스 로직별 예외 클래스 사용

## 서비스 성능 최적화

### 1. 데이터베이스 최적화
- **SlowQueryInterceptor**: 느린 쿼리 자동 감지
- **인덱스 활용**: 기존 인덱스 최대한 활용
- **쿼리 최적화**: N+1 쿼리 문제 해결

### 2. 세션 관리 최적화
- **통계 수집**: 세션 검증 성능 모니터링
- **캐싱**: 세션 정보 캐싱 활용
- **연결 풀**: HikariCP 최적화

### 3. MQTT 통신 최적화
- **필터링**: 5단계 메시지 필터링 시스템
- **재연결**: 지수 백오프 재연결 알고리즘
- **상태 모니터링**: 실시간 연결 상태 확인

## 서비스 사용 가이드

### 1. 세션 검증 (권장 방식)
```java
// 통합 세션 검증 사용 (권장)
SessionValidationResult result = unifiedSessionService.validateSession(
    session, req, model, "A"); // 관리자 권한 필요
if (!result.isValid()) {
    return result.getRedirectUrl();
}
String userId = result.getUserId();
```

### 2. 권한 검증
```java
// 권한별 검증 메서드 사용
boolean isAdmin = unifiedSessionService.isAdmin(session);
boolean canManageDevices = unifiedSessionService.canManageDevices(session);
boolean canManageUsers = unifiedSessionService.canManageUsers(session);
```

### 3. 에러 처리
```java
// GlobalExceptionHandler가 자동으로 처리
// 별도의 try-catch 불필요
public String exampleMethod() {
    // 비즈니스 로직 수행
    return "success";
}
```

### 4. 데이터 조회
```java
// 서비스 계층을 통한 데이터 조회
List<Map<String, Object>> dataList = dataService.selectSensorData(param);
```

## 마이그레이션 가이드

### 1. 기존 컨트롤러 수정
```java
// 기존 방식 (복잡함)
SessionValidationService.ValidationResult validationResult = 
    sessionValidationService.validateSession(session, req, model, "A");
if (!validationResult.isValid()) {
    return "redirect:" + validationResult.getRedirectUrl();
}
if (!sessionSecurityService.validateSessionSecurity(session, request)) {
    return "redirect:/login/login";
}
if (!sessionManagementService.hasPermission(session, "A")) {
    return "redirect:/main/main";
}

// 통합 방식 (간단함)
SessionValidationResult result = unifiedSessionService.validateSession(
    session, req, model, "A");
if (!result.isValid()) {
    return result.getRedirectUrl();
}
```

### 2. 점진적 마이그레이션
- **1단계**: 새로운 컨트롤러에 `UnifiedSessionService` 적용
- **2단계**: 기존 컨트롤러를 하나씩 마이그레이션
- **3단계**: 기존 세션 관리 서비스 정리

### 3. 테스트 및 검증
- **단위 테스트**: 각 서비스별 단위 테스트
- **통합 테스트**: 컨트롤러-서비스 통합 테스트
- **성능 테스트**: 세션 검증 성능 테스트

## 주의사항

### 1. 서비스 계층 분리
- **컨트롤러**: HTTP 처리만 담당
- **서비스**: 비즈니스 로직 담당
- **매퍼**: 데이터 접근만 담당

### 2. 의존성 관리
- **순환 의존성 방지**: A → B → A 형태 금지
- **인터페이스 의존**: 구현체가 아닌 인터페이스에 의존
- **단방향 의존성**: 상위 계층에서 하위 계층으로만 의존

### 3. 트랜잭션 관리
- **서비스 계층**: `@Transactional` 어노테이션 사용
- **읽기 전용**: `@Transactional(readOnly = true)` 사용
- **쓰기 작업**: `@Transactional` 사용

### 4. 에러 처리
- **GlobalExceptionHandler**: 모든 예외를 중앙에서 처리
- **커스텀 예외**: 비즈니스 로직별 예외 클래스 사용
- **로깅**: 모든 에러를 로그로 기록

## 결론

이 서비스 아키텍처 구조를 따라 개발하면:
- **코드 중복 제거**: 통합된 서비스 사용
- **유지보수성 향상**: 계층별 책임 분리
- **성능 최적화**: 통계 수집 및 모니터링
- **개발 생산성 향상**: 표준화된 패턴 사용
- **에러 처리 표준화**: 중앙화된 예외 처리
