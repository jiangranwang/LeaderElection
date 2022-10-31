package simulator;

import network.Address;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import utils.AddressComparator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Membership {
    private static final Logger LOG = Logger.getLogger(Membership.class.getName());
    private final Address id;
    private final String membership_path;
    private final int num_servers; // node ids are 0 to num_servers-1 (inclusive)

    private final ConcurrentHashMap<Address, String> status; // node status
    private final ConcurrentHashMap<Address, Integer> suspects; // suspect count
    private final List<Address> all_nodes;
    private final List<Address> active_nodes;
    private final Lock address_lock;

    public Membership(String membership_path, int num_servers, Address id) {
        this.id = id;
        this.membership_path = membership_path;
        this.status = new ConcurrentHashMap<>();
        this.suspects = new ConcurrentHashMap<>();
        this.num_servers = num_servers;
        this.all_nodes = new ArrayList<>();
        this.active_nodes = new ArrayList<>();
        this.address_lock = new ReentrantLock();
        update();
    }

    public void update() {
        JSONParser jsonParser = new JSONParser();
        boolean success = false;
        while (!success) {
            try (FileReader reader = new FileReader(membership_path + id.getId() + ".json")) {
                Object obj = jsonParser.parse(reader);
                reader.close();
                JSONObject config = (JSONObject) obj;

                JSONObject status_obj = (JSONObject) config.get("status");
                JSONObject suspects_obj = (JSONObject) config.get("suspects");

                for (int node_id = 0; node_id < num_servers; node_id++) {
                    String node_id_str = String.valueOf(node_id);
                    if (status_obj.containsKey(node_id_str)) {
                        status.put(new Address(node_id), (String) status_obj.get(node_id_str));
                    } else {
                        status.put(new Address(node_id), "failed");
                    }
                    if (suspects_obj.containsKey(node_id_str)) {
                        suspects.put(new Address(node_id), Integer.parseInt((String) suspects_obj.get(node_id_str)));
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

        address_lock.lock();
        all_nodes.clear();
        active_nodes.clear();
        for (Map.Entry<Address, String> entry: status.entrySet()) {
            all_nodes.add(entry.getKey());
            if (Objects.equals(entry.getValue(), "active")) {
                active_nodes.add(entry.getKey());
            }
        }
        address_lock.unlock();
    }

    // TODO: only get nodes that are active
    public List<Address> getRandomNodes(int num_nodes, List<Address> excludedNodes) {
        if (num_nodes > all_nodes.size()) {
            throw new RuntimeException("Cannot get " + num_nodes + " nodes from list!!!");
        }

        List<Address> candidates = new ArrayList<>(all_nodes);
        if (excludedNodes != null) {
            for (Address ip: excludedNodes) {
                candidates.remove(ip);
            }
            if (candidates.size() < num_nodes) {
                LOG.log(Level.WARNING, "candidate size is less than required nodes!");
                candidates = new ArrayList<>(all_nodes);
            }
        }
        Collections.shuffle(candidates);
        return candidates.subList(0, num_nodes);
    }

    public ConcurrentHashMap<Address, String> getStatus() {
        return status;
    }

    public ConcurrentHashMap<Address, Integer> getSuspects() {
        return suspects;
    }

    public Address getLowestActiveId() {
        if (active_nodes.size() == 0) {
            all_nodes.sort(new AddressComparator<>());
            return all_nodes.get(0);
        }
        active_nodes.sort(new AddressComparator<>());
        return active_nodes.get(0);
    }

    public List<Address> getAllNodes() {
        return all_nodes;
    }
}
