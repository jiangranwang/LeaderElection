package simulator.event;

import enums.EventType;
import network.Address;

public class ResponseCheckEvent extends Event {
    private final int numNodes; // minimum number of Response message needed to be received by the initiator
    public ResponseCheckEvent(long time, Address id, int numNodes) {
        super(EventType.RESPONSE_CHECK, time, id);
        this.numNodes = numNodes;
    }

    public int getNumNodes() {
        return numNodes;
    }
}
