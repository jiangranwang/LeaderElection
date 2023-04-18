package simulator.event;

import enums.EventType;
import network.Address;
import network.message.Message;

public class ReceiveMsgEvent extends Event {
    private final Message msg; // the message to be received
    public ReceiveMsgEvent(long time, Address id, Message msg) {
        super(EventType.RECEIVE_MSG, time, id);

        this.msg = msg;
    }

    public Message getMsg() {
        return msg;
    }
}
