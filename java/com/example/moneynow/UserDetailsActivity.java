package com.example.moneynow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

public class UserDetailsActivity extends AppCompatActivity {

    private TextView textViewUserID, textViewUserEmail, textViewUserRole, textViewUserBalance;
    private Button buttonDeleteAccount;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private String userID;
    private String userUid; // User UID in the database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        textViewUserID = findViewById(R.id.textViewUserID);
        textViewUserEmail = findViewById(R.id.textViewUserEmail);
        textViewUserRole = findViewById(R.id.textViewUserRole);
        textViewUserBalance = findViewById(R.id.textViewUserBalance);
        buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount);
        View backButton = findViewById(R.id.backButton);

        // Set click listener for the back button
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(UserDetailsActivity.this, ManageAccountActivity.class);
            startActivity(intent);
            finish();
        });

        // Get the data from the intent
        userID = getIntent().getStringExtra("USER_ID");
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        String userRole = getIntent().getStringExtra("USER_ROLE");

        // Set the user details
        textViewUserID.setText(userID);
        textViewUserEmail.setText(userEmail);
        textViewUserRole.setText(userRole);

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users");

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Fetch and display the user balance
        fetchUserBalance();

        // Set click listener for the delete account button
        buttonDeleteAccount.setOnClickListener(v -> deleteUserAccount());
    }

    private void fetchUserBalance() {
        databaseReference.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Object balanceObj = snapshot.child("balance").getValue();
                    userUid = snapshot.getKey(); // Get the correct user UID

                    // Check the type of the balance and convert to string
                    String balance;
                    if (balanceObj instanceof Long) {
                        balance = String.valueOf((Long) balanceObj);
                    } else if (balanceObj instanceof Double) {
                        balance = String.valueOf((Double) balanceObj);
                    } else if (balanceObj instanceof String) {
                        balance = (String) balanceObj;
                    } else {
                        balance = "0"; // Default value in case of null or unknown type
                    }

                    textViewUserBalance.setText(balance);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("UserDetailsActivity", "Failed to fetch user balance", databaseError.toException());
                Toast.makeText(UserDetailsActivity.this, "Failed to fetch user balance", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteUserAccount() {
        if (userUid != null) {
            // Delete the user from Firebase Authentication
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                // Delete from Realtime Database first
                databaseReference.child(userUid).removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Now delete from Authentication
                        currentUser.delete().addOnCompleteListener(authTask -> {
                            if (authTask.isSuccessful()) {
                                Toast.makeText(UserDetailsActivity.this, "User account deleted successfully", Toast.LENGTH_SHORT).show();
                                finish(); // Close the activity
                            } else {
                                Log.e("UserDetailsActivity", "Failed to delete user account from authentication", authTask.getException());
                                Toast.makeText(UserDetailsActivity.this, "Failed to delete user account from authentication", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Log.e("UserDetailsActivity", "Failed to delete user from database", task.getException());
                        Toast.makeText(UserDetailsActivity.this, "Failed to delete user from database", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(UserDetailsActivity.this, "User authentication not found, only removed from database", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity
            }
        } else {
            Toast.makeText(UserDetailsActivity.this, "User UID not found, cannot delete account", Toast.LENGTH_SHORT).show();
        }
    }
}
