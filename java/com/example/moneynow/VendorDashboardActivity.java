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
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;

public class VendorDashboardActivity extends AppCompatActivity {

    private TextView textViewVendorBalance;
    private TextView textViewVendorID;
    private ListView vendorTransactionListView;
    private DatabaseReference databaseReference;
    private List<Map<String, Object>> transactionList;
    private TransactionAdapter transactionAdapter;
    private String vendorId;
    private Button buttonTransactionHistory1, buttonBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_dashboard);

        // Initialize Firebase Auth
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        vendorId = firebaseAuth.getCurrentUser().getUid();

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app");
        databaseReference = database.getReference("users").child(vendorId);

        // Initialize UI components
        textViewVendorBalance = findViewById(R.id.textViewVendorBalance);
        textViewVendorID = findViewById(R.id.textViewVendorID);
        vendorTransactionListView = findViewById(R.id.vendorTransactionListView);
        View buttonTransactionHistory = findViewById(R.id.buttonTransactionHistory1);
        buttonBackToLogin = findViewById(R.id.buttonBackToLogin);

        // Set click listener for View All button
        buttonTransactionHistory.setOnClickListener(view -> {
            Intent intent = new Intent(VendorDashboardActivity.this, VendorTransactionHistoryActivity.class);
            intent.putExtra("userId", vendorId); // Pass vendorId to TransactionHistoryActivity
            startActivity(intent);
        });

        // Set click listener for Back to Login button
        buttonBackToLogin.setOnClickListener(view -> {
            // Handle back to login action
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(VendorDashboardActivity.this, LoginActivity.class));
            finish();
        });

        // Initialize transaction list and adapter
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList);
        vendorTransactionListView.setAdapter(transactionAdapter);

        // Set click listener for transaction items
        vendorTransactionListView.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> transaction = transactionList.get(position);
            Log.d("VendorTransactionHistoryActivity", "Transaction clicked: " + transaction.toString());
            Intent intent = new Intent(VendorDashboardActivity.this, VendorTransactionDetailActivity.class);
            intent.putExtra("transactionDetails", (HashMap<String, Object>) transaction);
            startActivity(intent);
        });

        // Directly update TextView for testing
        textViewVendorID.setText("Welcome Test UserID");

        // Fetch vendor data including balance and transactions
        fetchVendorData();
    }

    private void fetchVendorData() {
        // Fetch vendor balance
        databaseReference.child("balance").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Double balance = dataSnapshot.getValue(Double.class);
                if (balance != null) {
                    runOnUiThread(() -> {
                        textViewVendorBalance.setText("RM" + balance);
                        Log.d("VendorDashboardActivity", "Balance displayed: RM" + balance);
                    });
                } else {
                    Log.e("VendorDashboardActivity", "Balance is null or could not be fetched");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("VendorDashboardActivity", "Failed to fetch balance", databaseError.toException());
            }
        });

        // Fetch vendor ID and set welcome message
        databaseReference.child("userID").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String userID = dataSnapshot.getValue(String.class);
                    if (userID != null) {
                        runOnUiThread(() -> {
                            textViewVendorID.setText("Welcome " + userID);
                            Log.d("VendorDashboardActivity", "User ID displayed: " + userID);
                        });
                    } else {
                        Log.e("VendorDashboardActivity", "User ID is null or could not be fetched");
                    }
                } else {
                    Log.e("VendorDashboardActivity", "User ID does not exist");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("VendorDashboardActivity", "Failed to fetch user ID", databaseError.toException());
            }
        });

        // Fetch vendor transaction history
        databaseReference.child("transactionHistory").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                transactionList.clear(); // Clear the previous list
                if (dataSnapshot.exists()) {
                    Log.d("VendorDashboardActivity", "Transaction history found: " + dataSnapshot.getChildrenCount());
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
                                        Log.d("VendorDashboardActivity", "Processing transaction - Amount: " + finalAmount + ", Date: " + dateValue + ", StaffId: " + staffId);
                                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(dateValue)); // Format date to readable string
                                        Map<String, Object> transaction = new HashMap<>();
                                        transaction.put("amount", String.format(Locale.getDefault(), "RM%.2f", finalAmount)); // Format amount with "RM" prefix
                                        transaction.put("date", date);
                                        transaction.put("staffUserID", staffUserID); // Use staffUserID for display
                                        transaction.put("transactionId", transactionId); // Add transaction ID
                                        transaction.put("status", status); // Add transaction status
                                        transactionList.add(transaction);
                                        Log.d("VendorDashboardActivity", "Transaction added - StaffUserID: " + staffUserID + ", Date: " + date + ", Amount: " + finalAmount);
                                        runOnUiThread(() -> transactionAdapter.notifyDataSetChanged());
                                    } else {
                                        Log.e("VendorDashboardActivity", "Staff userID not found for staffId: " + staffId);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Log.e("VendorDashboardActivity", "Failed to fetch staff userID", databaseError.toException());
                                }
                            });
                        } else {
                            Log.e("VendorDashboardActivity", "Transaction data is incomplete: Amount=" + amount + ", Date=" + dateValue + ", StaffId=" + staffId);
                        }
                    }
                } else {
                    Log.d("VendorDashboardActivity", "No transaction history found for user");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("VendorDashboardActivity", "Failed to fetch transactions", databaseError.toException());
            }
        });
    }

    // Custom adapter to handle transaction list
    public class TransactionAdapter extends ArrayAdapter<Map<String, Object>> {

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
