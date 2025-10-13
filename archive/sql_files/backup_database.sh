#!/bin/bash
# ========================================
# HnT Sensor API 데이터베이스 백업 스크립트
# ========================================

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 현재 날짜/시간 설정
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 데이터베이스 설정
DB_HOST="hntsolution.co.kr"
DB_PORT="3306"
DB_NAME="hnt"
DB_USER="root"
DB_PASS="HntRoot123!"

# 백업 디렉토리 설정
BACKUP_DIR="/backup/database"
BACKUP_FILE="${BACKUP_DIR}/hnt_backup_${TIMESTAMP}.sql"

# 백업 디렉토리 생성
mkdir -p "${BACKUP_DIR}"

echo -e "${YELLOW}========================================"
echo "데이터베이스 백업 시작"
echo "========================================"
echo "백업 시간: ${TIMESTAMP}"
echo "백업 파일: ${BACKUP_FILE}"
echo "========================================${NC}"

# mysqldump 실행
mysqldump -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -p"${DB_PASS}" \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    --hex-blob \
    --add-drop-database \
    --add-drop-table \
    --add-locks \
    --disable-keys \
    --extended-insert \
    --quick \
    --lock-tables=false \
    --databases "${DB_NAME}" > "${BACKUP_FILE}"

# 백업 결과 확인
if [ $? -eq 0 ]; then
    echo -e "${GREEN}========================================"
    echo "백업 성공!"
    echo "백업 파일: ${BACKUP_FILE}"
    echo "파일 크기: $(du -h "${BACKUP_FILE}" | cut -f1)"
    echo "========================================${NC}"
    
    # 7일 이상 된 백업 파일 삭제
    echo -e "${YELLOW}오래된 백업 파일 정리 중...${NC}"
    find "${BACKUP_DIR}" -name "hnt_backup_*.sql" -mtime +7 -delete 2>/dev/null
    echo -e "${GREEN}정리 완료!${NC}"
    
else
    echo -e "${RED}========================================"
    echo "백업 실패! 오류 코드: $?"
    echo "========================================${NC}"
    exit 1
fi

echo -e "${GREEN}백업 완료!${NC}"
