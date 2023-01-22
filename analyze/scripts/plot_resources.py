import numpy as np
import matplotlib.pyplot as plt

plt.rcParams.update({'font.size': 18, 'font.family': 'Times New Roman'})
servers = [i for i in range(10, 26)]
input_dir = '../../LeaderElection-on-Pis/analyze/results/logs/'
plot_dir = '../../icdcs figures/plots/zookeeper/'  # '../../LeaderElection-on-Pis/analyze/plots/'


def clean_data(data):
    ret = []
    length = max([len(d) for d in data])
    for d in data:
        ret.append([float(v) for v in d] + [0.0 for _ in range(length - len(d))])
    return np.asarray(ret).mean(axis=0)


def clean_packet(packet):
    packet.sort()
    vals = []
    time = 0
    count = 0
    for t, v in packet:
        if t > time + 1:
            vals.append(count)
            count = 0
            time += 1
        count += v
    vals = np.asarray(vals)

    half_window = 25
    if vals.shape[0] < 10:
        half_window = 2
    order = 3
    b = np.mat([[k**i for i in range(order+1)] for k in range(-half_window, half_window+1)])
    m = np.linalg.pinv(b).A[0]
    first_vals = vals[0] - np.abs(vals[1:half_window+1][::-1] - vals[0])
    last_vals = vals[-1] + np.abs(vals[-half_window-1:-1][::-1] - vals[-1])
    vals = np.concatenate((first_vals, vals, last_vals))
    ret = np.convolve(m[::-1], vals, mode='valid')
    return ret


def get_data(fn, run_num=1):
    cpu = []
    mem = []
    packet = []
    for server in servers:
        with open(f'{input_dir}/run_{run_num}/{server}/{fn}.txt', 'r') as f:
            data = np.asarray([s.split() for s in f.readlines()]).T
            cpu.append(data[0])
            mem.append(data[1])
        with open(f'{input_dir}/run_{run_num}/{server}/{fn}_packets.txt', 'r') as f:
            data = [[float(v.split()[4]), int(v.split()[6])] for v in f.readlines()]
            packet += data
    cpu = clean_data(cpu)
    mem = clean_data(mem)
    packet = clean_packet(packet)
    return cpu, mem, packet


def plot(total, zookeeper, ylabel, fn):
    fig, ax = plt.subplots(figsize=(10, 5))
    handle1, = ax.plot(total, '-')
    handle2, = ax.plot(zookeeper, '--')

    ax.set_xlabel('Time (s)', fontsize=20, fontweight='bold')
    ax.set_ylabel(ylabel, fontsize=20, fontweight='bold')
    ax.grid(axis='y')
    ax.legend([handle1, handle2], ['c-tolerant Election Protocol', 'Zookeeper'])

    plt.tight_layout(pad=0.75)
    plt.savefig(f'{plot_dir}/{fn}.png')


cpu_medley, mem_medley, packet_medley = get_data('medley', 1)
cpu_leader, mem_leader, packet_leader = get_data('leader', 3)
cpu_zookeeper, mem_zookeeper, packet_zookeeper = get_data('zookeeper', 2)

cpu_total = np.maximum(cpu_medley, np.pad(cpu_leader, (0, cpu_medley.shape[0] - cpu_leader.shape[0]), 'constant'))
mem_total = np.maximum(mem_medley, np.pad(mem_leader, (0, mem_medley.shape[0] - mem_leader.shape[0]), 'constant'))
packet_total = packet_medley + np.pad(packet_leader, (0, packet_medley.shape[0] - packet_leader.shape[0]), 'constant')

stable_mem = mem_total[100:575].mean()
spike_mem = mem_total.max()

start = 100
length = 600
cpu_total = cpu_total[start:length]
cpu_zookeeper = cpu_zookeeper[start:length]
mem_total = mem_total[start:length]
mem_zookeeper = mem_zookeeper[start:length]
packet_total = packet_total[start:length]
packet_zookeeper = packet_zookeeper[start:length]
plot(cpu_total, cpu_zookeeper, 'CPU Utilization (%)', 'cpu')
plot(mem_total, mem_zookeeper, 'Memory Utilization (%)', 'memory')
plot(packet_total / 1000, packet_zookeeper / 1000, 'Bandwidth (Kbps)', 'packet')

print(f'memory spike is {(spike_mem - stable_mem) / stable_mem}% more than average stable memory')
print(f'cpu increase: {(cpu_zookeeper.mean() - cpu_total.mean()) / cpu_zookeeper.mean()}%')
print(f'memory increase: {(mem_zookeeper.mean() - mem_total.mean()) / mem_zookeeper.mean()}%')
print(f'bandwidth increase: {(packet_zookeeper.mean() - packet_total.mean()) / packet_zookeeper.mean()}%')
print(f'our bandwidth average: {packet_total.mean()}, std: {packet_total.std()}')
print(f'zookeeper bandwidth average: {packet_zookeeper.mean()}, std: {packet_zookeeper.std()}')
