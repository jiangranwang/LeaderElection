package simulator.event;

import enums.EventType;
import network.Address;
import network.message.Message;

public class ExitCritEvent extends Event {
    public ExitCritEvent(long time, Address id) {
        super(EventType.EXIT_CRIT, time, id);
    }
}
