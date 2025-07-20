package com.example.tradeup.Controller;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserController {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Context context;

    public UserController(Context context) {
        this.context = context;
    }

    // 1. Đăng ký tài khoản mới với role = "user"
    public void registerUser(String username, String email, String phone, String password, OnCompleteListener<Void> listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            firebaseUser.sendEmailVerification();

                            String userId = firebaseUser.getUid();
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("uid", userId);
                            userMap.put("username", username);
                            userMap.put("email", email);
                            userMap.put("phoneNumber", phone);
                            userMap.put("role", "user");
                            userMap.put("avatar", "");
                            userMap.put("displayName", "");
                            userMap.put("bio", "");
                            userMap.put("contactInfo", "");
                            userMap.put("isActive", true); // Mặc định là đang hoạt động

                            db.collection("users").document(userId)
                                    .set(userMap)
                                    .addOnCompleteListener(listener);
                        }
                    } else {
                        listener.onComplete(Tasks.forException(task.getException()));
                    }
                });
    }

    // 2. Cập nhật vai trò người dùng
    public void updateUserRole(String userId, String newRole, OnCompleteListener<Void> listener) {
        db.collection("users").document(userId)
                .update("role", newRole)
                .addOnCompleteListener(listener);
    }

    // 3. Cập nhật thông tin hồ sơ, nếu đổi email thì xử lý cả FirebaseAuth
    public void updateUserWithEmailCheck(String userId, String newEmail, Map<String, Object> updates, OnCompleteListener<Void> listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("Người dùng chưa đăng nhập")));
            return;
        }

        String currentEmail = user.getEmail();
        if (!currentEmail.equals(newEmail)) {
            user.updateEmail(newEmail)
                    .addOnSuccessListener(unused -> {
                        // Gửi email xác thực từ Firebase sau khi đổi email thành công
                        user.sendEmailVerification();
                        updates.put("email", newEmail);

                        db.collection("users").document(userId)
                                .update(updates)
                                .addOnCompleteListener(listener);
                    })
                    .addOnFailureListener(e -> listener.onComplete(Tasks.forException(e)));
        } else {
            // Email không thay đổi, chỉ cập nhật thông tin khác
            db.collection("users").document(userId)
                    .update(updates)
                    .addOnCompleteListener(listener);
        }
    }

    // 4. Cập nhật avatar từ URL
    public void updateUserAvatar(String userId, String imageUrl, OnCompleteListener<Void> listener) {
        db.collection("users").document(userId)
                .update("avatar", imageUrl)
                .addOnCompleteListener(listener);
    }

    // 5. Upload ảnh avatar lên Cloudinary
    public void uploadAvatarImage(Uri imageUri, String userId, OnCompleteListener<Uri> listener) {
        AddImgController addImgController = new AddImgController(context);
        addImgController.uploadImageToCloudinary(imageUri, userId, uploadTask -> {
            if (uploadTask.isSuccessful()) {
                Uri downloadUri = uploadTask.getResult();
                listener.onComplete(Tasks.forResult(downloadUri));
            } else {
                listener.onComplete(Tasks.forException(uploadTask.getException()));
            }
        });
    }

    // 6. Xóa tài khoản: giữ lại userId và đặt isActive = false
    public void deactivateUserAccount(OnCompleteListener<Void> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onComplete(Tasks.forException(new Exception("Người dùng chưa đăng nhập")));
            return;
        }

        String userId = user.getUid();

        // Cập nhật trạng thái tài khoản trong Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("isActive", false);
        updates.put("deletedAt", Timestamp.now());

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    // Sau khi cập nhật Firestore, xóa khỏi Authentication
                    user.delete()
                            .addOnSuccessListener(aVoid -> listener.onComplete(Tasks.forResult(null)))
                            .addOnFailureListener(e -> listener.onComplete(Tasks.forException(e)));
                })
                .addOnFailureListener(e -> listener.onComplete(Tasks.forException(e)));
    }
}
