package com.example.reminder;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reminder.database.AppDatabaseHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskListDetailsActivity extends BaseActivity implements TaskAdapter.OnTaskClickListener {

  public static final String EXTRA_TASK_LIST_ID = "task_list_id";
  public static final String EXTRA_TASK_LIST_NAME = "task_list_name";

  private RecyclerView rvTasks;
  private TextView tvTaskListName;
  private TextView tvNoTasks;
  private TaskAdapter taskAdapter;
  private List<Task> taskList;
  private AppDatabaseHelper dbHelper;
  private long taskListId;
  private String taskListName;
  private Map<Long, String> taskListNames;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_task_list_details);

    taskListId = getIntent().getLongExtra(EXTRA_TASK_LIST_ID, -1);
    taskListName = getIntent().getStringExtra(EXTRA_TASK_LIST_NAME);

    if (taskListId == -1 || taskListName == null) {
      finish();
      return;
    }

    dbHelper = new AppDatabaseHelper(this);
    taskListNames = new HashMap<>();
    taskListNames.put(taskListId, taskListName);

    initViews();
    loadTasks();
  }

  private void initViews() {
    rvTasks = findViewById(R.id.rv_tasks);
    tvTaskListName = findViewById(R.id.tv_task_list_name);
    tvNoTasks = findViewById(R.id.tv_no_tasks);

    tvTaskListName.setText(taskListName);

    // Enable back button in action bar
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setTitle(taskListName);
    }
  }

  private void loadTasks() {
    taskList = dbHelper.getTasksForTaskList(taskListId);

    if (taskList.isEmpty()) {
      tvNoTasks.setVisibility(View.VISIBLE);
      rvTasks.setVisibility(View.GONE);
    } else {
      tvNoTasks.setVisibility(View.GONE);
      rvTasks.setVisibility(View.VISIBLE);

      taskAdapter = new TaskAdapter(taskList, taskListNames);
      taskAdapter.setOnTaskClickListener(this);

      rvTasks.setLayoutManager(new LinearLayoutManager(this));
      rvTasks.setAdapter(taskAdapter);
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

  @Override
  public boolean onSupportNavigateUp() {
    onBackPressed();
    return true;
  }
}