import numpy as np
from java import *
from calcStepsAlgo import stepsAlg, calc_peak_thresh


def calcBPM(t_vec, ir_vec, red_vec):
    t_vec = np.array(t_vec)
    ir_vec = np.array(ir_vec)
    red_vec = np.array(red_vec)
    s1 = 0  # change this for different range of data
    s2 = len(t_vec)  # change this for ending range of data
    t_vec = np.array(t_vec[s1:s2])
    ir_vec = ir_vec[s1:s2]
    red_vec = red_vec[s1:s2]

    # sample rate and heart rate ranges
    samp_rate = 1 / np.mean(np.diff(t_vec))  # average sample rate for determining peaks
    heart_rate_range = [0, 250]  # BPM
    heart_rate_range_hz = np.divide(heart_rate_range, 60.0)
    max_time_bw_samps = 1 / heart_rate_range_hz[1]  # max seconds between beats
    max_pts_bw_samps = max_time_bw_samps * samp_rate  # max points between beats

    ## FFT and plotting frequency spectrum of data
    f_vec = np.arange(0, int(len(t_vec) / 2)) * (samp_rate / (len(t_vec)))
    f_vec = f_vec * 60
    fft_var = np.fft.fft(ir_vec)
    fft_var = np.append(np.abs(fft_var[0]).astype(int),
                        2.0 * np.abs(fft_var[1:int(len(fft_var) / 2)]),
                        np.abs(fft_var[int(len(fft_var) / 2)]).astype(int))

    bpm_max_loc = np.argmin(np.abs(f_vec - heart_rate_range[1]))
    f_step = 1
    f_max_loc = np.argmax(fft_var[f_step:bpm_max_loc]) + f_step

    return int(f_vec[f_max_loc])


def calcSteps(mode, mean_mag_arr):
    if mode=="Running":
        threshold=0.002121
    elif mode=="Walking":
        threshold=0.00161
    # threshold = stepsAlg(mode)
    return calc_peak_thresh(mean_mag_arr, threshold=threshold)


