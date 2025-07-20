package com.example.tradeup.View;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup.R;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    EditText edtUserName, edtEmail, edtPhoneNumber, edtPassWord;
    Button btnRegister;
    ProgressBar progressBarRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        addControl();
        addEvent();
    }

    private void addControl() {
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

            if (TextUtils.isEmpty(username)) {
                edtUserName.setError("Vui lòng nhập tên người dùng");
                return;
            }
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("Email không hợp lệ");
                return;
            }
            if (TextUtils.isEmpty(phone) || !phone.matches("\\d{10}")) {
                edtPhoneNumber.setError("Số điện thoại phải có 10 chữ số");
                return;
            }
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                edtPassWord.setError("Mật khẩu ít nhất 6 ký tự");
                return;
            }

            progressBarRegister.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);

            // ✅ Kiểm tra email đã đăng ký chưa
            FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                    .addOnCompleteListener(task -> {
                        progressBarRegister.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);

                        if (task.isSuccessful()) {
                            boolean emailUsed = !task.getResult().getSignInMethods().isEmpty();
                            if (emailUsed) {
                                edtEmail.setError("Email đã được đăng ký");
                                Toast.makeText(this, "Email đã tồn tại. Vui lòng chọn email khác.", Toast.LENGTH_SHORT).show();
                            } else {
                                // Chuyển sang màn hình OTP
                                Intent intent = new Intent(RegisterActivity.this, OtpEmailActivity.class);
                                intent.putExtra("username", username);
                                intent.putExtra("email", email);
                                intent.putExtra("phone", phone);
                                intent.putExtra("password", password);
                                startActivity(intent);
                            }
                        } else {
                            Toast.makeText(this, "Lỗi kiểm tra email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
