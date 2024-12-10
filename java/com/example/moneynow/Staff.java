package com.example.moneynow;

public class Staff {
    private String name;
    private String email;
    private String role; // Add role field

    public Staff() {
        // Default constructor required for calls to DataSnapshot.getValue(Staff.class)
    }

    public Staff(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role; // Set role
    }

    // Getters and setters (if needed)
}
