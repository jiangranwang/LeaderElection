package network;

import network.message.Message;
import simulator.EventService;
import simulator.LogicalTime;
import simulator.event.Event;
import simulator.event.ReceiveMsgEvent;
import utils.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Network {
    private static final Logger LOG = Logger.getLogger(Network.class.getName());

    private static final Random random = new Random();
    private static HashMap<Address, EventService> eventServices;
    private static List<Address> addresses;

    public static void initialize(HashMap<Address, EventService> eventServices, List<Address> addresses) {
        Network.eventServices = eventServices;
        Network.addresses = addresses;
    }

    // TODO: route message using hops & message drop (exclude multicast, which need resend mechanism)
    public static void unicast(Message msg) {
        Address dst = msg.getDst();
        if (!eventServices.containsKey(dst)) {
            LOG.log(Level.WARNING, "Event service " + dst + " does not exist!");
            return;
        }

        LOG.log(Level.INFO, "Sending message " + msg);
        EventService eventService = eventServices.get(dst);
        Event event = new ReceiveMsgEvent(LogicalTime.time + random.nextInt(Config.one_hop_delay), msg);
        eventService.addEvent(event);
    }

    public static void multicast(Message msg, List<Address> target_nodes, boolean excludeSelf) {
        LOG.log(Level.INFO, "Multicasting message " + msg);
        for (Address ip: target_nodes) {
            if (excludeSelf && ip.equals(msg.getSrc())) {
                continue;
            }
            Message curr_msg = new Message(msg, ip);
            unicast(curr_msg);
        }
    }
}
