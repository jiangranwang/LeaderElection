package simulator;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import enums.EventType;
import network.Address;
import network.Network;
import network.message.Message;
import network.message.payload.MessagePayload;
import network.message.payload.election.QueryPayload;
import network.message.payload.election.QueryResponsePayload;
import simulator.event.Event;
import simulator.event.ReceiveMsgEvent;
import utils.Config;

public class Simulator {
    private static final Logger LOG = Logger.getLogger(Simulator.class.getName());
    private static final Random random = new Random();

    private static final List<Address> addresses = new ArrayList<>();
    private static final HashMap<Address, Server> servers = new HashMap<>();

    private static void initialize(String[] args) {
        if (args.length != 1) {
            LOG.log(Level.SEVERE, "You should provide the config file as the first argument");
            System.exit(1);
        }

        LOG.log(Level.INFO, "Initializing...");
        Config.parse(args[0]);

        // build ips
        for (int id = 0; id < Config.num_servers; id++) {
            Address address = new Address(id);
            addresses.add(address);
        }

        for (Address ip: addresses) {
            Server server = new Server(ip);

            servers.put(ip, server);
        }

        Network.initialize(addresses);
        EventService.initialize(servers);
    }

    private static void run() {
        LogicalTime.time = 0;
        // pick a server as the coordinator
        Address coordinator = addresses.get(0);
        // get k + f + 1 random servers to send query message
        int num_nodes = Math.min(Config.num_servers, Config.f + Config.k + 1);
        servers.get(coordinator).sendQuery(num_nodes, null);

        TimerTask updateMembership = new TimerTask() {
            public void run() {
                for (Map.Entry<Address, Server> entry : servers.entrySet()) {
                    entry.getValue().updateMembership();
                }
            }
        };

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(updateMembership, 0, Config.granularity);

        EventService.processAll();

        timer.cancel();
    }

    public static void main(String[] args) {
        initialize(args);
        run();
    }
}
