package com.example.reminder.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.reminder.Task;
import com.example.reminder.database.AppDatabaseHelper;

/**
 * BroadcastReceiver to handle alarms for task reminders
 * This is triggered by AlarmManager at the scheduled time
 */
public class AlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_TASK_ID = "task_id";
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");
        
        // Get the task ID from the intent
        long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
        
        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID");
            return;
        }
        
        Log.d(TAG, "Processing alarm for task ID: " + taskId);
        
        // Get the task from the database
        AppDatabaseHelper dbHelper = new AppDatabaseHelper(context);
        Task task = dbHelper.getTaskById(taskId);
        
        if (task != null && !task.isCompleted()) {
            // Show notification for the task
            NotificationHelper.showTaskReminderNotification(
                    context,
                    task,
                    (int) taskId); // Use task ID as notification ID
            
            Log.d(TAG, "Notification shown for task: " + task.getTitle());
        } else {
            Log.d(TAG, "Task not found or already completed");
        }
    }
}