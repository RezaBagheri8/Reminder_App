package com.example.reminder;

public class TaskList {
    private int id;
    private String name;
    private int taskCount; // To store the number of tasks in this list

    public TaskList(String name) {
        this.name = name;
        this.taskCount = 0; // Default to 0 tasks when created
    }

    public TaskList(int id, String name, int taskCount) {
        this.id = id;
        this.name = name;
        this.taskCount = taskCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }
}