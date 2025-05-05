package com.example.traveltracker.models;

import java.io.Serializable;

public class FutureTrip implements Serializable {
    private String futureTripId;
    private String userId;
    private String city;
    private String state;
    private String plannedDate;
    private String duration;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String generatedItinerary = ""; // Store AI result

    // Required empty constructor for Firebase
    public FutureTrip() {}

    // Constructor
    public FutureTrip(String userId, String city, String state, String plannedDate, String duration) {
        this.userId = userId;
        this.city = city;
        this.state = state;
        this.plannedDate = plannedDate;
        this.duration = duration;
    }

    // --- Getters ---
    public String getFutureTripId() { return futureTripId; }
    public String getUserId() { return userId; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPlannedDate() { return plannedDate; }
    public String getDuration() { return duration; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getGeneratedItinerary() { return generatedItinerary; }

    // --- Setters ---
    public void setFutureTripId(String futureTripId) { this.futureTripId = futureTripId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setPlannedDate(String plannedDate) { this.plannedDate = plannedDate; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setGeneratedItinerary(String generatedItinerary) { this.generatedItinerary = generatedItinerary; }
}