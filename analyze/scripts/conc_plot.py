import json 
import matplotlib.pyplot as plt 
# import pandas as pd
import numpy as np

# CUT_FIRST = True
CUT_FIRST = False

NORMALIZE = True
# NORMALIZE = False

# MODE = "waitTimes"
# MODE = "e2eMsgTotal"
# MODE = "h2hMsgTotal"
# MODE = "e2eMsgSizeTotal"
MODE = "h2hMsgSizeTotal"

temp=""
d = {}
slows = 0
for fname in ["../cr.txt"]:   
# for fname in ["../cr.txt","../cr_lower.txt","../cr_lower_detailed.txt"]:   
    with open(fname) as f:
        for line in f:
            if "}{" in line or "conc_reqs" in line:
                temp+="}"
                a = json.loads(temp)
                temp = "{"
                lst = a['algorithmMetric']['waitTimes']
                if a['algorithmMetric']['slows']!=0:
                    slows+=1
                if MODE == "waitTimes":
                    norm_ave = sum(lst)/(len(lst))
                else:
                    norm_ave = a['networkMetric'][MODE]

                if NORMALIZE:
                    norm_ave /= len(lst)
                
                cr = a['conc_requesters']
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
        if a['algorithmMetric']['slows']!=0:
            slows+=1
        if MODE == "waitTimes":
            norm_ave = sum(lst)/(len(lst))
        else:
            norm_ave = a['networkMetric'][MODE]

        if NORMALIZE:
            norm_ave /= len(lst)
        
        cr = a['conc_requesters']
        if cr in d:
            d[cr].append(norm_ave)
        else:
            d[cr] = [norm_ave]
        
        # print(f"leftover: {temp}")
        

# print(d)
mins = []
maxs = []
_25ths = []
_75ths = []
inds = []
aves = []
for key, value in d.items():
    # if int(key) < 31 and int(key)>1:
        st = sorted(value)
        mins.append(st[0])
        maxs.append(st[-1])
        _25ths.append(st[len(st)//4])
        _75ths.append(st[3*(len(st)//4)])
        inds.append(key)
        aves.append(sum(st)/len(st))

thick = 10
thin = 5

if CUT_FIRST:
    inds.pop(0)
    maxs.pop(0)
    _75ths.pop(0)
    _25ths.pop(0)
    mins.pop(0)
    aves.pop(0)

inds = np.array(inds)
maxs = np.array(maxs)
_75ths = np.array(_75ths)
_25ths = np.array(_25ths)
mins = np.array(mins)
aves = np.array(aves)

plt.bar(inds,maxs-_75ths,width=thin,bottom=_75ths)
plt.bar(inds,_75ths-_25ths,width=thick,bottom=_25ths)
plt.bar(inds,_25ths-mins,width=thin,bottom=mins)
plt.scatter(inds,aves)
plt.ylim(ymin=0,ymax=max(maxs)*1.1)


plt.xlabel('Concurrent Requesters')
if NORMALIZE:
    plt.ylabel(f"{MODE} (Normalized)")
else:
    plt.ylabel(f"{MODE}")

plt.title(f"{MODE} Against Concurrent Requests")
plt.show()
plt.savefig("../img/test.png")

print(f"slows: {slows}")
print(aves)
print(inds)