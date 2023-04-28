package com.fgtit.reader.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.fgtit.reader.models.User;

import java.util.ArrayList;

public class DBService extends SQLiteOpenHelper {

    private static final String DB_NAME = "fingerprint_db";
    private static final int DB_VERSION = 4;

    public DBService(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT,"
                + "username TEXT,"
                + "profile_photo_url TEXT DEFAULT NULL,"
                + "fp_data TEXT DEFAULT NULL,"
                + "card_data TEXT DEFAULT NULL,"
                + "created_at NUMERIC DEFAULT CURRENT_TIMESTAMP);";

        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Log.e("DBService", "onUpgrade: " + oldVersion + " -> " + newVersion);
        // If you need to add a column
        if (newVersion > oldVersion) {
            try{
                db.execSQL("ALTER TABLE users ADD COLUMN profile_photo_url TEXT DEFAULT NULL");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<User> performGetUsersQuery(String sqlQuery) {
        ArrayList<User> users = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(sqlQuery, null);

        if (cursor.moveToFirst()) {
            do {
                User user = new User(
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        null
                );

                users.add(user);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return users;
    }

    public ArrayList<User> getAllUsers() {
        return performGetUsersQuery("SELECT " +
                "id,name,username,fp_data,card_data,profile_photo_url,created_at " +
                "FROM users ORDER BY id DESC LIMIT 300");
    }

    public ArrayList<User> getAllUsers(String searchTerm) {
        return performGetUsersQuery("SELECT " +
                "id,name,username,fp_data,card_data,profile_photo_url,created_at " +
                "FROM users WHERE name LIKE '%" + searchTerm + "%' OR username LIKE '%" +
                searchTerm + "%' ORDER BY id DESC LIMIT 300");
    }

    public void addNewUser(String name, String username, String fp_data, String card_data, String profile_photo_url) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("name", name);
        values.put("username", username);
        values.put("fp_data", fp_data);
        values.put("card_data", card_data);
        values.put("profile_photo_url", profile_photo_url);

        db.insert("users", null, values);

        db.close();
    }

    public User findUserByFpOrCard(String data) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " +
                "id,name,username,fp_data,card_data,profile_photo_url,created_at " +
                "FROM users WHERE fp_data = ? OR card_data = ?", new String[]{data, data});

        User user = null;

        if (cursor.moveToFirst()) {
            user = new User(
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    null
            );
        }

        cursor.close();
        db.close();

        return user;
    }

    public void updateFingerPrintData(String username, String fp_data) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("fp_data", fp_data);

        db.update("users", values, "username = ?", new String[]{username});

        db.close();
    }

    public void updateCardData(String username, String card_data) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("card_data", card_data);

        db.update("users", values, "username = ?", new String[]{username});

        db.close();
    }

    public void deleteUser(String username) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete("users", "username = ?", new String[]{username});

        db.close();
    }
}

