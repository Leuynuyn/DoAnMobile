package com.example.tradeup.View;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup.Controller.SendGridEmailSender;
import com.example.tradeup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class OtpEmailActivity extends AppCompatActivity {

    EditText edtNumberOne, edtNumberTwo, edtNumberThree, edtNumberFour, edtNumberFive, edtNumberSix;
    Button btnVerifyEmail;
    TextView tvEmail, tvResend;

    String username, email, phone, password, otp;
    long otpTimestamp;
    CountDownTimer resendTimer;

    final long OTP_VALIDITY_DURATION = 120_000; // 2 phút

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_email);
        addControl();
        addEvent();
        sendOtp();
    }

    private void addControl() {
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
        tvResend.setEnabled(false);
    }

    private void addEvent() {
        btnVerifyEmail.setOnClickListener(v -> verifyOtp());
        tvResend.setOnClickListener(v -> sendOtp());
    }

    private void sendOtp() {
        otp = String.valueOf(new Random().nextInt(900000) + 100000);
        otpTimestamp = System.currentTimeMillis();

        String subject = "Mã xác minh TradeUp";
        String message = "Mã OTP của bạn là: " + otp;

        SendGridEmailSender.sendEmail(this, email, subject, message, new SendGridEmailSender.EmailCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(OtpEmailActivity.this, "OTP đã gửi", Toast.LENGTH_SHORT).show();
                startResendCountdown();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(OtpEmailActivity.this, "Lỗi gửi OTP: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startResendCountdown() {
        tvResend.setEnabled(false);
        resendTimer = new CountDownTimer(30_000, 1_000) {
            public void onTick(long millisUntilFinished) {
                tvResend.setText("Gửi lại mã sau " + (millisUntilFinished / 1000) + "s");
            }

            public void onFinish() {
                tvResend.setText("Gửi lại mã");
                tvResend.setEnabled(true);
            }
        }.start();
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

        if (System.currentTimeMillis() - otpTimestamp > OTP_VALIDITY_DURATION) {
            Toast.makeText(this, "OTP đã hết hạn", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!inputOtp.equals(otp)) {
            Toast.makeText(this, "Mã OTP không chính xác", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = auth.getCurrentUser().getUid();
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("userId", userId);
                    userMap.put("username", username);
                    userMap.put("email", email);
                    userMap.put("phoneNumber", phone);
                    userMap.put("role", "customer");
                    userMap.put("avatar", "");
                    userMap.put("displayName", "");
                    userMap.put("bio", "");
                    userMap.put("contactInfo", "");

                    db.collection("users").document(userId).set(userMap)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(OtpEmailActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(OtpEmailActivity.this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(OtpEmailActivity.this, "Lỗi lưu thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e -> {
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("email address is already in use")) {
                        Toast.makeText(OtpEmailActivity.this, "Email đã được đăng ký. Vui lòng dùng email khác.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(OtpEmailActivity.this, "Lỗi tạo tài khoản: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (resendTimer != null) resendTimer.cancel();
        super.onDestroy();
    }
}
