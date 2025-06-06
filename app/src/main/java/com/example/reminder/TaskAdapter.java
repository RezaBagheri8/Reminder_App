package com.example.reminder;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reminder.notification.AlarmScheduler;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private Map<Long, String> taskListNames;
    private OnTaskClickListener listener;
    private boolean isCompletedList;

    public interface OnTaskClickListener {
        void onTaskChecked(int position, boolean isChecked);
    }

    public TaskAdapter(List<Task> taskList, Map<Long, String> taskListNames) {
        this.taskList = taskList;
        this.taskListNames = taskListNames;
        this.isCompletedList = !taskList.isEmpty() && taskList.get(0).isCompleted();
    }

    public void setTaskListNames(Map<Long, String> taskListNames) {
        this.taskListNames = taskListNames;
        notifyDataSetChanged();
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        // Set task name
        holder.tvTaskName.setText(task.getTitle());

        // Set task completion status
        holder.cbTaskComplete.setChecked(task.isCompleted());

        // Apply different styles for completed tasks
        if (task.isCompleted()) {
            holder.tvTaskName.setPaintFlags(holder.tvTaskName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskName.setAlpha(0.5f);
            holder.tvTaskTime.setAlpha(0.5f);
            if (task.getTaskListId() != -1) {
                holder.tvTaskList.setAlpha(0.5f);
            }
        } else {
            holder.tvTaskName.setPaintFlags(holder.tvTaskName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTaskName.setAlpha(1.0f);
            holder.tvTaskTime.setAlpha(1.0f);
            if (task.getTaskListId() != -1) {
                holder.tvTaskList.setAlpha(1.0f);
            }
        }

        // Set task time
        if (task.getDueDate() != null) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.tvTaskTime.setText(timeFormat.format(task.getDueDate()));
            holder.tvTaskTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvTaskTime.setVisibility(View.GONE);
        }

        // Set task list name if available
        if (task.getTaskListId() != -1 && taskListNames.containsKey(task.getTaskListId())) {
            holder.tvTaskList.setText(taskListNames.get(task.getTaskListId()));
            holder.tvTaskList.setVisibility(View.VISIBLE);
        } else {
            holder.tvTaskList.setVisibility(View.GONE);
        }
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
        TextView tvTaskName;
        TextView tvTaskTime;
        TextView tvTaskList;
        CheckBox cbTaskComplete;

        TaskViewHolder(View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tv_task_name);
            tvTaskTime = itemView.findViewById(R.id.tv_task_time);
            tvTaskList = itemView.findViewById(R.id.tv_task_list);
            cbTaskComplete = itemView.findViewById(R.id.cb_task_complete);

            cbTaskComplete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskChecked(position, cbTaskComplete.isChecked());
                }
            });
        }
    }
}