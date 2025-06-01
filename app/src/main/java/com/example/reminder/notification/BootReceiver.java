package com.example.reminder.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.reminder.Task;
import com.example.reminder.database.AppDatabaseHelper;

import java.util.List;

/**
 * BroadcastReceiver to reschedule task reminders after device reboot
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Device rebooted, rescheduling task reminders");
            // Reschedule all task reminders
            rescheduleTaskReminders(context);
        }
    }
    
    /**
     * Reschedule all task reminders from the database
     * @param context The application context
     */
    private void rescheduleTaskReminders(Context context) {
        // Get all tasks from the database
        AppDatabaseHelper dbHelper = new AppDatabaseHelper(context);
        List<Task> tasks = dbHelper.getAllTasks();
        
        // Reschedule reminders for tasks with due dates that haven't been completed
        for (Task task : tasks) {
            if (task.getDueDate() != null && !task.isCompleted()) {
                // Only schedule if due date is in the future
                if (task.getDueDate().getTime() > System.currentTimeMillis()) {
                    // Use AlarmScheduler instead of WorkManager
                    AlarmScheduler.scheduleTaskReminder(context, task);
                    Log.d(TAG, "Rescheduled alarm for task: " + task.getTitle());
                }
            }
        }
    }
}