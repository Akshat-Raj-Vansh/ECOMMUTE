package com.example.roughapp;

public class Bike {
    private double Latitude;
    private double Longitude;

    public Bike() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Bike(double Latitude, double Longitude) {
        this.Latitude = Latitude;
        this.Longitude = Longitude;
    }


    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double Latitude) {
        this.Latitude = Latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setLongitude(double Longitude) {
        this.Longitude = Longitude;
    }
}
