package com.example.activityfeedback.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.example.activityfeedback.R;
import com.example.activityfeedback.adapters.FormListAdapter;
import com.example.activityfeedback.adapters.StudentAdapter;
import com.example.activityfeedback.models.Form;
import com.example.activityfeedback.models.User;
import com.example.activityfeedback.utils.FirebaseHelper;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.UUID;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.SearchView;


public class ProfessorDashboardActivity extends AppCompatActivity {

    private Button btnUploadExcel;
    private SearchView searchView;

    private RecyclerView recyclerView;
    private StudentAdapter adapter;
    private List<User> studentList = new ArrayList<>();
    private ActivityResultLauncher<Intent> excelFilePickerLauncher;

    private String userId;
    private String userName;
    private RecyclerView formsRecyclerView;
    private FormListAdapter formListAdapter;
    private List<Form> formList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyTextView;
    private ExtendedFloatingActionButton addFormFab;

    private TextView userNameTextView;
    private TextView currentTimeTextView;
    private Handler timeUpdateHandler;
    private Runnable timeUpdateRunnable;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_dashboard);


        userId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");
        setTitle("Professor Dashboard");

        formsRecyclerView = findViewById(R.id.forms_recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        emptyTextView = findViewById(R.id.empty_text_view);
        addFormFab = findViewById(R.id.add_form_fab);
        userNameTextView = findViewById(R.id.user_name_text_view);
        //currentTimeTextView = findViewById(R.id.current_time_text_view);

        userNameTextView.setText("Current User: " + userName.toUpperCase());

        searchView = findViewById(R.id.search_view);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterForms(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterForms(newText);
                return true;
            }
        });


        timeUpdateHandler = new Handler(Looper.getMainLooper());
        timeUpdateRunnable = () -> {
            updateCurrentTime();
            timeUpdateHandler.postDelayed(timeUpdateRunnable, 1000);
        };

        formList = new ArrayList<>();
        formListAdapter = new FormListAdapter(this, formList, form -> {
            Intent intent = new Intent(this, FormResultsActivity.class);
            intent.putExtra("formId", form.getFormId());
            intent.putExtra("formTitle", form.getTitle());
            startActivity(intent);
        }, true);

        formsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        formsRecyclerView.setAdapter(formListAdapter);
        swipeRefreshLayout.setOnRefreshListener(this::loadForms);

        addFormFab.setOnClickListener(view -> {
            Intent intent = new Intent(this, FormCreationActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        loadForms();

        btnUploadExcel = findViewById(R.id.btnUploadExcel);
        recyclerView = findViewById(R.id.recyclerViewStudents);
        adapter = new StudentAdapter(studentList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        Button btnViewStudents = findViewById(R.id.btnViewStudents);
        btnViewStudents.setOnClickListener(v -> {
            Intent intent = new Intent(ProfessorDashboardActivity.this, StudentListActivity.class);
            startActivity(intent);


        });


        // Register the Activity Result Launcher
        excelFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        int flags = result.getData().getFlags();
                        int takeFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        try {
                            getContentResolver().takePersistableUriPermission(fileUri, takeFlags);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Permission denied for the file", Toast.LENGTH_SHORT).show();
                        }
                        parseExcelAndRegister(fileUri);
                    } else {
                        // Fallback: Add test students manually if no file picked
                        addTestStudents();
                    }
                });

        btnUploadExcel.setOnClickListener(v -> pickExcelFile());
        addTestStudents();
    }



    private void pickExcelFile() {
        // Check if the app has permission to read external storage (for Android < 13)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // If not, request permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                // If permission already granted, open file picker
                openFilePicker();
            }
        } else {
            // For Android 13 (API level 33) and above, use the new permission model
            openFilePicker();
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open file picker
                openFilePicker();
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Function to open the file picker after permission is granted
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        excelFilePickerLauncher.launch(intent);
    }


    public void parseExcelAndRegister(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            List<User> studentList = new ArrayList<>();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header

                String name = row.getCell(0).getStringCellValue();
                String roll = row.getCell(1).getStringCellValue();
                String email = row.getCell(2).getStringCellValue();
                String phone = row.getCell(3).getStringCellValue();
                String department = row.getCell(4).getStringCellValue();
                int year = (int) row.getCell(5).getNumericCellValue();

                // Temporary ID for now (real one set during registration)
                String tempUserId = UUID.randomUUID().toString();

                User student = new User();
                student.setUserId(tempUserId);
                student.setName(name);
                student.setRollNumber(roll);
                student.setEmail(email);
                student.setPhone(phone);
                student.setDepartment(department);
                student.setYear(year);
                student.setRole("student");

                studentList.add(student);
            }

            workbook.close();

            // 🔐 Register in FirebaseAuth and save to Database
            FirebaseHelper.registerStudents(studentList);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing Excel: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    private void addTestStudents() {
        studentList.clear();
        studentList.add(new User(null, "test1@example.com", "Test Student 1", "ENR001", "student"));
        studentList.add(new User(null, "test2@example.com", "Test Student 2", "ENR002", "student"));
        studentList.add(new User(null, "test3@example.com", "Test Student 3", "ENR003", "student"));
        adapter.notifyDataSetChanged();
        FirebaseHelper.registerStudents(studentList);
    }

    private void updateCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentTime = sdf.format(new Date());
        //currentTimeTextView.setText(currentTime);
    }

    private void loadForms() {
        swipeRefreshLayout.setRefreshing(true);

        FirebaseHelper.getFormsCreatedBy(userId, forms -> {
            formList.clear();
            formList.addAll(forms);
            formListAdapter.notifyDataSetChanged();

            swipeRefreshLayout.setRefreshing(false);

            if (formList.isEmpty()) {
                emptyTextView.setVisibility(View.VISIBLE);
                formsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyTextView.setVisibility(View.GONE);
                formsRecyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void filterForms(String query) {
        List<Form> filteredList = new ArrayList<>();

        for (Form form : formList) {
            if (form.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(form);
            }
        }

        formListAdapter.updateList(filteredList);
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
        } else if (id == R.id.action_view_students) {
            Intent intent = new Intent(this, StudentListActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadForms();
        updateCurrentTime();
        timeUpdateHandler.postDelayed(timeUpdateRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable);
    }
}
