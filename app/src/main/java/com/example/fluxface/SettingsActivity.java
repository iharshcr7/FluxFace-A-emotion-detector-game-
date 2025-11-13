package com.example.fluxface;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat musicSwitch, sfxSwitch;
    private ImageButton backButton;
    private SharedPreferences prefs;

    // We define public keys so all activities can access them
    public static final String PREFS_NAME = "GameSettings";
    public static final String MUSIC_MUTED = "MusicMuted";
    public static final String SFX_MUTED = "SfxMuted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        musicSwitch = findViewById(R.id.switchMusic);
        sfxSwitch = findViewById(R.id.switchSfx);
        backButton = findViewById(R.id.btnBackSettings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 1. Load saved preferences
        loadSettings();

        // 2. Save preference when Music switch is toggled
        musicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(MUSIC_MUTED, !isChecked).apply();
            // Immediately update the music player
            MusicManager.updateMute(this);
            // If music was just un-muted, start it
            if (isChecked) {
                MusicManager.start(this);
            }
        });

        // 3. Save preference when Sound Effects switch is toggled
        sfxSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(SFX_MUTED, !isChecked).apply();
        });

        // 4. Back button listener
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Ensure music continues playing
        MusicManager.start(this);
    }

    // In your setOnCheckedChangeListener for the music switch:

    private void loadSettings() {
        // Load the saved "muted" state. Default to "false" (not muted).
        boolean isMusicMuted = prefs.getBoolean(MUSIC_MUTED, false);
        boolean isSfxMuted = prefs.getBoolean(SFX_MUTED, false);

        // Set the switches. Remember, "Checked" = ON, so we use !isMuted.
        musicSwitch.setChecked(!isMusicMuted);
        sfxSwitch.setChecked(!isSfxMuted);
    }
}