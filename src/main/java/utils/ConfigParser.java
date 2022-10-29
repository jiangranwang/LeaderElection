package utils;

import election.Simulator;
import org.json.simple.JSONObject;

import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.parser.JSONParser;

public class ConfigParser {
    private static final Logger LOG = Logger.getLogger(Simulator.class.getName());

    public String membership_file;
    public int num_servers;

    public ConfigParser(String config_path) {
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(config_path)) {
            Object obj = jsonParser.parse(reader);
            JSONObject config = (JSONObject) obj;

            membership_file = (String) config.get("membership_file");
            num_servers = Integer.parseInt((String) config.get("num_servers"));
        } catch (Exception e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "Failed to parse configuration file");
            System.exit(1);
        }
    }
}
