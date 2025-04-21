package com.example.activityfeedback.activities;

import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.activityfeedback.R;
import com.example.activityfeedback.adapters.ResultsAdapter;
import com.example.activityfeedback.models.StudentAnswerStatus;
import com.example.activityfeedback.models.Submission;
import com.example.activityfeedback.models.User;
import com.example.activityfeedback.utils.FirebaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import java.io.OutputStream;


public class FormResultsActivity extends AppCompatActivity {

    private String formId;
    private String formTitle;
    private RecyclerView submissionsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private ResultsAdapter resultsAdapter;
    private List<Submission> submissionList;
    private TextView textAnswered, textNotAnswered;
    private int totalStudents = 0;
    private Button downloadCsvButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_results);

        // Get data from intent
        formId = getIntent().getStringExtra("formId");
        formTitle = getIntent().getStringExtra("formTitle");
        textAnswered = findViewById(R.id.text_answered);
        textNotAnswered = findViewById(R.id.text_not_answered);


        // Set up ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Results: " + formTitle);
        }

        // Initialize views
        submissionsRecyclerView = findViewById(R.id.submissions_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyTextView = findViewById(R.id.empty_text_view);
        downloadCsvButton = findViewById(R.id.btn_download_unanswered);

        // Setup RecyclerView
        submissionList = new ArrayList<>();
        resultsAdapter = new ResultsAdapter(this, submissionList);
        submissionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        submissionsRecyclerView.setAdapter(resultsAdapter);

        // Set up the download CSV button
        downloadCsvButton.setOnClickListener(v -> downloadCsv());

        // Load submissions
        loadSubmissions();
    }

    private void loadSubmissions() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getFormSubmissions(formId, submissions -> {
            submissionList.clear();
            submissionList.addAll(submissions);
            resultsAdapter.notifyDataSetChanged();

            // Get all students next
            FirebaseHelper.getAllStudents(students -> {
                totalStudents = students.size();
                int answeredCount = submissionList.size();
                int notAnsweredCount = totalStudents - answeredCount;

                textAnswered.setText("Answered: " + answeredCount);
                textNotAnswered.setText("Not Answered: " + notAnsweredCount);

                progressBar.setVisibility(View.GONE);

                if (submissionList.isEmpty()) {
                    emptyTextView.setVisibility(View.VISIBLE);
                    submissionsRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyTextView.setVisibility(View.GONE);
                    submissionsRecyclerView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void downloadCsv() {
        FirebaseHelper.getStudentsWithAnswerStatus(formId, new FirebaseHelper.OnStudentAnswersFetchListener() {
            @Override
            public void onStudentAnswersFetch(List<StudentAnswerStatus> studentStatuses) {
                if (studentStatuses != null && !studentStatuses.isEmpty()) {
                    String csvContent = generateCsvContent(studentStatuses);
                    saveCsvToFile(csvContent);
                } else {
                    Toast.makeText(FormResultsActivity.this, "No students found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(FormResultsActivity.this, "Failed to fetch students", Toast.LENGTH_SHORT).show();
            }
        });
    }




    private String generateCsvContent(List<StudentAnswerStatus> unansweredStudents) {
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Name,Email,Status\n");  // Header row

        // Loop through the students and append their data to the CSV string
        for (StudentAnswerStatus student : unansweredStudents) {
            csvContent.append(student.getName())
                    .append(",")
                    .append(student.getEmail())
                    .append(",Not Answered\n");
        }

        return csvContent.toString();
    }
    private void saveCsvToFile(String csvContent) {
        String fileName = "unanswered_students.csv";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                    outputStream.write(csvContent.getBytes());
                    outputStream.flush();
                    Toast.makeText(this, "CSV saved to Downloads folder", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to save CSV", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // For older Android versions (API 28 and below)
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter writer = new OutputStreamWriter(fos)) {
                writer.write(csvContent);
                writer.flush();
                Toast.makeText(this, "CSV saved to Downloads", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving CSV", Toast.LENGTH_SHORT).show();
            }
        }
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
