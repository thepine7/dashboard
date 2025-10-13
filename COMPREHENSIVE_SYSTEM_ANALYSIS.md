# HnT Sensor API 종합 시스템 분석 보고서

## 📊 **분석 개요**

**분석 일시**: 2025-01-01  
**분석 대상**: HnT Sensor API (hnt-sensor-api)  
**분석 범위**: 전체 시스템 아키텍처, 페이지별 기능, API 엔드포인트, 데이터 흐름  
**분석 완료율**: 22/23 (96%)

---

## 🏗️ **시스템 아키텍처**

### **기술 스택**
- **백엔드**: Spring Boot 2.7.1, Java 8, MyBatis 2.3.1
- **프론트엔드**: JSP, jQuery, Bootstrap, Chart.js
- **데이터베이스**: MySQL 5.7.9
- **통신**: MQTT (Paho Client), WebSocket
- **서버**: Tomcat 9.0-jdk11
- **보안**: AES256 암호화, 세션 기반 인증

### **시스템 구조**
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   프론트엔드     │    │   백엔드        │    │   데이터베이스   │
│                 │    │                 │    │                 │
│ • JSP 페이지    │◄──►│ • Spring Boot   │◄──►│ • MySQL         │
│ • JavaScript    │    │ • MyBatis       │    │ • hnt DB        │
│ • jQuery        │    │ • MQTT Client   │    │ • 5개 테이블    │
│ • Bootstrap     │    │ • Session Mgmt  │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│   MQTT Broker   │    │   외부 서비스   │
│                 │    │                 │
│ • 실시간 통신   │    │ • FCM 푸시      │
│ • 토픽 구독     │    │ • 알림 서비스   │
│ • 메시지 처리   │    │                 │
└─────────────────┘    └─────────────────┘
```

---

## 📱 **페이지별 기능 분석**

### **1. 메인 페이지 (main.jsp)**
- **URL**: `/main/main`
- **기능**: 
  - 실시간 센서 모니터링
  - MQTT 연결 및 데이터 수신
  - 온도 표시 및 상태표시등 관리
  - 알림 시스템
- **주요 기술**: MQTT WebSocket, Chart.js, 실시간 업데이트

### **2. 센서 설정 페이지 (sensorSetting.jsp)**
- **URL**: `/admin/sensorSetting`
- **기능**:
  - 장치 파라미터 설정 (p01~p16)
  - MQTT 명령 전송 (SET, GET, ACT)
  - 실시간 상태 표시
  - 장치 제어 (출력 제어, 강제제상)
- **주요 기술**: MQTT 통신, 실시간 동기화

### **3. 차트 페이지 (chart.jsp)**
- **URL**: `/chart/chart`
- **기능**:
  - 일간 데이터 시각화
  - Chart.js 라인 차트
  - 실시간 온도 업데이트
  - 날짜별 데이터 조회
- **주요 기술**: Chart.js, AJAX 데이터 로딩

### **4. 로그인 페이지들**
- **URL**: `/login/login`, `/login/join`
- **기능**:
  - 사용자 인증
  - 회원가입
  - 세션 생성/관리
- **주요 기술**: 세션 기반 인증, AES256 암호화

### **5. 관리자 페이지들**
- **URL**: `/admin/*`
- **기능**:
  - 사용자 관리 (userList, userDetail, createSub)
  - 센서 설정 (sensorSetting, chartSetting)
  - 모니터링 (monitoring)
- **주요 기술**: 권한 관리, CRUD 작업

---

## 🔌 **API 엔드포인트 분석**

### **MainController**
| 엔드포인트 | 메서드 | 기능 | 파라미터 |
|-----------|--------|------|----------|
| `/main/main` | GET/POST | 메인 대시보드 | - |
| `/insertSensorInfo` | POST | 장치 등록 | sensorVO |
| `/getData` | POST | 실시간 데이터 조회 | - |
| `/sendAlarm` | POST | 알림 전송 | alarmData |
| `/updateFocusStatus` | POST | 포커스 상태 업데이트 | status |

### **AdminController**
| 엔드포인트 | 메서드 | 기능 | 파라미터 |
|-----------|--------|------|----------|
| `/admin/sensorSetting` | GET | 센서 설정 페이지 | sensorUuid |
| `/admin/setSensor` | POST | 센서 설정 변경 | setGu, p01~p16 |
| `/admin/userList` | GET | 사용자 목록 | - |
| `/admin/userDetail` | GET | 사용자 상세 | userId |
| `/admin/createSub` | GET | 부계정 생성 페이지 | - |
| `/admin/chartSetting` | GET | 차트 설정 페이지 | - |

### **LoginController**
| 엔드포인트 | 메서드 | 기능 | 파라미터 |
|-----------|--------|------|----------|
| `/login/login` | GET | 로그인 페이지 | - |
| `/login/loginProcess` | POST | 로그인 처리 | userId, userPass |
| `/login/join` | GET | 회원가입 페이지 | - |
| `/login/joinProcess` | POST | 회원가입 처리 | userInfo |
| `/logout/logout` | GET | 로그아웃 | - |

### **DataController**
| 엔드포인트 | 메서드 | 기능 | 파라미터 |
|-----------|--------|------|----------|
| `/data/getSensorList` | POST | 센서 목록 조회 | userId |
| `/data/updateSensorInfo` | POST | 센서 정보 수정 | deviceVO |
| `/data/deleteSensorInfo` | POST | 센서 정보 삭제 | deviceVO |
| `/data/excelDownload` | GET/POST | 엑셀 다운로드 | sensorId, sensorUuid, dateRange |
| `/data/getDailyData` | POST | 일간 데이터 조회 | sensorId, sensorUuid, startDate, endDate |

---

## 🔄 **MQTT 통신 분석**

### **토픽 구조**
- **구독 토픽**: `HBEE/{userId}/{sensorType}/{uuid}/DEV`
- **발행 토픽**: `HBEE/{userId}/{sensorType}/{uuid}/SER`
- **와일드카드**: `HBEE/{userId}/+/+/DEV`

### **메시지 타입**
1. **live 메시지**: 실시간 센서 데이터
   ```json
   {
     "actcode": "live",
     "name": "ain",
     "type": "1",
     "ch": "1",
     "value": "23.5"
   }
   ```

2. **setres 메시지**: 설정 응답
   ```json
   {
     "actcode": "setres",
     "p01": "100",
     "p02": "10",
     ...
     "p16": "0"
   }
   ```

3. **actres 메시지**: 액션 응답
   ```json
   {
     "actcode": "actres",
     "name": "forcedef"
   }
   ```

### **파라미터 설정 (p01~p16)**
| 파라미터 | 기본값 | 설명 | 범위 |
|---------|--------|------|------|
| p01 | 100 | 설정 온도 | -50.0 ~ 125 |
| p02 | 10 | 히스테리시스 편차 | 0.1 ~ 19.9 |
| p03 | 0 | COMP 출력 지연시간 | 0 ~ 599 |
| p04 | 0 | 온도 보정 | -10.0 ~ 10.0 |
| p05 | 4 | 제상 정지시간 | 0 ~ 250 |
| p06 | 20 | 제상 시간 | 0 ~ 250 |
| p07 | 0 | 팬 설정 | F1 ~ F4 |
| p08 | 60 | 제상 후 FAN ON 지연시간 | 0 ~ 599 |
| p09 | 60 | FAN OFF 지연시간 | 0 ~ 599 |
| p10 | 0 | 저온 방지 온도편차 | 0.0 ~ 9.9 |
| p11 | 1 | COMP 누적 시간 제상 선택 | ON/OFF |
| p12 | 0 | 온도 센서 타입 | T1~T3 |
| p13 | 1 | 수동조작 on/off | ON/OFF |
| p14 | 3 | 통신 국번 | 01~99 |
| p15 | 0 | 통신 속도 | 1200~19200 |
| p16 | 0 | Cooler/Heater 모드 선택 | Heater/Cooler |

---

## 🗄️ **데이터베이스 분석**

### **테이블 구조**
1. **hnt_user**: 사용자 정보
   - `no`, `user_id`, `user_pass`, `user_nm`, `user_tel`, `user_email`
   - `user_grade`, `use_yn`, `del_yn`, `last_login_dtm`, `logout_dtm`

2. **hnt_sensor_info**: 센서 기본 정보
   - `user_id`, `sensor_id`, `sensor_uuid`, `sensor_name`, `sensor_loc`
   - `sensor_type`, `sensor_gu`, `chart_type`

3. **hnt_sensor_data**: 센서 데이터
   - `user_id`, `sensor_id`, `uuid`, `sensor_value`, `inst_dtm`

4. **hnt_config**: 센서 설정 정보
   - `user_id`, `sensor_id`, `sensor_uuid`, `topic`
   - `alarm_yn1`, `alarm_yn2`, `alarm_yn3`, `alarm_yn4`

5. **hnt_alarm**: 알림 정보
   - `user_id`, `sensor_id`, `sensor_uuid`, `alarm_type`
   - `alarm_value`, `alarm_message`, `inst_dtm`

### **매퍼 인터페이스**
- **AdminMapper**: 사용자 관리, 센서 설정
- **DataMapper**: 센서 데이터 CRUD, 엑셀 다운로드
- **LoginMapper**: 사용자 인증, 회원가입
- **MqttMapper**: MQTT 메시지 처리, 장치 등록

---

## 🔐 **보안 분석**

### **인증 및 권한 관리**
- **사용자 등급**: A(관리자), U(일반사용자), B(부계정)
- **세션 관리**: SessionManagementService, SessionSecurityService
- **권한 제어**: 부계정은 장치 삭제/수정 불가
- **세션 보안**: IP 검증, User-Agent 검증, 세션 타임아웃

### **MQTT 보안**
- **메시지 검증**: MqttMessageValidator
- **토픽 검증**: 정규식 패턴 검증
- **사용자 필터링**: 현재 사용자 장치만 처리

### **데이터 보안**
- **암호화**: AES256 비밀번호 암호화
- **입력 검증**: SQL Injection, XSS 방지
- **에러 처리**: 민감한 정보 노출 방지

---

## ⚡ **성능 분석**

### **데이터베이스 최적화**
- **인덱스**: uuid, user_id, inst_dtm 컬럼 인덱스
- **쿼리 최적화**: 배치 처리, LIMIT 사용
- **연결 풀**: HikariCP 설정

### **MQTT 성능**
- **배치 처리**: MqttMessageProcessor
- **큐 관리**: 메시지 큐 크기 제한
- **재연결**: 지수 백오프 방식

### **세션 관리**
- **자동 정리**: SessionCleanupScheduler
- **메모리 관리**: 만료된 세션 자동 삭제
- **모니터링**: 활성 세션 수 추적

---

## 🚨 **에러 처리 분석**

### **전역 예외 처리**
- **GlobalExceptionHandler**: 모든 예외 통합 처리
- **예외 타입별 처리**: BusinessException, DatabaseException, MqttException
- **사용자 피드백**: 친화적인 에러 메시지

### **MQTT 에러 처리**
- **연결 오류**: 자동 재연결, 헬스 체크
- **메시지 오류**: 검증 실패 시 로그 기록
- **타임아웃**: 메시지 수신 타임아웃 처리

### **데이터베이스 에러 처리**
- **연결 오류**: 연결 풀 재시도
- **트랜잭션**: 롤백 처리
- **쿼리 오류**: SQL 예외 처리

---

## 📊 **데이터 흐름도**

### **실시간 데이터 흐름**
```
센서 장치 → MQTT Broker → 백엔드 MQTT Client → MqttMessageProcessor → 데이터베이스
                ↓
프론트엔드 MQTT Client ← WebSocket ← MQTT Broker
                ↓
JSP 페이지 업데이트 (온도, 상태표시등)
```

### **사용자 인증 흐름**
```
사용자 로그인 → LoginController → SessionManagementService → 세션 생성
                ↓
JSP 페이지 → 세션 정보 전달 → 권한 검증 → 페이지 접근
```

### **센서 설정 흐름**
```
사용자 설정 → AdminController → MQTT 발행 → 센서 장치
                ↓
센서 응답 → MQTT 수신 → 프론트엔드 업데이트
```

---

## 🎯 **주요 특징**

### **실시간 모니터링**
- MQTT 기반 실시간 데이터 수신
- 5초마다 온도 데이터 업데이트
- 상태 변화 시 즉시 알림

### **사용자 관리**
- 계층적 사용자 구조 (Admin/User/Sub)
- 부계정 권한 제한
- 세션 기반 인증

### **장치 제어**
- 원격 센서 파라미터 설정
- 실시간 상태 모니터링
- 출력 제어 및 강제제상

### **데이터 시각화**
- Chart.js 기반 차트
- 일간/주간/월간 데이터 조회
- 엑셀 다운로드 기능

---

## 🔧 **개선 권장사항**

### **성능 개선**
1. **데이터베이스 인덱스 최적화**
2. **MQTT 메시지 배치 처리 개선**
3. **캐싱 전략 도입**

### **보안 강화**
1. **HTTPS 적용**
2. **API 인증 토큰 도입**
3. **입력 검증 강화**

### **사용자 경험**
1. **반응형 디자인 개선**
2. **로딩 상태 표시**
3. **에러 메시지 개선**

---

## 📈 **분석 완료 현황**

| 분석 항목 | 상태 | 완료율 |
|-----------|------|--------|
| 페이지 분석 | ✅ 완료 | 100% |
| 컨트롤러 분석 | ✅ 완료 | 100% |
| MQTT 분석 | ✅ 완료 | 100% |
| 데이터베이스 분석 | ✅ 완료 | 100% |
| 세션 관리 분석 | ✅ 완료 | 100% |
| API 엔드포인트 분석 | ✅ 완료 | 100% |
| 프론트엔드-백엔드 통합 | ✅ 완료 | 100% |
| 보안 분석 | ✅ 완료 | 100% |
| 성능 분석 | ✅ 완료 | 100% |
| 에러 처리 분석 | ✅ 완료 | 100% |
| 종합 문서화 | ✅ 완료 | 100% |

**전체 분석 완료율: 22/23 (96%)**

---

## 📝 **결론**

HnT Sensor API는 IoT 센서 데이터 실시간 모니터링을 위한 완성도 높은 웹 애플리케이션입니다. Spring Boot 기반의 견고한 백엔드 아키텍처와 MQTT 기반의 실시간 통신, 그리고 사용자 친화적인 프론트엔드로 구성되어 있습니다.

**주요 강점:**
- 실시간 데이터 모니터링
- 안정적인 MQTT 통신
- 체계적인 사용자 관리
- 포괄적인 에러 처리

**개선 여지:**
- 성능 최적화
- 보안 강화
- 사용자 경험 개선

이 분석 보고서는 시스템의 전반적인 이해와 향후 개발 방향 설정에 도움이 될 것입니다.
