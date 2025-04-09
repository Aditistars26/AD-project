package com.example.activityfeedback.adapters;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.activityfeedback.R;
import com.example.activityfeedback.models.Submission;
import com.example.activityfeedback.utils.DateTimeConverter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ResultViewHolder> {

    private final Context context;
    private final List<Submission> submissions;

    public ResultsAdapter(Context context, List<Submission> submissions) {
        this.context = context;
        this.submissions = submissions;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_result, parent, false);
        return new ResultViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        Submission submission = submissions.get(position);

        // Get student name
        FirebaseDatabase.getInstance().getReference("users")
                .child(submission.getStudentId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String studentName = snapshot.child("name").getValue(String.class);
                            holder.studentNameTextView.setText(studentName);
                        } else {
                            holder.studentNameTextView.setText("Unknown Student");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.studentNameTextView.setText("Unknown Student");
                    }
                });

        holder.submittedAtTextView.setText("Submitted: " + DateTimeConverter.formatForDisplay(submission.getSubmittedAt()));

        // Build answers text
        StringBuilder answersText = new StringBuilder();
        Map<String, Object> answers = submission.getAnswers();

        if (answers != null && !answers.isEmpty()) {
            // Get the form to get question texts
            FirebaseDatabase.getInstance().getReference("forms")
                    .child(submission.getFormId())
                    .child("questions")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            StringBuilder sb = new StringBuilder();

                            for (Map.Entry<String, Object> entry : answers.entrySet()) {
                                String questionId = entry.getKey();
                                Object answer = entry.getValue();

                                // Get question text
                                if (snapshot.hasChild(questionId)) {
                                    String questionText = snapshot.child(questionId).child("text").getValue(String.class);
                                    sb.append("• ").append(questionText).append("\n   Answer: ").append(answer).append("\n\n");
                                } else {
                                    sb.append("• Question ").append(questionId).append("\n   Answer: ").append(answer).append("\n\n");
                                }
                            }

                            holder.answersTextView.setText(sb.toString().trim());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Fallback display if we can't get question texts
                            StringBuilder sb = new StringBuilder();
                            for (Map.Entry<String, Object> entry : answers.entrySet()) {
                                sb.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                            }
                            holder.answersTextView.setText(sb.toString().trim());
                        }
                    });
        } else {
            holder.answersTextView.setText("No answers submitted");
        }
    }

    @Override
    public int getItemCount() {
        return submissions.size();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameTextView;
        TextView submittedAtTextView;
        TextView answersTextView;

        ResultViewHolder(View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.student_name_text_view);
            submittedAtTextView = itemView.findViewById(R.id.submitted_at_text_view);
            answersTextView = itemView.findViewById(R.id.answers_text_view);
        }
    }
}