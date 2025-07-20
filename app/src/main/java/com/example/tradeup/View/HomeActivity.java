package com.example.tradeup.View;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.tradeup.R;
import com.example.tradeup.View.FragmentNav.AccountFragment;
import com.example.tradeup.View.FragmentNav.CategoryFragment;
import com.example.tradeup.View.FragmentNav.ChatFragment;
import com.example.tradeup.View.FragmentNav.HomeFragment;
import com.example.tradeup.View.FragmentNav.PostFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout navHome, navCategory, navPost, navChat, navAccount;
    private View chatDot;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Người dùng chưa đăng nhập → chuyển về LoginActivity
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        currentUserId = user.getUid();

        addControl();
        addEvent();

        // Mặc định chọn Home
        setSelectedTab(navHome);
        loadFragment(new HomeFragment());

        // Bắt đầu lắng nghe tin nhắn mới
        listenForUnreadMessages();
    }

    private void addControl() {
        navHome = findViewById(R.id.nav_home);
        navCategory = findViewById(R.id.nav_category);
        navPost = findViewById(R.id.nav_post);
        navChat = findViewById(R.id.nav_chat);
        navAccount = findViewById(R.id.nav_account);
        chatDot = findViewById(R.id.chat_notification_dot);
    }

    private void addEvent() {
        navHome.setOnClickListener(v -> {
            setSelectedTab(navHome);
            loadFragment(new HomeFragment());
        });

        navCategory.setOnClickListener(v -> {
            setSelectedTab(navCategory);
            loadFragment(new CategoryFragment());
        });

        navPost.setOnClickListener(v -> {
            setSelectedTab(navPost);
            loadFragment(new PostFragment());
        });

        navChat.setOnClickListener(v -> {
            setSelectedTab(navChat);
            loadFragment(new ChatFragment());
            chatDot.setVisibility(View.GONE); // Khi vào xem chat → ẩn dot
        });

        navAccount.setOnClickListener(v -> {
            setSelectedTab(navAccount);
            loadFragment(new AccountFragment());
        });
    }

    private void setSelectedTab(LinearLayout selectedLayout) {
        navHome.setSelected(false);
        navCategory.setSelected(false);
        navPost.setSelected(false);
        navChat.setSelected(false);
        navAccount.setSelected(false);

        selectedLayout.setSelected(true);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Lắng nghe tất cả messages chưa đọc bằng collectionGroup
     */
    private void listenForUnreadMessages() {
        db.collectionGroup("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;

                    if (snapshots != null && !snapshots.isEmpty()) {
                        chatDot.setVisibility(View.VISIBLE); // Có tin chưa đọc
                    } else {
                        chatDot.setVisibility(View.GONE); // Không có tin
                    }
                });
    }
}
