package com.example.moneynow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Button buttonManageRefunds = findViewById(R.id.buttonManageRefunds);
        Button buttonManageAccounts = findViewById(R.id.buttonManageAccounts);
        Button buttonBackToLogin = findViewById(R.id.buttonBackToLogin);

        buttonManageRefunds.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManageRefundsActivity.class);
            startActivity(intent);
        });

        buttonManageAccounts.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManageAccountActivity.class);
            startActivity(intent);
        });

        buttonBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
