package simulator.event;

import enums.EventType;

public class LeaderCheckEvent extends Event {
    public LeaderCheckEvent(long time) {
        super(EventType.LEADER_CHECK, time);
    }
}
