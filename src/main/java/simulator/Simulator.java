package simulator;

import java.io.BufferedReader;  
import java.io.File;
import java.io.FileReader;  
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Runtime;
import java.lang.Thread;

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


    private static void loadTrace(int num_nodes) {
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
                servers.get(ip).loadRequest(LogicalTime.time + time);
            }
            br.close();
        } catch (IOException e) {
            System.out.println("Tracefile access failed");
            e.printStackTrace();
            System.exit(1);
        }
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
            loadTrace(num_nodes);
        }

        if (Config.useChurn) {

            File flag = new File(Config.membershipFile+"flag");
            while (!flag.exists()) {
                // loadTrace(num_nodes);
                // System.out.println("Running more requests");
                // EventService.processAll();
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    ;
                }
                flag = new File(Config.membershipFile+"flag");
            }

            System.out.println("flag hit");
            try {
                Runtime.getRuntime().exec("rm "+Config.membershipFile+"flag");
            } catch (Exception e) {
                ;
            }
        }

        // System.out.println("Entering flag checker");
        // int i = 0;
        // while (i++ < 10000) {
        //     try {
        //         String[] commands = {"ls output.log"};
        //         Process proc = Runtime.getRuntime().exec(commands);
        //         BufferedReader stdInput = new BufferedReader(new 
        //             InputStreamReader(proc.getInputStream()));
        //             String s = null;
        //             while ((s = stdInput.readLine()) != null) {
        //                 System.out.println(s);
        //                 break;
        //             }
        //     } catch (Exception e) {
        //         // System.out.println(e.getMessage());
        //         continue;
        //     }
        // }


        // File flag = new File("output.log","r");
        // while (!flag.exists());


        // File flag = null;
        // do {
        //     flag = new File("output.log","r");
        // } while (!flag.exists());

        // System.out.println("out of loop with i ="+String.valueOf(i));
        // try {
        //     Runtime.getRuntime().exec("rm "+Config.membershipFile+"flag");
        // } catch (Exception e) {
        //     ;
        // }

        // File flag = null;
        // if (Config.useChurn) {
        //     try {
        //         System.out.println(System.getProperty("user.home"));
        //         String[] commands = {"ls "+ System.getProperty("user.home")};
        //         Process proc = Runtime.getRuntime().exec(commands);
        //         BufferedReader stdInput = new BufferedReader(new 
        //          InputStreamReader(proc.getInputStream()));
        //          String s = null;
        //          while ((s = stdInput.readLine()) != null) {
        //              System.out.println(s);
        //          }
        //     } catch (Exception e) {
        //         System.out.println(e.getMessage());
        //     }

        //     System.out.println("Checking for flag at " + Config.membershipFile+"flag");
        //     do {
        //         flag = new File(Config.membershipFile+"flag","r");
        //     } while (!flag.exists());
    
        //     System.out.println("flag hit");
        //     try {
        //         Runtime.getRuntime().exec("rm "+Config.membershipFile+"flag");
        //     } catch (Exception e) {
        //         ;
        //     }
        // }


        EventService.processAll();


        if (Config.useChurn) {
            File flag = new File(Config.membershipFile+"done");
            while (!flag.exists()) {
                loadTrace(num_nodes);
                System.out.println("Running more requests");
                EventService.processAll();
                flag = new File(Config.membershipFile+"done");
            }

            System.out.println("done flag hit");
            try {
                Runtime.getRuntime().exec("rm "+Config.membershipFile+"done");
            } catch (Exception e) {
                ;
            }
        }
        // if (Config.useChurn) {
        //     flag = new File(Config.membershipFile+"done");
        //     while (!flag.exists()) {
        //         loadTrace(num_nodes);
        //         System.out.println("Running more requests");
        //         EventService.processAll();
        //         flag = new File(Config.membershipFile+"done");
        //     }
    
        //     System.out.println("done flag hit");
        //     try {
        //         Runtime.getRuntime().exec("rm "+Config.membershipFile+"done");
        //     } catch (Exception e) {
        //         ;
        //     }
        // }
        

        

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
        obj.put("churn_ratio",Config.churnRatio);
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
