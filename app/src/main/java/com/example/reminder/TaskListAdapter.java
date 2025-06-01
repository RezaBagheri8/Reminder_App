package com.example.reminder;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskListHolder> {
    private static final String TAG = "TaskListAdapter";

    private List<TaskList> taskLists;
    private OnTaskListClickListener listener;

    public interface OnTaskListClickListener {
        void onTaskListClick(int position);

        void onDeleteClick(TaskList taskList);
    }

    public TaskListAdapter(List<TaskList> taskLists) {
        this.taskLists = taskLists;
    }

    public void setOnTaskListClickListener(OnTaskListClickListener listener) {
        this.listener = listener;
        Log.d(TAG, "Click listener set: " + (listener != null));
    }

    @NonNull
    @Override
    public TaskListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_list, parent, false);
        return new TaskListHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskListHolder holder, int position) {
        TaskList currentTaskList = taskLists.get(position);
        holder.textViewTaskListName.setText(currentTaskList.getName());
        holder.textViewTaskCount.setText(currentTaskList.getTaskCount() + " tasks");

        // Set click listener in onBindViewHolder
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Item clicked at position: " + position);
            if (listener != null) {
                listener.onTaskListClick(position);
            } else {
                Log.e(TAG, "Listener is null!");
                // Show a toast for debugging
                Toast.makeText(v.getContext(), "Debug: Click listener is null", Toast.LENGTH_SHORT).show();
            }
        });

        holder.imageButtonDeleteTaskList.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(currentTaskList);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskLists.size();
    }

    class TaskListHolder extends RecyclerView.ViewHolder {
        private TextView textViewTaskListName;
        private TextView textViewTaskCount;
        private ImageButton imageButtonDeleteTaskList;

        public TaskListHolder(@NonNull View itemView) {
            super(itemView);
            textViewTaskListName = itemView.findViewById(R.id.textViewTaskListName);
            textViewTaskCount = itemView.findViewById(R.id.textViewTaskCount);
            imageButtonDeleteTaskList = itemView.findViewById(R.id.imageButtonDeleteTaskList);
        }
    }

    public void setTaskLists(List<TaskList> newLists) {
        this.taskLists = newLists;
        notifyDataSetChanged();
    }
}