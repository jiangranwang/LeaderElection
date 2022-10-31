package simulator.event;

import enums.EventType;

public class AckCheckEvent extends Event {
    public AckCheckEvent(long time) {
        super(EventType.ACK_CHECK, time);
    }
}
