# 통합 세션 관리 서비스 가이드

## 개요

`UnifiedSessionService`는 기존의 분산된 세션 관리 로직을 하나의 통합된 서비스로 제공합니다. 이를 통해 코드 중복을 제거하고 세션 관리를 표준화할 수 있습니다.

## 주요 기능

### 1. 통합 세션 검증
- **기본 세션 검증**: 세션 유효성, 사용자 정보 확인
- **보안 검증**: IP 주소, User-Agent 등 보안 요소 검증
- **권한 검증**: 사용자 등급별 권한 확인
- **모델 설정**: JSP 모델에 세션 정보 자동 설정

### 2. 권한 관리
- **관리자 권한**: `isAdmin()` - A 등급 확인
- **일반 사용자 권한**: `isUser()` - A, U 등급 확인
- **부계정 권한**: `isSubAccount()` - B 등급 확인
- **장치 관리 권한**: `canManageDevices()` - A, U 등급만 장치 삭제/수정 가능
- **사용자 관리 권한**: `canManageUsers()` - A 등급만 사용자 관리 가능

### 3. 세션 통계
- **검증 통계**: 총 검증 횟수, 성공/실패 횟수, 성공률
- **권한 검증 통계**: 권한 검증 횟수
- **보안 검증 통계**: 보안 검증 횟수

## 사용 방법

### 1. 기본 사용법

```java
@Autowired
private UnifiedSessionService unifiedSessionService;

@RequestMapping(value = "/example", method = RequestMethod.GET)
public String examplePage(HttpServletRequest req, HttpServletResponse res, Model model) {
    HttpSession session = req.getSession();
    
    // 통합 세션 검증 (기본 + 보안 + 권한 + 모델 설정)
    SessionValidationResult result = unifiedSessionService.validateSession(
        session, req, model, "A"); // 관리자 권한 필요
    
    if (!result.isValid()) {
        return result.getRedirectUrl();
    }
    
    // 검증 성공 시 사용자 정보 사용
    String userId = result.getUserId();
    String userGrade = result.getUserGrade();
    String userNm = result.getUserNm();
    
    // 비즈니스 로직 수행
    return "example/page";
}
```

### 2. 권한별 사용법

#### 관리자 페이지
```java
// 관리자 권한 필요
SessionValidationResult result = unifiedSessionService.validateSession(
    session, req, model, "A");
```

#### 일반 사용자 페이지
```java
// 일반 사용자 권한 필요 (A, U 등급)
SessionValidationResult result = unifiedSessionService.validateSession(
    session, req, model, "U");
```

#### 부계정 페이지
```java
// 부계정 권한 필요 (B 등급)
SessionValidationResult result = unifiedSessionService.validateSession(
    session, req, model, "B");
```

#### 권한 검증 없음
```java
// 권한 검증 없이 기본 세션 검증만
SessionValidationResult result = unifiedSessionService.validateSession(
    session, req, model);
```

### 3. API에서 사용법

```java
@RequestMapping(value = "/api/example", method = RequestMethod.POST)
public @ResponseBody Map<String, Object> exampleApi(HttpServletRequest req) {
    Map<String, Object> resultMap = new HashMap<String, Object>();
    HttpSession session = req.getSession();
    
    // 통합 세션 검증 (모델 설정 없음)
    SessionValidationResult result = unifiedSessionService.validateSession(
        session, req, "U");
    
    if (!result.isValid()) {
        resultMap.put("resultCode", "999");
        resultMap.put("resultMessage", "세션이 유효하지 않습니다.");
        return resultMap;
    }
    
    // 비즈니스 로직 수행
    resultMap.put("resultCode", "200");
    resultMap.put("resultMessage", "성공");
    
    return resultMap;
}
```

## 기존 코드 마이그레이션

### 1. 기존 방식 (복잡함)

```java
// 기존 방식 - 여러 서비스 호출
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

sessionManagementService.setSessionInfoToModel(session, model);
String userId = validationResult.getUserId();
```

### 2. 통합 방식 (간단함)

```java
// 통합 방식 - 한 번 호출로 모든 처리
SessionValidationResult result = unifiedSessionService.validateSession(
    session, req, model, "A");
if (!result.isValid()) {
    return result.getRedirectUrl();
}
String userId = result.getUserId();
```

## 권한 검증 메서드

### 1. 기본 권한 검증
```java
// 특정 권한 확인
boolean hasPermission = unifiedSessionService.hasPermission(session, "A");

// 관리자 권한 확인
boolean isAdmin = unifiedSessionService.isAdmin(session);

// 일반 사용자 권한 확인
boolean isUser = unifiedSessionService.isUser(session);

// 부계정 권한 확인
boolean isSubAccount = unifiedSessionService.isSubAccount(session);
```

### 2. 기능별 권한 검증
```java
// 장치 관리 권한 확인 (A, U 등급만 장치 삭제/수정 가능)
boolean canManageDevices = unifiedSessionService.canManageDevices(session);

// 사용자 관리 권한 확인 (A 등급만 사용자 관리 가능)
boolean canManageUsers = unifiedSessionService.canManageUsers(session);
```

## 세션 통계 모니터링

### 1. 세션 통계 조회
```java
// 세션 통계 조회
Map<String, Object> sessionStats = unifiedSessionService.getSessionStats();
Map<String, Object> validationStats = unifiedSessionService.getValidationStats();
```

### 2. API를 통한 통계 조회
```bash
# 세션 관리 상태 확인
curl -X GET http://localhost:8888/api/session/status

# 응답 예시
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "세션 관리 상태 조회 성공",
  "timestamp": 1703123456789,
  "sessionStats": {
    "totalValidations": 150,
    "successfulValidations": 145,
    "failedValidations": 5,
    "permissionChecks": 120,
    "securityChecks": 150,
    "successRate": 96.7
  },
  "validationStats": {
    "totalValidations": 150,
    "successfulValidations": 145,
    "failedValidations": 5,
    "permissionChecks": 120,
    "securityChecks": 150
  }
}
```

## 에러 처리

### 1. 검증 실패 시
```java
SessionValidationResult result = unifiedSessionService.validateSession(session, req, model, "A");
if (!result.isValid()) {
    // result.getRedirectUrl() - 리다이렉트 URL
    // result.getErrorMessage() - 에러 메시지
    return result.getRedirectUrl();
}
```

### 2. 권한 부족 시
```java
// 권한 부족 시 자동으로 /main/main으로 리다이렉트
SessionValidationResult result = unifiedSessionService.validateSession(session, req, model, "A");
// A 권한이 없으면 자동으로 /main/main으로 리다이렉트
```

## 성능 최적화

### 1. 통계 수집
- 모든 세션 검증, 권한 검증, 보안 검증이 자동으로 통계 수집
- 성능 모니터링을 통한 병목 지점 식별 가능

### 2. 캐싱
- 세션 정보는 Spring의 세션 관리 기능을 활용
- 불필요한 DB 조회 최소화

### 3. 로깅
- 모든 검증 과정이 로그로 기록
- 디버깅 및 모니터링 용이

## 마이그레이션 체크리스트

- [ ] 기존 컨트롤러에서 `SessionValidationService` 대신 `UnifiedSessionService` 사용
- [ ] 권한 검증 로직을 `validateSession()`에 통합
- [ ] 모델 설정 로직을 `validateSession()`에 통합
- [ ] 에러 처리 로직을 통합된 방식으로 변경
- [ ] 세션 통계 모니터링 추가
- [ ] 기존 세션 관리 서비스 의존성 정리

## 예제 페이지

통합 세션 관리 서비스의 사용 예제는 다음 페이지에서 확인할 수 있습니다:

- **관리자 페이지**: `/unified/admin`
- **사용자 페이지**: `/unified/user`
- **공개 페이지**: `/unified/public`
- **권한 검증 API**: `/unified/check-permission`
- **세션 통계 API**: `/unified/session-stats`

## 주의사항

1. **기존 서비스와의 호환성**: 기존 `SessionManagementService`, `SessionSecurityService`, `SessionValidationService`는 여전히 사용 가능
2. **점진적 마이그레이션**: 모든 컨트롤러를 한 번에 변경할 필요 없이 점진적으로 마이그레이션 가능
3. **에러 처리**: 통합 서비스 사용 시 기존 에러 처리 로직을 확인하고 필요시 수정
4. **테스트**: 마이그레이션 후 충분한 테스트 수행

## 결론

`UnifiedSessionService`를 사용하면:
- **코드 중복 제거**: 여러 서비스 호출을 하나로 통합
- **유지보수성 향상**: 중앙화된 세션 관리 로직
- **성능 모니터링**: 통합된 통계 수집
- **일관된 에러 처리**: 표준화된 에러 응답
- **개발 생산성 향상**: 간단한 API 사용
