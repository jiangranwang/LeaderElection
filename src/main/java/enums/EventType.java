package enums;

import simulator.event.*;

public enum EventType {
    RECEIVE_MSG(ReceiveMsgEvent.class),
    ROUTE_MSG(RouteMsgEvent.class),
    RESEND(ResendEvent.class),
    EXIT_CRIT(ExitCritEvent.class),
    INITATE_REQ(InitiateRequestEvent.class);

    private final Class<? extends Event> eventClass;
    EventType(Class<? extends Event> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }
}
