package com.example.moneynow;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    private EditText amountInput, vendorIdInput, passwordInput;
    private ImageView payButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app");
        databaseReference = database.getReference("users");

        // Initialize UI components
        amountInput = findViewById(R.id.amount_input);
        vendorIdInput = findViewById(R.id.vendor_id_input);
        passwordInput = findViewById(R.id.password_input);
        payButton = findViewById(R.id.pay_button);
        View backButton = findViewById(R.id.backButton);

        // Set click listener for the back button
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(PaymentActivity.this, StaffDashboardActivity.class);
            startActivity(intent);
            finish();
        });

        // Set click listener for the Pay button
        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processPayment();
            }
        });
    }

    private void processPayment() {
        String amountStr = amountInput.getText().toString().trim();
        String userID = vendorIdInput.getText().toString().trim(); // Use userID instead of userId
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(userID) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Attempting to process payment for userID: " + userID);

        double amount = Double.parseDouble(amountStr);
        String formattedAmount = "RM" + amount; // Format the amount with "RM" prefix

        if (firebaseAuth.getCurrentUser() != null) {
            String staffId = firebaseAuth.getCurrentUser().getUid();

            // Validate password
            firebaseAuth.signInWithEmailAndPassword(firebaseAuth.getCurrentUser().getEmail(), password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Authentication successful. Fetching vendor details...");

                            // Fetch all users to find the one with the matching userID
                            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    boolean userFound = false;
                                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                        String fetchedUserID = userSnapshot.child("userID").getValue(String.class);
                                        if (userID.equals(fetchedUserID)) {
                                            userFound = true;
                                            String userIdKey = userSnapshot.getKey();
                                            Log.d(TAG, "Vendor found: " + userIdKey);

                                            // Fetch staff details to check balance before updating vendor balance
                                            databaseReference.child(staffId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot staffSnapshot) {
                                                    Double staffBalance = staffSnapshot.child("balance").getValue(Double.class);
                                                    if (staffBalance != null && staffBalance >= amount) {
                                                        // Deduct the amount from staff balance
                                                        Double newStaffBalance = staffBalance - amount;
                                                        databaseReference.child(staffId).child("balance").setValue(newStaffBalance);

                                                        // Fetch vendor balance and update
                                                        Double vendorBalance = userSnapshot.child("balance").getValue(Double.class);
                                                        if (vendorBalance != null) {
                                                            vendorBalance += amount;

                                                            // Update vendor balance
                                                            databaseReference.child(userIdKey).child("balance").setValue(vendorBalance);

                                                            // Update transaction history for both staff and vendor
                                                            String transactionId = databaseReference.child("transactions").push().getKey();
                                                            if (transactionId != null) {
                                                                Map<String, Object> transaction = new HashMap<>();
                                                                transaction.put("amount", formattedAmount); // Use formatted amount
                                                                transaction.put("date", System.currentTimeMillis());
                                                                transaction.put("staffId", staffId);
                                                                transaction.put("vendorId", userIdKey);
                                                                transaction.put("status", "completed");

                                                                // Add transaction to the global transactions
                                                                databaseReference.child("transactions").child(transactionId).setValue(transaction);

                                                                // Add transaction to vendor's transaction history
                                                                databaseReference.child(userIdKey).child("transactionHistory").child(transactionId).setValue(transaction);

                                                                // Add transaction to staff's transaction history
                                                                databaseReference.child(staffId).child("transactionHistory").child(transactionId).setValue(transaction);

                                                                Toast.makeText(PaymentActivity.this, "Payment successful", Toast.LENGTH_SHORT).show();
                                                                finish(); // Close the activity after payment
                                                            }
                                                        } else {
                                                            Log.e(TAG, "Failed to fetch vendor balance for userID: " + userIdKey);
                                                            Toast.makeText(PaymentActivity.this, "Failed to fetch vendor balance", Toast.LENGTH_SHORT).show();
                                                        }
                                                    } else {
                                                        Log.e(TAG, "Insufficient funds for staff: " + staffId);
                                                        Toast.makeText(PaymentActivity.this, "Insufficient funds", Toast.LENGTH_SHORT).show();
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                    Log.e(TAG, "Failed to fetch staff details: " + databaseError.getMessage());
                                                    Toast.makeText(PaymentActivity.this, "Failed to process payment: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                            break;
                                        }
                                    }

                                    if (!userFound) {
                                        Log.e(TAG, "Vendor not found for userID: " + userID);
                                        Toast.makeText(PaymentActivity.this, "Vendor not found", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Log.e(TAG, "Database error: " + databaseError.getMessage());
                                    Toast.makeText(PaymentActivity.this, "Failed to process payment: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Log.e(TAG, "Authentication failed");
                            Toast.makeText(PaymentActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
