package com.example.reminder;

import java.util.Date;

public class Task {
    private long id;
    private String title;
    private boolean isCompleted;
    private String time;
    private long taskListId = -1; // Initialize to -1 to indicate no task list
    private Date dueDate; // For storing the full date and time for the task deadline

    public Task(String title, String time) {
        this.title = title;
        this.time = time;
        this.isCompleted = false;
    }

    public Task(String title, String time, boolean isCompleted) {
        this.title = title;
        this.time = time;
        this.isCompleted = isCompleted;
    }

    public Task(String title, String time, boolean isCompleted, Date dueDate) {
        this.title = title;
        this.time = time;
        this.isCompleted = isCompleted;
        this.dueDate = dueDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDisplayText() {
        return title;
    }

    public long getTaskListId() {
        return taskListId;
    }

    public void setTaskListId(long taskListId) {
        this.taskListId = taskListId;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }
}