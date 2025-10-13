<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${pageTitle}</title>
    <link rel="stylesheet" href="/css/bootstrap.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <style>
        .unified-session-demo {
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
        }
        .demo-section {
            background: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 20px;
            margin-bottom: 20px;
        }
        .demo-section h4 {
            color: #495057;
            margin-bottom: 15px;
        }
        .code-example {
            background: #2d3748;
            color: #e2e8f0;
            padding: 15px;
            border-radius: 6px;
            font-family: 'Courier New', monospace;
            font-size: 14px;
            overflow-x: auto;
            margin: 10px 0;
        }
        .comparison-table {
            width: 100%;
            border-collapse: collapse;
            margin: 15px 0;
        }
        .comparison-table th,
        .comparison-table td {
            border: 1px solid #dee2e6;
            padding: 12px;
            text-align: left;
        }
        .comparison-table th {
            background-color: #e9ecef;
            font-weight: bold;
        }
        .old-way {
            background-color: #fff3cd;
        }
        .new-way {
            background-color: #d1edff;
        }
        .stats-card {
            background: white;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 20px;
            margin: 10px 0;
        }
        .stats-value {
            font-size: 24px;
            font-weight: bold;
            color: #007bff;
        }
        .stats-label {
            color: #6c757d;
            font-size: 14px;
        }
    </style>
</head>
<body>
    <div class="unified-session-demo">
        <div class="row">
            <div class="col-12">
                <h1 class="mb-4">${pageTitle}</h1>
                <p class="lead">${message}</p>
            </div>
        </div>
        
        <!-- 기존 방식 vs 통합 방식 비교 -->
        <div class="demo-section">
            <h4>🔍 기존 방식 vs 통합 방식 비교</h4>
            <table class="comparison-table">
                <thead>
                    <tr>
                        <th>구분</th>
                        <th>기존 방식</th>
                        <th>통합 방식</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><strong>세션 검증</strong></td>
                        <td class="old-way">
                            SessionValidationService.validateSession()<br>
                            + SessionSecurityService.validateSessionSecurity()<br>
                            + 권한 검증 로직
                        </td>
                        <td class="new-way">
                            UnifiedSessionService.validateSession()<br>
                            (한 번 호출로 모든 처리)
                        </td>
                    </tr>
                    <tr>
                        <td><strong>권한 검증</strong></td>
                        <td class="old-way">
                            hasPermission() 별도 호출<br>
                            권한별 메서드 분산
                        </td>
                        <td class="new-way">
                            validateSession()에 권한 파라미터<br>
                            통합된 권한 검증
                        </td>
                    </tr>
                    <tr>
                        <td><strong>모델 설정</strong></td>
                        <td class="old-way">
                            setSessionInfoToModel() 별도 호출<br>
                            수동으로 세션 정보 설정
                        </td>
                        <td class="new-way">
                            validateSession()에 model 파라미터<br>
                            자동으로 세션 정보 설정
                        </td>
                    </tr>
                    <tr>
                        <td><strong>에러 처리</strong></td>
                        <td class="old-way">
                            각 단계별 에러 처리<br>
                            중복된 에러 처리 로직
                        </td>
                        <td class="new-way">
                            통합된 에러 처리<br>
                            일관된 에러 응답
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
        
        <!-- 코드 예제 -->
        <div class="demo-section">
            <h4>💻 코드 예제</h4>
            
            <h5>기존 방식 (복잡함)</h5>
            <div class="code-example">
// 1. 기본 세션 검증
SessionValidationService.ValidationResult validationResult = sessionValidationService.validateSession(session, req, model, "A");
if (!validationResult.isValid()) {
    return "redirect:" + validationResult.getRedirectUrl();
}

// 2. 보안 검증
if (!sessionSecurityService.validateSessionSecurity(session, request)) {
    return "redirect:/login/login";
}

// 3. 권한 검증
if (!sessionManagementService.hasPermission(session, "A")) {
    return "redirect:/main/main";
}

// 4. 모델 설정
sessionManagementService.setSessionInfoToModel(session, model);

// 5. 사용자 정보 추출
String userId = validationResult.getUserId();
String userGrade = validationResult.getUserGrade();
            </div>
            
            <h5>통합 방식 (간단함)</h5>
            <div class="code-example">
// 통합 세션 검증 (한 번 호출로 모든 처리)
SessionValidationResult result = unifiedSessionService.validateSession(session, req, model, "A");
if (!result.isValid()) {
    return result.getRedirectUrl();
}

// 사용자 정보 사용
String userId = result.getUserId();
String userGrade = result.getUserGrade();
            </div>
        </div>
        
        <!-- 권한 검증 예제 -->
        <div class="demo-section">
            <h4>🔐 권한 검증 예제</h4>
            <div class="row">
                <div class="col-md-6">
                    <div class="stats-card">
                        <div class="stats-value" id="adminPermission">-</div>
                        <div class="stats-label">관리자 권한</div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="stats-card">
                        <div class="stats-value" id="userPermission">-</div>
                        <div class="stats-label">사용자 권한</div>
                    </div>
                </div>
            </div>
            <button class="btn btn-primary" onclick="checkPermissions()">권한 검증 실행</button>
        </div>
        
        <!-- 세션 통계 -->
        <div class="demo-section">
            <h4>📊 세션 통계</h4>
            <div class="row">
                <div class="col-md-3">
                    <div class="stats-card">
                        <div class="stats-value" id="totalValidations">-</div>
                        <div class="stats-label">총 검증 횟수</div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="stats-card">
                        <div class="stats-value" id="successfulValidations">-</div>
                        <div class="stats-label">성공한 검증</div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="stats-card">
                        <div class="stats-value" id="failedValidations">-</div>
                        <div class="stats-label">실패한 검증</div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="stats-card">
                        <div class="stats-value" id="successRate">-</div>
                        <div class="stats-label">성공률 (%)</div>
                    </div>
                </div>
            </div>
            <button class="btn btn-info" onclick="loadSessionStats()">통계 새로고침</button>
        </div>
        
        <!-- 마이그레이션 가이드 -->
        <div class="demo-section">
            <h4>🚀 마이그레이션 가이드</h4>
            <ol>
                <li><strong>기존 컨트롤러 수정</strong>: SessionValidationService 대신 UnifiedSessionService 사용</li>
                <li><strong>권한 검증 통합</strong>: hasPermission() 호출을 validateSession()에 통합</li>
                <li><strong>모델 설정 자동화</strong>: setSessionInfoToModel() 호출을 validateSession()에 통합</li>
                <li><strong>에러 처리 표준화</strong>: 통합된 에러 처리 로직 사용</li>
                <li><strong>통계 모니터링</strong>: 세션 관리 통계를 통한 성능 모니터링</li>
            </ol>
        </div>
    </div>
    
    <script src="/js/jquery-3.6.0.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <script>
        // 권한 검증 실행
        function checkPermissions() {
            // 관리자 권한 검증
            $.post('/unified/check-permission', {permission: 'A'}, function(data) {
                $('#adminPermission').text(data.hasPermission ? 'YES' : 'NO');
                $('#adminPermission').css('color', data.hasPermission ? '#28a745' : '#dc3545');
            });
            
            // 사용자 권한 검증
            $.post('/unified/check-permission', {permission: 'U'}, function(data) {
                $('#userPermission').text(data.hasPermission ? 'YES' : 'NO');
                $('#userPermission').css('color', data.hasPermission ? '#28a745' : '#dc3545');
            });
        }
        
        // 세션 통계 로드
        function loadSessionStats() {
            $.get('/unified/session-stats', function(data) {
                if (data.success) {
                    const stats = data.sessionStats;
                    $('#totalValidations').text(stats.totalValidations || 0);
                    $('#successfulValidations').text(stats.successfulValidations || 0);
                    $('#failedValidations').text(stats.failedValidations || 0);
                    $('#successRate').text((stats.successRate || 0).toFixed(1) + '%');
                }
            });
        }
        
        // 페이지 로드 시 초기 데이터 로드
        $(document).ready(function() {
            loadSessionStats();
        });
    </script>
</body>
</html>
