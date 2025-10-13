#!/bin/bash

# HnT Sensor API 롤백 스크립트
# 작성일: 2025-09-26
# 목적: 배포 실패 시 이전 버전으로 자동 롤백

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
TOMCAT1_DIR="Y:/docker/tomcat"
TOMCAT2_DIR="Y:/docker/tomcat2"
ROOT_WAR="ROOT.war"
BACKUP_DIR="backup"

# 사용법 출력
usage() {
    echo "사용법: $0 [옵션]"
    echo ""
    echo "옵션:"
    echo "  -t, --tomcat [1|2]     롤백할 톰캣 서버 선택 (기본값: 2)"
    echo "  -v, --version [버전]   특정 버전으로 롤백"
    echo "  -l, --list             사용 가능한 백업 버전 목록"
    echo "  -h, --help             도움말 출력"
    echo ""
    echo "예시:"
    echo "  $0 -t 1                # 톰캣1을 최신 백업으로 롤백"
    echo "  $0 -t 2 -v 20250101_120000  # 톰캣2를 특정 버전으로 롤백"
    echo "  $0 -l                  # 사용 가능한 백업 버전 목록"
}

# 기본값 설정
TOMCAT_SERVER=2
VERSION=""

# 명령행 인수 파싱
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--tomcat)
            TOMCAT_SERVER="$2"
            shift 2
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -l|--list)
            list_backups
            exit 0
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

# 톰캣 서버 검증
if [[ "$TOMCAT_SERVER" != "1" && "$TOMCAT_SERVER" != "2" ]]; then
    log_error "잘못된 톰캣 서버 번호: $TOMCAT_SERVER (1 또는 2여야 함)"
    exit 1
fi

# 대상 디렉토리 설정
if [[ "$TOMCAT_SERVER" == "1" ]]; then
    TARGET_DIR="$TOMCAT1_DIR"
    TOMCAT_NAME="톰캣1"
else
    TARGET_DIR="$TOMCAT2_DIR"
    TOMCAT_NAME="톰캣2"
fi

# 백업 목록 조회
list_backups() {
    log_info "사용 가능한 백업 버전 목록:"
    echo ""
    
    if [[ -d "$TARGET_DIR" ]]; then
        ls -la "$TARGET_DIR" | grep "ROOT_.*\.war" | awk '{print $9}' | sort -r | head -10
    else
        log_warning "대상 디렉토리를 찾을 수 없습니다: $TARGET_DIR"
    fi
}

# 헬스체크 함수
health_check() {
    local url=$1
    local max_attempts=30
    local attempt=1
    
    log_info "헬스체크 수행 중: $url"
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s "$url" > /dev/null 2>&1; then
            log_success "헬스체크 성공: $url"
            return 0
        fi
        
        log_info "헬스체크 시도 $attempt/$max_attempts..."
        sleep 10
        ((attempt++))
    done
    
    log_error "헬스체크 실패: $url"
    return 1
}

# 롤백 실행
rollback() {
    log_info "=== 롤백 시작 ==="
    log_info "대상 서버: $TOMCAT_NAME"
    log_info "대상 디렉토리: $TARGET_DIR"
    
    # 대상 디렉토리 확인
    if [[ ! -d "$TARGET_DIR" ]]; then
        log_error "대상 디렉토리를 찾을 수 없습니다: $TARGET_DIR"
        exit 1
    fi
    
    # 백업 파일 찾기
    local backup_file=""
    
    if [[ -n "$VERSION" ]]; then
        # 특정 버전으로 롤백
        backup_file="$TARGET_DIR/ROOT_${VERSION}.war"
        if [[ ! -f "$backup_file" ]]; then
            log_error "지정된 백업 파일을 찾을 수 없습니다: $backup_file"
            exit 1
        fi
    else
        # 최신 백업으로 롤백
        backup_file=$(ls -t "$TARGET_DIR"/ROOT_*.war 2>/dev/null | head -1)
        if [[ -z "$backup_file" ]]; then
            log_error "사용 가능한 백업 파일이 없습니다."
            exit 1
        fi
    fi
    
    log_info "롤백 대상 파일: $backup_file"
    
    # 현재 WAR 파일 백업 (롤백 실패 시 복구용)
    if [[ -f "$TARGET_DIR/$ROOT_WAR" ]]; then
        local current_backup="ROOT_current_$(date +%Y%m%d_%H%M%S).war"
        cp "$TARGET_DIR/$ROOT_WAR" "$TARGET_DIR/$current_backup"
        log_info "현재 WAR 파일 백업: $current_backup"
    fi
    
    # 롤백 실행
    log_info "롤백 수행 중..."
    cp "$backup_file" "$TARGET_DIR/$ROOT_WAR"
    
    if [[ -f "$TARGET_DIR/$ROOT_WAR" ]]; then
        log_success "롤백 완료: $TARGET_DIR/$ROOT_WAR"
    else
        log_error "롤백 실패: WAR 파일 복사에 실패했습니다."
        exit 1
    fi
    
    # 헬스체크 수행
    local port=$((8080 + (TOMCAT_SERVER - 1) * 80))
    local health_url="http://iot.hntsolution.co.kr:$port/health/"
    
    if health_check "$health_url"; then
        log_success "롤백 후 헬스체크 성공"
    else
        log_warning "롤백 후 헬스체크 실패 - 서버 상태를 확인해주세요"
    fi
    
    log_success "=== 롤백 완료 ==="
    log_info "대상 서버: $TOMCAT_NAME"
    log_info "롤백 파일: $backup_file"
    log_warning "$TOMCAT_NAME 서버를 재시작해주세요!"
}

# 메인 실행
main() {
    log_info "HnT Sensor API 롤백 스크립트 시작"
    
    # 현재 사용자 확인
    if [[ $EUID -eq 0 ]]; then
        log_warning "root 사용자로 실행 중입니다. 주의하세요!"
    fi
    
    # 롤백 실행
    rollback
}

# 스크립트 실행
main "$@"
