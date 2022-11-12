import sys
import time
import json
import signal
import numpy as np
import matplotlib.pyplot as plt


membership_path = '../../Inconsistency-on-Medley/bin/membership/'
num_servers = 49
update_interval = 200
result = []
drop_rate = 0.2


def handler(sig, frame):
    print('clean up')
    plt.figure(figsize=(10, 7))
    plt.plot(result)
    plt.grid(axis='y')
    plt.title(f'drop_rate={drop_rate}')
    plt.savefig(f'plots/inconsistency_measure/{drop_rate}.png')
    plt.show()
    plt.close()
    sys.exit(0)


def estimate():
    fns = [membership_path + str(i) + '.json' for i in range(num_servers)]
    while True:
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


signal.signal(signal.SIGINT, handler)
estimate()
signal.pause()
