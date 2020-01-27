package com.example.roughapp;

public class Bike {
    public String ID;
    public String OTP;

    public Bike() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Bike(String ID, String OTP) {
        this.ID = ID;
        this.OTP = OTP;
    }

}
