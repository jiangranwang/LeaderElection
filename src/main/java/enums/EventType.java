/*
Different event types. This is useful for events that are not immediately triggered but instead with a delay. For
example, the initiator will check if it receives enough Response messages after a certain timeout.
 */

package enums;

import simulator.event.*;

public enum EventType {
    RECEIVE_MSG(ReceiveMsgEvent.class),
    RESPONSE_CHECK(ResponseCheckEvent.class),
    LEADER_CHECK(LeaderCheckEvent.class),
    ACK_CHECK(AckCheckEvent.class),
    ROUTE_MSG(RouteMsgEvent.class),
    RESEND(ResendEvent.class),
    SET_SUSPECT(SetSuspectEvent.class);

    private final Class<? extends Event> eventClass;
    EventType(Class<? extends Event> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }
}
