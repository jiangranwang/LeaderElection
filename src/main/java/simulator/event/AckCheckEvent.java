package simulator.event;

import enums.EventType;
import network.Address;

public class AckCheckEvent extends Event {
    public AckCheckEvent(long time, Address id) {
        super(EventType.ACK_CHECK, time, id);
    }
}
