package com.example.tradeup.View.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.Model.Product;
import com.example.tradeup.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SalesHistoryAdapter extends RecyclerView.Adapter<SalesHistoryAdapter.SalesViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final OnSalesActionListener listener;

    public interface OnSalesActionListener {
        void onEditClick(Product product);
        void onStatusChanged(Product product, String newStatus);
        void onDeleteClick(Product product); // Đã có
    }

    public SalesHistoryAdapter(Context context, List<Product> productList, OnSalesActionListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SalesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sales_history, parent, false);
        return new SalesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SalesViewHolder holder, int position) {
        Product product = productList.get(position);

        // Tên sản phẩm
        holder.productName.setText(product.getTitle());

        // Giá sản phẩm
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.productPrice.setText(format.format(product.getPrice()));

        // Hình ảnh sản phẩm
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrls().get(0))
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(R.drawable.ic_placeholder);
        }

        // Spinner trạng thái
        String status = product.getStatus();
        if (status != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    context,
                    R.array.status_options,
                    android.R.layout.simple_spinner_item
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.statusSpinner.setAdapter(adapter);

            int index = adapter.getPosition(status);
            if (index >= 0) holder.statusSpinner.setSelection(index);

            holder.statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                boolean isFirstSelection = true;

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    String selected = parent.getItemAtPosition(pos).toString();
                    if (!isFirstSelection && !selected.equals(product.getStatus())) {
                        listener.onStatusChanged(product, selected);
                    }
                    isFirstSelection = false;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // Nút chỉnh sửa
        holder.editButton.setOnClickListener(v -> listener.onEditClick(product));

        // ✅ Nút xoá sản phẩm
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class SalesViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice;
        Spinner statusSpinner;
        Button editButton, deleteButton; // Thêm deleteButton

        public SalesViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            statusSpinner = itemView.findViewById(R.id.statusSpinner);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton); // Ánh xạ deleteButton
        }
    }
}
