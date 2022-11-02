package utils.metric;

import network.Address;
import org.json.simple.JSONObject;
import utils.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkMetric {
    private static final ConcurrentHashMap<Address, HashMap<Address, Integer>> e2eMsgs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Address, HashMap<Address, Integer>> h2hMsgs = new ConcurrentHashMap<>();

    public static void initialize(List<Address> addresses) {
        for (Address ip: addresses) {
            e2eMsgs.put(ip, new HashMap<>());
            h2hMsgs.put(ip, new HashMap<>());
        }
    }

    public static void e2eRecord(Address src, Address dst) {
        e2eMsgs.get(src).put(dst, e2eMsgs.get(src).getOrDefault(dst, 0) + 1);
    }

    public static void h2hRecord(Address src, Address dst) {
        h2hMsgs.get(src).put(dst, h2hMsgs.get(src).getOrDefault(dst, 0) + 1);
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getStat() {
        int e2eMsgTotal = 0;
        int h2hMsgTotal = 0;
        HashMap<Address, Integer> e2eMsgsTotal = new HashMap<>();
        HashMap<Address, Integer> h2hMsgsTotal = new HashMap<>();

        for (Address src: e2eMsgs.keySet()) {
            int curr = 0;
            for (Map.Entry<Address, Integer> entry: e2eMsgs.get(src).entrySet()) {
                e2eMsgTotal += entry.getValue();
                curr += entry.getValue();
            }
            e2eMsgsTotal.put(src, curr);
        }
        for (Address src: h2hMsgs.keySet()) {
            int curr = 0;
            for (Map.Entry<Address, Integer> entry: h2hMsgs.get(src).entrySet()) {
                h2hMsgTotal += entry.getValue();
                curr += entry.getValue();
            }
            h2hMsgsTotal.put(src, curr);
        }

        JSONObject obj = new JSONObject();

        obj.put("e2eMsgTotal", e2eMsgTotal);
        obj.put("h2hMsgTotal", h2hMsgTotal);

        if (Config.verbose > 0) {
            JSONObject e2eMsgsTotalObj = new JSONObject();
            JSONObject h2hMsgsTotalObj = new JSONObject();

            for (Map.Entry<Address, Integer> entry: e2eMsgsTotal.entrySet()) {
                e2eMsgsTotalObj.put(entry.getKey().toString(), entry.getValue());
            }
            for (Map.Entry<Address, Integer> entry: h2hMsgsTotal.entrySet()) {
                h2hMsgsTotalObj.put(entry.getKey().toString(), entry.getValue());
            }

            obj.put("e2eMsgsTotal", e2eMsgsTotalObj);
            obj.put("h2hMsgsTotal", h2hMsgsTotalObj);
        }

        if (Config.verbose > 1) {
            JSONObject e2eMsgsTotalObj = new JSONObject();
            JSONObject h2hMsgsTotalObj = new JSONObject();
            for (Address ip: e2eMsgs.keySet()) {
                JSONObject e2eMsgsObj = new JSONObject();
                for (Map.Entry<Address, Integer> entry : e2eMsgs.get(ip).entrySet()) {
                    e2eMsgsObj.put(entry.getKey().toString(), entry.getValue());
                }
                e2eMsgsTotalObj.put(ip.toString(), e2eMsgsObj);
            }
            for (Address ip: h2hMsgs.keySet()) {
                JSONObject h2hMsgsObj = new JSONObject();
                for (Map.Entry<Address, Integer> entry : h2hMsgs.get(ip).entrySet()) {
                    h2hMsgsObj.put(entry.getKey().toString(), entry.getValue());
                }
                h2hMsgsTotalObj.put(ip.toString(), h2hMsgsObj);
            }

            obj.put("e2eMsgs", e2eMsgsTotalObj);
            obj.put("h2hMsgs", h2hMsgsTotalObj);
        }

        return obj;
    }
}
