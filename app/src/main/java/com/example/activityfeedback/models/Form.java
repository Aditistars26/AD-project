package com.example.activityfeedback.models;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


public class Form {
    private String formId;
    private String title;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private boolean isActive;
    private Map<String, Question> questions;

    public Form() {
        // Required for Firebase
        questions = new HashMap<>();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Form(String formId, String title, String description, String createdBy) {
        this.formId = formId;
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.lastModified = this.createdAt;
        this.isActive = true;
        this.questions = new HashMap<>();
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Map<String, Question> getQuestions() {
        return questions;
    }

    public void setQuestions(Map<String, Question> questions) {
        this.questions = questions;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void addQuestion(String id, Question question) {
        questions.put(id, question);
        this.lastModified = LocalDateTime.now();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void removeQuestion(String id) {
        questions.remove(id);
        this.lastModified = LocalDateTime.now();
    }
}