package simulator;

import enums.EventType;
import enums.MessageType;
import network.Address;
import network.Network;
import network.message.Message;
import network.message.payload.MessagePayload;
import network.message.payload.election.*;
import simulator.event.Event;
import simulator.event.LeaderCheckEvent;
import simulator.event.ReceiveMsgEvent;
import simulator.event.ResponseCheckEvent;
import utils.AddressComparator;
import utils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    private final Address id;
    private final Membership membership;
    private final EventService eventService;
    private final AtomicInteger seqNo;

    // algorithm related variables
    private ConcurrentLinkedQueue<Address> queryReceivedIds;
    private Address temp_id; // temporary minimum id
    private Address leader_id;
    private Lock temp_id_lock;
    private Lock leader_id_lock;

    public Server(Address id) {
        this.id = id;
        this.membership = new Membership(Config.membership_file, Config.num_servers, id);
        this.eventService = new EventService(id, this);
        this.seqNo = new AtomicInteger(0);

        this.queryReceivedIds = new ConcurrentLinkedQueue<>();
        this.temp_id = null;
        this.leader_id = null;
        this.temp_id_lock = new ReentrantLock();
        this.leader_id_lock = new ReentrantLock();
    }

    public void processEvent(Event event) {
        if (event.getType() == EventType.RECEIVE_MSG) {
            processMsg(((ReceiveMsgEvent) event).getMsg());
        } else if (event.getType() == EventType.RESPONSE_CHECK) {
            checkQueryResponse((ResponseCheckEvent) event);
        } else if (event.getType() == EventType.LEADER_CHECK) {
            checkLeader((LeaderCheckEvent) event);
        } else {
            throw new RuntimeException("Event type " + event.getType() + " not found!!!");
        }
    }

    private void checkLeader(LeaderCheckEvent event) {
        if (leader_id == null) {
            LOG.log(Level.INFO, "Leader id not set, resending notify leader message.");
            MessagePayload payload = new NotifyLeaderPayload();
            Message msg = new Message(id, seqNo.incrementAndGet(), temp_id, payload);
            Network.unicast(msg);

            Event next_event = new LeaderCheckEvent(LogicalTime.time + Config.event_check_timeout);
            eventService.addEvent(next_event);
        }
    }

    private void checkQueryResponse(ResponseCheckEvent event) {
        List<Address> target_nodes = event.getTargetNodes();
        if (queryReceivedIds.size() < target_nodes.size()) {
            // missing some query response message
            LOG.log(Level.INFO, "Resending query message to unresponsive nodes.");
            sendQuery(target_nodes.size() - queryReceivedIds.size(), new ArrayList<>(queryReceivedIds));
            return;
        }

        MessagePayload payload = new NotifyLeaderPayload();
        Message msg = new Message(id, seqNo.incrementAndGet(), temp_id, payload);
        Network.unicast(msg);

        Event next_event = new LeaderCheckEvent(LogicalTime.time + Config.event_check_timeout);
        eventService.addEvent(next_event);
    }

    private void processMsg(Message msg) {
        MessagePayload payload = msg.getPayload();
        if (payload.getType() == MessageType.QUERY) {
            // send back with the lowest hash id
            Address lowest_id = membership.getLowestActiveId();
            MessagePayload response_payload = new QueryResponsePayload(lowest_id);
            Message response_msg = new Message(id, seqNo.incrementAndGet(), msg.getSrc(), response_payload);
            Network.unicast(response_msg);
        } else if (payload.getType() == MessageType.QUERY_RESPONSE) {
            queryReceivedIds.add(msg.getSrc());
            Address new_id = ((QueryResponsePayload) payload).getLowestId();
            temp_id_lock.lock();
            temp_id = AddressComparator.getMin(temp_id, new_id);
            temp_id_lock.unlock();
        } else if (payload.getType() == MessageType.NOTIFY_LEADER) {
            leader_id_lock.lock();
            leader_id = new Address(id.getId());
            leader_id_lock.unlock();
            LOG.log(Level.INFO, "Leader " + leader_id + " gets notified at time " + LogicalTime.time);
            MessagePayload response_payload = new LeaderPayload();
            Message response_msg = new Message(id, seqNo.incrementAndGet(), null, response_payload);
            Network.multicast(response_msg, membership.getAllNodes(), true);
        } else if (payload.getType() == MessageType.LEADER) {
            leader_id_lock.lock();
            leader_id = msg.getSrc();
            leader_id_lock.unlock();
            LOG.log(Level.INFO, "Leader " + leader_id + " gets recognized by " + id + " at time " + LogicalTime.time);
            MessagePayload response_payload = new LeaderAckPayload();
            Message response_msg = new Message(id, seqNo.incrementAndGet(), msg.getSrc(), response_payload);
            Network.unicast(response_msg);
        } else if (payload.getType() == MessageType.LEADER_ACK) {
            // TODO: to be implemented together with resending mechanism
        } else {
            throw new RuntimeException("Message payload type " + payload.getType() + " not found!!!");
        }
    }

    public void sendQuery(int num_nodes, List<Address> excludedNodes) {
        queryReceivedIds.clear();

        List<Address> target_nodes = membership.getRandomNodes(num_nodes, excludedNodes);
        LOG.log(Level.INFO, id + ": sending query message to " + target_nodes);
        MessagePayload payload = new QueryPayload();

        for (Address ip: target_nodes) {
            Message msg = new Message(id, seqNo.incrementAndGet(), ip, payload);
            Network.unicast(msg);
        }

        // check response after certain time
        // TODO: instead of passing target_nodes, we can just pass in the size to reduce overhead
        Event event = new ResponseCheckEvent(LogicalTime.time + Config.event_check_timeout, target_nodes);
        eventService.addEvent(event);
    }

    public void updateMembership() {
        membership.update();
    }

    public EventService getEventService() {
        return eventService;
    }
}
