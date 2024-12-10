package com.example.moneynow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private ImageView loginButton;
    private TextView forgotPasswordLink;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // Ensure this is the correct layout file

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app");
        databaseReference = database.getReference("users");

        // Initialize UI components
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);

        // Set click listener for the login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Snackbar.make(v, "Please fill in all fields", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                // Attempt to log in via Firebase
                loginUser(email, password, v);
            }
        });

        // Set click listener for the forgot password link
        forgotPasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    // Method to handle login via Firebase
    private void loginUser(String email, String password, View view) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                            if (currentUser != null) {
                                String userId = currentUser.getUid();
                                Log.d("LoginSuccess", "User logged in with ID: " + userId);

                                // Fetch user role and navigate accordingly
                                fetchUserRoleAndNavigate(userId);
                            }
                        } else {
                            Log.e("LoginFailed", task.getException().getMessage());

                            if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                showAlertDialog("User not found", "The email address or password is incorrect. Please try again.");
                            } else {
                                Snackbar.make(view, "Login failed: " + task.getException().getMessage(), Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    // Method to show alert dialog
    private void showAlertDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Method to fetch user role and navigate accordingly
    private void fetchUserRoleAndNavigate(String userId) {
        DatabaseReference userRef = databaseReference.child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String role = dataSnapshot.child("role").getValue(String.class);

                    if (role != null) {
                        navigateToRoleBasedActivity(role);
                    } else {
                        Log.e("LoginActivity", "User role not found");
                    }
                } else {
                    Log.e("LoginActivity", "User data not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("LoginActivity", "Database error: " + databaseError.getMessage());
            }
        });
    }

    // Method to navigate to the appropriate activity based on role
    private void navigateToRoleBasedActivity(String role) {
        Intent intent;

        switch (role) {
            case "admin":
                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                break;
            case "staff":
                intent = new Intent(LoginActivity.this, StaffDashboardActivity.class);
                break;
            case "vendor":
                intent = new Intent(LoginActivity.this, VendorDashboardActivity.class);
                break;
            default:
                Log.e("LoginActivity", "Unknown role: " + role);
                return;
        }

        startActivity(intent);
        finish(); // Close login activity
    }
}
