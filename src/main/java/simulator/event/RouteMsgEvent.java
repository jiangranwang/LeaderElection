package simulator.event;

import enums.EventType;
import network.Address;
import network.message.Message;

public class RouteMsgEvent extends Event {
    private final Message msg;

    public RouteMsgEvent(long time, Address id, Message msg) {
        super(EventType.ROUTE_MSG, time, id);
        this.msg = msg;
    }

    public Message getMsg() {
        return msg;
    }
}
