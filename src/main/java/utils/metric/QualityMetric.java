package utils.metric;

import network.Address;
import org.json.simple.JSONObject;
import utils.AddressComparator;
import utils.Config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class QualityMetric {
    private static Address leader = null;

    public static void setLeader(Address leader) {
        QualityMetric.leader = AddressComparator.getMin(QualityMetric.leader, leader);
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getStat() {
        ConcurrentHashMap<Address, Integer> trueSuspects = AlgorithmMetric.getTrueSuspects();
        List<Address> suspectCounts = trueSuspects.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).map(Map.Entry::getKey).collect(Collectors.toList());

        JSONObject obj = new JSONObject();
        obj.put("suspectRank", leader == null ? -1 :
                suspectCounts.contains(leader) ? suspectCounts.indexOf(leader) : Config.numServers - 1);
        obj.put("hashRank", leader == null ? -1 : leader.getId());
        obj.put("suspectCount", trueSuspects.getOrDefault(leader, 0));

        return obj;
    }
}
