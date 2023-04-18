package network.message.payload.election;

import enums.MessageType;
import javafx.util.Pair;
import network.Address;
import network.message.payload.MessagePayload;
import utils.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class QueryResponsePayload extends MessagePayload {
    private static final long serialVersionUID = 2385748139579123L;

    private final Address lowestId; // lowest hash id in its membership list. Used for algorithm 1 & 2
    private final HashSet<Address> lowestIds; // a set of the lowest hash ids. Used for algorithm 3 & 4
    private final HashSet<Pair<Address, Integer>> highestSuspectIds; // a set of high suspect-count nodes. Used for algorithm 3 & 4

    public QueryResponsePayload(Address lowestId) {
        super(MessageType.QUERY_RESPONSE);
        assert Config.algorithm != 3;
        this.lowestId = lowestId;
        this.lowestIds = new HashSet<>();
        this.highestSuspectIds = new HashSet<>();
    }

    public QueryResponsePayload(List<Address> lowestIds, List<Pair<Address, Integer>> highestSuspectIds) {
        super(MessageType.QUERY_RESPONSE);
        assert Config.algorithm == 3 || Config.algorithm == 4;
        this.lowestId = null;
        this.lowestIds = new HashSet<>(lowestIds);
        this.highestSuspectIds = new HashSet<>(highestSuspectIds);
    }

    public List<Address> getLowestIds() {
        return new ArrayList<>(lowestIds);
    }

    public List<Pair<Address, Integer>> getHighestSuspectIds() {
        return new ArrayList<>(highestSuspectIds);
    }

    @Override
    public String toString() {
        return "type: " + MessageType.QUERY_RESPONSE + ", value: " + lowestId;
    }

    public Address getLowestId() {
        return lowestId;
    }
}
