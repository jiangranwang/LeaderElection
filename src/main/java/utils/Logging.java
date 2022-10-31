package utils;

import network.Address;
import simulator.LogicalTime;

import java.util.logging.Level;

public class Logging {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Logging.class.getName());

    public static void log(Level level, Address id, String msg) {
        LOG.log(level, "(time=" + LogicalTime.time + ", node=" + id + "): " + msg);
    }
}

