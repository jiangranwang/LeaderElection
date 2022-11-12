if [ ! -f 'run.sh' ]; then
  echo 'run.sh does not exist. Please run inside the bin/ folder'
  exit
fi

algos=(1 2 3 4)
num_run=100
config_name="config.json"
drop_rates=(0.05 0.1 0.15 0.2)
ks=(4 7 10 13)

for (( idx=0; idx<4; i++ )); do
  ks=${ks[$idx]}
  drop_rate=${drop_rates[$idx]}
  echo "drop rate: ${drop_rate}"
  echo "$(cat $config_name | jq --arg drop_rate "$drop_rate" '.msg_drop_rate = $drop_rate')" > $config_name
  echo "$(cat $config_name | jq --arg k "$k" '.k = $k')" > $config_name
  cd ../../Inconsistency-on-Medley/bin/
  echo "$(cat $config_name | jq --arg drop_rate "$drop_rate" '.msg_drop_rate = $drop_rate')" > $config_name
  ./run.sh &
  pid=$!
  cd ../../LeaderElection/bin/
  for (( i=1; i<=$num_run; i++ )); do
    for algo in ${algos[@]}; do
      echo "algorithm: ${algo}"
      echo "$(cat $config_name | jq --arg algo "$algo" '.algorithm = $algo')" > $config_name
      if [ ! -d "../analyze/results/${drop_rate}/algo${algo}/" ]; then
        mkdir "../analyze/results/${drop_rate}/algo${algo}/"
      fi
      ./run.sh $config_name
      mv stats.json "../analyze/results/${drop_rate}/algo${algo}/stats_${i}.json"
    done
  done
  echo "killing medley process..."
  kill -INT $pid
done
