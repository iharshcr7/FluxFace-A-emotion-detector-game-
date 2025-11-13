package com.example.fluxface;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

public class MusicManager {

    public static MediaPlayer musicPlayer;
    private static boolean isMusicMuted = false;

    // Call this to START or RESUME the main theme
    public static void start(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        isMusicMuted = prefs.getBoolean(SettingsActivity.MUSIC_MUTED, false);

        if (isMusicMuted) {
            return; // Don't play if muted
        }

        if (musicPlayer == null) {
            musicPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.celtic_harp_323844);
            musicPlayer.setLooping(true);
        }

        if (!musicPlayer.isPlaying()) {
            musicPlayer.start();
        }
    }

    // Call this to PAUSE the main theme (e.g., when entering the game)
    public static void pause() {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
        }
    }

    // Call this when the app is fully closing
    public static void stop() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.release();
            musicPlayer = null;
        }
    }

    // Call this from Settings to update the mute status
    public static void updateMute(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        isMusicMuted = prefs.getBoolean(SettingsActivity.MUSIC_MUTED, false);

        if (isMusicMuted && musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
        } else if (!isMusicMuted && musicPlayer != null && !musicPlayer.isPlaying()) {
            // Only resume if it's supposed to be playing (i.e., we are in a menu)
            // This requires the activity to call start() again from its onStart()
        }
    }
}