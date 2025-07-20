package com.example.tradeup.View.FragmentNav;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.Controller.ChatController;
import com.example.tradeup.Model.Chat;
import com.example.tradeup.R;
import com.example.tradeup.View.ChatRoomActivity;
import com.example.tradeup.View.Adapter.ChatAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class ChatFragment extends Fragment {

    private TextView tvEmptyChat;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private final List<Chat> chatList = new ArrayList<>();
    private final ChatController chatController = new ChatController();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewChats);
        tvEmptyChat = view.findViewById(R.id.tvEmptyChat);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        chatAdapter = new ChatAdapter(getContext(), chatList, new ChatAdapter.OnChatClickListener() {
            @Override
            public void onChatClick(String chatId, String receiverId, String receiverName, String receiverAvatar) {
                Intent intent = new Intent(getContext(), ChatRoomActivity.class);
                intent.putExtra("chatId", chatId);
                intent.putExtra("receiverId", receiverId);
                intent.putExtra("receiverName", receiverName);
                intent.putExtra("receiverAvatar", receiverAvatar);
                startActivity(intent);
            }

            @Override
            public void onChatLongClick(Chat chat) {
                showDeleteDialog(chat);
            }
        });

        recyclerView.setAdapter(chatAdapter);
        currentUid = FirebaseAuth.getInstance().getUid();
        loadChats();
        return view;
    }

    private void loadChats() {
        db.collection("chats")
                .whereArrayContains("users", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    chatList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Chat chat = doc.toObject(Chat.class);
                            if (chat != null) {
                                List<String> deletedBy = (List<String>) doc.get("deletedBy");
                                if (deletedBy != null && deletedBy.contains(currentUid)) continue;

                                chat.setId(doc.getId());
                                chatList.add(chat);
                            }
                        }

                        Collections.sort(chatList, (c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
                        tvEmptyChat.setVisibility(View.GONE);
                    } else {
                        tvEmptyChat.setVisibility(View.VISIBLE);
                    }

                    chatAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Log.e("ChatFragment", "Lỗi khi tải danh sách chat: " + e.getMessage());
                });
    }

    private void showDeleteDialog(Chat chat) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa cuộc trò chuyện")
                .setMessage("Bạn có chắc chắn muốn xóa cuộc trò chuyện này không?")
                .setPositiveButton("Xóa", (dialog, which) -> xoaCuocTroChuyenMem(chat.getId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void xoaCuocTroChuyenMem(String chatId) {
        DocumentReference chatDoc = db.collection("chats").document(chatId);

        chatDoc.update("deletedBy", FieldValue.arrayUnion(currentUid))
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Đã xóa đoạn chat", Toast.LENGTH_SHORT).show();
                    kiemTraVaXoaVinhVienNeuCaHaiDaXoa(chatId);
                    loadChats();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi khi xóa", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void kiemTraVaXoaVinhVienNeuCaHaiDaXoa(String chatId) {
        DocumentReference chatDoc = db.collection("chats").document(chatId);
        chatDoc.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                List<String> users = (List<String>) doc.get("users");
                List<String> deletedBy = (List<String>) doc.get("deletedBy");

                if (deletedBy != null && users != null && deletedBy.containsAll(users)) {
                    xoaCuocTroChuyenVaTinNhan(chatId);
                }
            }
        });
    }

    private void xoaCuocTroChuyenVaTinNhan(String chatId) {
        CollectionReference messagesRef = db.collection("chats").document(chatId).collection("messages");

        messagesRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            WriteBatch batch = db.batch();

            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                batch.delete(doc.getReference());
            }

            batch.delete(db.collection("chats").document(chatId));
            batch.commit();
        });
    }
}