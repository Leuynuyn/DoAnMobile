package com.example.tradeup;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    EditText edtUserName, edtEmail, edtPhoneNumber, edtPassWord;
    Button btnRegister;
    ProgressBar progressBarRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        addControl();
        addEvent();
    }

    private void addControl() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        edtUserName = findViewById(R.id.edtUserName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        edtPassWord = findViewById(R.id.edtPassWord);
        btnRegister = findViewById(R.id.btnRegister);
        progressBarRegister = findViewById(R.id.progressBarRegister);
    }

    private void addEvent() {
        btnRegister.setOnClickListener(v -> {
            String username = edtUserName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String phone = edtPhoneNumber.getText().toString().trim();
            String password = edtPassWord.getText().toString().trim();

            // Kiểm tra input
            if (TextUtils.isEmpty(username)) {
                edtUserName.setError("Vui lòng nhập tên người dùng");
                return;
            }
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("Vui lòng nhập email hợp lệ");
                return;
            }
            if (TextUtils.isEmpty(phone) || !phone.matches("\\d{10}")) {
                edtPhoneNumber.setError("Vui lòng nhập số điện thoại 10 số");
                return;
            }
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                edtPassWord.setError("Mật khẩu phải có ít nhất 6 ký tự");
                return;
            }

            progressBarRegister.setVisibility(View.VISIBLE);
            // Kiểm tra email đã tồn tại
            mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().getSignInMethods().size() > 0) {
                    progressBarRegister.setVisibility(View.GONE);
                    Toast.makeText(RegisterActivity.this, "Email đã được đăng ký", Toast.LENGTH_SHORT).show();
                } else {
                    // Chuyển sang màn hình OTP
                    Intent intent = new Intent(RegisterActivity.this, OtpEmailActivity.class);
                    intent.putExtra("username", username);
                    intent.putExtra("email", email);
                    intent.putExtra("phone", phone);
                    intent.putExtra("password", password);
                    startActivity(intent);
                    progressBarRegister.setVisibility(View.GONE);
                }
            });
        });
    }
}