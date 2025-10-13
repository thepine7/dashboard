package com.andrew.hnt.api.model;

public class ExcelTest {

    public ExcelTest(String name, String birth, String phoneNumber, String address) {
        this.name = name;
        this.birth = birth;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public String name;
    public String birth;
    public String phoneNumber;
    public String address;
    
    // Getters
    public String getName() { return name; }
    public String getBirth() { return birth; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
    
    // Setters
    public void setName(String name) { this.name = name; }
    public void setBirth(String birth) { this.birth = birth; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAddress(String address) { this.address = address; }
}
