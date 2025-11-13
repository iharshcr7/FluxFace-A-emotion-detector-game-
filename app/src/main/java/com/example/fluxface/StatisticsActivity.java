package com.example.fluxface;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    TextView statBestEasy, statBestNormal, statBestHard, statTotalPlayed, statWinPercentage;
    ImageButton backButton;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Find all views
        statBestEasy = findViewById(R.id.statBestEasy);
        statBestNormal = findViewById(R.id.statBestNormal);
        statBestHard = findViewById(R.id.statBestHard);
        statTotalPlayed = findViewById(R.id.statTotalPlayed);
        statWinPercentage = findViewById(R.id.statWinPercentage);
        backButton = findViewById(R.id.btnBackStats);

        dbHelper = new DatabaseHelper(this);

        backButton.setOnClickListener(v -> finish());

        loadStats();
    }

    // In StatisticsActivity.java

    private void loadStats() {
        // Get the current logged-in user's ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("userEmail", null);

        if (userEmail == null) {
            Toast.makeText(this, "Please log in to view stats", Toast.LENGTH_SHORT).show();
            // Set all stats to default text if not logged in
            statBestEasy.setText("Calm Mode: --:--");
            statBestNormal.setText("Normal Mode: --:--");
            statBestHard.setText("Challenge Mode: --:--");
            statTotalPlayed.setText("Total Games Played: 0");
            statWinPercentage.setText("Win Percentage: --%");
            return;
        }

        int userId = dbHelper.getUserId(userEmail);
        if (userId == -1) {
            Toast.makeText(this, "User not found in database.", Toast.LENGTH_SHORT).show();
            return; // User not found
        }

        // --- Fetch Stats ---
        int bestEasy = dbHelper.getBestTime(userId, "EASY");
        int bestNormal = dbHelper.getBestTime(userId, "NORMAL");
        int bestHard = dbHelper.getBestTime(userId, "HARD");
        int totalPlayed = dbHelper.getTotalGamesPlayed(userId);

        // --- NEW: Call your getWinPercentage method ---
        double winPercentage = dbHelper.getWinPercentage(userId);

        // --- Display Stats ---
        statBestEasy.setText("Calm Mode: " + formatTime(bestEasy));
        statBestNormal.setText("Normal Mode: " + formatTime(bestNormal));
        statBestHard.setText("Challenge Mode: " + formatTime(bestHard));
        statTotalPlayed.setText("Total Games Played: " + totalPlayed);

        // --- NEW: Display the calculated win percentage ---
        // We use String.format to show it with one decimal place, e.g., "85.7%"
        statWinPercentage.setText(String.format(Locale.getDefault(), "Win Percentage: %.1f%%", winPercentage));
    }


    /**
     * Helper method to convert seconds (e.g., 90) to a "MM:SS" string (e.g., "01:30").
     */
    private String formatTime(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "--:--";
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}