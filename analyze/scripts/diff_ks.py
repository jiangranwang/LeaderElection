import json
import os
import numpy as np
import matplotlib.pyplot as plt


d = {0.05: [1, 2, 3, 4],
     0.1: [1, 3, 5, 7],
     0.15: [1, 4, 8, 10],
     0.2: [1, 5, 9, 13]}
num_runs = 100
num_coordinator = 5
prefix = ''

for drop_rate in d:
    datas = []
    ks = d[drop_rate]
    for k in ks:
        data = []
        folder = f'results/{prefix}/{num_coordinator}/random/49/{drop_rate}_{k}/algo1/'
        for run in range(1, num_runs + 1):
            file_name = folder + f'stats_{run}.json'
            if not os.path.exists(file_name):
                print(f'file {file_name} does not exist. skipped')
                continue
            with open(file_name, 'r') as f:
                obj = json.load(f)
                # if obj['algorithmMetric']['totalLatency'] == 0 or obj['algorithmMetric']['leaderChanges'] < 1:
                #     continue
                data.append(obj['algorithmMetric']['leaderChanges'])
        print(drop_rate, k, np.max(data))
        datas.append([np.mean(data), np.std(data)])

    datas = np.asarray(datas)
    fig, ax = plt.subplots(figsize=(10, 5))
    ax.errorbar(ks, datas.T[0], datas.T[1], fmt='o', capsize=10, markersize=10)
    ax.set_xticks(ks)
    ax.set_xticklabels(ks)
    ax.set_title(f'drop_rate = {drop_rate}')
    plt.savefig(f'diff_ks_dr{drop_rate}_nc{num_coordinator}.png')
