
import matplotlib.pyplot as plt 
import numpy as np

# DELAY = True
DELAY = False


c=6
h = 50
T = 500
d = np.arange(0.05,1.0,.05)
# d = np.arange(0.05,0.5,.05)
if not DELAY:

    y=(1-(1-d)**c)
    # plt.ylim(ymin=0,ymax=1)
    # plt.xlim(xmin=0,xmax=1)
    plt.xticks(d[1::2])
    plt.plot(d,y)
    plt.xlabel('h2h Drop Rate')
    plt.ylabel('e2e Drop Rate')
    plt.title("Drop Rate Relationship")
    plt.savefig("../img/drop_relationship.png")
else:
    # want to graph entire delay
    def calc_est(r):
        end_frac = (1-(1-r)**c)/((1-r)**c)
        mid_section = (1-(1-r)**(c-1))/r - 1
        return c*h + (T + r*mid_section/(1-r) + h*c*(c-1)/2)*end_frac
    
    y = calc_est(d)
    print(y)
    # plt.ylim(ymin=1)
    plt.xticks(d)
    plt.plot(d,y)
    # plt.yscale('log')
    plt.xlabel('h2h Drop Rate')
    plt.title('Expected E2E message delay')
    plt.ylabel("Expected Delay")
    plt.savefig("../img/delay_estimation.png")