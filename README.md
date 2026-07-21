# 📱 SMART TODO - Android Native App

> Ứng dụng quản lý công việc thông minh được xây dựng bằng **Java + XML**, kiến trúc **MVVM**, tích hợp **Firebase** và **Material Design 3**.

---

## 🚀 Tính năng

| Module | Tính năng |
|--------|-----------|
| 🔐 Auth | Đăng ký, Đăng nhập, Quên mật khẩu, Auto-login |
| 📋 Task | CRUD đầy đủ, Upload ảnh (Camera/Gallery), Priority, Deadline |
| 📁 Category | CRUD Category với color picker |
| 🔔 Reminder | AlarmManager + BroadcastReceiver, tự đặt lại sau reboot |
| 📊 Statistics | Pie Chart (category), Bar Chart (monthly), MPAndroidChart |
| 🖼️ Widget | Home screen widget, task hôm nay, nút Refresh |
| 👤 Profile | Xem thông tin, đổi avatar, thống kê nhanh |
| 🔄 Multi-device | Firestore real-time sync tự động |

---

## ⚙️ Cấu hình Firebase

### Bước 1: Tạo Project Firebase

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** → Đặt tên `SmartTodo`
3. Hoàn thành các bước setup

### Bước 2: Thêm Android App

1. Trong Firebase Console → **"Add app"** → Chọn Android
2. **Package name**: `com.smarttodo`
3. **App nickname**: `Smart TODO`
4. Click **"Register app"**

### Bước 3: Lấy SHA-1

Mở terminal trong thư mục project và chạy:

```powershell
# Windows
cd C:\Users\OS\StudioProjects\todolist
.\gradlew signingReport
```

Tìm dòng `SHA1:` trong output và copy giá trị đó.

Thêm SHA-1 vào Firebase Console:
- Project Settings → Your apps → Add fingerprint

### Bước 4: Download google-services.json

1. Trong Firebase Console → Project Settings → Download `google-services.json`
2. Copy file vào thư mục: `app/google-services.json`

```
todolist/
├── app/
│   ├── google-services.json  ← Đặt file ở đây
│   └── src/
```

### Bước 5: Kích hoạt Firebase Services

#### Authentication
- Firebase Console → **Authentication** → **Sign-in method**
- Enable **Email/Password**

#### Firestore Database
- Firebase Console → **Firestore Database** → **Create database**
- Chọn mode: **Start in test mode** (cho development)
- Chọn location gần nhất

**Firestore Security Rules (cho production):**
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users: chỉ đọc/ghi data của chính mình
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    // Tasks
    match /tasks/{taskId} {
      allow read, write: if request.auth != null
        && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null
        && request.auth.uid == request.resource.data.userId;
    }
    // Categories
    match /categories/{categoryId} {
      allow read, write: if request.auth != null
        && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null
        && request.auth.uid == request.resource.data.userId;
    }
  }
}
```

#### Firebase Storage
- Đã được gỡ bỏ và thay thế bằng **Cloudinary** để hỗ trợ sử dụng hoàn toàn miễn phí mà không cần nhập thẻ Visa.

### Bước 6: Cấu hình Cloudinary (Lưu trữ ảnh)

1. Đăng ký tài khoản miễn phí tại [cloudinary.com](https://cloudinary.com/)
2. Lấy **Cloud Name** trên Dashboard.
3. Vào **Settings (⚙️) → Upload → Add upload preset**.
4. Đổi **Signing Mode** thành **Unsigned** và lưu lại.
5. Cập nhật Cloud Name và Upload Preset vào code:
   - Thay Cloud Name trong `app/src/main/java/com/smarttodo/SmartTodoApplication.java`
   - Thay Upload Preset trong `app/src/main/java/com/smarttodo/repository/StorageRepository.java`

---

## 🏃‍♂️ Chạy Project

### Yêu cầu
- Android Studio Hedgehog hoặc mới hơn
- JDK 11+
- Android SDK 26+
- Internet connection

### Các bước

```bash
# 1. Clone/mở project trong Android Studio
# File → Open → Chọn thư mục todolist

# 2. Đảm bảo đã có google-services.json trong app/

# 3. Sync Gradle (Android Studio tự động hoặc click Sync Now)

# 4. Chạy app
# Run → Run 'app' hoặc Shift+F10
```

---

## 📦 Build APK

```bash
# Debug APK
.\gradlew assembleDebug

# Release APK (cần keystore)
.\gradlew assembleRelease
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📁 Cấu trúc Project

```
app/src/main/java/com/smarttodo/
├── activity/
│   ├── SplashActivity.java        # Màn hình splash
│   ├── LoginActivity.java         # Đăng nhập
│   ├── RegisterActivity.java      # Đăng ký
│   ├── ForgotPasswordActivity.java
│   ├── MainActivity.java          # Danh sách task chính
│   ├── AddTaskActivity.java       # Thêm task
│   ├── EditTaskActivity.java      # Sửa task
│   ├── TaskDetailActivity.java    # Chi tiết task
│   ├── CategoryActivity.java      # Quản lý category
│   ├── ProfileActivity.java       # Hồ sơ user
│   └── StatisticsActivity.java    # Thống kê
│
├── adapter/
│   ├── TaskAdapter.java           # RecyclerView adapter cho task
│   └── CategoryAdapter.java       # RecyclerView adapter cho category
│
├── model/
│   ├── User.java                  # Model user
│   ├── Task.java                  # Model task
│   └── Category.java              # Model category
│
├── repository/
│   ├── AuthRepository.java        # Firebase Auth operations
│   ├── TaskRepository.java        # Firestore task CRUD
│   ├── CategoryRepository.java    # Firestore category CRUD
│   ├── StorageRepository.java     # Firebase Storage upload
│   └── UserRepository.java        # User profile operations
│
├── viewmodel/
│   ├── AuthViewModel.java         # Auth state & operations
│   ├── TaskViewModel.java         # Task list, filter, CRUD
│   ├── CategoryViewModel.java     # Category management
│   ├── ProfileViewModel.java      # User profile
│   └── StatisticsViewModel.java   # Statistics computation
│
├── firebase/
│   └── FirebaseManager.java       # Singleton Firebase access
│
├── notification/
│   └── NotificationHelper.java    # Alarm & notification management
│
├── receiver/
│   ├── AlarmReceiver.java         # BroadcastReceiver cho alarm
│   └── BootReceiver.java          # Re-schedule alarms after boot
│
├── widget/
│   └── TaskWidgetProvider.java    # Home screen widget
│
├── listener/
│   ├── OnCompleteListener.java    # Generic callback
│   ├── OnTaskListener.java        # Task callbacks
│   └── OnCategoryListener.java    # Category callbacks
│
└── utils/
    ├── Constants.java             # App constants
    ├── DateUtils.java             # Date formatting & comparison
    ├── ValidationUtils.java       # Input validation
    └── PreferenceManager.java     # SharedPreferences wrapper
```

---

## 📚 Dependencies

```kotlin
// Firebase
firebase-bom:33.7.0
firebase-auth
firebase-firestore
firebase-analytics

// Cloudinary
cloudinary-android:2.5.0

// Navigation
navigation-fragment:2.7.7
navigation-ui:2.7.7

// Lifecycle
lifecycle-viewmodel:2.8.7
lifecycle-livedata:2.8.7

// UI
material:1.12.x
recyclerview:1.3.2
cardview:1.0.0
swiperefreshlayout:1.1.0
circleimageview:3.1.0

// Image
glide:4.16.0

// Charts
MPAndroidChart:v3.1.0

// Background
work-runtime:2.9.1
```

---

## 🔥 Firestore Collections

```
users/
  {uid}/
    name: String
    email: String
    avatar: String (URL)
    createdAt: Timestamp

categories/
  {categoryId}/
    userId: String
    name: String
    color: String (#hex)
    createdAt: Timestamp

tasks/
  {taskId}/
    userId: String
    title: String
    description: String
    categoryId: String
    priority: Int (1=High, 2=Medium, 3=Low)
    imageUrl: String (nullable)
    deadline: Timestamp (nullable)
    reminderTime: Timestamp (nullable)
    completed: Boolean
    createdAt: Timestamp
    updatedAt: Timestamp
```

---

## ⚡ Kiến trúc MVVM

```
View (Activity/XML)
    ↕ observe LiveData
ViewModel
    ↕ calls
Repository
    ↕ reads/writes
Firebase (Firestore/Auth/Storage)
```

---

## 📝 Ghi chú quan trọng

> [!IMPORTANT]
> 1. **google-services.json**: Phải có file này trong `app/` trước khi build
> 2. **Internet**: App yêu cầu kết nối internet để sử dụng Firebase
> 3. **Test mode**: Firestore Rules mặc định là test mode - hãy cập nhật trước khi production

> [!WARNING]
> Permissions camera trên Android 13+ cần request runtime permission.
> App đã xử lý nhưng cần test trên thiết bị thật.

---

## 🎨 Design System

- **Primary Color**: `#4CAF50` (Green)
- **Background**: `#121212` (Dark)
- **Surface**: `#1E1E1E`
- **Card**: `#252525`
- **Font**: Inter (system default fallback)
- **Priority**: Red (High), Orange (Medium), Green (Low)

---

*Được xây dựng bởi Smart TODO Team - 2024*
