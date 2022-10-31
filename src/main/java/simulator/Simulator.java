package simulator;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import network.Address;
import network.Network;
import utils.Config;

public class Simulator {
    private static final Logger LOG = Logger.getLogger(Simulator.class.getName());
    private static final Random random = new Random();

    private static final List<Address> addresses = new ArrayList<>();
    private static final HashMap<Address, Server> servers = new HashMap<>();
    private final static HashMap<Address, EventService> eventServices = new HashMap<>();

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
            EventService eventService = server.getEventService();

            servers.put(ip, server);
            eventServices.put(ip, eventService);
        }

        Network.initialize(eventServices, addresses);
    }

    private static void run() {
        LogicalTime.time = 0;
        // pick a server as the coordinator
        Address coordinator = addresses.get(0);
        // get k + f + 1 random servers to send query message
        int num_nodes = Math.min(Config.num_servers, Config.f + Config.k + 1);
        servers.get(coordinator).sendQuery(num_nodes, null);
        while (LogicalTime.time < Config.end_time) {
            // process existing buffered events
            for (Map.Entry<Address, EventService> entry: eventServices.entrySet()) {
                entry.getValue().processAll(LogicalTime.time);
            }

            LogicalTime.time += Config.granularity;
            for (Map.Entry<Address, Server> entry: servers.entrySet()) {
                entry.getValue().updateMembership();
            }
        }
    }

    public static void main(String[] args) {
        initialize(args);
        run();
    }
}
