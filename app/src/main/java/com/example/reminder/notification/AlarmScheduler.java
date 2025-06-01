package com.example.reminder.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.reminder.Task;

import java.util.Date;

/**
 * Scheduler class to manage task reminder alarms using AlarmManager
 * for more precise timing than WorkManager
 */
public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";

    /**
     * Schedule a reminder notification for a task
     * @param context The application context
     * @param task The task to schedule a reminder for
     */
    public static void scheduleTaskReminder(Context context, Task task) {
        // Only schedule if the task has a due date
        Date dueDate = task.getDueDate();
        if (dueDate == null) {
            return;
        }
        
        // Get the alarm time in milliseconds
        long alarmTimeMillis = dueDate.getTime();
        
        // If due date is in the past, don't schedule
        long currentTimeMillis = System.currentTimeMillis();
        if (alarmTimeMillis <= currentTimeMillis) {
            Log.d(TAG, "Not scheduling alarm for past due date");
            return;
        }
        
        // Create an intent for the alarm receiver
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_TASK_ID, task.getId());
        
        // Create a unique request code based on the task ID
        int requestCode = (int) task.getId();
        
        // Create a pending intent that will be triggered when the alarm fires
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Get the AlarmManager service
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // For Android 12+, check if we have permission to schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
            Log.e(TAG, "Cannot schedule exact alarm - permission not granted");
            return;
        }
        
        // Schedule the alarm with the appropriate method based on API level
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For Android M and above, use setExactAndAllowWhileIdle for precise timing even in Doze mode
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent);
                Log.d(TAG, "Scheduled exact alarm with setExactAndAllowWhileIdle for task: " + task.getTitle() + 
                        " at time: " + new Date(alarmTimeMillis));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // For Android KitKat to Lollipop, use setExact
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent);
                Log.d(TAG, "Scheduled exact alarm with setExact for task: " + task.getTitle() + 
                        " at time: " + new Date(alarmTimeMillis));
            } else {
                // For older versions, use set
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent);
                Log.d(TAG, "Scheduled alarm with set for task: " + task.getTitle() + 
                        " at time: " + new Date(alarmTimeMillis));
            }
        }
    }
    
    /**
     * Cancel a scheduled reminder for a task
     * @param context The application context
     * @param taskId The ID of the task to cancel the reminder for
     */
    public static void cancelTaskReminder(Context context, long taskId) {
        // Create an intent matching the one used to set the alarm
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId);
        
        // Create a matching pending intent
        int requestCode = (int) taskId;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Get the AlarmManager service and cancel the alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Cancelled alarm for task ID: " + taskId);
        }
    }
}