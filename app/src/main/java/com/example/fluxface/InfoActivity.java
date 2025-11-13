package com.example.fluxface;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        ImageButton backButton = findViewById(R.id.btnBackInfo);
        backButton.setOnClickListener(v -> {
            finish(); // Closes this screen and goes back to the dashboard
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Ensure music continues playing
        MusicManager.start(this);
    }
}