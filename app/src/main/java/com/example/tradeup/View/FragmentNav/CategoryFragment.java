package com.example.tradeup.View.FragmentNav;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tradeup.R;
import com.example.tradeup.View.ClothesActivity;
import com.example.tradeup.View.ShoeActivity;

public class CategoryFragment extends Fragment {

    public CategoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        // Bắt sự kiện khi người dùng bấm vào danh mục "Quần áo"
        LinearLayout quanAoLayout = view.findViewById(R.id.quanAo);
        quanAoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ClothesActivity.class);
            startActivity(intent);
        });
        LinearLayout shoesLayout = view.findViewById(R.id.shoes);
        shoesLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ShoeActivity.class);
            startActivity(intent);
        });
        // Các danh mục khác có thể xử lý tương tự nếu bạn có Activity riêng
        // LinearLayout sach = view.findViewById(R.id.sach);
        // sach.setOnClickListener(...)

        return view;
    }
}
