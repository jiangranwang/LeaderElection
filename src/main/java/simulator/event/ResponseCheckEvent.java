package simulator.event;

import enums.EventType;
import network.Address;

import java.util.List;

public class ResponseCheckEvent extends Event {
    private final List<Address> target_nodes;
    public ResponseCheckEvent(long time, Address id, List<Address> target_nodes) {
        super(EventType.RESPONSE_CHECK, time, id);
        this.target_nodes = target_nodes;
    }

    public List<Address> getTargetNodes() {
        return target_nodes;
    }
}
