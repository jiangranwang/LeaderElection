package simulator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import network.Address;
import network.Network;
import org.json.simple.JSONObject;
import utils.Config;
import utils.metric.*;

public class Simulator {
    private static final Logger LOG = Logger.getLogger(Simulator.class.getName());

    private static final List<Address> addresses = new ArrayList<>();
    private static final HashMap<Address, Server> servers = new HashMap<>();

    private static void initialize(String[] args) {
        if (args.length != 1) {
            LOG.log(Level.SEVERE, "You should provide the config file as the first argument");
            System.exit(1);
        }

        LOG.log(Level.INFO, "Initializing...");
        Config.parse(args[0]); // this must be called before anything else

        // build ips
        for (int id = 0; id < Config.numServers; id++) {
            Address ip = new Address(id);
            addresses.add(ip);
            Server server = new Server(ip);
            servers.put(ip, server);
        }

        Network.initialize(addresses);
        EventService.initialize(servers);
        NetworkMetric.initialize(addresses);
    }

    private static void run() {
        LogicalTime.time = 0;
        // pick a server as the coordinator
        Address coordinator = addresses.get(Config.random.nextInt(Config.numServers));
        LOG.log(Level.INFO, "Coordinator is: " + coordinator);
        // get k + f + 1 random servers to send query message
        int num_nodes = Math.min(Config.numServers, Config.f + Config.k + 1);
        AlgorithmMetric.setElectionStartTime(LogicalTime.time);
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

    @SuppressWarnings("unchecked")
    private static void conclude() {
        for (Map.Entry<Address, Server> entry: servers.entrySet()) {
            LOG.log(Level.INFO, "Node " + entry.getKey() + " has leader: " + entry.getValue().getLeaderId());
            if (entry.getValue().getLeaderId() == null) {
                String msg = "Node " + entry.getKey() + " does not have correct leader! It has "
                        + entry.getValue().getLeaderId() + " instead";
                LOG.log(Level.SEVERE, msg);
            }
        }

        JSONObject obj = new JSONObject();
        obj.put("networkMetric", NetworkMetric.getStat());
        obj.put("algorithmMetric", AlgorithmMetric.getStat());
        obj.put("qualityMetric", QualityMetric.getStat());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(obj.toJSONString());
        String prettyJson = gson.toJson(je);
        try {
            FileWriter file = new FileWriter(Config.statsFile);
            file.write(prettyJson);
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.log(Level.INFO, "Saved stats to config file: " + Config.statsFile);
    }

    public static void main(String[] args) {
        initialize(args);
        run();
        conclude();
    }
}
