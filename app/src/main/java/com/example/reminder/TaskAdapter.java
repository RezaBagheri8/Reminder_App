package com.example.reminder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskChecked(int position, boolean isChecked);
    }

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task, position);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateTask(int position, Task task) {
        if (position >= 0 && position < taskList.size()) {
            taskList.set(position, task);
            // Use post to avoid layout computation conflicts
            if (recyclerView != null) {
                recyclerView.post(() -> notifyItemChanged(position));
            } else {
                notifyItemChanged(position);
            }
        }
    }

    private RecyclerView recyclerView;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    public void addTaskToTop(Task task) {
        taskList.add(0, task);
        notifyItemInserted(0);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox cbTask;
        private TextView tvTaskTitle;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbTask = itemView.findViewById(R.id.cb_task);
            tvTaskTitle = itemView.findViewById(R.id.tv_task_title);
        }

        public void bind(Task task, int position) {
            tvTaskTitle.setText(task.getDisplayText());

            // Clear the listener first to prevent triggering during setChecked
            cbTask.setOnCheckedChangeListener(null);
            cbTask.setChecked(task.isCompleted());

            // Set the listener after setting the checked state
            cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onTaskChecked(position, isChecked);
                }
            });
        }
    }
}