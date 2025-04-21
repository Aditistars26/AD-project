package com.example.activityfeedback.models;

import java.util.List;

public class User {
    private String userId;
    private String email;
    private String name;
    private String phone;
    private String department;
    private int year;
    private String rollNumber;
    private String enrollmentId;
    private String className;
    private List<String> electives;
    private String role;

    public User() {
        // Required for Firebase
    }

    public User(String userId, String email, String name, String rollNumber, String role) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.rollNumber = rollNumber;
        this.role = role;
    }



    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isProfessor() {
        return "professor".equals(role);
    }

    public boolean isStudent() {
        return "student".equals(role);
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(String enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public List<String> getElectives() {
        return electives;
    }

    public void setElectives(List<String> electives) {
        this.electives = electives;
    }

    public String getPhone(){
        return phone;
    }
    public String getDepartment(){
        return department;
    }
    public void setPhone(String phone) {
        this.phone=phone;
    }

    public void setDepartment(String department) {
        this.department= department;
    }
}