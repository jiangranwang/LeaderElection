package simulator.event;

import enums.EventType;
import network.Address;
import network.message.Message;

import java.util.List;

public class ResendEvent extends Event {
    private final Message msg;
    private final List<Address> target_nodes;

    public ResendEvent(long time, Address id, Message msg, List<Address> target_nodes) {
        super(EventType.RESEND, time, id);

        this.msg = msg;
        this.target_nodes = target_nodes;
    }

    public Message getMsg() {
        return msg;
    }

    public List<Address> getTargetNodes() {
        return target_nodes;
    }
}
