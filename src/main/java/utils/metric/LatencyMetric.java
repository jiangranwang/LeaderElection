package utils.metric;

import org.json.simple.JSONObject;

public class LatencyMetric {
    private static long queryStartTime, queryEndTime, leaderStartTime, leaderEndTime;

    public static void setLeaderEndTime(long leaderEndTime) {
        LatencyMetric.leaderEndTime = leaderEndTime;
    }

    public static void setLeaderStartTime(long leaderStartTime) {
        LatencyMetric.leaderStartTime = leaderStartTime;
    }

    public static void setQueryEndTime(long queryEndTime) {
        LatencyMetric.queryEndTime = queryEndTime;
    }

    public static void setQueryStartTime(long queryStartTime) {
        LatencyMetric.queryStartTime = queryStartTime;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getStat() {
        JSONObject obj = new JSONObject();
        obj.put("queryLatency", queryEndTime - queryStartTime);
        obj.put("leaderLatency", leaderEndTime - leaderStartTime);
        obj.put("totalLatency", leaderEndTime - queryStartTime);
        return obj;
    }
}
