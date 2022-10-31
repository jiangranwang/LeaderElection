package simulator.event;

import enums.EventType;
import network.Address;

public class LeaderCheckEvent extends Event {
    public LeaderCheckEvent(long time, Address id) {
        super(EventType.LEADER_CHECK, time, id);
    }
}
