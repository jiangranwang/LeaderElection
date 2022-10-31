package simulator.event;

import enums.EventType;
import network.message.Message;

public class ReceiveMsgEvent extends Event {
    private final Message msg;
    public ReceiveMsgEvent(long time, Message msg) {
        super(EventType.RECEIVE_MSG, time);

        this.msg = msg;
    }

    public Message getMsg() {
        return msg;
    }
}
