package com.example.activityfeedback.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.activityfeedback.R;
import com.example.activityfeedback.models.Form;
import com.example.activityfeedback.models.Question;
import com.example.activityfeedback.models.Submission;
import com.example.activityfeedback.utils.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormSubmissionActivity extends AppCompatActivity {

    private String formId;
    private String userId;
    private LinearLayout questionsContainer;
    private Button submitButton;
    private ProgressBar progressBar;
    private TextView formTitleTextView, formDescriptionTextView;
    private Form form;
    private Map<String, View> questionViews;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_submission);

        // Get data from intent
        formId = getIntent().getStringExtra("formId");
        userId = getIntent().getStringExtra("userId");

        // Set up ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Submit Feedback");
        }

        // Initialize views
        questionsContainer = findViewById(R.id.questions_container);
        submitButton = findViewById(R.id.submit_button);
        progressBar = findViewById(R.id.progress_bar);
        formTitleTextView = findViewById(R.id.form_title_text_view);
        formDescriptionTextView = findViewById(R.id.form_description_text_view);

        setupTouchInterceptor();

        questionViews = new HashMap<>();

        // Load form details
        loadForm();

        // Setup submit button
        submitButton.setOnClickListener(v -> submitForm());
    }

    private void setupTouchInterceptor() {
        View rootView = findViewById(android.R.id.content);
        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View currentFocus = getCurrentFocus();
                if (currentFocus instanceof EditText) {
                    // Get the location of the touch event
                    Rect outRect = new Rect();
                    currentFocus.getGlobalVisibleRect(outRect);

                    // If the touch is outside the EditText bounds
                    if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                        // Clear focus and hide keyboard
                        currentFocus.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    }
                }
            }

            return false;
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void loadForm() {
        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        DatabaseReference formRef = FirebaseDatabase.getInstance().getReference("forms").child(formId);
        formRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Parse form data
                    String title = dataSnapshot.child("title").getValue(String.class);
                    String description = dataSnapshot.child("description").getValue(String.class);

                    formTitleTextView.setText(title);
                    formDescriptionTextView.setText(description);

                    // Create form object
                    form = new Form(formId, title, description, null);

                    // Parse questions
                    if (dataSnapshot.hasChild("questions")) {
                        DataSnapshot questionsSnapshot = dataSnapshot.child("questions");
                        for (DataSnapshot questionSnapshot : questionsSnapshot.getChildren()) {
                            String questionId = questionSnapshot.getKey();
                            String type = questionSnapshot.child("type").getValue(String.class);
                            String text = questionSnapshot.child("text").getValue(String.class);

                            Question question = new Question(questionId, type, text);

                            if ("multiple_choice".equals(type)) {
                                if (questionSnapshot.hasChild("options")) {
                                    // Create a new ArrayList to store options
                                    List<String> optionsList = new ArrayList<>();

                                    // Get options from Firebase
                                    Iterable<DataSnapshot> optionSnapshots = questionSnapshot.child("options").getChildren();
                                    for (DataSnapshot optionSnapshot : optionSnapshots) {
                                        String option = optionSnapshot.getValue(String.class);
                                        if (option != null) {
                                            optionsList.add(option);
                                        }
                                    }

                                    // Set the options list
                                    question.setOptions(optionsList);
                                }
                            } else if ("rating".equals(type)) {
                                if (questionSnapshot.hasChild("min")) {
                                    question.setMin(questionSnapshot.child("min").getValue(Integer.class));
                                }
                                if (questionSnapshot.hasChild("max")) {
                                    question.setMax(questionSnapshot.child("max").getValue(Integer.class));
                                }
                            }

                            // Add question to form
                            form.addQuestion(questionId, question);

                            // Add question view to container
                            addQuestionView(question);
                        }
                    }

                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                } else {
                    Toast.makeText(FormSubmissionActivity.this, "Form not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(FormSubmissionActivity.this, "Error loading form", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void addQuestionView(Question question) {
        View questionView;
        String questionType = question.getType();

        switch (questionType) {
            case "multiple_choice":
                questionView = getLayoutInflater().inflate(R.layout.item_question, questionsContainer, false);
                TextView multipleChoiceQuestionText = questionView.findViewById(R.id.question_text);
                RadioGroup optionsRadioGroup = questionView.findViewById(R.id.options_radio_group);

                multipleChoiceQuestionText.setText(question.getText());
                optionsRadioGroup.setVisibility(View.VISIBLE);

                // Add radio buttons for options
                if (question.getOptions() != null) {
                    for (String option : question.getOptions()) {
                        RadioButton radioButton = new RadioButton(this);
                        radioButton.setText(option);
                        optionsRadioGroup.addView(radioButton);
                    }
                }
                break;

            case "rating":
                questionView = getLayoutInflater().inflate(R.layout.item_question, questionsContainer, false);
                TextView ratingQuestionText = questionView.findViewById(R.id.question_text);
                LinearLayout ratingLayout = (LinearLayout) questionView.findViewById(R.id.rating_seek_bar).getParent();
                SeekBar ratingSeekBar = questionView.findViewById(R.id.rating_seek_bar);
                TextView ratingValueTextView = questionView.findViewById(R.id.rating_value);

                ratingQuestionText.setText(question.getText());
                // Make the parent layout visible first
                ratingLayout.setVisibility(View.VISIBLE);
                ratingSeekBar.setVisibility(View.VISIBLE);
                ratingValueTextView.setVisibility(View.VISIBLE);

                int min = question.getMin();
                int max = question.getMax();
                ratingSeekBar.setMax(max - min);
                ratingSeekBar.setProgress(0);
                ratingValueTextView.setText(String.valueOf(min));

                ratingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int value = min + progress;
                        ratingValueTextView.setText(String.valueOf(value));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
                break;

            default: // text
                questionView = getLayoutInflater().inflate(R.layout.item_question, questionsContainer, false);
                TextView textQuestionText = questionView.findViewById(R.id.question_text);
                EditText answerEditText = questionView.findViewById(R.id.answer_edit_text);

                textQuestionText.setText(question.getText());
                answerEditText.setVisibility(View.VISIBLE);

                // Configure EditText behavior
                answerEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                answerEditText.setSingleLine(false);
                answerEditText.setMaxLines(5);

                // Set OnClickListener to explicitly show keyboard when the EditText is tapped
                answerEditText.setOnClickListener(v -> {
                    answerEditText.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(answerEditText, InputMethodManager.SHOW_IMPLICIT);
                });

                // Handle Done button
                answerEditText.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // Hide keyboard but don't clear focus yet
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        return true;
                    }
                    return false;
                });

                // Only hide keyboard when focus changes, don't prevent focus
                answerEditText.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                });

                textQuestionText.setText(question.getText());
                answerEditText.setVisibility(View.VISIBLE);
                break;
        }

        questionViews.put(question.getId(), questionView);
        questionsContainer.addView(questionView);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void submitForm() {
        // Validate answers
        Map<String, Object> answers = new HashMap<>();
        boolean isValid = true;

        for (Map.Entry<String, Question> entry : form.getQuestions().entrySet()) {
            String questionId = entry.getKey();
            Question question = entry.getValue();
            View questionView = questionViews.get(questionId);

            if (questionView == null) continue;

            switch (question.getType()) {
                case "multiple_choice":
                    RadioGroup optionsRadioGroup = questionView.findViewById(R.id.options_radio_group);
                    int selectedId = optionsRadioGroup.getCheckedRadioButtonId();

                    if (selectedId == -1) {
                        Toast.makeText(this, "Please answer all questions", Toast.LENGTH_SHORT).show();
                        isValid = false;
                        break;
                    }

                    RadioButton selectedOption = questionView.findViewById(selectedId);
                    answers.put(questionId, selectedOption.getText().toString());
                    break;

                case "rating":
                    SeekBar ratingSeekBar = questionView.findViewById(R.id.rating_seek_bar);
                    int rating = question.getMin() + ratingSeekBar.getProgress();
                    answers.put(questionId, rating);
                    break;

                default: // text
                    EditText answerEditText = questionView.findViewById(R.id.answer_edit_text);
                    answerEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    answerEditText.setOnEditorActionListener((v, actionId, event) -> {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            answerEditText.clearFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(answerEditText.getWindowToken(), 0);
                            return true;
                        }
                        return false;
                    });
                    String answer = answerEditText.getText().toString().trim();

                    if (answer.isEmpty()) {
                        answerEditText.setError("Please provide an answer");
                        isValid = false;
                        break;
                    }

                    answers.put(questionId, answer);
                    break;
            }

            if (!isValid) break;
        }

        if (!isValid) return;

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        // Create submission
        Submission submission = new Submission(null, formId, userId);
        submission.setAnswers(answers);
        submission.setSubmittedAt(LocalDateTime.now());

        // Save to Firebase
        FirebaseHelper.submitForm(submission, success -> {
            progressBar.setVisibility(View.GONE);
            submitButton.setEnabled(true);

            if (success) {
                Toast.makeText(this, "Form submitted successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to submit form", Toast.LENGTH_SHORT).show();
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