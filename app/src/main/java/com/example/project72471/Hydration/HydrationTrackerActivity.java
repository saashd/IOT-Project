package com.example.project72471.Hydration;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project72471.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HydrationTrackerActivity extends Fragment {

    int start_hour, start_min, end_hour, end_min, notification_interval, glass_size, totalWaterConsumption;
    double quantity;
    private DatabaseReference reference;
    private View view;
    private String todayDate;


    public HydrationTrackerActivity() {
        this.start_hour = -1;
        this.start_min = -1;
        this.end_hour = -1;
        this.end_min = -1;
        this.totalWaterConsumption = 0;
        notification_interval = -1;
        this.quantity = 2.0;
        glass_size = 150;

    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_hydration_tracker, container, false);

        Button addWaterBtn = (Button) view.findViewById(R.id.floatingActionButton);
        addWaterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                totalWaterConsumption = totalWaterConsumption + glass_size;
                reference.child(todayDate).child("total_consumption").setValue(totalWaterConsumption);
                if (totalWaterConsumption >= quantity * 1000) {
                    reference.child(todayDate).child("goalAchieved").setValue(true);
                }
                setUpPieChart();

            }
        });
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Object data = dataSnapshot.child("end_hour").getValue();
                if (data != null) {
                    end_hour = ((Long) data).intValue();
                    end_min = ((Long) dataSnapshot.child("end_min").getValue()).intValue();

                }
                data = dataSnapshot.child("glass_size").getValue();
                if (data != null) {

                    glass_size = ((Long) data).intValue();
                }
                data = dataSnapshot.child("notification_interval").getValue();
                if (data != null) {

                    notification_interval = ((Long) data).intValue();
                }
                data = dataSnapshot.child("quantity").getValue();
                if (data != null) {
                    quantity = (Math.round(Double.parseDouble((String) data) * 10.0)) / 10.0;

                }
                data = dataSnapshot.child("start_hour").getValue();
                if (data != null) {
                    start_hour = ((Long) data).intValue();
                    start_min = ((Long) dataSnapshot.child("start_min").getValue()).intValue();

                }
                {

                    data = dataSnapshot.child(todayDate).child("total_consumption").getValue();

                    if (data != null) {
                        totalWaterConsumption = ((Long) data).intValue();

                    } else {
                        reference.child(todayDate).child("total_consumption").setValue(0);

                    }
                }

//                handleNotification();
                setUpPieChart();
//                setResetApp();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }


        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        reference = FirebaseDatabase.getInstance().getReference().child("Users").child(userUid).child("Hydration");
        todayDate = DateFormat.getDateInstance().format(new Date());

    }


    public void setUpPieChart() {
        PieChart pieChart = (PieChart) view.findViewById(R.id.chart);
        if (pieChart != null) {
            pieChart.getDescription().setEnabled(false);
            pieChart.getLegend().setEnabled(false);
            pieChart.setTransparentCircleColor(Color.WHITE);
            //pieChart.animateX(2000);
            pieChart.animateY(1000);

            List<PieEntry> entries = new ArrayList<>();

            entries.add(new PieEntry(totalWaterConsumption, "Water Consumed"));
            int remaining = Math.max(0, ((int) (quantity * 1000) - totalWaterConsumption));
            entries.add(new PieEntry(remaining, "Remaining"));

            PieDataSet set = new PieDataSet(entries, "Election Results");
            int color_green = getResources().getColor(R.color.teal_700);
            int color_blue = getResources().getColor(R.color.colorPrimary);

            set.setColors(new int[]{color_blue, color_green});
            set.setValueTextSize(25);

            PieData data = new PieData(set);
            pieChart.setData(data);

            pieChart.invalidate(); // refresh
        }
    }


}
