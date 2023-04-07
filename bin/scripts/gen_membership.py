import json
import sys
import numpy as np


num_nodes = int(sys.argv[1])
missings = {}
threshold = 5
for node in range(threshold):
    num_missing = np.random.randint(num_nodes // 4, num_nodes // 2)
    candidates = set(list(range(num_nodes)))
    candidates.remove(node)
    candidates = list(candidates)
    np.random.shuffle(candidates)
    for v in candidates[:num_missing]:
        if v not in missings:
            missings[v] = set()
        missings[v].add(node)

for node in range(num_nodes):
    d = {'suspects': {}, 'status': {}}
    for v in range(num_nodes):
        if node in missings and v in missings[node]:
            continue
        d['status'][str(v)] = 'active'
    obj = json.dumps(d, indent=2)
    with open(f'membership/{node}.json', 'w') as f:
        f.write(obj)
