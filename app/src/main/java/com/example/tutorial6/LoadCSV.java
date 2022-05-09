package com.example.tutorial6;
// Like Tutorial3 with tiny changes in "back" button.


// No need in this Activity. All logic moved to CsvFragment.
// File not deleted only for Yara and Alon convenience

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import java.util.List;


public class LoadCSV extends AppCompatActivity {
    private String fileName;
    private String numOfSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        fileName = getIntent().getStringExtra("fileName");
        numOfSteps = getIntent().getStringExtra("numOfSteps");

        setContentView(R.layout.activity_load_csv);
        Button BackButton = (Button) findViewById(R.id.button_back);
        LineChart lineChart = (LineChart) findViewById(R.id.line_chart);

        LineDataSet lineDataSetAxisX = new LineDataSet(new ArrayList<Entry>(), "X-axis");
        lineDataSetAxisX.setColors(Color.RED);
        LineDataSet lineDataSetAxisY = new LineDataSet(new ArrayList<Entry>(), "Y-axis");
        lineDataSetAxisY.setColors(Color.GREEN);
        LineDataSet lineDataSetAxisZ = new LineDataSet(new ArrayList<Entry>(), "Z-axis");
        lineDataSetAxisZ.setColors(Color.BLUE);

        ArrayList<String[]> csvData = new ArrayList<>();
        try {

            csvData = CsvRead("/storage/self/primary/IOT/" + fileName + ".csv");
            ArrayList<ILineDataSet> dataSets = new ArrayList<>();

            dataSets.add(lineDataSetAxisX);
            dataSets.add(lineDataSetAxisY);
            dataSets.add(lineDataSetAxisZ);

            LineData data = new LineData(dataSets);

            for (int i = 5; i < csvData.size(); i++) {
                String[] parts = csvData.get(i);
                data.addEntry(new Entry(Float.parseFloat(parts[0]) / 1000, Float.parseFloat(parts[1])), 0);
                lineDataSetAxisX.notifyDataSetChanged(); // let the data know a dataSet changed
                data.addEntry(new Entry(Float.parseFloat(parts[0]) / 1000, Float.parseFloat(parts[2])), 1);
                lineDataSetAxisY.notifyDataSetChanged(); // let the data know a dataSet changed
                data.addEntry(new Entry(Float.parseFloat(parts[0]) / 1000, Float.parseFloat(parts[3])), 2);
                lineDataSetAxisZ.notifyDataSetChanged(); // let the data know a dataSet changed

            }

            //        Set dataset labels that appear in the bottom of the chart
            Legend l = lineChart.getLegend();
            l.setTextSize(15f);
            l.setTextColor(Color.BLACK);
            l.setForm(Legend.LegendForm.LINE);
            XAxis xval = lineChart.getXAxis();

            xval.setDrawLabels(true);
            xval.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
            xval.setGranularity(1f);

            lineChart.setData(data);
            lineChart.notifyDataSetChanged(); // let the chart know it's data changed
            lineChart.invalidate();
        } catch (Exception e) {
            Toast.makeText(this, "ERROR", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickBack();
            }
        });
    }

    private void ClickBack() {
        finish();

    }

    private ArrayList<String[]> CsvRead(String path) {
        ArrayList<String[]> CsvData = new ArrayList<>();
        try {
            File file = new File(path);
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] nextline;
            while ((nextline = reader.readNext()) != null) {
                if (nextline != null) {
                    CsvData.add(nextline);

                }
            }

        } catch (Exception e) {
        }
        return CsvData;
    }

    private ArrayList<Entry> DataValues(ArrayList<String[]> csvData) {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        for (int i = 0; i < csvData.size(); i++) {

            dataVals.add(new Entry(Integer.parseInt(csvData.get(i)[1]),
                    Float.parseFloat(csvData.get(i)[0])));


        }

        return dataVals;
    }

}