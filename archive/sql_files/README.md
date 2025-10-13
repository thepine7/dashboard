# 커서 AI 설정 파일

## 프로젝트 개요
- **프로젝트명**: HnT Sensor API (hnt-sensor-api)
- **기술 스택**: Spring Boot 2.7.1, Java 8, MyBatis, MySQL, MQTT
- **패키징**: WAR 파일 (Tomcat 배포용)
- **기본 포트**: 8888
- **프로젝트 타입**: IoT 센서 데이터 실시간 모니터링 웹 애플리케이션
- **주요 기능**: 실시간 센서 데이터 수집, 사용자 관리, 알림 시스템, 데이터 시각화

## 서버 환경 및 호환성 규칙
- **톰캣 서버**: 9.0-jdk11 버전 사용
- **Java 호환성**: Java 8 코드 (톰캣 9.0-jdk11에서 실행)
- **Spring Boot**: 2.7.1 버전 (톰캣 9.0 완전 호환)
- **패키지 규칙**: javax.* 패키지 사용 (jakarta.* 사용 금지)
- **MyBatis**: 2.3.1 버전 (Spring Boot 2.7.1 호환)
- **JSTL**: javax.servlet.jsp.jstl 사용 (jakarta 사용 금지)
- **HTTP Client**: org.apache.httpcomponents 사용 (client5 사용 금지)
- **Jasper**: Spring Boot에서 자동 제공 (별도 버전 지정 금지)

### 톰캣 9.0-jdk11 호환성 매트릭스
| 구성요소 | 호환 버전 | 비호환 버전 | 상태 |
|---------|----------|------------|------|
| Spring Boot | 2.7.1 | 3.x | ✅ 호환 |
| Java | 8 | 17+ | ✅ 호환 |
| MyBatis | 2.3.1 | 3.x | ✅ 호환 |
| JSTL | javax | jakarta | ✅ 호환 |
| 패키지 | javax.* | jakarta.* | ✅ 호환 |
| HTTP Client | httpcomponents | httpcomponents.client5 | ✅ 호환 |

### 업그레이드 제한사항
- **Spring Boot 3.x 업그레이드 금지**: 톰캣 9.0과 호환되지 않음
- **Java 17+ 업그레이드 금지**: 톰캣 9.0-jdk11 환경에서 실행 불가
- **Jakarta EE 업그레이드 금지**: 톰캣 9.0은 Java EE (javax) 사용
- **MyBatis 3.x 업그레이드 금지**: Spring Boot 2.7.1과 호환되지 않음

## MQTT 통신 프로토콜 규칙

### 기본 토픽 구조
- **공통 토픽 형식**: `HBEE/($userId)/TC/($mac)/DEV`
- **서버 요청 토픽**: `HBEE/($userId)/TC/($mac)/SER`
- **클라이언트 구독 토픽**: `HBEE/+/+/+/DEV` (응답 수신용)
- **메시지 필터링**: 마지막 요소가 DEV인 경우만 처리 (응답 메시지)

### 메시지 타입별 규칙

#### 1. 현재 온도 알림 (Current Temperature Notification)
- **목적**: 현재 온도(또는 기타 센서) 측정값 전송
- **JSON 페이로드**:
```json
{
  "actcode": "live",
  "name": "ain",
  "type": "1"~"99",
  "ch": "1" ~ "99",
  "value": "23.5"
}
```
- **설명**:
  - `actcode`: "live" (실시간 데이터)
  - `name`: "ain" (Analog Input)
  - `type`: "1"~"99" (temp, humidity, Pressure, CO2, PT100, 4-20mA 등)
  - `ch`: "1"~"99" (채널 번호)
  - `value`: 센서 측정값 (예: "23.5")
  - **에러 처리**: 센서 연결 에러 시 `value`는 `"Error"`로 전송

#### 2. Input 상태 변화 알림 (Input Status Change Notification)
- **목적**: Input 상태 변화 알림
- **JSON 페이로드**:
```json
{
  "actcode": "live",
  "name": "din",
  "type": "1" ~ "99",
  "ch": "1" ~ "99",
  "value": "0" ~ "65000"
}
```
- **설명**:
  - `actcode`: "live"
  - `name`: "din" (Digital Input)
  - `type`: "1"~"99" (Alarm, H/L, Counter, Freq 등)
  - `ch`: "1"~"99" (채널 번호)
  - `value`: "0"~"65000" (수치값)

#### 3. Output 상태 변화 알림/출력 단자 수동 제어 응답
- **목적**: Output 상태 변화 알림 또는 출력 단자 수동 제어에 대한 응답
- **JSON 페이로드**:
```json
{
  "actcode": "live",
  "name": "output",
  "type": "1" ~ "99",
  "ch": "1" ~ "99",
  "value": "1" or "0"
}
```
- **설명**:
  - `actcode`: "live"
  - `name`: "output"
  - `type`: "1"~"99" (COMP, DEF, FAN, DOUT, ROUT, AOUT 등)
  - `ch`: "1"~"99" (채널 번호)
  - `value`: "1" or "0" (ON/OFF 상태)
- **전송 로직**: 브로커 초기 연결 시 1회 전송, 이후 이벤트 발생 시마다 전송

### 서버 요청 및 장치 응답 규칙

#### 파라미터 읽기 (GET&type=1)
- **요청**: `GET&type=1` → type 1: parameter, 2: status
- **응답 actcode**: `setres`
- **응답 JSON**:
```json
{
  "actcode": "setres",
  "p01": "1" ~ "99",
  "p02": "1" ~ "99",
  "p03": "1" ~ "99",
  "p04": "1" ~ "99",
  "p05": "1" ~ "99",
  ...
  "pxx": "1" ~ "99"
}
```
- **설명**: 장치의 설정 파라미터(p01~p16)를 읽어오는 응답

#### 상태 읽기 (GET&type=2)
- **요청**: `GET&type=2` → type 1: parameter, 2: status
- **응답 actcode**: `live`
- **응답 JSON (Input 상태)**:
```json
{
  "actcode": "live",
  "name": "din",
  "type": "1" ~ "99",
  "ch": "1" ~ "99",
  "value": "0" ~ "65000"
}
```
- **응답 JSON (Output 상태)**:
```json
{
  "actcode": "live",
  "name": "output",
  "type": "1" ~ "99",
  "ch": "1" ~ "99",
  "value": "1" or "0"
}
```
- **설명**: 
  - Input 상태: `name: "din"` (Digital Input) - Alarm, H/L, Counter, Freq 등
  - Output 상태: `name: "output"` - COMP, DEF, FAN, DOUT, ROUT, AOUT 등

#### 파라미터 설정
- **요청**: `SET&p01=100&p02=10&p03=0&p04=0&p05=4&p06=20&p07=0&p08=60&p09=60&p10=0&p11=1&p12=0&p13=1&p14=3&p15=0&p16=0`

#### 강제 제상 시작/종료
- **시작 요청**: `ACT&name=forcedef&value=1`
- **종료 요청**: `ACT&name=forcedef&value=0`
- **응답 actcode**: `actres`
- **응답 JSON**:
```json
{
  "actcode": "actres",
  "name": "forcedef"
}
```

#### 출력 단자 수동 제어
- **요청**: `ACT&name=output&type=1&ch=1&value=0`
- **Type 종류**: COMP, DEF, FAN, DOUT, ROUT, AOUT, 99
- **응답 actcode**: `actres`
- **응답 JSON**:
```json
{
  "actcode": "actres",
  "name": "output"
}
```

#### 등록 초기화(장치 삭제)
- **요청**: `ACT&name=userId&value=0`
- **응답 actcode**: `actres`
- **응답 JSON**:
```json
{
  "actcode": "actres",
  "name": "userId"
}
```

### 파라미터 설정 규칙 (p01~p16)

| 파라미터 | 기본값 | 디코딩 | 설명 | 범위 | 인코딩 |
|---------|--------|--------|------|------|--------|
| p01 | 100 | 10.0 | 설정 온도 | -50.0 ~ 125 / -200 ~ 850 | -500 ~ 1250 / -2000 ~ 8500 |
| p02 | 10 | 1.0 | 히스테리시스 편차 | 0.1 ~ 19.9 | 1 ~ 199 |
| p03 | 0 | 0 | COMP 출력 지연시간(sec) | 0 ~ 599 | 0 ~ 599 |
| p04 | 0 | 0.0 | 온도 보정 | -10.0 ~ 10.0 | -100 ~ 100 |
| p05 | 4 | 4 | 제상 정지시간(hour) | 0 ~ 250 | 0 ~ 250 |
| p06 | 20 | 20 | 제상 시간(min) | 0 ~ 250 | 0 ~ 250 |
| p07 | 0 | F1 | 팬 설정 | F1 ~ F4 | 0 ~ 3 |
| p08 | 60 | 60 | 제상 후 FAN ON 지연시간(sec) | 0 ~ 599 | 0 ~ 599 |
| p09 | 60 | 60 | FAN OFF 지연시간(sec) | 0 ~ 599 | 0 ~ 599 |
| p10 | 0 | 0.0 | 저온 방지 온도편차 | 0.0 ~ 9.9 | 0 ~ 99 |
| p11 | 1 | ON | COMP 누적 시간 제상 선택 | ON/OFF | 1/0 |
| p12 | 0 | T-1 | 온도 센서 타입 | T1~T3 | 0~2 |
| p13 | 1 | ON | 수동조작 on/off | ON/OFF | 1/0 |
| p14 | 3 | 1 | 통신 국번 | 01~99 | 1~99 |
| p15 | 0 | 9600 | 통신 속도 | 1200~19200 | 0~4 |
| p16 | 0 | Cooler | Cooler/Heater 모드 선택 | Heater/Cooler | 1/0 |

**참고사항**:
- T1, T2 센서 타입: 범위 -50.0 ~ 125, 인코딩 -500 ~ 1250
- T3 센서 타입: 범위 -200 ~ 850, 인코딩 -2000 ~ 8500
- -200.0은 2000으로, -20.0은 200으로 인코딩 (실제값 × 10)

## 실시간 모니터링 규칙 (2025-08-03 정밀 분석 기반)

### 데이터 수신 주기
- **현재온도**: 5초마다 수신됨 (live 메시지의 ain 데이터)
- **상태 데이터**: 상태 변화 시 즉시 수신 (din, output 메시지)

### 초기 페이지 로딩 시 요청
- **실행 조건(게이트)**: 페이지 로딩 완료(`window.load`) + MQTT 연결 성공(`mqtt:connected` 이벤트) 둘 다 충족 시 1회 수행
- **요청 타이밍**: 
  1. GET&type=1 (설정값 요청) - 조건 충족 후 0.5초
  2. GET&type=2 (상태표시 요청) - 위 요청 후 2.0초(+2.0s)
- **응답 처리**: 각각의 요청값에 맞는 데이터를 파싱하고 UI에 표시

### 에러 해제 후 재요청 규칙
- **에러 해제 조건**: 정상 온도 데이터 수신 시 자동 에러 해제
- **재요청 타이밍**: 에러 해제 후 2초 간격으로 한 번만 요청
- **요청 순서**:
  1. GET&type=1 (설정값 요청) - 에러 해제 후 2초
  2. GET&type=2 (상태표시 요청) - 에러 해제 후 4초 (2초 + 2초)
- **적용 페이지**: 메인 페이지, 챠트데이터 페이지, 센서설정 페이지 모두 동일 적용

### 설정온도 표시 규칙
- **데이터 소스**: GET&type=1 응답의 p01 파라미터
- **표시 형식**: "20.0°C" 형태로 표시
- **디코딩 규칙**:
  - 1자리: "0.1" → "0.1°C"
  - 2자리: "20" → "2.0°C"
  - 3자리: "200" → "20.0°C"
  - 4자리: "2000" → "200.0°C"
- **업데이트 시점**: GET&type=1 응답 수신 시 즉시 업데이트

### 상태표시등 표시 규칙
- **데이터 소스**: GET&type=2 응답의 din/output 메시지
- **표시 요소**: 운전, 콤프, 제상, FAN, 이상 상태표시등
- **색상 규칙**:
  - **운전**: 정상 시 녹색, 통신이상 시 회색
  - **콤프/제상/FAN**: 동작 시 빨간색, 정지 시 회색
  - **이상**: 이상 시 빨간색, 정상 시 회색
- **업데이트 시점**: GET&type=2 응답 수신 시 즉시 업데이트

### 현재온도 표시 규칙
- **데이터 소스**: live 메시지의 ain 데이터 (5초마다 수신)
- **표시 형식**: "27.6°C" 형태로 표시
- **에러 표시**: 센서 연결 에러 시 "Error" 표시
- **업데이트 시점**: live 메시지 수신 시 즉시 업데이트

### 상태 변경 시 처리
- **초기설정**: 운전 녹색, 나머지 회색
- **초기 로딩 이후**: 상태가 변경될 때 장치에서 상태표시값을 자동으로 전송
- **상태표시값 수신**: 실시간으로 상태표시값을 받으면 UI 갱신
- **추가 요청 불필요**: 초기 로딩 후에는 별도의 GET&type=1, GET&type=2 요청을 하지 않음

### 실시간 온도 데이터 기반 에러 처리 규칙
- **에러 추적 변수**: `deviceLastDataTime`, `deviceErrorCounters`, `deviceErrorStates`
- **에러 조건**: 15초 동안 온도 데이터 미수신
- **에러 카운터**: 3번 연속 미수신 시 에러 상태로 변경
- **에러 상태 지속**: 에러 상태가 되면 더 이상 에러체크를 하지 않음
- **에러 해제**: 정상 온도 데이터 수신 시 자동으로 에러 해제 및 상태 복구

### 장치별 독립적인 상태 관리 규칙
- **장치별 상태 추적 변수**: 각 장치마다 독립적인 상태 변수 관리
  - `deviceStatusStates`: 장치별 운전 상태 ('gray', 'green')
  - `deviceErrorDisplayStates`: 장치별 이상 표시등 상태 ('gray', 'red')
  - `deviceTempStates`: 장치별 온도 표시 상태
  - `deviceDinErrorStates`: 장치별 DIN 이상 상태
- **장치별 완전 독립**: 한 장치의 상태 변경이 다른 장치에 영향 없음
- **장치별 에러 처리**: 각 장치마다 독립적인 에러 카운터 및 상태 관리
- **장치별 DOM 요소**: `$('#status'+sensor_uuid)` 형태로 장치별 고유 ID 사용
- **장치별 로깅**: 각 장치의 상태 변경을 독립적으로 로그 기록

### 상태표시등 색상 규칙

#### 기본 상태표시등
- **운전/콤프/제상/FAN**: 회색 (기본 상태)
- **콤프/제상/FAN 동작**: 빨간색 (활성 상태)
- **이상**: 
  - 이상일 때 빨간색
  - 기본 상태는 회색
- **운전**: 
  - 정상일 때 녹색
  - 통신이상이면 회색

#### DIN 처리 시 규칙
- **DIN value=1**: 운전 녹색, 이상 빨간색
- **DIN value=0**: 운전 녹색, 이상 회색

#### 실시간 온도 처리 시 규칙
- **실시간 온도가 Error일때**:
  - 이상: 빨간색
  - 운전: 녹색 (정상 상태 유지)
  - 현재온도: "Error" 표시
- **Error에서 정상 복구 시**:
  - 이상: DIN 이상 상태가 없으면 회색으로 변경
  - 운전: 녹색으로 변경
  - 현재온도: 정상 온도값으로 표시

#### 통신이상 시 표시
- **현재온도**: "Error" 표시
- **이상 상태표시등**: 빨간색 (통신이상)
- **운전/콤프/제상/FAN 상태표시등**: 회색 (비활성)

### 상태표시등 깜빡임 방지 규칙
- **중복 방지 로직**: 실제 DOM 요소의 이미지 src 속성을 직접 비교하여 중복 업데이트 방지
- **이미지 src 직접 비교**: `$('#status'+sensor_uuid+' img').attr('src') !== '/images/green.png'`
- **변수 기반 비교 대신**: DOM 상태와 변수 상태의 동기화 보장
- **적용 대상**: 모든 상태표시등 (운전, 콤프, 제상, FAN, 이상)
- **성능 최적화**: 불필요한 DOM 조작 최소화로 깜빡임 현상 완전 제거
- **안정성 향상**: 실제 화면 상태와 내부 변수 상태의 일치성 보장

### 사용자별 장치 필터링 규칙
- **현재 사용자 확인**: 토픽에서 추출한 userId와 현재 로그인한 사용자 비교
- **필터링 조건**: 현재 사용자의 장치가 아닌 경우 파싱하지 않음
- **처리 순서**: 
  1. 토픽에서 userId 추출
  2. 현재 로그인한 사용자와 비교
  3. 일치하지 않으면 즉시 return (파싱 중단)
  4. 일치하는 경우에만 메시지 파싱 및 처리 진행
- **UI 장치리스트 필터링**: 추가로 UI에 표시되는 장치리스트에 있는 장치만 처리

### 설정값 표시 방법
- **설정온도**: GET&type=1 응답의 p01 파라미터에서 추출하여 표시 (디코딩 규칙 적용)
- **상태표시등**: GET&type=2 응답의 din/output 메시지에서 실시간 상태 업데이트 (색상 규칙 적용)
- **현재온도**: live 메시지의 ain 데이터에서 실시간 업데이트 (5초마다, 에러 시 "Error" 표시)
- **표시 우선순위**: 현재온도 > 설정온도 > 상태표시등 순으로 업데이트

### 메시지 파싱 및 처리 규칙
- **배열 형태 메시지 처리**: MQTT 메시지가 배열 형태로 수신되는 경우 첫 번째 요소 추출 후 처리
- **JSON 유효성 검사**: 메시지 파싱 전 JSON 유효성 검사 수행
- **토픽 구조별 파싱**: 
  - 5개 이상 요소: `HBEE/userId/TC/uuid/DEV` → uuid=topicArr[3], userId=topicArr[1]
  - 4개 요소: `HBEE/userId/uuid/DEV` → uuid=topicArr[2], userId=topicArr[1]
  - 4개 미만: 파싱 불가 (undefined 처리)
- **초기 로딩 시 에러 표시 방지**: 페이지 초기 로딩 시 데이터가 없는 경우 "Error" 표시하지 않고 빈 상태로 유지

### 페이지 간 이동 및 세션 관리 규칙
- **Main 링크 처리**: 장치설정페이지에서 Main 클릭 시 다중 소스에서 사용자 정보 확인
- **세션 정보 확인 순서**:
  1. Hidden input 필드 (`userId`, `loginUserId`, `userGrade`)
  2. URL 파라미터 (`?userId=xxx&userGrade=xxx`)
  3. 세션 정보
- **세션 만료 처리**: 사용자 정보가 없을 때 명확한 에러 메시지와 함께 로그인 페이지로 이동
- **기본값 설정**: userGrade가 없을 때 기본값 "U" 설정

### 백업 폴더
- **백업 폴더**: D:\Project\SW\CursorAI\backup 폴더에 오늘 날짜로 백업
- 백업업 할때 README.md 도 같이 백업

## 🔄 내일 작업 예정 (2025-08-03)

### 우선순위 1: 에러 체크 문제 해결
- **문제**: 장치별 에러 체크가 독립적으로 동작하지 않음
- **현상**: 한 장치 에러 시 모든 장치 상태가 변경됨
- **해결 방안**: 
  - `chkError_${item.sensor_uuid}` 함수 호출 방식 수정
  - 장치별 독립적인 에러 상태 관리 로직 구현
  - 에러 체크 함수의 DOM 요소 선택자 수정

### 우선순위 2: 실제 기능 테스트
- **사용자 삭제 기능**: 부계정 vs 메인 사용자 구분 삭제 테스트
- **장치 삭제 기능**: 센서 데이터 완전 삭제 테스트
- **부계정 권한 제한**: 장치 삭제/수정 버튼 숨김 테스트
- **장치 전송 기능**: 기존 소유자 데이터 삭제 후 새 소유자 등록 테스트

### 우선순위 3: 성능 최적화
- **DB 인덱스**: 성능 개선 효과 확인
- **MQTT 연결**: 안정성 개선 효과 확인
- **콘솔 로그**: 디버그 메시지 필터링 효과 확인

## 사용자 관리 및 권한 규칙

### 사용자 등급별 권한 체계
- **A (관리자)**: 모든 기능 사용 가능
- **U (일반사용자)**: 장치 삭제/수정 가능
- **B (부계정)**: 장치 삭제/수정 불가능 (읽기 전용)

### 부계정 삭제 시 데이터 처리 규칙
- **부계정 삭제 시**: 부계정 사용자 정보만 삭제하고 메인 사용자의 장치 정보는 보존
- **메인 사용자 삭제 시**: 모든 사용자 관련 데이터 완전 삭제

#### 삭제 로직 비교
| 사용자 등급 | 삭제되는 데이터 | 보존되는 데이터 |
|------------|----------------|----------------|
| **B (부계정)** | `hnt_user` 테이블의 부계정 정보만 | 메인 사용자의 모든 장치 정보 및 센서 데이터 |
| **A/U (메인)** | 모든 사용자 관련 데이터 | 없음 (완전 삭제) |

#### 부계정 삭제 시 보존되는 데이터
- **장치 기본 정보**: `hnt_sensor_info` 테이블의 메인 사용자 장치 정보
- **장치 설정 정보**: `hnt_config` 테이블의 메인 사용자 설정 정보
- **센서 데이터**: `hnt_sensor_data` 테이블의 메인 사용자 센서 데이터
- **알림 데이터**: `hnt_alarm` 테이블의 메인 사용자 알림 데이터

#### 메인 사용자 삭제 시 삭제되는 데이터
- **사용자 정보**: `hnt_user` 테이블의 사용자 정보
- **장치 기본 정보**: `hnt_sensor_info` 테이블의 모든 장치 정보
- **장치 설정 정보**: `hnt_config` 테이블의 모든 설정 정보
- **센서 데이터**: `hnt_sensor_data` 테이블의 모든 센서 데이터
- **알림 데이터**: `hnt_alarm` 테이블의 모든 알림 데이터

### 부계정 권한 제한 기능
- **백엔드 권한 체크**: 부계정(B 등급) 사용자의 장치 삭제/수정 시도 차단
- **프론트엔드 권한 체크**: 부계정 사용자에게 관리 메뉴 및 삭제/수정 버튼 숨김
- **이중 보안**: 프론트엔드 + 백엔드 모두에서 권한 체크
- **로그 기록**: 권한 없는 사용자의 시도 로그 기록
- **명확한 에러 메시지**: 사용자에게 권한 부족 이유 안내

## MQTT 연결 안정성 및 호환성 규칙

### Paho MQTT JavaScript Client 호환성
- **라이브러리**: `mqttws31-min.js` (로컬 파일 사용)
- **CDN 사용 금지**: 외부 의존성 제거로 안정성 확보
- **지원되는 속성만 사용**: 호환성 문제 방지

### 지원되는 MQTT 연결 속성
```javascript
client.connect({
    onSuccess: onConnect,        // 연결 성공 콜백
    onFailure: onFailure,        // 연결 실패 콜백
    userName: 'hnt1',           // 사용자명
    password: 'abcde',          // 비밀번호
    cleanSession: true,         // 세션 정리
    keepAliveInterval: 60,      // Keep Alive 간격
    mqttVersion: 4,             // MQTT 버전
    timeout: 30,                // 연결 타임아웃
    useSSL: false               // SSL 사용 여부
});
```

### 지원되지 않는 속성 (사용 금지)
- ❌ **`reconnect: true`** - 라이브러리에서 지원하지 않음
- ❌ **`maxReconnectAttempts: 10`** - 라이브러리에서 지원하지 않음

### MQTT 연결 안정성 개선 (2025-08-02 구현)
- **지수 백오프 재연결**: 2초 → 4초 → 8초 → 16초 → 30초 (최대)
- **최대 재시도 횟수**: 10회 제한
- **연결 상태 표시**: 실시간 연결 시도 횟수 및 상태 표시
- **네트워크 상태 확인**: `navigator.onLine` 체크로 오프라인 시 연결 중단
- **자동 재연결**: 연결 끊김 시 자동 재연결 시도

### MQTT 연결 재시도 로직
```javascript
// 연결 재시도 카운터
var reconnectAttempts = 0;
var maxReconnectAttempts = 10;
var baseReconnectDelay = 2000; // 2초

// 지수 백오프 방식으로 재연결 지연 시간 계산
var delay = Math.min(baseReconnectDelay * Math.pow(2, reconnectAttempts - 1), 30000);
```
- ❌ **기타 문서화되지 않은 속성** - 호환성 문제 발생 가능

### 에러 차단 시스템 규칙
- **디버깅 시**: 에러 차단 시스템 완전 제거하여 모든 콘솔 메시지 확인
- **운영 시**: 선택적 에러 차단으로 MQTT 디버그 메시지만 허용
- **MQTT 디버그 메시지 키워드**:
  - `MQTT`, `연결`, `호스트`, `포트`, `클라이언트`
  - `=== MQTT`, `연결 성공`, `연결 실패`, `연결 끊김`

### 라이브러리 파일 관리
- **로컬 라이브러리**: `/js/mqttws31-min.js` 사용
- **백업 파일**: `mqtt_lib_original.js` - 정상 작동하는 원본 파일
- **현재 파일**: `mqtt_lib.js` - 커스텀 MQTT 라이브러리
- **복원 방법**: `mqtt_lib_original.js` → `mqtt_lib.js` 복사

### 문제 해결 가이드

#### MQTT 연결 문제 해결 순서
1. **라이브러리 호환성 확인**: 지원되지 않는 속성 제거
2. **에러 차단 시스템 확인**: MQTT 디버그 메시지 차단 여부 확인
3. **원본 파일 복원**: `mqtt_lib_original.js` → `mqtt_lib.js` 복사
4. **톰캣 서버 재시작**: Synology Container Manager에서 재시작
5. **콘솔 에러 확인**: 브라우저 개발자 도구에서 에러 메시지 확인

#### 자주 발생하는 문제 및 해결책
- **"Unknown property, reconnect"**: `reconnect` 속성 제거
- **"Unknown property, maxReconnectAttempts"**: `maxReconnectAttempts` 속성 제거
- **MQTT 디버그 메시지 안 보임**: 에러 차단 시스템 제거
- **GitHub 관련 에러**: 에러 차단 시스템으로 처리
- **MQTT 초기 연결 실패**: 지수 백오프 재연결 로직으로 해결
- **사이드바 사라짐**: CSS 미디어 쿼리 `display: none` 설정 확인

### Android WebView 호환성
- **CustomEvent 폴리필**: 일부 WebView에서 `CustomEvent` 미지원 → 폴리필로 `mqtt:connected` 이벤트 보장
- **초기 동기화 트리거**: `mqtt:connected` 이벤트를 사용해 앱/PC 공통으로 초기 동기화 수행

## 사용자 및 장치 관리 기능 구현 현황

### 1. 사용자 삭제 로직 구현 ✅

#### 부계정 vs 메인 사용자 구분 삭제
- **구현 위치**: `AdminController.java` - `deleteUser` 메서드
- **구현 내용**:
```java
// 삭제할 사용자의 등급 확인
UserInfo userInfo = loginService.getUserInfoByUserId(userId);
String userGrade = userInfo.getUserGrade();

if("B".equals(userGrade)) {
    // 부계정인 경우: 부계정 사용자 정보만 삭제 (장치 정보 보존)
    adminService.deleteSubUser(userId);
} else {
    // 메인 사용자인 경우: 모든 정보 삭제
    adminService.deleteUser(userId);
}
```

#### 삭제 로직 비교
| 사용자 등급 | 삭제되는 데이터 | 보존되는 데이터 | 구현 메서드 |
|------------|----------------|----------------|-------------|
| **B (부계정)** | `hnt_user` 테이블의 부계정 정보만 | 메인 사용자의 모든 장치 정보 및 센서 데이터 | `deleteSubUser()` |
| **A/U (메인)** | 모든 사용자 관련 데이터 | 없음 (완전 삭제) | `deleteUser()` |

#### 메인 사용자 삭제 시 삭제 순서
1. **사용자 센서 데이터 삭제**: `adminMapper.deleteUserSensorData(userId)`
2. **사용자 알림 데이터 삭제**: `adminMapper.deleteDeviceAlarm(alarmParam)`
3. **사용자 장치 설정 정보 삭제**: `adminMapper.deleteConfig(userId, "")`
4. **사용자 장치 기본 정보 삭제**: `adminMapper.deleteSensor(userId)`
5. **사용자 정보 삭제**: `adminMapper.deleteUser(userId)`

### 2. 장치 삭제 로직 구현 ✅

#### 완전 삭제 순서
- **구현 위치**: `DataServiceImpl.java` - `deleteSensorInfo` 메서드
- **삭제 순서**:
```java
// 1. 센서 데이터 삭제 (모든 센서 데이터 완전 삭제)
dataMapper.deleteSensorData(param);

// 2. 장치 관련 알림 데이터 삭제
adminMapper.deleteDeviceAlarm(param);

// 3. 장치 기본 정보 삭제
dataMapper.deleteSensorInfo(param);

// 4. 장치 설정 정보 삭제
adminMapper.deleteConfig(deviceVO.getUserId(), deviceVO.getSensorUuid());
```

#### 삭제되는 데이터 항목
- **센서 데이터**: `hnt_sensor_data` 테이블의 모든 센서 데이터
- **알림 데이터**: `hnt_alarm` 테이블의 장치 관련 알림
- **장치 기본 정보**: `hnt_sensor_info` 테이블의 장치 정보
- **장치 설정 정보**: `hnt_config` 테이블의 장치 설정

### 3. 부계정 권한 제한 기능 구현 ✅

#### 백엔드 권한 체크
- **구현 위치**: `DataController.java` - `deleteSensorInfo`, `updateSensorInfo` 메서드
- **권한 체크 로직**:
```java
// 부계정(B 등급) 사용자는 장치 삭제/수정 불가
if("B".equals(userGrade)) {
    resultMap.put("resultCode", "403");
    resultMap.put("resultMessage", "부계정 사용자는 장치 삭제 권한이 없습니다.");
    return resultMap;
}

// A(관리자) 또는 U(일반사용자)만 장치 삭제/수정 가능
if(!"A".equals(userGrade) && !"U".equals(userGrade)) {
    resultMap.put("resultCode", "403");
    resultMap.put("resultMessage", "장치 삭제 권한이 없습니다.");
    return resultMap;
}
```

#### 프론트엔드 권한 체크
- **메인 화면**: `main.jsp` - 설정 버튼 숨김
```jsp
<c:if test="${userGrade ne 'B'}">
    <a href="javascript:goSensorSetting_${item.sensor_uuid}();">
        <img src="/images/setting2.png" width="30" height="30">
    </a>
</c:if>
```

- **센서 설정 화면**: `sensorSetting.jsp` - 저장 버튼 권한 체크
```javascript
$('#save1').click(function() {
    if('${userGrade}' === 'B') {
        alert("부계정은 설정을 변경할 수 없습니다.");
        return;
    }
    saveSensorSetting();
});
```

### 4. 장치 전송 기능 구현 ✅

#### 기존 소유자 데이터 완전 삭제
- **구현 위치**: `LoginServiceImpl.java` - `insertSensorInfo` 메서드
- **전송 로직**:
```java
// 1. 다른 사용자가 해당 장치를 소유하고 있는지 확인
Map<String, Object> existingOwner = mqttMapper.getSensorInfoByUuid(checkParam);

if(existingOwner != null && existingOwner.size() > 0) {
    String existingUserId = String.valueOf(existingOwner.get("user_id"));
    
    // 2. 기존 소유자의 모든 데이터 삭제
    mqttMapper.deleteSensorInfoByUuid(checkParam);
    mqttMapper.deleteConfigByUuid(checkParam);
    mqttMapper.deleteSensorDataByUuid(checkParam);
    mqttMapper.deleteAlarmByUuid(checkParam);
    
    // 3. 새 사용자에게 장치 등록
    mqttMapper.insertSensorInfo(param);
}
```

#### 삭제되는 기존 소유자 데이터
- **장치 기본 정보**: `hnt_sensor_info` 테이블
- **장치 설정 정보**: `hnt_config` 테이블
- **센서 데이터**: `hnt_sensor_data` 테이블
- **알림 데이터**: `hnt_alarm` 테이블

### 5. DB 연결 최적화 구현 ✅

#### HikariCP 연결 풀 설정
- **구현 위치**: `application.yml`
- **최적화 설정**:
```yaml
hikari:
    max-lifetime: 900000          # 연결 최대 수명 (15분)
    connection-timeout: 30000     # 연결 타임아웃 (30초)
    minimum-idle: 5               # 최소 유휴 연결 수
    maximum-pool-size: 20         # 최대 연결 풀 크기
    idle-timeout: 600000          # 유휴 타임아웃 (10분)
    leak-detection-threshold: 60000  # 누수 감지 임계값 (1분)
```

#### 트랜잭션 관리
- **구현 위치**: `MqttApplicationRunner.java`
- **트랜잭션 메서드**:
```java
@Transactional
public void insertSensorDataWithTransaction(SensorVO sensorVO) {
    mqttMapper.insertSensorData(sensorVO);
}
```

### 6. 테스트 필요 항목 목록

#### 사용자 삭제 테스트
- [ ] **부계정 삭제 테스트**: `thepien7` 부계정 삭제 후 `thepine` 메인 사용자 데이터 보존 확인
- [ ] **메인 사용자 삭제 테스트**: 메인 사용자 삭제 시 모든 관련 데이터 완전 삭제 확인
- [ ] **삭제 로그 확인**: 각 단계별 삭제 로그 메시지 확인

#### 장치 삭제 테스트
- [ ] **장치 완전 삭제 테스트**: 장치 삭제 시 센서 데이터, 알림 데이터, 설정 정보 모두 삭제 확인
- [ ] **삭제 순서 확인**: 1→2→3→4 순서로 삭제되는지 확인
- [ ] **MQTT 장치 삭제**: DB 삭제 후 센서 자체 삭제 처리 확인

#### 부계정 권한 테스트
- [ ] **UI 권한 테스트**: 부계정 로그인 시 설정/삭제 버튼 숨김 확인
- [ ] **API 권한 테스트**: 부계정이 API 호출 시 403 에러 반환 확인
- [ ] **권한 로그 확인**: 권한 없는 사용자 시도 로그 기록 확인

#### 장치 전송 테스트
- [ ] **기존 소유자 확인**: 다른 사용자가 기존 장치 등록 시 기존 소유자 확인
- [ ] **데이터 삭제 확인**: 기존 소유자의 모든 데이터 완전 삭제 확인
- [ ] **새 소유자 등록**: 새 사용자에게 장치 정상 등록 확인

#### DB 성능 테스트
- [ ] **연결 풀 테스트**: HikariCP 연결 풀 정상 동작 확인
- [ ] **트랜잭션 테스트**: MQTT 데이터 저장 시 트랜잭션 정상 처리 확인
- [ ] **누수 감지 테스트**: 연결 누수 감지 기능 정상 동작 확인

## 🔥 중요: 실수 방지 규칙 (2025-08-03 업데이트)

### 1. MQTT 설정온도 요청 관련
- **setTimeout 타이밍**: `setSensor` 함수 호출은 반드시 3000ms (3초) 지연으로 설정
  ```javascript
  setTimeout(function() {
    setSensor_${item.sensor_uuid}();
  }, 3000); // 절대 100ms나 다른 값으로 변경하지 말 것
  ```
- **조건부 호출 금지**: `setSensor` 함수는 절대 조건부로 호출하지 말 것
- **독립적 실행**: `setSensor`는 `getData` 함수와 독립적으로 실행되어야 함

### 2. MQTT 메시지 필터링
- **3중 필터링 필수**: 사용자, 장치, actcode 모두 확인
  ```javascript
  // 1. 사용자 확인
  if(rcvTopicArr[1] !== currentUserId) return;
  // 2. 장치 UUID 확인  
  if(rcvTopicArr[3] !== targetDeviceUuid) return;
  // 3. actcode 확인 (setres만 처리)
  if(jsonObj.actcode !== "setres") return;
  ```
- **actcode 구분**: `live`는 현재온도, `setres`는 설정온도 응답

### 3. 사용자 정보 전달
- **URL 파라미터 읽기**: 페이지 로딩 시 URL에서 userId, userGrade 읽기
- **Hidden input 설정**: URL 파라미터로 hidden input 값 설정
- **페이지 이동 시**: 모든 네비게이션에서 userId, userGrade 전달

### 4. MQTT 토픽 구조 이해
- **sensor_id vs user_id**: 
  - `sensor_id`: 센서의 실제 소유자 (MQTT 토픽의 첫 번째 세그먼트)
  - `user_id`: 센서를 사용하는 사용자
  - 부계정(B 등급): `user_id ≠ sensor_id`
  - 메인 사용자(A/U 등급): `user_id = sensor_id`

### 5. 디버깅 규칙
- **MQTT 요청/응답 로깅**: 클라이언트와 서버 양쪽에 상세 로깅 추가
- **토픽 가시성 확인**: MQTT 클라이언트에서 토픽 발행 확인
- **타이밍 이슈**: 페이지 로딩과 MQTT 요청 타이밍 차이 주의

### 6. 백업 코드 참조
- **백업본 확인**: 수정 전 반드시 백업 코드와 비교
- **동일한 패턴 적용**: 백업본에서 작동하는 패턴을 그대로 적용
- **임의 변경 금지**: 작동하는 코드를 임의로 최적화하지 말 것

### 7. AI 동작 지침 (업데이트)
- 코드를 작성할 때 항상 타입 안전성을 고려하세요
- 에러 처리를 포함한 견고한 코드를 작성하세요
- 성능과 가독성을 모두 고려하세요
- 새로운 기능 추가 시 기존 코드와의 호환성을 확인하세요
- 새로운 코드 추가 시 기존 코드와 연관된 코드가 있는지 확인하세요
- **MQTT 관련 수정 시 반드시 백업본과 비교하세요**
- **setTimeout 값 변경 시 신중하게 검토하세요**
- **사용자 정보 전달 문제가 발생하면 URL 파라미터를 확인하세요**

## 프로젝트 폴더 구조
