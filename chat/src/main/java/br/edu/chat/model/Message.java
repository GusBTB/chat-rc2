package br.edu.chat.model;

public class Message {

    private int id;
    private int senderUserId;
    private Integer receiverUserId;
    private Integer groupId;
    private String content;
    private MessageType messageType;
    private String createdAt;

    public Message() {
    }

    public Message(int senderUserId, Integer receiverUserId, Integer groupId,
            String content, MessageType messageType, String createdAt) {
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.groupId = groupId;
        this.content = content;
        this.messageType = messageType;
        this.createdAt = createdAt;
    }

    public Message(int id, int senderUserId, Integer receiverUserId, Integer groupId,
            String content, MessageType messageType, String createdAt) {
        this.id = id;
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.groupId = groupId;
        this.content = content;
        this.messageType = messageType;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(int senderUserId) {
        this.senderUserId = senderUserId;
    }

    public Integer getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(Integer receiverUserId) {
        this.receiverUserId = receiverUserId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", senderUserId=" + senderUserId +
                ", receiverUserId=" + receiverUserId +
                ", groupId=" + groupId +
                ", content='" + content + '\'' +
                ", messageType=" + messageType +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}