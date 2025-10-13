package com.andrew.hnt.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 데이터베이스 백업 서비스
 * MySQL 데이터베이스의 백업 및 복구 기능 제공
 */
@Service
public class DatabaseBackupService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupService.class);

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${backup.directory:/backup}")
    private String backupDirectory;

    /**
     * 데이터베이스 백업
     */
    public Map<String, Object> backupDatabase(String backupId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("데이터베이스 백업 시작: {}", backupId);
            
            // 백업 디렉토리 생성
            String dbBackupDir = Paths.get(backupDirectory, backupId, "database").toString();
            Files.createDirectories(Paths.get(dbBackupDir));
            
            // 데이터베이스 이름 추출
            String databaseName = extractDatabaseName(databaseUrl);
            
            // mysqldump 명령어 실행
            String dumpFileName = String.format("hnt_%s.sql", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            String dumpFilePath = Paths.get(dbBackupDir, dumpFileName).toString();
            
            ProcessBuilder pb = new ProcessBuilder(
                "mysqldump",
                "--host=" + extractHost(databaseUrl),
                "--port=" + extractPort(databaseUrl),
                "--user=" + databaseUsername,
                "--password=" + databasePassword,
                "--single-transaction",
                "--routines",
                "--triggers",
                "--events",
                "--hex-blob",
                "--add-drop-database",
                "--create-options",
                "--disable-keys",
                "--extended-insert",
                "--quick",
                "--lock-tables=false",
                databaseName
            );
            
            pb.redirectOutput(new File(dumpFilePath));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // 백업 파일 압축
                String compressedFile = compressDumpFile(dumpFilePath);
                
                // 원본 파일 삭제
                Files.deleteIfExists(Paths.get(dumpFilePath));
                
                result.put("success", true);
                result.put("message", "데이터베이스 백업이 완료되었습니다.");
                result.put("backupFile", compressedFile);
                result.put("backupSize", Files.size(Paths.get(compressedFile)));
                result.put("databaseName", databaseName);
                result.put("backupTime", LocalDateTime.now().toString());
                
                logger.info("데이터베이스 백업 완료: {}", compressedFile);
            } else {
                // 에러 로그 읽기
                String errorLog = readProcessError(process);
                throw new RuntimeException("mysqldump 실행 실패 (exit code: " + exitCode + "): " + errorLog);
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "데이터베이스 백업 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("데이터베이스 백업 실패: {}", backupId, e);
        }
        
        return result;
    }

    /**
     * 데이터베이스 복구
     */
    public Map<String, Object> restoreDatabase(String backupId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("데이터베이스 복구 시작: {}", backupId);
            
            // 백업 파일 찾기
            String dbBackupDir = Paths.get(backupDirectory, backupId, "database").toString();
            Path backupDir = Paths.get(dbBackupDir);
            
            if (!Files.exists(backupDir)) {
                throw new RuntimeException("데이터베이스 백업 디렉토리를 찾을 수 없습니다: " + dbBackupDir);
            }
            
            // 백업 파일 찾기 (압축된 파일 우선)
            Optional<Path> backupFile = Files.list(backupDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".sql.gz") || path.toString().endsWith(".sql"))
                .findFirst();
            
            if (!backupFile.isPresent()) {
                throw new RuntimeException("데이터베이스 백업 파일을 찾을 수 없습니다.");
            }
            
            String backupFilePath = backupFile.get().toString();
            String databaseName = extractDatabaseName(databaseUrl);
            
            // 복구 전 백업 (안전장치)
            Map<String, Object> safetyBackup = backupDatabase(backupId + "_safety");
            if (!(Boolean) safetyBackup.get("success")) {
                throw new RuntimeException("복구 전 안전 백업 실패: " + safetyBackup.get("message"));
            }
            
            // 압축 해제 (필요한 경우)
            String sqlFile = backupFilePath;
            if (backupFilePath.endsWith(".gz")) {
                sqlFile = decompressDumpFile(backupFilePath);
            }
            
            // mysql 명령어 실행
            ProcessBuilder pb = new ProcessBuilder(
                "mysql",
                "--host=" + extractHost(databaseUrl),
                "--port=" + extractPort(databaseUrl),
                "--user=" + databaseUsername,
                "--password=" + databasePassword,
                databaseName
            );
            
            pb.redirectInput(new File(sqlFile));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                result.put("success", true);
                result.put("message", "데이터베이스 복구가 완료되었습니다.");
                result.put("restoredDatabase", databaseName);
                result.put("restoreTime", LocalDateTime.now().toString());
                result.put("safetyBackupId", backupId + "_safety");
                
                logger.info("데이터베이스 복구 완료: {}", databaseName);
            } else {
                // 에러 로그 읽기
                String errorLog = readProcessError(process);
                throw new RuntimeException("mysql 실행 실패 (exit code: " + exitCode + "): " + errorLog);
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "데이터베이스 복구 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("데이터베이스 복구 실패: {}", backupId, e);
        }
        
        return result;
    }

    /**
     * 데이터베이스 상태 확인
     */
    public Map<String, Object> checkDatabaseStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String databaseName = extractDatabaseName(databaseUrl);
            
            // 테이블 목록 조회
            ProcessBuilder pb = new ProcessBuilder(
                "mysql",
                "--host=" + extractHost(databaseUrl),
                "--port=" + extractPort(databaseUrl),
                "--user=" + databaseUsername,
                "--password=" + databasePassword,
                "--execute=SHOW TABLES;",
                databaseName
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                String output = readProcessOutput(process);
                String[] tables = output.split("\n");
                
                result.put("success", true);
                result.put("databaseName", databaseName);
                result.put("tableCount", tables.length - 1); // 헤더 제외
                result.put("tables", Arrays.asList(tables));
                result.put("status", "healthy");
                
                logger.info("데이터베이스 상태 확인 완료: {} 테이블", tables.length - 1);
            } else {
                result.put("success", false);
                result.put("message", "데이터베이스 연결 실패");
                result.put("status", "unhealthy");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "데이터베이스 상태 확인 중 오류가 발생했습니다: " + e.getMessage());
            result.put("status", "error");
            logger.error("데이터베이스 상태 확인 실패", e);
        }
        
        return result;
    }

    /**
     * 데이터베이스 URL에서 호스트 추출
     */
    private String extractHost(String url) {
        // jdbc:mysql://hntsolution.co.kr:3306/hnt?...
        String[] parts = url.substring(13).split(":");
        return parts[0];
    }

    /**
     * 데이터베이스 URL에서 포트 추출
     */
    private String extractPort(String url) {
        // jdbc:mysql://hntsolution.co.kr:3306/hnt?...
        String[] parts = url.substring(13).split(":");
        if (parts.length > 1) {
            return parts[1].split("/")[0];
        }
        return "3306"; // 기본 포트
    }

    /**
     * 데이터베이스 URL에서 데이터베이스 이름 추출
     */
    private String extractDatabaseName(String url) {
        // jdbc:mysql://hntsolution.co.kr:3306/hnt?...
        String[] parts = url.split("/");
        if (parts.length > 3) {
            return parts[3].split("\\?")[0];
        }
        return "hnt"; // 기본 데이터베이스
    }

    /**
     * 덤프 파일 압축
     */
    private String compressDumpFile(String dumpFilePath) throws IOException {
        String compressedFile = dumpFilePath + ".gz";
        
        try (FileInputStream fis = new FileInputStream(dumpFilePath);
             FileOutputStream fos = new FileOutputStream(compressedFile);
             java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, length);
            }
        }
        
        return compressedFile;
    }

    /**
     * 덤프 파일 압축 해제
     */
    private String decompressDumpFile(String compressedFilePath) throws IOException {
        String decompressedFile = compressedFilePath.replace(".gz", "");
        
        try (FileInputStream fis = new FileInputStream(compressedFilePath);
             FileOutputStream fos = new FileOutputStream(decompressedFile);
             java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(fis)) {
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
        
        return decompressedFile;
    }

    /**
     * 프로세스 출력 읽기
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    /**
     * 프로세스 에러 읽기
     */
    private String readProcessError(Process process) throws IOException {
        StringBuilder error = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
        }
        return error.toString();
    }
}
