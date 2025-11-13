package com.example.fluxface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity2 extends AppCompatActivity {

    EditText loginEmail, loginPassword;
    ImageButton loginButton;
    TextView goToSignup;
    DatabaseHelper databaseHelper; // NEW

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        goToSignup = findViewById(R.id.goToSignup);

        databaseHelper = new DatabaseHelper(this); // NEW

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateLogin(v);
            }
        });

        goToSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, MainActivity3.class);
                startActivity(intent);
            }
        });
    }

    private void validateLogin(View view) {
        String email = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.setError("Enter valid email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            loginPassword.setError("Password is required");
            return;
        }

        // --- MODIFIED: Check database and save to SharedPreferences ---
        String userName = databaseHelper.checkUserLogin(email, password);

        if (userName != null) {
            // Login is successful! Save user state to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("userName", userName);
            editor.putString("userEmail", email); // Save email to use as a user ID
            editor.putBoolean("isLoggedIn", true);
            editor.apply();

            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MainActivity2.this, MainActivity4.class);
            startActivity(intent);
            finish(); // Close login activity
        } else {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show();
        }
    }
}