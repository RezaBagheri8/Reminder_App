package com.example.reminder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.reminder.database.AppDatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TaskListActivity extends BaseActivity implements TaskListAdapter.OnTaskListClickListener {

    private RecyclerView recyclerView;
    private TaskListAdapter taskListAdapter;
    private List<TaskList> taskLists;
    private AppDatabaseHelper dbHelper;
    private FloatingActionButton fabAddTaskList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        setupBottomNavigation();

        recyclerView = findViewById(R.id.recyclerViewTaskLists);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new AppDatabaseHelper(this);
        taskLists = new ArrayList<>();
        taskListAdapter = new TaskListAdapter(taskLists);
        recyclerView.setAdapter(taskListAdapter);
        taskListAdapter.setOnTaskListClickListener(this);

        fabAddTaskList = findViewById(R.id.fabAddTaskList);
        fabAddTaskList.setOnClickListener(v -> showAddTaskListDialog());

        loadTaskLists();
    }

    private void loadTaskLists() {
        taskLists.clear();
        taskLists.addAll(dbHelper.getAllTaskLists());
        taskListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onTaskListClick(int position) {
//        if (position >= 0 && position < taskLists.size()) {
//            TaskList taskList = taskLists.get(position);
//            Intent intent = new Intent(this, TaskListDetailsActivity.class);
//            intent.putExtra(TaskListDetailsActivity.EXTRA_TASK_LIST_ID, (long) taskList.getId());
//            intent.putExtra(TaskListDetailsActivity.EXTRA_TASK_LIST_NAME, taskList.getName());
//            startActivity(intent);
//        }
    }

    @Override
    public void onDeleteClick(TaskList taskList) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task List")
                .setMessage(
                        "Are you sure you want to delete this task list? All tasks in this list will also be deleted.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteTaskList(taskList);
                    loadTaskLists();
                    setResult(RESULT_OK);
                    Toast.makeText(this, "Task list deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddTaskListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Task List");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_add_task_list, null);
        final EditText input = viewInflated.findViewById(R.id.editTextTaskListName);
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String taskListName = input.getText().toString().trim();
            if (!taskListName.isEmpty()) {
                TaskList newTaskList = new TaskList(taskListName);
                dbHelper.addTaskList(newTaskList);
                loadTaskLists();
            } else {
                Toast.makeText(TaskListActivity.this, "Task list name cannot be empty", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }
}