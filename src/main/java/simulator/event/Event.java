/*
Base class for all types of events.
 */

package simulator.event;

import enums.EventType;
import network.Address;

public class Event {
    private final EventType type; // event type
    private final long time; // time when the event happens
    private final Address id; // at which node the event happens

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
