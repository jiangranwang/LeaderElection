import matplotlib.pyplot as plt
import json
import numpy as np


drop_rates = [0.05, 0.1, 0.15, 0.2]
algos = [1, 2, 3]
num_runs = 100
num_servers = 49


def set_box_color(bp, color):
    for item in ['boxes', 'whiskers', 'fliers', 'medians', 'caps']:
        plt.setp(bp[item], color=color)


def plot(data, fn):
    width = 0.1
    x = np.arange(len(drop_rates))
    fig, ax = plt.subplots(figsize=(10, 7))
    bp1 = ax.boxplot(data[0], positions=x - 0.2, widths=width)
    bp2 = ax.boxplot(data[1], positions=x, widths=width)
    bp3 = ax.boxplot(data[2], positions=x + 0.2, widths=width)
    ax.grid(axis='y')
    ax.set_title(fn)
    ax.set_xticks(x)
    ax.set_xticklabels(drop_rates)
    set_box_color(bp1, 'red')
    set_box_color(bp2, 'green')
    set_box_color(bp3, 'blue')
    handle1, = plt.plot([1, 1], '-', color='red')
    handle2, = plt.plot([1, 1], '-', color='green')
    handle3, = plt.plot([1, 1], '-', color='blue')
    ax.legend([handle1, handle2, handle3],
              ['algo1', 'algo2', 'algo3'])
    handle1.set_visible(False)
    handle2.set_visible(False)
    handle3.set_visible(False)

    plt.savefig('plots/' + fn + '.png')


def get_data():
    e2e_msgs = []
    h2h_msgs = []
    latencies = []
    for drop_rate in drop_rates:
        e2e_msg = []
        h2h_msg = []
        latency = []
        for algo in algos:
            folder = f'results/{drop_rate}/algo{algo}/'
            e2e = []
            h2h = []
            lat = []
            for run in range(1, num_runs + 1):
                file_name = folder + f'stats_{run}.json'
                with open(file_name, 'r') as f:
                    obj = json.load(f)
                    if obj['latencyMetric']['totalLatency'] == 0:
                        continue
                    e2e.append(obj['networkMetric']['e2eMsgTotal'])
                    h2h.append(obj['networkMetric']['h2hMsgTotal'])
                    lat.append(obj['latencyMetric']['totalLatency'])
            e2e_msg.append(e2e)
            h2h_msg.append(h2h)
            latency.append(lat)
        e2e_msgs.append(e2e_msg)
        h2h_msgs.append(h2h_msg)
        latencies.append(latency)

    e2e_msgs = np.asarray(e2e_msgs, dtype=object).transpose(1, 0)
    h2h_msgs = np.asarray(h2h_msgs, dtype=object).transpose(1, 0)
    latencies = np.asarray(latencies, dtype=object).transpose(1, 0)
    return e2e_msgs, h2h_msgs, latencies


# e2e_msgs_data, h2h_msgs_data, latencies_data = get_data()
# plot(e2e_msgs_data, 'e2e_msgs')
# plot(h2h_msgs_data, 'h2h_msgs')
# plot(latencies_data, 'latencies')


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
