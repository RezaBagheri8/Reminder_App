package com.example.reminder;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import java.text.ParseException;
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
    private RecyclerView rvIncompleteTasks;
    private RecyclerView rvCompletedTasks;
    private TaskAdapter incompleteTasksAdapter;
    private TaskAdapter completedTasksAdapter;
    private List<Task> incompleteTaskList;
    private List<Task> completedTaskList;
    private AppDatabaseHelper dbHelper;
    private List<TaskList> taskLists;
    private Map<Long, String> taskListNames;
    private ActivityResultLauncher<Intent> taskListLauncher;

    // Date navigation components
    private ImageButton btnPreviousDay;
    private ImageButton btnNextDay;
    private TextView tvCurrentDate;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat dayFormat;

    // Permission request launcher for notifications (Android 13+)
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
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
        incompleteTaskList = new ArrayList<>();
        completedTaskList = new ArrayList<>();

        // Initialize date formats
        dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        selectedDate = Calendar.getInstance();

        // Initialize the activity result launcher
        taskListLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
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
        setupRecyclerViews();
    }

    private void refreshAllData() {
        taskListNames.clear();
        incompleteTaskList.clear();
        completedTaskList.clear();
        setupTaskLists();
        setupTaskList();
        setupRecyclerViews();
    }

    /**
     * Request notification permission for Android 13+ (TIRAMISU)
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)) {
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
        selectedDate = Calendar.getInstance(); // Reset to today
        updateCurrentDateDisplay();
        refreshAllData();
        // Refresh task lists when returning to this activity
        setupTaskLists();
        // Also refresh tasks to remove any tasks from deleted task lists
        setupTaskList();
        setupRecyclerViews();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        rvIncompleteTasks = findViewById(R.id.rv_incomplete_tasks);
        rvCompletedTasks = findViewById(R.id.rv_completed_tasks);

        // Initialize add task button
        Button btnAddTask = findViewById(R.id.btn_add_task);
        btnAddTask.setOnClickListener(v -> showAddTaskDialog());

        // Initialize date navigation components
        btnPreviousDay = findViewById(R.id.btn_previous_day);
        btnNextDay = findViewById(R.id.btn_next_day);
        tvCurrentDate = findViewById(R.id.tv_current_date);
        updateCurrentDateDisplay();

        // Set click listener for previous day button
        btnPreviousDay.setOnClickListener(v -> previousDay());

        // Set click listener for next day button
        btnNextDay.setOnClickListener(v -> nextDay());
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
    }

    private void setupTaskList() {
        incompleteTaskList.clear();
        completedTaskList.clear();
        List<Task> allTasks = dbHelper.getAllTasks();

        // Filter tasks for selected date
        for (Task task : allTasks) {
            if (task.getDueDate() != null) {
                Calendar taskDate = Calendar.getInstance();
                taskDate.setTime(task.getDueDate());

                // Compare year, month and day
                if (taskDate.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                        taskDate.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                        taskDate.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH)) {

                    if (task.isCompleted()) {
                        completedTaskList.add(task);
                    } else {
                        incompleteTaskList.add(task);
                    }
                }
            }
        }
    }

    private void setupRecyclerViews() {
        // Setup incomplete tasks RecyclerView
        if (incompleteTasksAdapter == null) {
            incompleteTasksAdapter = new TaskAdapter(incompleteTaskList, taskListNames);
            incompleteTasksAdapter.setOnTaskClickListener(this);
            rvIncompleteTasks.setLayoutManager(new LinearLayoutManager(this));
            rvIncompleteTasks.setAdapter(incompleteTasksAdapter);
        } else {
            incompleteTasksAdapter.notifyDataSetChanged();
        }

        // Setup completed tasks RecyclerView
        if (completedTasksAdapter == null) {
            completedTasksAdapter = new TaskAdapter(completedTaskList, taskListNames);
            completedTasksAdapter.setOnTaskClickListener(this);
            rvCompletedTasks.setLayoutManager(new LinearLayoutManager(this));
            rvCompletedTasks.setAdapter(completedTasksAdapter);
        } else {
            completedTasksAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onTaskChecked(int position, boolean isChecked) {
        Task task;
        if (isChecked) {
            // Task is being completed
            task = incompleteTaskList.get(position);
            task.setCompleted(true);
            incompleteTaskList.remove(position);
            completedTaskList.add(0, task);

            // Cancel any scheduled notifications
            if (task.getDueDate() != null) {
                AlarmScheduler.cancelTaskReminder(this, task.getId());
            }
        } else {
            // Task is being uncompleted
            task = completedTaskList.get(position);
            task.setCompleted(false);
            completedTaskList.remove(position);
            incompleteTaskList.add(0, task);

            // Reschedule notification if task has a due date
            if (task.getDueDate() != null) {
                scheduleTaskReminder(task);
            }
        }

        // Update task completion status in database
        dbHelper.updateTaskCompletion(task.getId(), isChecked);

        // Refresh both adapters
        incompleteTasksAdapter.notifyDataSetChanged();
        completedTasksAdapter.notifyDataSetChanged();
    }

    private void showAddTaskDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_task);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText etTaskName = dialog.findViewById(R.id.et_task_name);
        TextView tvSelectedDate = dialog.findViewById(R.id.tv_selected_date);
        TextView tvSelectedTime = dialog.findViewById(R.id.tv_selected_time);
        Spinner spinnerTaskList = dialog.findViewById(R.id.spinner_task_list);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnSave = dialog.findViewById(R.id.btn_save);

        // Setup task list spinner
        setupTaskListSpinner(spinnerTaskList);

        // Date picker
        View datePickerLayout = dialog.findViewById(R.id.date_picker_layout);
        datePickerLayout.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        tvSelectedDate.setText(new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                .format(selectedDate.getTime()));
                        validateForm(etTaskName, tvSelectedDate, tvSelectedTime, btnSave);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        // Time picker
        View timePickerLayout = dialog.findViewById(R.id.time_picker_layout);
        timePickerLayout.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        Calendar selectedTime = Calendar.getInstance();
                        selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedTime.set(Calendar.MINUTE, minute);
                        tvSelectedTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(selectedTime.getTime()));
                        validateForm(etTaskName, tvSelectedDate, tvSelectedTime, btnSave);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true);
            timePickerDialog.show();
        });

        // Text change listener for task name
        etTaskName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateForm(etTaskName, tvSelectedDate, tvSelectedTime, btnSave);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Save button
        btnSave.setEnabled(false);
        btnSave.setOnClickListener(v -> {
            String taskName = etTaskName.getText().toString();
            String dateStr = tvSelectedDate.getText().toString();
            String timeStr = tvSelectedTime.getText().toString();

            try {
                // Parse date and time
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault());
                Date dueDate = dateFormat.parse(dateStr + " " + timeStr);

                // Create and save task
                Task task = new Task(taskName, timeStr, false, dueDate);
                if (spinnerTaskList.getSelectedItemPosition() > 0) {
                    TaskList selectedList = taskLists.get(spinnerTaskList.getSelectedItemPosition() - 1);
                    task.setTaskListId(selectedList.getId());
                }

                long taskId = dbHelper.createTask(task.getTitle(), task.getTime(), task.getTaskListId(),
                        task.getDueDate());
                task.setId(taskId);

                // Schedule notification
                scheduleTaskReminder(task);

                // Refresh task list
                setupTaskList();
                setupRecyclerViews();

                dialog.dismiss();
            } catch (ParseException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error creating task", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void validateForm(EditText etTaskName, TextView tvSelectedDate,
            TextView tvSelectedTime, Button btnSave) {
        boolean isValid = !etTaskName.getText().toString().trim().isEmpty() &&
                !tvSelectedDate.getText().toString().equals("Select Date") &&
                !tvSelectedTime.getText().toString().equals("Select Time");
        btnSave.setEnabled(isValid);
    }

    private void setupTaskListSpinner(Spinner spinner) {
        List<String> taskListNames = new ArrayList<>();
        taskListNames.add("No List");
        for (TaskList taskList : taskLists) {
            taskListNames.add(taskList.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, taskListNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
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

    private void previousDay() {
        selectedDate.add(Calendar.DAY_OF_MONTH, -1);
        updateCurrentDateDisplay();
        setupTaskList();
        setupRecyclerViews();
    }

    private void nextDay() {
        selectedDate.add(Calendar.DAY_OF_MONTH, 1);
        updateCurrentDateDisplay();
        setupTaskList();
        setupRecyclerViews();
    }

    private void updateCurrentDateDisplay() {
        tvCurrentDate
                .setText(dayFormat.format(selectedDate.getTime()) + " " + dateFormat.format(selectedDate.getTime()));
    }
}