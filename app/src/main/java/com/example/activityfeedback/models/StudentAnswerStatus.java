package com.example.activityfeedback.models;

public class StudentAnswerStatus {
        public User student;
        public boolean hasAnswered;

        public StudentAnswerStatus(User student, boolean hasAnswered) {
            this.student = student;
            this.hasAnswered = hasAnswered;
        }
    public String getName() {
        return student.getName();
    }

    public String getEmail() {
        return student.getEmail();
    }

}

