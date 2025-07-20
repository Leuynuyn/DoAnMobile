package com.example.tradeup.View.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.Model.Product;
import com.example.tradeup.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);

    }

    public ProductAdapter(Context context, List<Product> productList, OnProductClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.productName.setText(product.getTitle());

        // ✅ Định dạng tiền Việt Nam
        NumberFormat formatVn = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedPrice = formatVn.format(product.getPrice());
        holder.productPrice.setText(formattedPrice);

        // ✅ Gán điểm rating nếu có
        // holder.productRating.setRating(product.getRating());

        // ✅ Hiển thị ảnh
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrls().get(0))
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(R.drawable.ic_placeholder);
        }

        // ✅ Gán trạng thái
        String status = product.getStatus(); // "available", "out_of_stock", "sale", etc.

        switch (status) {
            case "available":
                holder.productStatus.setText("Còn hàng");
                holder.productStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "out_of_stock":
                holder.productStatus.setText("Hết hàng");
                holder.productStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                break;
        }

        // ✅ Click item
        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice, productStatus;
        RatingBar productRating;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName  = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productStatus = itemView.findViewById(R.id.productStatus);
            productRating = itemView.findViewById(R.id.productRating);
        }
    }
}
