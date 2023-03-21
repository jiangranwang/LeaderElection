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

    private final ConcurrentHashMap<Address, String> status = new ConcurrentHashMap<>();; // node status
    private final ConcurrentHashMap<Address, Integer> suspects = new ConcurrentHashMap<>();; // suspect count
    private final List<Address> allNodes = new ArrayList<>();
    private final List<Address> activeNodes = new ArrayList<>();
    private final Lock addressLock = new ReentrantLock();

    public Membership(Address id) {
        this.id = id;
        update();
    }

    public void update() {
        JSONParser jsonParser = new JSONParser();
        boolean success = false;
        while (!success) {
            try (FileReader reader = new FileReader(Config.membershipFile + id.getId() + ".json")) {
                Object obj = jsonParser.parse(reader);
                reader.close();
                JSONObject config = (JSONObject) obj;

                JSONObject statusObj = (JSONObject) config.get("status");
                JSONObject suspectsObj = (JSONObject) config.get("suspects");

                for (int nodeId = 0; nodeId < Config.numServers; nodeId++) {
                    String nodeIdStr = String.valueOf(nodeId);
                    if (statusObj.containsKey(nodeIdStr)) {
                        status.put(new Address(nodeId), (String) statusObj.get(nodeIdStr));
                    } else {
                        status.put(new Address(nodeId), "failed");
                    }
                    if (suspectsObj.containsKey(nodeIdStr)) {
                        suspects.put(new Address(nodeId), Integer.parseInt((String) suspectsObj.get(nodeIdStr)));
                    }
                }
                success = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                LOG.log(Level.SEVERE, "Membership file does not exist!");
                System.exit(1);
            } catch (Exception e) {
                LOG.log(Level.FINE, "Membership file is being written right now...");
            }
        }

        addressLock.lock();
        allNodes.clear();
        activeNodes.clear();
        for (Map.Entry<Address, String> entry: status.entrySet()) {
            allNodes.add(entry.getKey());
            if (Objects.equals(entry.getValue(), "active")) {
                activeNodes.add(entry.getKey());
            }
        }
        addressLock.unlock();
    }

    public List<Address> getRandomNodes(int numNodes, List<Address> excludedNodes, boolean activeOnly) {
        if (numNodes > allNodes.size()) {
            LOG.log(Level.WARNING, "Cannot get " + numNodes + " nodes from list!!!");
            numNodes = allNodes.size();
        }

        addressLock.lock();
        List<Address> candidates = new ArrayList<>(activeOnly ? activeNodes : allNodes);
        addressLock.unlock();
        if (excludedNodes != null) {
            for (Address ip: excludedNodes) {
                candidates.remove(ip);
            }
            if (candidates.size() < numNodes) {
                LOG.log(Level.WARNING, "candidate size is less than required nodes!");
                addressLock.lock();
                candidates = new ArrayList<>(allNodes);
                addressLock.unlock();
            }
        }
        Collections.shuffle(candidates);
        return candidates.subList(0, numNodes);
    }

    public HashMap<Address, String> getStatus() {
        return new HashMap<>(status);
    }

    public HashMap<Address, Integer> getSuspects() {
        return new HashMap<>(suspects);
    }

    public Address getLowestActiveId() {
        Address result;
        addressLock.lock();
        if (activeNodes.size() == 0) {
            allNodes.sort(new AddressComparator<>());
            result = allNodes.get(0);
        } else {
            activeNodes.sort(new AddressComparator<>());
            result = activeNodes.get(0);
            // if (random.nextDouble() < 0.5) result = activeNodes.get(1);
        }
        addressLock.unlock();
        return result;
    }

    public void addNodeToAll(Address addr) {
        allNodes.add(addr);
    }
    
    public List<Address> getAllNodes(boolean excludeSelf) {
        update();
        addressLock.lock();
        List<Address> copy = allNodes.stream().filter(key -> !(excludeSelf && key.equals(id))).collect(Collectors.toList());
        addressLock.unlock();
        return copy;
    }

    public Pair<List<Address>, List<Pair<Address, Integer>>> getPairIds(
            int numLowNodes, int numSuspectCounts, boolean activeOnly) {
        List<Pair<Address, Integer>> highestSuspectNodes = getHighestSuspectNodes(numSuspectCounts, activeOnly);
        addressLock.lock();
        List<Address> lowestIds = new ArrayList<>(activeOnly ? activeNodes : allNodes)
                .stream().filter(key -> !highestSuspectNodes.stream().map(Pair::getKey).collect(Collectors.toList())
                        .contains(key)).collect(Collectors.toList());
        addressLock.unlock();
        if (lowestIds.size() < numLowNodes && activeOnly) {
            return getPairIds(numLowNodes, numSuspectCounts, false);
        }
        lowestIds.sort(new AddressComparator<>());
        return new Pair<>(lowestIds.subList(0, Math.min(numLowNodes, lowestIds.size())), highestSuspectNodes);
    }

    public List<Pair<Address, Integer>> getHighestSuspectNodes(int numNodes, boolean activeOnly) {
        Map<Address, Integer> suspectCandidates = new HashMap<>();
        addressLock.lock();
        (activeOnly ? activeNodes : allNodes).stream().filter(suspects::containsKey).collect(Collectors.toList())
                .forEach(key -> suspectCandidates.put(key, suspects.get(key)));
        addressLock.unlock();
        List<Map.Entry<Address, Integer>> list = suspectCandidates.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toList());
        // List<Map.Entry<Address, Integer>> listFiltered = list.stream()
                // .filter(entry -> entry.getValue() >= Config.suspectCountThreshold).collect(Collectors.toList());
        return list.subList(0, Math.min(numNodes, list.size())).stream()
        // return listFiltered.subList(0, Math.min(numNodes, listFiltered.size())).stream()
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }
}
