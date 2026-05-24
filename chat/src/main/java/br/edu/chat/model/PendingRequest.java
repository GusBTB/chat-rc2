package br.edu.chat.model;

public class PendingRequest {

    private int id;
    private RequestType requestType;
    private int requesterUserId;
    private Integer targetUserId;
    private Integer groupId;
    private Integer pendingMessageId;
    private RequestStatus status;
    private String createdAt;

    public PendingRequest() {
    }

    public PendingRequest(RequestType requestType, int requesterUserId,
            Integer targetUserId, Integer groupId, RequestStatus status, String createdAt) {
        this(requestType, requesterUserId, targetUserId, groupId, null, status, createdAt);
    }

    public PendingRequest(RequestType requestType, int requesterUserId,
            Integer targetUserId, Integer groupId, Integer pendingMessageId,
            RequestStatus status, String createdAt) {
        this.requestType = requestType;
        this.requesterUserId = requesterUserId;
        this.targetUserId = targetUserId;
        this.groupId = groupId;
        this.pendingMessageId = pendingMessageId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public PendingRequest(int id, RequestType requestType, int requesterUserId,
            Integer targetUserId, Integer groupId, RequestStatus status, String createdAt) {
        this(id, requestType, requesterUserId, targetUserId, groupId, null, status, createdAt);
    }

    public PendingRequest(int id, RequestType requestType, int requesterUserId,
            Integer targetUserId, Integer groupId, Integer pendingMessageId,
            RequestStatus status, String createdAt) {
        this.id = id;
        this.requestType = requestType;
        this.requesterUserId = requesterUserId;
        this.targetUserId = targetUserId;
        this.groupId = groupId;
        this.pendingMessageId = pendingMessageId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public int getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(int requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public Integer getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Integer targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getPendingMessageId() {
        return pendingMessageId;
    }

    public void setPendingMessageId(Integer pendingMessageId) {
        this.pendingMessageId = pendingMessageId;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PendingRequest{" +
                "id=" + id +
                ", requestType=" + requestType +
                ", requesterUserId=" + requesterUserId +
                ", targetUserId=" + targetUserId +
                ", groupId=" + groupId +
                ", pendingMessageId=" + pendingMessageId +
                ", status=" + status +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}