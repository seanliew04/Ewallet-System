package com.example.moneynow;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VendorTransactionHistoryActivity extends AppCompatActivity {

    private EditText editTextVendorId, editTextDate, editTextAmount;
    private ListView transactionHistoryListView;
    private DatabaseReference databaseReference;
    private List<Map<String, Object>> transactionList;
    private TransactionAdapter transactionAdapter;
    private String vendorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_transaction_history);

        // Get vendorId from intent
        vendorId = getIntent().getStringExtra("userId");

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app");
        databaseReference = database.getReference("users").child(vendorId).child("transactionHistory");

        // Initialize UI components
        editTextVendorId = findViewById(R.id.editTextVendorId);
        editTextDate = findViewById(R.id.editTextDate);
        editTextAmount = findViewById(R.id.editTextAmount);
        transactionHistoryListView = findViewById(R.id.transactionHistoryListView);
        Button buttonApplyFilter = findViewById(R.id.buttonApplyFilter);
        Button buttonResetFilter = findViewById(R.id.buttonResetFilter);
        ImageButton backButton = findViewById(R.id.backButton);

        // Set click listener for the back button
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(VendorTransactionHistoryActivity.this, VendorDashboardActivity.class);
            startActivity(intent);
            finish();
        });

        // Initialize transaction list and adapter
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList);
        transactionHistoryListView.setAdapter(transactionAdapter);

        transactionHistoryListView.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> transaction = transactionList.get(position);
            Log.d("VendorTransactionHistoryActivity", "Transaction clicked: " + transaction.toString());
            Intent intent = new Intent(VendorTransactionHistoryActivity.this, VendorTransactionDetailActivity.class);
            intent.putExtra("transactionDetails", (HashMap<String, Object>) transaction);
            startActivity(intent);
        });

        // Set click listener for Apply Filter button
        buttonApplyFilter.setOnClickListener(view -> applyFilter());

        // Set click listener for Reset Filter button
        buttonResetFilter.setOnClickListener(view -> resetFilter());

        // Fetch and display transaction history
        fetchTransactionHistory();
    }

    private void fetchTransactionHistory() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                transactionList.clear(); // Clear existing transactions

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Double amount;
                    try {
                        amount = snapshot.child("amount").getValue(Double.class); // Read amount as Double
                    } catch (Exception e) {
                        String amountStr = snapshot.child("amount").getValue(String.class); // Fallback to String
                        amount = amountStr != null ? Double.parseDouble(amountStr.replace("RM", "")) : 0.0; // Convert String to Double
                    }
                    Long dateValue = snapshot.child("date").getValue(Long.class); // Read date as Long
                    String staffId = snapshot.child("staffId").getValue(String.class); // Read staffId
                    String transactionId = snapshot.getKey(); // Get transaction ID
                    String status = snapshot.child("status").getValue(String.class); // Get transaction status

                    if (amount != null && dateValue != null && staffId != null) {
                        // Fetch staff userID using staffId
                        FirebaseDatabase database = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app");
                        DatabaseReference staffReference = database.getReference("users").child(staffId).child("userID");

                        Double finalAmount = amount;
                        staffReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot staffSnapshot) {
                                String staffUserID = staffSnapshot.getValue(String.class);
                                if (staffUserID != null) {
                                    Log.d("VendorTransactionHistoryActivity", "Processing transaction - Amount: " + finalAmount + ", Date: " + dateValue + ", Staff UserID: " + staffUserID);
                                    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(dateValue)); // Format date to readable string
                                    Map<String, Object> transaction = new HashMap<>();
                                    transaction.put("amount", String.format(Locale.getDefault(), "RM%.2f", finalAmount)); // Format amount with "RM" prefix
                                    transaction.put("date", date);
                                    transaction.put("staffUserID", staffUserID); // Use staffUserID for display
                                    transaction.put("transactionId", transactionId); // Add transaction ID
                                    transaction.put("status", status); // Add transaction status
                                    transactionList.add(transaction);
                                    Log.d("VendorTransactionHistoryActivity", "Transaction added - Staff UserID: " + staffUserID + ", Date: " + date + ", Amount: " + finalAmount + ", Transaction ID: " + transactionId + ", Status: " + status);
                                    runOnUiThread(() -> transactionAdapter.notifyDataSetChanged());
                                } else {
                                    Log.e("VendorTransactionHistoryActivity", "Staff UserID not found for staffId: " + staffId);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.e("VendorTransactionHistoryActivity", "Failed to fetch Staff UserID", databaseError.toException());
                            }
                        });
                    } else {
                        Log.e("VendorTransactionHistoryActivity", "Transaction data is incomplete: Amount=" + amount + ", Date=" + dateValue + ", StaffId=" + staffId + ", Transaction ID=" + transactionId + ", Status=" + status);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("VendorTransactionHistoryActivity", "Failed to fetch transactions", databaseError.toException());
            }
        });
    }

    private void applyFilter() {
        String vendorId = editTextVendorId.getText().toString().trim();
        String dateStr = editTextDate.getText().toString().trim();
        String amountStr = editTextAmount.getText().toString().trim();

        SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfTransaction = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Long date = null;
        try {
            if (!dateStr.isEmpty()) {
                date = sdfInput.parse(dateStr).getTime();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Double amount = null;
        try {
            if (!amountStr.isEmpty()) {
                amount = Double.parseDouble(amountStr.replace("RM", ""));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        // If all filter fields are empty, reset to original transaction list
        if (vendorId.isEmpty() && dateStr.isEmpty() && amountStr.isEmpty()) {
            transactionAdapter.clear();
            transactionAdapter.addAll(transactionList);
            transactionAdapter.notifyDataSetChanged();
            return;
        }

        // Apply filter to transaction list
        List<Map<String, Object>> filteredList = new ArrayList<>();
        for (Map<String, Object> transaction : transactionList) {
            boolean matches = true;

            if (!vendorId.isEmpty() && !vendorId.equals(transaction.get("staffUserID"))) {
                matches = false;
            }
            if (date != null) {
                String dateStrTransaction = (String) transaction.get("date");
                Long transactionDate = null;
                try {
                    transactionDate = sdfTransaction.parse(dateStrTransaction).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                // Compare just the date parts of the timestamp
                if (transactionDate != null) {
                    String inputDateString = sdfInput.format(date);
                    String transactionDateString = sdfInput.format(transactionDate);

                    if (!inputDateString.equals(transactionDateString)) {
                        matches = false;
                    }
                } else {
                    matches = false;
                }
            }
            if (amount != null) {
                Double transactionAmount = Double.parseDouble(((String) transaction.get("amount")).replace("RM", ""));
                if (!transactionAmount.equals(amount)) {
                    matches = false;
                }
            }

            if (matches) {
                filteredList.add(transaction);
            }
        }

        // Update adapter with filtered list
        transactionAdapter.clear();
        transactionAdapter.addAll(filteredList);
        transactionAdapter.notifyDataSetChanged();
    }

    private void resetFilter() {
        editTextVendorId.setText("");
        editTextDate.setText("");
        editTextAmount.setText("");

        // Clear the adapter and fetch the original transaction history
        transactionList.clear();
        fetchTransactionHistory();
    }

    // Custom adapter to handle transaction list
    private class TransactionAdapter extends ArrayAdapter<Map<String, Object>> {

        public TransactionAdapter(@NonNull Context context, List<Map<String, Object>> transactions) {
            super(context, 0, transactions);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_transaction, parent, false);
            }

            Map<String, Object> transaction = getItem(position);
            if (transaction != null) {
                TextView textViewDate = convertView.findViewById(R.id.textViewTransactionDate);
                TextView textViewAmount = convertView.findViewById(R.id.textViewTransactionAmount);
                TextView textViewStaffUserID = convertView.findViewById(R.id.textViewTransactionVendorUserID); // Ensure this ID matches the one in list_item_transaction.xml

                textViewDate.setText((String) transaction.get("date"));
                textViewAmount.setText((String) transaction.get("amount")); // Use amount as String
                textViewStaffUserID.setText((String) transaction.get("staffUserID")); // Use the correct key for the user ID
            }

            return convertView;
        }
    }
}
