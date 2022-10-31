package network.message.payload.election;

import enums.MessageType;
import network.message.payload.MessagePayload;

public class LeaderPayload extends MessagePayload {
    public LeaderPayload() {
        super(MessageType.LEADER);
    }
}
