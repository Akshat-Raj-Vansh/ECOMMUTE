package com.example.roughapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
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
import com.google.firebase.functions.HttpsCallableResult;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapScreen extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, View.OnDragListener {

    private GoogleMap userLocation;
    private GoogleMap bikeLocations;
    private CardView openScanner;
    private CardView bookedRide;
    private Button more;
    private CircularImageView showCurrentPosition;
    private ConstraintLayout navMenu;
    private CircularImageView profilePic;
    private GoogleSignInAccount account;
    private FirebaseFunctions firebaseFunctions;
    private GoogleSignInClient client;
    private GoogleSignInOptions gso;
    private String cycleID;
    private Animation clockwise, anticlockise;
    private Button history, profile, logout, wallet, settings;
    private LocationManager locationManager;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseFirestore firebaseFirestore;
    private LatLng previousLocation;
    private static final int RC_BARCODE_CAPTURE = 9001;
    private int showCurrPos = 0;
    private static final String TAG = "BarcodeMain";
    int isNavOpen = 0;
    private Bitmap bikeMarker;
    private Bitmap stationMarker;

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
        displayMap();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        userLocation = googleMap;
        bikeLocations = googleMap;
        userLocation.setMyLocationEnabled(true);
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
                bikeLocations.addMarker(new MarkerOptions()
                        .position(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()))
                        .title("Bike's Location")).setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("Bike2").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("Bike2").getValue(Bike.class)).getLongitude());
                bikeLocations.addMarker(new MarkerOptions()
                        .position(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()))
                        .title("Bike's Location")).setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("Bike3").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("Bike3").getValue(Bike.class)).getLongitude());
                bikeLocations.addMarker(new MarkerOptions()
                        .position(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()))
                        .title("Bike's Location")).setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("Bike4").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("Bike4").getValue(Bike.class)).getLongitude());
                bikeLocations.addMarker(new MarkerOptions()
                        .position(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()))
                        .title("Bike's Location")).setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
                bikeLocation.setLatitude(Objects.requireNonNull(ds.child("Bike5").getValue(Bike.class)).getLatitude());
                bikeLocation.setLongitude(Objects.requireNonNull(ds.child("Bike5").getValue(Bike.class)).getLongitude());
                bikeLocations.addMarker(new MarkerOptions()
                        .position(new LatLng(bikeLocation.getLatitude(), bikeLocation.getLongitude()))
                        .title("Bike's Location")).setIcon(BitmapDescriptorFactory.fromBitmap(bikeMarker));
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
                    //Geocoder geocoder = new Geocoder(getApplicationContext());
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
        previousLocation = new LatLng(70, 70);
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
                scanQRCode();
                break;
            case R.id.more_button:
                isNavOpen = 1 - isNavOpen;
                if (isNavOpen == 1) {
                    openScanner.setVisibility(View.GONE);
                    showCurrentPosition.setVisibility(View.GONE);
                    navMenu.setVisibility(View.VISIBLE);
                    more.startAnimation(clockwise);
                } else {
                    openScanner.setVisibility(View.VISIBLE);
                    showCurrentPosition.setVisibility(View.VISIBLE);
                    navMenu.setVisibility(View.GONE);
                    more.startAnimation(anticlockise);
                }
                break;
            case R.id.profile:
            case R.id.profile_nav:
                Toast.makeText(MapScreen.this, "Profile Page will come here", Toast.LENGTH_SHORT).show();
                Log.d("FirebaseFunctions", addNumber(5, 6).toString());
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
                startActivity(new Intent(MapScreen.this, PaymentPortal.class));
                finish();
                break;
            case R.id.showCurrentPos:
                userLocation.moveCamera(CameraUpdateFactory.newLatLngZoom(previousLocation, 18.5f));
                showCurrPos = 1 - showCurrPos;
                displayMap();
                break;
            case R.id.bookedRide:
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
        firebaseFirestore
                .collection("Bikes")
                .document(cycleID)
                .update(rideDetails);

        bookedRide.setVisibility(View.GONE);
        openScanner.setVisibility(View.VISIBLE);
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

    private Task<String> addNumber(int first, int sec) {
        // Create the arguments to the callable function.
        Toast.makeText(MapScreen.this, "Inside the addNumber function", Toast.LENGTH_SHORT).show();
        Map<String, Integer> data = new HashMap<>();
        data.put("firstNumber", first);
        data.put("secondNumber", sec);

        return firebaseFunctions
                .getHttpsCallable("addNumbers")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        String result = (String) task.getResult().getData();
                        Toast.makeText(MapScreen.this, result, Toast.LENGTH_SHORT).show();
                        Log.d("DebX", "inside here");
                        return result;
                    }
                });
    }


    private void searchCycle(String display) {
        Toast.makeText(this, display, Toast.LENGTH_SHORT).show();
        Map<String, Object> rideDetails = new HashMap<>();
        rideDetails.put("Status", "Booked");
        rideDetails.put("Booked By", account.getId());
        rideDetails.put("Start Time", Calendar.getInstance().getTime());
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
        cycleID = display;
        bookedRide.setVisibility(View.VISIBLE);
        openScanner.setVisibility(View.GONE);
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        return false;
    }
}

