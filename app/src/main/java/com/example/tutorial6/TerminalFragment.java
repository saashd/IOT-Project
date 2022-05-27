package com.example.tutorial6;
// binds bluetooth service. Maintains the graph

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.tutorial6.StepsDetections.StepsDetection;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {


    private TextView displayNumOfSteps;
    private TextView displayBPM;
    private TextView displaySPO2;
    private Integer stepCount = 0;


    private Map<String, List<Float>> mRawAccelValues = new HashMap<String, List<Float>>();
    private ArrayList<Double> magVals = new ArrayList<Double>();
    private ArrayList<Double> magValsPos = new ArrayList<Double>();
    private ArrayList<Double> magValsNeg = new ArrayList<Double>();


    private double thresholdWalking = 3.0;
    private double thresholdRunning = 10.0;

    private double avgX, avgY, avgZ, avgIr, avgRed = 0;
    private double sumRedRms, sumIrRms = 0;
    private double ESpO2 = 100.0;

    private int sumplesMag = 100;
    private int samplingsSPO2 = 0;
    private int samplingsBPM = 0;
    private ArrayList<Integer> bpmSamples = new ArrayList<Integer>(Arrays.asList(70, 70, 70, 70, 70));
    private int avgBpm = 70;


    private enum Connected {False, Pending, True;}

    private SerialService service;
    private String deviceAddress;
    private String selectedMode;
    private String fileName;
    private String numOfSteps;
    private Boolean isReceiving = false;
    private TextView receiveText;

    private List<String[]> receivedData = new ArrayList<>();


    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;


    LineChart mpLineChart;
    LineDataSet lineDataSetAxisX;
    LineDataSet lineDataSetAxisY;
    LineDataSet lineDataSetAxisZ;
    LineDataSet lineDataSetAxisAcc;

    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    LineData data;


    private List<Float> irData = new ArrayList<Float>();
    private List<Float> redData = new ArrayList<Float>();
    private List<Float> timeData = new ArrayList<Float>();

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        deviceAddress = getArguments().getString("device");
        selectedMode = getArguments().getString("mode");


    }


    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        super.onDestroy();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));

    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        displayNumOfSteps = view.findViewById(R.id.estimatedSteps);
        displayBPM = view.findViewById(R.id.bpm);
        displaySPO2 = view.findViewById(R.id.spo);

        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
        mpLineChart = (LineChart) view.findViewById(R.id.line_chart);
        mpLineChart.getDescription().setText("Time [sec]");

//        lineDataSetAxisX = new LineDataSet(emptyDataValues(), "X-axis");
//        lineDataSetAxisX.setColors(Color.RED);
//
//        lineDataSetAxisY = new LineDataSet(emptyDataValues(), "Y-axis");
//        lineDataSetAxisY.setColors(Color.GREEN);
//
//        lineDataSetAxisZ = new LineDataSet(emptyDataValues(), "Z-axis");
//        lineDataSetAxisZ.setColors(Color.BLUE);

        lineDataSetAxisAcc = new

                LineDataSet(emptyDataValues(), "Acc. Norm");
        lineDataSetAxisAcc.setColors(R.color.colorPrimaryDark);
//        lineDataSetAxisAcc.setCircleColor(R.color.colorAccent);
//        lineDataSetAxisAcc.setCircleHoleColor(R.color.colorAccent);

        //        Set dataset labels that appear in the bottom of the chart
        Legend l = mpLineChart.getLegend();
        l.setTextSize(15f);
        l.setTextColor(Color.BLACK);
        l.setForm(Legend.LegendForm.LINE);
        XAxis xval = mpLineChart.getXAxis();

        xval.setDrawLabels(true);
        xval.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xval.setGranularity(1f);


//        dataSets.add(lineDataSetAxisX);
//        dataSets.add(lineDataSetAxisY);
//        dataSets.add(lineDataSetAxisZ);
        dataSets.add(lineDataSetAxisAcc);

        data = new

                LineData(dataSets);
        mpLineChart.setData(data);
        mpLineChart.invalidate();

        Button buttonReset = (Button) view.findViewById(R.id.resetButton);
        Button buttonSaveData = (Button) view.findViewById(R.id.saveButton);
        Button buttonStop = (Button) view.findViewById(R.id.stopButton);
        Button buttonStart = (Button) view.findViewById(R.id.startButton);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {


                if (connected.name().equals("True")) {
                    //                Start recording if first time or if data was stopped or reset been made

//TODO: Run code only if really needed, my billing account won't make it till presentation.
//                    MapsFragment childFragment = new MapsFragment();
//                    FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
//                    transaction.replace(R.id.child_fragment_container, childFragment).addToBackStack("map").commit();

                    if (!isReceiving) {
                        isReceiving = true;
                    }
                    displaySPO2.setText("Calculating....");
                    displayBPM.setText("Calculating....");
                    Toast.makeText(getContext(), "Recording Started", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "No device connected", Toast.LENGTH_SHORT).show();

                }


            }

        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseReceiving();

            }
        });


        buttonSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseReceiving();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                final EditText inputSteps = new EditText(getActivity());
                inputSteps.setInputType(InputType.TYPE_CLASS_NUMBER);
                inputSteps.setHint("Enter Number of Steps");

                final EditText inputFileName = new EditText(getActivity());
                inputFileName.setInputType(InputType.TYPE_CLASS_TEXT);
                inputFileName.setHint("Enter File Name");

                LinearLayout lay = new LinearLayout(getContext());
                lay.setOrientation(LinearLayout.VERTICAL);
                lay.addView(inputSteps);
                lay.addView(inputFileName);

                builder.setView(lay)
                        .setCancelable(false)
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                numOfSteps = inputSteps.getText().toString();
                                fileName = inputFileName.getText().toString();
                                if (fileName.matches("")) {
                                    Toast.makeText(getContext(), "Please enter  file name", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (numOfSteps.matches("")) {
                                    Toast.makeText(getContext(), "Please enter number of steps", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                saveToCsv();
                                reset();
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("Return", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //  Action for 'NO' Button
                                dialog.cancel();
                            }
                        });
                //Creating dialog box
                AlertDialog alert = builder.create();
                alert.show();

            }
        });

        buttonReset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FragmentManager manager = getChildFragmentManager();
                manager.popBackStackImmediate();
                reset();
                FragmentManager fm = getActivity()
                        .getSupportFragmentManager();
                fm.popBackStack("fragB", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                Toast.makeText(getContext(), "Reset", Toast.LENGTH_SHORT).show();
            }

        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_devices, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.bt_settings) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            return true;
        } else if (id == R.id.load1) {
            Fragment fragment = new CsvFragment();
            getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private String[] clean_str(String[] stringsArr) {
        for (int i = 0; i < stringsArr.length; i++) {
            stringsArr[i] = stringsArr[i].replaceAll(" ", "");
        }


        return stringsArr;
    }

    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void saveToCsv() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());

            File file = new File("/storage/self/primary/IOT/");
            file.mkdirs();
            String csv = "/storage/self/primary/IOT/" + fileName + ".csv";

            CSVWriter csvWriter = new CSVWriter(new FileWriter(csv, false));
            String[] row = new String[]{"NAME:", fileName + ".csv"};
            csvWriter.writeNext(row);
            row = new String[]{"EXPERIMENT TIME:", currentDateandTime};
            csvWriter.writeNext(row);
            row = new String[]{"ACTIVITY TYPE:", selectedMode};
            csvWriter.writeNext(row);
            row = new String[]{"COUNT OF ACTUAL STEPS", numOfSteps};
            csvWriter.writeNext(row);
            row = new String[]{"ESTIMATED NUMBER OF STEPS", stepCount.toString()};
            csvWriter.writeNext(row);
            row = new String[]{"   "};
            csvWriter.writeNext(row);
            row = new String[]{"Time [sec]", "ACC X", "ACC Y", "ACC Z"};
            csvWriter.writeNext(row);
            for (String[] r : receivedData) {
                csvWriter.writeNext(r);
            }
            csvWriter.close();
            Toast.makeText(getContext(), "Data Saved", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(getContext(), "ERROR", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void reset() {
        //Reset Saved Data
        isReceiving = false;
        receivedData = new ArrayList<>();
        stepCount = 0;
        displayNumOfSteps.setText(stepCount.toString());
        displayBPM.setText("");
        displaySPO2.setText("");


        //Clear displayed graph
        LineData data = mpLineChart.getData();
        ILineDataSet set1 = data.getDataSetByIndex(0);
//        ILineDataSet set2 = data.getDataSetByIndex(1);
//        ILineDataSet set3 = data.getDataSetByIndex(2);
//        ILineDataSet set4 = data.getDataSetByIndex(3);
        set1.removeLast();
//        set2.removeLast();
//        set3.removeLast();
//        set4.removeLast();
        while (set1.removeLast()) {
        }
//        while (set2.removeLast()) {
//        }
//        while (set3.removeLast()) {
//
//        }
//        while (set4.removeLast()) {
//
//        }
        mpLineChart.notifyDataSetChanged();
        mpLineChart.invalidate();
        receiveText.setText("");

    }

    private void pauseReceiving() {
        isReceiving = false;
        Toast.makeText(getContext(), "Data Receiving Paused", Toast.LENGTH_SHORT).show();
    }


    // Updates done while message received from the device
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void receive(byte[] message) {
//        if (hexEnabled) {
//            receiveText.append(TextUtil.toHexString(message) + '\n');
//        } else {
        String msg = new String(message);
        if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
            // don't show CR as ^M if directly before LF
            String msg_to_save = msg;
            msg_to_save = msg.replace(TextUtil.newline_crlf, TextUtil.emptyString);
            if (msg_to_save.length() > 1) {

                String[] parts = msg_to_save.split(",");
                parts = clean_str(parts);


                float xVal = Float.parseFloat(parts[0]);
                float yVal = Float.parseFloat(parts[1]);
                float zVal = Float.parseFloat(parts[2]);
                float time_msec = Float.parseFloat(parts[3]);

                double rate = 0.333;
                avgX = avgX * rate + (double) xVal * (1.0 - rate);//average val level by low pass filter
                avgY = avgY * rate + (double) yVal * (1.0 - rate);//average val level by low pass filter
                avgZ = avgZ * rate + (double) zVal * (1.0 - rate);//average val level by low pass filter


                String row[] = new String[]{String.valueOf(time_msec / 1000.0), parts[0], parts[1], parts[2]};
                receivedData.add(row);

                double mag = Math.sqrt(Math.pow(xVal, 2) + Math.pow(yVal, 2) + Math.pow(zVal, 2));

                addVal("x", Float.parseFloat(parts[0]));
                addVal("y", Float.parseFloat(parts[1]));
                addVal("z", Float.parseFloat(parts[2]));
                addVal("mag", (float) mag);

                double magNoG = calcSteps(mag);
                data.addEntry(new Entry(time_msec / 1000, (float) magNoG), 0);
                lineDataSetAxisAcc.notifyDataSetChanged(); // let the data know a dataSet changed
                mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
                mpLineChart.invalidate(); // refresh

                calcHealthParams(parts[4], parts[5], time_msec);
            }

            msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
            // sand here msg to function that saves it to csv
            // special handling if CR and LF come in separate fragments
            if (pendingNewline && msg.charAt(0) == '\n') {
                Editable edt = receiveText.getEditableText();
                if (edt != null && edt.length() > 1)
                    edt.replace(edt.length() - 2, edt.length(), "");
            }
            pendingNewline = msg.charAt(msg.length() - 1) == '\r';
        }
        receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));
    }

    private double[] lowPassFilter(int val, double avgVal, double sumRms, double frate) {
        avgVal = avgVal * frate + (double) val * (1.0 - frate);//average val level by low pass filter
        sumRms += (val - avgVal) * (val - avgVal); //square sum of alternate component of red level
        return new double[]{avgVal, sumRms};

    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.N)
    private double calcSteps(double mag) {
        //              Calculate Steps
        double meanMag = mRawAccelValues.get("mag").stream()
                .mapToDouble(d -> d)
                .average()
                .orElse(0.0);
        double magNoG = mag - meanMag;

        if (magNoG >= 0) {
            magValsPos.add(magNoG);
        } else {
            magValsNeg.add(magNoG);
        }


        double thresholdPos = selectedMode.equals("Walking") ? thresholdWalking : thresholdRunning;
        if (magValsPos.size() > sumplesMag) {
            thresholdPos = new StepsDetection(getContext(), thresholdPos).findPeak(magValsPos);
            Log.println(Log.ASSERT, "threshold", String.valueOf(thresholdPos));
            sumplesMag += 100;
        }
        if (magNoG >= thresholdPos) {
            stepCount += 1;
        }


        displayNumOfSteps.setText(stepCount.toString());
        return magNoG;

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void calcHealthParams(String ir, String red, float time_msec) {
//                Calculate SPO2
        int irVal = Integer.parseInt(ir);
        int redVal = Integer.parseInt(red);
        File file = new File("/storage/self/primary/IOT/");
        file.mkdirs();
        String csv = "/storage/self/primary/IOT/oximeterData.csv";

        CSVWriter csvWriter = null;
        try {
            csvWriter = new CSVWriter(new FileWriter(csv, true));
            String[] row = new String[]{String.valueOf(time_msec), String.valueOf(irVal), String.valueOf(redVal)};
            csvWriter.writeNext(row);
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        double frate = 0.95; //low pass filter for IR/red LED value to eliminate AC component
        avgIr = lowPassFilter(irVal, avgIr, sumIrRms, frate)[0];
        sumIrRms = lowPassFilter(irVal, avgIr, sumIrRms, frate)[1];
        avgRed = lowPassFilter(redVal, avgRed, sumRedRms, frate)[0];
        sumRedRms = lowPassFilter(redVal, avgRed, sumRedRms, frate)[1];

        samplingsSPO2 += 1;
        if (samplingsSPO2 % 50 == 0) {
            SPO2();
            sumIrRms = 0;
            sumRedRms = 0;
            samplingsSPO2 = 0;
            avgIr = 0;
            avgRed = 0;
            ESpO2 = 100.0;

        }

//                Calculate BPM
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(getContext()));
        }
        Python py = Python.getInstance();
        PyObject pyobj = py.getModule("test");


        if (irVal > 50000) {
            timeData.add((float) (time_msec / 1000.0)); //milli sec to sec
            irData.add((float) irVal);
            redData.add((float) redVal);
            samplingsBPM += 1;
        }
        if (samplingsBPM % 30 == 0) {
            PyObject obj = pyobj.callAttr("calcBPM", timeData.toArray(), irData.toArray(), redData.toArray());
            int avgSize = 5;
            int currentBpm = obj.toInt();
            avgBpm = bpmSamples.stream().mapToInt(Integer::intValue).sum() / avgSize;
            if (currentBpm > 1.5 * avgBpm || currentBpm < avgBpm / 1.5) {
                bpmSamples.add((int) Math.round(0.9 * avgBpm + 0.1 * currentBpm));
            } else {
                bpmSamples.add(currentBpm);
            }
            bpmSamples.remove(0);

            timeData = new ArrayList<Float>();
            irData = new ArrayList<Float>();
            redData = new ArrayList<Float>();
            samplingsBPM = 0;

            Log.println(Log.ASSERT, "bpm", obj.toString());
            Log.println(Log.ASSERT, "avg bpm", String.valueOf(avgBpm));

            if (40.0 < avgBpm && avgBpm < 250.0) {
                displayBPM.setText(String.valueOf(avgBpm));

            }
        }

    }

    private void SPO2() {
        double R = (Math.sqrt(sumRedRms) / avgRed) / (Math.sqrt(sumIrRms) / avgIr);
        double SpO2 = -23.3 * (R - 0.4) + 100;
        ESpO2 = 0.7 * ESpO2 + (1.0 - 0.7) * SpO2;
        Log.println(Log.ASSERT, "spo", String.valueOf(ESpO2));
        if (90.0 < ESpO2 && ESpO2 < 100.0) {
            displaySPO2.setText(String.valueOf((int) Math.round(ESpO2)));

        }
    }


    private void addVal(String key, Float val) {
        if (!mRawAccelValues.containsKey(key)) {
            mRawAccelValues.put(key, new ArrayList<Float>());
        }
        mRawAccelValues.get(key).add(val);
    }


    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSerialRead(byte[] data) {
        if (isReceiving) {
            try {
                receive(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private ArrayList<Entry> emptyDataValues() {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        return dataVals;
    }


}
