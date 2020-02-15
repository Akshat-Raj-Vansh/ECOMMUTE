package com.example.roughapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
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
    private long bookedTime, endedTime;
    private CircularImageView showCurrentPosition, bikeOptions;
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L;
    private ConstraintLayout navMenu;
    private CircularImageView profilePic;
    private GoogleSignInAccount account;
    private FirebaseFunctions firebaseFunctions;
    private GoogleSignInClient client;
    private GoogleSignInOptions gso;
    private String cycleID;
    private TextView showDetails, timeElapsed, bookingTime;
    private Animation clockwise, anticlockise;
    private Button history, profile, logout, wallet, settings;
    private LocationManager locationManager;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseFirestore firebaseFirestore;
    private LatLng previousLocation;
    private LinearLayout details;
    private static final int RC_BARCODE_CAPTURE = 9001;
    private int showCurrPos = 0;
    private static final String TAG = "BarcodeMain";
    int isNavOpen = 0;
    Handler handler;
    int Hours, Seconds, Minutes, MilliSeconds;
    private Marker bike1, bike2, bike3, bike4, bike5;
    private Bitmap bikeMarker;
    private Bitmap stationMarker;
    private String value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_screen);
        initialize();
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
        displayMap();
        NewAccountBonus();
        checkPreviousBooking();
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
                                    showTimeElapsed();
                                    cycleID = value;
                                    Toast.makeText(MapScreen.this, value, Toast.LENGTH_SHORT).show();
                                    bookingTime.setText(document.get("Booking Time").toString());
                                    bookedRide.setVisibility(View.VISIBLE);
                                    details.setVisibility(View.GONE);
                                    showDetails.setVisibility(View.VISIBLE);
                                    openScanner.setVisibility(View.GONE);
                                } else if (value.equals("NULL")) {
                                    bookedRide.setVisibility(View.GONE);
                                    details.setVisibility(View.GONE);
                                    showDetails.setVisibility(View.GONE);
                                    openScanner.setVisibility(View.VISIBLE);
                                }
                            }
                            Log.d("DEBUG", "DocumentSnapshot data: " + document.getData());
                        } else {
                            Log.d("DEBUG", "Found null document");
                        }
                    }
                });
    }


    private void NewAccountBonus() {
        value = "0";
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
        bike3 = bikeLocations.addMarker(new MarkerOptions()
                .position(new LatLng(50, 50))
                .title("Bike's Location"));
        bike4 = bikeLocations.addMarker(new MarkerOptions()
                .position(new LatLng(50, 50))
                .title("Bike's Location"));
        bike5 = bikeLocations.addMarker(new MarkerOptions()
                .position(new LatLng(50, 50))
                .title("Bike's Location"));
        bike1.setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
        bike2.setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
        bike3.setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
        bike4.setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
        bike5.setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
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
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("Bike1").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("Bike1").getValue(Bike.class)).getLongitude());
                bike1.setPosition(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()));
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("Bike2").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("Bike2").getValue(Bike.class)).getLongitude());
                bike2.setPosition(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()));
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("Bike3").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("Bike3").getValue(Bike.class)).getLongitude());
                bike3.setPosition(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()));
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("Bike4").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("Bike4").getValue(Bike.class)).getLongitude());
                bike4.setPosition(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()));
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("Bike5").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("Bike5").getValue(Bike.class)).getLongitude());
                bike5.setPosition(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()));
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
                Circle circle = bikeLocations.addCircle(new CircleOptions()
                        .center(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()))
                        .radius(10)
                        .strokeWidth(3)
                        .strokeColor(getColor(R.color.colorMapStroke))
                        .fillColor(getColor(R.color.colorMap)));
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
//                    Geocoder geocoder = new Geocoder(getApplicationContext());
//                    try {
//                        List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
//                        String address = addressList.get(0).getCountryName();
////                        CircleOptions circleOptions = new CircleOptions()
////                                .center(new LatLng(location.getLatitude(), location.getLongitude()));
////                        circleOptions.radius(3);
////                        circleOptions.fillColor(getColor(R.color.colorPrimary));
////                        circleOptions.strokeColor(getColor(R.color.colorPrimaryDark));// In meters
//
//                        //   marker.setPosition(latLng);
//                        //   userLocation.addCircle(circleOptions);
//                        //  userLocation.addMarker(new MarkerOptions().position(latLng).title(address));
//                        if (showCurrPos == 1) {
//                            userLocation.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18.5f));
//                            showCurrPos = 1 - showCurrPos;
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
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
        timeElapsed = findViewById(R.id.time_elapsed);
        bookingTime = findViewById(R.id.booking_time);
        previousLocation = new LatLng(70, 70);
        handler = new Handler();
        bookedRide.setVisibility(View.GONE);
        details.setVisibility(View.GONE);
        showDetails.setVisibility(View.GONE);
        openScanner.setVisibility(View.GONE);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scan_qr:
                checkMinBalance();
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
                //showDetailsFlag = 1 - showDetailsFlag;
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
                                endRide();
                            }
                        }).setNegativeButton("no", null).show();
                break;
        }
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
                                if (Double.valueOf(document.get("Balance").toString()) > 150 && Double.valueOf(document.get("Dues").toString()) == 0)
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

    private void logOut() {
        client.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> history) {
                startActivity(new Intent(MapScreen.this, SplashScreen.class));
                finish();
            }
        });
    }


    private void endRide() {
        Toast.makeText(this, cycleID, Toast.LENGTH_SHORT).show();
        Map<String, Object> rideDetails = new HashMap<>();
        rideDetails.put("Status", "Free");
        rideDetails.put("Booked By", account.getId());
        rideDetails.put("End Time", Calendar.getInstance().getTime());
        endedTime = Calendar.getInstance().getTimeInMillis();
        EndingTime = Calendar.getInstance().getTime().toString();
        getTime(bookedTime, endedTime)
                .addOnCompleteListener(new OnCompleteListener<String>() {
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
        handler.removeCallbacks(runnable);
        bookedRide.setVisibility(GONE);
        openScanner.setVisibility(View.VISIBLE);
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

    private Task<String> getTime(long st, long et) {
        Map<String, Object> data = new HashMap<>();
        data.put("startTime", st / 1000);
        data.put("endTime", et / 1000);
        data.put("plan", 0);
        Log.d("Functions", "Inside the getTime function");
        return firebaseFunctions
                .getHttpsCallable("getTime")
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
                                Log.d("Burnout", task.getResult().getData().toString());
                                extractDetails(task.getResult().getData().toString());
                            }
                        });
                        return "Voila";
                    }
                });
    }

    private void extractDetails(String details) {
        StringTokenizer stringTokenizer = new StringTokenizer(details, ",");
        String timeElapsed = stringTokenizer.nextToken();
        String cost = stringTokenizer.nextToken();
        String plan = stringTokenizer.nextToken();
        timeElapsed = timeElapsed.substring(timeElapsed.indexOf('=') + 1);
        cost = cost.substring(cost.indexOf('=') + 1);
        plan = plan.substring(plan.indexOf('=') + 1, plan.length() - 1);
        Map<String, Object> rideDetails = new HashMap<>();
        rideDetails.put("Dues", cost);
        firebaseFirestore.collection("Users").
                document(account.getId()).
                update(rideDetails);
        storeHistory(BookingTime, EndingTime, timeElapsed, cost, plan);
        Log.d("DetailsF", "Cost: " + cost);
        Log.d("DetailsF", "Time Elapsed: " + timeElapsed);
        Log.d("DetailsF", "Plan: " + plan);
        new AlertDialog.Builder(MapScreen.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Ride Details")
                .setMessage("Booking Time: " + timeElapsed + " seconds" + "\nCost: Rs." + cost + "\nPlan Active: " + plan)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(MapScreen.this, WalletScreen.class));
                        finish();
                    }
                }).show();

    }

    private void searchCycle(String display) {
        Toast.makeText(this, display, Toast.LENGTH_SHORT).show();
        String time = Calendar.getInstance().getTime().toString();
        bookedTime = Calendar.getInstance().getTimeInMillis();
        BookingTime = Calendar.getInstance().getTime().toString();
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
        rideDetails.clear();
        rideDetails.put("Bike ID", display);
        rideDetails.put("Booking Time", time);
        firebaseFirestore.collection("Users").document(account.getId())
                .update(rideDetails);
        cycleID = display;
        showTimeElapsed();
        bookedRide.setVisibility(View.VISIBLE);
        details.setVisibility(View.GONE);
        showDetails.setVisibility(View.VISIBLE);
        openScanner.setVisibility(View.GONE);
    }

    private void showTimeElapsed() {
        StartTime = SystemClock.uptimeMillis();
        handler.postDelayed(runnable, 0);
    }

    public Runnable runnable = new Runnable() {

        public void run() {
            MillisecondTime = SystemClock.uptimeMillis() - StartTime;
            UpdateTime = TimeBuff + MillisecondTime;
            Seconds = (int) (UpdateTime / 1000);
            Minutes = Seconds / 60;
            Hours = Minutes / 60;
            Seconds = Seconds % 60;
            MilliSeconds = (int) (UpdateTime % 1000);
            timeElapsed.setText(String.format("%02d", Hours) + " hrs " + String.format("%02d", Minutes) + " min "
                    + String.format("%02d", Seconds) + " sec");
            handler.postDelayed(this, 0);
        }

    };

    @Override
    public boolean onDrag(View v, DragEvent event) {
        return false;
    }
}

