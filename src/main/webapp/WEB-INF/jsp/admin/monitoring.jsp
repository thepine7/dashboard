<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>시스템 모니터링 - H&T Solutions</title>
    <link rel="stylesheet" href="/css/templatemo_main.css">
    <link rel="stylesheet" href="/css/common-buttons.css">
    <link rel="stylesheet" href="/css/responsive-common.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    
    <!-- 세션 정보 공통 템플릿 -->
    <jsp:include page="/WEB-INF/jsp/common/session-info.jsp" />
    <style>
        .monitoring-card {
            background: white;
            border-radius: 10px;
            padding: 20px;
            margin-bottom: 20px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .metric-value {
            font-size: 2rem;
            font-weight: bold;
            color: #2c3e50;
        }
        .metric-label {
            color: #7f8c8d;
            font-size: 0.9rem;
        }
        .status-normal { color: #27ae60; }
        .status-warning { color: #f39c12; }
        .status-critical { color: #e74c3c; }
    </style>
</head>
<body>
    <div class="container-fluid">
        <div class="row">
            <div class="col-12">
                <h2><i class="bi bi-graph-up"></i> 시스템 모니터링</h2>
            </div>
        </div>
        
        <!-- 시스템 메트릭 -->
        <div class="row">
            <div class="col-md-3">
                <div class="monitoring-card">
                    <div class="metric-label">CPU 사용률</div>
                    <div class="metric-value" id="cpuUsage">0%</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="monitoring-card">
                    <div class="metric-label">메모리 사용률</div>
                    <div class="metric-value" id="memoryUsage">0%</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="monitoring-card">
                    <div class="metric-label">활성 세션</div>
                    <div class="metric-value" id="activeSessions">0</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="monitoring-card">
                    <div class="metric-label">MQTT 연결</div>
                    <div class="metric-value" id="mqttStatus">연결됨</div>
                </div>
            </div>
        </div>
        
        <!-- 성능 차트 -->
        <div class="row">
            <div class="col-md-6">
                <div class="monitoring-card">
                    <h5>응답 시간 추이</h5>
                    <canvas id="responseTimeChart"></canvas>
                </div>
            </div>
            <div class="col-md-6">
                <div class="monitoring-card">
                    <h5>요청 수 추이</h5>
                    <canvas id="requestCountChart"></canvas>
                </div>
            </div>
        </div>
        
        <!-- 로그 모니터링 -->
        <div class="row">
            <div class="col-12">
                <div class="monitoring-card">
                    <h5>실시간 로그</h5>
                    <div id="logContainer" style="height: 300px; overflow-y: auto; background: #f8f9fa; padding: 10px; border-radius: 5px;">
                        <div class="log-entry">시스템 모니터링을 시작합니다...</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script>
        // 차트 초기화
        const responseTimeCtx = document.getElementById('responseTimeChart').getContext('2d');
        const requestCountCtx = document.getElementById('requestCountChart').getContext('2d');
        
        const responseTimeChart = new Chart(responseTimeCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: '응답 시간 (ms)',
                    data: [],
                    borderColor: '#3498db',
                    backgroundColor: 'rgba(52, 152, 219, 0.1)',
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
        
        const requestCountChart = new Chart(requestCountCtx, {
            type: 'bar',
            data: {
                labels: [],
                datasets: [{
                    label: '요청 수',
                    data: [],
                    backgroundColor: '#27ae60'
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
        
        // 모니터링 데이터 업데이트
        function updateMonitoringData() {
            fetch('/admin/monitoring/metrics')
                .then(response => response.json())
                .then(data => {
                    // 시스템 메트릭 업데이트
                    document.getElementById('cpuUsage').textContent = data.cpuUsage + '%';
                    document.getElementById('memoryUsage').textContent = data.memoryUsage + '%';
                    document.getElementById('activeSessions').textContent = data.activeSessions;
                    document.getElementById('mqttStatus').textContent = data.mqttConnected ? '연결됨' : '연결 끊김';
                    
                    // 차트 업데이트
                    updateChart(responseTimeChart, data.responseTimeHistory);
                    updateChart(requestCountChart, data.requestCountHistory);
                })
                .catch(error => {
                    console.error('모니터링 데이터 업데이트 실패:', error);
                });
        }
        
        function updateChart(chart, data) {
            const now = new Date().toLocaleTimeString();
            chart.data.labels.push(now);
            chart.data.datasets[0].data.push(data);
            
            // 최대 20개 데이터 포인트 유지
            if (chart.data.labels.length > 20) {
                chart.data.labels.shift();
                chart.data.datasets[0].data.shift();
            }
            
            chart.update();
        }
        
        // 5초마다 데이터 업데이트
        setInterval(updateMonitoringData, 5000);
        
        // 초기 데이터 로드
        updateMonitoringData();
    </script>
</body>
</html>
