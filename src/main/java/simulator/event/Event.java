package simulator.event;

import enums.EventType;

public class Event {
    private final EventType type;
    private final long time;

    public Event(EventType type, long time) {
        this.type = type;
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public EventType getType() {
        return type;
    }
}
