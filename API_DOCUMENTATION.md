# HnT Sensor API 문서

## 📋 API 개요

HnT Sensor API는 IoT 센서 데이터 실시간 모니터링을 위한 REST API입니다.

### 기본 정보
- **Base URL**: `https://iot.hntsolution.co.kr:8888`
- **API 버전**: v1.0.0
- **인증 방식**: 세션 기반 인증
- **응답 형식**: JSON
- **문자 인코딩**: UTF-8

## 🔐 인증

### 세션 기반 인증
모든 API 요청은 유효한 세션이 필요합니다.

```http
Cookie: JSESSIONID=your-session-id
```

### 인증 실패 응답
```json
{
  "success": false,
  "resultCode": "401",
  "resultMessage": "인증이 필요합니다.",
  "timestamp": "2025-01-01T00:00:00"
}
```

## 📊 API 엔드포인트

### 1. 메인 대시보드 API

#### 1.1 메인 화면 조회
```http
GET /main/main
```

**설명**: 메인 대시보드 화면을 조회합니다.

**요청 헤더**:
- `Cookie`: JSESSIONID (필수)

**응답**:
- **성공 (200)**: HTML 페이지 반환
- **실패 (401)**: 로그인 페이지로 리다이렉트

#### 1.2 실시간 센서 데이터 조회
```http
POST /main/getData
```

**설명**: 현재 사용자의 센서 데이터를 실시간으로 조회합니다.

**요청 헤더**:
- `Cookie`: JSESSIONID (필수)
- `Content-Type`: application/json

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "데이터 조회 성공",
  "data": {
    "sensor_value": "25.5",
    "sensor_type": "temp",
    "timestamp": "2025-01-01T00:00:00"
  },
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 1.3 센서 정보 저장
```http
POST /main/insertSensorInfo
```

**설명**: 새로운 센서 정보를 저장합니다.

**요청 파라미터**:
- `sensorUuid` (string, 필수): 센서 UUID
- `sensorName` (string, 필수): 센서 이름

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "센서 정보가 저장되었습니다.",
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 1.4 알림 전송
```http
POST /main/sendAlarm
```

**설명**: 센서 알림을 전송합니다.

**요청 파라미터**:
- `sensorUuid` (string, 필수): 센서 UUID
- `alarmMessage` (string, 필수): 알림 메시지

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "알림이 전송되었습니다.",
  "timestamp": "2025-01-01T00:00:00"
}
```

### 2. 관리자 API

#### 2.1 센서 설정 화면
```http
GET /admin/sensorSetting
```

**설명**: 센서 설정 화면을 조회합니다.

**요청 파라미터**:
- `sensorUuid` (string, 선택): 센서 UUID

**응답**:
- **성공 (200)**: HTML 페이지 반환
- **실패 (401)**: 로그인 페이지로 리다이렉트

#### 2.2 센서 설정 변경
```http
POST /admin/setSensor
```

**설명**: 센서 설정을 변경합니다.

**요청 파라미터**:
- `sensorUuid` (string, 필수): 센서 UUID
- `p01` (string, 선택): 설정 온도
- `p02` (string, 선택): 히스테리시스 편차
- `p03` (string, 선택): COMP 출력 지연시간
- `p04` (string, 선택): 온도 보정
- `p05` (string, 선택): 제상 정지시간
- `p06` (string, 선택): 제상 시간
- `p07` (string, 선택): 팬 설정
- `p08` (string, 선택): 제상 후 FAN ON 지연시간
- `p09` (string, 선택): FAN OFF 지연시간
- `p10` (string, 선택): 저온 방지 온도편차
- `p11` (string, 선택): COMP 누적 시간 제상 선택
- `p12` (string, 선택): 온도 센서 타입
- `p13` (string, 선택): 수동조작 on/off
- `p14` (string, 선택): 통신 국번
- `p15` (string, 선택): 통신 속도
- `p16` (string, 선택): Cooler/Heater 모드 선택

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "센서 설정이 변경되었습니다.",
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 2.3 사용자 목록 조회
```http
GET /admin/userList
```

**설명**: 사용자 목록을 조회합니다.

**요청 파라미터**:
- `page` (int, 선택): 페이지 번호 (기본값: 1)
- `size` (int, 선택): 페이지 크기 (기본값: 10)
- `search` (string, 선택): 검색어

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "사용자 목록 조회 성공",
  "data": {
    "users": [
      {
        "userId": "testUser",
        "userNm": "테스트사용자",
        "userGrade": "U",
        "userEmail": "test@example.com",
        "userTel": "010-1234-5678",
        "regDtm": "2025-01-01T00:00:00"
      }
    ],
    "totalCount": 100,
    "pageNumber": 1,
    "pageSize": 10
  },
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 2.4 사용자 상세 조회
```http
GET /admin/userDetail
```

**설명**: 사용자 상세 정보를 조회합니다.

**요청 파라미터**:
- `userId` (string, 필수): 사용자 ID

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "사용자 상세 조회 성공",
  "data": {
    "userId": "testUser",
    "userNm": "테스트사용자",
    "userGrade": "U",
    "userEmail": "test@example.com",
    "userTel": "010-1234-5678",
    "regDtm": "2025-01-01T00:00:00",
    "sensors": [
      {
        "sensorUuid": "test-uuid",
        "sensorName": "테스트센서",
        "sensorType": "temp",
        "regDtm": "2025-01-01T00:00:00"
      }
    ]
  },
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 2.5 하위 사용자 생성
```http
POST /admin/createSub
```

**설명**: 하위 사용자를 생성합니다.

**요청 파라미터**:
- `userId` (string, 필수): 사용자 ID
- `userNm` (string, 필수): 사용자명
- `userEmail` (string, 필수): 이메일
- `userTel` (string, 필수): 전화번호
- `userGrade` (string, 필수): 사용자 등급 (B)

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "하위 사용자가 생성되었습니다.",
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 2.6 차트 설정 화면
```http
GET /admin/chartSetting
```

**설명**: 차트 설정 화면을 조회합니다.

**응답**:
- **성공 (200)**: HTML 페이지 반환
- **실패 (401)**: 로그인 페이지로 리다이렉트

### 3. 로그인 API

#### 3.1 로그인 화면
```http
GET /login/login
```

**설명**: 로그인 화면을 조회합니다.

**응답**:
- **성공 (200)**: HTML 페이지 반환

#### 3.2 로그인 처리
```http
POST /login/loginProcess
```

**설명**: 사용자 로그인을 처리합니다.

**요청 파라미터**:
- `userId` (string, 필수): 사용자 ID
- `userPw` (string, 필수): 비밀번호
- `saveId` (string, 선택): ID 저장 여부 (Y/N)

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "로그인되었습니다.",
  "data": {
    "userId": "testUser",
    "userNm": "테스트사용자",
    "userGrade": "U"
  },
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 3.3 로그아웃
```http
GET /login/logout
```

**설명**: 사용자 로그아웃을 처리합니다.

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "로그아웃되었습니다.",
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 3.4 회원가입 화면
```http
GET /login/join
```

**설명**: 회원가입 화면을 조회합니다.

**응답**:
- **성공 (200)**: HTML 페이지 반환

#### 3.5 회원가입 처리
```http
POST /login/joinProcess
```

**설명**: 사용자 회원가입을 처리합니다.

**요청 파라미터**:
- `userId` (string, 필수): 사용자 ID
- `userPw` (string, 필수): 비밀번호
- `userNm` (string, 필수): 사용자명
- `userEmail` (string, 필수): 이메일
- `userTel` (string, 필수): 전화번호
- `userGrade` (string, 필수): 사용자 등급 (A/U)

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "회원가입이 완료되었습니다.",
  "timestamp": "2025-01-01T00:00:00"
}
```

### 4. 데이터 API

#### 4.1 센서 목록 조회
```http
POST /data/getSensorList
```

**설명**: 센서 목록을 조회합니다.

**요청 파라미터**:
- `userId` (string, 필수): 사용자 ID

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "센서 목록 조회 성공",
  "data": {
    "sensors": [
      {
        "sensorUuid": "test-uuid",
        "sensorName": "테스트센서",
        "sensorType": "temp",
        "sensorValue": "25.5",
        "status": "normal",
        "regDtm": "2025-01-01T00:00:00"
      }
    ]
  },
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 4.2 센서 정보 수정
```http
POST /data/updateSensorInfo
```

**설명**: 센서 정보를 수정합니다.

**요청 파라미터**:
- `sensorUuid` (string, 필수): 센서 UUID
- `sensorName` (string, 필수): 센서 이름
- `sensorType` (string, 필수): 센서 타입

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "센서 정보가 수정되었습니다.",
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 4.3 센서 정보 삭제
```http
POST /data/deleteSensorInfo
```

**설명**: 센서 정보를 삭제합니다.

**요청 파라미터**:
- `sensorUuid` (string, 필수): 센서 UUID

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "센서 정보가 삭제되었습니다.",
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 4.4 엑셀 다운로드
```http
GET /data/excelDownload
```

**설명**: 센서 데이터를 엑셀 파일로 다운로드합니다.

**요청 파라미터**:
- `userId` (string, 필수): 사용자 ID
- `startDate` (string, 선택): 시작 날짜 (yyyy-MM-dd)
- `endDate` (string, 선택): 종료 날짜 (yyyy-MM-dd)
- `sensorUuid` (string, 선택): 센서 UUID

**응답**:
- **성공 (200)**: 엑셀 파일 다운로드
- **실패 (400)**: 에러 메시지

#### 4.5 일간 데이터 조회
```http
POST /data/getDailyData
```

**설명**: 일간 센서 데이터를 조회합니다.

**요청 파라미터**:
- `userId` (string, 필수): 사용자 ID
- `sensorUuid` (string, 필수): 센서 UUID
- `date` (string, 필수): 조회 날짜 (yyyy-MM-dd)

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "일간 데이터 조회 성공",
  "data": {
    "date": "2025-01-01",
    "sensorUuid": "test-uuid",
    "sensorName": "테스트센서",
    "data": [
      {
        "time": "00:00",
        "value": "25.5"
      },
      {
        "time": "00:30",
        "value": "25.8"
      }
    ]
  },
  "timestamp": "2025-01-01T00:00:00"
}
```

### 5. 차트 API

#### 5.1 차트 데이터 조회
```http
GET /chart/chart
```

**설명**: 차트 데이터 화면을 조회합니다.

**요청 파라미터**:
- `userId` (string, 선택): 사용자 ID

**응답**:
- **성공 (200)**: HTML 페이지 반환
- **실패 (401)**: 로그인 페이지로 리다이렉트

### 6. 헬스체크 API

#### 6.1 기본 헬스체크
```http
GET /health/
```

**설명**: 애플리케이션 상태를 확인합니다.

**응답**:
```json
{
  "status": "UP",
  "timestamp": 1640995200000,
  "application": "HnT Sensor API",
  "version": "1.0.0"
}
```

#### 6.2 데이터베이스 연결 상태
```http
GET /health/db
```

**설명**: 데이터베이스 연결 상태를 확인합니다.

**응답**:
```json
{
  "status": "UP",
  "database": "hnt",
  "connectionCount": 5,
  "maxConnections": 20,
  "timestamp": 1640995200000
}
```

#### 6.3 MQTT 연결 상태
```http
GET /health/mqtt
```

**설명**: MQTT 연결 상태를 확인합니다.

**응답**:
```json
{
  "status": "UP",
  "broker": "iot.hntsolution.co.kr:1883",
  "connectedClients": 10,
  "timestamp": 1640995200000
}
```

### 7. 백업 API

#### 7.1 전체 시스템 백업
```http
POST /admin/backup/full
```

**설명**: 전체 시스템을 백업합니다.

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "전체 백업이 완료되었습니다.",
  "data": {
    "backupId": "backup-20250101-000000",
    "backupSize": "1.2GB",
    "backupPath": "/backup/backup-20250101-000000.tar.gz"
  },
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 7.2 데이터베이스 백업
```http
POST /admin/backup/database
```

**설명**: 데이터베이스만 백업합니다.

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "데이터베이스 백업이 완료되었습니다.",
  "data": {
    "backupId": "db-backup-20250101-000000",
    "backupSize": "800MB",
    "backupPath": "/backup/db-backup-20250101-000000.sql"
  },
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 7.3 백업 목록 조회
```http
GET /admin/backup/list
```

**설명**: 백업 목록을 조회합니다.

**요청 파라미터**:
- `page` (int, 선택): 페이지 번호 (기본값: 1)
- `size` (int, 선택): 페이지 크기 (기본값: 10)

**응답**:
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "백업 목록 조회 성공",
  "data": {
    "backups": [
      {
        "backupId": "backup-20250101-000000",
        "backupType": "full",
        "backupSize": "1.2GB",
        "backupPath": "/backup/backup-20250101-000000.tar.gz",
        "createdAt": "2025-01-01T00:00:00"
      }
    ],
    "totalCount": 10,
    "pageNumber": 1,
    "pageSize": 10
  },
  "timestamp": "2025-01-01T00:00:00"
}
```

## 📝 공통 응답 형식

### 성공 응답
```json
{
  "success": true,
  "resultCode": "200",
  "resultMessage": "요청이 성공적으로 처리되었습니다.",
  "data": {
    // 응답 데이터
  },
  "timestamp": "2025-01-01T00:00:00"
}
```

### 에러 응답
```json
{
  "success": false,
  "resultCode": "400",
  "resultMessage": "요청 처리 중 오류가 발생했습니다.",
  "details": {
    // 상세 에러 정보 (선택사항)
  },
  "timestamp": "2025-01-01T00:00:00"
}
```

## 🔢 HTTP 상태 코드

| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 500 | 서버 내부 오류 |
| 503 | 서비스 이용 불가 |
| 504 | 게이트웨이 타임아웃 |

## 📋 에러 코드

| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 400 | 입력 데이터 오류 |
| 401 | 인증 필요 |
| 403 | 권한 부족 |
| 404 | 리소스 없음 |
| 500 | 서버 내부 오류 |
| 503 | 데이터베이스 오류 |
| 504 | MQTT 통신 오류 |
| 999 | 알 수 없는 오류 |

## 🔒 보안 고려사항

1. **세션 관리**: 모든 API 요청은 유효한 세션이 필요합니다.
2. **입력 검증**: 모든 입력 데이터는 서버에서 검증됩니다.
3. **SQL 인젝션 방지**: PreparedStatement 사용으로 SQL 인젝션을 방지합니다.
4. **XSS 방지**: 입력 데이터의 HTML 태그를 이스케이프 처리합니다.
5. **권한 검증**: 사용자 등급에 따른 접근 권한을 검증합니다.

## 📞 지원

API 사용 중 문제가 발생하면 다음으로 문의하세요:

- **이메일**: support@hntsolution.co.kr
- **전화**: 02-1234-5678
- **문서**: https://docs.hntsolution.co.kr