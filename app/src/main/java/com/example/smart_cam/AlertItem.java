package com.example.smart_cam;

public class AlertItem {
    private int id;
    private String title;
    private String description;
    private String location;
    private String time;
    private String status;
    private String severity;
    private String attendedUser;

    public AlertItem(int id, String title, String description, String location,
                      String time, String status, String severity, String attendedUser) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.time = time;
        this.status = status;
        this.severity = severity;
        this.attendedUser = attendedUser;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
    public String getSeverity() { return severity; }
    public String getAttendedUser() { return attendedUser; }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAttendedUser(String attendedUser) {
        this.attendedUser = attendedUser;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
