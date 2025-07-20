package com.example.tradeup.View.FragmentNav;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.bumptech.glide.Glide;
import com.example.tradeup.Controller.UploadImgItem;
import com.example.tradeup.Model.Product;
import com.example.tradeup.R;
import com.example.tradeup.View.CartActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PostFragment extends Fragment {

    private EditText etTitle, etDescription, etPrice, etLocation;
    private Spinner spCategory, spCondition;
    private ImageView imgAvatar;
    private LinearLayout imageContainer;
    private Button btnSelectAvatar, btnAddImage, btnPostProduct;

    private Uri avatarUri;
    private List<Uri> imageUris = new ArrayList<>();

    private UploadImgItem uploadImgItem;

    private static final int MAX_IMAGE_COUNT = 10;

    public PostFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post, container, false);

        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etPrice = view.findViewById(R.id.etPrice);
        etLocation = view.findViewById(R.id.etLocation);
        spCategory = view.findViewById(R.id.spCategory);
        spCondition = view.findViewById(R.id.spCondition);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        imageContainer = view.findViewById(R.id.imageContainer);
        btnSelectAvatar = view.findViewById(R.id.btnSelectAvatar);
        btnAddImage = view.findViewById(R.id.btnAddImage);
        btnPostProduct = view.findViewById(R.id.btnPostProduct);

        uploadImgItem = new UploadImgItem();

        btnSelectAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            avatarLauncher.launch(intent);
        });

        btnAddImage.setOnClickListener(v -> {
            if (imageUris.size() >= MAX_IMAGE_COUNT) {
                Toast.makeText(getContext(), "Chỉ chọn tối đa 10 ảnh", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            addImageLauncher.launch(intent);
        });

        btnPostProduct.setOnClickListener(v -> {
            postProduct();
        });

        return view;
    }

    private final ActivityResultLauncher<Intent> avatarLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        avatarUri = selectedImage;
                        // Dùng Glide để hiển thị ảnh đại diện
                        Glide.with(this)
                                .load(avatarUri)
                                .into(imgAvatar);
                    } else {
                        Toast.makeText(getContext(), "Không thể lấy ảnh từ thư viện", Toast.LENGTH_SHORT).show();
                    }
                }
            });


    private final ActivityResultLauncher<Intent> addImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        imageUris.add(uri);
                        addThumbnail(uri);
                    }
                }
            });

    private void addThumbnail(Uri uri) {
        ImageView imageView = new ImageView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
        params.setMargins(8, 0, 8, 0);
        imageView.setLayoutParams(params);
        imageView.setImageURI(uri);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageContainer.addView(imageView);
    }

    private void postProduct() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        String condition = spCondition.getSelectedItem().toString();
        String location = etLocation.getText().toString().trim();

        if (title.isEmpty() || priceStr.isEmpty() || avatarUri == null || imageUris.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập đủ thông tin và chọn ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        String id = UUID.randomUUID().toString();
        String sellerId = currentUser.getUid();  // ✅ Lấy UID thật từ FirebaseAuth
        long timestamp = System.currentTimeMillis();
        String status = "available";

        Toast.makeText(getContext(), "Đang tải ảnh...", Toast.LENGTH_SHORT).show();

        uploadImgItem.uploadImageToItemFolder(requireContext(), avatarUri)
                .addOnSuccessListener(avatarUrl -> {
                    List<String> imageUrls = new ArrayList<>();
                    uploadMultipleImages(0, imageUrls, avatarUrl, id, title, description, price,
                            category, condition, location, sellerId, status, timestamp);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi khi tải ảnh đại diện", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadMultipleImages(int index, List<String> imageUrls, String avatarUrl,
                                      String id, String title, String description, double price,
                                      String category, String condition, String location,
                                      String sellerId, String status, long timestamp) {
        if (index >= imageUris.size()) {
            Product product = new Product(id, title, description, price, category, condition, location,
                    imageUrls, sellerId, status, timestamp, avatarUrl);

            FirebaseFirestore.getInstance().collection("products").document(id)
                    .set(product)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(), "Đăng sản phẩm thành công!", Toast.LENGTH_SHORT).show();
                        resetForm();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Lỗi lưu sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        Uri uri = imageUris.get(index);
        uploadImgItem.uploadImageToItemFolder(requireContext(), uri)
                .addOnSuccessListener(url -> {
                    imageUrls.add(url);
                    uploadMultipleImages(index + 1, imageUrls, avatarUrl, id, title, description, price,
                            category, condition, location, sellerId, status, timestamp);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi khi upload ảnh thứ " + (index + 1), Toast.LENGTH_SHORT).show();
                });
    }

    private void resetForm() {
        etTitle.setText("");
        etDescription.setText("");
        etPrice.setText("");
        etLocation.setText("");
        spCategory.setSelection(0);
        spCondition.setSelection(0);
        imgAvatar.setImageResource(0);
        imageContainer.removeAllViews();
        avatarUri = null;
        imageUris.clear();
    }
}
