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
import android.widget.ImageButton;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StaffDashboardActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private TextView nameInputs, textView3;
    private ListView staffTransactionListView;
    private List<Map<String, Object>> transactionList;
    private TransactionAdapter transactionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Database with correct URL
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app");
        databaseReference = database.getReference("users");

        // Initialize UI components
        nameInputs = findViewById(R.id.name_inputs);
        textView3 = findViewById(R.id.textView3);
        staffTransactionListView = findViewById(R.id.staffTransactionListView);

        // Initialize the transaction list and adapter
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList);
        staffTransactionListView.setAdapter(transactionAdapter);

        // Initialize the ImageButton
        ImageButton transferButton = findViewById(R.id.imageButton);
        View backButton = findViewById(R.id.buttonBackToLogin2);

        // Set click listener for the back button
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(StaffDashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
        // Set an OnClickListener to navigate to the transfer page
        transferButton.setOnClickListener(view -> {
            // Intent to navigate to the TransferActivity
            Intent intent = new Intent(StaffDashboardActivity.this, PaymentActivity.class);
            startActivity(intent);
        });

        Button buttonBackToLogin = findViewById(R.id.buttonTransactionHistory);

        // Set an OnClickListener to navigate  to the transaction list page
        buttonBackToLogin.setOnClickListener(view -> {
            // Intent to navigate back to the LoginActivity
            Intent intent = new Intent(StaffDashboardActivity.this, TransactionHistoryActivity.class);
            startActivity(intent);
            finish();
        });

        // Set item click listener for the ListView
        staffTransactionListView.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> clickedTransaction = transactionAdapter.getItem(position);
            if (clickedTransaction != null) {
                Intent intent = new Intent(StaffDashboardActivity.this, TransactionDetailActivity.class);
                intent.putExtra("transactionDetails", (HashMap<String, Object>) clickedTransaction);
                startActivity(intent);
            }
        });

        // Get the current user
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Get the user ID
            String userId = currentUser.getUid();
            Log.d("StaffDashboardActivity", "User ID: " + userId);

            // Fetch the user's staff ID from Firebase Realtime Database
            databaseReference.child(userId).child("userID").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String staffId = dataSnapshot.getValue(String.class);
                        nameInputs.setText(staffId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("StaffDashboardActivity", "Failed to fetch staff ID", databaseError.toException());
                }
            });

            // Fetch the user's balance from Firebase Realtime Database
            databaseReference.child(userId).child("balance").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d("StaffDashboardActivity", "DataSnapshot exists: " + dataSnapshot.exists());
                    if (dataSnapshot.exists()) {
                        Log.d("StaffDashboardActivity", "DataSnapshot value: " + dataSnapshot.getValue());
                        Double balance = dataSnapshot.getValue(Double.class);
                        if (balance != null) {
                            Log.d("StaffDashboardActivity", "Balance: " + balance);
                            textView3.setText("RM " + String.format("%.2f", balance));
                        } else {
                            Log.d("StaffDashboardActivity", "Balance is null");
                            textView3.setText("RM 0.00");
                        }
                    } else {
                        Log.d("StaffDashboardActivity", "Balance not found. DataSnapshot details: " + dataSnapshot);
                        textView3.setText("RM 0.00");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("StaffDashboardActivity", "Database error: " + databaseError.getMessage());
                    textView3.setText("Error");
                }
            });

            // Fetch the user's transaction history from Firebase Realtime Database
            databaseReference.child(userId).child("transactionHistory").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d("StaffDashboardActivity", "Transaction history found: " + dataSnapshot.exists());
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

                        Log.d("StaffDashboardActivity", "Transaction details - Date: " + dateValue + ", Amount: " + amountValue + ", VendorId: " + vendorId);

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
                                        Log.d("StaffDashboardActivity", "Transaction added - VendorUserID: " + vendorUserID + ", Date: " + date + ", Amount: " + finalAmountValue);
                                        transactionAdapter.notifyDataSetChanged(); // Notify adapter to update the ListView
                                    } else {
                                        Log.e("StaffDashboardActivity", "Vendor not found for vendorId: " + vendorId);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Log.e("StaffDashboardActivity", "Failed to fetch vendor details", databaseError.toException());
                                }
                            });
                        } else {
                            Log.e("StaffDashboardActivity", "Transaction data is incomplete: Date=" + dateValue + ", Amount=" + amountValue + ", VendorId=" + vendorId);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("StaffDashboardActivity", "Failed to fetch transaction history", databaseError.toException());
                }
            });

        } else {
            Log.d("StaffDashboardActivity", "Current user is null");
        }
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
                TextView textViewVendorUserID = convertView.findViewById(R.id.textViewTransactionVendorUserID); // Ensure this ID matches the one in list_item_transaction.xml

                textViewDate.setText((String) transaction.get("date"));
                textViewAmount.setText((String) transaction.get("amount")); // Use amount as String
                textViewVendorUserID.setText((String) transaction.get("vendorUserID")); // Use the correct key for the vendor userID
            }

            return convertView;
        }
    }
}
