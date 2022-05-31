package com.example.tutorial6.PersonalDetails;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.tutorial6.R;


public class HealthDetailsFragment extends Fragment {

    private double bmi;
    private double maxHR;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);


        bmi = Double.parseDouble(getArguments().getString("bmi"));
        maxHR = Double.parseDouble(getArguments().getString("maxHR"));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_health_details, container, false);

        TextView displayBMI = view.findViewById(R.id.bmi);
        TextView displayHR = view.findViewById(R.id.maxHR);
        displayBMI.setText(bmiRange());
        displayHR.setText("Average Maximum Heart Rate: \n" + maxHR);
        return view;
    }


    public String bmiRange() {
        if (bmi <= 18.5) {
            return "Your BMI is \n"+bmi+"\n You're in the underweight range";
        } else if (18.5 < bmi && bmi <= 24.9) {
            return "Your BMI is \n"+bmi+"\n You're in the healthy weight range";
        } else if (24.9 < bmi && bmi <= 29.9) {
            return "Your BMI is \n"+bmi+"\n You're in the overweight range";
        } else {
            return "Your BMI is \n"+bmi+"\n You're in the obese range";
        }
    }
}