package network;

import network.message.Message;
import simulator.EventService;
import simulator.LogicalTime;
import simulator.event.Event;
import simulator.event.ReceiveMsgEvent;
import utils.Config;
import utils.Logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class Network {
    private static List<Address> addresses = new ArrayList<>();

    private static final Random random = new Random();

    public static void initialize(List<Address> addresses) {
        Network.addresses = addresses;
    }

    // TODO: route message using hops & message drop (exclude multicast, which need resend mechanism)
    public static void unicast(Message msg) {
        Logging.log(Level.FINE, msg.getSrc(), "Sending message " + msg);
        Event event = new ReceiveMsgEvent(LogicalTime.time + random.nextInt(Config.one_hop_delay), msg.getDst(), msg);
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
