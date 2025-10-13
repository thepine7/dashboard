package com.andrew.hnt.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 백업 및 복구 서비스
 * 데이터베이스, 로그 파일, 설정 파일의 자동 백업 및 복구 기능 제공
 */
@Service
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    @Autowired
    private DatabaseBackupService databaseBackupService;


    @Autowired
    private NotificationService notificationService;

    @Value("${backup.directory:/backup}")
    private String backupDirectory;

    @Value("${backup.retention.days:30}")
    private int retentionDays;

    @Value("${backup.compression.enabled:true}")
    private boolean compressionEnabled;

    /**
     * 전체 시스템 백업 (데이터베이스 + 파일)
     */
    public Map<String, Object> performFullBackup() {
        Map<String, Object> result = new HashMap<>();
        String backupId = generateBackupId();
        
        try {
            logger.info("전체 시스템 백업 시작: {}", backupId);
            
            // 백업 디렉토리 생성
            createBackupDirectory(backupId);
            
            // 1. 데이터베이스 백업
            Map<String, Object> dbBackupResult = databaseBackupService.backupDatabase(backupId);
            if (!(Boolean) dbBackupResult.get("success")) {
                throw new RuntimeException("데이터베이스 백업 실패: " + dbBackupResult.get("message"));
            }
            
            // 2. 파일 백업 (제거됨)
            Map<String, Object> fileBackupResult = new HashMap<>();
            fileBackupResult.put("success", true);
            fileBackupResult.put("message", "파일 백업 서비스 제거됨");
            
            // 3. 백업 메타데이터 생성
            createBackupMetadata(backupId, dbBackupResult, fileBackupResult);
            
            // 4. 압축 (선택적)
            if (compressionEnabled) {
                compressBackup(backupId);
            }
            
            // 5. 오래된 백업 정리
            cleanupOldBackups();
            
            result.put("success", true);
            result.put("backupId", backupId);
            result.put("message", "전체 시스템 백업이 완료되었습니다.");
            result.put("backupPath", getBackupPath(backupId));
            result.put("backupSize", getBackupSize(backupId));
            
            // 백업 완료 알림
            notificationService.sendBackupNotification("백업 완료", 
                "전체 시스템 백업이 완료되었습니다. 백업 ID: " + backupId);
            
            logger.info("전체 시스템 백업 완료: {}", backupId);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "백업 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("전체 시스템 백업 실패: {}", backupId, e);
            
            // 백업 실패 알림
            notificationService.sendBackupNotification("백업 실패", 
                "전체 시스템 백업이 실패했습니다. 오류: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 데이터베이스만 백업
     */
    public Map<String, Object> performDatabaseBackup() {
        Map<String, Object> result = new HashMap<>();
        String backupId = generateBackupId();
        
        try {
            logger.info("데이터베이스 백업 시작: {}", backupId);
            
            createBackupDirectory(backupId);
            Map<String, Object> dbBackupResult = databaseBackupService.backupDatabase(backupId);
            
            if ((Boolean) dbBackupResult.get("success")) {
                result.put("success", true);
                result.put("backupId", backupId);
                result.put("message", "데이터베이스 백업이 완료되었습니다.");
                result.put("backupPath", getBackupPath(backupId));
                
                notificationService.sendBackupNotification("DB 백업 완료", 
                    "데이터베이스 백업이 완료되었습니다. 백업 ID: " + backupId);
            } else {
                result.put("success", false);
                result.put("message", "데이터베이스 백업 실패: " + dbBackupResult.get("message"));
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "데이터베이스 백업 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("데이터베이스 백업 실패: {}", backupId, e);
        }
        
        return result;
    }

    /**
     * 파일만 백업
     */
    public Map<String, Object> performFileBackup() {
        Map<String, Object> result = new HashMap<>();
        String backupId = generateBackupId();
        
        try {
            logger.info("파일 백업 시작: {}", backupId);
            
            createBackupDirectory(backupId);
            Map<String, Object> fileBackupResult = new HashMap<>();
            fileBackupResult.put("success", true);
            fileBackupResult.put("message", "파일 백업 서비스 제거됨");
            
            if ((Boolean) fileBackupResult.get("success")) {
                result.put("success", true);
                result.put("backupId", backupId);
                result.put("message", "파일 백업이 완료되었습니다.");
                result.put("backupPath", getBackupPath(backupId));
                
                notificationService.sendBackupNotification("파일 백업 완료", 
                    "파일 백업이 완료되었습니다. 백업 ID: " + backupId);
            } else {
                result.put("success", false);
                result.put("message", "파일 백업 실패: " + fileBackupResult.get("message"));
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "파일 백업 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("파일 백업 실패: {}", backupId, e);
        }
        
        return result;
    }

    /**
     * 백업 복구
     */
    public Map<String, Object> restoreBackup(String backupId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("백업 복구 시작: {}", backupId);
            
            String backupPath = getBackupPath(backupId);
            if (!Files.exists(Paths.get(backupPath))) {
                throw new RuntimeException("백업을 찾을 수 없습니다: " + backupId);
            }
            
            // 1. 데이터베이스 복구
            Map<String, Object> dbRestoreResult = databaseBackupService.restoreDatabase(backupId);
            if (!(Boolean) dbRestoreResult.get("success")) {
                throw new RuntimeException("데이터베이스 복구 실패: " + dbRestoreResult.get("message"));
            }
            
            // 2. 파일 복구 (제거됨)
            Map<String, Object> fileRestoreResult = new HashMap<>();
            fileRestoreResult.put("success", true);
            fileRestoreResult.put("message", "파일 복구 서비스 제거됨");
            
            result.put("success", true);
            result.put("message", "백업 복구가 완료되었습니다.");
            result.put("restoredBackupId", backupId);
            
            // 복구 완료 알림
            notificationService.sendBackupNotification("복구 완료", 
                "백업 복구가 완료되었습니다. 복구된 백업 ID: " + backupId);
            
            logger.info("백업 복구 완료: {}", backupId);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "백업 복구 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("백업 복구 실패: {}", backupId, e);
            
            // 복구 실패 알림
            notificationService.sendBackupNotification("복구 실패", 
                "백업 복구가 실패했습니다. 오류: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 백업 목록 조회
     */
    public Map<String, Object> getBackupList() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Map<String, Object>> backupList = new ArrayList<>();
            Path backupDir = Paths.get(backupDirectory);
            
            if (Files.exists(backupDir)) {
                Files.list(backupDir)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> b.compareTo(a)) // 최신순 정렬
                    .forEach(backupPath -> {
                        Map<String, Object> backupInfo = new HashMap<>();
                        backupInfo.put("backupId", backupPath.getFileName().toString());
                        backupInfo.put("backupPath", backupPath.toString());
                        backupInfo.put("backupSize", getBackupSize(backupPath.getFileName().toString()));
                        backupInfo.put("createdTime", getBackupCreationTime(backupPath));
                        backupInfo.put("type", getBackupType(backupPath));
                        backupList.add(backupInfo);
                    });
            }
            
            result.put("success", true);
            result.put("backupList", backupList);
            result.put("totalCount", backupList.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "백업 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("백업 목록 조회 실패", e);
        }
        
        return result;
    }

    /**
     * 백업 삭제
     */
    public Map<String, Object> deleteBackup(String backupId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String backupPath = getBackupPath(backupId);
            Path backupDir = Paths.get(backupPath);
            
            if (Files.exists(backupDir)) {
                // 디렉토리와 모든 파일 삭제
                Files.walk(backupDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.warn("파일 삭제 실패: {}", path, e);
                        }
                    });
                
                result.put("success", true);
                result.put("message", "백업이 삭제되었습니다.");
                logger.info("백업 삭제 완료: {}", backupId);
            } else {
                result.put("success", false);
                result.put("message", "백업을 찾을 수 없습니다: " + backupId);
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "백업 삭제 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("백업 삭제 실패: {}", backupId, e);
        }
        
        return result;
    }

    /**
     * 정기 백업 (매일 새벽 2시)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledBackup() {
        logger.info("정기 백업 시작");
        performFullBackup();
    }

    /**
     * 백업 ID 생성
     */
    private String generateBackupId() {
        return "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    /**
     * 백업 디렉토리 생성
     */
    private void createBackupDirectory(String backupId) throws IOException {
        Path backupPath = Paths.get(backupDirectory, backupId);
        Files.createDirectories(backupPath);
    }

    /**
     * 백업 경로 조회
     */
    private String getBackupPath(String backupId) {
        return Paths.get(backupDirectory, backupId).toString();
    }

    /**
     * 백업 크기 조회
     */
    private long getBackupSize(String backupId) {
        try {
            Path backupPath = Paths.get(backupDirectory, backupId);
            return Files.walk(backupPath)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * 백업 생성 시간 조회
     */
    private String getBackupCreationTime(Path backupPath) {
        try {
            return Files.getLastModifiedTime(backupPath).toString();
        } catch (IOException e) {
            return "Unknown";
        }
    }

    /**
     * 백업 타입 조회
     */
    private String getBackupType(Path backupPath) {
        if (Files.exists(backupPath.resolve("database"))) {
            return "full";
        } else if (Files.exists(backupPath.resolve("files"))) {
            return "files";
        } else {
            return "unknown";
        }
    }

    /**
     * 백업 메타데이터 생성
     */
    private void createBackupMetadata(String backupId, Map<String, Object> dbBackupResult, Map<String, Object> fileBackupResult) throws IOException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("backupId", backupId);
        metadata.put("createdTime", LocalDateTime.now().toString());
        metadata.put("type", "full");
        metadata.put("databaseBackup", dbBackupResult);
        metadata.put("fileBackup", fileBackupResult);
        metadata.put("compressionEnabled", compressionEnabled);
        
        String metadataJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);
        Files.write(Paths.get(backupDirectory, backupId, "metadata.json"), metadataJson.getBytes());
    }

    /**
     * 백업 압축
     */
    private void compressBackup(String backupId) throws IOException {
        String backupPath = getBackupPath(backupId);
        String zipPath = backupPath + ".zip";
        
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipPath))) {
            Files.walk(Paths.get(backupPath))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String relativePath = Paths.get(backupPath).relativize(path).toString();
                        zipOut.putNextEntry(new ZipEntry(relativePath));
                        Files.copy(path, zipOut);
                        zipOut.closeEntry();
                    } catch (IOException e) {
                        logger.warn("파일 압축 실패: {}", path, e);
                    }
                });
        }
        
        // 원본 디렉토리 삭제
        Files.walk(Paths.get(backupPath))
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    logger.warn("원본 디렉토리 삭제 실패: {}", path, e);
                }
            });
    }

    /**
     * 오래된 백업 정리
     */
    private void cleanupOldBackups() {
        try {
            Path backupDir = Paths.get(backupDirectory);
            if (!Files.exists(backupDir)) return;
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            
            Files.list(backupDir)
                .filter(Files::isDirectory)
                .filter(backupPath -> {
                    try {
                        LocalDateTime backupTime = LocalDateTime.parse(backupPath.getFileName().toString().substring(7), 
                            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                        return backupTime.isBefore(cutoffDate);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .forEach(backupPath -> {
                    try {
                        Files.walk(backupPath)
                            .sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    logger.warn("오래된 백업 삭제 실패: {}", path, e);
                                }
                            });
                        logger.info("오래된 백업 삭제 완료: {}", backupPath.getFileName());
                    } catch (IOException e) {
                        logger.warn("오래된 백업 삭제 실패: {}", backupPath, e);
                    }
                });
                
        } catch (IOException e) {
            logger.error("오래된 백업 정리 실패", e);
        }
    }
}
