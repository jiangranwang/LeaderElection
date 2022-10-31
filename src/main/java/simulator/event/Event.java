package simulator.event;

import enums.EventType;
import network.Address;

public class Event {
    private final EventType type;
    private final long time;
    private final Address id;

    public Event(EventType type, long time, Address id) {
        this.type = type;
        this.time = time;
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public Address getId() {
        return id;
    }

    public EventType getType() {
        return type;
    }

    public String toString() {
        return "type: " + type.toString() + " at time: " + time;
    }
}
