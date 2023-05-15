package simulator.event;

import enums.EventType;
import network.Address;
import network.message.Message;

import java.util.List;

public class ResendEvent extends Event {
    private final Message msg;
    // private final List<Address> target_nodes;
    // private final int ttl;

    public ResendEvent(long time, Address id, Message msg) {
        super(EventType.RESEND, time, id);

        this.msg = msg;
        // this.target_nodes = target_nodes;
        // this.ttl = ttl;
    }

    public Message getMsg() {
        return msg;
    }

    // public int getTtl() {
    //     return ttl;
    // }

    // public List<Address> getTargetNodes() {
    //     return target_nodes;
    // }
}
