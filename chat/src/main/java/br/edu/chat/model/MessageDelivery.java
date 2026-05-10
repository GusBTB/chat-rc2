package br.edu.chat.model;

public class MessageDelivery {

    private int deliveryId;
    private int messageId;
    private int senderUserId;
    private int receiverUserId;
    private Integer groupId;
    private String content;
    private MessageType messageType;
    private String createdAt;

    public MessageDelivery() {
    }

    public MessageDelivery(int deliveryId, int messageId, int senderUserId, int receiverUserId,
            Integer groupId, String content, MessageType messageType, String createdAt) {
        this.deliveryId = deliveryId;
        this.messageId = messageId;
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.groupId = groupId;
        this.content = content;
        this.messageType = messageType;
        this.createdAt = createdAt;
    }

    public int getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(int deliveryId) {
        this.deliveryId = deliveryId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(int senderUserId) {
        this.senderUserId = senderUserId;
    }

    public int getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(int receiverUserId) {
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
        return "MessageDelivery{" +
                "deliveryId=" + deliveryId +
                ", messageId=" + messageId +
                ", senderUserId=" + senderUserId +
                ", receiverUserId=" + receiverUserId +
                ", groupId=" + groupId +
                ", content='" + content + '\'' +
                ", messageType=" + messageType +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}