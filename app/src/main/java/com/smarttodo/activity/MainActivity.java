package com.smarttodo.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.smarttodo.adapter.TaskAdapter;
import com.smarttodo.databinding.ActivityMainBinding;
import com.smarttodo.firebase.FirebaseManager;
import com.smarttodo.model.Category;
import com.smarttodo.model.Task;
import com.smarttodo.viewmodel.CategoryViewModel;
import com.smarttodo.viewmodel.StatisticsViewModel;
import com.smarttodo.viewmodel.TaskViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MainActivity - màn hình chính của ứng dụng.
 * Hiển thị danh sách task, search, filter theo category chips.
 * Bottom navigation điều hướng đến Statistics và Profile.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private TaskViewModel     taskViewModel;
    private CategoryViewModel categoryViewModel;
    private TaskAdapter       taskAdapter;

    // Map categoryId -> Category để hiển thị trong adapter
    private Map<String, Category> categoryMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Kiểm tra auth
        if (!FirebaseManager.getInstance().isUserLoggedIn()) {
            navigateToLogin();
            return;
        }

        initViewModels();
        setupRecyclerView();
        setupObservers();
        setupClickListeners();
        setupBottomNavigation();
        setupSearch();
        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void initViewModels() {
        taskViewModel     = new ViewModelProvider(this).get(TaskViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        // Load data
        taskViewModel.loadAllTasks();
        categoryViewModel.loadCategories();
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this);
        binding.recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewTasks.setAdapter(taskAdapter);

        taskAdapter.setOnTaskClickListener(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {
                // Mở TaskDetailActivity
                Intent intent = new Intent(MainActivity.this, TaskDetailActivity.class);
                intent.putExtra("taskId", task.getTaskId());
                startActivity(intent);
            }

            @Override
            public void onTaskLongClick(Task task) {
                // Mở EditTaskActivity khi long click
                Intent intent = new Intent(MainActivity.this, EditTaskActivity.class);
                intent.putExtra("taskId", task.getTaskId());
                startActivity(intent);
            }

            @Override
            public void onCompleteToggle(Task task, boolean isCompleted) {
                taskViewModel.toggleComplete(task.getTaskId(), isCompleted);
            }

            @Override
            public void onDeleteClick(Task task) {
                showDeleteConfirmation(task);
            }
        });
    }

    private void setupObservers() {
        // Observe tất cả tasks (real-time từ Firestore)
        taskViewModel.allTasks.observe(this, tasks -> {
            // Khi tasks thay đổi, áp dụng filter hiện tại
            taskViewModel.applyFilter();
        });

        // Observe filtered tasks để hiển thị lên UI
        taskViewModel.filteredTasks.observe(this, tasks -> {
            taskAdapter.submitList(tasks);
            updateEmptyState(tasks);
        });

        // Observe categories để build categoryMap và UI chips
        categoryViewModel.categories.observe(this, categories -> {
            if (categories == null) return;

            // Build categoryMap
            categoryMap.clear();
            for (Category cat : categories) {
                categoryMap.put(cat.getCategoryId(), cat);
            }
            taskAdapter.setCategoryMap(categoryMap);

            // Build category filter chips
            buildCategoryChips(categories);
        });

        // Observe loading
        taskViewModel.isLoading.observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.swipeRefresh.setRefreshing(false);
        });

        // Observe error
        taskViewModel.errorMessage.observe(this, error -> {
            if (error != null) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_SHORT).show();
                taskViewModel.clearMessages();
            }
        });

        // Observe success message
        taskViewModel.successMessage.observe(this, msg -> {
            if (msg != null) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(Color.parseColor("#4CAF50")).show();
                taskViewModel.clearMessages();
            }
        });

        // Load user name
        loadUserGreeting();
    }

    private void setupClickListeners() {
        // Statistics button in top right header
        binding.btnStatistics.setOnClickListener(v -> {
            startActivity(new Intent(this, StatisticsActivity.class));
        });

        // Filter button
        binding.btnFilter.setOnClickListener(v -> showFilterDialog());

        // Categories button
        binding.btnCategories.setOnClickListener(v -> {
            startActivity(new Intent(this, CategoryActivity.class));
        });

        // SwipeRefresh (Tự động tắt sau 1.5 - 2s)
        binding.swipeRefresh.setOnRefreshListener(() -> {
            taskViewModel.loadAllTasks();
            binding.swipeRefresh.postDelayed(() -> {
                if (binding != null && binding.swipeRefresh != null) {
                    binding.swipeRefresh.setRefreshing(false);
                }
            }, 1500);
        });

        // "Tất cả" chip
        binding.chipAll.setOnClickListener(v -> {
            taskViewModel.setFilterCategory(null);
        });
    }

    /**
     * Setup bottom navigation
     */
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == com.smarttodo.R.id.nav_home) {
                return true;
            } else if (id == com.smarttodo.R.id.nav_add_task) {
                startActivity(new Intent(this, AddTaskActivity.class));
                return false;
            } else if (id == com.smarttodo.R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    /**
     * Setup search text change listener
     */
    private void setupSearch() {
        binding.searchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                taskViewModel.setSearchQuery(s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    /**
     * Build dynamic category filter chips
     */
    private void buildCategoryChips(List<Category> categories) {
        // Xóa chips cũ (trừ "Tất cả")
        int childCount = binding.chipGroupCategory.getChildCount();
        if (childCount > 1) {
            binding.chipGroupCategory.removeViews(1, childCount - 1);
        }

        // Thêm chip cho mỗi category
        for (Category cat : categories) {
            Chip chip = new Chip(this);
            chip.setText(cat.getName());
            chip.setCheckable(true);
            try {
                int color = Color.parseColor(cat.getColor());
                chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(color));
                chip.setChipStrokeWidth(1.5f);
                chip.setTextColor(color);
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                        Color.argb(30, Color.red(color), Color.green(color), Color.blue(color))));
            } catch (Exception e) {
                // Bỏ qua nếu màu không hợp lệ
            }

            chip.setOnClickListener(v -> {
                taskViewModel.setFilterCategory(cat.getCategoryId());
            });

            binding.chipGroupCategory.addView(chip);
        }
    }

    /**
     * Hiển thị trạng thái rỗng khi không có task
     */
    private void updateEmptyState(List<Task> tasks) {
        boolean isEmpty = tasks == null || tasks.isEmpty();
        binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewTasks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    /**
     * Load tên user cho greeting
     */
    private void loadUserGreeting() {
        com.google.firebase.auth.FirebaseUser user = FirebaseManager.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                binding.tvUserName.setText(displayName);
            }
        }
    }

    /**
     * Dialog xác nhận xóa task
     */
    private void showDeleteConfirmation(Task task) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa task")
                .setMessage("Bạn có chắc muốn xóa task \"" + task.getTitle() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    taskViewModel.deleteTask(task.getTaskId());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Hiển thị Filter Dialog
     */
    private void showFilterDialog() {
        String[] priorities = {"Tất cả", "Cao", "Trung bình", "Thấp"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Lọc theo độ ưu tiên")
                .setItems(priorities, (dialog, which) -> {
                    taskViewModel.setFilterPriority(which); // 0=all, 1=high, 2=medium, 3=low
                })
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
