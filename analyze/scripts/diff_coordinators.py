import json
import os
import numpy as np
import matplotlib.pyplot as plt


num_coordinators = [1, 3, 5, 7, 9]
num_runs = 100
ks = [3, 5, 8, 9]
drop_rates = [0.05, 0.1, 0.15, 0.2]
prefix = ''

for i, drop_rate in enumerate(drop_rates):
    datas = []
    for num_coordinator in num_coordinators:
        data = []
        folder = f'results/{prefix}/{num_coordinator}/random/49/{drop_rate}_{ks[i]}/algo1/'
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
        datas.append([np.mean(data), np.std(data)])

    datas = np.asarray(datas)
    fig, ax = plt.subplots(figsize=(10, 5))
    ax.errorbar(num_coordinators, datas.T[0], datas.T[1], fmt='o', capsize=10, markersize=10)
    ax.set_xticks(num_coordinators)
    ax.set_xticklabels(num_coordinators)
    ax.set_title(f'drop_rate = {drop_rate}')
    plt.savefig(f'diff_coordinators_dr{drop_rate}_k{ks[i]}.png')
