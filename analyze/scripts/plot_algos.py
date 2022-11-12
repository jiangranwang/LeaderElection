import matplotlib.pyplot as plt
import json
import numpy as np


drop_rates = [0.05, 0.1, 0.15, 0.2]
algos = [1, 2, 3, 4]
num_runs = 100
num_servers = 49


def set_box_color(bp, color):
    for item in ['boxes', 'whiskers', 'fliers', 'medians', 'caps']:
        plt.setp(bp[item], color=color)


def plot(data, fn):
    width = 0.1
    x = np.arange(len(drop_rates))
    fig, ax = plt.subplots(figsize=(10, 7))
    bp1 = ax.boxplot(data[0], positions=x - 0.3, widths=width)
    bp2 = ax.boxplot(data[1], positions=x - 0.1, widths=width)
    bp3 = ax.boxplot(data[2], positions=x + 0.1, widths=width)
    bp4 = ax.boxplot(data[3], positions=x + 0.3, widths=width)
    ax.grid(axis='y')
    ax.set_title(fn)
    ax.set_xticks(x)
    ax.set_xticklabels(drop_rates)
    set_box_color(bp1, 'red')
    set_box_color(bp2, 'green')
    set_box_color(bp3, 'blue')
    set_box_color(bp4, 'cyan')
    handle1, = plt.plot([1, 1], '-', color='red')
    handle2, = plt.plot([1, 1], '-', color='green')
    handle3, = plt.plot([1, 1], '-', color='blue')
    handle4, = plt.plot([1, 1], '-', color='cyan')
    ax.legend([handle1, handle2, handle3, handle4],
              ['algo1', 'algo2', 'algo3', 'algo4'])
    handle1.set_visible(False)
    handle2.set_visible(False)
    handle3.set_visible(False)
    handle4.set_visible(False)

    plt.savefig('plots/' + fn + '.png')


def get_data():
    keys = [('networkMetric', 'e2eMsgTotal'),
            ('networkMetric', 'h2hMsgTotal'),
            ('networkMetric', 'e2eMsgSizeTotal'),
            ('networkMetric', 'h2hMsgSizeTotal'),
            ('latencyMetric', 'totalLatency'),
            ('qualityMetric', 'suspect_rank'),
            ('qualityMetric', 'hash_rank')]
    ret = []
    for key1, key2 in keys:
        all_datas = []
        for drop_rate in drop_rates:
            datas = []
            for algo in algos:
                folder = f'results/{drop_rate}/algo{algo}/'
                data = []
                for run in range(1, num_runs + 1):
                    file_name = folder + f'stats_{run}.json'
                    with open(file_name, 'r') as f:
                        obj = json.load(f)
                        if obj['latencyMetric']['totalLatency'] == 0:
                            continue
                        data.append(obj[key1][key2])
                datas.append(data)
            all_datas.append(datas)
        all_datas = np.asarray(all_datas, dtype=object).transpose(1, 0)
        ret.append(all_datas)
    return ret


raw_data = get_data()
plot(raw_data[0], 'e2e_msgs')
plot(raw_data[1], 'h2h_msgs')
plot(raw_data[2], 'e2e_msg_sizes')
plot(raw_data[3], 'h2h_msg_sizes')
plot(raw_data[4], 'latencies')
plot(raw_data[5], 'suspect_rank')
plot(raw_data[6], 'hash_rank')


# plot inconsistency related
for drop_rate in drop_rates:
    freqs = {}
    for run in range(1, num_runs + 1):
        fn = f'results/{drop_rate}/algo3/stats_{run}.json'
        with open(fn, 'r') as f:
            obj = json.load(f)
            trueSuspects = obj['latencyMetric']['trueSuspects']
            excludedSuspects = set(obj['latencyMetric']['excludedSuspects'])
            sortedSuspects = sorted([(trueSuspects[k], k) for k in trueSuspects], reverse=True)
            for i in range(len(sortedSuspects)):
                if int(sortedSuspects[i][1]) in excludedSuspects:
                    freqs[i] = freqs.get(i, 0) + 1
    sortedFreqs = [freqs.get(k, 0) for k in range(num_servers)]
    plt.figure(figsize=(10, 7))
    plt.bar(range(num_servers), height=sortedFreqs, width=0.75)
    plt.xlabel('suspect rank')
    plt.ylabel('frequency')
    plt.title(f'drop_rate={drop_rate}')
    plt.savefig(f'plots/inconsistency_{drop_rate}.png')
    plt.close()
