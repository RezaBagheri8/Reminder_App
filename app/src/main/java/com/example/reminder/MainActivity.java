package com.example.reminder;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.reminder.database.AppDatabaseHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements TaskAdapter.OnTaskClickListener {

    private TextView tvGreeting;
    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private ImageView ivAddTask;
    private EditText etNewTask;
    private AppDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupBottomNavigation();

        dbHelper = new AppDatabaseHelper(this);


        initViews();
        setupGreeting();
        setupTaskList(); // This will now load from DB
        setupRecyclerView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        rvTasks = findViewById(R.id.rv_tasks);
        ivAddTask = findViewById(R.id.iv_add_task);
        etNewTask = findViewById(R.id.et_new_task);

        // Set click listener for add button
        ivAddTask.setOnClickListener(v -> addNewTaskFromInput());
    }

    private void setupGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }

        tvGreeting.setText(greeting);
    }

    private void setupTaskList() {
        taskList = dbHelper.getAllTasks(); // Load tasks from database
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(taskList);
        taskAdapter.setOnTaskClickListener(this);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
        rvTasks.setNestedScrollingEnabled(false);
    }

    @Override
    public void onTaskChecked(int position, boolean isChecked) {
        if (position >= 0 && position < taskList.size()) {
            Task task = taskList.get(position);
            task.setCompleted(isChecked);
            dbHelper.updateTaskCompletion(task.getId(), isChecked); // Update completion in DB
            // Don't call updateTask here since it causes layout conflicts
            // The task is already updated in memory, no need to notify adapter
        }
    }

    // Method to add new task from input field
    private void addNewTaskFromInput() {
        String taskTitle = etNewTask.getText().toString().trim();

        if (TextUtils.isEmpty(taskTitle)) {
            Toast.makeText(this, "Please enter a task", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current time for the task
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mma", Locale.getDefault());
        String currentTime = timeFormat.format(calendar.getTime());

        // Create new task and add to the database
        long newTaskId = dbHelper.createTask(taskTitle, currentTime, 0); // Assuming taskListId 0 for now
        Task newTask = new Task(taskTitle, currentTime, false);
        newTask.setId(newTaskId);
        taskList.add(0, newTask); // Add to top of list
        taskAdapter.notifyItemInserted(0);

        // Scroll to top to show the new task
        rvTasks.smoothScrollToPosition(0);

        // Clear the input field
        etNewTask.setText("");

        Toast.makeText(this, "Task added successfully", Toast.LENGTH_SHORT).show();
    }
}