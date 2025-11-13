package com.example.fluxface;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SplashActivity extends AppCompatActivity {

    VideoView videoView;
    private Handler splashHandler;
    private Runnable startVideoRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This block makes the app full-screen
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        setContentView(R.layout.activity_splash);

        videoView = findViewById(R.id.bgVideo);

        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.intro_gray;
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);

        // Prepare listener to proceed when video completes
        videoView.setOnCompletionListener(mp -> {
            // Redirect to MainActivity4 instead of MainActivity after splash video
            Intent intent = new Intent(SplashActivity.this, MainActivity4.class);
            startActivity(intent);
            finish();
        });

        // Start the video immediately (no delay) and keep the smooth fade-in
        videoView.setAlpha(0f);
        splashHandler = new Handler(Looper.getMainLooper());
        startVideoRunnable = new Runnable() {
            @Override
            public void run() {
                if (videoView != null) {
                    videoView.start();
                    // Fade in quickly so the transition looks smooth
                    videoView.animate().alpha(1f).setDuration(100).start();
                }
            }
        };
        // Start immediately (no delay) and keep the smooth fade-in
        startVideoRunnable.run();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (splashHandler != null && startVideoRunnable != null) {
            splashHandler.removeCallbacks(startVideoRunnable);
        }
    }
}