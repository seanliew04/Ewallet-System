package com.example.moneynow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageAccountActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextRole, editTextBalance, editTextPassword, editTextID;
    private Button buttonAddUser;
    private ListView userListView;
    private DatabaseReference databaseReference;
    private List<User> userList; // List of User objects
    private ArrayAdapter<User> userAdapter;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_management);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://moneynow-8e209-default-rtdb.asia-southeast1.firebasedatabase.app");
        databaseReference = database.getReference("users");

        // Initialize UI components
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextRole = findViewById(R.id.editTextRole);
        editTextBalance = findViewById(R.id.editTextBalance);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextID = findViewById(R.id.editTextID);
        buttonAddUser = findViewById(R.id.buttonAddUser);
        userListView = findViewById(R.id.userListView);
        View backButton = findViewById(R.id.backButton);

        // Initialize user list and adapter
        userList = new ArrayList<>();
        userAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);
        userListView.setAdapter(userAdapter);

        // Set click listener for the back button
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(ManageAccountActivity.this, AdminDashboardActivity.class);
            startActivity(intent);
            finish();
        });
        // Set click listener for the Add User button
        buttonAddUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString().trim();
                String role = editTextRole.getText().toString().trim();
                String balanceStr = editTextBalance.getText().toString().trim();
                double balance = balanceStr.isEmpty() ? 0 : Double.parseDouble(balanceStr);
                String password = editTextPassword.getText().toString().trim(); // Get the user input password
                String userID = editTextID.getText().toString().trim();

                if (!email.isEmpty() && !role.isEmpty() && !password.isEmpty() && !userID.isEmpty()) {
                    createUserInAuthAndDatabase(email, password, role, balance, userID);
                } else {
                    // Show error message
                    Log.e("ManageAccountActivity", "Email, Role, Password, or User ID is empty");
                }
            }
        });



        // Fetch existing users to display in the ListView
        fetchUsers();

        // Set the item click listener
        userListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected user
                User selectedUser = userList.get(position);

                // Start UserDetailsActivity with the selected user details
                Intent intent = new Intent(ManageAccountActivity.this, UserDetailsActivity.class);
                intent.putExtra("USER_ID", selectedUser.getUserID());
                intent.putExtra("USER_EMAIL", selectedUser.getEmail());
                intent.putExtra("USER_ROLE", selectedUser.getRole());
                startActivity(intent);
            }
        });
    }

    private void createUserInAuthAndDatabase(String email, String password, String role, double balance, String userID) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // User is successfully created in Firebase Authentication
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();
                                saveUserDetails(uid, email, role, balance, userID); // Save user details in Realtime Database
                            }
                        } else {
                            // Handle creation failure
                            Log.e("ManageAccountActivity", "User creation failed", task.getException());
                        }
                    }
                });
    }

    private void saveUserDetails(String uid, String email, String role, double balance, String userID) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);
        userMap.put("role", role);
        userMap.put("balance", balance);
        userMap.put("userID", userID);

        // Initialize empty transaction history
        userMap.put("transactionHistory", new HashMap<>()); // Empty transaction history

        databaseReference.child(uid).setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d("ManageAccountActivity", "User details saved successfully");
                    // Optionally, clear fields or show success message
                    editTextEmail.setText("");
                    editTextRole.setText("");
                    editTextBalance.setText("");
                    editTextPassword.setText("");
                    editTextID.setText("");
                    fetchUsers(); // Refresh the user list
                } else {
                    Log.e("ManageAccountActivity", "Failed to save user details", task.getException());
                }
            }
        });
    }

    private void fetchUsers() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear(); // Clear the previous list
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String uid = snapshot.getKey();
                    String email = snapshot.child("email").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);
                    String userID = snapshot.child("userID").getValue(String.class);

                    if (email != null && role != null && userID != null) {
                        userList.add(new User(uid, email, role, userID));
                    }
                }
                userAdapter.notifyDataSetChanged(); // Notify adapter to refresh the ListView
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ManageAccountActivity", "Failed to fetch users", databaseError.toException());
            }
        });
    }
}
