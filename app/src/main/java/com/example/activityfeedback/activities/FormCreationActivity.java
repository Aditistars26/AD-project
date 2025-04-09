package com.example.activityfeedback.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import com.example.activityfeedback.R;
import com.example.activityfeedback.adapters.QuestionAdapter;
import com.example.activityfeedback.models.Form;
import com.example.activityfeedback.models.Question;
import com.example.activityfeedback.utils.FirebaseHelper;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormCreationActivity extends AppCompatActivity {

    private String userId;
    private EditText titleEditText, descriptionEditText;
    private Button addQuestionButton, saveFormButton;
    private RecyclerView questionsRecyclerView;
    private QuestionAdapter questionAdapter;
    private List<Question> questionList;
    private ProgressBar progressBar;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_creation);

        // Get user ID from intent
        userId = getIntent().getStringExtra("userId");

        // Set up ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Feedback Form");
        }

        // Initialize views
        titleEditText = findViewById(R.id.title_edit_text);
        descriptionEditText = findViewById(R.id.description_edit_text);
        addQuestionButton = findViewById(R.id.add_question_button);
        saveFormButton = findViewById(R.id.save_form_button);
        questionsRecyclerView = findViewById(R.id.questions_recycler_view);
        progressBar = findViewById(R.id.progress_bar);

        // Setup RecyclerView for questions
        questionList = new ArrayList<>();
        questionAdapter = new QuestionAdapter(this, questionList);
        questionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        questionsRecyclerView.setAdapter(questionAdapter);

        // Setup add question button
        addQuestionButton.setOnClickListener(v -> showAddQuestionDialog());

        // Setup save form button
        saveFormButton.setOnClickListener(v -> saveForm());
    }

    private void showAddQuestionDialog() {
        // Clear focus from any EditText and hide keyboard
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            currentFocus.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }

        // Rest of the dialog creation code remains the same
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.item_question_input, null);
        builder.setView(dialogView);

        EditText questionTextEditText = dialogView.findViewById(R.id.question_text_edit_text);
        RadioGroup questionTypeRadioGroup = dialogView.findViewById(R.id.question_type_radio_group);
        TextInputLayout optionsLayout = dialogView.findViewById(R.id.options_layout);
        EditText optionsEditText = dialogView.findViewById(R.id.options_edit_text);
        TextInputLayout ratingMinLayout = dialogView.findViewById(R.id.rating_min_layout);
        EditText ratingMinEditText = dialogView.findViewById(R.id.rating_min_edit_text);
        TextInputLayout ratingMaxLayout = dialogView.findViewById(R.id.rating_max_layout);
        EditText ratingMaxEditText = dialogView.findViewById(R.id.rating_max_edit_text);

        // Show/hide fields based on selected question type
        questionTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButton = dialogView.findViewById(checkedId);
            String type = radioButton.getText().toString().toLowerCase();

            switch (type) {
                case "multiple choice":
                    optionsLayout.setVisibility(View.VISIBLE);
                    ratingMinLayout.setVisibility(View.GONE);
                    ratingMaxLayout.setVisibility(View.GONE);
                    break;
                case "rating":
                    optionsLayout.setVisibility(View.GONE);
                    ratingMinLayout.setVisibility(View.VISIBLE);
                    ratingMaxLayout.setVisibility(View.VISIBLE);
                    break;
                default: // text
                    optionsLayout.setVisibility(View.GONE);
                    ratingMinLayout.setVisibility(View.GONE);
                    ratingMaxLayout.setVisibility(View.GONE);
                    break;
            }
        });

        AlertDialog dialog = builder.setTitle("Add Question")
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String questionText = questionTextEditText.getText().toString().trim();

                if (questionText.isEmpty()) {
                    Toast.makeText(this, "Question text cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                int selectedTypeId = questionTypeRadioGroup.getCheckedRadioButtonId();
                if (selectedTypeId == -1) {
                    Toast.makeText(this, "Please select a question type", Toast.LENGTH_SHORT).show();
                    return;
                }

                RadioButton selectedType = dialogView.findViewById(selectedTypeId);
                String type = selectedType.getText().toString().toLowerCase();
                type = type.replace(" ", "_"); // Convert "multiple choice" to "multiple_choice"

                Question question = new Question("q" + (questionList.size() + 1), type, questionText);

                switch (type) {
                    case "multiple_choice":
                        String optionsText = optionsEditText.getText().toString().trim();
                        if (optionsText.isEmpty()) {
                            Toast.makeText(this, "Please provide options", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        List<String> options = Arrays.asList(optionsText.split(","));
                        question.setOptions(options);
                        break;
                    case "rating":
                        try {
                            int min = Integer.parseInt(ratingMinEditText.getText().toString().trim());
                            int max = Integer.parseInt(ratingMaxEditText.getText().toString().trim());
                            if (min >= max) {
                                Toast.makeText(this, "Max must be greater than min", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            question.setMin(min);
                            question.setMax(max);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Please enter valid min and max values", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        break;
                }

                questionList.add(question);
                questionAdapter.notifyDataSetChanged();
                dialog.dismiss();
            });
        });

        dialog.show();

        // Set focus to the question text edit text
        questionTextEditText.requestFocus();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveForm() {
        // Validate input
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            titleEditText.requestFocus();
            return;
        }

        if (questionList.isEmpty()) {
            Toast.makeText(this, "Please add at least one question", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        saveFormButton.setEnabled(false);

        // Create form
        Form form = new Form(null, title, description, userId);
        form.setCreatedAt(LocalDateTime.now());
        form.setLastModified(LocalDateTime.now());
        form.setActive(true);

        // Add questions
        Map<String, Question> questions = new HashMap<>();
        for (Question question : questionList) {
            questions.put(question.getId(), question);
        }
        form.setQuestions(questions);

        // Save to Firebase
        FirebaseHelper.createForm(form, success -> {
            progressBar.setVisibility(View.GONE);
            saveFormButton.setEnabled(true);

            if (success) {
                Toast.makeText(this, "Form created successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to create form", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}