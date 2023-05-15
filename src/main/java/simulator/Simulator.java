package simulator;

import java.io.BufferedReader;  
import java.io.FileReader;  
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

import org.apache.commons.math3.distribution.ZipfDistribution;


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
        // Address coordinator = addresses.get(Config.random.nextInt(Config.numServers));
        // LOG.log(Level.INFO, "Coordinator is: " + coordinator);
        // get k + f + 1 random servers to send query message
        int num_nodes = Config.numServers;
        

        if (!Config.fromTrace) {
            int i = 0;
            for (Map.Entry<Address,Server> elem : servers.entrySet()) {
                // System.out.println("Initiating another request with i = " + String.valueOf(i));
                elem.getValue().initiateRequest();
                if (++i >= Config.concRequesters) break;
            }
        } else {
            // going to read in from the tracefile

            String line = "";  
            // String splitBy = ",";
            long time;
            int requester=0;
            //parsing a CSV file into BufferedReader class constructor
            Random rand = new Random();
            ZipfDistribution zipfDistribution = new ZipfDistribution(num_nodes, 2.0);

            try {
                BufferedReader br = new BufferedReader(new FileReader(Config.traceFile));  
                while ((line = br.readLine()) != null) {   //returns a Boolean value  
                    // String[] ln = line.split(splitBy);    // use comma as separator
                    // time = Long.parseLong(ln[0]);
                    time = Long.valueOf((long) (Long.parseLong(line)*Config.irRatio));
                    if (Config.spatialDistro.equals("zipfian")) {
                        requester = zipfDistribution.sample();
                        while (requester >= num_nodes) {
                            System.out.println("Decrementing from requester="+String.valueOf(requester));
                            requester-=1;
                        }
                    } else if (Config.spatialDistro.equals("uniform")) {
                        requester = rand.nextInt(num_nodes);
                    } else {
                        System.out.println("UNKNOWN DISTRIBUTION");
                        System.exit(1);
                    }
                    Address ip = new Address(requester);
                    servers.get(ip).loadRequest(time);
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // int num_nodes = Math.min(Config.numServers, Config.f + Config.k + 1);
        // AlgorithmMetric.setElectionStartTime(LogicalTime.time);
        // servers.get(coordinator).sendQuery(num_nodes, null);

        // TimerTask updateMembership = new TimerTask() {
        //     public void run() {
        //         for (Map.Entry<Address, Server> entry : servers.entrySet()) {
        //             entry.getValue().updateMembership();
        //         }
        //     }
        // };

        // Timer timer = new Timer();
        // timer.scheduleAtFixedRate(updateMembership, 0, Config.granularity);

        EventService.processAll();

        // timer.cancel();
    }

    @SuppressWarnings("unchecked")
    private static void conclude() {
        // for (Map.Entry<Address, Server> entry: servers.entrySet()) {
        //     LOG.log(Level.INFO, "Node " + entry.getKey() + " has leader: " + entry.getValue().getLeaderId());
        //     if (entry.getValue().getLeaderId() == null) {
        //         String msg = "Node " + entry.getKey() + " does not have correct leader! It has "
        //                 + entry.getValue().getLeaderId() + " instead";
        //         LOG.log(Level.SEVERE, msg);
        //     }
        // }

        JSONObject obj = new JSONObject();
        obj.put("networkMetric", NetworkMetric.getStat());
        obj.put("algorithmMetric", AlgorithmMetric.getStat());
        obj.put("conc_requesters",Config.concRequesters);
        obj.put("msg_drop_rate",Config.msgDropRate);
        obj.put("N", Config.numServers);
        obj.put("ir_ratio", Config.irRatio);
        // obj.put("qualityMetric", QualityMetric.getStat());

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
        // System.out.println("Printing works ");
        run();
        conclude();
    }
}
