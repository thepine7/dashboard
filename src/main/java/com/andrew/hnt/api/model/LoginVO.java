package com.andrew.hnt.api.model;

public class LoginVO {
    public String userId;    // 사용자 아이디
    public String userPass;    // 사용자 비밀번호
    public String saveId;    // 아이디 저장 여부 (PC 웹 전용, DB에는 auto_login으로 저장)
    public String token;    // 앱 토큰
    private String userAgent;  // User-Agent (PC/앱 구분)
    
    // Getters
    public String getUserId() { return userId; }
    public String getUserPass() { return userPass; }
    public String getSaveId() { return saveId; }
    public String getToken() { return token; }
    public String getUserAgent() { return userAgent; }
    
    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserPass(String userPass) { this.userPass = userPass; }
    public void setSaveId(String saveId) { this.saveId = saveId; }
    public void setToken(String token) { this.token = token; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
