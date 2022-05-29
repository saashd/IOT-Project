from java import *
import numpy as np
from scipy.signal import find_peaks
import math
import glob
import matplotlib.pyplot as plt


def get_signal(x_arr, y_arr, z_arr):
    x_arr = np.array(x_arr)
    y_arr = np.array(y_arr)
    z_arr = np.array(z_arr)
    mag_arr = np.sqrt(x_arr ** 2 + y_arr ** 2 + z_arr ** 2)
    mean_mag_arr = mag_arr - np.mean(mag_arr)
    mean_mag_arr[mean_mag_arr < 2.0] = 0
    filter_window_size = 5
    filter_kernel5 = np.convolve(mean_mag_arr,
                                 np.ones((filter_window_size,)) / filter_window_size,
                                 mode='valid')
    return filter_kernel5


def steps_count(x_arr, y_arr, z_arr):
    signal = get_signal(x_arr, y_arr, z_arr)
    count = 0
    for i, j in zip(signal, signal[1:]):
        if i > 0 and j == 0:
            count += 1
    return count


def get_mag_mean_filter_kernel5(file_name):
    actual_steps = None
    t_arr = []
    x_arr = []
    y_arr = []
    z_arr = []
    with open(file_name, newline='') as csvfile:
        for i, row in enumerate(csvfile):
            row = row.replace("\n", "")
            row = row.replace('"', "")
            row = row.split(',')
            if i == 3:
                actual_steps = int(row[1])
            if i >= 6:
                row = [float(j) for j in row]
                t_arr.append(row[0])
                x_arr.append(row[1])
                y_arr.append(row[2])
                z_arr.append(row[3])
    return get_signal(x_arr, y_arr, z_arr), actual_steps


def calc_threshold(data, actual_steps):
    min_diff = None
    best_thesh = None
    for threshold in np.arange(0, 1, 0.001):
        peaks, _ = find_peaks(data, threshold=threshold)
        diff = abs(actual_steps - len(peaks))
        if min_diff is None or min_diff > diff:
            min_diff = diff
            best_thesh = threshold
    return best_thesh


def calc_peak_thresh(data, threshold=0.001):
    print("threshold: ", threshold)
    peaks, _ = find_peaks(data, threshold)
    # Plotting
    peak_pos = [data[pos] for pos in peaks]
    fig = plt.figure(figsize=(15, 5))
    ax = fig.subplots()
    ax.plot(data)
    # ax.scatter(peaks,peak_pos, color = 'r', s = 15, marker = 'D', label = 'Maxima')
    ax.legend()
    ax.grid()
    plt.show()
    plt.savefig("/storage/self/primary/sampled_data/plot.png")
    return len(peaks)


def get_init_threshold(mode):
    walking_data = glob.glob("/storage/self/primary/sampled_data/" + mode + "/*.csv")
    thresh = []
    for file in walking_data:
        data, actual_steps = get_mag_mean_filter_kernel5(file)
        thresh.append(calc_threshold(data, actual_steps))
    optimal_thresh = np.array(thresh).mean()
    return optimal_thresh


def stepsAlg(mode):
    files = glob.glob("/storage/self/primary/sampled_data/" + mode + "/*.csv")
    thresh = []
    for file in files:
        data, actual_steps = getData(file)
        thresh.append(calc_threshold(data, actual_steps))
    optimal_thresh = sum(thresh) / len(thresh)
    return optimal_thresh
