if [ ! -f 'run.sh' ]; then
  echo 'run.sh does not exist. Please run inside the bin/ folder'
  exit
fi

algos=(3)
num_run=100
config_name="config.json"
drop_rates=(0.05 0.1 0.15 0.2)

for drop_rate in ${drop_rates[@]}; do
  echo "$(cat $config_name | jq --arg drop_rate "$drop_rate" '.msg_drop_rate = $drop_rate')" > $config_name
  for (( i=1; i<=$num_run; i++ )); do
    for algo in ${algos[@]}; do
      echo "$(cat $config_name | jq --arg algo "$algo" '.algorithm = $algo')" > $config_name
      if [ ! -d "../analyze/results/${drop_rate}/algo${algo}/" ]; then
        mkdir "../analyze/results/${drop_rate}/algo${algo}/"
      fi
      ./run.sh $config_name
      mv stats.json "../analyze/results/${drop_rate}/algo${algo}/stats_${i}.json"
    done
  done
  echo "press enter for next msg drop rate"
  read # wait for next msg drop rate to be changed on medley
done

