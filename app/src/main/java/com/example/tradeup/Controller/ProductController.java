package com.example.tradeup.Controller;

import android.util.Log;

import com.example.tradeup.Model.Product;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductController {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference productRef = db.collection("products");

    // Thêm sản phẩm mới
    public void addProduct(Product product, OnCompleteListener<Void> listener) {
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId(UUID.randomUUID().toString());
        }

        productRef.document(product.getId())
                .set(product)
                .addOnCompleteListener(listener);
    }

    // Cập nhật sản phẩm
    public void updateProduct(Product product, OnCompleteListener<Void> listener) {
        if (product.getId() == null || product.getId().isEmpty()) {
            Log.e("ProductController", "updateProduct: Product ID is null or empty");
            return;
        }

        productRef.document(product.getId())
                .set(product)
                .addOnCompleteListener(listener);
    }

    // Xoá sản phẩm
    public void deleteProduct(String productId, OnCompleteListener<Void> listener) {
        productRef.document(productId)
                .delete()
                .addOnCompleteListener(listener);
    }

    // Lấy tất cả sản phẩm (mới nhất trước)
    public void getAllProducts(OnCompleteListener<QuerySnapshot> listener) {
        productRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("ProductController", "getAllProducts error: ", task.getException());
                    }
                    listener.onComplete(task);
                });
    }

    // Lọc theo người bán
    public void getProductsBySeller(String sellerId, OnCompleteListener<QuerySnapshot> listener) {
        productRef.whereEqualTo("sellerId", sellerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("ProductController", "getProductsBySeller error: ", task.getException());
                    }
                    listener.onComplete(task);
                });
    }

    // Lọc theo danh mục
    public void getProductsByCategory(String category, OnCompleteListener<QuerySnapshot> listener) {
        productRef.whereEqualTo("category", category)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("ProductController", "getProductsByCategory error: ", task.getException());
                    }
                    listener.onComplete(task);
                });
    }

    // Lọc theo trạng thái
    public void getProductsByStatus(String status, OnCompleteListener<QuerySnapshot> listener) {
        productRef.whereEqualTo("status", status)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("ProductController", "getProductsByStatus error: ", task.getException());
                    }
                    listener.onComplete(task);
                });
    }

    // Lọc theo danh mục + trạng thái
    public void getProductsByCategoryAndStatus(String category, String status, OnCompleteListener<QuerySnapshot> listener) {
        productRef.whereEqualTo("category", category)
                .whereEqualTo("status", status)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("ProductController", "getProductsByCategoryAndStatus error: ", task.getException());
                    }
                    listener.onComplete(task);
                });
    }

    // Tìm kiếm theo tiêu đề
    // Tìm kiếm theo tiêu đề (không phân biệt hoa thường, dấu)
    public void searchProductsByTitle(String keyword, OnProductSearchResult callback) {
        db.collection("products")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Product> filteredProducts = new ArrayList<>();
                    String processedKeyword = removeAccents(keyword.toLowerCase());

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Product product = doc.toObject(Product.class);
                        product.setId(doc.getId());

                        String title = product.getTitle();
                        if (title != null && removeAccents(title.toLowerCase()).contains(processedKeyword)) {
                            filteredProducts.add(product);
                        }
                    }

                    callback.onResult(filteredProducts);
                })
                .addOnFailureListener(e -> {
                    callback.onResult(new ArrayList<>()); // Trả về danh sách rỗng nếu lỗi
                });
    }

    public static String removeAccents(String s) {
        String normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("đ", "d")
                .replaceAll("Đ", "D");
    }

}
