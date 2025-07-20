package com.example.tradeup.Controller;

import androidx.annotation.NonNull;

import com.example.tradeup.Model.Message;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.*;

import java.util.*;

public class ChatController {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference chatRef = db.collection("chats");

    public interface MessageListener {
        void onMessagesLoaded(List<Message> messages);
    }

    // Sinh ID chat duy nhất từ 2 UID
    public String generateChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    // Nghe tin nhắn giữa 2 người dùng
    public void listenToMessages(String senderId, String receiverId, MessageListener listener) {
        String chatId = generateChatId(senderId, receiverId);
        listenToMessagesByChatId(chatId, listener);
    }

    // Nghe tin nhắn theo chatId
    public void listenToMessagesByChatId(String chatId, MessageListener listener) {
        chatRef.document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    List<Message> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        if (msg != null) messages.add(msg);
                    }
                    listener.onMessagesLoaded(messages);
                });
    }

    // Gửi tin nhắn
    public void sendMessage(String senderId, String receiverId, String text) {
        String chatId = generateChatId(senderId, receiverId);
        long timestamp = System.currentTimeMillis();

        // ✅ Thêm trường isRead = false (tin nhắn mới mặc định chưa đọc)
        Message message = new Message(senderId, receiverId, text, timestamp, false);

        // 1. Thêm tin nhắn mới
        chatRef.document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener(docRef -> {
                    // Thành công
                })
                .addOnFailureListener(Throwable::printStackTrace);

        // 2. Cập nhật thông tin cuộc trò chuyện
        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("id", chatId);
        chatUpdate.put("lastMessage", text);
        chatUpdate.put("timestamp", timestamp);
        chatUpdate.put("lastSenderId", senderId);
        chatUpdate.put("users", Arrays.asList(senderId, receiverId));

        chatRef.document(chatId).set(chatUpdate, SetOptions.merge());
    }

    // Lấy danh sách chat của người dùng
    public void getUserChats(String userId, OnCompleteListener<QuerySnapshot> listener) {
        chatRef.whereArrayContains("users", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(listener);
    }

    // Xóa toàn bộ cuộc trò chuyện và tin nhắn
    public void deleteChatWithMessages(String chatId) {
        CollectionReference messagesRef = chatRef.document(chatId).collection("messages");

        messagesRef.get().addOnSuccessListener(querySnapshot -> {
            WriteBatch batch = db.batch();

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                batch.delete(doc.getReference());
            }

            // Sau khi xóa hết tin nhắn thì xóa luôn document chat
            batch.commit().addOnSuccessListener(unused -> {
                chatRef.document(chatId).delete();
            }).addOnFailureListener(Throwable::printStackTrace);
        }).addOnFailureListener(Throwable::printStackTrace);
    }
}
