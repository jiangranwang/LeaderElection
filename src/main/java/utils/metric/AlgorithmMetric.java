package utils.metric;

import network.Address;
import org.json.simple.JSONObject;
import simulator.LogicalTime;
import utils.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AlgorithmMetric {
    private static long electionStartTime, electionEndTime;
    private static List<Address> excludedSuspects;
    private static final ConcurrentHashMap<Address, Integer> trueSuspects = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Address, Address> correctLeaders = new ConcurrentHashMap<>();
    private static int leaderChanges = 0;
    private static Address leaderIp;

    public static void setCorrectLeader(Address ip, Address leader) {
        if (correctLeaders.containsKey(ip) && correctLeaders.get(ip).equals(leader)) return;
        correctLeaders.put(ip, leader);
        leaderChanges++;
        leaderIp = leader;
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
        obj.put("excludedSuspects", excludedSuspects.toString().trim());
        obj.put("leaderChanges", (float) leaderChanges / (float) Config.numServers);

        List<Map.Entry<Address, Integer>> sortedTrueSuspects = trueSuspects.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toList());
        HashSet<Address> topSuspects = sortedTrueSuspects.subList(0, Math.min(5, sortedTrueSuspects.size()))
                .stream().map(Map.Entry::getKey).collect(Collectors.toCollection(HashSet::new));
        obj.put("leaderIsTopSuspect", topSuspects.contains(leaderIp) ? 1 : 0);
        obj.put("zeroIsTopSuspect", topSuspects.contains(new Address(0)) ? 1 : 0);
        return obj;
    }
}
