package com.example.reminder;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reminder.database.AppDatabaseHelper;
import com.example.reminder.notification.AlarmScheduler;
import com.example.reminder.notification.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

    // Date and time picker components
    private ImageButton btnSetDateTime;
    private TextView tvDueDate;
    private Calendar selectedDateTime;
    private boolean hasSetDueDate = false;

    // Permission request launcher for notifications (Android 13+)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, can show notifications
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    // Permission denied, inform user about limitations
                    Toast.makeText(this, "Notification permission denied. You won't receive task reminders.",
                            Toast.LENGTH_LONG).show();
                }
            });

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

        // Create notification channel for Android O and above
        NotificationHelper.createNotificationChannel(this);

        // Request notification permission for Android 13+
        requestNotificationPermission();

        // Request exact alarm permission for Android 12+
        checkAndRequestExactAlarmPermission();

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

    /**
     * Request notification permission for Android 13+ (TIRAMISU)
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.POST_NOTIFICATIONS)) {
                // Explain to the user why we need the permission
                Toast.makeText(this, "Notification permission is needed to remind you about task deadlines",
                        Toast.LENGTH_LONG).show();
            }
            // Request the permission
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /**
     * Check and request permission to schedule exact alarms for Android 12+ (S)
     */
    private void checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                // Show a dialog explaining why we need the permission
                Toast.makeText(this, "This app needs permission to set exact alarms for task reminders",
                        Toast.LENGTH_LONG).show();

                // Open system settings to allow the user to grant the permission
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
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

        // Initialize date and time picker components
        btnSetDateTime = findViewById(R.id.btn_set_date_time);
        tvDueDate = findViewById(R.id.tv_due_date);
        selectedDateTime = Calendar.getInstance();
        spinnerTaskLists = findViewById(R.id.spinner_task_lists);

        // Set click listener for add button
        ivAddTask.setOnClickListener(v -> addNewTaskFromInput());

        // Set click listener for date time button
        btnSetDateTime.setOnClickListener(v -> showDateTimePicker());
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
                R.layout.spinner_item, taskListNames);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
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
            dbHelper.updateTaskCompletion(task.getId(), isChecked); // Update completion in DB

            // If task is completed and has a due date, cancel any scheduled notifications
            // This is also handled in the adapter, but we do it here as well for redundancy
            if (isChecked && task.getDueDate() != null) {
                AlarmScheduler.cancelTaskReminder(this, task.getId());
            }

            // Don't call updateTask here since it causes layout conflicts
            // The task is already updated in memory, no need to notify adapter
        }
    }

    /**
     * Shows the date picker dialog, followed by the time picker dialog
     */
    private void showDateTimePicker() {
        final Calendar currentDate = Calendar.getInstance();
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH);
        int day = currentDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDateTime.set(Calendar.YEAR, selectedYear);
                    selectedDateTime.set(Calendar.MONTH, selectedMonth);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, selectedDay);

                    // After date is set, show time picker
                    showTimePicker();
                }, year, month, day);

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    /**
     * Shows the time picker dialog after date has been selected
     */
    private void showTimePicker() {
        final Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                    selectedDateTime.set(Calendar.MINUTE, selectedMinute);
                    selectedDateTime.set(Calendar.SECOND, 0);

                    // Format and display the selected date and time
                    updateDueDateDisplay();
                    hasSetDueDate = true;
                }, hour, minute, false);

        timePickerDialog.show();
    }

    /**
     * Updates the due date display with the selected date and time
     */
    private void updateDueDateDisplay() {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
        String formattedDateTime = dateTimeFormat.format(selectedDateTime.getTime());
        tvDueDate.setText(formattedDateTime);
    }

    // Method to add new task from input field
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
        Date dueDate = hasSetDueDate ? selectedDateTime.getTime() : null;
        long newTaskId = dbHelper.createTask(taskTitle, currentTime, taskListId, dueDate); // Assuming taskListId 0 for now
        Task newTask = new Task(taskTitle, currentTime, false);
        newTask.setId(newTaskId);
        newTask.setTaskListId(taskListId);
        if (hasSetDueDate) {
            newTask.setDueDate(dueDate);

            // Schedule a notification for the task due date
            scheduleTaskReminder(newTask);
        }

        taskList.add(0, newTask); // Add to top of list
        taskAdapter.notifyItemInserted(0);

        // Scroll to top to show the new task
        rvTasks.smoothScrollToPosition(0);

        // Clear the input field and reset date/time
        etNewTask.setText("");
        tvDueDate.setText("Not set");
        hasSetDueDate = false;

        Toast.makeText(this, "Task added successfully", Toast.LENGTH_SHORT).show();
    }

    /**
     * Schedule a reminder notification for a task with a due date
     *
     * @param task The task to schedule a reminder for
     */
    private void scheduleTaskReminder(Task task) {
        if (task != null && task.getDueDate() != null) {
            // For Android 12+, check if we have permission to schedule exact alarms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    // We don't have permission, prompt the user again
                    Toast.makeText(this, "Please grant permission to set exact alarms for reminders",
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                    return;
                }
            }

            // Schedule the notification using AlarmScheduler for more precise timing
            AlarmScheduler.scheduleTaskReminder(this, task);

            // Inform the user that a reminder has been set
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            String formattedDateTime = dateTimeFormat.format(task.getDueDate());
            Toast.makeText(this, "Reminder set for " + formattedDateTime, Toast.LENGTH_SHORT).show();
        }
    }
}