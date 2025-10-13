package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.service.SecurityAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 보안 감사 컨트롤러
 * 보안 감사, 취약점 스캔, 보안 이벤트 관리 기능 제공
 */
@RestController
@RequestMapping("/admin/security")
public class SecurityController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityController.class);

    @Autowired
    private SecurityAuditService securityAuditService;

    /**
     * 보안 감사 실행
     */
    @PostMapping("/audit")
    public Map<String, Object> performSecurityAudit() {
        logger.info("보안 감사 실행 요청");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 수동 보안 감사 실행
            Map<String, Object> auditResult = new HashMap<>();
            
            auditResult.put("loginAnalysis", securityAuditService.analyzeLoginAttempts());
            auditResult.put("suspiciousActivity", securityAuditService.analyzeSuspiciousActivities());
            auditResult.put("securityConfig", securityAuditService.validateSecurityConfiguration());
            auditResult.put("vulnerabilityScan", securityAuditService.performVulnerabilityScan());
            auditResult.put("securityEvents", securityAuditService.getSecurityEventsSummary());
            
            result.put("success", true);
            result.put("message", "보안 감사가 완료되었습니다.");
            result.put("auditResult", auditResult);
            result.put("auditTime", System.currentTimeMillis());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "보안 감사 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("보안 감사 실행 실패", e);
        }
        
        return result;
    }

    /**
     * 보안 이벤트 조회
     */
    @GetMapping("/events")
    public Map<String, Object> getSecurityEvents(
            @RequestParam(defaultValue = "ALL") String eventType,
            @RequestParam(defaultValue = "24") int hours) {
        
        logger.info("보안 이벤트 조회 요청: eventType={}, hours={}", eventType, hours);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> eventsSummary = securityAuditService.getSecurityEventsSummary();
            
            result.put("success", true);
            result.put("eventsSummary", eventsSummary);
            result.put("requestedEventType", eventType);
            result.put("requestedHours", hours);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "보안 이벤트 조회 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("보안 이벤트 조회 실패", e);
        }
        
        return result;
    }

    /**
     * 취약점 스캔 실행
     */
    @PostMapping("/vulnerability-scan")
    public Map<String, Object> performVulnerabilityScan() {
        logger.info("취약점 스캔 실행 요청");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> scanResult = securityAuditService.performVulnerabilityScan();
            
            result.put("success", true);
            result.put("message", "취약점 스캔이 완료되었습니다.");
            result.put("scanResult", scanResult);
            result.put("scanTime", System.currentTimeMillis());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "취약점 스캔 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("취약점 스캔 실행 실패", e);
        }
        
        return result;
    }

    /**
     * 보안 설정 검증
     */
    @GetMapping("/config-validation")
    public Map<String, Object> validateSecurityConfiguration() {
        logger.info("보안 설정 검증 요청");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> validationResult = securityAuditService.validateSecurityConfiguration();
            
            result.put("success", true);
            result.put("validationResult", validationResult);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "보안 설정 검증 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("보안 설정 검증 실패", e);
        }
        
        return result;
    }

    /**
     * 보안 대시보드 데이터
     */
    @GetMapping("/dashboard")
    public Map<String, Object> getSecurityDashboard() {
        logger.info("보안 대시보드 데이터 조회 요청");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            // 보안 이벤트 요약
            dashboard.put("securityEvents", securityAuditService.getSecurityEventsSummary());
            
            // 로그인 시도 분석
            dashboard.put("loginAnalysis", securityAuditService.analyzeLoginAttempts());
            
            // 의심스러운 활동 분석
            dashboard.put("suspiciousActivity", securityAuditService.analyzeSuspiciousActivities());
            
            // 보안 설정 검증
            dashboard.put("securityConfig", securityAuditService.validateSecurityConfiguration());
            
            result.put("success", true);
            result.put("dashboard", dashboard);
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "보안 대시보드 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("보안 대시보드 데이터 조회 실패", e);
        }
        
        return result;
    }

    /**
     * 보안 이벤트 기록
     */
    @PostMapping("/events/record")
    public Map<String, Object> recordSecurityEvent(
            @RequestParam String eventType,
            @RequestParam String description,
            @RequestParam String severity,
            @RequestBody(required = false) Map<String, Object> details) {
        
        logger.info("보안 이벤트 기록 요청: eventType={}, severity={}", eventType, severity);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            securityAuditService.recordSecurityEvent(eventType, description, severity, details);
            
            result.put("success", true);
            result.put("message", "보안 이벤트가 기록되었습니다.");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "보안 이벤트 기록 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("보안 이벤트 기록 실패", e);
        }
        
        return result;
    }

    /**
     * 로그인 실패 기록
     */
    @PostMapping("/events/failed-login")
    public Map<String, Object> recordFailedLogin(
            HttpServletRequest request,
            @RequestParam String ipAddress) {
        
        // 세션에서 사용자 정보 가져오기
        HttpSession session = request.getSession(false);
        String userId = null;
        if (session != null) {
            userId = (String) session.getAttribute("userId");
        }
        
        logger.info("로그인 실패 기록 요청: userId={}, ipAddress={}", userId, ipAddress);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (userId != null) {
                securityAuditService.recordFailedLogin(userId, ipAddress);
            } else {
                logger.warn("세션에서 사용자 정보를 찾을 수 없습니다.");
            }
            
            result.put("success", true);
            result.put("message", "로그인 실패가 기록되었습니다.");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "로그인 실패 기록 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("로그인 실패 기록 실패", e);
        }
        
        return result;
    }

    /**
     * 의심스러운 활동 기록
     */
    @PostMapping("/events/suspicious-activity")
    public Map<String, Object> recordSuspiciousActivity(
            @RequestParam String activityType,
            @RequestParam String description) {
        
        logger.info("의심스러운 활동 기록 요청: activityType={}", activityType);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            securityAuditService.recordSuspiciousActivity(activityType, description);
            
            result.put("success", true);
            result.put("message", "의심스러운 활동이 기록되었습니다.");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "의심스러운 활동 기록 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("의심스러운 활동 기록 실패", e);
        }
        
        return result;
    }

    /**
     * 보안 리포트 생성
     */
    @GetMapping("/report")
    public Map<String, Object> generateSecurityReport(
            @RequestParam(defaultValue = "7") int days) {
        
        logger.info("보안 리포트 생성 요청: {} days", days);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> report = new HashMap<>();
            
            // 보안 이벤트 요약
            report.put("securityEvents", securityAuditService.getSecurityEventsSummary());
            
            // 로그인 시도 분석
            report.put("loginAnalysis", securityAuditService.analyzeLoginAttempts());
            
            // 의심스러운 활동 분석
            report.put("suspiciousActivity", securityAuditService.analyzeSuspiciousActivities());
            
            // 보안 설정 검증
            report.put("securityConfig", securityAuditService.validateSecurityConfiguration());
            
            // 취약점 스캔 결과
            report.put("vulnerabilityScan", securityAuditService.performVulnerabilityScan());
            
            report.put("reportPeriod", days + " days");
            report.put("generatedAt", System.currentTimeMillis());
            
            result.put("success", true);
            result.put("report", report);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "보안 리포트 생성 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("보안 리포트 생성 실패", e);
        }
        
        return result;
    }

    /**
     * 보안 설정 조회
     */
    @GetMapping("/settings")
    public Map<String, Object> getSecuritySettings() {
        logger.info("보안 설정 조회 요청");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("auditEnabled", true);
            settings.put("vulnerabilityScanEnabled", true);
            settings.put("eventLoggingEnabled", true);
            settings.put("failedLoginThreshold", 5);
            settings.put("suspiciousActivityThreshold", 10);
            settings.put("auditSchedule", "0 0 3 * * ?"); // 매일 새벽 3시
            settings.put("notificationChannels", new String[]{"email", "slack"});
            
            result.put("success", true);
            result.put("settings", settings);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "보안 설정 조회 중 오류가 발생했습니다: " + e.getMessage());
            logger.error("보안 설정 조회 실패", e);
        }
        
        return result;
    }
}
