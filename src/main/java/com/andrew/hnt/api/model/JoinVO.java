package com.andrew.hnt.api.model;

/**
 * 회원가입 요청 VO (안드로이드 앱 호환용)
 * 앱에서 회원가입 시 전송하는 데이터 구조와 동일하게 구성
 */
public class JoinVO {
    private String userId;      // 사용자 아이디
    private String userPass;    // 사용자 비밀번호
    private String userNm;      // 사용자 이름
    private String userTel;     // 사용자 전화번호
    private String userEmail;   // 사용자 이메일
    private String deviceId;    // 디바이스 ID (앱 전용)
    private String token;       // FCM 토큰
    
    // Getters
    public String getUserId() { return userId; }
    public String getUserPass() { return userPass; }
    public String getUserNm() { return userNm; }
    public String getUserTel() { return userTel; }
    public String getUserEmail() { return userEmail; }
    public String getDeviceId() { return deviceId; }
    public String getToken() { return token; }
    
    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserPass(String userPass) { this.userPass = userPass; }
    public void setUserNm(String userNm) { this.userNm = userNm; }
    public void setUserTel(String userTel) { this.userTel = userTel; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setToken(String token) { this.token = token; }
}






