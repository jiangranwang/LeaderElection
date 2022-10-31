package network.message.payload.election;

import enums.MessageType;
import network.message.payload.MessagePayload;

public class NotifyLeaderPayload extends MessagePayload {
    public NotifyLeaderPayload() {
        super(MessageType.NOTIFY_LEADER);
    }
}
