# Churn-tolerant Leader Election
This repository implements the simulation code for (a family of four different) 
churn-tolerant leader election protocols.

Run with Simulated Membership
----------
The simple option is to run the leader election protocols with simulated membership
files (generated by `bin/scripts/gen_membership.py`). Start by building the project:
```
$ mvn clean package
```

And then go to the `bin/` folder. To run a single experiment:
```
$ ./run.sh <config_file> # we provide a default file (config.json) for <config_file> 
```

To run multiple experiments with different network configurations:
```
$ ./scripts/run_multiple.sh # set run_medley to false
```

Run with Real Traces of Membership Protocol
----------
Another option is to run the leader election protocols with real membership traces 
injected (as we did in our paper).
We use [Medley](https://github.com/jiangranwang/Inconsistency-on-Medley) as our
underlying weakly-consistent membership protocol. Start by cloning both this repository
and the Medley repository and place both folders under the same directory:
```
folder/
    LeaderElection/
    Inconsistency-on-Medley/
```

Both projects are built using Apache Maven. Run the following command in your terminal:
```
$ cd folder/LeaderElection/ && mvn clean package
$ cd folder/Inconsistency-on-Medley/ && mvn clean package
```

All the scripts are inside the `bin/` folder:

```
$ cd folder/Leader/Election/bin/
```

To run multiple experiments with different network configurations:
```
$ ./scripts/run_multiple.sh # set run_medley to true
```

After running multiple experiments with different network configurations, you can use the
script `analyze/scripts/plot_algos.py` to plot the figures. The results in the paper can 
be reproduced in this way.

Config File Fields
----------
- `membership_file`: the path of the folder that stores the membership files
  - If membership files are simulated, the default path is `membership/`
  - If the membership files are generated from Medley, the default path is `../../Inconsistency-on-Medley/bin/membership/`
- `topology_file`: topology file path. We provide some example topology files stored in `bin/topo/`
- `stats_file`: file path that stores the statistics of the leader election run
- `num_servers`: number of servers in the system
- `one_hop_delay`: simulated one way hop delay
- `one_hop_radius`: node communication range
- `msg_drop_rate`: message drop rate in percentage
- `granularity`: the frequency that reads from the membership files
- `end_time`: simulation end time
- `event_check_timeout`: timeout to check for message responses
- `algorithm`: which algorithm to run (1-Base Protocol, 2-Optimistic Protocol, 3-Preferred Protocol, 4-Hybrid Protocol)
- `k`: inconsistency value c
- `f`: number of maximum simultaneous failure
- `num_low_node`: number of low hash nodes included in the Response message (for algorithm 3 and 4)
- `num_suspect_count`: number of top suspect count nodes included in the Response message (for algorithm 3 and 4)
- `num_coordinator`: number of initiators
- `suspect_count_threshold`: the lowest number of suspect count for a node to be included in the top suspect count nodes
- `verbose`: verbosity for the statistic file

Repository Structure
----------
```
LeaderElection/
  analyze/
    results/      # experiment result .json files
    scripts/      # scripts to plot data
  bin/            # bash scripts
    membership/   # stores the simulated membership files
    scripts/      # scripts to run the experiment
    topo/         # topology files
  src/main/java/  # source code
    enums/        # enumerate types
    network/      # network simulation
      message/    # different types of message details
    simulator/    # contains the entrypoint of the code
      event/      # different types of simulated events
    utils/        # utiliy functions
```