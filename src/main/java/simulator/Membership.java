package simulator;

import javafx.util.Pair;
import network.Address;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import utils.AddressComparator;
import utils.Config;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Membership {
    private static final Logger LOG = Logger.getLogger(Membership.class.getName());
    private final Address id;
    private final String membership_path;
    private final int num_servers; // node ids are 0 to num_servers-1 (inclusive)

    private final ConcurrentHashMap<Address, String> status = new ConcurrentHashMap<>();; // node status
    private final ConcurrentHashMap<Address, Integer> suspects = new ConcurrentHashMap<>();; // suspect count
    private final List<Address> all_nodes = new ArrayList<>();
    private final List<Address> active_nodes = new ArrayList<>();
    private final Lock address_lock = new ReentrantLock();
    private final Random random = new Random();

    public Membership(String membership_path, int num_servers, Address id) {
        this.id = id;
        this.membership_path = membership_path;
        this.num_servers = num_servers;
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
        Address result;
        address_lock.lock();
        if (active_nodes.size() == 0) {
            all_nodes.sort(new AddressComparator<>());
            result = all_nodes.get(0);
        } else {
            active_nodes.sort(new AddressComparator<>());
            result = active_nodes.get(0);
            // if (random.nextDouble() < 0.5) result = active_nodes.get(1);
        }
        address_lock.unlock();
        return result;
    }

    public List<Address> getAllNodes() {
        address_lock.lock();
        List<Address> copy = new ArrayList<>(all_nodes);
        address_lock.unlock();
        return copy;
    }

    public Pair<List<Address>, List<Pair<Address, Integer>>> getPairIds(
            int num_low_nodes, int num_suspect_counts, boolean activeOnly) {
        List<Pair<Address, Integer>> highest_suspect_ids = getHighestSuspectNodes(num_suspect_counts, activeOnly);
        address_lock.lock();
        List<Address> lowest_ids = new ArrayList<>(activeOnly ? active_nodes : all_nodes)
                .stream().filter(key -> !highest_suspect_ids.stream().map(Pair::getKey).collect(Collectors.toList())
                        .contains(key)).collect(Collectors.toList());
        address_lock.unlock();
        if (lowest_ids.size() < num_low_nodes && activeOnly) {
            return getPairIds(num_low_nodes, num_suspect_counts, false);
        }
        lowest_ids.sort(new AddressComparator<>());
        return new Pair<>(lowest_ids.subList(0, Math.min(num_low_nodes, lowest_ids.size())), highest_suspect_ids);
    }

    public List<Pair<Address, Integer>> getHighestSuspectNodes(int num_nodes, boolean activeOnly) {
        Map<Address, Integer> suspect_candidates = new HashMap<>();
        address_lock.lock();
        (activeOnly ? active_nodes : all_nodes).stream().filter(suspects::containsKey).collect(Collectors.toList())
                .forEach(key -> suspect_candidates.put(key, suspects.get(key)));
        address_lock.unlock();
        List<Map.Entry<Address, Integer>> list = suspect_candidates.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toList());
        List<Map.Entry<Address, Integer>> list_filtered = list.stream()
                .filter(entry -> entry.getValue() >= Config.suspect_count_threshold).collect(Collectors.toList());
        return list_filtered.subList(0, Math.min(num_nodes, list_filtered.size())).stream()
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }
}
