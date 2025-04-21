package com.example.activityfeedback.utils;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.activityfeedback.models.Form;
import com.example.activityfeedback.models.Question;
import com.example.activityfeedback.models.StudentAnswerStatus;
import com.example.activityfeedback.models.Submission;
import com.example.activityfeedback.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.function.Consumer;

public class FirebaseHelper {

    private static List<User> cachedStudents = new ArrayList<>();

    public static void setCachedStudents(List<User> students) {
        cachedStudents = students;
    }

    public static List<User> getCachedStudents() {
        return cachedStudents;
    }

    private static final String TAG = "FirebaseHelper";
    private static final String DATABASE_URL = "https://activity-feedback-a9397-default-rtdb.firebaseio.com/";

    private static FirebaseDatabase database;
    private static FirebaseAuth auth;

    // Initialize Firebase components
    public static void initialize() {
        if (database == null) {
            database = FirebaseDatabase.getInstance(DATABASE_URL);
//            database.setPersistenceEnabled(true);

        }

        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
    }

    //Interfacces
    public interface PasswordResetCallback {
        void onComplete(boolean isSuccess, Exception exception);
    }

    public static void resetPassword(String email, PasswordResetCallback callback) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onComplete(true, null);
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onComplete(false, e);
                });
    }

    // Authentication methods
    public static void registerUser(String email, String password, String name, String rollNumber, String role,
                                    OnCompleteListener<AuthResult> listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String userId = task.getResult().getUser().getUid();
                        User newUser = new User(userId, email, name, rollNumber, role);
                        database.getReference("users").child(userId).setValue(newUser);
                    }
                    listener.onComplete(task);
                });
    }

    public void saveStudentsToFirebase(List<User> students) {
        DatabaseReference usersRef = database.getReference("users");

        for (User student : students) {
            String userId = student.getUserId();
            usersRef.child(userId).setValue(student)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("FirebaseHelper", "Student data saved: " + student.getEnrollmentId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirebaseHelper", "Error saving student data", e);
                    });
        }
    }

    // Register students in Firebase Authentication and save to Firebase Database
    public static void registerStudents(List<User> students) {
        for (User student : students) {
            String email = student.getEmail();
            String password = "Password123";

            registerUser(email, password, student.getName(), student.getRollNumber(), "student", task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String userId = task.getResult().getUser().getUid();
                    student.setUserId(userId);

                    // Save full user data to Firebase Database
                    saveStudentToFirebase(student);
                } else {
                    Log.e("RegisterStudent", "Failed to register: " + email, task.getException());
                }
            });
        }
    }


    public static void saveStudentToFirebase(User student) {
        DatabaseReference usersRef = database.getReference("users");

        String userId = student.getUserId();  // Assuming this is populated in the User object
        usersRef.child(userId).setValue(student)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Student data saved: " + student.getRollNumber());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving student data", e);
                });
    }



    public static void fetchAllStudents(OnStudentsFetchedListener listener) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> students = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    if (user != null && "student".equalsIgnoreCase(user.getRole())) {
                        students.add(user);
                    }
                }
                listener.onStudentsFetched(students);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onStudentsFetched(null);
            }
        });
    }

    public interface OnStudentsFetchedListener {
        void onStudentsFetched(List<User> students);
    }


    private static void updateStudentListUI() {
    }


    public static void loginUser(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    public static void logoutUser() {
        auth.signOut();
    }

    public static void resetPassword(String email, OnCompleteListener<Void> listener) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(listener);
    }

    public static String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    // User methods
    public static void getCurrentUser(Consumer<User> callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.accept(null);
            return;
        }

        database.getReference("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            user.setUserId(userId);
                        }
                        callback.accept(user);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error getting user", error.toException());
                        callback.accept(null);
                    }
                });
    }

    // Form methods
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createForm(Form form, Consumer<Boolean> callback) {
        DatabaseReference formsRef = database.getReference("forms");
        String formId = form.getFormId() != null ? form.getFormId() : formsRef.push().getKey();
        form.setFormId(formId);

        // Convert LocalDateTime to String for Firebase
        Map<String, Object> formValues = new HashMap<>();
        formValues.put("formId", form.getFormId());
        formValues.put("title", form.getTitle());
        formValues.put("description", form.getDescription());
        formValues.put("createdBy", form.getCreatedBy());
        formValues.put("createdAt", DateTimeConverter.toString(form.getCreatedAt()));
        formValues.put("lastModified", DateTimeConverter.toString(form.getLastModified()));
        formValues.put("isActive", form.isActive());
        formValues.put("questions", form.getQuestions());

        formsRef.child(formId).setValue(formValues)
                .addOnCompleteListener(task -> callback.accept(task.isSuccessful()));
    }

    public static void getAllStudents(OnStudentsLoadedListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> students = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User student = snapshot.getValue(User.class);
                    if (student != null && "student".equalsIgnoreCase(student.getRole())) {
                        students.add(student);
                    }
                }
                listener.onStudentsLoaded(students);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onStudentsLoaded(new ArrayList<>()); // Return empty on error
            }
        });
    }

    public interface OnStudentsLoadedListener {
        void onStudentsLoaded(List<User> students);
    }


    public static void getFormsCreatedBy(String userId, Consumer<List<Form>> callback) {
        database.getReference("forms")
                .orderByChild("createdBy")
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Form> forms = new ArrayList<>();
                        for (DataSnapshot formSnapshot : snapshot.getChildren()) {
                            Form form = parseFormFromSnapshot(formSnapshot);
                            if (form != null) {
                                forms.add(form);
                            }
                        }
                        callback.accept(forms);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error getting forms", error.toException());
                        callback.accept(new ArrayList<>());
                    }
                });
    }

    public static void getActiveForms(Consumer<List<Form>> callback) {
        database.getReference("forms")
                .orderByChild("isActive")
                .equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Form> forms = new ArrayList<>();
                        for (DataSnapshot formSnapshot : snapshot.getChildren()) {
                            Form form = parseFormFromSnapshot(formSnapshot);
                            if (form != null) {
                                forms.add(form);
                            }
                        }
                        callback.accept(forms);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error getting forms", error.toException());
                        callback.accept(new ArrayList<>());
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static Form parseFormFromSnapshot(DataSnapshot snapshot) {
        try {
            Form form = new Form();
            form.setFormId(snapshot.getKey());
            form.setTitle(snapshot.child("title").getValue(String.class));
            form.setDescription(snapshot.child("description").getValue(String.class));
            form.setCreatedBy(snapshot.child("createdBy").getValue(String.class));

            // Parse dates - handle with more error checking
            String createdAtStr = snapshot.child("createdAt").getValue(String.class);
            form.setCreatedAt(DateTimeConverter.toLocalDateTime(createdAtStr));

            String lastModifiedStr = snapshot.child("lastModified").getValue(String.class);
            form.setLastModified(DateTimeConverter.toLocalDateTime(lastModifiedStr));

            // Handle boolean with null check
            Boolean isActive = snapshot.child("isActive").getValue(Boolean.class);
            form.setActive(isActive != null ? isActive : true); // Default to true if null

            // Parse questions with better error handling
            Map<String, Question> questions = new HashMap<>();
            if (snapshot.hasChild("questions")) {
                for (DataSnapshot questionSnapshot : snapshot.child("questions").getChildren()) {
                    try {
                        String questionId = questionSnapshot.getKey();
                        String type = questionSnapshot.child("type").getValue(String.class);
                        String text = questionSnapshot.child("text").getValue(String.class);

                        if (questionId != null && type != null && text != null) {
                            Question question = new Question(questionId, type, text);

                            // Handle different question types
                            if ("multiple_choice".equals(type) && questionSnapshot.hasChild("options")) {
                                List<String> options = new ArrayList<>();
                                for (DataSnapshot optionSnapshot : questionSnapshot.child("options").getChildren()) {
                                    String option = optionSnapshot.getValue(String.class);
                                    if (option != null) {
                                        options.add(option);
                                    }
                                }
                                question.setOptions(options);
                            } else if ("rating".equals(type)) {
                                Integer min = questionSnapshot.child("min").getValue(Integer.class);
                                Integer max = questionSnapshot.child("max").getValue(Integer.class);
                                question.setMin(min != null ? min : 1);
                                question.setMax(max != null ? max : 5);
                            }

                            questions.put(questionId, question);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing question", e);
                        // Continue with other questions
                    }
                }
            }
            form.setQuestions(questions);
            return form;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing form", e);
            return null;
        }
    }

    // Submission methods
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void submitForm(Submission submission, Consumer<Boolean> callback) {
        DatabaseReference submissionsRef = database.getReference("submissions");
        String submissionId = submission.getSubmissionId() != null ?
                submission.getSubmissionId() : submissionsRef.push().getKey();
        submission.setSubmissionId(submissionId);

        // Convert LocalDateTime to String for Firebase
        Map<String, Object> submissionValues = new HashMap<>();
        submissionValues.put("submissionId", submission.getSubmissionId());
        submissionValues.put("formId", submission.getFormId());
        submissionValues.put("studentId", submission.getStudentId());
        submissionValues.put("submittedAt", DateTimeConverter.toString(submission.getSubmittedAt()));
        submissionValues.put("answers", submission.getAnswers());

        submissionsRef.child(submissionId).setValue(submissionValues)
                .addOnCompleteListener(task -> callback.accept(task.isSuccessful()));
    }

    public static void getFormSubmissions(String formId, Consumer<List<Submission>> callback) {
        database.getReference("submissions")
                .orderByChild("formId")
                .equalTo(formId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Submission> submissions = new ArrayList<>();
                        for (DataSnapshot submissionSnapshot : snapshot.getChildren()) {
                            Submission submission = parseSubmissionFromSnapshot(submissionSnapshot);
                            if (submission != null) {
                                submissions.add(submission);
                            }
                        }
                        callback.accept(submissions);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error getting submissions", error.toException());
                        callback.accept(new ArrayList<>());
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static Submission parseSubmissionFromSnapshot(DataSnapshot snapshot) {
        try {
            Submission submission = new Submission();
            submission.setSubmissionId(snapshot.getKey());
            submission.setFormId(snapshot.child("formId").getValue(String.class));
            submission.setStudentId(snapshot.child("studentId").getValue(String.class));

            // Parse date
            String submittedAtStr = snapshot.child("submittedAt").getValue(String.class);
            submission.setSubmittedAt(DateTimeConverter.toLocalDateTime(submittedAtStr));

            // Parse answers
            Map<String, Object> answers = new HashMap<>();
            if (snapshot.hasChild("answers")) {
                for (DataSnapshot answerSnapshot : snapshot.child("answers").getChildren()) {
                    answers.put(answerSnapshot.getKey(), answerSnapshot.getValue());
                }
            }
            submission.setAnswers(answers);
            return submission;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing submission", e);
            return null;
        }
    }

    public static void hasStudentSubmittedForm(String formId, String studentId, Consumer<Boolean> callback) {
        database.getReference("submissions")
                .orderByChild("formId")
                .equalTo(formId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasSubmitted = false;
                        for (DataSnapshot submissionSnapshot : snapshot.getChildren()) {
                            String submissionStudentId = submissionSnapshot.child("studentId").getValue(String.class);
                            if (studentId.equals(submissionStudentId)) {
                                hasSubmitted = true;
                                break;
                            }
                        }
                        callback.accept(hasSubmitted);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking submission status", error.toException());
                        callback.accept(false);
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void updateFormStatus(String formId, boolean isActive, LocalDateTime lastModified, Consumer<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isActive", isActive);
        updates.put("lastModified", DateTimeConverter.toString(lastModified));

        database.getReference("forms").child(formId)
                .updateChildren(updates)
                .addOnCompleteListener(task -> callback.accept(task.isSuccessful()));
    }

    public static void deleteForm(String formId, Consumer<Boolean> callback) {
        // First, delete all submissions for this form
        database.getReference("submissions")
                .orderByChild("formId")
                .equalTo(formId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Batch delete all submissions
                        Map<String, Object> submissionUpdates = new HashMap<>();
                        for (DataSnapshot submissionSnapshot : snapshot.getChildren()) {
                            submissionUpdates.put(submissionSnapshot.getKey(), null);
                        }

                        if (!submissionUpdates.isEmpty()) {
                            database.getReference("submissions").updateChildren(submissionUpdates);
                        }

                        // Now delete the form itself
                        database.getReference("forms").child(formId).removeValue()
                                .addOnCompleteListener(task -> callback.accept(task.isSuccessful()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error deleting submissions", error.toException());
                        callback.accept(false);
                    }
                });
    }


    /**
     * Get a specific student's submission for a form
     */
    public static void getStudentSubmission(String formId, String userId, SubmissionCallback callback) {
        database.getReference("submissions")
                .orderByChild("formId")
                .equalTo(formId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Submission foundSubmission = null;
                        for (DataSnapshot submissionSnapshot : snapshot.getChildren()) {
                            String submissionUserId = submissionSnapshot.child("studentId").getValue(String.class);
                            if (userId.equals(submissionUserId)) {
                                foundSubmission = parseSubmissionFromSnapshot(submissionSnapshot);
                                break;
                            }
                        }
                        callback.onCallback(foundSubmission);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error getting student submission", error.toException());
                        callback.onCallback(null);
                    }
                });
    }

    /**
     * Get all questions for a form
     */
    public static void getFormQuestions(String formId, QuestionsCallback callback) {
        database.getReference("forms").child(formId).child("questions")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Question> questions = new ArrayList<>();
                        for (DataSnapshot questionSnapshot : snapshot.getChildren()) {
                            try {
                                String questionId = questionSnapshot.getKey();
                                String type = questionSnapshot.child("type").getValue(String.class);
                                String text = questionSnapshot.child("text").getValue(String.class);
                                Integer order = questionSnapshot.child("order").getValue(Integer.class);

                                if (questionId != null && type != null && text != null) {
                                    Question question = new Question(questionId, type, text);

                                    // Set question order if available
                                    if (order != null) {
                                        question.setOrder(order);
                                    }

                                    // Handle different question types
                                    if ("multiple_choice".equals(type) && questionSnapshot.hasChild("options")) {
                                        List<String> options = new ArrayList<>();
                                        for (DataSnapshot optionSnapshot : questionSnapshot.child("options").getChildren()) {
                                            String option = optionSnapshot.getValue(String.class);
                                            if (option != null) {
                                                options.add(option);
                                            }
                                        }
                                        question.setOptions(options);
                                    } else if ("rating".equals(type)) {
                                        Integer min = questionSnapshot.child("min").getValue(Integer.class);
                                        Integer max = questionSnapshot.child("max").getValue(Integer.class);
                                        question.setMin(min != null ? min : 1);
                                        question.setMax(max != null ? max : 5);
                                    }

                                    questions.add(question);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing question", e);
                                // Continue with other questions
                            }
                        }

                        // Sort questions by order if available
                        questions.sort((q1, q2) -> {
                            Integer order1 = q1.getOrder();
                            Integer order2 = q2.getOrder();
                            if (order1 == null) order1 = 0;
                            if (order2 == null) order2 = 0;
                            return order1.compareTo(order2);
                        });

                        callback.onCallback(questions);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error getting form questions", error.toException());
                        callback.onCallback(new ArrayList<>());
                    }
                });
    }

    public interface SubmissionCallback {
        void onCallback(Submission submission);
    }

    public interface QuestionsCallback {
        void onCallback(List<Question> questions);
    }

    public static void getStudentsWithAnswerStatus(String formId, final OnStudentAnswersFetchListener listener) {
        DatabaseReference usersRef = database.getReference("users");
        DatabaseReference submissionsRef = database.getReference("submissions").child(formId);

        usersRef.orderByChild("role").equalTo("student").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<StudentAnswerStatus> studentStatusList = new ArrayList<>();
                long totalStudents = dataSnapshot.getChildrenCount();
                int[] checkedCount = {0};

                if (totalStudents == 0) {
                    listener.onStudentAnswersFetch(studentStatusList);
                    return;
                }

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    User student = userSnapshot.getValue(User.class);

                    if (student == null || userId == null) {
                        checkedCount[0]++;
                        if (checkedCount[0] == totalStudents) {
                            listener.onStudentAnswersFetch(studentStatusList);
                        }
                        continue;
                    }

                    checkIfAnsweredForm(userId, submissionsRef, isAnswered -> {
                        studentStatusList.add(new StudentAnswerStatus(student, isAnswered));

                        checkedCount[0]++;
                        if (checkedCount[0] == totalStudents) {
                            listener.onStudentAnswersFetch(studentStatusList);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onError(databaseError.toException());
            }
        });
    }

    // ✅ SIMPLER check for whether a user submitted
    private static void checkIfAnsweredForm(String userId, DatabaseReference submissionsRef, final OnAnsweredListener listener) {
        submissionsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listener.onAnswered(dataSnapshot.exists()); // only check if submission exists
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onAnswered(false);
            }
        });
    }

    // Interface for the callback to return fetched students

    public interface OnStudentAnswersFetchListener {
        void onStudentAnswersFetch(List<StudentAnswerStatus> studentStatuses);
        void onError(Exception e);
    }



    // Interface for checking if a student has answered the form
    public interface OnAnsweredListener {
        void onAnswered(boolean isAnswered);
    }

    }