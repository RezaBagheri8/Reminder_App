package com.example.reminder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.reminder.database.AppDatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TaskListActivity extends BaseActivity implements TaskListAdapter.OnItemClickListener {

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
        taskListAdapter.setOnItemClickListener(this);

        fabAddTaskList = findViewById(R.id.fabAddTaskList);
        fabAddTaskList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskListDialog();
            }
        });

        loadTaskLists();
    }

    private void loadTaskLists() {
        taskLists.clear();
        taskLists.addAll(dbHelper.getAllTaskLists());
        taskListAdapter.notifyDataSetChanged();
    }

    private void showAddTaskListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Task List");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_add_task_list, null, false);
        final EditText input = viewInflated.findViewById(R.id.editTextTaskListName);
        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String taskListName = input.getText().toString().trim();
                if (!taskListName.isEmpty()) {
                    TaskList newTaskList = new TaskList(taskListName);
                    dbHelper.addTaskList(newTaskList);
                    loadTaskLists();
                } else {
                    Toast.makeText(TaskListActivity.this, "Task list name cannot be empty", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public void onItemClick(TaskList taskList) {
        // TODO: Handle item click (e.g., open a new activity for tasks in this list)
        Toast.makeText(this, "Clicked: " + taskList.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(TaskList taskList) {
        dbHelper.deleteTaskList(taskList);
        loadTaskLists();
        Toast.makeText(this, "Deleted: " + taskList.getName(), Toast.LENGTH_SHORT).show();
    }
}