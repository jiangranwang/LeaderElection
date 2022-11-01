package utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Config {
    private static final Logger LOG = Logger.getLogger(Config.class.getName());

    public static String membership_file;
    public static int num_servers, one_hop_delay, granularity, k, f, event_check_timeout, algorithm, num_low_node,
            num_suspect_count, suspect_count_threshold;
    public static long end_time;

    public static void parse(String config_path) {
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(config_path)) {
            Object obj = jsonParser.parse(reader);
            reader.close();
            JSONObject config = (JSONObject) obj;

            membership_file = (String) config.get("membership_file");

            num_servers = Integer.parseInt((String) config.get("num_servers"));
            one_hop_delay = Integer.parseInt((String) config.get("one_hop_delay"));
            granularity = Integer.parseInt((String) config.get("granularity"));
            k = Integer.parseInt((String) config.get("k"));
            f = Integer.parseInt((String) config.get("f"));
            event_check_timeout = Integer.parseInt((String) config.get("event_check_timeout"));
            algorithm = Integer.parseInt((String) config.get("algorithm"));
            if (algorithm < 1 || algorithm > 3) throw new RuntimeException("Algorithm should be 1, 2, or 3!");
            num_low_node = Integer.parseInt((String) config.get("num_low_node"));
            num_suspect_count = Integer.parseInt((String) config.get("num_suspect_count"));
            suspect_count_threshold = Integer.parseInt((String) config.get("suspect_count_threshold"));

            end_time = Long.parseLong((String) config.get("end_time"));
        } catch (Exception e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "Failed to parse configuration file");
            System.exit(1);
        }
    }
}
