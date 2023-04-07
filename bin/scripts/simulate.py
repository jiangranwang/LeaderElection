import numpy as np
import matplotlib.pyplot as plt


plt.rcParams.update({'font.size': 18, 'font.family': 'Times New Roman'})


def gen_membership(num_node, low_hash=False, churn=None):
    if low_hash:
        miss = range(5)
    else:
        threshold = min(num_node // 10, round(np.random.exponential(2.5)))
        miss = np.random.choice(range(num_node), threshold, replace=False)
    mean = num_node // 10
    bound = num_node // 20

    membership = {i: set(range(num_node)) for i in range(num_node)}  # node v: which memberships node v is missing in
    for node in miss:
        if churn is None:
            num_missing = np.random.randint(max(0, mean - bound), min(num_node, mean + bound))
        else:
            num_missing = churn
        candidates = set(list(range(num_node)))
        candidates.remove(node)
        candidates = list(candidates)
        np.random.shuffle(candidates)
        for v in candidates[:num_missing]:
            membership[v].remove(node)
    return membership


def sample(membership, num_node, sample_size):
    node2missing = [set() for _ in range(num_node)]
    for i in range(num_node):
        missing = set([i for i in range(num_node)])
        for key in membership[i]:
            missing.remove(int(key))
        node2missing[i] = missing
    targets = np.random.choice(range(num_node), sample_size, replace=False)
    tops = {}
    for node in targets:
        for miss in node2missing[node]:
            tops[miss] = tops.get(miss, 0) + 1
    arr = [tops[k] for k in tops]
    m = 0 if len(arr) == 0 else max(arr)
    estimate = m * num_node / sample_size
    return estimate


def z_score(num_node, sample_size, num_sample=10, num_run=100):
    vals = []
    for _ in range(num_run):
        membership = gen_membership(num_node)
        missing2node = [set() for _ in range(num_node)]
        for i in range(num_node):
            missing = set([i for i in range(num_node)])
            for key in membership[i]:
                missing.remove(int(key))
            for key in missing:
                missing2node[key].add(i)

        c = 0
        for i in range(num_node):
            c = max(c, len(missing2node[i]))
        if c == 0:
            continue
        for _ in range(num_sample):
            estimate_c = sample(membership, num_node, sample_size)
            vals.append((c, estimate_c))

    return vals


def feedback(dynamic_membership, num_node, churn, start_c, num_run, sample_sizes=[]):
    membership = gen_membership(num_node, True, churn)
    start_churn = churn
    estimate_c = [start_c for _ in range(len(sample_sizes) + 1)]
    vals = []
    alpha = 0.2
    for it in range(num_run):
        if dynamic_membership:
            churn = max(int(start_churn * 0.5), min(int(start_churn * 1.5), churn + round((np.random.uniform() - 0.5) * 5)))
            membership = gen_membership(num_node, True, churn)
        reverse_membership = {k: set() for k in range(num_node)}
        for node in membership:
            for v in membership[node]:
                reverse_membership[v].add(node)
        actual_c = max([num_node - len(reverse_membership[k]) for k in range(num_node)])

        # run pseudo leader election with multiple initiators
        num_initiator = 10  # np.random.randint(1, 10)
        diff = 0
        for _ in range(num_initiator):
            # get elected leader by querying random estimate_c nodes
            targets = np.random.choice(range(num_node), estimate_c[0] + 1, replace=False)
            responses = set([min(membership[node]) for node in targets])
            diff += min(2, len(responses))
            # diff += sum([0 if 0 in membership[node] else 1 for node in targets])
        diff /= num_initiator
        curr_c = num_node * (1 - np.exp(np.log(2.01-diff)/(estimate_c[0]+1)))
        estimate_c[0] = round((1-alpha) * estimate_c[0] + alpha * curr_c)

        # high_threshold = 0.2 - np.log(estimate_c) * 0.035
        # low_threshold = high_threshold / 2
        # if diff > high_threshold:
        #     estimate_c = min(estimate_c + 1, num_node)
        # elif diff < low_threshold:
        #     estimate_c = max(1, estimate_c - 1)
        # print(it, actual_c, estimate_c, diff)
        # print(estimate_c, actual_c, diff)
        # print(2 - (1-actual_c/num_node)**(estimate_c+1), diff, actual_c, c)
        val = [actual_c, estimate_c[0]]
        for i, sample_size in enumerate(sample_sizes):
            curr_c = sample(membership, num_node, sample_size)
            estimate_c[i+1] = round((1-alpha) * estimate_c[i+1] + alpha * curr_c)
            val.append(estimate_c[i+1])
        vals.append(val)
        # if actual_c <= estimate_c[0]:
        #     break
    return vals


def plot_estimate(num_nodes, sample_sizes, num_run):
    fig, ax = plt.subplots(figsize=(10, 7))

    rerun = False
    if rerun:
        means = [[] for _ in range(1 + 2 * len(sample_sizes))]
        stds = [[] for _ in range(1 + 2 * len(sample_sizes))]
        for i, num_node in enumerate(num_nodes):
            vals = feedback(True, num_node, num_node // 10, num_node // 10, num_run, sample_sizes)
            curr = np.asarray([v[1] - v[0] for v in vals])
            means[0].append(curr.mean())
            stds[0].append(curr.std())
            for j in range(len(sample_sizes)):
                curr = np.asarray([v[j+2] - v[0] for v in vals])
                means[2*(j+1)].append(curr.mean())
                stds[2*(j+1)].append(curr.std())

        for i, sample_size in enumerate(sample_sizes):
            for num_node in num_nodes:
                vals = z_score(num_node, sample_size, 10, num_run)
                curr = np.asarray([v[1] - v[0] for v in vals])
                means[2*i+1].append(curr.mean())
                stds[2*i+1].append(curr.std())
        means = np.asarray(means)
        stds = np.asarray(stds)
        np.save('means', means)
        np.save('stds', stds)
    else:
        means = np.load('means.npy')
        stds = np.load('stds.npy')
    x = np.arange(len(num_nodes))
    offset = -0.3
    colors = ['orange', 'red', 'red', 'green', 'green', 'blue', 'blue']
    fmts = ['o', '*', 'o', '*', 'o', '*', 'o']
    handles = []

    for i in range(len(means)):
        for j, num_node in enumerate(num_nodes):
            print(i, num_node, means[i][j], stds[i][j], abs(means[i][j]) / num_node)
        handle = ax.errorbar(x + offset, means[i], stds[i], color=colors[i], fmt=fmts[i], capsize=10, markersize=10)
        offset += 0.1
        # handle, = plt.plot([1, 1], '-', color=colors[i], marker=fmts[i])
        handles.append(handle)
        # handle.set_visible(False)

    legends = ['Feedback']
    for sample_size in sample_sizes:
        legends.append(f'Size={sample_size} Sample')
        legends.append(f'Size={sample_size} Sample-Window')

    ax.set_xticks(x)
    ax.set_xticklabels(num_nodes)
    ax.set_xlabel('Number of Nodes')
    ax.set_ylabel(r'Average of Estimate $c$ - Actual $c$')

    ax.legend(handles, legends, bbox_to_anchor=(0., 1.02, 1., .102), loc='lower left',
              ncol=2, mode="expand", borderaxespad=0.)

    plt.grid()
    plt.tight_layout()
    plt.savefig('../analyze/accuracy.png')


def plot_diff_cs(ks, num_run, num_node):
    datas = []
    for k in ks:
        data = []
        for run in range(num_run):
            membership = gen_membership(num_node, True, num_node // 5)
            num_initiator = 5
            for _ in range(num_initiator):
                targets = np.random.choice(range(num_node), k + 1, replace=False)
                data.append(min([min(membership[node]) for node in targets]))
        datas.append((np.mean(data), np.std(data)))
        print(k, sum(data) / (num_run * num_initiator))

    x = np.arange(len(ks))
    datas = np.asarray(datas).T
    fig, ax = plt.subplots(figsize=(10, 6))
    ax.errorbar(x, datas[0], datas[1], fmt='o', capsize=10, markersize=10)
    ax.set_xticks(x)
    ax.set_xticklabels(ks)
    ax.set_xlabel(r'Value $c$')
    ax.set_ylabel('Leader Hash Rank')
    # plt.tight_layout()
    plt.savefig('../analyze/diff_cs.png')


def plot_diff_initiators(num_initiators, num_run, num_node):
    datas = []
    for num_initiator in num_initiators:
        data = []
        for run in range(num_run):
            membership = gen_membership(num_node, True, num_node // 5)
            for _ in range(num_initiator):
                targets = np.random.choice(range(num_node), 3, replace=False)
                data.append(min([min(membership[node]) for node in targets]))
        datas.append((np.mean(data), np.std(data)))

    x = np.arange(len(num_initiators))
    datas = np.asarray(datas).T
    fig, ax = plt.subplots(figsize=(10, 6))
    ax.errorbar(x, datas[0], datas[1], fmt='o', capsize=10, markersize=10)
    ax.set_xticks(x)
    ax.set_xticklabels(num_initiators)
    # plt.tight_layout()
    ax.set_xlabel('Number of Initiators')
    ax.set_ylabel('Leader Hash Rank')
    plt.savefig('../analyze/diff_initiators.png')


if __name__ == '__main__':
    plot_diff_cs([1, 2, 3, 4, 5], 1000, 128)
    # plot_diff_initiators([1, 3, 5, 7, 9], 1000, 128)
    # num_nodes = [128, 256, 512, 1024]
    # sample_sizes = [10, 30, 50]
    # num_run = 100
    # plot_estimate(num_nodes, sample_sizes, num_run)

    # vals = feedback(True, 128, 13, 13, 100)
    # vals = np.asarray(vals).T
    # plt.figure(figsize=(10, 5))
    # plt.plot(vals[0])
    # plt.plot(vals[1])
    # plt.legend([r'Actual $c$', r'Estimate $c$'])
    # plt.xlabel('Number of Iterations')
    # plt.ylabel(r'Value $c$')
    # plt.tight_layout()
    # plt.savefig('../analyze/feedback_example.png')

    # freqs = {i: [] for i in range(13)}
    # for _ in range(100):
    #     vals = feedback(False, 128, 13, 1, 100)
    #     vals = np.asarray(vals).T
    #     diffs = vals[0] - vals[1]
    #     for i in range(len(diffs)):
    #         if diffs[i] < 0:
    #             break
    #         freqs[diffs[i]].append(i)
    # means = [np.mean(freqs[i]) for i in range(13)]
    # stds = [np.std(freqs[i]) for i in range(13)]
    # fig, ax = plt.subplots(figsize=(10, 5))
    # ax.errorbar(range(13), means, stds, fmt='o', capsize=10, markersize=10)
    # ax.set_xlabel(r'Actual $c$ - Estimate $c$')
    # ax.set_ylabel('Number of Iterations')
    # plt.tight_layout()
    # plt.savefig('../analyze/convergence.png')
