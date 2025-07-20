package com.example.tradeup.View;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.Model.CartItem;
import com.example.tradeup.R;
import com.example.tradeup.View.Adapter.CartAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerCartItems;
    private TextView tvTotalPrice;
    private Button btnCheckout;

    private CartAdapter adapter;
    private List<CartItem> cartItems;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutCart), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        recyclerCartItems = findViewById(R.id.recyclerCartItems);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnCheckout = findViewById(R.id.btnCheckout);

        cartItems = new ArrayList<>();

        adapter = new CartAdapter(this, cartItems);
        recyclerCartItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerCartItems.setAdapter(adapter);

        loadCartItems();

        adapter.setOnCartItemChangeListener(updatedList -> {
            cartItems = updatedList;
            updateTotalPrice();
            saveCartToFirestore();  // Lưu thay đổi số lượng lên Firestore
        });

        btnCheckout.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng thanh toán chưa được triển khai.", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCartItems() {
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem giỏ hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("carts")
                .document(currentUser.getUid())
                .collection("items")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    cartItems.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        CartItem item = doc.toObject(CartItem.class);
                        if (item != null) {
                            cartItems.add(item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateTotalPrice();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải giỏ hàng", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getPrice() * item.getQuantity();
        }
        tvTotalPrice.setText(String.format("%.0fđ", total));
    }

    private void saveCartToFirestore() {
        if (currentUser == null) return;

        WriteBatch batch = db.batch();
        CollectionReference cartRef = db.collection("carts").document(currentUser.getUid()).collection("items");

        for (CartItem item : cartItems) {
            DocumentReference docRef = cartRef.document(item.getProductId());
            batch.set(docRef, item);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // Lưu thành công
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi lưu giỏ hàng", Toast.LENGTH_SHORT).show();
                });
    }
}
