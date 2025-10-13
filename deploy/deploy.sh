#!/bin/bash

# HnT Sensor API 자동 배포 스크립트
# 작성일: 2025-09-26
# 목적: 톰캣1/톰캣2 자동 배포 및 환경별 설정 관리

set -e  # 에러 발생 시 스크립트 종료

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 설정 변수
PROJECT_DIR="/d/Project/SW/CursorAI/tomcat22"
TOMCAT1_DIR="Y:/docker/tomcat"
TOMCAT2_DIR="Y:/docker/tomcat2"
TOMCAT1_PORT="8080"
TOMCAT2_PORT="8888"
WAR_FILE="hnt-sensor-api-0.0.1-SNAPSHOT.war"
ROOT_WAR="ROOT.war"

# 사용법 출력
usage() {
    echo "사용법: $0 [옵션]"
    echo ""
    echo "옵션:"
    echo "  -t, --tomcat [1|2]     배포할 톰캣 서버 선택 (기본값: 2)"
    echo "  -b, --build            빌드만 수행 (배포하지 않음)"
    echo "  -d, --deploy           배포만 수행 (빌드하지 않음)"
    echo "  -c, --clean            깨끗한 빌드 수행"
    echo "  -s, --skip-tests       테스트 건너뛰기"
    echo "  -h, --help             도움말 출력"
    echo ""
    echo "예시:"
    echo "  $0 -t 1                # 톰캣1에 배포"
    echo "  $0 -t 2                # 톰캣2에 배포"
    echo "  $0 -b                  # 빌드만 수행"
    echo "  $0 -d -t 1             # 톰캣1에 배포만 수행"
    echo "  $0 -c -s               # 깨끗한 빌드 (테스트 건너뛰기)"
}

# 기본값 설정
TOMCAT_SERVER=2
BUILD_ONLY=false
DEPLOY_ONLY=false
CLEAN_BUILD=false
SKIP_TESTS=false

# 명령행 인수 파싱
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--tomcat)
            TOMCAT_SERVER="$2"
            if [[ "$TOMCAT_SERVER" != "1" && "$TOMCAT_SERVER" != "2" ]]; then
                log_error "톰캣 서버는 1 또는 2여야 합니다."
                exit 1
            fi
            shift 2
            ;;
        -b|--build)
            BUILD_ONLY=true
            shift
            ;;
        -d|--deploy)
            DEPLOY_ONLY=true
            shift
            ;;
        -c|--clean)
            CLEAN_BUILD=true
            shift
            ;;
        -s|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            log_error "알 수 없는 옵션: $1"
            usage
            exit 1
            ;;
    esac
done

# 프로젝트 디렉토리 확인
if [[ ! -d "$PROJECT_DIR" ]]; then
    log_error "프로젝트 디렉토리를 찾을 수 없습니다: $PROJECT_DIR"
    exit 1
fi

cd "$PROJECT_DIR"

# 배포 정보 출력
log_info "=== HnT Sensor API 자동 배포 시작 ==="
log_info "프로젝트 디렉토리: $PROJECT_DIR"
if [[ "$BUILD_ONLY" == true ]]; then
    log_info "모드: 빌드만 수행"
elif [[ "$DEPLOY_ONLY" == true ]]; then
    log_info "모드: 배포만 수행"
else
    log_info "모드: 빌드 + 배포"
fi

if [[ "$TOMCAT_SERVER" == "1" ]]; then
    log_info "대상 톰캣: 톰캣1 (포트: $TOMCAT1_PORT, 경로: $TOMCAT1_DIR)"
    TARGET_DIR="$TOMCAT1_DIR"
    TARGET_PORT="$TOMCAT1_PORT"
    TARGET_URL="iot.hntsolution.co.kr:$TOMCAT1_PORT"
else
    log_info "대상 톰캣: 톰캣2 (포트: $TOMCAT2_PORT, 경로: $TOMCAT2_DIR)"
    TARGET_DIR="$TOMCAT2_DIR"
    TARGET_PORT="$TOMCAT2_PORT"
    TARGET_URL="iot.hntsolution.co.kr:$TOMCAT2_PORT"
fi

# 빌드 단계
if [[ "$DEPLOY_ONLY" != true ]]; then
    log_info "=== 빌드 단계 시작 ==="
    
    # 깨끗한 빌드
    if [[ "$CLEAN_BUILD" == true ]]; then
        log_info "깨끗한 빌드 수행 중..."
        mvn clean
    fi
    
    # Maven 빌드
    log_info "Maven 빌드 수행 중..."
    if [[ "$SKIP_TESTS" == true ]]; then
        mvn package -DskipTests
    else
        mvn package
    fi
    
    # 빌드 결과 확인
    if [[ ! -f "target/$WAR_FILE" ]]; then
        log_error "빌드 실패: WAR 파일이 생성되지 않았습니다."
        exit 1
    fi
    
    log_success "빌드 완료: target/$WAR_FILE"
fi

# 배포 단계
if [[ "$BUILD_ONLY" != true ]]; then
    log_info "=== 배포 단계 시작 ==="
    
    # 대상 디렉토리 확인
    if [[ ! -d "$TARGET_DIR" ]]; then
        log_error "대상 디렉토리를 찾을 수 없습니다: $TARGET_DIR"
        exit 1
    fi
    
    # 기존 WAR 파일 백업
    if [[ -f "$TARGET_DIR/$ROOT_WAR" ]]; then
        log_info "기존 WAR 파일 백업 중..."
        BACKUP_NAME="ROOT_$(date +%Y%m%d_%H%M%S).war"
        cp "$TARGET_DIR/$ROOT_WAR" "$TARGET_DIR/$BACKUP_NAME"
        log_success "백업 완료: $BACKUP_NAME"
    fi
    
    # WAR 파일 복사
    log_info "WAR 파일 복사 중..."
    cp "target/$WAR_FILE" "$TARGET_DIR/$ROOT_WAR"
    
    if [[ -f "$TARGET_DIR/$ROOT_WAR" ]]; then
        log_success "배포 완료: $TARGET_DIR/$ROOT_WAR"
    else
        log_error "배포 실패: WAR 파일 복사에 실패했습니다."
        exit 1
    fi
    
    # 배포 후 정보 출력
    log_success "=== 배포 완료 ==="
    log_info "대상 서버: 톰캣$TOMCAT_SERVER"
    log_info "배포 경로: $TARGET_DIR/$ROOT_WAR"
    log_info "접속 URL: http://$TARGET_URL"
    log_warning "톰캣$TOMCAT_SERVER 서버를 재시작해주세요!"
fi

log_success "=== 자동 배포 스크립트 완료 ==="
