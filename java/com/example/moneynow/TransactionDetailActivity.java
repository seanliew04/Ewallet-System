package com.example.moneynow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class TransactionDetailActivity extends AppCompatActivity {

    private EditText transactionIdInput;
    private EditText transactionDateInput;
    private EditText transactionAmountInput;
    private EditText transactionVendorUserIDInput;
    private EditText transactionStatusInput;
    private Button requestRefundButton;

    private DatabaseReference databaseReference;
    private String transactionId;
    private String vendorUID; // Ensure this holds the vendor's UID
    private String amount; // Added amount field

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app");
        databaseReference = database.getReference("refundRequests");

        // Initialize UI components
        transactionIdInput = findViewById(R.id.transactionIdInput);
        transactionDateInput = findViewById(R.id.transactionDateInput);
        transactionAmountInput = findViewById(R.id.transactionAmountInput);
        transactionVendorUserIDInput = findViewById(R.id.transactionVendorUserIDInput);
        transactionStatusInput = findViewById(R.id.transactionStatusInput);
        requestRefundButton = findViewById(R.id.requestRefundButton);

        // Get transaction details from intent
        HashMap<String, Object> transactionDetails = (HashMap<String, Object>) getIntent().getSerializableExtra("transactionDetails");
        if (transactionDetails != null) {
            // Add null checks before calling toString()
            transactionId = transactionDetails.get("transactionId") != null ? transactionDetails.get("transactionId").toString() : "";
            String date = transactionDetails.get("date") != null ? transactionDetails.get("date").toString() : "";
            amount = transactionDetails.get("amount") != null ? transactionDetails.get("amount").toString() : "";
            vendorUID = transactionDetails.get("vendorUserID") != null ? transactionDetails.get("vendorUserID").toString() : ""; // Ensure this is the vendor's UID
            String status = transactionDetails.get("status") != null ? transactionDetails.get("status").toString() : "";

            transactionIdInput.setText(transactionId);
            transactionDateInput.setText(date);
            transactionAmountInput.setText(amount);
            transactionVendorUserIDInput.setText(vendorUID);
            transactionStatusInput.setText(status);
        } else {
            Toast.makeText(this, "Transaction details are missing.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set request refund button action
        requestRefundButton.setOnClickListener(v -> checkAndRequestRefund(transactionId, vendorUID, amount));
    }

    private void checkAndRequestRefund(String transactionId, String vendorUID, String amount) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        databaseReference.orderByChild("transactionId").equalTo(transactionId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Refund request already exists
                    Toast.makeText(TransactionDetailActivity.this, "Refund request for this transaction already submitted.", Toast.LENGTH_SHORT).show();
                } else {
                    // No existing refund request, proceed with the request
                    requestRefund(transactionId, vendorUID, amount, userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("TransactionDetailActivity", "Database error: " + databaseError.getMessage());
                Toast.makeText(TransactionDetailActivity.this, "Failed to check refund status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestRefund(String transactionId, String vendorUID, String amount, String userId) {
        DatabaseReference refundRequestRef = databaseReference.push();

        Long currentDate = System.currentTimeMillis();

        // Add necessary fields to the refund request
        refundRequestRef.child("userId").setValue(userId);
        refundRequestRef.child("vendorId").setValue(vendorUID); // Ensure the vendor UID is stored
        refundRequestRef.child("transactionId").setValue(transactionId);
        refundRequestRef.child("date").setValue(currentDate);
        refundRequestRef.child("amount").setValue(amount); // Store the amount
        refundRequestRef.child("status").setValue("pending");

        Toast.makeText(this, "Refund request submitted", Toast.LENGTH_SHORT).show();
        finish(); // Close the activity after submission
    }
}
