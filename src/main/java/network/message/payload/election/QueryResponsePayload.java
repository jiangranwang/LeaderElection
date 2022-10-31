package network.message.payload.election;

import enums.MessageType;
import network.Address;
import network.message.payload.MessagePayload;

public class QueryResponsePayload extends MessagePayload {
    private static final long serialVersionUID = 2385748139579123L;

    private final Address lowest_id;

    public QueryResponsePayload(Address lowest_id) {
        super(MessageType.QUERY_RESPONSE);
        this.lowest_id = lowest_id;
    }

    public Address getLowestId() {
        return lowest_id;
    }
}
