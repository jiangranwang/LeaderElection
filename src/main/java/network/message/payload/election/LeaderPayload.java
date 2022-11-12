package network.message.payload.election;

import enums.MessageType;
import network.message.payload.MessagePayload;

public class LeaderPayload extends MessagePayload {
    private final int leaderNo;

    public LeaderPayload(int leaderNo) {
        super(MessageType.LEADER);

        this.leaderNo = leaderNo;
    }

    public int getLeaderNo() {
        return leaderNo;
    }
}
