package com.example.fluxface;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    // --- Database Info ---
    public static final String DATABASE_NAME = "FluxFace.db";
    public static final int DATABASE_VERSION = 1;

    // --- Users Table ---
    public static final String USERS_TABLE = "USERS_TABLE";
    public static final String COLUMN_USER_ID = "ID";
    public static final String COLUMN_USER_NAME = "USER_NAME";
    public static final String COLUMN_USER_EMAIL = "USER_EMAIL";
    public static final String COLUMN_USER_PASSWORD = "USER_PASSWORD";

    // --- Scores Table ---
    public static final String SCORES_TABLE = "SCORES_TABLE";
    public static final String COLUMN_SCORE_ID = "ID";
    public static final String COLUMN_SCORE_USER_ID_FK = "USER_ID"; // Foreign key to link to USERS_TABLE
    public static final String COLUMN_SCORE_DIFFICULTY = "DIFFICULTY"; // "EASY", "NORMAL", "HARD"
    public static final String COLUMN_SCORE_TIME_SECONDS = "TIME_SECONDS";
    public static final String COLUMN_SCORE_SCORE = "SCORE";
    public static final String COLUMN_GAME_WON = "GAME_WON";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Called the first time the database is created
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users Table
        String createUsersTableStatement = "CREATE TABLE " + USERS_TABLE + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USER_NAME + " TEXT, " +
                COLUMN_USER_EMAIL + " TEXT UNIQUE, " +
                COLUMN_USER_PASSWORD + " TEXT)";
        db.execSQL(createUsersTableStatement);

        // Create Scores Table
        String createScoresTableStatement = "CREATE TABLE " + SCORES_TABLE + " (" +
                COLUMN_SCORE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SCORE_USER_ID_FK + " INTEGER, " +
                COLUMN_SCORE_DIFFICULTY + " TEXT, " +
                COLUMN_SCORE_TIME_SECONDS + " INTEGER, " +
                COLUMN_SCORE_SCORE + " INTEGER, " +
                COLUMN_GAME_WON + " INTEGER, " + // 1 for true, 0 for false
                "FOREIGN KEY(" + COLUMN_SCORE_USER_ID_FK + ") REFERENCES " + USERS_TABLE + "(" + COLUMN_USER_ID + "))";
        db.execSQL(createScoresTableStatement);
    }

    // Called if the database version number changes
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For a simple project, we can just drop and recreate
        db.execSQL("DROP TABLE IF EXISTS " + SCORES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + USERS_TABLE);
        onCreate(db);
    }

    // --- User Methods ---

    /**
     * Adds a new user to the database.
     * @return true if successful, false otherwise.
     */
    public boolean addUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_USER_NAME, name);
        cv.put(COLUMN_USER_EMAIL, email);
        cv.put(COLUMN_USER_PASSWORD, password); // Note: In a real app, you would HASH this password

        long insert = db.insert(USERS_TABLE, null, cv);
        db.close();
        return insert != -1;
    }

    /**
     * Checks if a user's email and password are correct.
     * @return The user's name if login is successful, null otherwise.
     */
    public String checkUserLogin(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_ID, COLUMN_USER_NAME};
        String selection = COLUMN_USER_EMAIL + " = ? AND " + COLUMN_USER_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};

        Cursor cursor = db.query(USERS_TABLE, columns, selection, selectionArgs, null, null, null);

        String userName = null;
        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(COLUMN_USER_NAME);
            if(nameColumnIndex != -1) {
                userName = cursor.getString(nameColumnIndex);
            }
        }
        cursor.close();
        db.close();
        return userName;
    }

    // ... (Your addUser and checkUserLogin methods are here) ...

    /**
     * Gets the user's ID from their email.
     * @return User ID, or -1 if not found.
     */
    public int getUserId(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(USERS_TABLE, new String[]{COLUMN_USER_ID},
                COLUMN_USER_EMAIL + " = ?", new String[]{email},
                null, null, null);
        int userId = -1;
        if (cursor.moveToFirst()) {
            int userIdIndex = cursor.getColumnIndex(COLUMN_USER_ID);
            if (userIdIndex != -1) {
                userId = cursor.getInt(userIdIndex);
            }
        }
        cursor.close();
        db.close();
        return userId;
    }

    /**
     * Adds a new game score to the database.
     */
    /**
     * Adds a new game result (win or loss) to the database.
     */
    public void addGameResult(int userId, String difficulty, int timeInSeconds, int score, boolean isWin) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_SCORE_USER_ID_FK, userId);
        cv.put(COLUMN_SCORE_DIFFICULTY, difficulty);
        cv.put(COLUMN_SCORE_TIME_SECONDS, timeInSeconds);
        cv.put(COLUMN_SCORE_SCORE, score);
        cv.put(COLUMN_GAME_WON, isWin ? 1 : 0); // Store boolean as 1 or 0

        db.insert(SCORES_TABLE, null, cv);
        db.close();
    }

    /**
     * Gets the user's best time for a specific difficulty.
     * @return Best time in seconds, or -1 if no time is recorded.
     */
    public int getBestTime(int userId, String difficulty) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT MIN(" + COLUMN_SCORE_TIME_SECONDS + ") FROM " + SCORES_TABLE +
                " WHERE " + COLUMN_SCORE_USER_ID_FK + " = " + userId +
                " AND " + COLUMN_SCORE_DIFFICULTY + " = '" + difficulty + "'";

        Cursor cursor = db.rawQuery(query, null);
        int bestTime = -1;
        if (cursor.moveToFirst()) {
            bestTime = cursor.getInt(0);
        }
        cursor.close();
        db.close();

        // MIN() returns 0 if no rows match, so we check and return -1
        return (bestTime == 0) ? -1 : bestTime;
    }

    /**
     * Gets the total number of games played by the user.
     */
    public int getTotalGamesPlayed(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + SCORES_TABLE +
                " WHERE " + COLUMN_SCORE_USER_ID_FK + " = " + userId;

        Cursor cursor = db.rawQuery(query, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    /**
     * Gets the user's win percentage.
     * @return A value between 0 and 100.
     */
    public int getWinPercentage(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Get total games
        Cursor cursorTotal = db.rawQuery("SELECT COUNT(*) FROM " + SCORES_TABLE + " WHERE " + COLUMN_SCORE_USER_ID_FK + " = " + userId, null);
        int totalGames = 0;
        if (cursorTotal.moveToFirst()) {
            totalGames = cursorTotal.getInt(0);
        }
        cursorTotal.close();

        if (totalGames == 0) {
            return 0; // Avoid division by zero
        }

        // Get games won
        Cursor cursorWon = db.rawQuery("SELECT COUNT(*) FROM " + SCORES_TABLE + " WHERE " + COLUMN_SCORE_USER_ID_FK + " = " + userId + " AND " + COLUMN_GAME_WON + " = 1", null);
        int gamesWon = 0;
        if (cursorWon.moveToFirst()) {
            gamesWon = cursorWon.getInt(0);
        }
        cursorWon.close();
        db.close();

        // Calculate percentage
        return (int) (((double) gamesWon / totalGames) * 100.0);
    }
}