package com.smarttodo.repository;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smarttodo.firebase.FirebaseManager;
import com.smarttodo.listener.OnTaskListener;
import com.smarttodo.model.Task;
import com.smarttodo.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository xử lý tất cả CRUD operations cho Task trên Firestore.
 * Sử dụng LiveData để tự động cập nhật UI khi dữ liệu thay đổi (real-time sync).
 */
public class TaskRepository {

    private final FirebaseFirestore db;

    public TaskRepository() {
        this.db = FirebaseManager.getInstance().getFirestore();
    }

    private String getUserId() {
        return FirebaseManager.getInstance().getCurrentUserId();
    }

    /**
     * Lấy tất cả task của user, lắng nghe real-time updates.
     * Sắp xếp Java in-memory để không bị lỗi thiếu Index trên Firestore Console.
     */
    public void getAllTasks(MutableLiveData<List<Task>> liveData) {
        String uid = getUserId();
        if (uid == null) return;

        db.collection(Constants.COLLECTION_TASKS)
                .whereEqualTo(Constants.FIELD_USER_ID, uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("TaskRepository", "Snapshot error: ", error);
                        return;
                    }
                    if (snapshots == null) return;

                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        try {
                            Task task = doc.toObject(Task.class);
                            if (task != null) {
                                task.setTaskId(doc.getId());
                                tasks.add(task);
                            }
                        } catch (Exception e) {
                            Log.e("TaskRepository", "Error parsing task: ", e);
                        }
                    }

                    // Sắp xếp theo ngày tạo mới nhất (descending) trực tiếp trong RAM
                    Collections.sort(tasks, (t1, t2) -> {
                        if (t1.getCreatedAt() == null && t2.getCreatedAt() == null) return 0;
                        if (t1.getCreatedAt() == null) return 1;
                        if (t2.getCreatedAt() == null) return -1;
                        return t2.getCreatedAt().compareTo(t1.getCreatedAt());
                    });

                    liveData.postValue(tasks);
                });
    }

    /**
     * Thêm task mới
     */
    public void addTask(Task task, OnTaskListener.OnTaskOperationComplete listener) {
        String uid = getUserId();
        if (uid == null) {
            listener.onFailure("Chưa đăng nhập");
            return;
        }
        task.setUserId(uid);

        if (task.getTaskId() == null || task.getTaskId().isEmpty()) {
            String id = db.collection(Constants.COLLECTION_TASKS).document().getId();
            task.setTaskId(id);
        }

        db.collection(Constants.COLLECTION_TASKS)
                .document(task.getTaskId())
                .set(task)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    /**
     * Cập nhật task
     */
    public void updateTask(Task task, OnTaskListener.OnTaskOperationComplete listener) {
        if (task.getTaskId() == null) {
            listener.onFailure("Task ID không hợp lệ");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("title",        task.getTitle());
        updates.put("description",  task.getDescription());
        updates.put("categoryId",   task.getCategoryId());
        updates.put("priority",     task.getPriority());
        updates.put("imageUrl",     task.getImageUrl());
        updates.put("deadline",     task.getDeadline());
        updates.put("reminderTime", task.getReminderTime());
        updates.put("completed",    task.isCompleted());
        updates.put(Constants.FIELD_UPDATED_AT, com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection(Constants.COLLECTION_TASKS)
                .document(task.getTaskId())
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    /**
     * Xóa task
     */
    public void deleteTask(String taskId, OnTaskListener.OnTaskOperationComplete listener) {
        db.collection(Constants.COLLECTION_TASKS)
                .document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    /**
     * Đánh dấu task hoàn thành / chưa hoàn thành
     */
    public void toggleTaskComplete(String taskId, boolean completed,
                                   OnTaskListener.OnTaskOperationComplete listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("completed", completed);
        updates.put(Constants.FIELD_UPDATED_AT, com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection(Constants.COLLECTION_TASKS)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    /**
     * Lấy task theo ID
     */
    public void getTaskById(String taskId, OnTaskListener.OnTaskLoaded listener) {
        db.collection(Constants.COLLECTION_TASKS)
                .document(taskId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Task task = snapshot.toObject(Task.class);
                        if (task != null) task.setTaskId(snapshot.getId());
                        listener.onSuccess(task);
                    } else {
                        listener.onFailure("Task không tồn tại");
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    /**
     * Lấy task trong ngày hôm nay (dùng cho Widget)
     */
    public void getTodayTasks(OnTaskListener.OnTasksLoaded listener) {
        String uid = getUserId();
        if (uid == null) return;

        Date startOfDay = com.smarttodo.utils.DateUtils.getStartOfDay(null);
        Date endOfDay   = com.smarttodo.utils.DateUtils.getEndOfDay(null);

        db.collection(Constants.COLLECTION_TASKS)
                .whereEqualTo(Constants.FIELD_USER_ID, uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        try {
                            Task task = doc.toObject(Task.class);
                            if (task != null && task.getDeadline() != null) {
                                if (task.getDeadline().compareTo(startOfDay) >= 0 && task.getDeadline().compareTo(endOfDay) <= 0) {
                                    task.setTaskId(doc.getId());
                                    tasks.add(task);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    listener.onSuccess(tasks);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    /**
     * Lấy 3 task gần nhất theo deadline (dùng cho Widget)
     */
    public void getUpcomingTasks(OnTaskListener.OnTasksLoaded listener) {
        String uid = getUserId();
        if (uid == null) return;

        db.collection(Constants.COLLECTION_TASKS)
                .whereEqualTo(Constants.FIELD_USER_ID, uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        try {
                            Task task = doc.toObject(Task.class);
                            if (task != null && !task.isCompleted()) {
                                task.setTaskId(doc.getId());
                                tasks.add(task);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Collections.sort(tasks, (t1, t2) -> {
                        if (t1.getDeadline() == null && t2.getDeadline() == null) return 0;
                        if (t1.getDeadline() == null) return 1;
                        if (t2.getDeadline() == null) return -1;
                        return t1.getDeadline().compareTo(t2.getDeadline());
                    });
                    if (tasks.size() > 3) tasks = tasks.subList(0, 3);
                    listener.onSuccess(tasks);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    /**
     * Lấy task có reminder cần đặt lại sau reboot
     */
    public void getTasksWithReminder(OnTaskListener.OnTasksLoaded listener) {
        String uid = getUserId();
        if (uid == null) return;

        db.collection(Constants.COLLECTION_TASKS)
                .whereEqualTo(Constants.FIELD_USER_ID, uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        try {
                            Task task = doc.toObject(Task.class);
                            if (task != null && !task.isCompleted() && task.getReminderTime() != null) {
                                if (task.getReminderTime().after(new Date())) {
                                    task.setTaskId(doc.getId());
                                    tasks.add(task);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    listener.onSuccess(tasks);
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }
}
