package com.example.tradeup.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.Controller.ProductController;
import com.example.tradeup.Model.Product;
import com.example.tradeup.R;
import com.example.tradeup.View.Adapter.ProductAdapter;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ClothesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private final List<Product> productList = new ArrayList<>();
    private final ProductController productController = new ProductController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clothes);

        recyclerView = findViewById(R.id.recyclerViewClothes);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 cột

        adapter = new ProductAdapter(this, productList, product -> {
            // Mở chi tiết sản phẩm
            Intent intent = new Intent(this, ChitietActivity.class);
            intent.putExtra("productId", product.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        loadClothesProducts();
    }

    private void loadClothesProducts() {
        productController.getProductsByCategory("Quần áo", task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                productList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Product product = doc.toObject(Product.class);
                    productList.add(product);
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
