package com.example.reminder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskListHolder> {

    private List<TaskList> taskLists;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TaskList taskList);
        void onDeleteClick(TaskList taskList);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public TaskListAdapter(List<TaskList> taskLists) {
        this.taskLists = taskLists;
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

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(taskLists.get(position));
                    }
                }
            });

            imageButtonDeleteTaskList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(taskLists.get(position));
                    }
                }
            });
        }
    }

    public void setTaskLists(List<TaskList> newLists) {
        this.taskLists = newLists;
        notifyDataSetChanged();
    }
}