package com.example.roughapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.example.roughapp.Barcode.BarcodeCaptureActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import static android.view.View.GONE;

public class MapScreen extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, View.OnDragListener {

    private GoogleMap userLocation;
    private GoogleMap bikeLocations;
    private CardView openScanner;
    private CardView bookedRide;
    private Button more, endride;
    private String BookingTime, EndingTime;
    private CircularImageView showCurrentPosition, bikeOptions, status, powerStatus;
    private ConstraintLayout navMenu;
    private CircularImageView profilePic;
    private GoogleSignInAccount account;
    private FirebaseFunctions firebaseFunctions;
    private GoogleSignInClient client;
    private GoogleSignInOptions gso;
    private String cycleID = "00000";
    private TextView showDetails, bookingTime;
    private Animation clockwise, anticlockise;
    private Button history, profile, logout, wallet, settings;
    private LocationManager locationManager;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseFirestore firebaseFirestore;
    private LatLng previousLocation;
    private SharedPreferences localValues;
    private LinearLayout details;
    private static final int RC_BARCODE_CAPTURE = 9001;
    private int showCurrPos = 0;
    private static final String TAG = "BarcodeMain";
    int isNavOpen = 0;
    private Marker bike1, bike2;
    private Bitmap bikeMarker;
    private Bitmap stationMarker;
    private String value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_screen);
        initialize();
        localValues = getSharedPreferences("Ride Details", MODE_PRIVATE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        firebaseFunctions = FirebaseFunctions.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        account = GoogleSignIn.getLastSignedInAccount(this);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        client = GoogleSignIn.getClient(this, gso);
        setImageToMarker();
        clockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_clockwise);
        anticlockise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_anticlockwise);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        assert account != null;
        Uri photo_url = account.getPhotoUrl();
        if (photo_url != null) {
            Glide.with(MapScreen.this).load(photo_url)
                    .into(profilePic);
        }
        openScanner.setOnClickListener(this);
        openScanner.setOnDragListener(this);
        more.setOnClickListener(this);
        profilePic.setOnClickListener(this);
        profile.setOnClickListener(this);
        logout.setOnClickListener(this);
        wallet.setOnClickListener(this);
        settings.setOnClickListener(this);
        history.setOnClickListener(this);
        showCurrentPosition.setOnClickListener(this);
        bookedRide.setOnClickListener(this);
        endride.setOnClickListener(this);
        bikeOptions.setOnClickListener(this);
        status.setOnClickListener(this);
        powerStatus.setOnClickListener(this);
        displayMap();
        NewAccountBonus();
        checkPreviousBooking();
        updateStatus();
    }

    public void initialize() {
        openScanner = findViewById(R.id.scan_qr);
        showCurrentPosition = findViewById(R.id.showCurrentPos);
        more = findViewById(R.id.more_button);
        profilePic = findViewById(R.id.profile);
        navMenu = findViewById(R.id.nav_menu);
        history = findViewById(R.id.history_nav);
        profile = findViewById(R.id.profile_nav);
        logout = findViewById(R.id.logout_nav);
        wallet = findViewById(R.id.wallet_nav);
        settings = findViewById(R.id.settings_nav);
        bookedRide = findViewById(R.id.bookedRide);
        endride = findViewById(R.id.end_ride);
        showDetails = findViewById(R.id.showRideDetails);
        details = findViewById(R.id.ride_details);
        bookingTime = findViewById(R.id.booking_time);
        powerStatus = findViewById(R.id.statusPower);
        previousLocation = new LatLng(70, 70);
        status = findViewById(R.id.status);
        status.setVisibility(GONE);
        bookedRide.setVisibility(View.GONE);
        details.setVisibility(View.GONE);
        showDetails.setVisibility(View.GONE);
        openScanner.setVisibility(View.GONE);
        bikeOptions = findViewById(R.id.lockUnlock);
        powerStatus.setVisibility(GONE);
        bikeOptions.setVisibility(GONE);
    }

    private void NewAccountBonus() {
        value = "0";
        openScanner.setVisibility(View.VISIBLE);
        DocumentReference documentReference = firebaseFirestore.collection("Users").document(account.getId());
        documentReference.get().
                addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                value = document.get("New").toString();
                                if (value.equals("1")) {
                                    new AlertDialog.Builder(MapScreen.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Welcome to ECOMMUTE!")
                                            .setMessage("You are eligible for free ride for first 30 minutes!")
                                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            }).show();
                                }
                                Log.d("DEBUG", "DocumentSnapshot data: " + document.getData());
                            } else {
                                Log.d("DEBUG", "Found null document");
                            }
                        } else {
                            Log.d("DEBUG", "Error finding the document");
                        }
                    }
                });
        firebaseFirestore.collection("Users").document(account.getId()).update("New", "0");
    }

    private void checkPreviousBooking() {
        value = "NULL";
        DocumentReference documentReference = firebaseFirestore.collection("Users").document(account.getId());
        documentReference.get().
                addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                value = document.get("Bike ID").toString();
                                if (!value.equals("NULL")) {
                                    // showTimeElapsed();
                                    cycleID = value;
                                    updateStatus();
                                    Toast.makeText(MapScreen.this, value, Toast.LENGTH_SHORT).show();
                                    BookingTime = document.get("Booking Time").toString();
                                    bookingTime.setText(BookingTime);
                                    bookedRide.setVisibility(View.VISIBLE);
                                    details.setVisibility(View.GONE);
                                    showDetails.setVisibility(View.VISIBLE);
                                    openScanner.setVisibility(View.GONE);
                                    bikeOptions.setVisibility(View.VISIBLE);
                                } else if (value.equals("NULL")) {
                                    bookedRide.setVisibility(View.GONE);
                                    details.setVisibility(View.GONE);
                                    showDetails.setVisibility(View.GONE);
                                    openScanner.setVisibility(View.VISIBLE);
                                    bikeOptions.setVisibility(GONE);
                                }
                            }
                            Log.d("DEBUG", "DocumentSnapshot data: " + document.getData());
                        } else {
                            Log.d("DEBUG", "Found null document");
                        }
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        userLocation = googleMap;
        bikeLocations = googleMap;
        userLocation.setMyLocationEnabled(true);
        bike1 = bikeLocations.addMarker(new MarkerOptions()
                .position(new LatLng(50, 50))
                .title("Bike's Location"));
        bike1.setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
        bike2 = bikeLocations.addMarker(new MarkerOptions()
                .position(new LatLng(50, 50))
                .title("Bike's Location"));
        bike1.setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
        bike2.setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
        displayMap();
        addBikes();
        showStation();
    }


    private void addBikes() {
        Log.d("LocationX", "Reached Inside the function");
        DatabaseReference databaseReference = firebaseDatabase.getReference().child("Location of Bikes");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ds) {
                Bike bikeLocation = new Bike();
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("12345").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("12345").getValue(Bike.class)).getLongitude());
                bike1.setPosition(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()));
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("13598").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("13598").getValue(Bike.class)).getLongitude());
                bike2.setPosition(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });
    }

    private void showStation() {

        Log.d("LocationX", "Reached Inside the function");
        DatabaseReference databaseReference = firebaseDatabase.getReference();
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ds) {
                Bike bikeLocation = new Bike();
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("Station").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("Station").getValue(Bike.class)).getLongitude());
                bikeLocations.addMarker(new MarkerOptions()
                        .position(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()))
                        .title("Station's Location")).setIcon(BitmapDescriptorFactory.fromBitmap(stationMarker));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });
    }

    private void displayMap() {

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng latLng = new LatLng(latitude, longitude);
                    previousLocation = latLng;
                    if (showCurrPos == 1) {
                        userLocation.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18.5f));
                        showCurrPos = 1 - showCurrPos;
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng latLng = new LatLng(latitude, longitude);
                    previousLocation = latLng;
                    if (showCurrPos == 1) {
                        userLocation.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18.5f));
                        showCurrPos = 1 - showCurrPos;
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }
    }


    private void setImageToMarker() {
        int height = 100;
        int width = 100;
        BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.cycle);
        Bitmap b = bitmapdraw.getBitmap();
        bikeMarker = Bitmap.createScaledBitmap(b, width, height, false);
        BitmapDrawable bitmapdraw2 = (BitmapDrawable) getResources().getDrawable(R.drawable.stand);
        Bitmap b2 = bitmapdraw2.getBitmap();
        stationMarker = Bitmap.createScaledBitmap(b2, width, height, false);
    }


    private void updateStatus() {
        firebaseDatabase.getReference("Bikes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String lockS = dataSnapshot.child(cycleID).child("Lock").getValue(String.class);
                String powerS = dataSnapshot.child(cycleID).child("Power").getValue(String.class);
                assert lockS != null;
                if (lockS.equals("Locked")) {
                    status.setImageResource(R.drawable.locked);
                } else if (lockS.equals("Unlocked")) {
                    status.setImageResource(R.drawable.unlocked);
                }
                assert powerS != null;
                if (powerS.equals("On")) {
                    powerStatus.setImageResource(R.drawable.on);
                } else if (powerS.equals("Off")) {
                    powerStatus.setImageResource(R.drawable.off);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scan_qr:
                checkMinBalance();
                break;
            case R.id.lockUnlock:
                if (status.getVisibility() == View.GONE) {
                    status.setVisibility(View.VISIBLE);
                    powerStatus.setVisibility(View.VISIBLE);
                } else {
                    status.setVisibility(GONE);
                    powerStatus.setVisibility(GONE);
                }
                break;
            case R.id.statusPower:
                setPowerStatus();
                break;
            case R.id.status:
                setLockStatus();
                break;
            case R.id.more_button:
                isNavOpen = 1 - isNavOpen;
                if (isNavOpen == 1) {
                    openScanner.setVisibility(GONE);
                    showCurrentPosition.setVisibility(GONE);
                    navMenu.setVisibility(View.VISIBLE);
                    more.startAnimation(clockwise);
                } else {
                    openScanner.setVisibility(View.VISIBLE);
                    showCurrentPosition.setVisibility(View.VISIBLE);
                    navMenu.setVisibility(GONE);
                    more.startAnimation(anticlockise);
                }
                break;
            case R.id.profile:
            case R.id.profile_nav:
                Toast.makeText(MapScreen.this, "Profile Page will come here", Toast.LENGTH_SHORT).show();
                break;
            case R.id.logout_nav:
                new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Log Out")
                        .setMessage("Do you want to log out?")
                        .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                logOut();
                            }
                        }).setNegativeButton("no", null).show();
                break;
            case R.id.wallet_nav:
                startActivity(new Intent(MapScreen.this, WalletScreen.class));
                finish();
                break;
            case R.id.showCurrentPos:
                userLocation.moveCamera(CameraUpdateFactory.newLatLngZoom(previousLocation, 18.5f));
                showCurrPos = 1 - showCurrPos;
                displayMap();
                break;
            case R.id.bookedRide:
                if (showDetails.getVisibility() == View.VISIBLE) {
                    showDetails.setVisibility(GONE);
                    details.setVisibility(View.VISIBLE);
                } else {
                    showDetails.setVisibility(View.VISIBLE);
                    details.setVisibility(GONE);
                }
                break;

            case R.id.end_ride:
                new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("End ride?")
                        .setMessage("Do you want to end this ride?")
                        .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkLockBeforeEndingRide();
                            }
                        }).setNegativeButton("no", null).show();
                break;
        }
    }

    private void scanQRCode() {
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, false);
        startActivityForResult(intent, RC_BARCODE_CAPTURE);
        Log.d(TAG, "Reached the end of scanQRCode");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    final Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                    new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Book Ride")
                            .setMessage("Do you want to book this ride?")
                            .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    searchCycle(barcode.displayValue);
                                }
                            }).setNegativeButton("no", null).show();
                } else {
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void searchCycle(final String display) {
        Toast.makeText(this, display, Toast.LENGTH_SHORT).show();
        if (display.equals("12345") || display.equals("13598")) {
            firebaseDatabase.getReference("Bikes").child(display).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String booking = dataSnapshot.child("Status").getValue(String.class);
                    assert booking != null;
                    if (booking.equals("Free")) {
                        String time = Calendar.getInstance().getTime().toString();
                        //bookedTime = Calendar.getInstance().getTimeInMillis();
                        BookingTime = Calendar.getInstance().getTime().toString();
                        localValues.edit().putString("Booking Time", BookingTime).apply();
                        Map<String, Object> rideDetails = new HashMap<>();
                        rideDetails.put("Status", "Booked");
                        rideDetails.put("Booked By", account.getId());
                        rideDetails.put("Start Time", time);
                        bookingTime.setText(time);
                        // Add a new document with a generated ID
                        firebaseFirestore.collection("Bikes").document(display)
                                .set(rideDetails)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("Firestore-Login", "DocumentSnapshot successfully written!");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("Firestore-Login", "Error writing document", e);
                                    }
                                });
                        firebaseDatabase.getReference("Bikes").child(display).child("Power").setValue("Off");
                        firebaseDatabase.getReference("Bikes").child(display).child("Lock").setValue("Unlocked");
                        firebaseDatabase.getReference("Bikes").child(display).child("Status").setValue("Booked");
                        rideDetails.clear();
                        rideDetails.put("Bike ID", display);
                        rideDetails.put("Booking Time", time);
                        firebaseFirestore.collection("Users").document(account.getId())
                                .update(rideDetails);
                        cycleID = display;
                        bookedRide.setVisibility(View.VISIBLE);
                        details.setVisibility(View.GONE);
                        showDetails.setVisibility(View.VISIBLE);
                        openScanner.setVisibility(View.GONE);
                        bikeOptions.setVisibility(View.VISIBLE);

                    } else
                        Toast.makeText(MapScreen.this, "Ride already Booked!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else
            Toast.makeText(MapScreen.this, "Aren't you a smart little boy!", Toast.LENGTH_SHORT).show();
    }

    private Task<String> getTimeDifference(String start, String end) {
        Map<String, Object> data = new HashMap<>();
        data.put("startTime", start);
        data.put("endTime", end);
        data.put("plan", 0);
        Log.d("FunctionsCustom", "Inside the getTimeDifference function");
        return firebaseFunctions
                .getHttpsCallable("getTimeDifference")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Log.d("Functions", "Insidest the getTime function");
                        //int result = (int)task.getResult().getData();

                        task.addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                            @Override
                            public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                                //  Object result = task.getResult().getData();
                                Log.d("FunctionsCustom", "time diff " + task.getResult().getData().toString());
                                extractDetails(task.getResult().getData().toString());
                            }
                        });
                        return "Voila";
                    }
                });
    }

    private void checkMinBalance() {
        DocumentReference documentReference = firebaseFirestore.collection("Users").document(account.getId());
        documentReference.get().
                addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                if (Integer.valueOf(Objects.requireNonNull(document.getString("Dues"))) == 0 && Integer.valueOf(Objects.requireNonNull(document.getString("Balance"))) > 50)
                                    scanQRCode();
                                else {
                                    new AlertDialog.Builder(MapScreen.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Warning!")
                                            .setMessage("You don't have enough balance to book a ride! Please clear your previous dues and have a minimum amount of Rs. 150 in your wallet to book this ride.")
                                            .setPositiveButton("Pay", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    startActivity(new Intent(MapScreen.this, WalletScreen.class));
                                                    finish();
                                                }
                                            }).show();
                                }
                                Log.d("DEBUG", "DocumentSnapshot data: " + document.getData());
                            } else {
                                Log.d("DEBUG", "Found null document");
                            }
                        } else {
                            Log.d("DEBUG", "Error finding the document");
                        }
                    }
                });
    }

    private void setPowerStatus() {
        firebaseDatabase.getReference("Bikes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String s = dataSnapshot.child(cycleID).child("Power").getValue(String.class);
                final String lockS = dataSnapshot.child(cycleID).child("Lock").getValue(String.class);
                if (s.equals("Off")) {
                    powerStatus.setImageResource(R.drawable.off);
                    new AlertDialog.Builder(MapScreen.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Power Status")
                            .setMessage("Do you want to turn ON the engine of the ride?")
                            .setPositiveButton("Turn ON", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (lockS.equals("Unlocked")) {
                                        firebaseDatabase.getReference("Bikes").child(cycleID).child("Power").setValue("On");
                                        powerStatus.setImageResource(R.drawable.on);
//                                    } else if(lockS.equals("Locked")){
//                                        new AlertDialog.Builder(MapScreen.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Power Status")
//                                                .setMessage("Ride is locked! Can't turn ON the engine!")
//                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                                                    @Override
//                                                    public void onClick(DialogInterface dialog, int which) {
//                                                        dialog.dismiss();
//                                                    }
//                                                });
                                    }
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
                } else if (s.equals("On")) {
                    powerStatus.setImageResource(R.drawable.on);
                    new AlertDialog.Builder(MapScreen.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Power Status")
                            .setMessage("Do you want to turn off the engine of the ride?")
                            .setPositiveButton("Turn OFF", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    firebaseDatabase.getReference("Bikes").child(cycleID).child("Power").setValue("Off");
                                    powerStatus.setImageResource(R.drawable.off);
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setLockStatus() {
        firebaseDatabase.getReference("Bikes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String s = dataSnapshot.child(cycleID).child("Lock").getValue(String.class);
                Toast.makeText(MapScreen.this, s, Toast.LENGTH_SHORT).show();
                assert s != null;
                if (s.equals("Locked")) {
                    status.setImageResource(R.drawable.locked);
                    new AlertDialog.Builder(MapScreen.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Warning!")
                            .setMessage("Do you want to unlock the ride?")
                            .setPositiveButton("Unlock", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    firebaseDatabase.getReference("Bikes").child(cycleID).child("Lock").setValue("Unlocked");
                                    status.setImageResource(R.drawable.unlocked);
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
                    Toast.makeText(MapScreen.this, "Changing the icon to Locked", Toast.LENGTH_SHORT).show();
                } else if (s.equals("Unlocked")) {
                    status.setImageResource(R.drawable.unlocked);
                    Toast.makeText(MapScreen.this, "Changing the icon to Unlocked", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkLockBeforeEndingRide() {
        firebaseDatabase.getReference("Bikes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String lockS = dataSnapshot.child(cycleID).child("Lock").getValue(String.class);
                String powerS = dataSnapshot.child(cycleID).child("Power").getValue(String.class);
                assert powerS != null;
                assert lockS != null;
                if (lockS.equals("Unlocked") || powerS.equals("On")) {
                    new AlertDialog.Builder(MapScreen.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("WARNING!")
                            .setMessage("Make sure that engine is OFF and the ride is LOCKED!")
                            .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                } else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void storeHistory(String booking_time, String end_time, String time_elapsed, String cost, String plan) {
        Map<String, Object> data = new HashMap<>();
        data.put("Booking Time", booking_time);
        data.put("End Time", end_time);
        data.put("Time Elapsed", time_elapsed);
        data.put("Cost", cost);
        data.put("Plan", plan);
        firebaseFirestore.collection("Bikes").document(cycleID).collection(account.getId()).document(booking_time)
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Firestore-Login", "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore-Login", "Error writing document", e);
                    }
                });
        Map<String, Object> dataforUser = new HashMap<>();
        dataforUser.put("Booking Time", booking_time);
        dataforUser.put("End Time", end_time);
        dataforUser.put("Time Elapsed", time_elapsed);
        dataforUser.put("Cost", cost);
        dataforUser.put("Plan", plan);
        firebaseFirestore.collection("Users").document(account.getId()).collection(cycleID).document(booking_time)
                .set(dataforUser)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Firestore-Login", "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore-Login", "Error writing document", e);
                    }
                });
        cycleID = "00000";
    }


    private void antiTheft() {
        firebaseDatabase.getReference("Location of Bikes").child(cycleID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                firebaseDatabase.getReference("Bikes").child(cycleID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String state = dataSnapshot.child("Lock").getValue(String.class);
                        assert state != null;
                        if (state.equals("Locked")) {
                            Toast.makeText(MapScreen.this, "Ride Compromised!", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void endRide() {
        Toast.makeText(this, cycleID, Toast.LENGTH_SHORT).show();
        Map<String, Object> rideDetails = new HashMap<>();
        rideDetails.put("Status", "Free");
        rideDetails.put("Booked By", account.getId());
        rideDetails.put("End Time", Calendar.getInstance().getTime().toString());
        //  endedTime = Calendar.getInstance().getTimeInMillis();
        // BookingTime = localValues.getString("Booking Values", "");
        EndingTime = Calendar.getInstance().getTime().toString();
        getTimeDifference(BookingTime, EndingTime).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    if (e instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                        FirebaseFunctionsException.Code code = ffe.getCode();
                        Log.d("Functions", code.toString());
                    }
                    Log.w(TAG, "addNumbers:onFailure", e);
                    return;
                }
                String result = task.getResult();
                Log.d("Functions", String.valueOf(result));
            }
        });
        firebaseFirestore
                .collection("Bikes")
                .document(cycleID)
                .update(rideDetails);
        rideDetails.clear();
        rideDetails.put("Bike ID", "NULL");
        rideDetails.put("Booking Time", "NULL");
        firebaseFirestore.collection("Users").document(account.getId()).update(rideDetails);
        firebaseDatabase.getReference("Bikes").child(cycleID).child("Status").setValue("Free");
        localValues.edit().putString("Booking Time", "").apply();
        // handler.removeCallbacks(runnable);
        bookedRide.setVisibility(GONE);
        openScanner.setVisibility(View.VISIBLE);
        bikeOptions.setVisibility(GONE);
        powerStatus.setVisibility(GONE);
        status.setVisibility(GONE);
    }

    private void extractDetails(String details) {
        StringTokenizer stringTokenizer = new StringTokenizer(details, ",");
        String timeElapsed = stringTokenizer.nextToken();
        String cost = stringTokenizer.nextToken();
        String plan = stringTokenizer.nextToken();
        timeElapsed = timeElapsed.substring(timeElapsed.indexOf('=') + 1);
        cost = cost.substring(cost.indexOf('=') + 1);
        plan = plan.substring(plan.indexOf('=') + 1, plan.length() - 1);
        updateDues(cost);
        storeHistory(BookingTime, EndingTime, timeElapsed, cost, plan);
        Log.d("DetailsF", "Cost: " + cost);
        Log.d("DetailsF", "Time Elapsed: " + timeElapsed);
        Log.d("DetailsF", "Plan: " + plan);
        new AlertDialog.Builder(MapScreen.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Ride Details")
                .setMessage("Booking Time: " + BookingTime + "\nEnding Time: " + EndingTime + "\nTime Elapsed: " + timeElapsed + " seconds" + "\nCost: Rs." + cost + "\nPlan Active: " + plan)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(MapScreen.this, WalletScreen.class));
                        finish();
                    }
                }).show();

    }

    private void updateDues(final String cost) {
        DocumentReference documentReference = firebaseFirestore.collection("Users").document(account.getId());
        documentReference.get().
                addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String currentBal = document.getString("Balance");
                                assert currentBal != null;
                                if (Integer.valueOf(currentBal) > Integer.valueOf(cost)) {
                                    firebaseFirestore.collection("Users")
                                            .document(account.getId())
                                            .update("Balance", Integer.toString(Integer.valueOf(currentBal) - Integer.valueOf(cost)));
                                } else if (Integer.valueOf(currentBal) == 0) {
                                    Map<String, Object> rideDetails = new HashMap<>();
                                    rideDetails.put("Dues", cost);
                                    firebaseFirestore.collection("Users").
                                            document(account.getId()).
                                            update(rideDetails);
                                } else if (Integer.valueOf(currentBal) != 0 && Integer.valueOf(cost) > 0) {
                                    String newcost = Integer.toString(Integer.valueOf(cost) - Integer.valueOf(currentBal));
                                    Map<String, Object> newDue = new HashMap<>();
                                    newDue.put("Dues", newcost);
                                    newDue.put("Balance", "0");
                                    firebaseFirestore.collection("Users").
                                            document(account.getId()).
                                            update(newDue);
                                }
                            }
                            Log.d("DEBUG", "DocumentSnapshot data: " + document.getData());
                        } else {
                            Log.d("DEBUG", "Found null document");
                        }
                    }
                });
    }

    private void logOut() {
        client.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> history) {
                startActivity(new Intent(MapScreen.this, SplashScreen.class));
                finish();
            }
        });
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        return false;
    }
}

