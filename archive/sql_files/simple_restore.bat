@echo off
REM ========================================
REM HnT 데이터베이스 간단 복원 스크립트
REM ========================================

echo 데이터베이스 복원을 시작합니다...
echo.

REM 사용자로부터 백업 파일명 입력
set /p "backup_file=백업 파일명을 입력하세요 (예: hnt_backup_20241223_143022.sql): "

REM 파일 존재 확인
if not exist "%backup_file%" (
    echo ❌ 파일을 찾을 수 없습니다: %backup_file%
    pause
    exit /b 1
)

echo.
echo ⚠️  경고: 이 작업은 현재 데이터베이스의 모든 데이터를 덮어씁니다!
echo 복원할 파일: %backup_file%
echo.
set /p "confirm=정말로 복원하시겠습니까? (y/N): "

if /i not "%confirm%"=="y" (
    echo 복원이 취소되었습니다.
    pause
    exit /b 0
)

echo.
echo 복원을 시작합니다...

REM MySQL 복원 실행
mysql -h hntsolution.co.kr -P 3306 -u root -pHntRoot123! hnt < %backup_file%

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ 복원이 완료되었습니다!
    echo.
) else (
    echo.
    echo ❌ 복원에 실패했습니다.
    echo.
)

pause
