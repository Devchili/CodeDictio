package com.meranel.codediction;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private TextView loginLink;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ensure requestWindowFeature() is called before adding content
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText); // Initialize confirm password field
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();  // Initialize Firestore

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Login Activity
                startActivity(new Intent(Register.this, Login.class));
            }
        });
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim(); // Get confirm password

        // Validate confirm password
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            confirmPasswordEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required.");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email.");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required.");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters long.");
            passwordEditText.requestFocus();
            return;
        }

        registerButton.setEnabled(false); // Disable button to prevent multiple clicks

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        registerButton.setEnabled(true); // Re-enable button

                        if (task.isSuccessful()) {
                            Toast.makeText(Register.this, "User Registered Successfully", Toast.LENGTH_SHORT).show();
                            // Store user data in Firestore
                            storeUserData();
                        } else {
                            Toast.makeText(Register.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void storeUserData() {
        String userId = mAuth.getCurrentUser().getUid();
        String email = emailEditText.getText().toString().trim();

        // Create a new user with their email and other information
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        // Add more fields as required

        // Add a new document with the user ID
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Register.this, "User Data Saved Successfully", Toast.LENGTH_SHORT).show();
                    // Navigate to another activity or main screen
                    startActivity(new Intent(Register.this, Login.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(Register.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
