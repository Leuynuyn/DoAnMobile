package com.example.tradeup.View.FragmentNav;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.View.EditProductActivity;
import com.example.tradeup.View.LoginActivity;
import com.example.tradeup.View.ProfileActivity;
import com.example.tradeup.View.SaleHistoryActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountFragment extends Fragment {

    private TextView tvUserName, tvThongtin, tvLichSuBanHang;
    private Button btnLogOut;
    ImageView imgAvatar;
    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUserName = view.findViewById(R.id.tvUserName);
        tvThongtin = view.findViewById(R.id.tvThongtin);
        btnLogOut = view.findViewById(R.id.btnLogOut);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvLichSuBanHang = view.findViewById(R.id.tvLichSuBanHang);

        // Tải thông tin người dùng
        loadUserProfile();

        tvThongtin.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), ProfileActivity.class);
            startActivity(intent);
        });

        tvLichSuBanHang.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), SaleHistoryActivity.class);
            startActivity(intent);
        });

        btnLogOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // Lấy đúng field giống ProfileActivity
                        String username = snapshot.getString("username");
                        String avatarUrl = snapshot.getString("avatar");

                        tvUserName.setText(username != null ? username : "Người dùng");

                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .circleCrop()
                                    .into(imgAvatar);
                        } else {
                            imgAvatar.setImageResource(R.drawable.default_avatar);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                });
    }
}
