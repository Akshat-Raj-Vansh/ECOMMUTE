package com.example.roughapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class SplashScreen extends AppCompatActivity {

    GoogleSignInAccount account;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        account = GoogleSignIn.getLastSignedInAccount(SplashScreen.this);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (account == null) {
                    startActivity(new Intent(SplashScreen.this, LoginScreen.class));
                    finish();
                } else {
                    startActivity(new Intent(SplashScreen.this, MapScreen.class));
                    finish();
                }
            }
        }, 1000);
    }
}
