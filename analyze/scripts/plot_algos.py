import matplotlib.pyplot as plt
import json
import os
import numpy as np


# settings
plt.rcParams.update({'font.size': 18, 'font.family': 'Times New Roman'})
colors = [
    (0.4, 0.7607843137254902, 0.6470588235294118, 1.0),
    (0.9882352941176471, 0.5529411764705883, 0.3843137254901961, 1.0),
    (0.5529411764705883, 0.6274509803921569, 0.796078431372549, 1.0),
    (0.9058823529411765, 0.5411764705882353, 0.7647058823529411, 1.0)
]
markers = ['o', 'P', 'X', 'v']


ind = '.'  # '../../LeaderElection-on-Pis/analyze'
outd = '../../icdcs/plots/simulation/var_drop_rate/'
algos = [1, 2, 3, 4]
num_runs = 100

topology = 'random'
topologies = ['grid', 'random', 'cluster']
drop_rate = 0.05
drop_rates = [0.05, 0.1, 0.15, 0.2]
num_server = 49
num_servers = [32, 64, 128, 256]
labels = drop_rates
xlabel = 'Message Drop Rate'


def set_box_color(bp, color):
    for item in ['boxes', 'whiskers', 'fliers', 'medians', 'caps']:
        plt.setp(bp[item], color=color)


def get_mean(vals):
    return np.asarray([np.mean(val) for val in vals])


def get_std(vals):
    return np.asarray([np.std(val) for val in vals])


def plot(data, fn, ylabel=''):
    x = np.arange(len(labels))
    fig, ax = plt.subplots(figsize=(10, 5))
    ax.errorbar(x-0.3, get_mean(data[0]), get_std(data[0]), color=colors[0], fmt=markers[0], capsize=10, markersize=10)
    ax.errorbar(x-0.1, get_mean(data[1]), get_std(data[1]), color=colors[1], fmt=markers[1], capsize=10, markersize=10)
    ax.errorbar(x+0.1, get_mean(data[2]), get_std(data[2]), color=colors[2], fmt=markers[2], capsize=10, markersize=10)
    ax.errorbar(x+0.3, get_mean(data[3]), get_std(data[3]), color=colors[3], fmt=markers[3], capsize=10, markersize=10)
    ax.grid(axis='y')
    # ax.set_title(fn)
    ax.set_xticks(x)
    ax.set_xticklabels(labels)
    ax.set_xlabel(xlabel, fontsize=20, fontweight='bold')
    ax.set_ylabel(ylabel, fontsize=20, fontweight='bold')
    handle1, = plt.plot([1, 1], '-', color=colors[0], marker=markers[0])
    handle2, = plt.plot([1, 1], '-', color=colors[1], marker=markers[1])
    handle3, = plt.plot([1, 1], '-', color=colors[2], marker=markers[2])
    handle4, = plt.plot([1, 1], '-', color=colors[3], marker=markers[3])
    ax.legend([handle1, handle2, handle3, handle4],
              ['Base', 'Optimistic', 'Preferred', 'Hybrid'])
    handle1.set_visible(False)
    handle2.set_visible(False)
    handle3.set_visible(False)
    handle4.set_visible(False)

    plt.tight_layout(pad=0.75)
    plt.savefig(f'{outd}/' + fn + '.png')


def get_data():
    keys = [('networkMetric', 'e2eMsgTotal'),
            ('networkMetric', 'h2hMsgTotal'),
            ('networkMetric', 'e2eMsgSizeTotal'),
            ('networkMetric', 'h2hMsgSizeTotal'),
            ('algorithmMetric', 'totalLatency'),
            ('qualityMetric', 'suspectRank'),
            ('qualityMetric', 'hashRank'),
            ('algorithmMetric', 'leaderIsTopSuspect'),
            ('algorithmMetric', 'leaderChanges'),
            ('algorithmMetric', 'zeroIsTopSuspect'),
            ('qualityMetric', 'suspectCount')]
    ret = []
    for key1, key2 in keys:
        all_datas = []
        for algo in algos:
            datas = []
            # for topology in topologies:
            # for drop_rate in drop_rates:
            # for num_server in num_servers:
            for drop_rate in drop_rates:
                folder = f'{ind}/results/{topology}/{num_server}/{drop_rate}/algo{algo}/'
                data = []
                for run in range(1, num_runs + 1):
                    file_name = folder + f'stats_{run}.json'
                    if not os.path.exists(file_name):
                        print(f'file {file_name} does not exist. skipped')
                        continue
                    with open(file_name, 'r') as f:
                        obj = json.load(f)
                        if obj['algorithmMetric']['totalLatency'] == 0 or obj['algorithmMetric']['leaderChanges'] < 1:
                            continue
                        if algo <= 2 and obj['qualityMetric']['hashRank'] != 0:
                            continue
                        data.append(obj[key1][key2])
                datas.append(list(data))
            all_datas.append(datas)
        # all_datas = np.asarray(all_datas, dtype=object).transpose(1, 0)
        result = [[list(data) for data in datas] for datas in all_datas]
        ret.append(result)
    return ret


raw_data = get_data()

# val1 = ((get_mean(raw_data[4][0]) - get_mean(raw_data[4][1])) / get_mean(raw_data[4][0])).mean()
# val2 = ((get_mean(raw_data[4][2]) - get_mean(raw_data[4][3])) / get_mean(raw_data[4][2])).mean()
# print(f'Duration of algo2 is {val1}% less than algo1, and duration of algo4 is {val2}% less than algo3')
# val3 = (np.mean(raw_data[8][3][0]) - 1) % 100
# val4 = (np.mean(raw_data[8][3][3]) - 1) % 100
# print(f'Leader change of Hybrid is {val3}% worse than Base at 0.05, and {val4}% worse at 0.2')
# val5 = ((np.mean(raw_data[5][2][0]) + np.mean(raw_data[5][3][0])) / 2) / np.mean(raw_data[5][0][0])
# val6 = ((np.mean(raw_data[5][2][3]) + np.mean(raw_data[5][3][3])) / 2) / np.mean(raw_data[5][0][3])
# print(f'Unhealthy rank: p3 and p4 is {val5} times better than p1 at 0.05, and {val6} times better at 0.2')
# assert False
# plot(raw_data[0], 'e2e_msgs')
# plot(raw_data[1], 'h2h_msgs')
plot(raw_data[2], 'e2e_msg_sizes', 'Total Message Size (bytes)')
# plot(raw_data[3], 'h2h_msg_sizes')
plot(raw_data[4], 'latencies', 'Completion Time (time unit)')
plot(raw_data[5], 'suspect_rank', 'Unhealthy Rank')
plot(raw_data[6], 'hash_rank', 'Hash Rank')
# plot(raw_data[7], 'leader_within_top_5_suspect')
plot(raw_data[8], 'leader_changes', 'Leader Change Count')
# plot(raw_data[9], '0_within_top_5_suspect')
# plot(raw_data[10], 'suspect_count')


# plot inconsistency related
# for drop_rate in drop_rates:
#     freqs = {}
#     for run in range(1, num_runs + 1):
#         fn = f'results/{drop_rate}/algo3/stats_{run}.json'
#         with open(fn, 'r') as f:
#             obj = json.load(f)
#             trueSuspects = obj['algorithmMetric']['trueSuspects']
#             excludedSuspects = set(obj['algorithmMetric']['excludedSuspects'])
#             sortedSuspects = sorted([(trueSuspects[k], k) for k in trueSuspects], reverse=True)
#             for i in range(len(sortedSuspects)):
#                 if int(sortedSuspects[i][1]) in excludedSuspects:
#                     freqs[i] = freqs.get(i, 0) + 1
#     sortedFreqs = [freqs.get(k, 0) for k in range(num_server)]
#     plt.figure(figsize=(10, 7))
#     plt.bar(range(num_server), height=sortedFreqs, width=0.75)
#     plt.xlabel('suspect rank')
#     plt.ylabel('frequency')
#     plt.title(f'drop_rate={drop_rate}')
#     plt.savefig(f'plots/inconsistency_{drop_rate}.png')
#     plt.close()
