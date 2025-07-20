package com.example.tradeup.View;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.Model.Product;
import com.example.tradeup.R;
import com.example.tradeup.View.Adapter.SalesHistoryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SaleHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SalesHistoryAdapter adapter;
    private List<Product> myProducts = new ArrayList<>();
    private FirebaseFirestore firestore;
    private static final String TAG = "SaleHistoryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sale_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.salesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        firestore = FirebaseFirestore.getInstance();

        adapter = new SalesHistoryAdapter(this, myProducts, new SalesHistoryAdapter.OnSalesActionListener() {
            @Override
            public void onEditClick(Product product) {
                Intent intent = new Intent(SaleHistoryActivity.this, EditProductActivity.class);
                intent.putExtra("productId", product.getId());
                startActivity(intent);
            }

            @Override
            public void onStatusChanged(Product product, String newStatus) {
                firestore.collection("products")
                        .document(product.getId())
                        .update("status", newStatus)
                        .addOnSuccessListener(unused ->
                                Toast.makeText(SaleHistoryActivity.this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(SaleHistoryActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onDeleteClick(Product product) {
                firestore.collection("products")
                        .document(product.getId())
                        .delete()
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(SaleHistoryActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                            myProducts.remove(product);
                            adapter.notifyDataSetChanged();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(SaleHistoryActivity.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Xóa sản phẩm lỗi: ", e);
                        });
            }
        });

        recyclerView.setAdapter(adapter);
        loadUserProducts();
    }

    private void loadUserProducts() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "Đang truy vấn sản phẩm với sellerId = " + userId);

        firestore.collection("products")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myProducts.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Product product = doc.toObject(Product.class);
                        if (product != null) {
                            product.setId(doc.getId());  // Gán ID Firestore cho Product
                            myProducts.add(product);
                        }
                    }

                    Log.d(TAG, "Tổng sản phẩm tìm thấy: " + myProducts.size());
                    adapter.notifyDataSetChanged();

                    if (myProducts.isEmpty()) {
                        Toast.makeText(this, "Bạn chưa đăng sản phẩm nào.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải sản phẩm: " + e.getMessage());
                    Toast.makeText(SaleHistoryActivity.this, "Không thể tải sản phẩm", Toast.LENGTH_SHORT).show();
                });
    }
}
