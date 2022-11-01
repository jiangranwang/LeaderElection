package network;

import javafx.util.Pair;
import network.message.Message;
import simulator.EventService;
import simulator.LogicalTime;
import simulator.event.Event;
import simulator.event.ReceiveMsgEvent;
import simulator.event.RouteMsgEvent;
import utils.Config;
import utils.Logging;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class Network {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Network.class.getName());

    private static final HashMap<Address, Pair<Double, Double>> coordinates = new HashMap<>();
    private static final HashMap<Address, HashMap<Address, Double>> distanceMap = new HashMap<>();
    private static final HashMap<Address, HashMap<Address, Double>> hopDistMap = new HashMap<>();
    private static final HashMap<Address, HashMap<Address, Double>> hopNumMap = new HashMap<>();
    private static final HashMap<Address, HashMap<Address, Address>> routingTable = new HashMap<>();
    private static final ReadWriteLock topology_lock = new ReentrantReadWriteLock();
    private static final Random random = new Random();

    public static void initialize(List<Address> addresses) {
        // parse topology file
        try {
            List<String> content = Files.readAllLines(Paths.get(Config.topology_file));
            if (addresses.size() != content.size()) {
                LOG.log(Level.SEVERE, "Number of servers (" + addresses.size() + ") and topology file ("
                        + content.size() + ") doesn't match!");
                System.exit(1);
            }
            for (int i = 0; i < content.size(); i++) {
                String[] coordinate = content.get(i).split(",");
                assert coordinate.length == 2;
                coordinates.put(addresses.get(i),
                        new Pair<>(Double.parseDouble(coordinate[0]), Double.parseDouble(coordinate[1])));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        build_topology();
    }

    // return value indicates whether there is a partition
    private static boolean build_topology() {
        topology_lock.writeLock().lock();

        // generate physical distance
        for (Address from : coordinates.keySet()) {
            for (Address to : coordinates.keySet()) {
                if (from.equals(to)) continue;
                double physical_dist = getPhysicalDist(
                        coordinates.get(from).getKey(), coordinates.get(from).getValue(),
                        coordinates.get(to).getKey(), coordinates.get(to).getValue());
                if (!distanceMap.containsKey(from)) {
                    distanceMap.put(from, new HashMap<>());
                }
                if (!distanceMap.containsKey(to)) {
                    distanceMap.put(to, new HashMap<>());
                }
                distanceMap.get(from).put(to, physical_dist);
                distanceMap.get(to).put(from, physical_dist);
            }
        }

        // generate hop distance map & routing table
        for (Address from : coordinates.keySet()) {
            hopDistMap.put(from, new HashMap<>());
            routingTable.put(from, new HashMap<>());
            hopNumMap.put(from, new HashMap<>());
        }
        for (Address from : coordinates.keySet()) {
            for (Address to : coordinates.keySet()) {
                double distance = distanceMap.get(from).getOrDefault(to, 0.0);
                if (distance > Config.one_hop_radius) {
                    hopDistMap.get(from).put(to, Double.MAX_VALUE);
                    hopDistMap.get(to).put(from, Double.MAX_VALUE);
                } else {
                    hopDistMap.get(from).put(to, distance);
                    routingTable.get(from).put(to, to);
                    hopDistMap.get(to).put(from, distance);
                    routingTable.get(to).put(from, from);
                }
            }
        }
        for (Address middle : coordinates.keySet()) {
            for (Address from : coordinates.keySet()) {
                for (Address to : coordinates.keySet()) {
                    if (hopDistMap.get(from).get(middle) + hopDistMap.get(middle).get(to)
                            < hopDistMap.get(from).get(to)) {
                        hopDistMap.get(from).put(to, hopDistMap.get(from).get(middle) + hopDistMap.get(middle).get(to));
                        routingTable.get(from).put(to, routingTable.get(from).get(middle));
                        hopDistMap.get(to).put(from, hopDistMap.get(from).get(middle) + hopDistMap.get(middle).get(to));
                        routingTable.get(to).put(from, routingTable.get(to).get(middle));
                    }
                }
            }
        }

        // check network partition
        for (Address from : coordinates.keySet()) {
            for (Address to : coordinates.keySet()) {
                if (!routingTable.get(from).containsKey(to)) {
                    topology_lock.writeLock().unlock();
                    Logging.log(Level.WARNING, null, "Network partition detected.");
                    return false;
                }
            }
        }

        // generate hop number map
        for (Address from : coordinates.keySet()) {
            for (Address to : coordinates.keySet()) {
                if (from.equals(to)) {
                    hopNumMap.get(from).put(to, 0.0);
                    continue;
                }
                Address nextHop = routingTable.get(from).get(to);
                double hop = 1.0;
                while (!nextHop.equals(to)) {
                    nextHop = routingTable.get(nextHop).get(to);
                    hop += 1.0;
                }
                hopNumMap.get(from).put(to, hop);
            }
        }

        topology_lock.writeLock().unlock();
        return true;
    }

    private static double getPhysicalDist(Double x1, Double y1, Double x2, Double y2) {
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    private static Address getNextHop(Address src, Address dst) {
        if (src.equals(dst)) return dst;
        topology_lock.readLock().lock();
        Address next_hop = routingTable.get(src).get(dst);
        topology_lock.readLock().unlock();
        return next_hop;
    }

    public static void unicast(Message msg) {
        if (msg.getSrc() == msg.getCurr()) {
            Logging.log(Level.FINE, msg.getSrc(), "Sending message " + msg);
        }
        Address next_hop = getNextHop(msg.getCurr(), msg.getDst());
        msg.setCurr(next_hop);
        LOG.log(Level.FINER, "Routing message " + msg + " through node " + next_hop);
        Event event;
        if (msg.getDst() == next_hop)
            event = new ReceiveMsgEvent(LogicalTime.time + random.nextInt(Config.one_hop_delay), next_hop, msg);
        else
            event = new RouteMsgEvent(LogicalTime.time + random.nextInt(Config.one_hop_delay), next_hop, msg);
        EventService.addEvent(event);
    }

    public static void multicast(Message msg, List<Address> target_nodes, boolean excludeSelf) {
        Logging.log(Level.FINE, msg.getSrc(), "Multicasting message " + msg);
        for (Address ip: target_nodes) {
            if (excludeSelf && ip.equals(msg.getSrc())) {
                continue;
            }
            Message curr_msg = new Message(msg, ip);
            unicast(curr_msg);
        }
    }
}
