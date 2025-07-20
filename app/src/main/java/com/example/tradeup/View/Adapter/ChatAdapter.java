package com.example.tradeup.View.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.Model.Chat;
import com.example.tradeup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final Context context;
    private final List<Chat> chatList;
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(String chatId, String receiverId, String receiverName, String receiverAvatar);
        void onChatLongClick(Chat chat);
    }

    public ChatAdapter(Context context, List<Chat> chatList, OnChatClickListener listener) {
        this.context = context;
        this.chatList = chatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        String currentUid = FirebaseAuth.getInstance().getUid();

        // Gán nội dung tin nhắn cuối với tiền tố "Bạn: " nếu là người gửi
        if (currentUid != null && chat.getLastSenderId() != null && chat.getLastSenderId().equals(currentUid)) {
            holder.tvLastMessage.setText("Bạn: " + chat.getLastMessage());
        } else {
            holder.tvLastMessage.setText(chat.getLastMessage());
        }

        // Hiển thị thời gian gửi
        String formattedTime = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(chat.getTimestamp()));
        holder.tvTime.setText(formattedTime);

        if (currentUid == null) {
            holder.tvUsername.setText("Không xác định");
            holder.imgAvatar.setImageResource(R.drawable.default_avatar);
            return;
        }

        // Tìm người dùng còn lại
        String otherUserId = chat.getUsers().stream()
                .filter(id -> !id.equals(currentUid))
                .findFirst()
                .orElse(null);

        if (otherUserId == null) {
            holder.tvUsername.setText("Không xác định");
            holder.imgAvatar.setImageResource(R.drawable.default_avatar);
            return;
        }

        // Lấy thông tin người nhận
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String username = snapshot.getString("username");
                    String avatarUrl = snapshot.getString("avatar");

                    holder.tvUsername.setText(username != null ? username : "Người dùng");

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(context)
                                .load(avatarUrl)
                                .placeholder(R.drawable.default_avatar)
                                .circleCrop()
                                .into(holder.imgAvatar);
                    } else {
                        holder.imgAvatar.setImageResource(R.drawable.default_avatar);
                    }

                    holder.itemView.setOnClickListener(v -> {
                        listener.onChatClick(chat.getId(), otherUserId,
                                username != null ? username : "Người dùng",
                                avatarUrl != null ? avatarUrl : "");
                    });

                    holder.itemView.setOnLongClickListener(v -> {
                        listener.onChatLongClick(chat);
                        return true;
                    });
                })
                .addOnFailureListener(e -> {
                    holder.tvUsername.setText("Người dùng");
                    holder.imgAvatar.setImageResource(R.drawable.default_avatar);
                });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvUsername, tvLastMessage, tvTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
