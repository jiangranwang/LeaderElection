package network.message.payload.election;

import enums.MessageType;
import network.message.payload.MessagePayload;

public class QueryPayload extends MessagePayload {
    private static final long serialVersionUID = 2874891435243124L;

    public QueryPayload() {
        super(MessageType.QUERY);
    }
}
