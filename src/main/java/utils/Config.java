package utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Config {
    private static final Logger LOG = Logger.getLogger(Config.class.getName());

    public static String membershipFile, topologyFile, statsFile, traceFile, spatialDistro;
    // public static int numServers, oneHopDelay, granularity, k, f, eventCheckTimeout, algorithm, numLowNode,
    //         numSuspectCount, suspectCountThreshold, verbose;
    public static int numServers, oneHopDelay, granularity, eventCheckTimeout, critDuration, concRequesters, verbose;
    public static long endTime;
    public static double oneHopRadius, msgDropRate, irRatio, churnRatio;
    public static boolean fromTrace, batched, useChurn;

    public static int reqInitProcessedCt = 0;

    public static Random random = new Random();
    private static final long seed = System.currentTimeMillis();

    public static void parse(String config_path) {
        random.setSeed(seed);
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(config_path)) {
            Object obj = jsonParser.parse(reader);
            reader.close();
            JSONObject config = (JSONObject) obj;

            membershipFile = (String) config.get("membership_file");
            topologyFile = (String) config.get("topology_file");
            statsFile = (String) config.get("stats_file");
            traceFile = (String) config.get("trace_file");
            spatialDistro = (String) config.get("spatial_distro");

            fromTrace = ((String) config.get("from_trace")).equals("true");
            batched = ((String) config.get("batched")).equals("true");
            useChurn = ((String) config.get("use_churn")).equals("true");

            numServers = Integer.parseInt((String) config.get("num_servers"));
            oneHopDelay = Integer.parseInt((String) config.get("one_hop_delay"));
            granularity = Integer.parseInt((String) config.get("granularity"));
            // k = Integer.parseInt((String) config.get("k"));
            // f = Integer.parseInt((String) config.get("f"));
            eventCheckTimeout = Integer.parseInt((String) config.get("event_check_timeout"));
            critDuration = Integer.parseInt((String) config.get("crit_duration"));
            concRequesters = Integer.parseInt((String) config.get("conc_requesters"));
            // algorithm = Integer.parseInt((String) config.get("algorithm"));
            // if (algorithm > 4) throw new RuntimeException("Algorithm should be 1, 2, 3, or 4!");
            // numLowNode = Integer.parseInt((String) config.get("num_low_node"));
            // numSuspectCount = Integer.parseInt((String) config.get("num_suspect_count"));
            // suspectCountThreshold = Integer.parseInt((String) config.get("suspect_count_threshold"));
            verbose = Integer.parseInt((String) config.get("verbose"));
            if (verbose > 2) throw new RuntimeException("Verbose should be 0, 1, or 2!");

            endTime = Long.parseLong((String) config.get("end_time"));

            oneHopRadius = Double.parseDouble((String) config.get("one_hop_radius"));
            msgDropRate = Double.parseDouble((String) config.get("msg_drop_rate"));
            irRatio = Double.parseDouble((String) config.get("ir_ratio"));
            churnRatio = Double.parseDouble((String) config.get("churn_ratio"));
        } catch (Exception e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "Failed to parse configuration file");
            System.exit(1);
        }
    }
}
