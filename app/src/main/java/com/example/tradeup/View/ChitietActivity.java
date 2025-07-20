package com.example.tradeup.View;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.tradeup.Model.CartItem;
import com.example.tradeup.Model.Product;
import com.example.tradeup.R;
import com.example.tradeup.View.Adapter.ImagesPagerAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.NumberFormat;
import java.util.*;

public class ChitietActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private ViewPager2 viewPager;
    private TextView textTitle, textPrice, textDescription;
    private Button btnAddToCart;
    private TextView tvSellerName;
    private ImageView imgSellerAvatar;

    private Product currentProduct;
    private String sellerId;
    private String sellerName;
    private String sellerAvatar;
    ImageView iconChat, iconReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chitiet);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();

        String productId = getIntent().getStringExtra("productId");
        if (productId != null) {
            loadProductDetail(productId);
        } else {
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnAddToCart.setOnClickListener(v -> {
            if (currentProduct != null) {
                showQuantityDialog();
            } else {
                Toast.makeText(this, "Sản phẩm chưa được tải xong", Toast.LENGTH_SHORT).show();
            }
        });

        iconChat.setOnClickListener(v -> openChatRoom());
        iconReport.setOnClickListener(v -> showReportDialog());
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPagerImages);
        textTitle = findViewById(R.id.textProductTitle);
        textPrice = findViewById(R.id.textProductPrice);
        textDescription = findViewById(R.id.textProductDescription);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        tvSellerName = findViewById(R.id.tvSellerName);
        imgSellerAvatar = findViewById(R.id.imgSellerAvatar);
        iconChat = findViewById(R.id.iconChat);
        iconReport = findViewById(R.id.iconReport);
    }

    private void loadProductDetail(String productId) {
        db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        currentProduct = snapshot.toObject(Product.class);
                        if (currentProduct != null) {
                            showProduct(currentProduct);
                        }
                    } else {
                        Toast.makeText(this, "Sản phẩm không tồn tại", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi khi tải sản phẩm", Toast.LENGTH_SHORT).show());
    }

    private void showProduct(Product product) {
        textTitle.setText(product.getTitle());
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        textPrice.setText(format.format(product.getPrice()));
        textDescription.setText(product.getDescription());

        List<String> imageUrls = product.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            viewPager.setAdapter(new ImagesPagerAdapter(this, imageUrls));
        }

        sellerId = product.getSellerId();
        loadSellerInfo(sellerId);
    }

    private void loadSellerInfo(String sellerId) {
        if (sellerId == null || sellerId.isEmpty()) return;

        db.collection("users").document(sellerId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    sellerName = snapshot.getString("username");
                    sellerAvatar = snapshot.getString("avatar");

                    tvSellerName.setText(
                            currentUser != null && sellerId.equals(currentUser.getUid()) ?
                                    "Bạn" : (sellerName != null ? sellerName : "Người bán")
                    );

                    if (sellerAvatar != null && !sellerAvatar.isEmpty()) {
                        Glide.with(this)
                                .load(sellerAvatar)
                                .placeholder(R.drawable.default_avatar)
                                .circleCrop()
                                .into(imgSellerAvatar);
                    } else {
                        imgSellerAvatar.setImageResource(R.drawable.default_avatar);
                    }
                })
                .addOnFailureListener(e -> {
                    tvSellerName.setText("Không thể tải người bán");
                    imgSellerAvatar.setImageResource(R.drawable.default_avatar);
                });
    }

    private void showQuantityDialog() {
        try {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity, null);
            TextView tvQuantity = dialogView.findViewById(R.id.tvQuantity);
            Button btnMinus = dialogView.findViewById(R.id.btnDecrease);
            Button btnPlus = dialogView.findViewById(R.id.btnIncrease);

            final int[] quantity = {1};
            tvQuantity.setText(String.valueOf(quantity[0]));

            btnMinus.setOnClickListener(v -> {
                if (quantity[0] > 1) {
                    quantity[0]--;
                    tvQuantity.setText(String.valueOf(quantity[0]));
                }
            });

            btnPlus.setOnClickListener(v -> {
                quantity[0]++;
                tvQuantity.setText(String.valueOf(quantity[0]));
            });

            new AlertDialog.Builder(this)
                    .setTitle("Chọn số lượng")
                    .setView(dialogView)
                    .setPositiveButton("Thêm vào giỏ", (dialog, which) -> {
                        addToCart(currentProduct, quantity[0]);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();

        } catch (Exception e) {
            Log.e("ChitietActivity", "Lỗi khi hiển thị dialog: " + e.getMessage());
            Toast.makeText(this, "Lỗi khi hiển thị dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void addToCart(Product product, int quantity) {
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String productId = product.getId();

        DocumentReference cartItemRef = db.collection("carts")
                .document(userId)
                .collection("items")
                .document(productId);

        cartItemRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long currentQty = documentSnapshot.getLong("quantity");
                int newQuantity = (currentQty != null ? currentQty.intValue() : 0) + quantity;

                cartItemRef.update("quantity", newQuantity)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(this, "Đã cập nhật giỏ hàng", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show());
            } else {
                String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                        ? product.getImageUrls().get(0)
                        : "";

                CartItem cartItem = new CartItem(
                        productId,
                        product.getTitle(),
                        product.getPrice(),
                        quantity,
                        imageUrl
                );

                cartItemRef.set(cartItem)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Thêm vào giỏ thất bại", Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi khi truy vấn giỏ hàng", Toast.LENGTH_SHORT).show());
    }

    private void showSellerOptionsDialog() {
        if (currentUser != null && currentUser.getUid().equals(sellerId)) {
            Toast.makeText(this, "Đây là sản phẩm của bạn", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Trò chuyện", "Tố cáo"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn hành động")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openChatRoom();
                    } else {
                        showReportDialog();
                    }
                })
                .show();
    }

    private void openChatRoom() {
        if (currentUser == null || sellerId == null) return;

        String currentUid = currentUser.getUid();
        String chatId = generateChatId(currentUid, sellerId);

        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("receiverId", sellerId);
        intent.putExtra("receiverName", sellerName);
        intent.putExtra("receiverAvatar", sellerAvatar);
        startActivity(intent);
    }

    private String generateChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    private void showReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn lý do tố cáo");

        String[] reasons = {
                "Hàng giả / nhái thương hiệu",
                "Lừa đảo / Gian lận",
                "Thông tin sai sự thật",
                "Hành vi không phù hợp",
                "Khác"
        };

        final int[] selected = {-1};

        builder.setSingleChoiceItems(reasons, -1, (dialog, which) -> selected[0] = which);

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            if (selected[0] == -1) {
                Toast.makeText(this, "Vui lòng chọn lý do", Toast.LENGTH_SHORT).show();
            } else {
                String selectedReason = reasons[selected[0]];
                sendReportToFirestore(selectedReason);
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void sendReportToFirestore(String reason) {
        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", currentUser.getUid());
        report.put("reportedUserId", sellerId);
        report.put("reason", reason);
        report.put("timestamp", new Timestamp(new Date()));

        db.collection("reports")
                .add(report)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this, "Tố cáo đã được gửi", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Gửi tố cáo thất bại", Toast.LENGTH_SHORT).show());
    }
}
