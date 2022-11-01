package enums;

import simulator.event.*;

public enum EventType {
    RECEIVE_MSG(ReceiveMsgEvent.class),
    RESPONSE_CHECK(ResponseCheckEvent.class),
    LEADER_CHECK(LeaderCheckEvent.class),
    ACK_CHECK(AckCheckEvent.class),
    ROUTE_MSG(RouteMsgEvent.class),
    RESEND_EVENT(ResendEvent.class);

    private final Class<? extends Event> eventClass;
    EventType(Class<? extends Event> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }
}
