package simulator;

import enums.*;
import network.Address;
import network.Network;
import network.message.Message;
import network.message.payload.MessagePayload;
import network.message.payload.election.*;
import simulator.event.*;
import utils.*;
import javafx.util.Pair;
import utils.metric.AlgorithmMetric;
import utils.metric.QualityMetric;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Server {
    private final Address id;
    private final Membership membership;
    private final AtomicInteger seqNo = new AtomicInteger(0);
    private final AtomicInteger messageNo = new AtomicInteger(0);
    private final ConcurrentHashMap<Integer, HashSet<Address>> ackedIds = new ConcurrentHashMap<>();

    // algorithm related variables
    private final ConcurrentLinkedQueue<Address> queryReceivedIds = new ConcurrentLinkedQueue<>();
    private Address tempId = null; // temporary minimum id
    private Address leaderId = null;
    private final Lock tempIdLock = new ReentrantLock();
    private final Lock leaderIdLock = new ReentrantLock();
    private final AtomicInteger leaderNo = new AtomicInteger(0);
    private int receivedLeaderNo = -1;
    private int leaderNotifyCount = 10; // resend notify leader at most x times
    private final Set<Address> leaderNodes = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Address, Integer> excludedNodes = new ConcurrentHashMap<>();

    public Server(Address id) {
        this.id = id;
        this.membership = new Membership(id);
    }

    private synchronized List<Address> getPotentialLeaders() {
        // find the top numSuspectCount nodes and exclude them
        List<Map.Entry<Address, Integer>> list = excludedNodes.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toList());
        List<Map.Entry<Address, Integer>> listFiltered = list.stream()
                .filter(entry -> entry.getValue() >= Config.suspectCountThreshold).collect(Collectors.toList());
        List<Address> finalExcludedNodes = listFiltered
                .subList(0, Math.min(Config.numSuspectCount, listFiltered.size())).stream()
                .map(Map.Entry::getKey).collect(Collectors.toList());
        List<Address> potentialLeaders = leaderNodes.stream().filter(key -> !finalExcludedNodes.contains(key))
                .collect(Collectors.toList());

        Logging.log(Level.FINE, id, "Received leader ids: " + leaderNodes);
        Logging.log(Level.FINE, id, "Top suspect count nodes to be excluded: " + finalExcludedNodes);
        Logging.log(Level.FINE, id, "Potential leaders: " + potentialLeaders);
        AlgorithmMetric.setExcludedSuspects(finalExcludedNodes);

        return potentialLeaders;
    }

    public void processEvent(Event event) {
        Logging.log(Level.FINER, id, "Processing event: " + event.toString());
        if (event.getType() == EventType.RECEIVE_MSG) {
            processMsg(((ReceiveMsgEvent) event).getMsg());

        } else if (event.getType() == EventType.RESPONSE_CHECK) {
            int numNodes = ((ResponseCheckEvent) event).getNumNodes();
            if (queryReceivedIds.size() < numNodes) {
                // missing some query response message
                Logging.log(Level.INFO, id, "Resending query message to unresponsive nodes.");
                Logging.log(Level.FINE, id, "Received ids are: " + queryReceivedIds);
                sendQuery(numNodes - queryReceivedIds.size(), new ArrayList<>(queryReceivedIds));
                return;
            }

            for (Address ip: Network.getAddresses()) {
                EventService.addEvent(new SetSuspectEvent(LogicalTime.time, ip));
            }

            if (Config.algorithm == 2 || Config.algorithm == 4) {
                // we must have notified the tempId previously
                return;
            }

            if (Config.algorithm == 3) {
                List<Address> potentialLeaders = getPotentialLeaders();
                if (potentialLeaders.size() == 0) {
                    Logging.log(Level.INFO, id, "Algorithm 3 resulting potential leaders set is empty. Restarting...");
                    Config.numLowNode = Math.min(Config.numLowNode + 1, Config.numServers);
                    Config.numSuspectCount = Math.max(Config.numSuspectCount - 1, 0);
                    sendQuery(Config.f + Config.k + 1, null);
                    return;
                }

                tempIdLock.lock();
                tempId = potentialLeaders.stream().min(new AddressComparator<>()).orElse(new Address(-1));
                tempIdLock.unlock();
            }

            Logging.log(Level.INFO, id, "Node " + tempId + " is chosen as the leader. Sending notify leader...");

            QualityMetric.setLeader(tempId);
            MessagePayload payload = new NotifyLeaderPayload(leaderNo.incrementAndGet());
            Message msg = new Message(id, seqNo.incrementAndGet(), tempId, payload);
            Network.unicast(msg);

            Event nextEvent = new LeaderCheckEvent(LogicalTime.time + Config.eventCheckTimeout, id);
            EventService.addEvent(nextEvent);

        } else if (event.getType() == EventType.LEADER_CHECK) {
            leaderIdLock.lock();
            if (leaderId == null) {
                leaderIdLock.unlock();
                if (leaderNotifyCount == 0) {
                    Logging.log(Level.WARNING, id, "Resending notify leader too many times, leader might be dead.");
                    return;
                }
                leaderNotifyCount--;
                Logging.log(Level.INFO, id, "Leader id not set, resending notify leader message.");
                MessagePayload payload = new NotifyLeaderPayload(leaderNo.incrementAndGet());
                Message msg = new Message(id, seqNo.incrementAndGet(), tempId, payload);
                Network.unicast(msg);

                Event nextEvent = new LeaderCheckEvent(LogicalTime.time + Config.eventCheckTimeout, id);
                EventService.addEvent(nextEvent);
                return;
            }
            leaderIdLock.unlock();

        } else if (event.getType() == EventType.ROUTE_MSG) {
            Network.unicast(((RouteMsgEvent) event).getMsg());

        } else if (event.getType() == EventType.RESEND) {
            Message msg = ((ResendEvent) event).getMsg();
            leaderIdLock.lock();
            if (msg.getPayload().getType() == MessageType.LEADER && leaderId.lt(id)) {
                // a new leader has been elected, no need to wait for other leader ack
                leaderIdLock.unlock();
                return;
            }
            leaderIdLock.unlock();
            List<Address> targetNodes = ((ResendEvent) event).getTargetNodes();
            List<Address> newTargetNodes = targetNodes.stream()
                    .filter(key -> !ackedIds.getOrDefault(msg.getMessageNo(), new HashSet<>()).contains(key)).collect(Collectors.toList());
            if (newTargetNodes.size() == 0) {
                return;
            }
            Logging.log(Level.FINE, id, "Resending msg (" + msg + ") to nodes: " + newTargetNodes);
            Network.multicast(msg, newTargetNodes);

            int ttl = ((ResendEvent) event).getTtl();
            if (ttl == 0) {
                Logging.log(Level.FINE, id, "Resending canceled");
                return;
            }
            Event nextEvent = new ResendEvent(LogicalTime.time + Config.eventCheckTimeout, id, msg, newTargetNodes, ttl - 1);
            EventService.addEvent(nextEvent);

        } else if (event.getType() == EventType.SET_SUSPECT) {
            AlgorithmMetric.setTrueSuspects(membership.getSuspects());
        } else {
            throw new RuntimeException("Event type " + event.getType() + " not found!!!");
        }
    }

    private void processMsg(Message msg) {
        Logging.log(Level.FINE, id, "Receiving message " + msg);
        MessagePayload payload = msg.getPayload();
        if (payload.getType() == MessageType.QUERY) {
            if (Config.algorithm == 3 || Config.algorithm == 4) {
                Pair<List<Address>, List<Pair<Address, Integer>>> idPair = membership.getPairIds(
                        ((QueryPayload) payload).getNumNodes(),
                        ((QueryPayload) payload).getNumSuspects(),
                        true);
                Logging.log(Level.FINER, id, "lows: " + idPair.getKey() + ", suspects: " + idPair.getValue());
                MessagePayload responsePayload = new QueryResponsePayload(idPair.getKey(), idPair.getValue());
                Message responseMsg = new Message(id, seqNo.incrementAndGet(), msg.getSrc(), responsePayload);
                Network.unicast(responseMsg);
                return;
            }

            // send back with the lowest hash id
            Address lowestId = membership.getLowestActiveId();
            MessagePayload responsePayload = new QueryResponsePayload(lowestId);
            Message responseMsg = new Message(id, seqNo.incrementAndGet(), msg.getSrc(), responsePayload);
            Network.unicast(responseMsg);

        } else if (payload.getType() == MessageType.QUERY_RESPONSE) {
            queryReceivedIds.add(msg.getSrc());
            Address newId = ((QueryResponsePayload) payload).getLowestId();
            tempIdLock.lock();
            // algorithm 2 related
            if (Config.algorithm == 2 && newId.lt(tempId)) {
                tempId = newId;
                Address currId = new Address(tempId);
                tempIdLock.unlock();
                QualityMetric.setLeader(currId);
                MessagePayload newPayload = new NotifyLeaderPayload(leaderNo.incrementAndGet());
                Message newMsg = new Message(id, seqNo.incrementAndGet(), currId, newPayload);
                Network.unicast(newMsg);

                Event nextEvent = new LeaderCheckEvent(LogicalTime.time + Config.eventCheckTimeout, id);
                EventService.addEvent(nextEvent);
                return;
            }
            // algorithm 3 & 4 related
            if (Config.algorithm == 3 || Config.algorithm == 4) {
                tempIdLock.unlock();
                leaderNodes.addAll(((QueryResponsePayload) payload).getLowestIds());
                ((QueryResponsePayload) payload).getHighestSuspectIds()
                        .forEach(entry -> excludedNodes.put(entry.getKey(), entry.getValue()
                                + excludedNodes.getOrDefault(entry.getKey(), 0)));

                if (Config.algorithm == 4) {
                    List<Address> potentialLeaders = getPotentialLeaders();
                    if (potentialLeaders.size() == 0) {
                        return;
                    }
                    tempIdLock.lock();
                    Address currId = potentialLeaders.stream().min(new AddressComparator<>()).orElse(new Address(-1));
                    if (currId.equals(tempId)) return;
                    tempId = new Address(currId);
                    tempIdLock.unlock();

                    QualityMetric.setLeader(currId);
                    MessagePayload newPayload = new NotifyLeaderPayload(leaderNo.incrementAndGet());
                    Message newMsg = new Message(id, seqNo.incrementAndGet(), currId, newPayload);
                    Network.unicast(newMsg);

                    Event nextEvent = new LeaderCheckEvent(LogicalTime.time + Config.eventCheckTimeout, id);
                    EventService.addEvent(nextEvent);
                }
                return;
            }
            tempId = AddressComparator.getMin(tempId, newId);
            tempIdLock.unlock();

        } else if (payload.getType() == MessageType.NOTIFY_LEADER) {
            int newLeaderNo = ((NotifyLeaderPayload) payload).getLeaderNo();
            leaderIdLock.lock();
            if (receivedLeaderNo >= newLeaderNo) {
                leaderIdLock.unlock();
                return;
            }
            receivedLeaderNo = newLeaderNo;
            leaderId = new Address(id);
            AlgorithmMetric.setCorrectLeader(id, leaderId);
            MessagePayload responsePayload = new LeaderPayload(receivedLeaderNo);
            leaderIdLock.unlock();
            Logging.log(Level.INFO, id, "Leader gets notified");
            Message responseMsg = new Message(id, seqNo.incrementAndGet(), null, responsePayload, messageNo.incrementAndGet());
            ackedIds.put(responseMsg.getMessageNo(), new HashSet<>());
            Network.multicast(responseMsg, membership.getAllNodes(true));

            Event event = new ResendEvent(LogicalTime.time + Config.eventCheckTimeout, id, responseMsg, membership.getAllNodes(true), 10);
            EventService.addEvent(event);

        } else if (payload.getType() == MessageType.LEADER) {
            int newLeaderNo = ((LeaderPayload) payload).getLeaderNo();
            leaderIdLock.lock();
            if (receivedLeaderNo >= newLeaderNo) {
                leaderIdLock.unlock();
                return;
            }
            receivedLeaderNo = newLeaderNo;
            leaderId = msg.getSrc();
            AlgorithmMetric.setCorrectLeader(id, leaderId);

            Logging.log(Level.INFO, id, "Leader " + leaderId + " gets recognized");
            leaderIdLock.unlock();
            MessagePayload responsePayload = new LeaderAckPayload();
            Message responseMsg = new Message(id, seqNo.incrementAndGet(), msg.getSrc(), responsePayload, msg.getMessageNo());
            Network.unicast(responseMsg);

        } else if (payload.getType() == MessageType.LEADER_ACK) {
            ackedIds.get(msg.getMessageNo()).add(msg.getSrc());

        } else {
            throw new RuntimeException("Message payload type " + payload.getType() + " not found!!!");
        }
    }

    public void sendQuery(int numNodes, List<Address> excludes) {
        queryReceivedIds.clear();
        leaderNodes.clear();
        excludedNodes.clear();
        leaderNotifyCount = 10;

        List<Address> targetNodes = membership.getRandomNodes(numNodes, excludes, true);
        Logging.log(Level.INFO, id, "Node sending query message to " + targetNodes);
        MessagePayload payload = new QueryPayload();
        if (Config.algorithm == 3 || Config.algorithm == 4) {
            payload = new QueryPayload(Config.numLowNode, Config.numSuspectCount);
        }

        for (Address ip: targetNodes) {
            Message msg = new Message(id, seqNo.incrementAndGet(), ip, payload);
            Network.unicast(msg);
        }

        // check response after certain time
        Event event = new ResponseCheckEvent(LogicalTime.time + Config.eventCheckTimeout, id, targetNodes.size());
        EventService.addEvent(event);
    }

    public void updateMembership() {
        membership.update();
    }

    public Address getLeaderId() {
        return leaderId;
    }
}
