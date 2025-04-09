package com.example.activityfeedback.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.activityfeedback.R;
import com.example.activityfeedback.utils.FirebaseHelper;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText;
    private RadioGroup roleRadioGroup;
    private Button registerButton;
    private TextView loginTextView;
    private ProgressBar progressBar;

    private EditText rollEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        nameEditText = findViewById(R.id.name_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        rollEditText = findViewById(R.id.roll_edit_text);
        roleRadioGroup = findViewById(R.id.role_radio_group);
        registerButton = findViewById(R.id.register_button);
        loginTextView = findViewById(R.id.login_text_view);
        progressBar = findViewById(R.id.progress_bar);

        // Setup register button
        registerButton.setOnClickListener(v -> attemptRegistration());

        // Setup login text view
        loginTextView.setOnClickListener(v -> {
            finish(); // Go back to login
        });
    }

    private void attemptRegistration() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Check selected role
        int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRole = findViewById(selectedRoleId);
        String role = selectedRole.getText().toString().toLowerCase();

        // Validate inputs
        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            nameEditText.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        String rollNumber = rollEditText.getText().toString().trim();

        if (rollNumber.isEmpty()) {
            rollEditText.setError("Roll number is required");
            rollEditText.requestFocus();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);

        // Attempt registration
        FirebaseHelper.registerUser(email, password, name, rollNumber, role,  task -> {
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);

            if (task.isSuccessful()) {
                Toast.makeText(RegisterActivity.this,
                        "Registration successful", Toast.LENGTH_SHORT).show();

                // Navigate to appropriate dashboard
                Intent intent;
                if ("professor".equals(role)) {
                    intent = new Intent(RegisterActivity.this, ProfessorDashboardActivity.class);
                } else {
                    intent = new Intent(RegisterActivity.this, StudentDashboardActivity.class);
                }
                String userId = FirebaseHelper.getCurrentUserId();
                intent.putExtra("userId", userId);
                intent.putExtra("userName", name);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(RegisterActivity.this,
                        "Registration failed: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
                System.out.println("Registration failed: " + task.getException().getMessage());
            }
        });
    }
}