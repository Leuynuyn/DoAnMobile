package com.example.tradeup.View;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.Controller.ChatController;
import com.example.tradeup.Model.Message;
import com.example.tradeup.R;
import com.example.tradeup.View.Adapter.MessageAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText edtMessage;
    private Button btnSend;
    private ImageView imgReceiver;
    private TextView tvReceiverName;

    private MessageAdapter messageAdapter;
    private final List<Message> messageList = new ArrayList<>();

    private String receiverId, receiverName, receiverAvatar;
    private String chatId;

    private final ChatController chatController = new ChatController();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        currentUserId = FirebaseAuth.getInstance().getUid();

        // Lấy dữ liệu từ Intent
        receiverId = getIntent().getStringExtra("receiverId");
        receiverName = getIntent().getStringExtra("receiverName");
        receiverAvatar = getIntent().getStringExtra("receiverAvatar");

        // Kiểm tra null
        if (receiverId == null || receiverId.isEmpty() || currentUserId == null) {
            Toast.makeText(this, "Lỗi: thiếu dữ liệu người nhận hoặc người gửi", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Tạo chatId từ 2 UID
        chatId = chatController.generateChatId(currentUserId, receiverId);

        // Gán UI
        recyclerView = findViewById(R.id.recyclerViewMessages);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        imgReceiver = findViewById(R.id.imgReceiverAvatar);
        tvReceiverName = findViewById(R.id.tvReceiverName);

        tvReceiverName.setText(receiverName);
        Glide.with(this).load(receiverAvatar)
                .placeholder(R.drawable.default_avatar)
                .circleCrop()
                .into(imgReceiver);

        // Cài đặt RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(this, messageList, currentUserId);
        recyclerView.setAdapter(messageAdapter);

        // Lắng nghe tin nhắn mới
        chatController.listenToMessages(currentUserId, receiverId, messages -> {
            messageList.clear();
            messageList.addAll(messages);
            messageAdapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(messageList.size() - 1);
        });

        // Gửi tin nhắn
        btnSend.setOnClickListener(v -> {
            String text = edtMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                chatController.sendMessage(currentUserId, receiverId, text); // ✅ Sẽ tự động cập nhật lastSenderId
                edtMessage.setText("");
            }
        });

        // Log debug
        Log.d("ChatRoomActivity", "chatId: " + chatId);
        Log.d("ChatRoomActivity", "receiverId: " + receiverId);
        Log.d("ChatRoomActivity", "receiverName: " + receiverName);
    }
}
