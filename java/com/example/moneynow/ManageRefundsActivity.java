package com.example.moneynow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManageRefundsActivity extends AppCompatActivity {

    private List<RefundRequest> refundRequestsList = new ArrayList<>();
    private RefundRequestsAdapter refundRequestsAdapter;
    private ListView listView;
    private Button approveSelectedButton, rejectSelectedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_refunds);

        // Initialize ListView and Adapter
        listView = findViewById(R.id.refundRequestsListView);
        refundRequestsAdapter = new RefundRequestsAdapter(this, refundRequestsList);
        listView.setAdapter(refundRequestsAdapter);

        // Initialize Buttons
        approveSelectedButton = findViewById(R.id.approveSelectedButton);
        rejectSelectedButton = findViewById(R.id.rejectSelectedButton);
        View backButton = findViewById(R.id.backButton);

        // Set click listener for the back button
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(ManageRefundsActivity.this, AdminDashboardActivity.class);
            startActivity(intent);
            finish();
        });


        // Set Click Listeners
        approveSelectedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                approveSelectedRefunds();
            }
        });

        rejectSelectedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rejectSelectedRefunds();
            }
        });

        fetchRefundRequests();
    }

    private void fetchRefundRequests() {
        DatabaseReference refundRequestsRef = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("refundRequests");

        refundRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                refundRequestsList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String uid = snapshot.child("userId").getValue(String.class);
                    String vendorId = snapshot.child("vendorId").getValue(String.class);
                    String transactionId = snapshot.getKey(); // Use the key as the transaction ID
                    Long dateValue = snapshot.child("date").getValue(Long.class);
                    String amount = snapshot.child("amount").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);

                    if ("pending".equals(status)) {
                        String date = dateValue != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(dateValue)) : "Unknown Date";

                        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");
                        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot staffSnapshot) {
                                String staffId = staffSnapshot.child("userID").getValue(String.class);

                                Log.d("fetchRefundRequests", "Vendor ID: " + vendorId);

                                RefundRequest refundRequest = new RefundRequest(staffId, uid, vendorId, transactionId, date, amount, status);

                                refundRequestsList.add(refundRequest);
                                refundRequestsAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(ManageRefundsActivity.this, "Failed to fetch staff ID", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ManageRefundsActivity.this, "Failed to fetch refund requests", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void approveSelectedRefunds() {
        List<RefundRequest> selectedRefundRequests = refundRequestsAdapter.getSelectedRefundRequests();
        if (selectedRefundRequests.isEmpty()) {
            Toast.makeText(ManageRefundsActivity.this, "Cannot process refund because no requests are selected.", Toast.LENGTH_SHORT).show();
            return;
        }
        for (RefundRequest refundRequest : selectedRefundRequests) {
            // Update refund status and process refund
            updateRefundStatus(refundRequest, "approved");
            initiateRefundProcess(refundRequest);
        }
    }

    private void rejectSelectedRefunds() {
        List<RefundRequest> selectedRefundRequests = refundRequestsAdapter.getSelectedRefundRequests();
        if (selectedRefundRequests.isEmpty()) {
            Toast.makeText(ManageRefundsActivity.this, "Cannot reject refund because no requests are selected.", Toast.LENGTH_SHORT).show();
            return;
        }
        for (RefundRequest refundRequest : selectedRefundRequests) {
            // Update refund status to "rejected"
            updateRefundStatus(refundRequest, "rejected");
            refundRequestsList.remove(refundRequest); // Remove the rejected request from the list
            refundRequestsAdapter.notifyDataSetChanged(); // Update the adapter
        }
    }


    private void updateRefundStatus(RefundRequest refundRequest, String newStatus) {
        DatabaseReference refundRequestsRef = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("refundRequests");

        // Update the status in the existing node
        refundRequestsRef.child(refundRequest.getTransactionId()).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ManageRefundsActivity.this, "Refund status updated successfully", Toast.LENGTH_SHORT).show();

                    // Log the status change
                    Log.d("ManageRefundsActivity", "Updated status to: " + newStatus);

                    // Remove the request from the local list and notify the adapter
                    refundRequestsList.remove(refundRequest);
                    refundRequestsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(ManageRefundsActivity.this, "Failed to update refund status", Toast.LENGTH_SHORT).show());
    }


    private void initiateRefundProcess(RefundRequest refundRequest) {
        transferAmountToStaffAccount(refundRequest);
    }

    private void transferAmountToStaffAccount(RefundRequest refundRequest) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");

        Log.d("ManageRefundsActivity", "Processing refund for Staff UID: " + refundRequest.getUid() + ", Vendor ID: " + refundRequest.getVendorId());

        usersRef.child(refundRequest.getUid()).child("balance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                double currentStaffBalance = 0.0;
                if (dataSnapshot.exists()) {
                    Object balanceValue = dataSnapshot.getValue();
                    if (balanceValue instanceof Long) {
                        currentStaffBalance = ((Long) balanceValue).doubleValue();
                    } else if (balanceValue instanceof Double) {
                        currentStaffBalance = (Double) balanceValue;
                    } else if (balanceValue instanceof String) {
                        currentStaffBalance = Double.parseDouble((String) balanceValue);
                    }
                    Log.d("ManageRefundsActivity", "Current Staff Balance: " + currentStaffBalance);
                } else {
                    Log.d("ManageRefundsActivity", "Staff balance field not found, initializing to 0.00");
                    usersRef.child(refundRequest.getUid()).child("balance").setValue(0);
                    currentStaffBalance = 0.0;
                }

                String amountStr = refundRequest.getAmount().replace("RM", "").trim();
                double refundAmount = amountStr != null ? Double.parseDouble(amountStr) : 0.0;
                double newStaffBalance = currentStaffBalance + refundAmount;
                Log.d("ManageRefundsActivity", "New Staff Balance: " + newStaffBalance);

                usersRef.child(refundRequest.getUid()).child("balance").setValue(newStaffBalance)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ManageRefundsActivity.this, "Amount refunded to staff successfully", Toast.LENGTH_SHORT).show();
                            Log.d("ManageRefundsActivity", "Staff balance updated to: " + newStaffBalance);

                            // Add refund details to staff's transaction history
                            addRefundToTransactionHistory(refundRequest, refundAmount);

                            // Fetch vendor UID using vendor UserID
                            fetchVendorUIDUsingVendorUserID(refundRequest.getVendorId(), refundAmount);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ManageRefundsActivity.this, "Failed to update staff balance", Toast.LENGTH_SHORT).show();
                            Log.d("ManageRefundsActivity", "Failed to update staff balance: " + e.getMessage());
                        });
            }

            private void addRefundToTransactionHistory(RefundRequest refundRequest, double refundAmount) {
                DatabaseReference transactionHistoryRef = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .getReference("users").child(refundRequest.getUid()).child("transactionHistory");

                String transactionId = transactionHistoryRef.push().getKey();
                if (transactionId != null) {
                    Map<String, Object> transactionMap = new HashMap<>();
                    transactionMap.put("transactionId", transactionId);
                    transactionMap.put("amount", refundAmount);
                    transactionMap.put("date", System.currentTimeMillis());
                    transactionMap.put("type", "refund"); // Transaction type (e.g., refund)
                    transactionMap.put("vendorId", refundRequest.getVendorId());

                    transactionHistoryRef.child(transactionId).setValue(transactionMap)
                            .addOnSuccessListener(aVoid -> Log.d("ManageRefundsActivity", "Refund details added to transaction history"))
                            .addOnFailureListener(e -> Log.d("ManageRefundsActivity", "Failed to add refund details to transaction history", e));
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ManageRefundsActivity.this, "Failed to fetch staff balance", Toast.LENGTH_SHORT).show();
                Log.d("ManageRefundsActivity", "Failed to fetch staff balance: " + databaseError.getMessage());
            }
        });
    }


    private void fetchVendorUIDUsingVendorUserID(String vendorUserID, double refundAmount) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");

        usersRef.orderByChild("userID").equalTo(vendorUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String vendorUid = snapshot.getKey(); // Get the correct vendor UID
                        Log.d("fetchVendorUID", "Fetched Vendor UID: " + vendorUid);

                        // Deduct amount from the correct vendor balance
                        deductAmountFromVendor(vendorUid, refundAmount);
                    }
                } else {
                    Log.e("fetchVendorUID", "Vendor UserID not found!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("fetchVendorUID", "Failed to fetch vendor UID", databaseError.toException());
            }
        });
    }

    private void deductAmountFromVendor(String vendorUid, double refundAmount) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");

        Log.d("ManageRefundsActivity", "Attempting to update Vendor UID: " + vendorUid);

        usersRef.child(vendorUid).child("balance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                double currentVendorBalance = 0.0;
                if (dataSnapshot.exists()) {
                    Object balanceValue = dataSnapshot.getValue();
                    if (balanceValue instanceof Long) {
                        currentVendorBalance = ((Long) balanceValue).doubleValue();
                    } else if (balanceValue instanceof Double) {
                        currentVendorBalance = (Double) balanceValue;
                    } else if (balanceValue instanceof String) {
                        currentVendorBalance = Double.parseDouble((String) balanceValue);
                    }
                    Log.d("ManageRefundsActivity", "Current Vendor Balance: " + currentVendorBalance);
                } else {
                    Log.d("ManageRefundsActivity", "Vendor balance field not found, initializing to 0.00");
                    usersRef.child(vendorUid).child("balance").setValue(0);
                    currentVendorBalance = 0.0;
                }

                double newVendorBalance = currentVendorBalance - refundAmount;
                Log.d("ManageRefundsActivity", "New Vendor Balance: " + newVendorBalance);

                usersRef.child(vendorUid).child("balance").setValue(newVendorBalance)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ManageRefundsActivity.this, "Amount deducted from vendor successfully", Toast.LENGTH_SHORT).show();
                            Log.d("ManageRefundsActivity", "Vendor balance updated to: " + newVendorBalance);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ManageRefundsActivity.this, "Failed to update vendor balance", Toast.LENGTH_SHORT).show();
                            Log.d("ManageRefundsActivity", "Failed to update vendor balance: " + e.getMessage());
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ManageRefundsActivity.this, "Failed to fetch vendor balance", Toast.LENGTH_SHORT).show();
                Log.d("ManageRefundsActivity", "Failed to fetch vendor balance: " + databaseError.getMessage());
            }
        });
    }
}