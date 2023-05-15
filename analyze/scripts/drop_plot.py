import json 
import matplotlib.pyplot as plt 
# import pandas as pd
import numpy as np

# CUT_FIRST = True
CUT_FIRST = False
CUT_LAST = False
# CUT_LAST = True

MODE = "waitTimes"
# MODE = "e2eMsgTotal" 
# MODE = "e2eMsgSizeTotal"
# MODE = "h2hMsgTotal"
# MODE = "h2hMsgSizeTotal"

temp=""
d = {}
slows = 0
# with open("../droprate_256_inc30.txt") as f:
# for filename in ["../varydrop_data_random256_cr64_100.txt","../drop_lower.txt"]:
# for distro in ["W"]:
# for distro in ["P"]:
for distro in ["P","W"]:
    filename = f"../drop_outputs/{distro}_uniform.txt"
    with open(filename) as f:
        for line in f:
            # print("another line")
            if "}{" in line or "run_num" in line:
                temp+="}"
                a = json.loads(temp)
                if "run_num" in line:
                    temp = ""
                else:
                    temp = "{"
                lst = a['algorithmMetric']['waitTimes']
                if a['algorithmMetric']['slows']!=0:
                    slows+=1
                if MODE == "waitTimes":
                    norm_ave = sum(lst)/(len(lst))
                else:
                    # norm_ave = (a['networkMetric'][MODE])/len(lst)
                    norm_ave = a['networkMetric'][MODE]


                rate = a['msg_drop_rate']
                if rate in d:
                    d[rate].append(norm_ave)
                else:
                    d[rate] = [norm_ave]


            else:
                temp+=line
        # print(f"leftover: {temp}")
    a = json.loads(temp)
    if "run_num" in line:
        temp = ""
    else:
        temp = "{"
    lst = a['algorithmMetric']['waitTimes']
    if a['algorithmMetric']['slows']!=0:
        slows+=1
    if MODE == "waitTimes":
        norm_ave = sum(lst)/(len(lst))
    else:
        norm_ave = a['networkMetric'][MODE]

    rate = a['msg_drop_rate']
    if rate in d:
        d[rate].append(norm_ave)
    else:
        d[rate] = [norm_ave]

    temp = ""




        
    # print(d)
    mins = []
    maxs = []
    _25ths = []
    _75ths = []
    inds = []
    aves = []
    for key, value in d.items():
        st = sorted(value)
        # print(len(st))
        mins.append(st[0])
        maxs.append(st[-1])
        _25ths.append(st[len(st)//4])
        _75ths.append(st[3*(len(st)//4)])
        inds.append(key)
        aves.append(sum(st)/len(st))

    thick = 0.04
    thin = 0.01


    if CUT_FIRST:
        inds.pop(0)
        maxs.pop(0)
        _75ths.pop(0)
        _25ths.pop(0)
        mins.pop(0)
        aves.pop(0)

    # print(f"Sizes before trim - {len(inds)}")
    if CUT_LAST:
        max_ind = inds.index(max(inds))
        inds.pop(max_ind)
        maxs.pop(max_ind)
        _75ths.pop(max_ind)
        _25ths.pop(max_ind)
        mins.pop(max_ind)
        aves.pop(max_ind)

    # print(f"Sizes after trim - {len(inds)}")


    # print(inds)
    # print(aves)
    # print(inds)
    # print(inds)

    inds = np.array(inds)
    maxs = np.array(maxs)
    _75ths = np.array(_75ths)
    _25ths = np.array(_25ths)
    mins = np.array(mins)
    aves = np.array(aves)

    plt.bar(inds,maxs-_75ths,width=thin,bottom=_75ths)
    plt.bar(inds,_75ths-_25ths,width=thick,bottom=_25ths)
    plt.bar(inds,_25ths-mins,width=thin,bottom=mins)
    plt.scatter(inds,aves,s=10)



plt.xlabel('Drop Rate')

plt.ylabel(f"{MODE}")
plt.ylim(ymin=1)
plt.yscale('log')
plt.xticks(inds)

plt.title(f"{MODE} with Drop Rate")
# plt.subtitle("(N=256,conc=64)")
plt.show()
# plt.savefig("../img/test.png")
plt.savefig("../img/test.png")

print(f"slows: {slows}")