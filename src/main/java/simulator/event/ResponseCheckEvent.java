package simulator.event;

import enums.EventType;
import network.Address;

public class ResponseCheckEvent extends Event {
    private final int num_nodes;
    public ResponseCheckEvent(long time, Address id, int num_nodes) {
        super(EventType.RESPONSE_CHECK, time, id);
        this.num_nodes = num_nodes;
    }

    public int getNumNodes() {
        return num_nodes;
    }
}
