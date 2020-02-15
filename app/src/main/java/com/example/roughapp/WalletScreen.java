package com.example.roughapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WalletScreen extends AppCompatActivity implements View.OnClickListener {


    EditText amountEt;
    Button recharge, back;
    String value, amount, balance;
    private TextView currentbal;
    private TextView currentdues;
    private GoogleSignInAccount account;
    private CheckBox basic, advanced, economy;

    FirebaseFirestore database = FirebaseFirestore.getInstance();
    final int UPI_PAYMENT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_screen);
        initializeViews();
        account = GoogleSignIn.getLastSignedInAccount(this);
        updateDisplay();
        getPreDues();
        recharge.setOnClickListener(this);
        back.setOnClickListener(this);
        basic.setOnClickListener(this);
        advanced.setOnClickListener(this);
        economy.setOnClickListener(this);
        getPrePlan();
    }

    void initializeViews() {
        currentbal = findViewById(R.id.currentbal);
        recharge = findViewById(R.id.recharge);
        amountEt = findViewById(R.id.amount_et);
        back = findViewById(R.id.back_button);
        currentdues = findViewById(R.id.currentdues);
        basic = findViewById(R.id.basic_plan);
        advanced = findViewById(R.id.advanced_plan);
        economy = findViewById(R.id.economy_plan);
    }

    private void updateDisplay() {
        DocumentReference documentReference = database.collection("Users").document(account.getId());
        documentReference.get().
                addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                //getNewBalance();
                                balance = document.get("Balance").toString();
                                currentbal.setText("Rs. " + balance);
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

    private void updateDatabase() {
        double previously = getPreBalance();
        database.collection("Users")
                .document(Objects.requireNonNull(account.getId()))
                .update("Balance", previously + Double.valueOf(amount));
        updateDisplay();
        //currentbal.setText(Double.toString(previously+ Double.valueOf(amount)));
    }

    private void getPrePlan() {
        DocumentReference documentReference = database.collection("Users").document(account.getId());
        documentReference.get().
                addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                value = document.get("Plan").toString();
                                showPlan(value);
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

    private void showPlan(String value) {
        if (value.equals("Basic")) {
            basic.setChecked(true);
            advanced.setChecked(false);
            economy.setChecked(false);
        } else if (value.equals("Advanced")) {
            basic.setChecked(false);
            advanced.setChecked(true);
            economy.setChecked(false);
        } else if (value.equals("Economy")) {
            advanced.setChecked(false);
            basic.setChecked(false);
            economy.setChecked(true);
        }
    }

    private double getPreBalance() {
        value = "0.0";
        DocumentReference documentReference = database.collection("Users").document(account.getId());
        documentReference.get().
                addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                value = document.get("Balance").toString();
                                Log.d("DEBUG", "DocumentSnapshot data: " + document.getData());
                            } else {
                                Log.d("DEBUG", "Found null document");
                            }
                        } else {
                            Log.d("DEBUG", "Error finding the document");
                        }
                    }
                });
        return Double.valueOf(value);
    }

    private double getPreDues() {
        value = "0.0";
        DocumentReference documentReference = database.collection("Users").document(account.getId());
        documentReference.get().
                addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                value = document.get("Dues").toString();
                                currentdues.setText("Rs. " + value);
                                Log.d("DEBUG", "DocumentSnapshot data: " + document.getData());
                            } else {
                                Log.d("DEBUG", "Found null document");
                            }
                        } else {
                            Log.d("DEBUG", "Error finding the document");
                        }
                    }
                });
        return Double.valueOf(value);
    }


    void payUsingUpi(String amount, String upiId, String name, String note) {

        Uri uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR")
                .build();


        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);

        // will always show a dialog to user to choose an app
        Intent chooser = Intent.createChooser(upiPayIntent, "Pay with");

        // check if intent resolves
        if (null != chooser.resolveActivity(getPackageManager())) {
            startActivityForResult(chooser, UPI_PAYMENT);
        } else {
            Toast.makeText(WalletScreen.this, "No UPI app found, please install one to continue", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case UPI_PAYMENT:
                if ((RESULT_OK == resultCode) || (resultCode == 11)) {
                    if (data != null) {
                        String trxt = data.getStringExtra("response");
                        Log.d("UPI", "onActivityResult: " + trxt);
                        ArrayList<String> dataList = new ArrayList<>();
                        dataList.add(trxt);
                        upiPaymentDataOperation(dataList);
                    } else {
                        Log.d("UPI", "onActivityResult: " + "Return data is null");
                        ArrayList<String> dataList = new ArrayList<>();
                        dataList.add("nothing");
                        upiPaymentDataOperation(dataList);
                    }
                } else {
                    Log.d("UPI", "onActivityResult: " + "Return data is null"); //when user simply back without payment
                    ArrayList<String> dataList = new ArrayList<>();
                    dataList.add("nothing");
                    upiPaymentDataOperation(dataList);
                }
                break;
        }
    }

    private void upiPaymentDataOperation(ArrayList<String> data) {
        if (isConnectionAvailable(WalletScreen.this)) {
            String str = data.get(0);
            Log.d("UPIPAY", "upiPaymentDataOperation: " + str);
            String paymentCancel = "";
            if (str == null) str = "discard";
            String status = "";
            String approvalRefNo = "";
            String response[] = str.split("&");
            for (int i = 0; i < response.length; i++) {
                String equalStr[] = response[i].split("=");
                if (equalStr.length >= 2) {
                    if (equalStr[0].toLowerCase().equals("Status".toLowerCase())) {
                        status = equalStr[1].toLowerCase();
                    } else if (equalStr[0].toLowerCase().equals("ApprovalRefNo".toLowerCase()) || equalStr[0].toLowerCase().equals("txnRef".toLowerCase())) {
                        approvalRefNo = equalStr[1];
                    }
                } else {
                    paymentCancel = "Payment cancelled by user.";
                }
            }

            if (status.equals("success")) {
                //Code to handle successful transaction here.
                updateDatabase();
                Toast.makeText(WalletScreen.this, "Transaction successful.", Toast.LENGTH_SHORT).show();
                Log.d("UPI", "responseStr: " + approvalRefNo);
            } else if ("Payment cancelled by user.".equals(paymentCancel)) {
                Toast.makeText(WalletScreen.this, "Payment cancelled by user.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(WalletScreen.this, "Transaction failed.Please try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(WalletScreen.this, "Internet connection is not available. Please check and try again", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isConnectionAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()
                    && netInfo.isConnectedOrConnecting()
                    && netInfo.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.recharge:
                amount = amountEt.getText().toString();
                payUsingUpi(amount, "7355026029@ybl", account.getDisplayName(), "Payment to the developers");
                break;
            case R.id.back_button:
                startActivity(new Intent(WalletScreen.this, MapScreen.class));
                finish();
                break;
            case R.id.basic_plan:
                setPlan("Basic");
                advanced.setChecked(false);
                economy.setChecked(false);
                break;
            case R.id.advanced_plan:
                setPlan("Advanced");
                basic.setChecked(false);
                economy.setChecked(false);
                break;
            case R.id.economy_plan:
                setPlan("Economy");
                advanced.setChecked(false);
                basic.setChecked(false);
                break;
        }
    }

    private void setPlan(String plan) {
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("Plan", plan);
        database.collection("Users").document(account.getId())
                .update(userDetails)
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(WalletScreen.this, MapScreen.class));
        finish();
    }
}