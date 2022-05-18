import numpy as np

def main(list):
    l=[1,2,3]
    arr = np.array(l)
    arr = np.array2string(arr, precision=2, separator=',',
                          suppress_small=True)
    return arr