package com.example.reminder;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public abstract class BaseActivity extends AppCompatActivity {

    private LinearLayout navAbout, navHome, navTasks;
    private ImageView navAboutIcon, navHomeIcon, navTasksIcon;
    private TextView navAboutText, navHomeText, navTasksText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupBottomNavigation() {
        // Include the bottom navigation layout
        RelativeLayout mainLayout = findViewById(R.id.main_layout);
        View navView = LayoutInflater.from(this).inflate(R.layout.bottom_navigation, mainLayout, false);
        mainLayout.addView(navView);

        // Initialize navigation elements
        navAbout = findViewById(R.id.nav_about);
        navHome = findViewById(R.id.nav_home);
        navTasks = findViewById(R.id.nav_tasks);

        navAboutIcon = findViewById(R.id.nav_about_icon);
        navHomeIcon = findViewById(R.id.nav_home_icon);
        navTasksIcon = findViewById(R.id.nav_tasks_icon);

        navAboutText = findViewById(R.id.nav_about_text);
        navHomeText = findViewById(R.id.nav_home_text);
        navTasksText = findViewById(R.id.nav_tasks_text);

        // Set click listeners
        navHome.setOnClickListener(v -> navigateToActivity(MainActivity.class));
        navTasks.setOnClickListener(v -> navigateToActivity(TaskListActivity.class));

        // Update navigation state
        updateNavigationState();
    }

    private void navigateToActivity(Class<?> activityClass) {
        if (!this.getClass().equals(activityClass)) {
            Intent intent = new Intent(this, activityClass);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            // Add smooth transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    private void updateNavigationState() {
        // Reset all navigation items to unselected state
        resetNavigationItems();

        // Set selected state for current activity
        if (this instanceof MainActivity) {
            setSelectedState(navHomeIcon, navHomeText);
        } else if (this instanceof TaskListActivity) {
            setSelectedState(navTasksIcon, navTasksText);
        }
    }

    private void resetNavigationItems() {
        setUnselectedState(navAboutIcon, navAboutText);
        setUnselectedState(navHomeIcon, navHomeText);
        setUnselectedState(navTasksIcon, navTasksText);
    }

    private void setSelectedState(ImageView icon, TextView text) {
        icon.setColorFilter(ContextCompat.getColor(this, R.color.nav_icon_selected));
        text.setTextColor(ContextCompat.getColor(this, R.color.nav_text_selected));
    }

    private void setUnselectedState(ImageView icon, TextView text) {
        icon.setColorFilter(ContextCompat.getColor(this, R.color.nav_icon_unselected));
        text.setTextColor(ContextCompat.getColor(this, R.color.nav_text_unselected));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update navigation state when activity resumes
        if (navHome != null) {
            updateNavigationState();
        }
    }
}