package utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Config {
    private static final Logger LOG = Logger.getLogger(Config.class.getName());

    public static String membershipFile, topologyFile;
    public static int numServers, oneHopDelay, granularity, k, f, eventCheckTimeout, algorithm, numLowNode,
            numSuspectCount, suspectCountThreshold;
    public static long endTime;
    public static double oneHopRadius, msgDropRate;

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

            numServers = Integer.parseInt((String) config.get("num_servers"));
            oneHopDelay = Integer.parseInt((String) config.get("one_hop_delay"));
            granularity = Integer.parseInt((String) config.get("granularity"));
            k = Integer.parseInt((String) config.get("k"));
            f = Integer.parseInt((String) config.get("f"));
            eventCheckTimeout = Integer.parseInt((String) config.get("event_check_timeout"));
            algorithm = Integer.parseInt((String) config.get("algorithm"));
            if (algorithm < 1 || algorithm > 3) throw new RuntimeException("Algorithm should be 1, 2, or 3!");
            numLowNode = Integer.parseInt((String) config.get("num_low_node"));
            numSuspectCount = Integer.parseInt((String) config.get("num_suspect_count"));
            suspectCountThreshold = Integer.parseInt((String) config.get("suspect_count_threshold"));

            endTime = Long.parseLong((String) config.get("end_time"));

            oneHopRadius = Double.parseDouble((String) config.get("one_hop_radius"));
            msgDropRate = Double.parseDouble((String) config.get("msg_drop_rate"));
        } catch (Exception e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "Failed to parse configuration file");
            System.exit(1);
        }
    }
}
