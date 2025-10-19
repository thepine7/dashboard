package com.andrew.hnt.api.model;

public class UserInfo {
    public int no;    // 번호 - 사용자 고유 아이디
    public String userNm;    // 사용자명
    public String userTel;    // 사용자 전화번호
    public String userEmail;    // 사용자 메일주소
    public String userId;    // 사용자 아이디
    public String userPass;    // 사용자 비밀번호
    public String userGrade;    // 사용자 등급 - A : Admin / U : User
    public String useYn;    // 사용 여부
    public String delYn;    // 삭제 여부
    public String instId;    // 입력자 아이디
    public String mdfId;    // 수정자 아이디
    public String token;    // 앱 토큰
    public String sensorId;
    public String loginDtm = "";
    public String logoutDtm = "";
    public String loginYn;
    public String mainId;
    public String parentUserId;
    public String userAgent;    // User-Agent (PC/앱 구분)
    public String autoLogin;    // 자동 로그인 여부 (PC/앱 공통)
    
    // Getters
    public int getNo() { return no; }
    public String getUserNm() { return userNm; }
    public String getUserTel() { return userTel; }
    public String getUserEmail() { return userEmail; }
    public String getUserId() { return userId; }
    public String getUserPass() { return userPass; }
    public String getUserGrade() { return userGrade; }
    public String getUseYn() { return useYn; }
    public String getDelYn() { return delYn; }
    public String getInstId() { return instId; }
    public String getMdfId() { return mdfId; }
    public String getToken() { return token; }
    public String getSensorId() { return sensorId; }
    public String getLoginDtm() { return loginDtm; }
    public String getLogoutDtm() { return logoutDtm; }
    public String getLoginYn() { return loginYn; }
    public String getMainId() { return mainId; }
    public String getParentUserId() { return parentUserId; }
    public String getUserAgent() { return userAgent; }
    public String getAutoLogin() { return autoLogin; }
    
    // Setters
    public void setNo(int no) { this.no = no; }
    public void setUserNm(String userNm) { this.userNm = userNm; }
    public void setUserTel(String userTel) { this.userTel = userTel; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserPass(String userPass) { this.userPass = userPass; }
    public void setUserGrade(String userGrade) { this.userGrade = userGrade; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public void setDelYn(String delYn) { this.delYn = delYn; }
    public void setInstId(String instId) { this.instId = instId; }
    public void setMdfId(String mdfId) { this.mdfId = mdfId; }
    public void setToken(String token) { this.token = token; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public void setLoginDtm(String loginDtm) { this.loginDtm = loginDtm; }
    public void setLogoutDtm(String logoutDtm) { this.logoutDtm = logoutDtm; }
    public void setLoginYn(String loginYn) { this.loginYn = loginYn; }
    public void setMainId(String mainId) { this.mainId = mainId; }
    public void setParentUserId(String parentUserId) { this.parentUserId = parentUserId; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setAutoLogin(String autoLogin) { this.autoLogin = autoLogin; }
}
