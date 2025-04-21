package com.example.activityfeedback.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.activityfeedback.R;
import com.example.activityfeedback.adapters.StudentAdapter;
import com.example.activityfeedback.models.User;
import com.example.activityfeedback.utils.FirebaseHelper;

import java.util.List;

public class StudentListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private StudentAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        recyclerView = findViewById(R.id.recyclerViewStudentList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FirebaseHelper.initialize(); // Ensure Firebase is initialized

        adapter = new StudentAdapter(); // Empty constructor
        recyclerView.setAdapter(adapter);

        // Initially load students
        fetchStudents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload students every time this screen resumes
        fetchStudents();
    }

    private void fetchStudents() {
        FirebaseHelper.fetchAllStudents(students -> {
            if (students != null) {
                adapter.setStudentList(students); // Your adapter must have this method
            } else {
                Log.e("StudentListActivity", "No students found or error occurred");
            }
        });
    }
}
