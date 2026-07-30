package com.example.connectu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    EditText etStudentId, etPassword;
    Button btnLogin;
    ImageView imgTogglePassword;
    FirebaseFirestore db;

    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Check existing login session
        SharedPreferences prefs = getSharedPreferences("ConnectU", MODE_PRIVATE);
        String savedStudentId = prefs.getString("studentId", null);

        if (savedStudentId != null && !savedStudentId.isEmpty()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("studentId", savedStudentId);
            startActivity(intent);
            finish();
            return;
        }

        // 2. Initialize UI components
        etStudentId = findViewById(R.id.etStudentId);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        imgTogglePassword = findViewById(R.id.imgTogglePassword);

        // 3. Initialize Firestore database
        db = FirebaseFirestore.getInstance();

        // 4. Set button actions
        imgTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        btnLogin.setOnClickListener(v -> loginStudent());
    }

    private void togglePasswordVisibility() {
        // 5. Toggle password visibility
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imgTogglePassword.setImageResource(R.drawable.eye_close);
            isPasswordVisible = false;
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            imgTogglePassword.setImageResource(R.drawable.eye_open);
            isPasswordVisible = true;
        }

        etPassword.setSelection(etPassword.getText().length());
    }

    private void loginStudent() {
        // 6. Get user input
        String studentId = etStudentId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (studentId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter Student ID and password", Toast.LENGTH_SHORT).show();
            return;
        }
        // 7. Validate login credentials from Firestore
        db.collection("students").document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String correctPassword = documentSnapshot.getString("password");

                        if (password.equals(correctPassword)) {
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                            // 8. Save login session
                            SharedPreferences prefs = getSharedPreferences("ConnectU", MODE_PRIVATE);
                            prefs.edit().putString("studentId", studentId).apply();
                            // 9. Redirect user to main page
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("studentId", studentId);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}