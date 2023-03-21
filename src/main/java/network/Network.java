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
import utils.metric.NetworkMetric;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
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
    private static final ReadWriteLock topologyLock = new ReentrantReadWriteLock();
    private static List<Address> addresses;

    public static void initialize(List<Address> addresses) {
        Network.addresses = addresses;
        // parse topology file
        try {
            List<String> content = Files.readAllLines(Paths.get(Config.topologyFile));
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

        buildTopology();
    }

    // return value indicates whether there is a partition
    private static void buildTopology() {
        topologyLock.writeLock().lock();

        // generate physical distance
        for (Address from : coordinates.keySet()) {
            for (Address to : coordinates.keySet()) {
                if (from.equals(to)) continue;
                double physicalDist = getPhysicalDist(
                        coordinates.get(from).getKey(), coordinates.get(from).getValue(),
                        coordinates.get(to).getKey(), coordinates.get(to).getValue());
                if (!distanceMap.containsKey(from)) {
                    distanceMap.put(from, new HashMap<>());
                }
                if (!distanceMap.containsKey(to)) {
                    distanceMap.put(to, new HashMap<>());
                }
                distanceMap.get(from).put(to, physicalDist);
                distanceMap.get(to).put(from, physicalDist);
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
                if (distance > Config.oneHopRadius) {
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
                    topologyLock.writeLock().unlock();
                    Logging.log(Level.WARNING, null, "Network partition detected.");
                    return;
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

        topologyLock.writeLock().unlock();
    }

    private static double getPhysicalDist(Double x1, Double y1, Double x2, Double y2) {
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    private static Address getNextHop(Address src, Address dst) {
        if (src.equals(dst)) return dst;
        topologyLock.readLock().lock();
        Address nextHop = routingTable.get(src).get(dst);
        topologyLock.readLock().unlock();
        return nextHop;
    }

    public static void unicast(Message msg) {
        if (msg.getSrc() == msg.getCurr()) {
            NetworkMetric.e2eRecord(msg.getSrc(), msg.getDst(), msg.getByteSize());
            Logging.log(Level.FINE, msg.getSrc(), "Sending message " + msg);
        }
        Address nextHop = getNextHop(msg.getCurr(), msg.getDst());
        msg.setCurr(nextHop);
        NetworkMetric.h2hRecord(msg.getCurr(), nextHop, msg.getByteSize());
        LOG.log(Level.FINER, "Routing message " + msg + " through node " + nextHop);
        // if (Config.random.nextDouble() < Config.msgDropRate) {
        //     LOG.log(Level.FINE, "Message " + msg + " dropped.");
        //     System.out.println("Message dropped from " + msg.getSrc() + " to " + msg.getDst());
        //     return;
        // }
        Event event;
        if (msg.getDst() == nextHop)
            event = new ReceiveMsgEvent(LogicalTime.time + Config.random.nextInt(Config.oneHopDelay), nextHop, msg);
        else
            event = new RouteMsgEvent(LogicalTime.time + Config.random.nextInt(Config.oneHopDelay), nextHop, msg);
        EventService.addEvent(event);
    }

    public static void multicast(Message msg, List<Address> targetNodes) {
        Logging.log(Level.FINE, msg.getSrc(), "Multicasting message " + msg);
        for (Address ip: targetNodes) {
            Message currMsg = new Message(msg, ip);
            unicast(currMsg);
        }
    }

    public static List<Address> getAddresses() {
        return addresses;
    }
}
