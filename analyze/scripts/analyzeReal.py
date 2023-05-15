import json 
import matplotlib.pyplot as plt 
# import pandas as pd
import numpy as np
import sys

MODE = "waitTimes"
# MODE = "e2eMsgTotal"
# MODE = "e2eMsgSizeTotal"
# MODE = "h2hMsgTotal"
# MODE = "h2hMsgSizeTotal"

param = sys.argv[1]
batched = sys.argv[2]
param_options = ["ir_ratio","msg_drop_rate","N"]

if param not in param_options:
    print("incorrect param, options are:")
    print(param_options)

def graphline(temporal,spatial,param=param):

    colors = {
        "Poisson,uniform" : "red",
        "Poisson,zipfian" : "orange",
        "Weibull,zipfian" : "blue",
        "Weibull,uniform" : "green"
    }

    abbrev = {
        "P" : "Poisson",
        "W" : "Weibull"
    }

    folders = {
        "ir_ratio":"ratio_outputs",
        "msg_drop_rate": "drop_outputs",
        "N" : "inc_N_outputs"
    }

    batched_terms = {
        "batch" : "distr_batched",
        "Nbatch" : "distr_nonbatched"
    }

    temp=""
    d = {}
        
    with open(f"../{batched_terms[batched]}/{folders[param]}/{temporal}_{spatial}.txt") as f:
        for line in f:
            if "}{" in line or "num_serv" in line:
                temp+="}"
                a = json.loads(temp)
                if "run_num" in line:
                    temp = ""
                else:
                    temp = "{"
                lst = a['algorithmMetric']['waitTimes']
                n = int(a['N'])
                # if a['algorithmMetric']['slows']!=0:
                #     slows+=1
                if MODE == "waitTimes":
                    norm_ave = sum(lst)/(len(lst))
                else:
                    norm_ave = a['networkMetric'][MODE] / n

                
                cr = float(a[param])
                if cr in d:
                    d[cr].append(norm_ave)
                else:
                    d[cr] = [norm_ave]
        
            else:
                temp+=line

        # print(f"temp is {temp}")
        a = json.loads(temp)
        temp = ""
        lst = a['algorithmMetric']['waitTimes']
        n = int(a['N'])
        # if a['algorithmMetric']['slows']!=0:
        #     slows+=1
        if MODE == "waitTimes":
            norm_ave = sum(lst)/(len(lst))
        else:
            norm_ave = a['networkMetric'][MODE] / n

        cr = float(a[param])
        if cr in d:
            d[cr].append(norm_ave)
        else:
            d[cr] = [norm_ave]
        
        # print(f"leftover: {temp}")
        

    # print(sorted(d[1024]))
    # mins = []
    # maxs = []
    # _25ths = []
    # _75ths = []
    inds = []
    aves = []
    for key, value in d.items():
        st = sorted(value)
        # mins.append(st[0])
        # maxs.append(st[-1])
        # _25ths.append(st[len(st)//4])
        # _75ths.append(st[3*(len(st)//4)])
        inds.append(key)
        aves.append(sum(st)/len(st))

    # thick = 10
    # thin = 4

    # print(maxs[-3:])

    inds = np.array(inds)
    # maxs = np.array(maxs)
    # _75ths = np.array(_75ths)
    # _25ths = np.array(_25ths)
    # mins = np.array(mins)
    aves = np.array(aves)

    # plt.bar(inds,maxs-_75ths,width=thin,bottom=_75ths)
    # plt.bar(inds,_75ths-_25ths,width=thick,bottom=_25ths)
    # plt.bar(inds,_25ths-mins,width=thin,bottom=mins)
    # plt.plot(inds,aves)
    # plt.scatter(inds,aves,label=f"{temporal}_{spatial}")
    keystring = f"{abbrev[temporal]},{spatial}"
    if temporal=="W":
        lstyle = "dashed"
        linewidth=2
    else:
        lstyle = "solid"
        linewidth = 3
    plt.scatter(inds,aves,label=keystring,color=f"{colors[keystring]}")
    # plt.scatter(inds,aves,label=keystring,color=f"{colors[keystring]}",linestyle=lstyle,linewidth=linewidth)
    # plt.xlabel(inds)


for temporal in ["P","W"]:
    for spatial in ["uniform","zipfian"]:
        try:
            graphline(temporal,spatial)
        except FileNotFoundError:
            print(f"No data for {temporal},{spatial}")
# graphline("P","zipfian")

if param=="msg_drop_rate":
    plt.yscale("log")
else:
    plt.ylim(ymin=0)

# plt.xscale("log")




# plt.xlabel('N')
# if NORMALIZE:
#     plt.ylabel(f"{MODE} (Normalized)")
# else:
    # plt.ylabel(f"{MODE}")

plt.ylabel(f"{MODE}")
plt.xlabel(param)
plt.legend()
plt.title(f"{MODE} against {param}")
plt.show()
plt.savefig(f"../img/test.png")

# print(f"slows: {slows}")