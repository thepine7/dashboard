# 코드 중복 방지 규칙 (Code Duplication Prevention Rules)

## 📋 개요

이 문서는 HnT Sensor API 프로젝트에서 코드 중복을 방지하고 유지보수성을 향상시키기 위한 규칙을 정의합니다.

## 🚫 중복 금지 규칙

### 1. 파일 중복 금지

#### **JavaScript 파일 위치 규칙**
- **`src/main/resources/static/js/`** ✅ **유일한 JavaScript 파일 위치**
- **`src/main/webapp/js/`** ❌ **사용 금지** (중복 방지)

#### **중복 파일 생성 금지**
```bash
# ❌ 잘못된 방법 (중복 파일)
src/main/resources/static/js/ajax-session-manager.js
src/main/webapp/js/ajax-session-manager.js

# ✅ 올바른 방법 (단일 파일)
src/main/resources/static/js/ajax-session-manager.js
```

### 2. 라이브러리 파일 중복 금지

#### **Chart.js 라이브러리**
```bash
# ❌ 잘못된 방법 (여러 버전)
chart.js
Chart.min.js
chart-2.9.4.min.js
chart.umd.js
chart.umd.js.map

# ✅ 올바른 방법 (단일 버전)
chart-2.9.4.min.js
```

#### **MQTT 관련 파일**
```bash
# ❌ 잘못된 방법 (여러 MQTT 파일)
mqtt_lib.js
mqtt-connection-manager.js
mqtt-connection-manager-improved.js
mqtt-connection-manager-enhanced.js
common-mqtt-handler.js
page-topic-manager.js

# ✅ 올바른 방법 (통합 파일)
unified-mqtt-manager.js
```

### 3. TypeScript 파일 금지

JavaScript 프로젝트이므로 TypeScript 관련 파일은 불필요:

```bash
# ❌ 불필요한 파일들
helpers.d.ts
types.d.ts
helpers.js
helpers.js.map
chunks/helpers.core.d.ts
chunks/helpers.segment.js
chunks/helpers.segment.js.map
```

## ✅ 올바른 파일 구조

### **정적 리소스 파일 구조**
```
src/main/resources/static/js/
├── unified-mqtt-manager.js      # 통합 MQTT 관리
├── ajax-session-manager.js      # AJAX 세션 관리
├── common-ui-utils.js           # 공통 UI 유틸리티
├── log-filter.js                # 로그 필터링
├── session-manager.js           # 세션 관리
├── sensor-setting.js            # 센서 설정
├── mqttws31-min.js              # MQTT 클라이언트 라이브러리
├── chart-2.9.4.min.js           # 차트 라이브러리
├── jquery.min.js                # jQuery 라이브러리
└── bootstrap.min.js             # Bootstrap 라이브러리
```

## 🔍 중복 검사 체크리스트

### **새 파일 생성 전 확인사항**
- [ ] **동일한 기능의 파일이 이미 존재하는가?**
- [ ] **기존 파일에 기능을 추가할 수 있는가?**
- [ ] **`static/js/` 폴더에만 생성하는가?**
- [ ] **`webapp/js/` 폴더에 생성하지 않는가?**

### **라이브러리 파일 추가 전 확인사항**
- [ ] **동일한 라이브러리의 다른 버전이 있는가?**
- [ ] **최신 버전으로 통합할 수 있는가?**
- [ ] **TypeScript 파일(.d.ts)이 필요한가?** (JavaScript 프로젝트이므로 불필요)

### **함수 추가 전 확인사항**
- [ ] **동일한 기능의 함수가 이미 존재하는가?**
- [ ] **기존 함수를 수정할 수 있는가?**
- [ ] **공통 함수로 만들 수 있는가?**

## 🛠️ 중복 제거 절차

### **1단계: 중복 파일 식별**
```bash
# 중복 파일 검색
find src/main -name "*.js" -type f | sort
```

### **2단계: 기능 비교**
- 파일 내용 비교
- 기능 중복 여부 확인
- 최신 버전 식별

### **3단계: 통합 또는 삭제**
- 기능 통합 가능한 경우: 기존 파일에 통합
- 기능 중복인 경우: 최신 버전만 유지
- 불필요한 파일: 삭제

### **4단계: 참조 업데이트**
- JSP 파일의 script 태그 수정
- import/require 구문 수정
- 빌드 및 배포

## 📊 중복 제거 효과

### **성능 개선**
- **파일 크기 감소**: 중복 파일 제거로 WAR 파일 크기 감소
- **로딩 속도 향상**: 불필요한 파일 로딩 제거
- **메모리 사용량 감소**: 중복 코드 제거

### **유지보수성 향상**
- **단일 책임**: 하나의 파일만 관리
- **혼동 방지**: 어떤 파일을 수정해야 하는지 명확
- **일관성 보장**: 동일한 기능의 통일된 구현

### **개발 효율성 향상**
- **빠른 검색**: 필요한 파일을 쉽게 찾을 수 있음
- **명확한 구조**: 파일 역할이 명확해짐
- **디버깅 용이**: 문제 발생 시 원인 파악이 쉬워짐

## 🚨 주의사항

### **삭제 전 확인사항**
- [ ] **파일이 실제로 사용되고 있는가?**
- [ ] **다른 파일에서 참조하고 있는가?**
- [ ] **빌드에 영향을 주지 않는가?**
- [ ] **백업이 있는가?**

### **통합 시 주의사항**
- [ ] **기존 기능이 손상되지 않는가?**
- [ ] **새로운 기능이 정상 작동하는가?**
- [ ] **테스트가 완료되었는가?**
- [ ] **문서가 업데이트되었는가?**

## 📚 관련 문서

- [COMMON_JS_GUIDE.md](./COMMON_JS_GUIDE.md) - 공통 JavaScript 파일 사용 가이드
- [DATABASE_STRUCTURE_GUIDE.md](./DATABASE_STRUCTURE_GUIDE.md) - 데이터베이스 구조 가이드

---

**마지막 업데이트**: 2025-10-01  
**버전**: 1.0.0  
**작성자**: HnT Solutions Development Team
