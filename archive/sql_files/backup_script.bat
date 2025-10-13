@echo off
REM HnT Sensor API 자동 백업 스크립트
REM 작성일: 2025-09-16
REM 작성자: AI Assistant

echo ========================================
echo HnT Sensor API 자동 백업 시작
echo ========================================

REM 현재 날짜와 시간 가져오기
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "YY=%dt:~2,2%" & set "YYYY=%dt:~0,4%" & set "MM=%dt:~4,2%" & set "DD=%dt:~6,2%"
set "HH=%dt:~8,2%" & set "Min=%dt:~10,2%" & set "Sec=%dt:~12,2%"
set "timestamp=%YYYY%%MM%%DD%_%HH%%Min%%Sec%"

REM 백업 디렉토리 생성
set "backup_dir=D:\Project\SW\CursorAI\backup\%timestamp%"
mkdir "%backup_dir%"

echo 백업 디렉토리 생성: %backup_dir%

REM 1. 소스 코드 백업
echo 소스 코드 백업 중...
xcopy "D:\Project\SW\CursorAI\tomcat22\src" "%backup_dir%\src" /E /I /Y
xcopy "D:\Project\SW\CursorAI\tomcat22\pom.xml" "%backup_dir%\" /Y
xcopy "D:\Project\SW\CursorAI\tomcat22\README.md" "%backup_dir%\" /Y

REM 2. 설정 파일 백업
echo 설정 파일 백업 중...
xcopy "D:\Project\SW\CursorAI\tomcat22\src\main\resources\application.yml" "%backup_dir%\config\" /Y
xcopy "D:\Project\SW\CursorAI\tomcat22\src\main\resources\mapper" "%backup_dir%\config\mapper" /E /I /Y

REM 3. 데이터베이스 백업 (MySQL)
echo 데이터베이스 백업 중...
set "db_backup_file=%backup_dir%\database_backup_%timestamp%.sql"
mysqldump -h hntsolution.co.kr -P 3306 -u root -pHntRoot123! hnt > "%db_backup_file%"

if %errorlevel% equ 0 (
    echo 데이터베이스 백업 완료: %db_backup_file%
) else (
    echo 데이터베이스 백업 실패!
)

REM 4. WAR 파일 백업
echo WAR 파일 백업 중...
if exist "D:\Project\SW\CursorAI\tomcat22\target\hnt-sensor-api-0.0.1-SNAPSHOT.war" (
    copy "D:\Project\SW\CursorAI\tomcat22\target\hnt-sensor-api-0.0.1-SNAPSHOT.war" "%backup_dir%\hnt-sensor-api-0.0.1-SNAPSHOT_%timestamp%.war"
    echo WAR 파일 백업 완료
) else (
    echo WAR 파일이 존재하지 않습니다.
)

REM 5. 톰캣 배포 파일 백업
echo 톰캣 배포 파일 백업 중...
if exist "Y:\docker\tomcat\ROOT.war" (
    copy "Y:\docker\tomcat\ROOT.war" "%backup_dir%\tomcat1_ROOT_%timestamp%.war"
    echo 톰캣1 배포 파일 백업 완료
)

if exist "Y:\docker\tomcat2\ROOT.war" (
    copy "Y:\docker\tomcat2\ROOT.war" "%backup_dir%\tomcat2_ROOT_%timestamp%.war"
    echo 톰캣2 배포 파일 백업 완료
)

REM 6. 백업 정보 파일 생성
echo 백업 정보 파일 생성 중...
echo HnT Sensor API 백업 정보 > "%backup_dir%\backup_info.txt"
echo ======================================== >> "%backup_dir%\backup_info.txt"
echo 백업 일시: %timestamp% >> "%backup_dir%\backup_info.txt"
echo 백업 디렉토리: %backup_dir% >> "%backup_dir%\backup_info.txt"
echo ======================================== >> "%backup_dir%\backup_info.txt"
echo. >> "%backup_dir%\backup_info.txt"
echo 백업된 파일들: >> "%backup_dir%\backup_info.txt"
echo - 소스 코드 (src/) >> "%backup_dir%\backup_info.txt"
echo - 설정 파일 (config/) >> "%backup_dir%\backup_info.txt"
echo - 데이터베이스 백업 (database_backup_%timestamp%.sql) >> "%backup_dir%\backup_info.txt"
echo - WAR 파일 (hnt-sensor-api-0.0.1-SNAPSHOT_%timestamp%.war) >> "%backup_dir%\backup_info.txt"
echo - 톰캣 배포 파일 (tomcat*_ROOT_%timestamp%.war) >> "%backup_dir%\backup_info.txt"
echo. >> "%backup_dir%\backup_info.txt"
echo 복구 방법: >> "%backup_dir%\backup_info.txt"
echo 1. 소스 코드 복구: xcopy "%backup_dir%\src" "D:\Project\SW\CursorAI\tomcat22\" /E /I /Y >> "%backup_dir%\backup_info.txt"
echo 2. 데이터베이스 복구: mysql -h hntsolution.co.kr -P 3306 -u root -p hnt ^< "%backup_dir%\database_backup_%timestamp%.sql" >> "%backup_dir%\backup_info.txt"
echo 3. WAR 파일 복구: copy "%backup_dir%\hnt-sensor-api-0.0.1-SNAPSHOT_%timestamp%.war" "D:\Project\SW\CursorAI\tomcat22\target\" >> "%backup_dir%\backup_info.txt"

REM 7. 백업 완료 메시지
echo ========================================
echo 백업 완료!
echo 백업 위치: %backup_dir%
echo ========================================

REM 8. 오래된 백업 파일 정리 (30일 이상 된 파일 삭제)
echo 오래된 백업 파일 정리 중...
forfiles /p "D:\Project\SW\CursorAI\backup" /d -30 /c "cmd /c if @isdir==TRUE rmdir /s /q @path"

echo 백업 스크립트 실행 완료!
pause

