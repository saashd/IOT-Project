package com.example.tutorial6.StepsDetections;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;

public class StepsDetection {
    private final Context context;
    private final Double threshold;

    public StepsDetection(Context context, Double threshold) {
        this.context = context;
        this.threshold = threshold;
    }

    public Double findPeak(ArrayList<Double> arr) {
        int n = arr.size();
        ArrayList<Double> peaks = new ArrayList<Double>();
        if (n <= 2) {
            return this.threshold;
        }
        // Check for every other element
        for (int i = 1; i < n - 1; i++) {
            // Check if the neighbors are smaller
            if (arr.get(i) >= arr.get(i - 1) && arr.get(i) >= arr.get(i + 1)) {
                peaks.add(arr.get(i));
            }
        }
        double sum = 0;
        for (double d : arr) {
            sum += d;
        }
        if (threshold <= sum/ arr.size()) {
            return sum / arr.size();
        }
        return threshold;
    }


}
