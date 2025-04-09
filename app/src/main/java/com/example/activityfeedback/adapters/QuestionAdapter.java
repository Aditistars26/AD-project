package com.example.activityfeedback.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.activityfeedback.R;
import com.example.activityfeedback.models.Question;

import java.util.List;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder> {

    private final Context context;
    private final List<Question> questions;

    public QuestionAdapter(Context context, List<Question> questions) {
        this.context = context;
        this.questions = questions;
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question question = questions.get(position);
        holder.questionTextView.setText((position + 1) + ". " + question.getText());

        String typeDescription = "";
        switch (question.getType()) {
            case "multiple_choice":
                typeDescription = "Multiple Choice: " + String.join(", ", question.getOptions());
                break;
            case "rating":
                typeDescription = "Rating: " + question.getMin() + " to " + question.getMax();
                break;
            case "text":
                typeDescription = "Text Response";
                break;
        }

        holder.questionTypeTextView.setText(typeDescription);
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView questionTextView;
        TextView questionTypeTextView;

        QuestionViewHolder(View itemView) {
            super(itemView);
            questionTextView = itemView.findViewById(R.id.question_text);
            questionTypeTextView = itemView.findViewById(R.id.question_type_text_view);
        }
    }
}