if [ ! -f 'run.sh' ]; then
  echo 'run.sh does not exist. Please run inside the bin/ folder'
  exit
fi

num_run=100
# num_run=100
medley_run=10 #every medley_run runs, we want to rerun medley for a new trace
conc_step_size=30
num_server=256
config_name="config.json"
# drop_rates=(0.05 0.25 0.5)
# drop_rates=(0.45)
topologies=("grid" "random" "cluster")


# echo "Starting"

# drop rate 0.05 hardcoded
# num runs 49 hardcoded
# 30 concurrent requests hardcoded
# for topology in ${topologies[@]}; do
#     coordinate_path="topo/${topology}${num_server}.txt"
#     tmp="$(mktemp)"
#     jq --arg coordinate_path "$coordinate_path" '.topology_file = $coordinate_path' $config_name > "$tmp"
#     mv "$tmp" $config_name
#     cd ../../Inconsistency-on-Medley/bin/

#     tmp="$(mktemp)"
#     jq --arg coordinate_path "$coordinate_path" '.coordinate_path = $coordinate_path' $config_name > "$tmp"
#     rp="${topology}${num_serv}_route.txt"
#     jq --arg rp "$rp" '.routing_path = $rp' "$tmp" > $config_name

#     cd ../../LeaderElection/bin/
#     for (( i=1; i<=$num_run; i++ )); do
#         if (( ($i-1) % $medley_run == 0 )); then
#           cd ../../Inconsistency-on-Medley/bin/
#           ./run.sh $config_name # run medley for about 30 secs (end time 60k)
#           cd ../../LeaderElection/bin/
#           echo "topology: ${topology}, run_num: ${i}"
#         fi
#         ./run.sh $config_name
#         cat stats.json
#     done
# done

# drop rate 0.05 hardcoded
# sliding through number of concurrent requests
# random256 topo
# for (( conc_reqs=6; conc_reqs<=30; conc_reqs+=1)); do 
# for (( conc_reqs=1; conc_reqs<=$num_server/2; conc_reqs+=$conc_step_size)); do 
#     # modify the number of concurrent requests
#     tmp="$(mktemp)"
#     jq --arg conc_reqs "$conc_reqs" '.conc_requesters = $conc_reqs' $config_name > "$tmp"
#     mv "$tmp" $config_name
#     for (( i=1; i<=$num_run; i++ )); do
#         # echo "conc_reqs: ${conc_reqs}, run_num: ${i}"
#         ./run.sh $config_name
#         cat stats.json
#     done
# done 

# drop rate sliding
# num servers 256
# conc hardcoded 64 (quarter of system)
# random256 topo
# drop_rates=(0.1 0.2)
# # drop_rates=(0.05 0.15 0.25 0.35 0.45)
# for drop_rate in ${drop_rates[@]}; do
#     # modify the drop rate
#     tmp="$(mktemp)"
#     jq --arg drop_rate "$drop_rate" '.msg_drop_rate = $drop_rate' $config_name > "$tmp"
#     mv "$tmp" $config_name
#     for (( i=1; i<=$num_run; i++ )); do
#         if (( ($i-1) % $medley_run == 0 )); then
#           cd ../../Inconsistency-on-Medley/bin/
#           tmp="$(mktemp)"
#           jq --arg drop_rate "$drop_rate" '.msg_drop_rate = $drop_rate' $config_name > "$tmp"
#           mv "$tmp" $config_name
#           ./run.sh $config_name # run medley for about 30 secs (end time 60k)
#           cd ../../LeaderElection/bin/
#           # echo "drop_rate: ${drop_rate}, run_num: ${i}"
#         fi
#         ./run.sh $config_name
#         cat stats.json
#     done
# done 



# N sliding
# drop rate 0.05
# conc requests (hardcoded at 10, 30)
num_servers=(32 49 64 128 256 512 1024)
for num_serv in ${num_servers[@]}; do
    # modify N
    tmp="$(mktemp)"
    jq --arg num_serv "$num_serv" '.num_servers = $num_serv' $config_name > "$tmp"
    top_file="topo/random${num_serv}.txt"
    jq --arg top_file "$top_file" '.topology_file = $top_file' "$tmp" > $config_name

    for (( i=1; i<=$num_run; i++ )); do
        if (( ($i-1) % $medley_run == 0 )); then
          cd ../../Inconsistency-on-Medley/bin/

          tmp="$(mktemp)"
          jq --arg num_serv "$num_serv" '.num_server = $num_serv' $config_name > "$tmp"
          top_file="topo/random${num_serv}.txt"
          jq --arg top_file "$top_file" '.coordinate_path = $top_file' "$tmp" > $config_name
          rp="random${num_serv}_route.txt"
          jq --arg rp "$rp" '.routing_path = $rp' $config_name > "$tmp"
          mv "$tmp" $config_name

          ./run.sh $config_name # run medley for about 30 secs (end time 60k)
          cd ../../LeaderElection/bin/
          echo "num_serv: ${num_serv}, run_num: ${i}"
        fi
        ./run.sh $config_name
        cat stats.json
    done
done 