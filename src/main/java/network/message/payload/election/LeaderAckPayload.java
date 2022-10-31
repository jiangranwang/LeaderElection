package network.message.payload.election;

import enums.MessageType;
import network.message.payload.MessagePayload;

public class LeaderAckPayload extends MessagePayload {
    public LeaderAckPayload() {
        super(MessageType.LEADER_ACK);
    }
}
