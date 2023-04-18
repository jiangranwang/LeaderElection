package network.message.payload.election;

import enums.MessageType;
import network.message.payload.MessagePayload;

public class NotifyLeaderPayload extends MessagePayload {
    private final int leaderNo; // uniquely identifies the leader number

    public NotifyLeaderPayload(int leaderNo) {
        super(MessageType.NOTIFY_LEADER);

        this.leaderNo = leaderNo;
    }

    public int getLeaderNo() {
        return leaderNo;
    }
}
