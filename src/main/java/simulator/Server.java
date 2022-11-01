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
    private final ConcurrentHashMap<Integer, HashSet<Address>> acked_ids = new ConcurrentHashMap<>();

    // algorithm related variables
    private final ConcurrentLinkedQueue<Address> queryReceivedIds = new ConcurrentLinkedQueue<>();
    private Address temp_id = null; // temporary minimum id
    private Address leader_id = null;
    private final Lock temp_id_lock = new ReentrantLock();
    private final Lock leader_id_lock = new ReentrantLock();
    private int leader_notify_count = 3; // resend notify leader at most 3 times
    private final Set<Address> leaderNodes = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Address, Integer> excludedNodes = new ConcurrentHashMap<>();

    public Server(Address id) {
        this.id = id;
        this.membership = new Membership(id);
    }

    public void processEvent(Event event) {
        Logging.log(Level.FINER, id, "Processing event: " + event.toString());
        if (event.getType() == EventType.RECEIVE_MSG) {
            processMsg(((ReceiveMsgEvent) event).getMsg());

        } else if (event.getType() == EventType.RESPONSE_CHECK) {
            int num_nodes = ((ResponseCheckEvent) event).getNumNodes();
            if (queryReceivedIds.size() < num_nodes) {
                // missing some query response message
                Logging.log(Level.INFO, id, "Resending query message to unresponsive nodes.");
                Logging.log(Level.FINE, id, "Received ids are: " + queryReceivedIds);
                sendQuery(num_nodes - queryReceivedIds.size(), new ArrayList<>(queryReceivedIds));
                return;
            }

            if (Config.algorithm == 2) {
                // we must have notified the temp_id previously
                return;
            }

            if (Config.algorithm == 3) {
                // find the top num_suspect_count nodes and exclude them
                List<Map.Entry<Address, Integer>> list = excludedNodes.entrySet().stream()
                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toList());
                List<Map.Entry<Address, Integer>> list_filtered = list.stream()
                        .filter(entry -> entry.getValue() >= Config.suspect_count_threshold).collect(Collectors.toList());
                List<Address> final_excluded_nodes = list_filtered
                        .subList(0, Math.min(Config.num_suspect_count, list_filtered.size())).stream()
                        .map(Map.Entry::getKey).collect(Collectors.toList());
                List<Address> potentialLeaders = leaderNodes.stream().filter(key -> !final_excluded_nodes.contains(key))
                        .collect(Collectors.toList());

                if (potentialLeaders.size() == 0) {
                    Logging.log(Level.INFO, id, "Algorithm 3 resulting potential leaders set is empty. Restarting...");
                    Config.num_low_node = Math.min(Config.num_low_node + 1, Config.num_servers);
                    Config.num_suspect_count = Math.max(Config.num_suspect_count - 1, 0);
                    sendQuery(Config.f + Config.k + 1, null);
                    return;
                }

                Logging.log(Level.FINE, id, "Received leader ids: " + leaderNodes);
                Logging.log(Level.FINE, id, "Top suspect count nodes to be excluded: " + final_excluded_nodes);
                Logging.log(Level.FINE, id, "Potential leaders: " + potentialLeaders);

                temp_id_lock.lock();
                temp_id = potentialLeaders.stream().min(new AddressComparator<>()).get();
                temp_id_lock.unlock();
            }

            Logging.log(Level.INFO, id, "Node " + temp_id + " is chosen as the leader. Sending notify leader...");

            MessagePayload payload = new NotifyLeaderPayload();
            Message msg = new Message(id, seqNo.incrementAndGet(), temp_id, payload);
            Network.unicast(msg);

            Event next_event = new LeaderCheckEvent(LogicalTime.time + Config.event_check_timeout, id);
            EventService.addEvent(next_event);

        } else if (event.getType() == EventType.LEADER_CHECK) {
            if (leader_id == null) {
                if (leader_notify_count == 0) {
                    Logging.log(Level.WARNING, id, "Resending notify leader too many times, leader might be dead.");
                    return;
                }
                leader_notify_count--;
                Logging.log(Level.INFO, id, "Leader id not set, resending notify leader message.");
                MessagePayload payload = new NotifyLeaderPayload();
                Message msg = new Message(id, seqNo.incrementAndGet(), temp_id, payload);
                Network.unicast(msg);

                Event next_event = new LeaderCheckEvent(LogicalTime.time + Config.event_check_timeout, id);
                EventService.addEvent(next_event);
            }

        } else if (event.getType() == EventType.ROUTE_MSG) {
            Network.unicast(((RouteMsgEvent) event).getMsg());

        } else if (event.getType() == EventType.RESEND_EVENT) {
            Message msg = ((ResendEvent) event).getMsg();
            List<Address> target_nodes = ((ResendEvent) event).getTargetNodes();
            List<Address> new_target_nodes = target_nodes.stream()
                    .filter(key -> acked_ids.getOrDefault(msg.getMessageNo(), new HashSet<>()).contains(key)).collect(Collectors.toList());
            if (new_target_nodes.size() == 0) return;
            Logging.log(Level.FINE, id, "Resending msg (" + msg + ") to nodes: " + new_target_nodes);
            Network.multicast(msg, new_target_nodes, false);

            Event next_event = new ResendEvent(LogicalTime.time + Config.event_check_timeout, id, msg, new_target_nodes);
            EventService.addEvent(next_event);

        } else {
            throw new RuntimeException("Event type " + event.getType() + " not found!!!");
        }
    }

    private void processMsg(Message msg) {
        Logging.log(Level.FINE, id, "Receiving message " + msg);
        MessagePayload payload = msg.getPayload();
        if (payload.getType() == MessageType.QUERY) {
            if (Config.algorithm == 3) {
                Pair<List<Address>, List<Pair<Address, Integer>>> id_pair = membership.getPairIds(
                        ((QueryPayload) payload).getNumNodes(),
                        ((QueryPayload) payload).getNumSuspects(),
                        true);
                Logging.log(Level.FINER, id, "lows: " + id_pair.getKey() + ", suspects: " + id_pair.getValue());
                MessagePayload response_payload = new QueryResponsePayload(id_pair.getKey(), id_pair.getValue());
                Message response_msg = new Message(id, seqNo.incrementAndGet(), msg.getSrc(), response_payload);
                Network.unicast(response_msg);
                return;
            }

            // send back with the lowest hash id
            Address lowest_id = membership.getLowestActiveId();
            MessagePayload response_payload = new QueryResponsePayload(lowest_id);
            Message response_msg = new Message(id, seqNo.incrementAndGet(), msg.getSrc(), response_payload);
            Network.unicast(response_msg);

        } else if (payload.getType() == MessageType.QUERY_RESPONSE) {
            queryReceivedIds.add(msg.getSrc());
            Address new_id = ((QueryResponsePayload) payload).getLowestId();
            temp_id_lock.lock();
            // algorithm 2 related
            if (Config.algorithm == 2 && new_id.lessThan(temp_id)) {
                temp_id = new_id;
                Address curr_id = new Address(temp_id);
                temp_id_lock.unlock();
                MessagePayload new_payload = new NotifyLeaderPayload();
                Message new_msg = new Message(id, seqNo.incrementAndGet(), curr_id, new_payload);
                Network.unicast(new_msg);

                Event next_event = new LeaderCheckEvent(LogicalTime.time + Config.event_check_timeout, id);
                EventService.addEvent(next_event);
                return;
            }
            // algorithm 3 related
            if (Config.algorithm == 3) {
                leaderNodes.addAll(((QueryResponsePayload) payload).getLowestIds());
                ((QueryResponsePayload) payload).getHighestSuspectIds()
                        .forEach(entry -> excludedNodes.put(entry.getKey(), entry.getValue()
                                + excludedNodes.getOrDefault(entry.getKey(), 0)));
                return;
            }
            temp_id = AddressComparator.getMin(temp_id, new_id);
            temp_id_lock.unlock();

        } else if (payload.getType() == MessageType.NOTIFY_LEADER) {
            leader_id_lock.lock();
            if (!id.lessThan(leader_id)) {
                leader_id_lock.unlock();
                return;
            }
            leader_id = AddressComparator.getMin(leader_id, new Address(id));
            leader_id_lock.unlock();
            Logging.log(Level.INFO, id, "Leader gets notified");
            MessagePayload response_payload = new LeaderPayload();
            Message response_msg = new Message(id, seqNo.incrementAndGet(), null, response_payload, messageNo.incrementAndGet());
            Network.multicast(response_msg, membership.getAllNodes(), true);

            Event event = new ResendEvent(LogicalTime.time + Config.event_check_timeout, id, response_msg, membership.getAllNodes());
            EventService.addEvent(event);

        } else if (payload.getType() == MessageType.LEADER) {
            leader_id_lock.lock();
            if (!msg.getSrc().lessThan(leader_id)) {
                leader_id_lock.unlock();
                return;
            }
            leader_id = AddressComparator.getMin(leader_id, msg.getSrc());
            leader_id_lock.unlock();
            Logging.log(Level.INFO, id, "Leader " + leader_id + " gets recognized");
            MessagePayload response_payload = new LeaderAckPayload();
            Message response_msg = new Message(id, seqNo.incrementAndGet(), msg.getSrc(), response_payload, msg.getMessageNo());
            Network.unicast(response_msg);

        } else if (payload.getType() == MessageType.LEADER_ACK) {
            acked_ids.getOrDefault(msg.getMessageNo(), new HashSet<>()).add(msg.getSrc());

        } else {
            throw new RuntimeException("Message payload type " + payload.getType() + " not found!!!");
        }
    }

    public void sendQuery(int num_nodes, List<Address> excludes) {
        queryReceivedIds.clear();
        temp_id = null;
        leader_id = null;
        leaderNodes.clear();
        excludedNodes.clear();
        leader_notify_count = 3;

        List<Address> target_nodes = membership.getRandomNodes(num_nodes, excludes, true);
        Logging.log(Level.INFO, id, "Node sending query message to " + target_nodes);
        MessagePayload payload = new QueryPayload();
        if (Config.algorithm == 3) {
            payload = new QueryPayload(Config.num_low_node, Config.num_suspect_count);
        }

        for (Address ip: target_nodes) {
            Message msg = new Message(id, seqNo.incrementAndGet(), ip, payload);
            Network.unicast(msg);
        }

        // check response after certain time
        Event event = new ResponseCheckEvent(LogicalTime.time + Config.event_check_timeout, id, target_nodes.size());
        EventService.addEvent(event);
    }

    public void updateMembership() {
        membership.update();
    }

    public Address getLeaderId() {
        return leader_id;
    }
}
