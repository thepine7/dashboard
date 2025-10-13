<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>H&T Solutions - 에러 페이지</title>
    <link rel="icon" href="/images/hntbi.png" type="image/png">
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 0;
            background-color: #f5f5f5;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }
        .error-container {
            background: white;
            padding: 40px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            text-align: center;
            max-width: 500px;
            width: 90%;
        }
        .error-icon {
            font-size: 72px;
            color: #e74c3c;
            margin-bottom: 20px;
        }
        .error-title {
            font-size: 28px;
            color: #2c3e50;
            margin-bottom: 10px;
        }
        .error-message {
            font-size: 16px;
            color: #7f8c8d;
            margin-bottom: 30px;
            line-height: 1.5;
        }
        .error-details {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 30px;
            text-align: left;
        }
        .error-details h3 {
            margin-top: 0;
            color: #2c3e50;
        }
        .error-details p {
            margin: 5px 0;
            color: #6c757d;
        }
        .action-buttons {
            display: flex;
            gap: 15px;
            justify-content: center;
            flex-wrap: wrap;
        }
        .btn {
            padding: 12px 24px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            text-decoration: none;
            font-size: 14px;
            transition: background-color 0.3s;
        }
        .btn-primary {
            background-color: #3498db;
            color: white;
        }
        .btn-primary:hover {
            background-color: #2980b9;
        }
        .btn-secondary {
            background-color: #95a5a6;
            color: white;
        }
        .btn-secondary:hover {
            background-color: #7f8c8d;
        }
        .footer {
            margin-top: 30px;
            padding-top: 20px;
            border-top: 1px solid #ecf0f1;
            color: #95a5a6;
            font-size: 12px;
        }
    </style>
</head>
<body>
    <div class="error-container">
        <div class="error-icon">⚠️</div>
        <h1 class="error-title">페이지를 찾을 수 없습니다</h1>
        <p class="error-message">
            요청하신 페이지를 찾을 수 없습니다.<br>
            페이지가 이동되었거나 삭제되었을 수 있습니다.
        </p>
        
        <div class="error-details">
            <h3>에러 정보</h3>
            <p><strong>상태 코드:</strong> ${pageContext.errorData.statusCode}</p>
            <p><strong>요청 URI:</strong> ${pageContext.errorData.requestURI}</p>
            <p><strong>서버 정보:</strong> ${pageContext.servletContext.serverInfo}</p>
            <c:if test="${not empty pageContext.exception}">
                <p><strong>예외 메시지:</strong> ${pageContext.exception.message}</p>
            </c:if>
        </div>
        
        <div class="action-buttons">
            <a href="javascript:history.back()" class="btn btn-secondary">이전 페이지</a>
            <a href="/login/login" class="btn btn-primary">로그인 페이지</a>
            <a href="/main/main" class="btn btn-primary">메인 페이지</a>
        </div>
        
        <div class="footer">
            <p>H&T Solutions © 2024. 문제가 지속되면 관리자에게 문의하세요.</p>
        </div>
    </div>
</body>
</html>
