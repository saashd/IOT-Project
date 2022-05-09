package com.example.tutorial6;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
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
import java.io.FilenameFilter;
import java.util.ArrayList;


public class CsvFragment extends Fragment {

    Spinner spinner;
    String selectedCSV;
    String[] filenames;
    String fileDate;
    String activityType;
    String totSteps;

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_csv, container, false);
        Button loadCSV = (Button) view.findViewById(R.id.loadCSV);
        TextView date = (TextView) view.findViewById(R.id.date);
        TextView type = (TextView) view.findViewById(R.id.type);
        TextView steps = (TextView) view.findViewById(R.id.steps);

        //Get names of all files in IOT directory
        getCSVFiles();

        spinner = (Spinner) view.findViewById(R.id.csvSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.support_simple_spinner_dropdown_item, filenames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCSV = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        LineChart lineChart = (LineChart) view.findViewById(R.id.line_chart);

        LineDataSet lineDataSetAxisX = new LineDataSet(new ArrayList<Entry>(), "X-axis");
        lineDataSetAxisX.setColors(Color.RED);
        LineDataSet lineDataSetAxisY = new LineDataSet(new ArrayList<Entry>(), "Y-axis");
        lineDataSetAxisY.setColors(Color.GREEN);
        LineDataSet lineDataSetAxisZ = new LineDataSet(new ArrayList<Entry>(), "Z-axis");
        lineDataSetAxisZ.setColors(Color.BLUE);


        loadCSV.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getActivity(), selectedCSV, Toast.LENGTH_SHORT).show();
                try {
                    ArrayList<String[]> csvData = new ArrayList<>();
                    csvData = CsvRead("/storage/self/primary/IOT/" + selectedCSV);
                    ArrayList<ILineDataSet> dataSets = new ArrayList<>();

                    dataSets.add(lineDataSetAxisX);
                    dataSets.add(lineDataSetAxisY);
                    dataSets.add(lineDataSetAxisZ);

                    LineData data = new LineData(dataSets);
                    date.setText("EXPERIMENT TIME: " + csvData.get(1)[1]);
                    type.setText("ACTIVITY TYPE: " + csvData.get(2)[1]);
                    steps.setText("COUNT OF ACTUAL STEPS: " + csvData.get(3)[1]);


                    for (int i = 6; i < csvData.size(); i++) {
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
                    Toast.makeText(getActivity(), "ERROR", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }


            }

        });

        // Inflate the layout for this fragment
        return view;
    }

    private void getCSVFiles() {
        File f = new File("/storage/self/primary/IOT");
        // This filter will only include files ending with .py
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File f, String name) {
                return name.endsWith(".csv");
            }
        };
        // This is how to apply the filter
        filenames = f.list(filter);

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


}