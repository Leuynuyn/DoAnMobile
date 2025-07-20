package com.example.tradeup.View;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    ImageView imgAvatar;
    TextView txtUsername, txtEmail, txtPhone, txtBio, txtDisplayName, txtContactInfo;
    Button btnChange;

    FirebaseAuth auth;
    FirebaseFirestore db;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải thông tin...");
        progressDialog.setCancelable(false);

        addControl();
        addEvent();
        loadUserData();

    }

    private void addControl() {
        txtUsername = findViewById(R.id.txtUsername);
        txtEmail = findViewById(R.id.txtEmail);
        txtPhone = findViewById(R.id.txtPhone);
        txtDisplayName = findViewById(R.id.txtDisplayName);
        txtContactInfo = findViewById(R.id.txtContactInfo);
        txtBio = findViewById(R.id.txtBio);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnChange = findViewById(R.id.btnChange);
    }

    private void addEvent() {
        btnChange.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, UpdateProfileActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        progressDialog.show();

        String uid = currentUser.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    progressDialog.dismiss();
                    if (document.exists()) {
                        txtUsername.setText(document.getString("username"));
                        txtEmail.setText(document.getString("email"));
                        txtPhone.setText(document.getString("phoneNumber"));
                        txtDisplayName.setText(document.getString("displayName") != null ? document.getString("displayName") : "Chưa có");
                        txtContactInfo.setText(document.getString("contactInfo") != null ? document.getString("contactInfo") : "Chưa có");
                        txtBio.setText(document.getString("bio") != null ? document.getString("bio") : "Chưa có");

                        String avatarUrl = document.getString("avatar");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .into(imgAvatar);
                        } else {
                            imgAvatar.setImageResource(R.drawable.default_avatar);
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tự động reload khi quay lại màn hình
        loadUserData();
    }
}
