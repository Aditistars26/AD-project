package com.example.activityfeedback.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.activityfeedback.models.User;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    private List<User> studentList = new ArrayList<>();

    public StudentAdapter() {}

    public StudentAdapter(List<User> studentList) {
        this.studentList = studentList;
    }

    public void setStudentList(List<User> students) {
        this.studentList = students;
        notifyDataSetChanged();
    }

        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User student = studentList.get(position);
            holder.name.setText(student.getName());
            holder.info.setText(student.getEmail() + " | " + student.getEnrollmentId());
        }

        @Override
        public int getItemCount() {
            return studentList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, info;

            ViewHolder(View view) {
                super(view);
                name = view.findViewById(android.R.id.text1);
                info = view.findViewById(android.R.id.text2);
            }
        }
    }


