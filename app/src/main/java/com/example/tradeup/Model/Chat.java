package com.example.tradeup.Model;

import java.util.List;

public class Chat {
    private String id;
    private List<String> users; // Danh sách UID của 2 người
    private String lastMessage;
    private long timestamp;
    private String lastSenderId;

    public Chat() {}

    public Chat(String id, List<String> users, String lastMessage, long timestamp, String lastSenderId) {
        this.id = id;
        this.users = users;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.lastSenderId = lastSenderId;
    }

    // Getter + Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }
}
