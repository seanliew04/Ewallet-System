package com.example.moneynow;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "MoneyNow.db"; // Database name
    private static final String TABLE_NAME = "users"; // Table name
    private static final String COL_1 = "id"; // ID column
    private static final String COL_2 = "email"; // Email column
    private static final String COL_3 = "password"; // Password column

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1); // Version 1
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createTable = "CREATE TABLE " + TABLE_NAME + " ("
                + COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_2 + " TEXT NOT NULL, "
                + COL_3 + " TEXT NOT NULL)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop the old table if it exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Add user method
    public boolean addUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, email); // Insert email
        contentValues.put(COL_3, password); // Insert password
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1; // Returns true if insert was successful
    }

    // Validate user login
    public boolean validateUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE email=? AND password=?", new String[]{email, password});
        return cursor.getCount() > 0; // Returns true if user exists
    }
}
