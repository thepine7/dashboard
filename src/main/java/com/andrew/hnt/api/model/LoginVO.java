package com.andrew.hnt.api.model;

public class LoginVO {
    public String userId;    // 사용자 아이디
    public String userPass;    // 사용자 비밀번호
    public String saveId;    // 아이디 저장 여부
    public String token;    // 앱 토큰
    
    // Getters
    public String getUserId() { return userId; }
    public String getUserPass() { return userPass; }
    public String getSaveId() { return saveId; }
    public String getToken() { return token; }
    
    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserPass(String userPass) { this.userPass = userPass; }
    public void setSaveId(String saveId) { this.saveId = saveId; }
    public void setToken(String token) { this.token = token; }
}
