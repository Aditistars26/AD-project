package com.example.activityfeedback.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.activityfeedback.R;
import com.example.activityfeedback.models.Form;
import com.example.activityfeedback.utils.DateTimeConverter;
import com.example.activityfeedback.utils.FirebaseHelper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.util.List;

public class FormListAdapter extends RecyclerView.Adapter<FormListAdapter.FormViewHolder> {

    private final Context context;
    private List<Form> forms;
    private final OnFormClickListener listener;
    private final boolean isProfessorView;

    public interface OnFormClickListener {
        void onFormClick(Form form);
    }

    public FormListAdapter(Context context, List<Form> forms, OnFormClickListener listener, boolean isProfessorView) {
        this.context = context;
        this.forms = forms;
        this.listener = listener;
        this.isProfessorView = isProfessorView;
    }

    @NonNull
    @Override
    public FormViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_form, parent, false);
        return new FormViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull FormViewHolder holder, int position) {
        Form form = forms.get(position);
        holder.titleTextView.setText(form.getTitle());
        holder.descriptionTextView.setText(form.getDescription());
        holder.createdAtTextView.setText("Created: " + DateTimeConverter.formatForDisplay(form.getCreatedAt()));
        holder.questionCountTextView.setText(form.getQuestions().size() + " questions");

        if (form.isActive()) {
            holder.statusTextView.setText("Active");
            holder.statusTextView.setTextColor(context.getResources().getColor(R.color.colorSuccess));
        } else {
            holder.statusTextView.setText("Inactive");
            holder.statusTextView.setTextColor(context.getResources().getColor(R.color.colorError));
        }

        if (isProfessorView) {
            holder.professorControls.setVisibility(View.VISIBLE);

            Button toggleButton = holder.toggleActiveButton;
            if (form.isActive()) {
                toggleButton.setText("Deactivate Form");
                toggleButton.setBackgroundResource(R.drawable.button_secondary);
            } else {
                toggleButton.setText("Activate Form");
                toggleButton.setBackgroundResource(R.drawable.button_primary);
                toggleButton.setTextColor(context.getResources().getColor(android.R.color.white));
            }

            toggleButton.setOnClickListener(v -> {
                form.setActive(!form.isActive());
                form.setLastModified(LocalDateTime.now());

                FirebaseHelper.updateFormStatus(form.getFormId(), form.isActive(), form.getLastModified(), success -> {
                    if (success) {
                        notifyItemChanged(position);
                    } else {
                        form.setActive(!form.isActive());
                        Toast.makeText(context, "Failed to update form status", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            Button deleteButton = holder.deleteFormButton;
            deleteButton.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete Form");
                builder.setMessage("Are you sure you want to delete this form? This will also delete all submissions for this form. This action cannot be undone.");
                builder.setPositiveButton("Delete", (dialog, which) -> {
                    Toast.makeText(context, "Deleting form...", Toast.LENGTH_SHORT).show();

                    FirebaseHelper.deleteForm(form.getFormId(), success -> {
                        if (success) {
                            int adapterPosition = holder.getAdapterPosition();
                            if (adapterPosition != RecyclerView.NO_POSITION) {
                                forms.remove(adapterPosition);
                                notifyItemRemoved(adapterPosition);
                                Toast.makeText(context, "Form deleted", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "Failed to delete form", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            });
        } else {
            holder.professorControls.setVisibility(View.GONE);
        }

        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFormClick(form);
            }
        });
    }

    @Override
    public int getItemCount() {
        return forms.size();
    }

    public void updateList(List<Form> filteredForms) {
        this.forms.clear();
        this.forms.addAll(filteredForms);
        notifyDataSetChanged();
    }

    static class FormViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView createdAtTextView;
        TextView questionCountTextView;
        TextView statusTextView;
        LinearLayout professorControls;
        Button toggleActiveButton;
        Button deleteFormButton;

        FormViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            titleTextView = itemView.findViewById(R.id.title_text_view);
            descriptionTextView = itemView.findViewById(R.id.description_text_view);
            createdAtTextView = itemView.findViewById(R.id.created_at_text_view);
            questionCountTextView = itemView.findViewById(R.id.question_count_text_view);
            statusTextView = itemView.findViewById(R.id.status_text_view);
            professorControls = itemView.findViewById(R.id.professor_controls);

            if (professorControls != null) {
                toggleActiveButton = itemView.findViewById(R.id.toggle_active_button);
                deleteFormButton = itemView.findViewById(R.id.delete_form_button);

                if (deleteFormButton != null) {
                    deleteFormButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
