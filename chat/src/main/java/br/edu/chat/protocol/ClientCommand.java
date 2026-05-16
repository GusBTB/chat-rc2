package br.edu.chat.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientCommand {

    private final CommandType type;
    private final String rawInput;
    private final List<String> args;
    private final List<String> targetUsers;
    private final String groupName;
    private final String content;
    private final Integer requestId;
    private final String errorMessage;

    private ClientCommand(Builder builder) {
        this.type = builder.type;
        this.rawInput = builder.rawInput;
        this.args = Collections.unmodifiableList(new ArrayList<>(builder.args));
        this.targetUsers = Collections.unmodifiableList(new ArrayList<>(builder.targetUsers));
        this.groupName = builder.groupName;
        this.content = builder.content;
        this.requestId = builder.requestId;
        this.errorMessage = builder.errorMessage;
    }

    public CommandType getType() {
        return type;
    }

    public String getRawInput() {
        return rawInput;
    }

    public List<String> getArgs() {
        return args;
    }

    public List<String> getTargetUsers() {
        return targetUsers;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getContent() {
        return content;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isInvalid() {
        return type == CommandType.INVALID;
    }

    public boolean isUnknown() {
        return type == CommandType.UNKNOWN;
    }

    @Override
    public String toString() {
        return "ClientCommand{" +
                "type=" + type +
                ", args=" + args +
                ", targetUsers=" + targetUsers +
                ", groupName='" + groupName + '\'' +
                ", content='" + content + '\'' +
                ", requestId=" + requestId +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

    public static Builder builder(CommandType type) {
        return new Builder(type);
    }

    public static class Builder {
        private CommandType type;
        private String rawInput = "";
        private final List<String> args = new ArrayList<>();
        private final List<String> targetUsers = new ArrayList<>();
        private String groupName;
        private String content;
        private Integer requestId;
        private String errorMessage;

        public Builder(CommandType type) {
            this.type = type;
        }

        public Builder rawInput(String rawInput) {
            this.rawInput = rawInput;
            return this;
        }

        public Builder addArg(String arg) {
            this.args.add(arg);
            return this;
        }

        public Builder addTargetUser(String user) {
            this.targetUsers.add(user);
            return this;
        }

        public Builder targetUsers(List<String> users) {
            this.targetUsers.clear();
            this.targetUsers.addAll(users);
            return this;
        }

        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder requestId(Integer requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ClientCommand build() {
            return new ClientCommand(this);
        }
    }
}
