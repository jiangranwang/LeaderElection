package simulator;

import network.Address;
import simulator.event.Event;
import utils.Config;
import utils.EventComparator;

import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class EventService {
    private static final ConcurrentSkipListSet<Event> events = new ConcurrentSkipListSet<>(new EventComparator<>());
    private static HashMap<Address, Server> servers;

    public static void addEvent(Event event) {
        if (event.getTime() > Config.end_time) return;
        events.add(event);
    }

    public static void initialize(HashMap<Address, Server> servers) {
        EventService.servers = servers;
    }

    public static void processAll() {
        while (!events.isEmpty()) {
            Event event = events.pollFirst();
            if (event != null) {
                LogicalTime.time = event.getTime();
                servers.get(event.getId()).processEvent(event);
            }
        }
    }
}
