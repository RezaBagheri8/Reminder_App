package com.example.reminder;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends BaseActivity implements TaskAdapter.OnTaskClickListener {

    private TextView tvGreeting;
    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private ImageView ivAddTask;
    private EditText etNewTask;
    private Spinner spinnerTaskLists;
    private AppDatabaseHelper dbHelper;
    private List<TaskList> taskLists;
    private Map<Long, String> taskListNames;
    private ActivityResultLauncher<Intent> taskListLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupBottomNavigation();

        dbHelper = new AppDatabaseHelper(this);
        taskListNames = new HashMap<>();
        taskList = new ArrayList<>();

        // Initialize the activity result launcher
        taskListLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Refresh everything when returning from TaskListActivity after a deletion
                        refreshAllData();
                    }
                });

        initViews();
        setupGreeting();
        setupTaskLists();
        setupTaskList();
        setupRecyclerView();
    }

    private void refreshAllData() {
        // Clear and reload all data
        taskListNames.clear();
        taskList.clear();
        setupTaskLists();
        setupTaskList();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllData();
        // Refresh task lists when returning to this activity
        setupTaskLists();
        // Also refresh tasks to remove any tasks from deleted task lists
        setupTaskList();
        setupRecyclerView();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        rvTasks = findViewById(R.id.rv_tasks);
        ivAddTask = findViewById(R.id.iv_add_task);
        etNewTask = findViewById(R.id.et_new_task);
        spinnerTaskLists = findViewById(R.id.spinner_task_lists);

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

    private void setupTaskLists() {
        taskLists = dbHelper.getAllTaskLists();
        List<String> taskListNames = new ArrayList<>();
        taskListNames.add("No List"); // Add default option

        for (TaskList taskList : taskLists) {
            taskListNames.add(taskList.getName());
            this.taskListNames.put(Long.valueOf(taskList.getId()), taskList.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, taskListNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaskLists.setAdapter(adapter);
    }

    private void setupTaskList() {
        taskList.clear();
        taskList.addAll(dbHelper.getAllTasks());
    }

    private void setupRecyclerView() {
        if (taskAdapter == null) {
            taskAdapter = new TaskAdapter(taskList, taskListNames);
            taskAdapter.setOnTaskClickListener(this);
            rvTasks.setLayoutManager(new LinearLayoutManager(this));
            rvTasks.setAdapter(taskAdapter);
            rvTasks.setNestedScrollingEnabled(false);
        } else {
            taskAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onTaskChecked(int position, boolean isChecked) {
        if (position >= 0 && position < taskList.size()) {
            Task task = taskList.get(position);
            task.setCompleted(isChecked);
            dbHelper.updateTaskCompletion(task.getId(), isChecked);
        }
    }

    private void addNewTaskFromInput() {
        String taskTitle = etNewTask.getText().toString().trim();

        if (TextUtils.isEmpty(taskTitle)) {
            Toast.makeText(this, "Please enter a task", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected task list ID
        int selectedPosition = spinnerTaskLists.getSelectedItemPosition();
        long taskListId = 0; // Default to no list
        if (selectedPosition > 0) { // If not "No List"
            taskListId = taskLists.get(selectedPosition - 1).getId();
        }

        // Get current time for the task
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mma", Locale.getDefault());
        String currentTime = timeFormat.format(calendar.getTime());

        // Create new task and add to the database
        long newTaskId = dbHelper.createTask(taskTitle, currentTime, taskListId);
        Task newTask = new Task(taskTitle, currentTime, false);
        newTask.setId(newTaskId);
        newTask.setTaskListId(taskListId);
        taskList.add(0, newTask);
        taskAdapter.notifyItemInserted(0);

        // Scroll to top to show the new task
        rvTasks.smoothScrollToPosition(0);

        // Clear the input field
        etNewTask.setText("");

        Toast.makeText(this, "Task added successfully", Toast.LENGTH_SHORT).show();
    }
}