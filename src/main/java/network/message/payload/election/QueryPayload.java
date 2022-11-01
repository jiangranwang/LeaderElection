package network.message.payload.election;

import enums.MessageType;
import network.message.payload.MessagePayload;
import utils.Config;

public class QueryPayload extends MessagePayload {
    private static final long serialVersionUID = 2874891435243124L;

    private final int numLowNode; // number of low hash nodes
    private final int numSuspectCount; // number of high suspect count nodes

    public QueryPayload() {
        super(MessageType.QUERY);
        assert Config.algorithm != 3;
        this.numLowNode = -1;
        this.numSuspectCount = -1;
    }

    public QueryPayload(int numLowNode, int numSuspectCount) {
        super(MessageType.QUERY);
        assert Config.algorithm == 3;
        this.numLowNode = numLowNode;
        this.numSuspectCount = numSuspectCount;
    }

    public int getNumNodes() {
        return numLowNode;
    }

    public int getNumSuspects() {
        return numSuspectCount;
    }
}
