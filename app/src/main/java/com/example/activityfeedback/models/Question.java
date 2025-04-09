package com.example.activityfeedback.models;

import java.util.List;

public class Question {
    private String id;
    private String type;
    private String text;
    private List<String> options;
    private int min;
    private int max;
    private Integer order;

    public Question() {
        // Required for Firebase
    }

    public Question(String id, String type, String text) {
        this.id = id;
        this.type = type;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    // Make sure your Question class has an order field
 public void setOrder(Integer order) {
     this.order = order;
 }
//
 public Integer getOrder() {
     return order;
 }

}