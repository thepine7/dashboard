# HnT Sensor API 배포 자동화 가이드

## 📋 개요

이 문서는 HnT Sensor API의 자동화된 배포 시스템 사용법을 설명합니다.

## 🚀 빠른 시작

### 1. 자동 배포 스크립트 사용

```bash
# 톰캣2에 배포 (기본값)
./deploy/deploy.sh

# 톰캣1에 배포
./deploy/deploy.sh -t 1

# 빌드만 수행
./deploy/deploy.sh -b

# 배포만 수행
./deploy/deploy.sh -d -t 2

# 깨끗한 빌드 (테스트 건너뛰기)
./deploy/deploy.sh -c -s
```

### 2. 환경별 설정

```bash
# 개발 환경
export SPRING_PROFILES_ACTIVE=development

# 테스트 환경
export SPRING_PROFILES_ACTIVE=testing

# 운영 환경 (톰캣1)
export SPRING_PROFILES_ACTIVE=production_tomcat1

# 운영 환경 (톰캣2)
export SPRING_PROFILES_ACTIVE=production_tomcat2
```

## 📁 파일 구조

```
deploy/
├── deploy.sh                 # 자동 배포 스크립트
├── environment-config.yml    # 환경별 설정 파일
├── ci-cd-pipeline.yml       # GitHub Actions CI/CD 파이프라인
└── README.md               # 이 파일
```

## 🔧 자동 배포 스크립트

### 사용법

```bash
./deploy/deploy.sh [옵션]
```

### 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `-t, --tomcat [1\|2]` | 배포할 톰캣 서버 선택 | 2 |
| `-b, --build` | 빌드만 수행 (배포하지 않음) | - |
| `-d, --deploy` | 배포만 수행 (빌드하지 않음) | - |
| `-c, --clean` | 깨끗한 빌드 수행 | - |
| `-s, --skip-tests` | 테스트 건너뛰기 | - |
| `-h, --help` | 도움말 출력 | - |

### 예시

```bash
# 톰캣1에 배포
./deploy/deploy.sh -t 1

# 톰캣2에 배포
./deploy/deploy.sh -t 2

# 빌드만 수행
./deploy/deploy.sh -b

# 톰캣1에 배포만 수행
./deploy/deploy.sh -d -t 1

# 깨끗한 빌드 (테스트 건너뛰기)
./deploy/deploy.sh -c -s
```

## 🌍 환경별 설정

### 개발 환경 (development)

- **포트**: 8080
- **데이터베이스**: localhost:3306/hnt_dev
- **MQTT**: localhost:1883
- **로깅 레벨**: DEBUG

### 테스트 환경 (testing)

- **포트**: 8080
- **데이터베이스**: test.hntsolution.co.kr:3306/hnt_test
- **MQTT**: test.hntsolution.co.kr:1883
- **로깅 레벨**: INFO

### 운영 환경 (production_tomcat1)

- **포트**: 8080 (내부), 8080 (외부)
- **데이터베이스**: hntsolution.co.kr:3306/hnt
- **MQTT**: iot.hntsolution.co.kr:1883
- **로깅 레벨**: INFO
- **접속 URL**: iot.hntsolution.co.kr:8080

### 운영 환경 (production_tomcat2)

- **포트**: 8080 (내부), 8888 (외부)
- **데이터베이스**: hntsolution.co.kr:3306/hnt
- **MQTT**: iot.hntsolution.co.kr:1883
- **로깅 레벨**: INFO
- **접속 URL**: iot.hntsolution.co.kr:8888

## 🔄 CI/CD 파이프라인

### GitHub Actions 워크플로우

1. **코드 품질 검사**
   - Checkstyle 검사
   - SpotBugs 정적 분석
   - OWASP Dependency Check

2. **테스트 실행**
   - 단위 테스트
   - 통합 테스트
   - 성능 테스트

3. **빌드**
   - Maven 빌드
   - WAR 파일 생성
   - 아티팩트 업로드

4. **배포**
   - 톰캣1/톰캣2 자동 배포
   - 헬스체크
   - 알림 발송

### 트리거 조건

- **main 브랜치 푸시**: 톰캣1 배포
- **develop 브랜치 푸시**: 톰캣2 배포
- **Pull Request**: 코드 품질 검사 및 테스트
- **수동 실행**: 환경 선택 가능

## 📊 모니터링 및 알림

### 배포 후 확인사항

1. **헬스체크**
   ```bash
   curl http://iot.hntsolution.co.kr:8888/health
   ```

2. **로그 확인**
   ```bash
   tail -f logs/hnt-sensor-api.log
   ```

3. **데이터베이스 연결 확인**
   ```bash
   curl http://iot.hntsolution.co.kr:8888/admin/health/db
   ```

### 알림 설정

- **Slack**: 배포 성공/실패 알림
- **Email**: 중요 에러 알림
- **Discord**: 개발팀 알림

## 🛠️ 문제 해결

### 일반적인 문제

1. **빌드 실패**
   ```bash
   # 의존성 문제 해결
   mvn clean install -U
   
   # 테스트 건너뛰기
   mvn clean package -DskipTests
   ```

2. **배포 실패**
   ```bash
   # 톰캣 서버 상태 확인
   systemctl status tomcat
   
   # 로그 확인
   tail -f /var/log/tomcat/catalina.out
   ```

3. **환경 설정 문제**
   ```bash
   # 환경 변수 확인
   echo $SPRING_PROFILES_ACTIVE
   
   # 설정 파일 확인
   cat src/main/resources/application.yml
   ```

### 로그 위치

- **애플리케이션 로그**: `logs/hnt-sensor-api.log`
- **에러 로그**: `logs/hnt-sensor-api-error.log`
- **MQTT 로그**: `logs/hnt-sensor-api-mqtt.log`
- **데이터베이스 로그**: `logs/hnt-sensor-api-db.log`
- **성능 로그**: `logs/hnt-sensor-api-performance.log`

## 📈 성능 최적화

### 배포 최적화

1. **증분 배포**: 변경된 파일만 배포
2. **캐시 활용**: Maven 캐시, 의존성 캐시
3. **병렬 처리**: 테스트 및 빌드 병렬화

### 모니터링

1. **응답 시간**: 평균, P95, P99
2. **에러율**: 4xx, 5xx 에러 비율
3. **리소스 사용량**: CPU, 메모리, 디스크

## 🔒 보안 고려사항

### 환경 변수

- **민감한 정보**: 환경 변수로 관리
- **암호화**: 데이터베이스 비밀번호, API 키
- **접근 제어**: 환경별 권한 분리

### 배포 보안

- **서명 검증**: WAR 파일 무결성 확인
- **백업**: 배포 전 기존 파일 백업
- **롤백**: 문제 발생 시 이전 버전으로 복구

## 📞 지원

문제가 발생하면 다음을 확인하세요:

1. **로그 파일**: `logs/` 디렉토리
2. **시스템 상태**: `systemctl status tomcat`
3. **네트워크 연결**: 데이터베이스, MQTT 연결 상태
4. **리소스 사용량**: CPU, 메모리, 디스크 공간

---

**마지막 업데이트**: 2025-09-26  
**버전**: 1.0.0  
**작성자**: HnT 개발팀
