package com.smarttodo.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Model class đại diện cho một Task trong ứng dụng.
 * Lưu trữ trên Firestore collection "tasks".
 * Priority: 1=High, 2=Medium, 3=Low
 */
public class Task {

    // Priority constants
    public static final int PRIORITY_HIGH   = 1;
    public static final int PRIORITY_MEDIUM = 2;
    public static final int PRIORITY_LOW    = 3;

    @DocumentId
    private String taskId;

    private String userId;
    private String title;
    private String description;
    private String categoryId;
    private int priority;       // 1=High, 2=Medium, 3=Low
    private String imageUrl;    // URL ảnh từ Firebase Storage (có thể null)
    private Date deadline;      // Hạn chót
    private Date reminderTime;  // Thời gian nhắc nhở (có thể null)
    private boolean completed;

    @ServerTimestamp
    private Date createdAt;
    
    @ServerTimestamp
    private Date updatedAt;

    // Constructor rỗng bắt buộc cho Firestore deserialization
    public Task() {}

    public Task(String userId, String title, String description,
                String categoryId, int priority, Date deadline) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.priority = priority;
        this.deadline = deadline;
        this.completed = false;
    }

    // ========== Getters & Setters ==========

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public Date getReminderTime() { return reminderTime; }
    public void setReminderTime(Date reminderTime) { this.reminderTime = reminderTime; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Kiểm tra task có bị quá hạn không
     */
    public boolean isOverdue() {
        if (deadline == null || completed) return false;
        return deadline.before(new Date());
    }

    /**
     * Lấy tên priority dạng String
     */
    public String getPriorityName() {
        switch (priority) {
            case PRIORITY_HIGH:   return "Cao";
            case PRIORITY_MEDIUM: return "Trung bình";
            case PRIORITY_LOW:    return "Thấp";
            default:              return "Không xác định";
        }
    }
}
