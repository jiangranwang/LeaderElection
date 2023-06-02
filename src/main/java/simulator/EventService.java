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
    private static Integer eventCt = 0;
    private static long startTime = 0;

    public static void addEvent(Event event) {
        // if (event.getTime() > Config.endTime) {
        //     System.out.println("Quitting early because reached end");
        //     return;
        // }
        events.add(event);
    }

    public static boolean hasEvent() {
        return !events.isEmpty();
    }

    public static void initialize(HashMap<Address, Server> servers) {
        EventService.servers = servers;
    }

    public static void processAll() {
        startTime = System.currentTimeMillis();
        while (!events.isEmpty()) {
            // System.out.println("Size of events is " + events.size());
            Event event = events.pollFirst();
            if (event != null) {
                eventCt++;
                LogicalTime.time = event.getTime();
                servers.get(event.getId()).processEvent(event);
            }
        }
        // System.out.println("Executed " + String.valueOf(eventCt) + " events over " +String.valueOf(System.currentTimeMillis()-startTime));
    }
}
