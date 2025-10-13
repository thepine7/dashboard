@echo off
REM ========================================
REM HnT Sensor API 데이터베이스 복원 스크립트
REM ========================================

setlocal enabledelayedexpansion

REM 데이터베이스 설정
set "DB_HOST=hntsolution.co.kr"
set "DB_PORT=3306"
set "DB_NAME=hnt"
set "DB_USER=root"
set "DB_PASS=HntRoot123!"

REM 백업 디렉토리 설정
set "BACKUP_DIR=D:\Project\SW\CursorAI\backup\database"

echo ========================================
echo 데이터베이스 복원 스크립트
echo ========================================
echo.

REM 백업 파일 목록 표시
echo 사용 가능한 백업 파일:
echo ========================================
dir /b "%BACKUP_DIR%\hnt_backup_*.sql" 2>nul
echo ========================================
echo.

REM 사용자로부터 복원할 파일 선택
set /p "BACKUP_FILE=복원할 백업 파일명을 입력하세요 (전체 경로): "

REM 파일 존재 확인
if not exist "%BACKUP_FILE%" (
    echo 오류: 파일을 찾을 수 없습니다 - %BACKUP_FILE%
    pause
    exit /b 1
)

echo.
echo ========================================
echo 복원 경고!
echo ========================================
echo 이 작업은 현재 데이터베이스의 모든 데이터를 덮어씁니다.
echo 백업 파일: %BACKUP_FILE%
echo 대상 데이터베이스: %DB_NAME%
echo ========================================
echo.

set /p "CONFIRM=정말로 복원하시겠습니까? (y/N): "
if /i not "%CONFIRM%"=="y" (
    echo 복원이 취소되었습니다.
    pause
    exit /b 0
)

echo.
echo ========================================
echo 데이터베이스 복원 시작...
echo ========================================

REM MySQL 복원 실행
mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% < "%BACKUP_FILE%"

REM 복원 결과 확인
if %ERRORLEVEL% EQU 0 (
    echo ========================================
    echo 복원 성공!
    echo 복원 파일: %BACKUP_FILE%
    echo ========================================
) else (
    echo ========================================
    echo 복원 실패! 오류 코드: %ERRORLEVEL%
    echo ========================================
    pause
    exit /b 1
)

echo 복원 완료!
pause
