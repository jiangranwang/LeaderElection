package network.message.payload;

import enums.MessageType;

import java.io.Serializable;

public class MessagePayload implements Cloneable, Serializable {
    private static final long serialVersionUID = 8135931604175469785L;

    private final MessageType type;

    public MessagePayload(MessageType type) {
        this.type = type;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "type: " + type;
    }

    @Override
    public MessagePayload clone() {
        try {
            return (MessagePayload) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}