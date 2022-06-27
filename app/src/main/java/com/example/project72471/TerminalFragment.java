package com.example.project72471;
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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
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
import androidx.fragment.app.FragmentTransaction;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.project72471.LocationTracker.MapsFragment;
import com.example.project72471.LocationTracker.MyLocation;
import com.example.project72471.Serial.SerialListener;
import com.example.project72471.Serial.SerialService;
import com.example.project72471.Serial.SerialSocket;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opencsv.CSVWriter;
import com.project72471.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
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
    private TextView receiveText;
    private Integer stepsCounted = 0;


    private Map<String, List<Float>> mRawAccelValues = new HashMap<String, List<Float>>();
    private ArrayList<Double> meanMagVals = new ArrayList<Double>();


    private double avgIr, avgRed = 0;
    private double sumRedRms, sumIrRms = 0;
    private double ESpO2 = 100.0;

    private int samplingsSPO2 = 0;
    private int samplingsBPM = 0;
    private ArrayList<Integer> bpmSamples = new ArrayList<Integer>(Arrays.asList(70, 70, 70, 70, 70));
    private int avgBpm = 70;
    private PyObject pyobjTest;
    private PyObject pyobjcalcStepsAlgo;

    private DatabaseReference reference;
    private String userUid;
    private String todayDate;
    private Instant startRec;
    private Instant endRec;
    private boolean popUpDialog = true;
    private boolean popUpStarted = false;
    private MapsFragment childFragment;


    private enum Connected {False, Pending, True;}

    private SerialService service;
    private String deviceAddress;
    private String selectedMode;
    private String fileName;
    private String numOfSteps;
    private Boolean isReceiving = false;

    private List<String[]> receivedData = new ArrayList<>();


    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;


    LineChart mpLineChart;
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


        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(getContext()));
        }
        deviceAddress = getArguments().getString("device");
        selectedMode = getArguments().getString("mode");
        Python py = Python.getInstance();
        pyobjTest = py.getModule("test");
        pyobjcalcStepsAlgo = py.getModule("calcStepsAlgo");

        userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        todayDate = DateFormat.getDateTimeInstance().format(new Date());
        reference = FirebaseDatabase.getInstance().getReference().child("Users").child(userUid).child(todayDate);

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


        lineDataSetAxisAcc = new LineDataSet(emptyDataValues(), "");
        lineDataSetAxisAcc.setColors(R.color.colorPrimaryDark);


        //        Set dataset labels that appear in the bottom of the chart
        Legend l = mpLineChart.getLegend();
        l.setTextSize(15f);
        l.setTextColor(Color.BLACK);
        l.setForm(Legend.LegendForm.LINE);
        XAxis xval = mpLineChart.getXAxis();

        xval.setDrawLabels(true);
        xval.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xval.setGranularity(1f);


        dataSets.add(lineDataSetAxisAcc);

        data = new LineData(dataSets);
        mpLineChart.setData(data);
        mpLineChart.getAxisLeft().setAxisMinimum(0);
        mpLineChart.invalidate();

        Button buttonReset = (Button) view.findViewById(R.id.resetButton);
        Button buttonSaveData = (Button) view.findViewById(R.id.saveButton);
        Button buttonStop = (Button) view.findViewById(R.id.stopButton);
        Button buttonStart = (Button) view.findViewById(R.id.startButton);


        buttonStart.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {


                if (connected.name().equals("True")) {
                    //                Start recording if first time or if data was stopped or reset been made

//TODO: Run code only if really needed, my billing account won't make it till presentation.
                    Bundle args = new Bundle();
                    childFragment = new MapsFragment();
                    childFragment.setArguments(args);
                    FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                    transaction.replace(R.id.child_fragment_container, childFragment).addToBackStack("map").commit();


                    if (!isReceiving) {
                        isReceiving = true;
                    }
                    displaySPO2.setText("Calculating....");
                    displayBPM.setText("Calculating....");
                    Toast.makeText(getContext(), "Recording Started", Toast.LENGTH_SHORT).show();
                    startRec = Instant.now();
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
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                endRec = Instant.now();
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
                            @RequiresApi(api = Build.VERSION_CODES.O)
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
                                saveToDB();
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveToDB() {
        reference.child(selectedMode).child("steps").setValue(stepsCounted);
        reference.child(selectedMode).child("mode").setValue(selectedMode);
        long timeElapsed = Duration.between(startRec, endRec).toMillis();
        reference.child(selectedMode).child("duration").setValue(timeElapsed);


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
            row = new String[]{"ESTIMATED NUMBER OF STEPS", stepsCounted.toString()};
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
        stepsCounted = 0;
        displayNumOfSteps.setText(stepsCounted.toString());
        mRawAccelValues = new HashMap<String, List<Float>>();
        displayBPM.setText("");
        displaySPO2.setText("");
        receiveText.setText("");


        //Clear displayed graph
        LineData data = mpLineChart.getData();
        ILineDataSet set1 = data.getDataSetByIndex(0);
        set1.removeLast();
        while (set1.removeLast()) {
        }
        mpLineChart.notifyDataSetChanged();
        mpLineChart.invalidate();

    }

    private void pauseReceiving() {
        isReceiving = false;
        Toast.makeText(getContext(), "Data Receiving Paused", Toast.LENGTH_SHORT).show();
    }


    // Updates done while message received from the device
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void receive(byte[] message) {
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

            pendingNewline = msg.charAt(msg.length() - 1) == '\r';
        }
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
        if (mRawAccelValues.get("x").size() > 0 && mRawAccelValues.get("x").size() % 50 == 0) {
            PyObject obj = pyobjcalcStepsAlgo.callAttr("steps_count", mRawAccelValues.get("x").toArray(), mRawAccelValues.get("y").toArray(), mRawAccelValues.get("z").toArray());
            if (obj != null && obj.toInt() > stepsCounted) {
                stepsCounted = obj.toInt();
            }
        }
        double meanMag = mRawAccelValues.get("mag").stream()
                .mapToDouble(d -> d)
                .average()
                .orElse(0.0);
        meanMagVals.add(meanMag);
        double magNoG = mag - meanMag;

        displayNumOfSteps.setText(stepsCounted.toString());
        return magNoG > 2.0 ? magNoG : 0;

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void calcHealthParams(String ir, String red, float time_msec) {
        int irVal = Integer.parseInt(ir);
        int redVal = Integer.parseInt(red);
        if (irVal > 5000) {
            double frate = 0.95; //low pass filter for IR/red LED value to eliminate AC component
            avgIr = lowPassFilter(irVal, avgIr, sumIrRms, frate)[0];
            sumIrRms = lowPassFilter(irVal, avgIr, sumIrRms, frate)[1];
            avgRed = lowPassFilter(redVal, avgRed, sumRedRms, frate)[0];
            sumRedRms = lowPassFilter(redVal, avgRed, sumRedRms, frate)[1];
            samplingsSPO2 += 1;
            timeData.add((float) (time_msec / 1000.0)); //milli sec to sec
            irData.add((float) irVal);
            redData.add((float) redVal);
            samplingsBPM += 1;

        }
        //                Calculate SPO2
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
        if (bpmSamples.size() != 0 && samplingsBPM % 30 == 0) {
            PyObject obj = pyobjTest.callAttr("calcBPM", timeData.toArray(), irData.toArray(), redData.toArray());
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


            if (40.0 < avgBpm && avgBpm < 250.0) {
                displayBPM.setText(String.valueOf(avgBpm));

            }
            if (popUpDialog && !popUpStarted && avgBpm > 150) {
                popUpStarted = true;
                PopUpWindow();
            }

        }

    }

    private void SPO2() {
        double R = (Math.sqrt(sumRedRms) / avgRed) / (Math.sqrt(sumIrRms) / avgIr);
        double SpO2 = -23.3 * (R - 0.4) + 100;
        ESpO2 = 0.7 * ESpO2 + (1.0 - 0.7) * SpO2;
        if (90.0 < ESpO2 && ESpO2 < 100.0) {
            displaySPO2.setText(String.valueOf((int) Math.round(ESpO2)));

        }
    }


    private void PopUpWindow() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
        r.play();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final TextView text = new TextView(getActivity());
        text.setText("You Heart Rate is Critical!\n Do You want to notify Your Emergency Contact ");


        LinearLayout lay = new LinearLayout(getContext());
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.addView(text);

        builder.setView(lay)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    public void onClick(DialogInterface dialog, int id) {
                        r.stop();

                        pauseReceiving();
                        MyLocation currLocation = childFragment.getCurrentLocation();
                        FirebaseDatabase.getInstance().getReference().child("Users").child(userUid).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                String emergencyContact = "";
                                Object data = dataSnapshot.child("emergencyContact").getValue();
                                if (data != null) {
                                    emergencyContact = (String) data;
                                }

                                Intent email = new Intent(Intent.ACTION_SEND);
                                email.putExtra(Intent.EXTRA_EMAIL, new String[]{emergencyContact});
                                email.putExtra(Intent.EXTRA_SUBJECT, "Emergency");
                                email.putExtra(Intent.EXTRA_TEXT, "My Current Location is: \n\n latitude: " + currLocation.getLatitude() + "\n longitude: " + currLocation.getLongitude() + "\n\n Current Heart Rate is: " + avgBpm + " bpm");
                                email.setType("message/rfc822");
                                startActivity(Intent.createChooser(email, "Send mail..."));

                                dialog.cancel();
                                popUpDialog = false;
                                popUpStarted = false;


                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });


                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        r.stop();
                        //  Action for 'NO' Button
                        dialog.cancel();
                        popUpDialog = false;
                        popUpStarted = false;
                    }
                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        alert.show();
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
