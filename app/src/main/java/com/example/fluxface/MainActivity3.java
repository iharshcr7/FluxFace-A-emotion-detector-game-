package com.example.fluxface;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity3 extends AppCompatActivity {

    EditText signupName, signupEmail, signupPassword, signupConfirmPassword;
    ImageButton signupButton;
    DatabaseHelper databaseHelper; // NEW

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        signupName = findViewById(R.id.signupName);
        signupEmail = findViewById(R.id.signupEmail);
        signupPassword = findViewById(R.id.signupPassword);
        signupConfirmPassword = findViewById(R.id.signupConfirmPassword);
        signupButton = findViewById(R.id.signupButton);

        databaseHelper = new DatabaseHelper(this); // NEW

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateSignup();
            }
        });
    }

    private void validateSignup() {
        String name = signupName.getText().toString().trim();
        String email = signupEmail.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();
        String confirmPassword = signupConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            signupName.setError("Enter your name");
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signupEmail.setError("Enter valid email");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            signupPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            signupConfirmPassword.setError("Passwords do not match");
            return;
        }

        // --- MODIFIED: Save to database instead of SharedPreferences ---
        boolean success = databaseHelper.addUser(name, email, password);

        if (success) {
            Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show();
            finish(); // Go back to login
        } else {
            Toast.makeText(this, "Email already exists. Please try another.", Toast.LENGTH_SHORT).show();
        }
    }
}