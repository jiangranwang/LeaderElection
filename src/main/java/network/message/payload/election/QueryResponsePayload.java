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

    private final Address lowest_id;
    private final List<Address> lowest_ids;
    private final List<Pair<Address, Integer>> highest_suspect_ids;

    public QueryResponsePayload(Address lowest_id) {
        super(MessageType.QUERY_RESPONSE);
        assert Config.algorithm != 3;
        this.lowest_id = lowest_id;
        this.lowest_ids = new ArrayList<>();
        this.highest_suspect_ids = new ArrayList<>();
    }

    public QueryResponsePayload(List<Address> lowest_ids, List<Pair<Address, Integer>> highest_suspect_ids) {
        super(MessageType.QUERY_RESPONSE);
        assert Config.algorithm == 3;
        this.lowest_id = null;
        this.lowest_ids = lowest_ids;
        this.highest_suspect_ids = highest_suspect_ids;
    }

    public List<Address> getLowestIds() {
        return lowest_ids;
    }

    public List<Pair<Address, Integer>> getHighestSuspectIds() {
        return highest_suspect_ids;
    }

    @Override
    public String toString() {
        return "type: " + MessageType.QUERY_RESPONSE + ", value: " + lowest_id;
    }

    public Address getLowestId() {
        return lowest_id;
    }
}
