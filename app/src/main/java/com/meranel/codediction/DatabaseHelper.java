package com.meranel.codediction;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "bookmarks.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_BOOKMARKS = "bookmarks";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_WORD = "word";
    private static final String COLUMN_MEANING = "meaning";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_BOOKMARKS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_WORD + " TEXT, " +
                COLUMN_MEANING + " TEXT)";
        db.execSQL(createTableQuery);
    }

    public void insertBookmark(Context context, String word, String meaning) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if the word already exists in the database
        if (!isWordBookmarked(db, word)) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_WORD, word);
            values.put(COLUMN_MEANING, meaning);
            try {
                long result = db.insertOrThrow(TABLE_BOOKMARKS, null, values);
                if (result != -1) {
                    Log.d("Database", "Bookmark inserted successfully: " + word);
                    // Show a toast message indicating successful insertion
                    Toast.makeText(context, "Bookmark added for: " + word, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("Database", "Failed to insert bookmark: " + word);
                }
            } catch (Exception e) {
                Log.e("Database", "Error inserting bookmark: " + e.getMessage());
                e.printStackTrace();
            } finally {
                db.close();
            }
        } else {
            Toast.makeText(context, "Bookmark already exists for: " + word, Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to check if a word is already bookmarked
    private boolean isWordBookmarked(SQLiteDatabase db, String word) {
        Cursor cursor = db.query(TABLE_BOOKMARKS, null, COLUMN_WORD + "=?", new String[]{word}, null, null, null);
        boolean isBookmarked = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return isBookmarked;
    }


    public void deleteBookmark(String word) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_BOOKMARKS, COLUMN_WORD + " = ?", new String[]{word});
        } catch (Exception e) {
            Log.e("Database", "Error deleting bookmark: " + e.getMessage());
            e.printStackTrace();
        } finally {
            db.close();
        }
    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade logic if needed
    }
}
