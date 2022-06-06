package com.example.project72471.Statistics;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project72471.R;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public class StatisticsFragment extends Fragment {


    private BarChart barChart;
    private LineChart lineChart;
    private DatabaseReference reference;
    private String currentMonth;
    private List<BarEntry> entriesHydration = new ArrayList<>();
    private View view;
    private int monthMaxDays;
    private HorizontalBarChart horizBarChart;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        reference = FirebaseDatabase.getInstance().getReference().child("Users").child(userUid);
        LocalDate currentdate = LocalDate.now();
        currentMonth = String.valueOf(new DateFormatSymbols().getShortMonths()[currentdate.getMonthValue() - 1]);

        Calendar c = Calendar.getInstance();
        monthMaxDays = c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_statistics, container, false);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


                DataSnapshot referenceHydration = dataSnapshot.child("Hydration");
                createWaterConsumptionChart(referenceHydration);
                createStepsChart(dataSnapshot);


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        return view;
    }

    private void addVal(HashMap<Integer, Float> map, Integer key, Float val) {
        if (!map.containsKey(key)) {
            map.put(key, 0.0F);
        }
        map.put(key, map.get(key) + val);

    }

    private void createStepsChart(DataSnapshot dataSnapshot) {
        DataSnapshot referenceWalking = dataSnapshot.child("Walking");
        DataSnapshot referenceRunning = dataSnapshot.child("Running");
        HashMap<Integer, Float> walkingStepsPerDay = new HashMap<Integer, Float>();
        HashMap<Integer, Float> runningStepsPerDay = new HashMap<Integer, Float>();
        ArrayList<BarEntry> entriesStepsRunning = new ArrayList<BarEntry>();
        ArrayList<BarEntry> entriesStepsWalking = new ArrayList<BarEntry>();
        for (DataSnapshot data : referenceWalking.getChildren()) {
            if (data.getKey().indexOf(currentMonth) != -1) {
                int day = Integer.parseInt(data.getKey().replace(",", "").split(" ")[1]);
                float steps = ((Long) data.child("steps").getValue()).intValue();

                addVal(walkingStepsPerDay, day, steps);
            }
        }
        for (DataSnapshot data : referenceRunning.getChildren()) {

            if (data.getKey().indexOf(currentMonth) != -1) {
                int day = Integer.parseInt(data.getKey().replace(",", "").split(" ")[1]);
                float steps = ((Long) data.child("steps").getValue()).intValue();
                addVal(runningStepsPerDay, day, steps);
            }
        }

//        for (int i = 1; i <= monthMaxDays; i++) {
//            if (!walkingStepsPerDay.containsKey(i)) {
//                addVal(walkingStepsPerDay, i, 0.0F);
//            }
//            if (!runningStepsPerDay.containsKey(i)) {
//                addVal(runningStepsPerDay, i, 0.0F);
//            }
//        }

        for (Map.Entry<Integer, Float> entry : walkingStepsPerDay.entrySet()) {

            entriesStepsWalking.add(new BarEntry(entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<Integer, Float> entry : runningStepsPerDay.entrySet()) {
            entriesStepsRunning.add(new BarEntry(entry.getKey(), entry.getValue()));
        }


        Collections.sort(entriesStepsRunning, new EntryXComparator());
        Collections.sort(entriesStepsWalking, new EntryXComparator());

        horizBarChart = (HorizontalBarChart) view.findViewById(R.id.bar_chart_steps);
        horizBarChart.getDescription().setText("Day");


        BarDataSet set1 = new BarDataSet(entriesStepsRunning, "Tot. Steps while Running");
        set1.setColors(new int[]{Color.parseColor("#E53935")});

        BarDataSet set2 = new BarDataSet(entriesStepsWalking, "Tot. Steps while Walking");
        set2.setColors(new int[]{Color.parseColor("#1E88E5")});


        List<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);
        dataSets.add(set2);

        XAxis xAxis = horizBarChart.getXAxis();
        xAxis.setGranularity(1f);
        horizBarChart.setVisibleXRangeMaximum(monthMaxDays);

        BarData data = new BarData(dataSets);
        data.setBarWidth(0.9f); // set custom bar width
        horizBarChart.getXAxis().setDrawGridLines(false);
        horizBarChart.getAxisLeft().setDrawGridLines(false);
        horizBarChart.setData(data);
        horizBarChart.getAxisLeft().setAxisMinimum(0);
        horizBarChart.setFitBars(true); // make the x-axis fit exactly all bars
        horizBarChart.invalidate();


    }

    private void createHoursChart(DataSnapshot dataSnapshot) {
        DataSnapshot referenceWalking = dataSnapshot.child("Walking");
        DataSnapshot referenceRunning = dataSnapshot.child("Running");
        int[] result = getStepsAndDuration(referenceWalking);
        int totHoursWalking = result[0];
        int totStepsWalking = result[1];
        result = getStepsAndDuration(referenceRunning);
        int totHoursRunning = result[0];
        int totStepsRunning = result[1];

    }

    private int[] getStepsAndDuration(DataSnapshot dataSnapshot) {
        int totHours = 0;
        int totSteps = 0;
        for (DataSnapshot data : dataSnapshot.getChildren()) {
            if (data.getKey().indexOf(currentMonth) != -1) {
                totHours += ((Long) data.child("duration").getValue()).intValue();
                totSteps += ((Long) data.child("steps").getValue()).intValue();
            }
        }
        return new int[]{totHours, totSteps};
    }

    private void createWaterConsumptionChart(DataSnapshot referenceHydration) {
        float dailyGoal = 0;
        for (DataSnapshot data : referenceHydration.getChildren()) {
            if (data.getKey().indexOf(currentMonth) != -1) {

                int day = Integer.parseInt(data.getKey().replace(",", "").split(" ")[1]);
                float amount = ((Long) data.child("total_consumption").getValue()).floatValue() / 1000;
                entriesHydration.add(new BarEntry(day, amount));
            } else if (data.getKey().equals("quantity")) {
                dailyGoal = Float.parseFloat((String) data.getValue());

            }
        }


        LimitLine ll1 = new LimitLine(dailyGoal, "Daily Goal");
        ll1.setLineWidth(4f);
        ll1.enableDashedLine(1.0f, 1.0f, 0f);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1.setTextSize(10f);

        barChart = (BarChart) view.findViewById(R.id.bar_chart);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
        leftAxis.addLimitLine(ll1);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        barChart.setVisibleXRangeMaximum(monthMaxDays);
//        xAxis.setLabelCount(entriesHydration.size(), true);

        BarDataSet set = new BarDataSet(entriesHydration, "Water Amount (liters)");


        BarData data = new BarData(set);
        data.setBarWidth(0.9f); // set custom bar width
        barChart.getDescription().setText("Day");
        barChart.setData(data);
        barChart.setFitBars(true); // make the x-axis fit exactly all bars
        barChart.invalidate(); // refresh


    }


}