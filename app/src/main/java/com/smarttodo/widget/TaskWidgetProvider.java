package com.smarttodo.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.smarttodo.R;
import com.smarttodo.activity.MainActivity;
import com.smarttodo.firebase.FirebaseManager;
import com.smarttodo.listener.OnTaskListener;
import com.smarttodo.model.Task;
import com.smarttodo.repository.TaskRepository;
import com.smarttodo.utils.DateUtils;

import java.util.List;

/**
 * TaskWidgetProvider - Widget hiển thị task hôm nay hoặc 3 task gần nhất.
 * Sử dụng RemoteViews để hiển thị nội dung trong widget.
 * Click vào widget mở MainActivity.
 * Nút Refresh làm mới dữ liệu widget.
 */
public class TaskWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_REFRESH = "com.smarttodo.WIDGET_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // Xử lý nút Refresh
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName widget = new ComponentName(context, TaskWidgetProvider.class);
            int[] appWidgetIds = manager.getAppWidgetIds(widget);
            onUpdate(context, manager, appWidgetIds);
        }
    }

    /**
     * Cập nhật nội dung widget
     */
    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task);

        // Click vào widget -> mở MainActivity
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPending = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, openAppPending);

        // Nút Refresh
        Intent refreshIntent = new Intent(context, TaskWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPending = PendingIntent.getBroadcast(
                context, 1, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_refresh, refreshPending);

        // Kiểm tra user đăng nhập
        if (!FirebaseManager.getInstance().isUserLoggedIn()) {
            views.setTextViewText(R.id.tv_widget_task1, "Chưa đăng nhập");
            views.setTextViewText(R.id.tv_widget_task2, "");
            views.setTextViewText(R.id.tv_widget_task3, "");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        // Load tasks từ Firestore
        TaskRepository repo = new TaskRepository();
        repo.getTodayTasks(new OnTaskListener.OnTasksLoaded() {
            @Override
            public void onSuccess(List<Task> tasks) {
                updateWidgetViews(context, appWidgetManager, appWidgetId, views, tasks);
            }

            @Override
            public void onFailure(String error) {
                views.setTextViewText(R.id.tv_widget_task1, "Lỗi tải dữ liệu");
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        });

        // Update ngay với trạng thái loading
        views.setTextViewText(R.id.tv_widget_task1, "Đang tải...");
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * Cập nhật hiển thị task trong widget
     */
    private void updateWidgetViews(Context context, AppWidgetManager manager,
                                   int appWidgetId, RemoteViews views, List<Task> tasks) {
        // Text IDs cho 3 task slots
        int[] taskTextIds = {R.id.tv_widget_task1, R.id.tv_widget_task2, R.id.tv_widget_task3};

        if (tasks == null || tasks.isEmpty()) {
            views.setTextViewText(R.id.tv_widget_task1, "🎉 Không có task hôm nay!");
            views.setTextViewText(R.id.tv_widget_task2, "");
            views.setTextViewText(R.id.tv_widget_task3, "");
        } else {
            for (int i = 0; i < taskTextIds.length; i++) {
                if (i < tasks.size()) {
                    Task task = tasks.get(i);
                    String displayText = (task.isCompleted() ? "✅ " : "⏳ ") + task.getTitle();
                    if (task.getDeadline() != null) {
                        displayText += "\n    " + DateUtils.formatTime(task.getDeadline());
                    }
                    views.setTextViewText(taskTextIds[i], displayText);
                } else {
                    views.setTextViewText(taskTextIds[i], "");
                }
            }
        }

        manager.updateAppWidget(appWidgetId, views);
    }
}
