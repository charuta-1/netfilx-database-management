package com.netflix.model;

public class Actor {
    private int actorId;
    private String fullName;

    public Actor() {}

    public Actor(int actorId, String fullName) {
        this.actorId = actorId;
        this.fullName = fullName;
    }

    // Getters and Setters
    public int getActorId() { return actorId; }
    public void setActorId(int actorId) { this.actorId = actorId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    @Override
    public String toString() {
        return "Actor{" +
                "actorId=" + actorId +
                ", fullName='" + fullName + "'" +
                '}';
    }
}