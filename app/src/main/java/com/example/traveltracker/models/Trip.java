package com.example.traveltracker.models;

import java.io.Serializable;


public class Trip implements Serializable {
    private String tripId;
    private String city;
    private String state;
    private String dateVisited;
    private String duration;
    private String userId;

    private double latitude = 0.0;
    private double longitude = 0.0;

    public Trip() {}

    // Constructor without imageURLs
    public Trip(String city, String state, String dateVisited, String duration, String userId) {
        this.city = city;
        this.state = state;
        this.dateVisited = dateVisited;
        this.duration = duration;
        this.userId = userId;

    }

    // --- Getters ---
    public String getTripId() { return tripId; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getDateVisited() { return dateVisited; }
    public String getDuration() { return duration; }
    public String getUserId() { return userId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }


    // --- Setters ---
    public void setTripId(String tripId) { this.tripId = tripId; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setDateVisited(String dateVisited) { this.dateVisited = dateVisited; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    @Override
    public String toString() {
        return city + ", " + state + " (" + dateVisited + ")";
    }
}