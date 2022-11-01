package network.message.payload.election;

import enums.MessageType;
import javafx.util.Pair;
import network.Address;
import network.message.payload.MessagePayload;
import utils.Config;

import java.util.ArrayList;
import java.util.List;

public class QueryResponsePayload extends MessagePayload {
    private static final long serialVersionUID = 2385748139579123L;

    private final Address lowestId;
    private final List<Address> lowestIds;
    private final List<Pair<Address, Integer>> highestSuspectIds;

    public QueryResponsePayload(Address lowestId) {
        super(MessageType.QUERY_RESPONSE);
        assert Config.algorithm != 3;
        this.lowestId = lowestId;
        this.lowestIds = new ArrayList<>();
        this.highestSuspectIds = new ArrayList<>();
    }

    public QueryResponsePayload(List<Address> lowestIds, List<Pair<Address, Integer>> highestSuspectIds) {
        super(MessageType.QUERY_RESPONSE);
        assert Config.algorithm == 3;
        this.lowestId = null;
        this.lowestIds = lowestIds;
        this.highestSuspectIds = highestSuspectIds;
    }

    public List<Address> getLowestIds() {
        return lowestIds;
    }

    public List<Pair<Address, Integer>> getHighestSuspectIds() {
        return highestSuspectIds;
    }

    @Override
    public String toString() {
        return "type: " + MessageType.QUERY_RESPONSE + ", value: " + lowestId;
    }

    public Address getLowestId() {
        return lowestId;
    }
}
