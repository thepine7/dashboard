# ========================================
# HnT Sensor API 데이터베이스 백업 스크립트 (PowerShell)
# ========================================

# 현재 날짜/시간 설정
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

# 데이터베이스 설정
$dbHost = "hntsolution.co.kr"
$dbPort = "3306"
$dbName = "hnt"
$dbUser = "root"
$dbPass = "HntRoot123!"

# 백업 디렉토리 설정
$backupDir = "D:\Project\SW\CursorAI\backup\database"
$backupFile = "$backupDir\hnt_backup_$timestamp.sql"

# 백업 디렉토리 생성
if (!(Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir -Force
}

Write-Host "========================================" -ForegroundColor Yellow
Write-Host "데이터베이스 백업 시작" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "백업 시간: $timestamp" -ForegroundColor White
Write-Host "백업 파일: $backupFile" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Yellow

try {
    # mysqldump 실행
    $mysqldumpArgs = @(
        "-h", $dbHost,
        "-P", $dbPort,
        "-u", $dbUser,
        "-p$dbPass",
        "--single-transaction",
        "--routines",
        "--triggers",
        "--events",
        "--hex-blob",
        "--add-drop-database",
        "--add-drop-table",
        "--add-locks",
        "--disable-keys",
        "--extended-insert",
        "--quick",
        "--lock-tables=false",
        "--databases", $dbName
    )
    
    $process = Start-Process -FilePath "mysqldump" -ArgumentList $mysqldumpArgs -RedirectStandardOutput $backupFile -Wait -PassThru
    
    if ($process.ExitCode -eq 0) {
        $fileSize = (Get-Item $backupFile).Length
        $fileSizeKB = [math]::Round($fileSize / 1KB, 2)
        
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "백업 성공!" -ForegroundColor Green
        Write-Host "백업 파일: $backupFile" -ForegroundColor White
        Write-Host "파일 크기: $fileSizeKB KB" -ForegroundColor White
        Write-Host "========================================" -ForegroundColor Green
        
        # 7일 이상 된 백업 파일 삭제
        Write-Host "오래된 백업 파일 정리 중..." -ForegroundColor Yellow
        $oldFiles = Get-ChildItem -Path $backupDir -Filter "hnt_backup_*.sql" | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-7) }
        $oldFiles | Remove-Item -Force
        Write-Host "정리 완료! 삭제된 파일 수: $($oldFiles.Count)" -ForegroundColor Green
        
    } else {
        throw "mysqldump 실행 실패. 종료 코드: $($process.ExitCode)"
    }
    
} catch {
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "백업 실패!" -ForegroundColor Red
    Write-Host "오류: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    exit 1
}

Write-Host "백업 완료!" -ForegroundColor Green
