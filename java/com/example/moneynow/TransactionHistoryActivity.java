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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class TransactionHistoryActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FirebaseDatabase database;
    private ListView transactionHistoryListView;
    private List<Map<String, Object>> transactionList;
    private TransactionAdapter transactionAdapter;
    private String userId;
    private EditText editTextVendorId, editTextDate, editTextAmount;
    private Button buttonApplyFilter, buttonResetFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Get current user
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }

        // Initialize Firebase Database with correct URL
        database = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app");
        databaseReference = database.getReference("users");

        // Initialize UI components
        transactionHistoryListView = findViewById(R.id.transactionHistoryListView);
        editTextVendorId = findViewById(R.id.editTextVendorId);
        editTextDate = findViewById(R.id.editTextDate);
        editTextAmount = findViewById(R.id.editTextAmount);
        buttonApplyFilter = findViewById(R.id.buttonApplyFilter);
        buttonResetFilter = findViewById(R.id.buttonResetFilter);
        View backButton = findViewById(R.id.backButton);

        // Set click listener for the back button
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(TransactionHistoryActivity.this, StaffDashboardActivity.class);
            startActivity(intent);
            finish();
        });


        // Initialize transaction list and adapter
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList);
        transactionHistoryListView.setAdapter(transactionAdapter);

        // Set click listener for transaction items
        transactionHistoryListView.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> transaction = transactionList.get(position);
            Intent intent = new Intent(TransactionHistoryActivity.this, TransactionDetailActivity.class);
            intent.putExtra("transactionDetails", (HashMap<String, Object>) transaction);
            startActivity(intent);
        });

        // Set click listener for Apply Filter button
        buttonApplyFilter.setOnClickListener(view -> applyFilter());

        // Set click listener for Reset Filter button
        buttonResetFilter.setOnClickListener(view -> resetFilter());

        // Fetch transaction history
        fetchTransactionHistory();
    }

    private void fetchTransactionHistory() {
        databaseReference.child(userId).child("transactionHistory").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("TransactionHistoryActivity", "Transaction history found: " + dataSnapshot.exists());
                transactionList.clear(); // Clear existing transactions

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Long dateValue = snapshot.child("date").getValue(Long.class);
                    String amountValue;
                    try {
                        amountValue = snapshot.child("amount").getValue(String.class);
                    } catch (Exception e) {
                        Long amountLong = snapshot.child("amount").getValue(Long.class);
                        amountValue = amountLong != null ? "RM" + amountLong.toString() : "RM0";
                    }
                    String vendorId = snapshot.child("vendorId").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);
                    String transactionId = snapshot.getKey();

                    Log.d("TransactionHistoryActivity", "Transaction details - Date: " + dateValue + ", Amount: " + amountValue + ", VendorId: " + vendorId);

                    if (dateValue != null && amountValue != null && vendorId != null) {
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(dateValue));

                        // Fetch vendor userID using vendorId
                        DatabaseReference vendorReference = database.getReference("users").child(vendorId).child("userID");
                        String finalAmountValue = amountValue;
                        vendorReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot vendorSnapshot) {
                                String vendorUserID = vendorSnapshot.getValue(String.class);
                                if (vendorUserID != null) {
                                    Map<String, Object> transaction = new HashMap<>();
                                    transaction.put("transactionId", transactionId);
                                    transaction.put("amount", finalAmountValue);
                                    transaction.put("date", date);
                                    transaction.put("vendorUserID", vendorUserID);
                                    transaction.put("status", status);
                                    transactionList.add(transaction);
                                    Log.d("TransactionHistoryActivity", "Transaction added - VendorUserID: " + vendorUserID + ", Date: " + date + ", Amount: " + finalAmountValue);
                                    transactionAdapter.notifyDataSetChanged(); // Notify adapter to update the ListView
                                } else {
                                    Log.e("TransactionHistoryActivity", "Vendor not found for vendorId: " + vendorId);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.e("TransactionHistoryActivity", "Failed to fetch vendor details", databaseError.toException());
                            }
                        });
                    } else {
                        Log.e("TransactionHistoryActivity", "Transaction data is incomplete: Date=" + dateValue + ", Amount=" + amountValue + ", VendorId=" + vendorId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("TransactionHistoryActivity", "Failed to fetch transaction history", databaseError.toException());
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

            if (!vendorId.isEmpty() && !vendorId.equals(transaction.get("vendorUserID"))) {
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
                TextView textViewVendorUserID = convertView.findViewById(R.id.textViewTransactionVendorUserID);

                textViewDate.setText((String) transaction.get("date"));
                textViewAmount.setText((String) transaction.get("amount"));
                textViewVendorUserID.setText((String) transaction.get("vendorUserID"));
            }

            return convertView;
        }
    }
}


