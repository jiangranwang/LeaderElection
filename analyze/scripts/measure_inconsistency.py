import sys
import time
import json
import signal
import argparse
import numpy as np
import matplotlib.pyplot as plt


# parser = argparse.ArgumentParser()
# parser.add_argument('--num_servers', dest='num_servers', type=int, required=True)
# parser.add_argument('--drop_rate', dest='drop_rate', type=float, required=True)
# args = parser.parse_args()

membership_path = '../../Inconsistency-on-Medley/bin/membership/'
num_servers = 49  # args.num_servers
update_interval = 1000
result = []
drop_rate = 0.05  # args.drop_rate


def handler(sig, frame):
    print('clean up')
    print(result)
    np.savetxt(f'plots/inconsistency/{drop_rate}_{num_servers}.txt', result)
    plt.figure(figsize=(10, 7))
    plt.plot(result)
    plt.grid(axis='y')
    plt.title(f'drop_rate={drop_rate}')
    plt.savefig(f'plots/inconsistency/{drop_rate}_{num_servers}.png')
    # plt.show()
    plt.close()
    sys.exit(0)


def estimate():
    fns = [membership_path + str(i) + '.json' for i in range(num_servers)]
    for ct in range(60):
        time.sleep(update_interval / 1000)
        stats = [0 for _ in range(num_servers)]
        for fn in fns:
            while True:
                try:
                    with open(fn, 'r') as f:
                        obj = json.load(f)
                        for entry in obj['status']:
                            stats[int(entry)] += 1
                    break
                except:
                    continue
        result.append(max(num_servers - np.asarray(stats)))
        print(ct, result[-1])
    np.savetxt(f'plots/inconsistency/{drop_rate}_{num_servers}.txt', result)


# signal.signal(signal.SIGINT, handler)
estimate()
# signal.pause()
