package simulator.event;

import enums.EventType;
import network.Address;

public class SetSuspectEvent extends Event {
    public SetSuspectEvent(long time, Address id) {
        super(EventType.SET_SUSPECT, time, id);
    }
}
