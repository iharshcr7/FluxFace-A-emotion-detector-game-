package com.example.fluxface;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    SeekBar ageSeekBar;
    TextView ageText;
    ImageButton enterBtn;
    private int selectedAge = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ageText = findViewById(R.id.ageText);
        ageSeekBar = findViewById(R.id.ageSeekBar);
        enterBtn = findViewById(R.id.btnEnter);

        ageSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ageText.setText("Age: " + progress);
                selectedAge = progress;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // MODIFIED: This now opens the Dashboard (MainActivity4)
        enterBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MainActivity4.class);
            startActivity(intent);
        });
    }

}