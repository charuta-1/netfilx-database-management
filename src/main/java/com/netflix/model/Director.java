package com.netflix.model;

public class Director {
    private int directorId;
    private String fullName;

    public Director() {}

    public Director(int directorId, String fullName) {
        this.directorId = directorId;
        this.fullName = fullName;
    }

    // Getters and Setters
    public int getDirectorId() { return directorId; }
    public void setDirectorId(int directorId) { this.directorId = directorId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    @Override
    public String toString() {
        return "Director{" +
                "directorId=" + directorId +
                ", fullName='" + fullName + "'" +
                '}';
    }
}