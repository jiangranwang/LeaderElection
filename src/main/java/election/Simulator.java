package election;

import java.util.logging.Level;
import java.util.logging.Logger;

import utils.ConfigParser;
import utils.MembershipParser;

public class Simulator {
    private static final Logger LOG = Logger.getLogger(Simulator.class.getName());

    public static void main(String[] args) {
        if (args.length != 1) {
            LOG.log(Level.SEVERE, "You should provide the config file as the first argument");
            return;
        }

        LOG.log(Level.INFO, "Leader election program started");
        ConfigParser config = new ConfigParser(args[0]);
        MembershipParser membership = new MembershipParser(config.membership_file, config.num_servers);
        LOG.log(Level.INFO, membership.getStatus().toString());
        LOG.log(Level.INFO, membership.getSuspects().toString());
    }
}
