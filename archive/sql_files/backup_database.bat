@echo off
REM ========================================
REM HnT Sensor API 데이터베이스 백업 스크립트
REM ========================================

setlocal enabledelayedexpansion

REM 현재 날짜/시간 설정
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "YY=%dt:~2,2%" & set "YYYY=%dt:~0,4%" & set "MM=%dt:~4,2%" & set "DD=%dt:~6,2%"
set "HH=%dt:~8,2%" & set "Min=%dt:~10,2%" & set "Sec=%dt:~12,2%"
set "timestamp=%YYYY%%MM%%DD%_%HH%%Min%%Sec%"

REM 데이터베이스 설정
set "DB_HOST=hntsolution.co.kr"
set "DB_PORT=3306"
set "DB_NAME=hnt"
set "DB_USER=root"
set "DB_PASS=HntRoot123!"

REM 백업 디렉토리 설정
set "BACKUP_DIR=D:\Project\SW\CursorAI\backup\database"
set "BACKUP_FILE=%BACKUP_DIR%\hnt_backup_%timestamp%.sql"

REM 백업 디렉토리 생성
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo ========================================
echo 데이터베이스 백업 시작
echo ========================================
echo 백업 시간: %timestamp%
echo 백업 파일: %BACKUP_FILE%
echo ========================================

REM mysqldump 실행
mysqldump -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% ^
    --single-transaction ^
    --routines ^
    --triggers ^
    --events ^
    --hex-blob ^
    --add-drop-database ^
    --add-drop-table ^
    --add-locks ^
    --disable-keys ^
    --extended-insert ^
    --quick ^
    --lock-tables=false ^
    --databases %DB_NAME% > "%BACKUP_FILE%"

REM 백업 결과 확인
if %ERRORLEVEL% EQU 0 (
    echo ========================================
    echo 백업 성공!
    echo 백업 파일: %BACKUP_FILE%
    echo 파일 크기: 
    for %%A in ("%BACKUP_FILE%") do echo %%~zA bytes
    echo ========================================
    
    REM 7일 이상 된 백업 파일 삭제
    echo 오래된 백업 파일 정리 중...
    forfiles /p "%BACKUP_DIR%" /m hnt_backup_*.sql /d -7 /c "cmd /c del @path" 2>nul
    echo 정리 완료!
    
) else (
    echo ========================================
    echo 백업 실패! 오류 코드: %ERRORLEVEL%
    echo ========================================
    pause
    exit /b 1
)

echo 백업 완료!
pause
