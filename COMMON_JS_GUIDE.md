# 공통 JavaScript 파일 사용 가이드

## 📋 개요

이 문서는 HnT Sensor API 프로젝트의 공통 JavaScript 파일 사용 규칙과 가이드를 제공합니다.

## 🗂️ 공통 파일 구조

### 📁 파일 위치 규칙

#### **정적 리소스 파일 위치**
- **`src/main/resources/static/js/`** ✅ **유일한 JavaScript 파일 위치**
- **`src/main/webapp/js/`** ❌ **사용 금지** (중복 방지)

#### **핵심 공통 파일들**

| 파일명 | 역할 | 사용 위치 | 위치 |
|--------|------|-----------|------|
| `unified-mqtt-manager.js` | 통합 MQTT 연결 관리 | MQTT 관련 JSP | `static/js/` |
| `ajax-session-manager.js` | AJAX 요청 및 세션 관리 | 모든 JSP 파일 | `static/js/` |
| `common-ui-utils.js` | 공통 UI 유틸리티 | 모든 JSP 파일 | `static/js/` |
| `log-filter.js` | 로그 필터링 시스템 | 모든 JSP 파일 | `static/js/` |
| `session-manager.js` | 세션 관리 | 모든 JSP 파일 | `static/js/` |
| `mqttws31-min.js` | MQTT 클라이언트 라이브러리 | MQTT 관련 JSP | `static/js/` |
| `chart-2.9.4.min.js` | 차트 라이브러리 | 차트 관련 JSP | `static/js/` |

## 🚀 데이터베이스 쿼리 최적화 규칙

### 📊 MyBatis XML 쿼리 작성 규칙

#### **1. 기존 인덱스 활용 우선**
- ✅ **새로운 인덱스 생성 금지**: 기존 인덱스를 최대한 활용
- ✅ **복합 인덱스 우선**: `idx_hnt_sensor_data_uuid_inst_dtm` (uuid, inst_dtm) 활용
- ✅ **인덱스 힌트 사용 금지**: MySQL 쿼리 플래너가 자동으로 최적 인덱스 선택

#### **2. 쿼리 작성 예시**
```xml
<!-- ✅ 올바른 예시: 기존 인덱스 활용 -->
<select id="selectSensorData" parameterType="java.util.HashMap" resultType="java.util.HashMap">
    select
        date_format(inst_dtm, '%Y-%m-%d') as getDate
        , date_format(inst_dtm, '%Y-%m-%d %H') as inst_dtm
        , round(avg(sensor_value), 1) as sensor_value
    from hnt_sensor_data
    where uuid = #{sensorUuid}
    and inst_dtm between #{startDateTime} and #{endDateTime}
    group by date_format(inst_dtm, '%Y-%m-%d'), date_format(inst_dtm, '%Y-%m-%d %H')
    order by getDate asc, inst_dtm asc
    limit 200
</select>

<!-- ❌ 잘못된 예시: 인덱스 힌트 사용 -->
<select id="selectSensorData" parameterType="java.util.HashMap" resultType="java.util.HashMap">
    select ...
    from hnt_sensor_data USE INDEX (idx_sensor_data_uuid_time)  <!-- 금지 -->
    where ...
</select>
```

#### **3. 쿼리 최적화 체크리스트**
- [ ] **기존 인덱스 활용**: 새 인덱스 생성 대신 기존 인덱스 활용
- [ ] **복합 인덱스 우선**: 단일 인덱스보다 복합 인덱스 활용
- [ ] **WHERE 절 순서**: 인덱스 컬럼 순서에 맞춰 WHERE 절 작성
- [ ] **인덱스 힌트 금지**: USE INDEX, FORCE INDEX 등 사용 금지
- [ ] **EXPLAIN 확인**: 쿼리 실행 계획으로 인덱스 사용 여부 확인

---

## 📁 파일별 상세 기능

### 1. common-utils.js

#### 주요 기능
- **모달 표시 함수**: `showModal()`, `showAlert()`, `showError()`, `showWarning()`, `showSuccess()`
- **로그 함수**: `debugLog()`, `infoLog()`, `warnLog()`, `errorLog()`
- **사용자 정보 조회**: `getUserId()`, `getUserGrade()`, `getCurrentUserInfo()`
- **세션 검증**: `validateUserInfo()`
- **AJAX 공통 설정**: `setupAjaxDefaults()`, `ajaxRequest()`
- **상태 표시등 업데이트**: `updateStatus()`, `updateComp()`, `updateDefr()`, `updateFan()`, `updateError()`

#### 사용 예시
```javascript
// 모달 표시
showSuccess("작업이 완료되었습니다.");
showError("오류가 발생했습니다.");
showWarning("주의가 필요합니다.");

// 사용자 정보 조회
var userInfo = getCurrentUserInfo();
var userId = getUserId();

// 세션 검증
if (!validateUserInfo()) {
    return;
}

// AJAX 요청
ajaxRequest({
    url: '/api/data',
    success: function(result) {
        console.log('성공:', result);
    }
});
```

### 2. ajax-session-manager.js

#### 주요 기능
- **AJAX 기본 설정**: 타임아웃, 캐시, CSRF 토큰 처리
- **세션 만료 감지**: 401, 403 상태 코드 처리
- **에러 처리**: 통합된 AJAX 에러 핸들링
- **리다이렉트 관리**: 중복 리다이렉트 방지

#### 사용 예시
```javascript
// AJAX 요청 (세션 처리 포함)
AjaxSessionManager.ajax({
    url: '/api/data',
    type: 'POST',
    data: JSON.stringify(data),
    success: function(result) {
        console.log('성공:', result);
    }
});

// 에러 처리
AjaxSessionManager.handleAjaxError(xhr, status, error);
```

### 3. session-utils.js

#### 주요 기능
- **세션 상태 확인**: `isSessionExpired()`
- **사용자 정보 검증**: `validateUserInfo()`
- **세션 만료 처리**: 자동 로그인 페이지 리다이렉트
- **에러 메시지 표시**: `showError()`, `showSuccess()`

#### 사용 예시
```javascript
// 세션 검증
if (SessionUtils.validateUserInfo()) {
    // 정상 처리
}

// 현재 사용자 정보 조회
var userInfo = SessionUtils.getCurrentUserInfo();
```

### 4. mqtt_lib.js

#### 주요 기능
- **MQTT 연결 관리**: 자동 재연결, 연결 상태 모니터링
- **메시지 필터링**: 사용자별 메시지 필터링
- **실시간 데이터 처리**: 센서 데이터 실시간 업데이트
- **에러 처리**: MQTT 연결 에러 처리

#### 사용 예시
```javascript
// MQTT 연결 시작
startConnect();

// MQTT 연결 상태 확인
if (isMqttConnectedSafe()) {
    console.log('MQTT 연결됨');
}
```

### 5. error-blocking-system.js

#### 주요 기능
- **콘솔 에러 필터링**: MQTT 관련 메시지만 허용
- **네트워크 요청 차단**: 외부 서비스 요청 차단
- **전역 에러 이벤트 차단**: 불필요한 에러 메시지 차단

## 📝 JSP 파일별 공통 파일 사용 규칙

### 필수 공통 파일 (모든 JSP 파일)

```html
<!-- 필수 공통 파일들 -->
<script src="/js/error-blocking-system.js"></script>
<script src="/js/common-utils.js"></script>
<script src="/js/session-utils.js"></script>
<script src="/js/ajax-session-manager.js"></script>
```

### MQTT 관련 JSP 파일 (추가)

```html
<!-- MQTT 관련 파일들 (main.jsp, admin/sensorSetting.jsp 등) -->
<script src="/js/mqttws31-min.js"></script>
<script src="/js/unified-mqtt-manager.js"></script>
```

#### **MQTT 파일 통합 규칙**
- **`unified-mqtt-manager.js`** ✅ **유일한 MQTT 관리 파일**
- **`mqtt_lib.js`** ❌ **삭제됨** (통합됨)
- **`mqtt-connection-manager-*.js`** ❌ **삭제됨** (통합됨)
- **`common-mqtt-handler.js`** ❌ **삭제됨** (통합됨)

## ❌ 금지사항

### 1. 중복 함수 정의 금지

```javascript
// ❌ 잘못된 방법 (중복 정의)
function showError(message) {
    alert(message);
}

function validateSession() {
    var userId = $('#userId').val();
    if (!userId) {
        location.href = "/login";
    }
}

// ✅ 올바른 방법 (공통 함수 사용)
showError("에러가 발생했습니다.");
if (!validateUserInfo()) {
    return;
}
```

### 2. 중복 파일 생성 금지

```html
<!-- ❌ 잘못된 방법 (중복 파일) -->
<!-- static/js/ajax-session-manager.js -->
<!-- webapp/js/ajax-session-manager.js -->

<!-- ✅ 올바른 방법 (단일 파일) -->
<!-- static/js/ajax-session-manager.js만 사용 -->
```

### 3. 라이브러리 파일 중복 금지

```html
<!-- ❌ 잘못된 방법 (여러 Chart.js 파일) -->
<script src="/js/chart.js"></script>
<script src="/js/Chart.min.js"></script>
<script src="/js/chart-2.9.4.min.js"></script>

<!-- ✅ 올바른 방법 (단일 라이브러리) -->
<script src="/js/chart-2.9.4.min.js"></script>
```

### alert() 사용 금지

```javascript
// ❌ 잘못된 방법
alert("성공했습니다.");
alert("오류가 발생했습니다.");

// ✅ 올바른 방법
showSuccess("성공했습니다.");
showError("오류가 발생했습니다.");
```

## ✅ 올바른 사용법

### 1. 모달 표시

```javascript
// 성공 메시지
showSuccess("작업이 완료되었습니다.");

// 에러 메시지
showError("오류가 발생했습니다.");

// 경고 메시지
showWarning("주의가 필요합니다.");

// 정보 메시지
showAlert("정보를 확인해주세요.");
```

### 2. 세션 검증

```javascript
// 세션 검증
if (!validateUserInfo()) {
    return;
}

// 사용자 정보 조회
var userInfo = getCurrentUserInfo();
var userId = getUserId();
```

### 3. AJAX 요청

```javascript
// 공통 AJAX 요청
ajaxRequest({
    url: '/api/data',
    type: 'POST',
    data: JSON.stringify(data),
    success: function(result) {
        showSuccess("처리 완료");
    }
});

// SessionUtils 사용
SessionUtils.ajax({
    url: '/api/data',
    success: function(result) {
        console.log('성공:', result);
    }
});
```

### 4. 로그 출력

```javascript
// 로그 레벨별 출력
debugLog("디버그 메시지", data);
infoLog("정보 메시지", data);
warnLog("경고 메시지", data);
errorLog("에러 메시지", data);
```

## 🔧 개발 가이드

### 1. 코드 중복 방지 체크리스트

#### **새 파일 생성 전 확인사항**
- [ ] **동일한 기능의 파일이 이미 존재하는가?**
- [ ] **기존 파일에 기능을 추가할 수 있는가?**
- [ ] **`static/js/` 폴더에만 생성하는가?**
- [ ] **`webapp/js/` 폴더에 생성하지 않는가?**

#### **라이브러리 파일 추가 전 확인사항**
- [ ] **동일한 라이브러리의 다른 버전이 있는가?**
- [ ] **최신 버전으로 통합할 수 있는가?**
- [ ] **TypeScript 파일(.d.ts)이 필요한가?** (JavaScript 프로젝트이므로 불필요)

#### **함수 추가 전 확인사항**
- [ ] **동일한 기능의 함수가 이미 존재하는가?**
- [ ] **기존 함수를 수정할 수 있는가?**
- [ ] **공통 함수로 만들 수 있는가?**

### 2. 새로운 공통 함수 추가

새로운 공통 함수가 필요한 경우 `common-ui-utils.js`에 추가:

```javascript
// common-ui-utils.js에 추가
function newCommonFunction(param) {
    // 공통 로직 구현
    return result;
}
```

### 2. JSP 파일에서 공통 함수 사용

```javascript
// JSP 파일에서 사용
function someFunction() {
    // 공통 함수 사용
    if (!validateUserInfo()) {
        return;
    }
    
    var userInfo = getCurrentUserInfo();
    showSuccess("처리 완료");
}
```

### 3. 에러 처리

```javascript
// AJAX 에러 처리
$.ajax({
    url: '/api/data',
    success: function(result) {
        // 성공 처리
    },
    error: function(xhr, status, error) {
        // 공통 에러 처리 사용
        AjaxSessionManager.handleAjaxError(xhr, status, error);
    }
});
```

## 📊 성능 최적화

### 1. 공통 파일 로딩 순서

```html
<!-- 올바른 로딩 순서 -->
<script src="/js/error-blocking-system.js"></script>
<script src="/js/common-utils.js"></script>
<script src="/js/session-utils.js"></script>
<script src="/js/ajax-session-manager.js"></script>
<!-- MQTT 관련 (필요한 경우만) -->
<script src="/js/mqttws31-min.js"></script>
<script src="/js/mqtt_lib.js"></script>
```

### 2. 중복 로딩 방지

- 공통 파일은 한 번만 로딩
- 중복된 `<script>` 태그 제거
- 필요한 파일만 로딩

## 🐛 문제 해결

### 1. 공통 함수가 정의되지 않음

```javascript
// 함수 존재 여부 확인
if (typeof showError === 'function') {
    showError("에러 메시지");
} else {
    console.error("showError 함수를 찾을 수 없습니다.");
}
```

### 2. 세션 검증 실패

```javascript
// 세션 상태 확인
if (SessionUtils.isSessionExpired()) {
    console.warn("세션이 만료되었습니다.");
    return;
}
```

### 3. AJAX 요청 실패

```javascript
// AJAX 에러 처리
$.ajax({
    url: '/api/data',
    error: function(xhr, status, error) {
        // 상세 에러 정보 로깅
        console.error('AJAX 오류:', {
            status: xhr.status,
            statusText: xhr.statusText,
            responseText: xhr.responseText
        });
        
        // 공통 에러 처리
        AjaxSessionManager.handleAjaxError(xhr, status, error);
    }
});
```

## 📈 모니터링 및 디버깅

### 1. 콘솔 로그 확인

```javascript
// 로그 레벨 설정
CURRENT_LOG_LEVEL = LOG_LEVEL.DEBUG; // 개발 환경
CURRENT_LOG_LEVEL = LOG_LEVEL.INFO;  // 운영 환경
```

### 2. 세션 상태 모니터링

```javascript
// 세션 상태 확인
console.log('세션 상태:', SessionUtils.isSessionExpired());
console.log('사용자 정보:', getCurrentUserInfo());
```

## 📚 참고 자료

- [jQuery AJAX 문서](https://api.jquery.com/jquery.ajax/)
- [Bootstrap 모달 문서](https://getbootstrap.com/docs/4.0/components/modal/)
- [MQTT.js 문서](https://github.com/mqttjs/MQTT.js)
- [코드 중복 방지 규칙](./CODE_DUPLICATION_PREVENTION_RULES.md)

---

**마지막 업데이트**: 2025-10-01  
**버전**: 1.1.0  
**작성자**: HnT Solutions Development Team
