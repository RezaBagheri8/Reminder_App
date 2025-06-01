package com.example.reminder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskClickListener listener;
    private Map<Long, String> taskListNames; // Map of task list IDs to their names

    public interface OnTaskClickListener {
        void onTaskChecked(int position, boolean isChecked);
    }

    public TaskAdapter(List<Task> taskList, Map<Long, String> taskListNames) {
        this.taskList = taskList;
        this.taskListNames = taskListNames;
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
        private TextView tvTaskListName;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbTask = itemView.findViewById(R.id.cb_task);
            tvTaskTitle = itemView.findViewById(R.id.tv_task_title);
            tvTaskListName = itemView.findViewById(R.id.tv_task_list_name);
        }

        public void bind(Task task, int position) {
            tvTaskTitle.setText(task.getDisplayText());

            // Set task list name
            String taskListName = taskListNames.get(task.getTaskListId());
            if (taskListName != null && !taskListName.isEmpty()) {
                tvTaskListName.setVisibility(View.VISIBLE);
                tvTaskListName.setText(taskListName);
            } else {
                tvTaskListName.setVisibility(View.GONE);
            }

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