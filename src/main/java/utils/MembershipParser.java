package utils;

import election.Simulator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MembershipParser {
    private static final Logger LOG = Logger.getLogger(Simulator.class.getName());
    private final String membership_path;
    private final int num_servers; // node ids are 0 to num_servers-1 (inclusive)

    private final HashMap<Integer, HashMap<Integer, String>> status;
    private final HashMap<Integer, HashMap<Integer, Integer>> suspects;

    public MembershipParser(String membership_path, int num_servers) {
        this.membership_path = membership_path;
        this.status = new HashMap<>();
        this.suspects = new HashMap<>();
        this.num_servers = num_servers;
        update();
    }

    public void update() {
        JSONParser jsonParser = new JSONParser();

        for (int file_id = 0; file_id < num_servers; file_id++) {
            HashMap<Integer, String> curr_status = new HashMap<>();
            HashMap<Integer, Integer> curr_suspects = new HashMap<>();

            boolean success = false;
            while (!success) {
                try (FileReader reader = new FileReader(membership_path + file_id + ".json")) {
                    Object obj = jsonParser.parse(reader);
                    JSONObject config = (JSONObject) obj;

                    JSONObject status_obj = (JSONObject) config.get("status");
                    JSONObject suspects_obj = (JSONObject) config.get("suspects");

                    for (int id = 0; id < num_servers; id++) {
                        String id_str = String.valueOf(id);
                        if (status_obj.containsKey(id_str)) {
                            curr_status.put(id, (String) status_obj.get(id_str));
                        } else {
                            curr_status.put(id, "failed");
                        }
                        if (suspects_obj.containsKey(id_str)) {
                            curr_suspects.put(id, Integer.parseInt((String) suspects_obj.get(id_str)));
                        }
                    }
                    success = true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    LOG.log(Level.SEVERE, "Membership file does not exist!");
                    System.exit(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            status.put(file_id, curr_status);
            suspects.put(file_id, curr_suspects);
        }
    }

    public HashMap<Integer, HashMap<Integer, String>> getStatus() {
        return status;
    }

    public HashMap<Integer, HashMap<Integer, Integer>> getSuspects() {
        return suspects;
    }
}
