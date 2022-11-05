import matplotlib.pyplot as plt
import json
import numpy as np


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
    handle1, = plt.plot([1,1], '-', color='red')
    handle2, = plt.plot([1,1], '-', color='green')
    handle3, = plt.plot([1,1], '-', color='blue')
    ax.legend([handle1, handle2, handle3],
              ['algo1', 'algo2', 'algo3'])
    handle1.set_visible(False)
    handle2.set_visible(False)
    handle3.set_visible(False)

    plt.savefig('plots/' + fn + '.png')


drop_rates = [0.05, 0.1, 0.15, 0.2]
algos = [1, 2, 3]
num_runs = 100
e2eMsgs = []
h2hMsgs = []
latencies = []

for drop_rate in drop_rates:
    e2eMsg = []
    h2hMsg = []
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
        e2eMsg.append(e2e)
        h2hMsg.append(h2h)
        latency.append(lat)
    e2eMsgs.append(e2eMsg)
    h2hMsgs.append(h2hMsg)
    latencies.append(latency)

e2eMsgs = np.asarray(e2eMsgs, dtype=object).transpose(1, 0)
h2hMsgs = np.asarray(h2hMsgs, dtype=object).transpose(1, 0)
latencies = np.asarray(latencies, dtype=object).transpose(1, 0)

plot(e2eMsgs, 'e2eMsgs')
plot(h2hMsgs, 'h2hMsgs')
plot(latencies, 'latencies')
