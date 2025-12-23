package com.netflix.model;

public class Country {
    private int countryId;
    private String name;

    public Country() {}

    public Country(int countryId, String name) {
        this.countryId = countryId;
        this.name = name;
    }

    // Getters and Setters
    public int getCountryId() { return countryId; }
    public void setCountryId(int countryId) { this.countryId = countryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "Country{" +
                "countryId=" + countryId +
                ", name='" + name + "'" +
                '}';
    }
}