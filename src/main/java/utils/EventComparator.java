package utils;

import simulator.event.Event;

import java.util.Comparator;

public class EventComparator<T extends Comparable<T>> implements Comparator<Event> {
    @Override
    public int compare(Event a, Event b) {
        if (a == b) return 0;
        return a.getTime() > b.getTime() ? 1 : -1;
    }
}