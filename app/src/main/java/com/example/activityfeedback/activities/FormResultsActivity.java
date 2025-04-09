package com.example.activityfeedback.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.activityfeedback.R;
import com.example.activityfeedback.adapters.ResultsAdapter;
import com.example.activityfeedback.models.Submission;
import com.example.activityfeedback.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class FormResultsActivity extends AppCompatActivity {

    private String formId;
    private String formTitle;
    private RecyclerView submissionsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private ResultsAdapter resultsAdapter;
    private List<Submission> submissionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_results);

        // Get data from intent
        formId = getIntent().getStringExtra("formId");
        formTitle = getIntent().getStringExtra("formTitle");

        // Set up ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Results: " + formTitle);
        }

        // Initialize views
        submissionsRecyclerView = findViewById(R.id.submissions_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyTextView = findViewById(R.id.empty_text_view);

        // Setup RecyclerView
        submissionList = new ArrayList<>();
        resultsAdapter = new ResultsAdapter(this, submissionList);
        submissionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        submissionsRecyclerView.setAdapter(resultsAdapter);

        // Load submissions
        loadSubmissions();
    }

    private void loadSubmissions() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getFormSubmissions(formId, submissions -> {
            submissionList.clear();
            submissionList.addAll(submissions);
            resultsAdapter.notifyDataSetChanged();

            progressBar.setVisibility(View.GONE);

            // Show empty view if no submissions
            if (submissionList.isEmpty()) {
                emptyTextView.setVisibility(View.VISIBLE);
                submissionsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyTextView.setVisibility(View.GONE);
                submissionsRecyclerView.setVisibility(View.VISIBLE);
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