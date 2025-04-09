//package com.example.activityfeedback.activities;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import android.os.Bundle;
//
//import com.example.activityfeedback.R;
//
//public class ViewSubmissionActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_view_submission);
//    }
//}

package com.example.activityfeedback.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.activityfeedback.R;
import com.example.activityfeedback.adapters.SubmissionAnswerAdapter;
import com.example.activityfeedback.models.Question;
import com.example.activityfeedback.models.Submission;
import com.example.activityfeedback.utils.FirebaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewSubmissionActivity extends AppCompatActivity {

    private String formId;
    private String userId;
    private String formTitle;

    private TextView titleTextView;
    private TextView submittedDateTextView;
    private TextView statusTextView;
    private RecyclerView answersRecyclerView;
    private ProgressBar loadingProgressBar;
    private Button closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_submission);

        // Get data from intent
        formId = getIntent().getStringExtra("formId");
        userId = getIntent().getStringExtra("userId");
        formTitle = getIntent().getStringExtra("formTitle");

        // Initialize views
        titleTextView = findViewById(R.id.submission_title_text_view);
        submittedDateTextView = findViewById(R.id.submission_date_text_view);
        statusTextView = findViewById(R.id.submission_status_text_view);
        answersRecyclerView = findViewById(R.id.submission_answers_recycler_view);
        loadingProgressBar = findViewById(R.id.loading_progress_bar);
        closeButton = findViewById(R.id.close_button);

        // Set title
        setTitle("View Submission");
        titleTextView.setText(formTitle);
        statusTextView.setText("Status: Already Submitted");

        // Setup RecyclerView
        answersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup close button
        closeButton.setOnClickListener(v -> finish());

        // Load submission data
        loadSubmissionData();
    }

    private void loadSubmissionData() {
        loadingProgressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getStudentSubmission(formId, userId, submission -> {
            if (submission != null) {
                displaySubmission(submission);
            }
            loadingProgressBar.setVisibility(View.GONE);
        });
    }

    private void displaySubmission(Submission submission) {
        // Format and display submission date
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
//        String submittedDate = sdf.format(new Date(String.valueOf(submission.getSubmittedAt())));
//        submittedDateTextView.setText("Submitted on: " + submittedDate);

        // Load questions to match with answers
        FirebaseHelper.getFormQuestions(formId, questions -> {
            // Create adapter with questions and answers
            SubmissionAnswerAdapter adapter = new SubmissionAnswerAdapter(
                    this, questions, submission.getAnswers());
            answersRecyclerView.setAdapter(adapter);
        });
    }


}