package network.message.payload.election;

import enums.MessageType;
import network.message.payload.MessagePayload;
import utils.Config;

public class QueryPayload extends MessagePayload {
    private static final long serialVersionUID = 2874891435243124L;

    private final int num_low_node; // number of low hash nodes
    private final int num_suspect_count; // number of high suspect count nodes

    public QueryPayload() {
        super(MessageType.QUERY);
        assert Config.algorithm != 3;
        this.num_low_node = -1;
        this.num_suspect_count = -1;
    }

    public QueryPayload(int num_low_node, int num_suspect_count) {
        super(MessageType.QUERY);
        assert Config.algorithm == 3;
        this.num_low_node = num_low_node;
        this.num_suspect_count = num_suspect_count;
    }

    public int getNumNodes() {
        return num_low_node;
    }

    public int getNumSuspects() {
        return num_suspect_count;
    }
}
