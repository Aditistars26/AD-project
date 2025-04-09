//package com.example.activityfeedback.adapters;
//
//public class SubmissionAnswerAdapter {
//}

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
import java.util.Map;

public class SubmissionAnswerAdapter extends RecyclerView.Adapter<SubmissionAnswerAdapter.ViewHolder> {

    private Context context;
    private List<Question> questions;
    private Map<String,Object> answers;

    public SubmissionAnswerAdapter(Context context, List<Question> questions, Map<String, Object> answers) {
        this.context = context;
        this.questions = questions;
        this.answers = answers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_submission_answer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question question = questions.get(position);
        String questionText = question.getText();
        String questionId = question.getId();

        // Get the answer for this question
        //String answer = (String) answers.get(questionId);
        Object answerObj = answers.get(questionId);
        String answer = String.valueOf(answerObj); // this avoids ClassCastException

        if (answer == null) {
            answer = "No answer provided";
        }

        // Set the question and answer text
        holder.questionTextView.setText((position + 1) + ". " + questionText);
        holder.answerTextView.setText(answer);
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView questionTextView;
        TextView answerTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            questionTextView = itemView.findViewById(R.id.question_text_view);
            answerTextView = itemView.findViewById(R.id.answer_text_view);
        }
    }
}