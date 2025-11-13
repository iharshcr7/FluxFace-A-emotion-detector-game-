package com.example.fluxface;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity5 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main5);

        ImageButton happyButton = findViewById(R.id.happyButton);
        ImageButton neutralButton = findViewById(R.id.neutralButton);
        ImageButton sadButton = findViewById(R.id.sadButton);

        // When Happy button is clicked, start the game in HARD mode
        happyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity5.this, GameActivity.class);
            intent.putExtra("DIFFICULTY", "HARD");
            startActivity(intent);
        });

        // When Neutral button is clicked, start the game in MEDIUM mode
        neutralButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity5.this, GameActivity.class);
            intent.putExtra("DIFFICULTY", "MEDIUM");
            startActivity(intent);
        });

        // When Sad button is clicked, start the game in EASY mode
        sadButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity5.this, GameActivity.class);
            intent.putExtra("DIFFICULTY", "EASY");
            startActivity(intent);
        });
    }
}