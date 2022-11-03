package utils.metric;

import network.Address;
import org.json.simple.JSONObject;
import simulator.LogicalTime;
import utils.Config;

import java.util.concurrent.ConcurrentHashMap;

public class LatencyMetric {
    private static long electionStartTime, electionEndTime;
    private static final ConcurrentHashMap<Address, Address> correctLeaders = new ConcurrentHashMap<>();

    public static void setCorrectLeader(Address ip, Address leader) {
        if (correctLeaders.containsKey(ip) && correctLeaders.get(ip).equals(leader)) return;
        correctLeaders.put(ip, leader);
        if (correctLeaders.size() == Config.numServers) electionEndTime = LogicalTime.time;
    }

    public static void setElectionStartTime(long electionStartTime) {
        LatencyMetric.electionStartTime = electionStartTime;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getStat() {
        JSONObject obj = new JSONObject();
        obj.put("totalLatency", electionEndTime - electionStartTime);
        return obj;
    }
}
