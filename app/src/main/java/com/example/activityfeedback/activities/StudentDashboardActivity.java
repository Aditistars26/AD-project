package com.example.activityfeedback.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.activityfeedback.R;
import com.example.activityfeedback.adapters.FormListAdapter;
import com.example.activityfeedback.models.Form;
import com.example.activityfeedback.utils.FirebaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class StudentDashboardActivity extends AppCompatActivity {

    private String userId;
    private String userName;
    private RecyclerView formsRecyclerView;
    private FormListAdapter formListAdapter;
    private List<Form> formList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyTextView;
    private TextView userNameTextView;
    private TextView currentTimeTextView;
    private Handler timeUpdateHandler;
    private Runnable timeUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // Get user data from intent
        userId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");

        // Set title
        setTitle("Student Dashboard");

        // Initialize views
        formsRecyclerView = findViewById(R.id.forms_recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        emptyTextView = findViewById(R.id.empty_text_view);
        userNameTextView = findViewById(R.id.user_name_text_view);
        currentTimeTextView = findViewById(R.id.current_time_text_view);

        // Set user name
        userNameTextView.setText("Current User: " + userName.toUpperCase());

        // Initialize time update handler
        timeUpdateHandler = new Handler(Looper.getMainLooper());
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentTime();
                timeUpdateHandler.postDelayed(this, 1000); // Update every second
            }
        };

        // Setup RecyclerView
        formList = new ArrayList<>();
        formListAdapter = new FormListAdapter(this, formList, form -> {
            // Check if student has already submitted this form
            FirebaseHelper.hasStudentSubmittedForm(form.getFormId(), userId, hasSubmitted -> {
                if (hasSubmitted) {
//                    Toast.makeText(StudentDashboardActivity.this,
//                            "You have already submitted feedback for this form",
//                            Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ViewSubmissionActivity.class);
                    intent.putExtra("formId", form.getFormId());
                    intent.putExtra("userId", userId);
                    intent.putExtra("formTitle", form.getTitle());
                    startActivity(intent);
                } else {
                    // Student hasn't submitted yet, allow submission
                    Intent intent = new Intent(this, FormSubmissionActivity.class);
                    intent.putExtra("formId", form.getFormId());
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                }
            });
        }, false);

        formsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        formsRecyclerView.setAdapter(formListAdapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadForms);

        // Load forms
        loadForms();
    }

    private void updateCurrentTime() {
        // Format current time as UTC in YYYY-MM-DD HH:MM:SS format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentTime = sdf.format(new Date());
        currentTimeTextView.setText(currentTime);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadForms();

        // Start time updates
        updateCurrentTime();
        timeUpdateHandler.postDelayed(timeUpdateRunnable, 1000);
    }

    private void loadForms() {
        swipeRefreshLayout.setRefreshing(true);

        FirebaseHelper.getActiveForms(forms -> {
            formList.clear();
            formList.addAll(forms);
            formListAdapter.notifyDataSetChanged();

            swipeRefreshLayout.setRefreshing(false);

            // Show empty view if no forms
            if (formList.isEmpty()) {
                emptyTextView.setVisibility(View.VISIBLE);
                formsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyTextView.setVisibility(View.GONE);
                formsRecyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            FirebaseHelper.logoutUser();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop time updates when activity is paused
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable);
    }
}