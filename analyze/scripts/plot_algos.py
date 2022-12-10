import matplotlib.pyplot as plt
import json
import numpy as np


algos = [1, 2, 3, 4]
num_runs = 100
drop_rates = [0.05, 0.1, 0.15, 0.2]
num_server = 49
drop_rate = 0.1
num_servers = [32, 64, 96, 128]
labels = drop_rates


def set_box_color(bp, color):
    for item in ['boxes', 'whiskers', 'fliers', 'medians', 'caps']:
        plt.setp(bp[item], color=color)


def plot(data, fn):
    def get_mean(vals):
        return [np.mean(val) for val in vals]
    def get_std(vals):
        return [np.std(val) for val in vals]
    width = 0.1
    x = np.arange(len(labels))
    fig, ax = plt.subplots(figsize=(10, 7))
    bp1 = ax.errorbar(x-0.3, get_mean(data[0]), get_std(data[0]), color='red', fmt='o')
    bp2 = ax.errorbar(x-0.1, get_mean(data[1]), get_std(data[1]), color='green', fmt='o')
    bp3 = ax.errorbar(x+0.1, get_mean(data[2]), get_std(data[2]), color='blue', fmt='o')
    bp4 = ax.errorbar(x+0.3, get_mean(data[3]), get_std(data[3]), color='cyan', fmt='o')
    ax.grid(axis='y')
    ax.set_title(fn)
    ax.set_xticks(x)
    ax.set_xticklabels(drop_rates)
    # set_box_color(bp1, 'red')
    # set_box_color(bp2, 'green')
    # set_box_color(bp3, 'blue')
    # set_box_color(bp4, 'cyan')
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
        # for num_server in num_servers:
        for drop_rate in drop_rates:
            datas = []
            for algo in algos:
                folder = f'results/{num_server}/{drop_rate}/algo{algo}/'
                data = []
                for run in range(1, num_runs + 1):
                    file_name = folder + f'stats_{run}.json'
                    with open(file_name, 'r') as f:
                        obj = json.load(f)
                        if obj['algorithmMetric']['totalLatency'] == 0:
                            continue
                        if algo <= 2 and obj['qualityMetric']['hashRank'] != 0:
                            continue
                        data.append(obj[key1][key2])
                datas.append(data)
            all_datas.append(datas)
        all_datas = np.asarray(all_datas, dtype=object).transpose(1, 0)
        result = [[list(data) for data in datas] for datas in all_datas]
        ret.append(result)
    return ret


raw_data = get_data()
plot(raw_data[0], 'e2e_msgs')
plot(raw_data[1], 'h2h_msgs')
plot(raw_data[2], 'e2e_msg_sizes')
plot(raw_data[3], 'h2h_msg_sizes')
plot(raw_data[4], 'latencies')
plot(raw_data[5], 'suspect_rank')
plot(raw_data[6], 'hash_rank')
plot(raw_data[7], 'leader_within_top_5_suspect')
plot(raw_data[8], 'leader_changes')
plot(raw_data[9], '0_within_top_5_suspect')
plot(raw_data[10], 'suspect_count')


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
