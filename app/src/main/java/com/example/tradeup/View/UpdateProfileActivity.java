package com.example.tradeup.View;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.tradeup.Controller.UserController;
import com.example.tradeup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int EMAIL_VERIFY_CHECK_INTERVAL = 3000; // ms

    ImageView imgAvatar;
    EditText edtUsername, edtDisplayName, edtEmail, edtPhone, edtBio, edtContactInfo;
    Button btnChooseImage, btnSaveProfile;
    Uri selectedImageUri;
    String currentAvatarUrl = null;

    FirebaseAuth auth;
    FirebaseFirestore db;
    ProgressDialog dialog;

    UserController userController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userController = new UserController(this);

        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage("Đang xử lý...");

        checkImagePermission();
        addControl();
        addEvent();
        loadUserData();
    }

    private void checkImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 101);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            }
        }
    }

    private void addControl() {
        imgAvatar = findViewById(R.id.imgAvatar);
        edtUsername = findViewById(R.id.edtUsername);
        edtDisplayName = findViewById(R.id.edtDisplayName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtBio = findViewById(R.id.edtBio);
        edtContactInfo = findViewById(R.id.edtContactInfo);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
    }

    private void addEvent() {
        btnChooseImage.setOnClickListener(v -> chooseImage());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            imgAvatar.setImageURI(selectedImageUri);
        } else {
            Toast.makeText(this, "Không thể chọn ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        dialog.show();
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(document -> {
                    dialog.dismiss();
                    if (document.exists()) {
                        edtUsername.setText(document.getString("username"));
                        edtDisplayName.setText(document.getString("displayName"));
                        edtEmail.setText(document.getString("email"));
                        edtPhone.setText(document.getString("phoneNumber"));
                        edtBio.setText(document.getString("bio"));
                        edtContactInfo.setText(document.getString("contactInfo"));

                        String avatarUrl = document.getString("avatar");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            currentAvatarUrl = avatarUrl;
                            Glide.with(this).load(avatarUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .into(imgAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        dialog.setMessage("Đang lưu thông tin...");
        dialog.show();

        String newEmail = edtEmail.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", edtUsername.getText().toString().trim());
        updates.put("displayName", edtDisplayName.getText().toString().trim());
        updates.put("phoneNumber", edtPhone.getText().toString().trim());
        updates.put("bio", edtBio.getText().toString().trim());
        updates.put("contactInfo", edtContactInfo.getText().toString().trim());

        // Upload avatar nếu thay đổi
        if (selectedImageUri != null) {
            userController.uploadAvatarImage(selectedImageUri, user.getUid(), task -> {
                if (task.isSuccessful()) {
                    String avatarUrl = task.getResult().toString();
                    updates.put("avatar", avatarUrl);
                    processEmailAndUpdate(user, newEmail, updates);
                } else {
                    dialog.dismiss();
                    Toast.makeText(this, "Lỗi upload ảnh: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            updates.put("avatar", currentAvatarUrl != null ? currentAvatarUrl : "");
            processEmailAndUpdate(user, newEmail, updates);
        }
    }

    private void processEmailAndUpdate(FirebaseUser user, String newEmail, Map<String, Object> updates) {
        String currentEmail = user.getEmail();
        if (!currentEmail.equals(newEmail)) {
            // Nếu đổi email
            user.updateEmail(newEmail)
                    .addOnSuccessListener(unused -> {
                        user.sendEmailVerification();
                        showWaitVerifyDialog(user, newEmail, updates);
                    })
                    .addOnFailureListener(e -> {
                        dialog.dismiss();
                        Toast.makeText(this, "Lỗi đổi email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Nếu không đổi email
            updates.put("email", newEmail);
            updateUserFirestore(user.getUid(), updates);
        }
    }

    private void showWaitVerifyDialog(FirebaseUser user, String email, Map<String, Object> updates) {
        dialog.setMessage("Đã gửi email xác thực. Vui lòng xác minh...");
        dialog.show();

        // Bắt đầu kiểm tra định kỳ xem email đã xác minh chưa
        new Thread(() -> {
            AtomicBoolean verified = new AtomicBoolean(false);
            while (!verified.get()) {
                try {
                    Thread.sleep(EMAIL_VERIFY_CHECK_INTERVAL);
                    user.reload().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && user.isEmailVerified()) {
                            verified.set(true);
                            runOnUiThread(() -> {
                                updates.put("email", email);
                                updateUserFirestore(user.getUid(), updates);
                            });
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void updateUserFirestore(String uid, Map<String, Object> updates) {
        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(UpdateProfileActivity.this, ProfileActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
