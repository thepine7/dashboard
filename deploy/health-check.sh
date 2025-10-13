#!/bin/bash

# HnT Sensor API 헬스체크 스크립트
# 작성일: 2025-09-26
# 목적: 배포 후 서비스 상태 확인 및 자동 롤백

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
TOMCAT1_URL="http://iot.hntsolution.co.kr:8080"
TOMCAT2_URL="http://iot.hntsolution.co.kr:8888"
HEALTH_ENDPOINT="/health/"
MAX_ATTEMPTS=30
CHECK_INTERVAL=10
ROLLBACK_ON_FAILURE=true

# 사용법 출력
usage() {
    echo "사용법: $0 [옵션]"
    echo ""
    echo "옵션:"
    echo "  -t, --tomcat [1|2]     체크할 톰캣 서버 선택 (기본값: 2)"
    echo "  -a, --all              모든 톰캣 서버 체크"
    echo "  -m, --max-attempts [N] 최대 시도 횟수 (기본값: 30)"
    echo "  -i, --interval [N]     체크 간격(초) (기본값: 10)"
    echo "  -n, --no-rollback      실패 시 롤백하지 않음"
    echo "  -h, --help             도움말 출력"
    echo ""
    echo "예시:"
    echo "  $0 -t 1                # 톰캣1 헬스체크"
    echo "  $0 -a                  # 모든 톰캣 서버 헬스체크"
    echo "  $0 -t 2 -m 60 -i 5     # 톰캣2를 60회, 5초 간격으로 체크"
}

# 기본값 설정
TOMCAT_SERVER=2
CHECK_ALL=false

# 명령행 인수 파싱
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--tomcat)
            TOMCAT_SERVER="$2"
            shift 2
            ;;
        -a|--all)
            CHECK_ALL=true
            shift
            ;;
        -m|--max-attempts)
            MAX_ATTEMPTS="$2"
            shift 2
            ;;
        -i|--interval)
            CHECK_INTERVAL="$2"
            shift 2
            ;;
        -n|--no-rollback)
            ROLLBACK_ON_FAILURE=false
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

# 헬스체크 함수
check_health() {
    local url=$1
    local server_name=$2
    local attempt=1
    
    log_info "=== $server_name 헬스체크 시작 ==="
    log_info "URL: $url$HEALTH_ENDPOINT"
    log_info "최대 시도 횟수: $MAX_ATTEMPTS"
    log_info "체크 간격: ${CHECK_INTERVAL}초"
    
    while [[ $attempt -le $MAX_ATTEMPTS ]]; do
        log_info "[$attempt/$MAX_ATTEMPTS] 헬스체크 시도 중..."
        
        # HTTP 상태 코드 확인
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" "$url$HEALTH_ENDPOINT" || echo "000")
        
        if [[ "$http_code" == "200" ]]; then
            log_success "$server_name 헬스체크 성공 (HTTP $http_code)"
            
            # 응답 내용 확인
            local response=$(curl -s "$url$HEALTH_ENDPOINT" || echo "")
            if [[ -n "$response" ]]; then
                log_info "응답 내용: $response"
            fi
            
            return 0
        else
            log_warning "$server_name 헬스체크 실패 (HTTP $http_code)"
        fi
        
        if [[ $attempt -lt $MAX_ATTEMPTS ]]; then
            log_info "${CHECK_INTERVAL}초 후 재시도..."
            sleep $CHECK_INTERVAL
        fi
        
        ((attempt++))
    done
    
    log_error "$server_name 헬스체크 최종 실패"
    return 1
}

# 롤백 실행
execute_rollback() {
    local server_num=$1
    
    if [[ "$ROLLBACK_ON_FAILURE" == "true" ]]; then
        log_warning "헬스체크 실패로 인한 롤백 실행..."
        
        if [[ -f "./rollback.sh" ]]; then
            ./rollback.sh -t "$server_num"
        else
            log_error "롤백 스크립트를 찾을 수 없습니다: ./rollback.sh"
            return 1
        fi
    else
        log_warning "롤백이 비활성화되어 있습니다."
    fi
}

# 톰캣1 헬스체크
check_tomcat1() {
    if check_health "$TOMCAT1_URL" "톰캣1"; then
        log_success "톰캣1 헬스체크 완료"
        return 0
    else
        log_error "톰캣1 헬스체크 실패"
        execute_rollback 1
        return 1
    fi
}

# 톰캣2 헬스체크
check_tomcat2() {
    if check_health "$TOMCAT2_URL" "톰캣2"; then
        log_success "톰캣2 헬스체크 완료"
        return 0
    else
        log_error "톰캣2 헬스체크 실패"
        execute_rollback 2
        return 1
    fi
}

# 모든 톰캣 서버 헬스체크
check_all() {
    log_info "=== 모든 톰캣 서버 헬스체크 시작 ==="
    
    local tomcat1_result=0
    local tomcat2_result=0
    
    # 톰캣1 체크
    check_tomcat1 || tomcat1_result=1
    
    # 톰캣2 체크
    check_tomcat2 || tomcat2_result=1
    
    # 결과 요약
    log_info "=== 헬스체크 결과 요약 ==="
    if [[ $tomcat1_result -eq 0 ]]; then
        log_success "톰캣1: 정상"
    else
        log_error "톰캣1: 실패"
    fi
    
    if [[ $tomcat2_result -eq 0 ]]; then
        log_success "톰캣2: 정상"
    else
        log_error "톰캣2: 실패"
    fi
    
    # 전체 결과
    if [[ $tomcat1_result -eq 0 && $tomcat2_result -eq 0 ]]; then
        log_success "모든 톰캣 서버가 정상입니다."
        return 0
    else
        log_error "일부 톰캣 서버에 문제가 있습니다."
        return 1
    fi
}

# 메인 실행
main() {
    log_info "HnT Sensor API 헬스체크 스크립트 시작"
    
    # 현재 시간
    log_info "시작 시간: $(date)"
    
    # 톰캣 서버 검증
    if [[ "$CHECK_ALL" == "false" && "$TOMCAT_SERVER" != "1" && "$TOMCAT_SERVER" != "2" ]]; then
        log_error "잘못된 톰캣 서버 번호: $TOMCAT_SERVER (1 또는 2여야 함)"
        exit 1
    fi
    
    # 헬스체크 실행
    if [[ "$CHECK_ALL" == "true" ]]; then
        check_all
    elif [[ "$TOMCAT_SERVER" == "1" ]]; then
        check_tomcat1
    else
        check_tomcat2
    fi
    
    # 종료 시간
    log_info "종료 시간: $(date)"
}

# 스크립트 실행
main "$@"
