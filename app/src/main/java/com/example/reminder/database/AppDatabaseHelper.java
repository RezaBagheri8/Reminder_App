package com.example.reminder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.reminder.TaskList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "reminder_app.db";

    // Task Lists table
    public static final String TABLE_TASK_LISTS = "task_lists";
    public static final String COLUMN_LIST_ID = "_id";
    public static final String COLUMN_LIST_NAME = "name";

    // Tasks table
    public static final String TABLE_TASKS = "tasks";
    public static final String COLUMN_TASK_ID = "_id";
    public static final String COLUMN_TASK_TITLE = "title";
    public static final String COLUMN_TASK_TIME = "time";
    public static final String COLUMN_TASK_IS_COMPLETED = "is_completed";
    public static final String COLUMN_TASK_LIST_FK = "task_list_id"; // Foreign key to task_lists table
    public static final String COLUMN_TASK_DUE_DATE = "due_date"; // For storing the task deadline

    private static final String CREATE_TASK_LISTS_TABLE = "CREATE TABLE " + TABLE_TASK_LISTS + "("
            + COLUMN_LIST_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_LIST_NAME + " TEXT"
            + ")";

    private static final String CREATE_TASKS_TABLE = "CREATE TABLE "
            + TABLE_TASKS + "("
            + COLUMN_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_TASK_TITLE + " TEXT NOT NULL, "
            + COLUMN_TASK_TIME + " TEXT, "
            + COLUMN_TASK_IS_COMPLETED + " INTEGER NOT NULL DEFAULT 0, "
            + COLUMN_TASK_LIST_FK + " INTEGER, "
            + COLUMN_TASK_DUE_DATE + " INTEGER, "
            + "FOREIGN KEY(" + COLUMN_TASK_LIST_FK + ") REFERENCES " + TABLE_TASK_LISTS + "(" + COLUMN_LIST_ID + ") ON DELETE CASCADE"
            + ");";

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TASK_LISTS_TABLE);
        db.execSQL(CREATE_TASKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASK_LISTS);
        onCreate(db);
    }

    // --- Task List Operations ---

    public long addTaskList(TaskList taskList) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LIST_NAME, taskList.getName());
        long id = db.insert(TABLE_TASK_LISTS, null, values);
        db.close();
        return id;
    }

    public List<TaskList> getAllTaskLists() {
        List<TaskList> taskList = new ArrayList<>();
        // Select all task lists and count the number of tasks for each list
        String selectQuery = "SELECT tl." + COLUMN_LIST_ID + ", tl." + COLUMN_LIST_NAME + ", COUNT(t." + COLUMN_TASK_ID
                + ") AS task_count " +
                "FROM " + TABLE_TASK_LISTS + " tl " +
                "LEFT JOIN " + TABLE_TASKS + " t ON tl." + COLUMN_LIST_ID + " = t." + COLUMN_TASK_LIST_FK +
                " GROUP BY tl." + COLUMN_LIST_ID + ", tl." + COLUMN_LIST_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                TaskList list = new TaskList(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIST_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LIST_NAME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow("task_count")));
                taskList.add(list);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return taskList;
    }

    public void deleteTaskList(TaskList taskList) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Start a transaction to ensure both operations complete or none do
            db.beginTransaction();

            // First delete all tasks associated with this task list
            db.delete(TABLE_TASKS, COLUMN_TASK_LIST_FK + " = ?",
                    new String[] { String.valueOf(taskList.getId()) });

            // Then delete the task list itself
            db.delete(TABLE_TASK_LISTS, COLUMN_LIST_ID + " = ?",
                    new String[] { String.valueOf(taskList.getId()) });

            // Mark transaction as successful
            db.setTransactionSuccessful();
        } finally {
            // End transaction
            db.endTransaction();
            db.close();
        }
    }

    // --- Task Operations ---

    public long createTask(String title, String time, long taskListId) {
        return createTask(title, time, taskListId, null);
    }

    public long createTask(String title, String time, long taskListId, Date dueDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_TITLE, title);
        values.put(COLUMN_TASK_TIME, time);
        values.put(COLUMN_TASK_IS_COMPLETED, 0); // Default to not completed
        values.put(COLUMN_TASK_LIST_FK, taskListId);

        if (dueDate != null) {
            values.put(COLUMN_TASK_DUE_DATE, dueDate.getTime()); // Store as milliseconds since epoch
        }

        long insertId = db.insert(TABLE_TASKS, null, values);
        db.close();
        return insertId;
    }

    public void updateTaskCompletion(long taskId, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_IS_COMPLETED, isCompleted ? 1 : 0);
        db.update(TABLE_TASKS, values, COLUMN_TASK_ID + " = ?", new String[] { String.valueOf(taskId) });
        db.close();
    }

    public void deleteTask(long taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COLUMN_TASK_ID + " = ?", new String[] { String.valueOf(taskId) });
        db.close();
    }

    public List<com.example.reminder.Task> getAllTasks() {
        List<com.example.reminder.Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                com.example.reminder.Task task = new com.example.reminder.Task(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TIME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_IS_COMPLETED)) == 1);
                task.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID)));
                task.setTaskListId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_LIST_FK)));

                // Get due date if it exists
                int dueDateColumnIndex = cursor.getColumnIndex(COLUMN_TASK_DUE_DATE);
                if (dueDateColumnIndex != -1 && !cursor.isNull(dueDateColumnIndex)) {
                    long dueDateMillis = cursor.getLong(dueDateColumnIndex);
                    task.setDueDate(new Date(dueDateMillis));
                }

                tasks.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    public List<com.example.reminder.Task> getTasksForTaskList(long taskListId) {
        List<com.example.reminder.Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " WHERE " + COLUMN_TASK_LIST_FK + " = ?";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[] { String.valueOf(taskListId) });

        if (cursor.moveToFirst()) {
            do {
                com.example.reminder.Task task = new com.example.reminder.Task(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TIME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_IS_COMPLETED)) == 1);
                task.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID)));
                task.setTaskListId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_LIST_FK)));

                // Get due date if it exists
                int dueDateColumnIndex = cursor.getColumnIndex(COLUMN_TASK_DUE_DATE);
                if (dueDateColumnIndex != -1 && !cursor.isNull(dueDateColumnIndex)) {
                    long dueDateMillis = cursor.getLong(dueDateColumnIndex);
                    task.setDueDate(new Date(dueDateMillis));
                }

                tasks.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    /**
     * Get a task by its ID
     * @param taskId The ID of the task to retrieve
     * @return The task with the specified ID, or null if not found
     */
    public com.example.reminder.Task getTaskById(long taskId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_TASKS,
                null,
                COLUMN_TASK_ID + " = ?",
                new String[]{String.valueOf(taskId)},
                null,
                null,
                null);

        com.example.reminder.Task task = null;

        if (cursor.moveToFirst()) {
            task = new com.example.reminder.Task(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TITLE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TIME)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_IS_COMPLETED)) == 1
            );
            task.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID)));
            task.setTaskListId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TASK_LIST_FK)));

            // Get due date if it exists
            int dueDateColumnIndex = cursor.getColumnIndex(COLUMN_TASK_DUE_DATE);
            if (dueDateColumnIndex != -1 && !cursor.isNull(dueDateColumnIndex)) {
                long dueDateMillis = cursor.getLong(dueDateColumnIndex);
                task.setDueDate(new Date(dueDateMillis));
            }
        }

        cursor.close();
        db.close();
        return task;
    }
}