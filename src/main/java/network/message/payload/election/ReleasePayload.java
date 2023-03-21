package network.message.payload.election;

import enums.MessageType;
import network.message.payload.MessagePayload;
import network.Address;

public class ReleasePayload extends MessagePayload {
    private static final long serialVersionUID = 2874891435243124L;

    private final int seqNum; // sequence number of request being released
    private final Address releaser;

    public ReleasePayload() {
        super(MessageType.RELEASE);
        this.seqNum = -1;
        this.releaser = null;
    }

    public ReleasePayload(int seqNum, Address releaser) {
        super(MessageType.RELEASE);
        this.seqNum = seqNum;
        this.releaser = releaser;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public Address getReleaser() {
        return releaser;
    }
}
