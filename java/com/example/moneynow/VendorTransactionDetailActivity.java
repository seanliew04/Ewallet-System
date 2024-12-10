package com.example.moneynow;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;

public class VendorTransactionDetailActivity extends AppCompatActivity {

    private EditText transactionIdInput;
    private EditText transactionDateInput;
    private EditText transactionAmountInput;
    private EditText transactionStaffUserIDInput;
    private EditText transactionStatusInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_transaction_detail);

        // Initialize UI components
        transactionIdInput = findViewById(R.id.transactionIdInput);
        transactionDateInput = findViewById(R.id.transactionDateInput);
        transactionAmountInput = findViewById(R.id.transactionAmountInput);
        transactionStaffUserIDInput = findViewById(R.id.transactionStaffUserIDInput);
        transactionStatusInput = findViewById(R.id.transactionStatusInput);

        // Get transaction details from intent
        HashMap<String, Object> transactionDetails = (HashMap<String, Object>) getIntent().getSerializableExtra("transactionDetails");
        Log.d("VendorTransactionDetailActivity", "Received transaction details: " + transactionDetails.toString());
        if (transactionDetails != null) {
            String transactionId = transactionDetails.get("transactionId") != null ? transactionDetails.get("transactionId").toString() : "";
            String date = transactionDetails.get("date") != null ? transactionDetails.get("date").toString() : "";
            String amount = transactionDetails.get("amount") != null ? transactionDetails.get("amount").toString() : "";
            String staffUserID = transactionDetails.get("staffUserID") != null ? transactionDetails.get("staffUserID").toString() : "";
            String status = transactionDetails.get("status") != null ? transactionDetails.get("status").toString() : "";

            transactionIdInput.setText(transactionId);
            transactionDateInput.setText(date);
            transactionAmountInput.setText(amount);
            transactionStaffUserIDInput.setText(staffUserID);
            transactionStatusInput.setText(status);

            Log.d("VendorTransactionDetailActivity", "Transaction ID: " + transactionId);
            Log.d("VendorTransactionDetailActivity", "Date: " + date);
            Log.d("VendorTransactionDetailActivity", "Amount: " + amount);
            Log.d("VendorTransactionDetailActivity", "Staff User ID: " + staffUserID);
            Log.d("VendorTransactionDetailActivity", "Status: " + status);
        } else {
            Toast.makeText(this, "Transaction details are missing.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
