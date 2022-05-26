import numpy as np
import matplotlib.pyplot as plt
from java import *


def calcBPM(t_vec, ir_vec, red_vec):
    t_vec = np.array(t_vec)
    ir_vec = np.array(ir_vec)
    red_vec = np.array(red_vec)

    # plt.style.use('ggplot')
    s1 = 0 # change this for different range of data
    s2 = len(t_vec) # change this for ending range of data
    t_vec = np.array(t_vec[s1:s2])
    ir_vec = ir_vec[s1:s2]
    red_vec = red_vec[s1:s2]

    # sample rate and heart rate ranges
    samp_rate = 1/np.mean(np.diff(t_vec)) # average sample rate for determining peaks
    heart_rate_range = [0,250] # BPM
    heart_rate_range_hz = np.divide(heart_rate_range,60.0)
    max_time_bw_samps = 1/heart_rate_range_hz[1] # max seconds between beats
    max_pts_bw_samps = max_time_bw_samps*samp_rate # max points between beats


    ## plotting time series data
    # fig = plt.figure(figsize=(14,8))
    # ax1 = fig.add_subplot(111)
    # ax1.set_xlabel('Time [s]',fontsize=24)
    # ax1.set_ylabel('IR Amplitude',fontsize=24,color='#CE445D',labelpad=10)
    # ax1.tick_params(axis='both',which='major',labelsize=16)
    # plt1 = ax1.plot(t_vec,ir_vec,label='IR',color='#CE445D',linewidth=4)
    # ax1_2 = plt.twinx()
    # ax1_2.grid('off')
    # ax1_2.set_ylabel('Red Amplitude',fontsize=24,color='#37A490',labelpad=10)
    # ax1_2.tick_params(axis='y',which='major',labelsize=16)
    # plt2 = ax1_2.plot(t_vec,red_vec,label='Red',color='#37A490',linewidth=4)
    # lns = plt1+plt2
    # labels = [l.get_label() for l in lns]
    # ax1_2.legend(lns,labels,fontsize=16,loc='upper center')
    # plt.xlim([t_vec[0],t_vec[-1]])
    # plt.tight_layout(pad=1.2)

    ## FFT and plotting frequency spectrum of data
    f_vec = np.arange(0,int(len(t_vec)/2))*(samp_rate/(len(t_vec)))
    f_vec = f_vec*60
    fft_var = np.fft.fft(red_vec)
    fft_var = np.append(np.abs(fft_var[0]).astype(int),2.0*np.abs(fft_var[1:int(len(fft_var)/2)]),
                np.abs(fft_var[int(len(fft_var)/2)]).astype(int))

    bpm_max_loc = np.argmin(np.abs(f_vec-heart_rate_range[1]))
    f_step = 1
    f_max_loc = np.argmax(fft_var[f_step:bpm_max_loc])+f_step
    # print('BPM: {0:2.1f}'.format(f_vec[f_max_loc]))
    # fig2 = plt.figure(figsize=(14,8))
    # ax2 = fig2.add_subplot(111)
    # ax2.loglog(f_vec,fft_var,color=[50/255,108/255,136/255],linewidth=4)
    # ax2.set_xlim([0,f_vec[-1]])
    # ax2.set_ylim([np.min(fft_var)-np.std(fft_var),np.max(fft_var)])
    # ax2.tick_params(axis='both',which='major',labelsize=16)
    # ax2.set_xlabel('Frequency [BPM]',fontsize=24)
    # ax2.set_ylabel('Amplitude',fontsize=24)
    # ax2.annotate('Heart Rate: {0:2.0f} BPM'.format(f_vec[f_max_loc]),
    #      xy = (f_vec[f_max_loc],fft_var[f_max_loc]+(np.std(fft_var)/10)),xytext=(-10,70),
    #      textcoords='offset points',arrowprops=dict(facecolor='k'),
    #      fontsize=16,horizontalalignment='center')
    # plt.show()
    # fig2.savefig('/storage/self/primary/IOT/max30102_fft_heart_rate.png',dpi=300,facecolor=[252/255,252/255,252/255])

    return int(f_vec[f_max_loc])



def main(x_vals, y_vals, z_vals):
    x_arr = np.array(x_vals)
    y_arr = np.array(y_vals)
    z_arr = np.array(z_vals)

    # Calc. norm of the acceleration.
    mag = np.sqrt(np.sum(np.square(x_arr) + np.square(y_arr) + np.square(z_arr)))

    # Subtracting the mean from the data will remove any constant effects, such as gravity.
    magNoG = mag - np.mean(mag)

    return magNoG
