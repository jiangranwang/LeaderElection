if [ ! -f 'run.sh' ]; then
  echo 'run.sh does not exist. Please run inside the bin/ folder'
  exit
fi

run_medley=false # if set to true, run Medley at the same time; otherwise, the memberships will be randomly generated
algos=(1 2 3 4) # which algorithms to run
num_run=100 # number of experiment runs for a single configuration
medley_run=100 # how frequent the medley should run
config_name="config.json" # configuration file name
mem_path="membership/" # membership file path
mode="drop_rates" # available modes: num_coordinators, drop_rates, num_servers, topologies

if [ $mode == "drop_rates" ]; then
  # different drop rates
  topologies=("random")
  num_servers=(49)
  drop_rates=(0.05 0.1 0.15 0.2)
  ks=(4 7 10 13)
  num_coordinators=(1)
  # drop_rates=(0.05 0.05 0.05 0.05 0.1 0.1 0.1 0.1 0.15 0.15 0.15 0.15 0.2 0.2 0.2 0.2)
  # ks=(1 2 3 4 1 3 5 7 1 4 8 10 1 5 9 13)
  # ks=($(for (( i=0; i<5; i++ )); do for x in "${ks[@]}"; do printf "$x%.0s "; done; done))
  # num_coordinators=(1 3 5 7 9)
elif [ $mode == "num_coordinators" ]; then
  # different number of initiators
  topologies=("random")
  num_servers=(49)
  drop_rates=(0.05)
  ks=(2 2 2 2 2)
  num_coordinators=(1 3 5 7 9)
elif [ $mode == "num_servers" ]; then
  # different number of servers
  topologies=("random")
  num_servers=(32 64 128 256)
  drop_rates=(0.05)
  ks=(2 5 9 17)
  num_coordinators=(1)
elif [ $mode == "topologies" ]; then
  # different topologies
  topologies=("grid" "random" "cluster")
  num_servers=(49)
  drop_rates=(0.05)
  ks=(4 4 4)
  num_coordinators=(1)
else
  echo "wrong mode!"
  exit
fi

idx=0
for coordinator in ${num_coordinators[@]}; do
  echo "$(cat $config_name | jq --arg coordinator "$coordinator" '.num_coordinator = $coordinator')" > $config_name
  for topology in ${topologies[@]}; do
    for num_server in ${num_servers[@]}; do
      coordinate_path="topo/${topology}${num_server}.txt"
      echo "$(cat $config_name | jq --arg num_server "$num_server" '.num_servers = $num_server')" > $config_name
      echo "$(cat $config_name | jq --arg coordinate_path "$coordinate_path" '.topology_file = $coordinate_path')" > $config_name
      if [ "$run_medley" = true ] ; then
        mem_path="../../Inconsistency-on-Medley/bin/membership/"
        cd ../../Inconsistency-on-Medley/bin/
        echo "$(cat $config_name | jq --arg num_server "$num_server" '.num_server = $num_server')" > $config_name
        echo "$(cat $config_name | jq --arg coordinate_path "$coordinate_path" '.coordinate_path = $coordinate_path')" > $config_name
        cd ../../LeaderElection/bin/
      fi
      echo "$(cat $config_name | jq --arg mem_path "$mem_path" '.membership_file = $mem_path')" > $config_name
      for drop_rate in ${drop_rates[@]}; do
        k=${ks[$idx]}
        echo "$(cat $config_name | jq --arg drop_rate "$drop_rate" '.msg_drop_rate = $drop_rate')" > $config_name
        echo "$(cat $config_name | jq --arg k "$k" '.k = $k')" > $config_name
        for (( i=1; i<=$num_run; i++ )); do
          if [ "$run_medley" = true ] ; then
            if (( ($i-1) % $medley_run == 0 )); then
              kill $(pidof java)
              echo "run number: $i"
              cd ../../Inconsistency-on-Medley/bin/
              echo "$(cat $config_name | jq --arg drop_rate "$drop_rate" '.msg_drop_rate = $drop_rate')" > $config_name
              ./run.sh $config_name & # run about 10 seconds
              sleep 10
              cd ../../LeaderElection/bin/
            fi
          fi
          for algo in ${algos[@]}; do
            echo "run: ${i}, algorithm: ${algo}, drop rate: ${drop_rate}, k: ${k}, num_server: ${num_server}, topology: ${topology}, num_coordinator: ${coordinator}"
            echo "$(cat $config_name | jq --arg algo "$algo" '.algorithm = $algo')" > $config_name
            fpath="../analyze/results/${coordinator}/${topology}/${num_server}/${drop_rate}_${k}/algo${algo}/"
            if [ ! -d ${fpath} ]; then
              mkdir -p ${fpath}
            fi
            if [ "$run_medley" = false ] ; then
              python3 scripts/gen_membership.py $num_server
            fi
            ./run.sh $config_name
            mv stats.json "${fpath}/stats_${i}.json"
            sleep 1
          done
        done
        idx=$(( $idx + 1 ))
      done
    done
  done
done