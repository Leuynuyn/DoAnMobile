package com.example.tradeup;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class OtpEmailActivity extends AppCompatActivity {
    EditText edtNumberOne, edtNumberTwo, edtNumberThree, edtNumberFour, edtNumberFive, edtNumberSix;
    Button btnVerifyEmail;
    TextView tvEmail, tvResend;
    String username, email, phone, password, otp;
    FirebaseAuth auth;
    FirebaseFirestore db;
    long otpTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_email);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        addControl();
        addEvent();
        sendOtp();
    }

    private void addControl() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        edtNumberOne = findViewById(R.id.edtNumberOne);
        edtNumberTwo = findViewById(R.id.edtNumberTwo);
        edtNumberThree = findViewById(R.id.edtNumberThree);
        edtNumberFour = findViewById(R.id.edtNumberFour);
        edtNumberFive = findViewById(R.id.edtNumberFive);
        edtNumberSix = findViewById(R.id.edtNumberSix);
        btnVerifyEmail = findViewById(R.id.btnVerifyEmail);
        tvEmail = findViewById(R.id.tvEmail);
        tvResend = findViewById(R.id.tvResend);

        username = getIntent().getStringExtra("username");
        email = getIntent().getStringExtra("email");
        phone = getIntent().getStringExtra("phone");
        password = getIntent().getStringExtra("password");
        tvEmail.setText(email);
    }

    private void addEvent() {
        btnVerifyEmail.setOnClickListener(v -> verifyOtp());
        tvResend.setOnClickListener(v -> {
            if (System.currentTimeMillis() - otpTimestamp < 30_000) {
                Toast.makeText(this, "Vui lòng đợi 30 giây trước khi gửi lại", Toast.LENGTH_SHORT).show();
                return;
            }
            sendOtp();
        });
    }

    private void sendOtp() {
        otp = String.valueOf(new Random().nextInt(900000) + 100000);
        otpTimestamp = System.currentTimeMillis();
        String subject = "OTP Verification - TradeUp App";
        String message = "Your OTP is: " + otp;

        SendGridEmailSender.sendEmail(this, email, subject, message, new SendGridEmailSender.EmailCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(OtpEmailActivity.this, "OTP đã gửi tới " + email, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(OtpEmailActivity.this, "Lỗi gửi OTP: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOtp() {
        String inputOtp = edtNumberOne.getText().toString() +
                edtNumberTwo.getText().toString() +
                edtNumberThree.getText().toString() +
                edtNumberFour.getText().toString() +
                edtNumberFive.getText().toString() +
                edtNumberSix.getText().toString();

        if (TextUtils.isEmpty(inputOtp) || inputOtp.length() != 6) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ 6 số OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (System.currentTimeMillis() - otpTimestamp > 300_000) { // OTP hết hạn sau 5 phút
            Toast.makeText(this, "OTP đã hết hạn, vui lòng gửi lại", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!inputOtp.equals(otp)) {
            Toast.makeText(this, "OTP không đúng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo tài khoản Firebase
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = auth.getCurrentUser().getUid();
                Map<String, Object> user = new HashMap<>();
                user.put("username", username);
                user.put("email", email);
                user.put("phone", phone);

                // Lưu vào Firestore
                db.collection("users").document(uid).set(user)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(OtpEmailActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(OtpEmailActivity.this, ProfileSettingActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(OtpEmailActivity.this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(OtpEmailActivity.this, "Lỗi đăng ký: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}