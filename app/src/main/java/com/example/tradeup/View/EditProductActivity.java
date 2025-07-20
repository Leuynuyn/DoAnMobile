package com.example.tradeup.View;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.tradeup.Model.Product;
import com.example.tradeup.R;
import com.google.firebase.firestore.*;

public class EditProductActivity extends AppCompatActivity {

    private ImageView imgAvatar;
    private EditText etTitle, etDescription, etPrice, etLocation;
    private Spinner spCategory, spCondition;
    private Button btnPostProduct;

    private FirebaseFirestore db;
    private DocumentReference productRef;
    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        // Ánh xạ view
        imgAvatar = findViewById(R.id.imgAvatar);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etLocation = findViewById(R.id.etLocation);
        spCategory = findViewById(R.id.spCategory);
        spCondition = findViewById(R.id.spCondition);
        btnPostProduct = findViewById(R.id.btnPostProduct);

        db = FirebaseFirestore.getInstance();

        productId = getIntent().getStringExtra("productId");
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(this, "ID sản phẩm không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        productRef = db.collection("products").document(productId);

        loadProductData();

        btnPostProduct.setOnClickListener(v -> updateProduct());
    }

    private void loadProductData() {
        productRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Product product = snapshot.toObject(Product.class);
                if (product != null) {
                    etTitle.setText(product.getTitle());
                    etDescription.setText(product.getDescription());
                    etPrice.setText(String.valueOf(product.getPrice()));
                    etLocation.setText(product.getLocation());

                    // Spinner set selection
                    setSpinnerSelection(spCategory, product.getCategory());
                    setSpinnerSelection(spCondition, product.getCondition());

                    // Load ảnh đại diện nếu có
                    if (product.getAvatarUrl() != null && !product.getAvatarUrl().isEmpty()) {
                        Glide.with(this)
                                .load(product.getAvatarUrl())
                                .placeholder(R.drawable.default_avatar)
                                .into(imgAvatar);
                    }
                } else {
                    Toast.makeText(this, "Không thể chuyển đổi sản phẩm", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Sản phẩm không tồn tại", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi khi tải sản phẩm", Toast.LENGTH_SHORT).show();
        });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter == null) return;

        for (int i = 0; i < adapter.getCount(); i++) {
            if (value.equalsIgnoreCase(adapter.getItem(i).toString())) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void updateProduct() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        String condition = spCondition.getSelectedItem().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) ||
                TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(location)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        productRef.update(
                "title", title,
                "description", description,
                "price", price,
                "location", location,
                "category", category,
                "condition", condition
        ).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi khi cập nhật sản phẩm", Toast.LENGTH_SHORT).show();
        });
    }
}
