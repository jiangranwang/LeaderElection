package enums;

import network.message.payload.MessagePayload;
import network.message.payload.election.*;

public enum MessageType {
    QUERY(QueryPayload.class),
    QUERY_RESPONSE(QueryResponsePayload.class),
    NOTIFY_LEADER(NotifyLeaderPayload.class),
    LEADER(LeaderPayload.class),
    LEADER_ACK(LeaderAckPayload.class);

    private final Class<? extends MessagePayload> payloadClass;
    MessageType(Class<? extends MessagePayload> payloadClass) {
        this.payloadClass = payloadClass;
    }

    public Class<? extends MessagePayload> getPayloadClass() {
        return payloadClass;
    }
}
