package com.example.tradeup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileSettingActivity extends AppCompatActivity {
    TextView txtUsername, txtEmail, txtPhone;
    Button btnLogout;
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_setting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        addControl();
        loadUserData();
        addEvent();
    }

    private void addControl() {
        txtUsername = findViewById(R.id.txtUsername);
        txtEmail = findViewById(R.id.txtEmail);
        txtPhone = findViewById(R.id.txtPhone);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadUserData() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        txtUsername.setText(document.getString("username"));
                        txtEmail.setText(document.getString("email"));
                        txtPhone.setText(document.getString("phone"));
                    }
                })
                .addOnFailureListener(e -> {
                    txtUsername.setText("Lỗi tải dữ liệu");
                });
    }

    private void addEvent() {
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(ProfileSettingActivity.this, LoginActivity.class));
            finish();
        });
    }
}