package simulator;

import network.Address;
import simulator.event.Event;

import java.util.concurrent.LinkedBlockingQueue;

public class EventService {
    private final LinkedBlockingQueue<Event> events;
    private final Address ip;
    private final Server server;

    public EventService(Address ip, Server server) {
        this.events = new LinkedBlockingQueue<>();
        this.ip = ip;
        this.server = server;
    }

    public void addEvent(Event event) {
        events.offer(event);
    }

    public void processAll(long time) {
        while (!events.isEmpty() && events.peek().getTime() <= time) {
            Event event = events.remove();
            server.processEvent(event);
        }
    }
}
