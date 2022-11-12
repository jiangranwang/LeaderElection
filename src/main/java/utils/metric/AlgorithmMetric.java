package utils.metric;

import network.Address;
import org.json.simple.JSONObject;
import simulator.LogicalTime;
import utils.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AlgorithmMetric {
    private static long electionStartTime, electionEndTime;
    private static List<Address> excludedSuspects;
    private static final ConcurrentHashMap<Address, Integer> trueSuspects = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Address, Address> correctLeaders = new ConcurrentHashMap<>();

    public static void setCorrectLeader(Address ip, Address leader) {
        if (correctLeaders.containsKey(ip) && correctLeaders.get(ip).equals(leader)) return;
        correctLeaders.put(ip, leader);
        if (correctLeaders.size() == Config.numServers) electionEndTime = LogicalTime.time;
    }

    public static void setElectionStartTime(long electionStartTime) {
        AlgorithmMetric.electionStartTime = electionStartTime;
    }

    public static void setTrueSuspects(HashMap<Address, Integer> currTrueSuspects) {
        for (Map.Entry<Address, Integer> entry: currTrueSuspects.entrySet()) {
            trueSuspects.put(entry.getKey(), trueSuspects.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
    }

    public static void setExcludedSuspects(List<Address> excludedSuspects) {
        AlgorithmMetric.excludedSuspects = excludedSuspects;
    }

    public static ConcurrentHashMap<Address, Integer> getTrueSuspects() {
        return trueSuspects;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getStat() {
        JSONObject obj = new JSONObject();
        obj.put("totalLatency", electionEndTime - electionStartTime);
        obj.put("trueSuspects", trueSuspects);
        obj.put("excludedSuspects", excludedSuspects);
        return obj;
    }
}
