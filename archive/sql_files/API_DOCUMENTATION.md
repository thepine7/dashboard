# HnT Sensor API 문서

## 개요
HnT Sensor API는 IoT 센서 데이터를 실시간으로 수집, 처리, 모니터링하는 Spring Boot 기반 웹 애플리케이션입니다.

## 기술 스택
- **Backend**: Spring Boot 2.7.1, Java 8
- **Database**: MySQL 5.7.9, MyBatis 2.3.1
- **MQTT**: Eclipse Paho MQTT Client
- **Frontend**: JSP, JavaScript, Chart.js
- **Security**: FCM Push Notifications, AES256 암호화

## API 엔드포인트

### 1. 인증 관련 API

#### POST /login/loginProcess
사용자 로그인 처리
- **Request Body**: `{"userId": "string", "password": "string"}`
- **Response**: 로그인 성공/실패 결과
- **Security**: 비밀번호 AES256 암호화

#### GET /login/logout
사용자 로그아웃 처리
- **Session**: 세션 무효화
- **Response**: 로그인 페이지로 리다이렉트

#### POST /login/joinProcess
사용자 회원가입 처리
- **Request Body**: 사용자 정보 (이름, 이메일, 전화번호, 비밀번호)
- **Validation**: 입력 데이터 검증 및 중복 확인

### 2. 메인 대시보드 API

#### GET /main/main
메인 대시보드 페이지
- **Authentication**: 세션 기반 인증 필요
- **Data**: 사용자 센서 목록, 실시간 데이터
- **Features**: 실시간 MQTT 데이터 수신

#### POST /main/getData
실시간 센서 데이터 조회
- **Response**: JSON 형태의 센서 데이터
- **Update**: 0.5초마다 자동 업데이트

#### POST /main/insertSensorInfo
센서 정보 등록
- **Request Body**: 센서 UUID, 사용자 ID, 센서 타입
- **MQTT**: 센서 등록 확인 메시지 전송

#### POST /main/sendAlarm
알림 전송
- **FCM**: Firebase Cloud Messaging을 통한 푸시 알림
- **Triggers**: 센서 임계값 초과, 통신 이상 등

### 3. 관리자 기능 API

#### GET /admin/sensorSetting
센서 설정 페이지
- **Authentication**: 관리자 권한 필요
- **Features**: 센서 파라미터 설정, MQTT 통신

#### POST /admin/setSensor
센서 설정 변경
- **MQTT**: `SET&p01=value&p02=value...` 형태로 센서에 전송
- **Response**: 설정 결과 및 센서 응답

#### GET /admin/userList
사용자 목록 조회
- **Pagination**: 페이징 처리
- **Search**: 사용자 검색 기능
- **Permissions**: 사용자 등급별 권한 관리

#### GET /admin/userDetail
사용자 상세 정보
- **Data**: 사용자 정보, 연결된 센서 목록
- **Actions**: 사용자 수정, 삭제 기능

#### POST /admin/createSub
하위 사용자 생성
- **Hierarchy**: 계층적 사용자 구조 (Admin > User > Sub-account)
- **Permissions**: 부계정 권한 제한

### 4. 데이터 처리 API

#### POST /data/getSensorList
센서 목록 조회
- **Filtering**: 사용자별 센서 필터링
- **Sorting**: 센서 타입, 등록일 기준 정렬

#### POST /data/updateSensorInfo
센서 정보 수정
- **Validation**: 권한 검증 (부계정 제한)
- **Database**: 센서 정보 업데이트

#### POST /data/deleteSensorInfo
센서 정보 삭제
- **Cascade**: 관련 데이터 완전 삭제
- **Security**: 권한 검증 및 로그 기록

#### GET /data/excelDownload
엑셀 데이터 다운로드
- **Format**: Excel (.xlsx) 파일
- **Data**: 센서 데이터, 통계 정보
- **Performance**: 대용량 데이터 배치 처리

### 5. 차트 데이터 API

#### GET /chart/chart
차트 데이터 페이지
- **Visualization**: Chart.js 기반 실시간 차트
- **Data**: 일간/시간별 센서 데이터
- **Real-time**: MQTT를 통한 실시간 업데이트

#### POST /data/getDailyData
일간 데이터 조회
- **Period**: 1일간 데이터 (30분 단위 평균)
- **Aggregation**: 데이터 집계 및 통계 처리

### 6. 헬스체크 API

#### GET /health
애플리케이션 상태 확인
- **Response**: 서버 상태, 데이터베이스 연결 상태
- **Monitoring**: JMX 메트릭 정보

## MQTT 통신 프로토콜

### 토픽 구조
```
HBEE/{userId}/TC/{sensorUuid}/DEV  # 센서 → 서버
HBEE/{userId}/TC/{sensorUuid}/SER  # 서버 → 센서
```

### 메시지 타입

#### 실시간 데이터 (live)
```json
{
  "actcode": "live",
  "name": "ain",
  "type": "1",
  "ch": "1", 
  "value": "23.5"
}
```

#### 설정 응답 (setres)
```json
{
  "actcode": "setres",
  "p01": "25.0",
  "p02": "1.0",
  "p03": "0"
}
```

#### 액션 응답 (actres)
```json
{
  "actcode": "actres",
  "name": "forcedef"
}
```

## 보안 기능

### 세션 관리
- **Timeout**: 30분 자동 만료
- **Security**: IP 주소, User-Agent 검증
- **Protection**: 세션 하이재킹 방지

### 데이터 암호화
- **Passwords**: AES256 암호화
- **Transmission**: HTTPS 통신
- **Storage**: 데이터베이스 암호화 저장

### 권한 관리
- **Admin (A)**: 모든 기능 사용 가능
- **User (U)**: 일반 사용자 기능
- **Sub-account (B)**: 읽기 전용, 제한된 기능

## 성능 최적화

### 데이터베이스
- **Connection Pool**: HikariCP 최적화 설정
- **Indexing**: 쿼리 성능 향상을 위한 인덱스
- **Batch Processing**: 대용량 데이터 배치 처리

### 메모리 관리
- **Thread Management**: MQTT 스레드 정리 강화
- **Garbage Collection**: 메모리 누수 방지
- **Resource Cleanup**: 애플리케이션 종료 시 리소스 정리

### 프론트엔드
- **DOM Optimization**: requestAnimationFrame 사용
- **Batch Processing**: 대량 DOM 조작 최적화
- **Caching**: 정적 리소스 캐싱

## 배포 정보

### 서버 환경
- **Tomcat**: 9.0-jdk11
- **Java**: 8 (호환성 유지)
- **Database**: MySQL 5.7.9
- **MQTT Broker**: Eclipse Mosquitto

### 배포 경로
- **Tomcat1**: `Y:\docker\tomcat\ROOT.war` (포트: 8080)
- **Tomcat2**: `Y:\docker\tomcat2\ROOT.war` (포트: 8888)

### 환경 변수
```yaml
DB_URL: jdbc:mysql://hntsolution.co.kr:3306/hnt
DB_USERNAME: root
DB_PASSWORD: HntRoot123!
```

## 모니터링

### 로깅
- **Level**: INFO, WARN, ERROR
- **Format**: 구조화된 로그 (JSON)
- **Rotation**: 일별 로그 파일 회전

### 메트릭
- **JMX**: JVM 메모리, 스레드, GC 정보
- **Custom**: 요청 수, 응답 시간, 에러율
- **MQTT**: 연결 상태, 메시지 처리량

## 문제 해결

### 일반적인 문제
1. **404 에러**: URL 매핑 확인, 컨트롤러 경로 검증
2. **세션 만료**: 로그인 상태 확인, 세션 타임아웃 설정
3. **MQTT 연결 실패**: 브로커 상태 확인, 네트워크 연결 검증

### 로그 확인
```bash
# 애플리케이션 로그
tail -f /usr/local/tomcat/logs/catalina.out

# MQTT 연결 상태
grep "MQTT" /usr/local/tomcat/logs/catalina.out

# 에러 로그
grep "ERROR" /usr/local/tomcat/logs/catalina.out
```

## 개발 가이드

### 코드 스타일
- **Language**: 한국어 주석, 영어 변수명
- **Formatting**: 2칸 들여쓰기
- **Naming**: camelCase (변수), PascalCase (클래스)

### 테스트
- **Unit Tests**: JUnit 5
- **Integration Tests**: Spring Boot Test
- **Mocking**: Mockito

### Git 워크플로우
- **Branch**: feature/기능명
- **Commit**: 명확한 커밋 메시지
- **Review**: 코드 리뷰 필수